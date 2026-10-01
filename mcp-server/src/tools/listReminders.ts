import { z } from 'zod';
import { SrutamClient } from '../supabase.js';
import { formatWhen, oneLine, wrapUntrusted } from './format.js';

export const listRemindersSchema = {
  upcoming_only: z
    .boolean()
    .optional()
    .default(true)
    .describe('Only show active reminders scheduled in the future (default: true)'),
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
    const when = item.event_time ? formatWhen(item.event_time) : 'no time set';
    const type = item.type ? `[${oneLine(item.type, 30)}]` : '';
    const status = item.status !== 'ACTIVE' ? ` (${item.status})` : '';
    const noteRef = `*(Note ID: \`${item.note_id}\`)*`;
    list += `- ${type} **${oneLine(item.title, 200)}**${status} - ${when} ${noteRef}\n`;
    if (item.person) list += `  - with: ${oneLine(item.person, 100)}\n`;
    if (item.location) list += `  - location: ${oneLine(item.location, 100)}\n`;
  });

  return {
    content: [
      {
        type: 'text' as const,
        text: `${wrapUntrusted(list)}\n\nThis is read-only: reminders are managed from the Srutam app, not from MCP.`,
      },
    ],
  };
}
