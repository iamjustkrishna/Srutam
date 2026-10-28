package space.iamjustkrishna.srutam.matrix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import space.iamjustkrishna.srutam.ui.components.TimeSavedReward
import space.iamjustkrishna.srutam.ui.theme.Sem
import space.iamjustkrishna.srutam.ui.theme.SrutamTheme
import space.iamjustkrishna.srutam.ui.theme.ThemeMode
import space.iamjustkrishna.srutam.utils.TimeSaved
import java.io.File

/** The time-saved reward at different moments and for different notes. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h480dp-480dpi", sdk = [34])
class TimeSavedRewardCaptureTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, result: TimeSaved.Result, at: Long, mode: ThemeMode = ThemeMode.LIGHT) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            SrutamTheme(themeMode = mode) {
                Column(Modifier.fillMaxSize().background(Sem.scaffold).padding(20.dp)) { TimeSavedReward(result) }
            }
        }
        composeRule.mainClock.advanceTimeBy(at)
        val file = File("../screenshots/screen-matrix/time-saved/$name.png").canonicalFile
        file.parentFile?.mkdirs()
        composeRule.onRoot().captureRoboImage(file.absolutePath)
    }

    private val shortNote = TimeSaved.of(words = 35, spokenMs = 12_000)
    private val longNote = TimeSaved.of(words = 300, spokenMs = 120_000)

    @Test fun shortNote_final() = capture("short_final", shortNote, 4_500)

    @Test fun shortNote_midway() = capture("short_midway", shortNote, 1_300)

    @Test fun longNote_milestone() = capture("long_milestone", longNote, 4_500)

    @Test fun shortNote_dark() = capture("short_dark", shortNote, 4_500, ThemeMode.COSMIC_DARK)
}
