// Thumb typing on a phone: how much of a message is typed at a time, which key is
// under a thumb, and where each thumb is (left half of the keyboard: left thumb).
import type { Pt } from "../cast/Rig";
import { keyPos } from "./Hands";

/** Typing `text` from `start` at `cps` characters a second (story frames at 30 fps). */
export const typed = (text: string, f: number, start: number, cps = 8) => {
  const x = Math.max(0, ((f - start) * cps) / 30);
  const n = Math.min(text.length, Math.floor(x));
  const done = n >= text.length;
  const key = !done && n > 0 ? text[n - 1] : undefined;
  return { text: text.slice(0, n), n, done, key, press: x - Math.floor(x) };
};

/** Thumb tips for the phone (phone coordinates): the typing thumb dips onto its key, the other rests. */
export const thumbs = (key: string | undefined, press: number, w: number): { l: Pt | null; r: Pt | null } => {
  if (!key) return { l: null, r: null };
  const p = keyPos(key, w);
  const dip = Math.sin(Math.min(1, press * 2) * Math.PI) * 10;
  const tip: Pt = [p[0], p[1] + 30 - dip];
  return p[0] < w / 2 ? { l: tip, r: [w * 0.78, p[1] + 80] } : { l: [w * 0.22, p[1] + 80], r: tip };
};
