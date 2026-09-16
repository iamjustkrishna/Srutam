import { z } from 'zod';
import { SrutamClient } from '../supabase.js';

export const listRecentNotesSchema = {
  limit: z.number().optional().default(10).describe('Maximum number of recent voice notes to retrieve (default: 10)'),
};

export async function handleListRecentNotes(
  client: SrutamClient,
  args: { limit?: number }
) {
  const notes = await client.listRecentNotes(args.limit || 10);

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

  const formatted = notes.map((note, index) => {
    return `### [${index + 1}] ${note.title} (ID: ${note.id})\n` +
      `*Recorded*: ${new Date(note.timestamp).toLocaleString()} | *Duration*: ${Math.round(note.duration_ms / 1000)}s\n` +
      `*Summary*: ${note.summary || 'Pending summary'}\n` +
      `*Key Points*: ${Array.isArray(note.key_points) ? note.key_points.join('; ') : JSON.stringify(note.key_points || [])}\n` +
      `---\n`;
  }).join('\n');

  return {
    content: [
      {
        type: 'text' as const,
        text: `Here are your ${notes.length} most recent voice notes from Srutam:\n\n${formatted}\nUse \`get_note_detail\` with any note ID to fetch the full transcript and actionable tasks.`,
      },
    ],
  };
}
