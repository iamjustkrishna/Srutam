package space.iamjustkrishna.srutam.matrix

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import space.iamjustkrishna.srutam.ui.screens.AboutScreen
import space.iamjustkrishna.srutam.ui.screens.PrivacyScreen
import space.iamjustkrishna.srutam.ui.screens.SettingsScreen
import space.iamjustkrishna.srutam.ui.theme.SrutamTheme
import space.iamjustkrishna.srutam.ui.theme.ThemeMode
import java.io.File

/** Settings, About and Privacy, drawn tall enough to show all of each at once. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h3400dp-xxhdpi", sdk = [34])
class SettingsCurrentCaptureTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun shot(name: String) {
        val file = File("../screenshots/screen-matrix/settings-current/$name.png").canonicalFile
        file.parentFile?.mkdirs()
        composeRule.onRoot().captureRoboImage(file.absolutePath)
    }

    private fun settings(name: String, mode: ThemeMode, timeSavedAvailable: Boolean) {
        composeRule.setContent { SrutamTheme(themeMode = mode) { SettingsScreen(onNavigateBack = {}, timeSavedAvailable = timeSavedAvailable) } }
        shot(name)
    }

    @Test fun light() = settings("light", ThemeMode.LIGHT, timeSavedAvailable = true)

    @Test fun dark() = settings("dark", ThemeMode.COSMIC_DARK, timeSavedAvailable = true)

    @Test fun newUserHasNoTimeSavedRow() {
        settings("new-user", ThemeMode.LIGHT, timeSavedAvailable = false)
        composeRule.onNodeWithText("Time saved").assertDoesNotExist()
        composeRule.onNodeWithText("Weekly recap").assertIsDisplayed()
    }

    @Test fun timeSavedRowShowsOnceThereIsSomething() {
        settings("with-time-saved", ThemeMode.LIGHT, timeSavedAvailable = true)
        composeRule.onNodeWithText("Time saved").assertIsDisplayed()
    }

    @Test fun speechRecognitionIsAFactAndUsageAnalyticsIsNotHere() {
        settings("facts", ThemeMode.LIGHT, timeSavedAvailable = true)
        composeRule.onNodeWithText("Speech recognition").assertIsDisplayed()
        composeRule.onNodeWithText("On device").assertIsDisplayed()
        composeRule.onNodeWithText("Usage analytics").assertDoesNotExist()
        composeRule.onNodeWithText("Privacy").assertDoesNotExist()
        composeRule.onNodeWithText("@iamjustkrishna").assertExists()
        composeRule.onNodeWithText("Follow @iamjustkrishna").assertDoesNotExist()
    }

    @Test fun about() {
        composeRule.setContent { SrutamTheme(themeMode = ThemeMode.LIGHT) { AboutScreen(onNavigateBack = {}, onOpenPrivacy = {}) } }
        shot("about")
        composeRule.onNodeWithText("Privacy").assertIsDisplayed()
        composeRule.onNodeWithText("Version").assertIsDisplayed()
        composeRule.onNodeWithText("Follow", substring = true).assertDoesNotExist()
    }

    @Test fun privacyHoldsTheAnalyticsSwitch() {
        composeRule.setContent { SrutamTheme(themeMode = ThemeMode.LIGHT) { PrivacyScreen(onNavigateBack = {}) } }
        shot("privacy")
        composeRule.onNodeWithText("Usage analytics").assertIsDisplayed()
    }

    @Test fun themeOptionsOpenRightUnderTheRow() {
        composeRule.setContent { SrutamTheme(themeMode = ThemeMode.LIGHT) { SettingsScreen(onNavigateBack = {}) } }
        composeRule.onNodeWithText("Cosmic dark").assertDoesNotExist()
        composeRule.onNodeWithText("Theme").performClick()
        composeRule.onNodeWithText("Light").assertIsDisplayed()
        composeRule.onNodeWithText("Cosmic dark").assertIsDisplayed()
        composeRule.onAllNodesWithText("System").assertCountEquals(2) // the value, and the option
        shot("theme-open")
    }

    /** What a signed-in person sees in the Srutam Cloud group today. */
    @Test fun signedInCloudGroup() {
        space.iamjustkrishna.srutam.utils.AppPreferences.saveCloudSession(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(), "user-1", "you@example.com", "token", "refresh"
        )
        composeRule.setContent { SrutamTheme(themeMode = ThemeMode.LIGHT) { SettingsScreen(onNavigateBack = {}) } }
        composeRule.onNodeWithText("you@example.com").assertIsDisplayed()
        composeRule.onNodeWithText("Sync now").assertIsDisplayed()
        composeRule.onNodeWithText("Scan QR").assertIsDisplayed()
        composeRule.onNodeWithText("Enter code").assertIsDisplayed()
        composeRule.onNodeWithText("Sign out").assertExists()
        composeRule.onNodeWithText("Request account & data deletion").assertExists()
        shot("cloud-signed-in")
    }
}
