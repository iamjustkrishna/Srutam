# Current Workspace State: Srutam

## Active Focus
- Milestone: Core Release Polish, Branch Separation (`once-reached-100` vs `main`), Functional Floating Overlay Dock, Notification Chronometer Optimization, and Apple-Grade Settings Overhaul.

## Verified Features in Codebase
- [x] **Milestone 100 Branch Preservation (`once-reached-100`)**:
  - Entire 2D Concept Mesh architecture preserved in dedicated branch `once-reached-100` at commit `aff0b9e`.
  - Includes `ConceptMeshCanvas.kt`, `ConceptMeshModel.kt`, `MeshNodeDetailCard.kt`, `MeshPhysicsEngine.kt`, zero-crash unified touch gestures, flexible catenary wire connections, cross-note idea connectors, and compact transcript dialogs.
  - Mesh view decoupled from core release branch `feature/core-release` and merged into `main`.
- [x] **Top App Bar Typography Harmonization (`SrutamTopAppBar.kt`)**:
  - Bold **Srutam** (`FontWeight.Bold`) across all screens (Notes, Insights, AI).
  - Non-bold italic accent text (`FontWeight.Normal`, `FontStyle.Italic`) for secondary titles (*Insights*, *AI*).
  - Verified live on physical hardware (`AAAEPVORMFIR4PWS`).
- [x] **Insights Filter Chips Redesign (`ActionItemsScreen.kt`)**:
  - Removed harsh solid white background on active chips.
  - Replaced with soft organic tint (`dotColor.copy(alpha = 0.12f)`), gentle border outline (`dotColor.copy(alpha = 0.32f)`), and radiant outer aura dot indicator.
  - Crisp dark slate active text (`#0F172A`).
  - Verified live on physical hardware for Next Steps, Ideas, and Decisions tabs.
- [x] **Settings Screen Visual Overhaul (`SettingsScreen.kt`)**:
  - Redesigned using Apple-grade CeramicWhite elevated cards with 20dp continuous squircle corners.
  - Squircle icon badges with Cobalt, Amber, and Emerald container tints.
  - Elegant Done pill button in top bar and uppercase tracked section headers.
  - Live verified on physical hardware.
- [x] **Functional On-Screen Floating Dock (`FloatingButtonService.kt` & `floating_record_button.xml`)**:
  - Expandable overlay dock accessible over any application.
  - Idle state: Direct "Record" button, "Open Srutam" shortcut, and collapse action.
  - Recording state: Live duration display (`REC mm:ss`), pause/resume toggle, red stop & save button, and discard action.
  - WindowManager dynamic re-measurement on expand/collapse preventing touch bounds clipping.
  - Verified live on device: one tap starts recording immediately, live timer increments, and stop button saves recording and collapses dock back to discreet edge pill.
- [x] **Zero-Flicker Recording Notification (`RecordingForegroundService.kt`)**:
  - Enabled native SystemUI Chronometer (`builder.setUsesChronometer(true)` and `.setWhen(System.currentTimeMillis() - durationMs)`).
  - Added `.setOnlyAlertOnce(true)` and cached `PendingIntent`s.
  - Eliminated the 1000ms notify loop during recording; notification updates strictly on state transitions.
  - Action buttons (Pause/Save) remain completely stable with zero flickering on device.
- [x] **Build & Verification**:
  - Clean build verified: `./gradlew assembleDebug` (BUILD SUCCESSFUL in 17s).
  - Deployed and live verified on physical USB hardware (`AAAEPVORMFIR4PWS`).
  - Strict zero em dashes policy maintained across all files.

## Planned Next Direction
- [ ] **Track C / Cloud and MCP Sync**: Sync `InsightEntity` records with Srutam Cloud / MCP server.
