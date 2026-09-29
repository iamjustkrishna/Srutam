package space.iamjustkrishna.srutam.repository

import android.content.Context
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import space.iamjustkrishna.srutam.ai.AIProcessor
import space.iamjustkrishna.srutam.ai.ResolvedReminderTime
import space.iamjustkrishna.srutam.data.*
import space.iamjustkrishna.srutam.service.*
import java.security.MessageDigest
import java.text.Normalizer
import java.time.Clock
import java.util.Locale
import java.util.UUID

class InsightsRepository(
    val database: AppDatabase,
    private val alarms: ReminderAlarmService,
    private val clock: Clock = Clock.systemUTC()
) {
    val insights = database.insightDao().getAllInsightsFlow()
    val reminders = database.reminderDao().getAllFlow()
    val recordings = database.recordingDao().getAllRecordings()

    suspend fun merge(recordingId: Long, result: AIProcessor.AIInsights) = database.withTransaction {
        val recording = database.recordingDao().getRecordingById(recordingId) ?: return@withTransaction
        val dao = database.insightDao()
        val existing = dao.getInsightsByRecordingId(recordingId).toMutableList()
        suspend fun insert(kind: String, raw: String, order: Int, rationale: String? = null, evidence: String? = null) {
            val text = raw.removePrefix("[ ]").removePrefix("[]").trim()
            if (text.isBlank()) return
            val key = fingerprint(kind, text)
            if (database.extractionSuppressionDao().contains(recordingId, kind, key)) return
            if (existing.any { it.kind == kind && (it.extractionFingerprint ?: fingerprint(kind, it.text)) == key }) return
            val entity = InsightEntity(
                id = UUID.randomUUID().toString(), recordingId = recordingId,
                recordingName = recording.name, kind = kind, text = text, rationale = rationale,
                evidence = evidence, createdAt = recording.timestamp, sourceOrder = order,
                extractionFingerprint = key
            )
            dao.insertInsight(entity)
            existing.add(entity)
        }
        result.actionItems.forEachIndexed { index, text -> insert(InsightKind.ACTION, text, index) }
        result.ideas.forEachIndexed { index, text -> insert(InsightKind.IDEA, text, index) }
        result.decisions.forEachIndexed { index, value -> insert(InsightKind.DECISION, value.text, index, value.rationale, value.evidence) }
        val reminderDao = database.reminderDao()
        val old = reminderDao.getRemindersByRecordingId(recordingId).toMutableList()
        for (candidate in result.reminders) {
            if (candidate.title.isBlank()) continue
            val key = fingerprint("REMINDER", "${candidate.title}\n${candidate.originalText}")
            if (database.extractionSuppressionDao().contains(recordingId, "REMINDER", key)) continue
            if (old.any { (it.extractionFingerprint ?: fingerprint("REMINDER", "${it.title}\n${it.originalText}")) == key }) continue
            val entity = ReminderEntity(
                recordingId = recordingId, recordingName = recording.name,
                title = candidate.title, eventTimeMs = candidate.eventTimeMs,
                originalText = candidate.originalText, person = candidate.person,
                location = candidate.location, type = candidate.type, createdAt = recording.timestamp,
                timePrecision = candidate.timePrecision, localDate = candidate.localDate,
                localTime = candidate.localTime, zoneId = candidate.zoneId,
                zoneInferred = recording.recordedZoneId == null, extractionFingerprint = key
            )
            reminderDao.insertReminders(listOf(entity))
            old.add(entity)
        }
        database.recordingDao().markInsightsImported(recordingId)
    }

    suspend fun importLegacy() {
        val type = object : TypeToken<List<String>>() {}.type
        for (recording in recordings.first().filter { !it.insightsImported }) {
            val actions = runCatching { Gson().fromJson<List<String>>(recording.actionItems ?: "[]", type) }.getOrNull().orEmpty()
            merge(recording.id, AIProcessor.AIInsights(
                summary = recording.summary.orEmpty(), keyPoints = emptyList(), actionItems = actions,
                wiifm = recording.wiifm.orEmpty()
            ))
        }
    }

    suspend fun toggleTask(id: String) = reminderWrites.withLock {
        database.withTransaction {
            val dao = database.insightDao()
            val item = dao.getById(id) ?: return@withTransaction
            val complete = item.status != InsightStatus.COMPLETED
            dao.updateInsight(item.copy(
                status = if (complete) InsightStatus.COMPLETED else InsightStatus.OPEN,
                completedAt = if (complete) clock.millis() else null, archivedAt = null
            ))
            if (complete) {
                database.reminderDao().getRemindersByRecordingId(item.recordingId)
                    .filter { it.linkedTaskId == id && it.status == ReminderStatus.ACTIVE }
                    .forEach { reminder ->
                        database.reminderDao().update(reminder.copy(
                            status = ReminderStatus.COMPLETED, notificationEnabled = false,
                            scheduleRevision = reminder.scheduleRevision + 1
                        ))
                        alarms.cancel(reminder)
                    }
            }
        }
    }

    suspend fun archiveCompleted() = database.insightDao().archiveCompletedActions(clock.millis())

    suspend fun restoreTask(id: String) = database.withTransaction {
        val dao = database.insightDao()
        val item = dao.getById(id) ?: return@withTransaction
        dao.updateInsight(item.copy(status = InsightStatus.COMPLETED, archivedAt = null, completedAt = clock.millis()))
    }

    suspend fun deleteTask(id: String) = reminderWrites.withLock {
        database.withTransaction {
            val dao = database.insightDao()
            val item = dao.getById(id) ?: return@withTransaction
            if (item.sourceInsightId == null && item.sourceReminderId == null) {
                database.extractionSuppressionDao().insert(ExtractionSuppression(
                    item.recordingId, item.kind, item.extractionFingerprint ?: fingerprint(item.kind, item.text)
                ))
            }
            database.reminderDao().getRemindersByRecordingId(item.recordingId)
                .filter { it.linkedTaskId == id }.forEach {
                    database.reminderDao().update(it.copy(
                        linkedTaskId = null,
                        status = if (it.status == ReminderStatus.ACTIVE) ReminderStatus.DISMISSED else it.status,
                        notificationEnabled = false, needsReview = false,
                        scheduleRevision = it.scheduleRevision + 1, scheduleError = null
                    ))
                    alarms.cancel(it)
                }
            dao.deleteInsight(item)
        }
    }

    /** Called only from explicit user actions; validates the final edited value, never AI consent. */
    suspend fun saveReminder(edited: ReminderEntity, notificationsAvailable: Boolean): ReminderEntity = reminderWrites.withLock {
        val saved = database.withTransaction {
            val dao = database.reminderDao()
            val old = dao.getReminderById(edited.id) ?: error("This reminder no longer exists.")
            require(sourceExists(old.recordingId)) { "Source note unavailable." }
            require(edited.title.isNotBlank()) { "Enter a title." }
            val enabled = edited.notificationEnabled && notificationsAvailable
            if (enabled) require(edited.timePrecision == "EXACT" && (edited.eventTimeMs ?: 0) > clock.millis()) {
                "Choose an unambiguous future date and time."
            }
            old.copy(
                title = edited.title.trim(), type = edited.type, eventTimeMs = edited.eventTimeMs,
                timePrecision = edited.timePrecision, localDate = edited.localDate,
                localTime = edited.localTime, zoneId = edited.zoneId, zoneInferred = false,
                notificationEnabled = enabled, confirmedAt = if (enabled) clock.millis() else null,
                needsReview = edited.timePrecision == "UNKNOWN", advanceNotification = enabled && edited.advanceNotification,
                extractionFingerprint = old.extractionFingerprint ?: fingerprint("REMINDER", "${old.title}\n${old.originalText}"),
                legacyReview = false,
                status = if ((edited.eventTimeMs ?: 0) > clock.millis() || enabled) ReminderStatus.ACTIVE else old.status,
                scheduleRevision = old.scheduleRevision + 1,
                scheduleError = if (edited.notificationEnabled && !notificationsAvailable)
                    "Saved without notifications. Enable notification permission, then review again." else null
            ).also { dao.update(it) }
        }
        alarms.cancel(saved)
        val result = if (saved.notificationEnabled) saved.copy(scheduleError = alarms.schedule(saved)) else saved
        database.reminderDao().update(result)
        result
    }

    suspend fun setReminderStatus(id: String, status: String) = reminderWrites.withLock {
        val item = database.reminderDao().getReminderById(id) ?: return@withLock
        require(status in listOf(ReminderStatus.COMPLETED, ReminderStatus.DISMISSED))
        database.reminderDao().update(item.copy(
            status = status, notificationEnabled = false, needsReview = false, legacyReview = false,
            // For resolved reminders confirmedAt records when they were resolved; it bounds the undo window.
            confirmedAt = clock.millis(),
            scheduleRevision = item.scheduleRevision + 1
        ))
        alarms.cancel(item)
    }

    /** Reopens a reminder that was marked done or dismissed within the last day, re-arming its alert when still upcoming. */
    suspend fun undoReminderStatus(id: String) = reminderWrites.withLock {
        val item = database.reminderDao().getReminderById(id) ?: return@withLock
        val now = clock.millis()
        val resolvedAt = item.confirmedAt
        if ((item.status != ReminderStatus.COMPLETED && item.status != ReminderStatus.DISMISSED) || resolvedAt == null || now - resolvedAt > UNDO_WINDOW_MS) return@withLock
        val rearm = item.type != ReminderType.MILESTONE && item.timePrecision == "EXACT" && (item.eventTimeMs ?: 0) > now
        val restored = item.copy(
            status = ReminderStatus.ACTIVE, notificationEnabled = rearm, needsReview = false,
            confirmedAt = if (rearm) now else null, scheduleRevision = item.scheduleRevision + 1, scheduleError = null
        )
        database.reminderDao().update(restored)
        if (rearm) database.reminderDao().update(restored.copy(scheduleError = alarms.schedule(restored)))
    }

    suspend fun disableReminder(id: String) = reminderWrites.withLock {
        val item = database.reminderDao().getReminderById(id) ?: return@withLock
        database.reminderDao().update(item.copy(
            notificationEnabled = false, confirmedAt = null,
            scheduleRevision = item.scheduleRevision + 1, scheduleError = null
        ))
        alarms.cancel(item)
    }

    suspend fun deleteReminder(id: String) = reminderWrites.withLock {
        database.withTransaction {
            val item = database.reminderDao().getReminderById(id) ?: return@withTransaction
            database.extractionSuppressionDao().insert(ExtractionSuppression(
                item.recordingId, "REMINDER", item.extractionFingerprint ?: fingerprint("REMINDER", "${item.title}\n${item.originalText}")
            ))
            database.reminderDao().delete(item)
            alarms.cancel(item)
        }
    }

    suspend fun clearReminderHistory() = reminderWrites.withLock {
        database.withTransaction {
            val reminders = database.reminderDao().getAll()
            val now = clock.millis()
            reminders.filter { item ->
                item.status != ReminderStatus.ACTIVE || ((item.eventTimeMs ?: Long.MAX_VALUE) < now && !item.needsReview)
            }.forEach { item ->
                database.reminderDao().delete(item)
                alarms.cancel(item)
            }
        }
    }

    suspend fun createTask(sourceId: String, fromReminder: Boolean, text: String, reminder: ReminderEntity? = null): String =
        reminderWrites.withLock {
            require(text.isNotBlank()) { "Enter a next step." }
            val result = database.withTransaction {
                val dao = database.insightDao()
                dao.getDerivedTask(sourceId)?.let { return@withTransaction it.id }
                val sourceIdea = if (!fromReminder) dao.getById(sourceId) else null
                val sourceReminder = if (fromReminder) database.reminderDao().getReminderById(sourceId) else null
                val recordingId = sourceIdea?.recordingId ?: sourceReminder?.recordingId ?: error("Source no longer exists.")
                require(sourceExists(recordingId)) { "Source note unavailable." }
                val id = UUID.randomUUID().toString()
                dao.insertInsight(InsightEntity(
                    id = id, recordingId = recordingId, kind = InsightKind.ACTION, text = text.trim(),
                    createdAt = clock.millis(), sourceInsightId = sourceIdea?.id, sourceReminderId = sourceReminder?.id
                ))
                if (sourceReminder != null) {
                    database.reminderDao().update(sourceReminder.copy(
                        status = ReminderStatus.DISMISSED, notificationEnabled = false,
                        needsReview = false, scheduleRevision = sourceReminder.scheduleRevision + 1, linkedTaskId = id
                    ))
                }
                if (reminder != null) {
                    require(ReminderPolicy.canSchedule(reminder, clock.millis())) { "Choose a future reminder time." }
                    database.reminderDao().insertReminders(listOf(reminder.copy(recordingId = recordingId, linkedTaskId = id)))
                }
                id
            }
            if (fromReminder) database.reminderDao().getReminderById(sourceId)?.let { alarms.cancel(it) }
            database.reminderDao().getAll().filter { it.linkedTaskId == result && it.notificationEnabled }.forEach {
                alarms.cancel(it)
                database.reminderDao().update(it.copy(scheduleError = alarms.schedule(it)))
            }
            result
        }

    private suspend fun sourceExists(recordingId: Long): Boolean =
        SourceIds.isChat(recordingId) || database.recordingDao().getRecordingById(recordingId) != null

    /** Creates a reminder or target date that has no source note, e.g. one requested in Srutam AI chat. */
    suspend fun createChatReminder(
        title: String, type: String, time: ResolvedReminderTime, notify: Boolean
    ): ReminderEntity = reminderWrites.withLock {
        require(title.isNotBlank()) { "Enter a title." }
        val now = clock.millis()
        val exactFuture = time.precision == "EXACT" && (time.eventTimeMs ?: 0) > now
        val enabled = notify && exactFuture && type != ReminderType.MILESTONE
        val item = ReminderEntity(
            recordingId = SourceIds.CHAT, recordingName = "Srutam AI", title = title.trim(),
            eventTimeMs = time.eventTimeMs, originalText = title.trim(), type = type,
            timePrecision = time.precision, localDate = time.localDate, localTime = time.localTime,
            zoneId = time.zoneId, zoneInferred = false, notificationEnabled = enabled,
            confirmedAt = if (enabled) now else null, needsReview = false
        )
        database.reminderDao().insertReminders(listOf(item))
        if (!enabled) return@withLock item
        item.copy(scheduleError = alarms.schedule(item)).also { database.reminderDao().update(it) }
    }

    /** Creates a next step, idea or decision with no source note. */
    suspend fun createChatInsight(kind: String, text: String, rationale: String? = null): String = reminderWrites.withLock {
        require(text.isNotBlank()) { "Enter some text." }
        val id = UUID.randomUUID().toString()
        database.insightDao().insertInsight(InsightEntity(
            id = id, recordingId = SourceIds.CHAT, recordingName = "Srutam AI", kind = kind,
            text = text.trim(), rationale = rationale?.trim()?.ifBlank { null }, createdAt = clock.millis(),
            extractionFingerprint = fingerprint(kind, text)
        ))
        id
    }

    suspend fun editInsightText(id: String, text: String) = reminderWrites.withLock {
        require(text.isNotBlank()) { "Enter some text." }
        val dao = database.insightDao()
        val item = dao.getById(id) ?: return@withLock
        dao.updateInsight(item.copy(text = text.trim(), extractionFingerprint = fingerprint(item.kind, text)))
    }

    suspend fun setInsightStatus(id: String, status: String) = reminderWrites.withLock {
        val dao = database.insightDao()
        val item = dao.getById(id) ?: return@withLock
        dao.updateInsight(item.copy(
            status = status, archivedAt = if (status == InsightStatus.ARCHIVED) clock.millis() else null,
            completedAt = if (status == InsightStatus.COMPLETED) (item.completedAt ?: clock.millis()) else null
        ))
    }

    suspend fun archiveTask(id: String) = reminderWrites.withLock {
        val dao = database.insightDao()
        val item = dao.getById(id) ?: return@withLock
        dao.updateInsight(item.copy(status = InsightStatus.ARCHIVED, archivedAt = clock.millis()))
    }

    suspend fun getReminder(id: String): ReminderEntity? = database.reminderDao().getReminderById(id)
    suspend fun getInsight(id: String): InsightEntity? = database.insightDao().getById(id)
    suspend fun allReminders(): List<ReminderEntity> = database.reminderDao().getAll()
    suspend fun allInsights(): List<InsightEntity> = database.insightDao().getAllInsightsFlow().first()

    suspend fun reconcile() = reminderWrites.withLock {
        database.insightDao().autoArchiveStaleCompleted(clock.millis() - 3 * 24 * 60 * 60 * 1000L, clock.millis())
        for (item in database.reminderDao().getAll()) {
            alarms.cancel(item) // Also cancels old payloads without the new URI identity.
            if (sourceExists(item.recordingId) && ReminderPolicy.canSchedule(item, clock.millis())) {
                database.reminderDao().update(item.copy(scheduleError = alarms.schedule(item)))
            }
        }
    }

    /** Invoked after audio deletion succeeds, before deleting its source metadata. */
    suspend fun deleteRecordingData(recordingId: Long) = reminderWrites.withLock {
        database.withTransaction {
            database.reminderDao().getRemindersByRecordingId(recordingId).forEach { alarms.cancel(it) }
            database.reminderDao().deleteRemindersByRecordingId(recordingId)
            database.insightDao().deleteInsightsByRecordingId(recordingId)
            database.extractionSuppressionDao().deleteForRecording(recordingId)
            database.recordingDao().deleteRecordingById(recordingId)
        }
    }

    companion object {
        private val reminderWrites = Mutex()
        private const val UNDO_WINDOW_MS = 24L * 60 * 60 * 1000
        fun from(context: Context) = InsightsRepository(AppDatabase.getDatabase(context), AndroidReminderAlarms(context.applicationContext))
        fun fingerprint(kind: String, text: String): String {
            val normalized = Normalizer.normalize(text, Normalizer.Form.NFKC).trim()
                .lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
            return MessageDigest.getInstance("SHA-256").digest("$kind:$normalized".toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        }
    }
}
