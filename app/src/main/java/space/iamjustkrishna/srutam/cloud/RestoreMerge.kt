package space.iamjustkrishna.srutam.cloud

import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.InsightStatus
import space.iamjustkrishna.srutam.data.NoteSource
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import space.iamjustkrishna.srutam.data.ReminderEntity
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

    // ---------------------------------------------------------------------------------------
    // Three-way merge, for a note restored from its recording and then signed in (see MergeBase)
    // ---------------------------------------------------------------------------------------

    /** A value the merge didn't use, kept so nothing is silently lost. */
    data class Conflict(
        val field: String,
        val kept: String?,
        val discarded: String?,
        /** "phone" or "cloud": whose value was kept. */
        val keptFrom: String,
        val at: Long
    )

    /** The parts of a note both sides can edit. */
    data class NoteContent(
        val title: String,
        val transcript: String?,
        val summary: String?,
        val keyPoints: String?,
        val wiifm: String?,
        val topics: String?,
        val isPrivate: Boolean
    )

    data class ContentMerge(val content: NoteContent, val conflicts: List<Conflict>, val changedOnPhone: Boolean)

    /**
     * Who to believe about one note.
     *  - [phoneNewer]: a field both sides changed goes to the phone's value.
     *  - [cloudMovedOn]: the cloud copy changed after the base. When it didn't, its differences
     *    from the base are older than the base (the phone's last edits never reached the cloud),
     *    so they're not changes to keep.
     */
    data class Sides(val phoneNewer: Boolean, val cloudMovedOn: Boolean, val now: Long)

    /** Picks one field: a side's change beats no change; two different changes go to the newer side. */
    private fun <V> pick(
        field: String,
        base: V,
        phone: V,
        cloud: V,
        sides: Sides,
        conflicts: MutableList<Conflict>
    ): V {
        val phoneChanged = phone != base
        val cloudChanged = sides.cloudMovedOn && cloud != base
        return when {
            phoneChanged && cloudChanged && phone != cloud -> {
                val keepPhone = sides.phoneNewer
                conflicts += Conflict(
                    field = field,
                    kept = (if (keepPhone) phone else cloud)?.toString(),
                    discarded = (if (keepPhone) cloud else phone)?.toString(),
                    keptFrom = if (keepPhone) "phone" else "cloud",
                    at = sides.now
                )
                if (keepPhone) phone else cloud
            }
            phoneChanged -> phone
            cloudChanged -> cloud
            else -> phone
        }
    }

    fun mergeContent(base: NoteContent, phone: NoteContent, cloud: NoteContent, sides: Sides): ContentMerge {
        val conflicts = mutableListOf<Conflict>()
        val merged = NoteContent(
            title = pick("title", base.title, phone.title, cloud.title, sides, conflicts),
            transcript = pick("transcript", base.transcript, phone.transcript, cloud.transcript, sides, conflicts),
            summary = pick("summary", base.summary, phone.summary, cloud.summary, sides, conflicts),
            keyPoints = pick("keyPoints", base.keyPoints, phone.keyPoints, cloud.keyPoints, sides, conflicts),
            wiifm = pick("wiifm", base.wiifm, phone.wiifm, cloud.wiifm, sides, conflicts),
            topics = pick("topics", base.topics, phone.topics, cloud.topics, sides, conflicts),
            isPrivate = pick("isPrivate", base.isPrivate, phone.isPrivate, cloud.isPrivate, sides, conflicts)
        )
        return ContentMerge(merged, conflicts, changedOnPhone = merged != cloud)
    }

    /** Rows to write and local rows to delete after merging one note's tasks, ideas, decisions or reminders. */
    data class ItemMerge<T>(val write: List<T>, val deleteLocal: List<T>, val conflicts: List<Conflict>)

    /**
     * Merges rows by id, like files in a git merge:
     *  - on both sides: [both] merges them field by field;
     *  - only on the phone: new there (keep), or deleted in the cloud (dropped, unless the phone edited it);
     *  - only in the cloud: new there (keep), or deleted on the phone (dropped, unless the cloud edited it).
     */
    private fun <T> mergeById(
        base: List<T>,
        phone: List<T>,
        cloud: List<T>,
        id: (T) -> String,
        sameContent: (T, T) -> Boolean,
        sides: Sides,
        both: (base: T?, phone: T, cloud: T, conflicts: MutableList<Conflict>) -> T
    ): ItemMerge<T> {
        val baseById = base.associateBy(id)
        val phoneById = phone.associateBy(id)
        val cloudById = cloud.associateBy(id)
        val write = mutableListOf<T>()
        val delete = mutableListOf<T>()
        val conflicts = mutableListOf<Conflict>()
        for ((key, p) in phoneById) {
            val c = cloudById[key]
            val b = baseById[key]
            when {
                c != null -> write += both(b, p, c, conflicts)
                b == null -> write += p // added on the phone
                !sameContent(p, b) -> write += p // deleted in the cloud, but edited on the phone: keep it
                sides.cloudMovedOn -> delete += p // deleted in the cloud
                else -> write += p // the cloud is older than the base: it never had this row
            }
        }
        for ((key, c) in cloudById) {
            if (key in phoneById) continue
            val b = baseById[key]
            when {
                b == null -> write += c // added in the cloud
                sides.cloudMovedOn && !sameContent(c, b) -> write += c // deleted on the phone, but edited in the cloud
                else -> Unit // deleted on the phone
            }
        }
        return ItemMerge(write, delete, conflicts)
    }

    private fun insightContent(a: InsightEntity, b: InsightEntity) =
        a.text == b.text && a.evidence == b.evidence && a.rationale == b.rationale && a.status == b.status

    fun mergeInsights(base: List<InsightEntity>, phone: List<InsightEntity>, cloud: List<InsightEntity>, sides: Sides) =
        mergeById(base, phone, cloud, { it.id }, ::insightContent, sides) { b, p, c, conflicts ->
            val status = mostProgressedInsight(p.status, c.status)
            val statusFrom = if (status == p.status) p else c
            p.copy(
                text = pick("insight:${p.id}:text", b?.text ?: p.text, p.text, c.text, sides, conflicts),
                evidence = pick("insight:${p.id}:evidence", b?.evidence ?: p.evidence, p.evidence, c.evidence, sides, conflicts),
                rationale = pick("insight:${p.id}:rationale", b?.rationale ?: p.rationale, p.rationale, c.rationale, sides, conflicts),
                status = status,
                completedAt = statusFrom.completedAt ?: p.completedAt ?: c.completedAt,
                archivedAt = statusFrom.archivedAt ?: p.archivedAt ?: c.archivedAt
            )
        }

    private fun reminderContent(a: ReminderEntity, b: ReminderEntity) =
        a.title == b.title && a.eventTimeMs == b.eventTimeMs && a.localDate == b.localDate &&
            a.localTime == b.localTime && a.person == b.person && a.location == b.location && a.status == b.status

    fun mergeReminders(base: List<ReminderEntity>, phone: List<ReminderEntity>, cloud: List<ReminderEntity>, sides: Sides) =
        mergeById(base, phone, cloud, { it.id }, ::reminderContent, sides) { b, p, c, conflicts ->
            // When the time moved, it moves as one: date, time, zone and the instant together.
            val baseTime = (b ?: p).let { listOf(it.eventTimeMs, it.localDate, it.localTime, it.zoneId) }
            val phoneTime = listOf(p.eventTimeMs, p.localDate, p.localTime, p.zoneId)
            val cloudTime = listOf(c.eventTimeMs, c.localDate, c.localTime, c.zoneId)
            val time = pick("reminder:${p.id}:time", baseTime, phoneTime, cloudTime, sides, conflicts)
            val timeFrom = if (time == phoneTime) p else c
            p.copy(
                title = pick("reminder:${p.id}:title", b?.title ?: p.title, p.title, c.title, sides, conflicts),
                person = pick("reminder:${p.id}:person", b?.person ?: p.person, p.person, c.person, sides, conflicts),
                location = pick("reminder:${p.id}:location", b?.location ?: p.location, p.location, c.location, sides, conflicts),
                eventTimeMs = timeFrom.eventTimeMs,
                localDate = timeFrom.localDate,
                localTime = timeFrom.localTime,
                zoneId = timeFrom.zoneId,
                timePrecision = timeFrom.timePrecision,
                status = mostProgressedReminder(p.status, c.status),
                confirmedAt = p.confirmedAt ?: c.confirmedAt,
                needsReview = p.needsReview && c.needsReview
            )
        }

    // ---------------------------------------------------------------------------------------
    // Notes AI re-processed after a reinstall: their tasks have new ids, so match them by text
    // ---------------------------------------------------------------------------------------

    private fun words(text: String): Set<String> =
        text.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length > 1 }.toSet()

    /** Two task texts are the same task when they share most of their words. */
    fun sameText(a: String, b: String): Boolean {
        val wa = words(a)
        val wb = words(b)
        if (wa.isEmpty() || wb.isEmpty()) return false
        val shared = wa.intersect(wb).size.toDouble()
        return shared / wa.union(wb).size >= 0.6
    }

    /**
     * The cloud's rows, with any progress the phone made on the same task (matched by kind and
     * text, since a re-run of AI gave it a new id) carried over: a task done before signing in stays done.
     */
    fun carryProgressByText(phone: List<InsightEntity>, cloud: List<InsightEntity>): List<InsightEntity> {
        val progressed = phone.filter { insightRank(it.status) > 0 }.toMutableList()
        return cloud.map { remote ->
            val match = progressed.firstOrNull { it.kind == remote.kind && sameText(it.text, remote.text) }
                ?: return@map remote
            progressed.remove(match)
            if (insightRank(match.status) <= insightRank(remote.status)) return@map remote
            remote.copy(status = match.status, completedAt = match.completedAt, archivedAt = match.archivedAt)
        }
    }
}
