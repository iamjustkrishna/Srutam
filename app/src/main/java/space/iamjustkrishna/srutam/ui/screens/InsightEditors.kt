@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package space.iamjustkrishna.srutam.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import space.iamjustkrishna.srutam.ai.ReminderTimeResolver
import space.iamjustkrishna.srutam.ai.ResolvedReminderTime
import space.iamjustkrishna.srutam.data.*
import java.time.*

@Composable
internal fun ReminderEditor(item: ReminderEntity, actionError: String? = null, onDismiss: () -> Unit, onSave: (ReminderEntity, Boolean) -> Unit) {
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
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Review reminder", style = MaterialTheme.typography.headlineSmall)
            if (item.originalText.isNotBlank()) Text("From your note: “${item.originalText}”")
            if (item.zoneInferred) Text("Time zone inferred. Check it before enabling notifications.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(ReminderType.MEETING, ReminderType.CALL, ReminderType.DEADLINE, ReminderType.MILESTONE, ReminderType.REMINDER).forEach { type ->
                    FilterChip(category == type, { category = type }, label = { Text(if (type == ReminderType.MILESTONE) "Target date" else type.lowercase().replaceFirstChar { it.titlecase() }) })
                }
            }
            DateTimeFields(date, { date = it }, time, { time = it }, zone, { zone = it })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable notifications", Modifier.weight(1f))
                Switch(enabled, {
                    enabled = it
                    if (it && !permission && Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                })
            }
            if (enabled) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Also notify 15 minutes before", Modifier.weight(1f)); Switch(advance, { advance = it }) }
                if (!permission) Text("Notifications are unavailable. Saving will keep this as a passive date.")
            }
            (error ?: actionError)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                try {
                    require(title.isNotBlank()) { "Enter a title." }
                    val resolved = editedReminderTime(date, time, zone)
                    if (enabled && permission) require(ReminderTimeResolver.isFuture(resolved, Clock.systemUTC())) { "Choose an unambiguous future date and time." }
                    onSave(item.copy(title = title, type = category, eventTimeMs = resolved.eventTimeMs,
                        localDate = resolved.localDate, localTime = resolved.localTime, zoneId = resolved.zoneId,
                        timePrecision = resolved.precision, notificationEnabled = enabled, advanceNotification = advance),
                        NotificationManagerCompat.from(context).areNotificationsEnabled())
                } catch (e: Exception) { error = e.message ?: "Check the date and time." }
            }, modifier = Modifier.fillMaxWidth()) { Text(if (enabled && permission) "Confirm and enable" else "Save date") }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

@Composable
internal fun InsightTaskEditor(sourceId: String, initialText: String, actionError: String? = null, onDismiss: () -> Unit, onSave: (String, ReminderEntity?) -> Unit) {
    var text by rememberSaveable(sourceId) { mutableStateOf(initialText) }
    var enabled by rememberSaveable(sourceId) { mutableStateOf(false) }
    var date by rememberSaveable(sourceId) { mutableStateOf("") }
    var time by rememberSaveable(sourceId) { mutableStateOf("") }
    var zone by rememberSaveable(sourceId) { mutableStateOf(ZoneId.systemDefault().id) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var permission by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permission = it }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Create next step", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(text, { text = it }, label = { Text("Next step") }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Add a reminder", Modifier.weight(1f))
                Switch(enabled, {
                    enabled = it
                    if (it && !permission && Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                })
            }
            if (enabled) {
                DateTimeFields(date, { date = it }, time, { time = it }, zone, { zone = it })
                if (!permission) Text("Notifications are unavailable. The task will be saved without a reminder.")
            }
            (error ?: actionError)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
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
            }, modifier = Modifier.fillMaxWidth()) { Text("Create next step") }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

@Composable
private fun DateTimeFields(date: String, setDate: (String) -> Unit, time: String, setTime: (String) -> Unit, zone: String, setZone: (String) -> Unit) {
    val context = LocalContext.current
    val initialDate = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
    val initialTime = runCatching { LocalTime.parse(time) }.getOrDefault(LocalTime.of(9, 0))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
            android.app.DatePickerDialog(context, { _, year, month, day ->
                setDate(LocalDate.of(year, month + 1, day).toString())
            }, initialDate.year, initialDate.monthValue - 1, initialDate.dayOfMonth).show()
        }) { Text(if (date.isBlank()) "Choose date" else date) }
        OutlinedButton(onClick = {
            android.app.TimePickerDialog(context, { _, hour, minute ->
                setTime(LocalTime.of(hour, minute).toString())
            }, initialTime.hour, initialTime.minute, android.text.format.DateFormat.is24HourFormat(context)).show()
        }) { Text(if (time.isBlank()) "Set time (optional)" else time) }
        if (time.isNotBlank()) TextButton(onClick = { setTime("") }) { Text("Clear time") }
    }
    OutlinedTextField(zone, setZone, label = { Text("Time zone") }, placeholder = { Text("Asia/Kolkata") }, singleLine = true, modifier = Modifier.fillMaxWidth())
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
