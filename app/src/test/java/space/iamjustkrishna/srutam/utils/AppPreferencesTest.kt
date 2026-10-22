package space.iamjustkrishna.srutam.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class AppPreferencesTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Reset shared preferences to ensure a clean slate
        context.getSharedPreferences("srutam_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun onboarding_isNotCompletedOnAFreshInstall() {
        assertFalse(AppPreferences.isOnboardingCompleted(context))
    }

    @Test
    fun onboarding_countsUsersWhoFinishedTheOldByokFlow() {
        AppPreferences.setByokOnboardingCompleted(context, true)

        assertTrue(AppPreferences.isOnboardingCompleted(context))
    }

    @Test
    fun reminderNotificationAsk_isPendingUntilAskedOnce() {
        assertFalse(AppPreferences.isReminderNotificationAskPending(context))

        AppPreferences.setReminderNotificationAskPending(context, true)
        assertTrue(AppPreferences.isReminderNotificationAskPending(context))

        AppPreferences.setReminderNotificationAsked(context)
        AppPreferences.setReminderNotificationAskPending(context, true)
        assertFalse(AppPreferences.isReminderNotificationAskPending(context))
    }

    @Test
    fun autoAiEnabled_isOnByDefaultForANewInstall() {
        setInstallTimes(firstInstall = 1_000L, lastUpdate = 1_000L)

        assertTrue(AppPreferences.isAutoAiEnabled(context))
    }

    @Test
    fun autoAiEnabled_isOnByDefaultForAnUpdatedInstallToo() {
        // Updated over an older install, then "Clear data": still someone who never chose.
        setInstallTimes(firstInstall = 1_000L, lastUpdate = 1_000L + 7 * 24 * 60 * 60 * 1000L)

        assertTrue(AppPreferences.isAutoAiEnabled(context))
    }

    @Test
    fun autoAiEnabled_anOldGuessedOffIsNotAChoice() {
        // Earlier versions saved "off" for older installs without the user choosing it.
        context.getSharedPreferences("srutam_prefs", Context.MODE_PRIVATE).edit()
            .putBoolean("auto_ai_enabled", false).commit()

        assertTrue(AppPreferences.isAutoAiEnabled(context))
    }

    @Test
    fun autoAiEnabled_aChoiceInSettingsAlwaysWins() {
        AppPreferences.setAutoAiEnabled(context, false)

        assertFalse(AppPreferences.isAutoAiEnabled(context))
    }

    private fun setInstallTimes(firstInstall: Long, lastUpdate: Long) {
        val info = shadowOf(context.packageManager).getInternalMutablePackageInfo(context.packageName)
        info.firstInstallTime = firstInstall
        info.lastUpdateTime = lastUpdate
    }

    @Test
    fun autoAiEnabled_updatesStateProperly() {
        AppPreferences.setAutoAiEnabled(context, true)
        assertTrue(AppPreferences.isAutoAiEnabled(context))

        AppPreferences.setAutoAiEnabled(context, false)
        assertFalse(AppPreferences.isAutoAiEnabled(context))
    }

    @Test
    fun askNameAfterRecording_isOffByDefaultSoNotesSaveRightAway() {
        assertFalse(AppPreferences.isAskNameAfterRecording(context))
    }

    @Test
    fun askNameAfterRecording_updatesStateProperly() {
        AppPreferences.setAskNameAfterRecording(context, true)
        assertTrue(AppPreferences.isAskNameAfterRecording(context))

        AppPreferences.setAskNameAfterRecording(context, false)
        assertFalse(AppPreferences.isAskNameAfterRecording(context))
    }

    @Test
    fun deleteContentWithRecording_isOffUntilSomeoneTicksIt() {
        assertFalse(AppPreferences.shouldDeleteContentWithRecording(context))
    }

    @Test
    fun deleteContentWithRecording_staysTickedOnceTickedAndUntickedOnceUnticked() {
        AppPreferences.setDeleteContentWithRecording(context, true)
        assertTrue(AppPreferences.shouldDeleteContentWithRecording(context))
        assertTrue("it stays for the next delete", AppPreferences.shouldDeleteContentWithRecording(context))

        AppPreferences.setDeleteContentWithRecording(context, false)
        assertFalse(AppPreferences.shouldDeleteContentWithRecording(context))
    }

    @Test
    fun byokOnboardingCompleted_defaultsToFalse() {
        assertFalse(AppPreferences.isByokOnboardingCompleted(context))
    }

    @Test
    fun byokOnboardingCompleted_updatesStateProperly() {
        AppPreferences.setByokOnboardingCompleted(context, true)
        assertTrue(AppPreferences.isByokOnboardingCompleted(context))

        AppPreferences.setByokOnboardingCompleted(context, false)
        assertFalse(AppPreferences.isByokOnboardingCompleted(context))
    }

    @Test
    fun themeMode_defaultsToSystem() {
        assertEquals(AppPreferences.THEME_SYSTEM, AppPreferences.getThemeMode(context))
    }

    @Test
    fun themeMode_updatesStateAndFlow() {
        AppPreferences.setThemeMode(context, AppPreferences.THEME_COSMIC_DARK)
        assertEquals(AppPreferences.THEME_COSMIC_DARK, AppPreferences.getThemeMode(context))
        assertEquals(AppPreferences.THEME_COSMIC_DARK, AppPreferences.themeModeFlow.value)

        AppPreferences.setThemeMode(context, AppPreferences.THEME_LIGHT)
        assertEquals(AppPreferences.THEME_LIGHT, AppPreferences.getThemeMode(context))
        assertEquals(AppPreferences.THEME_LIGHT, AppPreferences.themeModeFlow.value)
    }
}
