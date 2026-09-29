package space.iamjustkrishna.srutam.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import space.iamjustkrishna.srutam.ai.AIProcessor
import space.iamjustkrishna.srutam.repository.InsightsRepository
import space.iamjustkrishna.srutam.repository.RecordingRepository
import space.iamjustkrishna.srutam.service.ReminderAlarmService
import java.time.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class InsightsRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: InsightsRepository
    private val now = 1_800_000_000_000L
    private val scheduled = mutableListOf<ReminderEntity>()
    private val cancelled = mutableListOf<String>()
    private var scheduleFailure: String? = null
    private val alarms = object : ReminderAlarmService {
        override fun schedule(reminder: ReminderEntity): String? { scheduled += reminder; return scheduleFailure }
        override fun cancel(reminder: ReminderEntity) { cancelled += reminder.id }
    }
    @Before fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = InsightsRepository(db, alarms, Clock.fixed(Instant.ofEpochMilli(now), ZoneOffset.UTC))
        db.recordingDao().insertRecording(Recording(id = 1, audioFilePath = "test.m4a", timestamp = now, name = "Product roadmap"))
        Unit
    }
    @After fun close() { db.close() }
    private fun result() = AIProcessor.AIInsights(
        summary = "Summary", keyPoints = emptyList(), actionItems = listOf("Send proposal"),
        ideas = listOf("Offer a family plan"), wiifm = "Value",
        reminders = listOf(AIProcessor.AIReminder("Review", now + 3_600_000, "tomorrow at 3pm", timePrecision = "EXACT"))
    )

    @Test fun reprocessingPreservesCompletedArchivedAndConfirmedItems() = runBlocking {
        repository.merge(1, result())
        val first = db.insightDao().getInsightsByRecordingId(1)
        val task = first.first { it.kind == InsightKind.ACTION }
        repository.toggleTask(task.id)
        repository.archiveCompleted()
        val reminder = db.reminderDao().getAll().single()
        val confirmed = repository.saveReminder(reminder.copy(notificationEnabled = true), true)
        repository.merge(1, result())
        assertEquals(first.map { it.id }.toSet(), db.insightDao().getInsightsByRecordingId(1).map { it.id }.toSet())
        assertEquals(InsightStatus.ARCHIVED, db.insightDao().getById(task.id)!!.status)
        assertEquals(confirmed, db.reminderDao().getAll().single())
        assertEquals(1, scheduled.size)
    }

    @Test fun simultaneousConversionCreatesOneLinkedTaskAndPreservesIdea() = runBlocking {
        repository.merge(1, result())
        val idea = db.insightDao().getInsightsByRecordingId(1).first { it.kind == InsightKind.IDEA }
        val ids = (1..5).map { async { repository.createTask(idea.id, false, "Research family plan") } }.awaitAll()
        assertEquals(1, ids.toSet().size)
        assertNotNull(db.insightDao().getById(idea.id))
        assertEquals(idea.id, db.insightDao().getById(ids.first())!!.sourceInsightId)
        repository.merge(1, result())
        assertNotNull(db.insightDao().getById(ids.first()))
    }

    @Test fun deletedExtractionDoesNotReturnAndLegacyImportIsMarked() = runBlocking {
        repository.merge(1, result())
        val task = db.insightDao().getInsightsByRecordingId(1).first { it.kind == InsightKind.ACTION }
        repository.deleteTask(task.id)
        repository.merge(1, result())
        assertTrue(db.insightDao().getInsightsByRecordingId(1).none { it.kind == InsightKind.ACTION })
        assertTrue(db.recordingDao().getRecordingById(1)!!.insightsImported)
        assertNotNull(db.insightDao().getInsightsByRecordingId(1).firstOrNull { it.kind == InsightKind.IDEA })
    }

    @Test fun deletionSuppressionAppliesToReminders() = runBlocking {
        repository.merge(1, result())
        repository.deleteReminder(db.reminderDao().getAll().single().id)
        repository.merge(1, result())
        assertTrue(db.reminderDao().getAll().isEmpty())
    }

    @Test fun deletingConvertedTaskRetainsReminderHistoryAndAllowsNewConversion() = runBlocking {
        repository.merge(1, result())
        val reminder = db.reminderDao().getAll().single()
        val taskId = repository.createTask(reminder.id, true, "Review proposal")
        repository.deleteTask(taskId)
        val retained = db.reminderDao().getReminderById(reminder.id)!!
        assertEquals(ReminderStatus.DISMISSED, retained.status)
        assertFalse(retained.notificationEnabled)
        assertNull(retained.linkedTaskId)
        assertTrue(reminder.id in cancelled)
        repository.merge(1, result())
        assertEquals(listOf(reminder.id), db.reminderDao().getAll().map { it.id })
        val recreated = repository.createTask(reminder.id, true, "Review proposal again")
        assertNotEquals(taskId, recreated)
        assertEquals(reminder.id, db.insightDao().getById(recreated)!!.sourceReminderId)
    }

    @Test fun deniedPermissionSavesPassiveDateAndDoesNotSchedule() = runBlocking {
        repository.merge(1, result())
        val item = db.reminderDao().getAll().single()
        val saved = repository.saveReminder(item.copy(notificationEnabled = true), false)
        assertFalse(saved.notificationEnabled)
        assertNull(saved.confirmedAt)
        assertTrue(scheduled.isEmpty())
        assertNotNull(saved.scheduleError)
    }

    @Test fun savingAnUnresolvedReminderKeepsItReviewable() = runBlocking {
        repository.merge(1, result())
        val item = db.reminderDao().getAll().single()
        val saved = repository.saveReminder(item.copy(timePrecision = "UNKNOWN", eventTimeMs = null), false)
        assertTrue(saved.needsReview)
        assertFalse(saved.notificationEnabled)
        assertTrue(scheduled.isEmpty())
    }

    @Test fun editIncrementsRevisionAndCancellationOccursBeforeRescheduling() = runBlocking {
        repository.merge(1, result())
        val original = db.reminderDao().getAll().single()
        val first = repository.saveReminder(original.copy(notificationEnabled = true), true)
        val changed = repository.saveReminder(first.copy(eventTimeMs = now + 7_200_000), true)
        assertEquals(first.scheduleRevision + 1, changed.scheduleRevision)
        assertEquals(listOf(original.id, original.id), cancelled)
        repository.disableReminder(changed.id)
        assertFalse(db.reminderDao().getReminderById(changed.id)!!.notificationEnabled)
    }

    @Test fun schedulerFailurePreservesSavedReminderAndExposesError() = runBlocking {
        repository.merge(1, result())
        scheduleFailure = "Scheduling failed"
        val saved = repository.saveReminder(db.reminderDao().getAll().single().copy(notificationEnabled = true), true)
        assertEquals(scheduleFailure, db.reminderDao().getReminderById(saved.id)!!.scheduleError)
    }

    @Test fun reconciliationRetainsOldHistoryAndCancelsUnconfirmedAlarms() = runBlocking {
        val old = now - 120L * 24 * 60 * 60 * 1000
        db.insightDao().insertInsight(InsightEntity("archived", 1, kind = InsightKind.ACTION, text = "Old task",
            status = InsightStatus.ARCHIVED, archivedAt = old, completedAt = old))
        db.reminderDao().insertReminders(listOf(
            ReminderEntity(id = "old", recordingId = 1, title = "Old event", eventTimeMs = old, originalText = "", status = ReminderStatus.COMPLETED),
            ReminderEntity(id = "legacy", recordingId = 1, title = "Unconfirmed", eventTimeMs = now + 10000, originalText = "", legacyReview = true)
        ))
        repository.reconcile()
        assertNotNull(db.insightDao().getById("archived"))
        assertEquals(2, db.reminderDao().getAll().size)
        assertTrue(cancelled.containsAll(listOf("old", "legacy")))
        assertTrue(scheduled.isEmpty())
        repository.restoreTask("archived")
        repository.reconcile()
        assertEquals(InsightStatus.COMPLETED, db.insightDao().getById("archived")!!.status)
    }

    @Test fun sourceDeletionRemovesDependenciesAndCancelsAlarms() = runBlocking {
        repository.merge(1, result())
        val id = db.reminderDao().getAll().single().id
        repository.deleteRecordingData(1)
        assertNull(db.recordingDao().getRecordingById(1))
        assertTrue(db.insightDao().getAllInsightsFlow().first().isEmpty())
        assertTrue(db.reminderDao().getAll().isEmpty())
        assertTrue(id in cancelled)
    }

    @Test fun failedAudioDeletionPreservesSourceAndInsights() = runBlocking {
        repository.merge(1, result())
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sourceRepository = RecordingRepository(context, db.recordingDao()) { _, _ -> false }
        val failed = runCatching { sourceRepository.deleteRecording(db.recordingDao().getRecordingById(1)!!) }.isFailure
        assertTrue(failed)
        assertNotNull(db.recordingDao().getRecordingById(1))
        assertEquals(2, db.insightDao().getInsightsByRecordingId(1).size)
        assertEquals(1, db.reminderDao().getAll().size)
        assertTrue(cancelled.isEmpty())
    }

    @Test fun editingLegacyTitleKeepsOriginalExtractionIdentity() = runBlocking {
        val original = ReminderEntity(id = "legacy", recordingId = 1, title = "Review", eventTimeMs = now + 3600000,
            originalText = "tomorrow at 3pm", timePrecision = "EXACT")
        db.reminderDao().insertReminders(listOf(original))
        repository.saveReminder(original.copy(title = "My corrected title"), false)
        repository.merge(1, result())
        assertEquals(1, db.reminderDao().getAll().size)
        assertEquals("My corrected title", db.reminderDao().getAll().single().title)
    }
}
