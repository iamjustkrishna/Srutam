package space.iamjustkrishna.srutam.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import space.iamjustkrishna.srutam.ui.theme.*



import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import android.widget.Toast
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.InsightKind
import space.iamjustkrishna.srutam.data.InsightStatus
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.OutlinedButton
import space.iamjustkrishna.srutam.utils.RecordingNameFormatter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import space.iamjustkrishna.srutam.R
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import space.iamjustkrishna.srutam.utils.AudioStorage
import space.iamjustkrishna.srutam.utils.NetworkUtils
import space.iamjustkrishna.srutam.viewmodel.DetailViewModel
import kotlinx.coroutines.launch



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    recordingId: Long,
    onNavigateBack: () -> Unit,
    onShowChat: () -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    val recording by viewModel.recording.collectAsState()
    val noteInsights by viewModel.noteInsights.collectAsState()
    val context = LocalContext.current
    val askQuestionsEnabled = recording?.let {
        !it.isProcessing && !it.transcript.isNullOrBlank()
    } == true
    val askQuestionsTooltipState = rememberTooltipState(isPersistent = false)
    val coroutineScope = rememberCoroutineScope()
    var pendingScopedDelete by remember { mutableStateOf<Recording?>(null) }
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val recordingToDelete = pendingScopedDelete
        pendingScopedDelete = null
        if (result.resultCode == Activity.RESULT_OK && recordingToDelete != null) {
            viewModel.deleteRecording()
            onNavigateBack()
        }
    }

    LaunchedEffect(recordingId) {
        viewModel.loadRecording(recordingId)
    }

    val playbackState by viewModel.playbackState.collectAsState()

    DetailScreenContent(
        recording = recording,
        playbackState = playbackState,
        noteInsights = noteInsights,
        isOnline = NetworkUtils.isInternetAvailable(context),
        onNavigateBack = onNavigateBack,
        onShowChat = onShowChat,
        onDelete = {
            val currentRecording = recording ?: return@DetailScreenContent
            coroutineScope.launch {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intentSender = AudioStorage.createDeleteRequest(
                        context = context,
                        filePaths = listOf(currentRecording.audioFilePath)
                    )
                    if (intentSender != null) {
                        pendingScopedDelete = currentRecording
                        deleteLauncher.launch(
                            IntentSenderRequest.Builder(intentSender).build()
                        )
                    }
                } else {
                    viewModel.deleteRecording()
                    onNavigateBack()
                }
            }
        },
        onPlayPause = { viewModel.togglePlayPause() },
        onSeek = { viewModel.seekTo(it) },
        onSpeedChange = { viewModel.setPlaybackSpeed(it) },
        onRename = { viewModel.renameRecording(it) },
        onGenerateSummary = {
            viewModel.generateAiSummary()
            if (NetworkUtils.isInternetAvailable(context)) {
                Toast.makeText(context, "Analyzing voice note in background...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Queued for processing. AI summary will generate when internet is available.", Toast.LENGTH_LONG).show()
            }
        },
        onRetryProcessing = {
            viewModel.retryAiProcessing()
            if (NetworkUtils.isInternetAvailable(context)) {
                Toast.makeText(context, "Retrying processing in background...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Queued for retry when internet is connected.", Toast.LENGTH_LONG).show()
            }
        },
        onReprocess = {
            viewModel.reprocessWithAi()
            if (NetworkUtils.isInternetAvailable(context)) {
                Toast.makeText(context, "Re-processing note with AI...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Queued for fresh AI processing when internet connects.", Toast.LENGTH_LONG).show()
            }
        },
        onActionToggle = { viewModel.toggleActionComplete(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreenContent(
    recording: Recording?,
    playbackState: space.iamjustkrishna.srutam.player.PlaybackState,
    noteInsights: List<InsightEntity> = emptyList(),
    isOnline: Boolean = true,
    initialTabIndex: Int = 0,
    onNavigateBack: () -> Unit = {},
    onShowChat: () -> Unit = {},
    onDelete: () -> Unit = {},
    onPlayPause: () -> Unit = {},
    onSeek: (Int) -> Unit = {},
    onSpeedChange: (Float) -> Unit = {},
    onRename: (String) -> Unit = {},
    onGenerateSummary: () -> Unit = {},
    onRetryProcessing: () -> Unit = {},
    onReprocess: () -> Unit = {},
    onActionToggle: (InsightEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val askQuestionsEnabled = recording?.let {
        !it.isProcessing && !it.transcript.isNullOrBlank()
    } == true

    Scaffold(
        containerColor = CeramicWhite,
        floatingActionButton = {
            if (askQuestionsEnabled) {
                Surface(
                    onClick = onShowChat,
                    shape = CircleShape,
                    color = CobaltBlue,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "✦",
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Ask Srutam AI",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Note Details",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = Color(0xFF1C1C1E)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = Color(0xFF1C1C1E),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFF64748B))
                    }
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
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (recording == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            RecordingDetailsContent(
                recording = recording,
                playbackState = playbackState,
                noteInsights = noteInsights,
                isOnline = isOnline,
                initialTabIndex = initialTabIndex,
                onPlayPause = onPlayPause,
                onSeek = onSeek,
                onSpeedChange = onSpeedChange,
                onRename = onRename,
                onGenerateSummary = onGenerateSummary,
                onRetryProcessing = onRetryProcessing,
                onReprocess = onReprocess,
                onActionToggle = onActionToggle,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
fun RecordingDetails(
    recording: Recording,
    viewModel: DetailViewModel,
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val noteInsights by viewModel.noteInsights.collectAsState()
    RecordingDetailsContent(
        recording = recording,
        playbackState = playbackState,
        noteInsights = noteInsights,
        isOnline = isOnline,
        onPlayPause = { viewModel.togglePlayPause() },
        onSeek = { viewModel.seekTo(it) },
        onSpeedChange = { viewModel.setPlaybackSpeed(it) },
        onRename = { viewModel.renameRecording(it) },
        onGenerateSummary = { viewModel.generateAiSummary() },
        onRetryProcessing = { viewModel.retryAiProcessing() },
        onReprocess = { viewModel.reprocessWithAi() },
        onActionToggle = { viewModel.toggleActionComplete(it) },
        modifier = modifier
    )
}

@Composable
fun RecordingDetailsContent(
    recording: Recording,
    playbackState: space.iamjustkrishna.srutam.player.PlaybackState,
    noteInsights: List<InsightEntity> = emptyList(),
    isOnline: Boolean = true,
    initialTabIndex: Int = 0,
    onPlayPause: () -> Unit = {},
    onSeek: (Int) -> Unit = {},
    onSpeedChange: (Float) -> Unit = {},
    onRename: (String) -> Unit = {},
    onGenerateSummary: () -> Unit = {},
    onRetryProcessing: () -> Unit = {},
    onReprocess: () -> Unit = {},
    onActionToggle: (InsightEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedDetailTabIndex by remember { mutableStateOf(initialTabIndex) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val displayName = remember(recording.audioFilePath, recording.timestamp, recording.name) {
        RecordingNameFormatter.displayName(
            fileName = recording.audioFilePath,
            timestamp = recording.timestamp,
            savedName = recording.name
        )
    }

    if (showRenameDialog) {
        RenameDialog(
            currentName = displayName,
            onRename = { newName ->
                onRename(newName)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }

    val actions = remember(noteInsights) {
        noteInsights.filter { it.kind == InsightKind.ACTION }
    }
    val ideas = remember(noteInsights) {
        noteInsights.filter { it.kind == InsightKind.IDEA }
    }
    val decisions = remember(noteInsights) {
        noteInsights.filter { it.kind == InsightKind.DECISION }
    }
    val legacyActions = remember(recording.actionItems) {
        parseJsonArray(recording.actionItems)
    }
    val legacyPoints = remember(recording.keyPoints) {
        parseJsonArray(recording.keyPoints)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Metadata
        item {
            MetadataCard(
                recording = recording,
                displayName = displayName,
                onRenameClick = { showRenameDialog = true }
            )
        }

        // Audio Player
        item {
            AudioPlayerCard(
                playbackState = playbackState,
                onPlayPause = onPlayPause,
                onSeek = onSeek,
                onSpeedChange = onSpeedChange,
                fallbackDuration = recording?.duration ?: 0L
            )
        }

        // Segmented Control Tabs matching Apple Studio Mockup
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF2F2F7), RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("✦ Summary", "📄 Transcript", "💡 Insights").forEachIndexed { index, title ->
                    val isSelected = (selectedDetailTabIndex == index)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .background(
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedDetailTabIndex = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF1C1C1E) else Color(0xFF8E8E93)
                        )
                    }
                }
            }
        }

        // Tab 0: Summary & Insights
        if (selectedDetailTabIndex == 0) {
            val isFallbackSummary = recording.summary?.startsWith("This recording contains approximately") == true

            if (!recording.isProcessing && recording.summary.isNullOrBlank()) {
                item {
                    SummaryPromptCard(
                        isOnline = isOnline,
                        hasTranscript = !recording.transcript.isNullOrBlank(),
                        onGenerateSummary = onGenerateSummary
                    )
                }
            }

            // Fallback summary indicator card with one-click re-process
            if (!recording.isProcessing && isFallbackSummary) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Offline Audio Overview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This note has local audio details, but full AI summary, action items, and key points haven't been generated yet because of offline or spotty connection.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onReprocess,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Full AI Insights")
                            }
                        }
                    }
                }
            }

            // Processing Status
            if (recording.isProcessing) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = when (recording.aiStatus) {
                                    RecordingAiStatus.TRANSCRIBING -> "Transcribing locally..."
                                    RecordingAiStatus.SUMMARY_PROCESSING -> "Generating AI summary..."
                                    else -> "Processing with AI..."
                                },
                                modifier = Modifier.padding(start = 16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            // Deferred Summary (offline transcribed, no summary yet)
            if (!recording.isProcessing &&
                recording.aiStatus == RecordingAiStatus.SUMMARY_PENDING_OFFLINE &&
                !recording.transcript.isNullOrBlank() &&
                recording.summary.isNullOrBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Ready for AI Insights",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your audio has been transcribed locally. Tap below to generate AI summary, key points, and action items.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onGenerateSummary,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Generate AI Summary")
                            }
                        }
                    }
                }
            }

            // Error State with Retry
            if (!recording.isProcessing &&
                recording.aiStatus == RecordingAiStatus.ERROR &&
                recording.processingError != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Processing Failed",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = recording.processingError,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onRetryProcessing,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Retry Processing")
                            }
                        }
                    }
                }
            }

            // Summary
            if (recording.summary != null) {
                item {
                    SectionCard(
                        title = if (isFallbackSummary) "Audio Overview" else "Summary",
                        content = recording.summary
                    )
                }
            }

            // WIIFM
            if (recording.wiifm != null) {
                item {
                    SectionCard(
                        title = "What's In It For Me",
                        content = recording.wiifm
                    )
                }
            }

            // Key Points
            if (recording.keyPoints != null) {
                item {
                    BulletListCard(
                        title = "Key Points",
                        items = parseJsonArray(recording.keyPoints)
                    )
                }
            }

            // Re-process option for notes with existing summary
            if (!recording.isProcessing && !isFallbackSummary && recording.summary != null) {
                item {
                    OutlinedButton(
                        onClick = onReprocess,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 16.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Re-process Note with AI", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Tab 1: Transcript
        if (selectedDetailTabIndex == 1) {
            item {
                if (!recording.transcript.isNullOrBlank()) {
                    SectionCard(
                        title = "Full Transcript",
                        content = recording.transcript
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CeramicWhite),
                        border = BorderStroke(0.5.dp, SlateBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Transcript is not available yet.",
                                color = Color(0xFF8E8E93),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Tab 2: Insights (Next Steps, Key Ideas, Decisions)
        if (selectedDetailTabIndex == 2) {
            val hasRoomInsights = noteInsights.isNotEmpty()
            val hasLegacyContent = legacyActions.isNotEmpty() || legacyPoints.isNotEmpty()

            if (!hasRoomInsights && !hasLegacyContent) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CeramicWhite),
                        border = BorderStroke(1.dp, SlateBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = SlateGrouped,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = CobaltBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Insights Extracted Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Generate an AI summary to extract Next Steps, Key Ideas, and Decisions for this voice note.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (hasRoomInsights) {
                // Next Steps Section
                if (actions.isNotEmpty()) {
                    item {
                        val completedCount = actions.count { it.status == InsightStatus.COMPLETED }
                        val allDone = completedCount == actions.size
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = CobaltContainer,
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "✓",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CobaltBlue
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Next Steps",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (allDone) EmeraldContainer else SlateGrouped,
                                border = BorderStroke(
                                    0.5.dp,
                                    if (allDone) EmeraldSuccess.copy(alpha = 0.3f) else SlateBorder
                                )
                            ) {
                                Text(
                                    text = "$completedCount/${actions.size} done",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (allDone) OnEmeraldContainer else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    items(actions, key = { it.id }) { action ->
                        DetailActionItemCard(
                            action = action,
                            onToggle = { onActionToggle(action) }
                        )
                    }
                }

                // Key Ideas Section
                if (ideas.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF3C7),
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Key Ideas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF3C7),
                                border = BorderStroke(0.5.dp, Color(0xFFFDE68A))
                            ) {
                                Text(
                                    text = "${ideas.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    items(ideas, key = { it.id }) { idea ->
                        DetailIdeaItemCard(idea = idea)
                    }
                }

                // Decisions Section
                if (decisions.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldContainer,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Decisions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EmeraldContainer,
                                border = BorderStroke(0.5.dp, Color(0xFFA7F3D0))
                            ) {
                                Text(
                                    text = "${decisions.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnEmeraldContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    items(decisions, key = { it.id }) { decision ->
                        DetailDecisionItemCard(decision = decision)
                    }
                }
            } else {
                // Fallback for legacy recordings
                if (legacyActions.isNotEmpty()) {
                    item {
                        BulletListCard(
                            title = "Action Items",
                            items = legacyActions
                        )
                    }
                }
                if (legacyPoints.isNotEmpty()) {
                    item {
                        BulletListCard(
                            title = "Key Points",
                            items = legacyPoints
                        )
                    }
                }
            }
        }

        // Error
        if (recording.processingError != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Processing Error",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = recording.processingError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryPromptCard(
    isOnline: Boolean,
    hasTranscript: Boolean,
    onGenerateSummary: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (hasTranscript) {
                    "Transcript is ready locally. AI summary is not generated yet."
                } else {
                    "Transcript and AI summary not generated yet."
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isOnline) {
                    if (hasTranscript) {
                        "Tap below to generate the AI summary, key points, and action items."
                    } else {
                        "Tap below to transcribe locally and generate the AI summary."
                    }
                } else {
                    if (hasTranscript) {
                        "You are offline. Tap below to queue AI summary generation when internet connects."
                    } else {
                        "You are offline. Tap below to transcribe audio locally and queue AI summary."
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onGenerateSummary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (hasTranscript) "Generate AI Summary" else "Transcribe and Summarize")
            }
        }
    }
}

@Composable
fun MetadataCard(
    recording: Recording,
    displayName: String,
    onRenameClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CeramicWhite),
        border = BorderStroke(0.5.dp, SlateBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRenameClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onRenameClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Rename Note",
                        tint = CobaltBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = formatDate(recording.timestamp),
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93)
                )
                Text(
                    text = "•",
                    fontSize = 13.sp,
                    color = Color(0xFFC7C7CC)
                )
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = formatDuration(recording.duration),
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93)
                )
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CeramicWhite),
        border = BorderStroke(0.5.dp, SlateBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CobaltBlue
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = SlateBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun BulletListCard(
    title: String,
    items: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CeramicWhite),
        border = BorderStroke(0.5.dp, SlateBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CobaltBlue
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = SlateBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            items.forEach { item ->
                val cleanItem = item.removePrefix("[ ] ").removePrefix("- ").removePrefix("* ").trim()
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = CobaltBlue,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = cleanItem,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun AudioPlayerCard(
    playbackState: space.iamjustkrishna.srutam.player.PlaybackState,
    onPlayPause: () -> Unit,
    onSeek: (Int) -> Unit,
    onSpeedChange: (Float) -> Unit,
    fallbackDuration: Long = 0L,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CeramicWhite),
        border = BorderStroke(0.5.dp, SlateBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Main Playback Strip: Play Button + Waveform + Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circular Cobalt Blue Play Button
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(
                            enabled = !playbackState.isLoading && playbackState.error == null,
                            onClick = onPlayPause
                        ),
                    shape = CircleShape,
                    color = CobaltBlue,
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (playbackState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Interactive Waveform Scrubber with fallback duration for long audio
                val totalDuration = (if (playbackState.duration > 0) playbackState.duration else fallbackDuration.toInt()).coerceAtLeast(1)
                val currentPos = playbackState.currentPosition.coerceIn(0, totalDuration)
                val progress = (currentPos.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

                DetailWaveformScrubber(
                    progress = progress,
                    isPlaying = playbackState.isPlaying,
                    onSeekFraction = { fraction ->
                        onSeek((fraction * totalDuration).toInt())
                    },
                    modifier = Modifier.weight(1f)
                )

                // Timecode
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatTime(currentPos),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (playbackState.isPlaying) CobaltBlue else TextPrimary
                    )
                    Text(
                        text = formatTime(totalDuration),
                        fontSize = 10.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
            }

            // Speed Selector Pills Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Playback Speed",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF8E8E93)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        val isSelected = (playbackState.speed == speed)
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) CobaltBlue else Color(0xFFF2F2F7),
                            modifier = Modifier.clickable { onSpeedChange(speed) }
                        ) {
                            Text(
                                text = "${speed}x",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF3C3C43),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Error message
            if (playbackState.error != null) {
                Text(
                    text = playbackState.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun DetailWaveformScrubber(
    progress: Float,
    isPlaying: Boolean,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val barHeights = remember {
        listOf(
            6, 12, 18, 10, 24, 16, 8, 14, 22, 12, 6, 18, 26, 14,
            10, 20, 14, 8, 16, 22, 12, 8, 18, 24, 10, 14, 8, 6
        )
    }

    Row(
        modifier = modifier
            .height(40.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        if (size.width > 0) {
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            onSeekFraction(fraction)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    if (size.width > 0) {
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeekFraction(fraction)
                    }
                }
            },
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        barHeights.forEachIndexed { index, heightDp ->
            val barFraction = index.toFloat() / (barHeights.size - 1).coerceAtLeast(1)
            val isPlayed = progress >= barFraction

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(heightDp.dp)
                    .background(
                        color = if (isPlayed) CobaltBlue else Color(0xFFE5E5EA),
                        shape = CircleShape
                    )
            )
        }
    }
}

private fun parseJsonArray(json: String?): List<String> {
    if (json == null) return emptyList()
    return try {
        val gson = Gson()
        gson.fromJson(json, Array<String>::class.java).toList()
    } catch (e: Exception) {
        listOf(json)
    }
}

@Composable
private fun DetailActionItemCard(
    action: InsightEntity,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = action.status == InsightStatus.COMPLETED

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CeramicWhite.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, if (isCompleted) SlateBorder.copy(alpha = 0.5f) else SlateBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isCompleted) EmeraldSuccess else SlateSurface,
                border = BorderStroke(
                    1.5.dp,
                    if (isCompleted) EmeraldSuccess else Color(0xFFCBD5E1)
                ),
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggle)
            ) {
                if (isCompleted) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onToggle)
            ) {
                Text(
                    text = action.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCompleted) TextMuted else TextPrimary,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                if (!action.evidence.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SlateGrouped,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "\"${action.evidence}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailIdeaItemCard(
    idea: InsightEntity,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CeramicWhite.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "IDEA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB45309),
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = idea.text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            if (!idea.evidence.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFFBEB),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${idea.evidence}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailDecisionItemCard(
    decision: InsightEntity,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CeramicWhite.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldContainer,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DECISION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnEmeraldContainer,
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = decision.text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            if (!decision.rationale.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Rationale: ${decision.rationale}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            if (!decision.evidence.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFECFDF5),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${decision.evidence}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF065F46),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
