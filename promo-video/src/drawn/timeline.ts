// "Who was doing slides 8-12?", AI-illustrated cut (story frames at 30 fps; rendered at 60).
// Ten Canva illustrations (public/drawn/sNN.jpg, see SHOTS.md) are animated with camera
// moves; the phone screens are built in code. scripts/music-drawn.py mirrors these times.
import type { CaptionCue, Cue, Line } from "../stories/Story";

export const TOTAL = 1320; // 44 s

export const SHOTS = {
  s1: { from: 0, duration: 96 }, // 11 p.m., done for the night
  s2: { from: 96, duration: 135 }, // 1:43 a.m., "where are slides 8-12??"
  s3a: { from: 231, duration: 90 }, // Riya, furious
  s3b: { from: 321, duration: 75 }, // Kabir, "not me bro"
  s4: { from: 396, duration: 75 }, // nightmare: the red zero
  s5: { from: 471, duration: 120 }, // 2:30 a.m., doing it alone
  s7: { from: 591, duration: 75 }, // she remembers the recording
  ask: { from: 666, duration: 150 }, // asks Srutam
  s6: { from: 816, duration: 144 }, // canteen recording plays
  share: { from: 960, duration: 90 }, // shared to the group
  s8: { from: 1050, duration: 90 }, // Kabir, 4 a.m.
  s9: { from: 1140, duration: TOTAL - 1140 }, // presentation, "Recording this."
} as const;

export const BEAT = {
  banner11: [14, 40],
  banner143: 104,
  bannerKabir: 328,
  typeStart: 680,
  send: 724,
  answer: 736,
  answerEnd: 784,
  tapRef: 800,
  play: 826,
  replies: [978, 1010, 1028],
  band: 1250,
} as const;

const V = "voiceover/en";
export const VO: (Line & { src: string })[] = [
  { file: "d-gasp", at: 140, frames: 68, src: `${V}/drawn/meera/d-gasp.mp3` },
  { file: "d-who", at: 240, frames: 107, src: `${V}/drawn/riya/d-who.mp3` },
  { file: "d-notme", at: 352, frames: 36, src: `${V}/drawn/kabir/d-notme.mp3` },
  { file: "d-myself", at: 500, frames: 85, src: `${V}/drawn/meera/d-myself.mp3` },
  { file: "d-wait", at: 602, frames: 73, src: `${V}/drawn/meera/d-wait.mp3` },
  { file: "playback", at: BEAT.play, frames: 127, src: `${V}/slides/playback.wav` },
  { file: "s-canteen", at: 1064, frames: 64, src: `${V}/slides/kabir/s-canteen.mp3` },
  { file: "d-recording", at: 1196, frames: 35, src: `${V}/drawn/kabir/d-recording.mp3` },
];

export const SFX: Cue[] = [
  { file: "sfx2/night.wav", at: 0, volume: 0.35, dur: 96 },
  ...BEAT.banner11.map((at) => ({ file: "sfx2/blip.wav", at, volume: 0.32 })),
  { file: "sfx2/thump.wav", at: 96, volume: 0.6 },
  { file: "sfx2/blip.wav", at: BEAT.banner143, volume: 0.4 },
  { file: "sfx2/night.wav", at: 100, volume: 0.3, dur: 290, loop: true },
  { file: "sfx2/blip.wav", at: BEAT.bannerKabir, volume: 0.35 },
  { file: "sfx2/thump.wav", at: 396, volume: 0.55 },
  { file: "sfx2/air-long.wav", at: 471, volume: 0.25 },
  { file: "sfx2/night.wav", at: 471, volume: 0.3, dur: 345, loop: true },
  ...Array.from({ length: 10 }, (_, i) => ({ file: "sfx2/tick.wav", at: BEAT.typeStart + 2 + i * 4, volume: 0.22 })),
  { file: "sfx2/tap.wav", at: BEAT.send, volume: 0.4 },
  { file: "sfx2/blip.wav", at: BEAT.answer, volume: 0.3 },
  { file: "sfx2/tap.wav", at: BEAT.tapRef, volume: 0.45 },
  { file: "sfx2/rewind.wav", at: 808, volume: 0.35 },
  { file: "sfx2/class.wav", at: 816, volume: 0.22, dur: 144 },
  ...BEAT.replies.map((at) => ({ file: "sfx2/blip.wav", at, volume: 0.35 })),
  { file: "sfx2/night.wav", at: 1050, volume: 0.28, dur: 90 },
  ...Array.from({ length: 12 }, (_, i) => ({ file: "sfx2/tick.wav", at: 1052 + i * 2.6, volume: 0.15 })),
  { file: "sfx2/class.wav", at: 1140, volume: 0.18, dur: 110 },
  { file: "sfx2/swell.wav", at: BEAT.band - 24, volume: 0.35 },
];

export const CAPTIONS: CaptionCue[] = [
  { from: 140, to: 210, text: "“No. No no no no no.”" },
  { from: 240, to: 318, text: "“Who. Was doing. Eight to twelve?”" },
  { from: 352, to: 392, text: "“…not me?”" },
  { from: 500, to: 586, text: "“Fine. I'll do them myself.”" },
  { from: 602, to: 664, text: "“Wait. I recorded that meeting.”" },
  { from: BEAT.play, to: 955, text: "▶ Group meeting · Tue — “And Kabir, you'll do eight to twelve.” … “Done. Easy.”", top: true },
  { from: 1064, to: 1130, text: "“Who records a canteen?!”" },
  { from: 1196, to: 1240, text: "“Recording this.”" },
];
