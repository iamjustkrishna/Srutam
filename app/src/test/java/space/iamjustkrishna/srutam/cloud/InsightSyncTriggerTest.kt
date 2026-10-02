package space.iamjustkrishna.srutam.cloud

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import space.iamjustkrishna.srutam.ai.AIProcessor
import space.iamjustkrishna.srutam.data.AppDatabase
import space.iamjustkrishna.srutam.data.InsightKind
import space.iamjustkrishna.srutam.data.InsightStatus
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import space.iamjustkrishna.srutam.data.ReminderEntity
import space.iamjustkrishna.srutam.data.ReminderStatus
import space.iamjustkrishna.srutam.data.SourceIds
import space.iamjustkrishna.srutam.data.SyncStatus
import space.iamjustkrishna.srutam.repository.InsightsRepository
import space.iamjustkrishna.srutam.service.ReminderAlarmService
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Regression tests for the stranded-edit bug.
 *
 * Before this, only AiProcessingWorker ever marked a note as needing upload, and
 * only when AI processing finished a *newly recorded* note. Every later edit -
 * creating a next step, converting an idea, confirming a reminder, archiving an
 * insight - mutated Room rows that `getPendingSyncRecordings()` would never look
 * at again, so the change never reached the cloud and never appeared in MCP.
 *
 * Each test therefore starts from an already-SYNCED note (the state that used to
 * be a dead end) and asserts the mutation flips it back to PENDING.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class InsightSyncTriggerTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: InsightsRepository
    private val now = 1_800_000_000_000L
    private var syncRequests = 0

    private val alarms = object : ReminderAlarmService {
        override fun schedule(reminder: ReminderEntity): String? = null
        override fun cancel(reminder: ReminderEntity) = Unit
    }

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = InsightsRepository(
            db,
            alarms,
            Clock.fixed(Instant.ofEpochMilli(now), ZoneOffset.UTC),
            CloudSyncTrigger { syncRequests++ }
        )
        db.recordingDao().insertRecording(
            Recording(
                id = 1, audioFilePath = "test.m4a", timestamp = now, name = "Product roadmap",
                aiStatus = RecordingAiStatus.READY, syncStatus = SyncStatus.SYNCED, cloudId = "cloud-1"
            )
        )
        Unit
    }

    @After fun close() = db.close()

    private fun aiResult() = AIProcessor.AIInsights(
        summary = "Summary", keyPoints = emptyList(), actionItems = listOf("Send proposal"),
        ideas = listOf("Offer a family plan"), wiifm = "Value",
        reminders = listOf(
            AIProcessor.AIReminder("Review", now + 3_600_000, "tomorrow at 3pm", timePrecision = "EXACT")
        )
    )

    private suspend fun syncStatus(): String = db.recordingDao().getRecordingById(1)!!.syncStatus

    /** Re-marks the note SYNCED so the next assertion proves a fresh transition. */
    private suspend fun settle() {
        db.recordingDao().updateSyncStatus(1, SyncStatus.SYNCED, "cloud-1", now)
        assertEquals(SyncStatus.SYNCED, syncStatus())
    }

    private suspend fun seed() {
        repository.merge(1, aiResult())
        settle()
    }

    // The headline regression: this is the exact user-visible symptom - a next step
    // added to an existing note never showed up in a coding agent.
    @Test fun creatingANextStepFromAnIdeaMarksTheNoteForReupload() = runBlocking {
        seed()
        val idea = db.insightDao().getInsightsByRecordingId(1).first { it.kind == InsightKind.IDEA }
        repository.createTask(idea.id, fromReminder = false, text = "Research family plan")
        assertEquals(SyncStatus.PENDING, syncStatus())
    }

    @Test fun togglingATaskMarksTheNoteForReupload() = runBlocking {
        seed()
        val task = db.insightDao().getInsightsByRecordingId(1).first { it.kind == InsightKind.ACTION }
        repository.toggleTask(task.id)
        assertEquals(SyncStatus.PENDING, syncStatus())
    }

    @Test fun editingInsightTextMarksTheNoteForReupload() = runBlocking {
        seed()
        val idea = db.insightDao().getInsightsByRecordingId(1).first { it.kind == InsightKind.IDEA }
        repository.editInsightText(idea.id, "Offer a family and student plan")
        assertEquals(SyncStatus.PENDING, syncStatus())
    }

    @Test fun changingInsightStatusMarksTheNoteForReupload() = runBlocking {
        seed()
        val idea = db.insightDao().getInsightsByRecordingId(1).first { it.kind == InsightKind.IDEA }
        repository.setInsightStatus(idea.id, InsightStatus.ARCHIVED)
        assertEquals(SyncStatus.PENDING, syncStatus())
    }

    @Test fun archivingATaskMarksTheNoteForReupload() = runBlocking {
        seed()
        val task = db.insightDao().getInsightsByRecordingId(1).first { it.kind == InsightKind.ACTION }
        repository.archiveTask(task.id)
        assertEquals(SyncStatus.PENDING, syncStatus())
    }

    @Test fun deletingATaskMarksTheNoteForReupload() = runBlocking {
        seed()
        val task = db.insightDao().getInsightsByRecordingId(1).first { it.kind == InsightKind.ACTION }
        repository.deleteTask(task.id)
        assertEquals(SyncStatus.PENDING, syncStatus())
    }

    @Test fun confirmingAReminderMarksTheNoteForReupload() = runBlocking {
        seed()
        val reminder = db.reminderDao().getAll().single()
        repository.saveReminder(reminder.copy(notificationEnabled = true), notificationsAvailable = true)
        assertEquals(SyncStatus.PENDING, syncStatus())
    }

    @Test fun resolvingAReminderMarksTheNoteForReupload() = runBlocking {
        seed()
        val reminder = db.reminderDao().getAll().single()
        repository.setReminderStatus(reminder.id, ReminderStatus.COMPLETED)
        assertEquals(SyncStatus.PENDING, syncStatus())
    }

    @Test fun deletingAReminderMarksTheNoteForReupload() = runBlocking {
        seed()
        val reminder = db.reminderDao().getAll().single()
        repository.deleteReminder(reminder.id)
        assertEquals(SyncStatus.PENDING, syncStatus())
    }

    @Test fun archivingCompletedTasksMarksTheAffectedNote() = runBlocking {
        seed()
        val task = db.insightDao().getInsightsByRecordingId(1).first { it.kind == InsightKind.ACTION }
        repository.toggleTask(task.id)
        settle()
        // A bulk @Query UPDATE cannot report which rows it touched, so the owners
        // have to be read before it runs or they could never be marked.
        repository.archiveCompleted()
        assertEquals(SyncStatus.PENDING, syncStatus())
    }

    @Test fun everyMutationAlsoWakesTheSyncWorker() = runBlocking {
        seed()
        syncRequests = 0
        val task = db.insightDao().getInsightsByRecordingId(1).first { it.kind == InsightKind.ACTION }
        repository.toggleTask(task.id)
        assertTrue("marking dirty is useless if nothing wakes the worker", syncRequests > 0)
    }

    // Chat-created items have no parent recording, so there is no cloud note to
    // attach them to. Marking SourceIds.CHAT would corrupt an unrelated row.
    @Test fun chatSourcedInsightsDoNotMarkAnyNote() = runBlocking {
        seed()
        repository.createChatInsight(InsightKind.ACTION, "Standalone next step")
        assertEquals(SyncStatus.SYNCED, syncStatus())
        assertEquals(SourceIds.CHAT, db.insightDao().getAllInsights().first { it.text == "Standalone next step" }.recordingId)
    }

    // reconcile() runs on every app start. If it marked unconditionally, the whole
    // library would re-upload on every launch.
    @Test fun reconcileLeavesAnUnchangedLibraryAlone() = runBlocking {
        seed()
        syncRequests = 0
        repository.reconcile()
        assertEquals(SyncStatus.SYNCED, syncStatus())
        assertEquals("reconcile must not request a sync when nothing changed", 0, syncRequests)
    }
}
