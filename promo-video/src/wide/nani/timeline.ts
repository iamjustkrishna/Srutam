// "Nani's Laddoos", native 16:9 with English voices (30 fps, 1920x1080). The
// vertical timeline (src/nani/timeline.ts) with Nani's English lines: "Measure?
// Oh, sweetheart... you go by the smell!" runs 4.9 s (vs 4.2 s), so the kitchen
// holds 24 frames longer and everything after it moves 24 frames later.
import type { CaptionCue, Cue, Line } from "../../stories/Story";

/** Frames added to the kitchen; every later scene, line, beat and sound moves by this. */
const D = 24;

export const TOTAL = 1245 + D; // 42.3 s

export const SCENES = {
  kitchen: { from: 0, duration: 240 + D },
  listen: { from: 240 + D, duration: 140 },
  diwali: { from: 380 + D, duration: 220 },
  ask: { from: 600 + D, duration: 170 },
  voice: { from: 770 + D, duration: 240 },
  call: { from: 1010 + D, duration: TOTAL - (1010 + D) },
} as const;

/** "One fistful of gur for every bowl of besan... | and a little love. | That's it." */
const RECIPE_AT = 800 + D;

export const BEAT = {
  shade: 252 + D, // the notification shade pulls down
  start: 330 + D, // Start on Srutam's recording notification, on "listen"
  gur: 516 + D, // "...and how much gur goes in?!"
  qStart: 612 + D,
  qEnd: 680 + D,
  aStart: 690 + D,
  aEnd: 740 + D,
  refs: 746 + D,
  tapRef: 765 + D,
  playFrom: RECIPE_AT,
  pyaar: RECIPE_AT + 108, // "...and a little love" (3.6 s into the English take)
  band: 1120 + D,
} as const;

const M = "voiceover/nani/meera";
const N = "voiceover/en/nani/nani";
/** `frames` = ceil(duration * 30) of each file. */
export const VO: (Line & { src: string })[] = [
  { file: "n1-never", at: 14, frames: 84, src: `${M}/n1-never.mp3` },
  { file: "nani-smell", at: 104, frames: 147, src: `${N}/nani-smell.wav` }, // ends 251
  { file: "n2-listen", at: 252 + D, frames: 93, src: `${M}/n2-listen.mp3` },
  { file: "n3-diwali", at: 398 + D, frames: 184, src: `${M}/n3-diwali.mp3` },
  { file: "n4-ask", at: 612 + D, frames: 75, src: `${M}/n4-ask.mp3` },
  { file: "nani-recipe", at: RECIPE_AT, frames: 196, src: `${N}/nani-recipe.wav` }, // ends 1020, scene ends 1034
  { file: "nani-mine", at: 1030 + D, frames: 87, src: `${N}/nani-mine.wav` }, // ends 1141
  { file: "n5-end", at: 1140 + D, frames: 73, src: `${M}/n5-end.mp3` }, // ends 1237
];

export const SFX: Cue[] = [
  { file: "nani/sizzle.wav", at: 0, volume: 0.35, dur: 255 + D },
  { file: "sfx/whoosh.wav", at: BEAT.shade - 4, volume: 0.35 },
  { file: "sfx/tap.wav", at: BEAT.start, volume: 0.7 },
  { file: "sfx/whoosh.wav", at: 377 + D, volume: 0.3 },
  { file: "nani/crackers.wav", at: 380 + D, volume: 0.4, dur: 222 },
  { file: "nani/typing.wav", at: BEAT.qStart, volume: 0.35, dur: 70 },
  { file: "nani/typing.wav", at: BEAT.aStart, volume: 0.25, dur: 52 },
  { file: "sfx/pop.wav", at: BEAT.refs, volume: 0.5 },
  { file: "sfx/tap.wav", at: BEAT.tapRef, volume: 0.7 },
  { file: "sfx/whoosh.wav", at: 1007 + D, volume: 0.3 },
  { file: "sfx/shimmer.wav", at: BEAT.band, volume: 0.4 },
];

export const CAPTIONS: CaptionCue[] = [
  { from: 14, to: 92, text: "My Nani never measured anything." },
  { from: 104, to: 242, text: "“Measure? Oh, sweetheart, you go by the smell!”" },
  { from: 252 + D, to: 345 + D, text: "So one Sunday, I let Srutam listen." },
  { from: 403 + D, to: 502 + D, text: "This Diwali, I'm 1,400 km away…" },
  { from: BEAT.gur, to: 575 + D, text: "…and how much gur goes in?!" },
  { from: RECIPE_AT, to: BEAT.pyaar - 4, text: "“One fistful of gur for every bowl of besan…”" },
  { from: BEAT.pyaar, to: RECIPE_AT + 186, text: "“…and a little love. That's it.”" },
  { from: 1030 + D, to: 1030 + D + 80, text: "“Oh, wow! Just like mine!”" },
];
