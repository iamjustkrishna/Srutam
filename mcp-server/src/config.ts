import dotenv from 'dotenv';

dotenv.config();

export interface SrutamConfig {
  apiKey: string;
  supabaseUrl: string;
  supabaseAnonKey: string;
}

export function loadConfig(): SrutamConfig {
  const apiKey = process.env.SRUTAM_API_KEY;
  if (!apiKey) {
    throw new Error(
      'Missing SRUTAM_API_KEY environment variable. Generate an API Key in Srutam App Settings -> Developer & MCP.'
    );
  }

  // Supabase endpoint configuration (can be preset or overridden via env)
  let rawUrl =
    process.env.SUPABASE_URL || 'https://bnahuqxvpbtzaupyumeo.supabase.co';
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
