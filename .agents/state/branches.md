# Srutam Git Branching Architecture & Work Tracking

> **Status:** Authoritative Repository Map  
> **Updated:** September 18, 2026  
> **Current Release in Production:** v2.2.0 (`main`)  
> **Upcoming Hotfix:** v2.2.1 (`fix/single-instance-recording` -> `main`)  
> **Next Major Releases:** v2.3.0 (`feature/cloud-sync-mcp`), v2.4.0 (`feature/earbud-srutam-capture`)

---

## 1. Branch Overview & Active Status

| Branch | Base | Target | Status | Purpose / Key Features |
| :--- | :--- | :--- | :--- | :--- |
| **`main`** | - | Production | **ACTIVE / STABLE** | Production release branch (currently v2.2.0, versionCode 7). Receives only verified, tested releases and hotfixes. |
| **`fix/single-instance-recording`** | `main` | `main` | **IN FLIGHT** | Critical hotfix (v2.2.1, versionCode 8): Centralized `RecordingCoordinator` singleton mutex, ghost notification cleanup, dead button fix. |
| **`feature/cloud-sync-mcp`** | `main` | `main` (v2.3.0) | **UNDER WORK / TESTING** | Cloud Sync via Supabase, Developer Brain, `srutam-mcp` v1.1.0 server, 3-key limit, rate limiter, terminal dashboard. |
| **`feature/earbud-srutam-capture`** | `feature/cloud-sync-mcp` | `feature/cloud-sync-mcp` (v2.4.0) | **PAUSED / IDEATION** | Hands-free Bluetooth earbud capture (Realme Buds T310), Media3 MediaSession, audio cues, settings diagnostics. Paused for media focus exploration. |
| **`feature/screen-matrix-testing`** | `main` | Tooling | **ACTIVE** | Headless Robolectric Native Graphics screenshot matrix across phones, foldables, and tablets. |
| **`feature/core-release`** | Historical | Merged to `main` | **MERGED** | Core v2.2.0 release: floating dock, adaptive tablet workspace, refined settings, dark mode. |
| **`archive/v2.0.0-base`** | Historical | Reference | **ARCHIVED** | Archived v2.0.0 baseline before tablet workspace and floating dock. |
| **`once-reached-100`** | Historical | Milestone | **FROZEN** | Preserved 2D physics Concept Mesh to celebrate 100 Play Store downloads. |

---

## 2. Deep Dive: Branch Details & Current Work

### `main` (Production Release Train)
- **Current Commit:** `07b24a1` ("tab optimization and some bugs fixed")
- **App Version:** `2.2.0` (versionCode `7`)
- **Dependencies:** Android 15/16 SDK 36, Jetpack Compose, Room, Google Generative AI (Gemini), Roborazzi. Clean of unreleased backend libraries.
- **Workflow Invariant:** Never commit experimental or incomplete feature code directly to `main`. All releases must pass `testDebugUnitTest` and compile cleanly under `assembleRelease`.

---

### `fix/single-instance-recording` (Release v2.2.1 Hotfix)
- **Base:** Branched directly from `main` (`07b24a1`).
- **Target:** Merge back into `main` for Play Store release as **v2.2.1** (versionCode `8`).
- **What it solves:**
  1. **Ghost Notification Leak:** In `RecordingForegroundService`, `notificationManager.cancel(1001)` was missing, leaving ongoing notifications stuck in the drawer after saving via the floating dock.
  2. **Zombie Chronometer & Buttons:** SystemUI chronometer kept ticking forward even when the service was dead, and notification buttons ("Pause", "Save") failed silently because `isRecording == false`.
  3. **Race Conditions / Duplicate Sessions:** Multiple recording triggers (floating dock, feed FAB, tablet shutter, quick tile, volume buttons) had no concurrency guard.
- **Components Built:**
  - `RecordingCoordinator.kt`: Centralized singleton state machine with atomic mutex ensuring strictly 1 recording instance can ever run across the entire app.
  - Hardened teardown and self-healing in `RecordingForegroundService.kt`.
  - Reactive `StateFlow` collection in `FloatingButtonService.kt`, `Navigation.kt`, and `FeedScreen.kt`.

---

### `feature/cloud-sync-mcp` (Upcoming v2.3.0 Release)
- **Base:** Branched off `main`.
- **Target:** Next feature release after v2.2.1.
- **Key Deliverables:**
  1. **Supabase Cloud Sync:** Syncs voice notes, transcripts, and AI summaries to Postgres with Row Level Security.
  2. **Srutam MCP Server (`srutam-mcp` v1.1.0):** Model Context Protocol server enabling Cursor, Antigravity, and Claude Desktop to search notes, read transcripts, and track action items.
  3. **Multi-Developer Hardening:** 60s in-memory auth caching, 30s query caching, 60 req/min token-bucket rate limiter per API key, max 3 keys per user.
  4. **Home Screen Sync Pill:** Real-time visual sync status pill in the top app bar.
- **Current Status:** App-side code is functioning and passing unit tests. Awaiting database quota enforcement and multi-user stress testing before merging into `main`.

---

### `feature/earbud-srutam-capture` (Hardware Innovation Track / v2.4.0)
- **Base:** Branched off `feature/cloud-sync-mcp`.
- **Key Deliverables:**
  1. `EarbudCaptureService.kt`: Background service holding `MediaSession` and low-importance foreground notification.
  2. Intercepts Bluetooth earbud media clicks (e.g. right earbud triple-tap on Realme Buds T310 mapped to Play/Pause).
  3. Seamless auto-save and Auto-AI triggering without opening the phone.
  4. Live diagnostics inspector in Settings.
- **Current Blocker / Exploration:**
  - When music (Spotify / YouTube Music) is streaming, media buttons are consumed by the active music player.
  - Explored in `ideas/next_steps_roadmap.md` and `ideas/hardware_earbud_ideation.md` (e.g. BLE companion hardware, NotificationListenerService, or community exploration).
- **Status:** Paused cleanly in Git while isolating the v2.2.1 recording fix.

---

## 3. Merge & Synchronization Order

To prevent conflicts and ensure clean deployment:

```
                  [main] (v2.2.0)
                     │
                     ├─────────────────────────┐
                     │                         │
     [fix/single-instance-recording]           │
            (v2.2.1 Hotfix)                    │
                     │                         │
                     ▼                         │
            Merge to [main] ────────┐          │
            (Ship to Play Store)    │          │
                                    ▼          ▼
                       Rebase/Merge into [feature/cloud-sync-mcp]
                                    │      (v2.3.0 Release Track)
                                    │
                                    ▼
                       Rebase/Merge into [feature/earbud-srutam-capture]
                                           (v2.4.0 Hardware Track)
```

1. **Step 1:** Implement and verify `fix/single-instance-recording` on `main`.
2. **Step 2:** Tag and publish v2.2.1 from `main`.
3. **Step 3:** Rebase or merge `main` into `feature/cloud-sync-mcp` so the MCP track inherits the single-instance coordinator without merge debt.
4. **Step 4:** Rebase or merge `feature/cloud-sync-mcp` into `feature/earbud-srutam-capture`.
