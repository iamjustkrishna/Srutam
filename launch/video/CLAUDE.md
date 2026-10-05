# Playbook: Srutam launch video (read before touching anything here)

The goal is to make video changes cheaply. Use `srutam_video.py` for every mechanical step, and only spend tokens on creative or code changes. `README.md` lists the commands.

## Token and cost rules
1. **Never hand-roll pipeline commands.** Don't write curl, ffmpeg chains or Remotion render invocations yourself; `srutam_video.py` already wraps each one. If a step is missing, add a subcommand rather than an ad-hoc shell line.
2. **Audio-only change** (music, SFX volumes, narration mix): run `render --audio-only` (~1 min). Never re-render the picture for an audio change.
3. **Review with `stills`.** Contact sheets cost about 1.5k tokens to view; never watch a full render. View at most one sheet per format per iteration.
4. **Don't re-read big files.** `src/components/App.tsx` (~400 lines) is the app UI kit: grep for the component you need. The scene files are 100–200 lines.
5. **Check `credits` before any ElevenLabs call.** The account is on the free tier (10k credits a month). Don't regenerate narration unless the text changed: `voice --only <id>`.
6. **Background renders:** run the command itself with `run_in_background`. Never wait with `while pgrep -f render ...`, because the pattern matches its own shell and loops forever.

## Architecture
- **`src/timeline.ts`** holds the single source of truth: scene ids, minimum lengths and narration text. Each scene lasts `max(min, voice length + 1.05s)` and overlaps the next by 0.4s. `public/vo/manifest.json` (written by `voice`) holds the real voice lengths, word timings, SFX paths and the music file.
- **`src/Launch.tsx`** contains:
  - the background;
  - a `<Sequence>` per scene, wrapped in `SceneWrap` (zoom/blur in and out);
  - captions (`SHOW_CAPTIONS`, currently false);
  - all audio: music ducked to 0.16 while the voice plays (0.32 otherwise), narration, and SFX cues in `cues()` (name, time, volume) relative to scene starts.
- **`src/scenes/`:**
  - `Simple.tsx`: hook, title, newin, trust, end.
  - `Capture.tsx`: dock, transcribe, organize.
  - `NewFeatures.tsx`: cloud, connect (QR + CLI), agent.
  - Every scene receives local `t` (seconds) and `dur`, and calls `useLayout()` for `portrait` (9:16) layouts.
- **`src/components/App.tsx`** rebuilds the real app UI from the Compose source (1dp = 1px inside `Phone`). Components: `Phone`, `Screen`, `TopBar`, `NoteCard`, `FeedFilters`, `BottomBar`, `FeedScreen`, `DetailScreen`, `Segments`, `TaskRow`, `IdeaCard`, `DecisionCard`, `ReminderRow`, `ThemeCard`, `InsightsScreen`, `CloudCard`, `SettingsScreen`, `PairDialog`, `Toast`, and `MI` (Material icon paths).
- **`src/components/ui.tsx`** holds the video graphics: `Bg`, `Rise` (word reveal), `Chips`, `Tap` (touch ripple), `QR`.
- **`src/lib.ts`** holds the easing helpers `P(t,a,b)`, `E.*` and `L`, plus `tf()`, which returns transform, opacity and filter.
  - **Gotcha:** `tf()` sets `filter`, so put any `filter: drop-shadow(...)` *after* `...tf()` in the style object.

## Facts about the real app (verified in code, don't drift)
- **Capture:** the **Floating Dock** (`service/FloatingButtonService.kt`, `res/layout/floating_record_button.xml`):
  - Collapsed, it is a 52dp `#0F172A` edge tab showing the logo, docked left by default.
  - Expanded idle: a pill with radius 22, background `#0F172A`, border `#475569`, holding a red `#E11D48` mic button, an open-app button and a close button. Each button is a 38dp circle; the non-red ones are `#1E293B`.
  - Recording: the pill shows a red 13sp timer, pause, stop (`#E11D48`) and cancel.
  - Stopping shows the toast "Voice note saved".
  - **The volume-button shortcut is NOT live.** Its service isn't in the manifest, so never show it.
  - The Quick Settings tile and the notification shortcut exist.
- **Fonts:** Playfair Display (`res/font/playfair_display.ttf`) is used only for the top-bar "Srutam" title and accent. Everything else is Roboto (FontFamily.Default).
- **Colours:** background `#F4F5F8`, cards white with border `#E2E8F0`, ink `#0F172A`, secondary `#64748B`, cobalt `#2563EB`, emerald `#10B981`.
- **Note Details:** the tabs are "✦ Summary / 📄 Transcript / 💡 Insights", and the transcript card is "Full Transcript". There is **no** "Transcribed on this device" badge.
- **Insights:** segments "Ideas / Next Steps / Decisions" with dots `#D97706 / #2563EB / #0D9488`. A completed task gets an emerald check and a strikethrough. There is **no** "completed by agent" UI.
  - The Roborazzi renders in `app/src/test/screenshots/insights/phone/*.png` are the pixel reference.
- **Settings:**
  - Section header: "CLOUD SYNC & DEVELOPER BRAIN (MCP)".
  - Signed-in card: "Srutam Cloud Active", Sync, Auto-Sync, "MCP Agent Keys n/3", and a "Connect a computer" card with **Scan QR** and Enter code.
  - "Request account & data deletion" is an email request, not an instant delete.
- **Pairing** (`mcp-server/src/cli/pairPrompt.ts`): the CLI shows the QR.
  - CLI prompts: "Connect by scanning a code? [Y/n]", then `Your phone (k***@…) approved "<name>".`, then "Connect this computer to …? [Y/n]".
  - The phone shows the dialog "Connect this computer?" with Computer, srutam-mcp and Code expires rows, and Cancel / Connect buttons. The toast afterwards is `Connected "<name>"`.
- **MCP tools:** `search_notes`, `list_recent_notes`, `get_note_detail`, `list_action_items`, `update_action_item`, `append_agent_work_log`, `list_insights`, `list_reminders`. Agents cannot delete or edit notes. 16+ clients are supported.
- **Website:** srutam.iamjustkrishna.space (not srutam.space). The app is on Google Play.

## Sound design rules
- Transitions use `whoosh_soft` (filtered, -6 dB). Time it so the **peak** lands mid fade-in.
  - The peak is 0.16s into the file, so the cue starts at `cut + fade/2 - 0.16`. Use `whooshes()` in `src/social/kit.tsx`.
  - Never start a whoosh at, or before, the cut.
- The user finds bright or loud whooshes distracting. Keep them at ≤0.12 of `whoosh_soft`.

## Environment gotchas
- Node's `fetch` ignores HTTPS_PROXY unless you set `NODE_USE_ENV_PROXY=1`. The Python script uses urllib, which honours the proxy.
- `dl.google.com` is blocked by default, so the Android SDK and Roborazzi renders aren't possible unless the user allows it.
- The ElevenLabs Music API returns 402 on the free tier; use `music gen` (sound-effects loops).
- `create-video` scaffolds a nested `.git`; delete it if you ever re-scaffold.

## Music approach
- Prefer `music arrange <preset>` over `music use`. It automates filter and level per scene (the `ARRANGEMENT` table in `srutam_video.py`), so the music follows the video's energy, and it costs no credits.
- Edit the table, not the code, to re-score.

## User preferences so far
- **No mascot.** It was tried and rejected.
- **No captions** (the latest request).
- Keep whooshes and UI sounds subtle.
- Screens must match the real app.
- Narrator: ElevenLabs "George".
- Music being tried: old-school funk (`music.current` in `video.config.json`).
