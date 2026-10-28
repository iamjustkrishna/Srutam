package space.iamjustkrishna.srutam.utils

import java.net.URLEncoder
import java.time.DayOfWeek
import java.time.ZonedDateTime

/**
 * Where a shared card sends people: the Play Store page, tagged with a campaign so Play Console
 * and Firebase show which cards bring installs (Play passes `referrer` through to the first open).
 */
object ShareLink {
    const val PLAY_URL = "https://play.google.com/store/apps/details?id=space.iamjustkrishna.srutam"

    /** [campaign] says which card it was, [medium] where it was shared from. Both are short slugs. */
    fun tagged(campaign: String, medium: String = "card"): String {
        val referrer = "utm_source=srutam_share&utm_medium=$medium&utm_campaign=$campaign"
        return "$PLAY_URL&referrer=${URLEncoder.encode(referrer, "UTF-8")}"
    }

    /** The words that go with the picture. */
    fun caption(savedSeconds: Long, campaign: String, medium: String = "card"): String =
        "I saved ${TimeSaved.words(savedSeconds)} by talking instead of typing, with Srutam.\n${tagged(campaign, medium)}"
}

/** Totals for a span of time, for the share card and the weekly recap. */
data class TimePeriod(
    val savedSeconds: Long,
    val spokenSeconds: Long,
    val typingSeconds: Long,
    val notes: Int,
    val words: Int
)

/** The moments worth marking as the total time saved grows. Only the highest one crossed is shown. */
object TimeSavedMilestones {
    val STEPS: List<Long> = listOf(300L, 1_800L, 3_600L, 6 * 3_600L, 12 * 3_600L, 24 * 3_600L, 48 * 3_600L, 100 * 3_600L)

    /** The highest step reached and not yet celebrated, or null. A big jump (a restore) celebrates once, not once per step. */
    fun pending(allTimeSeconds: Long, lastCelebrated: Long): Long? =
        STEPS.filter { it <= allTimeSeconds && it > lastCelebrated }.maxOrNull()

    /** "30 minutes", "6 hours". */
    fun label(step: Long): String = TimeSaved.words(step)
}

/** The Sunday note: how much talking saved over the past week. */
object WeeklyRecap {
    const val HOUR = 18

    /** The next Sunday at [HOUR]:00 after [now], in [now]'s zone. */
    fun nextRun(now: ZonedDateTime): ZonedDateTime {
        var run = now.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            .withHour(HOUR).withMinute(0).withSecond(0).withNano(0)
        if (!run.isAfter(now)) run = run.plusWeeks(1)
        return run
    }

    /** Title and text for the notification, or null when the week saved under a minute. */
    fun message(week: TimePeriod): Pair<String, String>? {
        if (week.savedSeconds < TimeSaved.MIN_HEADER_SECONDS) return null
        val notes = if (week.notes == 1) "1 note" else "${week.notes} notes"
        return "Your week in Srutam" to "You saved ${TimeSaved.hero(week.savedSeconds)} by talking this week, across $notes."
    }
}
