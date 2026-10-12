// Single source of truth for the 30 s cut (30 fps). scripts/generate_audio.py
// mirrors the music cue points below.
export const FPS = 30;
export const WIDTH = 1080;
export const HEIGHT = 1920;

export const SCENES = {
  hook: { from: 0, duration: 110 },
  lost: { from: 110, duration: 90 },
  catch: { from: 200, duration: 270 },
  insights: { from: 470, duration: 170 },
  connect: { from: 640, duration: 150 },
  end: { from: 790, duration: 110 },
} as const;

export const TOTAL = SCENES.end.from + SCENES.end.duration; // 900

// Beats inside the catch scene (frames relative to the scene start)
export const CATCH = {
  dockGlow: 22,
  tap1: 45, // "Tap" - the dock tab expands
  tap2: 51, // "tap" - the red mic starts recording
  dive: 54, // spark dives into the phone
  words: 92, // live transcript starts ("Caught. And transcribed...")
  stop: 200, // tap stop
  saved: 212, // notification flips to "Saved"
} as const;

// Voiceover cue points (absolute frames). Word timings come from the
// ElevenLabs take: "Tap" lands at +5 frames, "tap" at +11, "Just ask" at +58.
export const VO = [
  { file: "v1-hook", at: 4 },
  { file: "v2-lost", at: 120 },
  { file: "v3-tap", at: 240 },
  { file: "v4-caught", at: 286 },
  { file: "v5-insights", at: 488 },
  { file: "v6-connect", at: 650 },
  { file: "v7-end", at: 806 },
] as const;

// Music cue points (absolute frames), mirrored in scripts/generate_audio.py
export const MUSIC = {
  dropStart: 228,
  caught: 251,
  night: 640,
  end: 790,
} as const;
