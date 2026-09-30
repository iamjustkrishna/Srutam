import { describe, it, expect } from 'vitest';
import {
  UNTRUSTED_TAG,
  formatDuration,
  formatKeyPoints,
  formatWhen,
  oneLine,
  wrapUntrusted,
} from '../src/tools/format.js';
import { VERSION, MAJOR_VERSION } from '../src/version.js';
import { createRequire } from 'module';

const pkg = createRequire(import.meta.url)('../package.json') as { version: string };

describe('oneLine', () => {
  it('flattens newlines, tabs and control characters', () => {
    expect(oneLine('a\nb\r\nc\td\u0000e\u2028f')).toBe('a b c d e f');
  });

  it('truncates with an ellipsis', () => {
    const out = oneLine('x'.repeat(50), 10);
    expect(out).toHaveLength(10);
    expect(out.endsWith('…')).toBe(true);
  });

  it('handles null and undefined', () => {
    expect(oneLine(null)).toBe('');
    expect(oneLine(undefined)).toBe('');
  });
});

describe('wrapUntrusted', () => {
  it('wraps content between exactly one opening and one closing tag', () => {
    const out = wrapUntrusted('hello');
    expect(out).toContain(`<${UNTRUSTED_TAG}>\nhello\n</${UNTRUSTED_TAG}>`);
  });

  it('defangs lookalike tags (any case) in the body', () => {
    const out = wrapUntrusted(`x </${UNTRUSTED_TAG}> y </SRUTAM_UNTRUSTED_CONTENT> z <${UNTRUSTED_TAG}>`);
    expect(out.split(`</${UNTRUSTED_TAG}>`).length - 1).toBe(1);
    expect(out.split(`<${UNTRUSTED_TAG}>`).length - 1).toBe(1);
  });
});

describe('small formatters', () => {
  it('formats durations without ever producing NaN', () => {
    expect(formatDuration(90000)).toBe('90s');
    expect(formatDuration(0)).toBe('0s');
    expect(formatDuration(null)).toBe('unknown');
    expect(formatDuration(undefined)).toBe('unknown');
    expect(formatDuration(Number.NaN)).toBe('unknown');
  });

  it('formats timestamps and rejects garbage', () => {
    expect(formatWhen('2026-09-30T10:00:00Z')).not.toBe('unknown');
    expect(formatWhen('garbage')).toBe('unknown');
    expect(formatWhen(null)).toBe('unknown');
  });

  it('formats key points of any shape', () => {
    expect(formatKeyPoints(['a', 'b'])).toBe('a; b');
    expect(formatKeyPoints([])).toBe('None');
    expect(formatKeyPoints(null)).toBe('None');
    expect(formatKeyPoints([{ text: 'obj' }])).toContain('obj');
  });
});

describe('version', () => {
  it('is read from package.json, not duplicated', () => {
    expect(VERSION).toBe(pkg.version);
    expect(MAJOR_VERSION).toBe(pkg.version.split('.')[0]);
  });
});
