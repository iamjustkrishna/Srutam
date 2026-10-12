// "2 a.m. Before the Exam" v2 (story frames at 30 fps; rendered at 60 fps, see
// stories/time.ts). The group chat asks Meera, who slept through the lecture too;
// Srutam didn't. The flashback only half-hears Prof, so the line lands when Srutam
// plays the lecture back. scripts/generate_audio_stories.py mirrors these times.
import type { CaptionCue, Cue, Line } from "../stories/Story";

export const TOTAL = 1296; // 43.2 s

export const SCENES = {
  night: { from: 0, duration: 132 },
  chat: { from: 132, duration: 82 },
  guilt: { from: 214, duration: 40 },
  start: { from: 254, duration: 46 },
  lecture: { from: 300, duration: 80 },
  insert: { from: 380, duration: 40 },
  type: { from: 420, duration: 120 },
  detail: { from: 540, duration: 36 },
  listen: { from: 576, duration: 192 },
  share: { from: 768, duration: 80 },
  door: { from: 848, duration: 108 },
  lapse: { from: 956, duration: 110 },
  remind: { from: 1066, duration: 30 },
  hall: { from: 1096, duration: 90 },
  end: { from: 1186, duration: TOTAL - 1186 },
} as const;

export const BEAT = {
  handHead: 60,
  buzz: 118,
  msg: [146, 168, 190],
  realise: 236,
  ripple: 254,
  startTap: 280,
  typeStart: 430,
  send: 480,
  answer: 496,
  answerEnd: 516,
  tapRef: 528,
  seek: 552,
  playBtn: 566,
  play: 576,
  relief: 690,
  yes: 742,
  mine: 774,
  replies: [800, 820],
  knock: 854,
  open: 868,
  riyaIn: 896,
  clink: [1002, 1028],
  notify: 1070,
  bell: 1100,
  paperDown: 1108,
  circle: 1124,
  nod: 1132,
  spin: 1138,
  catch: 1158,
  band: 1192,
} as const;

const V = "voiceover/exam";
export const VO: (Line & { src: string })[] = [
  { file: "e-forgot", at: 16, frames: 108, src: `${V}/meera/e-forgot.mp3` },
  { file: "prof-lecture", at: 270, frames: 66, src: `${V}/prof/prof-lecture.wav` },
  { file: "e-asleep", at: 304, frames: 98, src: `${V}/meera/e-asleep.mp3` },
  { file: "e-ask", at: 424, frames: 68, src: `${V}/meera/e-ask.mp3` },
  { file: "prof-recording", at: BEAT.play, frames: 186, src: `${V}/prof/prof-recording.wav` },
  { file: "kabir-chai", at: 882, frames: 30, src: `${V}/kabir/kabir-chai.wav` },
  { file: "riya-study", at: 914, frames: 42, src: `${V}/riya/riya-study.wav` },
  { file: "e-tagline", at: 1206, frames: 75, src: `${V}/meera/e-tagline.mp3` },
];

// Quiet, supporting effects: room tone, soft blips for messages, air on cuts.
export const SFX: Cue[] = [
  { file: "sfx2/night.wav", at: 0, volume: 0.5, dur: 254 },
  ...[8, 38, 68, 98].map((at) => ({ file: "sfx2/tick.wav", at, volume: 0.45 })),
  { file: "sfx2/tap.wav", at: BEAT.buzz, volume: 0.35 },
  ...BEAT.msg.map((at) => ({ file: "sfx2/blip.wav", at, volume: 0.35 })),
  { file: "sfx2/rewind.wav", at: BEAT.ripple - 10, volume: 0.45 },
  { file: "sfx2/class.wav", at: 254, volume: 0.45, dur: 166 },
  { file: "sfx2/tap.wav", at: BEAT.startTap, volume: 0.45 },
  { file: "sfx2/air.wav", at: 416, volume: 0.3 },
  { file: "sfx2/night.wav", at: 420, volume: 0.35, dur: 428 },
  ...Array.from({ length: 10 }, (_, i) => ({ file: "sfx2/tick.wav", at: BEAT.typeStart + 2 + i * 3.4, volume: 0.22 })),
  { file: "sfx2/tap.wav", at: BEAT.send, volume: 0.4 },
  { file: "sfx2/blip.wav", at: BEAT.answer, volume: 0.3 },
  { file: "sfx2/tap.wav", at: BEAT.tapRef, volume: 0.45 },
  { file: "sfx2/tap.wav", at: BEAT.seek, volume: 0.4 },
  { file: "sfx2/tap.wav", at: BEAT.playBtn, volume: 0.4 },
  { file: "sfx2/tap.wav", at: BEAT.mine - 4, volume: 0.35 },
  ...BEAT.replies.map((at) => ({ file: "sfx2/blip.wav", at, volume: 0.35 })),
  { file: "exam/knock-door.wav", at: BEAT.knock, volume: 0.55 },
  { file: "sfx2/air-long.wav", at: BEAT.open - 2, volume: 0.35 },
  ...BEAT.clink.map((at) => ({ file: "exam/clink.wav", at, volume: 0.22 })),
  { file: "sfx2/night.wav", at: 956, volume: 0.25, dur: 110 },
  { file: "sfx/knock.wav", at: BEAT.notify, volume: 0.6 },
  { file: "exam/bell.wav", at: BEAT.bell, volume: 0.18 },
  { file: "sfx2/page.wav", at: BEAT.paperDown, volume: 0.45 },
  { file: "exam/scribble.wav", at: BEAT.catch + 2, volume: 0.3 },
  { file: "sfx2/swell.wav", at: BEAT.band - 26, volume: 0.35 },
];

export const CAPTIONS: CaptionCue[] = [
  { from: 16, to: 124, text: "Exam at nine. And I forgot what Prof said." },
  { from: 304, to: 402, text: "Half the class was asleep. Srutam wasn't." },
  { from: 424, to: 492, text: "“What did Prof say will be on the exam?”" },
  { from: BEAT.play, to: 762, text: "▶ Econ lecture · “In Thursday's exam: chapters 4 and 7, and the metro case study. Twenty marks.”", top: true },
  { from: 882, to: 912, text: "“Chai's here!”" },
  { from: 914, to: 956, text: "“Come on, let's start!”" },
];
