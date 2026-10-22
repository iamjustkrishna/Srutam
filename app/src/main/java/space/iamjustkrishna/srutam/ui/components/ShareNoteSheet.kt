package space.iamjustkrishna.srutam.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notes
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
                ModeSelector(
                    selected = options.mode,
                    onSelect = { update(options.copy(mode = it)) }
                )
                Spacer(Modifier.height(16.dp))
            }

            // Everything below keeps its place whatever is picked, so the sheet doesn't jump:
            // parts that don't apply dim instead of disappearing.
            val textAlpha by animateFloatAsState(if (options.sendsText) 1f else 0.35f, tween(220), label = "textAlpha")
            Column(Modifier.alpha(textAlpha)) {
                SectionLabel("INCLUDE")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // At least one stays on: unticking the last one does nothing.
                    ContentChip("Summary & insights", options.summary, enabled = options.sendsText) {
                        if (!options.summary || options.transcript) update(options.copy(summary = !options.summary))
                    }
                    ContentChip("Transcript", options.transcript, enabled = options.sendsText) {
                        if (!options.transcript || options.summary) update(options.copy(transcript = !options.transcript))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            SectionLabel("PREVIEW")
            Box(Modifier.fillMaxWidth().height(if (audio != null) 280.dp else 300.dp)) {
                AnimatedContent(
                    targetState = options.mode,
                    transitionSpec = {
                        (fadeIn(tween(220, delayMillis = 60)) + scaleIn(tween(220, delayMillis = 60), initialScale = 0.97f))
                            .togetherWith(fadeOut(tween(120)))
                    },
                    label = "preview"
                ) { mode ->
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = if (mode == NoteShare.Mode.AUDIO) Arrangement.Center else Arrangement.Top
                    ) {
                        if (mode != NoteShare.Mode.AUDIO) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Sem.card,
                                border = BorderStroke(1.dp, Sem.border),
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            ) {
                                Crossfade(targetState = if (onlyMissingTranscript) null else text, animationSpec = tween(180), label = "previewText") { shown ->
                                    Text(
                                        text = shown ?: "This note has no transcript yet.",
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp,
                                        color = if (shown == null) Sem.textMuted else Sem.text,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(14.dp)
                                    )
                                }
                            }
                        }
                        if (mode != NoteShare.Mode.TEXT && audio != null) {
                            if (mode == NoteShare.Mode.BOTH) Spacer(Modifier.height(8.dp))
                            AudioAttachment(audio.name, durationMs = recording.duration)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.alpha(textAlpha), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = options.footer,
                    onCheckedChange = { update(options.copy(footer = it)) },
                    enabled = options.sendsText,
                    colors = CheckboxDefaults.colors(checkedColor = Sem.accent),
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    "Add \"shared from Srutam\"",
                    fontSize = 12.5.sp,
                    color = Sem.textSecondary,
                    modifier = Modifier.clickable(enabled = options.sendsText) { update(options.copy(footer = !options.footer)) }
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                run {
                    OutlinedButton(
                        onClick = {
                            NoteShare.copy(context, text)
                            logShare("copy")
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        enabled = canSend && options.sendsText,
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

/** Text / Audio / Text + audio, as a segmented control with a pill that slides to the choice. */
@Composable
private fun ModeSelector(selected: NoteShare.Mode, onSelect: (NoteShare.Mode) -> Unit) {
    val modes = NoteShare.Mode.entries
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Sem.chip, RoundedCornerShape(16.dp))
            .padding(4.dp)
    ) {
        val segment = maxWidth / modes.size
        val pillX by animateDpAsState(
            segment * selected.ordinal,
            spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
            label = "pill"
        )
        Box(
            Modifier
                .offset(x = pillX)
                .width(segment)
                .fillMaxHeight()
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .background(Sem.selected, RoundedCornerShape(12.dp))
        )
        Row(Modifier.fillMaxSize()) {
            modes.forEach { mode ->
                val isSelected = mode == selected
                val color by animateColorAsState(if (isSelected) Sem.accent else Sem.textMuted, tween(200), label = "modeColor")
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(mode) }
                        .semantics {
                            role = Role.RadioButton
                            this.selected = isSelected
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (mode) {
                            NoteShare.Mode.TEXT -> Icons.Default.Notes
                            NoteShare.Mode.AUDIO -> Icons.Default.GraphicEq
                            NoteShare.Mode.BOTH -> Icons.Default.Layers
                        },
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        mode.label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Sem.text else Sem.textMuted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentChip(label: String, checked: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (checked) Sem.accentContainer else Sem.card,
        border = BorderStroke(1.dp, if (checked) Sem.accentBorder else Sem.border),
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
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
private fun AudioAttachment(fileName: String, durationMs: Long) {
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
                val seconds = durationMs / 1000
                val length = if (seconds > 0) "%d:%02d · ".format(seconds / 60, seconds % 60) else ""
                Text(
                    length + "the recording only; its notes stay on your phone",
                    fontSize = 12.sp,
                    color = Sem.textSecondary
                )
            }
        }
    }
}
