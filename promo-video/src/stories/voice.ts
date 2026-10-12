// Voice helpers for the story videos: Rhubarb mouth shapes by line, whether a
// line is playing, and a small head bob while someone talks.
import { visemeAt, type Viseme } from "../cast/Mouth";
import type { Line } from "./Story";

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
  /** A head bob that follows speech. */
  const talkTilt = (files: string[], f: number, base = 0, amp = 2.2) => base + (talking(files, f) ? Math.sin(f / 2.7) * amp : 0);
  return { line, vis, visAny, talking, talkTilt };
};
