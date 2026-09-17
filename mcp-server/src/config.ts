import dotenv from 'dotenv';
import fs from 'fs';
import path from 'path';
import os from 'os';

dotenv.config();

export interface SrutamConfig {
  apiKey: string;
  supabaseUrl: string;
  supabaseAnonKey: string;
}

export function getConfigDir(): string {
  return path.join(os.homedir(), '.srutam');
}

export function getConfigPath(): string {
  return path.join(getConfigDir(), 'config.json');
}

export function readUserConfig(): { apiKey?: string; supabaseUrl?: string } | null {
  try {
    const configPath = getConfigPath();
    if (fs.existsSync(configPath)) {
      const content = fs.readFileSync(configPath, 'utf8');
      return JSON.parse(content);
    }
  } catch {
    // Ignore read/parse errors and fallback to env
  }
  return null;
}

export function saveUserConfig(data: { apiKey: string; supabaseUrl?: string }): void {
  const dir = getConfigDir();
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true, mode: 0o700 });
  }
  const configPath = getConfigPath();
  fs.writeFileSync(configPath, JSON.stringify(data, null, 2), { mode: 0o600 });
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

export function loadConfig(): SrutamConfig | null {
  const userConfig = readUserConfig();
  const apiKey = process.env.SRUTAM_API_KEY || userConfig?.apiKey;

  if (!apiKey) {
    return null;
  }

  const rawUrl =
    process.env.SUPABASE_URL ||
    userConfig?.supabaseUrl ||
    'https://bnahuqxvpbtzaupyumeo.supabase.co';
  const supabaseUrl = rawUrl.replace(/\/rest\/v1\/?$/, '').replace(/\/$/, '');
  const supabaseAnonKey =
    process.env.SUPABASE_ANON_KEY ||
    'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJuYWh1cXh2cGJ0emF1cHl1bWVvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk1ODM3NDQsImV4cCI6MjEwNTE1OTc0NH0.2GNtlaNyVSYy77FSYQ2mReZuzxbl4XSlZGQ-SJ-p7Ag';

  return {
    apiKey,
    supabaseUrl,
    supabaseAnonKey,
  };
}
