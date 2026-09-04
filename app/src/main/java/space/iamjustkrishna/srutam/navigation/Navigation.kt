package space.iamjustkrishna.srutam.navigation

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import space.iamjustkrishna.srutam.ui.screens.SaveRecordingDialog
import space.iamjustkrishna.srutam.ui.screens.SettingsScreen
import space.iamjustkrishna.srutam.utils.AudioFileReader
import space.iamjustkrishna.srutam.utils.AudioStorage
import space.iamjustkrishna.srutam.viewmodel.AudioFilesViewModel

sealed class Screen(val route: String) {
    data object Root : Screen("root")
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
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Root.route
    ) {
        composable(Screen.Root.route) {
            RootScreen(navController = navController)
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
                onShowChat = { navController.navigate(Screen.Chat.createRoute(recordingId)) }
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
    viewModel: AudioFilesViewModel = viewModel()
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(RootTab.NOTES) }

    var isServiceRecording by remember { mutableStateOf(false) }
    var isServicePaused by remember { mutableStateOf(false) }
    var recordingElapsedMs by remember { mutableStateOf(0L) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var pendingSavedFileName by remember { mutableStateOf<String?>(null) }

    // Observe background service recording status
    LaunchedEffect(Unit) {
        while (true) {
            isServiceRecording = RecordingForegroundService.isRecording
            isServicePaused = RecordingForegroundService.isPaused
            recordingElapsedMs = RecordingForegroundService.elapsedDurationMs
            delay(100)
        }
    }

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
                },
                onPauseToggle = {
                    if (isServicePaused) {
                        sendRecordingAction(context, RecordingForegroundService.ACTION_RESUME_RECORDING)
                    } else {
                        sendRecordingAction(context, RecordingForegroundService.ACTION_PAUSE_RECORDING)
                    }
                },
                onFinishRecording = {
                    sendRecordingAction(context, RecordingForegroundService.ACTION_STOP_RECORDING)
                    val newest = AudioFileReader.getRecordingsDirectory()
                        .listFiles { f -> f.isFile && f.extension.lowercase() == "m4a" }
                        ?.maxByOrNull { f -> f.lastModified() }
                    pendingSavedFileName = newest?.nameWithoutExtension ?: "recording_${System.currentTimeMillis()}"
                    showSaveDialog = true
                },
                onCancelRecording = {
                    sendRecordingAction(context, RecordingForegroundService.ACTION_DELETE_RECORDING)
                }
            )
        }

        if (showSaveDialog) {
            SaveRecordingDialog(
                defaultName = pendingSavedFileName ?: "recording_${System.currentTimeMillis()}",
                onSave = { chosenName ->
                    showSaveDialog = false
                    val dir = AudioFileReader.getRecordingsDirectory()
                    val newest = dir.listFiles { f -> f.isFile && f.extension.lowercase() == "m4a" }
                        ?.maxByOrNull { f -> f.lastModified() }
                    if (newest != null && newest.exists() && chosenName.isNotBlank() && chosenName != newest.nameWithoutExtension) {
                        val targetFile = File(dir, "$chosenName.m4a")
                        if (!targetFile.exists()) {
                            newest.renameTo(targetFile)
                        }
                    }
                    viewModel.loadAudioFiles()
                },
                onDiscard = {
                    showSaveDialog = false
                    val dir = AudioFileReader.getRecordingsDirectory()
                    val newest = dir.listFiles { f -> f.isFile && f.extension.lowercase() == "m4a" }
                        ?.maxByOrNull { f -> f.lastModified() }
                    if (newest != null && newest.exists()) {
                        AudioStorage.deleteAudioFile(context, newest.absolutePath)
                    }
                    viewModel.loadAudioFiles()
                }
            )
        }
    }
}

private fun sendRecordingAction(context: Context, action: String) {
    val intent = Intent(context, RecordingForegroundService::class.java).apply {
        this.action = action
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

