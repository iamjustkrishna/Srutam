import { z } from 'zod';
import { SrutamClient } from '../supabase.js';
import { formatNoteSummary, oneLine, wrapUntrusted } from './format.js';

export const searchNotesSchema = {
  query: z
    .string()
    .trim()
    .min(1)
    .max(200)
    .describe('Search query, keywords, or topics to search for across your voice notes'),
  limit: z
    .number()
    .int()
    .min(1)
    .max(25)
    .optional()
    .default(5)
    .describe('Maximum number of matching notes to return (default: 5, max: 25)'),
};

export async function handleSearchNotes(
  client: SrutamClient,
  args: { query: string; limit?: number }
) {
  const boundedLimit = Math.max(1, Math.min(args.limit || 5, 25));
  const results = await client.searchNotes(args.query, boundedLimit);
  const shownQuery = oneLine(args.query, 100);

  if (results.length === 0) {
    return {
      content: [
        {
          type: 'text' as const,
          text: `No voice notes found matching "${shownQuery}".`,
        },
      ],
    };
  }

  const formatted = results
    .map((note, index) => formatNoteSummary(note, index, { withWiifm: true }))
    .join('\n');

  return {
    content: [
      {
        type: 'text' as const,
        text:
          `Found ${results.length} note(s) matching "${shownQuery}":\n\n${wrapUntrusted(formatted)}\n\n` +
          `Use \`get_note_detail\` with the note ID to inspect the full transcript and action items.`,
      },
    ],
  };
}
