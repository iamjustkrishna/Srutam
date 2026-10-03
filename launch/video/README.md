# Srutam launch video

The Srutam 2.5 launch film (16:9 and 9:16), built with [Remotion](https://remotion.dev) plus ElevenLabs narration, sound effects and music loops. Everything is reproducible from this folder, and one script drives the whole pipeline.

## Setup (once per machine or session)

```bash
cd launch/video
npm ci                                   # Remotion + deps (needs registry.npmjs.org)
echo "ELEVENLABS_API_KEY=sk_..." > .env  # or export ELEVENLABS_API_KEY; .env is gitignored
```

Requirements:
- Node 22+, ffmpeg/ffprobe, Python 3.10+ (standard library only).
- A Chromium headless shell. The path is set in `video.config.json` → `render.browser` and in `remotion.config.ts`.
- Network access to `api.elevenlabs.io` for any generation step.

## The script

`python3 srutam_video.py <command>`:

| Command | What it does | Time | ElevenLabs cost |
|---|---|---|---|
| `credits` | Shows the credits left. | 1s | 0 |
| `timeline` | Prints scene start times and lengths. | 1s | 0 |
| `voice --audition` | Reads line 1 in each audition voice. | 10s | ~150 |
| `voice [--only id ...]` | Narration plus word timings for every scene (or only the named scenes). | 30s | ~1 per character (~650 in total) |
| `sfx [name ...]` | Sound effects defined in `video.config.json`. | 30s | 40/second (~450 for all) |
| `music gen <preset> [--seconds 20]` | A new seamless music loop. | 20s | 40/second (800 for 20s) |
| `music use <preset>` | Loops a sample to the video length with fades. | 2s | 0 |
| `music arrange <preset>` | Scores a loop to the edit, following `ARRANGEMENT` in `srutam_video.py`: muffled intro, open on the title, a drop at "New in 2.5", a breakdown at Trust, a full ending. | 5s | 0 |
| `music previews` | 32-second previews of every sample, written to `launch/music/`. | 5s | 0 |
| `stills [--fmt 9x16] [--at 10,20]` | Contact sheet for review; by default one frame per scene. | ~1 min | 0 |
| `render --audio-only [--suffix=-funk]` | Remixes the audio onto the last picture render. `--suffix` names a variation so it doesn't overwrite the main files. | ~1 min | 0 |
| `render [--fmt 16x9]` | Full render: picture, audio mix, -14 LUFS, mux. | ~5 min per format | 0 |
| `check` | Duration, loudness and size of the final files. | 5s | 0 |

The outputs are `launch/srutam-2.5-launch-16x9.mp4` and `launch/srutam-2.5-launch-9x16.mp4`.

### Common recipes

- **Try a new song:**
  1. Add a prompt under `music.presets` in `video.config.json`.
  2. `music gen my-song`
  3. `music use my-song`
  4. `render --audio-only`
- **Change a narration line:**
  1. Edit `vo:` in `src/timeline.ts`.
  2. `voice --only <scene>`
  3. `music use <current>`, because the video length may have changed.
  4. `render`
- **Change visuals:**
  1. Edit `src/scenes/*.tsx`.
  2. `stills --at <secs>` to check the frames.
  3. `render`
- **Live preview:** `npx remotion studio`.

## Notes

- ElevenLabs' **Music API needs a paid plan**. On the free tier, music comes from the Sound Effects model with `loop: true`, at most 30 seconds, and is looped to length.
- `gen/music.py` is a free synthesized fallback bed: `python3 gen/music.py <seconds> '<scene-starts json>'`.
- Generated audio is committed (`public/vo`, `public/sfx`, `public/music-samples`), so nobody pays for it twice. `public/music.wav`, `out/` and the `.mp4` files are regenerated.
