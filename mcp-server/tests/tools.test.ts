import { describe, it, expect, vi } from 'vitest';
import { z } from 'zod';
import { searchNotesSchema, handleSearchNotes } from '../src/tools/searchNotes.js';
import { listRecentNotesSchema, handleListRecentNotes } from '../src/tools/listRecentNotes.js';
import { getNoteDetailSchema, handleGetNoteDetail } from '../src/tools/getNoteDetail.js';
import { listActionItemsSchema, handleListActionItems } from '../src/tools/listActionItems.js';
import { updateActionItemSchema, handleUpdateActionItem } from '../src/tools/updateActionItem.js';
import { appendAgentLogSchema, handleAppendAgentLog } from '../src/tools/appendAgentLog.js';
import { SrutamClient } from '../src/supabase.js';

describe('MCP Tools Schema Validation', () => {
  it('validates searchNotes arguments', () => {
    const schema = z.object(searchNotesSchema);
    expect(schema.safeParse({ query: 'database architecture' }).success).toBe(true);
    expect(schema.safeParse({ query: 'database', limit: 10 }).success).toBe(true);
    // Invalid: empty query
    expect(schema.safeParse({ query: '' }).success).toBe(false);
    // Invalid: limit > 25
    expect(schema.safeParse({ query: 'test', limit: 50 }).success).toBe(false);
  });

  it('validates listRecentNotes arguments', () => {
    const schema = z.object(listRecentNotesSchema);
    expect(schema.safeParse({}).success).toBe(true);
    expect(schema.safeParse({ limit: 15 }).success).toBe(true);
    expect(schema.safeParse({ limit: 0 }).success).toBe(false);
  });

  it('validates getNoteDetail arguments', () => {
    const schema = z.object(getNoteDetailSchema);
    expect(schema.safeParse({ note_id: 'uuid-1234' }).success).toBe(true);
    expect(schema.safeParse({}).success).toBe(false);
  });

  it('validates listActionItems arguments', () => {
    const schema = z.object(listActionItemsSchema);
    expect(schema.safeParse({}).success).toBe(true);
    expect(schema.safeParse({ status: 'pending' }).success).toBe(true);
    expect(schema.safeParse({ status: 'completed', limit: 10 }).success).toBe(true);
    expect(schema.safeParse({ status: 'invalid_status' }).success).toBe(false);
  });

  it('validates updateActionItem arguments', () => {
    const schema = z.object(updateActionItemSchema);
    expect(schema.safeParse({ action_item_id: 'item-1', completed: true }).success).toBe(true);
    expect(schema.safeParse({ action_item_id: 'item-1', completed: false, agent_name: 'Cursor' }).success).toBe(true);
    expect(schema.safeParse({ action_item_id: 'item-1' }).success).toBe(false);
  });

  it('validates appendAgentLog arguments', () => {
    const schema = z.object(appendAgentLogSchema);
    expect(schema.safeParse({ note_id: 'note-1', message: 'Implemented feature' }).success).toBe(true);
    expect(schema.safeParse({ note_id: 'note-1', message: '' }).success).toBe(false);
  });
});

describe('MCP Tools Handlers with Mock Client', () => {
  const mockClient = {
    searchNotes: vi.fn(),
    listRecentNotes: vi.fn(),
    getNoteDetail: vi.fn(),
    listActionItems: vi.fn(),
    updateActionItem: vi.fn(),
    appendAgentLog: vi.fn(),
  } as unknown as SrutamClient;

  it('handleSearchNotes returns formatted markdown results', async () => {
    (mockClient.searchNotes as any).mockResolvedValueOnce([
      {
        id: 'note-123',
        title: 'Architecture Review',
        summary: 'Reviewed clean MVVM and Room migrations.',
        key_points: ['Use UDF pattern', 'Room version 7'],
        wiifm: "What's In It For Me: Keeps code reliable.",
        timestamp: '2026-09-30T10:00:00Z',
      },
    ]);

    const response = await handleSearchNotes(mockClient, { query: 'Architecture' });
    expect(response.content[0].type).toBe('text');
    expect(response.content[0].text).toContain('Architecture Review');
    expect(response.content[0].text).toContain('note-123');
    expect(response.content[0].text).toContain('Room version 7');
  });

  it('handleSearchNotes handles empty results gracefully', async () => {
    (mockClient.searchNotes as any).mockResolvedValueOnce([]);

    const response = await handleSearchNotes(mockClient, { query: 'Nonexistent' });
    expect(response.content[0].text).toContain('No voice notes found matching "Nonexistent"');
  });

  it('handleGetNoteDetail formats full note details', async () => {
    (mockClient.getNoteDetail as any).mockResolvedValueOnce({
      note: {
        id: 'note-456',
        title: 'Release Planning',
        summary: 'Finalize v2.5.0 and publish srutam-mcp.',
        key_points: ['Package to npm', 'Test across IDEs'],
        wiifm: "What's In It For Me: Streamlines coding workflow.",
        transcript: 'Today we are preparing the MCP server release.',
        timestamp: '2026-09-30T10:00:00Z',
        duration_ms: 15000,
        ai_status: 'READY',
      },
      actionItems: [
        { id: 'act-1', description: 'Run vitest suite', is_completed: true, completed_by: 'Antigravity' },
        { id: 'act-2', description: 'Dry run npm pack', is_completed: false },
      ],
      agentLogs: [
        { agent_name: 'Antigravity', message: 'Wrote unit tests', created_at: '2026-09-30T10:05:00Z' },
      ],
    });

    const response = await handleGetNoteDetail(mockClient, { note_id: 'note-456' });
    expect(response.content[0].text).toContain('# Release Planning');
    expect(response.content[0].text).toContain('Finalize v2.5.0');
    expect(response.content[0].text).toContain('[x] **(ID: `act-1`)** Run vitest suite');
    expect(response.content[0].text).toContain('[ ] **(ID: `act-2`)** Dry run npm pack');
    expect(response.content[0].text).toContain('Today we are preparing the MCP server release.');
  });

  it('handleListActionItems formats task list', async () => {
    (mockClient.listActionItems as any).mockResolvedValueOnce([
      { id: 'act-9', description: 'Verify OpenCode config', is_completed: false },
    ]);

    const response = await handleListActionItems(mockClient, { status: 'pending' });
    expect(response.content[0].text).toContain('Srutam Action Items (pending)');
    expect(response.content[0].text).toContain('Verify OpenCode config');
  });

  it('handleUpdateActionItem confirms update', async () => {
    (mockClient.updateActionItem as any).mockResolvedValueOnce({
      id: 'act-9',
      description: 'Verify OpenCode config',
      is_completed: true,
      completed_by: 'Antigravity',
      completed_at: '2026-09-30T10:10:00Z',
    });

    const response = await handleUpdateActionItem(mockClient, {
      action_item_id: 'act-9',
      completed: true,
      agent_name: 'Antigravity',
    });
    expect(response.content[0].text).toContain('Successfully updated task');
    expect(response.content[0].text).toContain('marked COMPLETED by Antigravity');
  });

  it('handleAppendAgentLog confirms log attachment', async () => {
    (mockClient.appendAgentLog as any).mockResolvedValueOnce({
      id: 'log-1',
      note_id: 'note-456',
      agent_name: 'Antigravity',
      message: 'Implemented test coverage',
      created_at: '2026-09-30T10:12:00Z',
    });

    const response = await handleAppendAgentLog(mockClient, {
      note_id: 'note-456',
      message: 'Implemented test coverage',
    });
    expect(response.content[0].text).toContain('Successfully attached work log to note');
  });
});
