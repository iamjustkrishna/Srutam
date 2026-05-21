// This file has been reorganized. All FeedScreen components are now in this file.
// The improvements include:
// - Better UI with rounded cards
// - AI button on each recording item
// - Rename/menu option (three-dot) on each item
// - Improved settings dialog
// - Better FAB animation
// - Smooth scrolling with proper spacing
// - Unified Material 3 theme throughout
// - Enhanced visual hierarchy

package space.iamjustkrishna.srutam.ui.screens


import android.app.Activity
import android.os.Build
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import space.iamjustkrishna.srutam.R
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import space.iamjustkrishna.srutam.service.PersistentRecordingNotificationService
import space.iamjustkrishna.srutam.service.RecordingForegroundService
import space.iamjustkrishna.srutam.utils.AudioFileInfo
import space.iamjustkrishna.srutam.utils.AudioStorage
import space.iamjustkrishna.srutam.utils.NetworkUtils
import space.iamjustkrishna.srutam.utils.RecordingNameFormatter
import space.iamjustkrishna.srutam.viewmodel.AudioFilesViewModel
import kotlin.math.roundToInt

private enum class FabState { IDLE, RECORDING_HELD, RECORDING_LOCKED }
private enum class FeedFilter(val label: String, val icon: ImageVector) {
    DEFAULT("Default", Icons.Default.FilterAlt),
    PROCESSED("Processed", Icons.Default.TaskAlt),
    UNPROCESSED("Unprocessed", Icons.Default.HourglassEmpty),
    LONGEST("Longest", Icons.Default.ArrowDownward),
    SHORTEST("Shortest", Icons.Default.ArrowUpward)
}

private const val META_SEPARATOR = " \u2022 "

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onRecordingClick: (Long) -> Unit,
    viewModel: AudioFilesViewModel = viewModel()
) {
    val audioFiles by viewModel.audioFiles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val recordingsByPath by viewModel.recordingsByPath.collectAsState()
    val playbackState by viewModel.audioPlayer.playbackState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isOnline = NetworkUtils.isInternetAvailable(context)

    var isRecording by remember { mutableStateOf(false) }
    var isRecordingPaused by remember { mutableStateOf(false) }
    var recordingElapsedMs by remember { mutableStateOf(0L) }
    var selectedFilter by remember { mutableStateOf(FeedFilter.DEFAULT) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var selectedFilePaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showMultiDeleteDialog by remember { mutableStateOf(false) }
    var removingFilePaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteToast by remember { mutableStateOf(false) }
    var deleteToastCount by remember { mutableStateOf(0) }
    var pendingScopedDeleteFiles by remember { mutableStateOf<List<AudioFileInfo>>(emptyList()) }
    val scope = rememberCoroutineScope()

    val isSelectionMode = selectedFilePaths.isNotEmpty()
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val filesToDelete = pendingScopedDeleteFiles
        pendingScopedDeleteFiles = emptyList()
        if (result.resultCode == Activity.RESULT_OK && filesToDelete.isNotEmpty()) {
            deleteToastCount = filesToDelete.size
            showDeleteToast = true
            scope.launch {
                var current = removingFilePaths
                for (file in filesToDelete) {
                    current = current + file.filePath
                    removingFilePaths = current
                    delay(60)
                }
                delay(140)
                if (filesToDelete.size == 1) {
                    viewModel.deleteAudioFile(filesToDelete.first())
                } else {
                    viewModel.deleteMultipleAudioFiles(filesToDelete)
                }
                removingFilePaths = emptySet()
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            isRecording = RecordingForegroundService.isRecording
            isRecordingPaused = RecordingForegroundService.isPaused
            recordingElapsedMs = RecordingForegroundService.elapsedDurationMs
            delay(200)
        }
    }

    LaunchedEffect(isRecording) {
        if (!isRecording) {
            delay(500)
            viewModel.loadAudioFiles()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadAudioFiles()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showSettingsDialog by remember { mutableStateOf(false) }

    val filteredAudioFiles = remember(audioFiles, recordingsByPath, selectedFilter) {
        when (selectedFilter) {
            FeedFilter.DEFAULT -> audioFiles
            FeedFilter.PROCESSED -> audioFiles.filter { audioFile ->
                recordingsByPath[audioFile.filePath].isProcessedForFilter()
            }
            FeedFilter.UNPROCESSED -> audioFiles.filter { audioFile ->
                !recordingsByPath[audioFile.filePath].isProcessedForFilter()
            }
            FeedFilter.LONGEST -> audioFiles.sortedByDescending { it.duration }
            FeedFilter.SHORTEST -> audioFiles.sortedBy { it.duration }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            onDismiss = { showSettingsDialog = false }
        )
    }

    fun deleteFiles(filesToDelete: List<AudioFileInfo>) {
        scope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intentSender = AudioStorage.createDeleteRequest(
                    context = context,
                    filePaths = filesToDelete.map { it.filePath }
                )
                if (intentSender != null) {
                    pendingScopedDeleteFiles = filesToDelete
                    deleteLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
            } else {
                deleteToastCount = filesToDelete.size
                showDeleteToast = true
                if (filesToDelete.size == 1) {
                    viewModel.deleteAudioFile(filesToDelete.first())
                } else {
                    var current = removingFilePaths
                    for (file in filesToDelete) {
                        current = current + file.filePath
                        removingFilePaths = current
                        delay(60)
                    }
                    delay(140)
                    viewModel.deleteMultipleAudioFiles(filesToDelete)
                    removingFilePaths = emptySet()
                }
            }
        }
    }

    if (showMultiDeleteDialog && selectedFilePaths.isNotEmpty()) {
        val filesToDelete = filteredAudioFiles.filter { it.filePath in selectedFilePaths }
        MultiDeleteConfirmationDialog(
            recordingNames = filesToDelete.map { audioFile ->
                RecordingNameFormatter.displayName(
                    fileName = audioFile.fileName,
                    timestamp = audioFile.timestamp,
                    savedName = recordingsByPath[audioFile.filePath]?.name
                )
            },
            onConfirm = {
                selectedFilePaths = emptySet()
                showMultiDeleteDialog = false
                deleteFiles(filesToDelete)
            },
            onDismiss = { showMultiDeleteDialog = false }
        )
    }

    if (showDeleteToast) {
        LaunchedEffect(deleteToastCount) {
            delay(2000)
            showDeleteToast = false
        }
    }


    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            "${selectedFilePaths.size} selected",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    navigationIcon = {
                        IconButton(onClick = { selectedFilePaths = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Deselect all")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showMultiDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.srutam_final_log),
                                    contentDescription = "Srutam",
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    "Srutam",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Text(
                                text = if (selectedFilter == FeedFilter.DEFAULT) {
                                    "${audioFiles.size} recordings"
                                } else {
                                    "${filteredAudioFiles.size} of ${audioFiles.size} recordings"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    actions = {
                        Box {
                            IconButton(onClick = { showFilterMenu = true }) {
                                Icon(ImageVector.vectorResource(R.drawable.filter), contentDescription = "Filter",
                                    modifier = Modifier.size(24.dp))
                            }
                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false },
                                shape = RoundedCornerShape(16.dp),
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                FeedFilter.entries.forEach { filter ->
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = filter.icon,
                                                contentDescription = null,
                                                tint = if (filter == selectedFilter) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        },
                                        text = {
                                            Text(
                                                text = filter.label,
                                                fontWeight = if (filter == selectedFilter) {
                                                    FontWeight.Bold
                                                } else {
                                                    FontWeight.Normal
                                                }
                                            )
                                        },
                                        trailingIcon = {
                                            if (filter == selectedFilter) {
                                                Icon(
                                                    imageVector = Icons.Default.Done,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedFilter = filter
                                            showFilterMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (!isOnline) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "Offline mode: local transcription works, and AI summaries will become available when internet is back.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                if (isLoading && audioFiles.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (filteredAudioFiles.isEmpty()) {
                    EmptyState()
                } else {
                    AudioFilesList(
                        audioFiles = filteredAudioFiles,
                        playbackState = playbackState,
                        onDeleteFile = { deleteFiles(listOf(it)) },
                        onPlayFile = { audioFile ->
                            if (playbackState.currentFilePath == audioFile.filePath) {
                                if (playbackState.isPlaying) {
                                    viewModel.audioPlayer.pause()
                                    viewModel.audioPlayer.seekTo(0)
                                } else {
                                    viewModel.audioPlayer.play()
                                }
                            } else {
                                viewModel.playAudio(audioFile)
                            }
                        },
                        onProcessAI = { audioFile ->
                            viewModel.processRecordingForAI(audioFile)
                        },
                        onRenameFile = { audioFile, newName ->
                            viewModel.renameRecording(audioFile, newName)
                        },
                        onRecordingClick = onRecordingClick,
                        recordingsByPath = recordingsByPath,
                        viewModel = viewModel,
                        selectedFilePaths = selectedFilePaths,
                        removingFilePaths = removingFilePaths,
                        onSelectionToggle = { filePath ->
                            selectedFilePaths = if (filePath in selectedFilePaths) {
                                selectedFilePaths - filePath
                            } else {
                                selectedFilePaths + filePath
                            }
                        },
                        isSelectionMode = isSelectionMode
                    )
                }

                Box(modifier = Modifier.align(Alignment.BottomCenter)){
            RecordingFAB(
                isRecording = isRecording,
                isPaused = isRecordingPaused,
                elapsedMs = recordingElapsedMs,
                onStartRecording = { startRecording(context) },
                onPauseRecording = { pauseRecording(context) },
                onResumeRecording = { resumeRecording(context) },
                onSaveRecording = { stopRecording(context) },
                onDeleteRecording = { deleteRecordingInProgress(context) }
            )
        }

        if (showDeleteToast) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = "Deleted $deleteToastCount recording${if (deleteToastCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var geminiKey by remember { mutableStateOf(space.iamjustkrishna.srutam.utils.AppPreferences.getGeminiApiKey(context)) }
    var isPersistentNotificationEnabled by remember {
        mutableStateOf(space.iamjustkrishna.srutam.utils.AppPreferences.isPersistentNotificationEnabled(context))
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val newValue = !isPersistentNotificationEnabled
                            isPersistentNotificationEnabled = newValue
                            space.iamjustkrishna.srutam.utils.AppPreferences.setPersistentNotificationEnabled(context, newValue)
                            if (newValue) {
                                startPersistentRecordingNotification(context)
                            } else {
                                stopPersistentRecordingNotification(context)
                            }
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Persistent Recording Notification",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Keep a start-recording button in notifications",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isPersistentNotificationEnabled,
                        onCheckedChange = { checked ->
                            isPersistentNotificationEnabled = checked
                            space.iamjustkrishna.srutam.utils.AppPreferences.setPersistentNotificationEnabled(context, checked)
                            if (checked) {
                                startPersistentRecordingNotification(context)
                            } else {
                                stopPersistentRecordingNotification(context)
                            }
                        }
                    )
                }

                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Gemini API Key",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Default key is provided. Use your own to increase limits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                    TextField(
                        value = geminiKey,
                        onValueChange = { geminiKey = it },
                        singleLine = true,
                        placeholder = { Text("Paste your Gemini API key") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ElevatedButton(
                        onClick = {
                            space.iamjustkrishna.srutam.utils.AppPreferences.setGeminiApiKey(context, geminiKey)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save API Key")
                    }
                }
            }
        },
        confirmButton = {
            ElevatedButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun startRecording(context: Context) {
    sendRecordingAction(context, RecordingForegroundService.ACTION_START_RECORDING)
}

private fun pauseRecording(context: Context) {
    sendRecordingAction(context, RecordingForegroundService.ACTION_PAUSE_RECORDING)
}

private fun resumeRecording(context: Context) {
    sendRecordingAction(context, RecordingForegroundService.ACTION_RESUME_RECORDING)
}

private fun stopRecording(context: Context) {
    sendRecordingAction(context, RecordingForegroundService.ACTION_STOP_RECORDING)
}

private fun deleteRecordingInProgress(context: Context) {
    sendRecordingAction(context, RecordingForegroundService.ACTION_DELETE_RECORDING)
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

private fun startPersistentRecordingNotification(context: Context) {
    val intent = Intent(context, PersistentRecordingNotificationService::class.java).apply {
        action = PersistentRecordingNotificationService.ACTION_START_RECORDING
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun stopPersistentRecordingNotification(context: Context) {
    val intent = Intent(context, PersistentRecordingNotificationService::class.java).apply {
        action = PersistentRecordingNotificationService.ACTION_STOP_NOTIFICATION
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

@Composable
fun RecordingIndicatorBanner() {
    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_animation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(12.dp)
                    .alpha(alpha)
            ) {
                drawCircle(color = Color.Red)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Recording in progress...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No recordings yet",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hold the recording button below to start capturing your audio notes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioFilesList(
    audioFiles: List<AudioFileInfo>,
    playbackState: space.iamjustkrishna.srutam.player.PlaybackState,
    onDeleteFile: (AudioFileInfo) -> Unit,
    onPlayFile: (AudioFileInfo) -> Unit,
    onProcessAI: (AudioFileInfo) -> Unit,
    onRenameFile: (AudioFileInfo, String) -> Unit,
    onRecordingClick: (Long) -> Unit,
    recordingsByPath: Map<String, Recording>,
    viewModel: AudioFilesViewModel,
    selectedFilePaths: Set<String> = emptySet(),
    removingFilePaths: Set<String> = emptySet(),
    onSelectionToggle: (String) -> Unit = {},
    isSelectionMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(audioFiles, key = { it.filePath }) { audioFile ->
            val isRemoving = audioFile.filePath in removingFilePaths
            AnimatedVisibility(
                visible = !isRemoving,
                enter = fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                AudioFileCard(
                    audioFile = audioFile,
                    recording = recordingsByPath[audioFile.filePath],
                    isPlaying = playbackState.isPlaying && playbackState.currentFilePath == audioFile.filePath,
                    onPlayClick = { onPlayFile(audioFile) },
                    onProcessAI = { onProcessAI(audioFile) },
                    onRenameClick = { newName -> onRenameFile(audioFile, newName) },
                    onDelete = { onDeleteFile(audioFile) },
                    onRecordingClick = onRecordingClick,
                    viewModel = viewModel,
                    isSelected = audioFile.filePath in selectedFilePaths,
                    onSelectionToggle = { onSelectionToggle(audioFile.filePath) },
                    isSelectionMode = isSelectionMode
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioFileCard(
    audioFile: AudioFileInfo,
    recording: Recording?,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onProcessAI: () -> Unit,
    onRenameClick: (String) -> Unit,
    onDelete: () -> Unit,
    onRecordingClick: (Long) -> Unit,
    viewModel: AudioFilesViewModel,
    isSelected: Boolean = false,
    onSelectionToggle: () -> Unit = {},
    isSelectionMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showDropdown by remember { mutableStateOf(false) }

    val displayName = remember(audioFile.fileName, audioFile.timestamp, recording?.name) {
        RecordingNameFormatter.displayName(
            fileName = audioFile.fileName,
            timestamp = audioFile.timestamp,
            savedName = recording?.name
        )
    }

    val selectionModeState by rememberUpdatedState(isSelectionMode)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (selectionModeState) {
                false
            } else
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                showDeleteConfirm = true
                false
            } else {
                false
            }
        }
    )

    if (showRenameDialog) {
        RenameDialog(
            currentName = displayName,
            onRename = { newName ->
                onRenameClick(newName)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }

    if (showDeleteConfirm) {
        DeleteConfirmationDialog(
            recordingName = displayName,
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (showInfoDialog) {
        AudioInfoDialog(
            displayName = displayName,
            audioFile = audioFile,
            onDismiss = { showInfoDialog = false }
        )
    }

    // Determine if AI has processed this file (summary ready)
    val isAiProcessed = recording?.aiStatus == RecordingAiStatus.READY && !recording.summary.isNullOrBlank()
    val isProcessing = recording?.isProcessing == true
    val isPlaybackActive = isPlaying

    SwipeToDismissBox(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        state = dismissState,
        backgroundContent = {
            // Delete background (shown when swiped left)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.error,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { showDeleteConfirm = true },
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    modifier = Modifier.padding(end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Delete",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        },
        content = {
            // Card content
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAiProcessed) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = if (isSelected) {
                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                } else {
                    null
                },
                modifier = Modifier
                    .height(72.dp)
                    .combinedClickable(
                        onClick = {
                            if (isSelectionMode) {
                                onSelectionToggle()
                            } else {
                                viewModel.getOrCreateRecordingId(audioFile) { recordingId ->
                                    onRecordingClick(recordingId)
                                }
                            }
                        },
                        onLongClick = {
                            onSelectionToggle()
                        }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    if (isPlaybackActive) {
                        val playbackTransition = rememberInfiniteTransition(label = "playback_shimmer")
                        val playbackOffset by playbackTransition.animateFloat(
                            initialValue = -500f,
                            targetValue = 500f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1400, easing = EaseInOut),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "playback_offset"
                        )
                        val playbackBrush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                                Color(0xFF00C2A8).copy(alpha = 0.22f),
                                Color(0xFF58D7FF).copy(alpha = 0.16f)
                            ),
                            start = Offset(playbackOffset, 0f),
                            end = Offset(playbackOffset + 500f, 140f)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(playbackBrush)
                        )
                    }
                    if (isProcessing) {
                        val shimmerTransition = rememberInfiniteTransition(label = "ai_processing_shimmer")
                        val shimmerOffset by shimmerTransition.animateFloat(
                            initialValue = -600f,
                            targetValue = 600f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "shimmer_offset"
                        )
                        val shimmerBrush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                            ),
                            start = Offset(shimmerOffset, 0f),
                            end = Offset(shimmerOffset + 600f, 120f)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(shimmerBrush)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play button
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable(enabled = !isSelectionMode, onClick = onPlayClick)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Stop" else "Play",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                    // Text info (name, date + duration, and optional summary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Combined Date & Duration to save horizontal space
                        val durationStr = formatDuration(audioFile.duration)
                        Text(
                            text = if (recording?.isProcessing == true) {
                                when (recording.aiStatus) {
                                    RecordingAiStatus.TRANSCRIBING -> "Transcribing locally...$META_SEPARATOR$durationStr"
                                    RecordingAiStatus.SUMMARY_PROCESSING -> "Generating summary...$META_SEPARATOR$durationStr"
                                    else -> "AI is processing...$META_SEPARATOR$durationStr"
                                }
                            } else if (recording?.aiStatus == RecordingAiStatus.SUMMARY_PENDING_OFFLINE) {
                                "Transcript ready$META_SEPARATOR$durationStr"
                            } else if (recording?.aiStatus == RecordingAiStatus.ERROR) {
                                "Error processing$META_SEPARATOR$durationStr"
                            } else {
                                "${formatDate(audioFile.timestamp)}$META_SEPARATOR$durationStr"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        val summaryPreview = recording?.summary
                        if (!summaryPreview.isNullOrBlank()) {
                            Text(
                                text = summaryPreview,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                        // Single AI Button indicating status
                        IconButton(
                            onClick = {
                                if (recording?.transcript?.isNotBlank() == true && recording.summary.isNullOrBlank()) {
                                    viewModel.generateSummaryForRecording(audioFile)
                                } else {
                                    onProcessAI()
                                }
                            },
                            enabled = !isSelectionMode && !isProcessing && !isAiProcessed,
                            modifier = Modifier.size(40.dp)
                        ) {
                        if (recording?.isProcessing == true) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.auto_awesome),
                                contentDescription = "AI Action",
                                // Dim the icon if already processed
                                tint = if (isAiProcessed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        }

                    // Dropdown menu button
                    Box {
                        IconButton(
                            onClick = { showDropdown = !showDropdown },
                            modifier = Modifier.size(40.dp),
                            enabled = !isSelectionMode
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Dropdown menu
                        DropdownMenu(
                            expanded = showDropdown,
                            onDismissRequest = { showDropdown = false },
                            modifier = Modifier
                                .shadow(8.dp, RoundedCornerShape(12.dp))
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(12.dp)
                                )
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Info") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showDropdown = false
                                    showInfoDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showDropdown = false
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Delete",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showDropdown = false
                                    showDeleteConfirm = true
                                }
                            )
                        }
                    }
                    }
                }
            }
        }
    )
}

private fun Recording?.isProcessedForFilter(): Boolean {
    if (this == null) return false
    return !transcript.isNullOrBlank() ||
        !summary.isNullOrBlank() ||
        aiStatus == RecordingAiStatus.READY ||
        aiStatus == RecordingAiStatus.SUMMARY_PENDING_OFFLINE
}

@Composable
fun RenameDialog(
    currentName: String,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentName) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Recording") },
        text = {
            androidx.compose.material3.TextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
        },
        confirmButton = {
            ElevatedButton(onClick = { onRename(text.takeIf { it.isNotBlank() } ?: currentName) }) {
                Text("Rename")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AudioInfoDialog(
    displayName: String,
    audioFile: AudioFileInfo,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Recording Info",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow(label = "Name", value = displayName)
                InfoRow(label = "Duration", value = formatDuration(audioFile.duration))
                InfoRow(label = "Date", value = formatDate(audioFile.timestamp))
                InfoRow(label = "Size", value = formatFileSize(audioFile.sizeBytes))
                InfoRow(label = "File", value = audioFile.filePath)
            }
        },
        confirmButton = {
            ElevatedButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DeleteConfirmationDialog(
    recordingName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                "Delete Recording?",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Are you sure you want to permanently delete \"$recordingName\"?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "This action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            ElevatedButton(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 4.dp)
                )
                Text("Delete", color = Color.White)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun MultiDeleteConfirmationDialog(
    recordingNames: List<String>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                "Delete ${recordingNames.size} Recording${if (recordingNames.size > 1) "s" else ""}?",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "You are about to permanently delete the following recordings:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(recordingNames) { name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                Text(
                    "This action cannot be undone, but you can undo within 5 seconds.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            ElevatedButton(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 4.dp)
                )
                Text("Delete All", color = Color.White)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

private fun parseJsonArray(json: String?): List<String> {
    if (json == null) return emptyList()
    return try {
        com.google.gson.Gson().fromJson(json, Array<String>::class.java).toList()
    } catch (e: Exception) {
        listOf(json)
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecordingFAB(
    isRecording: Boolean,
    isPaused: Boolean,
    elapsedMs: Long,
    onStartRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onSaveRecording: () -> Unit,
    onDeleteRecording: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var fabState by remember { mutableStateOf(FabState.IDLE) }
    var dragX by remember { mutableStateOf(0f) }
    var dragY by remember { mutableStateOf(0f) }

    val fabOffsetX by animateFloatAsState(
        targetValue = if (fabState == FabState.RECORDING_HELD) dragX else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fab_offsetX"
    )

    val lockThresholdPx = with(density) { -80.dp.toPx() }
    val cancelThresholdPx = with(density) { -100.dp.toPx() }

    val formattedElapsed = String.format("%02d:%02d", (elapsedMs / 1000) / 60, (elapsedMs / 1000) % 60)

    // FAB Colors: primary when idle, red when recording
    val fabColor by animateColorAsState(
        targetValue = when {
            fabState == FabState.RECORDING_HELD -> Color(0xFFE53935) // Recording Red
            fabState == FabState.RECORDING_LOCKED && !isPaused -> Color(0xFFE53935)
            fabState == FabState.RECORDING_LOCKED && isPaused -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(250),
        label = "fab_color"
    )

    val holdPulse = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by holdPulse.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {

        // ==========================================
        // STATE 1: RECORDING HELD (Unified Pill UI)
        // ==========================================
        AnimatedVisibility(
            visible = fabState == FabState.RECORDING_HELD,
            enter = fadeIn() + slideInHorizontally(),
            exit = fadeOut() + slideOutHorizontally(),
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {

                // UNIFIED PILL: Timer + Slide to Cancel
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 4.dp) // Centered vertically with 56dp FAB
                        .offset(x = (dragX * 0.3f).dp) // Subtle movement with drag
                        .shadow(3.dp, CircleShape)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsing Dot
                    Canvas(modifier = Modifier.size(10.dp).alpha(pulseAlpha)) {
                        drawCircle(Color.Red)
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    // Current Duration
                    Text(
                        text = formattedElapsed,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(24.dp))

                    // Slide to Cancel
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Slide to cancel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // LOCK INDICATOR (Floating above the FAB)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 80.dp, end = 8.dp)
                        .shadow(2.dp, RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = pulseAlpha),
                        modifier = Modifier.size(24.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = pulseAlpha),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ==========================================
        // STATE 2: RECORDING LOCKED (Standard Controls)
        // ==========================================
        AnimatedVisibility(
            visible = fabState == FabState.RECORDING_LOCKED,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
        ) {
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { onDeleteRecording(); fabState = FabState.IDLE }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }

                    Text(
                        text = formattedElapsed,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaused) MaterialTheme.colorScheme.onSurfaceVariant else Color.Red
                    )

                    IconButton(
                        onClick = { if (isPaused) onResumeRecording() else onPauseRecording() },
                        modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.Mic else Icons.Default.Pause,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    FloatingActionButton(
                        onClick = { onSaveRecording(); fabState = FabState.IDLE },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White)
                    }
                }
            }
        }

        // ==========================================
        // MAIN FAB (The fix for "breaking" gestures)
        // ==========================================
        AnimatedVisibility(
            visible = fabState != FabState.RECORDING_LOCKED,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset { IntOffset(fabOffsetX.roundToInt(), 0) }
                    .size(56.dp)
                    .shadow(if (fabState == FabState.RECORDING_HELD) 12.dp else 4.dp, CircleShape)
                    .background(fabColor, CircleShape)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()

                            val startTime = System.currentTimeMillis()
                            var isCurrentlyTouching = true

                            fabState = FabState.RECORDING_HELD
                            onStartRecording()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            while (isCurrentlyTouching) {
                                val event = awaitPointerEvent()
                                val dragEvent = event.changes.firstOrNull()

                                if (dragEvent != null && dragEvent.pressed) {
                                    val positionChange = dragEvent.positionChange()
                                    dragX += positionChange.x
                                    dragY += positionChange.y
                                    dragEvent.consume()

                                    if (dragX <= cancelThresholdPx) {
                                        onDeleteRecording()
                                        fabState = FabState.IDLE
                                        isCurrentlyTouching = false // Breaks the loop and prepares for next tap
                                    } else if (dragY <= lockThresholdPx) {
                                        fabState = FabState.RECORDING_LOCKED
                                        dragX = 0f
                                        isCurrentlyTouching = false
                                    }
                                } else {
                                    // Finger Released logic
                                    val duration = System.currentTimeMillis() - startTime
                                    if (fabState == FabState.RECORDING_HELD) {
                                        when {
                                            duration < 300 -> fabState = FabState.RECORDING_LOCKED
                                            duration < 1000 -> {
                                                onDeleteRecording()
                                                fabState = FabState.IDLE
                                            }
                                            else -> {
                                                onSaveRecording()
                                                fabState = FabState.IDLE
                                            }
                                        }
                                    }
                                    isCurrentlyTouching = false
                                }
                            }
                            // Important: Reset drag values so the UI snaps back for the next attempt
                            dragX = 0f
                            dragY = 0f
                        }
                    }
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.FiberManualRecord else Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun AudioPreviewModal(
    audioFile: AudioFileInfo,
    displayName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val previewPlayer = remember {
        val player = space.iamjustkrishna.srutam.player.AudioPlayer(context)
        player
    }
    
    LaunchedEffect(Unit) {
        try {
            isLoading = true
            val file = java.io.File(audioFile.filePath)
            if (file.exists()) {
                previewPlayer.prepare(file)
            } else {
                error = "Audio file not found"
            }
        } catch (e: Exception) {
            error = "Failed to load: ${e.message}"
        }
    }
    
    // Observe playback state
    val playbackState = previewPlayer.playbackState.collectAsState().value
    LaunchedEffect(playbackState) {
        isPlaying = playbackState.isPlaying
        currentPosition = playbackState.currentPosition.toLong()
        duration = playbackState.duration.toLong()
        isLoading = playbackState.isLoading
        if (playbackState.error != null) {
            error = playbackState.error
        }
    }
    
    // Cleanup on dismiss
    DisposableEffect(Unit) {
        onDispose {
            previewPlayer.release()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .pointerInput(Unit) { detectTapGestures { } }
            .clickable(enabled = false) { }
    ) {
        // Preview modal card
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = "Preview",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // File name
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Error message or content
                if (error != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    // Progress bar
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(2.dp)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(
                                        if (duration > 0) (currentPosition.toFloat() / duration).coerceIn(0f, 1f)
                                        else 0f
                                    )
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                        
                        // Time display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDuration(currentPosition),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatDuration(duration),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Play/Pause button
                    FloatingActionButton(
                        onClick = {
                            if (isPlaying) {
                                previewPlayer.pause()
                            } else {
                                previewPlayer.play()
                            }
                        },
                        modifier = Modifier.size(56.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    // Loading indicator
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    }
                }
                
                // Close button
                ElevatedButton(
                    onClick = {
                        previewPlayer.pause()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

// ============ PREVIEW COMPOSABLES ============

@Preview(showBackground = true, heightDp = 600, widthDp = 400)
@Composable
fun AudioPreviewModalPreview() {
    space.iamjustkrishna.srutam.ui.theme.SrutamTheme {
        AudioPreviewModal(
            audioFile = AudioFileInfo(
                filePath = "/Music/Srutam/recording_1234567890.m4a",
                fileName = "meeting_notes_2024.m4a",
                duration = 187000L,
                timestamp = System.currentTimeMillis(),
                sizeBytes = 5242880L
            ),
            displayName = "Meeting Notes - May 5",
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 150, widthDp = 400)
@Composable
fun AudioFileCardPreview() {
    space.iamjustkrishna.srutam.ui.theme.SrutamTheme {
        AudioFileCard(
            audioFile = AudioFileInfo(
                filePath = "/Music/Srutam/recording_1234567890.m4a",
                fileName = "recording_1234567890.m4a",
                duration = 187000L,
                timestamp = System.currentTimeMillis(),
                sizeBytes = 5242880L
            ),
            recording = Recording(
                id = 1L,
                audioFilePath = "/Music/Srutam/recording_1234567890.m4a",
                duration = 187000L,
                name = "Meeting Notes - May 5",
                timestamp = System.currentTimeMillis(),
                transcript = "This is a sample transcript of the recording.",
                summary = "Sample summary of the meeting",
                keyPoints = """["Point 1", "Point 2", "Point 3"]""",
                actionItems = """["Action 1", "Action 2"]""",
                wiifm = "What's in it for me section",
                isProcessing = false,
                aiStatus = RecordingAiStatus.READY,
                processingError = null
            ),
            isPlaying = false,
            onPlayClick = {},
            onProcessAI = {},
            onRenameClick = {},
            onDelete = {},
            onRecordingClick = {},
            viewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        )
    }
}
