package space.iamjustkrishna.srutam.matrix

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.ui.components.ShareNoteSheet
import space.iamjustkrishna.srutam.ui.theme.SrutamTheme
import space.iamjustkrishna.srutam.ui.theme.ThemeMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-420dpi", sdk = [34])
class ShareSheetCaptureTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun capture() {
        val audio = File.createTempFile("Team sync", ".m4a").apply { writeBytes(ByteArray(10)) }
        val note = Recording(
            id = 7, audioFilePath = audio.absolutePath, timestamp = 1_792_000_000_000, name = "Team sync",
            summary = "The new onboarding launches Friday, with two follow-ups.", keyPoints = "[\"Launch on Friday\",\"Release notes to Priya\"]",
            actionItems = "[\"[ ] Send the release notes to Priya\"]", transcript = "Okay, quick note before the team sync."
        )
        composeRule.setContent { SrutamTheme(themeMode = ThemeMode.LIGHT) { ShareNoteSheet(note, onDismiss = {}) } }
        composeRule.waitForIdle()
        val dir = File("../screenshots/screen-matrix/share-sheet").canonicalFile.apply { mkdirs() }
        captureScreenRoboImage(File(dir, "1_text.png").absolutePath)
        composeRule.onNodeWithText("Text + audio").performClick()
        composeRule.waitForIdle()
        captureScreenRoboImage(File(dir, "2_both.png").absolutePath)
        composeRule.onNodeWithText("Audio").performClick()
        composeRule.waitForIdle()
        captureScreenRoboImage(File(dir, "3_audio.png").absolutePath)
    }
}
