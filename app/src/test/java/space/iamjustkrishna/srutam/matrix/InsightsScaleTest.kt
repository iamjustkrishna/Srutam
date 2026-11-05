package space.iamjustkrishna.srutam.matrix

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.InsightKind
import space.iamjustkrishna.srutam.ui.screens.InsightsContent
import space.iamjustkrishna.srutam.ui.theme.SrutamTheme
import java.io.File

/** Hundreds or thousands of ideas: the tab counts stay readable and the list is built a page at a time. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi", application = Application::class)
class InsightsScaleTest {
    @get:Rule val rule = createComposeRule()

    private fun many(kind: String, n: Int, prefix: String) =
        List(n) { InsightEntity("$prefix$it", 1, kind = kind, text = "$prefix number $it", createdAt = InsightsFixtures.NOW - it) }

    private val big = InsightsFixtures.state(
        many(InsightKind.IDEA, 1_240, "Idea") + many(InsightKind.ACTION, 342, "Step") + many(InsightKind.DECISION, 56, "Decision")
    )

    private fun shot(name: String) {
        val file = File("../screenshots/screen-matrix/insights-scale/$name.png").canonicalFile
        file.parentFile?.mkdirs()
        rule.onRoot().captureRoboImage(file.absolutePath)
    }

    @Test fun countsAreShortenedAndStayInTheirTabs() {
        rule.setContent { SrutamTheme { InsightsContent(big) } }
        rule.onNodeWithTag("tab_count_IDEAS", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("1.2k", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("342", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("56", useUnmergedTree = true).assertIsDisplayed()
        shot("tabs-phone")
    }

    @Test fun ideasAreShownAPageAtATime() {
        rule.setContent { SrutamTheme { InsightsContent(big) } }
        rule.onNodeWithTag("tab_IDEAS").performClick()
        rule.onNodeWithText("Idea number 0").assertExists()
        val list = rule.onNodeWithTag("insights_list_IDEAS")
        list.performScrollToNode(hasText("Show 50 more · 1190 left"))
        rule.onNodeWithText("Idea number 49").assertExists()
        rule.onNodeWithText("Idea number 50").assertDoesNotExist()
        rule.onNodeWithText("Show 50 more · 1190 left").performClick()
        list.performScrollToNode(hasText("Show 50 more · 1140 left"))
        rule.onNodeWithText("Idea number 99").assertExists()
    }

    @Test @Config(qualifiers = "w320dp-h640dp-xhdpi")
    fun smallPhoneWithLargestText() {
        rule.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, 2f)) { SrutamTheme { InsightsContent(big) } }
        }
        rule.onNodeWithText("Steps", substring = true, useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("1.2k", useUnmergedTree = true).assertIsDisplayed()
        shot("tabs-small-phone-2x-text")
    }

    /** A long list is folded by when it was said: this week open, older ones closed until asked for. */
    @Test fun oldIdeasAreFoldedAndOpenOneAtATime() {
        val day = 86_400_000L
        val ideas = List(60) { InsightEntity("new$it", 1, kind = InsightKind.IDEA, text = "Fresh idea $it", createdAt = InsightsFixtures.NOW - it * 1_000L) } +
            List(40) { InsightEntity("mid$it", 1, kind = InsightKind.IDEA, text = "Mid idea $it", createdAt = InsightsFixtures.NOW - 20 * day - it * 1_000L) } +
            List(30) { InsightEntity("old$it", 1, kind = InsightKind.IDEA, text = "Old idea $it", createdAt = InsightsFixtures.NOW - 40 * day - it * 1_000L) }
        rule.setContent { SrutamTheme { InsightsContent(InsightsFixtures.state(ideas)) } }
        rule.onNodeWithTag("tab_IDEAS").performClick()
        val list = rule.onNodeWithTag("insights_list_IDEAS")
        rule.onNodeWithText("THIS WEEK").assertExists()
        list.performScrollToNode(hasText("Earlier this month"))
        rule.onNodeWithText("Earlier this month").assertExists()
        rule.onNodeWithText("Mid idea 0").assertDoesNotExist()
        list.performScrollToNode(hasText("August"))
        rule.onNodeWithText("Old idea 0").assertDoesNotExist()
        rule.onNodeWithText("August").performClick()
        list.performScrollToNode(hasText("Old idea 0"))
        rule.onNodeWithText("Old idea 0").assertExists()
    }
}
