// "Who Said That?" v2 (story frames at 30 fps; rendered at 60 fps, see stories/time.ts).
// Cold open on the outburst, then Tuesday: Arjun suggests the 21st, Rao overrules him
// himself. A week later nobody remembers; Srutam finds the moment and plays it, and
// Rao hears his own voice. Srutam never says who spoke.
import type { CaptionCue, Cue, Line } from "../stories/Story";

export const TOTAL = 1340; // 44.7 s

export const SCENES = {
  open: { from: 0, duration: 104 },
  freeze: { from: 104, duration: 42 },
  board: { from: 146, duration: 76 },
  arjun: { from: 222, duration: 82 },
  final: { from: 304, duration: 84 },
  deck: { from: 388, duration: 74 },
  note: { from: 462, duration: 46 },
  eyes: { from: 508, duration: 66 },
  dock: { from: 574, duration: 100 },
  cards: { from: 674, duration: 142 },
  week: { from: 816, duration: 56 },
  blame: { from: 872, duration: 94 },
  ask: { from: 966, duration: 114 },
  play: { from: 1080, duration: 80 },
  payoff: { from: 1160, duration: 84 },
  end: { from: 1244, duration: TOTAL - 1244 },
} as const;

export const BEAT = {
  windup: 2,
  impact: 14,
  freeze: 102,
  boardTap: 176,
  arjunHand: 228,
  shake: 310,
  chop: 360,
  pointArjun: 392,
  nodA: 432,
  thumb: 466,
  bandA: 514,
  bandN: 534,
  bandM: 554,
  tap: 604,
  cardDecision: 684,
  cardStep: 718,
  cardReminder: 752,
  lunch: 784,
  pageTear: 828,
  pointA: 886,
  pointN: 910,
  raise: 972,
  qStart: 1004,
  qEnd: 1028,
  aStart: 1032,
  aEnd: 1060,
  refs: 1062,
  tapRef: 1070,
  play: 1080,
  realise: 1118,
  rub: 1162,
  laugh: 1214,
  band: 1250,
} as const;

const V = "voiceover/meeting";
export const VO: (Line & { src: string })[] = [
  { file: "rao-who", at: 18, frames: 86, src: `${V}/rao/rao-who.wav` },
  { file: "rao-launch", at: 150, frames: 69, src: `${V}/rao/rao-launch.wav` },
  { file: "arjun-21", at: 225, frames: 77, src: `${V}/arjun/arjun-21.wav` },
  { file: "rao-final", at: 308, frames: 78, src: `${V}/rao/rao-final.wav` },
  { file: "rao-deck", at: 390, frames: 71, src: `${V}/rao/rao-deck.wav` },
  { file: "rao-note", at: 466, frames: 36, src: `${V}/rao/rao-note.wav` },
  { file: "w-nobody", at: 586, frames: 90, src: `${V}/meera/w-nobody.mp3` },
  { file: "w-decision", at: 680, frames: 132, src: `${V}/meera/w-decision.mp3` },
  { file: "arjun-her", at: 888, frames: 25, src: `${V}/arjun/arjun-her.wav` },
  { file: "neha-him", at: 912, frames: 28, src: `${V}/neha/neha-him.wav` },
  { file: "m-ask", at: 972, frames: 82, src: `${V}/meera/m-ask.mp3` },
  { file: "rao-recording", at: BEAT.play, frames: 77, src: `${V}/rao/rao-recording.wav` },
  { file: "rao-sheepish", at: 1164, frames: 74, src: `${V}/rao/rao-sheepish.wav` },
  { file: "w-minuted", at: 1262, frames: 64, src: `${V}/meera/w-minuted.mp3` },
];

// Effects sit under the story: soft air on cuts, a felt thump, quiet UI sounds.
export const SFX: Cue[] = [
  { file: "meeting/office.wav", at: 0, volume: 0.25, dur: 104 },
  { file: "sfx2/thump.wav", at: BEAT.impact, volume: 0.7 },
  { file: "sfx2/rewind.wav", at: BEAT.freeze + 4, volume: 0.5 },
  { file: "meeting/office.wav", at: 146, volume: 0.22, dur: 362 },
  { file: "sfx2/tap.wav", at: BEAT.boardTap, volume: 0.4 },
  { file: "sfx2/air.wav", at: BEAT.bandA - 4, volume: 0.35 },
  { file: "sfx2/air.wav", at: BEAT.bandN - 4, volume: 0.3 },
  { file: "sfx2/air.wav", at: BEAT.bandM - 4, volume: 0.3 },
  { file: "sfx2/tap.wav", at: BEAT.tap, volume: 0.6 },
  { file: "sfx2/blip.wav", at: BEAT.cardDecision, volume: 0.35 },
  { file: "sfx2/blip.wav", at: BEAT.cardStep, volume: 0.3 },
  { file: "sfx2/blip.wav", at: BEAT.cardReminder, volume: 0.3 },
  { file: "sfx2/tick.wav", at: 820, volume: 0.5 },
  { file: "sfx2/page.wav", at: BEAT.pageTear, volume: 0.5 },
  { file: "sfx2/tick.wav", at: 850, volume: 0.5 },
  { file: "meeting/office.wav", at: 872, volume: 0.2, dur: 468 },
  { file: "sfx2/air.wav", at: BEAT.raise, volume: 0.3 },
  { file: "sfx2/tap.wav", at: BEAT.qStart, volume: 0.35 },
  { file: "sfx2/blip.wav", at: BEAT.refs, volume: 0.3 },
  { file: "sfx2/tap.wav", at: BEAT.tapRef, volume: 0.5 },
  { file: "sfx2/swell.wav", at: BEAT.band - 26, volume: 0.35 },
];

export const CAPTIONS: CaptionCue[] = [
  { from: 18, to: 102, text: "“The 14th? Who said yes to the 14th?!”" },
  { from: 150, to: 218, text: "“So... do we launch on the 14th?”" },
  { from: 225, to: 300, text: "“Sir, the 21st would be safer...”" },
  { from: 308, to: 384, text: "“No, no. The 14th. Final.”" },
  { from: 390, to: 458, text: "“Arjun, pricing deck by Thursday.”" },
  { from: 466, to: 502, text: "“Someone note it down!”" },
  { from: 586, to: 674, text: "Nobody wrote it down. Except Srutam." },
  { from: 680, to: 812, text: "Decision. Next step. Reminder. Before lunch." },
  { from: 888, to: 911, text: "“Neha did!”" },
  { from: 912, to: 944, text: "“Arjun did!”" },
  { from: 972, to: 1054, text: "“Relax. Let's ask Srutam.”" },
  { from: BEAT.play, to: 1154, text: "▶ Tuesday sync, 0:42 · “No, no. The 14th. Final.”", top: true },
  { from: 1164, to: 1238, text: "“Okay... so the 14th is final, then.”" },
];
