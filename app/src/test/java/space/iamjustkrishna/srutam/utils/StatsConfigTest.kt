package space.iamjustkrishna.srutam.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsConfigTest {
    private val hour = 3_600L

    @Test fun theLadderNeverEnds() {
        val first = StatsConfig.milestones().take(12).toList()
        assertEquals(listOf(300L, 1_800L, hour, 6 * hour, 12 * hour, 24 * hour, 48 * hour, 100 * hour, 200 * hour, 300 * hour, 400 * hour, 500 * hour), first)
        assertEquals(1_300 * hour, StatsConfig.nextAfter(1_240 * hour))
    }

    @Test fun nextIsAlwaysAboveWhatIsSaved() {
        assertEquals(300L, StatsConfig.nextAfter(0))
        assertEquals(300L, StatsConfig.nextAfter(299))
        assertEquals(1_800L, StatsConfig.nextAfter(300))
        assertEquals(48 * hour, StatsConfig.nextAfter(24 * hour))
        assertEquals(200 * hour, StatsConfig.nextAfter(100 * hour))
        assertEquals(200 * hour, StatsConfig.nextAfter(150 * hour))
    }

    @Test fun pendingCelebratesOnlyTheHighestNewStep() {
        assertNull(StatsConfig.pending(299, 0))
        assertEquals(300L, StatsConfig.pending(1_000, 0))
        assertNull(StatsConfig.pending(1_000, 300))
        assertEquals(24 * hour, StatsConfig.pending(30 * hour, 12 * hour))
        // after the early ladder: every hundred hours
        assertEquals(100 * hour, StatsConfig.pending(150 * hour, 48 * hour))
        assertEquals(200 * hour, StatsConfig.pending(250 * hour, 100 * hour))
        assertNull(StatsConfig.pending(250 * hour, 200 * hour))
        assertEquals(1_200 * hour, StatsConfig.pending(1_240 * hour, 0))
    }

    @Test fun badgesStayAWindow() {
        // nothing yet: the next five
        assertEquals(StatsConfig.Badges(0, listOf(300L, 1_800L, hour, 6 * hour, 12 * hour)), StatsConfig.badges(0))
        // a few done: the last two reached, then what is ahead
        val going = StatsConfig.badges(9 * hour)
        assertEquals(StatsConfig.Badges(2, listOf(hour, 6 * hour, 12 * hour, 24 * hour, 48 * hour)), going)
        // the day it hits 24 hours
        val day = StatsConfig.badges(24 * hour)
        assertEquals(listOf(12 * hour, 24 * hour, 48 * hour, 100 * hour, 200 * hour), day.steps)
        assertEquals(4, day.earlier)
        // a long way in
        val far = StatsConfig.badges(1_240 * hour)
        assertEquals(listOf(1_100 * hour, 1_200 * hour, 1_300 * hour, 1_400 * hour, 1_500 * hour), far.steps)
        assertEquals(8 + 11 - 2, far.earlier)
    }

    @Test fun countsAreShortenedFromAThousand() {
        assertEquals("0", StatsConfig.shortCount(0))
        assertEquals("999", StatsConfig.shortCount(999))
        assertEquals("1.0k", StatsConfig.shortCount(1_000))
        assertEquals("1.2k", StatsConfig.shortCount(1_240))
        assertEquals("9.9k", StatsConfig.shortCount(9_999))
        assertEquals("12k", StatsConfig.shortCount(12_480))
        assertEquals("98k", StatsConfig.shortCount(98_999))
        assertEquals("99k+", StatsConfig.shortCount(99_000))
        assertEquals("99k+", StatsConfig.shortCount(1_000_000))
    }

    @Test fun bigTotalsDropMinutes() {
        assertEquals("6h 23m", TimeSaved.hero(6 * hour + 23 * 60))
        assertEquals("124h 59m", TimeSaved.hero(124 * hour + 59 * 60))
        assertEquals("1,240h", TimeSaved.hero(1_240 * hour + 59 * 60))
    }

    @Test fun milestoneNamesAndBadges() {
        assertEquals("First five minutes", TimeSavedMilestones.title(300))
        assertEquals("A whole day", TimeSavedMilestones.title(24 * hour))
        assertEquals("A hundred hours", TimeSavedMilestones.title(100 * hour))
        assertEquals("200 hours back", TimeSavedMilestones.title(200 * hour))
        assertEquals("1,200 hours back", TimeSavedMilestones.title(1_200 * hour))
        assertEquals("5m", TimeSavedMilestones.badge(300))
        assertEquals("6h", TimeSavedMilestones.badge(6 * hour))
        assertEquals("1,200h", TimeSavedMilestones.badge(1_200 * hour))
        assertTrue(TimeSavedMilestones.label(200 * hour).contains("200 hours"))
    }

    @Test fun typingCountsAtTheConfiguredSpeed() {
        // 40 words a minute: 120 words take 3 minutes
        assertEquals(180L, TimeSaved.of(120, 52_000L).typingSeconds)
        assertEquals(128L, TimeSaved.of(120, 52_000L).savedSeconds)
    }
}
