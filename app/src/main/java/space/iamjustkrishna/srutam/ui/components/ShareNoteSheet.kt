package space.iamjustkrishna.srutam.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
 * Picks what to share from a note (summary, transcript or everything, with or without its
 * audio), shows exactly what will be sent, then opens Android's share sheet or copies it.
 * Loads the note's tasks and reminders itself, so any screen can open it with just the note.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareNoteSheet(recording: Recording, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var options by remember { mutableStateOf(NoteShare.savedOptions(context)) }
    var insights by remember { mutableStateOf<List<InsightEntity>>(emptyList()) }
    var reminders by remember { mutableStateOf<List<ReminderEntity>>(emptyList()) }
    val audio = remember(recording.audioFilePath) {
        File(recording.audioFilePath).takeIf { !CloudRestore.isTextOnlyPath(recording.audioFilePath) && it.isFile }
    }

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
    val noTranscript = recording.transcript.isNullOrBlank() && options.content == NoteShare.Content.TRANSCRIPT

    fun update(next: NoteShare.Options) {
        options = next
        NoteShare.saveOptions(context, next)
    }

    fun logShare(how: String) = Analytics.log(
        context, "note_shared", mapOf(
            "content" to options.content.name.lowercase(),
            "audio" to (options.attachAudio && audio != null).toString(),
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
                .padding(bottom = 16.dp)
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
                        Text(
                            "This note is private. Sharing sends it outside Srutam.",
                            fontSize = 13.sp,
                            color = Sem.amberText
                        )
                    }
                }
            }

            // What to share
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Sem.chip, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NoteShare.Content.entries.forEach { content ->
                    val selected = options.content == content
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .background(if (selected) Sem.selected else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { update(options.copy(content = content)) }
                    ) {
                        Text(
                            content.label,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Sem.text else Sem.textMuted
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            if (audio != null) {
                ToggleRow("Attach audio", "Just the recording; the notes inside it stay on your phone.", options.attachAudio) {
                    update(options.copy(attachAudio = it))
                }
            }
            ToggleRow("Add \"shared from Srutam\"", null, options.footer) {
                update(options.copy(footer = it))
            }

            Spacer(Modifier.height(10.dp))
            Text("PREVIEW", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Sem.accent)
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Sem.card,
                border = BorderStroke(1.dp, Sem.border),
                modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp)
            ) {
                Text(
                    text = if (noTranscript) "This note has no transcript yet." else text,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = if (noTranscript) Sem.textMuted else Sem.text,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        NoteShare.copy(context, text)
                        logShare("copy")
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    enabled = !noTranscript,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Copy", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        runCatching { NoteShare.share(context, text, audio?.takeIf { options.attachAudio }) }
                            .onSuccess { logShare("share"); onDismiss() }
                            .onFailure { Toast.makeText(context, "Couldn't share this note", Toast.LENGTH_SHORT).show() }
                    },
                    enabled = !noTranscript,
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
private fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = Sem.text)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = Sem.textSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Sem.accent)
        )
    }
}
