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
    fun showsOnlyBeforeTheFirstRealNote() {
        assertTrue(SampleNote.shouldShow(hasRealNotes = false))
        assertFalse(SampleNote.shouldShow(hasRealNotes = true))
    }

    @Test
    fun opensLikeARealNoteWithItsInsights() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val note = SampleNote.recording(context)

        assertTrue(SampleNote.isSample(note.id))
        assertFalse(SampleNote.isSample(1L))
        assertEquals(space.iamjustkrishna.srutam.data.RecordingAiStatus.READY, note.aiStatus)
        val kinds = SampleNote.insights().groupingBy { it.kind }.eachCount()
        assertEquals(2, kinds[space.iamjustkrishna.srutam.data.InsightKind.ACTION])
        assertEquals(1, kinds[space.iamjustkrishna.srutam.data.InsightKind.IDEA])
        assertEquals(1, kinds[space.iamjustkrishna.srutam.data.InsightKind.DECISION])
        assertTrue(SampleNote.insights().all { it.recordingId == SampleNote.ID })
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
