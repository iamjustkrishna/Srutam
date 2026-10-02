import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import fs from 'fs';
import os from 'os';
import path from 'path';
import { parse as parseToml } from 'smol-toml';
import { parse as parseJsonc } from 'jsonc-parser';
import {
  CLIENTS,
  MANUAL_CLIENTS,
  STARTUP_TIMEOUT_SEC,
  addToClaudeCode,
  applyClient,
  claudeAddCommand,
  codexTomlBlock,
  commandOnPath,
  findClient,
  getLauncher,
  injectAmpMcpServer,
  injectCodexToml,
  injectCrushMcpServer,
  injectStandardMcpServer,
  injectVsCodeMcpServer,
  isClientDetected,
  manualSnippets,
  renderSnippet,
  resolveClientSelection,
  upsertCodexToml,
  type ExecFn,
} from '../src/cli/clients.js';
import { MAJOR_VERSION } from '../src/version.js';

const PKG = `srutam-mcp@${MAJOR_VERSION}`;
const LEGACY_KEY = 'srtm_live_0123456789abcdef0123456789abcdef0123456789abcdef';

describe('client table', () => {
  it('has unique ids and never claims a verified format for the ones we could not confirm', () => {
    const ids = CLIENTS.map((c) => c.id);
    expect(new Set(ids).size).toBe(ids.length);
    expect(CLIENTS.filter((c) => c.unverified).map((c) => c.id).sort()).toEqual(['amp', 'antigravity', 'copilot-cli']);
  });

  it('covers the frontier terminal agents', () => {
    for (const id of ['claude-code', 'codex', 'gemini-cli', 'qwen-code', 'crush', 'amp', 'copilot-cli', 'opencode']) {
      expect(CLIENTS.some((c) => c.id === id), id).toBe(true);
    }
  });

  it('every file-based client has a config path inside a directory it can create', () => {
    for (const c of CLIENTS.filter((c) => c.format !== 'claude-cli')) {
      expect(c.configPath, c.id).toBeTypeOf('function');
      expect(path.isAbsolute(c.configPath!()), c.id).toBe(true);
    }
  });
});

describe('selection parsing', () => {
  const detected = (ids: string[]) => (c: { id: string }) => ids.includes(c.id);

  it('accepts numbers, ids, names and aliases, deduplicated', () => {
    const { matched, unknown } = resolveClientSelection('1, codex gemini 2 Claude', CLIENTS, detected([]));
    expect(unknown).toEqual([]);
    expect(matched.map((c) => c.id)).toEqual(['claude-code', 'codex', 'gemini-cli']);
  });

  it('"a" and "all" mean every detected client', () => {
    const r = resolveClientSelection('a', CLIENTS, detected(['cursor', 'zed']));
    expect(r.matched.map((c) => c.id)).toEqual(['cursor', 'zed']);
    expect(resolveClientSelection('all', CLIENTS, detected(['zed'])).matched.map((c) => c.id)).toEqual(['zed']);
  });

  it('reports unknown tokens and out-of-range numbers, and supports manual mode', () => {
    const r = resolveClientSelection('nope 99 0 m', CLIENTS, detected([]));
    expect(r.unknown).toEqual(['nope', '99', '0']);
    expect(r.showManual).toBe(true);
    expect(r.matched).toEqual([]);
  });
});

describe('detection', () => {
  let tmp: string;
  beforeEach(() => {
    tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'srutam-detect-'));
  });
  afterEach(() => fs.rmSync(tmp, { recursive: true, force: true }));

  it('detects by application directory', () => {
    const installed = path.join(tmp, 'app');
    fs.mkdirSync(installed);
    const fake = { ...CLIENTS[0], detectPaths: () => [installed], detectCommand: undefined };
    expect(isClientDetected(fake)).toBe(true);
    expect(isClientDetected({ ...fake, detectPaths: () => [path.join(tmp, 'absent')] })).toBe(false);
  });

  it('detects by an executable on PATH without spawning anything', () => {
    const bin = path.join(tmp, 'bin');
    fs.mkdirSync(bin);
    fs.writeFileSync(path.join(bin, 'mytool'), '#!/bin/sh\n');
    expect(commandOnPath('mytool', { PATH: bin }, 'linux')).toBe(true);
    expect(commandOnPath('other', { PATH: bin }, 'linux')).toBe(false);
    fs.writeFileSync(path.join(bin, 'wintool.exe'), '');
    expect(commandOnPath('wintool', { PATH: bin, PATHEXT: '.EXE;.CMD' }, 'win32')).toBe(true);
  });
});

describe('Codex config.toml', () => {
  let dir: string;
  beforeEach(() => {
    dir = fs.mkdtempSync(path.join(os.tmpdir(), 'srutam-codex-'));
  });
  afterEach(() => fs.rmSync(dir, { recursive: true, force: true }));

  const srutam = (toml: string) => (parseToml(toml) as any).mcp_servers?.srutam;

  it('builds valid TOML with a startup timeout and no key', () => {
    const block = codexTomlBlock('linux');
    expect(srutam(block)).toEqual({ command: 'npx', args: ['-y', PKG, 'serve'], startup_timeout_sec: STARTUP_TIMEOUT_SEC });
    expect(block).not.toMatch(/srtm_live_|SRUTAM_API_KEY/);
    expect(srutam(codexTomlBlock('win32')).command).toBe('cmd');
  });

  it('creates a file from nothing', () => {
    const file = path.join(dir, 'nested', 'config.toml');
    expect(injectCodexToml(file, 'linux')).toBe(true);
    expect(srutam(fs.readFileSync(file, 'utf8')).command).toBe('npx');
  });

  it('appends to an existing file and keeps every comment and table', () => {
    const original = [
      '# my codex config',
      'model = "gpt-5"  # default model',
      '',
      '[mcp_servers.github]',
      'command = "docker"',
      'args = ["run", "-i", "github-mcp"]',
      '',
      '[profiles.fast]',
      'model = "small"',
      '',
    ].join('\n');
    const updated = upsertCodexToml(original, 'linux')!;
    expect(updated.startsWith(original.trimEnd())).toBe(true);
    const doc = parseToml(updated) as any;
    expect(doc.model).toBe('gpt-5');
    expect(doc.mcp_servers.github.command).toBe('docker');
    expect(doc.profiles.fast.model).toBe('small');
    expect(doc.mcp_servers.srutam.args).toEqual(['-y', PKG, 'serve']);
    expect(updated).toContain('# default model');
  });

  it('replaces an existing srutam table in place, keeping foreign keys and sub-tables', () => {
    const original = [
      '[mcp_servers.other]',
      'command = "x"',
      '',
      '[mcp_servers.srutam]  # mine',
      'command = "old-cmd"',
      'args = [',
      '  "old",',
      '  "multi-line",',
      ']',
      'tool_timeout_sec = 120',
      'startup_timeout_sec = 5',
      '',
      '[mcp_servers.srutam.env]',
      'SRUTAM_API_KEY = "kept-because-user-put-it-there"',
      '',
      '[projects.x]',
      'trust = true',
      '',
    ].join('\n');
    const updated = upsertCodexToml(original, 'linux')!;
    const doc = parseToml(updated) as any;
    expect(doc.mcp_servers.srutam.command).toBe('npx');
    expect(doc.mcp_servers.srutam.args).toEqual(['-y', PKG, 'serve']);
    expect(doc.mcp_servers.srutam.startup_timeout_sec).toBe(STARTUP_TIMEOUT_SEC);
    expect(doc.mcp_servers.srutam.tool_timeout_sec).toBe(120);
    expect(doc.mcp_servers.srutam.env.SRUTAM_API_KEY).toBe('kept-because-user-put-it-there');
    expect(doc.mcp_servers.other.command).toBe('x');
    expect(doc.projects.x.trust).toBe(true);
    expect(updated).not.toContain('old-cmd');
    expect(updated).not.toContain('multi-line');
  });

  it('is idempotent', () => {
    const once = upsertCodexToml('model = "x"\n', 'linux')!;
    expect(upsertCodexToml(once, 'linux')).toBe(once);
  });

  it('preserves CRLF line endings', () => {
    const crlf = 'model = "x"\r\n\r\n[mcp_servers.srutam]\r\ncommand = "old"\r\n';
    const updated = upsertCodexToml(crlf, 'linux')!;
    expect(updated).not.toMatch(/[^\r]\n/);
    expect((parseToml(updated) as any).mcp_servers.srutam.command).toBe('npx');
  });

  it('refuses shapes it will not edit blindly and leaves the file untouched', () => {
    for (const odd of ['mcp_servers = { github = { command = "x" } }\n', 'mcp_servers.srutam.command = "old"\n']) {
      const file = path.join(dir, 'odd.toml');
      fs.writeFileSync(file, odd);
      expect(upsertCodexToml(odd, 'linux')).toBeNull();
      expect(injectCodexToml(file, 'linux')).toBe(false);
      expect(fs.readFileSync(file, 'utf8')).toBe(odd);
    }
  });

  it('takes a one-time backup before changing an existing file', () => {
    const file = path.join(dir, 'config.toml');
    fs.writeFileSync(file, 'model = "x"\n');
    injectCodexToml(file, 'linux');
    injectCodexToml(file, 'win32');
    expect(fs.readFileSync(`${file}.srutam.bak`, 'utf8')).toBe('model = "x"\n');
  });
});

describe('JSON formats', () => {
  let dir: string;
  beforeEach(() => {
    dir = fs.mkdtempSync(path.join(os.tmpdir(), 'srutam-json-'));
  });
  afterEach(() => fs.rmSync(dir, { recursive: true, force: true }));
  const read = (f: string) => fs.readFileSync(f, 'utf8');

  it('VS Code uses "servers" with type stdio', () => {
    const f = path.join(dir, 'mcp.json');
    expect(injectVsCodeMcpServer(f, 'linux')).toBe(true);
    expect(JSON.parse(read(f))).toEqual({ servers: { srutam: { type: 'stdio', command: 'npx', args: ['-y', PKG, 'serve'] } } });
  });

  it('VS Code keeps comments and other servers in its JSONC', () => {
    const f = path.join(dir, 'mcp.json');
    fs.writeFileSync(f, '// my servers\n{\n  "servers": {\n    // db\n    "db": { "command": "x" },\n  },\n  "inputs": [],\n}\n');
    expect(injectVsCodeMcpServer(f, 'linux')).toBe(true);
    const text = read(f);
    expect(text).toContain('// my servers');
    expect(text).toContain('// db');
    const doc = parseJsonc(text, [], { allowTrailingComma: true });
    expect(doc.servers.db.command).toBe('x');
    expect(doc.servers.srutam.type).toBe('stdio');
    expect(doc.inputs).toEqual([]);
  });

  it('Crush uses the "mcp" key with type stdio', () => {
    const f = path.join(dir, 'crush.json');
    expect(injectCrushMcpServer(f, 'linux')).toBe(true);
    expect(JSON.parse(read(f)).mcp.srutam).toEqual({ type: 'stdio', command: 'npx', args: ['-y', PKG, 'serve'] });
  });

  it('Amp writes under the literal dotted key "amp.mcpServers"', () => {
    const f = path.join(dir, 'settings.json');
    fs.writeFileSync(f, JSON.stringify({ 'amp.tools.disable': ['x'] }));
    expect(injectAmpMcpServer(f, 'linux')).toBe(true);
    const doc = JSON.parse(read(f));
    expect(doc['amp.mcpServers'].srutam.command).toBe('npx');
    expect(doc['amp.tools.disable']).toEqual(['x']);
  });

  it('never writes a key, and strips one an older wizard embedded', () => {
    const f = path.join(dir, 'mcp.json');
    fs.writeFileSync(f, JSON.stringify({ mcpServers: { srutam: { command: 'npx', env: { SRUTAM_API_KEY: LEGACY_KEY } } } }));
    expect(injectStandardMcpServer(f, 'linux')).toBe(true);
    expect(read(f)).not.toContain(LEGACY_KEY);
  });

  it('leaves unparseable or wrongly-typed files untouched for every JSON format', () => {
    const cases: Array<[string, (f: string) => boolean, string]> = [
      ['vscode-broken', injectVsCodeMcpServer, '{ "servers": '],
      ['crush-wrongtype', injectCrushMcpServer, JSON.stringify({ mcp: [] })],
      ['amp-array-root', injectAmpMcpServer, '[1]'],
    ];
    for (const [name, fn, content] of cases) {
      const f = path.join(dir, `${name}.json`);
      fs.writeFileSync(f, content);
      expect(fn(f), name).toBe(false);
      expect(read(f), name).toBe(content);
    }
  });
});

describe('Claude Code via its own CLI', () => {
  const okExec = (calls: string[][]): ExecFn => (file, args) => {
    calls.push([file, ...args]);
    return { status: 0, stderr: '' };
  };

  it('removes any old entry then adds at user scope, with no shell and no key', () => {
    const calls: string[][] = [];
    const r = addToClaudeCode('linux', okExec(calls));
    expect(r.ok).toBe(true);
    expect(calls[0]).toEqual(['claude', '--version']);
    expect(calls[1]).toEqual(['claude', 'mcp', 'remove', '--scope', 'user', 'srutam']);
    expect(calls[2]).toEqual(['claude', 'mcp', 'add', '--scope', 'user', 'srutam', '--', 'npx', '-y', PKG, 'serve']);
    expect(JSON.stringify(calls)).not.toMatch(/srtm_live_/);
  });

  it('uses the cmd /c launcher on Windows', () => {
    expect(claudeAddCommand('win32').slice(-6)).toEqual(['cmd', '/c', 'npx', '-y', PKG, 'serve']);
  });

  it('falls back to printing the command when claude is not installed', () => {
    const r = addToClaudeCode('linux', () => ({ status: null, stderr: '', error: new Error('ENOENT') }));
    expect(r.ok).toBe(false);
    expect(r.detail).toContain('claude mcp add --scope user srutam -- npx');
  });

  it('reports a failing add without throwing', () => {
    const exec: ExecFn = (_f, args) => (args[1] === 'add' ? { status: 1, stderr: 'boom' } : { status: 0, stderr: '' });
    const r = addToClaudeCode('linux', exec);
    expect(r.ok).toBe(false);
    expect(r.detail).toContain('boom');
  });
});

describe('applyClient', () => {
  it('reports manual-only formats as not applied', () => {
    const r = applyClient({ id: 'x', name: 'X', format: 'manual', detectPaths: () => [] });
    expect(r.ok).toBe(false);
  });
});

describe('snippets and print-config', () => {
  it('every client snippet is valid for its format and key-free', () => {
    for (const c of CLIENTS) {
      const s = renderSnippet(c.format, 'linux');
      expect(s, c.id).not.toMatch(/srtm_live_/);
      if (c.format === 'codex-toml') expect(() => parseToml(s), c.id).not.toThrow();
      else if (c.format !== 'claude-cli') expect(() => JSON.parse(s), c.id).not.toThrow();
      expect(s, c.id).toContain(PKG);
    }
  });

  it('manual snippets cover every format, including the manual-only clients', () => {
    const text = manualSnippets('linux');
    for (const marker of ['mcpServers', 'context_servers', '"servers"', '[mcp_servers.srutam]', 'claude mcp add', 'amp.mcpServers', 'extensions:']) {
      expect(text, marker).toContain(marker);
    }
    expect(text).not.toMatch(/srtm_live_[0-9a-f]{6}/);
  });

  it('finds clients by id, alias, name and the manual-only list', () => {
    expect(findClient('codex').client?.id).toBe('codex');
    expect(findClient('Gemini').client?.id).toBe('gemini-cli');
    expect(findClient('VS Code / GitHub Copilot').client?.id).toBe('vscode');
    expect(findClient('goose').manual?.id).toBe('goose');
    expect(findClient('nothing').client).toBeUndefined();
    expect(MANUAL_CLIENTS.length).toBeGreaterThan(0);
  });

  it('launcher pins the major version and uses serve', () => {
    expect(getLauncher('linux').args).toEqual(['-y', PKG, 'serve']);
  });
});
