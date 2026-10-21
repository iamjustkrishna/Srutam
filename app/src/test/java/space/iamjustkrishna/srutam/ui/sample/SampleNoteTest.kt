package space.iamjustkrishna.srutam.ui.sample

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class SampleNoteTest {

    @Test
    fun showsOnlyBeforeTheFirstRealNoteAndUntilDismissed() {
        assertTrue(SampleNote.shouldShow(hasRealNotes = false, dismissed = false))
        assertFalse(SampleNote.shouldShow(hasRealNotes = true, dismissed = false))
        assertFalse(SampleNote.shouldShow(hasRealNotes = false, dismissed = true))
    }

    @Test
    fun keyPointsJson_parsesBackToTheSameList() {
        val parsed = JSONArray(SampleNote.keyPointsJson)

        assertEquals(SampleNote.KEY_POINTS, List(parsed.length()) { parsed.getString(it) })
    }

    @Test
    fun hasTheContentTheWelcomeNotePromises() {
        assertEquals(2, SampleNote.TASKS.size)
        assertTrue(SampleNote.IDEA.isNotBlank())
        assertTrue(SampleNote.DECISION.isNotBlank())
        assertTrue(SampleNote.REMINDER.isNotBlank())
    }
}
