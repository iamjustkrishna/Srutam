package space.iamjustkrishna.srutam.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeSavedTest {

    @Test fun wordCountIgnoresExtraSpaceAndNull() {
        assertEquals(0, TimeSaved.wordCount(null))
        assertEquals(0, TimeSaved.wordCount("   "))
        assertEquals(5, TimeSaved.wordCount("  send   the release\nnotes now "))
    }

    @Test fun aShortNoteSavesTheDifferenceBetweenTypingAndTalking() {
        // 35 words typed at 40 a minute is 52.5 s; said in 12 s.
        val r = TimeSaved.of(words = 35, spokenMs = 12_000)

        assertEquals(12, r.spokenSeconds)
        assertEquals(53, r.typingSeconds)
        assertEquals(41, r.savedSeconds)
        assertTrue(r.worthShowing)
    }

    @Test fun anythingThatSavedNothingIsNotWorthShowing() {
        val slow = TimeSaved.of(words = 2, spokenMs = 4_000) // typing 3 s, said in 4 s
        assertEquals(0, slow.savedSeconds)
        assertFalse(slow.worthShowing)

        // 5 words is 8 s of typing against 4 s of talking: 4 s saved, under the five worth showing.
        assertFalse(TimeSaved.of(words = 5, spokenMs = 4_000).worthShowing)
        assertTrue(TimeSaved.of(words = 6, spokenMs = 4_000).worthShowing)
    }

    @Test fun theFirstMilestoneIsFiveMinutes() {
        val some = TimeSaved.of(words = 35, spokenMs = 12_000)
        assertEquals(259, some.secondsToFirstMilestone)
        assertTrue(some.milestoneProgress > 0.13f && some.milestoneProgress < 0.14f)
        assertFalse(some.reachedFirstMilestone)

        val long = TimeSaved.of(words = 300, spokenMs = 120_000) // typing 450 s, said in 120 s -> 330 s
        assertTrue(long.reachedFirstMilestone)
        assertEquals(1f, long.milestoneProgress, 0f)
        assertEquals(0, long.secondsToFirstMilestone)
    }

    @Test fun durationsReadNaturally() {
        assertEquals("40 s", TimeSaved.short(40))
        assertEquals("2 min", TimeSaved.short(120))
        assertEquals("2 min 5 s", TimeSaved.short(125))
        assertEquals("1 h 5 min", TimeSaved.short(3900))

        assertEquals("1 second", TimeSaved.words(1))
        assertEquals("40 seconds", TimeSaved.words(40))
        assertEquals("5 minutes 30 seconds", TimeSaved.words(330))
        assertEquals("1 hour", TimeSaved.words(3600))
        assertEquals("6 hours 23 minutes", TimeSaved.words(6 * 3600 + 23 * 60))

        assertEquals("40s", TimeSaved.hero(40))
        assertEquals("5m 30s", TimeSaved.hero(330))
        assertEquals("6h 23m", TimeSaved.hero(6 * 3600 + 23 * 60))
    }

    private fun note(words: Int, spokenMs: Long, ts: Long) = space.iamjustkrishna.srutam.data.Recording(
        timestamp = ts, audioFilePath = "p$ts", duration = spokenMs, transcript = List(words) { "w" }.joinToString(" ")
    )

    @Test fun totalsSplitThisMonthFromAllTime() {
        val monthStart = 1_000L
        val totals = TimeSaved.totals(
            listOf(
                note(35, 12_000, ts = 2_000),   // this month: 41 s
                note(35, 12_000, ts = 500),     // before: 41 s
                space.iamjustkrishna.srutam.data.Recording(timestamp = 3_000, audioFilePath = "empty", duration = 9_000, transcript = null)
            ),
            monthStart
        )
        assertEquals(41, totals.thisMonthSeconds)
        assertEquals(82, totals.allTimeSeconds)
        assertEquals(2, totals.notes)
        assertEquals(70, totals.words)
    }

    @Test fun headerHidesUnderAMinuteAndFallsBackToAllTime() {
        val fresh = TimeSaved.Totals(30, 45, 1, 20, 10, 55)
        assertFalse(fresh.worthShowingInHeader)

        val earlyInMonth = TimeSaved.Totals(20, 3 * 3600L + 5 * 60, 9, 500, 100, 900)
        assertTrue(earlyInMonth.worthShowingInHeader)
        assertFalse(earlyInMonth.headerIsThisMonth)
        assertEquals(3 * 3600L + 5 * 60, earlyInMonth.headerSeconds)

        val steady = TimeSaved.Totals(6 * 3600L + 23 * 60, 20 * 3600L, 90, 5000, 100, 900)
        assertTrue(steady.headerIsThisMonth)
        assertEquals(6 * 3600L + 23 * 60, steady.headerSeconds)
    }

    @Test fun headerWordingsGoFromLongestToShortest() {
        val steady = TimeSaved.Totals(6 * 3600L + 23 * 60, 0, 1, 1, 1, 1)
        assertEquals(
            listOf("6h 23m saved this month \u203A", "6h 23m saved \u203A", "6h 23m \u203A", "6h \u203A"),
            TimeSaved.headerCandidates(steady)
        )
        val early = TimeSaved.Totals(0, 4 * 60L, 1, 1, 1, 1)
        assertEquals(listOf("4m saved \u203A", "4m \u203A"), TimeSaved.headerCandidates(early))
    }
}
