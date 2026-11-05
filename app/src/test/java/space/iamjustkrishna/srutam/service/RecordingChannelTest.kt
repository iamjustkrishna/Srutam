package space.iamjustkrishna.srutam.service

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The recording notification has to stay on the lock screen. Many phones hide low-importance
 * ("silent") notifications there, so the channel it settles on after the banner folds away must be
 * normal importance, with no sound or vibration, and public on the lock screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RecordingChannelTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager get() = context.getSystemService(NotificationManager::class.java)

    @Test fun theChannelAfterTheBannerIsNotLowImportance() {
        QuickRecordNotification.ensureChannel(context)

        val quiet: NotificationChannel = manager.getNotificationChannel("recording_quiet_v2")
        assertNotNull(quiet)
        assertTrue("lock screens hide low importance", quiet.importance >= NotificationManager.IMPORTANCE_DEFAULT)
        assertNull(quiet.sound)
        assertEquals(false, quiet.shouldVibrate())
        assertEquals(Notification.VISIBILITY_PUBLIC, quiet.lockscreenVisibility)
    }

    @Test fun theOldLowImportanceChannelIsRemoved() {
        manager.createNotificationChannel(NotificationChannel("recording_quiet_v1", "old", NotificationManager.IMPORTANCE_LOW))

        QuickRecordNotification.ensureChannel(context)

        assertNull(manager.getNotificationChannel("recording_quiet_v1"))
    }
}
