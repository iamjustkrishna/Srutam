package space.iamjustkrishna.srutam.service

import android.app.Application
import android.app.Notification
import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import space.iamjustkrishna.srutam.R
import space.iamjustkrishna.srutam.service.QuickRecordNotification.State

/** The one recording notification: Start before recording, timer with Pause and Save during, Resume when paused. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class QuickRecordNotificationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun inflate(state: State): View =
        QuickRecordNotification.views(context, state).apply(context, FrameLayout(context))

    private fun text(root: View, id: Int) = root.findViewById<TextView>(id)

    @After fun forgetSaved() = QuickRecordNotification.clearSaved()

    @Test fun theStartNotificationIsWantedWhenTheDockOrTheSettingIsOn() {
        assertFalse(QuickRecordNotification.idleWanted(dockEnabled = false, quickRecordEnabled = false))
        assertTrue(QuickRecordNotification.idleWanted(dockEnabled = true, quickRecordEnabled = false))
        assertTrue(QuickRecordNotification.idleWanted(dockEnabled = false, quickRecordEnabled = true))
        assertTrue(QuickRecordNotification.idleWanted(dockEnabled = true, quickRecordEnabled = true))
    }

    @Test fun idleShowsOnlyAStartButton() {
        val root = inflate(State.Idle)

        assertEquals("Start", text(root, R.id.quick_primary).text.toString())
        assertEquals(View.VISIBLE, text(root, R.id.quick_primary).visibility)
        assertEquals(View.GONE, text(root, R.id.quick_secondary).visibility)
        assertEquals(View.GONE, root.findViewById<View>(R.id.quick_timer).visibility)
        assertEquals("Ready to record", text(root, R.id.quick_text).text.toString())
    }

    @Test fun recordingShowsTheTimerWithPauseAndSave() {
        val root = inflate(State.Recording(elapsedMs = 42_000))

        assertEquals("Pause", text(root, R.id.quick_primary).text.toString())
        assertEquals("Save", text(root, R.id.quick_secondary).text.toString())
        assertEquals(View.VISIBLE, text(root, R.id.quick_secondary).visibility)
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.quick_timer).visibility)
        assertEquals("Recording", text(root, R.id.quick_title).text.toString())
    }

    @Test fun pausedShowsResumeAndSaveWithTheTimeSoFar() {
        val root = inflate(State.Paused(elapsedMs = 42_000))

        assertEquals("Resume", text(root, R.id.quick_primary).text.toString())
        assertEquals("Save", text(root, R.id.quick_secondary).text.toString())
        assertEquals("0:42", text(root, R.id.quick_text).text.toString())
        assertEquals(View.GONE, root.findViewById<View>(R.id.quick_timer).visibility)
    }

    @Test fun theButtonsSendTheRecordingServiceTheRightActions() {
        val app = shadowOf(context as Application)

        text(inflate(State.Idle), R.id.quick_primary).performClick()
        assertEquals(RecordingForegroundService.ACTION_START_RECORDING, app.nextStartedService.action)

        text(inflate(State.Recording(1_000)), R.id.quick_primary).performClick()
        assertEquals(RecordingForegroundService.ACTION_PAUSE_RECORDING, app.nextStartedService.action)

        text(inflate(State.Recording(1_000)), R.id.quick_secondary).performClick()
        assertEquals(RecordingForegroundService.ACTION_STOP_RECORDING, app.nextStartedService.action)

        text(inflate(State.Paused(1_000)), R.id.quick_primary).performClick()
        assertEquals(RecordingForegroundService.ACTION_RESUME_RECORDING, app.nextStartedService.action)
    }

    @Test fun theNotificationIsOngoingPublicAndOnTheSharedChannel() {
        for (state in listOf(State.Idle, State.Recording(1_000), State.Paused(1_000))) {
            val notification = QuickRecordNotification.build(context, state)

            assertEquals(Notification.VISIBILITY_PUBLIC, notification.visibility)
            assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
            assertEquals(QuickRecordNotification.CHANNEL_ID, notification.channelId)
            assertNotNull(notification.contentIntent)
        }
    }

    @Test fun oneIdIsSharedWithTheRecordingService() {
        assertEquals(RecordingForegroundService.NOTIFICATION_ID, QuickRecordNotification.NOTIFICATION_ID)
        assertEquals("recording_channel_v3", QuickRecordNotification.CHANNEL_ID)
    }

    @Test fun savedShowsAConfirmationWithTheLengthAndStartAgain() {
        val root = inflate(State.Saved(durationMs = 42_000, finishing = false))

        assertEquals("Saved", text(root, R.id.quick_title).text.toString())
        assertEquals("Your note is saved (0:42)", text(root, R.id.quick_text).text.toString())
        assertEquals("Start", text(root, R.id.quick_primary).text.toString())
        assertEquals(View.VISIBLE, text(root, R.id.quick_primary).visibility)
        assertEquals(View.GONE, text(root, R.id.quick_secondary).visibility)
    }

    @Test fun savedWhileTheTranscriptIsStillBeingWrittenSaysSoAndHasNoButton() {
        val root = inflate(State.Saved(durationMs = 42_000, finishing = true))

        assertEquals("Saved", text(root, R.id.quick_title).text.toString())
        assertEquals("Finishing transcript...", text(root, R.id.quick_text).text.toString())
        assertEquals(View.GONE, text(root, R.id.quick_primary).visibility)
    }

    @Test fun theSavedStateLastsAFewSecondsThenGoesBackToStart() {
        QuickRecordNotification.markSaved(context, durationMs = 42_000, finishing = false, nowMs = 10_000)

        assertEquals(State.Saved(42_000, false), QuickRecordNotification.currentState(nowMs = 10_000))
        assertEquals(State.Saved(42_000, false), QuickRecordNotification.currentState(nowMs = 12_999))
        assertEquals(State.Idle, QuickRecordNotification.currentState(nowMs = 13_000))
    }

    @Test fun theFinishingStateStaysUntilTheTranscriptIsDone() {
        QuickRecordNotification.markSaved(context, durationMs = 42_000, finishing = true, nowMs = 10_000)

        assertEquals(State.Saved(42_000, true), QuickRecordNotification.currentState(nowMs = 40_000))

        QuickRecordNotification.markSaved(context, durationMs = 42_000, finishing = false, nowMs = 41_000)

        assertEquals(State.Saved(42_000, false), QuickRecordNotification.currentState(nowMs = 41_500))
    }

    @Test fun everyStateStillHasATitleAndTextForPhonesThatFoldTheNotification() {
        for (state in listOf(State.Idle, State.Recording(1_000), State.Paused(1_000), State.Saved(1_000, false))) {
            val notification = QuickRecordNotification.build(context, state)

            assertTrue("no title for $state", !notification.extras.getCharSequence(Notification.EXTRA_TITLE).isNullOrBlank())
            assertTrue("no text for $state", !notification.extras.getCharSequence(Notification.EXTRA_TEXT).isNullOrBlank())
        }
    }

    @Test fun theNotificationSwitchReadsOnAndLockedWhileTheDockIsOn() {
        assertTrue(QuickRecordNotification.switchChecked(dockEnabled = true, stored = false))
        assertTrue(QuickRecordNotification.switchChecked(dockEnabled = true, stored = true))
        assertTrue(QuickRecordNotification.switchLocked(dockEnabled = true))
    }

    @Test fun withoutTheDockTheNotificationSwitchShowsTheUsersOwnChoice() {
        assertFalse(QuickRecordNotification.switchChecked(dockEnabled = false, stored = false))
        assertTrue(QuickRecordNotification.switchChecked(dockEnabled = false, stored = true))
        assertFalse(QuickRecordNotification.switchLocked(dockEnabled = false))
    }
}
