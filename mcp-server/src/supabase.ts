import { createClient, SupabaseClient } from '@supabase/supabase-js';
import crypto from 'crypto';
import { SrutamConfig } from './config.js';

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

  constructor(config: SrutamConfig) {
    this.client = createClient(config.supabaseUrl, config.supabaseAnonKey);
    this.apiKey = config.apiKey;
    this.keyHash = crypto
      .createHash('sha256')
      .update(this.apiKey)
      .digest('hex');
  }

  async authenticate(): Promise<string> {
    if (this.authenticatedUserId) {
      return this.authenticatedUserId;
    }

    const { data, error } = await this.client.rpc('verify_srutam_api_key', {
      raw_key_hash: this.keyHash,
    });

    if (error || !data) {
      throw new Error(
        `Failed to authenticate with Srutam Cloud: ${error?.message || 'Invalid or revoked SRUTAM_API_KEY'}`
      );
    }

    this.authenticatedUserId = data as string;
    return this.authenticatedUserId;
  }

  async searchNotes(query: string, limit: number = 5): Promise<any[]> {
    const userId = await this.authenticate();

    const { data, error } = await this.client.rpc('mcp_search_notes', {
      p_user_id: userId,
      p_query: query,
      p_limit: limit,
    });

    if (error) {
      throw new Error(`Search notes query failed: ${error.message}`);
    }

    return data || [];
  }

  async listRecentNotes(limit: number = 10): Promise<NoteRecord[]> {
    const userId = await this.authenticate();

    const { data, error } = await this.client
      .from('notes')
      .select('id, user_id, title, summary, key_points, wiifm, duration_ms, timestamp, is_private, ai_status')
      .eq('user_id', userId)
      .eq('is_private', false)
      .order('timestamp', { ascending: false })
      .limit(limit);

    if (error) {
      throw new Error(`Failed to list recent notes: ${error.message}`);
    }

    return (data as NoteRecord[]) || [];
  }

  async getNoteDetail(noteId: string): Promise<{
    note: NoteRecord;
    actionItems: ActionItemRecord[];
    agentLogs: AgentLogRecord[];
  }> {
    const userId = await this.authenticate();

    const { data: note, error: noteError } = await this.client
      .from('notes')
      .select('*')
      .eq('id', noteId)
      .eq('user_id', userId)
      .eq('is_private', false)
      .single();

    if (noteError || !note) {
      throw new Error(`Note not found or private: ${noteError?.message || 'Not found'}`);
    }

    const { data: actionItems } = await this.client
      .from('action_items')
      .select('*')
      .eq('note_id', noteId)
      .order('created_at', { ascending: true });

    const { data: agentLogs } = await this.client
      .from('agent_logs')
      .select('*')
      .eq('note_id', noteId)
      .order('created_at', { ascending: true });

    return {
      note: note as NoteRecord,
      actionItems: (actionItems as ActionItemRecord[]) || [],
      agentLogs: (agentLogs as AgentLogRecord[]) || [],
    };
  }

  async listActionItems(status: 'all' | 'pending' | 'completed' = 'pending', limit: number = 20): Promise<ActionItemRecord[]> {
    const userId = await this.authenticate();

    let query = this.client
      .from('action_items')
      .select('id, note_id, description, is_completed, completed_by, completed_at, created_at')
      .eq('user_id', userId);

    if (status === 'pending') {
      query = query.eq('is_completed', false);
    } else if (status === 'completed') {
      query = query.eq('is_completed', true);
    }

    const { data, error } = await query
      .order('created_at', { ascending: false })
      .limit(limit);

    if (error) {
      throw new Error(`Failed to list action items: ${error.message}`);
    }

    return (data as ActionItemRecord[]) || [];
  }

  async updateActionItem(
    actionItemId: string,
    completed: boolean,
    agentName: string = 'AI Agent'
  ): Promise<ActionItemRecord> {
    const userId = await this.authenticate();

    const { data, error } = await this.client
      .from('action_items')
      .update({
        is_completed: completed,
        completed_by: completed ? `agent:${agentName}` : null,
        completed_at: completed ? new Date().toISOString() : null,
      })
      .eq('id', actionItemId)
      .eq('user_id', userId)
      .select()
      .single();

    if (error || !data) {
      throw new Error(`Failed to update action item: ${error?.message || 'Not found'}`);
    }

    return data as ActionItemRecord;
  }

  async appendAgentLog(
    noteId: string,
    message: string,
    agentName: string = 'AI Agent'
  ): Promise<AgentLogRecord> {
    const userId = await this.authenticate();

    const { data, error } = await this.client
      .from('agent_logs')
      .insert({
        note_id: noteId,
        user_id: userId,
        agent_name: agentName,
        message: message,
      })
      .select()
      .single();

    if (error || !data) {
      throw new Error(`Failed to append agent log: ${error?.message || 'Insert failed'}`);
    }

    return data as AgentLogRecord;
  }
}
