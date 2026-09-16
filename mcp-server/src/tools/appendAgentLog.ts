import { z } from 'zod';
import { SrutamClient } from '../supabase.js';

export const appendAgentLogSchema = {
  note_id: z.string().describe('The UUID of the note to append the log/comment to'),
  message: z.string().describe('Audit message or implementation note left by the agent (e.g., "Created database migrations and API endpoints in commit 8a4c12")'),
  agent_name: z.string().optional().default('AI Agent').describe('Name of the agent or IDE leaving the log (e.g. "Cursor", "Antigravity")'),
};

export async function handleAppendAgentLog(
  client: SrutamClient,
  args: { note_id: string; message: string; agent_name?: string }
) {
  const log = await client.appendAgentLog(
    args.note_id,
    args.message,
    args.agent_name || 'AI Agent'
  );

  return {
    content: [
      {
        type: 'text' as const,
        text: `Successfully attached work log to note \`${log.note_id}\` by **${log.agent_name}**: "${log.message}".`,
      },
    ],
  };
}
