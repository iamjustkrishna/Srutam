# Architecture Decision Records (ADRs) - Srutam

## ADR-001: On-Device Speech Recognition
- **Status**: Superseded by ADR-005
- **Context**: Audio notes contain private, sensitive conversations. Users want instantaneous transcription without cloud cost, network dependency, or privacy compromises.
- **Decision**: Embed local speech recognition with cloud AI as an optional summarization layer.

## ADR-002: Dual Background Triggers (Hardware Button + Quick Settings)
- **Status**: Superseded by ADR-004
- **Context**: Users capture fleeting thoughts and need to record in < 1 second without unlocking and browsing apps.
- **Decision**: Volume down double-click caused severe hardware button collision with normal volume adjustments.

## ADR-003: Antigravity Universal Agent System
- **Status**: Accepted
- **Context**: Need unified specialist personas across ReadX and Srutam without duplicating agent configs.
- **Decision**: Orchestrate Srutam using global universal agents (mobile-ui-agent, qa-release-agent, state-keeper, ai-integration-agent, store-assets-agent, growth-marketing-agent).

## ADR-004: Apple Studio 3-Tab Hub & Floating Edge Dock
- **Status**: Accepted
- **Context**: The app required a bold, premium industrial design with frictionless voice recording and zero hardware key collisions.
- **Decision**: 
  1. 3-Tab Studio bottom dock (`Notes`, elevated 70dp center `Record` button, `Actions`).
  2. In-place modal recording console bottom sheet morphing directly from the center button without page transitions.
  3. Replace physical Volume Down trigger with an **On-Screen Floating Edge Dock** (AssistiveTouch/Dynamic Island overlay) for instant capture across any application.

## ADR-005: Srutam Voice Engine v1 & Multi-Provider AI
- **Status**: Accepted
- **Context**: Brand identity protection and avoiding vendor lock-in to a single LLM provider.
- **Decision**:
  1. Abstract on-device ASR under the proprietary branding **`Srutam Voice Engine v1 (On-Device)`**.
  2. Provide out-of-the-box **Srutam Cloud (Default with limits)** and a **BYOK (Bring Your Own Key)** option supporting OpenAI, Anthropic, Gemini, and Groq.

## ADR-006: Custom Modals, Real File Names, and Micro-Interactions
- **Status**: Accepted
- **Context**: Default Material3 rectangular dialogs clashed with the 26dp rounded aesthetic. Legacy database strings persisted auto-generated names instead of the true audio filename. Keyboard overlap obscured text input in AI chat.
- **Decision**:
  1. Replaced system AlertDialogs with `SrutamCustomDialog` featuring circular color-coded icon badges, rounded cards, and pill buttons.
  2. Extracted pure file base names in `RecordingNameFormatter.kt`, filtering placeholder strings while preserving explicit user renames.
  3. Integrated `Modifier.imePadding()` with dynamic bottom padding to keep chat inputs cleanly above the virtual keyboard across all screen sizes.
  4. Added spring-damped physics (`dampingRatio = 0.45f`, `stiffness = 400f`) and expanding radiant sonic waves to the record button.

## ADR-007: WhatsApp Slide-to-Lock, adjustResize Insets, & Editorial Brand Font
- **Status**: Accepted
- **Context**: Holding to record required users to keep their thumbs glued to the screen for long memos. In the AI copilot screen, opening the keyboard caused the input bar to jump too high above the keyboard with a large gap because Android's default `adjustPan` conflicted with Compose IME insets. The brand title required an elevated, modern editorial aesthetic.
- **Decision**:
  1. Implemented **WhatsApp-style Slide-to-Lock** in `StudioBottomBar.kt` with a floating frosted target pill and upward drag threshold (-160px) switching state to `RecordingMode.LOCKED`.
  2. Added `android:windowSoftInputMode="adjustResize"` to `MainActivity` in `AndroidManifest.xml` and docked the input bar directly above `WindowInsets.ime.union(WindowInsets.navigationBars)` with an 8dp margin, while hiding the idle floating bottom dock when `isImeVisible` is true.
  3. Bundled offline `playfair_display.ttf` font asset and applied `PlayfairDisplayFontFamily` to the primary "Srutam" TopAppBar headers.

## ADR-008: Apple Glassmorphic Modals & Uniform Pill Button Geometry
- **Status**: Accepted
- **Context**: The modal dialog for saving voice notes felt utilitarian with basic squared-off corners and mismatched button padding. The user required an Apple-grade modern glassmorphic look with strict geometric button consistency.
- **Decision**:
  1. Standardized all modal dialogs on `SrutamCustomDialog` with 32dp continuous corner curvature, 98% frosted white glass translucency, and a top-lit specular gradient border.
  2. Implemented concentric layered jewel badges (64dp glowing outer ring + 44dp vibrant gradient inner circle).
  3. Created `SrutamDialogConfirmButton` and `SrutamDialogDismissButton` enforcing exact 48dp height and continuous `CircleShape` pill rounding across all confirmation and dismissal interactions.


## ADR-009: Unified Top App Bar Architecture & Insights & Actions Hub
- **Status**: Accepted
- **Context**: The top app bar lacked consistency across primary screens (`FeedScreen`, `ActionItemsScreen`, `GlobalCopilotScreen`). Forcing every voice note into "Action Items" alienated casual talkers, journalers, and thinkers. Furthermore, completed tasks stacked up indefinitely without an archiving lifecycle.
- **Decision**:
  1. Created `SrutamTopAppBar` and `SquircleActionButton` to guarantee 100% top bar harmony across all screens (Playfair Display 30sp title, edge-to-edge status bar insets, and 42x42dp squircle action buttons).
  2. Evolved the Actions screen into the **Insights & Actions Hub** featuring a 3-way segmented capsule: `Tasks (X)`, `Key Ideas (Y)`, and `Decisions (Z)`.
  3. Implemented an active task archiving system in `AppPreferences` with Apple glassmorphic confirmation dialogs, active clearing, and instantaneous one-tap restoration.

## ADR-010: Supabase Cloud Sync & Local MCP Agent Server (@srutam/mcp-server)
- **Status**: Accepted
- **Context**: Users capture voice ideas on mobile during commutes, walks, or meetings and need immediate access to these structured thoughts, transcripts, and action items directly inside AI coding agents (Antigravity IDE, Cursor, Claude Desktop, Windsurf, Cline) without manual copying.
- **Decision**:
  1. **Cloud Backend**: Adopt Supabase (Postgres with Row Level Security and `pgvector` for semantic search).
  2. **Security & Auth**: Retain local-first offline storage on Android by default. Provide optional 1-Tap Google Sign-In (Credential Manager) and Email Magic Link to unlock sync.
  3. **Access Control**: Personal Access Tokens (API keys generated in Srutam Settings) authenticate external tools. All notes default to agent-accessible with an in-app "Private Note (Exclude from MCP)" toggle.
  4. **Data Sync Scope**: Sync text insights only (Title, Transcript, AI Summary, Key Points, Action Items, WIIFM, Vector Embeddings) for zero audio bandwidth waste and instantaneous syncing.
  5. **MCP Server Architecture**: Package `@srutam/mcp-server` under `mcp-server/` using standard `stdio` transport (`npx -y @srutam/mcp-server`), exposing 6 tools (`search_notes`, `list_recent_notes`, `get_note_detail`, `list_action_items`, `update_action_item`, `append_agent_work_log`).

## ADR-011: First-Class Room InsightEntity and Cross-Note Recurring Themes (Track A)
- **Status**: Accepted
- **Context**: In-memory JSON parsing from voice note fields was brittle and prevented cross-note aggregation. Action items, key ideas, and decisions needed first-class database lifecycles. Users also required automatic detection of recurring themes across notes without forced or false linkages.
- **Decision**:
  1. **Room Database Evolution**: Created `InsightEntity` (table `insight_items`) and `InsightDao` in database version 4 with reactive Flow queries (`getAllInsightsFlow`, `getActiveActionsFlow`, `getIdeasFlow`, `getDecisionsFlow`, `getArchivedActionsCountFlow`). Destructive migration permitted for development stage.
  2. **Upgraded AI Schema**: Prompted `AIProcessor` for structured actions, ideas, and decisions with evidence and rationale.
  3. **Insights Hub UI**: Built 4-segment capsule selector (`All`, `Next Steps`, `Ideas`, `Decisions`) in `ActionItemsScreen.kt`. `All` tab serves as executive overview with metrics, pending tasks, recurring themes, and recent decisions/ideas.
  4. **Recurring Theme Discovery**: Groups concepts appearing across 3 or more recordings, with user dismissal ("Not related") preserved in `AppPreferences`.
  5. **Floating Navigation Harmony**: Renamed bottom dock tab from Actions to Insights, with weighted distribution and single-line text constraints (`maxLines = 1`, `softWrap = false`) eliminating text wrapping.

## ADR-012: Interactive 2D Concept Mesh and Multi-Node Similarity Highlighting (Track B)
- **Status**: Accepted
- **Context**: Users reviewing high volumes of voice memos need to visualize the non-linear web of thoughts, trace how an idea evolved across notes, understand which decisions spawned follow-up tasks, and discover thematic cross-connections.
- **Decision**:
  1. **Deterministic Spatial Graph Layout**: Implemented `ConceptMeshBuilder` arranging source voice notes along an outer orbit, satellite insight nodes (actions in cobalt, ideas in amber, decisions in emerald) radiating around origin notes, and central integrative recurring theme clusters in royal violet.
  2. **Gesture-Driven Viewport**: Built `ConceptMeshCanvas` with smooth pan and pinch-to-zoom (0.4x to 2.6x), cosmic dot matrix backdrop, category filter chips (All, Next Steps, Ideas, Decisions, Notes, Themes), floating navigation controls, and floating legend pill.
  3. **Clickable Nodes & Frosted Glass Detail Card**: Tapping any node displays `MeshNodeDetailCard` above the bottom dock with category/status badges, full text, verbatim transcript evidence, origin voice note navigation, task status toggle, and connected nodes carousel.
  4. **Multi-Node Similarity Highlighting**: Selecting any node illuminates all connected and similar nodes with glowing halos, animated concentric pulse rings, and vibrant gradient strokes, while dimming unrelated nodes to 15-20% opacity.
  5. **Top Bar & Theme Card Integration**: Added top bar squircle toggle between the structured single-row capsule list and the 2D Concept Mesh, plus direct "Mesh" shortcut buttons on recurring theme cards.

## ADR-013: Milestone 100 Branch Preservation and Core Release Harmonization
- **Status**: Accepted
- **Context**: The interactive 2D Concept Mesh with physics simulation, dynamic wire sag, and cross-note concept linkers is a powerful advanced visual feature. To ensure the initial rollout remains laser-focused on lightning-fast voice capture and structured insight review, the mesh view is staged for rollout once the app reaches 100 downloads.
- **Decision**:
  1. Preserved the complete 2D Concept Mesh architecture (physics engine, wire simulation, node detail card, and touch canvas) in dedicated milestone branch `once-reached-100`.
  2. Created clean core release branch `feature/core-release` and merged into `main` with mesh view cleanly decoupled.
  3. Top bar headers: Bolded **Srutam** across all screens (Notes, Insights, AI) with non-bold italic accents for secondary labels (*Insights*, *AI*).
  4. Insights filter chips: Replaced stark white backgrounds with organic, softly tinted highlights (`dotColor.copy(alpha = 0.12f)`), radiant aura indicators, and dark slate typography.
  5. Settings screen: Redesigned with CeramicWhite cards, 20dp squircle corner curvature, squircle icon badge containers, uppercase tracked headers, and Cupertino dividers.

## ADR-014: Native Chronometer Notification and Interactive Edge Floating Dock Overlay
- **Status**: Accepted
- **Context**: The recording notification suffered from a visible 1-second button flicker caused by continuous `notificationManager.notify()` calls re-inflating RemoteViews. In addition, the on-screen floating dock needed full operational utility to start and control recordings directly without opening the full application.
- **Decision**:
  1. Native Notification Chronometer: Enabled `builder.setUsesChronometer(true)` with `.setWhen(System.currentTimeMillis() - durationMs)` and cached `PendingIntent` instances. Stopped the 1000ms notify loop during active recording so Android SystemUI updates time locally with zero button flicker. Notification updates only on discrete state changes (pause, resume, stop).
  2. Interactive Edge Floating Dock: Expanded the discreet 48dp edge pill into a multi-action console. Idle state provides direct "Record" button, app shortcut, and collapse trigger. In-flight recording state provides live duration timer, pause/resume, stop & save, and cancel actions.
  3. WindowManager Re-measurement: Added `windowManager.updateViewLayout(floatingView, params)` with `WRAP_CONTENT` on state transitions, allowing touch events to dispatch across the full expanded width without touch window clipping.

## ADR-015: Srutam 2.0.0 Core Polish, Compact Filters, Edge-Embedded Logo Dock, and BYOK Model Selection
- **Status**: Accepted
- **Context**: The Notes filter bar occupied excessive vertical screen height, with a "With Tasks" category that duplicated the dedicated Insights tab. Settings required a cleaner header, BYOK provider and model customization, and creator attribution. The floating dock needed to be embedded into the screen edge displaying the Srutam logo and supporting one-tap recording directly.
- **Decision**:
  1. Version Bump: Upgraded to `2.0.0` (versionCode 4).
  2. Compact Notes Filter Bar: Reduced height to 34dp with a 2-segment pill control (`All Notes` and `Pending AI`). Removed redundant "With Tasks" segment.
  3. Dedicated Empty States: Built custom empty states for Notes (`NotesEmptyState` guiding capture) and Pending AI (`PendingAiEmptyState` celebrating all caught up).
  4. Offline AI Resilience: Exposed one-tap "Generate Insights" and "Retry AI" badges on note cards, batch "Process All" banner on the Pending AI filter, and auto-sync upon network reconnection.
  5. Settings Screen Modernization: Minimal `<` chevron back button, clean 20sp "Settings" sans-serif title without "Done" button or "Srutam" prefix, single-line "Srutam Cloud (Default) Free", BYOK provider pills and model presets with custom model editing, real audio storage metrics, and creator footer linked to `@iamjustkrishna`.
  6. Quick Settings Dismissal Sync: Synchronized persistent notification dismissal with `AppPreferences.setPersistentNotificationEnabled(this, false)`.
  7. Edge-Embedded Logo Dock: Overlay handle tab displaying the gold Srutam logo docked flush on left/right screen edge. Tapping expands options (Record, Open App, Close). Recording switches to studio controls with live timer, pause/resume, red stop & save square, and cancel. Stopping or canceling auto-collapses flush back to the edge handle.

## ADR-016: Unified Glassmorphic Top Bars and Strict Horizontal Alignment Across Screens
- **Status**: Accepted
- **Context**: The Notes screen (`FeedScreen.kt`) was using a raw Material 3 `TopAppBar` with default 16dp horizontal padding and transparent background, whereas `ActionItemsScreen.kt` and `GlobalCopilotScreen.kt` used `SrutamTopAppBar` with 20dp horizontal padding, 30sp Playfair Display bold typography, a glassmorphic surface (`Color(0xFFF4F5F8)` at 85% opacity), and a hairline divider line (`Color(0xFFD6E0EC)` at 60% opacity). This caused a noticeable horizontal shift and visual inconsistency when switching tabs between Notes, Insights, and AI. Additionally, secondary screens (`SettingsScreen`, `DetailScreen`, `ChatScreen`) had inconsistent opaque headers.
- **Decision**:
  1. Replaced raw `TopAppBar` in `FeedScreen.kt` with `SrutamTopAppBar(title = "Srutam")` and `SquircleActionButton` for Search and Settings.
  2. Fixed start padding before "Srutam" on the Notes screen to 20dp, achieving pixel-perfect alignment with Insights and AI screens with zero jumping during tab switching.
  3. Kept clean "Srutam" title on the Notes screen without secondary accent text.
  4. Standardized all screen top bars (`FeedScreen`, `ActionItemsScreen`, `GlobalCopilotScreen`, `SettingsScreen`, `DetailScreen`, `ChatScreen`) on the unified glassmorphic translucent background (`Color(0xFFF4F5F8).copy(alpha = 0.85f)`) and matching hairline bottom divider (`Color(0xFFD6E0EC).copy(alpha = 0.6f)`).

## ADR-017: Screen Title Weight, Deep Edge Dock Embedding, Audio Feedback, and Modal Constraints
- **Status**: Accepted
- **Context**: The user requested 5 distinct refinements: (1) bolder screen titles in Notes, Insights, and AI screens, (2) deeper side dock edge embedding so it sits nestled into the screen bezel, (3) instant toast confirmation on pause/resume and save/discard, (4) compact save voice note modal with a disabled save button when the title field is empty, and (5) removal of unnecessary shadow behind the 3D logo in the app launcher icon.
- **Decision**:
  1. **Bold Screen Titles (`SrutamTopAppBar.kt`)**: Elevated title weight to `FontWeight.ExtraBold` and secondary accent to `FontWeight.SemiBold`, giving the Playfair Display font solid visual anchor across Notes, Insights, and AI headers.
  2. **Deep Edge Dock Embedding (`FloatingButtonService.kt`)**: Added `FLAG_LAYOUT_NO_LIMITS` with an 8dp negative x-offset and 52dp pill width. Applied asymmetric padding (11dp embedded side, 6dp outer side) so the 3D logo centers naturally within the visible 44dp protruding tab while 8dp remains embedded into the screen bezel. Ensured service automatically starts on app launch when enabled in preferences.
  3. **Toast Feedback on Recording Events (`FloatingButtonService.kt`, `Navigation.kt`)**: Added standard toast feedback on pause ("Recording paused"), resume ("Recording resumed"), save ("Voice note saved"), and discard ("Recording discarded").
  4. **Compact Save Note Modal with Blank Name Validation (`FeedScreen.kt`)**: Reduced dialog width to 86% of screen, padding to 20dp, and badge to 48dp. Added `enabled` property to `SrutamDialogConfirmButton`, dynamically disabling the button with a muted slate palette and inactive click listener when the note name is empty or whitespace-only.
  5. **Clean Launcher Icon Logo**: Re-rendered adaptive launcher foregrounds and legacy mipmaps directly from the clean master 1024x1024 asset without artificial drop shadow layers behind the 3D mark.

## ADR-018: First Impression Splash and Intelligent Android Permission Onboarding Lifecycle
- **Status**: Accepted
- **Context**: When first opening Srutam, users require context on why permissions are requested rather than being met with abrupt raw system dialogs. Furthermore, permissions vary across Android versions (API 29 to API 36): microphone (`RECORD_AUDIO`) is required everywhere; storage requires `READ_MEDIA_AUDIO` on Android 13+ (API 33+) versus `READ_EXTERNAL_STORAGE` (+ `WRITE_EXTERNAL_STORAGE` on API 29) on older versions; file deletion is handled without extra permissions via `MediaStore.createDeleteRequest()` on API 30+; notifications (`POST_NOTIFICATIONS`) are required on API 33+ for background foreground services. Handlers were needed for soft denials, permanent denials (directing to Settings), and seamless automatic transit to the main app upon grant.
- **Decision**:
  1. **Branded Animated Splash Screen (`SrutamSplashScreen.kt`)**: Created an editorial splash screen featuring the 3D azure voice logo with animated spring scale and fade-in, Playfair Display ExtraBold "Srutam" title, and italic subtitle "Pure Voice, Crystallized Thought". Transitions smoothly after 1.2 seconds.
  2. **Permissions Onboarding Screen (`PermissionsOnboardingScreen.kt`)**: Designed a welcome surface with security shield badge and status cards for Microphone Access, Audio Storage Access, and Notifications Access (API 33+). Each card displays real-time status badges ("Granted" with green checkmark versus "Required" / "Recommended").
  3. **Rationale and Settings Banners**: Added contextual soft-denial rationale explaining why microphone and audio access are required. Added a permanent-denial banner with a direct "Open App Settings" CTA invoking `ACTION_APPLICATION_DETAILS_SETTINGS` when permissions are blocked by the user.
  4. **Lifecycle-Aware State Machine (`MainActivity.kt`)**: Implemented an `AppStage` state machine (`SPLASH`, `PERMISSIONS`, `MAIN`) with `AnimatedContent` cross-fades. Registered a lifecycle observer (`ON_RESUME`) alongside Accompanist permissions so returning from system settings immediately verifies granted status and auto-routes to the main feed. Subsequent launches bypass the onboarding screen entirely.

## ADR-019: Long Audio Seeking Resilience and Variable Font Boldness Calibration
- **Status**: Accepted
- **Context**: Seeking in long audio files was failing or snapping back to the beginning due to three core issues: (1) standard `MediaPlayer.seekTo(int)` snapped to distant sync frames on long AAC/M4A containers instead of exact timestamps, (2) asynchronous seeking caused the 100ms progress update loop to read stale positions and clobber the seek before the hardware seek finished, and (3) gestures were limited to static taps without drag scrubbing. Separately, the Notes screen title "Srutam" appeared regular weight because Compose lacked `FontVariation.Settings` for the variable font `playfair_display.ttf`.
- **Decision**:
  1. **Frame-Exact Seeking (`AudioPlayer.kt`)**: Adopted `player.seekTo(position.toLong(), MediaPlayer.SEEK_CLOSEST)` on Android 8.0+ (API 26+) ensuring accurate sample-level positioning even in long compressed AAC files.
  2. **Asynchronous Seek Guard & Completion Listener (`AudioPlayer.kt`)**: Added `@Volatile isSeeking` flag to pause progress update polling during seek transitions. Registered `setOnSeekCompleteListener` to update current playback position only after the hardware decoder has confirmed the new offset. Added `pendingSeekPosition` buffer to queue seeks made during async preparation.
  3. **Continuous Drag Scrubbing (`FeedScreen.kt`, `DetailScreen.kt`)**: Enhanced waveform scrubbers with `detectHorizontalDragGestures` and `onPress` instant touch-down seeks, letting users fluidly drag and scrub through recordings of any length.
  4. **Variable Font Weight Calibration (`Type.kt`, `SrutamTopAppBar.kt`)**: Configured `FontVariation.Settings(FontVariation.weight(...))` across Normal (400) to ExtraBold (800) in `PlayfairDisplayFontFamily`. Set default title weight in `SrutamTopAppBar` to `FontWeight.Bold` (weight 700), giving "Srutam" a polished, slightly bolder appearance without excessive thickness.

## ADR-020: WorkManager Background AI Processing Pipeline, Foreground Notifications, and Offline Resilience
- **Status**: Accepted
- **Context**: Executing local Sherpa transcription and Gemini cloud summary generation directly within Activity ViewModels (`viewModelScope`) led to premature cancellation and process termination when users backgrounded the app, locked their phones, or swiped the app away. Furthermore, users received no notifications regarding analysis progress, completion, or offline state handling. The user required background persistence, live staged progress notifications, distinct completion notifications, subtle handling when internet is unavailable (without explicitly badgering the user with "waiting for internet" notifications), and batch AI processing from the notes list.
- **Decision**:
  1. **WorkManager with Foreground Notifications (`AiProcessingWorker.kt`)**: Adopted `CoroutineWorker` integrated with WorkManager and `setForegroundAsync` using `ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC`. Guarantees jobs survive backgrounding, phone locking, and app closure without Android OS low-memory termination.
  2. **Staged Dynamic Progress Notifications**: Created `ai_processing_channel` (importance LOW, silent) displaying live stages: "Transcribing [Title]..." during local Sherpa ONNX transcription, and "Generating Insights: [Title]..." during cloud Gemini summary generation. For batches, displays "Analyzing note X of Y: [Title]" with an ongoing deterministic progress bar.
  3. **Subtle Offline Handling & Auto-Resume (`AiSummaryNetworkWorker.kt`)**: Transcription runs purely locally and offline. If internet is unavailable when generating the summary, the recording is marked `SUMMARY_PENDING_OFFLINE` in Room database. The ongoing notification is dismissed cleanly without exposing "waiting for internet" alerts. Scheduled `AiSummaryNetworkWorker` with `NetworkType.CONNECTED` constraint to silently resume summary generation and alert the user once connectivity is restored.
  4. **Completion Notification with Note Deep Link**: Created `ai_completion_channel` (importance DEFAULT with sound/vibration) posting "AI Insights Ready" upon completion. Tapping the notification opens `MainActivity` with `EXTRA_OPEN_RECORDING_ID`, auto-navigating directly to the specific note (or main notes feed for batches).
  5. **Batch AI Selection Action (`FeedScreen.kt`, `AudioFilesViewModel.kt`)**: Added an AI sparkle action button (`Icons.Default.AutoAwesome`) to the multi-select TopAppBar next to Delete. Tapping it queues all selected voice notes for background processing via `AiProcessingWorker.enqueueProcessing()`, displays an instant confirmation toast, and exits selection mode.
## ADR-021: Floating Dock Right-Edge Dynamic Anchoring and Phone-Lock Foreground Persistence
- **Status**: Accepted
- **Context**: Two issues impacted the on-screen floating dock (`FloatingButtonService`):
  1. Right-edge positioning: Expanding the dock when anchored to the right edge used left-relative coordinates with a hardcoded width estimate (165dp vs 146dp actual), causing the menu to hover with a 27dp gap away from the right screen edge. Additionally, the left-to-right button layout placed the dismiss button directly beneath the user's thumb instead of the primary record action.
  2. Phone-lock disappearance: The floating dock was implemented as an ordinary background `Service` with `START_STICKY` to avoid notification shade clutter. However, on modern Android (and especially aggressive vendor battery daemons like Realme UI / ColorOS), non-foreground services are killed within seconds of screen lock, tearing down all `WindowManager` overlay views.
- **Decision**:
  1. **Dual Native Gravity (`FloatingButtonService.kt`)**: Replaced manual left-relative math with dynamic gravity (`Gravity.TOP or Gravity.START` on the left, `Gravity.TOP or Gravity.END` on the right). With `Gravity.END`, collapsed `x = -embedOffsetPx` (-8dp) embeds into the right bezel and expanded `x = 8dp` (+8dp) anchors the card with an exact 8dp margin, completely eliminating width guessing across all screen sizes and display cutouts.
  2. **Mirrored Layout Direction**: Applied `expandedIdleLayout.layoutDirection = if (isDockedLeft) LAYOUT_DIRECTION_LTR else LAYOUT_DIRECTION_RTL`, ensuring the primary record action and in-flight timer sit immediately adjacent to the right edge handle under the user's thumb, with dismiss at the outer tip.
  3. **Dock Edge & Position Persistence (`AppPreferences.kt`)**: Added `isFloatingDockOnLeft()` and `getFloatingDockY()`, persisting the dock's side and vertical offset across launches.
  4. **Foreground Service Promotion (`FloatingButtonService.kt`, `AndroidManifest.xml`)**: Promoted `FloatingButtonService` to an official Foreground Service with `foregroundServiceType="specialUse"` and permission `FOREGROUND_SERVICE_SPECIAL_USE`.
  5. **Low-Importance Silent Notification (`srutam_floating_dock_channel`)**: Configured the notification with `NotificationManager.IMPORTANCE_LOW` (silent, no sound, no vibration, no popup) titled "Srutam Floating Dock Active". Tapping opens Srutam; a "Hide Dock" action allows one-tap dismissal.
  6. **Start Foreground Service Callers (`MainActivity.kt`, `SettingsScreen.kt`)**: Updated service launches to `ContextCompat.startForegroundService()` to prevent background execution limits.

## ADR-022: Headless Robolectric Native Graphics and Roborazzi Screen Matrix Testing
- **Status**: Accepted
- **Context**: Visual verification across varied Android form factors (compact phones, modern tall phones, foldables, 7-inch tablets, and 10-inch tablets) previously required slow, resource-heavy Android Virtual Devices (AVDs) or physical hardware. Developers lacked a fast, headless, automated mechanism to snapshot and inspect every app screen across canonical device dimensions during development and CI.
- **Decision**:
  1. **Robolectric Native Graphics & Roborazzi**: Configured Roborazzi 1.37.0 with Robolectric 4.14.1 running in Native Graphics (RNG) mode (`robolectric.graphicsMode=NATIVE`), enabling pixel-accurate hardware-accelerated Skia rendering directly on the host JVM in ~45 seconds without emulators.
  2. **Canonical 5-Device Matrix**: Standardized on 5 device configurations covering the Android device spectrum:
     - `phone-compact`: 360x640 dp, 320 dpi (xhdpi)
     - `phone-standard`: 411x891 dp, 420 dpi
     - `foldable`: 673x841 dp, 420 dpi (book-fold inner screen)
     - `tablet-7inch`: 600x960 dp, 240 dpi (hdpi)
     - `tablet-10inch`: 1280x800 dp, 160 dpi (mdpi)
  3. **Stateless UI Decoupling**: Decoupled 8 core screens into stateless `*Content(...)` composables: `01_splash`, `02_permissions`, `03_feed_empty`, `04_feed_populated`, `05_detail`, `06_insights_hub`, `07_copilot_chat`, and `08_settings`.
  4. **Deterministic Mock Previews**: Created `ScreenMatrixPreviews.kt` providing self-contained, realistic mock models and data for all screens with zero network or database dependencies.
  5. **Snapshot Storage Hierarchy**: Saved full-screen PNGs to `screenshots/screen-matrix/{device}/{screen}.png` via relative path resolution from subproject `app/` to project root.
  6. **Universal Specialist Agent & Skill**: Registered `screen-matrix-agent` in `AGENTS.md` and created the `/screen-matrix` skill in the universal agent suite to automate future screen matrix sweeps on demand.

## ADR-023: Insights Segmented Capsule Filters and Tailored Light Gradients
- **Status**: Accepted
- **Context**: The Insights top filter bar suffered from wrap_content layout asymmetry where active tabs floated as isolated small pills leaving unbalanced grey gaps on standard phones, foldables, and tablets. Tab switching also caused sibling items to jitter due to dynamic horizontal padding.
- **Decision**:
  1. Converted the filter bar into a true 38dp segmented control with 3dp container insets and equal-width partition weighting (`Modifier.weight(1f).fillMaxHeight()`), guaranteeing an exact 1/3 slot per tab with zero position shifting across all screen profiles.
  2. Applied category-tailored light horizontal gradients to active tabs: Cobalt Blue (`#2563EB` 18% to 8%) for Next Steps, Warm Amber (`#D97706` 18% to 8%) for Ideas, and Teal/Emerald (`#0D9488` 18% to 8%) for Decisions, accompanied by 32% opacity border strokes and glowing concentric jewel dots.

## ADR-024: Note Details Reactive Insights Tab and Real-Time Task Toggling
- **Status**: Accepted
- **Context**: The Note Details screen previously featured a static "✓ Tasks" tab that only rendered a plain bullet list parsed from recording JSON strings. It lacked integration with first-class Room `InsightEntity` models, lacked interactive task completion, and omitted extracted Key Ideas and Decisions for that specific voice note.
- **Decision**:
  1. Replaced the "✓ Tasks" tab with an Apple-style `"💡 Insights"` tab positioned in the third slot: `[ ✦ Summary ]  [ 📄 Transcript ]  [ 💡 Insights ]` (Option 2).
  2. Connected `DetailViewModel` to Room's reactive `InsightDao.getInsightsByRecordingIdFlow(recordingId)` to stream live `InsightEntity` records.
  3. Added `toggleActionComplete(insight: InsightEntity)` executing asynchronous status updates (`OPEN` vs `COMPLETED`) directly in Room database with timestamp tracking.
  4. Implemented polished, sectioned insight cards for Next Steps (22dp rounded checkbox, completion counter pill, strikethrough styling), Key Ideas (amber bulb badge, quote containers), and Decisions (emerald checkmark badge, rationale, quote containers).
  5. Built in graceful fallback to legacy JSON arrays for older notes without database insight rows.
  6. Added `capture_05_detail_insights` to Roborazzi test suite and generated verified screenshots across 5 screen profiles.

## ADR-025: Srutam 2.1 Multi-Track Delivery and Pre-2.1 Instant Rollback Protocol
- **Status**: Accepted
- **Context**: Srutam 2.1 introduced 5 sequential architectural feature tracks: Auto-AI background processing, BYOK onboarding flow, Cosmic Void dark mode, adaptive tablet 3-panel workspace, and actionable AI reminders with exact alarms. To guarantee production safety and zero risk of breaking existing functionality, a permanent, immutable rollback protocol is required so developers and agents can instantly revert to the pre-2.1 working baseline if needed.
- **Git Safety Anchors**:
  1. **Pre-2.1 Base Commit**: `9d70583933842d61e97e8567cda0a3c54382cac3` (commit message: "r8 reduc").
  2. **Pre-2.1 Safety Tag**: `v2.0.0-pre-2.1` pointing directly to `9d70583`.
  3. **Pre-2.1 Archive Branch**: `archive/v2.0.0-base` tracking commit `9d70583`.
  4. **Srutam 2.1 Release Tag**: `v2.1.0` pointing to the tip of `main` containing all 5 merged feature tracks.
- **Rollback Protocol ("IF CONDITION TO DO WHAT")**:
  - **IF condition**: If any unexpected runtime regression, database migration issue, or device crash occurs in Srutam 2.1 and immediate reversion to the pre-2.1 working baseline is needed:
    - **Step 1 (Temporary inspection/build without touching main)**:
      ```bash
      git checkout v2.0.0-pre-2.1
      ./gradlew assembleDebug
      ```
    - **Step 2 (Hard revert main to pre-2.1 baseline)**:
      ```bash
      git checkout main
      git reset --hard v2.0.0-pre-2.1
      ```
    - **Step 3 (Safe non-destructive revert alternative on main)**:
      ```bash
      git checkout main
      git revert -m 1 97cef0e cd6f4e4 fec71ea 48f1b0a c9d0eb1
      ```
    - **Step 4 (Return back to 2.1 working version)**:
      ```bash
      git checkout v2.1.0
      ```

## ADR-026: Tablet Workspace 2-Panel Portrait & Landscape Architecture Redesign
- **Status**: Accepted
- **Context**: The tablet experience on Android tablets (both 7-inch portrait and 10-inch portrait/landscape) needed a dedicated, productive layout matching the approved design spec. The layout required a left sidebar with brand wordmark, tab navigation, active note pill highlight, and pinned new note button, paired with an executive detail workspace featuring audio scrubbers, segmented tab switcher, 3-column insight cards (Next Steps, Key Ideas, Key Decisions), transcript preview with embedded search, and referenced notes.
- **Decision**:
  1. **2-Panel Unified Architecture (`TabletWorkspaceScreen.kt`)**: Replaced raw phone scaling on tablets with a responsive 2-panel architecture:
     - Left Sidebar: 240dp on compact tablets (<768dp) and 280dp on standard tablets (>=768dp). Houses "Srutam" wordmark, 4 navigation tabs (Notes, Insights, AI, Settings), Recent notes list with active note capsule highlight, and pinned "+ New Note" bottom button.
     - Right Detail Workspace: Full-featured note workspace with back navigation, action buttons (Share, Bookmark, More), title and date, "Summarized" status badge, audio player with custom waveform scrubber and 10s seek/speed controls, segmented view switcher (`Summary` | `Transcript` | `Insights`), and responsive cards.
  2. **Responsive 3-Column Insight Cards**: On standard wide screens (>=768dp), Next Steps, Key Ideas, and Key Decisions sit in equal 1/3 columns. On compact portrait tablets (<768dp), cards transition smoothly to a horizontal scroll row with 240-260dp fixed widths, preventing text wrapping or column squishing.
  3. **Transcript Inline Search**: Embedded search bar inside the transcript preview card allows instant keyword filtering of spoken lines with timestamp seeking.
  4. **Dual Appearance**: Full support for both Light Mode and Cosmic Void Dark Mode across all sidebar pills, waveform scrubbers, cards, and input fields.
  5. **Portrait Summary Cleanup**: In portrait tablet orientation (`Configuration.ORIENTATION_PORTRAIT` or `screenWidthDp < screenHeightDp`), suppressed the 3-column cards (`Next Steps`, `Key Ideas`, `Key Decisions`) from the Summary section to maintain a clean vertical flow and eliminate card squishing. The dedicated `Insights` tab remains accessible for full-screen breakdown. In landscape mode, the 3-column cards remain visible side by side.
  6. **Centered Floating Studio Record Shutter**: Replaced the pinned sidebar "+ New Note" button with a floating record shutter positioned at `Alignment.BottomCenter` within a root `Box` overlaid above all screens. Idle state is a 56dp circular crimson ring button with spring tactile feedback; active state animates into a centered studio capsule with trash/discard, pulsing recording dot, live `MM:SS` timer, pause/resume, and red stop square button.
  7. **Universal Recording Save Flow**: Hoisted `SaveRecordingDialog` in `Navigation.kt` outside the `isTablet` branch, ensuring voice notes recorded on tablets can be titled, saved to the Room database, and written to disk with the exact same UX as phone.
  8. **Roborazzi Native Verification**: Captured and verified pixel-accurate snapshots across both `tablet-7inch` and `tablet-10inch` profiles in Light and Cosmic Dark modes.

## ADR-027: Tablet Note Management Actions & FileProvider Audio Sharing
- **Status**: Accepted
- **Context**: The tablet executive detail workspace provides header buttons for sharing, renaming, and deleting audio notes. These needed to be safely wired to the underlying storage and Room repository while complying with modern Android scoped storage and file sharing policies.
- **Decision**:
  1. **FileProvider Config (`file_provider_paths.xml`)**: Configured AndroidX `FileProvider` with authority `${applicationId}.provider` and paths covering `external-path`, `external-files-path`, `files-path`, and cache directories to safely share recordings via content URIs without file exposure exceptions.
  2. **Share Audio Intent**: Implemented `onShareFile` using `FileProvider.getUriForFile` and `Intent.ACTION_SEND` with `FLAG_GRANT_READ_URI_PERMISSION`, presented through `Intent.createChooser`.
  3. **Note Renaming and Deletion**: Wired `onRenameFile` to `viewModel.renameRecording` and `onDeleteFile` to `viewModel.deleteAudioFile` with immediate UI feedback via system Toasts.

## ADR-028: Tablet Dynamic Note Selection & Empty-Audio AI Processing Resilience
- **Status**: Accepted
- **Context**: 
  1. In the tablet layout, selecting or switching between different newly recorded notes appeared not to update the right detail pane. Both notes showed identical summary cards and key points.
  2. Clicking "Process with AI" or "Retry" on short or silent notes failed immediately and left the recording permanently in ERROR state.
- **Decision**:
  1. **Decoupled Fallback Mock Text**: `TabletExecutiveDetailWorkspace` previously fell back to static mock text ("The speaker discusses the product strategy...") whenever `recording?.summary` was null or empty. Because newly created notes had no summary yet, selecting any unprocessed note displayed the exact same mock text, making the right panel look frozen. We removed static fallback copy for real user notes and introduced clear, dedicated status cards:
     - In-flight: Animated circular progress indicator with live stage text ("Transcribing audio locally..." or "Analyzing with AI...").
     - Error: "Processing Incomplete" card showing the error detail and an immediate "Retry AI Processing" button.
     - Pending / Unprocessed: Clean "Ready for AI Insights" card with a prominent "Process Note with AI" CTA.
     - Summarized: Displays the authentic summary, WIIFM, bullet points, next steps, ideas, and decisions.
  2. **Empty / Silent Audio Handling in Worker**: `AiProcessingWorker` previously threw an uncaught `IllegalStateException: Transcript is empty` when `LocalTranscriber` returned blank text on silent or quiet microphone recordings, permanently setting `aiStatus = RecordingAiStatus.ERROR`. We now detect `transcript.isNullOrBlank()` and gracefully set a polite transcript ("No audible speech detected in this recording.") with an explanatory summary, marking the note as `READY` without failure.
  3. **Offline Fallback on Network / API Errors**: In the event of online LLM network or quota failures during insight generation, `AiProcessingWorker` now automatically falls back to `aiProcessor.generateFallbackInsights(transcript)`, ensuring the user always receives structured key points and next steps derived directly from the on-device transcription rather than being stuck in ERROR.

## ADR-029: Tablet Portrait Insights Capsule Switcher & Gemini 2.5 Flash Migration
- **Status**: Accepted
- **Context**: On tablets in portrait orientation (`Configuration.ORIENTATION_PORTRAIT`), rendering 3 parallel columns for Next Steps, Ideas, and Decisions squeezed each column to unreadable widths. Users needed a 3-option switcher on top to switch and view each category's content with full mobile parity. Clicking origin note links from inside an insight needed to stay within the 2-panel tablet interface. Additionally, cloud AI requests frequently failed with `503 UNAVAILABLE: No capacity available for model gemini-3.8-flash on the server`.
- **Decision**:
  1. **Orientation-Aware Insights (`TabletWorkspaceScreen.kt`)**: In `TabletInsights3ColumnWorkspace`, check `isPortrait`. In portrait mode, render the single-row `SingleRowInsightsCapsule` on top with options `Next Steps`, `Ideas`, and `Decisions`. Render the selected tab's full-width content below it with unconstrained internal scrolling `LazyColumn`. In landscape mode, preserve the 3 parallel columns.
  2. **Mobile Feature Parity & Theming (`ActionItemsScreen.kt`)**: Exposed `SingleRowInsightsCapsule`, `NextStepsTab`, `IdeasStreamTab`, and `DecisionsTimelineTab` as `internal`. Added `LocalIsCosmicDark.current` theme awareness to `SingleRowInsightsCapsule` and `CapsuleTabItem` for proper contrast and color-coded indicator dots (Blue for Next Steps, Amber for Ideas, Green for Decisions).
  3. **In-Workspace Tablet Navigation (`TabletWorkspaceScreen.kt`, `Navigation.kt`)**: Tapping an origin note chip from any insight item resolves the corresponding note in `recordingsByPath`, updates `selectedFilePath`, and switches `currentTab` to `RootTab.NOTES`, immediately displaying the note in the executive detail pane without opening the phone `DetailScreen`.
  4. **Gemini 2.5 Flash Migration (`AIProcessor.kt`, `AppPreferences.kt`, `SettingsScreen.kt`, `BYOKOnboardingScreen.kt`)**: Replaced `gemini-3.8-flash` with the stable, high-throughput `gemini-2.5-flash` model across all cloud inference methods and preset menus, completely eliminating 503 capacity errors and dropping cloud summarization latency to ~1.2 seconds.

## ADR-030: Unified Dialog Architecture & Cross-App Modal Consistency
- **Status**: Accepted
- **Context**: Note deletion dialogs varied across the app: the tablet workspace used basic rectangular Material3 `AlertDialog`s with flat text buttons, phone `DetailScreen` had no confirmation dialog prior to deletion, `ActionItemsScreen` used default `AlertDialog` for archiving tasks, and `FeedScreen` contained hundreds of lines of duplicated custom dialog code. Users requested standardizing note deletion on the signature `SaveRecordingDialog` aesthetic (26dp curvature, dual-circle gradient icon badge, and pill action buttons) and extracting the design into reusable, parameter-driven dialogs across all screens.
- **Decision**:
  1. **Centralized Dialog Framework (`SrutamDialogs.kt`)**:
     - `DialogBadgeType`: `PRIMARY` (Blue), `DESTRUCTIVE` (Crimson), `WARNING` (Amber), `INFO` (Slate), and `SUCCESS` (Emerald) defining cohesive badge gradient pairs and glow shadows.
     - `SrutamDialogIconBadge`: Dual-circle badge (56dp outer radial aura + 44dp inner gradient circle) with adaptive light/dark border styling.
     - `SrutamDialogConfirmButton` & `SrutamDialogDismissButton`: 42dp pill buttons with gradient fills, tactile scale feedback, and disabled state handling.
     - `SrutamCustomDialog` & `SrutamStandardDialog`: Root modal containers with 26dp rounded corners, `CosmicVoidCard` / frosted white surface, hairline gradient border, scrim, and slots for title, subtitle, custom content, and actions.
  2. **Domain Dialog Implementations**:
     - `DeleteConfirmationDialog` & `MultiDeleteConfirmationDialog`: Red destructive badge with trash icon, explicit note title confirmation, and prominent red gradient pill action.
     - `RenameDialog`: Blue primary badge with edit pencil, integrated text field, clear button, and validation preventing empty names.
     - `SaveRecordingDialog`: Blue primary badge with mic/save icon, audio duration metrics, and save action.
     - `AudioInfoDialog`: Slate info badge displaying file metadata (path, duration, size, sample rate, date).
     - `ArchiveTasksDialog`: Warning amber badge confirming task archiving.
  3. **Universal Screen Adoption**:
     - `TabletWorkspaceScreen.kt`: Replaced raw Material3 dialogs for delete and rename with `DeleteConfirmationDialog` and `RenameDialog`.
     - `DetailScreen.kt`: Added `DeleteConfirmationDialog` to the top bar delete action and updated rename dialog to `RenameDialog`.
     - `FeedScreen.kt`: Replaced 729 lines of local dialog code with imports from `SrutamDialogs.kt`.
     - `ActionItemsScreen.kt`: Replaced raw `AlertDialog` with `ArchiveTasksDialog`.
     - `Navigation.kt`: Standardized `SaveRecordingDialog` import.
  4. **Verification**: Validated compilation, unit tests, and live interactive rendering on Android tablet emulator in both Light and Cosmic Void Dark modes.

## ADR-031: Tablet Portrait AI Screen Polish & Mobile-Parity Suggested Questions
- **Status**: Accepted
- **Context**: In tablet portrait orientation, the AI screen (`TabletCopilot3PanelWorkspace`) divided the workspace into an awkward 3-panel / 2-column layout with a static sidebar mislabeled "Recent Questions". On standard mobile screens (`GlobalCopilotScreen`), suggested questions appear inside the chat view as a single horizontal scrolling row (`LazyRow`) with interactive chips. Furthermore, tablet AI queries were disconnected, and the bottom floating record button collided with the query input bar in portrait mode.
- **Decision**:
  1. **Responsive Orientation Layout (`TabletWorkspaceScreen.kt`)**: Check `isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT || configuration.screenWidthDp < configuration.screenHeightDp`. In portrait mode, remove the redundant left "Recent Questions" column and suppress the right "Sources" column, giving the active chat area full width. In landscape mode on large tablets, preserve the right "Sources" column.
  2. **Mobile-Parity Suggested Questions (`TabletWorkspaceScreen.kt`)**: Placed "Suggested Questions" directly inside the chat feed's `LazyColumn` as a horizontal `LazyRow` of chips (`💡 <question>`) matching `GlobalCopilotScreen`. Tapping any chip immediately invokes `onSendMessage(query)`.
  3. **Rich Message Cards & In-Workspace Citations**: Render user message pills on the right and AI response cards on the left with the "Srutam AI" header, clean typography, and cited notes pills. Tapping a cited note pill immediately switches to `RootTab.NOTES`, selects the note, and displays its details in the tablet workspace.
  4. **Inset Handling & Bottom Floating Shutter Collision Guard**: Added `.windowInsetsPadding(WindowInsets.navigationBars)` and responsive bottom padding (96dp in portrait idle, 12dp when IME virtual keyboard is open) so the query input bar clears the floating record button with clean spacing.
  5. **Direct ViewModel Binding (`Navigation.kt`)**: Passed `viewModel = viewModel` to `TabletWorkspaceLayout` in `Navigation.kt`, enabling internal handling of copilot queries via `viewModel.queryAllVoiceNotes` with offline fallback guards.
  6. **Verification**: Clean `./gradlew assembleDebug` build, all unit tests passed (`./gradlew testDebugUnitTest`), and verified interactively on Android tablet emulator (`emulator-5554`, 2560x1600) with tap tests and screenshots.

## ADR-032: Floating Dock Reactive Auto-Update & Tablet Sidebar Appearance Animation
- **Status**: Accepted
- **Context**: When recording and saving a voice note using the on-screen floating dock (`FloatingButtonService`), the toast "Voice note saved" appeared, but in tablet layout portrait orientation, the left unified sidebar (`TabletUnifiedSidebar`) list of voice notes did not update. The user had to restart or switch screens to see the new note. Additionally, when new notes arrived, they appeared abruptly without entrance animation or visual feedback.
- **Root Cause**:
  1. `FloatingButtonService` sends `ACTION_STOP_RECORDING` directly to `RecordingForegroundService`, which writes the `.m4a` file and inserts the Room DB row via `triggerAutoAiForFile()`.
  2. However, `AudioFilesViewModel.loadAudioFiles()` (which queries disk via `AudioFileReader.getAudioFiles()`) was only called in `Navigation.kt` when `SaveRecordingDialog` was dismissed (dialog was bypassed by floating dock saves).
  3. `AudioFilesViewModel` observed Room recordings in `observeRecordings()`, but did not reload the underlying `_audioFiles` list when Room row count differed from disk audio file count.
  4. `TabletUnifiedSidebar`'s `LazyColumn` items lacked entrance transition animations (`animateItem`) and visual indicators for recently saved notes.
- **Decision**:
  1. **Triple-Redundancy Event Pipeline**:
     - `RecordingForegroundService`: Added companion `_recordingSavedEvents = MutableSharedFlow<File>(extraBufferCapacity = 1)` and public `recordingSavedEvents: SharedFlow<File>`. Emits saved file immediately upon `stopRecording()`.
     - `AudioFilesViewModel`: Subscribes to `RecordingForegroundService.recordingSavedEvents` in `init` and triggers `loadAudioFiles()`. Additionally in `observeRecordings()`, reloads audio files if `recordings.isNotEmpty() && recordings.size != _audioFiles.value.size`.
     - `Navigation.kt`: Tracks `wasRecording` in the service polling loop; upon detecting recording transition from `true` to `false`, triggers `viewModel.loadAudioFiles()`.
  2. **Smooth Appearance Animation (`TabletWorkspaceScreen.kt`)**:
     - In `TabletUnifiedSidebar`'s `LazyColumn`, applied `Modifier.animateItem(fadeInSpec = tween(durationMillis = 400), placementSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))` to item surfaces.
     - Also applied `animateItem` to `TabletNotesFeed`'s `TabletCompactAudioCard` items for feed views.
  3. **Visual Feedback for Fresh Captures**:
     - Added `isRecentlySaved = (System.currentTimeMillis() - audio.timestamp) < 15_000L`.
     - When `isRecentlySaved && !isSelected`, renders an animated pulsing border (`CosmicGlowBlue` / `CobaltBlue` with oscillating alpha) and a subtle `"NEW"` pill badge next to the note title.
  4. **Auto-Selection of Newly Saved Notes**:
     - In `TabletWorkspaceLayout`, added `LaunchedEffect(audioFiles)` to auto-select the new top note (`audioFiles.first().filePath`) whenever `audioFiles.size` increases, immediately loading the new note in the detail panel.
  5. **Verification**: Compiled with `./gradlew assembleDebug`, passed unit tests (`./gradlew testDebugUnitTest`), installed on `emulator-5554` (2560x1600 portrait), recorded two notes via the floating dock, verified immediate list refresh, appearance animation, "NEW" badge, and auto-selection in the right detail panel.

## ADR-033: Hold-to-Record Tablet Parity, Elevated Toast Positioning & Dynamic Waveform Scaling
- **Status**: Accepted
- **Context**: 
  1. Tablet layout previously lacked the hold-to-record gesture option present on mobile. The shutter button only supported a simple single click, without hands-free tap-to-lock, hold-to-record, slide-up-to-lock, or slide-left-to-cancel gestures.
  2. System toasts and delete notifications were anchored to the bottom center, directly overlapping and obscuring the 56dp record button and bottom dock.
  3. Audio player waveform visualizers across the app were hardcoded to fixed lists of 28 to 36 bars, leaving vast empty horizontal stretches or compressing awkwardly on wide screens like tablets and foldables.
- **Decision**:
  1. **Tablet Hold-to-Record Parity (`TabletWorkspaceScreen.kt`)**: Replaced the static `Surface(onClick = ...)` in `TabletFloatingRecordShutter` with full `awaitEachGesture` pointer input handling matching `StudioBottomBar`. Supports quick tap (<350ms) to lock hands-free, press and hold (>=350ms) to record with animated floating lock pill indicator and radiant sonic halo, slide up to lock, slide left to cancel, and releasing to stop and open the save dialog immediately.
## ADR-034: Batch AI Processing, Auto-AI Settings, Tablet Top Bar AI Action & Waveform Polish
- **Status**: Accepted
- **Context**: 
  1. Offline voice notes or recordings without transcription/summarization required users to manually open and trigger AI processing one by one. There was no one-click way to process all pending recordings from the feed header.
  2. Users lacked a persistent toggle in settings to enable/disable automated background AI processing upon recording completion.
  3. The tablet feed top bar lacked a batch process button, while the mobile feed header had an unprocessed count indicator but no prominent batch trigger.
  4. In `TabletWorkspaceScreen.kt`, the `TabletExecutiveSummaryView` displayed 3 redundant preview cards below the main executive summary, which cluttered the UI and duplicated the purpose of `TabletFullInsightsView`.
  5. `TabletFullInsightsView` was not fully responsive across tablet orientations and lacked equal-height column distribution.
  6. Waveform canvases (`MiniWaveformCanvas`, `DetailWaveformCanvas`) were static and did not convey dynamic playback feedback.
- **Decision**:
  1. **Batch AI Processing Pipeline (`AudioFilesViewModel.kt`)**: Enhanced `processPendingOfflineRecordings()` to query both offline pending voice notes and recordings where `transcript.isNullOrBlank() || summary.isNullOrBlank()`. Enqueues `AiProcessingWorker` tasks sequentially or in parallel with live status tracking.
  2. **Top Bar "Process All" Action (`FeedScreen.kt`, `TabletWorkspaceScreen.kt`)**: Added a prominent "Process All" action pill to both the mobile and tablet feed top bars when pending unprocessed voice notes exist, complete with animated spin progress during processing.
  3. **Auto-AI Settings Preference (`SettingsScreen.kt`, `AppPreferences.kt`)**: Added `isAutoAiEnabled` boolean preference in `AppPreferences.kt` exposed as an interactive toggle in `SettingsScreen.kt`.
  4. **Executive Summary Deduplication (`TabletWorkspaceScreen.kt`)**: Removed the 3 redundant duplicate cards below the executive summary in `TabletExecutiveSummaryView`, directing full insight inspection to the dedicated `TabletFullInsightsView`.
  5. **Responsive 3-Column Insights Grid (`TabletWorkspaceScreen.kt`)**: Upgraded `TabletFullInsightsView` with an adaptive 3-column layout (Action Items, Key Ideas, Core Decisions) that gracefully adapts to screen width with equal column weighting.
  6. **Dynamic Waveform Canvases (`TabletWorkspaceScreen.kt`)**: Added animated phase wave motion to `MiniWaveformCanvas` during active playback, and enhanced `DetailWaveformCanvas` with vertical gradient coloring and a distinct playhead indicator dot.
  7. **Verification**: Kotlin compilation succeeded (`compileDebugKotlin`), and unit tests passed cleanly (`testDebugUnitTest`).

## ADR-035: Tablet Landscape Insights Layout Unification & AI Multi-Line Input Polish
- **Status**: Accepted
- **Context**: 
  1. In landscape orientation on wide tablet displays (>=600dp / 2560x1600), the Insights screen previously rendered 3 parallel columns side by side. This caused cramped horizontal layouts, awkward scrolling, and visual fragmentation. The user requested a single-column, tabbed switcher identical to the approved reference design, constrained to `widthIn(max = 760.dp)` and start-aligned with breathing room on the right.
  2. In the AI Screen (`TabletCopilot3PanelWorkspace` and `GlobalCopilotScreen`), the header title was named "Srutam AI Copilot" rather than the clean brand name "Srutam AI". The query input bar was locked to a single line, causing longer questions to scroll horizontally rather than expanding vertically, and the send button shifted or misaligned when multi-line content was entered.
  3. The floating record shutter button shadow on tablets previously produced rectangular clipping artifacts due to surface elevation rendering.
- **Decision**:
  1. **Unified Single-Column Tabbed Switcher (`TabletWorkspaceScreen.kt`)**: Unified `TabletInsights3ColumnWorkspace` across both portrait and landscape modes to render a start-aligned container with `Modifier.widthIn(max = 760.dp)`. Displays the executive header ("Insights", subtitle, and "✦ AI-Extracted" badge), 3-option capsule switcher (`Next Steps`, `Ideas`, `Decisions`), `ActionProgressCard`, and tab content (`NextStepsTab`, `IdeasStreamTab`, `DecisionsTimelineTab`) with flush 0dp horizontal padding.
  2. **Brand Title Polish ("Srutam AI")**: Renamed the copilot header across both `TabletCopilot3PanelWorkspace` and `GlobalCopilotScreen` to "Srutam AI".
  3. **Multi-Line Query Input Expansion**: Updated `TabletCopilot3PanelWorkspace` and `GlobalCopilotScreen` query input rows to anchor `Alignment.Bottom`. Configured `BasicTextField` with `singleLine = false`, `minLines = 1`, `maxLines = 5`, `lineHeight = 18.sp`, and bounded container height (`heightIn(min = 20.dp, max = 110.dp)`), allowing text to expand upwards smoothly as users type while keeping the circular send button anchored at the bottom baseline.
  4. **Circular Ambient Glow Shadow on Record Shutter**: Replaced clipped `Surface(shadowElevation = ...)` on `TabletFloatingRecordShutter` with `.shadow(elevation = shadowElevation, shape = CircleShape, clip = false, spotColor = Color(0xFFEF4444).copy(alpha = 0.45f), ambientColor = Color(0xFFEF4444).copy(alpha = 0.25f))` with spring-animated elevation across idle, pressed, and holding states.
  5. **Verification**: Full test suite passing (`testDebugUnitTest`), Kotlin compilation clean (`compileDebugKotlin`), and verified live on tablet emulator (`emulator-5554`, 2560x1600) with interactive screenshots confirming pixel-perfect design parity.

## ADR-036: Universal User Activity Analytics Engine & Tablet Landscape Activity Sidebar
- **Status**: Accepted
- **Context**: 
  1. On large landscape displays (tablets with `screenWidthDp >= 1000.dp`), the space to the right of the single-column tabbed insights container was unused. The user requested an activity sidebar matching their reference design.
  2. The user required activity metrics to be tracked universally across all form factors (phones, foldables, tablets) in state, while rendering the visual sidebar on large screens for now.
  3. The right column needed:
     - "Recent activity" card with "View all ->" and 4 metric tiles: Notes (last 7 days), AI extracted (last 7 days), Action items (completed), Ideas (captured), with interactive tab switching.
     - "Weekly activity" bar chart representing voice notes recorded over the rolling last 7 days, with dynamic Y-axis markers (0, mid, max), today highlighted, and interactive tap tooltip showing note count per day.
     - "How Srutam helps" educational guidance card with 3 feature badges: Capture, Extract, and Stay on track.
- **Decision**:
  1. **Universal Analytics Engine (`UserActivityAnalytics.kt`)**: Built pure analytical engine without database migrations. Derived `DailyNoteCount` and `UserActivityMetrics` directly from existing Room `Recording` and `InsightEntity` tables. Added unit tests in `UserActivityAnalyticsTest.kt`.
  2. **Reactive State Pipeline (`AudioFilesViewModel.kt`)**: Combined `repository.allRecordings` and `insightDao.getAllInsightsFlow()` into a unified `activityMetrics: StateFlow<UserActivityMetrics>` available to all screens and form factors.
  3. **Responsive Right Column (`TabletWorkspaceScreen.kt`)**: In `TabletInsights3ColumnWorkspace`, conditionally attached `TabletInsightsActivitySidebar(modifier = Modifier.width(360.dp))` separated by a `VerticalDivider` alongside `Modifier.weight(1f).widthIn(max = 760.dp)` when `!isPortrait && screenWidthDp >= 1000`.
  4. **Interactive Navigation & Tooltips**: Tapping "View all" or Notes tile routes to Notes workspace; tapping Action items routes to Next Steps; tapping Ideas routes to Ideas tab; tapping bars displays an animated dismissible tooltip with note counts.
  5. **Typography & Theme Polish**: Standardized tile titles to 2-line centered layout with `minLines = 2` to prevent awkward truncation and guarantee uniform subtitle baselines. Provided full theme adaptivity across Light and Cosmic Void Dark modes.
  6. **Verification**: Clean unit test execution (`testDebugUnitTest`), Kotlin compilation clean (`compileDebugKotlin`), debug APK installed and verified live on tablet emulator (`emulator-5554`, 2560x1600 landscape and portrait) with screenshot evidence.

## ADR-039: Single-Instance Recording Session Coordinator & Ghost Notification Dismissal Invariant
- **Status**: Accepted
- **Context**:
  1. Users reported a critical bug where stopping and saving a recording from the on-screen floating dock (`FloatingButtonService`) left a zombie ongoing notification in the notification shade.
  2. The zombie notification continued ticking seconds forward due to `setUsesChronometer(true)` executed locally by Android SystemUI, creating the false appearance that a second active recording was in flight.
  3. Action buttons on the notification ("Pause", "Save") failed silently because `isRecording` was already `false` and Android background execution limits restricted service launch from the background.
  4. Multiple components across the app (`FloatingButtonService`, `FeedScreen`, `TabletWorkspaceScreen`, `QuickRecordingTileService`, `VolumeButtonTriggerService`, `PersistentRecordingNotificationService`) could independently launch recording intents without concurrency protection or an atomic mutex.
- **Decision**:
  1. **Centralized Singleton Coordinator (`RecordingCoordinator.kt`)**: Built an authoritative session state machine (`Idle`, `Starting`, `Recording`, `Paused`, `Stopping`) backed by a synchronized mutex. Strictly only one session can ever run at once; any competing start request while non-idle is immediately rejected and logged.
  2. **Notification Dismissal Invariant (`RecordingForegroundService.kt`)**: Added explicit `notificationManager.cancel(1001)` in `stopRecording()` (`finally` block), `onDestroy()`, and all error/exception handlers, overcoming Android's ongoing notification retention quirks.
  3. **Zombie Notification Self-Healing**: In `onStartCommand()`, if `ACTION_PAUSE_RECORDING`, `ACTION_RESUME_RECORDING`, `ACTION_STOP_RECORDING`, or `ACTION_DELETE_RECORDING` arrives while `!isRecording`, the service immediately cancels the notification, calls `stopForeground(STOP_FOREGROUND_REMOVE)`, and terminates via `stopSelf()`.
  4. **Reactive StateFlow Migration**: Replaced ad-hoc polling loops (`while(isActive) delay(...)`) in `FloatingButtonService`, `Navigation`, and `FeedScreen` with direct reactive collection of `RecordingCoordinator.state`.
  5. **Automated Unit Testing (`RecordingCoordinatorTest.kt`)**: Implemented Robolectric unit test suite verifying state lifecycle, mutual exclusivity, concurrent thread competition, and rejection of invalid state transitions.
  6. **Version Bump**: Bumped to version `2.2.1` (`versionCode = 8`) in `app/build.gradle.kts`.

## ADR-040: Foreground Notification ID Collision Fix & Audio Playback Completion Icon Reset
- **Status**: Accepted
- **Context**:
  1. **Stuck Recording Notification After Stop & Save**: Despite the single-instance coordinator, testing on physical devices (Realme UI / ColorOS) revealed that after stopping and saving recording via the floating dock, the recording notification with its ticking chronometer remained stuck in the notification shade.
     - Root Cause 1: `FloatingButtonService` and `RecordingForegroundService` both used identical `NOTIFICATION_ID = 1001`. When `RecordingForegroundService` started, it posted onto ID 1001. When recording stopped and `RecordingForegroundService` terminated, Android refused to dismiss notification 1001 because `FloatingButtonService` was still alive as a foreground service registered with notification ID 1001.
     - Root Cause 2: `RecordingForegroundService` had `.setOngoing(true)` which set `FLAG_ONGOING_EVENT (0x02)`. On ColorOS / Realme UI, `stopForeground(STOP_FOREGROUND_REMOVE)` only stripped `FLAG_FOREGROUND_SERVICE (0x40)`, leaving `FLAG_ONGOING_EVENT` and keeping the notification pinned.
     - Root Cause 3: `notificationManager.cancel(1001)` was called before `stopForeground()`. AOSP `NotificationManagerService` rejects cancellations while `FLAG_FOREGROUND_SERVICE` is active.
  2. **Play/Pause Icon Not Resetting on Notes Screen**: When playing an audio note card on the Notes screen (`FeedScreen.kt`), when playback reached the end of the track, the play/pause icon remained stuck in the "Pause" state rather than reverting to "Play".
     - Root Cause: In `AudioPlayer.kt`, `setOnCompletionListener` called `pause()` and `seekTo(0)`, but on certain Android AAC decoders (M4A files recorded via `MediaRecorder`), native `OnCompletionListener` events can be delayed or dropped by the media pipeline before the audio sink reaches the exact stream duration. When the `while (mediaPlayer.isPlaying)` coroutine loop exited, `isPlaying` was never updated to `false` in `_playbackState`.
- **Decision**:
  1. **Notification ID Isolation**: Changed `FloatingButtonService.NOTIFICATION_ID` to `1002`, completely isolating it from `RecordingForegroundService.NOTIFICATION_ID` (`1001`).
  2. **Removed `.setOngoing(true)` & Teardown Order**: Removed `.setOngoing(true)` from `createNotification()`. Re-ordered teardown in `stopRecording()`, `cleanUpStaleNotification()`, and `onDestroy()` to execute `stopForeground(STOP_FOREGROUND_REMOVE)` (and `stopForeground(true)` for compat) *before* `notificationManager.cancel(1001)`.
  3. **Self-Healing Cancellation**: Added proactive sweeps in `MainActivity.onCreate()` and `onResume()`, and in `FloatingButtonService.stopRecording()` and `cancelRecording()`, clearing notification 1001 whenever `RecordingCoordinator.isIdle`.
  4. **Robust Audio Playback Completion (`AudioPlayer.kt`)**:
     - Built `handlePlaybackComplete()` that cancels progress updates, sets `isPlaying = false`, resets `currentPosition = 0`, and seeks `mediaPlayer` to 0.
     - Wired `setOnCompletionListener` to `handlePlaybackComplete()`.
     - In `startProgressUpdates()`, added automated end-of-track fallback detection: when position is within 100ms of duration or when `mediaPlayer.isPlaying` becomes false while `_playbackState.value.isPlaying` is still true, `handlePlaybackComplete()` is invoked immediately, ensuring the play/pause icon on `FeedScreen.kt` always resets cleanly to `Icons.Default.PlayArrow`.
     - In `play()`, if current position is at or near duration, auto-seeks to 0 before starting.
  5. **Verification**: Full unit test suite passed (`testDebugUnitTest`), debug APK compiled (`assembleDebug`), installed via adb to physical device `RMX2151` (`192.168.31.163:39509`), and confirmed zero lingering notifications in `dumpsys notification`.
