import { describe, it, expect, beforeEach, vi } from 'vitest';
import crypto from 'crypto';

const { rpc, createClient } = vi.hoisted(() => {
  const rpc = vi.fn();
  return { rpc, createClient: vi.fn(() => ({ rpc })) };
});
vi.mock('@supabase/supabase-js', () => ({ createClient }));

import { SrutamClient, AUTH_ERROR_MESSAGE } from '../src/supabase.js';

const API_KEY = 'srtm_live_unit_test_key';
const KEY_HASH = crypto.createHash('sha256').update(API_KEY).digest('hex');
const NOTE_ID = '3f1c2b8e-6a4d-4c1e-9b7a-2d5e8f0a1b3c';
const ITEM_ID = '7b9d1e2f-4a6c-4d8e-8f01-a2b3c4d5e6f7';

function makeClient() {
  return new SrutamClient({
    apiKey: API_KEY,
    supabaseUrl: 'https://x.supabase.co',
    supabaseAnonKey: 'anon',
  });
}

const authError = { code: '28000', message: 'unauthorized' };

describe('SrutamClient authorization model', () => {
  beforeEach(() => {
    rpc.mockReset();
    createClient.mockClear();
  });

  it('never runs in a session-persisting mode', () => {
    makeClient();
    const options = (createClient.mock.calls[0] as unknown as [string, string, any])[2];
    expect(options.auth).toMatchObject({ persistSession: false, autoRefreshToken: false, detectSessionInUrl: false });
  });

  it('sends the key hash - and never a user id - on every RPC', async () => {
    rpc.mockResolvedValue({ data: [], error: null });
    const client = makeClient();

    rpc.mockResolvedValueOnce({ data: [], error: null });
    await client.searchNotes('q', 3);
    rpc.mockResolvedValueOnce({ data: { note: { id: NOTE_ID }, actionItems: [], agentLogs: [] }, error: null });
    await client.getNoteDetail(NOTE_ID);
    rpc.mockResolvedValueOnce({ data: [], error: null });
    await client.listActionItems('all', 5);
    rpc.mockResolvedValueOnce({ data: { id: ITEM_ID, description: 'd' }, error: null });
    await client.updateActionItem(ITEM_ID, true, 'Cursor');
    rpc.mockResolvedValueOnce({ data: { id: 'l', note_id: NOTE_ID }, error: null });
    await client.appendAgentLog(NOTE_ID, 'msg', 'Cursor');
    rpc.mockResolvedValueOnce({ data: { userId: 'u', noteCount: 1, pendingActionsCount: 0 }, error: null });
    await client.getCloudStatus();

    expect(rpc).toHaveBeenCalledTimes(6);
    for (const [fn, args] of rpc.mock.calls as [string, Record<string, unknown>][]) {
      expect(fn).toMatch(/^mcp_/);
      expect(args.p_key_hash).toBe(KEY_HASH);
      expect(args).not.toHaveProperty('p_user_id');
      expect(JSON.stringify(args)).not.toContain(API_KEY); // raw key never leaves the process
    }
  });

  it('does not treat a key as authenticated after the first success (revocation is immediate)', async () => {
    const client = makeClient();
    rpc.mockResolvedValueOnce({ data: { userId: 'u', noteCount: 0, pendingActionsCount: 0 }, error: null });
    await expect(client.getCloudStatus()).resolves.toBeDefined();

    rpc.mockResolvedValueOnce({ data: null, error: authError });
    await expect(client.getCloudStatus()).rejects.toThrow(AUTH_ERROR_MESSAGE);
  });

  it('drops cached reads when the server rejects the key', async () => {
    const client = makeClient();
    rpc.mockResolvedValueOnce({ data: [{ id: NOTE_ID, title: 't' }], error: null });
    await client.searchNotes('cached');
    await client.searchNotes('cached'); // served from cache
    expect(rpc).toHaveBeenCalledTimes(1);

    rpc.mockResolvedValueOnce({ data: null, error: authError });
    await expect(client.listActionItems()).rejects.toThrow(AUTH_ERROR_MESSAGE);

    rpc.mockResolvedValueOnce({ data: [], error: null });
    await client.searchNotes('cached');
    expect(rpc).toHaveBeenCalledTimes(3); // cache was cleared by the auth failure
  });

  it('recognises an unauthorized error by message as well as by SQLSTATE', async () => {
    const client = makeClient();
    rpc.mockResolvedValueOnce({ data: null, error: { code: 'PGRST', message: 'unauthorized' } });
    await expect(client.getCloudStatus()).rejects.toThrow(AUTH_ERROR_MESSAGE);
  });

  it('surfaces other database errors with the function name', async () => {
    const client = makeClient();
    rpc.mockResolvedValueOnce({ data: null, error: { code: '22023', message: 'invalid status' } });
    await expect(client.listActionItems()).rejects.toThrow('mcp_list_action_items failed: invalid status');
  });

  it('reports a missing/private note clearly', async () => {
    const client = makeClient();
    rpc.mockResolvedValueOnce({ data: null, error: null });
    await expect(client.getNoteDetail(NOTE_ID)).rejects.toThrow(/not found/i);
  });

  it('reports an action item that cannot be updated', async () => {
    const client = makeClient();
    rpc.mockResolvedValueOnce({ data: null, error: null });
    await expect(client.updateActionItem(ITEM_ID, true)).rejects.toThrow(/not found|private/i);
  });

  it('clears the read cache after a successful update', async () => {
    const client = makeClient();
    rpc.mockResolvedValueOnce({ data: [], error: null });
    await client.listActionItems('pending', 20);
    await client.listActionItems('pending', 20);
    expect(rpc).toHaveBeenCalledTimes(1);

    rpc.mockResolvedValueOnce({ data: { id: ITEM_ID, description: 'd', is_completed: true }, error: null });
    await client.updateActionItem(ITEM_ID, true);

    rpc.mockResolvedValueOnce({ data: [], error: null });
    await client.listActionItems('pending', 20);
    expect(rpc).toHaveBeenCalledTimes(3);
  });

  it('bounds limits client-side and trims the query', async () => {
    const client = makeClient();
    rpc.mockResolvedValue({ data: [], error: null });
    await client.searchNotes('   spaced   ', 9999);
    expect(rpc).toHaveBeenCalledWith('mcp_search_notes', {
      p_key_hash: KEY_HASH,
      p_query: 'spaced',
      p_limit: 25,
    });
  });

  it('applies the runaway-loop guard per client', async () => {
    const client = makeClient();
    rpc.mockResolvedValue({ data: { userId: 'u', noteCount: 0, pendingActionsCount: 0 }, error: null });
    for (let i = 0; i < 60; i++) {
      await client.getCloudStatus();
    }
    await expect(client.getCloudStatus()).rejects.toThrow(/Rate limit exceeded/);

    // a separate client instance has its own budget (no hidden process-wide statics)
    await expect(makeClient().getCloudStatus()).resolves.toBeDefined();
  });
});
