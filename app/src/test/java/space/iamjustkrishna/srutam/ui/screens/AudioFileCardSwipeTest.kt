package space.iamjustkrishna.srutam.ui.screens

import android.app.Application
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import space.iamjustkrishna.srutam.ui.theme.SrutamTheme
import space.iamjustkrishna.srutam.utils.AudioFileInfo

/** Dragging a note card to the right asks to delete it; nothing is deleted before the dialog is confirmed. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi", application = Application::class)
class AudioFileCardSwipeTest {
    @get:Rule val rule = createComposeRule()

    private var deleteCalls = 0

    private fun showCard(isSelectionMode: Boolean = false) {
        rule.setContent {
            SrutamTheme {
                AudioFileCard(
                    audioFile = AudioFileInfo(
                        filePath = "/Music/Srutam/standup.m4a",
                        fileName = "standup.m4a",
                        duration = 60_000L,
                        timestamp = 1_790_508_600_000L,
                        sizeBytes = 1_000_000L
                    ),
                    recording = null,
                    isPlaying = false,
                    onPlayClick = {},
                    onProcessAI = {},
                    onRenameClick = {},
                    onDelete = { deleteCalls++ },
                    onRecordingClick = {},
                    isSelectionMode = isSelectionMode,
                    modifier = Modifier.testTag("card")
                )
            }
        }
    }

    @Test fun draggingTheCardToTheRightOpensTheDeleteDialog() {
        showCard()

        rule.onNodeWithTag("card").performTouchInput { swipeRight() }
        rule.waitForIdle()

        rule.onNodeWithText("Delete Recording?").assertExists()
        rule.onNodeWithText("Also delete insights, tasks and reminders").assertExists()
        assertEquals("nothing is deleted until the dialog is confirmed", 0, deleteCalls)
    }

    @Test fun confirmingTheDialogDeletes() {
        showCard()

        rule.onNodeWithTag("card").performTouchInput { swipeRight() }
        rule.waitForIdle()
        rule.onNode(hasText("Delete") and hasClickAction()).performClick()
        rule.waitForIdle()

        assertEquals(1, deleteCalls)
    }

    @Test fun cancellingTheDialogKeepsTheNote() {
        showCard()

        rule.onNodeWithTag("card").performTouchInput { swipeRight() }
        rule.waitForIdle()
        rule.onNodeWithText("Cancel").performClick()
        rule.waitForIdle()

        assertEquals(0, deleteCalls)
        rule.onNodeWithText("Delete Recording?").assertDoesNotExist()
    }

    @Test fun draggingToTheLeftDoesNothing() {
        showCard()

        rule.onNodeWithTag("card").performTouchInput { swipeLeft() }
        rule.waitForIdle()

        rule.onNodeWithText("Delete Recording?").assertDoesNotExist()
        assertEquals(0, deleteCalls)
    }

    @Test fun whileNotesAreBeingSelectedDraggingDoesNothing() {
        showCard(isSelectionMode = true)

        rule.onNodeWithTag("card").performTouchInput { swipeRight() }
        rule.waitForIdle()

        rule.onNodeWithText("Delete Recording?").assertDoesNotExist()
        assertEquals(0, deleteCalls)
    }
}
