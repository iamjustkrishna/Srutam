# Srutam: Zero-Friction AI Audio Note-Taker & Context Platform

## Project Overview

Srutam is a modern, privacy-first Android application and developer context platform that allows users to record spoken thoughts instantly using hardware button shortcuts (double-press Volume Down) or an on-screen floating dock, transcribe them on-device or via cloud AI, extract structured insights (Summaries, Action Items, Reminders, Ideas, Decisions, WIIFM), and expose this rich personal knowledge base directly to modern AI coding assistants (Cursor, OpenCode, Windsurf, Zed, Claude Desktop, Antigravity) via the Model Context Protocol (MCP).

---

## High-Level Architecture

Srutam is architected around modern Android Clean MVVM patterns with Unidirectional Data Flow (UDF), a local-first Room database, background WorkManager jobs, and an external TypeScript MCP server communicating over standard I/O (stdio).

```
Srutam/
|-- app/                               # Android Application Module
|   |-- src/main/java/space/iamjustkrishna/srutam/
|   |   |-- ai/                        # AI Processing & Inference Engines
|   |   |   |-- copilot/               # Global Copilot, BM25 Indexing & Cache
|   |   |   |-- provider/              # Gemini and LLM Provider Integrations
|   |   |   |-- AIProcessor.kt         # Core transcription & insight extraction
|   |   |-- analytics/                 # Privacy-preserving local metrics
|   |   |-- cloud/                     # Supabase Cloud Sync Engine & Auth
|   |   |-- data/                      # Room Database (v7), Entities & DAOs
|   |   |-- navigation/                # Compose Navigation & Deep Linking
|   |   |-- player/                    # Audio Playback Engine
|   |   |-- repository/                # Data Repository Abstractions
|   |   |-- service/                   # Foreground, Accessibility & Alarm Services
|   |   |-- ui/                        # Jetpack Compose UI (Material 3)
|   |   |   |-- components/            # Reusable UI widgets & app bars
|   |   |   |-- screens/               # Screen composables & bottom sheets
|   |   |   |-- theme/                 # Cosmic Void / Light / Dark Design Tokens
|   |   |-- utils/                     # Storage, Preferences & Audio Decoders
|   |   |-- viewmodel/                 # StateFlow ViewModels & Screen State
|   |   |-- MainActivity.kt            # Single Activity Entry Point
|   |   |-- SrutamApplication.kt       # Application Lifecycle & Service Inits
|-- mcp-server/                        # Model Context Protocol Server (TypeScript)
|   |-- src/                           # MCP Tools, Supabase Client & CLI Wizard
|   |-- dist/                          # Compiled Node.js CJS/ESM Bundle
|   |-- package.json                   # srutam-mcp NPM Package Configuration
|-- supabase/                          # Backend Cloud Infrastructure
|   |-- migrations/                    # SQL Schemas, RLS Policies & RPCs
|   |-- email-templates/               # Dark-mode OTP & Auth Templates
```

---

## Package Structure Breakdown

### 1. `space.iamjustkrishna.srutam.ai`
Handles audio transcription, insight extraction, and global querying.
- `AIProcessor.kt`: Coordinates audio transcription and invokes generative AI prompts to produce summaries, key points, action items, reminders, ideas, decisions, and WIIFM.
- `ai/copilot/GlobalCopilotEngine.kt`: Cross-note search engine powered by BM25 ranking and TF-IDF term scoring.
- `ai/copilot/AiQueryCache.kt`: Persistent cache to avoid redundant LLM invocations for previously answered queries.
- `ai/provider/`: Multi-provider abstraction supporting Gemini Cloud, BYOK (Bring Your Own Key), and on-device processing.

### 2. `space.iamjustkrishna.srutam.cloud`
Local-first cloud synchronization with Supabase backend.
- `SupabaseCloudClient.kt`: REST client for note synchronization, Personal Access Token (PAT) generation, key revocation, and remote task updates.
- `SupabaseAuthManager.kt`: Native Android Credential Manager integration for 1-tap Google Sign-In and email OTP authentication.
- `CloudSyncWorker.kt`: Android WorkManager coroutine worker executing background sync under network constraints.
- `CloudSyncManager.kt`: Orchestrates automatic push synchronization after AI completion and pull synchronization on app resume.

### 3. `space.iamjustkrishna.srutam.data`
Local SQLite persistence via Android Room (Database Version 7).
- `AppDatabase.kt`: Room database definition configuring entities and migration strategies.
- `Recording.kt` & `RecordingDao.kt`: Core voice note entity storing audio paths, transcripts, summaries, AI status, and cloud sync metadata (`syncStatus`, `isPrivate`, `cloudId`, `lastSyncedAt`).
- `InsightEntity.kt` & `InsightDao.kt`: Granular itemized insights (Action Items, Ideas, Decisions) with completion, archiving, and editing states.
- `ReminderEntity.kt` & `ReminderDao.kt`: Scheduled reminders with exact timestamps, review states, notification flags, and timezone inference.
- `ExtractionSuppression.kt`: Prevents re-extracting deleted or modified insights during re-processing.
- `InsightsMigration.kt`: Schema migration from version 6 to version 7 preserving user data while adding sync and insight tracking columns.

### 4. `space.iamjustkrishna.srutam.service`
Hardware button hooks, foreground recording, and background scheduling.
- `VolumeButtonTriggerService.kt`: Android Accessibility Service intercepting double-press Volume Down gestures to trigger hands-free recording even when the device is locked.
- `RecordingForegroundService.kt`: Persistent foreground service recording audio via MediaRecorder to high-quality AAC/M4A.
- `FloatingButtonService.kt`: System overlay service providing an on-screen floating dock for quick record, pause, and stop controls.
- `ReminderScheduler.kt` & `ReminderAlarmReceiver.kt`: Exact alarm scheduling via AlarmManager delivering rich notifications with direct action buttons (Complete, Snooze, Review).
- `BootRescheduleReceiver.kt`: Re-registers pending reminder alarms after device reboot.
- `AiProcessingWorker.kt` & `AiSummaryNetworkWorker.kt`: Background WorkManager execution for transcription and insight generation.

### 5. `space.iamjustkrishna.srutam.ui`
Modern, reactive UI built entirely with Jetpack Compose and Material 3 tokens.
- `ui/screens/FeedScreen.kt`: Chronological feed of recorded notes with swipe actions, cloud sync indicators, multi-select deletion, and search.
- `ui/screens/DetailScreen.kt`: Rich note detail view showing audio waveform playback, editable transcripts, itemized insight cards, and privacy lock controls.
- `ui/screens/ActionItemsScreen.kt`: Dedicated tasks and reminders hub with status filters, date groupings, and swipe completion.
- `ui/screens/GlobalCopilotScreen.kt`: Conversational cross-note search assistant querying your entire audio knowledge base.
- `ui/screens/DeveloperMcpSection.kt`: Developer dashboard displaying cloud sync status, 1-tap API key generator (max 3 keys), and copyable MCP config snippets for OpenCode, Cursor, Windsurf, Zed, and Claude Desktop.
- `ui/screens/SettingsScreen.kt`: Preferences for AI provider, audio quality, auto-sync, appearance themes, and storage cleanup.
- `ui/screens/TabletWorkspaceScreen.kt`: Adaptive multi-pane layout for large-screen tablets and foldables.
- `ui/theme/`: Cosmic Void pure OLED black, Dark, and Light themes with tailored semantic color palettes.

### 6. `space.iamjustkrishna.srutam.utils`
Utility classes and helpers.
- `AppPreferences.kt`: Encrypted and DataStore backed user preferences.
- `AudioStorage.kt`: Manages app-private audio file paths, folder structures, and storage quotas.
- `AudioFileReader.kt` & `AudioDecoder.kt`: Reads raw audio waveforms and metadata for visualization and playback.

---

## Standalone MCP Server (`mcp-server/`)

The `srutam-mcp` package is a standalone TypeScript server implementing the Model Context Protocol (MCP) specification over standard I/O (stdio). It bridges modern AI coding environments with the user's Srutam Cloud database.

### Implemented MCP Tools:
1. `search_notes`: Semantic and keyword search across transcripts, summaries, and key points.
2. `list_recent_notes`: Retrieves recently recorded notes with pagination and date filters.
3. `get_note_detail`: Fetches the full transcript, summary, action items, and metadata for a specific note.
4. `list_action_items`: Lists pending or completed tasks extracted from voice notes.
5. `update_action_item`: Marks action items as completed or archived directly from the coding agent.
6. `append_agent_work_log`: Appends audit logs and implementation notes from AI coding sessions back to the note.

### Supported Coding Assistants:
- OpenCode (`opencode.json`)
- Cursor (`~/.cursor/mcp.json`)
- Windsurf (`~/.codeium/windsurf/mcp_config.json`)
- Zed Editor (`~/.config/zed/settings.json`)
- Claude Desktop (`claude_desktop_config.json`)
- Google Antigravity IDE

---

## Supabase Backend (`supabase/`)

- `migrations/20260916_01_srutam_cloud_mcp.sql`: Database schema creating `notes`, `action_items`, `api_keys`, and `agent_logs` tables (the `embedding` column exists but is not yet populated or queried - search is full-text + substring).
- Row Level Security (RLS): Strict tenant isolation guaranteeing the phone app (JWT) can only read and write its own data.
- MCP RPC layer (`mcp_*`, called by `srutam-mcp` with the public anon key): every function takes the SHA-256 hash of the API key as `p_key_hash` and resolves the owning user **inside the database** (`private.resolve_mcp_key`), so the caller never asserts a user id. All are `SECURITY DEFINER` with a pinned `search_path`, honour `notes.is_private`, and are executable by `anon` only. Introduced by `20260930_04_mcp_key_auth_hardening.sql` (which replaced the earlier `p_user_id` signatures).
  - `mcp_search_notes`, `mcp_get_note_detail`, `mcp_list_action_items`, `mcp_update_action_item`, `mcp_append_agent_log`, `mcp_cloud_status`.
- Trigger Policies: Enforces a maximum limit of 3 active Personal Access Tokens per user (serialised with an advisory lock).
- `tests/mcp_rpc_isolation.sql`: dependency-free cross-tenant / private-note / grant assertions. Run against a scratch DB: `psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f supabase/tests/mcp_rpc_isolation.sql`.

---

## Tech Stack & Dependencies

- **Language**: Kotlin 2.0+ (Android), TypeScript / Node.js (MCP Server)
- **UI Toolkit**: Jetpack Compose, Material 3, Accompanist
- **Local Persistence**: Room Database 2.6.1 (Version 7)
- **Async Concurrency**: Kotlin Coroutines, StateFlow, SharedFlow
- **Background Jobs**: AndroidX WorkManager 2.10+
- **Authentication**: Android Credential Manager (Google One Tap), Supabase Auth
- **AI Integrations**: Google Generative AI SDK, Gemini 1.5 Flash / Pro, BM25 Engine
- **Audio Processing**: MediaRecorder (AAC/M4A), Android MediaPlayer, AudioTrack
- **Minimum SDK**: 29 (Android 10)
- **Target SDK**: 35 (Android 15 / 16)
