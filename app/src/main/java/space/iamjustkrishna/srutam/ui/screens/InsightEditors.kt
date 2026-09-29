@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package space.iamjustkrishna.srutam.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import space.iamjustkrishna.srutam.ai.ReminderTimeResolver
import space.iamjustkrishna.srutam.ai.ResolvedReminderTime
import space.iamjustkrishna.srutam.data.*
import space.iamjustkrishna.srutam.ui.theme.*
import java.time.*
import java.time.format.DateTimeFormatter

@Composable
internal fun ReminderEditor(
    item: ReminderEntity,
    actionError: String? = null,
    onDismiss: () -> Unit,
    onSave: (ReminderEntity, Boolean) -> Unit,
    onConvertToTask: (() -> Unit)? = null,
    onMarkDone: (() -> Unit)? = null
) {
    var title by rememberSaveable(item.id) { mutableStateOf(item.title) }
    var category by rememberSaveable(item.id) { mutableStateOf(item.type) }
    var date by rememberSaveable(item.id) { mutableStateOf(item.localDate.orEmpty()) }
    var time by rememberSaveable(item.id) { mutableStateOf(item.localTime.orEmpty()) }
    var zone by rememberSaveable(item.id) { mutableStateOf(item.zoneId ?: ZoneId.systemDefault().id) }
    var enabled by rememberSaveable(item.id) { mutableStateOf(item.notificationEnabled) }
    var advance by rememberSaveable(item.id) { mutableStateOf(item.advanceNotification) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var permission by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permission = it }
    val dark = LocalIsCosmicDark.current

    val maxSheetHeight = (LocalConfiguration.current.screenHeightDp * 0.60f).dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
            Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (dark) CobaltBlue.copy(alpha = 0.25f) else CobaltContainer,
                    border = BorderStroke(1.dp, if (dark) CosmicGlowBlue.copy(alpha = 0.4f) else CobaltBorder),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (dark) CosmicGlowBlue else CobaltBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = "Review reminder",
                    fontFamily = PlayfairDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = if (dark) TextOnDarkPrimary else TextPrimary
                )
            }

            if (item.originalText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (dark) CosmicVoidCardBorder.copy(alpha = 0.4f) else SlateGrouped,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "From your note: “${item.originalText}”",
                        fontSize = 12.sp,
                        color = if (dark) TextOnDarkSecondary else TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (dark) CosmicGlowBlue else CobaltBlue,
                    unfocusedBorderColor = if (dark) CosmicVoidCardBorder else SlateBorder,
                    focusedLabelColor = if (dark) CosmicGlowBlue else CobaltBlue,
                    unfocusedLabelColor = if (dark) TextOnDarkSecondary else TextSecondary,
                    focusedTextColor = if (dark) TextOnDarkPrimary else TextPrimary,
                    unfocusedTextColor = if (dark) TextOnDarkPrimary else TextPrimary,
                    focusedContainerColor = if (dark) CosmicVoidCard.copy(alpha = 0.5f) else SlateSurface,
                    unfocusedContainerColor = if (dark) CosmicVoidCard.copy(alpha = 0.5f) else SlateSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Category Chips
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ReminderType.MEETING, ReminderType.CALL, ReminderType.DEADLINE, ReminderType.MILESTONE, ReminderType.REMINDER).forEach { type ->
                    val selected = category == type
                    Surface(
                        onClick = { category = type },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) {
                            if (dark) CobaltBlue.copy(alpha = 0.35f) else CobaltContainer
                        } else {
                            if (dark) CosmicVoidCardBorder.copy(alpha = 0.4f) else SlateGrouped
                        },
                        border = BorderStroke(
                            1.dp,
                            if (selected) (if (dark) CosmicGlowBlue else CobaltBlue) else (if (dark) CosmicVoidCardBorder else SlateBorder)
                        )
                    ) {
                        Text(
                            text = if (type == ReminderType.MILESTONE) "Target date" else type.lowercase().replaceFirstChar { it.titlecase() },
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) (if (dark) CosmicGlowBlue else CobaltBlue) else (if (dark) TextOnDarkSecondary else TextSecondary),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Notification Card with embedded Date & Time pickers
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (dark) CosmicVoidCard else SlateSurface,
                border = BorderStroke(1.dp, if (enabled) (if (dark) CosmicGlowBlue.copy(alpha = 0.4f) else CobaltBorder) else (if (dark) CosmicVoidCardBorder else SlateBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (enabled) (if (dark) StardustGold.copy(alpha = 0.2f) else Color(0xFFFEF3C7)) else (if (dark) CosmicVoidCardBorder else SlateGrouped),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (enabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    tint = if (enabled) (if (dark) StardustGold else Color(0xFFD97706)) else (if (dark) TextOnDarkSecondary else TextMuted),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable notifications",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (dark) TextOnDarkPrimary else TextPrimary
                            )
                            Text(
                                text = if (enabled) "Alarm will notify on date" else "Save as a passive date",
                                fontSize = 11.5.sp,
                                color = if (dark) TextOnDarkSecondary else TextSecondary
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = {
                                enabled = it
                                if (it && !permission && Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = if (dark) CosmicGlowBlue else CobaltBlue,
                                uncheckedThumbColor = if (dark) TextOnDarkSecondary else TextMuted,
                                uncheckedTrackColor = if (dark) CosmicVoidCardBorder else SlateGrouped
                            )
                        )
                    }

                    HorizontalDivider(
                        color = if (dark) CosmicVoidCardBorder else SlateBorder.copy(alpha = 0.7f),
                        thickness = 0.8.dp
                    )

                    DateTimePickers(date, { date = it }, time, { time = it }, zone, { zone = it }, dark = dark)

                    if (enabled) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Also notify 15 minutes before",
                                fontSize = 12.5.sp,
                                color = if (dark) TextOnDarkSecondary else TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = advance,
                                onCheckedChange = { advance = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = if (dark) CosmicGlowBlue else CobaltBlue,
                                    uncheckedThumbColor = if (dark) TextOnDarkSecondary else TextMuted,
                                    uncheckedTrackColor = if (dark) CosmicVoidCardBorder else SlateGrouped
                                )
                            )
                        }
                    }

                    if (!permission) {
                        Text(
                            text = "Notifications are disabled. Saving will keep this as a passive date.",
                            fontSize = 11.5.sp,
                            color = if (dark) StardustGold else Color(0xFFD97706)
                        )
                    }
                }
            }

            (error ?: actionError)?.let {
                Text(
                    text = it,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Primary Confirm / Save Button
            Button(
                onClick = {
                    try {
                        require(title.isNotBlank()) { "Enter a title." }
                        val resolved = editedReminderTime(date, time, zone)
                        if (enabled && permission) require(ReminderTimeResolver.isFuture(resolved, Clock.systemUTC())) { "Choose an unambiguous future date and time." }
                        onSave(item.copy(title = title, type = category, eventTimeMs = resolved.eventTimeMs,
                            localDate = resolved.localDate, localTime = resolved.localTime, zoneId = resolved.zoneId,
                            timePrecision = resolved.precision, notificationEnabled = enabled, advanceNotification = advance),
                            NotificationManagerCompat.from(context).areNotificationsEnabled())
                    } catch (e: Exception) { error = e.message ?: "Check the date and time." }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (dark) CosmicGlowBlue else CobaltBlue,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = if (enabled && permission) "Confirm and enable" else "Save changes",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
internal fun InsightTaskEditor(
    sourceId: String,
    initialText: String,
    actionError: String? = null,
    onDismiss: () -> Unit,
    onSave: (String, ReminderEntity?) -> Unit
) {
    var text by rememberSaveable(sourceId) { mutableStateOf(initialText) }
    var enabled by rememberSaveable(sourceId) { mutableStateOf(false) }
    var date by rememberSaveable(sourceId) { mutableStateOf("") }
    var time by rememberSaveable(sourceId) { mutableStateOf("") }
    var zone by rememberSaveable(sourceId) { mutableStateOf(ZoneId.systemDefault().id) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var permission by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permission = it }
    val dark = LocalIsCosmicDark.current

    val maxSheetHeight = (LocalConfiguration.current.screenHeightDp * 0.60f).dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
            Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Playfair Display typography
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (dark) CobaltBlue.copy(alpha = 0.25f) else CobaltContainer,
                    border = BorderStroke(1.dp, if (dark) CosmicGlowBlue.copy(alpha = 0.4f) else CobaltBorder),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.TaskAlt,
                            contentDescription = null,
                            tint = if (dark) CosmicGlowBlue else CobaltBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = "Create next step",
                    fontFamily = PlayfairDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = if (dark) TextOnDarkPrimary else TextPrimary
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Next step") },
                placeholder = { Text("What needs to be done?") },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (dark) CosmicGlowBlue else CobaltBlue,
                    unfocusedBorderColor = if (dark) CosmicVoidCardBorder else SlateBorder,
                    focusedLabelColor = if (dark) CosmicGlowBlue else CobaltBlue,
                    unfocusedLabelColor = if (dark) TextOnDarkSecondary else TextSecondary,
                    focusedTextColor = if (dark) TextOnDarkPrimary else TextPrimary,
                    unfocusedTextColor = if (dark) TextOnDarkPrimary else TextPrimary,
                    focusedContainerColor = if (dark) CosmicVoidCard.copy(alpha = 0.5f) else SlateSurface,
                    unfocusedContainerColor = if (dark) CosmicVoidCard.copy(alpha = 0.5f) else SlateSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Styled "Add a reminder" container card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (dark) CosmicVoidCard else SlateSurface,
                border = BorderStroke(
                    1.dp,
                    if (enabled) (if (dark) CosmicGlowBlue.copy(alpha = 0.4f) else CobaltBorder)
                    else (if (dark) CosmicVoidCardBorder else SlateBorder)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (enabled) (if (dark) StardustGold.copy(alpha = 0.2f) else Color(0xFFFEF3C7)) else (if (dark) CosmicVoidCardBorder else SlateGrouped),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (enabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    tint = if (enabled) (if (dark) StardustGold else Color(0xFFD97706)) else (if (dark) TextOnDarkSecondary else TextMuted),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Add a reminder",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (dark) TextOnDarkPrimary else TextPrimary
                            )
                            Text(
                                text = if (enabled) "Alarm will trigger on scheduled date" else "Set date & notification alarm",
                                fontSize = 11.5.sp,
                                color = if (dark) TextOnDarkSecondary else TextSecondary
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = {
                                enabled = it
                                if (it && !permission && Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = if (dark) CosmicGlowBlue else CobaltBlue,
                                uncheckedThumbColor = if (dark) TextOnDarkSecondary else TextMuted,
                                uncheckedTrackColor = if (dark) CosmicVoidCardBorder else SlateGrouped
                            )
                        )
                    }

                    if (enabled) {
                        HorizontalDivider(
                            color = if (dark) CosmicVoidCardBorder else SlateBorder.copy(alpha = 0.7f),
                            thickness = 0.8.dp
                        )
                        DateTimePickers(date, { date = it }, time, { time = it }, zone, { zone = it }, dark = dark)
                        if (!permission) {
                            Text(
                                text = "Notifications are disabled. The task will be saved without an active alarm.",
                                fontSize = 11.5.sp,
                                color = if (dark) StardustGold else Color(0xFFD97706)
                            )
                        }
                    }
                }
            }

            (error ?: actionError)?.let {
                Text(
                    text = it,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Button(
                onClick = {
                    try {
                        require(text.isNotBlank()) { "Enter a next step." }
                        val reminder = if (enabled && NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                            val resolved = editedReminderTime(date, time, zone)
                            require(ReminderTimeResolver.isFuture(resolved, Clock.systemUTC())) { "Choose an unambiguous future date and time." }
                            ReminderEntity(recordingId = 0, title = text.trim(), eventTimeMs = resolved.eventTimeMs,
                                originalText = "", timePrecision = resolved.precision, localDate = resolved.localDate,
                                localTime = resolved.localTime, zoneId = resolved.zoneId, zoneInferred = false,
                                notificationEnabled = true, confirmedAt = System.currentTimeMillis(), needsReview = false)
                        } else null
                        onSave(text, reminder)
                    } catch (e: Exception) { error = e.message ?: "Could not create the next step." }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (dark) CosmicGlowBlue else CobaltBlue,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(
                    "Create next step",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp
                )
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Cancel",
                    color = if (dark) TextOnDarkSecondary else TextSecondary,
                    fontSize = 13.5.sp
                )
            }
        }
    }
}

@Composable
private fun DateTimePickers(
    date: String,
    setDate: (String) -> Unit,
    time: String,
    setTime: (String) -> Unit,
    zone: String,
    setZone: (String) -> Unit,
    dark: Boolean
) {
    val context = LocalContext.current
    val initialDate = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now().plusDays(1))
    val initialTime = runCatching { LocalTime.parse(time) }.getOrDefault(LocalTime.of(9, 0))

    val formattedDateText = remember(date) {
        if (date.isBlank()) "Choose date" else {
            runCatching {
                val parsed = LocalDate.parse(date)
                val today = LocalDate.now()
                when (parsed) {
                    today -> "Today (${parsed.format(DateTimeFormatter.ofPattern("MMM d"))})"
                    today.plusDays(1) -> "Tomorrow (${parsed.format(DateTimeFormatter.ofPattern("MMM d"))})"
                    else -> parsed.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
                }
            }.getOrDefault(date)
        }
    }

    val formattedTimeText = remember(time) {
        if (time.isBlank()) "Set time (optional)" else {
            runCatching {
                val parsed = LocalTime.parse(time)
                parsed.format(DateTimeFormatter.ofPattern("h:mm a"))
            }.getOrDefault(time)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date tile picker
            Surface(
                onClick = {
                    android.app.DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            setDate(LocalDate.of(year, month + 1, day).toString())
                        },
                        initialDate.year,
                        initialDate.monthValue - 1,
                        initialDate.dayOfMonth
                    ).show()
                },
                shape = RoundedCornerShape(12.dp),
                color = if (date.isNotBlank()) {
                    if (dark) CobaltBlue.copy(alpha = 0.2f) else CobaltContainer
                } else {
                    if (dark) CosmicVoidCardBorder.copy(alpha = 0.5f) else SlateGrouped
                },
                border = BorderStroke(
                    1.dp,
                    if (date.isNotBlank()) (if (dark) CosmicGlowBlue.copy(alpha = 0.5f) else CobaltBorder)
                    else (if (dark) CosmicVoidCardBorder else SlateBorder)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = if (date.isNotBlank()) (if (dark) CosmicGlowBlue else CobaltBlue) else (if (dark) TextOnDarkSecondary else TextMuted),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = formattedDateText,
                        fontSize = 12.5.sp,
                        fontWeight = if (date.isNotBlank()) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (date.isNotBlank()) (if (dark) TextOnDarkPrimary else CobaltBlueDark) else (if (dark) TextOnDarkSecondary else TextSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Time tile picker
            Surface(
                onClick = {
                    android.app.TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            setTime(LocalTime.of(hour, minute).toString())
                        },
                        initialTime.hour,
                        initialTime.minute,
                        android.text.format.DateFormat.is24HourFormat(context)
                    ).show()
                },
                shape = RoundedCornerShape(12.dp),
                color = if (time.isNotBlank()) {
                    if (dark) CobaltBlue.copy(alpha = 0.2f) else CobaltContainer
                } else {
                    if (dark) CosmicVoidCardBorder.copy(alpha = 0.5f) else SlateGrouped
                },
                border = BorderStroke(
                    1.dp,
                    if (time.isNotBlank()) (if (dark) CosmicGlowBlue.copy(alpha = 0.5f) else CobaltBorder)
                    else (if (dark) CosmicVoidCardBorder else SlateBorder)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (time.isNotBlank()) (if (dark) CosmicGlowBlue else CobaltBlue) else (if (dark) TextOnDarkSecondary else TextMuted),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = formattedTimeText,
                        fontSize = 12.5.sp,
                        fontWeight = if (time.isNotBlank()) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (time.isNotBlank()) (if (dark) TextOnDarkPrimary else CobaltBlueDark) else (if (dark) TextOnDarkSecondary else TextSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (time.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear time",
                            tint = if (dark) TextOnDarkSecondary else TextMuted,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { setTime("") }
                        )
                    }
                }
            }
        }

        // Informative, auto-detected timezone chip
        val detectedZone = remember(zone) {
            runCatching { ZoneId.of(zone.trim()) }.getOrDefault(ZoneId.systemDefault())
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(start = 2.dp, top = 2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = if (dark) TextOnDarkSecondary.copy(alpha = 0.6f) else TextMuted,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "Time zone: ${detectedZone.id} (${detectedZone.rules.getOffset(Instant.now())})",
                fontSize = 11.sp,
                color = if (dark) TextOnDarkSecondary.copy(alpha = 0.7f) else TextSecondary.copy(alpha = 0.8f)
            )
        }
    }
}

internal fun editedReminderTime(date: String, time: String, zone: String): ResolvedReminderTime {
    val zoneId = runCatching { ZoneId.of(zone.trim()) }.getOrElse { error("Enter a valid time zone, such as Asia/Kolkata.") }
    if (date.isBlank()) {
        require(time.isBlank()) { "Choose a date as well as a time." }
        return ResolvedReminderTime(zoneId = zoneId.id)
    }
    val parsedDate = runCatching { LocalDate.parse(date.trim()) }.getOrElse { error("Choose a valid date.") }
    if (time.isBlank()) return ResolvedReminderTime(localDate = parsedDate.toString(), zoneId = zoneId.id, precision = "DATE_ONLY")
    val parsedTime = runCatching { LocalTime.parse(time.trim()) }.getOrElse { error("Choose a valid time.") }
    val resolved = ReminderTimeResolver.resolveLocal(parsedDate, parsedTime, zoneId)
    require(resolved.precision == "EXACT") { "This time is ambiguous or skipped by a clock change. Choose another time." }
    return resolved
}
