import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import fs from 'fs';
import path from 'path';
import os from 'os';
import { parse } from 'jsonc-parser';
import {
  getCursorMcpPath,
  getWindsurfMcpPath,
  getOpenCodeMcpPath,
  getZedSettingsPath,
  getClineMcpPath,
  getAntigravityMcpPath,
  getClaudeDesktopMcpPath,
  getCodexConfigPath,
  getGeminiCliPath,
  getKiroMcpPath,
  getLauncher,
  injectStandardMcpServer,
  injectOpenCodeMcpServer,
  injectZedMcpServer,
} from '../src/cli/wizard.js';
import { MAJOR_VERSION } from '../src/version.js';

const LEGACY_KEY = 'srtm_live_0123456789abcdef0123456789abcdef0123456789abcdef';
const PKG = `srutam-mcp@${MAJOR_VERSION}`;

describe('Wizard Path Resolvers', () => {
  it('resolves the standard config file names', () => {
    expect(getCursorMcpPath()).toContain('.cursor');
    expect(getCursorMcpPath().endsWith('mcp.json')).toBe(true);
    expect(getWindsurfMcpPath()).toContain('windsurf');
    expect(getZedSettingsPath().endsWith('settings.json')).toBe(true);
    expect(getClaudeDesktopMcpPath().endsWith('claude_desktop_config.json')).toBe(true);
    expect(getAntigravityMcpPath().endsWith('mcp_config.json')).toBe(true);
    expect(getClineMcpPath().endsWith('cline_mcp_settings.json')).toBe(true);
    expect(getGeminiCliPath().endsWith(path.join('.gemini', 'settings.json'))).toBe(true);
  });

  it('resolves OpenCode to the user-level config, never the working directory', () => {
    const p = getOpenCodeMcpPath();
    expect(p.endsWith('opencode.json')).toBe(true);
    expect(p.startsWith(os.homedir())).toBe(true);
    expect(p.startsWith(process.cwd())).toBe(false);
  });

  it('honours CODEX_HOME and KIRO_HOME', () => {
    const saved = { c: process.env.CODEX_HOME, k: process.env.KIRO_HOME };
    try {
      process.env.CODEX_HOME = path.join(os.tmpdir(), 'codex-home');
      process.env.KIRO_HOME = path.join(os.tmpdir(), 'kiro-home');
      expect(getCodexConfigPath()).toBe(path.join(os.tmpdir(), 'codex-home', 'config.toml'));
      expect(getKiroMcpPath()).toBe(path.join(os.tmpdir(), 'kiro-home', 'settings', 'mcp.json'));
    } finally {
      if (saved.c === undefined) delete process.env.CODEX_HOME; else process.env.CODEX_HOME = saved.c;
      if (saved.k === undefined) delete process.env.KIRO_HOME; else process.env.KIRO_HOME = saved.k;
    }
  });
});

describe('Launch command', () => {
  it('uses npx with an explicit serve argument and a pinned major version', () => {
    expect(getLauncher('linux')).toEqual({ command: 'npx', args: ['-y', PKG, 'serve'] });
    expect(getLauncher('darwin')).toEqual({ command: 'npx', args: ['-y', PKG, 'serve'] });
  });

  it('goes through cmd.exe on Windows, where npx is a .cmd shim', () => {
    expect(getLauncher('win32')).toEqual({ command: 'cmd', args: ['/c', 'npx', '-y', PKG, 'serve'] });
  });
});

describe('Wizard JSON Configuration Injectors', () => {
  let tmpDir: string;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'srutam-wizard-test-'));
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  const read = (file: string) => fs.readFileSync(file, 'utf8');

  it('injects standard mcpServers configuration without any API key', () => {
    const targetFile = path.join(tmpDir, 'mcp.json');
    expect(injectStandardMcpServer(targetFile, 'linux')).toBe(true);

    const content = JSON.parse(read(targetFile));
    expect(content.mcpServers.srutam).toEqual({ command: 'npx', args: ['-y', PKG, 'serve'] });
    expect(read(targetFile)).not.toMatch(/srtm_live_|SRUTAM_API_KEY|env/);
  });

  it('writes the Windows launcher when asked to', () => {
    const targetFile = path.join(tmpDir, 'mcp.json');
    injectStandardMcpServer(targetFile, 'win32');
    expect(JSON.parse(read(targetFile)).mcpServers.srutam.command).toBe('cmd');
  });

  it('preserves existing servers and other settings', () => {
    const targetFile = path.join(tmpDir, 'mcp.json');
    fs.writeFileSync(targetFile, JSON.stringify({ mcpServers: { existing_tool: { command: 'existing-cmd' } }, other: 1 }, null, 2));

    expect(injectStandardMcpServer(targetFile, 'linux')).toBe(true);

    const content = JSON.parse(read(targetFile));
    expect(content.mcpServers.existing_tool).toEqual({ command: 'existing-cmd' });
    expect(content.mcpServers.srutam).toBeDefined();
    expect(content.other).toBe(1);
  });

  it('strips an API key that an older wizard version embedded in the entry', () => {
    const targetFile = path.join(tmpDir, 'mcp.json');
    fs.writeFileSync(
      targetFile,
      JSON.stringify({ mcpServers: { srutam: { command: 'npx', args: ['-y', 'srutam-mcp'], env: { SRUTAM_API_KEY: LEGACY_KEY } } } }, null, 2)
    );

    expect(injectStandardMcpServer(targetFile, 'linux')).toBe(true);
    expect(read(targetFile)).not.toContain(LEGACY_KEY);
    expect(JSON.parse(read(targetFile)).mcpServers.srutam.env).toBeUndefined();
  });

  it('injects OpenCode schema and mcp dictionary format', () => {
    const targetFile = path.join(tmpDir, 'opencode.json');
    expect(injectOpenCodeMcpServer(targetFile, 'linux')).toBe(true);

    const content = JSON.parse(read(targetFile));
    expect(content.$schema).toBe('https://opencode.ai/config.json');
    expect(content.mcp.srutam).toEqual({ type: 'local', command: ['npx', '-y', PKG, 'serve'], enabled: true });
  });

  it('injects Zed context_servers configuration', () => {
    const targetFile = path.join(tmpDir, 'settings.json');
    expect(injectZedMcpServer(targetFile, 'linux')).toBe(true);

    const content = JSON.parse(read(targetFile));
    expect(content.context_servers.srutam).toEqual({ command: { path: 'npx', args: ['-y', PKG, 'serve'] } });
  });

  it('creates missing parent directories and treats an empty file as fresh', () => {
    const nested = path.join(tmpDir, 'deep', 'nested', 'mcp.json');
    expect(injectStandardMcpServer(nested, 'linux')).toBe(true);
    const blank = path.join(tmpDir, 'blank.json');
    fs.writeFileSync(blank, '   \n');
    expect(injectStandardMcpServer(blank, 'linux')).toBe(true);
    expect(fs.existsSync(`${blank}.srutam.bak`)).toBe(false);
  });

  describe('never destroys a config it cannot understand', () => {
    it('keeps a Zed settings.json with comments and trailing commas intact', () => {
      const targetFile = path.join(tmpDir, 'settings.json');
      fs.writeFileSync(
        targetFile,
        ['// Zed settings', '{', '  // my theme', '  "theme": "One Dark",', '  "buffer_font_size": 15, // bigger', '  "vim_mode": true,', '}', ''].join('\n')
      );

      expect(injectZedMcpServer(targetFile, 'linux')).toBe(true);

      const after = read(targetFile);
      expect(after).toContain('// Zed settings');
      expect(after).toContain('// my theme');
      expect(after).toContain('// bigger');
      const parsed = parse(after, [], { allowTrailingComma: true });
      expect(parsed.theme).toBe('One Dark');
      expect(parsed.vim_mode).toBe(true);
      expect(parsed.context_servers.srutam.command.path).toBe('npx');
    });

    it('leaves an unparseable file byte-for-byte untouched and reports failure', () => {
      const targetFile = path.join(tmpDir, 'mcp.json');
      const broken = '{ "mcpServers": { "other": { "command": "x" } ';
      fs.writeFileSync(targetFile, broken);

      expect(injectStandardMcpServer(targetFile, 'linux')).toBe(false);
      expect(read(targetFile)).toBe(broken);
      expect(fs.readdirSync(tmpDir)).toEqual(['mcp.json']);
    });

    it('leaves wrongly-typed and non-object files untouched', () => {
      const a = path.join(tmpDir, 'a.json');
      const odd = JSON.stringify({ mcpServers: ['not', 'an', 'object'] });
      fs.writeFileSync(a, odd);
      expect(injectStandardMcpServer(a, 'linux')).toBe(false);
      expect(read(a)).toBe(odd);

      const b = path.join(tmpDir, 'b.json');
      fs.writeFileSync(b, '[1, 2, 3]');
      expect(injectStandardMcpServer(b, 'linux')).toBe(false);
      expect(read(b)).toBe('[1, 2, 3]');
    });

    it('takes a one-time backup before first modifying an existing file', () => {
      const targetFile = path.join(tmpDir, 'mcp.json');
      const original = JSON.stringify({ mcpServers: { a: { command: 'a' } } }, null, 2);
      fs.writeFileSync(targetFile, original);

      injectStandardMcpServer(targetFile, 'linux');
      expect(read(`${targetFile}.srutam.bak`)).toBe(original);

      injectStandardMcpServer(targetFile, 'win32');
      expect(read(`${targetFile}.srutam.bak`)).toBe(original);
    });
  });
});
