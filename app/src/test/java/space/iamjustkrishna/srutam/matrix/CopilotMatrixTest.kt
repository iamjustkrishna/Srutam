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
import space.iamjustkrishna.srutam.ai.copilot.ChatSession
import space.iamjustkrishna.srutam.ai.copilot.ProposalStatus
import space.iamjustkrishna.srutam.ai.copilot.ToolProposal
import space.iamjustkrishna.srutam.ai.copilot.UndoInfo
import space.iamjustkrishna.srutam.ui.screens.GlobalChatMessage
import space.iamjustkrishna.srutam.ui.screens.GlobalCopilotContent
import space.iamjustkrishna.srutam.ui.theme.SrutamTheme
import space.iamjustkrishna.srutam.ui.theme.ThemeMode
import androidx.compose.foundation.layout.fillMaxSize
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-420dpi", sdk = [34])
class CopilotMatrixTest {
    @get:Rule val rule = createComposeRule()

    private val pending = ToolProposal(tool = "create_reminder", args = emptyMap(), summary = "Remind you Wed, Sep 30 at 9:00 AM: Call Rahul about the invoice")
    private val applied = pending.copy(id = "done", status = ProposalStatus.APPLIED, undo = UndoInfo("delete_reminder"))
    private val failed = pending.copy(id = "bad", status = ProposalStatus.FAILED, resultNote = "That time is not in the future.")

    private val conversation = listOf(
        GlobalChatMessage("I can search your voice notes and help you set reminders. I always ask before changing anything.", false),
        GlobalChatMessage("Remind me tomorrow at 9 to call Rahul about the invoice", true),
        GlobalChatMessage("Here is what I would do. Confirm to apply it.", false, proposal = pending),
        GlobalChatMessage("Add a next step: send the deck", true),
        GlobalChatMessage("Done, added.", false, proposal = applied.copy(summary = "Add next step: \"Send the deck\"")),
        GlobalChatMessage("Move it to yesterday", true),
        GlobalChatMessage("I can't do that.", false, proposal = failed),
        GlobalChatMessage("What did I say about the kitchen?", true),
        GlobalChatMessage("You mentioned replacing the old wide leaves near the sink.", false, citedNotes = listOf(1L to "Water damage note"))
    )

    private val sessions = listOf(
        ChatSession("a", "Remind me tomorrow at 9 to call Rahul", System.currentTimeMillis(), conversation),
        ChatSession("b", "What is on my plate this week?", System.currentTimeMillis() - 86_400_000, conversation.take(3)),
        ChatSession("c", "Summarize my notes from Monday", System.currentTimeMillis() - 3 * 86_400_000, conversation.take(2))
    )

    private fun shot(name: String, dark: Boolean, history: Boolean = false) {
        val file = File("../screenshots/copilot/$name.png").canonicalFile.also { it.parentFile?.mkdirs() }
        rule.setContent {
            SrutamTheme(themeMode = if (dark) ThemeMode.COSMIC_DARK else ThemeMode.LIGHT) {
                GlobalCopilotContent(
                    messages = conversation, inputText = "", sessions = sessions, currentSessionId = "a", showHistory = history
                )
            }
        }
        rule.onRoot().captureRoboImage(file.absolutePath)
    }

    private fun overlayShot(name: String, dark: Boolean) {
        val file = File("../screenshots/copilot/$name.png").canonicalFile.also { it.parentFile?.mkdirs() }
        rule.setContent {
            SrutamTheme(themeMode = if (dark) ThemeMode.COSMIC_DARK else ThemeMode.LIGHT) {
                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize()) {
                    GlobalCopilotContent(messages = conversation + conversation.drop(1), inputText = "")
                    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.BottomCenter)) {
                        space.iamjustkrishna.srutam.ui.components.StudioBottomBar(
                            currentTab = space.iamjustkrishna.srutam.ui.components.RootTab.AI, onTabSelected = {},
                            isRecording = false, recordingElapsedMs = 0, onStartRecording = {}, onFinishRecording = {},
                            onCancelRecording = {}, transparent = true
                        )
                    }
                }
            }
        }
        rule.onRoot().captureRoboImage(file.absolutePath)
    }

    @Test fun overlayLight() = overlayShot("overlay_light", dark = false)
    @Test fun overlayDark() = overlayShot("overlay_dark", dark = true)

    @Test fun conversationLight() = shot("conversation_light", dark = false)
    @Test fun conversationDark() = shot("conversation_dark", dark = true)
    @Test fun historyLight() = shot("history_light", dark = false, history = true)
    @Test fun historyDark() = shot("history_dark", dark = true, history = true)
}
