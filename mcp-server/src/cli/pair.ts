import {
  PairingClient,
  PairStatus,
  describeThisMachine,
  formatCode,
  generateApiKey,
  keyPrefixOf,
  pairingUri,
} from '../pairing.js';
import { renderQr } from './qr.js';
import { VERSION } from '../version.js';

/**
 * The pairing state machine.
 *
 * Split from the terminal I/O so it can be driven by fakes in tests: every side effect
 * (clock, sleep, output, the [Y/n] confirmation) arrives through PairingDeps.
 */

export type PairingOutcome =
  | { kind: 'paired'; apiKey: string; keyName: string | null; accountHint: string | null }
  | { kind: 'declined' }
  | { kind: 'expired' }
  | { kind: 'cancelled' }
  | { kind: 'error'; message: string };

export interface PairingDeps {
  client: PairingClient;
  now(): number;
  sleep(ms: number): Promise<void>;
  write(line: string): void;
  /** The second confirmation: which account approved, answered on THIS machine. */
  confirmAccount(info: { accountHint: string | null; keyName: string | null }): Promise<boolean>;
  showQr: boolean;
  /** Resolves when the user asks to abort (Ctrl+C, [q], or [p] to paste instead). */
  abort?: { aborted: boolean };
}

export const POLL_INTERVAL_MS = 2000;

/** Spread polls slightly so many clients cannot synchronise into a thundering herd. */
function jitter(baseMs: number): number {
  return baseMs + Math.floor(Math.random() * 400) - 200;
}

export function renderPairingScreen(code: string, showQr: boolean): string {
  const uri = pairingUri(code);
  const lines = [
    '',
    '  Connect this computer to Srutam',
    '',
    '  1. Open Srutam on your phone',
    '  2. Settings > Cloud Sync & Developer Brain (MCP) > Connect a computer',
    '  3. Point the camera at this code',
    '',
  ];
  if (showQr) {
    lines.push(
      renderQr(uri)
        .split('\n')
        .map((l) => `    ${l}`)
        .join('\n'),
      ''
    );
  }
  lines.push(`  Or type this code in the app:   ${formatCode(code)}`, '');
  return lines.join('\n');
}

/**
 * Runs one pairing attempt: create a key, publish its hash, wait for the phone, then confirm
 * on this machine before the key is handed back to the caller to save.
 */
export async function pairComputer(
  deps: PairingDeps,
  options: { replacesKeyHash?: string | null } = {}
): Promise<PairingOutcome> {
  const { apiKey, keyHash } = generateApiKey();
  const machine = describeThisMachine();

  let start;
  try {
    start = await deps.client.start({
      keyHash,
      keyPrefix: keyPrefixOf(apiKey),
      label: machine.label,
      platform: machine.platform,
      clientVersion: VERSION,
      replacesKeyHash: options.replacesKeyHash ?? null,
    });
  } catch (err: any) {
    return { kind: 'error', message: err?.message ?? 'could not start pairing' };
  }

  deps.write(renderPairingScreen(start.code, deps.showQr));

  const expiresAt = Date.parse(start.expiresAt);
  let consecutiveNetworkErrors = 0;

  while (true) {
    if (deps.abort?.aborted) {
      await deps.client.cancel(keyHash).catch(() => undefined);
      return { kind: 'cancelled' };
    }

    // The server owns expiry; this only decides when to stop polling.
    if (Number.isFinite(expiresAt) && deps.now() > expiresAt) {
      await deps.client.cancel(keyHash).catch(() => undefined);
      return { kind: 'expired' };
    }

    await deps.sleep(jitter(POLL_INTERVAL_MS));

    let status: PairStatus;
    try {
      status = await deps.client.status(keyHash);
      consecutiveNetworkErrors = 0;
    } catch {
      // A blip mid-pairing must not lose the session; keep polling until the code expires.
      if (++consecutiveNetworkErrors >= 10) {
        return { kind: 'error', message: 'Lost connection to Srutam Cloud. Please try again.' };
      }
      continue;
    }

    if (status.status === 'approved') {
      const accountHint = status.accountHint ?? null;
      const keyName = status.keyName ?? null;

      // The defence against a relayed/photographed QR: the person at this keyboard must
      // recognise the account that approved before the key is ever used.
      const accepted = await deps.confirmAccount({ accountHint, keyName });
      if (!accepted) {
        await deps.client.revokeSelf(keyHash).catch(() => undefined);
        return { kind: 'declined' };
      }
      return { kind: 'paired', apiKey, keyName, accountHint };
    }

    if (status.status === 'expired' || status.status === 'cancelled') {
      return { kind: 'expired' };
    }
    if (status.status === 'unknown') {
      return { kind: 'error', message: 'The pairing request disappeared. Please try again.' };
    }
  }
}
