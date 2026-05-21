package space.iamjustkrishna.srutam.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
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
    val processingError: String? = null
)
