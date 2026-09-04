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
