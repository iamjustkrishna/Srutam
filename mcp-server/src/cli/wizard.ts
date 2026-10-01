import readline from 'readline/promises';
import { Writable } from 'stream';
import { stdin as input, stdout as output } from 'process';
import {
  DEFAULTS,
  getConfigPath,
  isStandardSupabaseUrl,
  loadConfig,
  resolveEndpoint,
  saveUserConfig,
  clearUserConfig,
} from '../config.js';
import { SrutamClient } from '../supabase.js';
import {
  CLIENTS,
  applyClient,
  isClientDetected,
  manualSnippets,
  resolveClientSelection,
  type ClientTarget,
} from './clients.js';

// Everything client-specific (paths, writers, launcher, snippets) lives in clients.ts.
export * from './clients.js';

export interface InitOptions {
  /** Comma-separated client ids/names/numbers, or "all" for every detected client. Skips the menu. */
  clients?: string;
  /** Non-interactive: reuse the saved / SRUTAM_API_KEY key and never prompt. */
  yes?: boolean;
}

const MAX_KEY_ATTEMPTS = 3;

function report(client: ClientTarget, result: { ok: boolean; detail: string }): boolean {
  if (result.ok) {
    console.log(`✓ Configured ${client.name}${result.detail ? ` (${result.detail})` : ''}`);
    if (client.unverified) {
      console.log(`  Note: the ${client.name} config format is not verified against the real app. If it does not load, see "srutam-mcp print-config ${client.id}".`);
    }
    return true;
  }
  if (result.detail && result.detail !== 'manual configuration only') {
    console.log(`✕ ${client.name}: ${result.detail}`);
  }
  return false;
}

export async function runWizard(options: InitOptions = {}): Promise<void> {
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

  const verify = async (key: string): Promise<boolean> => {
    console.log('\nVerifying API key with Srutam Cloud...');
    try {
      const status = await new SrutamClient({ apiKey: key, ...endpoint }).getCloudStatus();
      console.log(`Key verified successfully! (account ${status.userId.slice(0, 8)}…, ${status.noteCount} note(s) visible to agents)\n`);
      return true;
    } catch (err: any) {
      console.log(`Verification failed: ${err.message}`);
      return false;
    }
  };

  try {
    console.log('\n=======================================================');
    console.log('             Srutam MCP - Developer Setup');
    console.log("   Connect your phone's voice notes to your AI agent");
    console.log('=======================================================\n');

    // ---- 1. API key: reuse the saved one, or ask for a new one --------------------------------
    let existing: string | undefined;
    try {
      existing = loadConfig()?.apiKey;
    } catch {
      existing = undefined;
    }

    let apiKey = '';
    let keyAlreadySaved = false;

    if (existing) {
      const preview = `${existing.slice(0, 16)}...`;
      const reuse = options.yes || ((await ask(`Use the saved key ${preview}? [Y/n]: `)).trim().toLowerCase() !== 'n');
      if (reuse && (await verify(existing))) {
        apiKey = existing;
        keyAlreadySaved = true;
      } else if (options.yes) {
        console.log('The saved key was rejected. Run "srutam-mcp init" without --yes to enter a new one.\n');
        process.exitCode = 1;
        return;
      }
    } else if (options.yes) {
      console.log('No saved key. Run "srutam-mcp init" once interactively, or set SRUTAM_API_KEY.\n');
      process.exitCode = 1;
      return;
    }

    if (!apiKey) {
      console.log('To get your API key:');
      console.log('1. Open Srutam app on your phone.');
      console.log('2. Go to Settings -> Cloud Sync & Developer Brain (MCP) -> MCP Agent Keys -> New Key.');
      console.log('3. Tap Generate and copy your key.\n');

      for (let attempt = 1; attempt <= MAX_KEY_ATTEMPTS && !apiKey; attempt++) {
        const trimmed = (await askSecret('Enter your Srutam API key (input is hidden): ')).trim();
        if (!trimmed) {
          console.log('No key entered.\n');
          continue;
        }
        if (!trimmed.startsWith('srtm_live_')) {
          console.log('Warning: Srutam API keys typically start with "srtm_live_".');
        }
        if (await verify(trimmed)) {
          apiKey = trimmed;
        } else if (attempt < MAX_KEY_ATTEMPTS) {
          console.log('Please check the key copied from your phone and try again.\n');
        }
      }

      if (!apiKey) {
        console.log(`\nSetup aborted: no valid API key after ${MAX_KEY_ATTEMPTS} attempts.\n`);
        process.exitCode = 1;
        return;
      }
    }

    if (!keyAlreadySaved) {
      // The only place the key is stored (0600 on POSIX; user-profile ACL on Windows).
      saveUserConfig({
        apiKey,
        ...(endpoint.supabaseUrl !== DEFAULTS.supabaseUrl
          ? { supabaseUrl: endpoint.supabaseUrl, allowCustomUrl: !isStandardSupabaseUrl(endpoint.supabaseUrl) }
          : {}),
        ...(endpoint.supabaseAnonKey !== DEFAULTS.supabaseAnonKey ? { supabaseAnonKey: endpoint.supabaseAnonKey } : {}),
      });
      console.log(`Saved credentials to ${getConfigPath()}\n`);
    }

    // ---- 2. Choose clients -----------------------------------------------------------------------
    const detected = new Set(CLIENTS.filter((c) => isClientDetected(c)).map((c) => c.id));
    let selection = options.clients;

    if (selection === undefined) {
      console.log('Which AI coding assistants do you want to configure?\n');
      const line = (c: ClientTarget) =>
        `  [${String(CLIENTS.indexOf(c) + 1).padStart(2)}] ${c.name}${c.unverified ? '  (unverified format)' : ''}`;
      const found = CLIENTS.filter((c) => detected.has(c.id));
      const others = CLIENTS.filter((c) => !detected.has(c.id));
      if (found.length > 0) {
        console.log('  Detected on this machine:');
        found.forEach((c) => console.log(line(c)));
        console.log('');
      }
      console.log(found.length > 0 ? '  Other supported clients:' : '  Supported clients:');
      others.forEach((c) => console.log(line(c)));
      console.log('\n  [a] all detected   [m] show manual snippets   (comma-separate several: 1,3,5 or codex,claude-code)\n');

      const defaultChoice = found.length > 0 ? 'a' : 'm';
      selection = (await ask(`Choose [default: ${defaultChoice}]: `)).trim() || defaultChoice;
    }

    const { matched, unknown, showManual } = resolveClientSelection(selection, CLIENTS, (c) => detected.has(c.id));
    if (unknown.length > 0) {
      console.log(`Unknown client(s): ${unknown.join(', ')}. Run "srutam-mcp print-config" to see the supported names.`);
    }
    if (matched.length === 0 && !showManual && (selection === 'a' || /^all/i.test(selection))) {
      console.log('No supported assistants were detected on this machine.');
    }

    let configuredAny = false;
    for (const client of matched) {
      if (report(client, applyClient(client))) configuredAny = true;
    }

    if (showManual || (!configuredAny && options.clients === undefined)) {
      console.log('\n' + manualSnippets());
    }

    console.log('\nSetup complete!');
    if (configuredAny) {
      console.log(`Your API key is stored only in ${getConfigPath()}. The client config files contain no secret.`);
    }
    console.log('Restart or refresh MCP servers in your assistant.');
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
  console.log('If you ever pasted the key into a client config manually, remove it there too and revoke the key in the app.');
  console.log('To reconnect anytime, run: npx srutam-mcp init\n');
}
