// "2 a.m. Before the Exam" (30 fps, 1080x1920): scenes, voice lines, beats and
// sound in absolute frames; scripts/generate_audio_stories.py mirrors these times.
// Srutam answers from the lecture recording and plays the moment back.
import type { CaptionCue, Cue, Line } from "../stories/Story";

export const TOTAL = 1235; // 41.2 s

export const SCENES = {
  night: { from: 0, duration: 150 },
  papers: { from: 150, duration: 80 },
  lecture: { from: 230, duration: 78 },
  insert: { from: 308, duration: 72 },
  prof: { from: 380, duration: 92 },
  class: { from: 472, duration: 78 },
  ask: { from: 550, duration: 140 },
  listen: { from: 690, duration: 100 },
  door: { from: 790, duration: 90 },
  lapse: { from: 880, duration: 130 },
  remind: { from: 1010, duration: 34 },
  hall: { from: 1044, duration: 66 },
  end: { from: 1110, duration: TOTAL - 1110 },
} as const;

export const BEAT = {
  handHead: 64,
  grab: 128,
  flip: [156, 167, 178, 190, 203, 216],
  ripple: 230,
  snore: 262,
  chalk1: 236,
  recTick: 318,
  palm: [392, 418, 446, 500, 522],
  chalk2: 404,
  chalk3: 452,
  chalkTap: 522,
  headDrop: 500,
  rippleBack: 540,
  type: 566,
  answer: 640,
  refs: 680,
  tapRef: 686,
  play: 692,
  relief: 724,
  yes: 762,
  knock: 792,
  bang: 806,
  dash: 930,
  back: 952,
  clink: [962, 988],
  notify: 1014,
  bell: 1046,
  paperDown: 1054,
  spin: 1074,
  catch: 1094,
  band: 1130,
} as const;

const V = "voiceover/exam";
export const VO: (Line & { src: string })[] = [
  { file: "e-forgot", at: 20, frames: 108, src: `${V}/meera/e-forgot.mp3` },
  { file: "e-asleep", at: 246, frames: 98, src: `${V}/meera/e-asleep.mp3` },
  { file: "prof-chapters", at: 388, frames: 156, src: `${V}/prof/prof-chapters.wav` },
  { file: "e-ask", at: BEAT.type, frames: 68, src: `${V}/meera/e-ask.mp3` },
  { file: "prof-recording", at: BEAT.play, frames: 92, src: `${V}/prof/prof-recording.wav` },
  { file: "riya-record", at: 820, frames: 45, src: `${V}/riya/riya-record.wav` },
  { file: "kabir-chai", at: 888, frames: 40, src: `${V}/kabir/kabir-chai.wav` },
  { file: "e-tagline", at: 1140, frames: 75, src: `${V}/meera/e-tagline.mp3` },
];

export const SFX: Cue[] = [
  { file: "sfx/tick.wav", at: 6, volume: 0.35 },
  { file: "sfx/tick.wav", at: 36, volume: 0.35 },
  { file: "sfx/tick.wav", at: 66, volume: 0.35 },
  { file: "sfx/tick.wav", at: 96, volume: 0.35 },
  ...BEAT.flip.map((at, i) => ({ file: "exam/paper.wav", at, volume: 0.45 + (i % 2) * 0.15 })),
  { file: "exam/rewind.wav", at: BEAT.ripple - 8, volume: 0.45 },
  { file: "sfx/tap.wav", at: 262, volume: 0.35 },
  { file: "sfx/tap.wav", at: 290, volume: 0.35 },
  { file: "exam/snore.wav", at: BEAT.snore, volume: 0.4 },
  { file: "sfx/pop.wav", at: BEAT.recTick, volume: 0.3 },
  { file: "exam/chalk.wav", at: BEAT.chalk2, volume: 0.3 },
  { file: "exam/chalk.wav", at: BEAT.chalk3, volume: 0.3 },
  { file: "exam/snore.wav", at: 476, volume: 0.35 },
  { file: "sfx/tap.wav", at: BEAT.chalkTap, volume: 0.55 },
  { file: "sfx/whoosh.wav", at: BEAT.headDrop, volume: 0.25 },
  { file: "exam/rewind.wav", at: BEAT.rippleBack, volume: 0.35 },
  { file: "nani/typing.wav", at: BEAT.type + 4, volume: 0.35 },
  { file: "sfx/pop.wav", at: BEAT.answer, volume: 0.4 },
  { file: "sfx/pop.wav", at: BEAT.refs, volume: 0.45 },
  { file: "sfx/tap.wav", at: BEAT.tapRef, volume: 0.6 },
  { file: "sfx/shimmer.wav", at: BEAT.yes, volume: 0.35 },
  { file: "exam/knock-door.wav", at: BEAT.knock, volume: 0.9 },
  { file: "exam/door.wav", at: BEAT.bang, volume: 0.8 },
  { file: "sfx/whoosh.wav", at: BEAT.dash, volume: 0.45 },
  { file: "sfx/whoosh.wav", at: BEAT.back - 4, volume: 0.35 },
  ...BEAT.clink.map((at) => ({ file: "exam/clink.wav", at, volume: 0.45 })),
  { file: "sfx/knock.wav", at: BEAT.notify, volume: 0.9 },
  { file: "exam/bell.wav", at: BEAT.bell, volume: 0.45 },
  { file: "exam/paper.wav", at: BEAT.paperDown, volume: 0.5 },
  { file: "sfx/whoosh.wav", at: BEAT.spin, volume: 0.25 },
  { file: "exam/scribble.wav", at: BEAT.catch + 2, volume: 0.5 },
  { file: "sfx/shimmer.wav", at: BEAT.band, volume: 0.4 },
];

export const CAPTIONS: CaptionCue[] = [
  { from: 20, to: 128, text: "Exam at nine. And I forgot what Prof said." },
  { from: 246, to: 344, text: "Half the class was asleep. Srutam wasn't." },
  { from: 388, to: 544, text: "“Chapters 4 and 7… and the metro case study. Twenty marks.”" },
  { from: BEAT.type, to: 634, text: "“What did Prof say will be on the exam?”" },
  { from: BEAT.play, to: 784, text: "▶ Econ lecture, 41:05 · “…and the metro case study. Twenty marks.”", top: true },
  { from: 820, to: 866, text: "“You recorded it, right?!”" },
  { from: 888, to: 930, text: "“I'll get the chai!”" },
];
