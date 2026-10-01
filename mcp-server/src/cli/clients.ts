import fs from 'fs';
import path from 'path';
import os from 'os';
import { spawnSync } from 'child_process';
import { applyEdits, modify, parse, type ParseError } from 'jsonc-parser';
import { atomicWriteFileSync } from '../fsutil.js';
import { MAJOR_VERSION } from '../version.js';

// ---------------------------------------------------------------------------
// Launch command written into client configs
// ---------------------------------------------------------------------------

export interface Launcher {
  command: string;
  args: string[];
}

/**
 * The command a client runs to start the server. No API key: the server reads ~/.srutam/config.json itself,
 * so client config files (which get synced, committed and screenshotted) never contain a secret.
 * `serve` is explicit so startup never depends on TTY sniffing; the major version is pinned so a future
 * breaking release cannot reach an existing install unannounced. On Windows most clients spawn without a
 * shell, and `npx` is a .cmd shim, so it has to go through cmd.exe.
 */
export function getLauncher(platform: NodeJS.Platform = process.platform): Launcher {
  const args = ['-y', `srutam-mcp@${MAJOR_VERSION}`, 'serve'];
  return platform === 'win32' ? { command: 'cmd', args: ['/c', 'npx', ...args] } : { command: 'npx', args };
}

/** Codex/others can time out while `npx` downloads the package on first start. */
export const STARTUP_TIMEOUT_SEC = 60;

// ---------------------------------------------------------------------------
// Config file locations
// ---------------------------------------------------------------------------

function appData(): string {
  return process.env.APPDATA || path.join(os.homedir(), 'AppData', 'Roaming');
}

function xdgConfig(): string {
  return process.env.XDG_CONFIG_HOME || path.join(os.homedir(), '.config');
}

export function getCursorMcpPath(): string {
  return path.join(os.homedir(), '.cursor', 'mcp.json');
}

export function getWindsurfMcpPath(): string {
  return path.join(os.homedir(), '.codeium', 'windsurf', 'mcp_config.json');
}

/** OpenCode's user-level config only; never ./opencode.json in the working directory (usually git-tracked). */
export function getOpenCodeMcpPath(): string {
  return path.join(os.homedir(), '.config', 'opencode', 'opencode.json');
}

export function getZedSettingsPath(): string {
  if (process.platform === 'win32') return path.join(appData(), 'Zed', 'settings.json');
  return path.join(os.homedir(), '.config', 'zed', 'settings.json');
}

export function getClineMcpPath(): string {
  const tail = ['Code', 'User', 'globalStorage', 'saoudrizwan.claude-dev', 'settings', 'cline_mcp_settings.json'];
  if (process.platform === 'win32') return path.join(appData(), ...tail);
  if (process.platform === 'darwin') return path.join(os.homedir(), 'Library', 'Application Support', ...tail);
  return path.join(os.homedir(), '.config', ...tail);
}

export function getAntigravityMcpPath(): string {
  return path.join(os.homedir(), '.gemini', 'config', 'mcp_config.json');
}

export function getClaudeDesktopMcpPath(): string {
  if (process.platform === 'win32') return path.join(appData(), 'Claude', 'claude_desktop_config.json');
  if (process.platform === 'darwin') {
    return path.join(os.homedir(), 'Library', 'Application Support', 'Claude', 'claude_desktop_config.json');
  }
  return path.join(os.homedir(), '.config', 'Claude', 'claude_desktop_config.json');
}

export function getCodexConfigPath(): string {
  return path.join(process.env.CODEX_HOME || path.join(os.homedir(), '.codex'), 'config.toml');
}

export function getGeminiCliPath(): string {
  return path.join(os.homedir(), '.gemini', 'settings.json');
}

export function getQwenCodePath(): string {
  return path.join(os.homedir(), '.qwen', 'settings.json');
}

export function getCopilotCliPath(): string {
  return path.join(os.homedir(), '.copilot', 'mcp-config.json');
}

export function getAmpSettingsPath(): string {
  return path.join(os.homedir(), '.config', 'amp', 'settings.json');
}

export function getCrushConfigPath(): string {
  if (process.platform === 'win32') {
    const local = process.env.LOCALAPPDATA || path.join(os.homedir(), 'AppData', 'Local');
    return path.join(local, 'crush', 'crush.json');
  }
  return path.join(xdgConfig(), 'crush', 'crush.json');
}

export function getVsCodeUserMcpPath(): string {
  if (process.platform === 'win32') return path.join(appData(), 'Code', 'User', 'mcp.json');
  if (process.platform === 'darwin') {
    return path.join(os.homedir(), 'Library', 'Application Support', 'Code', 'User', 'mcp.json');
  }
  return path.join(xdgConfig(), 'Code', 'User', 'mcp.json');
}

export function getKiroMcpPath(): string {
  return path.join(process.env.KIRO_HOME || path.join(os.homedir(), '.kiro'), 'settings', 'mcp.json');
}

// ---------------------------------------------------------------------------
// Safe JSON / JSONC editing (never destructive)
// ---------------------------------------------------------------------------

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

/**
 * Inserts/replaces `value` at `jsonPath` while preserving everything else in the file, including comments and
 * formatting (Zed, OpenCode and VS Code configs are JSONC). A file that cannot be parsed is left exactly as-is
 * and false is returned. A one-time `<file>.srutam.bak` copy is taken before the first change to an existing file.
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
          `  Fix the file (or add the "srutam" entry manually with "srutam-mcp print-config") and re-run "srutam-mcp init".`
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

/** Cursor, Windsurf, Claude Desktop, Antigravity, Cline, Gemini CLI, Qwen Code, Kiro, ...: `{ "mcpServers": { ... } }` */
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

/** VS Code / GitHub Copilot: top-level `servers` (not `mcpServers`) with an explicit transport type. */
export function injectVsCodeMcpServer(filePath: string, platform: NodeJS.Platform = process.platform): boolean {
  const { command, args } = getLauncher(platform);
  return upsertJsonEntry(filePath, ['servers', 'srutam'], { type: 'stdio', command, args }, { servers: {} });
}

export function injectCrushMcpServer(filePath: string, platform: NodeJS.Platform = process.platform): boolean {
  const { command, args } = getLauncher(platform);
  return upsertJsonEntry(filePath, ['mcp', 'srutam'], { type: 'stdio', command, args }, { mcp: {} });
}

/** Amp keeps servers under the literal dotted key "amp.mcpServers". */
export function injectAmpMcpServer(filePath: string, platform: NodeJS.Platform = process.platform): boolean {
  const { command, args } = getLauncher(platform);
  return upsertJsonEntry(filePath, ['amp.mcpServers', 'srutam'], { command, args }, { 'amp.mcpServers': {} });
}

// ---------------------------------------------------------------------------
// Codex: ~/.codex/config.toml (TOML, edited as text so comments and other tables survive untouched)
// ---------------------------------------------------------------------------

const TOML_HEADER = '[mcp_servers.srutam]';
const OWNED_KEYS = new Set(['command', 'args', 'startup_timeout_sec']);

function tomlString(value: string): string {
  return `"${value.replace(/\\/g, '\\\\').replace(/"/g, '\\"')}"`;
}

export function codexTomlBlock(platform: NodeJS.Platform = process.platform, eol = '\n'): string {
  const { command, args } = getLauncher(platform);
  return [
    TOML_HEADER,
    `command = ${tomlString(command)}`,
    `args = [${args.map(tomlString).join(', ')}]`,
    `startup_timeout_sec = ${STARTUP_TIMEOUT_SEC}`,
  ].join(eol);
}

/** Net bracket depth change of a TOML line, ignoring quoted strings and comments (enough for multi-line arrays). */
function bracketDelta(line: string): number {
  let depth = 0;
  let quote: string | null = null;
  for (let i = 0; i < line.length; i++) {
    const c = line[i];
    if (quote) {
      if (c === '\\' && quote === '"') i++;
      else if (c === quote) quote = null;
    } else if (c === '"' || c === "'") quote = c;
    else if (c === '#') break;
    else if (c === '[') depth++;
    else if (c === ']') depth--;
  }
  return depth;
}

/**
 * Returns the updated TOML text with `[mcp_servers.srutam]` set to our launcher, or null when the file has a shape
 * we refuse to edit blindly (`mcp_servers` defined as an inline table, or `srutam` set through dotted keys).
 * Only the keys we own (command, args, startup_timeout_sec) are rewritten; any other key in that table, any
 * `[mcp_servers.srutam.*]` sub-table, comments and all other tables are preserved.
 */
export function upsertCodexToml(text: string, platform: NodeJS.Platform = process.platform): string | null {
  const eol = text.includes('\r\n') ? '\r\n' : '\n';
  const lines = text.split(/\r?\n/);

  if (lines.some((l) => /^\s*mcp_servers\s*=/.test(l) || /^\s*mcp_servers\.srutam[\s.=]/.test(l))) {
    return null;
  }

  const block = codexTomlBlock(platform, eol).split(eol);
  const headerRe = /^\s*\[\s*mcp_servers\s*\.\s*(?:srutam|"srutam"|'srutam')\s*\]\s*(?:#.*)?$/;
  const headerIndex = lines.findIndex((l) => headerRe.test(l));

  if (headerIndex === -1) {
    const trimmed = text.replace(/\s+$/, '');
    const prefix = trimmed === '' ? '' : trimmed + eol + eol;
    return prefix + block.join(eol) + eol;
  }

  const kept: string[] = [];
  let depth = 0;
  let end = lines.length;
  for (let i = headerIndex + 1; i < lines.length; i++) {
    if (depth === 0 && /^\s*\[/.test(lines[i])) {
      end = i;
      break;
    }
    depth += bracketDelta(lines[i]);
  }

  depth = 0;
  let skipping = false;
  for (let i = headerIndex + 1; i < end; i++) {
    const line = lines[i];
    if (skipping) {
      depth += bracketDelta(line);
      if (depth <= 0) skipping = false;
      continue;
    }
    const key = /^\s*([A-Za-z0-9_-]+)\s*=/.exec(line)?.[1];
    if (key && OWNED_KEYS.has(key)) {
      depth = bracketDelta(line);
      skipping = depth > 0;
      continue;
    }
    kept.push(line);
  }

  return [...lines.slice(0, headerIndex), ...block, ...kept, ...lines.slice(end)].join(eol);
}

export function injectCodexToml(filePath: string, platform: NodeJS.Platform = process.platform): boolean {
  try {
    const exists = fs.existsSync(filePath);
    const text = exists ? fs.readFileSync(filePath, 'utf8') : '';
    const updated = upsertCodexToml(text, platform);
    if (updated === null) {
      console.error(
        `✕ Skipped ${filePath}: "mcp_servers" is defined in a form the wizard will not edit automatically, so the file was left untouched.\n` +
          `  Add the srutam entry by hand (see "srutam-mcp print-config codex").`
      );
      return false;
    }
    fs.mkdirSync(path.dirname(filePath), { recursive: true });
    if (exists && text.trim() !== '') {
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

// ---------------------------------------------------------------------------
// Claude Code: configured through its own CLI (~/.claude.json is large and rewritten while it runs)
// ---------------------------------------------------------------------------

export interface ExecResult {
  status: number | null;
  stderr: string;
  error?: Error;
}
export type ExecFn = (file: string, args: string[]) => ExecResult;

const defaultExec: ExecFn = (file, args) => {
  const r = spawnSync(file, args, { encoding: 'utf8', timeout: 30_000, windowsHide: true, shell: false });
  return { status: r.status, stderr: `${r.stderr || ''}`.trim(), error: r.error };
};

export function claudeAddCommand(platform: NodeJS.Platform = process.platform): string[] {
  const { command, args } = getLauncher(platform);
  return ['mcp', 'add', '--scope', 'user', 'srutam', '--', command, ...args];
}

/** Runs `claude mcp add` (no shell). Replaces an existing "srutam" server. */
export function addToClaudeCode(
  platform: NodeJS.Platform = process.platform,
  exec: ExecFn = defaultExec
): { ok: boolean; detail: string } {
  const manual = `claude ${claudeAddCommand(platform).join(' ')}`;
  const probe = exec('claude', ['--version']);
  if (probe.error || probe.status !== 0) {
    return { ok: false, detail: `"claude" was not found on PATH. Run this yourself:\n    ${manual}` };
  }
  exec('claude', ['mcp', 'remove', '--scope', 'user', 'srutam']); // ignore "not found"
  const added = exec('claude', claudeAddCommand(platform));
  if (added.error || added.status !== 0) {
    return { ok: false, detail: `"claude mcp add" failed (${added.stderr || added.error?.message || 'unknown error'}). Run this yourself:\n    ${manual}` };
  }
  return { ok: true, detail: 'user scope (claude mcp list)' };
}

// ---------------------------------------------------------------------------
// Client table
// ---------------------------------------------------------------------------

export type ClientFormat =
  | 'mcpServers'
  | 'opencode'
  | 'zed'
  | 'vscode'
  | 'crush'
  | 'amp'
  | 'codex-toml'
  | 'claude-cli'
  | 'manual';

export interface ClientTarget {
  id: string;
  name: string;
  aliases?: string[];
  format: ClientFormat;
  /** Format was not confirmed against the real client; the wizard says so. */
  unverified?: boolean;
  configPath?: () => string;
  /** Any of these existing means the application is installed/used on this machine. */
  detectPaths: () => string[];
  /** Or this command being on PATH. */
  detectCommand?: string;
}

const parent = (p: () => string) => () => [path.dirname(p())];

export const CLIENTS: ClientTarget[] = [
  { id: 'claude-code', name: 'Claude Code', aliases: ['claude', 'claudecode'], format: 'claude-cli', detectPaths: () => [path.join(os.homedir(), '.claude')], detectCommand: 'claude' },
  { id: 'codex', name: 'OpenAI Codex (CLI + IDE)', aliases: ['openai-codex'], format: 'codex-toml', configPath: getCodexConfigPath, detectPaths: parent(getCodexConfigPath), detectCommand: 'codex' },
  { id: 'gemini-cli', name: 'Gemini CLI', aliases: ['gemini'], format: 'mcpServers', configPath: getGeminiCliPath, detectPaths: () => [getGeminiCliPath()], detectCommand: 'gemini' },
  { id: 'cursor', name: 'Cursor', format: 'mcpServers', configPath: getCursorMcpPath, detectPaths: parent(getCursorMcpPath) },
  { id: 'windsurf', name: 'Windsurf', format: 'mcpServers', configPath: getWindsurfMcpPath, detectPaths: parent(getWindsurfMcpPath) },
  { id: 'vscode', name: 'VS Code / GitHub Copilot', aliases: ['code', 'copilot', 'github-copilot'], format: 'vscode', configPath: getVsCodeUserMcpPath, detectPaths: parent(getVsCodeUserMcpPath) },
  { id: 'opencode', name: 'OpenCode', format: 'opencode', configPath: getOpenCodeMcpPath, detectPaths: parent(getOpenCodeMcpPath), detectCommand: 'opencode' },
  { id: 'claude-desktop', name: 'Claude Desktop', aliases: ['claude-app'], format: 'mcpServers', configPath: getClaudeDesktopMcpPath, detectPaths: parent(getClaudeDesktopMcpPath) },
  { id: 'zed', name: 'Zed', format: 'zed', configPath: getZedSettingsPath, detectPaths: parent(getZedSettingsPath) },
  { id: 'kiro', name: 'Kiro', format: 'mcpServers', configPath: getKiroMcpPath, detectPaths: () => [path.dirname(path.dirname(getKiroMcpPath()))] },
  { id: 'qwen-code', name: 'Qwen Code', aliases: ['qwen'], format: 'mcpServers', configPath: getQwenCodePath, detectPaths: parent(getQwenCodePath), detectCommand: 'qwen' },
  { id: 'crush', name: 'Crush', format: 'crush', configPath: getCrushConfigPath, detectPaths: parent(getCrushConfigPath), detectCommand: 'crush' },
  { id: 'amp', name: 'Amp', format: 'amp', unverified: true, configPath: getAmpSettingsPath, detectPaths: parent(getAmpSettingsPath), detectCommand: 'amp' },
  { id: 'copilot-cli', name: 'GitHub Copilot CLI', format: 'mcpServers', unverified: true, configPath: getCopilotCliPath, detectPaths: parent(getCopilotCliPath) },
  { id: 'cline', name: 'Cline (VS Code)', aliases: ['roo'], format: 'mcpServers', configPath: getClineMcpPath, detectPaths: () => [path.dirname(path.dirname(getClineMcpPath()))] },
  { id: 'antigravity', name: 'Antigravity', format: 'mcpServers', unverified: true, configPath: getAntigravityMcpPath, detectPaths: () => [path.dirname(getAntigravityMcpPath())] },
];

/** Manual-only clients: shown by `print-config`, never written automatically. */
export const MANUAL_CLIENTS: Array<{ id: string; name: string; format: ClientFormat; where: string }> = [
  { id: 'goose', name: 'Goose', format: 'manual', where: '~/.config/goose/config.yaml (or run "goose configure")' },
  { id: 'continue', name: 'Continue', format: 'manual', where: '~/.continue/config.yaml (format changes often; see its docs)' },
  { id: 'jetbrains', name: 'JetBrains AI Assistant / Junie', format: 'manual', where: 'Settings > Tools > AI Assistant > Model Context Protocol (paste as JSON)' },
];

/** True when `name` resolves to an executable on PATH (PATHEXT-aware on Windows). No process is spawned. */
export function commandOnPath(name: string, env: NodeJS.ProcessEnv = process.env, platform: NodeJS.Platform = process.platform): boolean {
  const pathVar = env.PATH || env.Path || '';
  const dirs = pathVar.split(platform === 'win32' ? ';' : ':').filter(Boolean);
  const exts = platform === 'win32' ? (env.PATHEXT || '.EXE;.CMD;.BAT;.COM').split(';') : [''];
  return dirs.some((dir) => exts.some((ext) => {
    try {
      return fs.statSync(path.join(dir, name + ext.toLowerCase())).isFile() || fs.statSync(path.join(dir, name + ext)).isFile();
    } catch {
      return false;
    }
  }));
}

export function isClientDetected(client: ClientTarget, env: NodeJS.ProcessEnv = process.env): boolean {
  try {
    if (client.detectPaths().some((p) => fs.existsSync(p))) return true;
  } catch {
    // fall through to the command check
  }
  return client.detectCommand ? commandOnPath(client.detectCommand, env) : false;
}

export interface ApplyResult {
  ok: boolean;
  detail: string;
}

/** Writes the srutam entry for one client. Never throws. */
export function applyClient(client: ClientTarget, platform: NodeJS.Platform = process.platform): ApplyResult {
  const file = client.configPath?.() ?? '';
  const done = (ok: boolean): ApplyResult => ({ ok, detail: file });
  switch (client.format) {
    case 'claude-cli':
      return addToClaudeCode(platform);
    case 'codex-toml':
      return done(injectCodexToml(file, platform));
    case 'opencode':
      return done(injectOpenCodeMcpServer(file, platform));
    case 'zed':
      return done(injectZedMcpServer(file, platform));
    case 'vscode':
      return done(injectVsCodeMcpServer(file, platform));
    case 'crush':
      return done(injectCrushMcpServer(file, platform));
    case 'amp':
      return done(injectAmpMcpServer(file, platform));
    case 'mcpServers':
      return done(injectStandardMcpServer(file, platform));
    default:
      return { ok: false, detail: 'manual configuration only' };
  }
}

// ---------------------------------------------------------------------------
// Client selection (numbers, ids, names, "a" = all detected)
// ---------------------------------------------------------------------------

const norm = (s: string) => s.toLowerCase().replace(/[^a-z0-9]/g, '');

export function resolveClientSelection(
  input: string,
  clients: ClientTarget[] = CLIENTS,
  detected: (c: ClientTarget) => boolean = isClientDetected
): { matched: ClientTarget[]; unknown: string[]; showManual: boolean } {
  const matched: ClientTarget[] = [];
  const unknown: string[] = [];
  let showManual = false;
  const add = (c: ClientTarget) => {
    if (!matched.includes(c)) matched.push(c);
  };

  for (const raw of input.split(/[\s,]+/).filter(Boolean)) {
    const token = norm(raw);
    if (token === 'a' || token === 'all' || token === 'detected') {
      clients.filter((c) => detected(c)).forEach(add);
    } else if (token === 'm' || token === 'manual') {
      showManual = true;
    } else if (/^\d+$/.test(raw) && Number(raw) >= 1 && Number(raw) <= clients.length) {
      add(clients[Number(raw) - 1]);
    } else {
      const hit = clients.find((c) => norm(c.id) === token || norm(c.name) === token || c.aliases?.some((a) => norm(a) === token));
      if (hit) add(hit);
      else unknown.push(raw);
    }
  }
  return { matched, unknown, showManual };
}

// ---------------------------------------------------------------------------
// Snippets (print-config and manual mode)
// ---------------------------------------------------------------------------

const pretty = (value: unknown) => JSON.stringify(value, null, 2);

export function renderSnippet(format: ClientFormat, platform: NodeJS.Platform = process.platform): string {
  const { command, args } = getLauncher(platform);
  switch (format) {
    case 'mcpServers':
      return pretty({ mcpServers: { srutam: { command, args } } });
    case 'opencode':
      return pretty({ $schema: 'https://opencode.ai/config.json', mcp: { srutam: { type: 'local', command: [command, ...args], enabled: true } } });
    case 'zed':
      return pretty({ context_servers: { srutam: { command: { path: command, args } } } });
    case 'vscode':
      return pretty({ servers: { srutam: { type: 'stdio', command, args } } });
    case 'crush':
      return pretty({ mcp: { srutam: { type: 'stdio', command, args } } });
    case 'amp':
      return pretty({ 'amp.mcpServers': { srutam: { command, args } } });
    case 'codex-toml':
      return codexTomlBlock(platform);
    case 'claude-cli':
      return `claude ${claudeAddCommand(platform).join(' ')}`;
    default:
      return `extensions:\n  srutam:\n    name: srutam\n    type: stdio\n    cmd: ${command}\n    args: [${args.join(', ')}]\n    enabled: true\n    timeout: 300`;
  }
}

/** Snippets for every supported client, grouped by identical format. */
export function manualSnippets(platform: NodeJS.Platform = process.platform): string {
  const groups = new Map<ClientFormat, string[]>();
  for (const c of CLIENTS) groups.set(c.format, [...(groups.get(c.format) ?? []), c.name]);
  const parts: string[] = [];
  for (const [format, names] of groups) {
    parts.push(`--- ${names.join(' / ')} ---\n${renderSnippet(format, platform)}`);
  }
  parts.push(`--- ${MANUAL_CLIENTS.map((m) => m.name).join(' / ')} (YAML shown; adapt) ---\n${renderSnippet('manual', platform)}`);
  parts.push(
    'No API key belongs in these files: the server reads it from ~/.srutam/config.json.\n' +
      'If your client runs somewhere that cannot see that file (WSL, a dev container, SSH), add an env entry instead:\n' +
      '  "env": { "SRUTAM_API_KEY": "srtm_live_..." }'
  );
  return parts.join('\n\n');
}

/** Finds a client by id/alias/name for `print-config`. */
export function findClient(input: string): { client?: ClientTarget; manual?: (typeof MANUAL_CLIENTS)[number] } {
  const token = norm(input);
  const client = CLIENTS.find((c) => norm(c.id) === token || norm(c.name) === token || c.aliases?.some((a) => norm(a) === token));
  if (client) return { client };
  return { manual: MANUAL_CLIENTS.find((m) => norm(m.id) === token || norm(m.name) === token) };
}
