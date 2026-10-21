package space.iamjustkrishna.srutam.data.signature

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import space.iamjustkrishna.srutam.data.AppDatabase
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.InsightKind
import space.iamjustkrishna.srutam.data.InsightStatus
import space.iamjustkrishna.srutam.data.NoteSource
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import space.iamjustkrishna.srutam.data.ReminderEntity
import java.io.File

/** A reinstall in miniature: write the block from one database, restore it into an empty one. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class NoteSignatureTest {

    private lateinit var before: AppDatabase
    private lateinit var after: AppDatabase
    private lateinit var audio: File

    private fun database(): AppDatabase = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java
    ).allowMainThreadQueries().build()

    @Before fun setUp() {
        before = database()
        after = database()
        audio = File.createTempFile("recording", ".m4a").apply {
            // ftyp + mdat is enough of an MP4 for the block to sit after.
            writeBytes(byteArrayOf(0, 0, 0, 16) + "ftypM4A isom".toByteArray() + byteArrayOf(0, 0, 0, 12) + "mdat".toByteArray() + ByteArray(4) { 9 })
        }
    }

    @After fun tearDown() {
        before.close()
        after.close()
        audio.delete()
    }

    private suspend fun insertProcessedNote(isPrivate: Boolean = false): Long {
        val id = before.recordingDao().insertRecording(
            Recording(
                audioFilePath = audio.absolutePath,
                name = "Team sync",
                transcript = "We ship on Friday",
                summary = "Ship Friday",
                aiStatus = RecordingAiStatus.READY,
                isPrivate = isPrivate,
                topics = "[\"Release\"]"
            )
        )
        before.insightDao().insertInsight(
            InsightEntity(id = "task-1", recordingId = id, kind = InsightKind.ACTION, text = "Send notes", status = InsightStatus.COMPLETED)
        )
        before.reminderDao().insertReminders(
            listOf(ReminderEntity(id = "rem-1", recordingId = id, title = "Check installs", eventTimeMs = 4_000_000_000_000L, originalText = "Monday"))
        )
        return id
    }

    @Test fun aNoteComesBackFromItsAudioFileWithoutAi() = runBlocking {
        val id = insertProcessedNote()
        val original = before.recordingDao().getRecordingById(id)!!

        assertTrue(NoteSignature.writeFor(before, id))
        val restoredId = NoteSignature.restoreFromFile(after, audio)

        assertNotNull(restoredId)
        val restored = after.recordingDao().getRecordingById(restoredId!!)!!
        assertEquals(original.noteUuid, restored.noteUuid)
        assertEquals("We ship on Friday", restored.transcript)
        assertEquals("Ship Friday", restored.summary)
        assertEquals("[\"Release\"]", restored.topics)
        assertEquals(RecordingAiStatus.READY, restored.aiStatus)
        assertEquals(NoteSource.FILE, restored.contentSource)
        assertEquals(NoteBlock.audioHash(audio), restored.audioHash)
        // A "done" task stays done.
        val tasks = after.insightDao().getInsightsByRecordingId(restoredId)
        assertEquals(InsightStatus.COMPLETED, tasks.single().status)
        assertEquals(1, after.reminderDao().getRemindersByRecordingId(restoredId).size)
    }

    @Test fun restoringTwiceDoesNotDuplicateTheNote() = runBlocking {
        val id = insertProcessedNote()
        NoteSignature.writeFor(before, id)

        val first = NoteSignature.restoreFromFile(after, audio)
        val second = NoteSignature.restoreFromFile(after, audio)

        assertEquals(first, second)
        assertEquals(1, after.recordingDao().getRecordingCount())
    }

    @Test fun privateNotesGetNoBlock() = runBlocking {
        val id = insertProcessedNote(isPrivate = true)

        assertFalse(NoteSignature.writeFor(before, id))
        assertNull(NoteBlock.read(audio))
    }

    @Test fun aFileWithoutABlockIsNotRestored() = runBlocking {
        assertNull(NoteSignature.restoreFromFile(after, audio))
    }
}
