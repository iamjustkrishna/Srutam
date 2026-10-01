import { z } from 'zod';
import { SrutamClient } from '../supabase.js';
import { formatWhen, oneLine, wrapUntrusted } from './format.js';

export const listInsightsSchema = {
  kind: z
    .enum(['all', 'idea', 'decision'])
    .optional()
    .default('all')
    .describe('Filter by insight kind (default: "all")'),
  limit: z
    .number()
    .int()
    .min(1)
    .max(50)
    .optional()
    .default(20)
    .describe('Maximum number of insights to retrieve (default: 20, max: 50)'),
};

export async function handleListInsights(
  client: SrutamClient,
  args: { kind?: 'all' | 'idea' | 'decision'; limit?: number }
) {
  const kind = args.kind || 'all';
  const boundedLimit = Math.max(1, Math.min(args.limit || 20, 50));
  const items = await client.listInsights(kind, boundedLimit);

  if (items.length === 0) {
    return {
      content: [
        {
          type: 'text' as const,
          text: `No ${kind === 'all' ? '' : kind + ' '}insights found in your Srutam account.`,
        },
      ],
    };
  }

  let list = `### Srutam Insights (${kind}):\n\n`;
  items.forEach((item) => {
    const label = item.kind === 'idea' ? '[idea]' : '[decision]';
    const noteRef = `*(Note ID: \`${item.note_id}\`)*`;
    const when = formatWhen(item.created_at);
    list += `- ${label} **ID: \`${item.id}\`** - ${oneLine(item.text, 500)} ${noteRef} *(${when})*\n`;
    if (item.rationale) {
      list += `  - rationale: ${oneLine(item.rationale, 300)}\n`;
    }
  });

  return {
    content: [
      {
        type: 'text' as const,
        text: `${wrapUntrusted(list)}\n\nUse \`get_note_detail\` with the note ID to see the full voice note this came from.`,
      },
    ],
  };
}
