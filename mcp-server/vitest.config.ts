import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    include: ['tests/**/*.test.ts'],
    // Redirects ~/.srutam to a throwaway directory for every test file, so no test can ever
    // read, overwrite or delete a developer's real credentials.
    setupFiles: ['tests/setup.ts'],
  },
});
