package space.iamjustkrishna.srutam.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

object ReminderType {
    const val MEETING = "MEETING"
    const val DEADLINE = "DEADLINE"
    const val REMINDER = "REMINDER"
    const val CALL = "CALL"
}

object ReminderStatus {
    const val ACTIVE = "ACTIVE"
    const val DISMISSED = "DISMISSED"
    const val COMPLETED = "COMPLETED"
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
    val eventTimeMs: Long,
    val originalText: String,
    val person: String? = null,
    val location: String? = null,
    val type: String = ReminderType.REMINDER,
    val status: String = ReminderStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis()
)
