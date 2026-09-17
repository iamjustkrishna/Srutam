import { createClient, SupabaseClient } from '@supabase/supabase-js';
import crypto from 'crypto';
import { SrutamConfig } from './config.js';
import { TtlCache, TokenBucketRateLimiter } from './cache.js';

export interface NoteRecord {
  id: string;
  user_id: string;
  title: string;
  transcript: string | null;
  summary: string | null;
  key_points: string[] | any;
  wiifm: string | null;
  ai_status: string;
  duration_ms: number;
  timestamp: string;
  is_private: boolean;
}

export interface ActionItemRecord {
  id: string;
  note_id: string;
  description: string;
  is_completed: boolean;
  completed_by: string | null;
  completed_at: string | null;
  created_at: string;
}

export interface AgentLogRecord {
  id: string;
  note_id: string;
  agent_name: string;
  message: string;
  created_at: string;
}

export class SrutamClient {
  private client: SupabaseClient;
  private apiKey: string;
  private keyHash: string;
  private authenticatedUserId: string | null = null;

  // Shared in-memory caches and rate limiter across requests
  private static authCache = new TtlCache<string>(60); // 60s key validation cache
  private static readCache = new TtlCache<any>(30);    // 30s query cache
  private static rateLimiter = new TokenBucketRateLimiter(60); // 60 requests/min per key

  constructor(config: SrutamConfig) {
    this.client = createClient(config.supabaseUrl, config.supabaseAnonKey);
    this.apiKey = config.apiKey;
    this.keyHash = crypto
      .createHash('sha256')
      .update(this.apiKey)
      .digest('hex');
  }

  async authenticate(): Promise<string> {
    // 1. Rate limiter check (prevent runaway loops)
    const rateCheck = SrutamClient.rateLimiter.tryConsume(this.keyHash);
    if (!rateCheck.allowed) {
      throw new Error('Rate limit exceeded (60 requests/minute per key). Please throttle agent queries.');
    }

    // 2. Instance memory check
    if (this.authenticatedUserId) {
      return this.authenticatedUserId;
    }

    // 3. TTL Cache check
    const cachedUserId = SrutamClient.authCache.get(this.keyHash);
    if (cachedUserId) {
      this.authenticatedUserId = cachedUserId;
      return cachedUserId;
    }

    // 4. Supabase RPC check
    const { data, error } = await this.client.rpc('verify_srutam_api_key', {
      raw_key_hash: this.keyHash,
    });

    if (error || !data) {
      throw new Error(
        `Failed to authenticate with Srutam Cloud: ${error?.message || 'Invalid or revoked SRUTAM_API_KEY'}`
      );
    }

    const userId = data as string;
    this.authenticatedUserId = userId;
    SrutamClient.authCache.set(this.keyHash, userId, 60);
    return userId;
  }

  async searchNotes(query: string, limit: number = 5): Promise<any[]> {
    const userId = await this.authenticate();
    const boundedLimit = Math.max(1, Math.min(limit, 25));
    const cleanQuery = query.trim().slice(0, 200);

    const cacheKey = `search:${userId}:${cleanQuery}:${boundedLimit}`;
    const cached = SrutamClient.readCache.get(cacheKey);
    if (cached) return cached;

    const { data, error } = await this.client.rpc('mcp_search_notes', {
      p_user_id: userId,
      p_query: cleanQuery,
      p_limit: boundedLimit,
    });

    if (error) {
      throw new Error(`Search notes query failed: ${error.message}`);
    }

    const results = data || [];
    SrutamClient.readCache.set(cacheKey, results, 30);
    return results;
  }

  async listRecentNotes(limit: number = 10): Promise<NoteRecord[]> {
    return this.searchNotes('', limit);
  }

  async getNoteDetail(noteId: string): Promise<{
    note: NoteRecord;
    actionItems: ActionItemRecord[];
    agentLogs: AgentLogRecord[];
  }> {
    const userId = await this.authenticate();
    const cleanNoteId = noteId.trim();

    const cacheKey = `note:${userId}:${cleanNoteId}`;
    const cached = SrutamClient.readCache.get(cacheKey);
    if (cached) return cached;

    const { data, error } = await this.client.rpc('mcp_get_note_detail', {
      p_user_id: userId,
      p_note_id: cleanNoteId,
    });

    if (error || !data || !data.note) {
      throw new Error(`Note not found or private: ${error?.message || 'Not found'}`);
    }

    const result = {
      note: data.note as NoteRecord,
      actionItems: (data.actionItems as ActionItemRecord[]) || [],
      agentLogs: (data.agentLogs as AgentLogRecord[]) || [],
    };

    SrutamClient.readCache.set(cacheKey, result, 30);
    return result;
  }

  async listActionItems(status: 'all' | 'pending' | 'completed' = 'pending', limit: number = 20): Promise<ActionItemRecord[]> {
    const userId = await this.authenticate();
    const boundedLimit = Math.max(1, Math.min(limit, 50));

    const cacheKey = `actions:${userId}:${status}:${boundedLimit}`;
    const cached = SrutamClient.readCache.get(cacheKey);
    if (cached) return cached;

    const { data, error } = await this.client.rpc('mcp_list_action_items', {
      p_user_id: userId,
      p_status: status,
      p_limit: boundedLimit,
    });

    if (error) {
      throw new Error(`Failed to list action items: ${error.message}`);
    }

    const items = (data as ActionItemRecord[]) || [];
    SrutamClient.readCache.set(cacheKey, items, 20);
    return items;
  }

  async updateActionItem(
    actionItemId: string,
    completed: boolean,
    agentName: string = 'AI Agent'
  ): Promise<ActionItemRecord> {
    const userId = await this.authenticate();

    const { data, error } = await this.client.rpc('mcp_update_action_item', {
      p_user_id: userId,
      p_item_id: actionItemId.trim(),
      p_completed: completed,
      p_agent_name: agentName.trim().slice(0, 100),
    });

    if (error || !data) {
      throw new Error(`Failed to update action item: ${error?.message || 'Not found'}`);
    }

    // Invalidate read cache on updates
    SrutamClient.readCache.clear();
    return data as ActionItemRecord;
  }

  async appendAgentLog(
    noteId: string,
    message: string,
    agentName: string = 'AI Agent'
  ): Promise<AgentLogRecord> {
    const userId = await this.authenticate();

    const { data, error } = await this.client.rpc('mcp_append_agent_log', {
      p_user_id: userId,
      p_note_id: noteId.trim(),
      p_agent_name: agentName.trim().slice(0, 100),
      p_message: message.trim().slice(0, 4000),
    });

    if (error || !data) {
      throw new Error(`Failed to append agent log: ${error?.message || 'Insert failed'}`);
    }

    // Invalidate note detail cache
    SrutamClient.readCache.delete(`note:${userId}:${noteId.trim()}`);
    return data as AgentLogRecord;
  }

  /**
   * Health and status check used by CLI dashboard.
   */
  async getCloudStatus(): Promise<{ userId: string; noteCount: number; pendingActionsCount: number }> {
    const userId = await this.authenticate();

    const notesRes = await this.client
      .from('notes')
      .select('*', { count: 'exact', head: true })
      .eq('user_id', userId);

    const actionsRes = await this.client
      .from('action_items')
      .select('*', { count: 'exact', head: true })
      .eq('user_id', userId)
      .eq('is_completed', false);

    return {
      userId,
      noteCount: notesRes.count || 0,
      pendingActionsCount: actionsRes.count || 0,
    };
  }
}
