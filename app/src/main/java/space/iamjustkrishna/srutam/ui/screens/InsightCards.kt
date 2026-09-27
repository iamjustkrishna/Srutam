@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package space.iamjustkrishna.srutam.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.iamjustkrishna.srutam.data.*
import space.iamjustkrishna.srutam.ui.theme.*
import space.iamjustkrishna.srutam.utils.InsightSource
import space.iamjustkrishna.srutam.viewmodel.InsightsUiState
import space.iamjustkrishna.srutam.viewmodel.ThemeCluster
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun InsightSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = LocalIsCosmicDark.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (dark) CosmicVoidCard else CeramicWhite,
        contentColor = if (dark) TextOnDarkPrimary else TextPrimary,
        border = BorderStroke(1.dp, if (dark) CosmicVoidCardBorder else SlateBorder),
        shadowElevation = if (dark) 0.dp else 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
internal fun SingleRowInsightsCapsule(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    ideasCount: Int,
    nextStepsCount: Int,
    decisionsCount: Int,
    modifier: Modifier = Modifier
) {
    val dark = LocalIsCosmicDark.current
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (dark) CosmicVoidCard else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, if (dark) CosmicVoidCardBorder else SlateBorder),
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Triple("IDEAS", "Ideas", ideasCount),
                Triple("NEXT_STEPS", "Next Steps", nextStepsCount),
                Triple("DECISIONS", "Decisions", decisionsCount)
            )
            tabs.forEach { (key, label, count) ->
                val isSelected = selectedTab == key
                Surface(
                    shape = RoundedCornerShape(19.dp),
                    color = if (isSelected) {
                        if (dark) DarkSurfaceCard else CeramicWhite
                    } else Color.Transparent,
                    shadowElevation = if (isSelected && !dark) 2.dp else 0.dp,
                    border = if (isSelected) BorderStroke(
                        1.dp,
                        if (dark) CosmicGlowBlue.copy(alpha = 0.35f) else Color(0xFFD6E0EC)
                    ) else null,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics { this.selected = isSelected }
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = { onTabSelected(key) }
                        )
                        .testTag("tab_$key")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$label ($count)",
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                if (dark) TextOnDarkPrimary else TextPrimary
                            } else {
                                if (dark) TextOnDarkSecondary else TextMuted
                            },
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ThematicFilterChipsRow(
    themes: List<ThemeCluster>,
    selectedTheme: String?,
    onSelectTheme: (String?) -> Unit,
    onDismissTheme: (String) -> Unit,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val dark = LocalIsCosmicDark.current
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "All" chip
        val isAllSelected = selectedTheme == null
        Surface(
            onClick = { onSelectTheme(null) },
            shape = RoundedCornerShape(14.dp),
            color = if (isAllSelected) {
                if (dark) DarkSurfaceCard else CeramicWhite
            } else {
                if (dark) CosmicVoidCard else Color(0xFFF1F5F9)
            },
            border = BorderStroke(
                1.dp,
                if (isAllSelected) (if (dark) CosmicGlowBlue else CobaltBlue) else (if (dark) CosmicVoidCardBorder else SlateBorder)
            ),
            shadowElevation = if (isAllSelected && !dark) 1.dp else 0.dp
        ) {
            Text(
                text = "All ($totalCount)",
                fontSize = 12.sp,
                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isAllSelected) (if (dark) CosmicGlowBlue else CobaltBlue) else (if (dark) TextOnDarkSecondary else TextSecondary),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Theme chips
        themes.forEach { theme ->
            val isThemeSelected = selectedTheme == theme.key
            Surface(
                onClick = { onSelectTheme(if (isThemeSelected) null else theme.key) },
                shape = RoundedCornerShape(14.dp),
                color = if (isThemeSelected) {
                    if (dark) DarkSurfaceCard else CeramicWhite
                } else {
                    if (dark) CosmicVoidCard else Color(0xFFF1F5F9)
                },
                border = BorderStroke(
                    1.dp,
                    if (isThemeSelected) (if (dark) CosmicGlowBlue else CobaltBlue) else (if (dark) CosmicVoidCardBorder else SlateBorder)
                ),
                shadowElevation = if (isThemeSelected && !dark) 1.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✦ ${theme.title} (${theme.noteCount})",
                        fontSize = 12.sp,
                        fontWeight = if (isThemeSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isThemeSelected) (if (dark) CosmicGlowBlue else CobaltBlue) else (if (dark) TextOnDarkSecondary else TextSecondary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss theme ${theme.title}",
                        tint = (if (dark) TextOnDarkSecondary else TextMuted).copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .clickable { onDismissTheme(theme.key) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun InsightPillBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
internal fun ExpandInsightsRow(
    title: String,
    detail: String,
    expanded: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null
) {
    val dark = LocalIsCosmicDark.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (dark) CosmicVoidCard else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (dark) CosmicVoidCardBorder else SlateBorder),
        contentColor = if (dark) TextOnDarkPrimary else TextPrimary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = if (dark) TextOnDarkSecondary else TextSecondary)
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = if (dark) TextOnDarkSecondary.copy(alpha = 0.7f) else TextMuted)
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                tint = if (dark) TextOnDarkSecondary else TextSecondary
            )
        }
    }
}

@Composable
internal fun InsightSourceChip(
    id: Long,
    state: InsightsUiState,
    open: (Long) -> Unit
) {
    val dark = LocalIsCosmicDark.current
    val source = state.sources[id] ?: InsightSource(id, "Source note", false)
    Surface(
        onClick = { open(id) },
        enabled = source.available,
        shape = RoundedCornerShape(8.dp),
        color = if (dark) CosmicVoidBackground.copy(alpha = 0.7f) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, if (dark) CosmicVoidCardBorder else Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = if (dark) CosmicGlowBlue else CobaltBlue,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = source.label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = if (dark) TextOnDarkSecondary else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun InsightTaskCard(
    item: InsightEntity,
    state: InsightsUiState,
    actions: InsightsActions,
    open: (Long) -> Unit
) {
    val dark = LocalIsCosmicDark.current
    val isCompleted = item.status == InsightStatus.COMPLETED

    InsightSurface {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { actions.toggle(item.id) },
                colors = CheckboxDefaults.colors(
                    checkedColor = if (dark) CosmicAuroraGreen else EmeraldSuccess,
                    checkmarkColor = Color.White
                ),
                modifier = Modifier
                    .padding(end = 4.dp)
                    .testTag("task_toggle_${item.id}")
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 10.dp)
            ) {
                Text(
                    text = item.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isCompleted) {
                        (if (dark) TextOnDarkSecondary else TextMuted)
                    } else {
                        (if (dark) TextOnDarkPrimary else TextPrimary)
                    },
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InsightSourceChip(item.recordingId, state, open)
                    Text(
                        text = formatHumanRelativeDate(item.createdAt),
                        fontSize = 11.sp,
                        color = if (dark) TextOnDarkSecondary.copy(alpha = 0.6f) else TextMuted
                    )
                }
            }
        }
    }
}

@Composable
internal fun EmptyInsights(title: String, text: String) {
    val dark = LocalIsCosmicDark.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (dark) CosmicVoidCard else Color(0xFFF1F5F9),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.TaskAlt,
                    contentDescription = null,
                    tint = if (dark) CosmicGlowBlue else CobaltBlue,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (dark) TextOnDarkPrimary else TextPrimary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (dark) TextOnDarkSecondary else TextSecondary
        )
    }
}

internal fun reminderDate(item: ReminderEntity): String = when {
    item.needsReview && item.legacyReview -> "Date needs review · previous estimate unverified"
    item.timePrecision == "UNKNOWN" -> "Date needs review"
    item.timePrecision == "DATE_ONLY" -> "${item.localDate ?: "Date needs review"} · time not set"
    item.eventTimeMs != null -> runCatching {
        val zone = ZoneId.of(item.zoneId ?: ZoneId.systemDefault().id)
        Instant.ofEpochMilli(item.eventTimeMs).atZone(zone).format(DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a z", Locale.getDefault()))
    }.getOrDefault("Date needs review")
    else -> "Date needs review"
}

@Composable
internal fun ReminderSummary(
    item: ReminderEntity,
    state: InsightsUiState,
    open: (Long) -> Unit,
    onReview: () -> Unit,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
    onDisable: () -> Unit,
    onConvert: () -> Unit
) {
    val dark = LocalIsCosmicDark.current
    val isMilestone = item.type == ReminderType.MILESTONE
    val badgeLabel = if (isMilestone) "TARGET DATE" else "SCHEDULED EVENT"
    val badgeContainer = if (isMilestone) {
        if (dark) Color(0xFF78350F).copy(alpha = 0.4f) else Color(0xFFFEF3C7)
    } else {
        if (dark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFDBEAFE)
    }
    val badgeContent = if (isMilestone) {
        if (dark) StardustGold else Color(0xFFD97706)
    } else {
        if (dark) CosmicGlowBlue else CobaltBlue
    }

    InsightSurface {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            InsightPillBadge(
                text = badgeLabel,
                containerColor = badgeContainer,
                contentColor = badgeContent
            )
            Text(
                text = if (item.notificationEnabled) "🔔 Alert on" else "Alert off",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (dark) TextOnDarkSecondary else TextMuted
            )
        }

        Text(
            text = item.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (dark) TextOnDarkPrimary else TextPrimary
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = if (dark) TextOnDarkSecondary else TextMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = reminderDate(item),
                fontSize = 13.sp,
                color = if (dark) TextOnDarkSecondary else TextSecondary
            )
        }

        if (item.type == ReminderType.MILESTONE && item.localDate != null && runCatching {
                LocalDate.parse(item.localDate).isBefore(
                    Instant.ofEpochMilli(state.now)
                        .atZone(ZoneId.of(item.zoneId ?: ZoneId.systemDefault().id))
                        .toLocalDate()
                )
            }.getOrDefault(false)) {
            Text("Overdue", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        item.scheduleError?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            InsightSourceChip(item.recordingId, state, open)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onReview, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                    Text(if (item.needsReview) "Review" else "Edit", fontSize = 12.sp)
                }
                TextButton(onClick = onDone, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("Done", fontSize = 12.sp)
                }
                TextButton(onClick = onDismiss, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("Dismiss", fontSize = 12.sp)
                }
                TextButton(onClick = onConvert, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("Create task", fontSize = 12.sp)
                }
            }
        }
    }
}
