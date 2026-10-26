package space.iamjustkrishna.srutam.matrix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.RobolectricTestRunner
import space.iamjustkrishna.srutam.ui.components.RootTab
import space.iamjustkrishna.srutam.ui.components.StudioBottomBar
import space.iamjustkrishna.srutam.ui.theme.Sem
import space.iamjustkrishna.srutam.ui.theme.SrutamTheme
import space.iamjustkrishna.srutam.ui.theme.ThemeMode
import java.io.File

/** The one-time record hint above the bottom bar's record button, in light and dark. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h360dp-480dpi", sdk = [34])
class RecordHintCaptureTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(mode: ThemeMode, name: String) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            SrutamTheme(themeMode = mode) {
                Box(Modifier.fillMaxSize().background(Sem.scaffold), contentAlignment = Alignment.BottomCenter) {
                    StudioBottomBar(
                        currentTab = RootTab.NOTES,
                        onTabSelected = {},
                        isRecording = false,
                        recordingElapsedMs = 0L,
                        onStartRecording = {},
                        onFinishRecording = {},
                        onCancelRecording = {}
                    )
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(2_000)
        val file = File("../screenshots/screen-matrix/record-hint/$name.png").canonicalFile
        file.parentFile?.mkdirs()
        composeRule.onRoot().captureRoboImage(file.absolutePath)
    }

    @Test fun light() = capture(ThemeMode.LIGHT, "light")

    @Test fun dark() = capture(ThemeMode.COSMIC_DARK, "dark")
}
