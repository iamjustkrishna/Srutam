// "The Brain Court", episode 1 (story frames at 30 fps; rendered at 60). Inside Aarav's head
// a courtroom tries Memory for losing the deadline his manager gave on the 3 p.m. call;
// Srutam walks in with the recording. Art: public/court/cNN(.b).jpg (see SCRIPT.md).
// scripts/court-audio.py mirrors these times.
import type { CaptionCue, Cue, Line } from "../stories/Story";

export const TOTAL = 1740; // 58 s

export const SHOTS = {
  gavel: { from: 0, duration: 84 }, // 1 "ORDER! Who lost the deadline?!"
  aarav: { from: 84, duration: 72 }, // 2 Aarav frozen, push into his head
  wide: { from: 156, duration: 40 }, // 3 the court inside the pink dome
  anxiety: { from: 196, duration: 170 }, // 4 Anxiety accuses Memory
  memory: { from: 366, duration: 220 }, // 5 Memory's defence
  judge: { from: 586, duration: 76 }, // 6 "Call the witnesses."
  overconf: { from: 662, duration: 188 }, // 7 "Bro. I'll remember it."
  sleep: { from: 850, duration: 110 }, // 8 "Was it… Friday?"
  spiral: { from: 960, duration: 130 }, // 9 "WHAT IF WE'RE ALREADY FIRED?!"
  doors: { from: 1090, duration: 50 }, // 10 "Objection."
  srutam: { from: 1140, duration: 60 }, // 11 "I have the recording."
  proof: { from: 1200, duration: 160 }, // 12 the recording plays
  react: { from: 1360, duration: 70 }, // 13 reactions
  closed: { from: 1430, duration: 120 }, // 14 "Case closed. Next time… record it."
  relief: { from: 1550, duration: 70 }, // 15 Aarav exhales, gets to work
  cta: { from: 1620, duration: TOTAL - 1620 }, // call to action
} as const;

export const BEAT = {
  slam: 8,
  push: 138,
  ask: 1204,
  answer: 1232,
  play: 1244,
  slam2: 1535,
  knock: 1566, // Srutam's knock-knock on Aarav's desk phone
  sent: 1592,
} as const;

const V = "voiceover/en/court";
export const VO: (Line & { src: string })[] = [
  { file: "c-order", at: 6, frames: 76, src: `${V}/judge/c-order.mp3` },
  { file: "c-what", at: 96, frames: 66, src: `${V}/aarav/c-what.mp3` },
  { file: "c-accuse", at: 202, frames: 159, src: `${V}/anxiety/c-accuse.mp3` },
  { file: "c-defend", at: 372, frames: 208, src: `${V}/memory/c-defend.mp3` },
  { file: "c-witnesses", at: 606, frames: 51, src: `${V}/judge/c-witnesses.mp3` },
  { file: "c-bro", at: 668, frames: 176, src: `${V}/overconf/c-bro.mp3` },
  { file: "c-friday", at: 856, frames: 95, src: `${V}/sleep/c-friday.mp3` },
  { file: "c-spiral", at: 966, frames: 122, src: `${V}/anxiety/c-spiral.mp3` },
  { file: "c-objection", at: 1098, frames: 32, src: `${V}/srutam/c-objection.mp3` },
  { file: "c-recording", at: 1146, frames: 47, src: `${V}/srutam/c-recording.mp3` },
  { file: "c-deadline", at: BEAT.play, frames: 111, src: `${V}/manager/c-deadline-phone.mp3` },
  { file: "c-told", at: 1382, frames: 39, src: `${V}/overconf/c-told.mp3` },
  { file: "c-closed", at: 1434, frames: 104, src: `${V}/judge/c-closed.mp3` },
  { file: "c-cta", at: 1628, frames: 75, src: `${V}/srutam/c-cta.mp3` },
];

export const SFX: Cue[] = [
  { file: "court/gavel.wav", at: BEAT.slam, volume: 0.7 },
  { file: "court/clock.wav", at: 86, volume: 0.35 },
  { file: "sfx2/air-long.wav", at: BEAT.push - 4, volume: 0.45 },
  { file: "sfx2/class.wav", at: 156, volume: 0.22, dur: 430 },
  { file: "court/boing.wav", at: 300, volume: 0.25 },
  { file: "sfx2/page.wav", at: 380, volume: 0.35 },
  { file: "sfx2/page.wav", at: 470, volume: 0.3 },
  { file: "court/laugh.wav", at: 820, volume: 0.25 },
  { file: "court/rings.wav", at: 962, volume: 0.35 },
  { file: "court/scratch.wav", at: 1091, volume: 0.6 },
  { file: "sfx/shimmer.wav", at: 1094, volume: 0.45 },
  { file: "sfx/chime.wav", at: 1148, volume: 0.3 },
  ...Array.from({ length: 9 }, (_, i) => ({ file: "sfx2/tick.wav", at: BEAT.ask + 2 + i * 1.7, volume: 0.2 })),
  // Srutam's notification is the app's own knock-knock (app/src/main/res/raw/srutam_knock.wav)
  { file: "sfx/knock.wav", at: BEAT.answer - 2, volume: 0.55 },
  { file: "sfx2/tap.wav", at: BEAT.play - 3, volume: 0.45 },
  { file: "court/boing.wav", at: 1366, volume: 0.35 },
  { file: "court/gavel.wav", at: BEAT.slam2, volume: 0.6 },
  { file: "court/typing.wav", at: 1558, volume: 0.4 },
  { file: "sfx/knock.wav", at: BEAT.knock, volume: 0.6 },
  { file: "sfx2/tap.wav", at: BEAT.sent, volume: 0.4 },
  { file: "sfx/whoosh.wav", at: 1616, volume: 0.35 },
];

export const CAPTIONS: CaptionCue[] = [
  { from: 6, to: 80, text: "“ORDER! Who lost the deadline?!”" },
  { from: 96, to: 150, text: "“What did she say the deadline was…?”" },
  { from: 202, to: 362, text: "“Your Honour! At 3 p.m. the manager SAID the deadline. And THIS one… lost it!”" },
  { from: 372, to: 582, text: "“I had it! It was right here, between a song lyric and that embarrassing thing from 2014.”" },
  { from: 606, to: 658, text: "“Call the witnesses.”" },
  { from: 668, to: 846, text: "“Bro. I told him ‘I'll remember it.’ That's… that's on me.”" },
  { from: 856, to: 952, text: "“Was it… Friday? Or…”" },
  { from: 966, to: 1088, text: "“What if it's TOMORROW? What if it was TODAY? WHAT IF WE'RE ALREADY FIRED?!”" },
  { from: 1098, to: 1132, text: "“Objection.”" },
  { from: 1146, to: 1196, text: "“I have the recording.”" },
  { from: 1382, to: 1424, text: "“Told you I'd remember.”" },
  { from: 1434, to: 1540, text: "“Case closed. Next time… record it.”" },
];
