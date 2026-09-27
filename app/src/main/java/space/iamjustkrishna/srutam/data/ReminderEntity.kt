package space.iamjustkrishna.srutam.data

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

object ReminderType {
    const val MEETING = "MEETING"
    const val DEADLINE = "DEADLINE"
    const val REMINDER = "REMINDER"
    const val CALL = "CALL"
    const val MILESTONE = "MILESTONE"
}

object ReminderStatus {
    const val ACTIVE = "ACTIVE"
    const val DISMISSED = "DISMISSED"
    const val COMPLETED = "COMPLETED"
    const val EXPIRED = "EXPIRED"
}

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["recordingId"]),
        Index(value = ["eventTimeMs"]),
        Index(value = ["status"])
    ]
)
data class ReminderEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val recordingId: Long,
    val recordingName: String = "Voice Note",
    val title: String,
    val eventTimeMs: Long?,
    val originalText: String,
    val person: String? = null,
    val location: String? = null,
    val type: String = ReminderType.REMINDER,
    val status: String = ReminderStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "'UNKNOWN'") val timePrecision: String = "UNKNOWN",
    val localDate: String? = null,
    val localTime: String? = null,
    val zoneId: String? = null,
    @ColumnInfo(defaultValue = "1") val zoneInferred: Boolean = true,
    @ColumnInfo(defaultValue = "0") val notificationEnabled: Boolean = false,
    val confirmedAt: Long? = null,
    @ColumnInfo(defaultValue = "1") val needsReview: Boolean = true,
    @ColumnInfo(defaultValue = "0") val advanceNotification: Boolean = false,
    @ColumnInfo(defaultValue = "0") val scheduleRevision: Int = 0,
    @ColumnInfo(defaultValue = "0") val legacyReview: Boolean = false,
    val extractionFingerprint: String? = null,
    val linkedTaskId: String? = null,
    val scheduleError: String? = null
)
