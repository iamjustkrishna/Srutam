package space.iamjustkrishna.srutam.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.positionChange
import space.iamjustkrishna.srutam.ui.components.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import space.iamjustkrishna.srutam.analytics.DailyNoteCount
import space.iamjustkrishna.srutam.analytics.UserActivityMetrics
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import space.iamjustkrishna.srutam.utils.AppPreferences
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.sin
import kotlin.math.cos
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import space.iamjustkrishna.srutam.utils.NetworkUtils
import space.iamjustkrishna.srutam.viewmodel.AudioFilesViewModel
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import space.iamjustkrishna.srutam.ui.components.*
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.InsightKind
import space.iamjustkrishna.srutam.data.InsightStatus
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import space.iamjustkrishna.srutam.player.PlaybackState
import space.iamjustkrishna.srutam.ui.components.CosmicBackground
import space.iamjustkrishna.srutam.ui.components.RootTab
import space.iamjustkrishna.srutam.ui.components.TabletSideNavRail
import space.iamjustkrishna.srutam.ui.theme.*
import space.iamjustkrishna.srutam.utils.AudioFileInfo
import space.iamjustkrishna.srutam.viewmodel.ThemeCluster
import kotlin.math.sin

enum class TabletDetailTab {
    SUMMARY,
    TRANSCRIPT,
    INSIGHTS
}

enum class TabletFeedFilter {
    ALL,
    PENDING
}

data class TranscriptSegment(
    val timestampMs: Int,
    val timestampString: String,
    val text: String
)

private fun parseTranscriptSegments(transcript: String?, durationMs: Long): List<TranscriptSegment> {
    if (transcript.isNullOrBlank()) return emptyList()

    val timestampRegex = Regex("""(?:\[)?(\d{1,2}):(\d{2})(?:\])?\s*(.*)""")
    val lines = transcript.lines().filter { it.isNotBlank() }
    val explicitSegments = mutableListOf<TranscriptSegment>()

    for (line in lines) {
        val match = timestampRegex.find(line.trim())
        if (match != null) {
            val mins = match.groupValues[1].toIntOrNull() ?: 0
            val secs = match.groupValues[2].toIntOrNull() ?: 0
            val text = match.groupValues[3].trim()
            val ms = (mins * 60 + secs) * 1000
            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
            if (text.isNotBlank()) {
                explicitSegments.add(TranscriptSegment(ms, timeStr, text))
            }
        }
    }

    if (explicitSegments.isNotEmpty()) {
        return explicitSegments
    }

    val sentences = transcript.split(Regex("""(?<=[.!?])\s+""")).filter { it.isNotBlank() }
    if (sentences.isEmpty()) {
        return listOf(TranscriptSegment(0, "00:00", transcript))
    }

    val totalMs = durationMs.coerceAtLeast(1000L)
    val stepMs = totalMs / sentences.size.coerceAtLeast(1)

    return sentences.mapIndexed { index, sentence ->
        val segMs = (index * stepMs).toInt()
        val totalSec = segMs / 1000
        val mins = totalSec / 60
        val secs = totalSec % 60
        val timeStr = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
        TranscriptSegment(segMs, timeStr, sentence.trim())
    }
}

private fun parseJsonArray(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val type = object : TypeToken<List<String>>() {}.type
        Gson().fromJson<List<String>>(json, type) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}

private fun formatHeaderTimestamp(timestamp: Long): String {
    val datePart = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    val timePart = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
    return "$datePart · $timePart"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TabletWorkspaceLayout(
    currentTab: RootTab,
    onTabSelected: (RootTab) -> Unit,
    onSettingsClick: () -> Unit,
    audioFiles: List<AudioFileInfo>,
    recordingsByPath: Map<String, Recording>,
    playbackState: PlaybackState,
    activeActions: List<InsightEntity> = emptyList(),
    allIdeas: List<InsightEntity> = emptyList(),
    allDecisions: List<InsightEntity> = emptyList(),
    themeClusters: List<ThemeCluster> = emptyList(),
    copilotMessages: List<GlobalChatMessage> = emptyList(),
    isRecording: Boolean = false,
    isPaused: Boolean = false,
    recordingElapsedMs: Long = 0L,
    onPlayFile: (AudioFileInfo) -> Unit = {},
    onPlayPause: () -> Unit = {},
    onSeek: (Int) -> Unit = {},
    onSpeedChange: (Float) -> Unit = {},
    onRecordingClick: (Long) -> Unit = {},
    onActionToggle: (InsightEntity) -> Unit = {},
    onNewNoteClick: () -> Unit = {},
    onStartRecording: () -> Unit = onNewNoteClick,
    onPauseToggle: () -> Unit = {},
    onFinishRecording: () -> Unit = {},
    onCancelRecording: () -> Unit = {},
    onSendCopilotQuery: (String) -> Unit = {},
    onReprocess: () -> Unit = {},
    onDeleteFile: (AudioFileInfo) -> Unit = {},
    onRenameFile: (AudioFileInfo, String) -> Unit = { _, _ -> },
    onShareFile: (AudioFileInfo) -> Unit = {},
    onProcessAI: (AudioFileInfo) -> Unit = {},
    viewModel: AudioFilesViewModel? = null,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isLargeTablet = screenWidthDp >= 900
    val isDark = LocalIsCosmicDark.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var internalCopilotMessages by remember {
        mutableStateOf(
            listOf(
                GlobalChatMessage(
                    text = "I can search across all your voice notes and answer any question about your recordings, meetings, and ideas.",
                    isUser = false
                )
            )
        )
    }
    var isCopilotLoading by remember { mutableStateOf(false) }

    val effectiveCopilotMessages = if (copilotMessages.isNotEmpty()) copilotMessages else internalCopilotMessages

    val handleSendCopilotQuery: (String) -> Unit = { queryText ->
        val trimmed = queryText.trim()
        if (trimmed.isNotBlank() && !isCopilotLoading) {
            if (!NetworkUtils.isInternetAvailable(context)) {
                internalCopilotMessages = internalCopilotMessages + GlobalChatMessage(text = trimmed, isUser = true) +
                        GlobalChatMessage(text = "An internet connection is required to query your voice notes.", isUser = false)
            } else {
                internalCopilotMessages = internalCopilotMessages + GlobalChatMessage(text = trimmed, isUser = true)
                if (viewModel != null) {
                    isCopilotLoading = true
                    coroutineScope.launch {
                        try {
                            val (answer, citedNotes) = viewModel.queryAllVoiceNotes(trimmed)
                            internalCopilotMessages = internalCopilotMessages + GlobalChatMessage(
                                text = answer,
                                isUser = false,
                                citedNotes = citedNotes
                            )
                        } catch (e: Exception) {
                            internalCopilotMessages = internalCopilotMessages + GlobalChatMessage(
                                text = "Sorry, I couldn't complete your request: ${e.message}",
                                isUser = false
                            )
                        } finally {
                            isCopilotLoading = false
                        }
                    }
                }
            }
            onSendCopilotQuery(trimmed)
        }
    }

    // Synthetic welcome note shown when no recordings exist yet (first launch)
    val welcomeAudioInfo = remember {
        AudioFileInfo(
            filePath = "srutam://welcome",
            fileName = "Welcome to Srutam.m4a",
            duration = 90_000L,
            timestamp = System.currentTimeMillis() - 86_400_000L,
            sizeBytes = 0L
        )
    }
    val welcomeRecordingData = remember {
        Recording(
            id = Long.MIN_VALUE,
            audioFilePath = "srutam://welcome",
            duration = 90_000L,
            name = "Welcome to Srutam ✦",
            summary = "Srutam is your AI-powered voice intelligence companion. It transcribes your voice notes in real-time, extracts key insights, action items, and decisions using on-device AI, and lets you search and query across all your notes with the built-in AI Copilot.",
            wiifm = "You'll never lose a brilliant idea or miss a follow-up again. Srutam turns your spoken thoughts into structured, searchable, AI-enhanced notes — privately on your device.",
            keyPoints = "[\"Record voice notes anywhere \u2014 in meetings, on walks, or brainstorming sessions\",\"Get automatic AI summaries, key points, action items, and decisions\",\"Search and query across all notes with Srutam AI Copilot\",\"Works with on-device transcription for full privacy\",\"Premium dark/light cosmic design with smooth animations\"]",
            isProcessing = false,
            aiStatus = RecordingAiStatus.READY
        )
    }
    val effectiveAudioFiles = if (audioFiles.isEmpty()) listOf(welcomeAudioInfo) else audioFiles
    val effectiveRecordingsByPath = if (audioFiles.isEmpty()) {
        mapOf("srutam://welcome" to welcomeRecordingData)
    } else {
        recordingsByPath
    }

    var selectedFilePath by remember {
        mutableStateOf<String?>(null)
    }
    var previousAudioFilesCount by remember { mutableIntStateOf(audioFiles.size) }
    LaunchedEffect(audioFiles) {
        if (audioFiles.size > previousAudioFilesCount && audioFiles.isNotEmpty()) {
            selectedFilePath = audioFiles.first().filePath
        }
        previousAudioFilesCount = audioFiles.size
    }
    val effectiveSelectedFilePath = selectedFilePath?.takeIf { path ->
        effectiveAudioFiles.any { it.filePath == path }
    } ?: effectiveAudioFiles.firstOrNull()?.filePath

    val selectedAudioFile = remember(effectiveSelectedFilePath, effectiveAudioFiles) {
        effectiveAudioFiles.firstOrNull { it.filePath == effectiveSelectedFilePath }
    }
    val selectedRecording = remember(selectedAudioFile, effectiveRecordingsByPath) {
        selectedAudioFile?.let { effectiveRecordingsByPath[it.filePath] }
    }
    val noteInsights = remember(selectedRecording, activeActions, allIdeas, allDecisions) {
        val recId = selectedRecording?.id
        if (recId != null && recId > 0) {
            (activeActions + allIdeas + allDecisions).filter { it.recordingId == recId }
        } else {
            emptyList()
        }
    }

    val bgModifier = if (isDark) {
        Modifier.background(CosmicVoidBackground)
    } else {
        Modifier.background(Color.White)
    }

    val sidebarWidth = if (screenWidthDp < 768) 240.dp else 280.dp

    Box(modifier = modifier.fillMaxSize().then(bgModifier)) {
        if (isDark) {
            CosmicBackground()
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // Unified Left Sidebar
            TabletUnifiedSidebar(
                currentTab = currentTab,
                onTabSelected = onTabSelected,
                onSettingsClick = onSettingsClick,
                audioFiles = effectiveAudioFiles,
                recordingsByPath = effectiveRecordingsByPath,
                selectedFilePath = selectedAudioFile?.filePath,
                onSelectFile = { audio ->
                    selectedFilePath = audio.filePath
                    if (currentTab != RootTab.NOTES) {
                        onTabSelected(RootTab.NOTES)
                    }
                },
                onProcessAI = onProcessAI,
                onProcessAll = {
                    if (viewModel != null) {
                        viewModel.processPendingOfflineRecordings(force = true) { count ->
                            if (count > 0) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Analyzing $count pending voice note${if (count > 1) "s" else ""} in background...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "All voice notes are already processed",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                },
                onNewNoteClick = onStartRecording,
                modifier = Modifier.width(sidebarWidth)
            )

            VerticalDivider(
                color = if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0),
                modifier = Modifier.fillMaxHeight().width(1.dp)
            )

            // Right Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                when (currentTab) {
                    RootTab.NOTES -> {
                        key(selectedAudioFile?.filePath) {
                            TabletExecutiveDetailWorkspace(
                                audioFile = selectedAudioFile,
                                recording = selectedRecording,
                                allAudioFiles = effectiveAudioFiles,
                                recordingsByPath = effectiveRecordingsByPath,
                                playbackState = playbackState,
                                noteInsights = noteInsights,
                                onPlayPause = onPlayPause,
                                onPlayFile = onPlayFile,
                                onSeek = onSeek,
                                onSpeedChange = onSpeedChange,
                                onSelectNote = { audio ->
                                    selectedFilePath = audio.filePath
                                },
                                onActionToggle = onActionToggle,
                                onReprocess = {
                                    selectedAudioFile?.let { onProcessAI(it) } ?: onReprocess()
                                },
                                onShare = { selectedAudioFile?.let { onShareFile(it) } },
                                onRename = { newName -> selectedAudioFile?.let { onRenameFile(it, newName) } },
                                onDelete = {
                                    selectedAudioFile?.let { onDeleteFile(it) }
                                    // Reset selection to first remaining file
                                    selectedFilePath = null
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    RootTab.ACTIONS -> {
                        val archivedCount by (viewModel?.archivedActionsCount?.collectAsState() ?: remember { mutableIntStateOf(0) })
                        val activityMetrics by (viewModel?.activityMetrics?.collectAsState() ?: remember { mutableStateOf(UserActivityMetrics()) })
                        TabletInsights3ColumnWorkspace(
                            activeActions = activeActions,
                            allIdeas = allIdeas,
                            allDecisions = allDecisions,
                            themeClusters = themeClusters,
                            isLargeTablet = isLargeTablet,
                            activityMetrics = activityMetrics,
                            onActionToggle = onActionToggle,
                            onRecordingClick = { recId ->
                                val targetAudio = effectiveAudioFiles.firstOrNull { audio ->
                                    effectiveRecordingsByPath[audio.filePath]?.id == recId
                                }
                                if (targetAudio != null) {
                                    selectedFilePath = targetAudio.filePath
                                }
                                onTabSelected(RootTab.NOTES)
                            },
                            onViewAllNotes = { onTabSelected(RootTab.NOTES) },
                            onArchiveConfirmed = { viewModel?.archiveCompletedActions() },
                            onRestoreArchived = { viewModel?.unarchiveAllActions() },
                            onDismissTheme = { clusterName -> viewModel?.dismissTheme(clusterName) },
                            archivedCount = archivedCount
                        )
                    }
                    RootTab.AI -> {
                        TabletCopilot3PanelWorkspace(
                            messages = effectiveCopilotMessages,
                            audioFiles = effectiveAudioFiles,
                            recordingsByPath = effectiveRecordingsByPath,
                            isLargeTablet = isLargeTablet,
                            isQueryLoading = isCopilotLoading,
                            onSendMessage = handleSendCopilotQuery,
                            onRecordingClick = { recId ->
                                val targetAudio = effectiveAudioFiles.firstOrNull { audio ->
                                    effectiveRecordingsByPath[audio.filePath]?.id == recId
                                }
                                if (targetAudio != null) {
                                    selectedFilePath = targetAudio.filePath
                                }
                                onTabSelected(RootTab.NOTES)
                            }
                        )
                    }
                }
            }
        }

        // Centered Floating Record Shutter (Smoothly glides to the right in AI tab to clear query field)
        if (!WindowInsets.isImeVisible) {
            val isAiTab = currentTab == RootTab.AI
            val shutterBias by animateFloatAsState(
                targetValue = if (isAiTab) 0.94f else 0.0f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                label = "shutter_horizontal_glide"
            )

            TabletFloatingRecordShutter(
                isRecording = isRecording,
                isPaused = isPaused,
                recordingElapsedMs = recordingElapsedMs,
                onStartRecording = onStartRecording,
                onPauseToggle = onPauseToggle,
                onFinishRecording = onFinishRecording,
                onCancelRecording = onCancelRecording,
                modifier = Modifier
                    .align(BiasAlignment(horizontalBias = shutterBias, verticalBias = 1.0f))
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
fun TabletUnifiedSidebar(
    currentTab: RootTab,
    onTabSelected: (RootTab) -> Unit,
    onSettingsClick: () -> Unit,
    audioFiles: List<AudioFileInfo>,
    recordingsByPath: Map<String, Recording>,
    selectedFilePath: String?,
    onSelectFile: (AudioFileInfo) -> Unit,
    onProcessAI: (AudioFileInfo) -> Unit = {},
    onProcessAll: () -> Unit = {},
    onNewNoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val sidebarBg = if (isDark) CosmicVoidCard else Color(0xFFFCFDFF)
    val textPrimary = if (isDark) TextOnDarkPrimary else Color(0xFF0F172A)
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary
    val context = LocalContext.current
    val autoAiState by AppPreferences.autoAiEnabledFlow.collectAsState()
    val isAutoAi = autoAiState ?: remember(context) { AppPreferences.isAutoAiEnabled(context) }
    val pendingCount = remember(audioFiles, recordingsByPath) {
        audioFiles.count { audio ->
            if (audio.filePath == "srutam://welcome") false
            else {
                val rec = recordingsByPath[audio.filePath]
                rec == null || rec.summary.isNullOrBlank() || rec.aiStatus == RecordingAiStatus.SUMMARY_PENDING_OFFLINE || rec.summary?.startsWith("This recording contains approximately") == true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(sidebarBg)
            .padding(top = 28.dp, start = 18.dp, end = 18.dp, bottom = 20.dp)
    ) {
        // "Srutam" Wordmark Header
        Text(
            text = "Srutam",
            fontFamily = FontFamily.Serif,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Navigation Tabs (Notes, Insights, AI, Settings)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TabletSidebarNavItem(
                icon = Icons.Default.Description,
                label = "Notes",
                isSelected = currentTab == RootTab.NOTES,
                onClick = { onTabSelected(RootTab.NOTES) }
            )
            TabletSidebarNavItem(
                icon = Icons.Default.AutoAwesome,
                label = "Insights",
                isSelected = currentTab == RootTab.ACTIONS,
                onClick = { onTabSelected(RootTab.ACTIONS) }
            )
            TabletSidebarNavItem(
                icon = Icons.Default.SmartToy,
                label = "AI",
                isSelected = currentTab == RootTab.AI,
                onClick = { onTabSelected(RootTab.AI) }
            )
            TabletSidebarNavItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                isSelected = false,
                onClick = onSettingsClick
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // "Recent" Section Header with "Process All" Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textSecondary
            )

            if (!isAutoAi) {
                val isEnabled = pendingCount > 0
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        !isEnabled -> if (isDark) Color(0xFF1E293B).copy(alpha = 0.35f) else Color(0xFFF1F5F9).copy(alpha = 0.6f)
                        isDark -> CosmicGlowBlue.copy(alpha = 0.2f)
                        else -> CobaltBlue.copy(alpha = 0.12f)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            !isEnabled -> if (isDark) Color(0xFF334155).copy(alpha = 0.3f) else Color(0xFFE2E8F0).copy(alpha = 0.5f)
                            isDark -> CosmicGlowBlue.copy(alpha = 0.5f)
                            else -> CobaltBlue.copy(alpha = 0.35f)
                        }
                    ),
                    modifier = Modifier
                        .alpha(if (isEnabled) 1.0f else 0.45f)
                        .clickable(enabled = isEnabled) {
                            onProcessAll()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = when {
                                !isEnabled -> textSecondary.copy(alpha = 0.5f)
                                isDark -> CosmicGlowBlue
                                else -> CobaltBlue
                            },
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = if (pendingCount > 0) "Process All ($pendingCount)" else "Process All",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                !isEnabled -> textSecondary.copy(alpha = 0.5f)
                                isDark -> CosmicGlowBlue
                                else -> CobaltBlue
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Recent Notes List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(audioFiles, key = { it.filePath }) { audio ->
                val recording = recordingsByPath[audio.filePath]
                val isSelected = audio.filePath == selectedFilePath
                val title = recording?.name?.takeIf { it.isNotBlank() } ?: audio.fileName.removeSuffix(".m4a")
                val dateText = formatHumanRelativeDate(audio.timestamp)
                val isRecentlySaved = remember(audio.timestamp) {
                    (System.currentTimeMillis() - audio.timestamp) < 15_000L
                }

                val itemBg = if (isSelected) {
                    if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.45f) else Color(0xFFEBF3FE)
                } else {
                    Color.Transparent
                }

                val borderModifier = if (isRecentlySaved && !isSelected) {
                    val infiniteTransition = rememberInfiniteTransition(label = "recent_pulse")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.35f,
                        targetValue = 0.9f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_alpha"
                    )
                    Modifier.border(
                        width = 1.dp,
                        color = (if (isDark) CosmicGlowBlue else CobaltBlue).copy(alpha = alpha),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }

                Surface(
                    onClick = { onSelectFile(audio) },
                    shape = RoundedCornerShape(12.dp),
                    color = itemBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(
                            fadeInSpec = tween(durationMillis = 400),
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                        .then(borderModifier)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = title,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        if (isDark) CosmicGlowBlue else CobaltBlue
                                    } else {
                                        textPrimary
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (isRecentlySaved) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background((if (isDark) CosmicGlowBlue else CobaltBlue).copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "NEW",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) CosmicGlowBlue else CobaltBlue
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = dateText,
                                fontSize = 11.5.sp,
                                color = textSecondary
                            )
                        }

                        // Right side: only icon to be clicked, perfectly aligned vertically centered
                        if (audio.filePath != "srutam://welcome") {
                            when {
                                recording?.isProcessing == true ||
                                recording?.aiStatus == RecordingAiStatus.TRANSCRIBING ||
                                recording?.aiStatus == RecordingAiStatus.SUMMARY_PROCESSING -> {
                                    Box(
                                        modifier = Modifier.size(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = if (isDark) CosmicGlowBlue else CobaltBlue
                                        )
                                    }
                                }
                                recording?.aiStatus == RecordingAiStatus.ERROR -> {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .clickable { onProcessAI(audio) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Retry AI",
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                recording == null || recording.summary.isNullOrBlank() -> {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .clickable { onProcessAI(audio) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "Process with AI",
                                            tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .clickable { onProcessAI(audio) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Summarized (tap to reprocess)",
                                            tint = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabletSidebarNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = LocalIsCosmicDark.current
    val pillBg = if (isSelected) {
        if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFEBF3FE)
    } else {
        Color.Transparent
    }
    val contentColor = if (isSelected) {
        if (isDark) CosmicGlowBlue else CobaltBlue
    } else {
        if (isDark) TextOnDarkSecondary else TextSecondary
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = pillBg,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
fun TabletExecutiveDetailWorkspace(
    audioFile: AudioFileInfo?,
    recording: Recording?,
    allAudioFiles: List<AudioFileInfo>,
    recordingsByPath: Map<String, Recording>,
    playbackState: PlaybackState,
    noteInsights: List<InsightEntity>,
    onPlayPause: () -> Unit,
    onPlayFile: (AudioFileInfo) -> Unit = {},
    onSeek: (Int) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSelectNote: (AudioFileInfo) -> Unit,
    onActionToggle: (InsightEntity) -> Unit,
    onOpenFullNote: () -> Unit = {},
    onReprocess: () -> Unit = {},
    onShare: () -> Unit = {},
    onRename: (String) -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val textPrimary = if (isDark) TextOnDarkPrimary else Color(0xFF0F172A)
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary

    if (audioFile == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(if (isDark) CosmicVoidBackground else Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Select a note to inspect details and AI synthesis",
                fontSize = 15.sp,
                color = textSecondary
            )
        }
        return
    }

    val title = recording?.name?.takeIf { it.isNotBlank() } ?: audioFile.fileName.removeSuffix(".m4a")
    val isSummarized = !recording?.summary.isNullOrBlank()
    val formattedDateTime = formatHeaderTimestamp(audioFile.timestamp)
    val currentPosition = playbackState.currentPosition
    val totalDuration = audioFile.duration.toInt().coerceAtLeast(1)
    val progress = (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    val isAudioPlaying = playbackState.isPlaying && playbackState.currentFilePath == audioFile.filePath

    var detailTab by remember { mutableStateOf(TabletDetailTab.SUMMARY) }
    var transcriptSearch by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    val isWelcomeNote = audioFile?.filePath == "srutam://welcome"
    val completedActionIds = remember { mutableStateMapOf<String, Boolean>() }

    val legacyActions = remember(recording?.actionItems) {
        parseJsonArray(recording?.actionItems)
    }
    val legacyPoints = remember(recording?.keyPoints) {
        parseJsonArray(recording?.keyPoints)
    }

    val nextSteps = remember(noteInsights, legacyActions) {
        val actionsFromEntity = noteInsights.filter { it.kind == InsightKind.ACTION }
        if (actionsFromEntity.isNotEmpty()) actionsFromEntity
        else legacyActions.mapIndexed { idx, text ->
            InsightEntity(id = (idx + 1).toString(), recordingId = recording?.id ?: 0L, kind = InsightKind.ACTION, text = text)
        }
    }

    val ideas = remember(noteInsights, legacyPoints) {
        val ideasFromEntity = noteInsights.filter { it.kind == InsightKind.IDEA }
        if (ideasFromEntity.isNotEmpty()) ideasFromEntity
        else legacyPoints.mapIndexed { idx, text ->
            InsightEntity(id = (idx + 100).toString(), recordingId = recording?.id ?: 0L, kind = InsightKind.IDEA, text = text)
        }
    }

    val decisions = remember(noteInsights) {
        val decisionsFromEntity = noteInsights.filter { it.kind == InsightKind.DECISION }
        if (decisionsFromEntity.isNotEmpty()) decisionsFromEntity
        else if (isWelcomeNote) listOf(
            InsightEntity(id = "200", recordingId = recording?.id ?: 0L, kind = InsightKind.DECISION, text = "Use on-device AI transcription for total privacy")
        ) else emptyList()
    }

    val transcriptSegments = remember(recording?.transcript, audioFile.duration) {
        parseTranscriptSegments(recording?.transcript, audioFile.duration)
    }

    val configuration = LocalConfiguration.current
    val isCompactWidth = configuration.screenWidthDp < 768
    val horizontalPadding = if (isCompactWidth) 18.dp else 32.dp

    // Rename dialog
    if (showRenameDialog) {
        RenameDialog(
            currentName = title,
            onRename = {
                onRename(it)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            recordingName = title,
            onConfirm = {
                onDelete()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    // Audio metadata & location info dialog
    if (showInfoDialog) {
        AudioInfoDialog(
            displayName = title,
            audioFile = audioFile,
            recording = recording,
            onDismiss = { showInfoDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) CosmicVoidBackground else Color.White)
            .padding(horizontal = horizontalPadding, vertical = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Header Row: File Info, Share audio, Rename, Delete (no back button in tablet two-pane mode)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showInfoDialog = true }) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "File info",
                    tint = textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (!isWelcomeNote) {
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share audio",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { showRenameDialog = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Rename note",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete note",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Note Title, Subtitle Date, and Action Button next to audio name
        var isTitleExpanded by remember(title) { mutableStateOf(false) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize()
            ) {
                Text(
                    text = title,
                    fontSize = if (isCompactWidth) 20.sp else 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = if (isTitleExpanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { isTitleExpanded = !isTitleExpanded }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedDateTime,
                    fontSize = 13.sp,
                    color = textSecondary
                )
            }

            if (!isWelcomeNote) {
                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier.wrapContentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        recording?.isProcessing == true ||
                        recording?.aiStatus == RecordingAiStatus.TRANSCRIBING ||
                        recording?.aiStatus == RecordingAiStatus.SUMMARY_PROCESSING -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFEBF3FE),
                                border = BorderStroke(1.dp, if (isDark) CosmicGlowBlue else CobaltBlue)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.5.dp,
                                        color = if (isDark) CosmicGlowBlue else CobaltBlue
                                    )
                                    Text(
                                        text = if (recording?.aiStatus == RecordingAiStatus.TRANSCRIBING) "Transcribing..." else "Analyzing...",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) CosmicGlowBlue else CobaltBlue
                                    )
                                }
                            }
                        }
                        recording?.aiStatus == RecordingAiStatus.ERROR -> {
                            Surface(
                                onClick = onReprocess,
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFEE2E2),
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Retry AI",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Retry AI",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                        !isSummarized -> {
                            Surface(
                                onClick = onReprocess,
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) CosmicGlowBlue else CobaltBlue,
                                shadowElevation = 2.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Process with AI",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Process with AI",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        else -> {
                            Surface(
                                onClick = onReprocess,
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFEBF3FE),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF3B82F6).copy(alpha = 0.4f) else Color(0xFFD6E4FC))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Summarized ↺",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) CosmicGlowBlue else CobaltBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Audio Player Bar (smart play: loads file if not currently active)
        TabletPlayerBar(
            isPlaying = isAudioPlaying,
            currentPosition = currentPosition,
            totalDuration = totalDuration,
            progress = progress,
            playbackSpeed = playbackState.speed,
            onPlayPause = {
                when {
                    isWelcomeNote -> { /* welcome note has no audio */ }
                    playbackState.currentFilePath != audioFile.filePath -> onPlayFile(audioFile)
                    else -> onPlayPause()
                }
            },
            onSeek = onSeek,
            onSpeedChange = onSpeedChange
        )

        Spacer(modifier = Modifier.height(22.dp))

        // 4. Segmented Tab Switcher: [ ✦ Summary ] [ Transcript ] [ Insights ]
        TabletSegmentedTabSwitcher(
            selectedTab = detailTab,
            onTabSelected = { detailTab = it }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 5. Tab Content
        when (detailTab) {
            TabletDetailTab.SUMMARY -> {
                when {
                    isWelcomeNote -> {
                        TabletExecutiveSummaryView(
                            summaryText = recording?.summary.orEmpty(),
                            wiifm = recording?.wiifm,
                            keyPoints = legacyPoints,
                            onReprocess = null,
                            nextSteps = nextSteps,
                            ideas = ideas,
                            decisions = decisions,
                            completedActionIds = completedActionIds,
                            onToggleAction = { insight ->
                                val key = insight.id.toString()
                                completedActionIds[key] = !(completedActionIds[key] ?: false)
                                onActionToggle(insight)
                            }
                        )
                    }
                    recording?.isProcessing == true ||
                    recording?.aiStatus == RecordingAiStatus.TRANSCRIBING ||
                    recording?.aiStatus == RecordingAiStatus.SUMMARY_PROCESSING -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) CosmicVoidCard else Color.White,
                            border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    strokeWidth = 3.dp,
                                    color = if (isDark) CosmicGlowBlue else CobaltBlue
                                )
                                Text(
                                    text = if (recording?.aiStatus == RecordingAiStatus.TRANSCRIBING) "Transcribing audio locally..." else "Analyzing with AI...",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Text(
                                    text = "Srutam is processing speech and synthesizing insights on your device.",
                                    fontSize = 13.sp,
                                    color = textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    recording?.aiStatus == RecordingAiStatus.ERROR -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) Color(0xFF261214) else Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF7F1D1D) else Color(0xFFFECACA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "Processing Incomplete",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                                )
                                Text(
                                    text = recording?.processingError ?: "Could not complete speech recognition or summary. Tap below to re-process.",
                                    fontSize = 13.sp,
                                    color = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C),
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = onReprocess,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Retry AI Processing", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    recording == null || recording.summary.isNullOrBlank() -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) CosmicVoidCard else Color.White,
                            border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Ready for AI Insights",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Text(
                                    text = "This note has not been summarized yet. Tap the button below to transcribe audio, generate executive summary, and extract action items.",
                                    fontSize = 13.sp,
                                    color = textSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = onReprocess,
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) CosmicGlowBlue else CobaltBlue)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Process Note with AI", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    else -> {
                        TabletExecutiveSummaryView(
                            summaryText = recording.summary.orEmpty(),
                            wiifm = recording.wiifm,
                            keyPoints = legacyPoints,
                            onReprocess = onReprocess,
                            nextSteps = nextSteps,
                            ideas = ideas,
                            decisions = decisions,
                            completedActionIds = completedActionIds,
                            onToggleAction = { insight ->
                                val key = insight.id.toString()
                                completedActionIds[key] = !(completedActionIds[key] ?: false)
                                onActionToggle(insight)
                            }
                        )
                    }
                }
            }
            TabletDetailTab.TRANSCRIPT -> {
                TabletFullTranscriptView(
                    transcriptSegments = transcriptSegments,
                    transcriptSearch = transcriptSearch,
                    onTranscriptSearchChange = { transcriptSearch = it },
                    onSeek = onSeek
                )
            }
            TabletDetailTab.INSIGHTS -> {
                TabletFullInsightsView(
                    nextSteps = nextSteps,
                    ideas = ideas,
                    decisions = decisions,
                    completedActionIds = completedActionIds,
                    onToggleAction = { insight ->
                        val key = insight.id.toString()
                        completedActionIds[key] = !(completedActionIds[key] ?: false)
                        onActionToggle(insight)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(76.dp))
    }
}

@Composable
fun TabletFloatingRecordShutter(
    isRecording: Boolean,
    isPaused: Boolean,
    recordingElapsedMs: Long,
    onStartRecording: () -> Unit,
    onPauseToggle: () -> Unit,
    onFinishRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isDark = LocalIsCosmicDark.current

    var recordingMode by remember { mutableStateOf(RecordingMode.IDLE) }
    var dragYOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isRecording) {
        if (!isRecording) {
            recordingMode = RecordingMode.IDLE
            dragYOffset = 0f
        }
    }

    val isExpanded = recordingMode == RecordingMode.LOCKED || (isRecording && recordingMode != RecordingMode.HOLDING)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        // Floating Slide-to-Lock Target Pill elevated comfortably above the shutter button
        androidx.compose.animation.AnimatedVisibility(
            visible = recordingMode == RecordingMode.HOLDING,
            enter = fadeIn(animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) +
                    slideInVertically(
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                    ) { it / 2 },
            exit = fadeOut(animationSpec = tween(150)) +
                    slideOutVertically(animationSpec = tween(150)) { it / 2 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-115).dp)
        ) {
            SlideToLockIndicator(dragYOffset = dragYOffset)
        }

        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                (fadeIn(animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) +
                        expandHorizontally(animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))) togetherWith
                        (fadeOut(animationSpec = tween(120)) +
                                shrinkHorizontally(animationSpec = tween(120)))
            },
            label = "tablet_shutter_morph"
        ) { activeRecording ->
            if (activeRecording) {
                // Expanded Studio Pill during active recording
                Surface(
                    shape = CircleShape,
                    color = if (isDark) CosmicVoidCard else Color.White,
                    border = BorderStroke(
                        1.5.dp,
                        if (isPaused) {
                            if (isDark) Color(0xFF78350F) else Color(0xFFFDE68A)
                        } else {
                            if (isDark) Color(0xFF991B1B) else Color(0xFFFECACA)
                        }
                    ),
                    shadowElevation = if (isDark) 0.dp else 4.dp,
                    modifier = Modifier.height(56.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Trash / Cancel Button
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                recordingMode = RecordingMode.IDLE
                                onCancelRecording()
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF451214) else Color(0xFFFEE2E2))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Cancel Recording",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Pulsing Dot + Live Elapsed Time
                        val infiniteTransition = rememberInfiniteTransition(label = "tablet_pulse_dot")
                        val dotAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, easing = EaseInOut),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "dot_alpha"
                        )
                        val minutes = (recordingElapsedMs / 1000) / 60
                        val seconds = (recordingElapsedMs / 1000) % 60
                        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .scale(if (isPaused) 1f else dotAlpha)
                                    .clip(CircleShape)
                                    .background(if (isPaused) Color(0xFFF59E0B) else Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formattedTime,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isPaused) Color(0xFFF59E0B) else (if (isDark) Color.White else Color(0xFF0F172A))
                            )
                        }

                        // Pause / Resume Button
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPauseToggle()
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "Resume" else "Pause",
                                tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Red Square Stop & Save Shutter Button
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    recordingMode = RecordingMode.IDLE
                                    onFinishRecording()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White)
                            )
                        }
                    }
                }
            } else {
                // Idle or Holding State: 56dp Circular Floating Shutter matching mobile studio with hold-to-record
                var isPressed by remember { mutableStateOf(false) }
                val recordScale by animateFloatAsState(
                    targetValue = when {
                        recordingMode == RecordingMode.HOLDING -> 1.14f
                        isPressed -> 0.92f
                        else -> 1.0f
                    },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "tablet_record_spring_scale"
                )

                // Smooth animated shadow matching the circular UI geometry
                val shadowElevation by animateDpAsState(
                    targetValue = when {
                        recordingMode == RecordingMode.HOLDING -> 12.dp
                        isPressed -> 2.dp
                        else -> 6.dp
                    },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "tablet_record_shadow"
                )

                // Expanding Radiant Sonic Halo while holding
                val haloTransition = rememberInfiniteTransition(label = "tablet_shutter_halo")
                val haloScale by haloTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.35f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "tablet_halo_scale"
                )
                val haloAlpha by haloTransition.animateFloat(
                    initialValue = 0.45f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "tablet_halo_alpha"
                )

                if (recordingMode == RecordingMode.HOLDING) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .scale(haloScale)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = haloAlpha))
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF3B1519) else Color(0xFFFECACA),
                    border = BorderStroke(
                        1.dp,
                        if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.6f) else Color(0xFFFCA5A5).copy(alpha = 0.5f)
                    ),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .size(56.dp)
                        .scale(recordScale)
                        .shadow(
                            elevation = shadowElevation,
                            shape = CircleShape,
                            clip = false,
                            spotColor = Color(0xFFEF4444).copy(alpha = 0.45f),
                            ambientColor = Color(0xFFEF4444).copy(alpha = 0.25f)
                        )
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                down.consume()
                                isPressed = true
                                val startTime = System.currentTimeMillis()
                                var dragX = 0f
                                var dragY = 0f
                                dragYOffset = 0f
                                recordingMode = RecordingMode.HOLDING
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onStartRecording()

                                var isTouching = true
                                while (isTouching) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull()

                                    if (change != null && change.pressed) {
                                        val posDelta = change.positionChange()
                                        dragX += posDelta.x
                                        dragY += posDelta.y
                                        dragYOffset = dragY
                                        change.consume()

                                        // 1. WhatsApp Slide-to-Lock: drag up >= 60dp (~160px)
                                        if (dragY <= -160f) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            recordingMode = RecordingMode.LOCKED
                                            dragYOffset = 0f
                                            isTouching = false
                                            isPressed = false
                                        }
                                        // 2. Slide left past -140px to cancel
                                        else if (dragX <= -140f) {
                                            onCancelRecording()
                                            recordingMode = RecordingMode.IDLE
                                            dragYOffset = 0f
                                            isTouching = false
                                            isPressed = false
                                        }
                                    } else {
                                        isTouching = false
                                        isPressed = false
                                        dragYOffset = 0f
                                        val duration = System.currentTimeMillis() - startTime
                                        if (duration < 350) {
                                            // Quick tap -> lock into active recording
                                            recordingMode = RecordingMode.LOCKED
                                        } else {
                                            // Hold released without locking -> stop recording & open save dialog
                                            recordingMode = RecordingMode.IDLE
                                            onFinishRecording()
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Inner concentric crimson ring
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFEF4444),
                                            Color(0xFFDC2626),
                                            Color(0xFFB91C1C)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Central concentric core dot
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF991B1B))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabletPlayerBar(
    isPlaying: Boolean,
    currentPosition: Int,
    totalDuration: Int,
    progress: Float,
    playbackSpeed: Float,
    onPlayPause: () -> Unit,
    onSeek: (Int) -> Unit,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val textPrimary = if (isDark) TextOnDarkPrimary else Color(0xFF0F172A)
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Play / Pause Button
            FilledIconButton(
                onClick = onPlayPause,
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isDark) CosmicGlowBlue else CobaltBlue
                ),
                modifier = Modifier.size(46.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Waveform scrubber canvas
            Column(modifier = Modifier.weight(1f)) {
                DetailWaveformCanvas(
                    progress = progress,
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .pointerInput(totalDuration) {
                            detectTapGestures { offset ->
                                val clickProgress = (offset.x / size.width).coerceIn(0f, 1f)
                                onSeek((clickProgress * totalDuration).toInt())
                            }
                        }
                        .pointerInput(totalDuration) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                val dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                                onSeek((dragProgress * totalDuration).toInt())
                            }
                        }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                    Text(
                        text = formatTime(totalDuration),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Secondary controls: (10) rewind, 1.0x speed pill, (10) forward
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onSeek((currentPosition - 10000).coerceAtLeast(0)) },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Replay10,
                    contentDescription = "Rewind 10s",
                    tint = textSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            // Speed Pill
            val currentSpeedLabel = if (playbackSpeed == 1.0f) "1.0x" else "${playbackSpeed}x"
            Surface(
                onClick = {
                    val next = when (playbackSpeed) {
                        1.0f -> 1.25f
                        1.25f -> 1.5f
                        1.5f -> 2.0f
                        2.0f -> 0.75f
                        else -> 1.0f
                    }
                    onSpeedChange(next)
                },
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0))
            ) {
                Text(
                    text = currentSpeedLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            IconButton(
                onClick = { onSeek((currentPosition + 10000).coerceAtMost(totalDuration)) },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Forward10,
                    contentDescription = "Forward 10s",
                    tint = textSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun TabletSegmentedTabSwitcher(
    selectedTab: TabletDetailTab,
    onTabSelected: (TabletDetailTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val barBg = if (isDark) CosmicVoidCard else Color(0xFFF1F5F9)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = barBg,
        border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabletDetailTabPill(
                icon = Icons.Default.AutoAwesome,
                label = "Summary",
                isSelected = selectedTab == TabletDetailTab.SUMMARY,
                onClick = { onTabSelected(TabletDetailTab.SUMMARY) },
                modifier = Modifier.weight(1f)
            )
            TabletDetailTabPill(
                icon = Icons.Default.Description,
                label = "Transcript",
                isSelected = selectedTab == TabletDetailTab.TRANSCRIPT,
                onClick = { onTabSelected(TabletDetailTab.TRANSCRIPT) },
                modifier = Modifier.weight(1f)
            )
            TabletDetailTabPill(
                icon = Icons.Default.Lightbulb,
                label = "Insights",
                isSelected = selectedTab == TabletDetailTab.INSIGHTS,
                onClick = { onTabSelected(TabletDetailTab.INSIGHTS) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TabletDetailTabPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val pillBg = if (isSelected) {
        if (isDark) Color(0xFF1E293B) else Color.White
    } else {
        Color.Transparent
    }
    val contentColor = if (isSelected) {
        if (isDark) CosmicGlowBlue else CobaltBlue
    } else {
        if (isDark) TextOnDarkSecondary else TextSecondary
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = pillBg,
        shadowElevation = if (isSelected && !isDark) 1.dp else 0.dp,
        modifier = modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
fun TabletExecutiveSummaryView(
    summaryText: String,
    wiifm: String? = null,
    keyPoints: List<String> = emptyList(),
    onReprocess: (() -> Unit)? = null,
    nextSteps: List<InsightEntity>,
    ideas: List<InsightEntity>,
    decisions: List<InsightEntity>,
    completedActionIds: Map<String, Boolean>,
    onToggleAction: (InsightEntity) -> Unit
) {
    val isDark = LocalIsCosmicDark.current
    val textPrimary = if (isDark) TextOnDarkPrimary else Color(0xFF0F172A)
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Summary Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) CosmicVoidCard else Color.White,
            border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Summary",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = summaryText,
                    fontSize = 13.5.sp,
                    color = textPrimary.copy(alpha = 0.85f),
                    lineHeight = 22.sp
                )
            }
        }

        // 2. What's In It For Me card
        if (!wiifm.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) CosmicVoidCard else Color.White,
                border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiObjects,
                            contentDescription = null,
                            tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "What's In It For Me",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(
                        color = if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0),
                        thickness = 0.5.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = wiifm,
                        fontSize = 13.5.sp,
                        color = textPrimary.copy(alpha = 0.85f),
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // 3. Key Points card
        if (keyPoints.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) CosmicVoidCard else Color.White,
                border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FormatListBulleted,
                            contentDescription = null,
                            tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Key Points",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(
                        color = if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0),
                        thickness = 0.5.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        keyPoints.forEach { point ->
                            val clean = point.removePrefix("[ ] ").removePrefix("- ").removePrefix("* ").trim()
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = "\u2022",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) CosmicGlowBlue else CobaltBlue,
                                    modifier = Modifier.padding(end = 8.dp, top = 1.dp)
                                )
                                Text(
                                    text = clean,
                                    fontSize = 13.5.sp,
                                    color = textPrimary.copy(alpha = 0.85f),
                                    lineHeight = 20.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Re-process button
        if (onReprocess != null) {
            OutlinedButton(
                onClick = onReprocess,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = if (isDark) CosmicGlowBlue else CobaltBlue
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Re-process Note with AI",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) CosmicGlowBlue else CobaltBlue
                )
            }
        }
    }
}

@Composable
fun TabletFullTranscriptView(
    transcriptSegments: List<TranscriptSegment>,
    transcriptSearch: String,
    onTranscriptSearchChange: (String) -> Unit,
    onSeek: (Int) -> Unit
) {
    val isDark = LocalIsCosmicDark.current
    val textPrimary = if (isDark) TextOnDarkPrimary else Color(0xFF0F172A)
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) CosmicVoidCard else Color.White,
        border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Search Bar
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) CosmicVoidBackground else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (transcriptSearch.isEmpty()) {
                            Text(
                                text = "Search spoken words or phrases in transcript...",
                                fontSize = 13.sp,
                                color = textSecondary
                            )
                        }
                        BasicTextField(
                            value = transcriptSearch,
                            onValueChange = onTranscriptSearchChange,
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 13.sp, color = textPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (transcriptSegments.isEmpty()) {
                Text(
                    text = "No transcript yet. Record a voice note to get started.",
                    fontSize = 13.sp,
                    color = textSecondary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    transcriptSegments.forEach { segment ->
                        // Build annotated text with search highlights
                        val annotatedText = if (transcriptSearch.isBlank()) {
                            buildAnnotatedString { append(segment.text) }
                        } else {
                            buildAnnotatedString {
                                val lowerText = segment.text.lowercase()
                                val lowerQuery = transcriptSearch.lowercase()
                                var startIdx = 0
                                while (startIdx < segment.text.length) {
                                    val matchIdx = lowerText.indexOf(lowerQuery, startIdx)
                                    if (matchIdx == -1) {
                                        append(segment.text.substring(startIdx))
                                        break
                                    }
                                    append(segment.text.substring(startIdx, matchIdx))
                                    withStyle(SpanStyle(
                                        background = if (isDark) Color(0xFFB45309) else Color(0xFFFDE68A),
                                        color = if (isDark) Color.White else Color(0xFF78350F)
                                    )) {
                                        append(segment.text.substring(matchIdx, matchIdx + transcriptSearch.length))
                                    }
                                    startIdx = matchIdx + transcriptSearch.length
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSeek(segment.timestampMs) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = segment.timestampString,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) CosmicGlowBlue else CobaltBlue,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = annotatedText,
                                fontSize = 13.5.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabletFullInsightsView(
    nextSteps: List<InsightEntity>,
    ideas: List<InsightEntity>,
    decisions: List<InsightEntity>,
    completedActionIds: Map<String, Boolean>,
    onToggleAction: (InsightEntity) -> Unit
) {
    val isDark = LocalIsCosmicDark.current
    val textPrimary = if (isDark) TextOnDarkPrimary else Color(0xFF0F172A)
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary

    if (nextSteps.isEmpty() && ideas.isEmpty() && decisions.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) CosmicVoidCard else Color.White,
            border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "No Insights Extracted Yet",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    text = "Process this note with AI to automatically extract action items, ideas, and key decisions.",
                    fontSize = 13.sp,
                    color = textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val configuration = LocalConfiguration.current
    val isWideLayout = configuration.screenWidthDp >= 840 || (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && configuration.screenWidthDp >= 600)

    val nextStepsCardContent: @Composable (Modifier) -> Unit = { mod ->
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF101B30) else Color(0xFFF4F8FE),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF1E3A8A) else Color(0xFFD6E4FC)),
            modifier = mod
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Next Steps (${nextSteps.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) CosmicGlowBlue else CobaltBlue
                )
                Spacer(modifier = Modifier.height(12.dp))
                nextSteps.forEach { action ->
                    val isChecked = completedActionIds[action.id.toString()] ?: (action.status == InsightStatus.COMPLETED)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onToggleAction(action) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = if (isDark) CosmicGlowBlue else CobaltBlue
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = action.text,
                                fontSize = 13.sp,
                                color = if (isChecked) textSecondary else textPrimary,
                                textDecoration = if (isChecked) TextDecoration.LineThrough else null
                            )
                            action.rationale?.let {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = it, fontSize = 11.sp, color = textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }

    val ideasCardContent: @Composable (Modifier) -> Unit = { mod ->
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF261D0B) else Color(0xFFFFFBEB),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF78350F) else Color(0xFFFEF3C7)),
            modifier = mod
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Key Ideas (${ideas.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                )
                Spacer(modifier = Modifier.height(12.dp))
                ideas.forEach { idea ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "◆", color = Color(0xFFD97706), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = idea.text, fontSize = 13.sp, color = textPrimary)
                            idea.rationale?.let {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = it, fontSize = 11.sp, color = textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }

    val decisionsCardContent: @Composable (Modifier) -> Unit = { mod ->
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF0D2319) else Color(0xFFF0FDF4),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF14532D) else Color(0xFFDCFCE7)),
            modifier = mod
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Key Decisions (${decisions.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                )
                Spacer(modifier = Modifier.height(12.dp))
                decisions.forEach { decision ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "◎", color = Color(0xFF16A34A), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = decision.text, fontSize = 13.sp, color = textPrimary)
                            decision.rationale?.let {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = it, fontSize = 11.sp, color = textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (isWideLayout) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            nextStepsCardContent(Modifier.weight(1f))
            ideasCardContent(Modifier.weight(1f))
            decisionsCardContent(Modifier.weight(1f))
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            nextStepsCardContent(Modifier.fillMaxWidth())
            ideasCardContent(Modifier.fillMaxWidth())
            decisionsCardContent(Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun TabletNextStepsCard(
    nextSteps: List<InsightEntity>,
    completedActionIds: Map<String, Boolean>,
    onToggleAction: (InsightEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val textPrimary = if (isDark) TextOnDarkPrimary else Color(0xFF0F172A)
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF101B30) else Color(0xFFF4F8FE),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF1E3A8A) else Color(0xFFD6E4FC)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Next Steps",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) CosmicGlowBlue else CobaltBlue
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = nextSteps.size.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) CosmicGlowBlue else CobaltBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                nextSteps.take(4).forEach { action ->
                    val isChecked = completedActionIds[action.id.toString()] ?: (action.status == InsightStatus.COMPLETED)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onToggleAction(action) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isChecked) (if (isDark) CosmicGlowBlue else CobaltBlue) else Color.Transparent,
                            border = BorderStroke(
                                1.5.dp,
                                if (isChecked) (if (isDark) CosmicGlowBlue else CobaltBlue)
                                else (if (isDark) Color(0xFF475569) else Color(0xFF94A3B8))
                            ),
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(14.dp)
                        ) {
                            if (isChecked) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = action.text,
                            fontSize = 12.sp,
                            color = if (isChecked) textSecondary else textPrimary,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Add next step",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) CosmicGlowBlue else CobaltBlue
                )
            }
        }
    }
}

@Composable
fun TabletKeyIdeasCard(
    ideas: List<InsightEntity>,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val textPrimary = if (isDark) TextOnDarkPrimary else Color(0xFF0F172A)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF261D0B) else Color(0xFFFFFBEB),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF78350F) else Color(0xFFFEF3C7)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "◆",
                    color = Color(0xFFD97706),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Key Ideas",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF78350F) else Color(0xFFFEF3C7))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = ideas.size.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ideas.take(4).forEach { idea ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "◆",
                            color = Color(0xFFD97706),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = idea.text,
                            fontSize = 12.sp,
                            color = textPrimary,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TabletKeyDecisionsCard(
    decisions: List<InsightEntity>,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val textPrimary = if (isDark) TextOnDarkPrimary else Color(0xFF0F172A)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF0D2319) else Color(0xFFF0FDF4),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF14532D) else Color(0xFFDCFCE7)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "◎",
                    color = Color(0xFF16A34A),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Key Decisions",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF14532D) else Color(0xFFDCFCE7))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = decisions.size.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                decisions.take(4).forEach { decision ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "◎",
                            color = Color(0xFF16A34A),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = decision.text,
                            fontSize = 12.sp,
                            color = textPrimary,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun TabletTopSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val barBg = if (isDark) CosmicVoidCard else Color.White
    val barBorder = if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) TextOnDarkPrimary else TextPrimary
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        color = barBg,
        border = BorderStroke(1.dp, barBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Input Pill
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = RoundedCornerShape(21.dp),
                color = if (isDark) CosmicVoidBackground else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search notes, transcripts, ideas...",
                                fontSize = 14.sp,
                                color = textSecondary
                            )
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = textPrimary,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Settings Action
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Profile Avatar Indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun TabletNotes3PanelWorkspace(
    audioFiles: List<AudioFileInfo>,
    recordingsByPath: Map<String, Recording>,
    playbackState: PlaybackState,
    allInsights: List<InsightEntity>,
    isLargeTablet: Boolean,
    searchQuery: String,
    onPlayFile: (AudioFileInfo) -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Int) -> Unit,
    onRecordingClick: (Long) -> Unit,
    onActionToggle: (InsightEntity) -> Unit,
    onNewNoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilePath by remember(audioFiles) {
        mutableStateOf(audioFiles.firstOrNull()?.filePath)
    }
    var detailTab by remember { mutableStateOf(TabletDetailTab.SUMMARY) }
    var filterMode by remember { mutableStateOf(TabletFeedFilter.ALL) }

    val filteredAudioFiles = remember(audioFiles, recordingsByPath, filterMode, searchQuery) {
        audioFiles.filter { audio ->
            val matchesFilter = when (filterMode) {
                TabletFeedFilter.ALL -> true
                TabletFeedFilter.PENDING -> {
                    val rec = recordingsByPath[audio.filePath]
                    rec?.summary.isNullOrBlank()
                }
            }
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                val rec = recordingsByPath[audio.filePath]
                audio.fileName.contains(searchQuery, ignoreCase = true) ||
                    rec?.name?.contains(searchQuery, ignoreCase = true) == true ||
                    rec?.summary?.contains(searchQuery, ignoreCase = true) == true ||
                    rec?.transcript?.contains(searchQuery, ignoreCase = true) == true
            }
            matchesFilter && matchesSearch
        }
    }

    val selectedAudioFile = remember(selectedFilePath, audioFiles) {
        audioFiles.firstOrNull { it.filePath == selectedFilePath } ?: audioFiles.firstOrNull()
    }
    val selectedRecording = remember(selectedAudioFile, recordingsByPath) {
        selectedAudioFile?.let { recordingsByPath[it.filePath] }
    }
    val noteInsights = remember(selectedRecording, allInsights) {
        val recId = selectedRecording?.id
        if (recId != null && recId > 0) {
            allInsights.filter { it.recordingId == recId }
        } else {
            emptyList()
        }
    }

    Row(modifier = modifier.fillMaxSize()) {
        // Panel 1: Notes List Pane
        TabletNotesListPane(
            audioFiles = filteredAudioFiles,
            recordingsByPath = recordingsByPath,
            selectedFilePath = selectedAudioFile?.filePath,
            playbackState = playbackState,
            filterMode = filterMode,
            allNotesCount = audioFiles.size,
            pendingCount = audioFiles.count { recordingsByPath[it.filePath]?.summary.isNullOrBlank() },
            onFilterChange = { filterMode = it },
            onSelectFile = { selectedFilePath = it.filePath },
            onPlayFile = onPlayFile,
            onNewNoteClick = onNewNoteClick,
            modifier = Modifier.width(if (isLargeTablet) 330.dp else 280.dp)
        )

        VerticalDivider(color = if (LocalIsCosmicDark.current) CosmicVoidCardBorder else Color(0xFFE2E8F0))

        // Panel 2: Note Detail Pane
        TabletNoteDetailPane(
            audioFile = selectedAudioFile,
            recording = selectedRecording,
            playbackState = playbackState,
            noteInsights = noteInsights,
            detailTab = detailTab,
            onTabChange = { detailTab = it },
            onPlayPause = onPlayPause,
            onSeek = onSeek,
            onOpenFullNote = { selectedRecording?.id?.let(onRecordingClick) },
            modifier = Modifier.weight(if (isLargeTablet) 1.25f else 1.8f)
        )

        // Panel 3: AI Insights Inspector (Rendered on Large Tablets)
        if (isLargeTablet) {
            VerticalDivider(color = if (LocalIsCosmicDark.current) CosmicVoidCardBorder else Color(0xFFE2E8F0))

            TabletAiInsightsInspectorPane(
                recording = selectedRecording,
                noteInsights = noteInsights,
                onActionToggle = onActionToggle,
                modifier = Modifier.width(320.dp)
            )
        }
    }
}

@Composable
fun TabletNotesListPane(
    audioFiles: List<AudioFileInfo>,
    recordingsByPath: Map<String, Recording>,
    selectedFilePath: String?,
    playbackState: PlaybackState,
    filterMode: TabletFeedFilter,
    allNotesCount: Int,
    pendingCount: Int,
    onFilterChange: (TabletFeedFilter) -> Unit,
    onSelectFile: (AudioFileInfo) -> Unit,
    onPlayFile: (AudioFileInfo) -> Unit,
    onNewNoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val paneBg = if (isDark) CosmicVoidCard else Color.White
    val textPrimary = if (isDark) TextOnDarkPrimary else TextPrimary
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(paneBg)
            .padding(top = 16.dp, start = 14.dp, end = 14.dp, bottom = 14.dp)
    ) {
        // Filter segmented bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(if (isDark) CosmicVoidBackground else Color(0xFFF1F5F9))
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabletFilterSegment(
                label = "All Notes",
                count = allNotesCount,
                isSelected = filterMode == TabletFeedFilter.ALL,
                onClick = { onFilterChange(TabletFeedFilter.ALL) },
                modifier = Modifier.weight(1f)
            )
            TabletFilterSegment(
                label = "Pending AI",
                count = pendingCount,
                isSelected = filterMode == TabletFeedFilter.PENDING,
                onClick = { onFilterChange(TabletFeedFilter.PENDING) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Note Cards List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(audioFiles, key = { it.filePath }) { audio ->
                val recording = recordingsByPath[audio.filePath]
                val isSelected = audio.filePath == selectedFilePath
                val isAudioPlaying = playbackState.isPlaying && playbackState.currentFilePath == audio.filePath

                TabletCompactAudioCard(
                    audioFile = audio,
                    recording = recording,
                    isSelected = isSelected,
                    isPlaying = isAudioPlaying,
                    onClick = { onSelectFile(audio) },
                    onPlayClick = { onPlayFile(audio) },
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(durationMillis = 400),
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // + New Note Button
        Button(
            onClick = onNewNoteClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) CosmicGlowBlue else CobaltBlue
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New note",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "New Note",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun TabletFilterSegment(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val selectedBg = if (isDark) Color(0xFF1E293B) else Color.White
    val selectedText = if (isDark) CosmicGlowBlue else CobaltBlue
    val unselectedText = if (isDark) TextOnDarkSecondary else TextSecondary

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(17.dp),
        color = if (isSelected) selectedBg else Color.Transparent,
        modifier = modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) selectedText else unselectedText
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) (if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)) else (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) selectedText else unselectedText
                )
            }
        }
    }
}

@Composable
fun TabletCompactAudioCard(
    audioFile: AudioFileInfo,
    recording: Recording?,
    isSelected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val cardBg = if (isSelected) {
        if (isDark) Color(0xFF1E293B).copy(alpha = 0.8f) else Color(0xFFEFF6FF)
    } else {
        if (isDark) CosmicVoidBackground else Color(0xFFF8FAFC)
    }
    val cardBorder = if (isSelected) {
        if (isDark) CosmicGlowBlue else CobaltBlue
    } else {
        if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)
    }
    val textPrimary = if (isDark) TextOnDarkPrimary else TextPrimary
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary

    val title = recording?.name?.takeIf { it.isNotBlank() } ?: audioFile.fileName.removeSuffix(".m4a")
    val durationText = formatDuration(audioFile.duration)
    val dateText = formatHumanRelativeDate(audioFile.timestamp)
    val summarySnippet = recording?.summary ?: recording?.transcript ?: "Voice recording ready for AI synthesis."
    val isSummarized = !recording?.summary.isNullOrBlank()

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, cardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Title & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (isSummarized) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.6f) else Color(0xFFDBEAFE))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "✦ Summarized",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) CosmicGlowBlue else CobaltBlue
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Pending",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = dateText,
                fontSize = 11.sp,
                color = textSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = summarySnippet,
                fontSize = 12.sp,
                color = textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Mini player scrubber row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isDark) CosmicGlowBlue else CobaltBlue)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play note",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Mini static waveform bars
                MiniWaveformCanvas(
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = durationText,
                    fontSize = 11.sp,
                    color = textSecondary
                )
            }
        }
    }
}

@Composable
fun MiniWaveformCanvas(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val barColor = if (isPlaying) {
        if (isDark) CosmicGlowBlue else CobaltBlue
    } else {
        if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mini_waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) (2f * Math.PI.toFloat()) else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mini_waveform_phase"
    )

    Canvas(modifier = modifier) {
        val barWidth = 2.dp.toPx()
        val targetSpacing = 1.5.dp.toPx()
        val step = barWidth + targetSpacing
        val count = (size.width / step).toInt().coerceAtLeast(16)
        val spacing = if (count > 1) (size.width - (count * barWidth)) / (count - 1) else targetSpacing
        val centerY = size.height / 2

        for (i in 0 until count) {
            val wave = if (isPlaying) {
                (sin(i * 0.45f + phase) * cos(i * 0.25f + phase * 0.5f) + 1f) / 2f
            } else {
                (sin(i * 0.45f) + 1f) / 2f
            }
            val factor = 0.25f + 0.75f * wave
            val barHeight = (size.height * factor).coerceAtLeast(4.dp.toPx())
            val x = i * (barWidth + spacing)
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, centerY - barHeight / 2),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
            )
        }
    }
}

@Composable
fun TabletNoteDetailPane(
    audioFile: AudioFileInfo?,
    recording: Recording?,
    playbackState: PlaybackState,
    noteInsights: List<InsightEntity>,
    detailTab: TabletDetailTab,
    onTabChange: (TabletDetailTab) -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Int) -> Unit,
    onOpenFullNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val paneBg = if (isDark) CosmicVoidCard else Color.White
    val textPrimary = if (isDark) TextOnDarkPrimary else TextPrimary
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary

    if (audioFile == null) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .background(paneBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Select a note to inspect details and AI synthesis",
                fontSize = 14.sp,
                color = textSecondary
            )
        }
        return
    }

    val title = recording?.name?.takeIf { it.isNotBlank() } ?: audioFile.fileName.removeSuffix(".m4a")
    val isSummarized = !recording?.summary.isNullOrBlank()
    val formattedDate = formatHumanRelativeDate(audioFile.timestamp)
    val durationText = formatDuration(audioFile.duration)
    val currentPosition = playbackState.currentPosition
    val totalDuration = audioFile.duration.toInt().coerceAtLeast(1)
    val progress = (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    val isAudioPlaying = playbackState.isPlaying && playbackState.currentFilePath == audioFile.filePath

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(paneBg)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Detail Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isSummarized) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.6f) else Color(0xFFDBEAFE))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "✦ Summarized",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) CosmicGlowBlue else CobaltBlue
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$formattedDate · $durationText",
                    fontSize = 12.sp,
                    color = textSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenFullNote) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit note",
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Large Waveform Player Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) CosmicVoidBackground else Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Waveform Canvas
                DetailWaveformCanvas(
                    progress = progress,
                    isPlaying = isAudioPlaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .pointerInput(totalDuration) {
                            detectTapGestures { offset ->
                                val clickProgress = (offset.x / size.width).coerceIn(0f, 1f)
                                onSeek((clickProgress * totalDuration).toInt())
                            }
                        }
                        .pointerInput(totalDuration) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                val dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                                onSeek((dragProgress * totalDuration).toInt())
                            }
                        }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Time counters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                    Text(
                        text = formatTime(totalDuration),
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Playback controls row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skip backward 10s
                    IconButton(onClick = { onSeek((currentPosition - 10000).coerceAtLeast(0)) }) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Replay 10s",
                            tint = textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Large Play / Pause button
                    FilledIconButton(
                        onClick = onPlayPause,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isDark) CosmicGlowBlue else CobaltBlue
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play pause",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Skip forward 10s
                    IconButton(onClick = { onSeek((currentPosition + 10000).coerceAtMost(totalDuration)) }) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Speed pill (1.0x)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "1.0x",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Segmented Tab Switcher: [ Summary ] [ Transcript ] [ Insights ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (isDark) CosmicVoidBackground else Color(0xFFF1F5F9))
                .padding(4.dp)
        ) {
            TabletDetailTabSegment(
                label = "✦ Summary",
                isSelected = detailTab == TabletDetailTab.SUMMARY,
                onClick = { onTabChange(TabletDetailTab.SUMMARY) },
                modifier = Modifier.weight(1f)
            )
            TabletDetailTabSegment(
                label = "📄 Transcript",
                isSelected = detailTab == TabletDetailTab.TRANSCRIPT,
                onClick = { onTabChange(TabletDetailTab.TRANSCRIPT) },
                modifier = Modifier.weight(1f)
            )
            TabletDetailTabSegment(
                label = "💡 Insights",
                isSelected = detailTab == TabletDetailTab.INSIGHTS,
                onClick = { onTabChange(TabletDetailTab.INSIGHTS) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Active Tab Body
        when (detailTab) {
            TabletDetailTab.SUMMARY -> {
                TabletSummaryTabContent(recording = recording)
            }
            TabletDetailTab.TRANSCRIPT -> {
                TabletTranscriptTabContent(recording = recording)
            }
            TabletDetailTab.INSIGHTS -> {
                TabletInsightsTabContent(noteInsights = noteInsights)
            }
        }
    }
}

@Composable
private fun TabletDetailTabSegment(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val selectedBg = if (isDark) Color(0xFF1E293B) else Color.White
    val selectedText = if (isDark) CosmicGlowBlue else CobaltBlue
    val unselectedText = if (isDark) TextOnDarkSecondary else TextSecondary

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(11.dp),
        color = if (isSelected) selectedBg else Color.Transparent,
        modifier = modifier.height(36.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) selectedText else unselectedText
            )
        }
    }
}

@Composable
private fun TabletSummaryTabContent(recording: Recording?) {
    val isDark = LocalIsCosmicDark.current
    val textPrimary = if (isDark) TextOnDarkPrimary else TextPrimary
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary
    val summary = recording?.summary ?: "No summary generated yet. Tap 'Generate Insights' or enable Auto-AI in Settings to summarize automatically."

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Summary",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = summary,
            fontSize = 14.sp,
            color = textSecondary,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Referenced In Section
        Text(
            text = "Referenced In",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = textPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TabletTagPill("Design team sync")
            TabletTagPill("Pricing discussion")
            TabletTagPill("Offline model evaluation")
        }
    }
}

@Composable
private fun TabletTagPill(tag: String) {
    val isDark = LocalIsCosmicDark.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0))
    ) {
        Text(
            text = tag,
            fontSize = 12.sp,
            color = if (isDark) TextOnDarkSecondary else TextSecondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun TabletTranscriptTabContent(recording: Recording?) {
    val isDark = LocalIsCosmicDark.current
    val textPrimary = if (isDark) TextOnDarkPrimary else TextPrimary
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary
    val transcript = recording?.transcript ?: "No transcript available. Audio is currently processing or offline transcription is queued."

    Column {
        Text(
            text = "Transcript",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = transcript,
            fontSize = 14.sp,
            color = textSecondary,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun TabletInsightsTabContent(noteInsights: List<InsightEntity>) {
    val isDark = LocalIsCosmicDark.current
    val textPrimary = if (isDark) TextOnDarkPrimary else TextPrimary
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary

    if (noteInsights.isEmpty()) {
        Text(
            text = "No extracted insights yet for this recording.",
            fontSize = 14.sp,
            color = textSecondary
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        noteInsights.forEach { insight ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) CosmicVoidBackground else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    val badgeColor = when (insight.kind) {
                        InsightKind.ACTION -> if (isDark) CosmicGlowBlue else CobaltBlue
                        InsightKind.IDEA -> Color(0xFFD97706)
                        InsightKind.DECISION -> Color(0xFF0D9488)
                        else -> if (isDark) CosmicGlowBlue else CobaltBlue
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                            .padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = insight.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimary
                        )
                        insight.rationale?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = it,
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailWaveformCanvas(
    progress: Float,
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val activeColor = if (isDark) CosmicGlowBlue else CobaltBlue
    val activeTopColor = if (isDark) Color(0xFF93C5FD) else Color(0xFF3B82F6)
    val inactiveColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val headIndicatorColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF1E40AF)

    val infiniteTransition = rememberInfiniteTransition(label = "detail_waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) (2f * Math.PI.toFloat()) else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "detail_waveform_phase"
    )

    Canvas(modifier = modifier) {
        val barWidth = 3.dp.toPx()
        val targetSpacing = 2.dp.toPx()
        val step = barWidth + targetSpacing
        val count = (size.width / step).toInt().coerceAtLeast(24)
        val spacing = if (count > 1) (size.width - (count * barWidth)) / (count - 1) else targetSpacing
        val centerY = size.height / 2
        val activeIndex = (progress * count).toInt().coerceIn(0, count)

        for (i in 0 until count) {
            val baseWave = (sin(i * 0.38f) * cos(i * 0.18f) + 1f) / 2f
            val dynamicWave = if (isPlaying && i <= activeIndex) {
                (sin(i * 0.38f + phase) * cos(i * 0.18f + phase * 0.6f) + 1f) / 2f
            } else {
                baseWave
            }
            val factor = 0.22f + 0.78f * dynamicWave
            val barHeight = (size.height * factor).coerceAtLeast(6.dp.toPx())
            val x = i * (barWidth + spacing)
            val isPassed = i <= activeIndex
            val isCurrentHead = i == activeIndex

            if (isPassed) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(activeTopColor, activeColor),
                        startY = centerY - barHeight / 2,
                        endY = centerY + barHeight / 2
                    ),
                    topLeft = Offset(x, centerY - barHeight / 2),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            } else {
                drawRoundRect(
                    color = inactiveColor,
                    topLeft = Offset(x, centerY - barHeight / 2),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }

            if (isCurrentHead && progress > 0f && progress < 1f) {
                drawCircle(
                    color = headIndicatorColor,
                    radius = 2.5.dp.toPx(),
                    center = Offset(x + barWidth / 2, centerY - barHeight / 2 - 4.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun TabletAiInsightsInspectorPane(
    recording: Recording?,
    noteInsights: List<InsightEntity>,
    onActionToggle: (InsightEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val paneBg = if (isDark) CosmicVoidCard else Color.White
    val textPrimary = if (isDark) TextOnDarkPrimary else TextPrimary
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary

    val nextSteps = remember(noteInsights) { noteInsights.filter { it.kind == InsightKind.ACTION } }
    val ideas = remember(noteInsights) { noteInsights.filter { it.kind == InsightKind.IDEA } }
    val decisions = remember(noteInsights) { noteInsights.filter { it.kind == InsightKind.DECISION } }

    var selectedSection by remember { mutableStateOf("Next Steps") }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(paneBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Pane Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI Insights",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Segment chips: Next Steps | Ideas | Decisions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TabletInspectorChip(
                label = "Next Steps",
                count = nextSteps.size,
                isSelected = selectedSection == "Next Steps",
                accentColor = if (isDark) CosmicGlowBlue else CobaltBlue,
                onClick = { selectedSection = "Next Steps" },
                modifier = Modifier.weight(1f)
            )
            TabletInspectorChip(
                label = "Ideas",
                count = ideas.size,
                isSelected = selectedSection == "Ideas",
                accentColor = Color(0xFFD97706),
                onClick = { selectedSection = "Ideas" },
                modifier = Modifier.weight(1f)
            )
            TabletInspectorChip(
                label = "Decisions",
                count = decisions.size,
                isSelected = selectedSection == "Decisions",
                accentColor = Color(0xFF0D9488),
                onClick = { selectedSection = "Decisions" },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Next Steps Section
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isDark) CosmicVoidBackground else Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Next Steps",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                    Text(
                        text = "${nextSteps.size} items",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (nextSteps.isEmpty()) {
                    Text(
                        text = "No pending action items.",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                } else {
                    nextSteps.forEach { action ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = action.status == InsightStatus.COMPLETED,
                                onCheckedChange = { onActionToggle(action) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = if (isDark) CosmicGlowBlue else CobaltBlue
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = action.text,
                                fontSize = 12.sp,
                                color = if (action.status == InsightStatus.COMPLETED) textSecondary else textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Key Ideas Card (Warm Amber)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isDark) Color(0xFF2E1A05).copy(alpha = 0.5f) else Color(0xFFFFFBEB),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF78350F) else Color(0xFFFDE68A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Key Ideas",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (ideas.isEmpty()) {
                    Text(
                        text = "Ideas will appear once synthesized.",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                } else {
                    ideas.forEach { idea ->
                        Text(
                            text = "• ${idea.text}",
                            fontSize = 12.sp,
                            color = if (isDark) TextOnDarkSecondary else Color(0xFF78350F),
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Key Decisions Card (Emerald)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isDark) Color(0xFF06281E).copy(alpha = 0.5f) else Color(0xFFF0FDF4),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF065F46) else Color(0xFFBBF7D0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircleOutline,
                        contentDescription = null,
                        tint = Color(0xFF0D9488),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Key Decisions",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (decisions.isEmpty()) {
                    Text(
                        text = "Decisions will appear once synthesized.",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                } else {
                    decisions.forEach { decision ->
                        Text(
                            text = "• ${decision.text}",
                            fontSize = 12.sp,
                            color = if (isDark) TextOnDarkSecondary else Color(0xFF065F46),
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ask About This Recording Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isDark) CosmicVoidBackground else Color(0xFFF1F5F9),
            border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ask about this recording",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) CosmicVoidCard else Color.White)
                        .border(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFCBD5E1), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ask a question...",
                        fontSize = 12.sp,
                        color = textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TabletInspectorChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) accentColor.copy(alpha = if (isDark) 0.3f else 0.15f) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
        border = BorderStroke(1.dp, if (isSelected) accentColor else Color.Transparent),
        modifier = modifier.height(30.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label $count",
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) accentColor else (if (isDark) TextOnDarkSecondary else TextSecondary)
            )
        }
    }
}

@Composable
fun TabletInsights3ColumnWorkspace(
    activeActions: List<InsightEntity>,
    allIdeas: List<InsightEntity>,
    allDecisions: List<InsightEntity>,
    themeClusters: List<ThemeCluster> = emptyList(),
    isLargeTablet: Boolean,
    activityMetrics: UserActivityMetrics = UserActivityMetrics(),
    onActionToggle: (InsightEntity) -> Unit,
    onRecordingClick: (Long) -> Unit = {},
    onViewAllNotes: () -> Unit,
    onArchiveConfirmed: () -> Unit = {},
    onRestoreArchived: () -> Unit = {},
    onDismissTheme: (String) -> Unit = {},
    archivedCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val paneBg = if (isDark) CosmicVoidCard else Color.White
    val textPrimary = if (isDark) TextOnDarkPrimary else TextPrimary
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT || configuration.screenWidthDp < configuration.screenHeightDp
    val showActivitySidebar = !isPortrait && configuration.screenWidthDp >= 1000

    var selectedTab by remember { mutableStateOf(InsightsTab.NEXT_STEPS) }
    var isCompletedExpanded by remember { mutableStateOf(false) }
    var showArchiveDialog by remember { mutableStateOf(false) }

    val pendingActions = remember(activeActions) {
        activeActions.filter { it.status == InsightStatus.OPEN }
    }
    val completedActions = remember(activeActions) {
        activeActions.filter { it.status == InsightStatus.COMPLETED }
    }
    val totalActiveActions = activeActions.size
    val completedActionsCount = completedActions.size
    val progressFraction = if (totalActiveActions > 0) completedActionsCount.toFloat() / totalActiveActions else 0f

    if (showArchiveDialog) {
        ArchiveTasksDialog(
            taskCount = completedActionsCount,
            onConfirm = {
                showArchiveDialog = false
                onArchiveConfirmed()
            },
            onDismiss = { showArchiveDialog = false }
        )
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(paneBg)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopStart
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = if (showActivitySidebar) 20.dp else 24.dp, top = 20.dp)
            ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Insights",
                        fontFamily = PlayfairDisplayFontFamily,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Turn your voice notes into clarity.",
                        fontSize = 14.sp,
                        color = textSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFDBEAFE))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI-Extracted",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) CosmicGlowBlue else CobaltBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3-Option Top Switcher Capsule matching mobile view
            SingleRowInsightsCapsule(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                nextStepsCount = pendingActions.size,
                ideasCount = allIdeas.size,
                decisionsCount = allDecisions.size,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content with full parity and flush padding matching outer container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    InsightsTab.NEXT_STEPS -> {
                        NextStepsTab(
                            pendingActions = pendingActions,
                            completedActions = completedActions,
                            themeClusters = themeClusters,
                            totalActiveCount = totalActiveActions,
                            completedCount = completedActionsCount,
                            progressFraction = progressFraction,
                            archivedCount = archivedCount,
                            isCompletedExpanded = isCompletedExpanded,
                            onToggleCompletedExpanded = { isCompletedExpanded = !isCompletedExpanded },
                            onActionToggle = onActionToggle,
                            onRecordingClick = onRecordingClick,
                            onArchiveClick = { showArchiveDialog = true },
                            onRestoreArchived = onRestoreArchived,
                            onDismissTheme = onDismissTheme,
                            contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 8.dp, bottom = 120.dp),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    InsightsTab.IDEAS -> {
                        IdeasStreamTab(
                            ideas = allIdeas,
                            onRecordingClick = onRecordingClick,
                            contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 8.dp, bottom = 120.dp),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    InsightsTab.DECISIONS -> {
                        DecisionsTimelineTab(
                            decisions = allDecisions,
                            onRecordingClick = onRecordingClick,
                            contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 8.dp, bottom = 120.dp),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    if (showActivitySidebar) {
        VerticalDivider(
            color = if (isDark) CosmicVoidCardBorder else Color(0xFFF1F5F9),
            modifier = Modifier.fillMaxHeight()
        )

        TabletInsightsActivitySidebar(
            metrics = activityMetrics,
            onViewAllNotes = onViewAllNotes,
            onNavigateToTab = { tab -> selectedTab = tab },
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
                .padding(start = 20.dp, end = 24.dp, top = 20.dp)
        )
    }
}
}

@Composable
fun TabletInsightsActivitySidebar(
    metrics: UserActivityMetrics,
    onViewAllNotes: () -> Unit,
    onNavigateToTab: (InsightsTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val textPrimary = if (isDark) TextOnDarkPrimary else TextPrimary
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            RecentActivityCard(
                metrics = metrics,
                isDark = isDark,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )
        }

        item {
            WeeklyActivityCard(
                metrics = metrics,
                isDark = isDark,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )
        }

        item {
            HowSrutamHelpsCard(
                isDark = isDark,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )
        }
    }
}

@Composable
private fun RecentActivityCard(
    metrics: UserActivityMetrics,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color
) {
    val cardBg = if (isDark) CosmicVoidBackground else Color.White
    val cardBorder = if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Column {
                Text(
                    text = "Recent activity",
                    fontFamily = PlayfairDisplayFontFamily,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Your voice notes, at a glance.",
                    fontSize = 11.sp,
                    color = textSecondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActivityMiniMetricTile(
                    count = metrics.notesLast7Days,
                    title = "Notes",
                    subtitle = "Last 7 days",
                    icon = Icons.Outlined.Description,
                    iconTint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    isDark = isDark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    modifier = Modifier.weight(1f)
                )

                ActivityMiniMetricTile(
                    count = metrics.aiExtractedLast7Days,
                    title = "AI extracted",
                    subtitle = "Last 7 days",
                    icon = Icons.Default.AutoAwesome,
                    iconTint = if (isDark) CosmicGlowBlue else CobaltBlue,
                    isDark = isDark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    modifier = Modifier.weight(1f)
                )

                ActivityMiniMetricTile(
                    count = metrics.actionItemsCompleted,
                    title = "Action items",
                    subtitle = "Completed",
                    icon = Icons.Default.CheckCircleOutline,
                    iconTint = Color(0xFF10B981),
                    isDark = isDark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    modifier = Modifier.weight(1f)
                )

                ActivityMiniMetricTile(
                    count = metrics.ideasCaptured,
                    title = "Ideas",
                    subtitle = "Captured",
                    icon = Icons.Outlined.Lightbulb,
                    iconTint = Color(0xFFD97706),
                    isDark = isDark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ActivityMiniMetricTile(
    count: Int,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) CosmicVoidCard else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFF1F5F9)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = count.toString(),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 8.5.sp,
                color = textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WeeklyActivityCard(
    metrics: UserActivityMetrics,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color
) {
    val cardBg = if (isDark) CosmicVoidBackground else Color.White
    val cardBorder = if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Weekly activity",
                fontFamily = PlayfairDisplayFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Notes recorded over the last 7 days.",
                fontSize = 11.sp,
                color = textSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            AnimatedVisibility(
                visible = selectedDayIndex != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val day = selectedDayIndex?.let { metrics.weeklyActivity.getOrNull(it) }
                if (day != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) CosmicVoidCard else Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFDBEAFE)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${day.count} voice ${if (day.count == 1) "note" else "notes"} on ${day.fullDayName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) CosmicGlowBlue else CobaltBlue
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = textSecondary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { selectedDayIndex = null }
                            )
                        }
                    }
                }
            }

            val maxCount = metrics.maxDailyCount.coerceAtLeast(4)
            val midCount = maxCount / 2
            val chartHeight = 100.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight + 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .height(chartHeight)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(text = "$maxCount", fontSize = 10.sp, color = textSecondary)
                    Text(text = "$midCount", fontSize = 10.sp, color = textSecondary)
                    Text(text = "0", fontSize = 10.sp, color = textSecondary)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        HorizontalDivider(color = if (isDark) CosmicVoidCardBorder.copy(alpha = 0.5f) else Color(0xFFF1F5F9))
                        HorizontalDivider(color = if (isDark) CosmicVoidCardBorder.copy(alpha = 0.5f) else Color(0xFFF1F5F9))
                        HorizontalDivider(color = if (isDark) CosmicVoidCardBorder.copy(alpha = 0.5f) else Color(0xFFF1F5F9))
                    }

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        metrics.weeklyActivity.forEachIndexed { index, day ->
                            val isSelected = selectedDayIndex == index
                            val heightFraction = (day.count.toFloat() / maxCount).coerceIn(0f, 1f)

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable {
                                        selectedDayIndex = if (selectedDayIndex == index) null else index
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Box(
                                    modifier = Modifier
                                        .height(chartHeight)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    val barHeight = (chartHeight * heightFraction).coerceAtLeast(if (day.count > 0) 6.dp else 2.dp)
                                    val barColor = when {
                                        day.isToday -> if (isDark) CosmicGlowBlue else CobaltBlue
                                        isSelected -> if (isDark) CosmicGlowBlue else CobaltBlue
                                        day.count > 0 -> if (isDark) CosmicGlowBlue.copy(alpha = 0.6f) else Color(0xFF93C5FD)
                                        else -> if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(18.dp)
                                            .height(barHeight)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(barColor)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = day.dayLabel,
                                    fontSize = 10.sp,
                                    fontWeight = if (day.isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (day.isToday || isSelected) textPrimary else textSecondary
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
private fun HowSrutamHelpsCard(
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color
) {
    val cardBg = if (isDark) CosmicVoidBackground else Color.White
    val cardBorder = if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "How Srutam helps",
                fontFamily = PlayfairDisplayFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HelpFeatureRow(
                    icon = Icons.Default.Mic,
                    title = "Capture",
                    description = "Speak naturally, anytime.",
                    isDark = isDark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                HelpFeatureRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Extract",
                    description = "AI finds key points, tasks and decisions.",
                    isDark = isDark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                HelpFeatureRow(
                    icon = Icons.Default.CheckCircleOutline,
                    title = "Stay on track",
                    description = "Turn conversations into action.",
                    isDark = isDark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }
        }
    }
}

@Composable
private fun HelpFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else Color(0xFFEFF6FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = textSecondary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TabletCopilot3PanelWorkspace(
    messages: List<GlobalChatMessage>,
    audioFiles: List<AudioFileInfo>,
    recordingsByPath: Map<String, Recording>,
    isLargeTablet: Boolean,
    onSendMessage: (String) -> Unit,
    onRecordingClick: (Long) -> Unit,
    isQueryLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT || configuration.screenWidthDp < configuration.screenHeightDp
    val isDark = LocalIsCosmicDark.current
    val paneBg = if (isDark) CosmicVoidCard else Color.White
    val textPrimary = if (isDark) TextOnDarkPrimary else TextPrimary
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary

    var inputQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isQueryLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val starterQueries = listOf(
        "What did I talk about recently?",
        "List all action items across notes",
        "Summarize my key ideas",
        "What decisions were made?",
        "Show all notes about user onboarding"
    )

    Row(modifier = modifier.fillMaxSize()) {
        // Main Chat Area (occupies full width in portrait, weight(1f) in landscape)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (isDark) CosmicVoidBackground else Color(0xFFF8FAFC))
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Srutam AI",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Ask anything across all your voice notes and ideas",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
            }

            HorizontalDivider(color = if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0))

            // Chat Feed
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Suggested Questions in a horizontal scrollable row (matching mobile screen)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = "Suggested Questions",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(starterQueries) { query ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isDark) CosmicVoidCard else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
                                    modifier = Modifier.clickable {
                                        onSendMessage(query)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "\uD83D\uDCA1",
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = query,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = textPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Chat Messages
                items(messages) { msg ->
                    if (msg.isUser) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = 18.dp,
                                    bottomEnd = 4.dp
                                ),
                                color = if (isDark) CosmicGlowBlue else CobaltBlue,
                                modifier = Modifier.widthIn(max = if (isPortrait) 520.dp else 600.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = 4.dp,
                                    bottomEnd = 18.dp
                                ),
                                color = if (isDark) CosmicVoidCard else Color.White,
                                border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
                                modifier = Modifier.widthIn(max = if (isPortrait) 560.dp else 660.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Srutam AI",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) CosmicGlowBlue else CobaltBlue
                                        )
                                    }

                                    Text(
                                        text = msg.text,
                                        fontSize = 14.sp,
                                        color = textPrimary,
                                        lineHeight = 20.sp
                                    )

                                    if (msg.citedNotes.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Referenced Notes:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textSecondary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(msg.citedNotes) { (id, title) ->
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isDark) CosmicVoidBackground else Color(0xFFF1F5F9),
                                                    border = BorderStroke(0.5.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
                                                    modifier = Modifier.clickable { onRecordingClick(id) }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Description,
                                                            contentDescription = null,
                                                            tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Text(
                                                            text = title,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = textPrimary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isQueryLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = if (isDark) CosmicGlowBlue else CobaltBlue
                            )
                            Text(
                                text = "Searching voice notes...",
                                fontSize = 12.sp,
                                color = if (isDark) CosmicGlowBlue else CobaltBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Query Input Bar
            val isKeyboardOpen = WindowInsets.isImeVisible
            val hasSourcesSidebar = isLargeTablet && !isPortrait
            val bottomPad = if (isKeyboardOpen) 12.dp else 24.dp
            val endPad = if (!hasSourcesSidebar && !isKeyboardOpen) 84.dp else 20.dp

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isDark) CosmicVoidCard else Color.White,
                border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFCBD5E1)),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 20.dp, end = endPad, bottom = bottomPad)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 6.dp)
                    ) {
                        if (inputQuery.isEmpty()) {
                            Text(
                                text = "Ask a question about your notes...",
                                fontSize = 13.sp,
                                color = textSecondary
                            )
                        }
                        BasicTextField(
                            value = inputQuery,
                            onValueChange = { inputQuery = it },
                            singleLine = false,
                            minLines = 1,
                            maxLines = 5,
                            textStyle = TextStyle(
                                color = textPrimary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 20.dp, max = 110.dp)
                        )
                    }

                    FilledIconButton(
                        onClick = {
                            if (inputQuery.isNotBlank() && !isQueryLoading) {
                                onSendMessage(inputQuery)
                                inputQuery = ""
                            }
                        },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isDark) CosmicGlowBlue else CobaltBlue,
                            disabledContainerColor = (if (isDark) CosmicGlowBlue else CobaltBlue).copy(alpha = 0.4f)
                        ),
                        enabled = inputQuery.isNotBlank() && !isQueryLoading,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Column 2/3: Sources & Related (Only in wide Landscape mode)
        if (isLargeTablet && !isPortrait) {
            VerticalDivider(color = if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0))

            Column(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .background(paneBg)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Sources (${audioFiles.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(audioFiles) { audio ->
                        val rec = recordingsByPath[audio.filePath]
                        val title = rec?.name ?: audio.fileName.removeSuffix(".m4a")
                        Surface(
                            onClick = { rec?.id?.let(onRecordingClick) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) CosmicVoidBackground else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatDuration(audio.duration),
                                        fontSize = 10.sp,
                                        color = textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
