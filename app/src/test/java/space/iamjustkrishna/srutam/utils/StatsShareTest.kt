package space.iamjustkrishna.srutam.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import space.iamjustkrishna.srutam.data.Recording
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class StatsShareTest {

    @Test fun theLinkIsTaggedForPlayAndEncoded() {
        val link = ShareLink.tagged("time_saved")
        assertTrue(link.startsWith("https://play.google.com/store/apps/details?id=space.iamjustkrishna.srutam&referrer="))
        // The referrer is one encoded value, so its own & and = can't break the link apart.
        assertEquals(1, link.count { it == '&' })
        assertTrue(link.contains("utm_source%3Dsrutam_share%26utm_medium%3Dcard%26utm_campaign%3Dtime_saved"))
    }

    @Test fun theCaptionSaysWhatWasSaved() {
        val caption = ShareLink.caption(6 * 3600L + 23 * 60, "milestone")
        assertTrue(caption.startsWith("I saved 6 hours 23 minutes by talking instead of typing, with Srutam."))
        assertTrue(caption.contains("utm_campaign%3Dmilestone"))
    }

    @Test fun onlyTheHighestUncelebratedMilestoneShows() {
        assertNull(TimeSavedMilestones.pending(299, 0))
        assertEquals(300L, TimeSavedMilestones.pending(300, 0))
        assertEquals(300L, TimeSavedMilestones.pending(1_000, 0))
        assertNull(TimeSavedMilestones.pending(1_000, 300))
        assertEquals(1_800L, TimeSavedMilestones.pending(2_000, 300))
        // A restore that jumps past several steps celebrates the top one once.
        assertEquals(12 * 3_600L, TimeSavedMilestones.pending(13 * 3_600L, 0))
        assertNull(TimeSavedMilestones.pending(13 * 3_600L, 12 * 3_600L))
    }

    @Test fun milestonesReadNaturally() {
        assertEquals("5 minutes", TimeSavedMilestones.label(300))
        assertEquals("1 hour", TimeSavedMilestones.label(3_600))
        assertEquals("12 hours", TimeSavedMilestones.label(12 * 3_600L))
    }

    @Test fun theRecapComesOnTheNextSundayEvening() {
        val zone = ZoneId.of("Asia/Kolkata")
        // Wednesday 2026-10-28, 10:00.
        val wed = ZonedDateTime.of(2026, 10, 28, 10, 0, 0, 0, zone)
        val run = WeeklyRecap.nextRun(wed)
        assertEquals(DayOfWeek.SUNDAY, run.dayOfWeek)
        assertEquals(1, run.dayOfMonth)
        assertEquals(11, run.monthValue)
        assertEquals(18, run.hour)

        // Sunday before the hour: the same day. Sunday after it: a week later.
        val sunMorning = ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, zone)
        assertEquals(1, WeeklyRecap.nextRun(sunMorning).dayOfMonth)
        val sunNight = ZonedDateTime.of(2026, 11, 1, 19, 0, 0, 0, zone)
        assertEquals(8, WeeklyRecap.nextRun(sunNight).dayOfMonth)
    }

    @Test fun aQuietWeekSendsNothing() {
        assertNull(WeeklyRecap.message(TimePeriod(30, 10, 40, 1, 25)))
        val message = WeeklyRecap.message(TimePeriod(4_980, 1_000, 5_980, 9, 700))
        assertNotNull(message)
        val (title, text) = message!!
        assertEquals("Your week in Srutam", title)
        assertEquals("You saved 1h 23m by talking this week, across 9 notes.", text)
        assertEquals("You saved 2m by talking this week, across 1 note.", WeeklyRecap.message(TimePeriod(120, 30, 150, 1, 100))!!.second)
    }

    @Test fun periodTotalsOnlyCountNotesInsideTheSpan() {
        fun note(ts: Long, words: Int, ms: Long) =
            Recording(timestamp = ts, audioFilePath = "p$ts", duration = ms, transcript = List(words) { "w" }.joinToString(" "))
        val week = TimeSaved.period(
            listOf(note(10_000, 35, 12_000), note(1_000, 35, 12_000)),
            sinceMs = 5_000
        )
        assertEquals(41, week.savedSeconds)
        assertEquals(1, week.notes)
        assertEquals(35, week.words)
        assertEquals(12, week.spokenSeconds)
        assertEquals(53, week.typingSeconds)
    }
}
