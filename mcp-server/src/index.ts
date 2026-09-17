#!/usr/bin/env node

import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { loadConfig } from './config.js';
import { SrutamClient } from './supabase.js';
import { runWizard, runLogout } from './cli/wizard.js';

import { searchNotesSchema, handleSearchNotes } from './tools/searchNotes.js';
import { listRecentNotesSchema, handleListRecentNotes } from './tools/listRecentNotes.js';
import { getNoteDetailSchema, handleGetNoteDetail } from './tools/getNoteDetail.js';
import { listActionItemsSchema, handleListActionItems } from './tools/listActionItems.js';
import { updateActionItemSchema, handleUpdateActionItem } from './tools/updateActionItem.js';
import { appendAgentLogSchema, handleAppendAgentLog } from './tools/appendAgentLog.js';

function printHelp(): void {
  console.log(`
Srutam MCP Server - Model Context Protocol for Srutam Voice Notes

Usage:
  srutam-mcp [command] [options]

Commands:
  init, setup    Interactive setup wizard to link mobile key and configure IDE
  logout, reset  Clear saved API credentials
  [none]         Start the stdio MCP server (used by Cursor, Antigravity, Claude)

Options:
  -h, --help     Show this help message
  -v, --version  Show version number

Environment Variables:
  SRUTAM_API_KEY      Your Personal Access Token (starts with srtm_live_)
  SUPABASE_URL        (Optional) Override Supabase URL
  SUPABASE_ANON_KEY   (Optional) Override Supabase Anon Key

Quick Start:
  npx -y srutam-mcp init
`);
}

async function startServer(): Promise<void> {
  const config = loadConfig();

  if (!config) {
    if (process.stdin.isTTY) {
      console.log('\n=======================================================');
      console.log('             Srutam MCP - Setup Needed');
      console.log('=======================================================');
      console.log('\nNo Srutam API key found.');
      console.log('Run the interactive setup wizard to get started in 30 seconds:\n');
      console.log('    npx -y srutam-mcp init\n');
      console.log('Or pass your key via environment variable:');
      console.log('    SRUTAM_API_KEY="srtm_live_..." srutam-mcp\n');
      process.exit(1);
    } else {
      console.error(
        'Missing SRUTAM_API_KEY. Configure the server using "npx srutam-mcp init" or provide SRUTAM_API_KEY environment variable.'
      );
      process.exit(1);
    }
  }

  const client = new SrutamClient(config);

  const server = new McpServer({
    name: 'srutam-mcp',
    version: '1.0.1',
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

async function main(): Promise<void> {
  const args = process.argv.slice(2);
  const command = args[0]?.toLowerCase();

  if (command === 'init' || command === 'setup' || command === '--setup') {
    await runWizard();
    return;
  }

  if (command === 'logout' || command === 'reset') {
    runLogout();
    return;
  }

  if (command === '--help' || command === '-h' || command === 'help') {
    printHelp();
    return;
  }

  if (command === '--version' || command === '-v') {
    console.log('srutam-mcp v1.0.1');
    return;
  }

  await startServer();
}

main().catch((err) => {
  console.error('Fatal error in Srutam MCP server:', err);
  process.exit(1);
});
