// "Knock Knock", native 16:9 with English voices (30 fps, 1920x1080). The
// vertical timeline (src/knock/timeline.ts) with Meera's "Okay Ma, I'll call you
// on Sunday. Promise!" (3.8 s) and Ma's "Oh! You remembered!" (2.1 s, vs 1.5 s):
// Ma's line starts 6 frames earlier and the end card comes 12 frames later, so it
// shrinks into place after she has finished; the logo knock (and the closing line
// after it) waits until the card has settled.
import type { CaptionCue, Cue, Line } from "../../stories/Story";

/** Frames the end card (and everything after it) moves later. */
const E = 12;

export const TOTAL = 960; // 32 s

export const SCENES = {
  hook: { from: 0, duration: 105 },
  promise: { from: 105, duration: 150 },
  life: { from: 255, duration: 150 },
  once: { from: 405, duration: 165 },
  knock: { from: 570, duration: 180 },
  payoff: { from: 750, duration: TOTAL - 750 },
} as const;

export const BEAT = {
  knock1: 10,
  knock2: 13,
  title: 80, // "The promise you forgot."
  bubble: 122,
  maThinks: 300,
  tapStart: 442,
  sayIt: 478, // "Call Ma, Sunday at seven."
  card: 540,
  notify: 600, // the reminder knocks
  laugh: 645,
  endCard: 820 + E,
  logoKnock: 820 + E + 18, // once the card has settled
} as const;

const M = "voiceover/knock/meera";
/** `frames` = ceil(duration * 30) of each file. */
export const VO: (Line & { src: string })[] = [
  { file: "k1-who", at: 44, frames: 33, src: `${M}/k1-who.mp3` },
  { file: "k2-promise", at: 122, frames: 114, src: "voiceover/en/knock/meera/k2-promise.mp3" }, // ends 236
  { file: "k3-busy", at: 290, frames: 90, src: `${M}/k3-busy.mp3` },
  { file: "k4-once", at: 418, frames: 120, src: `${M}/k4-once.mp3` },
  { file: "k5-knocks", at: 640, frames: 55, src: `${M}/k5-knocks.mp3` },
  { file: "ma-remembered", at: 762, frames: 63, src: "voiceover/en/knock/ma/ma-remembered.wav" }, // speech ends ~815, file 825
  { file: "k6-end", at: 870, frames: 73, src: `${M}/k6-end.mp3` }, // 20 frames after the knock, ends 943
];

export const SFX: Cue[] = [
  { file: "sfx/knock.wav", at: BEAT.knock1, volume: 1 },
  { file: "knock/street.wav", at: 105, volume: 0.35, dur: 150 },
  { file: "knock/horn.wav", at: 200, volume: 0.25 },
  { file: "sfx/whoosh.wav", at: 252, volume: 0.35 },
  { file: "knock/clock.wav", at: 255, volume: 0.45, dur: 150 },
  { file: "knock/tv-laugh.wav", at: 268, volume: 0.35 },
  { file: "knock/tv-laugh.wav", at: 380, volume: 0.3 },
  { file: "sfx/whoosh.wav", at: 402, volume: 0.35 },
  { file: "sfx/tap.wav", at: BEAT.tapStart, volume: 0.7 },
  { file: "sfx/pop.wav", at: BEAT.card, volume: 0.6 },
  { file: "sfx/knock.wav", at: BEAT.notify, volume: 1 },
  { file: "sfx/whoosh.wav", at: 747, volume: 0.3 },
  { file: "sfx/shimmer.wav", at: BEAT.endCard, volume: 0.4 },
  { file: "sfx/knock.wav", at: BEAT.logoKnock, volume: 0.9 },
];

// Narration only; on-screen dialogue lives in speech bubbles.
export const CAPTIONS: CaptionCue[] = [
  { from: 44, to: 76, text: "Who's there?" },
  { from: 290, to: 379, text: "Sunday came… and I got busy." },
  { from: 418, to: 466, text: "Now, I just say it once." },
  { from: 640, to: 693, text: "And Srutam knocks." },
];
