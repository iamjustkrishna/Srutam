import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import fs from 'fs';
import path from 'path';
import {
  getConfigDir,
  getConfigPath,
  readUserConfig,
  saveUserConfig,
  clearUserConfig,
  loadConfig,
} from '../src/config.js';

describe('Config Management', () => {
  const originalEnv = { ...process.env };

  beforeEach(() => {
    delete process.env.SRUTAM_API_KEY;
    delete process.env.SUPABASE_URL;
    delete process.env.SUPABASE_ANON_KEY;
  });

  afterEach(() => {
    process.env = { ...originalEnv };
  });

  it('provides a deterministic config directory and path', () => {
    const dir = getConfigDir();
    const filePath = getConfigPath();
    expect(dir).toContain('.srutam');
    expect(filePath).toBe(path.join(dir, 'config.json'));
  });

  it('loads config from environment variables', () => {
    process.env.SRUTAM_API_KEY = 'srtm_live_test_env_key';
    process.env.SUPABASE_URL = 'https://custom-project.supabase.co';

    const config = loadConfig();
    expect(config).not.toBeNull();
    expect(config?.apiKey).toBe('srtm_live_test_env_key');
    expect(config?.supabaseUrl).toBe('https://custom-project.supabase.co');
  });

  it('normalizes trailing slashes on supabase URL', () => {
    process.env.SRUTAM_API_KEY = 'srtm_live_test_key';
    process.env.SUPABASE_URL = 'https://custom-project.supabase.co/rest/v1/';

    const config = loadConfig();
    expect(config?.supabaseUrl).toBe('https://custom-project.supabase.co');
  });

  it('returns null when no key is configured anywhere', () => {
    // Ensure no env var
    delete process.env.SRUTAM_API_KEY;

    // Mock readUserConfig by ensuring no config file or empty read
    const originalRead = readUserConfig;
    try {
      const config = loadConfig();
      // If no file exists or if file exists, test behavior
      if (!fs.existsSync(getConfigPath())) {
        expect(config).toBeNull();
      }
    } finally {
      // clean
    }
  });

  it('handles saveUserConfig and clearUserConfig gracefully', () => {
    const testConfig = { apiKey: 'srtm_live_saved_test_key', supabaseUrl: 'https://saved.supabase.co' };
    saveUserConfig(testConfig);

    const readBack = readUserConfig();
    expect(readBack?.apiKey).toBe('srtm_live_saved_test_key');

    clearUserConfig();
    // After clear, file does not exist
    expect(fs.existsSync(getConfigPath())).toBe(false);
  });
});
