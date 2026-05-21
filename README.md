# Srutam

Srutam is an Android audio note app that lets you start recording quickly with a hardware button shortcut, save the audio locally, and browse recordings in a modern Compose UI. It also includes AI-assisted summaries, key points, action items, and chat-style questions about each recording.

## What it does

- Double-press Volume Down to start or stop recording
- Record audio in the background with a foreground service
- Store recordings in app-private external storage
- Show recordings in a feed with filters and rename support
- Delete one or more recordings with selection mode
- Generate AI summaries and structured insights
- Ask questions about a specific recording

## Tech stack

- Kotlin
- Jetpack Compose
- Material 3
- MVVM
- Room
- Coroutines
- Google Generative AI

## Project structure

- `app/src/main/java/space/iamjustkrishna/srutam/ai` - AI processing
- `app/src/main/java/space/iamjustkrishna/srutam/data` - Room entities and DAO
- `app/src/main/java/space/iamjustkrishna/srutam/repository` - Data access layer
- `app/src/main/java/space/iamjustkrishna/srutam/service` - Recording and trigger services
- `app/src/main/java/space/iamjustkrishna/srutam/ui` - Compose screens and theme
- `app/src/main/java/space/iamjustkrishna/srutam/viewmodel` - Screen state and actions
- `app/src/main/java/space/iamjustkrishna/srutam/utils` - File and audio helpers

## Setup

1. Add your Gemini API key to `local.properties`:

```properties
GEMINI_API_KEY=your_api_key_here
```

2. Build and install the app with Android Studio or Gradle.

3. Open the app and grant microphone permission.

4. Enable the accessibility service for Srutam in Android settings so the volume button shortcut works.

## Storage

Recordings are saved in the app-private music directory returned by `getExternalFilesDir(Environment.DIRECTORY_MUSIC)`. This keeps file deletion simple and avoids extra system approval prompts.

## Notes

- The app uses a local Room database for recording metadata
- AI features require network access
- Existing recordings may need to stay in the old location unless migrated separately
