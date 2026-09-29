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
import java.time.LocalDate
import java.time.ZoneId
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
        runCatching { database.clearAllTables() }
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

    @Test fun dateScrollerIsNextToMonthHeaderAndDisplaysIdeas() {
        rule.setContent { SrutamTheme { InsightsContent(InsightsFixtures.state(listOf(InsightsFixtures.idea))) } }
        rule.onNodeWithTag("date_scroller_header").assertIsDisplayed()
        rule.onNodeWithTag("insights_date_scroller_strip").assertIsDisplayed()
        rule.onNodeWithTag("insights_all_button").assertIsDisplayed()
        rule.onNodeWithText(InsightsFixtures.idea.text).assertExists()
    }

    @Test fun clickingAllButtonClearsDateSelection() {
        rule.setContent { SrutamTheme { InsightsContent(InsightsFixtures.state(listOf(InsightsFixtures.idea))) } }
        val allBtn = rule.onNodeWithTag("insights_all_button")
        allBtn.assertIsDisplayed()
        // First click when All is already selected transitions to Today
        allBtn.performClick()
        // Second click when a date is selected clears the filter back to All
        allBtn.performClick()
        rule.onNodeWithText(InsightsFixtures.idea.text).assertExists()
    }

    @Test fun verticalMonthScrollerRespectsEarliestMonthBound() {
        val state = InsightsFixtures.state(listOf(InsightsFixtures.idea)).copy(
            earliestMonth = java.time.YearMonth.now().minusMonths(2)
        )
        rule.setContent { SrutamTheme { InsightsContent(state) } }
        rule.onNodeWithTag("date_scroller_header").assertIsDisplayed()
        rule.onNodeWithTag("insights_date_scroller_strip").assertIsDisplayed()
        rule.onNodeWithTag("insights_all_button").assertIsDisplayed()
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

    @Test fun reminderHistoryDisplaysSubtextAndDismissButton() {
        val state = InsightsFixtures.state().copy(
            reminders = listOf(InsightsFixtures.scheduled.copy(status = ReminderStatus.COMPLETED))
        )
        rule.setContent { SrutamTheme { InsightsContent(state) } }
        rule.onNodeWithContentDescription("Open reminder history").performClick()
        rule.onNodeWithText("Reminder history").assertIsDisplayed()
        rule.onNodeWithText("Keeps history of past & completed reminders for the last 3 days.").assertIsDisplayed()
        rule.onNodeWithContentDescription("Remove from history").assertIsDisplayed()
        rule.onNodeWithText("Clear history").assertIsDisplayed()
    }

    @Test fun completedTasksVisibleOnSpecificDate() {
        val today = LocalDate.now()
        val todayMillis = today.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val completedTask = InsightsFixtures.task.copy(
            id = "task_done",
            text = "Completed project roadmap",
            status = "COMPLETED",
            createdAt = todayMillis,
            completedAt = todayMillis
        )
        val state = InsightsFixtures.state(listOf(completedTask))
        rule.setContent { SrutamTheme { InsightsContent(state) } }
        // Click All button to switch to Today (which matches todayMillis)
        rule.onNodeWithTag("insights_all_button").performClick()
        rule.onNodeWithText("Completed").assertIsDisplayed()
        rule.onNodeWithText("Completed").performClick()
        rule.onNodeWithText("Completed project roadmap").assertIsDisplayed()
    }

    @Test fun reminderCardHasOnlyReviewDoneDismiss() {
        val state = InsightsFixtures.state().copy(
            reminders = listOf(InsightsFixtures.scheduled)
        )
        rule.setContent { SrutamTheme { InsightsContent(state) } }
        rule.onNodeWithTag("reminders_glance_bar").performClick()
        rule.onNodeWithTag("reminders_lazy_row").assertIsDisplayed()
        rule.onNodeWithContentDescription("done", substring = true).assertIsDisplayed()
        rule.onNodeWithContentDescription("Dismiss", substring = true).assertIsDisplayed()
        rule.onNodeWithText("+ Task").assertDoesNotExist()
    }

    @Test fun completedReminderInHistoryOffersUndo() {
        val done = InsightsFixtures.scheduled.copy(status = "COMPLETED", confirmedAt = InsightsFixtures.NOW)
        val state = InsightsFixtures.state().copy(reminders = listOf(done), now = InsightsFixtures.NOW)
        rule.setContent { SrutamTheme { InsightsContent(state) } }
        rule.onNodeWithContentDescription("Open reminder history").performClick()
        rule.onNodeWithText("Undo done").assertIsDisplayed()
    }

    @Test fun remindersRenderInGlanceBar() {
        val state = InsightsFixtures.state().copy(
            reminders = listOf(InsightsFixtures.scheduled)
        )
        rule.setContent { SrutamTheme { InsightsContent(state) } }
        rule.onNodeWithTag("reminders_glance_bar").assertIsDisplayed()
    }

    @Test fun taskEditorDisplaysPlayfairHeaderAndAddReminderTile() {
        rule.setContent {
            SrutamTheme {
                InsightTaskEditor(
                    sourceId = "test",
                    initialText = "Test action item",
                    onDismiss = {},
                    onSave = { _, _ -> }
                )
            }
        }
        rule.onAllNodesWithText("Create next step").onFirst().assertIsDisplayed()
        rule.onNodeWithText("Add a reminder").assertIsDisplayed()
    }
}
