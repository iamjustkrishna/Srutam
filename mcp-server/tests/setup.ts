import fs from 'fs';
import os from 'os';
import path from 'path';
import { afterAll } from 'vitest';

const sandbox = fs.mkdtempSync(path.join(os.tmpdir(), 'srutam-test-config-'));
process.env.SRUTAM_CONFIG_DIR = path.join(sandbox, '.srutam');

// Never let a developer's shell environment leak into tests.
delete process.env.SRUTAM_API_KEY;
delete process.env.SUPABASE_URL;
delete process.env.SUPABASE_ANON_KEY;
delete process.env.SRUTAM_ALLOW_CUSTOM_URL;

afterAll(() => {
  fs.rmSync(sandbox, { recursive: true, force: true });
});
