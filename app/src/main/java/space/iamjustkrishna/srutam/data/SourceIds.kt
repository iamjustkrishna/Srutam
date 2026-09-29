package space.iamjustkrishna.srutam.data

/** Reserved recording ids for items that do not come from a voice note. */
object SourceIds {
    /** Reminders and insights created by the user through Srutam AI chat. */
    const val CHAT = 0L

    fun isChat(recordingId: Long) = recordingId == CHAT
}
