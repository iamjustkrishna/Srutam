import { PairingClient } from '../pairing.js';
import { pairComputer, type PairingOutcome } from './pair.js';
import { canShowQr } from './qr.js';

/**
 * Terminal front-end for QR pairing.
 *
 * Everything here is line-based (readline), not raw keypresses: the wizard already owns stdin
 * through a line queue, and line input is what keeps `echo ... | srutam-mcp init` and the tests
 * working. So instead of hot keys during the wait, the user is offered a choice before the code
 * is shown and again whenever one expires.
 */

export type PairPromptResult =
  | { kind: 'paired'; apiKey: string; keyName: string | null; accountHint: string | null }
  | { kind: 'paste' }
  | { kind: 'aborted'; message: string };

export interface PairPromptDeps {
  endpoint: { supabaseUrl: string; supabaseAnonKey: string };
  ask(question: string): Promise<string>;
  askSecret(question: string): Promise<string>;
  /** Test seam. */
  client?: PairingClient;
  sleep?(ms: number): Promise<void>;
  showQr?: boolean;
}

const MAX_CODES = 3;

const yes = (answer: string): boolean => answer.trim().toLowerCase() !== 'n';

export async function pairInteractively(deps: PairPromptDeps): Promise<PairPromptResult> {
  const client = deps.client ?? new PairingClient(deps.endpoint);
  const sleep = deps.sleep ?? ((ms: number) => new Promise<void>((r) => setTimeout(r, ms)));
  const showQr = deps.showQr ?? canShowQr();

  console.log('To connect this computer, Srutam shows a code here and you scan it with your phone.');
  console.log('(Nothing secret is shown: the code only lets your phone approve this computer.)\n');

  const wantsScan = yes(await deps.ask('Connect by scanning a code? [Y/n] (n = paste a key instead): '));
  if (!wantsScan) return { kind: 'paste' };

  for (let attempt = 1; attempt <= MAX_CODES; attempt++) {
    const outcome: PairingOutcome = await pairComputer({
      client,
      now: () => Date.now(),
      sleep,
      write: (text) => console.log(text),
      showQr,
      confirmAccount: async ({ accountHint, keyName }) => {
        const who = accountHint ?? 'an unknown account';
        console.log(`\nYour phone (${who}) approved "${keyName ?? 'this computer'}".`);
        // The relay defence: if someone else scanned the code, the account shown is not yours.
        return yes(await deps.ask(`Connect this computer to ${who}? [Y/n]: `));
      },
    });

    if (outcome.kind === 'paired') {
      return {
        kind: 'paired',
        apiKey: outcome.apiKey,
        keyName: outcome.keyName,
        accountHint: outcome.accountHint,
      };
    }

    if (outcome.kind === 'declined') {
      console.log('\nNot connected. That key has been revoked, so it cannot be used.');
      console.log('If you did not expect another account, someone may have scanned your code.\n');
      return { kind: 'paste' };
    }

    if (outcome.kind === 'expired') {
      if (attempt < MAX_CODES) {
        console.log('\nThat code expired.');
        if (yes(await deps.ask('Show a new code? [Y/n]: '))) continue;
      }
      return { kind: 'paste' };
    }

    if (outcome.kind === 'cancelled') {
      return { kind: 'aborted', message: 'Pairing cancelled.' };
    }

    console.log(`\n${outcome.message}`);
    return { kind: 'paste' };
  }

  return { kind: 'paste' };
}
