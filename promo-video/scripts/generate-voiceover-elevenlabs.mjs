// Generate the voiceover with ElevenLabs (Eleven v4, audio tags).
//
// The whole script is generated in ONE request so the read has a continuous,
// natural performance, then cut into one file per line using the character
// timestamps the API returns. File names match src/timeline.ts.
//
// Usage:
//   ELEVENLABS_API_KEY=... node scripts/generate-voiceover-elevenlabs.mjs [voiceId] [--audition]
//
// --audition renders only the hook line to public/voiceover/auditions/<voice>.mp3
// so voices can be compared cheaply.

import { execFileSync } from "node:child_process";
import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const KEY = process.env.ELEVENLABS_API_KEY;
if (!KEY) throw new Error("Set ELEVENLABS_API_KEY");

const voiceId = process.argv[2] ?? "JBFqnCBsd6RMkjVDRZzb"; // George - warm, captivating storyteller
const audition = process.argv.includes("--audition");
const MODEL = "eleven_v4";

// Audio tags in [brackets] direct the performance; they are not spoken.
// "Shrutam" is spelled for pronunciation (Sanskrit: shroo-tum).
const LINES = [
  ["v1-hook", "[hushed, intriguing] This idea... has four seconds to live."],
  ["v2-lost", "[sighs] Most never make it."],
  ["v3-tap", "[quick, playful] Tap, tap. From any app."],
  ["v4-caught", "[relieved] Caught. [warm] And transcribed, right on your phone."],
  ["v5-insights", "[confident] Shrutam pulls out your next steps, ideas, and decisions."],
  ["v6-connect", "[wonder] Every thought connects. [smiling] Just ask."],
  ["v7-end", "[warm, charismatic] Shrutam. Catch it before it's gone."],
];

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const outDir = join(root, "public", "voiceover");

const lines = audition ? LINES.slice(0, 1) : LINES;
const SEP = "\n\n";
const text = lines.map(([, t]) => t).join(SEP);

const res = await fetch(
  `https://api.elevenlabs.io/v1/text-to-speech/${voiceId}/with-timestamps?output_format=mp3_44100_128`,
  {
    method: "POST",
    headers: { "xi-api-key": KEY, "Content-Type": "application/json" },
    body: JSON.stringify({
      text,
      model_id: MODEL,
      // "Natural" on the v3/v4 stability scale: expressive but on-script.
      voice_settings: { stability: 0.5, similarity_boost: 0.8 },
    }),
  },
);
if (!res.ok) throw new Error(`${res.status} ${await res.text()}`);
const json = await res.json();
const audio = Buffer.from(json.audio_base64, "base64");

if (audition) {
  const dir = join(outDir, "auditions");
  mkdirSync(dir, { recursive: true });
  writeFileSync(join(dir, `${voiceId}.mp3`), audio);
  console.log(`audition written for ${voiceId}`);
  process.exit(0);
}

const full = join(outDir, "full-take.mp3");
mkdirSync(outDir, { recursive: true });
writeFileSync(full, audio);

const align = json.normalized_alignment ?? json.alignment;
const starts = align.character_start_times_seconds;
const ends = align.character_end_times_seconds;
const chars = align.characters.join("");

// Find each line's span in the aligned text and cut halfway into the pauses.
let cursor = 0;
const spans = lines.map(([name, t]) => {
  const spoken = t.replace(/\[[^\]]*\]\s*/g, "").trim();
  const probe = spoken.slice(0, 12);
  let i = chars.indexOf(probe, cursor);
  if (i < 0) i = chars.indexOf(t.slice(0, 12), cursor);
  if (i < 0) throw new Error(`Could not align "${name}"`);
  const lastWord = spoken.slice(-6);
  let j = chars.indexOf(lastWord, i);
  j = j < 0 ? i + spoken.length - 1 : j + lastWord.length - 1;
  cursor = j + 1;
  return { name, start: starts[i], end: ends[Math.min(j, ends.length - 1)] };
});

const durations = {};
spans.forEach((s, k) => {
  const prevEnd = k > 0 ? spans[k - 1].end : 0;
  const nextStart = k < spans.length - 1 ? spans[k + 1].start : s.end + 0.6;
  const from = Math.max(0, (prevEnd + s.start) / 2, s.start - 0.15);
  const to = Math.min((s.end + nextStart) / 2, s.end + 0.35);
  execFileSync("ffmpeg", [
    "-y", "-loglevel", "error", "-i", full,
    "-ss", from.toFixed(3), "-to", to.toFixed(3),
    "-af", "afade=t=in:d=0.02,areverse,afade=t=in:d=0.08,areverse",
    join(outDir, `${s.name}.mp3`),
  ]);
  durations[s.name] = +(to - from).toFixed(3);
  console.log(`${s.name}: ${from.toFixed(2)}-${to.toFixed(2)}s (${durations[s.name]}s)`);
});
writeFileSync(join(outDir, "durations.json"), JSON.stringify(durations, null, 2) + "\n");
