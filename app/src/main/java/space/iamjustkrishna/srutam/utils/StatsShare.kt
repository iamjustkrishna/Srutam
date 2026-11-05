package space.iamjustkrishna.srutam.utils

import java.net.URLEncoder
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

/** The moments worth marking as the total time saved grows (the ladder lives in [StatsConfig]). Only the highest one crossed is shown. */
object TimeSavedMilestones {
    /** The highest step reached and not yet celebrated, or null. A big jump (a restore) celebrates once, not once per step. */
    fun pending(allTimeSeconds: Long, lastCelebrated: Long): Long? = StatsConfig.pending(allTimeSeconds, lastCelebrated)

    /** "30 minutes", "6 hours". */
    fun label(step: Long): String = TimeSaved.words(step)

    /** The name of a milestone, for the sheet's title. */
    fun title(step: Long): String {
        val hours = step / 3_600L
        return when {
            step == 300L -> "First five minutes"
            step == 1_800L -> "Half an hour back"
            hours == 1L -> "An hour back"
            hours == 6L -> "Six hours back"
            hours == 12L -> "Half a day back"
            hours == 24L -> "A whole day"
            hours == 48L -> "Two whole days"
            hours == 100L -> "A hundred hours"
            else -> "${java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(hours)} hours back"
        }
    }

    /** "5m", "6h", "1,200h": for a small badge. */
    fun badge(step: Long): String =
        if (step < 3_600L) "${step / 60}m" else "${java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(step / 3_600L)}h"
}

/** The Sunday note: how much talking saved over the past week. */
object WeeklyRecap {
    private const val HOUR = StatsConfig.RECAP_HOUR

    /** The next Sunday at [HOUR]:00 after [now], in [now]'s zone. */
    fun nextRun(now: ZonedDateTime): ZonedDateTime {
        var run = now.with(java.time.temporal.TemporalAdjusters.nextOrSame(StatsConfig.RECAP_DAY))
            .withHour(HOUR).withMinute(0).withSecond(0).withNano(0)
        if (!run.isAfter(now)) run = run.plusWeeks(1)
        return run
    }

    /** Title and text for the notification, or null when the week saved under a minute. */
    fun message(week: TimePeriod): Pair<String, String>? {
        if (week.savedSeconds < StatsConfig.NOTABLE_SECONDS) return null
        val notes = if (week.notes == 1) "1 note" else "${week.notes} notes"
        return "Your week in Srutam" to "You saved ${TimeSaved.hero(week.savedSeconds)} by talking this week, across $notes."
    }
}
