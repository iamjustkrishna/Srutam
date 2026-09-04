# Srutam - Project Identity & Architecture

## Overview
Fast hardware-shortcut audio note app with instant Volume Down & Quick Settings recording, on-device Sherpa-ONNX transcription, AI-assisted voice summaries, action items, and conversational audio note chat in modern Jetpack Compose.

## Target Package & Identity
- **Application Name**: Srutam
- **Package / Namespace**: space.iamjustkrishna.srutam
- **Type**: Native Android (Kotlin + Jetpack Compose)
- **Compile SDK**: 36
- **Target SDK**: 36
- **Min SDK**: 29 (Android 10+)

## Subsystems & Architecture
1. **Quick Capture & Background Recording Services**:
   - VolumeButtonTriggerService: Double-press Volume Down hardware button trigger.
   - QuickRecordingTileService: Quick Settings tile one-tap toggle.
   - RecordingForegroundService & FloatingButtonService: Continuous microphone recording with system notification.
2. **On-Device Speech-to-Text**:
   - LocalTranscriber: Sherpa-ONNX (com.k2fsa.sherpa.onnx) offline ASR engine running local models without cloud latency.
3. **AI Voice Intelligence**:
   - AIProcessor: Structured extraction of summaries, key points, action items, and follow-up audio chat.
4. **Persistence & Data Layer**:
   - AppDatabase & RecordingDao: Room database tracking Recording entities, file paths, durations, and RecordingAiStatus.
5. **Playback & Audio Engine**:
   - AudioPlayer, AudioDecoder, and AudioFileScanner: Scans and plays recorded audio with waveform scrubbing.
6. **UI & Navigation**:
   - Modern Jetpack Compose Material 3 screens: FeedScreen, DetailScreen, ChatScreen.

## Build & Test Commands
- **Debug Build**: ./gradlew assembleDebug
- **Unit Tests**: ./gradlew test
- **Release Bundle**: ./gradlew bundleRelease

## Key Permissions
- ndroid.permission.RECORD_AUDIO (Microphone capture)
- ndroid.permission.FOREGROUND_SERVICE & FOREGROUND_SERVICE_MICROPHONE
- ndroid.permission.BIND_QUICK_SETTINGS_TILE
- ndroid.permission.READ_MEDIA_AUDIO / Storage permissions
- ndroid.permission.POST_NOTIFICATIONS
