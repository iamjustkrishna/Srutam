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
  6. **Feature Branch Isolation**: Implemented and verified entirely on dedicated branch `feature/background-ai-processing`.


