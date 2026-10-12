# Srutam promo: "The Spark" (30 s animatic)

A story-driven vertical (1080x1920) promo built with [Remotion](https://remotion.dev).
The hero is **Spark**, a thought that dies within seconds unless Srutam catches it.

| Time | Beat | What the app shows |
|---|---|---|
| 0:00 | Hook: "This idea has 4 seconds to live." | - |
| 0:03.7 | Ideas dying in the shower, driving, mid-meeting | - |
| 0:06.7 | Tap, tap: the floating dock, from any app | Floating dock (collapsed, expanded, recording), "Transcribed on this device", the "Saved" notification and chime |
| 0:15.7 | The spark splits into the category colors | Insights: Next Steps, Ideas, Decisions, a reminder |
| 0:21.3 | Night falls: every thought connects; just ask | Mesh, AI with Referenced Notes |
| 0:26.3 | "Srutam. Catch it before it's gone." Fades into the first frame so it loops | Logo |

The volume-button shortcut is deliberately absent: `VolumeButtonTriggerService` is not
registered in the manifest, so the video only shows capture paths the app ships.

## Color script

Colors come from the app's tokens (`ui/theme/Color.kt`, `SemanticColors.kt`, `InsightCards.kt`):
slate greys while the idea is dying (the gold spark is the only warm thing on screen),
crimson only while recording, cobalt / amber / teal for Next Steps / Ideas / Decisions,
Cosmic Void for the payoff, and ceramic white for the brand resolve.

## Layout

- `src/timeline.ts`: every scene, beat, VO and music cue in frames (one source of truth)
- `src/scenes/*`: one component per scene; each is also its own composition in Studio
- `src/components/AppUI.tsx`: replicas of real Srutam UI built from the app's layouts and strings
- `scripts/generate-voiceover-elevenlabs.mjs`: the voiceover (Eleven v4 with audio tags, voice "George"),
  generated in one take and cut per line using the API's character timestamps
- `scripts/generate_audio.py`: synthesized temp music bed and SFX (`numpy`, `soundfile`)
- `scripts/stills.mjs`: renders review stills

## Commands

```bash
npm i
npx remotion studio                                  # preview
npx remotion render SparkStory30 out/spark-story-30s.mp4
ELEVENLABS_API_KEY=... node scripts/generate-voiceover-elevenlabs.mjs [voiceId]
python scripts/generate_audio.py
```

If Remotion cannot download its own Chromium, point it at a local one with
`REMOTION_BROWSER_EXECUTABLE=/path/to/headless_shell`.

Loudness for social: `ffmpeg -i in.mp4 -c:v copy -af loudnorm=I=-14:TP=-1.5:LRA=11 -c:a aac -b:a 192k out.mp4`.

## Before publishing

- Replace the UI replicas with screen recordings of the app holding the same demo data.
- The voiceover was generated on an ElevenLabs free plan; commercial use needs a paid plan.
- Swap the synthesized music bed for a licensed track.
- Fonts: Inter and Playfair Display (SIL Open Font License).
