package space.iamjustkrishna.srutam.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import space.iamjustkrishna.srutam.service.SilenceWatchdog.Action
import space.iamjustkrishna.srutam.service.SilenceWatchdog.Companion.MINUTE

class SilenceWatchdogTest {
    private val watchdog = SilenceWatchdog().also { it.start(0L) }

    @Test fun staysQuietWhileSomeoneIsTalking() {
        var now = 0L
        repeat(60) {
            now += MINUTE
            watchdog.onSpeech(now - 1_000)
            assertEquals(Action.NONE, watchdog.check(now))
        }
    }

    @Test fun aRecordingThatNeverHeardSpeechAsksAfterThreeMinutes() {
        assertEquals(Action.NONE, watchdog.check(3 * MINUTE - 1))
        assertEquals(Action.ASK, watchdog.check(3 * MINUTE))
    }

    @Test fun afterSpeechItWaitsTenMinutesOfQuiet() {
        watchdog.onSpeech(20 * 1000L)

        assertEquals(Action.NONE, watchdog.check(20 * 1000L + 10 * MINUTE - 1))
        assertEquals(Action.ASK, watchdog.check(20 * 1000L + 10 * MINUTE))
    }

    @Test fun itAsksOnlyOnce() {
        assertEquals(Action.ASK, watchdog.check(3 * MINUTE))
        assertEquals(Action.NONE, watchdog.check(3 * MINUTE + 1_000))
        assertEquals(Action.NONE, watchdog.check(5 * MINUTE))
    }

    @Test fun noAnswerForFiveMinutesStopsAndSaves() {
        assertEquals(Action.ASK, watchdog.check(3 * MINUTE))

        assertEquals(Action.NONE, watchdog.check(3 * MINUTE + 5 * MINUTE - 1))
        assertEquals(Action.STOP, watchdog.check(3 * MINUTE + 5 * MINUTE))
    }

    @Test fun keepingTheRecordingStartsAFullQuietPeriodAgain() {
        assertEquals(Action.ASK, watchdog.check(3 * MINUTE))
        watchdog.onKeep(4 * MINUTE)

        assertEquals(Action.NONE, watchdog.check(4 * MINUTE + 10 * MINUTE - 1))
        assertEquals(Action.ASK, watchdog.check(4 * MINUTE + 10 * MINUTE))
    }

    @Test fun speakingAfterBeingAskedCancelsTheQuestion() {
        assertEquals(Action.ASK, watchdog.check(3 * MINUTE))

        assertTrue("the caller should dismiss the notification", watchdog.onSpeech(3 * MINUTE + 10_000))

        assertEquals(Action.NONE, watchdog.check(3 * MINUTE + 5 * MINUTE + 1))
    }

    @Test fun speechFromBeforeTheQuestionDoesNotCancelIt() {
        watchdog.onSpeech(10_000L)
        assertEquals(Action.ASK, watchdog.check(10_000L + 10 * MINUTE))

        assertFalse(watchdog.onSpeech(10_000L))
        assertEquals(Action.STOP, watchdog.check(10_000L + 15 * MINUTE))
    }

    @Test fun pausedTimeNeverCounts() {
        watchdog.onPaused()
        assertEquals(Action.NONE, watchdog.check(60 * MINUTE))

        watchdog.onResumed(60 * MINUTE)

        assertEquals(Action.NONE, watchdog.check(60 * MINUTE + 10 * MINUTE - 1))
        assertEquals(Action.ASK, watchdog.check(60 * MINUTE + 10 * MINUTE))
    }

    @Test fun startingAgainForgetsTheLastRecording() {
        assertEquals(Action.ASK, watchdog.check(3 * MINUTE))

        watchdog.start(100 * MINUTE)

        assertEquals(Action.NONE, watchdog.check(100 * MINUTE + 1))
        assertEquals(Action.ASK, watchdog.check(103 * MINUTE))
    }

    @Test fun reportsWholeMinutesOfQuiet() {
        watchdog.onSpeech(MINUTE)

        assertEquals(10L, watchdog.quietMinutes(11 * MINUTE + 30_000))
    }
}
