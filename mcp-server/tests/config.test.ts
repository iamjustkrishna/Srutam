import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import fs from 'fs';
import os from 'os';
import path from 'path';
import {
  ConfigError,
  DEFAULTS,
  getConfigDir,
  getConfigPath,
  readUserConfig,
  saveUserConfig,
  clearUserConfig,
  isStandardSupabaseUrl,
  loadConfig,
  validateSupabaseUrl,
} from '../src/config.js';

describe('Config Management', () => {
  const originalEnv = { ...process.env };

  beforeEach(() => {
    delete process.env.SRUTAM_API_KEY;
    delete process.env.SUPABASE_URL;
    delete process.env.SUPABASE_ANON_KEY;
    delete process.env.SRUTAM_ALLOW_CUSTOM_URL;
    clearUserConfig();
  });

  afterEach(() => {
    process.env = { ...originalEnv };
  });

  it('keeps test credentials out of the real home directory', () => {
    expect(getConfigDir()).not.toBe(path.join(os.homedir(), '.srutam'));
    expect(getConfigDir()).toContain('srutam-test-config-');
  });

  it('provides a deterministic config path inside the config directory', () => {
    expect(getConfigPath()).toBe(path.join(getConfigDir(), 'config.json'));
  });

  it('loads config from environment variables', () => {
    process.env.SRUTAM_API_KEY = 'srtm_live_test_env_key';
    process.env.SUPABASE_URL = 'https://custom-project.supabase.co';

    const config = loadConfig();
    expect(config).not.toBeNull();
    expect(config?.apiKey).toBe('srtm_live_test_env_key');
    expect(config?.supabaseUrl).toBe('https://custom-project.supabase.co');
  });

  it('falls back to the public defaults for the endpoint', () => {
    process.env.SRUTAM_API_KEY = 'srtm_live_test_key';
    const config = loadConfig();
    expect(config?.supabaseUrl).toBe(DEFAULTS.supabaseUrl);
    expect(config?.supabaseAnonKey).toBe(DEFAULTS.supabaseAnonKey);
  });

  it('normalizes trailing slashes and /rest/v1 on supabase URL', () => {
    process.env.SRUTAM_API_KEY = 'srtm_live_test_key';
    process.env.SUPABASE_URL = 'https://custom-project.supabase.co/rest/v1/';
    expect(loadConfig()?.supabaseUrl).toBe('https://custom-project.supabase.co');
  });

  it('returns null when no key is configured anywhere', () => {
    expect(loadConfig()).toBeNull();
  });

  it('prefers the environment key over the saved key', () => {
    saveUserConfig({ apiKey: 'srtm_live_from_file' });
    expect(loadConfig()?.apiKey).toBe('srtm_live_from_file');

    process.env.SRUTAM_API_KEY = 'srtm_live_from_env';
    expect(loadConfig()?.apiKey).toBe('srtm_live_from_env');
  });

  it('handles saveUserConfig and clearUserConfig gracefully', () => {
    saveUserConfig({ apiKey: 'srtm_live_saved_test_key', supabaseUrl: 'https://saved.supabase.co' });

    const readBack = readUserConfig();
    expect(readBack?.apiKey).toBe('srtm_live_saved_test_key');

    clearUserConfig();
    expect(fs.existsSync(getConfigPath())).toBe(false);
  });

  it('writes the saved config atomically without leaving temp files', () => {
    saveUserConfig({ apiKey: 'srtm_live_a' });
    saveUserConfig({ apiKey: 'srtm_live_b' });
    expect(readUserConfig()?.apiKey).toBe('srtm_live_b');
    expect(fs.readdirSync(getConfigDir())).toEqual(['config.json']);
  });

  it('ignores a corrupt or non-object saved config', () => {
    fs.mkdirSync(getConfigDir(), { recursive: true });
    fs.writeFileSync(getConfigPath(), '{ not json');
    expect(readUserConfig()).toBeNull();
    fs.writeFileSync(getConfigPath(), '["array"]');
    expect(readUserConfig()).toBeNull();
  });
});

describe('Supabase URL validation (redirect protection)', () => {
  const originalEnv = { ...process.env };
  afterEach(() => {
    process.env = { ...originalEnv };
  });

  it('accepts https *.supabase.co project URLs', () => {
    expect(validateSupabaseUrl('https://abc123.supabase.co')).toBe('https://abc123.supabase.co');
  });

  it.each([
    'http://abc123.supabase.co',
    'https://evil.example.com',
    'https://supabase.co.evil.example',
    'https://evilsupabase.co',
    'ftp://abc.supabase.co',
    'not a url',
  ])('rejects %s by default', (url) => {
    expect(() => validateSupabaseUrl(url)).toThrow(ConfigError);
  });

  it('allows custom https hosts and localhost only with explicit opt-in', () => {
    process.env.SRUTAM_ALLOW_CUSTOM_URL = '1';
    expect(validateSupabaseUrl('https://supabase.mycompany.internal')).toBe('https://supabase.mycompany.internal');
    expect(validateSupabaseUrl('http://127.0.0.1:54321')).toBe('http://127.0.0.1:54321');
    expect(() => validateSupabaseUrl('http://evil.example.com')).toThrow(ConfigError);
  });

  it('makes loadConfig throw instead of silently using a hostile SUPABASE_URL', () => {
    process.env.SRUTAM_API_KEY = 'srtm_live_test_key';
    process.env.SUPABASE_URL = 'https://evil.example.com';
    expect(() => loadConfig()).toThrow(ConfigError);
  });

  it('classifies standard vs self-hosted URLs', () => {
    expect(isStandardSupabaseUrl('https://abc.supabase.co')).toBe(true);
    expect(isStandardSupabaseUrl('https://supabase.mycompany.internal')).toBe(false);
    expect(isStandardSupabaseUrl('http://127.0.0.1:54321')).toBe(false);
  });

  it('honours a self-hosted URL saved by `init` (allowCustomUrl) without needing the env flag', () => {
    clearUserConfig();
    saveUserConfig({ apiKey: 'srtm_live_k', supabaseUrl: 'https://supabase.mycompany.internal', allowCustomUrl: true });
    expect(loadConfig()?.supabaseUrl).toBe('https://supabase.mycompany.internal');
    clearUserConfig();
  });

  it('rejects a saved self-hosted URL that was not opted in', () => {
    clearUserConfig();
    saveUserConfig({ apiKey: 'srtm_live_k', supabaseUrl: 'https://supabase.mycompany.internal' });
    expect(() => loadConfig()).toThrow(ConfigError);
    clearUserConfig();
  });
});

describe('.env files are not auto-loaded', () => {
  const originalCwd = process.cwd();
  const originalEnv = { ...process.env };
  let tmp: string;

  beforeEach(() => {
    delete process.env.SRUTAM_API_KEY;
    delete process.env.SUPABASE_URL;
    tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'srutam-hostile-repo-'));
    fs.writeFileSync(
      path.join(tmp, '.env'),
      'SRUTAM_API_KEY=srtm_live_from_hostile_repo\nSUPABASE_URL=https://evil.example.com\n'
    );
  });

  afterEach(() => {
    process.chdir(originalCwd);
    process.env = { ...originalEnv };
    fs.rmSync(tmp, { recursive: true, force: true });
  });

  it('ignores ./.env in the working directory', async () => {
    process.chdir(tmp);
    vi.resetModules(); // re-evaluate config.ts, which is where dotenv.config() used to run at import
    const fresh = await import('../src/config.js');

    expect(process.env.SRUTAM_API_KEY).toBeUndefined();
    expect(process.env.SUPABASE_URL).toBeUndefined();
    expect(fresh.loadConfig()).toBeNull();
  });
});
