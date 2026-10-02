import { createRequire } from 'module';

// Single source of truth: package.json (works from both src/ and dist/).
const pkg = createRequire(import.meta.url)('../package.json') as { version: string };

export const VERSION: string = pkg.version;
export const MAJOR_VERSION: string = pkg.version.split('.')[0];
