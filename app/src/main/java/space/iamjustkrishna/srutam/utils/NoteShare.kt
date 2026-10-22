package space.iamjustkrishna.srutam.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.InsightKind
import space.iamjustkrishna.srutam.data.InsightStatus
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.ReminderEntity
import space.iamjustkrishna.srutam.data.ReminderStatus
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Sharing a note as text, optionally with its audio. The text is plain, so it reads well in
 * WhatsApp, Gmail or Notes alike; only the title uses `*bold*`, which chat apps render and
 * others show harmlessly.
 */
object NoteShare {

    /** What goes out: the note as text, its recording, or both. */
    enum class Mode(val label: String) {
        TEXT("Text"),
        AUDIO("Audio"),
        BOTH("Text + audio")
    }

    data class Options(
        val mode: Mode = Mode.TEXT,
        /** Summary, key points, tasks, ideas, decisions and reminders. */
        val summary: Boolean = true,
        val transcript: Boolean = false,
        val footer: Boolean = true
    ) {
        val sendsText: Boolean get() = mode != Mode.AUDIO
        val sendsAudio: Boolean get() = mode != Mode.TEXT
    }

    const val FOOTER = "— shared from Srutam"

    private class Formats(locale: Locale) {
        val date: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a", locale)
        val day: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", locale)
        val time: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", locale)
    }

    private fun jsonList(json: String?): List<String> = try {
        if (json.isNullOrBlank()) emptyList()
        else Gson().fromJson(json, Array<String>::class.java)?.toList().orEmpty().filter { it.isNotBlank() }
    } catch (e: Exception) {
        emptyList()
    }

    /** The note as share text. [zone] and [locale] decide how times read. */
    fun text(
        recording: Recording,
        insights: List<InsightEntity>,
        reminders: List<ReminderEntity>,
        options: Options,
        zone: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault()
    ): String = buildString {
        val formats = Formats(locale)
        val title = recording.name.ifBlank { "Voice note" }
        val date = Instant.ofEpochMilli(recording.timestamp).atZone(zone).format(formats.date)
        append("*").append(title).append("*").append(" — ").append(date)

        fun section(heading: String, lines: List<String>) {
            if (lines.isEmpty()) return
            append("\n\n").append(heading)
            lines.forEach { append("\n").append(it) }
        }

        if (options.summary) {
            recording.summary?.trim()?.takeIf { it.isNotEmpty() }?.let { append("\n\n").append(it) }
            section("Key points", jsonList(recording.keyPoints).map { "• $it" })

            val visible = insights.filter { it.status != InsightStatus.ARCHIVED }.sortedBy { it.sourceOrder }
            val tasks = visible.filter { it.kind == InsightKind.ACTION }.map {
                (if (it.status == InsightStatus.COMPLETED) "☑ " else "☐ ") + it.text
            }.ifEmpty {
                // Notes from before tasks had their own rows keep them as "[ ] text" in actionItems.
                jsonList(recording.actionItems).map { item ->
                    val done = item.startsWith("[x]", ignoreCase = true)
                    (if (done) "☑ " else "☐ ") + item.removePrefix("[ ]").removePrefix("[x]").removePrefix("[X]").trim()
                }
            }
            section("Tasks", tasks)
            section("Ideas", visible.filter { it.kind == InsightKind.IDEA }.map { "💡 ${it.text}" })
            section("Decisions", visible.filter { it.kind == InsightKind.DECISION }.map { decision ->
                "✔ ${decision.text}" + (decision.rationale?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: "")
            })
            section("Reminders", reminders.filter { it.status == ReminderStatus.ACTIVE }.map { "🔔 " + reminderLine(it, zone, formats) })
        }

        if (options.transcript) {
            recording.transcript?.trim()?.takeIf { it.isNotEmpty() }?.let {
                append("\n\n")
                if (options.summary) append("Transcript\n")
                append(it)
            }
        }

        if (options.footer) append("\n\n").append(FOOTER)
    }

    private fun reminderLine(reminder: ReminderEntity, zone: ZoneId, formats: Formats): String {
        val time = when {
            reminder.eventTimeMs != null -> Instant.ofEpochMilli(reminder.eventTimeMs).atZone(zone).let {
                it.format(formats.day) + ", " + it.format(formats.time)
            }
            reminder.localDate != null -> runCatching {
                val day = LocalDate.parse(reminder.localDate).format(formats.day)
                val at = reminder.localTime?.let { t -> LocalTime.parse(t).format(formats.time) }
                if (at != null) "$day, $at" else day
            }.getOrNull()
            else -> null
        }
        return if (time != null) "${reminder.title} · $time" else reminder.title
    }

    /**
     * Opens the share sheet with [text] and/or the note's audio (without its note block).
     * With both, the text rides along as the caption; apps that drop captions on files (WhatsApp)
     * get it from the clipboard, so callers copy it too.
     */
    fun share(context: Context, text: String?, audio: File?) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            if (text != null) putExtra(Intent.EXTRA_TEXT, text)
            if (audio != null) {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, AudioShare.cleanUri(context, audio))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
        }
        context.startActivity(Intent.createChooser(intent, "Share note"))
    }

    fun copy(context: Context, text: String) {
        context.getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("Srutam note", text))
    }

    // ---- Remembered choices ----

    private const val PREFS = "srutam_note_share"

    fun savedOptions(context: Context): Options {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val summary = prefs.getBoolean("summary", true)
        val transcript = prefs.getBoolean("transcript", false)
        return Options(
            mode = runCatching { Mode.valueOf(prefs.getString("mode", null) ?: "") }.getOrDefault(Mode.TEXT),
            summary = summary || !transcript,
            transcript = transcript,
            footer = prefs.getBoolean("footer", true)
        )
    }

    fun saveOptions(context: Context, options: Options) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("mode", options.mode.name)
            .putBoolean("summary", options.summary)
            .putBoolean("transcript", options.transcript)
            .putBoolean("footer", options.footer)
            .apply()
    }
}
