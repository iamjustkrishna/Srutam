// "The 2 a.m. Idea": every scene, beat and cue in frames (30 fps, 1080x1920).
// scripts/generate_audio_bedtime.py mirrors the music cue points.
export const FPS = 30;
export const W = 1080;
export const H = 1920;

export const SCENES = {
  night1: { from: 0, duration: 165 }, // 2:13 a.m., the idea
  remember: { from: 165, duration: 90 }, // "I'll remember it in the morning"
  morning1: { from: 255, duration: 90 }, // forgot
  rewind: { from: 345, duration: 75 }, // VHS rewind
  night2: { from: 420, duration: 300 }, // tap Start, whisper
  save: { from: 720, duration: 100 }, // Save, back to sleep
  morning2: { from: 820, duration: 180 }, // it's all there
  later: { from: 1000, duration: 85 }, // three months later
  end: { from: 1085, duration: 115 }, // brand
} as const;

export const TOTAL = SCENES.end.from + SCENES.end.duration; // 1200 = 40 s

// Voiceover cue points (absolute frames) and burned-in captions
export const VO = [
  {
    file: "b1-idea",
    at: 6,
    frames: 145,
    caption: "2:13 a.m. The best idea I've ever had.",
  },
  {
    file: "b2-remember",
    at: 170,
    frames: 58,
    caption: "I'll remember it in the morning.",
  },
  {
    file: "b3-forgot",
    at: 272,
    frames: 72,
    caption: "I did not remember it in the morning.",
  },
  {
    file: "b4-rewind",
    at: 352,
    frames: 60,
    caption: "Okay. Let's try that again.",
  },
  {
    file: "b5-whisper",
    at: 462,
    frames: 256,
    caption:
      "Rooftop café, for book lovers... swap a book, get a chai. Call Priya about her terrace... Saturday.",
  },
  {
    file: "b6-sleep",
    at: 726,
    frames: 95,
    caption: "Tap. Say it. Back to sleep.",
  },
  {
    file: "b7-morning",
    at: 832,
    frames: 170,
    caption:
      "And in the morning, it's all there. The idea, the next step... even a reminder.",
  },
  { file: "b8-later", at: 1006, frames: 36, caption: "Three months later." },
  { file: "b9-end", at: 1092, frames: 81, caption: "" },
] as const;

// Beats (absolute frames), synced to word timings in the voiceover
export const BEAT = {
  eyesOpen: 12,
  bulbOn: 88, // "best idea"
  sleep: 215,
  alarm: 258,
  bulbPop: 290, // "did not remember"
  grabPhone: 430,
  tapStart: 455,
  bulbIntoPhone: 470,
  tapSave: 730, // "Tap."
  saved: 760, // "Say it."
  backToSleep: 788, // "Back to sleep"
  noteIn: 862, // "it's all there"
  ideaCard: 910, // "the idea"
  stepCard: 935, // "the next step"
  reminderCard: 952, // knock-knock (the app's reminder sound), in the pause before "even a reminder"
} as const;
