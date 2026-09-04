package space.iamjustkrishna.srutam.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.InsightStatus
import space.iamjustkrishna.srutam.ui.components.SquircleActionButton
import space.iamjustkrishna.srutam.ui.components.SrutamTopAppBar
import space.iamjustkrishna.srutam.ui.mesh.ConceptMeshCanvas
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
    var isMeshView by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(InsightsTab.NEXT_STEPS) }
    var showArchiveDialog by remember { mutableStateOf(false) }
    var isCompletedExpanded by remember { mutableStateOf(false) }

    val recordingsByPath by viewModel.recordingsByPath.collectAsState()
    val recordings = remember(recordingsByPath) { recordingsByPath.values.toList() }
    val allInsights by viewModel.allInsights.collectAsState()
    val activeActions by viewModel.activeActions.collectAsState()
    val allIdeas by viewModel.allIdeas.collectAsState()
    val allDecisions by viewModel.allDecisions.collectAsState()
    val themeClusters by viewModel.themeClusters.collectAsState()
    val archivedActionsCount by viewModel.archivedActionsCount.collectAsState()

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
                accentText = if (isMeshView) "Mesh" else "Insights",
                subtitle = if (isMeshView) "Interactive Concept Graph" else "Transcribed on this device",
                subtitleIcon = if (isMeshView) Icons.Default.Hub else Icons.Default.Lock,
                subtitleColor = if (isMeshView) CobaltBlue else Color(0xFF0F766E),
                actions = {
                    SquircleActionButton(
                        icon = if (isMeshView) Icons.Default.ViewAgenda else Icons.Default.Hub,
                        contentDescription = if (isMeshView) "Switch to List View" else "Explore 2D Concept Mesh",
                        onClick = { isMeshView = !isMeshView },
                        tint = if (isMeshView) CobaltBlue else TextPrimary
                    )
                    if (!isMeshView && completedActions.isNotEmpty()) {
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
        if (isMeshView) {
            ConceptMeshCanvas(
                recordings = recordings,
                insights = allInsights,
                themes = themeClusters,
                onRecordingClick = onRecordingClick,
                onActionToggle = { viewModel.toggleActionComplete(it) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
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
                            onActionToggle = { viewModel.toggleActionComplete(it) },
                            onRecordingClick = onRecordingClick,
                            onArchiveClick = { showArchiveDialog = true },
                            onRestoreArchived = { viewModel.unarchiveAllActions() },
                            onDismissTheme = { viewModel.dismissTheme(it) },
                            onExploreMesh = { isMeshView = true }
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
    }

    if (showArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = {
                Text(
                    text = "Archive Completed Tasks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Archive ${completedActions.size} completed items? They will be hidden from your active task view. You can restore them anytime.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.archiveCompletedActions()
                        showArchiveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Archive", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showArchiveDialog = false }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CeramicWhite,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun SingleRowInsightsCapsule(
    selectedTab: InsightsTab,
    onTabSelected: (InsightsTab) -> Unit,
    nextStepsCount: Int,
    ideasCount: Int,
    decisionsCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CapsuleTabItem(
                label = "Next Steps",
                count = nextStepsCount,
                dotColor = CobaltBlue,
                isSelected = selectedTab == InsightsTab.NEXT_STEPS,
                onClick = { onTabSelected(InsightsTab.NEXT_STEPS) }
            )
            CapsuleTabItem(
                label = "Ideas",
                count = ideasCount,
                dotColor = Color(0xFFD97706),
                isSelected = selectedTab == InsightsTab.IDEAS,
                onClick = { onTabSelected(InsightsTab.IDEAS) }
            )
            CapsuleTabItem(
                label = "Decisions",
                count = decisionsCount,
                dotColor = Color(0xFF0D9488),
                isSelected = selectedTab == InsightsTab.DECISIONS,
                onClick = { onTabSelected(InsightsTab.DECISIONS) }
            )
        }
    }
}

@Composable
private fun CapsuleTabItem(
    label: String,
    count: Int,
    dotColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    if (isSelected) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(dotColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "$label · $count",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "$label · $count",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF334155),
                maxLines = 1,
                softWrap = false
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
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp
        )
        if (badgeText != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = badgeColor.copy(alpha = 0.12f),
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
            // Checkbox
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
                    color = if (isCompleted) TextMuted else TextPrimary,
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
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CeramicWhite.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, Color(0xFFDDD6FE)), // Subtle violet accent
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
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Surfaced across ${cluster.noteCount} notes",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF7C3AED)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (onExploreMesh != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF3E8FF),
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
                                    tint = Color(0xFF7C3AED),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Mesh",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7C3AED)
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
                            color = TextMuted
                        )
                    }
                }
            }

            if (cluster.sampleSnippets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF5F3FF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${cluster.sampleSnippets.first()}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4C1D95),
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
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CeramicWhite.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, Color(0xFFA7F3D0)), // Subtle emerald border
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

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OriginNotePill(
                    name = decision.recordingName,
                    onClick = onRecordingClick
                )
                Text(
                    text = formatDate(decision.createdAt),
                    fontSize = 11.sp,
                    color = TextMuted
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
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CeramicWhite.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, Color(0xFFFDE68A)), // Subtle amber border
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

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OriginNotePill(
                    name = idea.recordingName,
                    onClick = onRecordingClick
                )
                Text(
                    text = formatDate(idea.createdAt),
                    fontSize = 11.sp,
                    color = TextMuted
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
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SlateGrouped,
        border = BorderStroke(1.dp, SlateBorder),
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
                tint = CobaltBlue,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = name.ifBlank { "Voice Note" },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NextStepsTab(
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
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Progress Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = CeramicWhite.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, SlateBorder),
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
                                color = TextPrimary
                            )
                            Text(
                                text = "$completedCount of $totalActiveCount items completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        if (completedCount > 0) {
                            Button(
                                onClick = onArchiveClick,
                                colors = ButtonDefaults.buttonColors(containerColor = CobaltContainer),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "Archive ($completedCount)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnCobaltContainer
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
                        color = CobaltBlue,
                        trackColor = SlateGrouped
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
                    badgeColor = CobaltBlue
                )
            }
        }

        if (pendingActions.isEmpty() && completedActions.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CeramicWhite.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.TaskAlt,
                            contentDescription = null,
                            tint = CobaltBlue,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No action items yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Commitments and to-dos mentioned in voice notes will appear here automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
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
                        color = SlateSurface,
                        border = BorderStroke(1.dp, SlateBorder),
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
                                color = TextSecondary
                            )
                            Icon(
                                imageVector = if (isCompletedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TextSecondary,
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
                    color = SlateSurface,
                    border = BorderStroke(1.dp, SlateBorder),
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
                            color = TextSecondary
                        )
                        TextButton(
                            onClick = onRestoreArchived,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Restore all",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CobaltBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdeasStreamTab(
    ideas: List<InsightEntity>,
    onRecordingClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFEF3C7).copy(alpha = 0.6f),
                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Connected Ideas",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "Distinct proposals and thoughts distilled from your voice notes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB45309)
                        )
                    }
                }
            }
        }

        if (ideas.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CeramicWhite.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No ideas captured yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Speak freely about creative proposals, brainstorms, and new plans in your voice notes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
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
private fun DecisionsTimelineTab(
    decisions: List<InsightEntity>,
    onRecordingClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = EmeraldContainer.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Decisions & Agreements",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = OnEmeraldContainer
                        )
                        Text(
                            text = "Explicit commitments and conclusions made across your voice notes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF047857)
                        )
                    }
                }
            }
        }

        if (decisions.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CeramicWhite.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No decisions logged yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "State your choices, conclusions, and agreements in your recordings to track them here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
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
