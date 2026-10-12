// Voice helpers for the story videos: Rhubarb mouth shapes by line, whether a
// line is playing, and a small head bob while someone talks.
import { visemeAt, type Viseme } from "../cast/Mouth";
import type { Line } from "./Story";

const OPEN: Record<Viseme, number> = { A: 0, B: 0.3, C: 0.7, D: 1, E: 0.6, F: 0.35, G: 0.3, H: 0.6, X: 0 };

export const voice = (lines: Line[], visemes: Record<string, string>) => {
  const line = (file: string) => lines.find((l) => l.file === file)!;
  const vis = (file: string, f: number, rest: Viseme = "X"): Viseme => {
    const v = visemeAt(visemes, file, line(file).at, f);
    return v === "X" ? rest : v;
  };
  /** The first line that is speaking among several (one character can have many lines). */
  const visAny = (files: string[], f: number, rest: Viseme = "X"): Viseme => {
    for (const file of files) {
      const v = vis(file, f);
      if (v !== "X") return v;
    }
    return rest;
  };
  const talking = (files: string[], f: number) => files.some((file) => f >= line(file).at && f <= line(file).at + line(file).frames);
  /**
   * Speech emphasis 0..1: how open the mouth has been over the last few frames.
   * Stressed syllables open wide, so beat gestures and nods land on them.
   */
  const emph = (files: string[], f: number, window = 5) => {
    let sum = 0;
    for (let i = 0; i < window; i++) {
      let o = 0;
      for (const file of files) o = Math.max(o, OPEN[vis(file, f - i)] ?? 0);
      sum += o * (1 - i / window);
    }
    return Math.min(1, sum / (window / 2));
  };
  /** A head that nods into the stressed words (and settles when the line ends). */
  const talkTilt = (files: string[], f: number, base = 0, amp = 2.2) => base + (talking(files, f) ? (emph(files, f) - 0.35) * amp * 1.6 : 0);
  return { line, vis, visAny, talking, talkTilt, emph };
};
