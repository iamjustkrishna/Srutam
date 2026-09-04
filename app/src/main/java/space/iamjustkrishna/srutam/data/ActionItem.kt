package space.iamjustkrishna.srutam.data

data class ActionItem(
    val id: String,
    val recordingId: Long,
    val recordingName: String,
    val text: String,
    val isCompleted: Boolean,
    val timestamp: Long
)
