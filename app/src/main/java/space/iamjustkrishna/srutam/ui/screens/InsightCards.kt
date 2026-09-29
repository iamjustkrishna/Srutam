@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)
package space.iamjustkrishna.srutam.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
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
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
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
            // Tab data with colored dot indicators matching main branch colors
            data class TabInfo(val key: String, val label: String, val count: Int, val dotColor: Color)
            val tabs = listOf(
                TabInfo("IDEAS", "Ideas", ideasCount, Color(0xFFD97706)),       // Amber
                TabInfo("NEXT_STEPS", "Next Steps", nextStepsCount, CobaltBlue), // Blue
                TabInfo("DECISIONS", "Decisions", decisionsCount, Color(0xFF0D9488)) // Teal
            )
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab.key
                Surface(
                    shape = RoundedCornerShape(19.dp),
                    color = if (isSelected) {
                        if (dark) DarkSurfaceCard else CeramicWhite
                    } else Color.Transparent,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                    border = if (isSelected) BorderStroke(
                        1.dp,
                        if (dark) tab.dotColor.copy(alpha = 0.35f) else tab.dotColor.copy(alpha = 0.18f)
                    ) else null,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics { this.selected = isSelected }
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(tab.key) }
                        )
                        .testTag("tab_${tab.key}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Colored dot indicator with glowing halo when selected
                        if (isSelected) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(tab.dotColor.copy(alpha = 0.22f), CircleShape)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(tab.dotColor, CircleShape)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(tab.dotColor.copy(alpha = 0.45f), CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${tab.label} · ${tab.count}",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                if (dark) TextOnDarkPrimary else Color(0xFF0F172A)
                            } else {
                                if (dark) TextOnDarkSecondary else TextMuted
                            },
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
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
    expanded: Boolean,
    onClick: () -> Unit,
    detail: String? = null,
    subtitle: String? = null,
    badges: (@Composable () -> Unit)? = null
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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (badges != null) {
                    badges()
                } else if (detail != null) {
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = if (dark) TextOnDarkSecondary else TextSecondary)
                }
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
    open: (Long) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val dark = LocalIsCosmicDark.current
    val source = state.sources[id] ?: InsightSource(id, "Source note", false)
    val displayLabel = if (compact && source.label.startsWith("Voice note · ")) {
        "Voice note"
    } else {
        source.label
    }
    Surface(
        onClick = { open(id) },
        enabled = source.available,
        shape = RoundedCornerShape(8.dp),
        color = if (dark) CosmicVoidBackground.copy(alpha = 0.7f) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, if (dark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = if (dark) CosmicGlowBlue else CobaltBlue,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = displayLabel,
                fontSize = 10.5.sp,
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
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isLocallyDone by remember(item.id, item.status) {
        mutableStateOf(item.status == InsightStatus.COMPLETED)
    }

    val isCompleted = isLocallyDone || item.status == InsightStatus.COMPLETED

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (dark) CosmicVoidCard else CeramicWhite,
        contentColor = if (dark) TextOnDarkPrimary else TextPrimary,
        border = BorderStroke(
            1.dp,
            if (isCompleted) {
                if (dark) CosmicAuroraGreen.copy(alpha = 0.4f) else EmeraldSuccess.copy(alpha = 0.4f)
            } else {
                if (dark) CosmicVoidCardBorder else SlateBorder
            }
        ),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Checkbox + Action Text
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    Checkbox(
                        checked = isCompleted,
                        onCheckedChange = { checked ->
                            if (item.status == InsightStatus.OPEN && checked) {
                                // Satisfying completion: immediate green checkmark + strikethrough + haptics
                                isLocallyDone = true
                                runCatching { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                scope.launch {
                                    delay(550)
                                    actions.toggle(item.id)
                                }
                            } else {
                                isLocallyDone = checked
                                actions.toggle(item.id)
                            }
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = if (dark) CosmicAuroraGreen else EmeraldSuccess,
                            checkmarkColor = Color.White,
                            uncheckedColor = if (dark) TextOnDarkSecondary.copy(alpha = 0.6f) else TextMuted
                        ),
                        modifier = Modifier
                            .padding(top = 1.dp, end = 10.dp)
                            .testTag("task_toggle_${item.id}")
                    )
                }
                Text(
                    text = item.text,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp,
                    color = if (isCompleted) {
                        (if (dark) TextOnDarkSecondary else TextMuted)
                    } else {
                        (if (dark) TextOnDarkPrimary else TextPrimary)
                    },
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: Metadata Footer (Source Chip on left, Relative Timestamp on right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 30.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InsightSourceChip(
                    id = item.recordingId,
                    state = state,
                    open = open,
                    compact = true,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatHumanRelativeDate(item.createdAt),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (dark) TextOnDarkSecondary.copy(alpha = 0.6f) else TextMuted,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
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

internal fun formatGlanceReminderTiming(
    item: ReminderEntity,
    nowMs: Long = System.currentTimeMillis()
): String {
    if (item.needsReview && item.legacyReview) return "Review date"

    val zone = runCatching {
        ZoneId.of(item.zoneId ?: ZoneId.systemDefault().id)
    }.getOrDefault(ZoneId.systemDefault())

    val today = runCatching {
        Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
    }.getOrDefault(LocalDate.now())

    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    // 1. If eventTimeMs is available
    if (item.eventTimeMs != null) {
        return runCatching {
            val zdt = Instant.ofEpochMilli(item.eventTimeMs).atZone(zone)
            val date = zdt.toLocalDate()
            val timeStr = zdt.format(timeFormatter)

            if (item.type == ReminderType.MILESTONE || !item.notificationEnabled) {
                when (date) {
                    today -> "Target: Today"
                    today.plusDays(1) -> "Target: Tomorrow"
                    else -> "Target: ${date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))}"
                }
            } else {
                when (date) {
                    today -> "Today, $timeStr"
                    today.plusDays(1) -> "Tomorrow, $timeStr"
                    today.minusDays(1) -> "Yesterday, $timeStr"
                    else -> if (date.year == today.year) {
                        zdt.format(DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.getDefault()))
                    } else {
                        zdt.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
                    }
                }
            }
        }.getOrElse { "Scheduled" }
    }

    // 2. If localDate is available
    if (item.localDate != null) {
        return runCatching {
            val date = LocalDate.parse(item.localDate)
            val parsedTimeStr = item.localTime?.let {
                runCatching {
                    LocalTime.parse(it).format(timeFormatter)
                }.getOrNull()
            }

            if (item.type == ReminderType.MILESTONE || !item.notificationEnabled) {
                when (date) {
                    today -> "Target: Today"
                    today.plusDays(1) -> "Target: Tomorrow"
                    else -> "Target: ${date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))}"
                }
            } else {
                when (date) {
                    today -> if (parsedTimeStr != null) "Today, $parsedTimeStr" else "Today"
                    today.plusDays(1) -> if (parsedTimeStr != null) "Tomorrow, $parsedTimeStr" else "Tomorrow"
                    else -> {
                        val base = date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
                        if (parsedTimeStr != null) "$base, $parsedTimeStr" else base
                    }
                }
            }
        }.getOrElse { item.originalText.ifBlank { "Scheduled" } }
    }

    return item.originalText.ifBlank { "Scheduled" }
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (dark) TextOnDarkSecondary else TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = reminderDate(item),
                    fontSize = 12.5.sp,
                    color = if (dark) TextOnDarkSecondary else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            InsightSourceChip(
                id = item.recordingId,
                state = state,
                open = open,
                modifier = Modifier.weight(1f, fill = false)
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

        HorizontalDivider(
            color = if (dark) CosmicVoidCardBorder.copy(alpha = 0.5f) else Color(0xFFF1F5F9),
            thickness = 1.dp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReminderActionButton(
                text = if (item.needsReview) "Review" else "Edit",
                onClick = onReview,
                isPrimary = item.needsReview,
                modifier = Modifier.weight(1f)
            )
            ReminderActionButton(
                text = "Done",
                onClick = onDone,
                modifier = Modifier.weight(1f)
            )
            ReminderActionButton(
                text = "Dismiss",
                onClick = onDismiss,
                isDestructive = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ReminderActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    isDestructive: Boolean = false
) {
    val dark = LocalIsCosmicDark.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = when {
            isPrimary -> if (dark) CobaltBlue.copy(alpha = 0.35f) else Color(0xFFEFF6FF)
            isDestructive -> if (dark) Color(0xFF7F1D1D).copy(alpha = 0.25f) else Color(0xFFFEF2F2)
            else -> if (dark) CosmicVoidBackground.copy(alpha = 0.6f) else Color(0xFFF1F5F9)
        },
        border = BorderStroke(
            1.dp,
            when {
                isPrimary -> if (dark) CosmicGlowBlue.copy(alpha = 0.4f) else Color(0xFFBFDBFE)
                isDestructive -> if (dark) Color(0xFFEF4444).copy(alpha = 0.3f) else Color(0xFFFECACA)
                else -> if (dark) CosmicVoidCardBorder else Color(0xFFE2E8F0)
            }
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 11.5.sp,
                fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Medium,
                color = when {
                    isPrimary -> if (dark) CosmicGlowBlue else CobaltBlue
                    isDestructive -> if (dark) Color(0xFFF87171) else Color(0xFFDC2626)
                    else -> if (dark) TextOnDarkPrimary else TextPrimary
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun CompactRemindersGlanceBar(
    reminders: List<ReminderEntity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryReminder: ReminderEntity? = null,
    nowMs: Long = System.currentTimeMillis(),
    expanded: Boolean = false
) {
    val primary = primaryReminder
        ?: reminders.minByOrNull { it.eventTimeMs ?: Long.MAX_VALUE }
        ?: reminders.firstOrNull()
        ?: return

    val dark = LocalIsCosmicDark.current
    val isMilestone = primary.type == ReminderType.MILESTONE || !primary.notificationEnabled
    val timingText = remember(primary, nowMs) { formatGlanceReminderTiming(primary, nowMs) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (dark) CosmicVoidCard else Color.White,
        border = BorderStroke(1.dp, if (dark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
        shadowElevation = if (dark) 0.dp else 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                contentDescription = "Dates & reminders"
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        if (isMilestone) {
                            if (dark) Color(0xFF78350F).copy(alpha = 0.35f) else Color(0xFFFEF3C7)
                        } else {
                            if (dark) CobaltBlue.copy(alpha = 0.25f) else Color(0xFFDBEAFE)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMilestone) Icons.Default.Flag else Icons.Default.Notifications,
                    contentDescription = null,
                    tint = if (isMilestone) {
                        if (dark) StardustGold else Color(0xFFD97706)
                    } else {
                        if (dark) CosmicGlowBlue else CobaltBlue
                    },
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (expanded) {
                Text(
                    text = "Dates & reminders (${reminders.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (dark) TextOnDarkPrimary else TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    text = timingText,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isMilestone) {
                        if (dark) StardustGold else Color(0xFFD97706)
                    } else {
                        if (dark) CosmicGlowBlue else CobaltBlue
                    },
                    maxLines = 1
                )

                Text(
                    text = "•",
                    fontSize = 11.sp,
                    color = if (dark) TextOnDarkSecondary.copy(alpha = 0.5f) else TextMuted.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                Text(
                    text = primary.title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (dark) TextOnDarkPrimary else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (reminders.size > 1) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (dark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = "+${reminders.size - 1} more",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (dark) TextOnDarkSecondary else TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse reminders" else "Expand reminders",
                tint = if (dark) TextOnDarkSecondary else TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
internal fun ReminderHorizontalCarousel(
    reminders: List<ReminderEntity>,
    state: InsightsUiState,
    open: (Long) -> Unit,
    onReview: (ReminderEntity) -> Unit,
    onDone: (ReminderEntity) -> Unit,
    onDismiss: (ReminderEntity) -> Unit,
    title: String = "Scheduled & Target Dates",
    modifier: Modifier = Modifier
) {
    if (reminders.isEmpty()) return
    val dark = LocalIsCosmicDark.current
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(listState)
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val cardWidth = remember(screenWidth) { (screenWidth - 32.dp) * 0.84f }

    val activeIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo.minByOrNull { item ->
                kotlin.math.abs((item.offset + item.size / 2) - center)
            }?.index ?: 0
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = if (dark) CosmicGlowBlue else CobaltBlue,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "$title (${reminders.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = if (dark) TextOnDarkPrimary else TextPrimary
                )
            }
            if (reminders.size > 1) {
                Text(
                    text = "${(activeIndex + 1).coerceAtMost(reminders.size)} of ${reminders.size}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (dark) TextOnDarkSecondary else TextMuted
                )
            }
        }

        LazyRow(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reminders_horizontal_carousel")
        ) {
            itemsIndexed(reminders, key = { _, it -> it.id }) { index, reminder ->
                ReminderHorizontalCard(
                    item = reminder,
                    state = state,
                    open = open,
                    onReview = { onReview(reminder) },
                    onDone = { onDone(reminder) },
                    onDismiss = { onDismiss(reminder) },
                    modifier = Modifier.width(cardWidth)
                )
            }
        }
    }
}

@Composable
internal fun ReminderHorizontalCard(
    item: ReminderEntity,
    state: InsightsUiState,
    open: (Long) -> Unit,
    onReview: () -> Unit,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
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

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (dark) CosmicVoidCard else Color.White,
        border = BorderStroke(1.dp, if (dark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
        shadowElevation = if (dark) 0.dp else 1.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (dark) TextOnDarkPrimary else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (dark) TextOnDarkSecondary else TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = reminderDate(item),
                        fontSize = 12.sp,
                        color = if (dark) TextOnDarkSecondary else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                InsightSourceChip(
                    id = item.recordingId,
                    state = state,
                    open = open,
                    compact = true,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            HorizontalDivider(
                color = if (dark) CosmicVoidCardBorder.copy(alpha = 0.5f) else Color(0xFFF1F5F9),
                thickness = 1.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReminderActionButton(
                    text = if (item.needsReview) "Review" else "Edit",
                    onClick = onReview,
                    isPrimary = item.needsReview,
                    modifier = Modifier.weight(1f)
                )
                ReminderActionButton(
                    text = "Done",
                    onClick = onDone,
                    modifier = Modifier.weight(1f)
                )
                ReminderActionButton(
                    text = "Dismiss",
                    onClick = onDismiss,
                    isDestructive = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
