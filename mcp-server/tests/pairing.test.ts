import { describe, it, expect, vi } from 'vitest';
import jsQR from 'jsqr';
import { renderQr, qrMatrix, canShowQr } from '../src/cli/qr.js';
import {
  describeThisMachine,
  formatCode,
  generateApiKey,
  keyPrefixOf,
  pairingUri,
  sha256,
} from '../src/pairing.js';
import { pairComputer, renderPairingScreen, type PairingDeps } from '../src/cli/pair.js';

const URI = 'srutam://pair?v=1&c=K7QF2M9D';

/** Paints a module matrix into an RGBA bitmap so jsQR can read it like a camera would. */
function rasterize(matrix: boolean[][], scale = 4) {
  const size = matrix.length * scale;
  const data = new Uint8ClampedArray(size * size * 4);
  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      const dark = matrix[Math.floor(y / scale)][Math.floor(x / scale)];
      const v = dark ? 0 : 255;
      const i = (y * size + x) * 4;
      data[i] = data[i + 1] = data[i + 2] = v;
      data[i + 3] = 255;
    }
  }
  return { data, width: size, height: size };
}

/**
 * Turns the ANSI the terminal would print back into a module matrix.
 * Each cell is ESC[<fg>m ESC[<bg>m plus the upper-half glyph, where the glyph paints the upper
 * module in the foreground colour and the lower module in the background colour.
 */
function parseRenderedQr(rendered: string): boolean[][] {
  const cellPattern = /\[(\d+)m\[(\d+)m▀/g;
  const matrix: boolean[][] = [];
  for (const row of rendered.split('\n')) {
    const cells = [...row.matchAll(cellPattern)];
    if (cells.length === 0) continue;
    matrix.push(cells.map((c) => c[1] === '30')); // 30 = black fg -> dark upper module
    matrix.push(cells.map((c) => c[2] === '40')); // 40 = black bg -> dark lower module
  }
  return matrix;
}

describe('QR rendering', () => {
  it('encodes a payload a camera can actually decode', () => {
    const { data, width, height } = rasterize(qrMatrix(URI));
    expect(jsQR(data, width, height)?.data).toBe(URI);
  });

  it('renders with correct polarity: the drawn code matches the encoder exactly', () => {
    // If foreground/background were swapped, or the half-block packing were off by a row, this
    // fails even though the output would still "look like" a QR code to a human.
    const expected = qrMatrix(URI);
    const drawn = parseRenderedQr(renderQr(URI));
    expect(drawn.length).toBeGreaterThanOrEqual(expected.length);
    for (let row = 0; row < expected.length; row++) {
      expect(drawn[row]).toEqual(expected[row]);
    }
  });

  it('round-trips the drawn code back through a decoder', () => {
    const drawn = parseRenderedQr(renderQr(URI)).slice(0, qrMatrix(URI).length);
    const { data, width, height } = rasterize(drawn);
    expect(jsQR(data, width, height)?.data).toBe(URI);
  });

  it('keeps a quiet zone so the finder patterns are not flush against content', () => {
    const matrix = qrMatrix(URI);
    expect(matrix[0].every((m) => !m)).toBe(true);
    expect(matrix[1].every((m) => !m)).toBe(true);
    expect(matrix.every((row) => !row[0] && !row[1])).toBe(true);
  });

  it('fits a standard 80x24 terminal', () => {
    const lines = renderQr(URI, { color: false }).split('\n');
    expect(lines.length).toBeLessThanOrEqual(22);
    expect(lines[0].length).toBeLessThanOrEqual(80);
  });

  it('falls back to text when a QR cannot be drawn', () => {
    const tty = (columns: number) => ({ isTTY: true, columns }) as NodeJS.WriteStream;
    expect(canShowQr(tty(80))).toBe(true);
    expect(canShowQr(tty(30))).toBe(false);
    expect(canShowQr({ isTTY: false, columns: 200 } as NodeJS.WriteStream)).toBe(false);

    const saved = process.env.SRUTAM_NO_QR;
    process.env.SRUTAM_NO_QR = '1';
    expect(canShowQr(tty(80))).toBe(false);
    if (saved === undefined) delete process.env.SRUTAM_NO_QR;
    else process.env.SRUTAM_NO_QR = saved;
  });

  it('always shows the typed code, so a broken QR is never a dead end', () => {
    expect(renderPairingScreen('K7QF2M9D', false)).toContain('K7QF-2M9D');
    expect(renderPairingScreen('K7QF2M9D', true)).toContain('K7QF-2M9D');
  });
});

describe('key and machine identity', () => {
  it('generates a 192-bit srtm_live_ key whose hash matches the server contract', () => {
    const { apiKey, keyHash } = generateApiKey();
    expect(apiKey).toMatch(/^srtm_live_[0-9a-f]{48}$/);
    expect(keyHash).toBe(sha256(apiKey));
    expect(keyHash).toMatch(/^[0-9a-f]{64}$/);
    expect(generateApiKey().apiKey).not.toBe(apiKey);
  });

  it('shows the same 16-character prefix the app displays', () => {
    // 'srtm_live_' + 6 hex, matching SupabaseCloudClient.kt's "srtm_live_${randomHex.take(6)}..."
    expect(keyPrefixOf('srtm_live_abcdef0123456789')).toBe('srtm_live_abcdef...');
  });

  it('describes this machine without leaking odd characters', () => {
    const { label, platform } = describeThisMachine();
    expect(label).not.toMatch(/[^A-Za-z0-9 ._()-]/);
    expect(['Windows', 'macOS', 'Linux']).toContain(platform);
  });

  it('formats codes for reading aloud and typing', () => {
    expect(formatCode('K7QF2M9D')).toBe('K7QF-2M9D');
    expect(pairingUri('K7QF2M9D')).toBe(URI);
  });
});

describe('pairing state machine', () => {
  const fakeClient = (statuses: any[]) => {
    let i = 0;
    return {
      start: vi.fn(async () => ({
        code: 'K7QF2M9D',
        expiresAt: new Date(Date.now() + 300_000).toISOString(),
      })),
      status: vi.fn(async () => statuses[Math.min(i++, statuses.length - 1)]),
      cancel: vi.fn(async () => undefined),
      revokeSelf: vi.fn(async () => true),
    } as any;
  };

  const deps = (client: any, over: Partial<PairingDeps> = {}): PairingDeps => ({
    client,
    now: () => Date.now(),
    sleep: async () => undefined,
    write: () => undefined,
    showQr: false,
    confirmAccount: async () => true,
    ...over,
  });

  it('returns the key only after the user confirms the account', async () => {
    const client = fakeClient([
      { status: 'pending' },
      { status: 'approved', accountHint: 'k***@gmail.com', keyName: 'DESKTOP' },
    ]);
    const confirmAccount = vi.fn(async () => true);
    const outcome = await pairComputer(deps(client, { confirmAccount }));

    expect(outcome.kind).toBe('paired');
    if (outcome.kind === 'paired') {
      expect(outcome.apiKey).toMatch(/^srtm_live_/);
      expect(outcome.accountHint).toBe('k***@gmail.com');
    }
    expect(confirmAccount).toHaveBeenCalledWith({
      accountHint: 'k***@gmail.com',
      keyName: 'DESKTOP',
    });
    expect(client.revokeSelf).not.toHaveBeenCalled();
  });

  it('revokes the key immediately when the user says the account is not theirs', async () => {
    const client = fakeClient([
      { status: 'approved', accountHint: 'evil@example.com', keyName: 'X' },
    ]);
    const outcome = await pairComputer(deps(client, { confirmAccount: async () => false }));

    expect(outcome.kind).toBe('declined');
    expect(client.revokeSelf).toHaveBeenCalledTimes(1);
  });

  it('never sends the raw key anywhere, only its hash', async () => {
    const client = fakeClient([{ status: 'approved', accountHint: 'a***@b.com', keyName: 'X' }]);
    const outcome = await pairComputer(deps(client));
    const sent = JSON.stringify(client.start.mock.calls[0][0]);

    expect(outcome.kind).toBe('paired');
    if (outcome.kind === 'paired') {
      expect(sent).not.toContain(outcome.apiKey);
      expect(sent).toContain(sha256(outcome.apiKey));
    }
  });

  it('gives up and cancels once the code has expired', async () => {
    const client = fakeClient([{ status: 'pending' }]);
    let clock = Date.now();
    const outcome = await pairComputer(deps(client, { now: () => (clock += 200_000) }));

    expect(outcome.kind).toBe('expired');
    expect(client.cancel).toHaveBeenCalled();
  });

  it('treats a server-side expiry as expired', async () => {
    expect((await pairComputer(deps(fakeClient([{ status: 'expired' }])))).kind).toBe('expired');
  });

  it('survives transient network errors instead of dropping the session', async () => {
    let call = 0;
    const client = {
      start: vi.fn(async () => ({
        code: 'K7QF2M9D',
        expiresAt: new Date(Date.now() + 300_000).toISOString(),
      })),
      status: vi.fn(async () => {
        if (++call <= 3) throw new Error('ECONNRESET');
        return { status: 'approved', accountHint: 'a***@b.com', keyName: 'X' };
      }),
      cancel: vi.fn(async () => undefined),
      revokeSelf: vi.fn(async () => true),
    } as any;

    expect((await pairComputer(deps(client))).kind).toBe('paired');
    expect(client.status).toHaveBeenCalledTimes(4);
  });

  it('stops after sustained network failure rather than looping forever', async () => {
    const client = {
      start: vi.fn(async () => ({
        code: 'K7QF2M9D',
        expiresAt: new Date(Date.now() + 300_000).toISOString(),
      })),
      status: vi.fn(async () => {
        throw new Error('offline');
      }),
      cancel: vi.fn(async () => undefined),
      revokeSelf: vi.fn(async () => true),
    } as any;

    expect((await pairComputer(deps(client))).kind).toBe('error');
  });

  it('cancels the pending request when the user aborts', async () => {
    const client = fakeClient([{ status: 'pending' }]);
    const outcome = await pairComputer(deps(client, { abort: { aborted: true } }));

    expect(outcome.kind).toBe('cancelled');
    expect(client.cancel).toHaveBeenCalled();
  });

  it('reports a failure to start without throwing', async () => {
    const client = {
      start: vi.fn(async () => {
        throw new Error('key already registered');
      }),
      status: vi.fn(),
      cancel: vi.fn(),
      revokeSelf: vi.fn(),
    } as any;

    const outcome = await pairComputer(deps(client));
    expect(outcome.kind).toBe('error');
    if (outcome.kind === 'error') expect(outcome.message).toContain('already registered');
  });
});
