// "2 a.m. Before the Exam" in 16:9 with English dialogue (story frames at 30 fps,
// rendered at 60 fps). Same story and beats as src/exam/timeline.ts; the lecture, the
// playback and the door run longer for the English lines, and every later beat moves
// with its scene. scripts/music-wide.py times the music bed to these scenes.
import type { CaptionCue, Cue, Line } from "../../stories/Story";

export const TOTAL = 1364; // 45.5 s

export const SCENES = {
  night: { from: 0, duration: 132 },
  chat: { from: 132, duration: 82 },
  guilt: { from: 214, duration: 40 },
  start: { from: 254, duration: 46 },
  lecture: { from: 300, duration: 100 },
  insert: { from: 400, duration: 40 },
  type: { from: 440, duration: 120 },
  detail: { from: 560, duration: 36 },
  listen: { from: 596, duration: 202 },
  share: { from: 798, duration: 80 },
  door: { from: 878, duration: 146 },
  lapse: { from: 1024, duration: 110 },
  remind: { from: 1134, duration: 30 },
  hall: { from: 1164, duration: 90 },
  end: { from: 1254, duration: TOTAL - 1254 },
} as const;

export const BEAT = {
  handHead: 60,
  buzz: 118,
  msg: [146, 168, 190],
  realise: 236,
  ripple: 254,
  startTap: 280,
  chalk: 330,
  typeStart: 450,
  send: 500,
  answer: 516,
  answerEnd: 536,
  tapRef: 548,
  seek: 572,
  playBtn: 586,
  play: 596,
  // Word onsets in prof-recording: "chapters four and seven", "metro case study", "Twenty marks".
  chapters: 596 + 49,
  metro: 596 + 115,
  marks: 596 + 164,
  relief: 714,
  yes: 762,
  mine: 804,
  replies: [830, 850],
  knock: 884,
  open: 898,
  riyaIn: 940,
  cup: 1058,
  clink: [1070, 1096],
  notify: 1138,
  bell: 1168,
  paperDown: 1176,
  circle: 1192,
  nod: 1200,
  spin: 1206,
  catch: 1226,
  band: 1260,
} as const;

const V = "voiceover/en/exam";
const M = "voiceover/exam/meera";
export const VO: (Line & { src: string })[] = [
  { file: "e-forgot", at: 16, frames: 108, src: `${M}/e-forgot.mp3` },
  { file: "prof-lecture", at: 262, frames: 94, src: `${V}/prof/prof-lecture.wav` },
  { file: "e-asleep", at: 318, frames: 98, src: `${M}/e-asleep.mp3` },
  { file: "e-ask", at: 444, frames: 68, src: `${M}/e-ask.mp3` },
  { file: "prof-recording", at: BEAT.play, frames: 198, src: `${V}/prof/prof-recording.wav` },
  { file: "kabir-chai", at: 912, frames: 41, src: `${V}/kabir/kabir-chai.wav` },
  { file: "riya-study", at: 958, frames: 60, src: `${V}/riya/riya-study.wav` },
  { file: "e-tagline", at: 1274, frames: 75, src: `${M}/e-tagline.mp3` },
];

// Quiet, supporting effects: room tone, soft blips for messages, air on cuts.
export const SFX: Cue[] = [
  { file: "sfx2/night.wav", at: 0, volume: 0.5, dur: 254 },
  ...[8, 38, 68, 98].map((at) => ({ file: "sfx2/tick.wav", at, volume: 0.45 })),
  { file: "sfx2/tap.wav", at: BEAT.buzz, volume: 0.35 },
  ...BEAT.msg.map((at) => ({ file: "sfx2/blip.wav", at, volume: 0.35 })),
  { file: "sfx2/rewind.wav", at: BEAT.ripple - 10, volume: 0.45 },
  { file: "sfx2/class.wav", at: 254, volume: 0.45, dur: 186 },
  { file: "sfx2/tap.wav", at: BEAT.startTap, volume: 0.45 },
  { file: "exam/chalk.wav", at: BEAT.chalk, volume: 0.25 },
  { file: "sfx2/air.wav", at: 436, volume: 0.3 },
  { file: "sfx2/night.wav", at: 440, volume: 0.35, dur: 438, loop: true },
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
  { file: "sfx2/night.wav", at: 1024, volume: 0.25, dur: 110 },
  { file: "sfx2/class.wav", at: 1134, volume: 0.22, dur: BEAT.bell + 4 - 1134 },
  { file: "sfx/knock.wav", at: BEAT.notify, volume: 0.6 },
  { file: "exam/bell.wav", at: BEAT.bell, volume: 0.18 },
  { file: "sfx2/page.wav", at: BEAT.paperDown, volume: 0.45 },
  { file: "exam/scribble.wav", at: BEAT.catch + 2, volume: 0.3 },
  { file: "sfx2/swell.wav", at: BEAT.band - 26, volume: 0.35 },
];

export const CAPTIONS: CaptionCue[] = [
  { from: 16, to: 124, text: "Exam at nine. And I forgot what Prof said." },
  { from: 262, to: 314, text: "“So, in Thursday's exam...”" },
  { from: 318, to: 416, text: "Half the class was asleep. Srutam wasn't." },
  { from: 444, to: 512, text: "“What did Prof say will be on the exam?”" },
  { from: BEAT.play, to: 790, text: "▶ Econ lecture · “In Thursday's exam: chapters 4 and 7, and the metro case study. Twenty marks.”", top: true },
  { from: 912, to: 953, text: "“Chai's here!”" },
  { from: 958, to: 1018, text: "“Come on, let's start!”" },
];
