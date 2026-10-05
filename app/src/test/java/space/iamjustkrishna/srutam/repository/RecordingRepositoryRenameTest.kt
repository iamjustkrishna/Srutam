package space.iamjustkrishna.srutam.repository

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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import space.iamjustkrishna.srutam.data.AppDatabase
import space.iamjustkrishna.srutam.data.Recording
import java.io.File

/** Renaming a note renames its file and its title together, and never leaves them different. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class RecordingRepositoryRenameTest {

    @get:Rule val folder = TemporaryFolder()

    private lateinit var db: AppDatabase
    private lateinit var repository: RecordingRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = RecordingRepository(context, db.recordingDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun audio(name: String) = folder.newFile(name).also { it.writeText("audio") }

    @Test
    fun renamingChangesTheFileAndTheTitleTogether() = runBlocking {
        val file = audio("rec_1.m4a")
        db.recordingDao().insertRecording(Recording(audioFilePath = file.absolutePath, name = "rec_1", transcript = "hello"))

        val result = repository.renameRecording(file.absolutePath, "abc")

        val renamed = result as RenameResult.Renamed
        assertEquals("abc", renamed.title)
        assertTrue(File(folder.root, "abc.m4a").exists())
        assertFalse(file.exists())
        assertNull(db.recordingDao().getRecordingByPath(file.absolutePath))
        val row = db.recordingDao().getRecordingByPath(renamed.newPath)!!
        assertEquals("abc", row.name)
        assertEquals("the note keeps its transcript", "hello", row.transcript)
        assertEquals(1, db.recordingDao().getRecordingCount())
    }

    @Test
    fun aSecondNoteCannotTakeTheSameName() = runBlocking {
        val first = audio("rec_1.m4a")
        val second = audio("rec_2.m4a")
        repository.renameRecording(first.absolutePath, "abc")

        val result = repository.renameRecording(second.absolutePath, "abc")

        assertTrue(result is RenameResult.Rejected)
        assertTrue("its file is untouched", second.exists())
        assertTrue(File(folder.root, "abc.m4a").exists())
    }

    @Test
    fun aRefusedRenameChangesNothingInTheStoredNote() = runBlocking {
        audio("abc.m4a")
        val second = audio("rec_2.m4a")
        db.recordingDao().insertRecording(Recording(audioFilePath = second.absolutePath, name = "Standup"))

        repository.renameRecording(second.absolutePath, "ABC")

        val row = db.recordingDao().getRecordingByPath(second.absolutePath)
        assertNotNull(row)
        assertEquals("Standup", row!!.name)
    }

    @Test
    fun aFileWithNoStoredNoteGetsOne() = runBlocking {
        val file = audio("rec_3.m4a")

        val result = repository.renameRecording(file.absolutePath, "Weekly sync", duration = 61_000L, timestamp = 123L)

        val row = db.recordingDao().getRecordingByPath((result as RenameResult.Renamed).newPath)!!
        assertEquals("Weekly sync", row.name)
        assertEquals(61_000L, row.duration)
        assertEquals(123L, row.timestamp)
    }

    @Test
    fun keepingTheSameNameIsAllowed() = runBlocking {
        val file = audio("abc.m4a")
        db.recordingDao().insertRecording(Recording(audioFilePath = file.absolutePath, name = "Old title"))

        val result = repository.renameRecording(file.absolutePath, "abc")

        assertTrue(result is RenameResult.Renamed)
        assertEquals("abc", db.recordingDao().getRecordingByPath(file.absolutePath)!!.name)
    }

    @Test
    fun aMissingFileIsReportedNotRenamed() = runBlocking {
        val result = repository.renameRecording(File(folder.root, "gone.m4a").absolutePath, "abc")

        assertTrue(result is RenameResult.Rejected)
    }
}
