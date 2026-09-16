import { z } from 'zod';
import { SrutamClient } from '../supabase.js';

export const searchNotesSchema = {
  query: z.string().describe('Search query, keywords, or topics to search for across your voice notes'),
  limit: z.number().optional().default(5).describe('Maximum number of matching notes to return (default: 5)'),
};

export async function handleSearchNotes(
  client: SrutamClient,
  args: { query: string; limit?: number }
) {
  const results = await client.searchNotes(args.query, args.limit || 5);

  if (results.length === 0) {
    return {
      content: [
        {
          type: 'text' as const,
          text: `No voice notes found matching "${args.query}".`,
        },
      ],
    };
  }

  const formatted = results.map((note, index) => {
    return `### [${index + 1}] ${note.title} (ID: ${note.id})\n` +
      `*Recorded*: ${new Date(note.timestamp).toLocaleString()}\n` +
      `*Summary*: ${note.summary || 'No summary available'}\n` +
      `*Key Points*: ${Array.isArray(note.key_points) ? note.key_points.join('; ') : JSON.stringify(note.key_points || [])}\n` +
      (note.wiifm ? `*Why It Matters (WIIFM)*: ${note.wiifm}\n` : '') +
      `---\n`;
  }).join('\n');

  return {
    content: [
      {
        type: 'text' as const,
        text: `Found ${results.length} note(s) matching "${args.query}":\n\n${formatted}\nUse \`get_note_detail\` with the note ID to inspect the full transcript and action items.`,
      },
    ],
  };
}
