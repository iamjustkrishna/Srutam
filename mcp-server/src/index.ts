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

const VERSION = '1.2.0';

function printHelp(): void {
  console.log(`
Srutam MCP Server - Model Context Protocol for Srutam Voice Notes (v${VERSION})

Usage:
  srutam-mcp [command] [options]

Commands:
  init, setup    Interactive setup wizard (OpenCode, Cursor, Windsurf, Claude, Zed, Antigravity, Cline)
  status         Test cloud connection and view synced note count
  logout, reset  Clear saved API credentials
  [none]         In terminal: Show interactive status dashboard
                 In IDE: Run stdio MCP server for OpenCode, Cursor, Windsurf, Claude, Zed, Antigravity

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

async function showStatus(): Promise<void> {
  const config = loadConfig();
  if (!config) {
    console.error('✕ Not configured. Run "srutam-mcp init" first.');
    process.exit(1);
  }

  const client = new SrutamClient(config);
  try {
    const status = await client.getCloudStatus();
    console.log('✓ Srutam Cloud connection healthy');
    console.log(`User ID:        ${status.userId}`);
    console.log(`Synced Notes:   ${status.noteCount}`);
    console.log(`Pending Tasks:  ${status.pendingActionsCount}`);
  } catch (err: any) {
    console.error(`✕ Connection failed: ${err.message}`);
    process.exit(1);
  }
}

async function showInteractiveDashboard(): Promise<void> {
  const config = loadConfig();

  console.log(`
  ███████╗██████╗ ██╗   ██╗████████╗ █████╗ ███╗   ███╗
  ██╔════╝██╔══██╗██║   ██║╚══██╔══╝██╔══██╗████╗ ████║
  ███████╗██████╔╝██║   ██║   ██║   ███████║██╔████╔██║
  ╚════██║██╔══██╗██║   ██║   ██║   ██╔══██║██║╚██╔╝██║
  ███████║██║  ██║╚██████╔╝   ██║   ██║  ██║██║ ╚═╝ ██║
  ╚══════╝╚═╝  ╚═╝ ╚═════╝    ╚═╝   ╚═╝  ╚═╝╚═╝     ╚═╝
             Model Context Protocol (v${VERSION})
`);

  if (!config) {
    console.log('Status: Not Configured ✕');
    console.log('No Srutam API key found on this system.\n');
    console.log('To link your mobile voice notes with Cursor, Antigravity, or Claude:');
    console.log('  → Run: npx -y srutam-mcp init\n');
    console.log('Commands:');
    console.log('  srutam-mcp init      Run the interactive setup wizard');
    console.log('  srutam-mcp --help    Show command line options\n');
    return;
  }

  const keyPreview = config.apiKey.slice(0, 14) + '...' + config.apiKey.slice(-4);
  console.log(`Active Key:     ${keyPreview}`);

  process.stdout.write('Cloud Check:    Connecting to Srutam Cloud... ');
  try {
    const client = new SrutamClient(config);
    const status = await client.getCloudStatus();
    console.log('✓ Connected');
    console.log(`Synced Notes:   ${status.noteCount} voice note(s)`);
    console.log(`Pending Tasks:  ${status.pendingActionsCount} action item(s)`);
    console.log(`Protection:     In-memory cache (60s auth, 30s query), Rate limiter (60 req/min)`);
  } catch (err: any) {
    console.log('✕ Error');
    console.log(`                ${err.message}`);
    console.log('\nTip: Run "npx srutam-mcp init" to re-authenticate.');
    return;
  }

  console.log('\nAvailable Tools:');
  console.log('  • search_notes          - Search voice memos by keywords & semantics');
  console.log('  • list_recent_notes     - List recently recorded voice notes');
  console.log('  • get_note_detail       - Fetch full transcript, summary, and action items');
  console.log('  • list_action_items     - List pending tasks across voice memos');
  console.log('  • update_action_item    - Mark task as completed (syncs to phone)');
  console.log('  • append_agent_work_log - Attach commit message or work log to note');

  console.log('\nCLI Commands:');
  console.log('  srutam-mcp init         Re-run setup wizard for another IDE');
  console.log('  srutam-mcp status       Quick cloud connection health check');
  console.log('  srutam-mcp logout       Disconnect and remove saved credentials');
  console.log('  srutam-mcp help         View complete command line reference');

  console.log('\nIDE Integration:');
  console.log('  Runs over stdio when invoked by OpenCode, Cursor, Windsurf, Claude, Zed, or Antigravity.');
  console.log('  Standard config entry in mcp.json / mcp_config.json:');
  console.log('  {\n    "mcpServers": {\n      "srutam": {\n        "command": "srutam-mcp"\n      }\n    }\n  }\n');
}

async function startServer(): Promise<void> {
  const config = loadConfig();

  if (!config) {
    console.error(
      'Missing SRUTAM_API_KEY. Configure the server using "npx srutam-mcp init" or provide SRUTAM_API_KEY environment variable.'
    );
    process.exit(1);
  }

  const client = new SrutamClient(config);

  const server = new McpServer({
    name: 'srutam-mcp',
    version: VERSION,
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
  console.error(`Srutam MCP server v${VERSION} running on stdio`);
}

async function main(): Promise<void> {
  const args = process.argv.slice(2);
  const command = args[0]?.toLowerCase();

  if (command === 'init' || command === 'setup' || command === '--setup') {
    await runWizard();
    return;
  }

  if (command === 'status' || command === 'check') {
    await showStatus();
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
    console.log(`srutam-mcp v${VERSION}`);
    return;
  }

  // When run interactively from terminal without arguments, show dashboard
  if (process.stdin.isTTY) {
    await showInteractiveDashboard();
    return;
  }

  // Otherwise (e.g. piped or spawned as child process by IDE), run MCP stdio server
  await startServer();
}

main().catch((err) => {
  console.error('Fatal error in Srutam MCP server:', err);
  process.exit(1);
});
