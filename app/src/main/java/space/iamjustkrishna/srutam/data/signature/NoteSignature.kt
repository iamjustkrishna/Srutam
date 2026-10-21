package space.iamjustkrishna.srutam.data.signature

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.gson.Gson
import space.iamjustkrishna.srutam.data.AppDatabase
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.NoteSource
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import space.iamjustkrishna.srutam.data.ReminderEntity
import space.iamjustkrishna.srutam.data.SourceIds
import space.iamjustkrishna.srutam.data.SyncStatus
import space.iamjustkrishna.srutam.service.RecordingCoordinator
import java.io.File
import java.util.concurrent.TimeUnit

/** What a note's block holds: everything needed to rebuild the note without AI. */
data class NotePayload(
    val v: Int = 1,
    val noteUuid: String,
    val audioHash: String?,
    val name: String,
    val timestamp: Long,
    val duration: Long,
    val recordedZoneId: String?,
    val transcript: String?,
    val summary: String?,
    val keyPoints: String?,
    val actionItems: String?,
    val wiifm: String?,
    val topics: String?,
    val aiStatus: String,
    val updatedAt: Long,
    val insights: List<InsightEntity>,
    val reminders: List<ReminderEntity>
)

/**
 * Keeps each note's block (see [NoteBlock]) in step with the database, and rebuilds notes from
 * blocks after a reinstall.
 */
object NoteSignature {
    private const val TAG = "NoteSignature"
    private val gson = Gson()

    fun toJson(payload: NotePayload): String = gson.toJson(payload)

    fun fromJson(json: String): NotePayload? = try {
        gson.fromJson(json, NotePayload::class.java)?.takeIf { it.noteUuid.isNotBlank() }
    } catch (e: Exception) {
        null
    }

    fun payloadOf(recording: Recording, insights: List<InsightEntity>, reminders: List<ReminderEntity>) = NotePayload(
        noteUuid = recording.noteUuid,
        audioHash = recording.audioHash,
        name = recording.name,
        timestamp = recording.timestamp,
        duration = recording.duration,
        recordedZoneId = recording.recordedZoneId,
        transcript = recording.transcript,
        summary = recording.summary,
        keyPoints = recording.keyPoints,
        actionItems = recording.actionItems,
        wiifm = recording.wiifm,
        topics = recording.topics,
        aiStatus = recording.aiStatus,
        updatedAt = recording.updatedAt,
        insights = insights,
        reminders = reminders
    )

    /**
     * Writes the block for one note. Private notes never get one: the file sits in a public folder
     * and travels with the audio when shared. Returns whether a block was written.
     */
    suspend fun writeFor(database: AppDatabase, recordingId: Long): Boolean =
        writeForResult(database, recordingId) == WriteResult.WRITTEN

    /** What happened to a note's block. */
    enum class WriteResult {
        WRITTEN,
        /** Nothing to write: no note, no file, a private note, or the recording still in progress. */
        SKIPPED,
        /** The file belongs to a previous install; the user has to allow Srutam to modify it. */
        NEEDS_ACCESS
    }

    /**
     * Like [writeFor], but says why nothing was written. When the file can't be written by path,
     * [fallbackWriter] gets a try: it writes through the file's MediaStore URI, which works once
     * the user has allowed Srutam to modify recordings from a previous install.
     */
    suspend fun writeForResult(
        database: AppDatabase,
        recordingId: Long,
        fallbackWriter: ((File, String) -> Boolean)? = null
    ): WriteResult {
        val dao = database.recordingDao()
        var recording = dao.getRecordingById(recordingId) ?: return WriteResult.SKIPPED
        val file = File(recording.audioFilePath)
        if (!file.isFile || file.absolutePath == RecordingCoordinator.activeFilePath) return WriteResult.SKIPPED
        if (recording.isPrivate) {
            NoteBlock.strip(file)
            return WriteResult.SKIPPED
        }
        if (recording.audioHash == null) {
            val hash = runCatching { NoteBlock.audioHash(file) }.getOrNull() ?: return WriteResult.SKIPPED
            dao.setAudioHash(recording.id, hash)
            recording = recording.copy(audioHash = hash)
        }
        val payload = payloadOf(
            recording,
            database.insightDao().getInsightsByRecordingId(recording.id),
            database.reminderDao().getRemindersByRecordingId(recording.id)
        )
        val json = toJson(payload)
        if (NoteBlock.write(file, json)) return WriteResult.WRITTEN
        if (fallbackWriter?.invoke(file, json) == true) return WriteResult.WRITTEN
        Log.d(TAG, "Could not write the block for ${file.name}; it belongs to a previous install")
        return WriteResult.NEEDS_ACCESS
    }

    /**
     * Rebuilds a note from the block inside [file], with no AI and no network. Returns the note's
     * id, or null when the file has no valid block. A note that already exists (same id) is only
     * pointed at this file if its old one is gone.
     */
    suspend fun restoreFromFile(
        database: AppDatabase,
        file: File,
        onRestored: ((NotePayload) -> Unit)? = null
    ): Long? {
        val payload = NoteBlock.read(file)?.let(::fromJson) ?: return null
        val dao = database.recordingDao()
        dao.getRecordingByNoteUuid(payload.noteUuid)?.let { existing ->
            if (existing.audioFilePath != file.absolutePath && !File(existing.audioFilePath).exists()) {
                dao.updateRecording(existing.copy(audioFilePath = file.absolutePath))
            }
            return existing.id
        }
        val audioHash = runCatching { NoteBlock.audioHash(file) }.getOrNull() ?: payload.audioHash
        onRestored?.invoke(payload)
        return database.withTransaction {
            val id = dao.insertRecording(recordingFrom(payload, file, audioHash))
            database.insightDao().insertInsights(
                payload.insights.orEmpty().map { it.copy(recordingId = id, recordingName = payload.name) }
            )
            database.reminderDao().insertReminders(
                payload.reminders.orEmpty().map { it.copy(recordingId = id, recordingName = payload.name) }
            )
            id
        }
    }

    internal fun recordingFrom(payload: NotePayload, file: File, audioHash: String?): Recording {
        // A block written mid-processing has no finished result; let the note be processed again.
        val finished = payload.aiStatus == RecordingAiStatus.READY
        return Recording(
            timestamp = payload.timestamp,
            audioFilePath = file.absolutePath,
            duration = payload.duration,
            name = payload.name,
            transcript = payload.transcript,
            summary = payload.summary,
            keyPoints = payload.keyPoints,
            actionItems = payload.actionItems,
            wiifm = payload.wiifm,
            aiStatus = if (finished) RecordingAiStatus.READY else RecordingAiStatus.NOT_REQUESTED,
            isProcessing = false,
            syncStatus = if (finished) SyncStatus.PENDING else SyncStatus.NOT_SYNCED,
            recordedZoneId = payload.recordedZoneId,
            insightsImported = true,
            noteUuid = payload.noteUuid,
            audioHash = audioHash,
            topics = payload.topics,
            updatedAt = payload.updatedAt,
            contentSource = NoteSource.FILE
        )
    }
}

/** Debounced background write of one note's block, so a burst of edits rewrites the file once. */
class NoteSignatureWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_RECORDING_ID, -1L)
        if (id <= 0) return Result.success()
        return try {
            val result = NoteSignature.writeForResult(
                AppDatabase.getDatabase(applicationContext),
                id,
                fallbackWriter = { file, json -> BlockWriteAccess.writeViaUri(applicationContext, file, json) }
            )
            when (result) {
                NoteSignature.WriteResult.NEEDS_ACCESS -> BlockWriteAccess.markPending(applicationContext, id)
                else -> BlockWriteAccess.clearPending(applicationContext, id)
            }
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Writing the note block failed for $id", e)
            Result.success()
        }
    }

    companion object {
        private const val TAG = "NoteSignatureWorker"
        private const val KEY_RECORDING_ID = "recording_id"
        private const val DEBOUNCE_SECONDS = 3L

        fun enqueue(context: Context, recordingId: Long) {
            if (recordingId <= 0 || SourceIds.isChat(recordingId)) return
            val request = OneTimeWorkRequestBuilder<NoteSignatureWorker>()
                .setInitialDelay(DEBOUNCE_SECONDS, TimeUnit.SECONDS)
                .setInputData(workDataOf(KEY_RECORDING_ID to recordingId))
                .build()
            runCatching {
                WorkManager.getInstance(context.applicationContext)
                    .enqueueUniqueWork("note_signature_$recordingId", ExistingWorkPolicy.REPLACE, request)
            }
        }
    }
}
