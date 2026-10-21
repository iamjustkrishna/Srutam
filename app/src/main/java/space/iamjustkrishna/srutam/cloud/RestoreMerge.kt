package space.iamjustkrishna.srutam.cloud

import space.iamjustkrishna.srutam.data.InsightStatus
import space.iamjustkrishna.srutam.data.NoteSource
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import space.iamjustkrishna.srutam.data.ReminderStatus
import kotlin.math.abs

/** A note as stored in Srutam Cloud. Times are epoch millis. */
data class CloudNote(
    val id: String,
    val noteUuid: String?,
    val audioHash: String?,
    val title: String,
    val transcript: String?,
    val summary: String?,
    val keyPoints: String?,
    val wiifm: String?,
    val aiStatus: String,
    val durationMs: Long,
    val timestampMs: Long,
    val isPrivate: Boolean,
    val topics: String?,
    /** When the content last changed on a phone, else when the row last changed. */
    val updatedAtMs: Long
)

/** An audio file on the phone that has no note yet. */
data class LocalAudio(val path: String, val title: String, val lastModifiedMs: Long, val durationMs: Long)

/**
 * The rules for merging Srutam Cloud into this phone after a sign-in. Pure logic, so every rule
 * is unit-tested; [CloudRestore] applies the result to the database.
 */
object RestoreMerge {

    /** Two notes this close in time and length are the same recording. */
    private const val SAME_TIME_MS = 5_000L
    private const val SAME_DURATION_MS = 2_000L

    enum class Winner { CLOUD, LOCAL }

    /** Finds the local note for [cloud]: by cloud id, then note id, then audio hash, then name and time. */
    fun match(locals: List<Recording>, cloud: CloudNote): Recording? =
        locals.firstOrNull { it.cloudId == cloud.id }
            ?: cloud.noteUuid?.let { uuid -> locals.firstOrNull { it.noteUuid == uuid } }
            ?: cloud.audioHash?.let { hash -> locals.firstOrNull { it.audioHash == hash } }
            ?: locals.firstOrNull { it.cloudId == null && sameRecording(it.name, it.timestamp, it.duration, cloud) }

    /** An audio file without a note (recorded before note blocks existed) that is this cloud note's audio. */
    fun matchAudio(files: List<LocalAudio>, cloud: CloudNote): LocalAudio? =
        files.firstOrNull { sameRecording(it.title, it.lastModifiedMs, it.durationMs, cloud) }

    private fun sameRecording(title: String, timeMs: Long, durationMs: Long, cloud: CloudNote): Boolean {
        val sameTime = abs(timeMs - cloud.timestampMs) <= SAME_TIME_MS
        val sameLength = durationMs <= 0 || cloud.durationMs <= 0 || abs(durationMs - cloud.durationMs) <= SAME_DURATION_MS
        return sameTime && (sameLength || title.trim().equals(cloud.title.trim(), ignoreCase = true))
    }

    /**
     * Whose content a matched note keeps.
     *  - AI queued or still running: the cloud (the AI result is thrown away).
     *  - Re-processed by AI after the reinstall: the cloud, the original with any agent edits.
     *  - Restored from the audio file's block, or from the cloud before: whichever is newer.
     */
    fun winner(local: Recording, cloud: CloudNote): Winner {
        val aiFinished = !local.isProcessing && local.aiStatus == RecordingAiStatus.READY
        if (!aiFinished) return Winner.CLOUD
        if (local.contentSource == NoteSource.AI) return Winner.CLOUD
        return if (local.updatedAt > cloud.updatedAtMs) Winner.LOCAL else Winner.CLOUD
    }

    /** OPEN < COMPLETED < ARCHIVED: a status only moves forward, so an agent's "done" is kept. */
    fun insightRank(status: String): Int = when (status) {
        InsightStatus.ARCHIVED -> 2
        InsightStatus.COMPLETED -> 1
        else -> 0
    }

    /** ACTIVE < DISMISSED / EXPIRED < COMPLETED. */
    fun reminderRank(status: String): Int = when (status) {
        ReminderStatus.COMPLETED -> 2
        ReminderStatus.ACTIVE -> 0
        else -> 1
    }

    fun mostProgressedInsight(a: String, b: String): String = if (insightRank(b) > insightRank(a)) b else a

    fun mostProgressedReminder(a: String, b: String): String = if (reminderRank(b) > reminderRank(a)) b else a
}
