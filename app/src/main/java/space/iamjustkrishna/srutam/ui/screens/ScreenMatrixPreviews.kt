package space.iamjustkrishna.srutam.ui.screens

import androidx.compose.runtime.Composable
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.InsightKind
import space.iamjustkrishna.srutam.data.InsightStatus
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import space.iamjustkrishna.srutam.player.PlaybackState
import space.iamjustkrishna.srutam.ui.theme.SrutamTheme
import space.iamjustkrishna.srutam.utils.AudioFileInfo
import space.iamjustkrishna.srutam.viewmodel.ThemeCluster

object ScreenMatrixMocks {
    val mockAudioFiles = listOf(
        AudioFileInfo(
            filePath = "/sdcard/Music/Srutam/Architecture_Strategy.m4a",
            fileName = "Architecture_Strategy.m4a",
            duration = 142000L,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 35,
            sizeBytes = 1024 * 1024 * 2
        ),
        AudioFileInfo(
            filePath = "/sdcard/Music/Srutam/Edge_AI_Whisper.m4a",
            fileName = "Edge_AI_Whisper.m4a",
            duration = 75000L,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 180,
            sizeBytes = 1024 * 900
        ),
        AudioFileInfo(
            filePath = "/sdcard/Music/Srutam/Sprint_42_Sync.m4a",
            fileName = "Sprint_42_Sync.m4a",
            duration = 48000L,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24,
            sizeBytes = 1024 * 650
        ),
        AudioFileInfo(
            filePath = "/sdcard/Music/Srutam/Vector_Embeddings_Idea.m4a",
            fileName = "Vector_Embeddings_Idea.m4a",
            duration = 32000L,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 48,
            sizeBytes = 1024 * 420
        )
    )

    val mockRecordings = mapOf(
        mockAudioFiles[0].filePath to Recording(
            id = 1L,
            timestamp = mockAudioFiles[0].timestamp,
            audioFilePath = mockAudioFiles[0].filePath,
            duration = mockAudioFiles[0].duration,
            name = "Architecture & Product Strategy",
            transcript = "We discussed transitioning our AI insight synthesis to background WorkManager workers so users never lose insights when closing the app. In addition, we decided to implement Roborazzi native graphics screenshot testing across five distinct form factors.",
            summary = "Key architectural decisions on background processing workers and automated JVM screen matrix testing across devices.",
            keyPoints = "[\"Background WorkManager for offline task queuing\",\"Roborazzi Native Graphics on JVM for screenshot verification\",\"Zero reliance on physical emulators\"]",
            actionItems = "[\"Implement WorkManager worker for background audio analysis\",\"Configure Roborazzi in build scripts\",\"Capture 40 cross-screen snapshots\"]",
            wiifm = "Eliminates visual regressions across compact phones, tablets, and foldables effortlessly.",
            aiStatus = RecordingAiStatus.READY,
            isProcessing = false
        ),
        mockAudioFiles[1].filePath to Recording(
            id = 2L,
            timestamp = mockAudioFiles[1].timestamp,
            audioFilePath = mockAudioFiles[1].filePath,
            duration = mockAudioFiles[1].duration,
            name = "Edge AI Whisper Tuning",
            transcript = null,
            summary = null,
            aiStatus = RecordingAiStatus.TRANSCRIBING,
            isProcessing = true
        ),
        mockAudioFiles[2].filePath to Recording(
            id = 3L,
            timestamp = mockAudioFiles[2].timestamp,
            audioFilePath = mockAudioFiles[2].filePath,
            duration = mockAudioFiles[2].duration,
            name = "Sprint 42 Sync",
            transcript = "Team agreed on prioritizing tablet adaptive layout with side navigation rail for larger screens.",
            summary = null,
            aiStatus = RecordingAiStatus.NOT_REQUESTED,
            isProcessing = false
        )
    )

    val mockActiveActions = listOf(
        InsightEntity(
            id = "act_1",
            recordingId = 1L,
            recordingName = "Architecture & Product Strategy",
            kind = InsightKind.ACTION,
            text = "Implement WorkManager background queue for AI processing",
            evidence = "We need tasks to finish even if app goes to background",
            status = InsightStatus.OPEN
        ),
        InsightEntity(
            id = "act_2",
            recordingId = 1L,
            recordingName = "Architecture & Product Strategy",
            kind = InsightKind.ACTION,
            text = "Verify Roborazzi native graphics tests execute on Windows host JVM",
            evidence = "Ensure no emulator or hardware dependency",
            status = InsightStatus.OPEN
        ),
        InsightEntity(
            id = "act_3",
            recordingId = 3L,
            recordingName = "Sprint 42 Sync",
            kind = InsightKind.ACTION,
            text = "Implement adaptive two-pane navigation rail on 600dp+ screens",
            evidence = "Tablet users need optimal horizontal layout",
            status = InsightStatus.COMPLETED
        )
    )

    val mockIdeas = listOf(
        InsightEntity(
            id = "idea_1",
            recordingId = 1L,
            recordingName = "Architecture & Product Strategy",
            kind = InsightKind.IDEA,
            text = "Add automated visual diff reports to CI pull request checks",
            status = InsightStatus.OPEN
        ),
        InsightEntity(
            id = "idea_2",
            recordingId = 2L,
            recordingName = "Edge AI Whisper Tuning",
            kind = InsightKind.IDEA,
            text = "Use quantized 8-bit model weights to reduce memory on compact devices",
            status = InsightStatus.OPEN
        )
    )

    val mockDecisions = listOf(
        InsightEntity(
            id = "dec_1",
            recordingId = 1L,
            recordingName = "Architecture & Product Strategy",
            kind = InsightKind.DECISION,
            text = "Adopt Roborazzi Native Graphics rather than booting heavy Android emulators",
            rationale = "Runs in under 30 seconds on JVM with pixel-accurate layout",
            status = InsightStatus.OPEN
        ),
        InsightEntity(
            id = "dec_2",
            recordingId = 3L,
            recordingName = "Sprint 42 Sync",
            kind = InsightKind.DECISION,
            text = "Keep persistent recording notification optional in app settings",
            rationale = "Allows power users quick controls while keeping standard notifications uncluttered",
            status = InsightStatus.OPEN
        )
    )

    val mockThemeClusters = listOf(
        ThemeCluster(
            key = "architecture",
            title = "System Architecture & Tests",
            noteCount = 2,
            noteIds = listOf(1L, 2L),
            noteNames = listOf("Architecture & Product Strategy", "Edge AI Whisper Tuning")
        ),
        ThemeCluster(
            key = "ui_ux",
            title = "Adaptive UI & Polish",
            noteCount = 2,
            noteIds = listOf(1L, 3L),
            noteNames = listOf("Architecture & Product Strategy", "Sprint 42 Sync")
        )
    )

    val mockChatMessages = listOf(
        GlobalChatMessage(
            text = "I can search across all your voice notes and answer any question about your recordings, meetings, and ideas.",
            isUser = false
        ),
        GlobalChatMessage(
            text = "What was decided regarding testing across multiple device sizes?",
            isUser = true
        ),
        GlobalChatMessage(
            text = "In your note 'Architecture & Product Strategy', you decided to adopt Roborazzi Native Graphics (RNG) running directly on the JVM across five screen profiles (Phone Compact, Phone Standard, Foldable, Tablet 7-inch, and Tablet 10-inch) without requiring an emulator or physical device.",
            isUser = false,
            citedNotes = listOf(Pair(1L, "Architecture & Product Strategy"))
        )
    )
}

@Composable
fun MatrixSplashPreview() {
    SrutamTheme {
        SrutamSplashScreenContent(scale = 1.0f, alpha = 1.0f)
    }
}

@Composable
fun MatrixPermissionsPreview() {
    SrutamTheme {
        PermissionsOnboardingContent(
            isMicGranted = false,
            isStorageGranted = false,
            isNotificationsGranted = true,
            isPermanentlyDenied = false
        )
    }
}

@Composable
fun MatrixFeedEmptyPreview() {
    SrutamTheme {
        FeedScreenContent(
            audioFiles = emptyList(),
            recordingsByPath = emptyMap(),
            playbackState = PlaybackState(),
            isLoading = false,
            isOnline = true
        )
    }
}

@Composable
fun MatrixFeedPopulatedPreview() {
    SrutamTheme {
        FeedScreenContent(
            audioFiles = ScreenMatrixMocks.mockAudioFiles,
            recordingsByPath = ScreenMatrixMocks.mockRecordings,
            playbackState = PlaybackState(
                isPlaying = true,
                currentFilePath = ScreenMatrixMocks.mockAudioFiles[0].filePath,
                duration = 142000,
                currentPosition = 42000
            ),
            isLoading = false,
            isOnline = true
        )
    }
}

@Composable
fun MatrixDetailPreview() {
    val recording = ScreenMatrixMocks.mockRecordings[ScreenMatrixMocks.mockAudioFiles[0].filePath]!!
    SrutamTheme {
        DetailScreenContent(
            recording = recording,
            playbackState = PlaybackState(
                isPlaying = false,
                currentFilePath = recording.audioFilePath,
                duration = recording.duration.toInt(),
                currentPosition = 35000
            ),
            isOnline = true
        )
    }
}

@Composable
fun MatrixInsightsPreview() {
    SrutamTheme {
        ActionItemsContent(
            activeActions = ScreenMatrixMocks.mockActiveActions,
            allIdeas = ScreenMatrixMocks.mockIdeas,
            allDecisions = ScreenMatrixMocks.mockDecisions,
            themeClusters = ScreenMatrixMocks.mockThemeClusters,
            archivedActionsCount = 1
        )
    }
}

@Composable
fun MatrixCopilotPreview() {
    SrutamTheme {
        GlobalCopilotContent(
            messages = ScreenMatrixMocks.mockChatMessages,
            inputText = "",
            isQueryLoading = false
        )
    }
}

@Composable
fun MatrixSettingsPreview() {
    SrutamTheme {
        SettingsScreen(onNavigateBack = {})
    }
}
