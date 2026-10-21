package space.iamjustkrishna.srutam.cloud

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
import space.iamjustkrishna.srutam.data.SyncStatus

/** Applying a sign-in restore to the database, end to end apart from the network. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class CloudRestoreTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var restore: CloudRestore

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        restore = CloudRestore(context, db, audioFiles = { emptyList() })
    }

    @After fun tearDown() = db.close()

    private fun json(text: String): JsonObject = JsonParser.parseString(text).asJsonObject

    private fun cloudNote(id: String, uuid: String, updatedAt: Long = 2_000_000L) = CloudNote(
        id = id, noteUuid = uuid, audioHash = null, title = "Cloud title", transcript = "cloud transcript",
        summary = "cloud summary", keyPoints = "[]", wiifm = null, aiStatus = RecordingAiStatus.READY,
        durationMs = 60_000L, timestampMs = 1_000_000L, isPrivate = false, topics = "[\"Hiring\"]", updatedAtMs = updatedAt
    )

    private fun doneTask(noteId: String) = json(
        """{"note_id":"$noteId","client_insight_id":"task-1","description":"Send notes","is_completed":true,"completed_at":"2026-10-01T10:00:00+00:00"}"""
    )

    @Test fun aCloudOnlyNoteComesBackAsTextOnly() = runBlocking {
        val summary = restore.apply(listOf(cloudNote("c1", "u1")), mapOf("c1" to listOf(doneTask("c1"))), emptyMap(), emptyMap())

        assertEquals(CloudRestore.Summary(restored = 1, withoutAudio = 1), summary)
        val note = db.recordingDao().getRecordingByNoteUuid("u1")!!
        assertTrue(CloudRestore.isTextOnlyPath(note.audioFilePath))
        assertEquals(NoteSource.CLOUD, note.contentSource)
        assertEquals(SyncStatus.SYNCED, note.syncStatus)
        assertEquals(InsightStatus.COMPLETED, db.insightDao().getInsightsByRecordingId(note.id).single().status)
    }

    @Test fun signingInWhileAiRunsLetsTheCloudWinAndKeepsDoneTasksDone() = runBlocking {
        val id = db.recordingDao().insertRecording(
            Recording(audioFilePath = "/music/a.m4a", noteUuid = "u1", aiStatus = RecordingAiStatus.SUMMARY_PROCESSING, isProcessing = true, timestamp = 1_000_000L, duration = 60_000L)
        )
        // A task the re-run of AI made after the reinstall: a duplicate of the cloud's, so it goes.
        db.insightDao().insertInsight(InsightEntity(id = "fresh-ai-task", recordingId = id, kind = InsightKind.ACTION, text = "Send notes"))

        restore.apply(listOf(cloudNote("c1", "u1")), mapOf("c1" to listOf(doneTask("c1"))), emptyMap(), emptyMap())

        val note = db.recordingDao().getRecordingById(id)!!
        assertEquals("cloud transcript", note.transcript)
        assertEquals(RecordingAiStatus.READY, note.aiStatus)
        assertEquals(false, note.isProcessing)
        assertEquals("c1", note.cloudId)
        assertEquals("/music/a.m4a", note.audioFilePath)
        val tasks = db.insightDao().getInsightsByRecordingId(id)
        assertEquals(listOf("task-1"), tasks.map { it.id })
        assertEquals(InsightStatus.COMPLETED, tasks.single().status)
        assertEquals(1, db.recordingDao().getRecordingCount())
    }

    @Test fun aNewerNoteFromTheAudioFileWinsButTakesTheAgentsDone() = runBlocking {
        val id = db.recordingDao().insertRecording(
            Recording(
                audioFilePath = "/music/a.m4a", noteUuid = "u1", transcript = "edited on phone",
                aiStatus = RecordingAiStatus.READY, contentSource = NoteSource.FILE, updatedAt = 9_000_000L
            )
        )
        db.insightDao().insertInsight(InsightEntity(id = "task-1", recordingId = id, kind = InsightKind.ACTION, text = "Send notes"))

        restore.apply(listOf(cloudNote("c1", "u1", updatedAt = 2_000_000L)), mapOf("c1" to listOf(doneTask("c1"))), emptyMap(), emptyMap())

        val note = db.recordingDao().getRecordingById(id)!!
        assertEquals("edited on phone", note.transcript)
        assertEquals("c1", note.cloudId)
        assertEquals(SyncStatus.PENDING, note.syncStatus)
        assertEquals(InsightStatus.COMPLETED, db.insightDao().getById("task-1")!!.status)
    }

    @Test fun aNoteOnlyOnThePhoneIsLeftToUpload() = runBlocking {
        val id = db.recordingDao().insertRecording(
            Recording(audioFilePath = "/music/b.m4a", noteUuid = "phone-only", aiStatus = RecordingAiStatus.READY, syncStatus = SyncStatus.PENDING, timestamp = 50_000_000L)
        )

        restore.apply(listOf(cloudNote("c1", "u1")), emptyMap(), emptyMap(), emptyMap())

        assertEquals(SyncStatus.PENDING, db.recordingDao().getRecordingById(id)!!.syncStatus)
        assertEquals(2, db.recordingDao().getRecordingCount())
    }
}
