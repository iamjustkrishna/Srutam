# Srutam - Zero-Friction AI Audio Note-Taker

## Project Overview

Srutam is a modern Android application that allows users to record audio notes instantly using a hardware button shortcut (double-press Volume Down), transcribe them on-device, and generate AI-driven summaries and action items.

## Architecture

The app follows **MVVM (Model-View-ViewModel)** architecture with clean separation of concerns:

```
space.iamjustkrishna.srutam/
â”œâ”€â”€ ai/                          # AI Processing Layer
â”‚   â””â”€â”€ AIProcessor.kt          # Handles transcription and AI analysis
â”œâ”€â”€ data/                        # Data Layer
â”‚   â”œâ”€â”€ Recording.kt            # Room Entity
â”‚   â”œâ”€â”€ RecordingDao.kt         # Database Access Object
â”‚   â””â”€â”€ AppDatabase.kt          # Room Database
â”œâ”€â”€ navigation/                  # Navigation
â”‚   â””â”€â”€ Navigation.kt           # Compose Navigation setup
â”œâ”€â”€ repository/                  # Repository Layer
â”‚   â””â”€â”€ RecordingRepository.kt  # Data access abstraction
â”œâ”€â”€ service/                     # Background Services
â”‚   â”œâ”€â”€ VolumeButtonTriggerService.kt    # Accessibility Service
â”‚   â””â”€â”€ RecordingForegroundService.kt    # Recording Service
â”œâ”€â”€ ui/
â”‚   â”œâ”€â”€ screens/                # Compose UI Screens
â”‚   â”‚   â”œâ”€â”€ FeedScreen.kt       # List of recordings
â”‚   â”‚   â”œâ”€â”€ DetailScreen.kt     # Recording details
â”‚   â”‚   â””â”€â”€ ChatScreen.kt       # AI Query interface
â”‚   â””â”€â”€ theme/                  # Material 3 theme
â”œâ”€â”€ viewmodel/                   # ViewModels
â”‚   â”œâ”€â”€ RecordingsViewModel.kt  # Feed screen VM
â”‚   â””â”€â”€ DetailViewModel.kt      # Detail & Chat VM
â”œâ”€â”€ MainActivity.kt             # Main entry point
â””â”€â”€ SrutamApplication.kt        # Application class
```

## Tech Stack

### Core Technologies
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM with Repository Pattern
- **Dependency Injection**: Manual DI (can be upgraded to Hilt)

### Key Libraries
- **Room Database** (2.6.1) - Local data persistence
- **Google Generative AI** (0.9.0) - Gemini AI for analysis
- **Navigation Compose** (2.8.5) - Screen navigation
- **Accompanist Permissions** (0.36.0) - Runtime permissions
- **Coroutines** (1.9.0) - Asynchronous operations
- **Gson** (2.11.0) - JSON serialization

### Android Components
- **AccessibilityService** - Intercepts volume button presses
- **ForegroundService** (Microphone type) - Background audio recording
- **MediaRecorder** - Audio recording to M4A format

## Features Implemented

### âœ… Core Features
1. **Hardware Button Trigger**
   - Double-press Volume Down to start/stop recording
   - Works even when screen is off
   - Implemented via AccessibilityService

2. **Background Recording**
   - Foreground Service with persistent notification
   - Records to M4A format in internal storage
   - Compliant with Android 16+ requirements

3. **AI Processing Pipeline**
   - On-device transcription (placeholder - needs ML Kit Speech integration)
   - AI-powered analysis using Gemini:
     - Summary generation
     - Key points extraction
     - Action items identification
     - WIIFM (What's In It For Me) section

4. **Modern UI**
   - Feed screen with swipe-to-delete
   - Detailed view with all AI-generated sections
   - Chat interface for querying recordings
   - Material 3 design system

5. **Data Persistence**
   - Room database for metadata and AI results
   - Internal storage for audio files
   - Real-time updates with Flow

## Setup Instructions

### 1. Configure API Key

**IMPORTANT**: You need to add your Google Generative AI API key to use the AI features.

**Option A: Using BuildConfig (Recommended)**

1. Add to `local.properties`:
   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```

2. Update `app/build.gradle.kts`:
   ```kotlin
   android {
       defaultConfig {
           // ... existing config
           buildConfigField("String", "GEMINI_API_KEY", "\"${project.findProperty("GEMINI_API_KEY")}\"")
       }
   }
   ```

3. Update `AIProcessor.kt` line 72:
   ```kotlin
   apiKey = BuildConfig.GEMINI_API_KEY
   ```

**Option B: Direct Replacement (Not recommended for production)**

Replace `"YOUR_API_KEY_HERE"` in `AIProcessor.kt` (lines 72 and 139) with your actual API key.

**Get an API Key**: https://ai.google.dev/

### 2. Enable Accessibility Service

After installing the app:
1. Go to **Settings** â†’ **Accessibility**
2. Find **Srutam** or **Volume Button Trigger Service**
3. Enable it
4. Grant the permission

### 3. Grant Permissions

The app will request:
- **RECORD_AUDIO** - Required for recording
- **POST_NOTIFICATIONS** - For foreground service notification

## How It Works

### Recording Flow
1. User double-presses Volume Down
2. `VolumeButtonTriggerService` (Accessibility) detects the gesture
3. Starts `RecordingForegroundService`
4. Service displays notification and begins recording
5. Second double-press stops recording
6. Audio saved to internal storage
7. Recording metadata inserted into Room database
8. AI processing begins in background

### AI Processing Flow
1. `AIProcessor.transcribeAudio()` - Converts audio to text
2. `AIProcessor.generateInsights()` - Calls Gemini API with structured prompt
3. Parses JSON response into:
   - Summary
   - Key Points (array)
   - Action Items (array)
   - WIIFM
4. Updates Room database with results
5. UI automatically refreshes via Flow

### Query Feature
1. User opens Detail screen â†’ Chat
2. Types question about recording
3. `AIProcessor.queryRecording()` sends transcript + question to Gemini
4. Response displayed in chat interface

## Important Notes

### Transcription Implementation
The current implementation uses a **placeholder** for transcription. To implement real transcription:

**Option 1: Android SpeechRecognizer**
```kotlin
val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
// Configure and start recognition
```

**Option 2: Google Cloud Speech-to-Text**
- Use Cloud Speech-to-Text API
- Requires network connection
- More accurate but costs money

**Option 3: ML Kit (Future)**
- Google is working on on-device speech recognition
- Not yet fully available for all use cases

### Gemini Nano vs Cloud
Current implementation uses **Gemini Cloud API** (requires internet).

For true on-device processing with **Gemini Nano**:
- Use Android AICore API (currently in preview)
- Requires Android 14+ with specific device support
- Limited model capabilities

### Permissions
- **BIND_ACCESSIBILITY_SERVICE** - System-level, granted by user in Settings
- **FOREGROUND_SERVICE_MICROPHONE** - Required for Android 16+
- **RECORD_AUDIO** - Runtime permission
- **POST_NOTIFICATIONS** - Runtime permission (Android 13+)

## File Structure Details

### Data Models
- **Recording** - Entity with all fields including AI results
- Room auto-generates DB at `srutam_database`
- Audio files stored in `app_files/recordings/`

### Services
- **VolumeButtonTriggerService** - Runs continuously when enabled
- **RecordingForegroundService** - Runs only during recording
- Both use minimal battery when idle

### ViewModels
- **RecordingsViewModel** - Manages list of recordings
- **DetailViewModel** - Handles single recording + chat messages
- Both use StateFlow for reactive UI updates

## Testing the App

1. **First Launch**:
   - Grant microphone permission
   - Enable accessibility service in Settings

2. **Record Audio**:
   - Double-press Volume Down
   - Speak into microphone
   - Double-press again to stop

3. **View Results**:
   - Open app
   - See recording in feed
   - Wait for "Processing..." to complete
   - Tap to view details

4. **Query Recording**:
   - In detail screen, tap Chat icon
   - Ask questions about the recording
   - Get AI-powered answers

## Future Enhancements

### Recommended Improvements
1. **On-device Transcription** - Integrate ML Kit when available
2. **Gemini Nano** - Switch to on-device AI when AICore stabilizes
3. **Audio Playback** - Add MediaPlayer to play recordings
4. **Export Options** - Share as text/PDF
5. **Categories/Tags** - Organize recordings
6. **Search** - Full-text search across transcripts
7. **Cloud Backup** - Sync to Google Drive
8. **Widget** - Quick record button on home screen
9. **Wear OS** - Smart watch recording

### Security Enhancements
1. **Encryption** - Encrypt audio files and database
2. **Biometric Lock** - Require fingerprint to access
3. **Secure Storage** - Use Android Keystore for API keys
4. **Auto-delete** - Option to delete old recordings

## Troubleshooting

### Recording Not Starting
- Check if accessibility service is enabled
- Verify microphone permission granted
- Look for errors in Logcat (tag: "RecordingService")

### AI Processing Fails
- Verify API key is configured
- Check internet connection (for cloud Gemini)
- Look at error message in recording detail

### Volume Buttons Not Working
- Ensure accessibility service is running
- Check if another app is using accessibility
- Try disabling and re-enabling the service

## Build Variants

Current setup uses single build variant. For production:

```kotlin
android {
    buildTypes {
        debug {
            buildConfigField("String", "GEMINI_API_KEY", "\"debug_key\"")
        }
        release {
            buildConfigField("String", "GEMINI_API_KEY", "\"prod_key\"")
            isMinifyEnabled = true
            proguardFiles(...)
        }
    }
}
```

## Performance Considerations

- **Database queries** run on IO dispatcher
- **AI processing** runs in background coroutine
- **UI updates** via StateFlow (no unnecessary recomposition)
- **Audio files** compressed with AAC codec
- **Memory** - Only loads transcript when needed

## Compliance

### Android Requirements
- âœ… Foreground service with proper type declaration
- âœ… Persistent notification during recording
- âœ… Runtime permissions with rationale
- âœ… Accessibility service description

### Privacy
- All data stored locally by default
- No analytics or tracking
- User controls deletion
- Clear permission purposes

## Credits

Built with modern Android development best practices:
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Google Generative AI](https://ai.google.dev/)
- [Material Design 3](https://m3.material.io/)

---

**Package**: space.iamjustkrishna.srutam  
**Min SDK**: 29 (Android 10)  
**Target SDK**: 35 (Android 16)  
**Language**: Kotlin  
**Architecture**: MVVM
