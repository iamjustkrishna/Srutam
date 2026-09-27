package space.iamjustkrishna.srutam.matrix

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import space.iamjustkrishna.srutam.ui.screens.InsightsContent
import space.iamjustkrishna.srutam.ui.screens.ReminderEditor
import space.iamjustkrishna.srutam.ui.screens.InsightTaskEditor
import space.iamjustkrishna.srutam.ui.theme.SrutamTheme
import space.iamjustkrishna.srutam.ui.theme.ThemeMode
import space.iamjustkrishna.srutam.viewmodel.InsightsUiState
import java.io.File

abstract class InsightsMatrixBase(private val profile: String) {
    @get:Rule val rule = createComposeRule()
    private fun show(state: InsightsUiState, dark: Boolean = false, fontScale: Float = 1f) {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                SrutamTheme(themeMode = if (dark) ThemeMode.COSMIC_DARK else ThemeMode.LIGHT) {
                    Surface(Modifier.fillMaxSize()) { InsightsContent(state) }
                }
            }
        }
        rule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }
    private fun capture(name: String) {
        val output = File("src/test/screenshots/insights/$profile/$name.png")
        output.parentFile?.mkdirs()
        rule.onAllNodes(isRoot()).onLast().captureRoboImage(output.absolutePath)
    }
    @Test fun empty() { show(InsightsFixtures.state(emptyList())); capture("empty") }
    @Test fun completed() {
        show(InsightsFixtures.state(listOf(InsightsFixtures.task.copy(status = "COMPLETED"))))
        capture("completed")
    }
    @Test fun ideasAndSources() { show(InsightsFixtures.state(listOf(InsightsFixtures.idea))); capture("ideas") }
    @Test fun decisionsDark() { show(InsightsFixtures.state(listOf(InsightsFixtures.decision)), dark = true); capture("decisions_dark") }
    @Test fun reviewAndExpandedDates() {
        show(InsightsFixtures.state().copy(reminders = listOf(InsightsFixtures.legacy, InsightsFixtures.scheduled)))
        rule.onNodeWithText("Dates & reminders").performClick()
        capture("dates")
    }
    @Test fun themesLargeText() {
        show(InsightsFixtures.state(listOf(InsightsFixtures.idea)).copy(themes = listOf(InsightsFixtures.theme)), fontScale = 1.5f)
        rule.onNodeWithText("Recurring themes").performClick()
        capture("themes_large")
    }
    @Test fun archive() {
        show(InsightsFixtures.state(listOf(InsightsFixtures.task.copy(status = "ARCHIVED"))))
        rule.onNodeWithContentDescription("Open archive").performClick()
        capture("archive")
    }
    @Test fun searchWithoutResults() {
        show(InsightsFixtures.state(listOf(InsightsFixtures.idea)))
        rule.onNodeWithTag("insights_search").performTextInput("unmatched")
        capture("search_empty")
    }
    @Test fun reminderReview() {
        rule.setContent {
            SrutamTheme(themeMode = ThemeMode.LIGHT) {
                Surface(Modifier.fillMaxSize()) {
                    ReminderEditor(InsightsFixtures.legacy, onDismiss = {}, onSave = { _, _ -> })
                }
            }
        }
        capture("reminder_editor")
    }
    @Test fun taskEditorWithLongText() {
        rule.setContent {
            SrutamTheme(themeMode = ThemeMode.LIGHT) {
                Surface(Modifier.fillMaxSize()) {
                    InsightTaskEditor("idea", "Explore a shared family subscription and discuss the pricing, privacy, and offline access requirements with the product team.",
                        onDismiss = {}, onSave = { _, _ -> })
                }
            }
        }
        capture("task_editor")
    }
    @Test fun loading() { show(InsightsUiState(now = InsightsFixtures.NOW)); capture("loading") }
    @Test fun failure() { show(InsightsUiState(error = "Could not load Insights. Please retry.", now = InsightsFixtures.NOW)); capture("error") }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h640dp-xhdpi", application = Application::class)
class CompactInsightsMatrixTest : InsightsMatrixBase("compact")

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi", application = Application::class)
class PhoneInsightsMatrixTest : InsightsMatrixBase("phone")

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w673dp-h841dp-420dpi", application = Application::class)
class FoldableInsightsMatrixTest : InsightsMatrixBase("foldable")

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w600dp-h960dp-xhdpi", application = Application::class)
class Tablet7InsightsMatrixTest : InsightsMatrixBase("tablet7")

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w800dp-h1280dp-xhdpi", application = Application::class)
class Tablet10InsightsMatrixTest : InsightsMatrixBase("tablet10")
