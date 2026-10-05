package space.iamjustkrishna.srutam.ui.components

import android.app.Application
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import space.iamjustkrishna.srutam.ui.theme.SrutamTheme
import java.io.File

/** Like a username check: the dialog says as you type that a name is taken, and will not accept it. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi", application = Application::class)
class RenameDialogTest {
    @get:Rule val rule = createComposeRule()
    @get:Rule val folder = TemporaryFolder()

    private val renamedTo = ArrayList<String>()

    private fun showDialog(): File {
        folder.newFile("abc.m4a")
        val own = folder.newFile("rec_2.m4a")
        rule.setContent {
            SrutamTheme {
                RenameDialog(
                    currentName = "rec_2",
                    onRename = { renamedTo += it },
                    onDismiss = {},
                    currentFilePath = own.absolutePath
                )
            }
        }
        return own
    }

    private fun type(text: String) {
        rule.onNode(hasSetTextAction()).performTextReplacement(text)
    }

    private fun waitForText(text: String) {
        rule.waitUntil(5_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun aNameAnotherNoteHasShowsAMessageAndBlocksRename() {
        showDialog()

        type("abc")
        waitForText("A note named \"abc\" already exists")

        rule.onNode(hasText("Rename") and hasClickAction()).assertIsNotEnabled()
    }

    @Test fun theMessageAppearsEvenWithDifferentCapitals() {
        showDialog()

        type("ABC")

        waitForText("A note named \"ABC\" already exists")
    }

    @Test fun anUnusedNameCanBeRenamed() {
        showDialog()

        type("Weekly sync")
        rule.waitForIdle()

        rule.onNode(hasText("Rename") and hasClickAction()).assertIsEnabled().performClick()
        assertEquals(listOf("Weekly sync"), renamedTo)
    }

    @Test fun aNameWithACharacterAFileCannotHaveIsRefused() {
        showDialog()

        type("a/b")
        waitForText("These characters can't be used: / \\ : * ? \" < > |")

        rule.onNode(hasText("Rename") and hasClickAction()).assertIsNotEnabled()
    }

    @Test fun theNoteKeepsItsOwnNameWithoutAMessage() {
        showDialog()
        rule.waitForIdle()

        rule.onNodeWithText("rec_2").assertExists()
        rule.onNode(hasText("Rename") and hasClickAction()).assertIsEnabled()
    }
}
