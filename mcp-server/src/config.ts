import fs from 'fs';
import path from 'path';
import os from 'os';
import { atomicWriteFileSync } from './fsutil.js';

/**
 * NOTE: this module deliberately does NOT load a `.env` file. The server is spawned by an
 * IDE with the *open project* as its working directory, so honouring `./.env` would let any
 * repository you open redirect the server (SUPABASE_URL) or swap credentials. Configuration
 * comes only from real environment variables and ~/.srutam/config.json.
 */

export interface SrutamConfig {
  apiKey: string;
  supabaseUrl: string;
  supabaseAnonKey: string;
}

export class ConfigError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'ConfigError';
  }
}

/** Public defaults. The anon key is a public, RLS-scoped key; it grants nothing without an API key. */
export const DEFAULTS = {
  supabaseUrl: 'https://bnahuqxvpbtzaupyumeo.supabase.co',
  supabaseAnonKey:
    'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJuYWh1cXh2cGJ0emF1cHl1bWVvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk1ODM3NDQsImV4cCI6MjEwNTE1OTc0NH0.2GNtlaNyVSYy77FSYQ2mReZuzxbl4XSlZGQ-SJ-p7Ag',
} as const;

export interface UserConfigFile {
  apiKey?: string;
  supabaseUrl?: string;
  supabaseAnonKey?: string;
  /** Set by `init` when a self-hosted endpoint was chosen, so the saved URL keeps validating. */
  allowCustomUrl?: boolean;
}

export function getConfigDir(): string {
  // SRUTAM_CONFIG_DIR lets tests (and unusual setups) avoid touching the real home directory.
  return process.env.SRUTAM_CONFIG_DIR || path.join(os.homedir(), '.srutam');
}

export function getConfigPath(): string {
  return path.join(getConfigDir(), 'config.json');
}

export function readUserConfig(): UserConfigFile | null {
  try {
    const configPath = getConfigPath();
    if (fs.existsSync(configPath)) {
      const parsed = JSON.parse(fs.readFileSync(configPath, 'utf8'));
      if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
        return parsed as UserConfigFile;
      }
    }
  } catch {
    // Ignore read/parse errors and fall back to environment
  }
  return null;
}

export function saveUserConfig(data: { apiKey: string; supabaseUrl?: string; supabaseAnonKey?: string; allowCustomUrl?: boolean }): void {
  const dir = getConfigDir();
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true, mode: 0o700 });
  }
  // On POSIX the file is 0600. On Windows the mode is ignored and the file inherits the
  // user-profile ACL, which is already private to the current user and administrators.
  atomicWriteFileSync(getConfigPath(), JSON.stringify(data, null, 2), 0o600);
}

export function clearUserConfig(): void {
  try {
    const configPath = getConfigPath();
    if (fs.existsSync(configPath)) {
      fs.unlinkSync(configPath);
    }
  } catch {
    // Ignore errors if file does not exist
  }
}

/**
 * Validates and normalises a Supabase project URL.
 *
 * Only https://<project>.supabase.co is accepted by default so that a stray environment
 * variable cannot silently point the server (and the key hash it sends) at another host.
 * Self-hosted Supabase is possible by opting in with SRUTAM_ALLOW_CUSTOM_URL=1 (or `init`, which records it
 * in the user-owned ~/.srutam/config.json).
 */
export function validateSupabaseUrl(rawUrl: string, allowCustom: boolean = process.env.SRUTAM_ALLOW_CUSTOM_URL === '1'): string {
  const cleaned = rawUrl.trim().replace(/\/rest\/v1\/?$/, '').replace(/\/+$/, '');

  let parsed: URL;
  try {
    parsed = new URL(cleaned);
  } catch {
    throw new ConfigError(`SUPABASE_URL is not a valid URL: "${rawUrl}"`);
  }

  const isSupabaseHost = parsed.protocol === 'https:' && /(^|\.)supabase\.co$/i.test(parsed.hostname);
  const isLocalDev =
    parsed.protocol === 'http:' && (parsed.hostname === 'localhost' || parsed.hostname === '127.0.0.1');

  if (isSupabaseHost || (allowCustom && (parsed.protocol === 'https:' || isLocalDev))) {
    return cleaned;
  }

  throw new ConfigError(
    `Refusing to use SUPABASE_URL "${parsed.origin}": it must be an https://*.supabase.co address. ` +
      'Set SRUTAM_ALLOW_CUSTOM_URL=1 if you intentionally run a self-hosted Supabase.'
  );
}

/** True when the URL passes validation without any opt-in (i.e. it is a regular https://*.supabase.co project). */
export function isStandardSupabaseUrl(url: string): boolean {
  try {
    validateSupabaseUrl(url, false);
    return true;
  } catch {
    return false;
  }
}

/** Resolves the (validated) Supabase endpoint from env, then ~/.srutam/config.json, then defaults. */
export function resolveEndpoint(): { supabaseUrl: string; supabaseAnonKey: string } {
  const userConfig = readUserConfig();
  const rawUrl = process.env.SUPABASE_URL || userConfig?.supabaseUrl || DEFAULTS.supabaseUrl;

  return {
    supabaseUrl: validateSupabaseUrl(
      rawUrl,
      process.env.SRUTAM_ALLOW_CUSTOM_URL === '1' || userConfig?.allowCustomUrl === true
    ),
    supabaseAnonKey: process.env.SUPABASE_ANON_KEY || userConfig?.supabaseAnonKey || DEFAULTS.supabaseAnonKey,
  };
}

/**
 * Returns null when no API key is configured. Throws ConfigError when the endpoint is invalid.
 */
export function loadConfig(): SrutamConfig | null {
  const userConfig = readUserConfig();
  const apiKey = process.env.SRUTAM_API_KEY || userConfig?.apiKey;

  if (!apiKey) {
    return null;
  }

  return { apiKey, ...resolveEndpoint() };
}
