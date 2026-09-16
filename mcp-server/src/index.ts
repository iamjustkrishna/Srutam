#!/usr/bin/env node

import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { loadConfig } from './config.js';
import { SrutamClient } from './supabase.js';

import { searchNotesSchema, handleSearchNotes } from './tools/searchNotes.js';
import { listRecentNotesSchema, handleListRecentNotes } from './tools/listRecentNotes.js';
import { getNoteDetailSchema, handleGetNoteDetail } from './tools/getNoteDetail.js';
import { listActionItemsSchema, handleListActionItems } from './tools/listActionItems.js';
import { updateActionItemSchema, handleUpdateActionItem } from './tools/updateActionItem.js';
import { appendAgentLogSchema, handleAppendAgentLog } from './tools/appendAgentLog.js';

async function main() {
  const config = loadConfig();
  const client = new SrutamClient(config);

  const server = new McpServer({
    name: 'srutam-mcp-server',
    version: '1.0.0',
  });

  // Tool 1: search_notes
  server.tool(
    'search_notes',
    'Search through voice notes, transcripts, summaries, and ideas captured in Srutam using semantic and keyword search.',
    searchNotesSchema,
    async (args) => handleSearchNotes(client, args)
  );

  // Tool 2: list_recent_notes
  server.tool(
    'list_recent_notes',
    'List the most recently recorded voice notes from Srutam with their titles, summaries, and key points.',
    listRecentNotesSchema,
    async (args) => handleListRecentNotes(client, args)
  );

  // Tool 3: get_note_detail
  server.tool(
    'get_note_detail',
    'Fetch the complete content of a specific voice note, including the verbatim transcript, executive summary, WIIFM, actionable tasks, and agent trails.',
    getNoteDetailSchema,
    async (args) => handleGetNoteDetail(client, args)
  );

  // Tool 4: list_action_items
  server.tool(
    'list_action_items',
    'List all actionable tasks and next steps extracted from voice recordings in Srutam, filtered by pending or completed status.',
    listActionItemsSchema,
    async (args) => handleListActionItems(client, args)
  );

  // Tool 5: update_action_item
  server.tool(
    'update_action_item',
    'Mark an action item or task as completed (or open) in Srutam. The completed status will sync back to the user phone.',
    updateActionItemSchema,
    async (args) => handleUpdateActionItem(client, args)
  );

  // Tool 6: append_agent_work_log
  server.tool(
    'append_agent_work_log',
    'Attach an implementation note, git commit reference, or work log to a specific Srutam voice note.',
    appendAgentLogSchema,
    async (args) => handleAppendAgentLog(client, args)
  );

  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error('Srutam MCP server running on stdio');
}

main().catch((err) => {
  console.error('Fatal error in Srutam MCP server:', err);
  process.exit(1);
});
