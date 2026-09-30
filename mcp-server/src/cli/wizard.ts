import readline from 'readline/promises';
import { Writable } from 'stream';
import { stdin as input, stdout as output } from 'process';
import fs from 'fs';
import path from 'path';
import os from 'os';
import { applyEdits, modify, parse, type ParseError } from 'jsonc-parser';
import { DEFAULTS, getConfigPath, isStandardSupabaseUrl, resolveEndpoint, saveUserConfig, clearUserConfig } from '../config.js';
import { SrutamClient } from '../supabase.js';
import { atomicWriteFileSync } from '../fsutil.js';
import { MAJOR_VERSION } from '../version.js';

// ---------------------------------------------------------------------------
// Config file locations
// ---------------------------------------------------------------------------

export function getCursorMcpPath(): string {
  return path.join(os.homedir(), '.cursor', 'mcp.json');
}

export function getWindsurfMcpPath(): string {
  return path.join(os.homedir(), '.codeium', 'windsurf', 'mcp_config.json');
}

/**
 * OpenCode's user-level config only. We intentionally never write to ./opencode.json in the
 * working directory: that is usually a git-tracked project file.
 */
export function getOpenCodeMcpPath(): string {
  return path.join(os.homedir(), '.config', 'opencode', 'opencode.json');
}

export function getZedSettingsPath(): string {
  if (process.platform === 'win32') {
    const appData = process.env.APPDATA || path.join(os.homedir(), 'AppData', 'Roaming');
    return path.join(appData, 'Zed', 'settings.json');
  }
  return path.join(os.homedir(), '.config', 'zed', 'settings.json');
}

export function getClineMcpPath(): string {
  const tail = ['Code', 'User', 'globalStorage', 'saoudrizwan.claude-dev', 'settings', 'cline_mcp_settings.json'];
  if (process.platform === 'win32') {
    const appData = process.env.APPDATA || path.join(os.homedir(), 'AppData', 'Roaming');
    return path.join(appData, ...tail);
  } else if (process.platform === 'darwin') {
    return path.join(os.homedir(), 'Library', 'Application Support', ...tail);
  }
  return path.join(os.homedir(), '.config', ...tail);
}

export function getAntigravityMcpPath(): string {
  return path.join(os.homedir(), '.gemini', 'config', 'mcp_config.json');
}

export function getClaudeDesktopMcpPath(): string {
  if (process.platform === 'win32') {
    const appData = process.env.APPDATA || path.join(os.homedir(), 'AppData', 'Roaming');
    return path.join(appData, 'Claude', 'claude_desktop_config.json');
  } else if (process.platform === 'darwin') {
    return path.join(os.homedir(), 'Library', 'Application Support', 'Claude', 'claude_desktop_config.json');
  }
  return path.join(os.homedir(), '.config', 'Claude', 'claude_desktop_config.json');
}

// ---------------------------------------------------------------------------
// Launch command written into IDE configs
// ---------------------------------------------------------------------------

export interface Launcher {
  command: string;
  args: string[];
}

/**
 * The command an IDE runs to start the server. Note what is NOT here: no API key. The server reads
 * ~/.srutam/config.json itself, so IDE config files (which get synced, committed and screenshotted)
 * never contain a secret. `serve` is explicit so startup never depends on TTY sniffing, and the major
 * version is pinned so a future breaking release cannot reach an existing install unannounced.
 * On Windows many clients spawn without a shell, so `npx` (a .cmd shim) must go through cmd.exe.
 */
export function getLauncher(platform: NodeJS.Platform = process.platform): Launcher {
  const args = ['-y', `srutam-mcp@${MAJOR_VERSION}`, 'serve'];
  return platform === 'win32' ? { command: 'cmd', args: ['/c', 'npx', ...args] } : { command: 'npx', args };
}

// ---------------------------------------------------------------------------
// Safe config editing (JSON with comments / trailing commas, never destructive)
// ---------------------------------------------------------------------------

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

/**
 * Inserts/replaces `value` at `jsonPath` while preserving everything else in the file, including
 * comments and formatting (Zed and OpenCode configs are JSONC). If the existing file cannot be
 * parsed it is left exactly as-is and false is returned - it is never overwritten.
 * A one-time `<file>.srutam.bak` copy is taken before the first modification of an existing file.
 */
function upsertJsonEntry(
  filePath: string,
  jsonPath: [string, string],
  value: unknown,
  freshDocument: Record<string, unknown>
): boolean {
  try {
    const exists = fs.existsSync(filePath);
    let text = exists ? fs.readFileSync(filePath, 'utf8') : '';
    if (text.charCodeAt(0) === 0xfeff) text = text.slice(1);

    const wasBlank = text.trim() === '';
    if (wasBlank) {
      text = JSON.stringify(freshDocument, null, 2) + '\n';
    }

    const errors: ParseError[] = [];
    const doc = parse(text, errors, { allowTrailingComma: true });
    if (errors.length > 0 || !isPlainObject(doc)) {
      console.error(
        `✕ Skipped ${filePath}: it is not valid JSON, so it was left untouched.\n` +
          `  Fix the file (or add the "srutam" entry manually - see option 9) and re-run "srutam-mcp init".`
      );
      return false;
    }
    if (doc[jsonPath[0]] !== undefined && !isPlainObject(doc[jsonPath[0]])) {
      console.error(`✕ Skipped ${filePath}: "${jsonPath[0]}" exists but is not an object, so the file was left untouched.`);
      return false;
    }

    const edits = modify(text, jsonPath, value, {
      formattingOptions: { insertSpaces: true, tabSize: 2, eol: text.includes('\r\n') ? '\r\n' : '\n' },
    });
    const updated = applyEdits(text, edits);

    fs.mkdirSync(path.dirname(filePath), { recursive: true });
    if (exists && !wasBlank) {
      const backupPath = `${filePath}.srutam.bak`;
      if (!fs.existsSync(backupPath)) fs.copyFileSync(filePath, backupPath);
    }
    atomicWriteFileSync(filePath, updated, 0o600);
    return true;
  } catch (err: any) {
    console.error(`Failed to update ${filePath}: ${err.message}`);
    return false;
  }
}

/** Cursor, Windsurf, Claude Desktop, Antigravity, Cline: `{ "mcpServers": { ... } }` */
export function injectStandardMcpServer(filePath: string, platform: NodeJS.Platform = process.platform): boolean {
  const { command, args } = getLauncher(platform);
  return upsertJsonEntry(filePath, ['mcpServers', 'srutam'], { command, args }, { mcpServers: {} });
}

export function injectOpenCodeMcpServer(filePath: string, platform: NodeJS.Platform = process.platform): boolean {
  const { command, args } = getLauncher(platform);
  return upsertJsonEntry(
    filePath,
    ['mcp', 'srutam'],
    { type: 'local', command: [command, ...args], enabled: true },
    { $schema: 'https://opencode.ai/config.json', mcp: {} }
  );
}

export function injectZedMcpServer(filePath: string, platform: NodeJS.Platform = process.platform): boolean {
  const { command, args } = getLauncher(platform);
  return upsertJsonEntry(filePath, ['context_servers', 'srutam'], { command: { path: command, args } }, {});
}

// ---------------------------------------------------------------------------
// Client table (drives menu, detection and configuration)
// ---------------------------------------------------------------------------

export interface ClientTarget {
  choice: string;
  name: string;
  configPath: () => string;
  /** A directory that exists only if the application is installed/used on this machine. */
  detectDir: () => string;
  write: (filePath: string) => boolean;
}

export const CLIENTS: ClientTarget[] = [
  { choice: '1', name: 'Cursor IDE', configPath: getCursorMcpPath, detectDir: () => path.dirname(getCursorMcpPath()), write: (p) => injectStandardMcpServer(p) },
  { choice: '2', name: 'Windsurf', configPath: getWindsurfMcpPath, detectDir: () => path.dirname(getWindsurfMcpPath()), write: (p) => injectStandardMcpServer(p) },
  { choice: '3', name: 'OpenCode', configPath: getOpenCodeMcpPath, detectDir: () => path.dirname(getOpenCodeMcpPath()), write: (p) => injectOpenCodeMcpServer(p) },
  { choice: '4', name: 'Claude Desktop', configPath: getClaudeDesktopMcpPath, detectDir: () => path.dirname(getClaudeDesktopMcpPath()), write: (p) => injectStandardMcpServer(p) },
  { choice: '5', name: 'Antigravity', configPath: getAntigravityMcpPath, detectDir: () => path.dirname(path.dirname(getAntigravityMcpPath())), write: (p) => injectStandardMcpServer(p) },
  { choice: '6', name: 'Zed Editor', configPath: getZedSettingsPath, detectDir: () => path.dirname(getZedSettingsPath()), write: (p) => injectZedMcpServer(p) },
  { choice: '7', name: 'Cline (VS Code)', configPath: getClineMcpPath, detectDir: () => path.dirname(path.dirname(getClineMcpPath())), write: (p) => injectStandardMcpServer(p) },
];

export function isClientDetected(client: ClientTarget): boolean {
  try {
    return fs.existsSync(client.detectDir());
  } catch {
    return false;
  }
}

export function manualSnippets(platform: NodeJS.Platform = process.platform): string {
  const { command, args } = getLauncher(platform);
  const pretty = (value: unknown) => JSON.stringify(value, null, 2);
  return [
    '--- Standard MCP (Cursor / Windsurf / Claude Desktop / Antigravity / Cline) ---',
    pretty({ mcpServers: { srutam: { command, args } } }),
    '\n--- OpenCode (~/.config/opencode/opencode.json) ---',
    pretty({ $schema: 'https://opencode.ai/config.json', mcp: { srutam: { type: 'local', command: [command, ...args], enabled: true } } }),
    '\n--- Zed Editor (settings.json) ---',
    pretty({ context_servers: { srutam: { command: { path: command, args } } } }),
    '\nNo API key belongs in these files: the server reads it from ~/.srutam/config.json.',
    'If your IDE runs somewhere that cannot see that file (WSL, a dev container, SSH), add',
    '  "env": { "SRUTAM_API_KEY": "srtm_live_..." }',
    'to the entry instead.',
  ].join('\n');
}

// ---------------------------------------------------------------------------
// Interactive wizard
// ---------------------------------------------------------------------------

const MAX_KEY_ATTEMPTS = 3;

export async function runWizard(): Promise<void> {
  // Throws ConfigError (reported by the caller) if SUPABASE_URL is not an allowed host.
  const endpoint = resolveEndpoint();

  // Output goes through a mutable filter so the API key is not echoed while it is typed.
  let muted = false;
  const filteredOutput = new Writable({
    write(chunk, encoding, callback) {
      if (!muted) output.write(chunk, encoding as BufferEncoding);
      callback();
    },
  });
  const rl = readline.createInterface({ input, output: filteredOutput, terminal: Boolean(input.isTTY) });

  // Lines are queued rather than read with rl.question(): question() silently drops any line that
  // arrives while no question is pending, which loses answers when input is piped
  // (`echo KEY | srutam-mcp init`) and the wizard is busy verifying the key over the network.
  const queued: string[] = [];
  let pending: { resolve: (line: string) => void; reject: (err: Error) => void } | null = null;
  let inputClosed = false;
  rl.on('line', (line) => {
    if (pending) {
      const { resolve } = pending;
      pending = null;
      resolve(line);
    } else {
      queued.push(line);
    }
  });
  rl.on('close', () => {
    inputClosed = true; // Ctrl+D / Ctrl+C / piped input ended: fail pending and future reads instead of hanging
    if (pending) {
      const { reject } = pending;
      pending = null;
      reject(new Error('Input closed before setup finished.'));
    }
  });

  const nextLine = (): Promise<string> =>
    new Promise((resolve, reject) => {
      if (queued.length > 0) return resolve(queued.shift() as string);
      if (inputClosed) return reject(new Error('Input closed before setup finished.'));
      pending = { resolve, reject };
    });

  const ask = async (question: string): Promise<string> => {
    output.write(question);
    return nextLine();
  };
  const askSecret = async (question: string): Promise<string> => {
    output.write(question);
    muted = true;
    try {
      return await nextLine();
    } finally {
      muted = false;
      output.write('\n');
    }
  };

  try {
    console.log('\n=======================================================');
    console.log('             Srutam MCP - Developer Setup');
    console.log("   Connect your phone's voice notes to your AI agent");
    console.log('=======================================================\n');

    console.log('To get your API key:');
    console.log('1. Open Srutam app on your phone.');
    console.log('2. Go to Settings -> Cloud Sync & Developer Brain (MCP) -> MCP Agent Keys -> New Key.');
    console.log('3. Tap Generate and copy your key.\n');

    let apiKey = '';
    for (let attempt = 1; attempt <= MAX_KEY_ATTEMPTS && !apiKey; attempt++) {
      const trimmed = (await askSecret('Enter your Srutam API key (input is hidden): ')).trim();
      if (!trimmed) {
        console.log('No key entered.\n');
        continue;
      }
      if (!trimmed.startsWith('srtm_live_')) {
        console.log('Warning: Srutam API keys typically start with "srtm_live_".');
      }

      console.log('\nVerifying API key with Srutam Cloud...');
      try {
        const status = await new SrutamClient({ apiKey: trimmed, ...endpoint }).getCloudStatus();
        console.log(
          `Key verified successfully! (account ${status.userId.slice(0, 8)}…, ${status.noteCount} note(s) visible to agents)\n`
        );
        apiKey = trimmed;
      } catch (err: any) {
        console.log(`Verification failed: ${err.message}`);
        if (attempt < MAX_KEY_ATTEMPTS) console.log('Please check the key copied from your phone and try again.\n');
      }
    }

    if (!apiKey) {
      console.log(`\nSetup aborted: no valid API key after ${MAX_KEY_ATTEMPTS} attempts.\n`);
      process.exitCode = 1;
      return;
    }

    // The only place the key is stored (0600 on POSIX; user-profile ACL on Windows).
    saveUserConfig({
      apiKey,
      ...(endpoint.supabaseUrl !== DEFAULTS.supabaseUrl
        ? { supabaseUrl: endpoint.supabaseUrl, allowCustomUrl: !isStandardSupabaseUrl(endpoint.supabaseUrl) }
        : {}),
      ...(endpoint.supabaseAnonKey !== DEFAULTS.supabaseAnonKey ? { supabaseAnonKey: endpoint.supabaseAnonKey } : {}),
    });
    console.log(`Saved credentials to ${getConfigPath()}\n`);

    console.log('Which AI Coding Assistant do you want to configure?');
    for (const client of CLIENTS) {
      console.log(`  [${client.choice}] ${client.name}`);
    }
    console.log('  [8] All detected assistants (only those already installed on this machine)');
    console.log('  [9] Manual configuration (show snippets)\n');

    const selected = ((await ask('Choose an option (1-9) [default: 8]: ')).trim()) || '8';

    let targets: ClientTarget[] = [];
    if (selected === '8') {
      targets = CLIENTS.filter(isClientDetected);
      if (targets.length === 0) {
        console.log('No supported assistants were detected on this machine.');
      }
    } else {
      targets = CLIENTS.filter((c) => c.choice === selected);
    }

    let configuredAny = false;
    for (const client of targets) {
      const filePath = client.configPath();
      if (client.write(filePath)) {
        console.log(`✓ Configured ${client.name} in ${filePath}`);
        configuredAny = true;
      }
    }

    if (selected === '9' || !configuredAny) {
      console.log('\n' + manualSnippets());
    }

    console.log('\nSetup complete!');
    if (configuredAny) {
      console.log('Your API key is stored only in ' + getConfigPath() + ' - the IDE config files contain no secret.');
    }
    console.log('Restart or refresh MCP servers in your IDE.');
    console.log('Then ask: "List my recent voice notes from Srutam" or "What action items did I speak about?"\n');
  } catch (err: any) {
    // Ctrl+D / closed stdin: report calmly instead of a stack trace.
    if (err instanceof Error && err.message.startsWith('Input closed')) {
      console.log(`\n${err.message} Nothing was changed beyond what is listed above.`);
      process.exitCode = 1;
      return;
    }
    throw err;
  } finally {
    rl.close();
  }
}

export function runLogout(): void {
  clearUserConfig();
  console.log(`Cleared Srutam credentials from ${getConfigPath()}.`);
  console.log('If you ever pasted the key into an IDE config manually, remove it there too and revoke the key in the app.');
  console.log('To reconnect anytime, run: npx srutam-mcp init\n');
}
