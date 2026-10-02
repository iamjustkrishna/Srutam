import { z } from 'zod';
import { SrutamClient } from '../supabase.js';
import { formatDuration, formatKeyPoint, formatWhen, oneLine, wrapUntrusted } from './format.js';

export const getNoteDetailSchema = {
  note_id: z.string().trim().uuid().describe('The UUID of the note to fetch details for'),
};

export async function handleGetNoteDetail(
  client: SrutamClient,
  args: { note_id: string }
) {
  const { note, actionItems, agentLogs } = await client.getNoteDetail(args.note_id);

  let body = `# ${oneLine(note.title)}\n`;
  body += `**Recorded**: ${formatWhen(note.timestamp)} | **Duration**: ${formatDuration(note.duration_ms)} | **AI Status**: ${oneLine(note.ai_status, 40)}\n\n`;

  body += `## Executive Summary\n${note.summary || 'No summary available.'}\n\n`;

  if (note.wiifm) {
    body += `## Why It Matters (WIIFM)\n${note.wiifm}\n\n`;
  }

  const keyPoints = Array.isArray(note.key_points) ? note.key_points : [];
  if (keyPoints.length > 0) {
    body += `## Key Ideas & Points\n`;
    keyPoints.forEach((point: unknown) => {
      body += `- ${formatKeyPoint(point)}\n`;
    });
    body += '\n';
  }

  if (actionItems.length > 0) {
    body += `## Action Items & Next Steps\n`;
    actionItems.forEach((item) => {
      const checkbox = item.is_completed ? '[x]' : '[ ]';
      const completedInfo =
        item.is_completed && item.completed_by ? ` *(Completed by ${oneLine(item.completed_by, 60)})*` : '';
      body += `- ${checkbox} **(ID: \`${item.id}\`)** ${oneLine(item.description, 500)}${completedInfo}\n`;
    });
    body += '\n';
  }

  if (agentLogs.length > 0) {
    body += `## Agent Implementation Trails\n`;
    agentLogs.forEach((log) => {
      body += `- **${oneLine(log.agent_name, 60)}** (${formatWhen(log.created_at)}): ${oneLine(log.message, 4000)}\n`;
    });
    body += '\n';
  }

  body += `## Full Spoken Transcript\n${note.transcript || 'No transcript generated.'}\n`;

  return {
    content: [
      {
        type: 'text' as const,
        text: wrapUntrusted(body),
      },
    ],
  };
}
