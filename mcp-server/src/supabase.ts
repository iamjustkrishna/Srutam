import { createClient, SupabaseClient } from '@supabase/supabase-js';
import crypto from 'crypto';
import { SrutamConfig } from './config.js';
import { TtlCache, TokenBucketRateLimiter } from './cache.js';

/** Row shape returned by mcp_search_notes (no transcript - fetch it with get_note_detail). */
export interface NoteSummary {
  id: string;
  title: string;
  summary: string | null;
  key_points: unknown;
  wiifm: string | null;
  duration_ms: number | null;
  timestamp: string;
  similarity_rank?: number;
}

/** Row shape returned inside mcp_get_note_detail. */
export interface NoteRecord extends NoteSummary {
  transcript: string | null;
  ai_status: string;
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

export interface InsightRecord {
  id: string;
  note_id: string;
  kind: 'idea' | 'decision';
  text: string;
  evidence: string | null;
  rationale: string | null;
  created_at: string;
}

export interface ReminderRecord {
  id: string;
  note_id: string;
  title: string;
  event_time: string | null;
  original_text: string | null;
  person: string | null;
  location: string | null;
  type: string | null;
  status: string;
  created_at: string;
}

export interface CloudStatus {
  userId: string;
  noteCount: number;
  pendingActionsCount: number;
}

export const AUTH_ERROR_MESSAGE =
  'Srutam rejected the API key (invalid or revoked). Generate a new key in the Srutam app and run "srutam-mcp init".';

/**
 * Client for the Srutam MCP RPC layer.
 *
 * Authorization model: every RPC receives the SHA-256 hash of the API key and the database
 * resolves the owning user itself. The client never asserts a user id, so knowing (or guessing)
 * someone's UUID gives no access. There is deliberately no client-side "authenticated" state:
 * revoking a key on the phone takes effect on the very next call (only the 30s read cache lags).
 */
export class SrutamClient {
  private client: SupabaseClient;
  private keyHash: string;

  private readCache = new TtlCache<any>(30); // 30s micro-cache for read queries
  // Guard against runaway agent loops. NOT a security control: it lives in this process only.
  private rateLimiter = new TokenBucketRateLimiter(60);

  constructor(config: SrutamConfig) {
    this.client = createClient(config.supabaseUrl, config.supabaseAnonKey, {
      auth: { persistSession: false, autoRefreshToken: false, detectSessionInUrl: false },
    });
    this.keyHash = crypto.createHash('sha256').update(config.apiKey).digest('hex');
  }

  private guard(): void {
    const rate = this.rateLimiter.tryConsume(this.keyHash);
    if (!rate.allowed) {
      throw new Error(
        `Rate limit exceeded (60 requests/minute). Slow down and retry in ${rate.retryAfterSeconds}s.`
      );
    }
  }

  private async rpc<T>(fn: string, args: Record<string, unknown>): Promise<T | null> {
    const { data, error } = await this.client.rpc(fn, { p_key_hash: this.keyHash, ...args });

    if (error) {
      if (error.code === '28000' || error.message === 'unauthorized') {
        this.readCache.clear();
        throw new Error(AUTH_ERROR_MESSAGE);
      }
      throw new Error(`${fn} failed: ${error.message}`);
    }
    return (data as T) ?? null;
  }

  async searchNotes(query: string, limit: number = 5): Promise<NoteSummary[]> {
    this.guard();
    const boundedLimit = Math.max(1, Math.min(limit, 25));
    const cleanQuery = query.trim().slice(0, 200);

    const cacheKey = `search:${cleanQuery}:${boundedLimit}`;
    const cached = this.readCache.get(cacheKey);
    if (cached) return cached;

    const data = await this.rpc<NoteSummary[]>('mcp_search_notes', {
      p_query: cleanQuery,
      p_limit: boundedLimit,
    });

    const results = Array.isArray(data) ? data : [];
    this.readCache.set(cacheKey, results, 30);
    return results;
  }

  async listRecentNotes(limit: number = 10): Promise<NoteSummary[]> {
    return this.searchNotes('', limit);
  }

  async getNoteDetail(noteId: string): Promise<{
    note: NoteRecord;
    actionItems: ActionItemRecord[];
    agentLogs: AgentLogRecord[];
  }> {
    this.guard();
    const cleanNoteId = noteId.trim();

    const cacheKey = `note:${cleanNoteId}`;
    const cached = this.readCache.get(cacheKey);
    if (cached) return cached;

    const data = await this.rpc<{
      note?: NoteRecord;
      actionItems?: ActionItemRecord[];
      agentLogs?: AgentLogRecord[];
    }>('mcp_get_note_detail', { p_note_id: cleanNoteId });

    if (!data || !data.note) {
      throw new Error('Note not found, or it is marked private in Srutam and hidden from agents.');
    }

    const result = {
      note: data.note,
      actionItems: data.actionItems || [],
      agentLogs: data.agentLogs || [],
    };

    this.readCache.set(cacheKey, result, 30);
    return result;
  }

  async listActionItems(
    status: 'all' | 'pending' | 'completed' = 'pending',
    limit: number = 20
  ): Promise<ActionItemRecord[]> {
    this.guard();
    const boundedLimit = Math.max(1, Math.min(limit, 50));

    const cacheKey = `actions:${status}:${boundedLimit}`;
    const cached = this.readCache.get(cacheKey);
    if (cached) return cached;

    const data = await this.rpc<ActionItemRecord[]>('mcp_list_action_items', {
      p_status: status,
      p_limit: boundedLimit,
    });

    const items = Array.isArray(data) ? data : [];
    this.readCache.set(cacheKey, items, 20);
    return items;
  }

  async listInsights(
    kind: 'all' | 'idea' | 'decision' = 'all',
    limit: number = 20
  ): Promise<InsightRecord[]> {
    this.guard();
    const boundedLimit = Math.max(1, Math.min(limit, 50));

    const cacheKey = `insights:${kind}:${boundedLimit}`;
    const cached = this.readCache.get(cacheKey);
    if (cached) return cached;

    const data = await this.rpc<InsightRecord[]>('mcp_list_insights', {
      p_kind: kind,
      p_limit: boundedLimit,
    });

    const items = Array.isArray(data) ? data : [];
    this.readCache.set(cacheKey, items, 20);
    return items;
  }

  async listReminders(
    upcomingOnly: boolean = true,
    limit: number = 20
  ): Promise<ReminderRecord[]> {
    this.guard();
    const boundedLimit = Math.max(1, Math.min(limit, 50));

    const cacheKey = `reminders:${upcomingOnly}:${boundedLimit}`;
    const cached = this.readCache.get(cacheKey);
    if (cached) return cached;

    const data = await this.rpc<ReminderRecord[]>('mcp_list_reminders', {
      p_upcoming_only: upcomingOnly,
      p_limit: boundedLimit,
    });

    const items = Array.isArray(data) ? data : [];
    this.readCache.set(cacheKey, items, 20);
    return items;
  }

  async updateActionItem(
    actionItemId: string,
    completed: boolean,
    agentName: string = 'AI Agent'
  ): Promise<ActionItemRecord> {
    this.guard();

    const data = await this.rpc<ActionItemRecord>('mcp_update_action_item', {
      p_item_id: actionItemId.trim(),
      p_completed: completed,
      p_agent_name: agentName.trim().slice(0, 100),
    });

    if (!data) {
      throw new Error('Action item not found, or it belongs to a note marked private in Srutam.');
    }

    // Invalidate read cache on updates
    this.readCache.clear();
    return data;
  }

  async appendAgentLog(
    noteId: string,
    message: string,
    agentName: string = 'AI Agent'
  ): Promise<AgentLogRecord> {
    this.guard();
    const cleanNoteId = noteId.trim();

    const data = await this.rpc<AgentLogRecord>('mcp_append_agent_log', {
      p_note_id: cleanNoteId,
      p_agent_name: agentName.trim().slice(0, 100),
      p_message: message.trim().slice(0, 4000),
    });

    if (!data) {
      throw new Error('Failed to append agent log: no row was returned.');
    }

    // Invalidate note detail cache
    this.readCache.delete(`note:${cleanNoteId}`);
    return data;
  }

  /**
   * Health/status check used by the CLI. Also serves as the API-key verification step in the
   * setup wizard: it throws AUTH_ERROR_MESSAGE for an invalid or revoked key.
   */
  async getCloudStatus(): Promise<CloudStatus> {
    this.guard();
    const data = await this.rpc<CloudStatus>('mcp_cloud_status', {});
    if (!data) {
      throw new Error('mcp_cloud_status returned no data.');
    }
    return data;
  }
}
