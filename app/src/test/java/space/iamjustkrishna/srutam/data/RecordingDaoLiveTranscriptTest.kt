package space.iamjustkrishna.srutam.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A note recorded in the app is saved under a temporary name and may be renamed (or discarded) in the
 * Save dialog after its live transcript has already been stored. These tests pin down that the stored
 * transcript follows the note instead of being orphaned.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class RecordingDaoLiveTranscriptTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: RecordingDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.recordingDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun transcriptIsSetWhenTheNoteHasNone() = runBlocking {
        val id = dao.insertRecording(Recording(audioFilePath = "/rec/a.m4a", transcript = null))

        assertEquals(1, dao.setTranscriptIfBlank(id, "hello world"))

        assertEquals("hello world", dao.getRecordingByPath("/rec/a.m4a")?.transcript)
    }

    @Test
    fun blankTranscriptCountsAsMissing() = runBlocking {
        val id = dao.insertRecording(Recording(audioFilePath = "/rec/a.m4a", transcript = ""))

        assertEquals(1, dao.setTranscriptIfBlank(id, "hello world"))

        assertEquals("hello world", dao.getRecordingByPath("/rec/a.m4a")?.transcript)
    }

    @Test
    fun anExistingTranscriptIsNeverOverwritten() = runBlocking {
        val id = dao.insertRecording(Recording(audioFilePath = "/rec/a.m4a", transcript = "from the worker"))

        assertEquals(0, dao.setTranscriptIfBlank(id, "from live"))

        assertEquals("from the worker", dao.getRecordingByPath("/rec/a.m4a")?.transcript)
    }

    @Test
    fun otherFieldsSurviveASetTranscript() = runBlocking {
        val id = dao.insertRecording(
            Recording(audioFilePath = "/rec/a.m4a", name = "Standup", summary = "kept", aiStatus = RecordingAiStatus.READY)
        )

        dao.setTranscriptIfBlank(id, "hello")

        val saved = dao.getRecordingByPath("/rec/a.m4a")!!
        assertEquals("Standup", saved.name)
        assertEquals("kept", saved.summary)
        assertEquals(RecordingAiStatus.READY, saved.aiStatus)
    }

    @Test
    fun renamingTheFileMovesTheNoteAndKeepsItsTranscript() = runBlocking {
        val id = dao.insertRecording(Recording(audioFilePath = "/rec/rec_1.m4a", name = "rec_1", transcript = "live text"))

        assertEquals(1, dao.movePath("/rec/rec_1.m4a", "/rec/Standup.m4a", "Standup"))

        assertNull(dao.getRecordingByPath("/rec/rec_1.m4a"))
        val moved = dao.getRecordingByPath("/rec/Standup.m4a")
        assertNotNull(moved)
        assertEquals(id, moved!!.id)
        assertEquals("Standup", moved.name)
        assertEquals("live text", moved.transcript)
        assertEquals(1, dao.getRecordingCount())
    }

    @Test
    fun movingANoteThatWasNeverStoredChangesNothing() = runBlocking {
        assertEquals(0, dao.movePath("/rec/none.m4a", "/rec/other.m4a", "other"))

        assertEquals(0, dao.getRecordingCount())
    }

    @Test
    fun discardingRemovesTheNoteThatHoldsTheTranscript() = runBlocking {
        dao.insertRecording(Recording(audioFilePath = "/rec/rec_1.m4a", transcript = "live text"))

        dao.getRecordingByPath("/rec/rec_1.m4a")?.let { dao.deleteRecording(it) }

        assertNull(dao.getRecordingByPath("/rec/rec_1.m4a"))
        assertEquals(0, dao.getRecordingCount())
    }
}
