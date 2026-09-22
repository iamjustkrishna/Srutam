import readline from 'readline/promises';
import { stdin as input, stdout as output } from 'process';
import fs from 'fs';
import path from 'path';
import os from 'os';
import { saveUserConfig, clearUserConfig, getConfigPath } from '../config.js';
import { SrutamClient } from '../supabase.js';

function getCursorMcpPath(): string {
  return path.join(os.homedir(), '.cursor', 'mcp.json');
}

function getWindsurfMcpPath(): string {
  return path.join(os.homedir(), '.codeium', 'windsurf', 'mcp_config.json');
}

function getOpenCodeMcpPath(): string {
  const localPath = path.join(process.cwd(), 'opencode.json');
  if (fs.existsSync(localPath)) return localPath;
  return path.join(os.homedir(), '.config', 'opencode', 'opencode.json');
}

function getZedSettingsPath(): string {
  if (process.platform === 'win32') {
    const appData = process.env.APPDATA || path.join(os.homedir(), 'AppData', 'Roaming');
    return path.join(appData, 'Zed', 'settings.json');
  }
  return path.join(os.homedir(), '.config', 'zed', 'settings.json');
}

function getClineMcpPath(): string {
  if (process.platform === 'win32') {
    const appData = process.env.APPDATA || path.join(os.homedir(), 'AppData', 'Roaming');
    return path.join(
      appData,
      'Code',
      'User',
      'globalStorage',
      'saoudrizwan.claude-dev',
      'settings',
      'cline_mcp_settings.json'
    );
  } else if (process.platform === 'darwin') {
    return path.join(
      os.homedir(),
      'Library',
      'Application Support',
      'Code',
      'User',
      'globalStorage',
      'saoudrizwan.claude-dev',
      'settings',
      'cline_mcp_settings.json'
    );
  }
  return path.join(
    os.homedir(),
    '.config',
    'Code',
    'User',
    'globalStorage',
    'saoudrizwan.claude-dev',
    'settings',
    'cline_mcp_settings.json'
  );
}

function getAntigravityMcpPath(): string {
  return path.join(os.homedir(), '.gemini', 'config', 'mcp_config.json');
}

function getClaudeDesktopMcpPath(): string {
  if (process.platform === 'win32') {
    const appData = process.env.APPDATA || path.join(os.homedir(), 'AppData', 'Roaming');
    return path.join(appData, 'Claude', 'claude_desktop_config.json');
  } else if (process.platform === 'darwin') {
    return path.join(os.homedir(), 'Library', 'Application Support', 'Claude', 'claude_desktop_config.json');
  }
  return path.join(os.homedir(), '.config', 'Claude', 'claude_desktop_config.json');
}

function injectStandardMcpServer(filePath: string, apiKey: string): boolean {
  try {
    const dir = path.dirname(filePath);
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }

    let config: any = { mcpServers: {} };
    if (fs.existsSync(filePath)) {
      try {
        const content = fs.readFileSync(filePath, 'utf8');
        config = JSON.parse(content);
        if (!config.mcpServers) {
          config.mcpServers = {};
        }
      } catch {
        config = { mcpServers: {} };
      }
    }

    config.mcpServers['srutam'] = {
      command: 'npx',
      args: ['-y', 'srutam-mcp'],
      env: {
        SRUTAM_API_KEY: apiKey,
      },
    };

    fs.writeFileSync(filePath, JSON.stringify(config, null, 2), 'utf8');
    return true;
  } catch (err: any) {
    console.error(`Failed to update ${filePath}: ${err.message}`);
    return false;
  }
}

function injectOpenCodeMcpServer(filePath: string, apiKey: string): boolean {
  try {
    const dir = path.dirname(filePath);
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }

    let config: any = {
      $schema: 'https://opencode.ai/config.json',
      mcp: {},
    };
    if (fs.existsSync(filePath)) {
      try {
        const content = fs.readFileSync(filePath, 'utf8');
        config = JSON.parse(content);
        if (!config.mcp) {
          config.mcp = {};
        }
      } catch {
        config = { $schema: 'https://opencode.ai/config.json', mcp: {} };
      }
    }

    config.mcp['srutam'] = {
      type: 'local',
      command: ['npx', '-y', 'srutam-mcp'],
      environment: {
        SRUTAM_API_KEY: apiKey,
      },
      enabled: true,
    };

    fs.writeFileSync(filePath, JSON.stringify(config, null, 2), 'utf8');
    return true;
  } catch (err: any) {
    console.error(`Failed to update ${filePath}: ${err.message}`);
    return false;
  }
}

function injectZedMcpServer(filePath: string, apiKey: string): boolean {
  try {
    const dir = path.dirname(filePath);
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }

    let config: any = {};
    if (fs.existsSync(filePath)) {
      try {
        const content = fs.readFileSync(filePath, 'utf8');
        config = JSON.parse(content);
      } catch {
        config = {};
      }
    }

    if (!config.context_servers) {
      config.context_servers = {};
    }

    config.context_servers['srutam'] = {
      command: {
        path: 'npx',
        args: ['-y', 'srutam-mcp'],
        env: {
          SRUTAM_API_KEY: apiKey,
        },
      },
    };

    fs.writeFileSync(filePath, JSON.stringify(config, null, 2), 'utf8');
    return true;
  } catch (err: any) {
    console.error(`Failed to update ${filePath}: ${err.message}`);
    return false;
  }
}

export async function runWizard(): Promise<void> {
  const rl = readline.createInterface({ input, output });

  try {
    console.log('\n=======================================================');
    console.log('             Srutam MCP - Developer Setup');
    console.log("   Connect your phone's voice notes to your AI agent");
    console.log('=======================================================\n');

    console.log('To get your API key:');
    console.log('1. Open Srutam app on your phone.');
    console.log('2. Go to Settings -> Srutam Cloud -> New Agent Key.');
    console.log('3. Tap Generate and copy your key.\n');

    let apiKey = '';
    while (!apiKey) {
      const answer = await rl.question('Enter your Srutam API key (starts with srtm_live_): ');
      const trimmed = answer.trim();
      if (!trimmed) {
        console.log('Please enter an API key, or press Ctrl+C to cancel.');
        continue;
      }
      if (!trimmed.startsWith('srtm_live_')) {
        console.log('Warning: Srutam API keys typically start with "srtm_live_".');
      }

      console.log('\nVerifying API key with Srutam Cloud...');
      try {
        const tempClient = new SrutamClient({
          apiKey: trimmed,
          supabaseUrl: 'https://bnahuqxvpbtzaupyumeo.supabase.co',
          supabaseAnonKey:
            'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJuYWh1cXh2cGJ0emF1cHl1bWVvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk1ODM3NDQsImV4cCI6MjEwNTE1OTc0NH0.2GNtlaNyVSYy77FSYQ2mReZuzxbl4XSlZGQ-SJ-p7Ag',
        });
        const userId = await tempClient.authenticate();
        console.log(`Key verified successfully! (User ID: ${userId.slice(0, 8)}...)\n`);
        apiKey = trimmed;
      } catch (err: any) {
        console.log(`Verification failed: ${err.message}`);
        console.log('Please verify the key copied from your phone and try again.\n');
      }
    }

    // Save locally
    saveUserConfig({ apiKey });
    console.log(`Saved credentials to ${getConfigPath()}\n`);

    // Ask for IDE configuration
    console.log('Which AI Coding Assistant do you want to configure?');
    console.log('  [1] Cursor IDE (~/.cursor/mcp.json)');
    console.log('  [2] Windsurf / Codeium (.codeium/windsurf/mcp_config.json)');
    console.log('  [3] OpenCode (opencode.json)');
    console.log('  [4] Claude Desktop (claude_desktop_config.json)');
    console.log('  [5] Antigravity (~/.gemini/config/mcp_config.json)');
    console.log('  [6] Zed Editor (settings.json)');
    console.log('  [7] Cline / Roo Code (VS Code extension)');
    console.log('  [8] All detected assistants (Auto-configure)');
    console.log('  [9] Manual configuration (show snippets)\n');

    const choice = await rl.question('Choose an option (1-9) [default: 1]: ');
    const selected = choice.trim() || '1';

    let configuredAny = false;

    if (selected === '1' || selected === '8') {
      const cursorPath = getCursorMcpPath();
      if (injectStandardMcpServer(cursorPath, apiKey)) {
        console.log(`✓ Configured Cursor IDE in ${cursorPath}`);
        configuredAny = true;
      }
    }

    if (selected === '2' || selected === '8') {
      const windsurfPath = getWindsurfMcpPath();
      if (injectStandardMcpServer(windsurfPath, apiKey)) {
        console.log(`✓ Configured Windsurf in ${windsurfPath}`);
        configuredAny = true;
      }
    }

    if (selected === '3' || selected === '8') {
      const opencodePath = getOpenCodeMcpPath();
      if (injectOpenCodeMcpServer(opencodePath, apiKey)) {
        console.log(`✓ Configured OpenCode in ${opencodePath}`);
        configuredAny = true;
      }
    }

    if (selected === '4' || selected === '8') {
      const claudePath = getClaudeDesktopMcpPath();
      if (injectStandardMcpServer(claudePath, apiKey)) {
        console.log(`✓ Configured Claude Desktop in ${claudePath}`);
        configuredAny = true;
      }
    }

    if (selected === '5' || selected === '8') {
      const antigravityPath = getAntigravityMcpPath();
      if (injectStandardMcpServer(antigravityPath, apiKey)) {
        console.log(`✓ Configured Antigravity in ${antigravityPath}`);
        configuredAny = true;
      }
    }

    if (selected === '6' || selected === '8') {
      const zedPath = getZedSettingsPath();
      if (injectZedMcpServer(zedPath, apiKey)) {
        console.log(`✓ Configured Zed Editor in ${zedPath}`);
        configuredAny = true;
      }
    }

    if (selected === '7' || selected === '8') {
      const clinePath = getClineMcpPath();
      if (injectStandardMcpServer(clinePath, apiKey)) {
        console.log(`✓ Configured Cline / Roo Code in ${clinePath}`);
        configuredAny = true;
      }
    }

    if (selected === '9' || !configuredAny) {
      console.log('\n--- Standard MCP (Cursor / Windsurf / Claude / Antigravity / Cline) ---');
      console.log(
        JSON.stringify(
          {
            mcpServers: {
              srutam: {
                command: 'npx',
                args: ['-y', 'srutam-mcp'],
                env: {
                  SRUTAM_API_KEY: apiKey,
                },
              },
            },
          },
          null,
          2
        )
      );

      console.log('\n--- OpenCode (opencode.json) ---');
      console.log(
        JSON.stringify(
          {
            $schema: 'https://opencode.ai/config.json',
            mcp: {
              srutam: {
                type: 'local',
                command: ['npx', '-y', 'srutam-mcp'],
                environment: {
                  SRUTAM_API_KEY: apiKey,
                },
                enabled: true,
              },
            },
          },
          null,
          2
        )
      );

      console.log('\n--- Zed Editor (settings.json) ---');
      console.log(
        JSON.stringify(
          {
            context_servers: {
              srutam: {
                command: {
                  path: 'npx',
                  args: ['-y', 'srutam-mcp'],
                  env: {
                    SRUTAM_API_KEY: apiKey,
                  },
                },
              },
            },
          },
          null,
          2
        )
      );
    }

    console.log('\nSetup complete!');
    console.log('Restart or refresh MCP servers in your IDE.');
    console.log('Then ask: "List my recent voice notes from Srutam" or "What action items did I speak about?"\n');
  } finally {
    rl.close();
  }
}

export function runLogout(): void {
  clearUserConfig();
  console.log(`Cleared Srutam credentials from ${getConfigPath()}.`);
  console.log('To reconnect anytime, run: npx srutam-mcp init\n');
}
