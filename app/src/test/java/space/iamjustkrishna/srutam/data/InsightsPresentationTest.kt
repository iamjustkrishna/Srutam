package space.iamjustkrishna.srutam.data

import org.junit.Assert.*
import org.junit.Test
import space.iamjustkrishna.srutam.utils.InsightNames
import space.iamjustkrishna.srutam.viewmodel.InsightsUiState
import space.iamjustkrishna.srutam.viewmodel.ThemeClusterEngine
import java.time.ZoneOffset

class InsightsPresentationTest {
    private fun item(id: String, kind: String, status: String = "OPEN") =
        InsightEntity(id, 1, kind = kind, text = id, status = status)

    @Test fun completedTasksDoNotForceNextStepsDefault() {
        assertEquals("IDEAS", InsightsUiState(true, items = listOf(item("task", "ACTION", "COMPLETED"), item("idea", "IDEA"))).initialTab())
        assertEquals("DECISIONS", InsightsUiState(true, items = listOf(item("decision", "DECISION"))).initialTab())
        assertEquals("NEXT_STEPS", InsightsUiState(true, items = listOf(item("task", "ACTION"), item("idea", "IDEA"))).initialTab())
    }

    @Test fun generatedNamesHaveReadableFallbackAndRenamesResolveLive() {
        val note = Recording(id = 1, timestamp = 1_800_000_000_000, name = "recording_1789041047256", audioFilePath = "file")
        val source = InsightNames.source(1, mapOf(1L to note), ZoneOffset.UTC)
        assertTrue(source.label.startsWith("Voice note ·"))
        assertFalse(source.label.contains("recording_"))
        assertEquals("Product roadmap", InsightNames.source(1, mapOf(1L to note.copy(name = "Product roadmap"))).label)
        assertFalse(InsightNames.source(99, mapOf(1L to note)).available)
    }

    @Test fun themesRequireDistinctNotesAndDoNotInventPhrasesAcrossStopWords() {
        val notes = (1L..3L).map { Recording(id = it, audioFilePath = "$it", name = "Recording",
            transcript = "Product strategy. Budget and planning. Future life work.", timestamp = it) }
        val themes = ThemeClusterEngine.build(notes, emptySet())
        assertEquals(1, themes.size)
        assertEquals("product strategy", themes.single().key)
        assertEquals(3, themes.single().noteCount)
        assertTrue(ThemeClusterEngine.build(notes.take(2), emptySet()).isEmpty())
        assertTrue(ThemeClusterEngine.build(notes, setOf("product strategy")).isEmpty())
    }

    @Test fun historyDoesNotAutoDismissPassiveMilestonesOrUnresolvedSuggestions() {
        val past = ReminderEntity(recordingId = 1, title = "Event", eventTimeMs = 1000, originalText = "",
            timePrecision = "EXACT", needsReview = false)
        val milestone = past.copy(id = "milestone", type = ReminderType.MILESTONE)
        val suggestion = past.copy(id = "suggestion", needsReview = true)
        val state = InsightsUiState(true, reminders = listOf(past, milestone, suggestion), now = 2000)
        assertEquals(listOf(past.id), state.history.map { it.id })
        assertEquals(setOf("milestone", "suggestion"), state.activeReminders.map { it.id }.toSet())
    }

    @Test fun generatedTranscriptionStatusDoesNotBecomeATheme() {
        val notes = (1L..3L).map { Recording(id = it, audioFilePath = "$it",
            name = "No Audible Speech Detected", transcript = "No audible speech detected.",
            summary = "Audio captured offline and ready for AI insights when connected.", timestamp = it) }
        assertTrue(ThemeClusterEngine.build(notes, emptySet()).isEmpty())
        val realNotes = notes.map { it.copy(transcript = "Speech recognition research.") }
        assertEquals("speech recognition research", ThemeClusterEngine.build(realNotes, emptySet()).single().key)
    }
}
