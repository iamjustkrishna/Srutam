package space.iamjustkrishna.srutam.ui.sample

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import space.iamjustkrishna.srutam.ui.theme.Sem
import java.util.Locale

/**
 * The built-in "Srutam welcome note" shown on Home before the first real note. It lives only in
 * memory: never saved to the database, never synced, never fed to themes, and its reminder is an
 * example that is never scheduled. Phone and tablet both read their content from here.
 */
object SampleNote {
    const val PATH = "srutam://welcome"
    const val TITLE = "Srutam welcome note"
    const val DURATION_MS = 38_000L

    const val TRANSCRIPT =
        "Okay, quick note before the team sync. We're going with the Friday launch for the new " +
            "onboarding. I need to send the release notes to Priya, and book a call with the design " +
            "team. One idea: we could show a sample note so new users see what Srutam does before " +
            "they record. And remind me to check the install numbers on Monday morning."

    const val SUMMARY =
        "A pre-meeting voice note: the new onboarding launches on Friday, two follow-ups are " +
            "needed, there's an idea for a sample note, and a reminder for Monday."

    const val WIIFM =
        "This is what every note becomes: speak naturally and Srutam pulls out the summary, " +
            "tasks, ideas, decisions and reminders for you."

    val KEY_POINTS = listOf(
        "The new onboarding launches on Friday",
        "Release notes go to Priya",
        "A sample note could show new users what Srutam does"
    )
    val TASKS = listOf("Send the release notes to Priya", "Book a call with the design team")
    const val IDEA = "Show a sample note so new users see what Srutam does before they record"
    const val DECISION = "Launch the new onboarding on Friday"
    const val REMINDER = "Check the install numbers · Monday, 9:00 AM"

    /** Key points as the JSON array the Recording entity stores. */
    val keyPointsJson: String
        get() = KEY_POINTS.joinToString(prefix = "[", postfix = "]") {
            "\"" + it.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        }

    /** Shown until the first real note exists or the user dismisses it. */
    fun shouldShow(hasRealNotes: Boolean, dismissed: Boolean) = !hasRealNotes && !dismissed
}

@Composable
private fun SampleBadge() {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Sem.amberContainer,
        border = BorderStroke(1.dp, Sem.amberBorder)
    ) {
        Text(
            text = "Sample",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = Sem.amberText,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/** Feed card for the sample note. */
@Composable
fun SampleNoteCard(
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Sem.card,
        border = BorderStroke(1.dp, Sem.border),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SampleBadge()
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "0:38 · Example",
                    fontSize = 12.sp,
                    color = Sem.textMuted,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss sample note", tint = Sem.textMuted, modifier = Modifier.size(18.dp))
                }
            }
            Text(
                text = SampleNote.TITLE,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Sem.text
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = SampleNote.SUMMARY,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                color = Sem.textSecondary,
                modifier = Modifier.padding(end = 12.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "2 tasks · 1 idea · 1 decision · 1 reminder",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Sem.accent
            )
        }
    }
}

/** Full view of the sample note. The "clip" is the transcript read aloud with text-to-speech. */
@Composable
fun SampleNoteDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var speaking by remember { mutableStateOf(false) }
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.setLanguage(Locale.getDefault())
                engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) { speaking = false }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { speaking = false }
                })
                tts.value = engine
            }
        }
        onDispose {
            engine?.stop()
            engine?.shutdown()
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Sem.scaffold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 640.dp)
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SampleBadge()
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Sem.textSecondary)
                    }
                }
                Text(SampleNote.TITLE, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Sem.text)
                Spacer(Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Sem.accentContainer,
                    border = BorderStroke(1.dp, Sem.accentBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = tts.value != null) {
                            val engine = tts.value ?: return@clickable
                            if (speaking) {
                                engine.stop()
                                speaking = false
                            } else {
                                engine.speak(SampleNote.TRANSCRIPT, TextToSpeech.QUEUE_FLUSH, null, "sample_note")
                                speaking = true
                            }
                        }
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Surface(shape = CircleShape, color = Sem.accent, modifier = Modifier.size(36.dp)) {}
                            Icon(
                                imageVector = if (speaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (speaking) "Stop" else "Play sample clip",
                                tint = Sem.card
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("Sample clip · 0:38", color = Sem.text, fontWeight = FontWeight.SemiBold)
                    }
                }

                Section("Summary") { Body(SampleNote.SUMMARY) }
                Section("Transcript") { Body(SampleNote.TRANSCRIPT) }
                Section("Tasks") {
                    SampleNote.TASKS.forEach { Item(Icons.Default.RadioButtonUnchecked, it) }
                }
                Section("Idea") { Item(Icons.Default.Lightbulb, SampleNote.IDEA) }
                Section("Decision") { Item(Icons.Default.Gavel, SampleNote.DECISION) }
                Section("Reminder") {
                    Item(Icons.Default.NotificationsActive, SampleNote.REMINDER)
                    Text(
                        text = "Example only. This reminder won't notify you.",
                        fontSize = 12.sp,
                        color = Sem.textMuted,
                        modifier = Modifier.padding(start = 30.dp, top = 2.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Record your first note and it will look like this. The sample disappears once you do.",
                    fontSize = 13.sp,
                    color = Sem.textSecondary
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(18.dp))
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = Sem.accent
    )
    Spacer(Modifier.height(6.dp))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
}

@Composable
private fun Body(text: String) {
    Text(text, fontSize = 14.sp, lineHeight = 20.sp, color = Sem.text)
}

@Composable
private fun Item(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = Sem.textSecondary, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Spacer(Modifier.width(12.dp))
        Body(text)
    }
}
