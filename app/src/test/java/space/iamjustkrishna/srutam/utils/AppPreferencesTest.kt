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
    fun autoAiEnabled_isOnForANewInstall() {
        setInstallTimes(firstInstall = 1_000L, lastUpdate = 1_000L)

        assertTrue(AppPreferences.isAutoAiEnabled(context))
    }

    @Test
    fun autoAiEnabled_staysOffForAnInstallThatPredatesTheDefault() {
        // Updated a week after it was installed, and never chose a value: turning it on would make the
        // app process every older note it finds.
        setInstallTimes(firstInstall = 1_000L, lastUpdate = 1_000L + 7 * 24 * 60 * 60 * 1000L)

        assertFalse(AppPreferences.isAutoAiEnabled(context))
    }

    @Test
    fun autoAiEnabled_theFirstAnswerIsRemembered() {
        setInstallTimes(firstInstall = 1_000L, lastUpdate = 1_000L)
        assertTrue(AppPreferences.isAutoAiEnabled(context))

        // A later app update must not flip it.
        setInstallTimes(firstInstall = 1_000L, lastUpdate = 1_000L + 7 * 24 * 60 * 60 * 1000L)

        assertTrue(AppPreferences.isAutoAiEnabled(context))
    }

    @Test
    fun autoAiEnabled_aStoredChoiceAlwaysWins() {
        setInstallTimes(firstInstall = 1_000L, lastUpdate = 1_000L)
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
