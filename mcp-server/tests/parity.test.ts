import { describe, it, expect, vi } from 'vitest';
import { z } from 'zod';
import { listInsightsSchema, handleListInsights } from '../src/tools/listInsights.js';
import { listRemindersSchema, handleListReminders } from '../src/tools/listReminders.js';
import { SrutamClient } from '../src/supabase.js';

const NOTE_ID = '3f1c2b8e-6a4d-4c1e-9b7a-2d5e8f0a1b3c';
const ITEM_ID = '7b9d1e2f-4a6c-4d8e-8f01-a2b3c4d5e6f7';
const TASK_ID = '11223344-5566-4778-8899-aabbccddeeff';

const mockClient = {
  listInsights: vi.fn(),
  listReminders: vi.fn(),
} as unknown as SrutamClient;

/**
 * The rendered bullet for the first item, without the trailing guidance block.
 * Needed because the reminder caveat deliberately spells out "[unconfirmed]" to
 * explain the marker, so asserting absence against the whole output would always
 * fail regardless of what the row actually rendered.
 */
const firstBullet = (text: string) => text.split('\n').find((line) => line.startsWith('- ')) ?? '';

/** A reminder row as mcp_list_reminders returns it after migration 08. */
const reminderRow = (overrides: Record<string, unknown> = {}) => ({
  id: ITEM_ID,
  note_id: NOTE_ID,
  title: 'Tennox profit projection target',
  event_time: '2026-11-20T00:00:00Z',
  original_text: 'profit projection by the twentieth',
  person: null,
  location: null,
  type: 'MILESTONE',
  status: 'ACTIVE',
  needs_review: false,
  confirmed_at: '2026-10-01T09:00:00Z',
  time_precision: 'EXACT',
  local_date: '2026-11-20',
  local_time: null,
  zone_id: 'Asia/Kolkata',
  linked_task_id: null,
  created_at: '2026-10-01T08:00:00Z',
  ...overrides,
});

/** An insight row as mcp_list_insights returns it after migration 08. */
const insightRow = (overrides: Record<string, unknown> = {}) => ({
  id: ITEM_ID,
  note_id: NOTE_ID,
  kind: 'idea' as const,
  text: 'Ship a desktop capture app',
  evidence: null,
  rationale: null,
  status: 'OPEN',
  completed_at: null,
  archived_at: null,
  source_insight_id: null,
  source_reminder_id: null,
  created_at: '2026-10-01T08:00:00Z',
  ...overrides,
});

describe('Reminder review state is rendered as three distinct states', () => {
  it('flags an unreviewed reminder as unconfirmed', async () => {
    (mockClient.listReminders as any).mockResolvedValueOnce([
      reminderRow({ needs_review: true, confirmed_at: null }),
    ]);
    const { content } = await handleListReminders(mockClient, {});
    expect(content[0].text).toContain('[unconfirmed]');
    expect(content[0].text).not.toContain('review state unknown');
  });

  it('says nothing extra for a reviewed reminder', async () => {
    (mockClient.listReminders as any).mockResolvedValueOnce([
      reminderRow({ needs_review: false }),
    ]);
    const { content } = await handleListReminders(mockClient, {});
    expect(firstBullet(content[0].text)).not.toContain('[unconfirmed]');
    expect(firstBullet(content[0].text)).not.toContain('review state unknown');
  });

  // The whole point of making needs_review nullable: a pre-migration-08 row has
  // no review state, and calling that confirmed would have an agent trust a
  // time the user never actually checked.
  it('never reports an unknown review state as confirmed', async () => {
    for (const unknown of [null, undefined]) {
      (mockClient.listReminders as any).mockResolvedValueOnce([
        reminderRow({ needs_review: unknown }),
      ]);
      const { content } = await handleListReminders(mockClient, {});
      expect(firstBullet(content[0].text)).toContain('review state unknown');
      expect(firstBullet(content[0].text)).not.toContain('[unconfirmed]');
    }
  });
});

describe('Undated targets and milestones stay legible', () => {
  it('uses the resolved local date when there is no event_time', async () => {
    (mockClient.listReminders as any).mockResolvedValueOnce([
      reminderRow({ event_time: null, local_date: '2026-11-20', local_time: null }),
    ]);
    const { content } = await handleListReminders(mockClient, {});
    expect(content[0].text).toContain('2026-11-20');
    expect(content[0].text).not.toContain('no time set');
  });

  it('falls back to a no-time marker only when there is genuinely no date', async () => {
    (mockClient.listReminders as any).mockResolvedValueOnce([
      reminderRow({ event_time: null, local_date: null, local_time: null }),
    ]);
    const { content } = await handleListReminders(mockClient, {});
    expect(content[0].text).toContain('no time set');
  });

  it('warns when the recording never pinned the time down', async () => {
    (mockClient.listReminders as any).mockResolvedValueOnce([
      reminderRow({ time_precision: 'UNKNOWN' }),
    ]);
    const { content } = await handleListReminders(mockClient, {});
    expect(content[0].text).toContain('time not pinned down');
  });

  it('surfaces the next step a reminder was converted into', async () => {
    (mockClient.listReminders as any).mockResolvedValueOnce([
      reminderRow({ linked_task_id: TASK_ID }),
    ]);
    const { content } = await handleListReminders(mockClient, {});
    expect(content[0].text).toContain(TASK_ID);
    expect(content[0].text).toContain('tracked as next step');
  });
});

describe('Insight lifecycle and provenance', () => {
  it('leaves an ordinary open insight unannotated', async () => {
    (mockClient.listInsights as any).mockResolvedValueOnce([insightRow({ status: 'OPEN' })]);
    const { content } = await handleListInsights(mockClient, {});
    expect(content[0].text).not.toContain('(OPEN)');
  });

  it('annotates a completed or archived insight', async () => {
    for (const status of ['COMPLETED', 'ARCHIVED']) {
      (mockClient.listInsights as any).mockResolvedValueOnce([insightRow({ status })]);
      const { content } = await handleListInsights(mockClient, { include_archived: true });
      expect(content[0].text).toContain('(' + status + ')');
    }
  });

  // A server without migration 08 omits status entirely; that must not crash or
  // render as a bogus state.
  it('tolerates a row with no status field at all', async () => {
    const row = insightRow();
    delete (row as Record<string, unknown>).status;
    (mockClient.listInsights as any).mockResolvedValueOnce([row]);
    const { content } = await handleListInsights(mockClient, {});
    expect(content[0].text).toContain('Ship a desktop capture app');
    expect(content[0].text).not.toContain('undefined');
  });

  it('links a derived next step back to the idea it came from', async () => {
    (mockClient.listInsights as any).mockResolvedValueOnce([
      insightRow({ source_insight_id: TASK_ID }),
    ]);
    const { content } = await handleListInsights(mockClient, {});
    expect(content[0].text).toContain('derived from insight');
    expect(content[0].text).toContain(TASK_ID);
  });

  it('prefers the reminder origin when a task came from a reminder', async () => {
    (mockClient.listInsights as any).mockResolvedValueOnce([
      insightRow({ source_reminder_id: TASK_ID, source_insight_id: ITEM_ID }),
    ]);
    const { content } = await handleListInsights(mockClient, {});
    expect(content[0].text).toContain('converted from reminder');
    expect(content[0].text).not.toContain('derived from insight');
  });

  it('passes include_archived through to the RPC rather than filtering locally', async () => {
    (mockClient.listInsights as any).mockResolvedValueOnce([]);
    await handleListInsights(mockClient, { kind: 'idea', include_archived: true, limit: 5 });
    expect(mockClient.listInsights).toHaveBeenLastCalledWith('idea', 5, true);

    (mockClient.listInsights as any).mockResolvedValueOnce([]);
    await handleListInsights(mockClient, {});
    expect(mockClient.listInsights).toHaveBeenLastCalledWith('all', 20, false);
  });
});

describe('Schemas accept the new parameters', () => {
  it('defaults include_archived to false and rejects non-booleans', () => {
    const schema = z.object(listInsightsSchema);
    expect(schema.parse({}).include_archived).toBe(false);
    expect(schema.parse({ include_archived: true }).include_archived).toBe(true);
    expect(schema.safeParse({ include_archived: 'yes' }).success).toBe(false);
  });

  it('still defaults upcoming_only to true', () => {
    const schema = z.object(listRemindersSchema);
    expect(schema.parse({}).upcoming_only).toBe(true);
  });
});
