import { z } from 'zod';
import { SrutamClient } from '../supabase.js';
import { formatWhen, oneLine, wrapUntrusted } from './format.js';

export const listRemindersSchema = {
  upcoming_only: z
    .boolean()
    .optional()
    .default(true)
    .describe(
      'Only show active reminders that have not passed. Undated targets and milestones are included, since they are still live commitments (default: true)'
    ),
  limit: z
    .number()
    .int()
    .min(1)
    .max(50)
    .optional()
    .default(20)
    .describe('Maximum number of reminders to retrieve (default: 20, max: 50)'),
};

export async function handleListReminders(
  client: SrutamClient,
  args: { upcoming_only?: boolean; limit?: number }
) {
  const upcomingOnly = args.upcoming_only ?? true;
  const boundedLimit = Math.max(1, Math.min(args.limit || 20, 50));
  const items = await client.listReminders(upcomingOnly, boundedLimit);

  if (items.length === 0) {
    return {
      content: [
        {
          type: 'text' as const,
          text: upcomingOnly
            ? 'No upcoming reminders found in your Srutam account.'
            : 'No reminders found in your Srutam account.',
        },
      ],
    };
  }

  let list = `### Srutam Reminders${upcomingOnly ? ' (upcoming)' : ''}:\n\n`;
  items.forEach((item) => {
    // Prefer the user's resolved local date/time over the UTC instant: a target
    // date captured as "May 20" has no clock time, and rendering it as an exact
    // timestamp would invent precision the user never gave.
    const when = item.event_time
      ? formatWhen(item.event_time)
      : item.local_date
        ? oneLine([item.local_date, item.local_time].filter(Boolean).join(' '), 40)
        : 'no time set';
    const type = item.type ? `[${oneLine(item.type, 30)}]` : '';
    const status = item.status !== 'ACTIVE' ? ` (${item.status})` : '';
    // Three-state, deliberately. null/undefined means the row predates migration
    // 08 and its review state is genuinely unknown - never render that as
    // confirmed, or an agent will trust a time the user never checked.
    const review =
      item.needs_review === true
        ? ' **[unconfirmed]**'
        : item.needs_review === null || item.needs_review === undefined
          ? ' *[review state unknown]*'
          : '';
    const noteRef = `*(Note ID: \`${item.note_id}\`)*`;
    list += `- ${type} **${oneLine(item.title, 200)}**${status}${review} - ${when} ${noteRef}\n`;
    if (item.person) list += `  - with: ${oneLine(item.person, 100)}\n`;
    if (item.location) list += `  - location: ${oneLine(item.location, 100)}\n`;
    if (item.time_precision === 'UNKNOWN') {
      list += `  - time not pinned down in the recording\n`;
    }
    if (item.linked_task_id) {
      list += `  - tracked as next step: \`${item.linked_task_id}\`\n`;
    }
  });

  const caveat =
    'This is read-only: reminders are managed from the Srutam app, not from MCP. ' +
    'Items marked [unconfirmed] were extracted from speech but not yet reviewed by the user - treat their times as provisional.';

  return {
    content: [
      {
        type: 'text' as const,
        text: `${wrapUntrusted(list)}\n\n${caveat}`,
      },
    ],
  };
}
