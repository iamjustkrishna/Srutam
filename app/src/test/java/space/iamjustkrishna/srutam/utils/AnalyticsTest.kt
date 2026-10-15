package space.iamjustkrishna.srutam.utils

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AnalyticsTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("srutam_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun durationIsBucketed() {
        assertEquals("0_15s", Analytics.durationBucket(5_000))
        assertEquals("15_60s", Analytics.durationBucket(30_000))
        assertEquals("1_5m", Analytics.durationBucket(120_000))
        assertEquals("5_15m", Analytics.durationBucket(600_000))
        assertEquals("15_60m", Analytics.durationBucket(1_800_000))
        assertEquals("60m_plus", Analytics.durationBucket(3_700_000))
    }

    @Test
    fun loggingIsASafeNoOpWhenFirebaseIsNotConfigured() {
        // No google-services.json in the test build, so FirebaseApp is not initialized. None of these
        // may throw, and the app must keep working with analytics simply doing nothing.
        Analytics.init(context)
        Analytics.recordingStarted(context)
        Analytics.recordingStarted(context, "dock")
        Analytics.recordingSaved(context, 42_000)
        Analytics.recordingDiscarded(context)
        Analytics.transcriptStored(context)
        Analytics.aiInsightsReady(context)
        Analytics.aiProcessingFailed(context, "transcription_failed")
        Analytics.silenceAutoSaved(context)
        Analytics.log(context, "custom_event", mapOf("a" to 1, "b" to true, "c" to "x"))
    }

    @Test
    fun analyticsPreferenceDefaultsOnAndCanOptOut() {
        assertTrue(AppPreferences.isAnalyticsEnabled(context))

        Analytics.setEnabled(context, false)
        assertFalse(AppPreferences.isAnalyticsEnabled(context))

        Analytics.setEnabled(context, true)
        assertTrue(AppPreferences.isAnalyticsEnabled(context))
    }
}
