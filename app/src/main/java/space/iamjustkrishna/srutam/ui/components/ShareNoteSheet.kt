package space.iamjustkrishna.srutam.ui.components

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.iamjustkrishna.srutam.cloud.CloudRestore
import space.iamjustkrishna.srutam.data.AppDatabase
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.ReminderEntity
import space.iamjustkrishna.srutam.ui.theme.Sem
import space.iamjustkrishna.srutam.utils.Analytics
import space.iamjustkrishna.srutam.utils.NoteShare
import java.io.File

/**
 * Picks what to share from a note: its text (summary and insights, transcript, or both), its
 * audio, or both; shows exactly what will be sent; then opens Android's share sheet or copies
 * the text. Loads the note's tasks and reminders itself, so any screen can open it with the note.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareNoteSheet(recording: Recording, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val audio = remember(recording.audioFilePath) {
        File(recording.audioFilePath).takeIf { !CloudRestore.isTextOnlyPath(recording.audioFilePath) && it.isFile }
    }
    var options by remember {
        mutableStateOf(NoteShare.savedOptions(context).let { if (audio == null) it.copy(mode = NoteShare.Mode.TEXT) else it })
    }
    var insights by remember { mutableStateOf<List<InsightEntity>>(emptyList()) }
    var reminders by remember { mutableStateOf<List<ReminderEntity>>(emptyList()) }

    LaunchedEffect(recording.id) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            insights = db.insightDao().getInsightsByRecordingId(recording.id)
            reminders = db.reminderDao().getRemindersByRecordingId(recording.id)
        }
    }

    val text = remember(recording, insights, reminders, options) {
        NoteShare.text(recording, insights, reminders, options)
    }
    val onlyMissingTranscript = options.sendsText && !options.summary && recording.transcript.isNullOrBlank()
    val canSend = !onlyMissingTranscript

    fun update(next: NoteShare.Options) {
        options = next
        NoteShare.saveOptions(context, next)
    }

    fun logShare(how: String) = Analytics.log(
        context, "note_shared", mapOf(
            "mode" to options.mode.name.lowercase(),
            "summary" to options.summary.toString(),
            "transcript" to options.transcript.toString(),
            "how" to how
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Sem.scaffold
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp)
                .navigationBarsPadding()
        ) {
            Text("Share note", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Sem.text)
            Spacer(Modifier.height(14.dp))

            if (recording.isPrivate) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Sem.amberSoft,
                    border = BorderStroke(1.dp, Sem.amberBorder),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Sem.amberText, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("This note is private. Sharing sends it outside Srutam.", fontSize = 13.sp, color = Sem.amberText)
                    }
                }
            }

            // Text, audio or both: only when there's audio to send.
            if (audio != null) {
                SectionLabel("SEND")
                StepSelector(
                    steps = NoteShare.Mode.entries.map { it.label },
                    selected = options.mode.ordinal,
                    onSelect = { update(options.copy(mode = NoteShare.Mode.entries[it])) }
                )
                Spacer(Modifier.height(14.dp))
            }

            if (options.sendsText) {
                SectionLabel("INCLUDE")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // At least one stays on: unticking the last one does nothing.
                    ContentChip("Summary & insights", options.summary) {
                        if (!options.summary || options.transcript) update(options.copy(summary = !options.summary))
                    }
                    ContentChip("Transcript", options.transcript) {
                        if (!options.transcript || options.summary) update(options.copy(transcript = !options.transcript))
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            SectionLabel("PREVIEW")
            if (options.sendsText) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Sem.card,
                    border = BorderStroke(1.dp, Sem.border),
                    modifier = Modifier.fillMaxWidth().heightIn(max = if (options.sendsAudio) 210.dp else 260.dp)
                ) {
                    Text(
                        text = if (onlyMissingTranscript) "This note has no transcript yet." else text,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = if (onlyMissingTranscript) Sem.textMuted else Sem.text,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp)
                    )
                }
            }
            if (options.sendsAudio && audio != null) {
                if (options.sendsText) Spacer(Modifier.height(8.dp))
                AudioAttachment(audio.name)
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (options.sendsText) {
                    Checkbox(
                        checked = options.footer,
                        onCheckedChange = { update(options.copy(footer = it)) },
                        colors = CheckboxDefaults.colors(checkedColor = Sem.accent),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        "Add \"shared from Srutam\"",
                        fontSize = 12.5.sp,
                        color = Sem.textSecondary,
                        modifier = Modifier.clickable { update(options.copy(footer = !options.footer)) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (options.sendsText) {
                    OutlinedButton(
                        onClick = {
                            NoteShare.copy(context, text)
                            logShare("copy")
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        enabled = canSend,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Copy", fontWeight = FontWeight.SemiBold)
                    }
                }
                Button(
                    onClick = {
                        val sendText = text.takeIf { options.sendsText }
                        val sendAudio = audio?.takeIf { options.sendsAudio }
                        runCatching {
                            // Apps like WhatsApp drop the caption on a file; the clipboard has it.
                            if (sendText != null && sendAudio != null) NoteShare.copy(context, sendText)
                            NoteShare.share(context, sendText, sendAudio)
                        }.onSuccess {
                            logShare("share")
                            if (sendText != null && sendAudio != null) {
                                Toast.makeText(context, "Text copied too: paste it if the app sends only the audio", Toast.LENGTH_LONG).show()
                            }
                            onDismiss()
                        }.onFailure {
                            Toast.makeText(context, "Couldn't share this note", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = canSend,
                    colors = ButtonDefaults.buttonColors(containerColor = Sem.accent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1.4f).height(50.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Sem.accent)
    Spacer(Modifier.height(8.dp))
}

/**
 * A stepped slider: a track with a stop per option and a knob that snaps to the nearest stop.
 * Tap a stop or its label, or drag the knob.
 */
@Composable
private fun StepSelector(steps: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val density = LocalDensity.current
    val onSelectNow by rememberUpdatedState(onSelect)
    val knob = 22.dp
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val segment = maxWidth / steps.size
        val widthPx = with(density) { maxWidth.toPx() }
        fun stepAt(x: Float) = (x / widthPx * steps.size).toInt().coerceIn(0, steps.lastIndex)
        val knobX by animateDpAsState(segment * selected + segment / 2 - knob / 2, label = "knob")

        Column(
            Modifier.pointerInput(steps.size) {
                detectTapGestures { onSelectNow(stepAt(it.x)) }
            }.pointerInput(steps.size) {
                detectHorizontalDragGestures { change, _ -> onSelectNow(stepAt(change.position.x)) }
            }
        ) {
            Box(Modifier.fillMaxWidth().height(knob), contentAlignment = Alignment.CenterStart) {
                // Track between the first and last stop, filled up to the knob.
                Box(
                    Modifier
                        .padding(horizontal = segment / 2)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Sem.chip, CircleShape)
                )
                Box(
                    Modifier
                        .offset(x = segment / 2)
                        .width(segment * selected)
                        .height(4.dp)
                        .background(Sem.accent, CircleShape)
                )
                steps.indices.forEach { i ->
                    Box(
                        Modifier
                            .offset(x = segment * i + segment / 2 - 4.dp)
                            .size(8.dp)
                            .background(if (i <= selected) Sem.accent else Sem.border, CircleShape)
                    )
                }
                Box(
                    Modifier
                        .offset(x = knobX)
                        .size(knob)
                        .background(Sem.card, CircleShape)
                        .background(Sem.accent.copy(alpha = 0.15f), CircleShape)
                        .padding(4.dp)
                        .background(Sem.accent, CircleShape)
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                steps.forEachIndexed { i, label ->
                    val isSelected = i == selected
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Sem.text else Sem.textMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                role = Role.RadioButton
                                this.selected = isSelected
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentChip(label: String, checked: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (checked) Sem.accentContainer else Sem.card,
        border = BorderStroke(1.dp, if (checked) Sem.accentBorder else Sem.border),
        modifier = Modifier
            .clickable(onClick = onClick)
            .semantics { role = Role.Checkbox; selected = checked }
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            if (checked) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Sem.accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (checked) Sem.accent else Sem.textSecondary
            )
        }
    }
}

@Composable
private fun AudioAttachment(fileName: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Sem.card,
        border = BorderStroke(1.dp, Sem.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).background(Sem.accentContainer, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AudioFile, contentDescription = null, tint = Sem.accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(fileName, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Sem.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("The recording only; its notes stay on your phone", fontSize = 12.sp, color = Sem.textSecondary)
            }
        }
    }
}
