# Srutam promo videos

## Cast v2: rig, lip-sync and pro storyboards

- `src/cast/Rig.tsx`: `Actor` wraps Meera or any cast member with two-bone IK arms
  (hand poses: point, open, palm, fist, grip, thumb, phone), head tilt, breathing and
  squash & stretch. `saccade()` and `blinkAt()` add idle eye life.
- `src/cast/Mouth.tsx`: the nine Rhubarb mouth shapes (A-H, X) in the sketch style.
- `scripts/lipsync-rhubarb.py`: runs [Rhubarb Lip Sync](https://github.com/DanielSWolf/rhubarb-lip-sync)
  (free, MIT) on each voice line and writes one mouth shape per frame to
  `src/<video>/visemes.ts`. English lines use the dialog text; Hindi lines use the
  phonetic recognizer. `python scripts/lipsync-rhubarb.py path/to/rhubarb`
- `src/cast/cast.ts`: Mr. Rao, Arjun, Neha, Prof. Iyer, Riya, Kabir. `CastSheet` still shows them.
- `src/storyboards/ProBoard.tsx` + `Notation.tsx`: 10-panel boards with shot sizes,
  camera moves (red), character action (orange), cuts, dialogue, sound and animation notes
  (`IdeaMeetingV2`, `IdeaExamV2`).
- `MeetingAnimationTest`: 13.6 s test of the "Who Said That?" climax with the new rig.

## "Who Said That?" v2 (44.7 s, 1080x1920, 60 fps): `WhoSaidThat`

Cold open a week later: Mr. Rao slams the table, "Fourteen? Kisne haan bola tha
fourteen ko?!" Freeze, "One week earlier": at Tuesday's sync Arjun suggests the 21st and
Rao overrules him himself ("Nahi nahi, fourteen. Final."), then asks someone to note it
down. Nobody does, except Srutam from the dock under the table; the Insights cards land
before lunch. Back to the outburst: "Neha ne!" "Arjun ne!" Meera asks Srutam AI "When did
we agree on the 14th?" and gets "Tuesday's sync, 0:42" with the quote and the source,
then plays it: the room hears Rao himself. He owns it ("Achha... toh fourteen hi final
hai.") and everyone laughs. End card: "Every meeting, minuted."

Srutam never claims to know who spoke (the app has no speaker diarization): it finds the
moment by time and quote, and the recording does the rest.

## "2 a.m. Before the Exam" v2 (41.3 s, 1080x1920, 60 fps): `BeforeTheExam`

2:07 a.m., the exam is at nine. The group chat asks "Meera, you were in class na?" She
slept through that lecture too, but Srutam didn't: a sepia flashback half-hears Prof while
her phone shows Srutam's ongoing "Recording" notification. Back at 2 a.m. she asks Srutam
AI; the answer cites "Econ lecture · 41:05" and plays the lecture: "Thursday ke exam mein,
chapter chaar aur saat, aur metro case study. Bees marks ka." She shares it, Kabir and
Riya walk in with chai, a smooth time-lapse to 4:10 with the lecture's Key Points, and at
8:45 the app's own knock-knock reminder: "Econ exam. Starts in 15 minutes." Q3 is the
metro case study. End card: "Listen once. Ask anytime."

What changed in v2:

- Hindi voices: AI4Bharat Indic-TTS (FastPitch + HiFi-GAN, native Hindi studio voices)
  instead of Kokoro's Hindi. Each character is voiced by steering the model's own pitch
  level and range, its pace and a light formant shift (WORLD), with natural Hinglish lines
  (`scripts/generate-voices-indic.py`). Playbacks from a phone get a phone EQ; the flashback
  is muffled. Measured pitch: Rao ~96 Hz, Prof ~101, Arjun ~125, Kabir ~128, Neha ~210,
  Riya ~253.
- Characters: face shapes (oval, round, square, long, heart), nose styles, eye sizes by
  age, eyelid creases, lips and ear detail; shoulders, deltoids and resting arms instead of
  a dome; collars, lapels and tie, a Nehru jacket with a pen, hood and drawstrings,
  headphones, earrings, a scrunchie and a swinging ponytail; trousers, shoes and a walk
  cycle for standing shots (`src/storyboards/People.tsx`, `LowerBody` in `src/cast/Rig.tsx`).
- Motion: rendered at 60 fps from 30 fps timelines (`src/stories/time.ts`): every pose,
  spring, camera move and blink gets twice the in-betweens; smooth half-blinks
  (`blinkK`), eased glances, no stepped time-lapse.
- Sound: a quieter kit (`public/sfx2/`: air, swell, felt thump, blips, taps, ticks, pages,
  room tones) under a softer score; music eases down under every line and drops out under
  the playback; effects at 60% so they carry transitions without taking focus.

```bash
python scripts/generate-voices-indic.py path/to/Indic-TTS/hi
python scripts/lipsync-rhubarb.py path/to/rhubarb
python scripts/generate_audio_stories.py
npx remotion render WhoSaidThat out/who-said-that.mp4
npx remotion render BeforeTheExam out/before-the-exam.mp4
```

## "Knock Knock" (31 s, 1080x1920): `KnockKnock`

Meera promises Ma a Sunday call, then binges a show. Next time she says it once to
Srutam, and on Sunday the reminder knocks: "Call Ma. Starts in 15 minutes." The app's
own knock-knock reminder sound (`public/sfx/knock.wav`) opens the film, lands the
payoff and closes the end card ("Say it once. Srutam knocks.").

## "Nani's Laddoos" (41.5 s, 1080x1920): `NanisLaddoos`

Nani never measured anything. Meera pulls down the shade and taps Start on Srutam's
recording notification ("Ready to record" → "Recording", Save / Pause); on Diwali, 1,400 km away, she asks Srutam AI how much jaggery goes
in, gets the answer with a "Referenced Notes:" pill, and hears Nani's own voice:
"...aur thoda sa pyaar. Bas." End card: "Keep the recipe. Keep the voice."

Both use real app strings (ReminderAlarmReceiver, QuickRecordNotification,
notification_quick_record.xml, GlobalCopilotScreen) and share `src/stories/Story.tsx`
(captions, ducking, pop-ins, lip-sync) and the storyboard kit in `src/storyboards/`.

- Meera: ElevenLabs Eleven v4, voice "Jessica" (`scripts/vo/knock-meera.json`, `scripts/vo/nani-meera.json`)
- Ma and Nani: Hindi, Kokoro TTS Hindi voices (`scripts/generate-voices-kokoro.py`), with English captions
- Score, SFX and lip-sync data: `scripts/generate_audio_stories.py` (mouths follow each voice's loudness)
- Storyboards for these and two more ideas: `node scripts/render-ideas.mjs`

```bash
ELEVENLABS_API_KEY=... node scripts/generate-voiceover-elevenlabs.mjs scripts/vo/knock-meera.json
ELEVENLABS_API_KEY=... node scripts/generate-voiceover-elevenlabs.mjs scripts/vo/nani-meera.json
python scripts/generate-voices-kokoro.py kokoro.onnx voices.bin
python scripts/generate_audio_stories.py
npx remotion render KnockKnock out/knock-knock.mp4
npx remotion render NanisLaddoos out/nanis-laddoos.mp4
```

## "The 2 a.m. Idea" (40 s, 1080x1920): `TwoAmIdea`

A relatable story in Meera's own voice (ElevenLabs Eleven v4, voice "Jessica"),
drawn in a warm hand-drawn character style:

| Time | Beat |
|---|---|
| 0:00 | 2:13 a.m. She wakes with "the best idea I've ever had". "I'll remember it in the morning." |
| 0:08 | Morning alarm. "I did not remember it in the morning." The idea-bulb pops. |
| 0:11 | VHS rewind to 2:13 a.m.: "Okay. Let's try that again." |
| 0:14 | She taps **Start** on Srutam's notification and whispers the idea, a next step and a day; taps **Save**: "Saved / Finishing transcript..." |
| 0:27 | Morning: the note is transcribed on the phone and titled, with an Idea, a Next Step and a Saturday reminder. The reminder card arrives with the app's own reminder sound, the knock-knock (`public/sfx/knock.wav`, a copy of `app/src/main/res/raw/srutam_knock.wav`; the "Saved" notification at 0:25 uses it too). |
| 0:33 | Three months later: her rooftop book café is open. End card. |

The notification states and labels are the app's own (`QuickRecordNotification.kt`):
"Ready to record" / Start, "Recording" / Pause + Save, "Saved" / "Finishing transcript...".
The whispered note contains the next step and the day, so every insight shown is
something the app could actually extract.

- `src/bedtime/`: timeline, scenes, props and phone screens
- `src/sketch/MeeraSketch.tsx`: the character, with expressions, blinking, lip-sync and hair sway
- `scripts/vo/bedtime.json`: the voiceover script; `scripts/generate_audio_bedtime.py`: score and SFX

```bash
ELEVENLABS_API_KEY=... node scripts/generate-voiceover-elevenlabs.mjs scripts/vo/bedtime.json
python scripts/generate_audio_bedtime.py
npx remotion render TwoAmIdea out/two-am-idea.mp4
```

# "The Spark" (30 s animatic): `SparkStory30`

A story-driven vertical (1080x1920) promo built with [Remotion](https://remotion.dev).
The hero is **Spark**, a thought that dies within seconds unless Srutam catches it.

| Time | Beat | What the app shows |
|---|---|---|
| 0:00 | Hook: "This idea has 4 seconds to live." | - |
| 0:03.7 | Ideas dying in the shower, driving, mid-meeting | - |
| 0:06.7 | Tap, tap: the floating dock, from any app | Floating dock (collapsed, expanded, recording), "Transcribed on this device", the "Saved" notification with the app's knock-knock |
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
ELEVENLABS_API_KEY=... node scripts/generate-voiceover-elevenlabs.mjs scripts/vo/spark.json
python scripts/generate_audio.py
```

If Remotion cannot download its own Chromium, point it at a local one with
`REMOTION_BROWSER_EXECUTABLE=/path/to/headless_shell`.

Loudness for social: `ffmpeg -i in.mp4 -c:v copy -af loudnorm=I=-14:TP=-1.5:LRA=11 -c:a aac -b:a 192k out.mp4`.

## Before publishing

- Replace the UI replicas with screen recordings of the app holding the same demo data.
- The voiceover was generated on an ElevenLabs free plan; commercial use needs a paid plan.
- Swap the synthesized music bed for a licensed track.
- Fonts: Inter, Playfair Display and Kalam (SIL Open Font License).
