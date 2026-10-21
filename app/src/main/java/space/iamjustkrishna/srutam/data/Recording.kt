package space.iamjustkrishna.srutam.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.UUID

@Entity(
    tableName = "recordings",
    indices = [
        Index(value = ["noteUuid"], unique = true),
        Index(value = ["audioHash"])
    ]
)
data class Recording(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val audioFilePath: String,
    val duration: Long = 0, // in milliseconds
    val name: String = "Recording", // User-defined name
    val transcript: String? = null,
    val summary: String? = null,
    val keyPoints: String? = null, // JSON array
    val actionItems: String? = null, // JSON array
    val wiifm: String? = null, // What's In It For Me
    val aiStatus: String = RecordingAiStatus.NOT_REQUESTED,
    val isProcessing: Boolean = false,
    val processingError: String? = null,
    val syncStatus: String = SyncStatus.NOT_SYNCED,
    val isPrivate: Boolean = false,
    val cloudId: String? = null,
    val lastSyncedAt: Long? = null,
    val recordedZoneId: String? = java.time.ZoneId.systemDefault().id,
    @ColumnInfo(defaultValue = "0") val insightsImported: Boolean = false,
    /** Stable id for this note across reinstalls and devices; the local [id] restarts after a reinstall. */
    @ColumnInfo(defaultValue = "''") val noteUuid: String = UUID.randomUUID().toString(),
    /** SHA-256 of the audio, not counting the note block Srutam appends to the file. Filled in lazily. */
    val audioHash: String? = null,
    /** 2-4 topics the AI tagged this note with, as a JSON array. */
    val topics: String? = null,
    /** When the note's content last changed; decides merges between the file block and the cloud. */
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = System.currentTimeMillis(),
    /** Where the current content came from: [NoteSource]. */
    @ColumnInfo(defaultValue = "'AI'") val contentSource: String = NoteSource.AI
)

/** Where a note's transcript and insights came from. */
object NoteSource {
    /** Processed on this install. */
    const val AI = "AI"
    /** Restored from the note block inside the audio file. */
    const val FILE = "FILE"
    /** Restored from Srutam Cloud. */
    const val CLOUD = "CLOUD"
}
