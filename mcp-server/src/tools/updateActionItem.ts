import { z } from 'zod';
import { SrutamClient } from '../supabase.js';

export const updateActionItemSchema = {
  action_item_id: z.string().min(1).max(64).describe('The UUID of the action item to update'),
  completed: z.boolean().describe('Whether the action item is marked completed (true) or open (false)'),
  agent_name: z
    .string()
    .max(100)
    .optional()
    .default('AI Agent')
    .describe('Name of the agent or IDE performing the update (e.g., "Cursor", "Antigravity")'),
};

export async function handleUpdateActionItem(
  client: SrutamClient,
  args: { action_item_id: string; completed: boolean; agent_name?: string }
) {
  const updated = await client.updateActionItem(
    args.action_item_id,
    args.completed,
    args.agent_name || 'AI Agent'
  );

  return {
    content: [
      {
        type: 'text' as const,
        text: `Successfully updated task "${updated.description}": marked ${updated.is_completed ? 'COMPLETED' : 'OPEN'} by ${updated.completed_by || 'user'}. This update will sync back to the user's phone on their next app resume.`,
      },
    ],
  };
}
