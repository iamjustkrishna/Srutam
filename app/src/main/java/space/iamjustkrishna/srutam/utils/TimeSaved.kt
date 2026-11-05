package space.iamjustkrishna.srutam.utils

import kotlin.math.max
import kotlin.math.roundToLong

/**
 * How much time talking a note saved, over typing the same words.
 *
 * Typing is counted at [StatsConfig.TYPING_WPM], the average person's speed. Speaking is not
 * estimated: it's the real length of the recording. Only the length and the word count of a note
 * go in, never its content, so totals are safe to show and share.
 */
object TimeSaved {
    fun wordCount(text: String?): Int =
        text?.trim()?.takeIf { it.isNotEmpty() }?.split(Regex("\\s+"))?.size ?: 0

    /** One note's numbers. */
    data class Result(
        val words: Int,
        val spokenSeconds: Long,
        val typingSeconds: Long,
        val savedSeconds: Long
    ) {
        /** Worth a celebration: it saved at least [StatsConfig.MIN_SECONDS_TO_SHOW]. */
        val worthShowing: Boolean get() = savedSeconds >= StatsConfig.MIN_SECONDS_TO_SHOW

        /** How far to the first milestone, 0..1. */
        val milestoneProgress: Float get() = (savedSeconds.toFloat() / StatsConfig.FIRST_MILESTONE_SECONDS).coerceIn(0f, 1f)

        val reachedFirstMilestone: Boolean get() = savedSeconds >= StatsConfig.FIRST_MILESTONE_SECONDS

        val secondsToFirstMilestone: Long get() = max(0L, StatsConfig.FIRST_MILESTONE_SECONDS - savedSeconds)
    }

    /** [spokenMs] is the recording's length. */
    fun of(words: Int, spokenMs: Long): Result {
        val spoken = (spokenMs / 1000.0).roundToLong()
        val typing = (words * 60.0 / StatsConfig.TYPING_WPM).roundToLong()
        return Result(words, spoken, typing, max(0L, typing - spoken))
    }

    fun of(transcript: String?, spokenMs: Long): Result = of(wordCount(transcript), spokenMs)

    /** Everything saved, for the header line and the Your Srutam screen. */
    data class Totals(
        val thisMonthSeconds: Long,
        val allTimeSeconds: Long,
        val notes: Int,
        val words: Int,
        val spokenSeconds: Long,
        val typingSeconds: Long
    ) {
        /** A month's first days are empty, so the line falls back to all time until it has a minute to show. */
        val headerSeconds: Long get() = if (thisMonthSeconds >= StatsConfig.HEADER_LINE_SECONDS) thisMonthSeconds else allTimeSeconds
        val headerIsThisMonth: Boolean get() = thisMonthSeconds >= StatsConfig.HEADER_LINE_SECONDS

        /** Nothing to show yet: new users never see the line. */
        val worthShowingInHeader: Boolean get() = headerSeconds >= StatsConfig.HEADER_LINE_SECONDS

        /** Enough saved for Your Srutam to open at all. */
        val worthShowingStats: Boolean get() = allTimeSeconds >= StatsConfig.MIN_SECONDS_TO_SHOW
    }

    /** Adds up every note that has a transcript. [monthStartMs] is the first moment of this month. */
    fun totals(notes: Collection<space.iamjustkrishna.srutam.data.Recording>, monthStartMs: Long): Totals {
        var month = 0L
        var all = 0L
        var count = 0
        var words = 0
        var spoken = 0L
        var typing = 0L
        for (n in notes) {
            if (n.transcript.isNullOrBlank()) continue
            val r = of(n.transcript, n.duration)
            count++
            words += r.words
            spoken += r.spokenSeconds
            typing += r.typingSeconds
            all += r.savedSeconds
            if (n.timestamp >= monthStartMs) month += r.savedSeconds
        }
        return Totals(month, all, count, words, spoken, typing)
    }

    /** What notes recorded at or after [sinceMs] add up to. */
    fun period(notes: Collection<space.iamjustkrishna.srutam.data.Recording>, sinceMs: Long): TimePeriod {
        var saved = 0L; var spoken = 0L; var typing = 0L; var count = 0; var words = 0
        for (n in notes) {
            if (n.timestamp < sinceMs || n.transcript.isNullOrBlank()) continue
            val r = of(n.transcript, n.duration)
            saved += r.savedSeconds; spoken += r.spokenSeconds; typing += r.typingSeconds
            count++; words += r.words
        }
        return TimePeriod(saved, spoken, typing, count, words)
    }

    /** The first moment of the current month in the device's time zone. */
    fun monthStart(now: Long = System.currentTimeMillis(), zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): Long =
        java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate().withDayOfMonth(1)
            .atStartOfDay(zone).toInstant().toEpochMilli()

    /**
     * The header line's wordings, longest first: the screen shows the first that fits.
     * `6h 23m saved this month ›` -> `6h 23m saved ›` -> `6h 23m ›` -> `6h ›`.
     */
    fun headerCandidates(t: Totals): List<String> {
        val hero = hero(t.headerSeconds)
        val coarse = hero.substringBefore(' ')
        val tail = if (t.headerIsThisMonth) " saved this month" else " saved"
        return listOf("$hero$tail ›", "$hero saved ›", "$hero ›", "$coarse ›").distinct()
    }

    /** "40 s", "2 min 5 s", "1 h 5 min": short, for numbers beside each other. */
    fun short(seconds: Long): String = when {
        seconds < 60 -> "$seconds s"
        seconds < 3600 -> {
            val m = seconds / 60
            val s = seconds % 60
            if (s == 0L) "$m min" else "$m min $s s"
        }
        else -> {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            if (m == 0L) "$h h" else "$h h $m min"
        }
    }

    /** "40 seconds", "5 minutes 30 seconds", "1 hour 5 minutes": for a sentence. */
    fun words(seconds: Long): String {
        fun unit(n: Long, one: String, many: String) = "$n ${if (n == 1L) one else many}"
        return when {
            seconds < 60 -> unit(seconds, "second", "seconds")
            seconds < 3600 -> {
                val m = seconds / 60
                val s = seconds % 60
                if (s == 0L) unit(m, "minute", "minutes") else "${unit(m, "minute", "minutes")} ${unit(s, "second", "seconds")}"
            }
            else -> {
                val h = seconds / 3600
                val m = (seconds % 3600) / 60
                if (m == 0L) unit(h, "hour", "hours") else "${unit(h, "hour", "hours")} ${unit(m, "minute", "minutes")}"
            }
        }
    }

    /** "6h 23m", the big number on a card. Seconds only while under a minute. */
    fun hero(seconds: Long): String = when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> {
            val m = seconds / 60
            val s = seconds % 60
            if (s == 0L) "${m}m" else "${m}m ${s}s"
        }
        seconds >= 1_000 * 3600L -> "${java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(seconds / 3600)}h"
        else -> {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            "${h}h ${m}m"
        }
    }
}
