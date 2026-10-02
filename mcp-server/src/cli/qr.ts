import { encode } from 'uqr';

/**
 * Terminal QR rendering.
 *
 * Two things decide whether a phone can actually read this:
 *
 *  1. Polarity. QR requires dark modules on a light background. Terminals vary (and many are
 *     dark), so colours are set explicitly with ANSI rather than relying on the theme.
 *  2. Quiet zone. The spec wants 4 light modules of margin; 2 is the practical minimum and keeps
 *     the code inside an 80x24 window. uqr adds the border for us.
 *
 * Each character cell is about twice as tall as it is wide, so two module rows are packed into
 * one text row with half-block characters. A ~33-module code then needs ~33 columns and ~17 rows.
 */

const WHITE_BG = '\u001b[47m';
const BLACK_FG = '\u001b[30m';
const RESET = '\u001b[0m';
const UPPER_HALF = '\u2580'; // the glyph paints the TOP half in the foreground colour

export interface QrRenderOptions {
  /** Force colour on/off. Defaults to auto-detection. */
  color?: boolean;
  /** Quiet-zone width in modules (default 2). */
  border?: number;
}

/**
 * Renders `text` as a QR code made of half-block characters.
 *
 * Foreground is black and background white, so a dark terminal theme cannot invert the code.
 * In a row pair, the upper module drives the foreground and the lower one the background.
 */
export function renderQr(text: string, options: QrRenderOptions = {}): string {
  const border = options.border ?? 2;
  const useColor = options.color ?? true;
  const result = encode(text, { border, ecc: 'M' });
  const size = result.size;
  const isDark = (row: number, col: number): boolean =>
    row >= 0 && row < size && col >= 0 && col < size ? Boolean(result.data[row][col]) : false;

  const lines: string[] = [];
  for (let row = 0; row < size; row += 2) {
    let line = '';
    for (let col = 0; col < size; col++) {
      const top = isDark(row, col);
      const bottom = isDark(row + 1, col);
      if (useColor) {
        // Black-on-white: the glyph's top half is the upper module, its background the lower one.
        line += `\u001b[${top ? 30 : 37}m\u001b[${bottom ? 40 : 47}m${UPPER_HALF}`;
      } else {
        line += top && bottom ? '\u2588' : top ? UPPER_HALF : bottom ? '\u2584' : ' ';
      }
    }
    lines.push(useColor ? `${line}${RESET}` : line);
  }
  return lines.join('\n');
}

/**
 * Whether a scannable QR can reasonably be drawn here. A code for a ~30-character URI is 33
 * modules plus the quiet zone, so a narrow or non-capable terminal gets the typed code instead.
 */
export function canShowQr(stream: NodeJS.WriteStream = process.stdout): boolean {
  if (process.env.SRUTAM_NO_QR === '1') return false;
  if (process.env.TERM === 'dumb') return false;
  if (!stream.isTTY) return false;
  const columns = stream.columns ?? 80;
  return columns >= 40;
}

/** Re-exported so tests can assert the exact module matrix that was drawn. */
export function qrMatrix(text: string, border = 2): boolean[][] {
  const result = encode(text, { border, ecc: 'M' });
  return result.data.map((row) => Array.from(row, (cell) => Boolean(cell)));
}
