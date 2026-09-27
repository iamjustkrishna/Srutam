package space.iamjustkrishna.srutam.matrix

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import space.iamjustkrishna.srutam.data.*
import space.iamjustkrishna.srutam.ui.screens.*
import space.iamjustkrishna.srutam.ui.theme.SrutamTheme
import space.iamjustkrishna.srutam.ui.theme.ThemeMode
import space.iamjustkrishna.srutam.utils.InsightSource
import space.iamjustkrishna.srutam.viewmodel.*

internal object InsightsFixtures {
    const val NOW = 1_790_508_600_000L
    val idea = InsightEntity("idea", 1, kind = InsightKind.IDEA, text = "Explore a shared family subscription", createdAt = NOW)
    val decision = InsightEntity("decision", 1, kind = InsightKind.DECISION, text = "Keep transcription on device", rationale = "Privacy and offline access", createdAt = NOW)
    val task = InsightEntity("task", 1, kind = InsightKind.ACTION, text = "Send the revised product proposal", createdAt = NOW)
    val theme = ThemeCluster("product strategy", "Product strategy", 3, listOf(1, 2, 3), emptyList())
    val sources = mapOf(
        1L to InsightSource(1, "Product roadmap", true),
        2L to InsightSource(2, "Customer interviews", true),
        3L to InsightSource(3, "Voice note · Sep 22, 2026, 10:30 AM", true)
    )
    val legacy = ReminderEntity(id = "legacy", recordingId = 1, title = "Product revenue target",
        eventTimeMs = NOW + 86400000, originalText = "May 20", type = ReminderType.MILESTONE, legacyReview = true, zoneId = "UTC")
    val scheduled = ReminderEntity(id = "scheduled", recordingId = 1, title = "Review the proposal",
        eventTimeMs = NOW + 3600000, originalText = "tomorrow at 3pm", type = ReminderType.MEETING,
        notificationEnabled = true, needsReview = false, confirmedAt = NOW, timePrecision = "EXACT",
        zoneId = "UTC", localDate = "2026-09-27", localTime = "15:00")
    fun state(items: List<InsightEntity> = listOf(idea, decision, task)) =
        InsightsUiState(loaded = true, items = items, sources = sources, now = NOW)
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi", application = Application::class)
class InsightsUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun liveScreenCreatesSavedStateViewModelAndLoads() {
        val database = AppDatabase.getDatabase(ApplicationProvider.getApplicationContext())
        runBlocking(Dispatchers.IO) { database.clearAllTables() }
        rule.setContent { SrutamTheme { InsightsScreen(onRecordingClick = {}) } }
        try {
            rule.waitUntil(10_000) { rule.onAllNodesWithText("No next steps").fetchSemanticsNodes().isNotEmpty() }
        } catch (failure: Throwable) {
            throw AssertionError("Live Insights did not load an empty database:\n${rule.onRoot().printToString()}", failure)
        }
        rule.onNodeWithContentDescription("Open archive").assertExists()
    }

    @Test fun waitsForLoadedDataAndNeverOverridesManualSelection() {
        var state by mutableStateOf(InsightsUiState())
        rule.setContent { SrutamTheme { InsightsContent(state) } }
        rule.onNodeWithTag("insights_loading").assertExists()
        rule.runOnIdle { state = InsightsFixtures.state(listOf(InsightsFixtures.idea)) }
        rule.onNodeWithTag("tab_IDEAS").assertIsSelected()
        rule.onNodeWithTag("tab_DECISIONS").performClick()
        rule.runOnIdle { state = InsightsFixtures.state() }
        rule.onNodeWithTag("tab_DECISIONS").assertIsSelected()
    }

    @Test fun completedOnlyStateIsCompactAndArchiveRemainsAccessible() {
        rule.setContent { SrutamTheme { InsightsContent(InsightsFixtures.state(listOf(InsightsFixtures.task.copy(status = "COMPLETED")))) } }
        rule.onNodeWithText("All caught up · 1 completed").assertIsDisplayed()
        rule.onNodeWithTag("action_progress").assertDoesNotExist()
        rule.onNodeWithContentDescription("Open archive").performClick()
        rule.onNodeWithText("No archived next steps.").assertIsDisplayed()
    }

    @Test fun searchShowsNoResultsAndCanBeCleared() {
        rule.setContent { SrutamTheme { InsightsContent(InsightsFixtures.state(listOf(InsightsFixtures.idea))) } }
        rule.onNodeWithTag("insights_search").performTextInput("unmatched")
        rule.onNodeWithText("No matching ideas").assertExists()
        rule.onNodeWithContentDescription("Clear search").performClick()
        rule.onNodeWithText(InsightsFixtures.idea.text).assertExists()
    }

    @Test fun themeDismissalCanBeUndoneFromIdeas() {
        var state by mutableStateOf(InsightsFixtures.state(listOf(InsightsFixtures.idea)).copy(themes = listOf(InsightsFixtures.theme)))
        rule.setContent { SrutamTheme { InsightsContent(state, actions = InsightsActions(
            dismissTheme = { state = state.copy(themes = emptyList()) },
            restoreTheme = { state = state.copy(themes = listOf(InsightsFixtures.theme)) }
        )) } }
        rule.onNodeWithText("Recurring themes").performClick()
        rule.onNodeWithContentDescription("Dismiss this theme").performClick()
        rule.onNodeWithText("Undo").performClick()
        rule.onNodeWithText("Recurring themes").assertExists()
    }

    @Test fun selectedTabSurvivesSavedStateRestoration() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent { SrutamTheme { InsightsContent(InsightsFixtures.state()) } }
        rule.onNodeWithTag("tab_DECISIONS").performClick()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("tab_DECISIONS").assertIsSelected()
    }

    @Test fun sourceNavigationUsesRecordingId() {
        var clicked = -1L
        rule.setContent { SrutamTheme { InsightsContent(InsightsFixtures.state(listOf(InsightsFixtures.idea)), onRecordingClick = { clicked = it }) } }
        rule.onNodeWithText("Product roadmap").performClick()
        rule.runOnIdle { assertEquals(1L, clicked) }
    }
}
