package space.iamjustkrishna.srutam.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onPlayFile: (AudioFileInfo) -> Unit = {},
    onPlayPause: () -> Unit = {},
    onSeek: (Int) -> Unit = {},
    onRecordingClick: (Long) -> Unit = {},
    onActionToggle: (InsightEntity) -> Unit = {},
    onNewNoteClick: () -> Unit = {},
    onSendCopilotQuery: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isLargeTablet = screenWidthDp >= 900
    val isDark = LocalIsCosmicDark.current

    var selectedFolder by remember { mutableStateOf("All Notes") }
    var searchQuery by remember { mutableStateOf("") }

    val bgModifier = if (isDark) {
        Modifier.background(CosmicVoidBackground)
    } else {
        Modifier.background(Color(0xFFF8FAFC))
    }

    Box(modifier = modifier.fillMaxSize().then(bgModifier)) {
        if (isDark) {
            CosmicBackground()
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // Left Navigation Rail
            TabletSideNavRail(
                currentTab = currentTab,
                onTabSelected = onTabSelected,
                onSettingsClick = onSettingsClick,
                selectedFolder = selectedFolder,
                onFolderSelected = { selectedFolder = it },
                isCompact = !isLargeTablet
            )

            // Main Content Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Top Search Bar
                TabletTopSearchBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onSettingsClick = onSettingsClick
                )

                // Active Tab Content
                when (currentTab) {
                    RootTab.NOTES -> {
                        TabletNotes3PanelWorkspace(
                            audioFiles = audioFiles,
                            recordingsByPath = recordingsByPath,
                            playbackState = playbackState,
                            allInsights = activeActions + allIdeas + allDecisions,
                            isLargeTablet = isLargeTablet,
                            searchQuery = searchQuery,
                            onPlayFile = onPlayFile,
                            onPlayPause = onPlayPause,
                            onSeek = onSeek,
                            onRecordingClick = onRecordingClick,
                            onActionToggle = onActionToggle,
                            onNewNoteClick = onNewNoteClick
                        )
                    }
                    RootTab.ACTIONS -> {
                        TabletInsights3ColumnWorkspace(
                            activeActions = activeActions,
                            allIdeas = allIdeas,
                            allDecisions = allDecisions,
                            isLargeTablet = isLargeTablet,
                            onActionToggle = onActionToggle,
                            onViewAllNotes = { onTabSelected(RootTab.NOTES) }
                        )
                    }
                    RootTab.AI -> {
                        TabletCopilot3PanelWorkspace(
                            messages = copilotMessages,
                            audioFiles = audioFiles,
                            recordingsByPath = recordingsByPath,
                            isLargeTablet = isLargeTablet,
                            onSendMessage = onSendCopilotQuery,
                            onRecordingClick = onRecordingClick
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
                    onPlayClick = { onPlayFile(audio) }
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

    Canvas(modifier = modifier) {
        val count = 22
        val barWidth = 2.dp.toPx()
        val spacing = (size.width - (count * barWidth)) / (count - 1).coerceAtLeast(1)
        val centerY = size.height / 2

        for (i in 0 until count) {
            val factor = 0.3f + 0.7f * ((sin(i * 0.5f) + 1f) / 2f)
            val barHeight = (size.height * factor).coerceAtLeast(4.dp.toPx())
            val x = i * (barWidth + spacing)
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, centerY - barHeight / 2),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .pointerInput(totalDuration) {
                            detectTapGestures { offset ->
                                val clickProgress = (offset.x / size.width).coerceIn(0f, 1f)
                                onSeek((clickProgress * totalDuration).toInt())
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
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val activeColor = if (isDark) CosmicGlowBlue else CobaltBlue
    val inactiveColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    Canvas(modifier = modifier) {
        val count = 48
        val barWidth = 3.dp.toPx()
        val spacing = (size.width - (count * barWidth)) / (count - 1).coerceAtLeast(1)
        val centerY = size.height / 2
        val activeIndex = (progress * count).toInt()

        for (i in 0 until count) {
            val factor = 0.25f + 0.75f * ((sin(i * 0.45f) + 1f) / 2f)
            val barHeight = (size.height * factor).coerceAtLeast(6.dp.toPx())
            val x = i * (barWidth + spacing)
            val isPassed = i <= activeIndex
            drawRoundRect(
                color = if (isPassed) activeColor else inactiveColor,
                topLeft = Offset(x, centerY - barHeight / 2),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
            )
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
    isLargeTablet: Boolean,
    onActionToggle: (InsightEntity) -> Unit,
    onViewAllNotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val paneBg = if (isDark) CosmicVoidCard else Color.White
    val textPrimary = if (isDark) TextOnDarkPrimary else TextPrimary
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(paneBg)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
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

            // Hero banner badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFDBEAFE))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ideas, decisions and next steps - automatically, from your conversations.",
                        fontSize = 12.sp,
                        color = if (isDark) CosmicGlowBlue else CobaltBlue
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3 Parallel Columns for Next Steps, Ideas, Decisions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Column 1: Next Steps
            TabletInsightColumnCard(
                title = "Next Steps",
                count = activeActions.size,
                icon = Icons.Default.FormatListBulleted,
                accentColor = if (isDark) CosmicGlowBlue else CobaltBlue,
                items = activeActions,
                isAction = true,
                onActionToggle = onActionToggle,
                modifier = Modifier.weight(1f)
            )

            // Column 2: Ideas
            TabletInsightColumnCard(
                title = "Ideas",
                count = allIdeas.size,
                icon = Icons.Default.Lightbulb,
                accentColor = Color(0xFFD97706),
                items = allIdeas,
                isAction = false,
                onActionToggle = onActionToggle,
                modifier = Modifier.weight(1f)
            )

            // Column 3: Decisions
            TabletInsightColumnCard(
                title = "Decisions",
                count = allDecisions.size,
                icon = Icons.Outlined.CheckCircleOutline,
                accentColor = Color(0xFF0D9488),
                items = allDecisions,
                isAction = false,
                onActionToggle = onActionToggle,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Connected Insights card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isDark) CosmicVoidBackground else Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Connected Insights",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Distinct thoughts, actions and decisions distilled from your voice notes.",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }

                Button(
                    onClick = onViewAllNotes,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color.White
                    ),
                    border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFCBD5E1)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "View All Notes",
                        fontSize = 12.sp,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = textPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TabletInsightColumnCard(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    items: List<InsightEntity>,
    isAction: Boolean,
    onActionToggle: (InsightEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val cardBg = if (isDark) CosmicVoidBackground else Color(0xFFF8FAFC)
    val cardBorder = if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) TextOnDarkPrimary else TextPrimary
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = modifier.fillMaxHeight()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
                Text(
                    text = "$count items",
                    fontSize = 12.sp,
                    color = textSecondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (items.isEmpty()) {
                Text(
                    text = "No $title recorded yet.",
                    fontSize = 12.sp,
                    color = textSecondary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                items.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) CosmicVoidCard else Color.White,
                        border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            if (isAction) {
                                Checkbox(
                                    checked = item.status == InsightStatus.COMPLETED,
                                    onCheckedChange = { onActionToggle(item) },
                                    colors = CheckboxDefaults.colors(checkedColor = accentColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.text,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textPrimary,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "From ${item.recordingName}",
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

@Composable
fun TabletCopilot3PanelWorkspace(
    messages: List<GlobalChatMessage>,
    audioFiles: List<AudioFileInfo>,
    recordingsByPath: Map<String, Recording>,
    isLargeTablet: Boolean,
    onSendMessage: (String) -> Unit,
    onRecordingClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val paneBg = if (isDark) CosmicVoidCard else Color.White
    val textPrimary = if (isDark) TextOnDarkPrimary else TextPrimary
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary

    var inputQuery by remember { mutableStateOf("") }

    val starterQueries = listOf(
        "What are the main next steps from my recent discussions?",
        "Summarize the key ideas about offline mode.",
        "What did I decide about pricing?",
        "Show all notes about user onboarding."
    )

    Row(modifier = modifier.fillMaxSize()) {
        // Column 1: Recent Questions (~240dp)
        Column(
            modifier = Modifier
                .width(if (isLargeTablet) 260.dp else 220.dp)
                .fillMaxHeight()
                .background(paneBg)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Questions",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                IconButton(onClick = { inputQuery = "" }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New question",
                        tint = textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            starterQueries.forEach { query ->
                Surface(
                    onClick = { inputQuery = query },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) CosmicVoidBackground else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = query,
                        fontSize = 12.sp,
                        color = textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        VerticalDivider(color = if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0))

        // Column 2: Active Chat Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (isDark) CosmicVoidBackground else Color(0xFFF8FAFC))
                .padding(20.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) CosmicVoidCard else Color.White,
                            border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Srutam AI Copilot",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Ask questions across all your voice notes, transcripts, and action items with grounded citations.",
                                    fontSize = 13.sp,
                                    color = textSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                } else {
                    items(messages) { msg ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (msg.isUser) (if (isDark) CosmicGlowBlue else CobaltBlue) else (if (isDark) CosmicVoidCard else Color.White),
                                border = if (!msg.isUser) BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)) else null,
                                modifier = Modifier.widthIn(max = 480.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    fontSize = 13.sp,
                                    color = if (msg.isUser) Color.White else textPrimary,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Query Input Bar
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isDark) CosmicVoidCard else Color.White,
                border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (inputQuery.isEmpty()) {
                            Text(
                                text = "Ask a question about your notes...",
                                fontSize = 13.sp,
                                color = textSecondary
                            )
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = inputQuery,
                            onValueChange = { inputQuery = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = textPrimary,
                                fontSize = 13.sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    FilledIconButton(
                        onClick = {
                            if (inputQuery.isNotBlank()) {
                                onSendMessage(inputQuery)
                                inputQuery = ""
                            }
                        },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isDark) CosmicGlowBlue else CobaltBlue
                        ),
                        modifier = Modifier.size(34.dp)
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

        // Column 3: Sources & Related (Large Tablet)
        if (isLargeTablet) {
            VerticalDivider(color = if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0))

            Column(
                modifier = Modifier
                    .width(260.dp)
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

                Spacer(modifier = Modifier.height(10.dp))

                audioFiles.take(5).forEach { audio ->
                    val rec = recordingsByPath[audio.filePath]
                    val title = rec?.name ?: audio.fileName.removeSuffix(".m4a")
                    Surface(
                        onClick = { rec?.id?.let(onRecordingClick) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) CosmicVoidBackground else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
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
