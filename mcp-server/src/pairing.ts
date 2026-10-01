import { createClient, SupabaseClient } from '@supabase/supabase-js';
import crypto from 'crypto';
import os from 'os';
import { SrutamConfig } from './config.js';

/**
 * Client for the QR pairing RPCs.
 *
 * Deliberately separate from SrutamClient: pairing runs *before* a key exists, and polling for
 * five minutes would otherwise trip that class's 60-calls-per-minute runaway-agent guard.
 *
 * The generated key never leaves this process. Only its SHA-256 hash is sent, and the phone
 * registers that hash under the account that approves it.
 */

export interface PairStart {
  code: string;
  expiresAt: string;
}

export type PairStatus =
  | { status: 'pending'; expiresAt: string }
  | { status: 'expired'; expiresAt?: string }
  | { status: 'cancelled'; expiresAt?: string }
  | { status: 'approved'; expiresAt?: string; accountHint: string | null; keyName: string | null }
  | { status: 'unknown' };

/** A new API key plus the hash the server will know it by. */
export function generateApiKey(): { apiKey: string; keyHash: string } {
  // 24 bytes = 192 bits, matching the keys the Android app generates.
  const apiKey = 'srtm_live_' + crypto.randomBytes(24).toString('hex');
  return { apiKey, keyHash: sha256(apiKey) };
}

export function sha256(value: string): string {
  return crypto.createHash('sha256').update(value).digest('hex');
}

/** "srtm_live_a1b2c3..." - the same short form the Srutam app shows for each key. */
export function keyPrefixOf(apiKey: string): string {
  return `${apiKey.slice(0, 16)}...`;
}

/** Hostname + OS, so the confirmation screen on the phone names a recognisable machine. */
export function describeThisMachine(): { label: string; platform: string } {
  const platform =
    process.platform === 'win32' ? 'Windows' : process.platform === 'darwin' ? 'macOS' : 'Linux';
  let host = '';
  try {
    host = os.hostname();
  } catch {
    host = '';
  }
  const cleanHost = host.replace(/[^A-Za-z0-9 ._-]/g, '').slice(0, 40);
  return { label: cleanHost ? `${cleanHost} (${platform})` : `A computer (${platform})`, platform };
}

export class PairingClient {
  private client: SupabaseClient;

  constructor(endpoint: { supabaseUrl: string; supabaseAnonKey: string }) {
    this.client = createClient(endpoint.supabaseUrl, endpoint.supabaseAnonKey, {
      auth: { persistSession: false, autoRefreshToken: false, detectSessionInUrl: false },
    });
  }

  private async rpc<T>(fn: string, args: Record<string, unknown>): Promise<T> {
    const { data, error } = await this.client.rpc(fn, args);
    if (error) {
      throw new Error(`${fn} failed: ${error.message}`);
    }
    return data as T;
  }

  async start(params: {
    keyHash: string;
    keyPrefix: string;
    label: string;
    platform: string;
    clientVersion: string;
    replacesKeyHash?: string | null;
  }): Promise<PairStart> {
    return this.rpc<PairStart>('mcp_pair_start', {
      p_key_hash: params.keyHash,
      p_key_prefix: params.keyPrefix,
      p_label: params.label,
      p_platform: params.platform,
      p_client_version: params.clientVersion,
      p_replaces_key_hash: params.replacesKeyHash ?? null,
    });
  }

  async status(keyHash: string): Promise<PairStatus> {
    return this.rpc<PairStatus>('mcp_pair_status', { p_key_hash: keyHash });
  }

  async cancel(keyHash: string): Promise<void> {
    await this.rpc('mcp_pair_cancel', { p_key_hash: keyHash });
  }

  /** Used when the user answers [n] to the account confirmation, and by `logout`. */
  async revokeSelf(keyHash: string): Promise<boolean> {
    const result = await this.rpc<{ revoked: boolean }>('mcp_revoke_self', { p_key_hash: keyHash });
    return Boolean(result?.revoked);
  }
}

/** The payload encoded in the QR. `v` lets the app reject codes from a newer protocol. */
export function pairingUri(code: string): string {
  return `srutam://pair?v=1&c=${encodeURIComponent(code)}`;
}

/** "K7QF2M9D" -> "K7QF-2M9D", which is far easier to read off a screen and retype. */
export function formatCode(code: string): string {
  return code.length === 8 ? `${code.slice(0, 4)}-${code.slice(4)}` : code;
}
