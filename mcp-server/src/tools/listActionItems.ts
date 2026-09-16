import { z } from 'zod';
import { SrutamClient } from '../supabase.js';

export const listActionItemsSchema = {
  status: z.enum(['all', 'pending', 'completed']).optional().default('pending').describe('Filter tasks by status (default: "pending")'),
  limit: z.number().optional().default(20).describe('Maximum number of action items to retrieve (default: 20)'),
};

export async function handleListActionItems(
  client: SrutamClient,
  args: { status?: 'all' | 'pending' | 'completed'; limit?: number }
) {
  const status = args.status || 'pending';
  const items = await client.listActionItems(status, args.limit || 20);

  if (items.length === 0) {
    return {
      content: [
        {
          type: 'text' as const,
          text: `No ${status} action items found in your Srutam account.`,
        },
      ],
    };
  }

  let formatted = `### Srutam Action Items (${status}):\n\n`;
  items.forEach((item) => {
    const box = item.is_completed ? '[x]' : '[ ]';
    const noteRef = `*(Note ID: \`${item.note_id}\`)*`;
    const by = item.completed_by ? ` (done by ${item.completed_by})` : '';
    formatted += `- ${box} **ID: \`${item.id}\`** - ${item.description} ${noteRef}${by}\n`;
  });

  formatted += `\nUse \`update_action_item\` with the task ID to mark any task completed.`;

  return {
    content: [
      {
        type: 'text' as const,
        text: formatted,
      },
    ],
  };
}
