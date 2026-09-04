# Srutam - Project Identity & Architecture

## Overview
Local-first Android voice-note app for frictionless capture, on-device Sherpa-ONNX transcription, optional AI organization, and conversational retrieval across recordings. The active capture experience is the in-app Studio recording bar, with Quick Settings and an optional floating overlay as secondary capture paths. Volume Down is legacy code, not the current product requirement.

## Target Package & Identity
- **Application Name**: Srutam
- **Package / Namespace**: space.iamjustkrishna.srutam
- **Type**: Native Android (Kotlin + Jetpack Compose)
- **Compile SDK**: 36
- **Target SDK**: 36
- **Min SDK**: 29 (Android 10+)

## Subsystems & Architecture
1. **Quick Capture & Background Recording Services**:
   - StudioBottomBar: Current primary in-app hold/tap recording experience with pause, resume, lock, discard, and save interactions.
   - QuickRecordingTileService: Quick Settings tile one-tap toggle.
   - FloatingButtonService: Optional on-screen overlay dock for capture from other apps.
   - PersistentRecordingNotificationService and RecordingForegroundService: Background microphone recording and notification controls.
   - VolumeButtonTriggerService: Legacy accessibility implementation; not currently manifest-registered or the active capture requirement.
2. **On-Device Speech-to-Text**:
   - LocalTranscriber: Sherpa-ONNX (com.k2fsa.sherpa.onnx) offline ASR engine running local models without cloud latency.
3. **AI Voice Intelligence**:
   - AIProcessor: Gemini-based structured extraction of summaries, key points, action items, WIIFM, and note/cross-note chat. Planned extraction also includes ideas and decisions.
4. **Persistence & Data Layer**:
   - AppDatabase & RecordingDao: Room database tracking Recording entities, file paths, durations, transcript data, AI results, and RecordingAiStatus. Actions, ideas, and decisions are planned to become first-class rows.
5. **Playback & Audio Engine**:
   - AudioPlayer, AudioDecoder, and AudioFileScanner: Scans and plays recorded audio with waveform scrubbing.
6. **UI & Navigation**:
   - Modern Jetpack Compose Material 3 screens: FeedScreen, DetailScreen, ChatScreen, GlobalCopilotScreen, current ActionItemsScreen, and SettingsScreen.

## Product identity and intended users

Srutam is not primarily a meeting-bot product or a generic cloud voice-journal service. Its strongest differentiators are fast Android capture, local transcription, privacy, and low ongoing effort. AI organization is a retention and retrieval feature, not the acquisition hook.

The intended users are privacy-conscious Android users who want to capture a thought in under two seconds without unlocking the phone or navigating through a recording app:

- People walking or commuting
- People with their hands occupied
- Students capturing study or lecture thoughts
- Writers capturing ideas before they disappear
- Developers and builders recording implementation ideas
- Users making personal journaling or stream-of-consciousness notes

These users should not have to manually curate a second brain. Srutam should organize notes quietly and show only information likely to matter.

## Visual theme and design system

The visual direction is **Apple Studio / quiet industrial**: light ceramic surfaces, restrained shadows, precise rounded geometry, and a small semantic color palette. The UI should feel tactile and premium without becoming glossy, noisy, or overly decorative.

### Theme implementation

- Theme implementation: `ui/theme/Theme.kt`
- Color tokens: `ui/theme/Color.kt`
- Typography tokens: `ui/theme/Type.kt`
- Main theme function: `SrutamTheme`
- Light and dark Material 3 color schemes are defined.
- Dynamic colors are intentionally disabled (`dynamicColor = false`) so the brand palette remains consistent across Android devices.
- The current product is primarily designed around the light scheme.

### Color tokens

Use the named tokens from `Color.kt` instead of introducing arbitrary hex values in new components.

#### Base surfaces and canvas

| Token | Hex | Role |
|---|---|---|
| `CeramicWhite` | `#FFFFFF` | Main light background, cards, and top-level surfaces |
| `SlateSurface` | `#F8FAFC` | Soft grouped surface and subtle component background |
| `SlateGrouped` | `#F1F5F9` | Secondary grouped controls and inactive pills |
| `SlateBorder` | `#E2E8F0` | Borders, dividers, and low-contrast outlines |
| `DarkSurfaceBase` | `#0F172A` | Dark theme background |
| `DarkSurfaceCard` | `#1E293B` | Dark theme cards and surfaces |
| `DarkSurfaceBorder` | `#334155` | Dark theme borders and outlines |

#### Signature accent: Cobalt Blue

Cobalt represents intelligence, clarity, navigation, links, primary controls, and AI actions.

| Token | Hex | Role |
|---|---|---|
| `CobaltBlue` | `#2563EB` | Primary accent and selected controls |
| `CobaltBlueDark` | `#1D4ED8` | Dark/pressed primary accent |
| `CobaltContainer` | `#EFF6FF` | Soft blue container |
| `CobaltBorder` | `#BFDBFE` | Blue outline |
| `OnCobaltContainer` | `#1E40AF` | Text/icons on blue containers |

#### Studio Crimson

Crimson is reserved for active recording, stop, destructive actions, and recording-related attention. It should not be used as a general brand accent.

| Token | Hex | Role |
|---|---|---|
| `StudioCrimson` | `#EF4444` | Active record/stop state |
| `StudioCrimsonDark` | `#DC2626` | Pressed/destructive state |
| `StudioCrimsonContainer` | `#FEF2F2` | Soft recording/destructive container |
| `OnStudioCrimsonContainer` | `#991B1B` | Text/icons on crimson containers |

#### Emerald success and validation

Emerald represents completed tasks, successful processing, privacy reassurance, and positive validation.

| Token | Hex | Role |
|---|---|---|
| `EmeraldSuccess` | `#10B981` | Success and completed state |
| `EmeraldContainer` | `#ECFDF5` | Soft success/privacy container |
| `OnEmeraldContainer` | `#065F46` | Text/icons on emerald containers |

#### Text hierarchy

| Token | Hex | Role |
|---|---|---|
| `TextPrimary` | `#0F172A` | Main readable text |
| `TextSecondary` | `#64748B` | Metadata and supporting text |
| `TextMuted` | `#94A3B8` | Disabled or low-emphasis text |
| `TextOnDarkPrimary` | `#F8FAFC` | Primary dark-theme text |
| `TextOnDarkSecondary` | `#94A3B8` | Secondary dark-theme text |

Insights may use an amber/ochre semantic accent for recurring ideas, but this should be centralized as a new theme token before implementation rather than repeated raw colors. The current UI uses values near `#D97706` and `#FEF3C7` for this purpose.

### Typography

- Brand and major screen titles use the bundled `playfair_display.ttf` through `PlayfairDisplayFontFamily`.
- Body text, metadata, labels, and controls use the default sans-serif Material typography.
- Playfair is for editorial identity and emphasis, not for long paragraphs or dense data.
- Current Material typography ranges from 11sp labels and 12sp body-small text to 32sp display text.
- Use semibold/bold sparingly for hierarchy; supporting metadata should remain quiet.
- Shared top-bar title treatment should be reused across Notes, Insights, Actions during transition, and AI.

### Shape and spacing language

- Small action buttons: approximately 12dp rounded squircles
- Cards: generally 12-16dp rounded corners
- Dialogs: approximately 32dp rounded/glassmorphic cards
- Pills, segmented controls, status badges, and primary actions: `CircleShape`
- Recording controls: circular geometry with crimson accent and soft halo/shadow
- Use consistent horizontal screen padding, generally 16dp
- Avoid sharp rectangular Material defaults unless the component is intentionally a text field or system-like control

### Surfaces, borders, and elevation

- Prefer `CeramicWhite`, `SlateSurface`, and `SlateGrouped` layering over heavy gradients.
- Use thin `SlateBorder` outlines to separate components.
- Use low, soft elevation for cards and controls; shadows should communicate touch hierarchy, not create dramatic depth.
- Glassmorphic dialogs use near-opaque white, rounded corners, a subtle top-lit border, and a soft ambient shadow.
- Do not use glass effects to reduce text contrast or hide functional state.

### Interaction and motion

- Primary interactions should feel tactile and responsive.
- The record button uses spring-damped scale/halo feedback and haptics.
- Recording state uses crimson; paused state may use amber; completed/success state uses emerald.
- Use motion to explain state changes such as recording, pause, save, discard, expand, and archive.
- Avoid animation that delays capture or makes a simple review action feel theatrical.

## Current user experience

### Capture

The active primary capture experience is the global Studio recording bar at the bottom of the root screen. It supports:

- Hold-to-record and release-to-stop/save
- Quick tap to lock a recording without holding
- Upward slide-to-lock during a hold
- Slide-left discard
- Pause and resume
- Live elapsed duration
- Waveform-style feedback
- Haptic feedback and spring/tactile animations
- Save Voice Note dialog with editable title

Secondary mechanisms are:

- Quick Settings tile through `QuickRecordingTileService`
- Optional floating overlay dock through `FloatingButtonService`
- Persistent recording notification through `PersistentRecordingNotificationService`

`VolumeButtonTriggerService` and its accessibility XML remain in the repository as legacy code. Volume Down is not the current primary capture path, and the service is not declared in the current manifest. Do not present Volume Down as a supported product requirement unless it is deliberately restored and re-registered.

### Root navigation

The current root has three destinations:

1. **Notes** - recording feed and audio library
2. **Actions** - current action/idea/decision hub; planned redesign name is **Insights**
3. **AI** - global Srutam AI copilot for questions across recordings

Detail, note-specific chat, and Settings are separate navigation destinations. The Studio recording bar remains global over root destinations and is hidden when the keyboard is open.

### Notes and note detail

The Notes feed supports newest-first audio browsing, human-readable dates, playback, inline waveform-style seeking, play/pause, per-note AI processing, summary previews, search/filtering, rename, deletion, and multi-select deletion.

The detail screen shows the title, date, duration, playback, transcript, summary, key points, action items, WIIFM, processing state, retry, rename/delete, and note-specific AI chat.

### Global AI copilot

`GlobalCopilotScreen` is a pull-based interface: the user asks what their notes say. Its current path is:

1. Read Room-backed recordings.
2. Build an in-memory BM25 index over title, summary, and transcript.
3. Retrieve up to four relevant notes.
4. Send excerpts and the question to Gemini.
5. Return an answer with source-note references.

This is separate from Insights, which should passively surface important information without requiring a query.

## Insights direction

The current Actions screen is being redesigned because a task-only screen is a poor fit for journaling, brainstorming, and free-form thinking. Many recordings should produce no task.

Insights should answer:

> What deserves my attention from the things I have recorded?

Recommended responsibilities:

| Destination | User question | Product job |
|---|---|---|
| Notes | What did I record? | Browse, play, search, and open recordings. |
| Insights | What deserves attention? | Surface actions, ideas, decisions, and recurring themes. |
| AI / Ask | What do my notes say about this? | Answer cross-note questions on demand. |

The default Insights view should be an overview, not an empty task list. Recommended sections are:

1. **Needs attention** - explicit open action items
2. **Recent decisions** - chronological decision log grouped by month
3. **Themes you keep returning to** - only when recurrence evidence is strong
4. **Recent ideas** - distilled ideas linked to source notes

The category capsule can filter the overview into `All | Next Steps | Ideas | Decisions`. These categories must behave differently:

- Next Steps: checkboxes, open/completed states, and manual archive
- Ideas: no checkboxes, recurrence count, and inline related-note accordion
- Decisions: chronological log with no completion state
- All: composed overview that remains useful when there are zero actions

The 2D connected-node view is optional and secondary. It should be opened from a theme, idea, or decision through an action such as `Explore connections`. It is useful when it explains an idea-to-decision-to-action chain, shows how a thought evolved, or traces an insight back to evidence. A graph with unexplained similarity lines is decoration and should not be the default screen.

Detailed implementation guidance is in `actions/insights-tab-and-node-architecture.md`.

## AI processing model

### Local transcription

`LocalTranscriber` uses the embedded Sherpa-ONNX online recognizer and local encoder, decoder, joiner, and token assets. `AudioDecoder` uses Android media codecs, converts audio to mono as needed, resamples to 16 kHz, and streams chunks to the recognizer.

Raw audio is intended to remain on the device.

### Optional cloud analysis

`AIProcessor` currently uses Gemini for summaries, key points, action items, WIIFM, note-specific questions, and cross-note questions. The accurate privacy boundary is:

- Audio transcription: on-device
- Raw audio storage: local
- Optional summaries and chat: cloud AI using transcript text

Settings contains UI for multiple providers, but the actual processor currently creates Gemini models directly. OpenAI, Anthropic, Groq, and general BYOK routing are planned, not fully implemented.

### Required structured analysis contract

The future analysis response should be versioned and include:

- `title`
- `summary`
- `whatsInItForMe`
- `keyPoints[]`
- `actionItems[]`
- `ideas[]`
- `decisions[]`

Each extracted item should retain short evidence where possible. The model must never invent action items, turn vague possibilities into decisions, duplicate one thought across every category, or force non-empty arrays. WIIFM must be grounded; `No clear personal benefit stated` is valid.

## Local data architecture

The current Room database is named `srutam_database` and primarily contains the `Recording` entity. Recording metadata includes ID, timestamp, audio path, duration, name, transcript, summary, key-point JSON, action-item JSON, WIIFM, AI status, processing flag, and processing error.

Current AI statuses include `NOT_REQUESTED`, `TRANSCRIBING`, `SUMMARY_PENDING_OFFLINE`, `SUMMARY_PROCESSING`, `READY`, and `ERROR`.

Actions, ideas, and decisions should become first-class Room rows rather than remaining JSON arrays inside `Recording`:

```text
recordings
insight_items
related_insights
analysis_runs
```

`insight_items` should contain the recording foreign key, kind (`ACTION`, `IDEA`, or `DECISION`), text, evidence, optional rationale/due date, source order, timestamps, status, completion/archive timestamps, and optional recurrence group ID.

Actions should follow:

```text
OPEN -> COMPLETED -> ARCHIVED
```

Archive hides an item from the active view but never deletes it. Completion/archive state should move from `AppPreferences` into Room so it can be queried, backed up, and synchronized.

## Processing lifecycle and current gap

When recording stops, the intended future lifecycle is:

```text
Finalize audio
  -> insert Recording in Room
  -> local transcription
  -> transcript stored offline
  -> optional cloud analysis when permitted and online
  -> structured insight rows
  -> related/search index update
  -> Compose flows refresh UI
```

The current recording service creates the audio file, but its `saveRecordingToDatabase()` method is currently only a log statement. The Feed can discover files from storage and the user can trigger processing manually, but the complete automatic recording-to-Room-to-analysis pipeline is not finished.

The preferred future processing mechanism is WorkManager for process-death recovery, retry handling, network constraints, and offline queues. Compose ViewModels should express user intent and observe state, not own the entire background pipeline.

Recommended statuses for the durable pipeline are `CAPTURED`, `TRANSCRIBING`, `TRANSCRIBED`, `WAITING_FOR_NETWORK`, `ANALYZING`, `READY`, and `ERROR`.

## Storage and privacy reality

The current code stores recordings in the public external music directory:

```text
/storage/emulated/0/Music/Srutam/
```

`AudioFileReader.getRecordingsDirectory()` is the implementation source of truth. Existing README and Settings wording that calls this “app-private” is inaccurate and must be corrected before release.

Cloud Sync and MCP must be opt-in. If transcript text is sent to cloud AI or Supabase, the UI must disclose that clearly. A note excluded from MCP must also be excluded from cloud search, related-note results, and agent responses.

## Future Cloud Sync and MCP context

The future Cloud/MCP architecture is documented in `ideas/srutam_cloud_mcp_server_plan.md`. It proposes:

- Supabase Postgres with Row Level Security
- Structured note and insight synchronization
- Optional pgvector semantic search
- User authentication and personal MCP API keys
- Per-note `Exclude from MCP` privacy control
- Agent work logs
- Agent-driven action completion with attribution
- An npm `@srutam/mcp-server` using stdio transport

Planned MCP operations include searching notes, listing recent notes, getting note detail, listing action items, updating action items, and appending agent work logs.

The local Android insight model should be the canonical domain model so Cloud/MCP does not introduce a second incompatible representation. Sync must be idempotent, map local and remote IDs explicitly, preserve local user decisions during conflicts, and record agent name/timestamp for agent changes. Raw audio remains local by default.

## Similarity and recurrence

The current BM25 engine supports keyword retrieval across recordings but is not a complete semantic recurrence system.

Recommended progression:

1. Start with conservative normalized-text/BM25 related matching.
2. Compute relationships only when a new insight arrives.
3. Cache pair scores and related-note records.
4. Require multiple distinct source recordings.
5. Show prominent recurring-theme banners only after at least three strong mentions.
6. Add on-device embeddings later if usage justifies the model size and complexity.

The proposed cosine threshold around `0.75` is an experiment, not a fixed truth. Optimize for precision because false claims about a user's recurring thoughts damage trust. Provide `Not the same idea` and `Dismiss theme` feedback.

## Current services and important files

### Application and navigation

- `app/src/main/java/space/iamjustkrishna/srutam/MainActivity.kt`
- `app/src/main/java/space/iamjustkrishna/srutam/SrutamApplication.kt`
- `app/src/main/java/space/iamjustkrishna/srutam/navigation/Navigation.kt`

### AI and retrieval

- `app/src/main/java/space/iamjustkrishna/srutam/ai/LocalTranscriber.kt`
- `app/src/main/java/space/iamjustkrishna/srutam/ai/AIProcessor.kt`
- `app/src/main/java/space/iamjustkrishna/srutam/ai/BM25SearchEngine.kt`

### Data and repository

- `app/src/main/java/space/iamjustkrishna/srutam/data/Recording.kt`
- `app/src/main/java/space/iamjustkrishna/srutam/data/RecordingDao.kt`
- `app/src/main/java/space/iamjustkrishna/srutam/data/AppDatabase.kt`
- `app/src/main/java/space/iamjustkrishna/srutam/data/ActionItem.kt`
- `app/src/main/java/space/iamjustkrishna/srutam/repository/RecordingRepository.kt`

### Capture and background services

- `app/src/main/java/space/iamjustkrishna/srutam/service/RecordingForegroundService.kt`
- `app/src/main/java/space/iamjustkrishna/srutam/service/QuickRecordingTileService.kt`
- `app/src/main/java/space/iamjustkrishna/srutam/service/FloatingButtonService.kt`
- `app/src/main/java/space/iamjustkrishna/srutam/service/PersistentRecordingNotificationService.kt`
- `app/src/main/java/space/iamjustkrishna/srutam/service/VolumeButtonTriggerService.kt` - legacy/not currently manifest-registered

### UI

- `app/src/main/java/space/iamjustkrishna/srutam/ui/screens/FeedScreen.kt`
- `app/src/main/java/space/iamjustkrishna/srutam/ui/screens/DetailScreen.kt`
- `app/src/main/java/space/iamjustkrishna/srutam/ui/screens/ChatScreen.kt`
- `app/src/main/java/space/iamjustkrishna/srutam/ui/screens/ActionItemsScreen.kt` - current Actions implementation, planned Insights replacement
- `app/src/main/java/space/iamjustkrishna/srutam/ui/screens/GlobalCopilotScreen.kt`
- `app/src/main/java/space/iamjustkrishna/srutam/ui/screens/SettingsScreen.kt`
- `app/src/main/java/space/iamjustkrishna/srutam/ui/components/StudioBottomBar.kt`
- `app/src/main/java/space/iamjustkrishna/srutam/ui/components/SrutamTopAppBar.kt`

## Known gaps and risks

- Automatic post-recording Room insertion and processing are incomplete.
- Existing analysis fields are JSON inside `Recording`, limiting global queries and clean sync.
- Ideas and decisions are not yet part of the actual AI response/storage pipeline.
- AI provider selection UI is ahead of provider implementation; Gemini remains the actual provider.
- Recurrence detection is not yet embedding-backed or tuned against labeled examples.
- Public storage wording conflicts with privacy/app-private claims.
- Volume Down code remains in the repository but is not an active manifest-registered path.
- Automated tests are currently mostly Android template tests.
- Release signing, store assets, listing copy, closed testing, and production privacy review remain incomplete.
- Cloud/MCP authentication, sync, privacy exclusion, and conflict handling are planned, not implemented.

## Recommended implementation order

1. Stabilize recording lifecycle and Room insertion.
2. Move transcription/analysis to durable background work.
3. Version and expand the structured AI analysis contract.
4. Introduce first-class `insight_items` and migrations.
5. Replace the current Actions screen with the Insights overview.
6. Move completion/archive state into Room.
7. Add conservative related-note surfacing.
8. Prototype the 2D node view only as a secondary explainable exploration mode.
9. Align storage/privacy messaging with actual behavior.
10. Integrate Cloud/MCP using the same domain model.
11. Add provider abstraction, tests, release hardening, and store preparation.

## Build & Test Commands
- **Debug Build**: ./gradlew assembleDebug
- **Unit Tests**: ./gradlew test
- **Release Bundle**: ./gradlew bundleRelease

## Key Permissions

The manifest currently covers the permission categories needed by the active implementation:

- `android.permission.RECORD_AUDIO` - microphone capture
- `android.permission.FOREGROUND_SERVICE` and `android.permission.FOREGROUND_SERVICE_MICROPHONE` - background microphone recording
- `android.permission.BIND_QUICK_SETTINGS_TILE` - Quick Settings tile service
- `android.permission.READ_MEDIA_AUDIO` plus legacy storage permissions where applicable - audio file access
- `android.permission.POST_NOTIFICATIONS` - recording notifications on Android 13+
- `android.permission.WAKE_LOCK` - recording continuity
- `android.permission.VIBRATE` - recording interaction feedback
- `android.permission.SYSTEM_ALERT_WINDOW` - optional floating overlay dock
