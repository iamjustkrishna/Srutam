import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import fs from 'fs';
import path from 'path';
import os from 'os';
import {
  getCursorMcpPath,
  getWindsurfMcpPath,
  getOpenCodeMcpPath,
  getZedSettingsPath,
  getClineMcpPath,
  getAntigravityMcpPath,
  getClaudeDesktopMcpPath,
  injectStandardMcpServer,
  injectOpenCodeMcpServer,
  injectZedMcpServer,
} from '../src/cli/wizard.js';

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

  it('resolves OpenCode MCP path', () => {
    const p = getOpenCodeMcpPath();
    expect(p.endsWith('opencode.json')).toBe(true);
  });

  it('resolves Zed settings path', () => {
    const p = getZedSettingsPath();
    expect(p.endsWith('settings.json')).toBe(true);
  });

  it('resolves Claude Desktop MCP path', () => {
    const p = getClaudeDesktopMcpPath();
    expect(p.endsWith('claude_desktop_config.json')).toBe(true);
  });

  it('resolves Antigravity MCP config path', () => {
    const p = getAntigravityMcpPath();
    expect(p.endsWith('mcp_config.json')).toBe(true);
  });

  it('resolves Cline MCP path', () => {
    const p = getClineMcpPath();
    expect(p.endsWith('cline_mcp_settings.json')).toBe(true);
  });
});

describe('Wizard JSON Configuration Injectors', () => {
  let tmpDir: string;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'srutam-wizard-test-'));
  });

  afterEach(() => {
    try {
      fs.rmSync(tmpDir, { recursive: true, force: true });
    } catch {
      // ignore
    }
  });

  it('injects standard mcpServers configuration for Cursor / Claude', () => {
    const targetFile = path.join(tmpDir, 'mcp.json');
    const success = injectStandardMcpServer(targetFile, 'srtm_live_key_123');
    expect(success).toBe(true);

    const content = JSON.parse(fs.readFileSync(targetFile, 'utf8'));
    expect(content.mcpServers).toBeDefined();
    expect(content.mcpServers.srutam).toBeDefined();
    expect(content.mcpServers.srutam.command).toBe('npx');
    expect(content.mcpServers.srutam.args).toEqual(['-y', 'srutam-mcp']);
    expect(content.mcpServers.srutam.env.SRUTAM_API_KEY).toBe('srtm_live_key_123');
  });

  it('preserves existing servers in standard mcpServers configuration', () => {
    const targetFile = path.join(tmpDir, 'mcp.json');
    const existing = {
      mcpServers: {
        existing_tool: { command: 'existing-cmd' },
      },
    };
    fs.writeFileSync(targetFile, JSON.stringify(existing, null, 2), 'utf8');

    injectStandardMcpServer(targetFile, 'srtm_live_key_456');

    const content = JSON.parse(fs.readFileSync(targetFile, 'utf8'));
    expect(content.mcpServers.existing_tool).toBeDefined();
    expect(content.mcpServers.srutam).toBeDefined();
    expect(content.mcpServers.srutam.env.SRUTAM_API_KEY).toBe('srtm_live_key_456');
  });

  it('injects OpenCode schema and mcp dictionary format', () => {
    const targetFile = path.join(tmpDir, 'opencode.json');
    const success = injectOpenCodeMcpServer(targetFile, 'srtm_live_key_opencode');
    expect(success).toBe(true);

    const content = JSON.parse(fs.readFileSync(targetFile, 'utf8'));
    expect(content.$schema).toBe('https://opencode.ai/config.json');
    expect(content.mcp.srutam).toBeDefined();
    expect(content.mcp.srutam.type).toBe('local');
    expect(content.mcp.srutam.command).toEqual(['npx', '-y', 'srutam-mcp']);
    expect(content.mcp.srutam.environment.SRUTAM_API_KEY).toBe('srtm_live_key_opencode');
    expect(content.mcp.srutam.enabled).toBe(true);
  });

  it('injects Zed context_servers configuration', () => {
    const targetFile = path.join(tmpDir, 'settings.json');
    const success = injectZedMcpServer(targetFile, 'srtm_live_key_zed');
    expect(success).toBe(true);

    const content = JSON.parse(fs.readFileSync(targetFile, 'utf8'));
    expect(content.context_servers.srutam).toBeDefined();
    expect(content.context_servers.srutam.command.path).toBe('npx');
    expect(content.context_servers.srutam.command.args).toEqual(['-y', 'srutam-mcp']);
    expect(content.context_servers.srutam.command.env.SRUTAM_API_KEY).toBe('srtm_live_key_zed');
  });
});
