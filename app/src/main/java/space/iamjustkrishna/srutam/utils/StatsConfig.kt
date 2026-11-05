package space.iamjustkrishna.srutam.utils

import java.time.DayOfWeek

/**
 * Every number behind "time saved" and Your Srutam, in one place. Screens and the maths read these
 * and never write their own, so changing how it is counted or when things appear is one edit here.
 */
object StatsConfig {
    /** What an average person types, in words a minute. Typing time is counted at this speed. */
    const val TYPING_WPM = 40

    /** Under this many seconds saved a note is not worth a mention. It is also when Your Srutam first opens. */
    const val MIN_SECONDS_TO_SHOW = 5L

    /** The line under "Srutam" on Notes stays hidden until this much is saved. */
    const val HEADER_LINE_SECONDS = 60L

    /** The first goal: five minutes saved. */
    const val FIRST_MILESTONE_SECONDS = 300L

    /** Words needed before the speaking pace is worth showing. */
    const val MIN_WORDS_FOR_PACE = 20

    // ---- buttons on the stats screens, in dp ----
    const val BUTTON_HEIGHT_DP = 44
    const val BUTTON_COMPACT_DP = 32

    // ---- the one-time tip under the header line ----
    const val TIP_DELAY_MS = 1_500L
    const val TIP_VISIBLE_MS = 7_000L

    // ---- the Sunday recap ----
    val RECAP_DAY: DayOfWeek = DayOfWeek.SUNDAY
    const val RECAP_HOUR = 18

    // ---- milestones ----
    private const val HOUR = 3_600L

    /** The early ladder, in seconds. */
    private val BASE_MILESTONES: List<Long> =
        listOf(300L, 1_800L, HOUR, 6 * HOUR, 12 * HOUR, 24 * HOUR, 48 * HOUR, 100 * HOUR)

    /** After the early ladder there is one every this many hours, for ever. */
    const val MILESTONE_REPEAT_HOURS = 100L

    /** How many badges the row on Your Srutam shows, and how many of those may be ones already reached. */
    const val BADGES_SHOWN = 5
    const val BADGES_REACHED_SHOWN = 2

    // ---- Insights counts ----
    /** A page of ideas, steps or decisions, and how many more each "Show more" adds. */
    const val INSIGHTS_PAGE = 50

    /** Counts of this many and over are shortened ("1.2k") so a tab never overflows. */
    const val COUNT_SHORT_FROM = 1_000
    const val COUNT_CAP = 99_000

    /** Every milestone, lowest first, without end. */
    fun milestones(): Sequence<Long> = sequence {
        yieldAll(BASE_MILESTONES)
        var step = (MILESTONE_REPEAT_HOURS * 2) * HOUR
        while (true) {
            yield(step)
            step += MILESTONE_REPEAT_HOURS * HOUR
        }
    }

    /** The first milestone above [seconds]. Never null. */
    fun nextAfter(seconds: Long): Long = milestones().first { it > seconds }

    /** The milestones at or below [seconds], lowest first. */
    fun reachedBy(seconds: Long): List<Long> = milestones().takeWhile { it <= seconds }.toList()

    /** The highest milestone reached that is above [lastCelebrated], or null. */
    fun pending(seconds: Long, lastCelebrated: Long): Long? =
        reachedBy(seconds).lastOrNull()?.takeIf { it > lastCelebrated }

    /** 1,240 becomes "1.2k", 12,480 "12k", 130,000 "99k+". Under [COUNT_SHORT_FROM] it is shown as is. */
    fun shortCount(n: Int): String = when {
        n < COUNT_SHORT_FROM -> n.toString()
        n >= COUNT_CAP -> "${COUNT_CAP / 1000}k+"
        n < 10_000 -> "${n / 1000}.${(n % 1000) / 100}k"
        else -> "${n / 1000}k"
    }

    /** The badges to draw: how many reached ones are folded away, and the visible steps. */
    data class Badges(val earlier: Int, val steps: List<Long>)

    fun badges(seconds: Long): Badges {
        val reached = reachedBy(seconds)
        val shownReached = reached.takeLast(BADGES_REACHED_SHOWN)
        val upcoming = milestones().dropWhile { it <= seconds }.take(BADGES_SHOWN - shownReached.size).toList()
        return Badges(reached.size - shownReached.size, shownReached + upcoming)
    }
}
