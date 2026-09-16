package space.iamjustkrishna.srutam.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.InsightStatus
import space.iamjustkrishna.srutam.data.ReminderEntity
import space.iamjustkrishna.srutam.data.ReminderStatus
import space.iamjustkrishna.srutam.ui.components.SquircleActionButton
import space.iamjustkrishna.srutam.ui.components.SrutamTopAppBar
import space.iamjustkrishna.srutam.ui.components.ArchiveTasksDialog
import space.iamjustkrishna.srutam.ui.theme.*
import space.iamjustkrishna.srutam.viewmodel.AudioFilesViewModel
import space.iamjustkrishna.srutam.viewmodel.ThemeCluster

enum class InsightsTab(val label: String) {
    NEXT_STEPS("Next Steps"),
    IDEAS("Ideas"),
    DECISIONS("Decisions")
}

@Composable
fun ActionItemsScreen(
    onRecordingClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: AudioFilesViewModel,
    modifier: Modifier = Modifier
) {
    val recordingsByPath by viewModel.recordingsByPath.collectAsState()
    val activeActions by viewModel.activeActions.collectAsState()
    val allIdeas by viewModel.allIdeas.collectAsState()
    val allDecisions by viewModel.allDecisions.collectAsState()
    val themeClusters by viewModel.themeClusters.collectAsState()
    val archivedActionsCount by viewModel.archivedActionsCount.collectAsState()
    val upcomingReminders by viewModel.upcomingReminders.collectAsState()

    ActionItemsContent(
        activeActions = activeActions,
        allIdeas = allIdeas,
        allDecisions = allDecisions,
        themeClusters = themeClusters,
        archivedActionsCount = archivedActionsCount,
        upcomingReminders = upcomingReminders,
        onRecordingClick = onRecordingClick,
        onSettingsClick = onSettingsClick,
        onActionToggle = { viewModel.toggleActionComplete(it) },
        onReminderComplete = { viewModel.updateReminderStatus(it, ReminderStatus.COMPLETED) },
        onArchiveConfirmed = { viewModel.archiveCompletedActions() },
        onRestoreArchived = { viewModel.unarchiveAllActions() },
        onDismissTheme = { viewModel.dismissTheme(it) },
        modifier = modifier
    )
}

@Composable
fun ActionItemsContent(
    activeActions: List<InsightEntity>,
    allIdeas: List<InsightEntity>,
    allDecisions: List<InsightEntity>,
    themeClusters: List<ThemeCluster> = emptyList(),
    archivedActionsCount: Int = 0,
    upcomingReminders: List<ReminderEntity> = emptyList(),
    initialTab: InsightsTab = InsightsTab.NEXT_STEPS,
    onRecordingClick: (Long) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onActionToggle: (InsightEntity) -> Unit = {},
    onReminderComplete: (String) -> Unit = {},
    onArchiveConfirmed: () -> Unit = {},
    onRestoreArchived: () -> Unit = {},
    onDismissTheme: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    var showArchiveDialog by remember { mutableStateOf(false) }
    var isCompletedExpanded by remember { mutableStateOf(false) }

    val pendingActions = remember(activeActions) {
        activeActions.filter { it.status == InsightStatus.OPEN }
    }
    val completedActions = remember(activeActions) {
        activeActions.filter { it.status == InsightStatus.COMPLETED }
    }
    val totalActiveActions = activeActions.size
    val completedActionsCount = completedActions.size
    val progressFraction = if (totalActiveActions > 0) completedActionsCount.toFloat() / totalActiveActions else 0f

    Scaffold(
        topBar = {
            SrutamTopAppBar(
                title = "Srutam",
                accentText = "Insights",
                subtitle = "Transcribed on this device",
                subtitleIcon = Icons.Default.Lock,
                subtitleColor = Color(0xFF0F766E),
                actions = {
                    if (completedActions.isNotEmpty()) {
                        SquircleActionButton(
                            icon = Icons.Default.Archive,
                            contentDescription = "Archive completed actions",
                            onClick = { showArchiveDialog = true },
                            tint = CobaltBlue
                        )
                    }
                    SquircleActionButton(
                        icon = Icons.Default.Settings,
                        contentDescription = "Settings",
                        onClick = onSettingsClick
                    )
                }
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (upcomingReminders.isNotEmpty()) {
                UpcomingRemindersSection(
                    reminders = upcomingReminders,
                    onReminderClick = onRecordingClick,
                    onReminderComplete = onReminderComplete
                )
            }

            // Sleek Single-Row Connected Intelligence Capsule
            SingleRowInsightsCapsule(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                nextStepsCount = pendingActions.size,
                ideasCount = allIdeas.size,
                decisionsCount = allDecisions.size,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Content Area based on Selected Tab
            when (selectedTab) {
                InsightsTab.NEXT_STEPS -> {
                    NextStepsTab(
                        pendingActions = pendingActions,
                        completedActions = completedActions,
                        themeClusters = themeClusters,
                        totalActiveCount = totalActiveActions,
                        completedCount = completedActionsCount,
                        progressFraction = progressFraction,
                        archivedCount = archivedActionsCount,
                        isCompletedExpanded = isCompletedExpanded,
                        onToggleCompletedExpanded = { isCompletedExpanded = !isCompletedExpanded },
                        onActionToggle = onActionToggle,
                        onRecordingClick = onRecordingClick,
                        onArchiveClick = { showArchiveDialog = true },
                        onRestoreArchived = onRestoreArchived,
                        onDismissTheme = onDismissTheme
                    )
                }
                InsightsTab.IDEAS -> {
                    IdeasStreamTab(
                        ideas = allIdeas,
                        onRecordingClick = onRecordingClick
                    )
                }
                InsightsTab.DECISIONS -> {
                    DecisionsTimelineTab(
                        decisions = allDecisions,
                        onRecordingClick = onRecordingClick
                    )
                }
            }
        }
    }

    if (showArchiveDialog) {
        ArchiveTasksDialog(
            taskCount = completedActions.size,
            onConfirm = {
                onArchiveConfirmed()
                showArchiveDialog = false
            },
            onDismiss = { showArchiveDialog = false }
        )
    }
}

@Composable
internal fun SingleRowInsightsCapsule(
    selectedTab: InsightsTab,
    onTabSelected: (InsightsTab) -> Unit,
    nextStepsCount: Int,
    ideasCount: Int,
    decisionsCount: Int,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(
                color = if (isDark) CosmicVoidCard else Color(0xFFF1F5F9),
                shape = RoundedCornerShape(19.dp)
            )
            .border(
                1.dp,
                if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0),
                RoundedCornerShape(19.dp)
            )
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CapsuleTabItem(
            label = "Next Steps",
            count = nextStepsCount,
            dotColor = CobaltBlue,
            gradientColors = listOf(
                Color(0xFF2563EB).copy(alpha = 0.18f),
                Color(0xFF3B82F6).copy(alpha = 0.08f)
            ),
            isSelected = selectedTab == InsightsTab.NEXT_STEPS,
            onClick = { onTabSelected(InsightsTab.NEXT_STEPS) },
            modifier = Modifier.weight(1f)
        )
        CapsuleTabItem(
            label = "Ideas",
            count = ideasCount,
            dotColor = Color(0xFFD97706),
            gradientColors = listOf(
                Color(0xFFD97706).copy(alpha = 0.18f),
                Color(0xFFF59E0B).copy(alpha = 0.08f)
            ),
            isSelected = selectedTab == InsightsTab.IDEAS,
            onClick = { onTabSelected(InsightsTab.IDEAS) },
            modifier = Modifier.weight(1f)
        )
        CapsuleTabItem(
            label = "Decisions",
            count = decisionsCount,
            dotColor = Color(0xFF0D9488),
            gradientColors = listOf(
                Color(0xFF0D9488).copy(alpha = 0.18f),
                Color(0xFF14B8A6).copy(alpha = 0.08f)
            ),
            isSelected = selectedTab == InsightsTab.DECISIONS,
            onClick = { onTabSelected(InsightsTab.DECISIONS) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CapsuleTabItem(
    label: String,
    count: Int,
    dotColor: Color,
    gradientColors: List<Color>,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .then(
                if (isSelected) {
                    Modifier
                        .background(
                            brush = Brush.horizontalGradient(gradientColors),
                            shape = shape
                        )
                        .border(
                            BorderStroke(1.dp, dotColor.copy(alpha = 0.32f)),
                            shape = shape
                        )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            if (isSelected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(12.dp)
                        .background(dotColor.copy(alpha = 0.22f), CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(dotColor, CircleShape)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(dotColor.copy(alpha = 0.45f), CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "$label · $count",
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) {
                    if (LocalIsCosmicDark.current) TextOnDarkPrimary else Color(0xFF0F172A)
                } else {
                    if (LocalIsCosmicDark.current) TextOnDarkSecondary else TextSecondary
                },
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InsightsSectionHeader(
    title: String,
    badgeText: String?,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) TextOnDarkSecondary else TextSecondary,
            letterSpacing = 1.sp
        )
        if (badgeText != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = badgeColor.copy(alpha = if (isDark) 0.22f else 0.12f),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactActionCard(
    action: InsightEntity,
    onToggle: () -> Unit,
    onRecordingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val isCompleted = action.status == InsightStatus.COMPLETED

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) CosmicVoidCard else CeramicWhite.copy(alpha = 0.95f),
        border = BorderStroke(
            1.dp,
            if (isDark) {
                if (isCompleted) CosmicVoidCardBorder.copy(alpha = 0.5f) else CosmicVoidCardBorder
            } else {
                if (isCompleted) SlateBorder.copy(alpha = 0.5f) else SlateBorder
            }
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Checkbox
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isCompleted) {
                    if (isDark) CosmicAuroraGreen else EmeraldSuccess
                } else {
                    if (isDark) Color(0xFF1E293B) else SlateSurface
                },
                border = BorderStroke(
                    1.5.dp,
                    if (isCompleted) {
                        if (isDark) CosmicAuroraGreen else EmeraldSuccess
                    } else {
                        if (isDark) CosmicVoidCardBorder else Color(0xFFCBD5E1)
                    }
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
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCompleted) {
                        if (isDark) TextOnDarkSecondary.copy(alpha = 0.6f) else TextMuted
                    } else {
                        if (isDark) TextOnDarkPrimary else TextPrimary
                    },
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                Spacer(modifier = Modifier.height(6.dp))
                OriginNotePill(
                    name = action.recordingName,
                    onClick = onRecordingClick
                )
            }
        }
    }
}

@Composable
private fun RecurringThemeCard(
    cluster: ThemeCluster,
    onDismiss: () -> Unit,
    onRecordingClick: (Long) -> Unit,
    onExploreMesh: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) CosmicVoidCard else CeramicWhite.copy(alpha = 0.95f),
        border = BorderStroke(
            1.dp,
            if (isDark) Color(0xFF4C1D95).copy(alpha = 0.5f) else Color(0xFFDDD6FE)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cluster.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) TextOnDarkPrimary else TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Surfaced across ${cluster.noteCount} notes",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (onExploreMesh != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF2E1065).copy(alpha = 0.6f) else Color(0xFFF3E8FF),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onExploreMesh() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Hub,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFC4B5FD) else Color(0xFF7C3AED),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Mesh",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFC4B5FD) else Color(0xFF7C3AED)
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Not related",
                            fontSize = 11.sp,
                            color = if (isDark) TextOnDarkSecondary else TextMuted
                        )
                    }
                }
            }

            if (cluster.sampleSnippets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF16102E) else Color(0xFFF5F3FF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${cluster.sampleSnippets.first()}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFFDDD6FE) else Color(0xFF4C1D95),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                cluster.noteIds.zip(cluster.noteNames).take(3).forEach { (id, name) ->
                    OriginNotePill(
                        name = name,
                        onClick = { onRecordingClick(id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DecisionCard(
    decision: InsightEntity,
    onRecordingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) CosmicVoidCard else CeramicWhite.copy(alpha = 0.95f),
        border = BorderStroke(
            1.dp,
            if (isDark) Color(0xFF065F46).copy(alpha = 0.5f) else Color(0xFFA7F3D0)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF065F46).copy(alpha = 0.5f) else EmeraldContainer,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (isDark) CosmicAuroraGreen else EmeraldSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DECISION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFA7F3D0) else OnEmeraldContainer,
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = decision.text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDark) TextOnDarkPrimary else TextPrimary
            )

            if (!decision.rationale.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Rationale: ${decision.rationale}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) TextOnDarkSecondary else TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OriginNotePill(
                    name = decision.recordingName,
                    onClick = onRecordingClick,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = formatDate(decision.createdAt),
                    fontSize = 11.sp,
                    color = if (isDark) TextOnDarkSecondary else TextMuted,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun IdeaCard(
    idea: InsightEntity,
    onRecordingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) CosmicVoidCard else CeramicWhite.copy(alpha = 0.95f),
        border = BorderStroke(
            1.dp,
            if (isDark) Color(0xFF78350F).copy(alpha = 0.5f) else Color(0xFFFDE68A)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF78350F).copy(alpha = 0.5f) else Color(0xFFFEF3C7),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = if (isDark) StardustGold else Color(0xFFD97706),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "IDEA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) StardustGold else Color(0xFFB45309),
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = idea.text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) TextOnDarkPrimary else TextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OriginNotePill(
                    name = idea.recordingName,
                    onClick = onRecordingClick,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = formatDate(idea.createdAt),
                    fontSize = 11.sp,
                    color = if (isDark) TextOnDarkSecondary else TextMuted,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun OriginNotePill(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isDark) Color(0xFF1E293B) else SlateGrouped,
        border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else SlateBorder),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = name.ifBlank { "Voice Note" },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) TextOnDarkPrimary else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun NextStepsTab(
    pendingActions: List<InsightEntity>,
    completedActions: List<InsightEntity>,
    themeClusters: List<ThemeCluster>,
    totalActiveCount: Int,
    completedCount: Int,
    progressFraction: Float,
    archivedCount: Int,
    isCompletedExpanded: Boolean,
    onToggleCompletedExpanded: () -> Unit,
    onActionToggle: (InsightEntity) -> Unit,
    onRecordingClick: (Long) -> Unit,
    onArchiveClick: () -> Unit,
    onRestoreArchived: () -> Unit,
    onDismissTheme: (String) -> Unit,
    onExploreMesh: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Progress Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isDark) CosmicVoidCard else CeramicWhite.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Action Progress",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) TextOnDarkPrimary else TextPrimary
                            )
                            Text(
                                text = "$completedCount of $totalActiveCount items completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) TextOnDarkSecondary else TextSecondary
                            )
                        }
                        if (completedCount > 0) {
                            Button(
                                onClick = onArchiveClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFF1E293B) else CobaltContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "Archive ($completedCount)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) CosmicGlowBlue else OnCobaltContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isDark) CosmicGlowBlue else CobaltBlue,
                        trackColor = if (isDark) Color(0xFF1E293B) else SlateGrouped
                    )
                }
            }
        }

        // Optional: Themes You Keep Returning To
        if (themeClusters.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                InsightsSectionHeader(
                    title = "THEMES YOU KEEP RETURNING TO",
                    badgeText = "${themeClusters.size} active",
                    badgeColor = Color(0xFF8B5CF6)
                )
            }

            items(themeClusters, key = { it.key }) { cluster ->
                RecurringThemeCard(
                    cluster = cluster,
                    onDismiss = { onDismissTheme(cluster.key) },
                    onRecordingClick = onRecordingClick,
                    onExploreMesh = onExploreMesh
                )
            }
        }

        // Active Tasks Section
        if (pendingActions.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                InsightsSectionHeader(
                    title = "NEEDS ATTENTION",
                    badgeText = "${pendingActions.size} open",
                    badgeColor = if (isDark) CosmicGlowBlue else CobaltBlue
                )
            }
        }

        if (pendingActions.isEmpty() && completedActions.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) CosmicVoidCard else CeramicWhite.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1E293B) else CobaltContainer.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TaskAlt,
                                contentDescription = null,
                                tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No action items yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) TextOnDarkPrimary else TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Commitments and to-dos mentioned in voice notes will appear here automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) TextOnDarkSecondary else TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(pendingActions, key = { it.id }) { action ->
                CompactActionCard(
                    action = action,
                    onToggle = { onActionToggle(action) },
                    onRecordingClick = { onRecordingClick(action.recordingId) }
                )
            }

            // Completed Section Header
            if (completedActions.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF131B2E) else SlateSurface,
                        border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else SlateBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onToggleCompletedExpanded)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Completed (${completedActions.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) TextOnDarkSecondary else TextSecondary
                            )
                            Icon(
                                imageVector = if (isCompletedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = if (isDark) TextOnDarkSecondary else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (isCompletedExpanded) {
                    items(completedActions, key = { it.id }) { action ->
                        CompactActionCard(
                            action = action,
                            onToggle = { onActionToggle(action) },
                            onRecordingClick = { onRecordingClick(action.recordingId) }
                        )
                    }
                }
            }
        }

        // Restorable Archived Banner
        if (archivedCount > 0) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF131B2E) else SlateSurface,
                    border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$archivedCount tasks archived",
                            fontSize = 12.sp,
                            color = if (isDark) TextOnDarkSecondary else TextSecondary
                        )
                        TextButton(
                            onClick = onRestoreArchived,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Restore all",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) CosmicGlowBlue else CobaltBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun IdeasStreamTab(
    ideas: List<InsightEntity>,
    onRecordingClick: (Long) -> Unit,
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color(0xFF2E1A05).copy(alpha = 0.8f) else Color(0xFFFEF3C7).copy(alpha = 0.6f),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF78350F) else Color(0xFFFDE68A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = if (isDark) StardustGold else Color(0xFFD97706),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Connected Ideas",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                        )
                        Text(
                            text = "Distinct proposals and thoughts distilled from your voice notes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309)
                        )
                    }
                }
            }
        }

        if (ideas.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) CosmicVoidCard else CeramicWhite.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF2E1A05) else Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = if (isDark) StardustGold else Color(0xFFD97706),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No ideas captured yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) TextOnDarkPrimary else TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Speak freely about creative proposals, brainstorms, and new plans in your voice notes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) TextOnDarkSecondary else TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(ideas, key = { it.id }) { idea ->
                IdeaCard(
                    idea = idea,
                    onRecordingClick = { onRecordingClick(idea.recordingId) }
                )
            }
        }
    }
}

@Composable
internal fun DecisionsTimelineTab(
    decisions: List<InsightEntity>,
    onRecordingClick: (Long) -> Unit,
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color(0xFF06281E).copy(alpha = 0.8f) else EmeraldContainer.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF065F46) else Color(0xFFA7F3D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (isDark) CosmicAuroraGreen else EmeraldSuccess,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Decisions & Agreements",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFA7F3D0) else OnEmeraldContainer
                        )
                        Text(
                            text = "Explicit commitments and conclusions made across your voice notes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
                        )
                    }
                }
            }
        }

        if (decisions.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) CosmicVoidCard else CeramicWhite.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF06281E) else EmeraldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TaskAlt,
                                contentDescription = null,
                                tint = if (isDark) CosmicAuroraGreen else EmeraldSuccess,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No decisions logged yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) TextOnDarkPrimary else TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "State your choices, conclusions, and agreements in your recordings to track them here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) TextOnDarkSecondary else TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(decisions, key = { it.id }) { decision ->
                DecisionCard(
                    decision = decision,
                    onRecordingClick = { onRecordingClick(decision.recordingId) }
                )
            }
        }
    }
}

@Composable
fun UpcomingRemindersSection(
    reminders: List<ReminderEntity>,
    onReminderClick: (Long) -> Unit,
    onReminderComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsCosmicDark.current
    val cardBg = if (isDark) CosmicVoidCard else Color.White
    val cardBorder = if (isDark) CosmicVoidCardBorder else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) TextOnDarkPrimary else TextPrimary
    val textSecondary = if (isDark) TextOnDarkSecondary else TextSecondary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "UPCOMING EVENTS & REMINDERS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = textSecondary
                )
            }
            Surface(
                shape = CircleShape,
                color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFEFF6FF)
            ) {
                Text(
                    text = "${reminders.size}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) CosmicGlowBlue else CobaltBlue,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(reminders, key = { it.id }) { reminder ->
                ReminderCard(
                    reminder = reminder,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    isDark = isDark,
                    onClick = { onReminderClick(reminder.recordingId) },
                    onComplete = { onReminderComplete(reminder.id) }
                )
            }
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: ReminderEntity,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeColor = when (reminder.type.uppercase()) {
        "MEETING" -> if (isDark) CosmicGlowBlue else CobaltBlue
        "DEADLINE" -> Color(0xFFDC2626)
        "CALL" -> Color(0xFF0D9488)
        else -> Color(0xFFD97706)
    }

    Surface(
        modifier = modifier
            .width(260.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = typeColor.copy(alpha = if (isDark) 0.25f else 0.12f)
                ) {
                    Text(
                        text = reminder.type.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = typeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                IconButton(
                    onClick = onComplete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Mark done",
                        tint = textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = reminder.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = textSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatHumanRelativeDate(reminder.eventTimeMs),
                    fontSize = 11.sp,
                    color = textSecondary
                )
            }

            if (!reminder.person.isNullOrBlank() || !reminder.location.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    reminder.person?.let { person ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = person,
                                fontSize = 11.sp,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    reminder.location?.let { loc ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = loc,
                                fontSize = 11.sp,
                                color = textSecondary,
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

