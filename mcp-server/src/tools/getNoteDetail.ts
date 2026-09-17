import { z } from 'zod';
import { SrutamClient } from '../supabase.js';

export const getNoteDetailSchema = {
  note_id: z.string().min(1).max(64).describe('The UUID of the note to fetch details for'),
};

export async function handleGetNoteDetail(
  client: SrutamClient,
  args: { note_id: string }
) {
  const { note, actionItems, agentLogs } = await client.getNoteDetail(args.note_id);

  let responseText = `# ${note.title}\n`;
  responseText += `**Recorded**: ${new Date(note.timestamp).toLocaleString()} | **Duration**: ${Math.round(note.duration_ms / 1000)}s | **AI Status**: ${note.ai_status}\n\n`;

  responseText += `## Executive Summary\n${note.summary || 'No summary available.'}\n\n`;

  if (note.wiifm) {
    responseText += `## Why It Matters (WIIFM)\n${note.wiifm}\n\n`;
  }

  const keyPoints = Array.isArray(note.key_points) ? note.key_points : [];
  if (keyPoints.length > 0) {
    responseText += `## Key Ideas & Points\n`;
    keyPoints.forEach((point: string) => {
      responseText += `- ${point}\n`;
    });
    responseText += '\n';
  }

  if (actionItems.length > 0) {
    responseText += `## Action Items & Next Steps\n`;
    actionItems.forEach((item) => {
      const checkbox = item.is_completed ? '[x]' : '[ ]';
      const completedInfo = item.is_completed && item.completed_by ? ` *(Completed by ${item.completed_by})*` : '';
      responseText += `- ${checkbox} **(ID: \`${item.id}\`)** ${item.description}${completedInfo}\n`;
    });
    responseText += '\n';
  }

  if (agentLogs.length > 0) {
    responseText += `## Agent Implementation Trails\n`;
    agentLogs.forEach((log) => {
      responseText += `- **${log.agent_name}** (${new Date(log.created_at).toLocaleTimeString()}): ${log.message}\n`;
    });
    responseText += '\n';
  }

  responseText += `## Full Spoken Transcript\n${note.transcript || 'No transcript generated.'}\n`;

  return {
    content: [
      {
        type: 'text' as const,
        text: responseText,
      },
    ],
  };
}
