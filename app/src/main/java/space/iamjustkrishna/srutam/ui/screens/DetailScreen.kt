package space.iamjustkrishna.srutam.ui.screens



import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recording Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        state = askQuestionsTooltipState,
                        tooltip = {
                            PlainTooltip {
                                Text("First transcribe and summarize.")
                            }
                        }
                    ) {
                        IconButton(
                            onClick = {
                                if (askQuestionsEnabled) {
                                    onShowChat()
                                } else {
                                    coroutineScope.launch {
                                        askQuestionsTooltipState.show()
                                    }
                                }
                            }
                        ) {
                            Icon(
                                ImageVector.vectorResource(R.drawable.aichat),
                                contentDescription = "Ask Questions",
                                modifier = Modifier.alpha(if (askQuestionsEnabled) 1f else 0.38f)
                            )
                        }
                    }
                    IconButton(onClick = {
                        val currentRecording = recording ?: return@IconButton
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
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
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
            RecordingDetails(
                recording = recording!!,
                viewModel = viewModel,
                isOnline = NetworkUtils.isInternetAvailable(context),
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Metadata
        item {
            MetadataCard(recording)
        }

        // Audio Player
        item {
            AudioPlayerCard(
                playbackState = playbackState,
                onPlayPause = { viewModel.togglePlayPause() },
                onSeek = { viewModel.seekTo(it) }
            )
        }

        if (!recording.isProcessing && recording.summary.isNullOrBlank()) {
            item {
                SummaryPromptCard(
                    isOnline = isOnline,
                    hasTranscript = !recording.transcript.isNullOrBlank(),
                    onGenerateSummary = { viewModel.generateAiSummary() }
                )
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
                                text = "Internet Available",
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
                            onClick = { viewModel.generateAiSummary() },
                            enabled = isOnline,
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
                            onClick = { viewModel.retryAiProcessing() },
                            enabled = isOnline,
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
                    title = "Summary",
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

        // Action Items
        if (recording.actionItems != null) {
            item {
                BulletListCard(
                    title = "Action Items",
                    items = parseJsonArray(recording.actionItems)
                )
            }
        }

        // Transcript
        if (recording.transcript != null) {
            item {
                SectionCard(
                    title = "Full Transcript",
                    content = recording.transcript
                )
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
                    "Transcript is not available yet."
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isOnline) {
                    if (hasTranscript) {
                        "Tap below to generate the AI summary."
                    } else {
                        "Tap below to transcribe locally and generate the AI summary."
                    }
                } else {
                    "You are offline. The transcript stays local, and AI summary will be enabled when internet is available."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onGenerateSummary,
                enabled = isOnline
            ) {
                Text(if (hasTranscript) "Generate AI Summary" else "Transcribe and Summarize")
            }
        }
    }
}

@Composable
fun MetadataCard(recording: Recording) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDate(recording.timestamp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatDuration(recording.duration),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            items.forEach { item ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "\u2022",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = item,
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Audio Player",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Time display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(playbackState.currentPosition),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatTime(playbackState.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Seekbar
            Slider(
                value = playbackState.currentPosition.toFloat(),
                onValueChange = { onSeek(it.toInt()) },
                valueRange = 0f..playbackState.duration.toFloat().coerceAtLeast(1f),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Play/Pause button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(64.dp),
                    enabled = !playbackState.isLoading && playbackState.error == null
                ) {
                    if (playbackState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Error message
            if (playbackState.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = playbackState.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
