// "2 a.m. Before the Exam" v2 (story frames at 30 fps; rendered at 60 fps, see
// stories/time.ts). The group chat asks Meera, who slept through the lecture too;
// Srutam didn't. The flashback only half-hears Prof, so the line lands when Srutam
// plays the lecture back. scripts/generate_audio_stories.py mirrors these times.
import type { CaptionCue, Cue, Line } from "../stories/Story";

export const TOTAL = 1240; // 41.3 s

export const SCENES = {
  night: { from: 0, duration: 132 },
  chat: { from: 132, duration: 82 },
  guilt: { from: 214, duration: 40 },
  lecture: { from: 254, duration: 96 },
  insert: { from: 350, duration: 48 },
  ask: { from: 398, duration: 122 },
  listen: { from: 520, duration: 192 },
  share: { from: 712, duration: 80 },
  door: { from: 792, duration: 108 },
  lapse: { from: 900, duration: 110 },
  remind: { from: 1010, duration: 30 },
  hall: { from: 1040, duration: 90 },
  end: { from: 1130, duration: TOTAL - 1130 },
} as const;

export const BEAT = {
  handHead: 60,
  buzz: 118,
  msg: [146, 168, 190],
  realise: 236,
  ripple: 254,
  type: 404,
  answer: 476,
  refs: 508,
  tapRef: 514,
  play: 520,
  relief: 640,
  yes: 690,
  mine: 718,
  replies: [744, 764],
  knock: 798,
  open: 812,
  riyaIn: 840,
  clink: [946, 972],
  notify: 1014,
  bell: 1044,
  paperDown: 1052,
  circle: 1068,
  nod: 1076,
  spin: 1082,
  catch: 1102,
  band: 1136,
} as const;

const V = "voiceover/exam";
export const VO: (Line & { src: string })[] = [
  { file: "e-forgot", at: 16, frames: 108, src: `${V}/meera/e-forgot.mp3` },
  { file: "prof-lecture", at: 262, frames: 66, src: `${V}/prof/prof-lecture.wav` },
  { file: "e-asleep", at: 296, frames: 98, src: `${V}/meera/e-asleep.mp3` },
  { file: "e-ask", at: 404, frames: 68, src: `${V}/meera/e-ask.mp3` },
  { file: "prof-recording", at: BEAT.play, frames: 186, src: `${V}/prof/prof-recording.wav` },
  { file: "kabir-chai", at: 826, frames: 30, src: `${V}/kabir/kabir-chai.wav` },
  { file: "riya-study", at: 858, frames: 42, src: `${V}/riya/riya-study.wav` },
  { file: "e-tagline", at: 1150, frames: 75, src: `${V}/meera/e-tagline.mp3` },
];

// Quiet, supporting effects: room tone, soft blips for messages, air on cuts.
export const SFX: Cue[] = [
  { file: "sfx2/night.wav", at: 0, volume: 0.5, dur: 254 },
  ...[8, 38, 68, 98].map((at) => ({ file: "sfx2/tick.wav", at, volume: 0.45 })),
  { file: "sfx2/tap.wav", at: BEAT.buzz, volume: 0.35 },
  ...BEAT.msg.map((at) => ({ file: "sfx2/blip.wav", at, volume: 0.35 })),
  { file: "sfx2/rewind.wav", at: BEAT.ripple - 10, volume: 0.45 },
  { file: "sfx2/class.wav", at: 254, volume: 0.45, dur: 144 },
  { file: "sfx2/air.wav", at: 394, volume: 0.35 },
  { file: "sfx2/night.wav", at: 398, volume: 0.35, dur: 394 },
  { file: "sfx2/tap.wav", at: BEAT.type, volume: 0.3 },
  { file: "sfx2/blip.wav", at: BEAT.refs, volume: 0.3 },
  { file: "sfx2/tap.wav", at: BEAT.tapRef, volume: 0.5 },
  { file: "sfx2/tap.wav", at: BEAT.mine - 4, volume: 0.35 },
  ...BEAT.replies.map((at) => ({ file: "sfx2/blip.wav", at, volume: 0.35 })),
  { file: "exam/knock-door.wav", at: BEAT.knock, volume: 0.55 },
  { file: "sfx2/air-long.wav", at: BEAT.open - 2, volume: 0.35 },
  ...BEAT.clink.map((at) => ({ file: "exam/clink.wav", at, volume: 0.22 })),
  { file: "sfx2/night.wav", at: 900, volume: 0.25, dur: 110 },
  { file: "sfx/knock.wav", at: BEAT.notify, volume: 0.6 },
  { file: "exam/bell.wav", at: BEAT.bell, volume: 0.18 },
  { file: "sfx2/page.wav", at: BEAT.paperDown, volume: 0.45 },
  { file: "exam/scribble.wav", at: BEAT.catch + 2, volume: 0.3 },
  { file: "sfx2/swell.wav", at: BEAT.band - 26, volume: 0.35 },
];

export const CAPTIONS: CaptionCue[] = [
  { from: 16, to: 124, text: "Exam at nine. And I forgot what Prof said." },
  { from: 296, to: 394, text: "Half the class was asleep. Srutam wasn't." },
  { from: BEAT.type, to: 472, text: "“What did Prof say will be on the exam?”" },
  { from: BEAT.play, to: 706, text: "▶ Econ lecture, 41:05 · “In Thursday's exam: chapters 4 and 7, and the metro case study. Twenty marks.”", top: true },
  { from: 826, to: 856, text: "“Chai's here!”" },
  { from: 858, to: 900, text: "“Come on, let's start!”" },
];
