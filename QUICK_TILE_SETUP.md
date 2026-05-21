# Quick Settings Tile Implementation - Setup Guide

## What Changed

The app now uses a **Quick Settings Tile** for easy voice recording access. Recording happens entirely in the notification - no separate screen opens.

## New Features

### 1. Quick Settings Tile Service
- **Location**: Pull down notification shade → Edit tiles → Add "Voice Record" tile
- **Usage**: Tap tile to start/stop recording
- **State**: Active (recording) / Inactive (idle)

### 2. Notification with Live Duration
- Shows recording duration that updates every second
- Format: "🔴 Recording - 0:05"
- "Stop & Analyze" button to finish recording
- Stays in notification shade only - no popup screen

### 3. Instant Feed Updates
- Recordings appear in Feed screen immediately after stopping
- Real-time database synchronization with Flow
- AI processing happens in background

## Setup Instructions

### 1. Add Quick Settings Tile

**For Most Android Devices:**
1. Swipe down from top of screen (notification shade)
2. Swipe down again to expand Quick Settings
3. Tap the **Edit** icon (pencil ✏️) or three-dot menu (⋮)
4. Find **"Voice Record"** in available tiles
5. Drag it to your active tiles area
6. Tap "Done" or back button

**For Realme/ColorOS:**
1. Swipe down from top → Quick Settings
2. Tap three-dot menu (⋮) at top-right
3. Look for "Voice Record" tile
4. Long-press and drag to active tiles
5. Tap "Done"

### 2. Grant Permissions
The app will request:
- ✅ Microphone access (RECORD_AUDIO)
- ✅ Notification permission (POST_NOTIFICATIONS)

### 3. Usage Flow

**Starting a Recording:**
1. Pull down Quick Settings
2. Tap "Voice Record" tile
3. Notification appears with timer: "🔴 Recording - 0:00"
4. Timer updates every second

**During Recording:**
- Notification shows live duration
- "Stop & Analyze" button available
- Recording continues in background

**Stopping Recording:**
- Tap "Stop & Analyze" in notification, OR
- Tap the Quick Settings tile again
- Recording saves to `/Music/Srutam/`
- Appears instantly in app Feed screen
- AI processing starts automatically

## What You See

### Notification Format
```
🔴 Recording - 1:23
Tap 'Stop & Analyze' to finish recording
[Stop & Analyze] button
```

### Feed Screen
- New recordings appear at the top instantly
- Shows "Processing..." while AI analyzes
- Displays summary when complete
- Updates in real-time without refresh

## Architecture

### Files Added
- `QuickRecordingTileService.kt` - TileService implementation

### Files Modified
- `RecordingForegroundService.kt` - Added live duration updates in notification
- `AndroidManifest.xml` - Added TileService declaration
- `AIProcessor.kt` - Improved AI output format

### Files Removed
- `RecordingActivity.kt` - No longer needed (recording is notification-only)

### Permissions Used
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

## Technical Details

### Recording Service Flow
1. **Tile Tap** → `QuickRecordingTileService.onClick()`
2. **Start Service** → `RecordingForegroundService.startRecording()`
3. **Show Notification** → Initial: "🔴 Recording - 0:00"
4. **Update Loop** → Notification updates every 1 second with duration
5. **Stop Service** → Save to database → Trigger AI processing
6. **Database Insert** → Room Flow emits update
7. **UI Update** → Feed screen shows new recording instantly

### Real-Time Feed Updates
```kotlin
// ViewModel observes database Flow
val recordings: StateFlow<List<Recording>> = repository.allRecordings
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

// DAO returns Flow for auto-updates
@Query("SELECT * FROM recordings ORDER BY timestamp DESC")
fun getAllRecordings(): Flow<List<Recording>>
```

When a new recording is inserted:
1. DAO emits new list
2. Repository forwards to ViewModel
3. StateFlow updates
4. Feed screen recomposes automatically

## AI Analysis Format

The AI outputs exactly four sections:

### 1. Executive Summary
- Exactly 2 sentences
- First: Main topic
- Second: Key takeaway

### 2. Action Items
- Checklist format: `[ ] Task description`
- Specific and actionable
- 3-5 items

### 3. Key Insights
- Bulleted list
- Specific details
- 3-5 points

### 4. The Value Prop ("What's In It For Me?")
- Explains personal utility
- Concrete application examples
- User-centric benefit statement

## Testing Checklist

- [ ] Quick Settings Tile appears in edit menu
- [ ] Tile toggles recording state correctly
- [ ] Notification shows with 🔴 icon
- [ ] Duration updates every second (0:00 → 0:01 → 0:02...)
- [ ] "Stop & Analyze" button works
- [ ] Recording appears in Feed instantly after stopping
- [ ] "Processing..." shows while AI analyzes
- [ ] Summary appears when AI completes
- [ ] No separate screen opens (notification only)
- [ ] Recording from locked screen works via tile

## Troubleshooting

### Tile doesn't appear
- Check Android version ≥ 7.0 (API 24+)
- Verify app is installed correctly
- Try uninstall → restart device → reinstall
- Check `adb logcat` for errors

### Duration doesn't update
- Ensure POST_NOTIFICATIONS permission granted
- Check notification channel importance is HIGH
- Verify service is running: `adb shell dumpsys activity services`

### Recording doesn't appear in Feed
- Check database: recordings should save to Room
- Verify Flow is collecting in ViewModel
- Check logs: `adb logcat | grep "RecordingService"`

### AI processing fails
- Check GEMINI_API_KEY in local.properties
- Verify internet connection (current implementation uses cloud API)
- Check logs: `adb logcat | grep "AIProcessor"`

## TODO: Production Enhancements

### 1. ML Kit Speech-to-Text
Current: Placeholder transcript
Required:
```kotlin
// Add dependency
implementation 'com.google.mlkit:speech-recognition:16.0.0'

// Implement in AIProcessor.kt
val recognizer = SpeechRecognition.getClient(...)
val result = recognizer.recognize(audioInputStream, config)
```

### 2. Gemini Nano (AICore)
Current: Cloud-based Gemini API
Required:
```kotlin
// Use AICore for on-device processing
val aiCore = AICore.getClient(context)
val model = aiCore.getGenerativeModel("gemini-nano")
```

Reference: https://developer.android.com/ai/gemini-nano

## Other Recording Methods

The app also supports:
- **FAB Button** in Feed screen (hold to record)
- **Volume Button** double-press (requires Accessibility Service)

The Quick Settings Tile is the recommended method for:
- ✅ Fastest access (from any screen)
- ✅ Works from lock screen
- ✅ No UI popup
- ✅ Most reliable

## Support

For issues or questions:
- Check LogCat: `adb logcat | grep -E "QuickRecordingTile|RecordingService"`
- File issues at project repository
- Tags to search: `QuickRecordingTile`, `RecordingService`, `AIProcessor`
