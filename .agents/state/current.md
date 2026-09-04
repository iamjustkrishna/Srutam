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
- [x] **Build & Physical Verification**:
  - Clean build `./gradlew assembleDebug` (BUILD SUCCESSFUL).
  - Live verified on physical hardware (`AAAEPVORMFIR4PWS`).
  - Strict zero em dashes policy maintained across all files.

## Planned Next Direction
- [ ] **Track C / Cloud and MCP Sync**: Sync `InsightEntity` records with Srutam Cloud / MCP server.
