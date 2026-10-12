// "Nani's Laddoos" (30 fps, 1080x1920). Scenes, voice lines, beats and sounds
// in absolute frames; scripts/generate_audio_stories.py mirrors these times.
import type { CaptionCue, Cue, Line } from "../stories/Story";

export const TOTAL = 1245; // 41.5 s

export const SCENES = {
  kitchen: { from: 0, duration: 240 },
  listen: { from: 240, duration: 140 },
  diwali: { from: 380, duration: 220 },
  ask: { from: 600, duration: 170 },
  voice: { from: 770, duration: 240 },
  call: { from: 1010, duration: TOTAL - 1010 },
} as const;

export const BEAT = {
  shade: 252, // the notification shade pulls down
  start: 330, // Start on Srutam's recording notification, on "listen"
  gur: 516, // "...and how much gur goes in?!"
  qStart: 612,
  qEnd: 680,
  aStart: 690,
  aEnd: 740,
  refs: 746,
  tapRef: 765,
  pyaar: 934, // "...aur thoda sa pyaar"
  band: 1120,
} as const;

const M = "voiceover/nani/meera";
const N = "voiceover/nani/nani";
export const VO: (Line & { src: string })[] = [
  { file: "n1-never", at: 14, frames: 82, src: `${M}/n1-never.mp3` },
  { file: "nani-smell", at: 108, frames: 126, src: `${N}/nani-smell.wav` },
  { file: "n2-listen", at: 252, frames: 100, src: `${M}/n2-listen.mp3` },
  { file: "n3-diwali", at: 398, frames: 191, src: `${M}/n3-diwali.mp3` },
  { file: "n4-ask", at: 612, frames: 73, src: `${M}/n4-ask.mp3` },
  { file: "nani-recipe", at: 800, frames: 197, src: `${N}/nani-recipe.wav` },
  { file: "nani-mine", at: 1030, frames: 83, src: `${N}/nani-mine.wav` },
  { file: "n5-end", at: 1140, frames: 83, src: `${M}/n5-end.mp3` },
];

export const SFX: Cue[] = [
  { file: "nani/sizzle.wav", at: 0, volume: 0.35, dur: 255 },
  { file: "sfx/whoosh.wav", at: BEAT.shade - 4, volume: 0.35 },
  { file: "sfx/tap.wav", at: BEAT.start, volume: 0.7 },
  { file: "sfx/whoosh.wav", at: 377, volume: 0.3 },
  { file: "nani/crackers.wav", at: 380, volume: 0.4, dur: 222 },
  { file: "nani/typing.wav", at: BEAT.qStart, volume: 0.35, dur: 70 },
  { file: "nani/typing.wav", at: BEAT.aStart, volume: 0.25, dur: 52 },
  { file: "sfx/pop.wav", at: BEAT.refs, volume: 0.5 },
  { file: "sfx/tap.wav", at: BEAT.tapRef, volume: 0.7 },
  { file: "sfx/whoosh.wav", at: 1007, volume: 0.3 },
  { file: "sfx/shimmer.wav", at: BEAT.band, volume: 0.4 },
];

export const CAPTIONS: CaptionCue[] = [
  { from: 14, to: 96, text: "My Nani never measured anything." },
  { from: 108, to: 234, text: "“Measure? Arre beta, you go by the smell!”" },
  { from: 252, to: 352, text: "So one Sunday, I let Srutam listen." },
  { from: 403, to: 502, text: "This Diwali, I'm 1,400 km away…" },
  { from: BEAT.gur, to: 571, text: "…and how much gur goes in?!" },
  { from: 800, to: 931, text: "“One fistful of gur for every bowl of besan…”" },
  { from: BEAT.pyaar, to: 997, text: "“…and a little love. That's it.”" },
  { from: 1030, to: 1113, text: "“Arre wah! Just like mine!”" },
];
