import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import fs from 'fs';
import path from 'path';
import os from 'os';
import { parse } from 'jsonc-parser';
import {
  CLIENTS,
  getCursorMcpPath,
  getWindsurfMcpPath,
  getOpenCodeMcpPath,
  getZedSettingsPath,
  getClineMcpPath,
  getAntigravityMcpPath,
  getClaudeDesktopMcpPath,
  getLauncher,
  injectStandardMcpServer,
  injectOpenCodeMcpServer,
  injectZedMcpServer,
  isClientDetected,
  manualSnippets,
} from '../src/cli/wizard.js';
import { MAJOR_VERSION } from '../src/version.js';

const LEGACY_KEY = 'srtm_live_0123456789abcdef0123456789abcdef0123456789abcdef';
const PKG_SPEC = `srutam-mcp@${MAJOR_VERSION}`;

describe('Wizard Path Resolvers', () => {
  it('resolves Cursor MCP path with mcp.json', () => {
    const p = getCursorMcpPath();
    expect(p).toContain('.cursor');
    expect(p.endsWith('mcp.json')).toBe(true);
  });

  it('resolves Windsurf MCP path with mcp_config.json', () => {
    const p = getWindsurfMcpPath();
    expect(p).toContain('windsurf');
    expect(p.endsWith('mcp_config.json')).toBe(true);
  });

  it('resolves OpenCode to the user-level config, never the working directory', () => {
    const p = getOpenCodeMcpPath();
    expect(p.endsWith('opencode.json')).toBe(true);
    expect(p.startsWith(os.homedir())).toBe(true);
    expect(p.startsWith(process.cwd())).toBe(false);
  });

  it('resolves Zed settings path', () => {
    expect(getZedSettingsPath().endsWith('settings.json')).toBe(true);
  });

  it('resolves Claude Desktop MCP path', () => {
    expect(getClaudeDesktopMcpPath().endsWith('claude_desktop_config.json')).toBe(true);
  });

  it('resolves Antigravity MCP config path', () => {
    expect(getAntigravityMcpPath().endsWith('mcp_config.json')).toBe(true);
  });

  it('resolves Cline MCP path', () => {
    expect(getClineMcpPath().endsWith('cline_mcp_settings.json')).toBe(true);
  });
});

describe('Launch command', () => {
  it('uses npx with an explicit serve argument and a pinned major version', () => {
    expect(getLauncher('linux')).toEqual({ command: 'npx', args: ['-y', PKG_SPEC, 'serve'] });
    expect(getLauncher('darwin')).toEqual({ command: 'npx', args: ['-y', PKG_SPEC, 'serve'] });
  });

  it('goes through cmd.exe on Windows, where npx is a .cmd shim', () => {
    expect(getLauncher('win32')).toEqual({ command: 'cmd', args: ['/c', 'npx', '-y', PKG_SPEC, 'serve'] });
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
    expect(content.mcpServers.srutam).toEqual({ command: 'npx', args: ['-y', PKG_SPEC, 'serve'] });
    expect(read(targetFile)).not.toMatch(/srtm_live_|SRUTAM_API_KEY|env/);
  });

  it('writes the Windows launcher when asked to', () => {
    const targetFile = path.join(tmpDir, 'mcp.json');
    injectStandardMcpServer(targetFile, 'win32');
    expect(JSON.parse(read(targetFile)).mcpServers.srutam.command).toBe('cmd');
  });

  it('preserves existing servers in standard mcpServers configuration', () => {
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
    expect(content.mcp.srutam).toEqual({ type: 'local', command: ['npx', '-y', PKG_SPEC, 'serve'], enabled: true });
    expect(read(targetFile)).not.toMatch(/srtm_live_|SRUTAM_API_KEY/);
  });

  it('injects Zed context_servers configuration', () => {
    const targetFile = path.join(tmpDir, 'settings.json');
    expect(injectZedMcpServer(targetFile, 'linux')).toBe(true);

    const content = JSON.parse(read(targetFile));
    expect(content.context_servers.srutam).toEqual({ command: { path: 'npx', args: ['-y', PKG_SPEC, 'serve'] } });
    expect(read(targetFile)).not.toMatch(/srtm_live_|SRUTAM_API_KEY/);
  });

  it('creates missing parent directories for a first-time config', () => {
    const targetFile = path.join(tmpDir, 'deep', 'nested', 'mcp.json');
    expect(injectStandardMcpServer(targetFile, 'linux')).toBe(true);
    expect(fs.existsSync(targetFile)).toBe(true);
  });

  it('treats an empty file as a fresh document', () => {
    const targetFile = path.join(tmpDir, 'mcp.json');
    fs.writeFileSync(targetFile, '   \n');
    expect(injectStandardMcpServer(targetFile, 'linux')).toBe(true);
    expect(JSON.parse(read(targetFile)).mcpServers.srutam).toBeDefined();
    expect(fs.existsSync(`${targetFile}.srutam.bak`)).toBe(false);
  });

  describe('never destroys a config it cannot understand', () => {
    it('keeps a Zed settings.json with comments and trailing commas intact', () => {
      const targetFile = path.join(tmpDir, 'settings.json');
      const original = [
        '// Zed settings',
        '{',
        '  // my theme',
        '  "theme": "One Dark",',
        '  "buffer_font_size": 15, // bigger',
        '  "vim_mode": true,',
        '}',
        '',
      ].join('\n');
      fs.writeFileSync(targetFile, original);

      expect(injectZedMcpServer(targetFile, 'linux')).toBe(true);

      const after = read(targetFile);
      expect(after).toContain('// Zed settings');
      expect(after).toContain('// my theme');
      expect(after).toContain('// bigger');
      const parsed = parse(after, [], { allowTrailingComma: true });
      expect(parsed.theme).toBe('One Dark');
      expect(parsed.buffer_font_size).toBe(15);
      expect(parsed.vim_mode).toBe(true);
      expect(parsed.context_servers.srutam.command.path).toBe('npx');
    });

    it('leaves an unparseable file byte-for-byte untouched and reports failure', () => {
      const targetFile = path.join(tmpDir, 'mcp.json');
      const broken = '{ "mcpServers": { "other": { "command": "x" } ';
      fs.writeFileSync(targetFile, broken);

      expect(injectStandardMcpServer(targetFile, 'linux')).toBe(false);

      expect(read(targetFile)).toBe(broken);
      expect(fs.readdirSync(tmpDir)).toEqual(['mcp.json']); // no temp or backup litter
    });

    it('leaves a file untouched when the container key has the wrong type', () => {
      const targetFile = path.join(tmpDir, 'mcp.json');
      const odd = JSON.stringify({ mcpServers: ['not', 'an', 'object'] });
      fs.writeFileSync(targetFile, odd);

      expect(injectStandardMcpServer(targetFile, 'linux')).toBe(false);
      expect(read(targetFile)).toBe(odd);
    });

    it('leaves a non-object JSON root untouched', () => {
      const targetFile = path.join(tmpDir, 'mcp.json');
      fs.writeFileSync(targetFile, '[1, 2, 3]');
      expect(injectStandardMcpServer(targetFile, 'linux')).toBe(false);
      expect(read(targetFile)).toBe('[1, 2, 3]');
    });

    it('takes a one-time backup before first modifying an existing file', () => {
      const targetFile = path.join(tmpDir, 'mcp.json');
      const original = JSON.stringify({ mcpServers: { a: { command: 'a' } } }, null, 2);
      fs.writeFileSync(targetFile, original);

      injectStandardMcpServer(targetFile, 'linux');
      expect(read(`${targetFile}.srutam.bak`)).toBe(original);

      injectStandardMcpServer(targetFile, 'win32'); // second run must not overwrite the pristine backup
      expect(read(`${targetFile}.srutam.bak`)).toBe(original);
    });
  });
});

describe('Client detection ("All detected assistants")', () => {
  it('only reports clients whose application directory already exists', () => {
    const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'srutam-detect-'));
    try {
      const installed = path.join(tmp, 'installed');
      fs.mkdirSync(installed);
      const fake = (dir: string) => ({ ...CLIENTS[0], detectDir: () => dir });

      expect(isClientDetected(fake(installed))).toBe(true);
      expect(isClientDetected(fake(path.join(tmp, 'absent')))).toBe(false);
    } finally {
      fs.rmSync(tmp, { recursive: true, force: true });
    }
  });

  it('covers all seven menu entries with unique choices 1-7', () => {
    expect(CLIENTS.map((c) => c.choice)).toEqual(['1', '2', '3', '4', '5', '6', '7']);
  });
});

describe('Manual snippets', () => {
  it('contain launch commands but no key material', () => {
    const text = manualSnippets('linux');
    expect(text).toContain('"serve"');
    expect(text).toContain('mcpServers');
    expect(text).toContain('context_servers');
    expect(text).not.toMatch(/srtm_live_[0-9a-f]{6}/);
  });
});
