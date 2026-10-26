package space.iamjustkrishna.srutam.matrix

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import space.iamjustkrishna.srutam.ui.screens.WelcomeScreen
import space.iamjustkrishna.srutam.ui.theme.SrutamTheme
import space.iamjustkrishna.srutam.ui.theme.ThemeMode
import java.io.File

/**
 * The first-run welcome screen across sizes and orientations. Heights are the usable area once
 * the system bars are out (a 1080x2400 phone is about 360x752 dp).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h752dp-480dpi", sdk = [34])
class WelcomeCaptureTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(mode: ThemeMode, name: String) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            SrutamTheme(themeMode = mode) {
                WelcomeScreen(
                    micPermissions = arrayOf(android.Manifest.permission.RECORD_AUDIO),
                    onFinished = {}
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(1200)
        val file = File("../screenshots/screen-matrix/welcome/$name.png").canonicalFile
        file.parentFile?.mkdirs()
        composeRule.onRoot().captureRoboImage(file.absolutePath)
    }

    @Test fun light() = capture(ThemeMode.LIGHT, "light")

    @Test fun dark() = capture(ThemeMode.COSMIC_DARK, "dark")

    @Test @Config(qualifiers = "w360dp-h640dp-320dpi")
    fun smallPhone() = capture(ThemeMode.LIGHT, "small_phone")

    @Test @Config(qualifiers = "w411dp-h860dp-420dpi")
    fun largePhone() = capture(ThemeMode.LIGHT, "large_phone")

    @Test @Config(qualifiers = "w800dp-h360dp-land-480dpi")
    fun phoneLandscape() = capture(ThemeMode.LIGHT, "phone_landscape")

    @Test @Config(qualifiers = "w800dp-h1240dp-240dpi")
    fun tabletPortrait() = capture(ThemeMode.LIGHT, "tablet_portrait")

    @Test @Config(qualifiers = "w1280dp-h760dp-land-240dpi")
    fun tabletLandscape() = capture(ThemeMode.LIGHT, "tablet_landscape")

    @Test @Config(qualifiers = "w673dp-h840dp-420dpi")
    fun foldableOpen() = capture(ThemeMode.LIGHT, "foldable_open")
}
