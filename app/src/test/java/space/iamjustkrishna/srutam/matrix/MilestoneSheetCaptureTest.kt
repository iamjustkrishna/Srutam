package space.iamjustkrishna.srutam.matrix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import space.iamjustkrishna.srutam.ui.components.MilestoneSheet
import space.iamjustkrishna.srutam.ui.theme.SrutamTheme
import space.iamjustkrishna.srutam.ui.theme.ThemeMode
import java.io.File

/** The milestone sheet over a plain screen, at phone and tablet widths. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MilestoneSheetCaptureTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, step: Long, allTime: Long) {
        composeRule.setContent {
            SrutamTheme(themeMode = ThemeMode.LIGHT) {
                Box(Modifier.fillMaxSize().background(Color(0xFFF4F5F8)))
                MilestoneSheet(step = step, allTimeSeconds = allTime, onShare = {}, onDismiss = {})
            }
        }
        composeRule.waitForIdle()
        val file = File("../screenshots/screen-matrix/milestone/$name.png").canonicalFile
        file.parentFile?.mkdirs()
        composeRule.onRoot().captureRoboImage(file.absolutePath)
    }

    @Test @Config(qualifiers = "w360dp-h780dp-xxhdpi", sdk = [34])
    fun phoneFiveMinutes() = capture("phone-5m", 300L, 420L)

    @Test @Config(qualifiers = "w360dp-h780dp-xxhdpi", sdk = [34])
    fun phoneHours() = capture("phone-12h", 12 * 3_600L, 13 * 3_600L)

    @Test @Config(qualifiers = "w800dp-h1280dp-xhdpi", sdk = [34])
    fun tablet() = capture("tablet-1h", 3_600L, 4_000L)
}
