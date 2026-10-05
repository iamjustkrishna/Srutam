package space.iamjustkrishna.srutam.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import space.iamjustkrishna.srutam.data.AppDatabase
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import java.io.IOException

/** The delete dialog's "also delete insights" box is off by default: then only the audio goes. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class RecordingRepositoryKeepContentTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context
    private val deletedPaths = ArrayList<String>()
    private var deleteSucceeds = true

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun repository() = RecordingRepository(
        context,
        db.recordingDao(),
        deleteAudio = { _, path ->
            deletedPaths += path
            deleteSucceeds
        }
    )

    @Test
    fun deletingOnlyTheAudioKeepsTheNoteAndItsInsights() = runBlocking {
        db.recordingDao().insertRecording(
            Recording(
                audioFilePath = "/rec/standup.m4a",
                name = "Standup",
                transcript = "we ship on friday",
                summary = "Ship on Friday",
                aiStatus = RecordingAiStatus.READY
            )
        )

        repository().deleteAudioKeepingContent("/rec/standup.m4a")

        assertEquals(listOf("/rec/standup.m4a"), deletedPaths)
        val kept = db.recordingDao().getRecordingByPath("/rec/standup.m4a")
        assertNotNull("the note must stay", kept)
        assertEquals("we ship on friday", kept!!.transcript)
        assertEquals("Ship on Friday", kept.summary)
        assertEquals(RecordingAiStatus.READY, kept.aiStatus)
    }

    @Test
    fun aFailedAudioDeleteIsReportedAndTheNoteStays() = runBlocking {
        db.recordingDao().insertRecording(Recording(audioFilePath = "/rec/a.m4a", transcript = "hello"))
        deleteSucceeds = false

        try {
            repository().deleteAudioKeepingContent("/rec/a.m4a")
            fail("expected the failed delete to be reported")
        } catch (expected: IOException) {
            // reported to the caller, which shows "Failed to delete file"
        }

        assertTrue(db.recordingDao().getRecordingByPath("/rec/a.m4a") != null)
        assertFalse(deletedPaths.isEmpty())
    }

    @Test
    fun deletingTheAudioOfANoteThatWasNeverStoredIsFine() {
        repository().deleteAudioKeepingContent("/rec/never_stored.m4a")

        assertEquals(listOf("/rec/never_stored.m4a"), deletedPaths)
    }
}
