package space.iamjustkrishna.srutam.service

import android.app.Application
import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import space.iamjustkrishna.srutam.R

/**
 * The recording notification on the lock screen needs a way to throw the recording away, next to
 * Pause and Save. It is only there while recording or paused.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NotificationDiscardTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun discardButton(state: QuickRecordNotification.State): View {
        val parent = FrameLayout(context)
        val root = QuickRecordNotification.views(context, state).apply(context, parent)
        return root.findViewById(R.id.quick_discard)
    }

    @Test fun recordingOffersDiscard() {
        val button = discardButton(QuickRecordNotification.State.Recording(elapsedMs = 12_000))
        assertEquals(View.VISIBLE, button.visibility)
        assertEquals("Discard recording", button.contentDescription)
        assert(button.hasOnClickListeners())
    }

    @Test fun pausedOffersDiscardToo() {
        val button = discardButton(QuickRecordNotification.State.Paused(elapsedMs = 12_000))
        assertEquals(View.VISIBLE, button.visibility)
        assert(button.hasOnClickListeners())
    }

    @Test fun thereIsNothingToDiscardWhenNotRecording() {
        assertEquals(View.GONE, discardButton(QuickRecordNotification.State.Idle).visibility)
        assertEquals(View.GONE, discardButton(QuickRecordNotification.State.Saved(durationMs = 5_000, finishing = false)).visibility)
    }
}
