package space.iamjustkrishna.srutam.ui.sample

import android.content.Context
import android.media.MediaMetadataRetriever
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.InsightKind
import space.iamjustkrishna.srutam.data.InsightStatus
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import space.iamjustkrishna.srutam.ui.theme.Sem
import space.iamjustkrishna.srutam.utils.AudioFileInfo
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume

/**
 * The built-in "Srutam welcome note" shown on Home before the first real note. It looks and
 * opens exactly like a real note (same feed card, same note screen), but it lives only in memory:
 * never saved to the database, never synced, never fed to themes, read-only, and its reminder is
 * an example that is never scheduled. It disappears once the first real note exists.
 *
 * Its clip is the transcript read aloud by the phone's text-to-speech, saved once to the app's
 * cache so the normal player can play it.
 */
object SampleNote {
    /** The sample's id. Negative, so it never collides with a real note (ids start at 1; 0 is chat). */
    const val ID = -100L
    const val PATH = "srutam://welcome"
    const val TITLE = "Srutam welcome note"
    const val DURATION_MS = 38_000L
    private const val CLIP_FILE = "srutam_welcome_note.wav"
    private const val TAG = "SampleNote"

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
        "A sample note could show new users what Srutam does",
        "Reminder: check the install numbers on Monday, 9:00 AM"
    )
    val TASKS = listOf("Send the release notes to Priya", "Book a call with the design team")
    const val IDEA = "Show a sample note so new users see what Srutam does before they record"
    const val DECISION = "Launch the new onboarding on Friday"
    const val REMINDER = "Check the install numbers · Monday, 9:00 AM"

    /** Key points as the JSON array the Recording entity stores. */
    val keyPointsJson: String
        get() = jsonArray(KEY_POINTS)

    private fun jsonArray(items: List<String>) = items.joinToString(prefix = "[", postfix = "]") {
        "\"" + it.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }

    /** Shown until the first real note exists. */
    fun shouldShow(hasRealNotes: Boolean) = !hasRealNotes

    fun isSample(recordingId: Long?) = recordingId == ID

    fun clipFile(context: Context) = File(context.applicationContext.cacheDir, CLIP_FILE)

    /** When the sample "was recorded": the install time, so it reads like the newest note. */
    private fun timestamp(context: Context): Long = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
    }.getOrDefault(System.currentTimeMillis())

    private fun durationOf(file: File): Long {
        if (!file.isFile) return DURATION_MS
        return runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            } finally {
                retriever.release()
            }
        }.getOrNull() ?: DURATION_MS
    }

    /** The sample as a feed item. Its path is the clip, so the feed's player plays it. */
    fun audioFile(context: Context): AudioFileInfo {
        val clip = clipFile(context)
        return AudioFileInfo(
            filePath = clip.absolutePath,
            fileName = "$TITLE.m4a",
            duration = durationOf(clip),
            timestamp = timestamp(context),
            sizeBytes = clip.length()
        )
    }

    /** The sample as a note, for the feed card and the note screen. */
    fun recording(context: Context): Recording {
        val clip = clipFile(context)
        return Recording(
            id = ID,
            timestamp = timestamp(context),
            audioFilePath = clip.absolutePath,
            duration = durationOf(clip),
            name = TITLE,
            transcript = TRANSCRIPT,
            summary = SUMMARY,
            keyPoints = keyPointsJson,
            actionItems = jsonArray(TASKS.map { "[ ] $it" }),
            wiifm = WIIFM,
            aiStatus = RecordingAiStatus.READY,
            isProcessing = false
        )
    }

    /** Its tasks, idea and decision, as the note screen shows them. */
    fun insights(): List<InsightEntity> {
        val tasks = TASKS.mapIndexed { i, text ->
            InsightEntity(id = "sample-task-$i", recordingId = ID, recordingName = TITLE, kind = InsightKind.ACTION, text = text, sourceOrder = i)
        }
        return tasks + listOf(
            InsightEntity(id = "sample-idea", recordingId = ID, recordingName = TITLE, kind = InsightKind.IDEA, text = IDEA, status = InsightStatus.OPEN),
            InsightEntity(id = "sample-decision", recordingId = ID, recordingName = TITLE, kind = InsightKind.DECISION, text = DECISION)
        )
    }

    private val clipLock = Mutex()

    /**
     * Makes the clip once with text-to-speech and returns it, or null when the phone has no
     * text-to-speech voice. Safe to call repeatedly.
     */
    suspend fun ensureClip(context: Context): File? = clipLock.withLock {
        val clip = clipFile(context)
        if (clip.isFile && clip.length() > 0) return clip
        val app = context.applicationContext
        val made = withContext(Dispatchers.Main) {
            withTimeoutOrNull(15_000L) { synthesize(app, clip) }
        } == true
        if (made && clip.isFile && clip.length() > 0) clip else null.also { clip.delete() }
    }

    private suspend fun synthesize(context: Context, target: File): Boolean = suspendCancellableCoroutine { cont ->
        var engine: TextToSpeech? = null
        fun finish(ok: Boolean) {
            engine?.shutdown()
            if (cont.isActive) cont.resume(ok)
        }
        engine = TextToSpeech(context) { status ->
            val tts = engine
            if (status != TextToSpeech.SUCCESS || tts == null) {
                finish(false)
                return@TextToSpeech
            }
            val english = listOf(Locale.US, Locale.UK, Locale.ENGLISH)
                .firstOrNull { tts.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }
            tts.language = english ?: Locale.getDefault()
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) = finish(true)
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = finish(false)
                override fun onError(utteranceId: String?, errorCode: Int) = finish(false)
            })
            val queued = tts.synthesizeToFile(TRANSCRIPT, null, target, "srutam_welcome_note")
            if (queued != TextToSpeech.SUCCESS) {
                Log.w(TAG, "Text-to-speech couldn't make the sample clip")
                finish(false)
            }
        }
        cont.invokeOnCancellation { engine?.shutdown() }
    }
}

/** The small tag that marks the sample note on its card and screen. */
@Composable
fun SampleBadge(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Sem.amberContainer,
        border = BorderStroke(1.dp, Sem.amberBorder),
        modifier = modifier
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
