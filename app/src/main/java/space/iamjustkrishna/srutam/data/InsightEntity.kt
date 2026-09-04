package space.iamjustkrishna.srutam.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object InsightKind {
    const val ACTION = "ACTION"
    const val IDEA = "IDEA"
    const val DECISION = "DECISION"
}

object InsightStatus {
    const val OPEN = "OPEN"
    const val COMPLETED = "COMPLETED"
    const val ARCHIVED = "ARCHIVED"
}

@Entity(
    tableName = "insight_items",
    indices = [
        Index(value = ["recordingId"]),
        Index(value = ["kind"]),
        Index(value = ["status"])
    ]
)
data class InsightEntity(
    @PrimaryKey
    val id: String,
    val recordingId: Long,
    val recordingName: String = "Voice Note",
    val kind: String,
    val text: String,
    val evidence: String? = null,
    val rationale: String? = null,
    val status: String = InsightStatus.OPEN,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val archivedAt: Long? = null,
    val sourceOrder: Int = 0
)
