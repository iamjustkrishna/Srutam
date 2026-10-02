import { z } from 'zod';
import { SrutamClient } from '../supabase.js';
import { formatNoteSummary, wrapUntrusted } from './format.js';

export const listRecentNotesSchema = {
  limit: z
    .number()
    .int()
    .min(1)
    .max(25)
    .optional()
    .default(10)
    .describe('Maximum number of recent voice notes to retrieve (default: 10, max: 25)'),
};

export async function handleListRecentNotes(
  client: SrutamClient,
  args: { limit?: number }
) {
  const boundedLimit = Math.max(1, Math.min(args.limit || 10, 25));
  const notes = await client.listRecentNotes(boundedLimit);

  if (notes.length === 0) {
    return {
      content: [
        {
          type: 'text' as const,
          text: 'You have no voice notes synced yet in Srutam.',
        },
      ],
    };
  }

  const formatted = notes
    .map((note, index) =>
      formatNoteSummary(note, index, { withDuration: true, summaryFallback: 'Pending summary' })
    )
    .join('\n');

  return {
    content: [
      {
        type: 'text' as const,
        text:
          `Here are your ${notes.length} most recent voice notes from Srutam:\n\n${wrapUntrusted(formatted)}\n\n` +
          `Use \`get_note_detail\` with any note ID to fetch the full transcript and actionable tasks.`,
      },
    ],
  };
}
