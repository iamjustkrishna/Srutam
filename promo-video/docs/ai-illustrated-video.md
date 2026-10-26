# Making an AI-illustrated short (how "Who was doing slides 8–12?" was made)

This is the process behind `WhoDidSlidesDrawn` (`src/drawn/`). Follow these steps to make the
next video the same way.

## Why this approach

We first drew characters in code: SVG rigs, then hand-built SVG frames copied from a reference
short. Even the best code-drawn frames looked stiff. Hands and feet were awkward, and the shots
didn't read clearly.

The approach that works:

1. Generate the **art** with an image model, made consistent by cast sheets.
2. Build the **motion, phone screens, text, voices and sound** in Remotion, where we have exact
   control.

Hard anatomy (hands, feet) is hidden by framing. The camera pushes in, and phone screens fill
the frame instead of being held by drawn hands.

## 1. Story first

Write the beats as an emotional arc, one image per beat. For this video:

1. The calm before: 11 p.m., everyone signs off.
2. The hit: 1:43 a.m., "where are slides 8–12??"
3. Blame: Riya is furious; Kabir sends "not me bro 💀".
4. Spiral: the nightmare professor and his red zero.
5. Low point: 2:30 a.m., doing it alone.
6. Turn: "Wait. I recorded that meeting."
7. Srutam answers with the exact quote.
8. Proof: the canteen recording plays.
9. Justice: "KABIR." … "on it 😭", then 4 a.m. and the chai tower.
10. Callback: Kabir presents and starts recording himself.

## 2. Cast sheets (Canva → generate-image)

- Make one **model sheet per character**: four full-body turnaround views plus five
  expression close-ups, on a plain background, with no text. The exact prompts are in
  `src/drawn/SHOTS.md`.
- Make the first sheet from text only. Make the others with that first sheet as an image
  reference ("exactly the same art style … a different character"), so the cast shares one
  style.
- Describe each character with the same words in every prompt: hair, outfit, one prop
  (Meera's pencil in her bun, Riya's planner and claw clip, Kabir's maroon hoodie).
- **References only work inside one Canva account.** When we switched accounts, every shot
  that referenced the old sheets failed with `SERVER_GENERIC`. The fix was to recreate the
  sheets in the new account and use the new ids.

## 3. Shots

Use one generate-image call per beat, at `PORTRAIT_9_16`, with the relevant cast sheet(s) as
`imageReferences`. Prompt pattern:

> Single frame from a hand-drawn 2D animated short film, same art style and character design as
> the reference sheet (thin ink lines, flat cel colours, soft painted lighting, detailed lived-in
> background). [time + place + lighting]. [Character] (exactly as in the reference: [2–3 visual
> anchors]) [action + exact expression]. [Props that tell the story]. [Mood]. Vertical
> composition, no text.

Tips:

- Ask for **no text**. Chats, the Srutam app, time stamps and captions are built in code, so
  they are exact and readable.
- Put the important face in the **upper-middle** of the frame and say it is **lit**. The first
  Kabir shot was too dark and too small to read, and the redo fixed it.
- Name the hard parts you will hide. "Medium close-up" and "from above" keep hands and feet
  small or out of frame.
- Check every result before using it, and regenerate any shot that doesn't read.
- Canva Free allows 20 AI generations a month; Pro allows more. Failed jobs may still use
  credits.

All shot ids are in `src/drawn/SHOTS.md`.

## 4. Get the full-size images

- The generator returns a Canva media id, plus a small preview (about 112×199 px).
- The full images are 944×1680. Save them as `public/drawn/s01.jpg … s09.jpg`, `s03a.jpg`
  and `s03b.jpg`.
- To download them from a cloud session, these hosts must be allowed in the environment's
  network settings (Network access → Allowed domains):
  - `media.canva.com`
  - `export-download.canva.com`
- The other option is to download each image from its Canva link and upload it to the session.
- Commit the images with `git add -f`, because the repo root ignores `*.jpg`.

## 5. Voices

1. Write one JSON per character in `scripts/vo/en/drawn-*.json`, with ElevenLabs v3 audio tags
   for delivery, e.g. `[furious, through gritted teeth]`.
2. Run the generator with your key:
   `ELEVENLABS_API_KEY=… node scripts/generate-voiceover-elevenlabs.mjs scripts/vo/en/drawn-meera.json`
3. **Check every cut by transcribing it.** Automatic cuts clipped two lines:
   - "Who. Was doing…" lost its first word;
   - "Recording this." came out as "this".

   We re-cut both from `full-take.mp3` with `ffmpeg -ss/-to` plus short fades.
4. Lines reused from the first version: `slides/playback.wav` (the canteen recording) and
   `slides/kabir/s-canteen.mp3` ("Who records a canteen?!").

## 6. Music and sound

- Generate the score with `python scripts/music-drawn.py`, which writes `public/drawn/music.wav`.
  Its cue times mirror `src/drawn/timeline.ts`:
  - cosy plucks, then a hit at 1:43;
  - a ticking pad, then a low drop for the nightmare;
  - a lonely pad, then bells on the realisation;
  - plucks under the flashback, a groove for the chat, a walk-down at 4 a.m.;
  - a resolve under the end band.
- Sound effects come from `public/sfx2/` and are listed in `SFX` in the timeline.

## 7. The composition (`src/drawn/`)

- `timeline.ts` holds the shot spans, beats, voice lines, sound effects and captions, in
  30 fps story frames. The video renders at 60 fps.
- `DrawnSlides.tsx` has the building blocks:
  - `Art`: a full-bleed illustration with an eased push and pan, a breath of handheld drift,
    and an optional impact shake.
  - `Banner`: a phone notification that springs down from the top.
  - `Chip`: the time of night, top centre.
  - `Lettering`: punched-in hand-lettered words (Kalam), e.g. "WE'RE GOING TO FAIL", "!!",
    "REC ●".
  - `PhoneOver`: the real app screens (`AskFlow`, `ChatScreen` from `storyboards/Kit.tsx`) on a
    floating phone over a blurred, darkened copy of the shot.
  - The canteen flashback gets a sepia grade, a vignette and a playback progress bar.
  - `EndBand` closes on "Srutam: Listen once. Ask anytime."
- The composition is registered in `src/Root.tsx` as `WhoDidSlidesDrawn`.

## 8. Review and render

- Review stills (render frames are story frames × 2):
  `node scripts/stills.mjs --id=WhoDidSlidesDrawn 60 260 700 …`
- Full render:
  `scripts/render/render.sh WhoDidSlidesDrawn 2640 out/who-did-slides-drawn.mp4`
- Check the result:
  `scripts/render/verify.sh out/who-did-slides-drawn.mp4 out/who-did-slides-drawn-sheet.png`
  It checks specs and loudness (-14 LUFS), counts silences and makes a 12-frame contact sheet.

Until the full-size images are in `public/drawn/`, a draft can be rendered from upscaled
previews (use `ffmpeg -vf scale=944:1680` on each preview). It shows timing and sound, but the
picture is blurry. Never publish a draft.
