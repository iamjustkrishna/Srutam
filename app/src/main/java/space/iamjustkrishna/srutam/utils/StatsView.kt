package space.iamjustkrishna.srutam.utils

import space.iamjustkrishna.srutam.data.Recording
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** The range the Your Srutam hero shows. */
enum class StatsRange(val label: String) {
    WEEK("Week"),
    MONTH("Month"),
    ALL("All time")
}

/** One bar of the chart. [seconds] is the time saved that day (or month). */
data class StatsBar(val seconds: Long, val isBest: Boolean, val isFuture: Boolean)

/**
 * Everything the Your Srutam screen shows for one range, worked out from the notes. Only lengths and
 * word counts go in, never a note's text.
 */
data class StatsView(
    val range: StatsRange,
    /** The small caps line over the number: "THIS MONTH", "LAST 7 DAYS", "ALL TIME". */
    val eyebrow: String,
    val savedSeconds: Long,
    /** "1h 12m more than September", or null when there is nothing to compare with. */
    val deltaText: String?,
    val deltaIsGain: Boolean,
    val bars: List<StatsBar>,
    /** Three labels under the chart: first, middle, last bar. */
    val axis: List<String>,
    /** "Best day Oct 14 · 1h 12m saved", or null. */
    val bestText: String?,
    val period: TimePeriod,
    /** Words a minute while speaking, for the notes in this range; 0 when there isn't enough to say. */
    val paceWpm: Int,
    /** How many times faster that is than typing, one decimal ("3.5"), or empty. */
    val paceMultiple: String,
    /** What the share card calls this range. */
    val cardLabel: String
)

object StatsViews {

    /** Under this many words there is no pace worth quoting. */

    fun build(
        recordings: Collection<Recording>,
        range: StatsRange,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault()
    ): StatsView {
        val perDay = savedPerDay(recordings, zone)
        fun sum(from: LocalDate, to: LocalDate): Long =
            perDay.entries.filter { !it.key.isBefore(from) && !it.key.isAfter(to) }.sumOf { it.value }
        fun startMs(d: LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli()

        return when (range) {
            StatsRange.WEEK -> {
                val first = today.minusDays(6)
                val days = (0..6).map { first.plusDays(it.toLong()) }
                val values = days.map { perDay[it] ?: 0L }
                val previous = sum(first.minusDays(7), first.minusDays(1))
                val total = values.sum()
                val bestIndex = bestIndex(values)
                fun dayName(d: LocalDate) = d.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
                StatsView(
                    range = range,
                    eyebrow = "LAST 7 DAYS",
                    savedSeconds = total,
                    deltaText = delta(total, previous, "the week before"),
                    deltaIsGain = total >= previous,
                    bars = bars(values, bestIndex, futureFrom = null),
                    axis = listOf(dayName(days[0]), dayName(days[3]), "Today"),
                    bestText = bestIndex?.let { "Best day ${dayName(days[it])} · ${TimeSaved.hero(values[it])} saved" },
                    period = TimeSaved.period(recordings, startMs(first)),
                    paceWpm = 0, paceMultiple = "", cardLabel = "Last 7 days"
                ).withPace()
            }

            StatsRange.MONTH -> {
                val month = YearMonth.from(today)
                val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
                val values = days.map { perDay[it] ?: 0L }
                val total = values.sum()
                val previousMonth = month.minusMonths(1)
                val previous = sum(previousMonth.atDay(1), previousMonth.atEndOfMonth())
                val bestIndex = bestIndex(values)
                val monthName = month.month.getDisplayName(TextStyle.SHORT, locale)
                StatsView(
                    range = range,
                    eyebrow = "THIS MONTH",
                    savedSeconds = total,
                    deltaText = delta(total, previous, previousMonth.month.getDisplayName(TextStyle.FULL, locale)),
                    deltaIsGain = total >= previous,
                    bars = bars(values, bestIndex, futureFrom = today.dayOfMonth),
                    axis = listOf("$monthName 1", "$monthName 15", "$monthName ${month.lengthOfMonth()}"),
                    bestText = bestIndex?.let { "Best day $monthName ${it + 1} · ${TimeSaved.hero(values[it])} saved" },
                    period = TimeSaved.period(recordings, startMs(month.atDay(1))),
                    paceWpm = 0, paceMultiple = "",
                    cardLabel = month.month.getDisplayName(TextStyle.FULL, locale)
                ).withPace()
            }

            StatsRange.ALL -> {
                val thisMonth = YearMonth.from(today)
                val months = (11 downTo 0).map { thisMonth.minusMonths(it.toLong()) }
                val values = months.map { m -> sum(m.atDay(1), m.atEndOfMonth()) }
                val bestIndex = bestIndex(values)
                fun name(m: YearMonth) = m.month.getDisplayName(TextStyle.SHORT, locale)
                StatsView(
                    range = range,
                    eyebrow = "ALL TIME",
                    savedSeconds = perDay.values.sum(),
                    deltaText = null,
                    deltaIsGain = false,
                    bars = bars(values, bestIndex, futureFrom = null),
                    axis = listOf(name(months.first()), name(months[months.size / 2]), name(months.last())),
                    bestText = bestIndex?.let { "Best month ${name(months[it])} · ${TimeSaved.hero(values[it])} saved" },
                    period = TimeSaved.period(recordings, 0L),
                    paceWpm = 0, paceMultiple = "", cardLabel = "All time"
                ).withPace()
            }
        }
    }

    /** Time saved on each day, in the given zone. Notes without a transcript count for nothing. */
    fun savedPerDay(recordings: Collection<Recording>, zone: ZoneId): Map<LocalDate, Long> {
        val out = HashMap<LocalDate, Long>()
        for (n in recordings) {
            if (n.transcript.isNullOrBlank()) continue
            val saved = TimeSaved.of(n.transcript, n.duration).savedSeconds
            if (saved <= 0) continue
            val day = Instant.ofEpochMilli(n.timestamp).atZone(zone).toLocalDate()
            out[day] = (out[day] ?: 0L) + saved
        }
        return out
    }

    private fun StatsView.withPace(): StatsView {
        val words = period.words
        val spoken = period.spokenSeconds
        if (words < StatsConfig.MIN_WORDS_FOR_PACE || spoken <= 0) return this
        val wpm = (words * 60.0 / spoken).toInt()
        val multiple = wpm.toDouble() / StatsConfig.TYPING_WPM
        return copy(paceWpm = wpm, paceMultiple = if (multiple >= 1.05) String.format(Locale.US, "%.1f", multiple) else "")
    }

    /** The tallest bar, unless it is under a minute (nothing worth lighting up). */
    private fun bestIndex(values: List<Long>): Int? {
        val max = values.maxOrNull() ?: return null
        return if (max >= StatsConfig.HEADER_LINE_SECONDS) values.indexOf(max) else null
    }

    private fun bars(values: List<Long>, best: Int?, futureFrom: Int?): List<StatsBar> =
        values.mapIndexed { i, v -> StatsBar(v, i == best, futureFrom != null && i + 1 > futureFrom) }

    /** "1h 12m more than September", or less; null when there was nothing before or it is the same. */
    private fun delta(now: Long, before: Long, than: String): String? {
        if (before < StatsConfig.HEADER_LINE_SECONDS || now == before) return null
        val diff = kotlin.math.abs(now - before)
        val word = if (now > before) "more" else "less"
        return "${TimeSaved.hero(diff)} $word than $than"
    }
}
