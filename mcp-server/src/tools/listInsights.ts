import { z } from 'zod';
import { SrutamClient } from '../supabase.js';
import { formatWhen, oneLine, wrapUntrusted } from './format.js';

export const listInsightsSchema = {
  kind: z
    .enum(['all', 'idea', 'decision'])
    .optional()
    .default('all')
    .describe('Filter by insight kind (default: "all")'),
  include_archived: z
    .boolean()
    .optional()
    .default(false)
    .describe('Include insights the user has archived (default: false)'),
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
  args: { kind?: 'all' | 'idea' | 'decision'; include_archived?: boolean; limit?: number }
) {
  const kind = args.kind || 'all';
  const includeArchived = args.include_archived ?? false;
  const boundedLimit = Math.max(1, Math.min(args.limit || 20, 50));
  const items = await client.listInsights(kind, boundedLimit, includeArchived);

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
    // Only annotate non-default lifecycle states, so an ordinary open idea stays
    // terse. status is optional because a server without migration 08 omits it.
    const status =
      item.status && item.status !== 'OPEN' ? ` (${oneLine(item.status, 20)})` : '';
    list += `- ${label} **ID: \`${item.id}\`**${status} - ${oneLine(item.text, 500)} ${noteRef} *(${when})*\n`;
    if (item.rationale) {
      list += `  - rationale: ${oneLine(item.rationale, 300)}\n`;
    }
    // Provenance: the app creates a NEW next step linked back to its source
    // rather than changing the idea, so without this the two look unrelated.
    if (item.source_reminder_id) {
      list += `  - converted from reminder: \`${item.source_reminder_id}\`\n`;
    } else if (item.source_insight_id) {
      list += `  - derived from insight: \`${item.source_insight_id}\`\n`;
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
