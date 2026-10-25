// "Who was doing slides 8-12?" (story frames at 30 fps; rendered at 60 fps, see
// stories/time.ts). 1:43 a.m., presentation at nine, nobody did slides 8-12. Last
// Tuesday in the canteen Meera recorded the group meeting, where Riya said "And Kabir,
// you'll do eight to twelve." Srutam AI finds that line by its words (it doesn't know
// who's speaking), plays it, and Kabir is on it till 4 a.m.
// scripts/music-slides.py mirrors these times.
import type { CaptionCue, Cue, Line } from "../stories/Story";

export const TOTAL = 1140; // 38 s

export const SCENES = {
  chat: { from: 0, duration: 195 },
  flash: { from: 195, duration: 355 },
  ask: { from: 550, duration: 130 },
  play: { from: 680, duration: 150 },
  share: { from: 830, duration: 105 },
  kabir: { from: 935, duration: TOTAL - 935 },
} as const;

export const BEAT = {
  msg: [10, 40, 92, 116, 152],
  kabirTyping: 128,
  recTap: 216,
  intro: 357, // "Meera, intro."
  oneToSeven: 380, // "I'll do one to seven."
  kabirName: 420, // "And Kabir..."
  eightTwelve: 432, // "...you'll do eight to twelve."
  kabirLooks: 424,
  easy: 494,
  typeStart: 558,
  send: 590,
  answer: 602,
  answerEnd: 636,
  tapRef: 662,
  play: 692,
  playEasy: 692 + 82,
  mine: 836,
  replies: [860, 908],
  kabirTyping2: 868,
  canteen: 975,
  band: 1060,
} as const;

const V = "voiceover/en/slides";
export const VO: (Line & { src: string })[] = [
  { file: "s-record", at: 206, frames: 104, src: `${V}/meera/s-record.mp3` },
  { file: "s-assign", at: 318, frames: 170, src: `${V}/riya/s-assign.mp3` },
  { file: "s-easy", at: BEAT.easy, frames: 49, src: `${V}/kabir/s-easy.mp3` },
  { file: "playback", at: BEAT.play, frames: 127, src: `${V}/playback.wav` },
  { file: "s-canteen", at: BEAT.canteen, frames: 64, src: `${V}/kabir/s-canteen.mp3` },
];

// Quiet, supporting effects: room tone, blips for messages, canteen murmur.
export const SFX: Cue[] = [
  { file: "sfx2/night.wav", at: 0, volume: 0.45, dur: 195 },
  ...BEAT.msg.map((at) => ({ file: "sfx2/blip.wav", at, volume: 0.35 })),
  { file: "sfx2/rewind.wav", at: 185, volume: 0.4 },
  { file: "sfx2/class.wav", at: 195, volume: 0.32, dur: 355 },
  { file: "sfx2/tap.wav", at: BEAT.recTap, volume: 0.45 },
  { file: "exam/scribble.wav", at: BEAT.kabirName + 4, volume: 0.25 },
  { file: "sfx2/air.wav", at: 546, volume: 0.3 },
  { file: "sfx2/night.wav", at: 550, volume: 0.32, dur: 385, loop: true },
  ...Array.from({ length: 8 }, (_, i) => ({ file: "sfx2/tick.wav", at: BEAT.typeStart + 2 + i * 3.4, volume: 0.22 })),
  { file: "sfx2/tap.wav", at: BEAT.send, volume: 0.4 },
  { file: "sfx2/blip.wav", at: BEAT.answer, volume: 0.3 },
  { file: "sfx2/tap.wav", at: BEAT.tapRef, volume: 0.45 },
  { file: "sfx2/tap.wav", at: BEAT.mine - 4, volume: 0.35 },
  ...BEAT.replies.map((at) => ({ file: "sfx2/blip.wav", at, volume: 0.35 })),
  { file: "sfx2/night.wav", at: 935, volume: 0.3, dur: 205 },
  ...Array.from({ length: 14 }, (_, i) => ({ file: "sfx2/tick.wav", at: 942 + i * 2.6, volume: 0.16 })),
  { file: "sfx2/swell.wav", at: BEAT.band - 26, volume: 0.35 },
];

export const CAPTIONS: CaptionCue[] = [
  { from: 206, to: 312, text: "“Recording this. Nobody remembers anything anyway.”" },
  { from: 318, to: 418, text: "“Okay. Meera, intro. I'll do one to seven.”" },
  { from: 420, to: 490, text: "“And Kabir, you'll do eight to twelve.”" },
  { from: BEAT.easy, to: 545, text: "“Done. Easy.”" },
  { from: BEAT.play, to: 822, text: "▶ Group meeting · “And Kabir, you'll do eight to twelve.” … “Done. Easy.”", top: true },
];
