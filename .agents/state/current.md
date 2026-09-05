# Current Workspace State: Srutam

## Active Focus
- Milestone: Srutam 2.0.0 Core Polish, Compact Filters, Edge-Embedded Logo Dock, Offline AI Resilience, and BYOK Model Customization.

## Verified Features in Codebase
- [x] **Milestone 100 Branch Preservation (`once-reached-100`)**:
  - Entire 2D Concept Mesh architecture preserved in dedicated branch `once-reached-100` at commit `aff0b9e`.
  - Staged for release upon reaching the 100-downloads milestone.
- [x] **Top App Bar Glassmorphic Surface (`SrutamTopAppBar.kt`)**:
  - Glassmorphic translucent surface (`#F4F5F8` at 88% opacity) with subtle bottom hairline border.
  - Bold **Srutam** across all screens with editorial italic accents for secondary labels.
- [x] **Compact Notes Filter Bar & Empty States (`FeedScreen.kt`)**:
  - Reduced filter row height to 34dp with 2-segment pill (`All Notes` and `Pending AI`).
  - Redundant "With Tasks" removed (tasks managed in dedicated Insights hub).
  - Apple-grade empty states: `NotesEmptyState` for first thought recording and `PendingAiEmptyState` for all-caught-up.
  - One-tap "Generate Insights" and "Retry AI" badges on note cards, plus batch "Process All" banner.
- [x] **Settings Screen Refinements (`SettingsScreen.kt`)**:
  - Minimal `<` chevron back button.
  - Clean sans-serif 20sp "Settings" title (no "Srutam" prefix, no "Done" button).
  - "Srutam Cloud (Default) Free" on one single row.
  - BYOK provider pills and model presets (`gpt-4o`, `claude-3-5-sonnet`, `gemini-1.5-flash`, `llama-3.3-70b-versatile`) with custom model input field.
  - Accurate audio storage directory metrics (`12.2 MB across 13 notes`).
  - Creator footer with `@iamjustkrishna` link.
- [x] **Quick Settings Dismissal Synchronization (`PersistentRecordingNotificationService.kt`)**:
  - Dismissing notification updates `AppPreferences.setPersistentNotificationEnabled(this, false)`.
- [x] **Edge-Embedded Logo Dock Overlay (`FloatingButtonService.kt`)**:
  - Discreet tab handle with gold Srutam logo docked flush against left/right edge.
  - Single tap pops out "Record", "Open App", and "Close".
  - Tap "Record" starts recording immediately and switches to in-flight studio controls with live timer, pause/resume, red stop square, and cancel.
  - Stopping or canceling auto-collapses flush back to the edge handle.
- [x] **Version 2.0.0 (`build.gradle.kts`)**:
  - Bumped to `versionCode = 4`, `versionName = "2.0.0"`.
- [x] **New 3D Brand Logo Integration**:
  - Full suite of assets generated from the new 3D fluid azure/cobalt voice and thought logo.
  - Master 1024x1024 artwork in `art/srutam_logo_1024.png`.
  - Scaled across all density buckets (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`) for `srutam_final_log.png` and `ic_launcher_foreground.png`.
  - Safe-zone padding (~62%) and soft elevation shadow applied to adaptive foreground.
  - `ic_launcher_background.xml` updated to `#FFFFFF`.
  - Legacy squircle and circular mipmap icons generated for all densities.
  - Added brand identity header card with the new 3D logo in `SettingsScreen.kt` About section.
- [x] **Settings Screen Seamless Top Bar & Apple-Style Toggles**:
  - Top bar background set to seamless `CeramicWhite` without grey artifacts.
  - Custom `SrutamSwitch` toggle with animated spring thumb physics.
  - Updated latest workable model presets for OpenAI, Anthropic (`claude-3-7-sonnet-20250219`), Gemini (`gemini-2.0-flash`), and Groq.
- [x] **Notes Filter Bar Polish**:
  - Compact 32dp height filter row with refined typography, count badges, and glowing `✦ Pending AI` badge.
- [x] **Floating Dock Icon-Only Record Trigger**:
  - Mic icon only (no text) in idle dock expansion for clean and fast capture.
- [x] **Unified Glassmorphic Top Bars & Notes Screen Alignment**:
  - Replaced raw `TopAppBar` in `FeedScreen.kt` with `SrutamTopAppBar(title = "Srutam")` and `SquircleActionButton`.
  - Matched exact 20dp start padding before "Srutam" on Notes screen with Insights (`ActionItemsScreen`) and AI (`GlobalCopilotScreen`) screens.
  - Retained clean "Srutam" title on Notes screen without secondary accent text.
  - Standardized all screen top bars (`FeedScreen`, `ActionItemsScreen`, `GlobalCopilotScreen`, `SettingsScreen`, `DetailScreen`, `ChatScreen`) on the unified glassmorphic translucent surface (`Color(0xFFF4F5F8).copy(alpha = 0.85f)`) and matching hairline bottom divider (`Color(0xFFD6E0EC).copy(alpha = 0.6f)`).
- [x] **Build & Physical Verification**:
  - Clean build `./gradlew assembleDebug` (BUILD SUCCESSFUL in 53s).
  - Strict zero em dashes policy maintained across all files.

## Planned Next Direction
- [ ] **Track C / Cloud and MCP Sync**: Sync `InsightEntity` records with Srutam Cloud / MCP server.
