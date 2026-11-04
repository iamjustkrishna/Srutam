package space.iamjustkrishna.srutam.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import space.iamjustkrishna.srutam.data.Recording
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class StatsViewTest {
    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 10, 28)

    /** 600 words said in 120 s: typing would take 900 s, so 780 s (13 minutes) saved. */
    private fun note(date: LocalDate, words: Int = 600, spokenMs: Long = 120_000) = Recording(
        timestamp = ZonedDateTime.of(date.year, date.monthValue, date.dayOfMonth, 12, 0, 0, 0, zone).toInstant().toEpochMilli(),
        audioFilePath = "p$date",
        duration = spokenMs,
        transcript = List(words) { "w" }.joinToString(" ")
    )

    private fun build(notes: List<Recording>, range: StatsRange) =
        StatsViews.build(notes, range, today, zone, Locale.US)

    @Test fun theMonthAddsUpItsDaysAndComparesWithTheLastMonth() {
        val view = build(listOf(note(LocalDate.of(2026, 10, 3)), note(LocalDate.of(2026, 10, 14)), note(LocalDate.of(2026, 9, 20))), StatsRange.MONTH)

        assertEquals("THIS MONTH", view.eyebrow)
        assertEquals(1_560, view.savedSeconds)
        assertEquals("13m more than September", view.deltaText)
        assertTrue(view.deltaIsGain)
        assertEquals(31, view.bars.size)
        assertEquals("Best day Oct 3 · 13m saved", view.bestText)
        assertEquals(listOf("Oct 1", "Oct 15", "Oct 31"), view.axis)
        assertEquals("October", view.cardLabel)
        // Days that haven't happened yet are marked, so the chart can draw them faintly.
        assertFalse(view.bars[27].isFuture)
        assertTrue(view.bars[28].isFuture)
    }

    @Test fun aSlowerMonthSaysLessNotMore() {
        val view = build(listOf(note(LocalDate.of(2026, 10, 3)), note(LocalDate.of(2026, 9, 1)), note(LocalDate.of(2026, 9, 2))), StatsRange.MONTH)
        assertEquals("13m less than September", view.deltaText)
        assertFalse(view.deltaIsGain)
    }

    @Test fun noComparisonWhenTheMonthBeforeHadNothing() {
        assertNull(build(listOf(note(LocalDate.of(2026, 10, 3))), StatsRange.MONTH).deltaText)
    }

    @Test fun theWeekIsTheLastSevenDaysEndingToday() {
        val view = build(
            listOf(note(LocalDate.of(2026, 10, 27)), note(LocalDate.of(2026, 10, 22)), note(LocalDate.of(2026, 10, 21))),
            StatsRange.WEEK
        )
        assertEquals("LAST 7 DAYS", view.eyebrow)
        assertEquals(7, view.bars.size)
        // Oct 22 to Oct 28 holds the 27th and the 22nd; the 21st is in the week before.
        assertEquals(1_560, view.savedSeconds)
        assertEquals("13m more than the week before", view.deltaText)
        assertEquals("Today", view.axis.last())
        assertEquals("Last 7 days", view.cardLabel)
    }

    @Test fun allTimeShowsTwelveMonthsAndTheBestOne() {
        val view = build(
            listOf(note(LocalDate.of(2026, 10, 3)), note(LocalDate.of(2026, 9, 1)), note(LocalDate.of(2026, 9, 2)), note(LocalDate.of(2025, 1, 5))),
            StatsRange.ALL
        )
        assertEquals("ALL TIME", view.eyebrow)
        assertEquals(12, view.bars.size)
        assertEquals(780L * 4, view.savedSeconds)
        assertEquals("Best month Sep · 26m saved", view.bestText)
        assertEquals(listOf("Nov", "May", "Oct"), view.axis)
        assertNull(view.deltaText)
        assertEquals("All time", view.cardLabel)
    }

    @Test fun speakingPaceIsWordsOverSpokenMinutes() {
        val view = build(listOf(note(LocalDate.of(2026, 10, 3)), note(LocalDate.of(2026, 10, 4))), StatsRange.MONTH)
        // 1,200 words in 240 s is 300 a minute: 7.5 times the typing pace.
        assertEquals(300, view.paceWpm)
        assertEquals("7.5", view.paceMultiple)
    }

    @Test fun tooFewWordsSaysNothingAboutPace() {
        val view = build(listOf(note(LocalDate.of(2026, 10, 3), words = 12, spokenMs = 2_000)), StatsRange.MONTH)
        assertEquals(0, view.paceWpm)
        assertEquals("", view.paceMultiple)
    }

    @Test fun aBestDayUnderAMinuteIsNotLitUp() {
        // 60 words in 30 s: typing 90 s, so 60 s saved is the least that counts; 45 words is under it.
        val view = build(listOf(note(LocalDate.of(2026, 10, 3), words = 45, spokenMs = 30_000)), StatsRange.MONTH)
        assertNull(view.bestText)
        assertTrue(view.bars.none { it.isBest })
    }

    @Test fun notesWithoutATranscriptCountForNothing() {
        val silent = Recording(timestamp = note(LocalDate.of(2026, 10, 3)).timestamp, audioFilePath = "x", duration = 50_000, transcript = null)
        assertEquals(0, build(listOf(silent), StatsRange.MONTH).savedSeconds)
    }
}
