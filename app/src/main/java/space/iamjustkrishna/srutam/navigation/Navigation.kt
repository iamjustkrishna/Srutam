package space.iamjustkrishna.srutam.navigation

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import space.iamjustkrishna.srutam.ui.theme.LocalIsCosmicDark
import space.iamjustkrishna.srutam.ui.theme.CosmicVoidCard
import space.iamjustkrishna.srutam.ui.theme.CosmicVoidCardBorder
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.delay
import java.io.File
import space.iamjustkrishna.srutam.service.RecordingForegroundService
import space.iamjustkrishna.srutam.ui.components.RootTab
import space.iamjustkrishna.srutam.ui.components.StudioBottomBar
import space.iamjustkrishna.srutam.ui.screens.ActionItemsScreen
import space.iamjustkrishna.srutam.ui.screens.ChatScreen
import space.iamjustkrishna.srutam.ui.screens.DetailScreen
import space.iamjustkrishna.srutam.ui.screens.FeedScreen
import space.iamjustkrishna.srutam.ui.screens.GlobalCopilotScreen
import space.iamjustkrishna.srutam.ui.components.SaveRecordingDialog
import space.iamjustkrishna.srutam.ui.screens.SettingsScreen
import space.iamjustkrishna.srutam.ui.screens.TabletWorkspaceLayout
import androidx.activity.compose.BackHandler
import space.iamjustkrishna.srutam.utils.AppPreferences
import space.iamjustkrishna.srutam.utils.AudioFileInfo
import space.iamjustkrishna.srutam.utils.AudioFileReader
import space.iamjustkrishna.srutam.utils.AudioStorage
import space.iamjustkrishna.srutam.viewmodel.AudioFilesViewModel

sealed class Screen(val route: String) {
    data object Root : Screen("root?focusRecordingId={focusRecordingId}") {
        fun createRoute(focusRecordingId: Long? = null) =
            if (focusRecordingId != null) "root?focusRecordingId=$focusRecordingId" else "root"
    }
    data object Detail : Screen("detail/{recordingId}") {
        fun createRoute(recordingId: Long) = "detail/$recordingId"
    }
    data object Chat : Screen("chat/{recordingId}") {
        fun createRoute(recordingId: Long) = "chat/$recordingId"
    }
    data object Settings : Screen("settings")
}

@Composable
fun SrutamNavigation(
    navController: NavHostController = rememberNavController(),
    initialRecordingId: Long? = null
) {
    LaunchedEffect(initialRecordingId) {
        if (initialRecordingId != null && initialRecordingId > 0L) {
            navController.navigate(Screen.Detail.createRoute(initialRecordingId))
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Root.route
    ) {
        composable(
            route = Screen.Root.route,
            arguments = listOf(
                navArgument("focusRecordingId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val focusRecordingId = backStackEntry.arguments?.getLong("focusRecordingId")?.takeIf { it > 0 }
            RootScreen(
                navController = navController,
                initialFocusRecordingId = focusRecordingId
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("recordingId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val recordingId = backStackEntry.arguments?.getLong("recordingId") ?: return@composable
            DetailScreen(
                recordingId = recordingId,
                onNavigateBack = { navController.popBackStack() },
                onShowChat = {
                    navController.navigate(Screen.Root.createRoute(recordingId)) {
                        popUpTo(Screen.Root.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("recordingId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val recordingId = backStackEntry.arguments?.getLong("recordingId") ?: return@composable
            ChatScreen(
                recordingId = recordingId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RootScreen(
    navController: NavHostController,
    initialFocusRecordingId: Long? = null,
    viewModel: AudioFilesViewModel = viewModel()
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    var currentTab by rememberSaveable { mutableStateOf(if (initialFocusRecordingId != null && initialFocusRecordingId > 0) RootTab.AI else RootTab.NOTES) }
    var focusedRecordingId by rememberSaveable { mutableStateOf(initialFocusRecordingId) }

    LaunchedEffect(initialFocusRecordingId) {
        if (initialFocusRecordingId != null && initialFocusRecordingId > 0L) {
            currentTab = RootTab.AI
            focusedRecordingId = initialFocusRecordingId
        }
    }

    BackHandler(enabled = currentTab != RootTab.NOTES) {
        currentTab = RootTab.NOTES
    }

    var isServiceRecording by remember { mutableStateOf(false) }
    var isServicePaused by remember { mutableStateOf(false) }
    var recordingElapsedMs by remember { mutableStateOf(0L) }
    val isDark = LocalIsCosmicDark.current
    var showSaveDialog by remember { mutableStateOf(false) }
    var pendingSavedFileName by remember { mutableStateOf<String?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    val showAboveToast: (String) -> Unit = { msg ->
        toastMessage = msg
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2200)
            toastMessage = null
        }
    }

    // Observe background service recording status
    LaunchedEffect(Unit) {
        var wasRecording = false
        while (true) {
            val currentRecording = RecordingForegroundService.isRecording
            if (wasRecording && !currentRecording) {
                // Recording just stopped in background service (e.g. via floating dock)
                viewModel.loadAudioFiles()
            }
            wasRecording = currentRecording
            isServiceRecording = currentRecording
            isServicePaused = RecordingForegroundService.isPaused
            recordingElapsedMs = RecordingForegroundService.elapsedDurationMs
            delay(100)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isTablet) {
            // Tablet: Side navigation rail + multi-panel workspace
            val audioFiles by viewModel.audioFiles.collectAsState()
            val recordingsByPath by viewModel.recordingsByPath.collectAsState()
            val playbackState by viewModel.audioPlayer.playbackState.collectAsState()
            val activeActions by viewModel.activeActions.collectAsState()
            val allIdeas by viewModel.allIdeas.collectAsState()
            val allDecisions by viewModel.allDecisions.collectAsState()
            val themeClusters by viewModel.themeClusters.collectAsState()

            TabletWorkspaceLayout(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                audioFiles = audioFiles,
                recordingsByPath = recordingsByPath,
                playbackState = playbackState,
                activeActions = activeActions,
                allIdeas = allIdeas,
                allDecisions = allDecisions,
                themeClusters = themeClusters,
                viewModel = viewModel,
                isRecording = isServiceRecording,
                isPaused = isServicePaused,
                recordingElapsedMs = recordingElapsedMs,
                onPlayFile = { viewModel.playAudio(it) },
                onPlayPause = { viewModel.audioPlayer.togglePlayPause() },
                onSeek = { viewModel.audioPlayer.seekTo(it) },
                onSpeedChange = { viewModel.audioPlayer.setPlaybackSpeed(it) },
                onRecordingClick = { /* Managed internally in TabletWorkspaceLayout to retain tablet 2-panel layout */ },
                onActionToggle = { viewModel.toggleActionComplete(it) },
                onStartRecording = {
                    sendRecordingAction(context, RecordingForegroundService.ACTION_START_RECORDING)
                    showAboveToast("Recording started")
                },
                onPauseToggle = {
                    if (isServicePaused) {
                        sendRecordingAction(context, RecordingForegroundService.ACTION_RESUME_RECORDING)
                        showAboveToast("Recording resumed")
                    } else {
                        sendRecordingAction(context, RecordingForegroundService.ACTION_PAUSE_RECORDING)
                        showAboveToast("Recording paused")
                    }
                },
                onFinishRecording = {
                    sendRecordingAction(context, RecordingForegroundService.ACTION_STOP_RECORDING, deferAutoAi = true)
                    val newest = AudioFileReader.getRecordingsDirectory()
                        .listFiles { f -> f.isFile && f.extension.lowercase() == "m4a" }
                        ?.maxByOrNull { f -> f.lastModified() }
                    pendingSavedFileName = newest?.nameWithoutExtension ?: "recording_${System.currentTimeMillis()}"
                    showSaveDialog = true
                },
                onCancelRecording = {
                    sendRecordingAction(context, RecordingForegroundService.ACTION_DELETE_RECORDING)
                    showAboveToast("Recording discarded")
                },
                onSendCopilotQuery = { /* TODO: Wire copilot query */ },
                onReprocess = {
                    val filePath = viewModel.audioPlayer.playbackState.value.currentFilePath
                        ?: viewModel.audioFiles.value.firstOrNull()?.filePath
                    val rec = filePath?.let { viewModel.recordingsByPath.value[it] }
                    if (rec != null) {
                        viewModel.retryAiProcessing(rec)
                    } else {
                        val audioFile = filePath?.let { path ->
                            viewModel.audioFiles.value.firstOrNull { it.filePath == path }
                        }
                        if (audioFile != null) {
                            viewModel.processRecordingForAI(audioFile)
                        }
                    }
                },
                onDeleteFile = { audioFile ->
                    viewModel.deleteAudioFile(audioFile)
                    showAboveToast("Note deleted")
                },
                onRenameFile = { audioFile, newName ->
                    viewModel.renameRecording(audioFile, newName)
                    showAboveToast("Note renamed")
                },
                onShareFile = { audioFile ->
                    try {
                        val file = File(audioFile.filePath)
                        if (file.exists()) {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                file
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "audio/*"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Voice Note"))
                        } else {
                            showAboveToast("File not found")
                        }
                    } catch (e: Exception) {
                        showAboveToast("Unable to share audio: ${e.message}")
                    }
                },
                onProcessAI = { audioFile ->
                    val rec = viewModel.recordingsByPath.value[audioFile.filePath]
                    if (rec != null) {
                        viewModel.retryAiProcessing(rec)
                    } else {
                        viewModel.processRecordingForAI(audioFile)
                    }
                    showAboveToast("Processing voice note with AI...")
                }
            )
        } else {
            // Phone: Bottom dock + tab-switched content
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when (currentTab) {
                    RootTab.NOTES -> {
                        FeedScreen(
                            onRecordingClick = { recordingId ->
                                navController.navigate(Screen.Detail.createRoute(recordingId))
                            },
                            onSettingsClick = {
                                navController.navigate(Screen.Settings.route)
                            },
                            viewModel = viewModel
                        )
                    }
                    RootTab.ACTIONS -> {
                        ActionItemsScreen(
                            onRecordingClick = { recordingId ->
                                navController.navigate(Screen.Detail.createRoute(recordingId))
                            },
                            onSettingsClick = {
                                navController.navigate(Screen.Settings.route)
                            },
                            viewModel = viewModel
                        )
                    }
                    RootTab.AI -> {
                        GlobalCopilotScreen(
                            viewModel = viewModel,
                            focusedRecordingId = focusedRecordingId,
                            onClearFocusedRecording = { focusedRecordingId = null },
                            onRecordingClick = { recordingId ->
                                navController.navigate(Screen.Detail.createRoute(recordingId))
                            },
                            onSettingsClick = {
                                navController.navigate(Screen.Settings.route)
                            }
                        )
                    }
                }

                val isKeyboardOpen = WindowInsets.isImeVisible

                // Floating Glassmorphic Bottom Bar with In-Place Morphing Recording Bar
                AnimatedVisibility(
                    visible = !isKeyboardOpen,
                    enter = fadeIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(150)),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    StudioBottomBar(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it },
                        isRecording = isServiceRecording,
                        isPaused = isServicePaused,
                        recordingElapsedMs = recordingElapsedMs,
                        onStartRecording = {
                            sendRecordingAction(context, RecordingForegroundService.ACTION_START_RECORDING)
                            showAboveToast("Recording started")
                        },
                        onPauseToggle = {
                            if (isServicePaused) {
                                sendRecordingAction(context, RecordingForegroundService.ACTION_RESUME_RECORDING)
                                showAboveToast("Recording resumed")
                            } else {
                                sendRecordingAction(context, RecordingForegroundService.ACTION_PAUSE_RECORDING)
                                showAboveToast("Recording paused")
                            }
                        },
                        onFinishRecording = {
                            sendRecordingAction(context, RecordingForegroundService.ACTION_STOP_RECORDING, deferAutoAi = true)
                            val newest = AudioFileReader.getRecordingsDirectory()
                                .listFiles { f -> f.isFile && f.extension.lowercase() == "m4a" }
                                ?.maxByOrNull { f -> f.lastModified() }
                            pendingSavedFileName = newest?.nameWithoutExtension ?: "recording_${System.currentTimeMillis()}"
                            showSaveDialog = true
                        },
                        onCancelRecording = {
                            sendRecordingAction(context, RecordingForegroundService.ACTION_DELETE_RECORDING)
                            showAboveToast("Recording discarded")
                        }
                    )
                }
            }
        }

        // Elevated In-App Toast Pill positioned cleanly above the bottom record button / dock
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn(animationSpec = tween(150)) + slideInVertically(animationSpec = tween(200)) { it / 2 },
            exit = fadeOut(animationSpec = tween(150)) + slideOutVertically(animationSpec = tween(200)) { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (isTablet) 148.dp else 140.dp)
                .zIndex(99f)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) CosmicVoidCard else Color(0xFF1E293B),
                border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFF334155)),
                shadowElevation = 6.dp
            ) {
                Text(
                    text = toastMessage.orEmpty(),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
                )
            }
        }
    }

    if (showSaveDialog) {
        SaveRecordingDialog(
            defaultName = pendingSavedFileName ?: "recording_${System.currentTimeMillis()}",
            onSave = { chosenName ->
                showSaveDialog = false
                val dir = AudioFileReader.getRecordingsDirectory()
                val newest = dir.listFiles { f -> f.isFile && f.extension.lowercase() == "m4a" }
                    ?.maxByOrNull { f -> f.lastModified() }
                var savedFile = newest
                if (newest != null && newest.exists() && chosenName.isNotBlank() && chosenName != newest.nameWithoutExtension) {
                    val targetFile = File(dir, "$chosenName.m4a")
                    if (!targetFile.exists()) {
                        if (newest.renameTo(targetFile)) {
                            savedFile = targetFile
                        }
                    }
                }
                showAboveToast("Voice note saved")
                viewModel.loadAudioFiles()

                if (savedFile != null && savedFile.exists() && AppPreferences.isAutoAiEnabled(context)) {
                    val audioFileInfo = AudioFileInfo(
                        filePath = savedFile.absolutePath,
                        fileName = savedFile.name,
                        duration = 0L,
                        timestamp = savedFile.lastModified(),
                        sizeBytes = savedFile.length()
                    )
                    viewModel.processRecordingForAI(audioFileInfo)
                }
            },
            onDiscard = {
                showSaveDialog = false
                val dir = AudioFileReader.getRecordingsDirectory()
                val newest = dir.listFiles { f -> f.isFile && f.extension.lowercase() == "m4a" }
                    ?.maxByOrNull { f -> f.lastModified() }
                if (newest != null && newest.exists()) {
                    AudioStorage.deleteAudioFile(context, newest.absolutePath)
                }
                showAboveToast("Recording discarded")
                viewModel.loadAudioFiles()
            }
        )
    }
}

private fun sendRecordingAction(context: Context, action: String, deferAutoAi: Boolean = false) {
    val intent = Intent(context, RecordingForegroundService::class.java).apply {
        this.action = action
        putExtra(RecordingForegroundService.EXTRA_DEFER_AUTO_AI, deferAutoAi)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

