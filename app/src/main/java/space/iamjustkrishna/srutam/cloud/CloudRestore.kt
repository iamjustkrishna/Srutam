package space.iamjustkrishna.srutam.cloud

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.first
import space.iamjustkrishna.srutam.data.AppDatabase
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.InsightKind
import space.iamjustkrishna.srutam.data.InsightStatus
import space.iamjustkrishna.srutam.data.NoteSource
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import space.iamjustkrishna.srutam.data.ReminderEntity
import space.iamjustkrishna.srutam.data.ReminderStatus
import space.iamjustkrishna.srutam.data.SyncStatus
import space.iamjustkrishna.srutam.data.signature.NotePayload
import space.iamjustkrishna.srutam.data.signature.NoteSignatureWorker
import space.iamjustkrishna.srutam.repository.InsightsRepository
import space.iamjustkrishna.srutam.service.ReminderScheduler
import space.iamjustkrishna.srutam.utils.AudioFileReader
import space.iamjustkrishna.srutam.utils.RecordingFileNames
import java.io.File
import java.time.OffsetDateTime
import java.util.UUID

/**
 * After a sign-in, pulls the account's notes, insights, tasks and reminders and merges them into
 * this phone by the rules in [RestoreMerge]. Runs inside [CloudSyncWorker] before any upload.
 */
class CloudRestore(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context),
    private val client: SupabaseCloudClient = SupabaseCloudClient(context),
    private val audioFiles: () -> List<LocalAudio> = {
        AudioFileReader.getAudioFiles().map {
            LocalAudio(it.filePath, RecordingFileNames.titleOf(it.fileName), it.timestamp, it.duration)
        }
    },
    private val mergeBase: MergeBase = MergeBase(context)
) {

    data class Summary(val restored: Int, val withoutAudio: Int)

    suspend fun run(): Result<Summary> {
        val notes = client.fetchAll(
            "notes",
            "id,note_uuid,audio_hash,title,transcript,summary,key_points,wiifm,ai_status,duration_ms," +
                "timestamp,is_private,topics,updated_at,client_updated_at"
        ).getOrElse { return Result.failure(it) }
        val tasks = client.fetchAll(
            "action_items", "note_id,client_insight_id,description,is_completed,completed_at,created_at"
        ).getOrElse { return Result.failure(it) }
        val insights = client.fetchAll(
            "note_insights",
            "note_id,client_insight_id,kind,text,evidence,rationale,status,completed_at,archived_at," +
                "source_insight_id,source_reminder_id,created_at"
        ).getOrElse { return Result.failure(it) }
        val reminders = client.fetchAll(
            "reminders",
            "note_id,client_reminder_id,title,event_time,original_text,person,location,type,status," +
                "needs_review,confirmed_at,time_precision,local_date,local_time,zone_id,linked_task_id,created_at"
        ).getOrElse { return Result.failure(it) }

        return try {
            Result.success(
                apply(
                    notes.map { parseNote(it) },
                    tasks.groupBy { it.str("note_id").orEmpty() },
                    insights.groupBy { it.str("note_id").orEmpty() },
                    reminders.groupBy { it.str("note_id").orEmpty() }
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Applying the cloud restore failed", e)
            Result.failure(e)
        }
    }

    /** Applies cloud rows to the database. Children are grouped by cloud note id. */
    internal suspend fun apply(
        cloudNotes: List<CloudNote>,
        tasksByNote: Map<String, List<JsonObject>>,
        insightsByNote: Map<String, List<JsonObject>>,
        remindersByNote: Map<String, List<JsonObject>>
    ): Summary {
        val dao = database.recordingDao()
        val locals = dao.getAllRecordings().first().toMutableList()
        val claimedPaths = locals.map { it.audioFilePath }.toMutableSet()
        val unclaimedAudio = audioFiles().filter { it.path !in claimedPaths }.toMutableList()
        val now = System.currentTimeMillis()
        var restored = 0
        var withoutAudio = 0
        val matched = mutableSetOf<Long>()

        // Keeps the unique noteUuid unique: take the cloud's only if no other local note has it.
        fun uuidFor(local: Recording?, cloud: CloudNote): String {
            val cloudUuid = cloud.noteUuid ?: return local?.noteUuid ?: UUID.randomUUID().toString()
            val owner = locals.firstOrNull { it.noteUuid == cloudUuid }
            return if (owner == null || owner.id == local?.id) cloudUuid else local?.noteUuid ?: UUID.randomUUID().toString()
        }

        for (cloud in cloudNotes) {
            val children = cloudChildren(
                cloud,
                tasksByNote[cloud.id].orEmpty(),
                insightsByNote[cloud.id].orEmpty(),
                remindersByNote[cloud.id].orEmpty()
            )
            // Each local note takes at most one cloud note.
            val local = RestoreMerge.match(locals.filter { it.id !in matched }, cloud)
            local?.let { matched += it.id }
            if (local == null) {
                // Not on this phone as a note. Its audio may still be here without a note.
                val audio = RestoreMerge.matchAudio(unclaimedAudio, cloud)
                if (audio != null) unclaimedAudio.remove(audio) else withoutAudio++
                val path = audio?.path ?: textOnlyPath(cloud.id)
                val id = database.withTransaction {
                    val id = dao.insertRecording(fromCloud(cloud, path, now).copy(noteUuid = uuidFor(null, cloud)))
                    replaceChildren(id, cloud.title, children, keepLocalExtras = false)
                    id
                }
                dao.getRecordingById(id)?.let { locals += it; matched += it.id }
                restored++
                continue
            }

            // Restored from its recording: merge both sides' changes against that block, like git.
            val base = if (local.contentSource == NoteSource.FILE) mergeBase.load(local.noteUuid) else null
            if (base != null) {
                mergeThreeWay(local, cloud, base, children, uuidFor(local, cloud), now)
                restored++
            } else when (RestoreMerge.winner(local, cloud)) {
                RestoreMerge.Winner.CLOUD -> database.withTransaction {
                    dao.updateRecording(
                        fromCloud(cloud, local.audioFilePath, now).copy(
                            id = local.id,
                            noteUuid = uuidFor(local, cloud),
                            audioHash = local.audioHash ?: cloud.audioHash,
                            recordedZoneId = local.recordedZoneId
                        )
                    )
                    // Extras made by a re-run of AI after the reinstall are duplicates of the cloud's,
                    // but a task done on one of them before signing in stays done on the cloud's.
                    val reprocessed = local.contentSource == NoteSource.AI
                    replaceChildren(
                        local.id,
                        cloud.title,
                        children,
                        keepLocalExtras = !reprocessed,
                        matchByText = reprocessed
                    )
                    restored++
                }
                RestoreMerge.Winner.LOCAL -> database.withTransaction {
                    dao.updateRecording(
                        local.copy(
                            cloudId = cloud.id,
                            noteUuid = uuidFor(local, cloud),
                            syncStatus = SyncStatus.PENDING
                        )
                    )
                    carryProgress(local.id, children)
                }
            }
            dao.getRecordingById(local.id)?.let { updated ->
                locals[locals.indexOfFirst { it.id == local.id }] = updated
            }
            NoteSignatureWorker.enqueue(context, local.id)
        }

        // Future reminders come back scheduled; the ones dropped above were cancelled.
        runCatching { InsightsRepository.from(context).reconcile() }
        return Summary(restored, withoutAudio)
    }

    private fun fromCloud(cloud: CloudNote, path: String, now: Long) = Recording(
        timestamp = cloud.timestampMs,
        audioFilePath = path,
        duration = cloud.durationMs,
        name = cloud.title,
        transcript = cloud.transcript,
        summary = cloud.summary,
        keyPoints = cloud.keyPoints,
        wiifm = cloud.wiifm,
        aiStatus = RecordingAiStatus.READY,
        isProcessing = false,
        processingError = null,
        syncStatus = SyncStatus.SYNCED,
        isPrivate = cloud.isPrivate,
        cloudId = cloud.id,
        // Lets an AI run that started before this restore see that the cloud took over.
        lastSyncedAt = now,
        insightsImported = true,
        noteUuid = cloud.noteUuid ?: UUID.randomUUID().toString(),
        audioHash = cloud.audioHash,
        topics = cloud.topics,
        updatedAt = cloud.updatedAtMs,
        contentSource = NoteSource.CLOUD
    )

    private data class Children(val insights: List<InsightEntity>, val reminders: List<ReminderEntity>)

    private fun cloudChildren(
        cloud: CloudNote,
        tasks: List<JsonObject>,
        insights: List<JsonObject>,
        reminders: List<JsonObject>
    ): Children {
        val fromTasks = tasks.mapIndexed { order, row ->
            val done = row.bool("is_completed") == true
            InsightEntity(
                id = row.str("client_insight_id") ?: UUID.randomUUID().toString(),
                recordingId = 0,
                recordingName = cloud.title,
                kind = InsightKind.ACTION,
                text = row.str("description").orEmpty(),
                status = if (done) InsightStatus.COMPLETED else InsightStatus.OPEN,
                createdAt = row.time("created_at") ?: cloud.timestampMs,
                completedAt = if (done) row.time("completed_at") else null,
                sourceOrder = order
            )
        }
        val fromInsights = insights.mapIndexed { order, row ->
            InsightEntity(
                id = row.str("client_insight_id") ?: UUID.randomUUID().toString(),
                recordingId = 0,
                recordingName = cloud.title,
                kind = if (row.str("kind") == "decision") InsightKind.DECISION else InsightKind.IDEA,
                text = row.str("text").orEmpty(),
                evidence = row.str("evidence"),
                rationale = row.str("rationale"),
                status = row.str("status") ?: InsightStatus.OPEN,
                createdAt = row.time("created_at") ?: cloud.timestampMs,
                completedAt = row.time("completed_at"),
                archivedAt = row.time("archived_at"),
                sourceOrder = order,
                sourceInsightId = row.str("source_insight_id"),
                sourceReminderId = row.str("source_reminder_id")
            )
        }
        val fromReminders = reminders.map { row ->
            ReminderEntity(
                id = row.str("client_reminder_id") ?: UUID.randomUUID().toString(),
                recordingId = 0,
                recordingName = cloud.title,
                title = row.str("title").orEmpty(),
                eventTimeMs = row.time("event_time"),
                originalText = row.str("original_text").orEmpty(),
                person = row.str("person"),
                location = row.str("location"),
                type = row.str("type") ?: "REMINDER",
                status = row.str("status") ?: ReminderStatus.ACTIVE,
                createdAt = row.time("created_at") ?: cloud.timestampMs,
                timePrecision = row.str("time_precision") ?: "UNKNOWN",
                localDate = row.str("local_date"),
                localTime = row.str("local_time"),
                zoneId = row.str("zone_id"),
                confirmedAt = row.time("confirmed_at"),
                needsReview = row.bool("needs_review") ?: true,
                // Reviewed reminders notify again once restored and rescheduled.
                notificationEnabled = row.time("confirmed_at") != null,
                linkedTaskId = row.str("linked_task_id")
            )
        }
        return Children(fromTasks + fromInsights, fromReminders)
    }

    /**
     * Makes the note's insights and reminders the cloud's. A status only moves forward, so a
     * "done" on either side is kept. Local rows the cloud doesn't have are dropped unless
     * [keepLocalExtras].
     */
    private suspend fun replaceChildren(
        recordingId: Long,
        name: String,
        cloud: Children,
        keepLocalExtras: Boolean,
        matchByText: Boolean = false
    ) {
        val insightDao = database.insightDao()
        val reminderDao = database.reminderDao()
        val localInsights = insightDao.getInsightsByRecordingId(recordingId).associateBy { it.id }
        val localReminders = reminderDao.getRemindersByRecordingId(recordingId).associateBy { it.id }
        val cloudInsights = if (matchByText) {
            RestoreMerge.carryProgressByText(localInsights.values.toList(), cloud.insights)
        } else {
            cloud.insights
        }

        insightDao.insertInsights(cloudInsights.map { remote ->
            val status = localInsights[remote.id]?.let { RestoreMerge.mostProgressedInsight(remote.status, it.status) } ?: remote.status
            remote.copy(recordingId = recordingId, recordingName = name, status = status)
        })
        reminderDao.insertReminders(cloud.reminders.map { remote ->
            val status = localReminders[remote.id]?.let { RestoreMerge.mostProgressedReminder(remote.status, it.status) } ?: remote.status
            remote.copy(recordingId = recordingId, recordingName = name, status = status)
        })
        if (!keepLocalExtras) {
            val cloudInsightIds = cloud.insights.map { it.id }.toSet()
            val cloudReminderIds = cloud.reminders.map { it.id }.toSet()
            localInsights.values.filter { it.id !in cloudInsightIds }.forEach { insightDao.deleteInsight(it) }
            localReminders.values.filter { it.id !in cloudReminderIds }.forEach {
                ReminderScheduler.cancelReminder(context, it)
                reminderDao.delete(it)
            }
        }
    }

    /**
     * Merges a note restored from its recording with its cloud copy, using the restored block as
     * the common ancestor: a change on only one side is kept, a field both sides changed goes to
     * the newer edit (the other value is kept in [MergeBase]'s conflicts), and tasks, ideas and
     * reminders merge by id with statuses only moving forward. The result uploads afterwards.
     */
    private suspend fun mergeThreeWay(
        local: Recording,
        cloud: CloudNote,
        base: NotePayload,
        children: Children,
        noteUuid: String,
        now: Long
    ) {
        val sides = RestoreMerge.Sides(
            phoneNewer = local.updatedAt >= cloud.updatedAtMs,
            cloudMovedOn = cloud.updatedAtMs > base.updatedAt,
            now = now
        )
        // JSON lists compare by value: the cloud's jsonb may be spaced differently from the phone's text.
        fun json(text: String?): String? = text?.let { runCatching { JsonParser.parseString(it).toString() }.getOrDefault(it) }
        val merged = RestoreMerge.mergeContent(
            base = RestoreMerge.NoteContent(base.name, base.transcript, base.summary, json(base.keyPoints), base.wiifm, json(base.topics), local.isPrivate),
            phone = RestoreMerge.NoteContent(local.name, local.transcript, local.summary, json(local.keyPoints), local.wiifm, json(local.topics), local.isPrivate),
            cloud = RestoreMerge.NoteContent(cloud.title, cloud.transcript, cloud.summary, json(cloud.keyPoints), cloud.wiifm, json(cloud.topics), cloud.isPrivate),
            sides = sides
        )
        val insightDao = database.insightDao()
        val reminderDao = database.reminderDao()
        val conflicts = merged.conflicts.toMutableList()
        database.withTransaction {
            val content = merged.content
            database.recordingDao().updateRecording(
                local.copy(
                    name = content.title,
                    transcript = content.transcript,
                    summary = content.summary,
                    keyPoints = content.keyPoints,
                    wiifm = content.wiifm,
                    topics = content.topics,
                    isPrivate = content.isPrivate,
                    aiStatus = RecordingAiStatus.READY,
                    isProcessing = false,
                    processingError = null,
                    cloudId = cloud.id,
                    noteUuid = noteUuid,
                    audioHash = local.audioHash ?: cloud.audioHash,
                    // The merge may hold phone changes the cloud lacks: upload it once the restore is done.
                    syncStatus = SyncStatus.PENDING,
                    // Lets an AI run that started before this restore see that the note was settled.
                    contentSource = NoteSource.CLOUD,
                    lastSyncedAt = now,
                    updatedAt = maxOf(local.updatedAt, cloud.updatedAtMs)
                )
            )

            val insights = RestoreMerge.mergeInsights(
                base.insights,
                insightDao.getInsightsByRecordingId(local.id),
                children.insights,
                sides
            )
            insightDao.insertInsights(insights.write.map { it.copy(recordingId = local.id, recordingName = content.title) })
            insights.deleteLocal.forEach { insightDao.deleteInsight(it) }
            conflicts += insights.conflicts

            val reminders = RestoreMerge.mergeReminders(
                base.reminders,
                reminderDao.getRemindersByRecordingId(local.id),
                children.reminders,
                sides
            )
            reminderDao.insertReminders(reminders.write.map { it.copy(recordingId = local.id, recordingName = content.title) })
            reminders.deleteLocal.forEach {
                ReminderScheduler.cancelReminder(context, it)
                reminderDao.delete(it)
            }
            conflicts += reminders.conflicts
        }
        mergeBase.recordConflicts(local.noteUuid, conflicts)
        mergeBase.clear(local.noteUuid)
    }

    /** The local note wins, but a status the cloud moved further (an agent's "done") is kept. */
    private suspend fun carryProgress(recordingId: Long, cloud: Children) {
        val insightDao = database.insightDao()
        val reminderDao = database.reminderDao()
        val remoteInsights = cloud.insights.associateBy { it.id }
        val remoteReminders = cloud.reminders.associateBy { it.id }
        insightDao.getInsightsByRecordingId(recordingId).forEach { local ->
            val remote = remoteInsights[local.id] ?: return@forEach
            if (RestoreMerge.insightRank(remote.status) > RestoreMerge.insightRank(local.status)) {
                insightDao.updateInsight(
                    local.copy(status = remote.status, completedAt = remote.completedAt ?: local.completedAt, archivedAt = remote.archivedAt ?: local.archivedAt)
                )
            }
        }
        reminderDao.getRemindersByRecordingId(recordingId).forEach { local ->
            val remote = remoteReminders[local.id] ?: return@forEach
            if (RestoreMerge.reminderRank(remote.status) > RestoreMerge.reminderRank(local.status)) {
                reminderDao.update(local.copy(status = remote.status))
            }
        }
    }

    companion object {
        private const val TAG = "CloudRestore"
        private const val TEXT_ONLY_SCHEME = "cloud://"

        /** Notes restored from the cloud without their audio point here instead of at a file. */
        fun textOnlyPath(cloudId: String) = "$TEXT_ONLY_SCHEME$cloudId"

        fun isTextOnlyPath(path: String) = path.startsWith(TEXT_ONLY_SCHEME)

        internal fun parseNote(row: JsonObject): CloudNote {
            val updated = row.time("client_updated_at") ?: row.time("updated_at") ?: 0L
            return CloudNote(
                id = row.str("id")!!,
                noteUuid = row.str("note_uuid"),
                audioHash = row.str("audio_hash"),
                title = row.str("title") ?: "Voice Note",
                transcript = row.str("transcript"),
                summary = row.str("summary"),
                keyPoints = row.raw("key_points"),
                wiifm = row.str("wiifm"),
                aiStatus = row.str("ai_status") ?: RecordingAiStatus.READY,
                durationMs = row.long("duration_ms") ?: 0L,
                timestampMs = row.time("timestamp") ?: 0L,
                isPrivate = row.bool("is_private") ?: false,
                topics = row.raw("topics"),
                updatedAtMs = updated
            )
        }

        private fun JsonObject.value(key: String): JsonElement? = get(key)?.takeIf { !it.isJsonNull }
        internal fun JsonObject.str(key: String): String? = value(key)?.asString
        private fun JsonObject.bool(key: String): Boolean? = value(key)?.asBoolean
        private fun JsonObject.long(key: String): Long? = value(key)?.asLong
        /** A JSON value (array or object) kept as its JSON text, as the Recording entity stores it. */
        private fun JsonObject.raw(key: String): String? = value(key)?.toString()
        private fun JsonObject.time(key: String): Long? = str(key)?.let {
            runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
        }
    }
}

/** True when the note's audio isn't on this device (restored from the cloud as text only, or deleted). */
fun Recording.isTextOnly(): Boolean =
    CloudRestore.isTextOnlyPath(audioFilePath) || !File(audioFilePath).exists()
