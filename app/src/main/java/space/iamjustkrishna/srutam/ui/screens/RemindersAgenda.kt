@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package space.iamjustkrishna.srutam.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.iamjustkrishna.srutam.data.ReminderEntity
import space.iamjustkrishna.srutam.data.ReminderType
import space.iamjustkrishna.srutam.ui.theme.*
import space.iamjustkrishna.srutam.viewmodel.InsightsUiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal enum class AgendaBucket(val label: String) {
    OVERDUE("Overdue"),
    TODAY("Today"),
    TOMORROW("Tomorrow"),
    THIS_WEEK("This week"),
    LATER("Later"),
    UNDATED("No date")
}

private const val AgendaAll = "All"
private const val AgendaReminders = "Reminders"
private const val AgendaTargets = "Targets"

internal fun isTargetDate(item: ReminderEntity): Boolean =
    item.type == ReminderType.MILESTONE || !item.notificationEnabled

private fun zoneOf(item: ReminderEntity): ZoneId =
    runCatching { ZoneId.of(item.zoneId ?: ZoneId.systemDefault().id) }.getOrDefault(ZoneId.systemDefault())

internal fun reminderLocalDate(item: ReminderEntity): LocalDate? {
    item.eventTimeMs?.let {
        return runCatching { Instant.ofEpochMilli(it).atZone(zoneOf(item)).toLocalDate() }.getOrNull()
    }
    return item.localDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
}

internal fun agendaBucket(item: ReminderEntity, nowMs: Long): AgendaBucket {
    val zone = zoneOf(item)
    val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
    val date = reminderLocalDate(item) ?: return AgendaBucket.UNDATED
    val timedAlertPassed = !isTargetDate(item) && item.eventTimeMs != null && item.eventTimeMs < nowMs
    return when {
        date.isBefore(today) || (date == today && timedAlertPassed) -> AgendaBucket.OVERDUE
        date == today -> AgendaBucket.TODAY
        date == today.plusDays(1) -> AgendaBucket.TOMORROW
        !date.isAfter(today.plusDays(7)) -> AgendaBucket.THIS_WEEK
        else -> AgendaBucket.LATER
    }
}

private fun agendaSortKey(item: ReminderEntity): Long =
    item.eventTimeMs
        ?: reminderLocalDate(item)?.atStartOfDay(zoneOf(item))?.toInstant()?.toEpochMilli()
        ?: Long.MAX_VALUE

/** Countdown for timed alerts within a day, or null when the calendar label reads better. */
internal fun reminderCountdown(item: ReminderEntity, nowMs: Long): String? {
    val at = item.eventTimeMs ?: return null
    if (isTargetDate(item)) return null
    val minutes = (at - nowMs) / 60_000
    return when {
        minutes < 0 -> "overdue"
        minutes < 1 -> "now"
        minutes < 60 -> "in ${minutes}m"
        minutes < 24 * 60 -> "in ${minutes / 60}h ${minutes % 60}m"
        else -> null
    }
}

@Composable
private fun reminderAccent(item: ReminderEntity, overdue: Boolean): Color {
    val dark = LocalIsCosmicDark.current
    return when {
        overdue -> if (dark) Color(0xFFF87171) else StudioCrimson
        isTargetDate(item) -> if (dark) StardustGold else Color(0xFFD97706)
        else -> if (dark) CosmicGlowBlue else CobaltBlue
    }
}

/** Two-line "next up" summary that replaces the old 44dp glance bar. Tap opens the agenda sheet. */
@Composable
internal fun NextUpReminderCard(
    reminders: List<ReminderEntity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    nowMs: Long = System.currentTimeMillis()
) {
    if (reminders.isEmpty()) return
    val dark = LocalIsCosmicDark.current
    val primary = remember(reminders, nowMs) {
        reminders.filter { agendaBucket(it, nowMs) != AgendaBucket.OVERDUE }.minByOrNull(::agendaSortKey)
            ?: reminders.minBy(::agendaSortKey)
    }
    val overdue = agendaBucket(primary, nowMs) == AgendaBucket.OVERDUE
    val accent = reminderAccent(primary, overdue)
    val countdown = remember(primary, nowMs) { reminderCountdown(primary, nowMs) }
    val timing = remember(primary, nowMs) { formatGlanceReminderTiming(primary, nowMs) }
    val kicker = when {
        overdue -> "OVERDUE"
        isTargetDate(primary) -> "NEXT TARGET"
        else -> "NEXT UP"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (dark) CosmicVoidCard else Color.White,
        border = BorderStroke(1.dp, if (dark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
        shadowElevation = if (dark) 0.dp else 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Dates & reminders. Open all ${reminders.size}" }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, top = 10.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = if (countdown != null) "$kicker · $countdown" else kicker,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = accent,
                    maxLines = 1
                )
                Text(
                    text = primary.title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (dark) TextOnDarkPrimary else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$timing · ${primary.recordingName}",
                    fontSize = 11.5.sp,
                    color = if (dark) TextOnDarkSecondary else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (dark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = "${reminders.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (dark) TextOnDarkSecondary else TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (dark) TextOnDarkSecondary else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
internal fun RemindersAgendaSheet(
    reminders: List<ReminderEntity>,
    state: InsightsUiState,
    onDismissRequest: () -> Unit,
    onOpenSource: (Long) -> Unit,
    onEdit: (ReminderEntity) -> Unit,
    onDone: (ReminderEntity) -> Unit,
    onDismissReminder: (ReminderEntity) -> Unit
) {
    val dark = LocalIsCosmicDark.current
    val maxSheetHeight = (LocalConfiguration.current.screenHeightDp * 0.60f).dp
    var filter by rememberSaveable { mutableStateOf(AgendaAll) }
    val nowMs = state.now

    val filtered = remember(reminders, filter) {
        when (filter) {
            AgendaReminders -> reminders.filter { !isTargetDate(it) }
            AgendaTargets -> reminders.filter { isTargetDate(it) }
            else -> reminders
        }
    }
    val grouped = remember(filtered, nowMs) {
        filtered.sortedBy(::agendaSortKey)
            .groupBy { agendaBucket(it, nowMs) }
            .toSortedMap()
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = if (dark) CosmicVoidCard else CeramicWhite,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.5.dp)
                    .background(if (dark) CosmicVoidCardBorder else Color(0xFFD1D1D6), CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .navigationBarsPadding()
                .testTag("reminders_agenda_sheet")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Dates & reminders",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (dark) TextOnDarkPrimary else TextPrimary
                    )
                    Text(
                        text = "${reminders.size} coming up",
                        fontSize = 12.sp,
                        color = if (dark) TextOnDarkSecondary else TextMuted
                    )
                }
                IconButton(onClick = onDismissRequest, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = if (dark) TextOnDarkSecondary else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(AgendaAll, AgendaReminders, AgendaTargets).forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(option, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold) },
                        shape = CircleShape
                    )
                }
            }
            HorizontalDivider(color = if (dark) CosmicVoidCardBorder else SlateBorder)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, false),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (grouped.isEmpty()) {
                    item {
                        Text(
                            text = when (filter) {
                                AgendaReminders -> "No scheduled reminders."
                                AgendaTargets -> "No target dates."
                                else -> "Nothing coming up."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (dark) TextOnDarkSecondary else TextSecondary,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
                grouped.forEach { (bucket, rows) ->
                    item(key = "header_${bucket.name}") {
                        Text(
                            text = bucket.label.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = if (bucket == AgendaBucket.OVERDUE) {
                                if (dark) Color(0xFFF87171) else StudioCrimson
                            } else {
                                if (dark) TextOnDarkSecondary else TextMuted
                            },
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    items(rows, key = { it.id }) { reminder ->
                        AgendaRow(
                            item = reminder,
                            overdue = bucket == AgendaBucket.OVERDUE,
                            nowMs = nowMs,
                            state = state,
                            onOpenSource = onOpenSource,
                            onEdit = { onEdit(reminder) },
                            onDone = { onDone(reminder) },
                            onDismiss = { onDismissReminder(reminder) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaRow(
    item: ReminderEntity,
    overdue: Boolean,
    nowMs: Long,
    state: InsightsUiState,
    onOpenSource: (Long) -> Unit,
    onEdit: () -> Unit,
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    val dark = LocalIsCosmicDark.current
    val accent = reminderAccent(item, overdue)
    val timing = remember(item, nowMs) { formatGlanceReminderTiming(item, nowMs) }
    val swipe = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { onDone(); true }
                SwipeToDismissBoxValue.EndToStart -> { onDismiss(); true }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = swipe,
        backgroundContent = {
            val toDone = swipe.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val active = swipe.dismissDirection != SwipeToDismissBoxValue.Settled
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        when {
                            !active -> Color.Transparent
                            toDone -> EmeraldSuccess
                            else -> StudioCrimson
                        }
                    )
                    .padding(horizontal = 18.dp),
                contentAlignment = if (toDone) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (active) {
                    Icon(
                        imageVector = if (toDone) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (dark) CosmicVoidCard else Color.White,
            border = BorderStroke(1.dp, if (dark) CosmicVoidCardBorder else Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onEdit)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp, top = 9.dp, bottom = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(
                            imageVector = if (isTargetDate(item)) Icons.Default.Flag else Icons.Default.Notifications,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = timing,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = item.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (dark) TextOnDarkPrimary else TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    InsightSourceChip(id = item.recordingId, state = state, open = onOpenSource, compact = true)
                }
                IconButton(
                    onClick = onDone,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircleOutline,
                        contentDescription = "Mark ${item.title} done",
                        tint = if (dark) TextOnDarkSecondary else TextMuted
                    )
                }
            }
        }
    }
}
