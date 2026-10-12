// "Who Said That?" (30 fps, 1080x1920): scenes, voice lines, beats and sound in
// absolute frames; scripts/generate_audio_stories.py mirrors these times.
// Srutam never claims to know who spoke: it finds the moment and plays it back.
import type { CaptionCue, Cue, Line } from "../stories/Story";

export const TOTAL = 1310; // 43.7 s

export const SCENES = {
  sync: { from: 0, duration: 280 },
  note: { from: 280, duration: 70 },
  eyes: { from: 350, duration: 90 },
  dock: { from: 440, duration: 100 },
  cards: { from: 540, duration: 150 },
  week: { from: 690, duration: 60 },
  slam: { from: 750, duration: 154 },
  point: { from: 896, duration: 89 },
  ask: { from: 985, duration: 121 },
  play: { from: 1106, duration: 79 },
  end: { from: 1185, duration: TOTAL - 1185 },
} as const;

export const BEAT = {
  boardPoint: 10,
  boardTap: 100,
  pointArjun: 160,
  armsDown: 250,
  nodA: 250,
  nodN: 254,
  thumb: 292,
  bandA: 352,
  bandN: 376,
  bandM: 400,
  tap: 506,
  cardDecision: 549,
  cardStep: 581,
  cardReminder: 614,
  lunch: 643,
  pageTear: 700,
  windup: 750,
  impact: 768,
  whip: 896,
  pointA: 910,
  pointN: 930,
  raise: 1004,
  qStart: 1040,
  qEnd: 1062,
  aStart: 1066,
  aEnd: 1092,
  refs: 1094,
  tapRef: 1100,
  play: 1106,
  band: 1215,
} as const;

const V = "voiceover/meeting";
export const VO: (Line & { src: string })[] = [
  { file: "rao-launch", at: 14, frames: 58, src: `${V}/rao/rao-launch.wav` },
  { file: "rao-final", at: 82, frames: 75, src: `${V}/rao/rao-final.wav` },
  { file: "rao-deck", at: 166, frames: 77, src: `${V}/rao/rao-deck.wav` },
  { file: "arjun-yes", at: 250, frames: 24, src: `${V}/arjun/arjun-yes.wav` },
  { file: "neha-yes", at: 254, frames: 21, src: `${V}/neha/neha-yes.wav` },
  { file: "rao-note", at: 284, frames: 63, src: `${V}/rao/rao-note.wav` },
  { file: "w-nobody", at: 450, frames: 89, src: `${V}/meera/w-nobody.mp3` },
  { file: "w-decision", at: 544, frames: 131, src: `${V}/meera/w-decision.mp3` },
  { file: "rao-who", at: 776, frames: 117, src: `${V}/rao/rao-who.wav` },
  { file: "arjun-her", at: 914, frames: 20, src: `${V}/arjun/arjun-her.wav` },
  { file: "neha-him", at: 934, frames: 22, src: `${V}/neha/neha-him.wav` },
  { file: "m-ask", at: 992, frames: 89, src: `${V}/meera/m-ask.mp3` },
  { file: "rao-recording", at: BEAT.play, frames: 75, src: `${V}/rao/rao-recording.wav` },
  { file: "w-minuted", at: 1232, frames: 74, src: `${V}/meera/w-minuted.mp3` },
];

export const SFX: Cue[] = [
  { file: "meeting/office.wav", at: 0, volume: 0.3, dur: 285 },
  { file: "sfx/tap.wav", at: BEAT.boardTap, volume: 0.3 },
  { file: "sfx/whoosh.wav", at: 346, volume: 0.4 },
  { file: "sfx/whoosh.wav", at: BEAT.bandN - 4, volume: 0.25 },
  { file: "sfx/whoosh.wav", at: BEAT.bandM - 4, volume: 0.25 },
  { file: "meeting/cricket.wav", at: 412, volume: 0.5 },
  { file: "sfx/tap.wav", at: BEAT.tap, volume: 0.7 },
  { file: "sfx/pop.wav", at: BEAT.tap + 2, volume: 0.35 },
  { file: "sfx/pop.wav", at: BEAT.cardDecision, volume: 0.5 },
  { file: "sfx/pop.wav", at: BEAT.cardStep, volume: 0.5 },
  { file: "sfx/pop.wav", at: BEAT.cardReminder, volume: 0.5 },
  { file: "sfx/pop.wav", at: BEAT.lunch, volume: 0.3 },
  // one week later: room tone and a clock, so the hush before the slam is never dead air
  { file: "meeting/office.wav", at: 688, volume: 0.22, dur: 82 },
  ...[692, 707, 722, 737, 752].map((at) => ({ file: "sfx/tick.wav", at, volume: 0.5 })),
  { file: "meeting/paper.wav", at: BEAT.pageTear, volume: 0.6 },
  { file: "meeting/bam.wav", at: BEAT.impact, volume: 1 },
  { file: "meeting/sting.wav", at: BEAT.impact + 1, volume: 0.6, dur: 70 },
  { file: "sfx/whoosh.wav", at: BEAT.whip - 2, volume: 0.5 },
  { file: "meeting/boing.wav", at: BEAT.pointA + 2, volume: 0.5 },
  { file: "meeting/boing-hi.wav", at: BEAT.pointN + 2, volume: 0.5 },
  { file: "meeting/scratch.wav", at: 978, volume: 0.45 },
  { file: "sfx/whoosh.wav", at: BEAT.raise, volume: 0.3 },
  { file: "sfx/pop.wav", at: BEAT.qStart, volume: 0.4 },
  { file: "sfx/pop.wav", at: BEAT.refs, volume: 0.45 },
  { file: "sfx/tap.wav", at: BEAT.tapRef, volume: 0.6 },
  { file: "sfx/shimmer.wav", at: BEAT.band, volume: 0.4 },
];

export const CAPTIONS: CaptionCue[] = [
  { from: 14, to: 72, text: "“Launch on the 14th?”" },
  { from: 82, to: 157, text: "“Fine. The 14th, final.”" },
  { from: 166, to: 243, text: "“Arjun, pricing deck by Thursday.”" },
  { from: 250, to: 276, text: "“Yes sir!” “Yes, yes!”" },
  { from: 284, to: 347, text: "“Great. Someone note it down!”" },
  { from: 450, to: 539, text: "Nobody wrote it down. Except Srutam." },
  { from: 544, to: 675, text: "Decision. Next step. Reminder. Before lunch." },
  { from: 776, to: 893, text: "“The 14th? Who said yes to the 14th?!”", top: true },
  { from: 992, to: 1081, text: "“Relax. Let's ask Srutam.”" },
  { from: BEAT.play, to: 1181, text: "▶ Tuesday sync, 0:42 · “Fine. The 14th, final.”", top: true },
];
