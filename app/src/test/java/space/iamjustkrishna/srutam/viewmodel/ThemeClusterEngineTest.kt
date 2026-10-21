package space.iamjustkrishna.srutam.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import space.iamjustkrishna.srutam.data.Recording

class ThemeClusterEngineTest {

    private fun note(id: Long, topics: String? = null, summary: String? = "A note.", transcript: String? = null) =
        Recording(id = id, audioFilePath = "/music/$id.m4a", name = "Recording", timestamp = id, topics = topics,
            summary = summary, transcript = transcript)

    @Test fun themesGroupNotesByTheirAiTopics() {
        val notes = listOf(
            note(1, """["Apartment hunt","Budget"]"""),
            note(2, """["apartment hunt"]"""),
            note(3, """["Team hiring"]"""),
            note(4, """["Team Hiring","Apartment Hunt"]""")
        )

        val themes = ThemeClusterEngine.build(notes, emptySet())

        assertEquals(listOf("apartment hunt", "team hiring"), themes.map { it.key })
        assertEquals(3, themes.first().noteCount)
        assertEquals(listOf(4L, 2L, 1L), themes.first().noteIds)
        assertFalse(themes.any { it.key == "budget" }) // one note isn't a recurring theme
    }

    @Test fun dismissedAndGenericTopicsAreLeftOut() {
        val notes = (1L..3L).map { note(it, """["Work","Apartment hunt"]""") }

        assertTrue(ThemeClusterEngine.build(notes, setOf("apartment hunt")).isEmpty())
    }

    @Test fun fillerHeavyNotesDoNotTurnFillerIntoThemes() {
        val filler = "Right now I kind of think that today, you know, right now it is kind of the thing."
        val notes = (1L..4L).map {
            note(it, topics = null, summary = "Right now kind of today. Kind of right now.", transcript = filler)
        }

        assertTrue(ThemeClusterEngine.build(notes, emptySet()).isEmpty())
    }

    @Test fun theFallbackReadsSummariesNotTranscripts() {
        val notes = (1L..3L).map {
            note(it, topics = null, summary = "Quarterly budget review.", transcript = "Garden irrigation plans.")
        }

        assertEquals(listOf("quarterly budget review"), ThemeClusterEngine.build(notes, emptySet()).map { it.key })
    }

    @Test fun taggedNotesAreNotCountedAgainByTheFallback() {
        val notes = (1L..3L).map { note(it, topics = "[]", summary = "Quarterly budget review.") }

        assertTrue(ThemeClusterEngine.build(notes, emptySet()).isEmpty())
    }

    @Test fun cleanTopicsKeepsTwoToFourRealSubjects() {
        val cleaned = ThemeClusterEngine.cleanTopics(
            listOf(" #Apartment hunt ", "apartment HUNT", "Work", "right now", "Team hiring", "Launch", "Budget", "Extra")
        )

        assertEquals(listOf("Apartment hunt", "Team hiring", "Launch", "Budget"), cleaned)
    }
}
