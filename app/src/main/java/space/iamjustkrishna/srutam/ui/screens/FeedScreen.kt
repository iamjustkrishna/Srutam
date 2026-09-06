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
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import space.iamjustkrishna.srutam.ui.theme.*
import space.iamjustkrishna.srutam.ui.components.SrutamTopAppBar
import space.iamjustkrishna.srutam.ui.components.SquircleActionButton
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
    onSettingsClick: () -> Unit = {},
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

    LaunchedEffect(isOnline) {
        if (isOnline) {
            viewModel.processPendingOfflineRecordings()
        }
    }

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

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            delay(120)
            searchFocusRequester.requestFocus()
        }
    }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
    }

    val filteredAudioFiles = remember(audioFiles, recordingsByPath, selectedFilter, searchQuery) {
        val baseList = when (selectedFilter) {
            FeedFilter.DEFAULT -> audioFiles
            FeedFilter.PROCESSED -> audioFiles.filter { audioFile ->
                val rec = recordingsByPath[audioFile.filePath]
                !rec?.actionItems.isNullOrBlank() && rec.actionItems != "[]"
            }
            FeedFilter.UNPROCESSED -> audioFiles.filter { audioFile ->
                val rec = recordingsByPath[audioFile.filePath]
                rec?.summary.isNullOrBlank()
            }
            FeedFilter.LONGEST -> audioFiles.sortedByDescending { it.duration }
            FeedFilter.SHORTEST -> audioFiles.sortedBy { it.duration }
        }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            val q = searchQuery.trim().lowercase()
            baseList.filter { file ->
                val rec = recordingsByPath[file.filePath]
                val nameMatch = file.fileName.lowercase().contains(q) || (rec?.name?.lowercase()?.contains(q) == true)
                val transcriptMatch = rec?.transcript?.lowercase()?.contains(q) == true ||
                        rec?.summary?.lowercase()?.contains(q) == true
                nameMatch || transcriptMatch
            }
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


    FeedScreenContent(
        audioFiles = audioFiles,
        recordingsByPath = recordingsByPath,
        playbackState = playbackState,
        isLoading = isLoading,
        isOnline = isOnline,
        selectedFilePaths = selectedFilePaths,
        removingFilePaths = removingFilePaths,
        showDeleteToast = showDeleteToast,
        deleteToastCount = deleteToastCount,
        onSelectionToggle = { filePath ->
            selectedFilePaths = if (filePath in selectedFilePaths) {
                selectedFilePaths - filePath
            } else {
                selectedFilePaths + filePath
            }
        },
        onClearSelection = { selectedFilePaths = emptySet() },
        onDeleteSelected = { showMultiDeleteDialog = true },
        onProcessBatchAI = {
            val filesToProcess = audioFiles.filter { it.filePath in selectedFilePaths }
            if (filesToProcess.isNotEmpty()) {
                viewModel.processBatchAI(filesToProcess)
                Toast.makeText(
                    context,
                    "Analyzing ${filesToProcess.size} voice notes in background...",
                    Toast.LENGTH_SHORT
                ).show()
                selectedFilePaths = emptySet()
            }
        },
        onProcessPendingOffline = {
            viewModel.processPendingOfflineRecordings()
        },
        onRecordingClick = onRecordingClick,
        onSettingsClick = onSettingsClick,
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
        onDeleteFile = { deleteFiles(listOf(it)) },
        viewModel = viewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreenContent(
    audioFiles: List<AudioFileInfo>,
    recordingsByPath: Map<String, Recording>,
    playbackState: space.iamjustkrishna.srutam.player.PlaybackState,
    isLoading: Boolean = false,
    isOnline: Boolean = true,
    selectedFilePaths: Set<String> = emptySet(),
    removingFilePaths: Set<String> = emptySet(),
    showDeleteToast: Boolean = false,
    deleteToastCount: Int = 0,
    onSelectionToggle: (String) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onProcessBatchAI: () -> Unit = {},
    onProcessPendingOffline: () -> Unit = {},
    onRecordingClick: (Long) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onPlayFile: (AudioFileInfo) -> Unit = {},
    onProcessAI: (AudioFileInfo) -> Unit = {},
    onRenameFile: (AudioFileInfo, String) -> Unit = { _, _ -> },
    onDeleteFile: (AudioFileInfo) -> Unit = {},
    viewModel: AudioFilesViewModel? = null,
    modifier: Modifier = Modifier
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    var selectedFilter by remember { mutableStateOf(FeedFilter.DEFAULT) }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            delay(120)
            searchFocusRequester.requestFocus()
        }
    }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
    }

    val isSelectionMode = selectedFilePaths.isNotEmpty()

    val filteredAudioFiles = remember(audioFiles, recordingsByPath, selectedFilter, searchQuery) {
        val baseList = when (selectedFilter) {
            FeedFilter.DEFAULT -> audioFiles
            FeedFilter.PROCESSED -> audioFiles.filter { audioFile ->
                val rec = recordingsByPath[audioFile.filePath]
                !rec?.actionItems.isNullOrBlank() && rec.actionItems != "[]"
            }
            FeedFilter.UNPROCESSED -> audioFiles.filter { audioFile ->
                val rec = recordingsByPath[audioFile.filePath]
                rec?.summary.isNullOrBlank()
            }
            FeedFilter.LONGEST -> audioFiles.sortedByDescending { it.duration }
            FeedFilter.SHORTEST -> audioFiles.sortedBy { it.duration }
        }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            val q = searchQuery.trim().lowercase()
            baseList.filter { file ->
                val rec = recordingsByPath[file.filePath]
                val nameMatch = file.fileName.lowercase().contains(q) || (rec?.name?.lowercase()?.contains(q) == true)
                val transcriptMatch = rec?.transcript?.lowercase()?.contains(q) == true ||
                        rec?.summary?.lowercase()?.contains(q) == true
                nameMatch || transcriptMatch
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF4F5F8),
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
                        containerColor = Color(0xFFF4F5F8).copy(alpha = 0.85f),
                        titleContentColor = TextPrimary
                    ),
                    modifier = Modifier.drawBehind {
                        drawLine(
                            color = Color(0xFFD6E0EC).copy(alpha = 0.6f),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onClearSelection) {
                            Icon(Icons.Default.Close, contentDescription = "Deselect all")
                        }
                    },
                    actions = {
                        IconButton(onClick = onProcessBatchAI) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "Generate AI Insights for selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onDeleteSelected) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            } else if (isSearchActive) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    "Search by title, transcript, or date...",
                                    fontSize = 13.sp,
                                    color = Color(0xFF8E8E93),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF8E8E93), modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF2F2F7),
                                unfocusedContainerColor = Color(0xFFF2F2F7),
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester)
                        )
                    },
                    actions = {
                        TextButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Text("Cancel", color = CobaltBlue, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFF4F5F8).copy(alpha = 0.85f),
                        titleContentColor = Color(0xFF1C1C1E)
                    ),
                    modifier = Modifier.drawBehind {
                        drawLine(
                            color = Color(0xFFD6E0EC).copy(alpha = 0.6f),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                )
            } else {
                SrutamTopAppBar(
                    title = "Srutam",
                    actions = {
                        SquircleActionButton(
                            icon = Icons.Default.Search,
                            contentDescription = "Search",
                            onClick = { isSearchActive = true }
                        )
                        SquircleActionButton(
                            icon = Icons.Default.Settings,
                            contentDescription = "Settings",
                            onClick = onSettingsClick
                        )
                    }
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            val pendingAiCount = remember(audioFiles, recordingsByPath) {
                audioFiles.count { audioFile ->
                    val rec = recordingsByPath[audioFile.filePath]
                    rec?.summary.isNullOrBlank()
                }
            }

            // Compact Modern Segmented Filter Capsule (32dp height)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .height(32.dp)
                    .background(
                        color = Color(0xFFEAEFF5),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(1.dp, Color(0xFFDCE4EE), RoundedCornerShape(16.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterSegmentItem(
                    title = "All Notes",
                    count = audioFiles.size,
                    isSelected = selectedFilter == FeedFilter.DEFAULT,
                    onClick = { selectedFilter = FeedFilter.DEFAULT },
                    modifier = Modifier.weight(1f)
                )
                FilterSegmentItem(
                    title = "Pending AI",
                    count = pendingAiCount,
                    isSelected = selectedFilter == FeedFilter.UNPROCESSED,
                    isAiPending = true,
                    onClick = { selectedFilter = FeedFilter.UNPROCESSED },
                    modifier = Modifier.weight(1f)
                )
            }

            // Pending AI Batch Banner
            if (selectedFilter == FeedFilter.UNPROCESSED && pendingAiCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 3.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CobaltContainer.copy(alpha = 0.6f))
                        .border(1.dp, CobaltBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CobaltBlue,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "$pendingAiCount note${if (pendingAiCount > 1) "s" else ""} ready for insights",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = CobaltBlue
                        )
                    }
                    Surface(
                        color = CobaltBlue,
                        shape = CircleShape,
                        modifier = Modifier.clickable {
                            onProcessPendingOffline()
                        }
                    ) {
                        Text(
                            text = "Process All",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (!isOnline) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "Offline mode: local transcription works. AI insights will process automatically when internet is back.",
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
                    if (searchQuery.isNotBlank()) {
                        SearchEmptyState(
                            query = searchQuery,
                            onClearSearch = { searchQuery = "" }
                        )
                    } else if (selectedFilter == FeedFilter.UNPROCESSED) {
                        PendingAiEmptyState()
                    } else {
                        NotesEmptyState()
                    }
                } else {
                    AudioFilesList(
                        audioFiles = filteredAudioFiles,
                        playbackState = playbackState,
                        onDeleteFile = onDeleteFile,
                        onPlayFile = onPlayFile,
                        onProcessAI = onProcessAI,
                        onRenameFile = onRenameFile,
                        onRecordingClick = onRecordingClick,
                        recordingsByPath = recordingsByPath,
                        viewModel = viewModel,
                        selectedFilePaths = selectedFilePaths,
                        removingFilePaths = removingFilePaths,
                        onSelectionToggle = onSelectionToggle,
                        isSelectionMode = isSelectionMode
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
fun NotesEmptyState(modifier: Modifier = Modifier) {
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
            Surface(
                shape = CircleShape,
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        tint = Color(0xFF475569)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Capture Your First Note",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap the record button below to speak your thoughts. We will transcribe and summarize them automatically.",
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
fun PendingAiEmptyState(modifier: Modifier = Modifier) {
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
            Surface(
                shape = CircleShape,
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        tint = CobaltBlue
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "All Caught Up",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Every voice note has been transcribed and synthesized with AI.",
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
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
    viewModel: AudioFilesViewModel? = null,
    selectedFilePaths: Set<String> = emptySet(),
    removingFilePaths: Set<String> = emptySet(),
    onSelectionToggle: (String) -> Unit = {},
    isSelectionMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, top = 8.dp, end = 14.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
    viewModel: AudioFilesViewModel? = null,
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
    val playerState = if (viewModel != null) {
        val state by viewModel.audioPlayer.playbackState.collectAsState()
        state
    } else {
        space.iamjustkrishna.srutam.player.PlaybackState()
    }
    val isPlaybackActive = isPlaying && playerState.currentFilePath == audioFile.filePath

    Card(
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isProcessing) Color(0xFFF4F8FF) else Color.White,
            contentColor = Color(0xFF0F172A)
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, Color(0xFF0066FF))
        } else if (isProcessing) {
            BorderStroke(1.dp, Color(0xFF0066FF).copy(alpha = 0.4f))
        } else {
            BorderStroke(1.dp, Color(0xFFE8EAEF))
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(26.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onSelectionToggle()
                    } else {
                        if (viewModel != null) {
                            viewModel.getOrCreateRecordingId(audioFile) { recordingId ->
                                onRecordingClick(recordingId)
                            }
                        } else {
                            onRecordingClick(recording?.id ?: 1L)
                        }
                    }
                },
                onLongClick = {
                    onSelectionToggle()
                }
            )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (isProcessing) {
                val shimmerTransition = rememberInfiniteTransition(label = "ai_processing_shimmer")
                val shimmerOffset by shimmerTransition.animateFloat(
                    initialValue = -400f,
                    targetValue = 900f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1600, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "shimmer_offset"
                )
                val shimmerBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFF4F8FF),
                        Color(0xFF0066FF).copy(alpha = 0.12f),
                        Color(0xFF64D2FF).copy(alpha = 0.22f),
                        Color(0xFFF4F8FF)
                    ),
                    start = Offset(shimmerOffset, 0f),
                    end = Offset(shimmerOffset + 400f, 200f)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(shimmerBrush)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Title (left) and AI Status/Action Pill (right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = displayName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    when {
                        isProcessing -> {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEBF3FF),
                                border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(11.dp),
                                        strokeWidth = 1.8.dp,
                                        color = Color(0xFF2563EB)
                                    )
                                    Text(
                                        text = "Analyzing...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                            }
                        }
                        isAiProcessed -> {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE2EEFE)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "✦",
                                        fontSize = 12.sp,
                                        color = Color(0xFF2563EB),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Summarized",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                            }
                        }
                        recording?.aiStatus == RecordingAiStatus.ERROR -> {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFEE2E2),
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .shadow(elevation = 2.dp, shape = CircleShape)
                                    .clickable(
                                        enabled = !isSelectionMode,
                                        onClick = onProcessAI
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Retry AI",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Retry AI",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                        recording?.transcript?.isNotBlank() == true -> {
                            Surface(
                                shape = CircleShape,
                                color = CobaltContainer,
                                border = BorderStroke(1.dp, CobaltBorder.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .shadow(
                                        elevation = 3.dp,
                                        shape = CircleShape,
                                        ambientColor = CobaltBlue.copy(alpha = 0.25f),
                                        spotColor = CobaltBlue.copy(alpha = 0.35f)
                                    )
                                    .clickable(
                                        enabled = !isSelectionMode,
                                        onClick = onProcessAI
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "✨",
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "Generate Insights",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CobaltBlue
                                    )
                                }
                            }
                        }
                        else -> {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFBACFFC),
                                modifier = Modifier
                                    .shadow(
                                        elevation = 4.dp,
                                        shape = CircleShape,
                                        ambientColor = Color(0xFF3B82F6).copy(alpha = 0.3f),
                                        spotColor = Color(0xFF2563EB).copy(alpha = 0.4f)
                                    )
                                    .clickable(
                                        enabled = !isSelectionMode,
                                        onClick = onProcessAI
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "✨",
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "AI Insights",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E40AF)
                                    )
                                }
                            }
                        }
                    }
                }

                // Row 2: Human-readable relative date
                Text(
                    text = formatHumanRelativeDate(audioFile.timestamp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )

                // Row 2.5: Summary Preview (Only shown AFTER AI processed, in smaller font size; NO placeholder text!)
                if (isProcessing) {
                    Text(
                        text = "Transcribing audio and extracting insights...",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (isAiProcessed && !recording?.summary.isNullOrBlank()) {
                    Text(
                        text = recording!!.summary!!.trim(),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Row 3: Audio Player Strip with Circular Blue Play button, Waveform Bars, Timestamp, and MoreVert Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Circular Ultramarine Blue Play Button
                    Surface(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = CircleShape,
                                spotColor = Color(0xFF0066FF).copy(alpha = 0.4f)
                            )
                            .clip(CircleShape)
                            .clickable(enabled = !isSelectionMode, onClick = onPlayClick),
                        shape = CircleShape,
                        color = Color(0xFF0066FF)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Inline Audio Waveform Bars
                    val totalDur = if (isPlaybackActive && playerState.duration > 0) {
                        playerState.duration.toLong()
                    } else {
                        audioFile.duration.coerceAtLeast(1)
                    }
                    val currentPos = if (isPlaybackActive) playerState.currentPosition else 0
                    val playbackProgress = (currentPos.toFloat() / totalDur.toFloat()).coerceIn(0f, 1f)

                    CardWaveformVisualizer(
                        progress = playbackProgress,
                        isPlaying = isPlaybackActive,
                        onSeekFraction = { fraction ->
                            val targetMs = (fraction * totalDur).toInt()
                            if (isPlaybackActive) {
                                viewModel?.audioPlayer?.seekTo(targetMs)
                            } else {
                                viewModel?.playAudio(audioFile)
                                viewModel?.audioPlayer?.seekTo(targetMs)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Current Timestamp or Total Duration
                    Text(
                        text = if (isPlaybackActive) {
                            formatDuration(currentPos.toLong())
                        } else {
                            formatDuration(audioFile.duration)
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )

                    // MoreVert button with custom redesigned dropdown menu in the last row
                    Box {
                        IconButton(
                            onClick = { showDropdown = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        MaterialTheme(
                            shapes = MaterialTheme.shapes.copy(
                                extraSmall = RoundedCornerShape(20.dp),
                                small = RoundedCornerShape(20.dp),
                                medium = RoundedCornerShape(20.dp)
                            )
                        ) {
                            DropdownMenu(
                                expanded = showDropdown,
                                onDismissRequest = { showDropdown = false },
                                modifier = Modifier
                                    .background(Color.White, RoundedCornerShape(20.dp))
                                    .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(20.dp))
                                    .width(180.dp),
                                shape = RoundedCornerShape(20.dp),
                                containerColor = Color.White,
                                shadowElevation = 10.dp
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Rename",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1E293B)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Edit,
                                            contentDescription = null,
                                            tint = Color(0xFF334155),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showDropdown = false
                                        showRenameDialog = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                )
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "File Info",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1E293B)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = null,
                                            tint = Color(0xFF334155),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showDropdown = false
                                        showInfoDialog = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                )
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Delete",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFFF3B30)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = null,
                                            tint = Color(0xFFFF3B30),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showDropdown = false
                                        showDeleteConfirm = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun FilterSegmentItem(
    title: String,
    count: Int,
    isSelected: Boolean,
    isAiPending: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) Color.White else Color.Transparent,
        shape = RoundedCornerShape(14.dp),
        border = if (isSelected) BorderStroke(1.dp, Color(0xFFE2E8F0)) else null,
        shadowElevation = if (isSelected) 1.5.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                letterSpacing = 0.25.sp,
                color = if (isSelected) Color(0xFF0F172A) else Color(0xFF64748B),
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(5.dp))
            if (isAiPending && count > 0) {
                Surface(
                    color = if (isSelected) CobaltContainer else Color(0xFFE0E7FF),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "✦",
                            fontSize = 8.5.sp,
                            color = CobaltBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$count",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CobaltBlue
                        )
                    }
                }
            } else {
                Surface(
                    color = if (isSelected) Color(0xFFF1F5F9) else Color(0xFFE2E8F0).copy(alpha = 0.7f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "$count",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color(0xFF334155) else Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CardWaveformVisualizer(
    progress: Float,
    isPlaying: Boolean,
    onSeekFraction: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val barHeights = remember {
        listOf(
            6, 10, 16, 22, 14, 8, 18, 24, 12, 8,
            14, 20, 26, 16, 10, 22, 18, 8, 14, 20,
            12, 6, 16, 24, 16, 10, 18, 22, 14, 8,
            12, 18, 24, 14, 10, 6
        )
    }

    Row(
        modifier = modifier
            .height(34.dp)
            .padding(horizontal = 4.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        if (size.width > 0) {
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            onSeekFraction?.invoke(fraction)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    if (size.width > 0) {
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeekFraction?.invoke(fraction)
                    }
                }
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        barHeights.forEachIndexed { index, heightDp ->
            val barFraction = index.toFloat() / (barHeights.size - 1).coerceAtLeast(1)
            val isPlayed = isPlaying && (progress >= barFraction)

            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(heightDp.dp)
                    .background(
                        color = if (isPlayed) Color(0xFF0066FF) else Color(0xFFCBD5E1),
                        shape = CircleShape
                    )
            )
        }
    }
}

private fun Recording?.isProcessedForFilter(): Boolean {
    if (this == null) return false
    return !transcript.isNullOrBlank() ||
        !summary.isNullOrBlank() ||
        aiStatus == RecordingAiStatus.READY ||
        aiStatus == RecordingAiStatus.SUMMARY_PENDING_OFFLINE
}

@Composable
fun SrutamDialogConfirmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    enabled: Boolean = true
) {
    val buttonBackground = when {
        !enabled -> Brush.verticalGradient(listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8)))
        isDestructive -> Brush.verticalGradient(listOf(Color(0xFFEF4444), Color(0xFFDC2626)))
        else -> Brush.verticalGradient(listOf(Color(0xFF3B82F6), Color(0xFF2563EB)))
    }
    val contentColor = if (enabled) Color.White else Color(0xFFF1F5F9)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .then(
                if (enabled) {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = CircleShape,
                        spotColor = if (isDestructive) Color(0x40DC2626) else Color(0x402563EB)
                    )
                } else Modifier
            )
            .background(brush = buttonBackground, shape = CircleShape)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun SrutamDialogDismissButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(
                color = Color(0xFFF1F5F9),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = Color(0xFFE2E8F0),
                shape = CircleShape
            )
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF64748B),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun SrutamCustomDialog(
    onDismissRequest: () -> Unit,
    iconBadge: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .padding(12.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(26.dp),
                    spotColor = Color(0x380F172A),
                    ambientColor = Color(0x180F172A)
                ),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFAFFFFFF),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.95f),
                        Color(0xFFCBD5E1).copy(alpha = 0.45f)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                iconBadge()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    letterSpacing = (-0.3).sp
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                content()
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (dismissButton != null) {
                        Box(modifier = Modifier.weight(1f)) {
                            dismissButton()
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        confirmButton()
                    }
                }
            }
        }
    }
}

@Composable
fun RenameDialog(
    currentName: String,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentName) }

    SrutamCustomDialog(
        onDismissRequest = onDismiss,
        iconBadge = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        brush = Brush.radialGradient(
                            listOf(
                                Color(0xFFDBEAFE),
                                Color(0xFFEFF6FF)
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White,
                                Color(0xFFBFDBFE)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            spotColor = Color(0x4D2563EB)
                        )
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color(0xFF3B82F6),
                                    Color(0xFF1D4ED8)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        title = "Rename Recording",
        subtitle = "Enter a new title for this audio recording.",
        content = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (text.isNotBlank()) {
                        IconButton(
                            onClick = { text = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear text",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        },
        confirmButton = {
            SrutamDialogConfirmButton(
                text = "Rename",
                onClick = { onRename(text.takeIf { it.isNotBlank() } ?: currentName) }
            )
        },
        dismissButton = {
            SrutamDialogDismissButton(
                text = "Cancel",
                onClick = onDismiss
            )
        }
    )
}

@Composable
fun SaveRecordingDialog(
    defaultName: String,
    onSave: (String) -> Unit,
    onDiscard: () -> Unit
) {
    var text by remember { mutableStateOf(defaultName) }
    val isNameValid = text.trim().isNotEmpty()

    SrutamCustomDialog(
        onDismissRequest = onDiscard,
        iconBadge = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.radialGradient(
                            listOf(
                                Color(0xFFDBEAFE),
                                Color(0xFFEFF6FF)
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White,
                                Color(0xFFBFDBFE)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = CircleShape,
                            spotColor = Color(0x4D2563EB)
                        )
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color(0xFF3B82F6),
                                    Color(0xFF1D4ED8)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        title = "Save Voice Note",
        subtitle = "Give your new recording a name or keep the default.",
        content = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (text.isNotBlank()) {
                        IconButton(
                            onClick = { text = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear text",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        },
        confirmButton = {
            SrutamDialogConfirmButton(
                text = "Save Note",
                enabled = isNameValid,
                onClick = {
                    if (isNameValid) {
                        onSave(text.trim())
                    }
                }
            )
        },
        dismissButton = {
            SrutamDialogDismissButton(
                text = "Discard",
                onClick = onDiscard
            )
        }
    )
}

@Composable
fun AudioInfoDialog(
    displayName: String,
    audioFile: AudioFileInfo,
    onDismiss: () -> Unit
) {
    SrutamCustomDialog(
        onDismissRequest = onDismiss,
        iconBadge = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        brush = Brush.radialGradient(
                            listOf(
                                Color(0xFFE2E8F0),
                                Color(0xFFF1F5F9)
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White,
                                Color(0xFFCBD5E1)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = CircleShape,
                            spotColor = Color(0x260F172A)
                        )
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color(0xFF64748B),
                                    Color(0xFF475569)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        },
        title = "File Info",
        subtitle = "Audio metadata and storage location details.",
        content = {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InfoRow(label = "Title", value = displayName)
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    InfoRow(label = "Duration", value = formatDuration(audioFile.duration))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    InfoRow(label = "Recorded Date", value = formatDate(audioFile.timestamp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    InfoRow(label = "File Size", value = formatFileSize(audioFile.sizeBytes))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    InfoRow(label = "File Path", value = audioFile.filePath)
                }
            }
        },
        confirmButton = {
            SrutamDialogConfirmButton(
                text = "Done",
                onClick = onDismiss
            )
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF64748B)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A)
        )
    }
}

@Composable
fun DeleteConfirmationDialog(
    recordingName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    SrutamCustomDialog(
        onDismissRequest = onDismiss,
        iconBadge = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        brush = Brush.radialGradient(
                            listOf(
                                Color(0xFFFEE2E2),
                                Color(0xFFFEF2F2)
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White,
                                Color(0xFFFECACA)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            spotColor = Color(0x4DDC2626)
                        )
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color(0xFFEF4444),
                                    Color(0xFFDC2626)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        title = "Delete Recording?",
        subtitle = "Are you sure you want to permanently delete \"$recordingName\"? This action cannot be undone.",
        content = {},
        confirmButton = {
            SrutamDialogConfirmButton(
                text = "Delete",
                onClick = onConfirm,
                isDestructive = true
            )
        },
        dismissButton = {
            SrutamDialogDismissButton(
                text = "Cancel",
                onClick = onDismiss
            )
        }
    )
}

@Composable
fun MultiDeleteConfirmationDialog(
    recordingNames: List<String>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    SrutamCustomDialog(
        onDismissRequest = onDismiss,
        iconBadge = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        brush = Brush.radialGradient(
                            listOf(
                                Color(0xFFFEE2E2),
                                Color(0xFFFEF2F2)
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White,
                                Color(0xFFFECACA)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            spotColor = Color(0x4DDC2626)
                        )
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color(0xFFEF4444),
                                    Color(0xFFDC2626)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        title = "Delete ${recordingNames.size} Recording${if (recordingNames.size > 1) "s" else ""}?",
        subtitle = "You are about to permanently delete the following recordings:",
        content = {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
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
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = name,
                                fontSize = 12.sp,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            SrutamDialogConfirmButton(
                text = "Delete All",
                onClick = onConfirm,
                isDestructive = true
            )
        },
        dismissButton = {
            SrutamDialogDismissButton(
                text = "Cancel",
                onClick = onDismiss
            )
        }
    )
}

@Composable
private fun SearchEmptyState(
    query: String,
    onClearSearch: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No matching recordings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "No notes or transcripts matched \"$query\".",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF2563EB),
                    modifier = Modifier.clickable { onClearSearch() }
                ) {
                    Text(
                        text = "Clear Search",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
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
