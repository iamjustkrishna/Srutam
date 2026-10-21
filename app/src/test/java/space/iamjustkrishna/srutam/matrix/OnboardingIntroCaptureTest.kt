package space.iamjustkrishna.srutam.matrix

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
import space.iamjustkrishna.srutam.ui.screens.OnboardingIntroScreen
import space.iamjustkrishna.srutam.ui.theme.SrutamTheme
import space.iamjustkrishna.srutam.ui.theme.ThemeMode
import java.io.File

/** Captures the three intro pages, the demo played through, in light and dark. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-420dpi", sdk = [34])
class OnboardingIntroCaptureTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String) {
        val file = File("../screenshots/screen-matrix/onboarding-intro/$name.png").canonicalFile
        file.parentFile?.mkdirs()
        composeRule.mainClock.advanceTimeBy(700)
        composeRule.onRoot().captureRoboImage(file.absolutePath)
    }

    private fun walkThrough(mode: ThemeMode, prefix: String) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            SrutamTheme(themeMode = mode) { OnboardingIntroScreen(onFinished = {}) }
        }
        capture("${prefix}_1_listen")

        composeRule.onNodeWithText("Not now").performClick()
        composeRule.mainClock.advanceTimeBy(1000)
        capture("${prefix}_2_demo_idle")

        composeRule.onNodeWithContentDescription("Try the demo").performClick()
        composeRule.mainClock.advanceTimeBy(900)
        capture("${prefix}_2_demo_listening")
        composeRule.mainClock.advanceTimeBy(6000)
        capture("${prefix}_2_demo_done")

        composeRule.onNodeWithText("Next").performClick()
        composeRule.mainClock.advanceTimeBy(1000)
        capture("${prefix}_3_yours")
    }

    @Test fun light() = walkThrough(ThemeMode.LIGHT, "light")

    @Test fun dark() = walkThrough(ThemeMode.COSMIC_DARK, "dark")
}
