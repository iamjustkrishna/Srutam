# Firebase Analytics setup

Analytics is wired into the app but stays off until you add the Firebase config file. The build and the app work fine without it (every analytics call is a safe no-op), so CI and a fresh clone are unaffected.

## Turn it on (one time)
1. In the Firebase console, create a project (or use an existing one).
2. Add an Android app with the package name `space.iamjustkrishna.srutam`.
3. For the release build, also add the app's signing SHA-256 (optional for Analytics, needed for some other Firebase features).
4. Download `google-services.json` and put it in `app/google-services.json`.
5. Rebuild. The app detects the file, applies the Google Services plugin, and Analytics starts.

`app/google-services.json` is gitignored (it is project config, not a secret, but it stays out of the repo like the other local config). Keep a copy somewhere safe and add it to your release machine and CI.

## How it is wired
- Version catalog: `firebaseBom`, `firebase-analytics`, and the `google-services` plugin in `gradle/libs.versions.toml`.
- Root `build.gradle.kts` puts the plugin on the classpath (`apply false`).
- `app/build.gradle.kts` applies the plugin only if `app/google-services.json` exists, and adds the Firebase BoM plus `firebase-analytics`.
- `utils/Analytics.kt` is the only place the app talks to Firebase. It no-ops when Firebase is not initialized, and it honors the user's opt-out.
- `SrutamApplication.onCreate` calls `Analytics.init`.
- Settings > PRIVACY has a "Share anonymous usage data" switch (on by default, opt-out), backed by `AppPreferences.isAnalyticsEnabled`.

## Events currently logged
Only product-usage signals, never audio, transcript text, note content or file paths.

| Event | When | Parameters |
| :--- | :--- | :--- |
| `recording_started` | a recording begins | `source` (optional; not yet populated) |
| `recording_saved` | a recording is stopped and kept | `duration_bucket` (0_15s, 15_60s, 1_5m, 5_15m, 15_60m, 60m_plus) |
| `recording_discarded` | a recording is cancelled/deleted on stop | |
| `transcript_stored` | the live transcript is saved on the note | |
| `ai_insights_ready` | AI insights finished for a note | |
| `ai_processing_failed` | transcription or insights failed | `reason` (audio_missing, transcription_failed) |
| `silence_auto_saved` | the "still recording?" prompt went unanswered and the note was saved | |

Firebase also logs its own automatic events (app opens, screen time, etc.).

## Before you ship with analytics on (important)
- **Play Data safety form:** declare that the app collects analytics data. At minimum this is usually "App activity" and "Device or other IDs", collected and linked for Analytics, and the Firebase/Google privacy terms apply. Review the exact fields in Play Console.
- **Privacy policy:** mention analytics and the opt-out in the Settings screen.
- **Consent:** for users in regions that require it (for example the EU under GDPR), you may need a consent prompt before collecting. The opt-out switch is a start, not necessarily full consent compliance. Check your obligations.
- **DebugView:** to verify events during testing, run `adb shell setprop debug.firebase.analytics.app space.iamjustkrishna.srutam`, then watch Firebase console > Analytics > DebugView. Turn it off with `adb shell setprop debug.firebase.analytics.app .none.`.

## To add a new event
Add a helper in `utils/Analytics.kt` (keep it to counts and short enums, never content), call it from the right place, and add a row to the table above.
