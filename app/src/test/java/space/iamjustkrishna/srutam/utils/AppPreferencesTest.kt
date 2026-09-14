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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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
    fun autoAiEnabled_defaultsToFalse() {
        assertFalse(AppPreferences.isAutoAiEnabled(context))
    }

    @Test
    fun autoAiEnabled_updatesStateProperly() {
        AppPreferences.setAutoAiEnabled(context, true)
        assertTrue(AppPreferences.isAutoAiEnabled(context))

        AppPreferences.setAutoAiEnabled(context, false)
        assertFalse(AppPreferences.isAutoAiEnabled(context))
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
