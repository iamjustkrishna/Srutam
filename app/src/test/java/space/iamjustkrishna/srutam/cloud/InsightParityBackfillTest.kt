package space.iamjustkrishna.srutam.cloud

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import space.iamjustkrishna.srutam.data.AppDatabase
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import space.iamjustkrishna.srutam.data.SyncStatus
import space.iamjustkrishna.srutam.service.InsightParityBackfill
import space.iamjustkrishna.srutam.utils.AppPreferences

/**
 * The backfill is the only thing that recovers notes already stranded in the
 * cloud: they were marked SYNCED while their children silently 404'd, and no
 * other code path ever revisits a SYNCED note. It must run exactly once, and it
 * must not burn its one-shot flag while signed out.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class InsightParityBackfillTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context
    private val now = 1_800_000_000_000L

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        db.recordingDao().insertRecording(
            Recording(id = 1, audioFilePath = "a.m4a", timestamp = now, name = "Synced note",
                aiStatus = RecordingAiStatus.READY, syncStatus = SyncStatus.SYNCED, cloudId = "c1")
        )
        db.recordingDao().insertRecording(
            Recording(id = 2, audioFilePath = "b.m4a", timestamp = now, name = "Also synced",
                aiStatus = RecordingAiStatus.READY, syncStatus = SyncStatus.SYNCED, cloudId = "c2")
        )
        db.recordingDao().insertRecording(
            Recording(id = 3, audioFilePath = "c.m4a", timestamp = now, name = "Never AI processed",
                aiStatus = RecordingAiStatus.NOT_REQUESTED, syncStatus = SyncStatus.NOT_SYNCED)
        )
        Unit
    }

    @After fun close() = db.close()

    private fun signIn() = AppPreferences.saveCloudSession(
        context, userId = "user-1", email = "k@example.com", accessToken = "tok", refreshToken = null
    )

    private suspend fun status(id: Long) = db.recordingDao().getRecordingById(id)!!.syncStatus

    @Test
    fun marksEverySyncedNoteForReupload() = runBlocking {
        signIn()
        InsightParityBackfill.runIfNeeded(context, db.recordingDao())

        assertEquals(SyncStatus.PENDING, status(1))
        assertEquals(SyncStatus.PENDING, status(2))
    }

    @Test
    fun leavesUnprocessedNotesAlone() = runBlocking {
        signIn()
        InsightParityBackfill.runIfNeeded(context, db.recordingDao())

        // The sync worker only picks up aiStatus = READY, so flipping a note that
        // was never processed would just leave it stuck PENDING forever.
        assertEquals(SyncStatus.NOT_SYNCED, status(3))
    }

    @Test
    fun runsOnlyOnce() = runBlocking {
        signIn()
        InsightParityBackfill.runIfNeeded(context, db.recordingDao())
        assertTrue(AppPreferences.isInsightParityBackfillDone(context))

        // Simulate the note syncing successfully, then a later app start.
        db.recordingDao().updateSyncStatus(1, SyncStatus.SYNCED, "c1", now)
        InsightParityBackfill.runIfNeeded(context, db.recordingDao())

        assertEquals("a second start must not re-dirty the library", SyncStatus.SYNCED, status(1))
    }

    @Test
    fun doesNothingAndKeepsItsFlagUnsetWhileSignedOut() = runBlocking {
        // Deliberately not signing in.
        InsightParityBackfill.runIfNeeded(context, db.recordingDao())

        assertEquals(SyncStatus.SYNCED, status(1))
        assertFalse(
            "burning the one-shot flag while signed out would mean the backfill never runs",
            AppPreferences.isInsightParityBackfillDone(context)
        )

        // ...and once signed in later, it still does its job.
        signIn()
        InsightParityBackfill.runIfNeeded(context, db.recordingDao())
        assertEquals(SyncStatus.PENDING, status(1))
        assertTrue(AppPreferences.isInsightParityBackfillDone(context))
    }
}
