package space.iamjustkrishna.srutam.ai.copilot

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.UUID

data class GlobalChatMessage(
    val text: String,
    val isUser: Boolean,
    val citedNotes: List<Pair<Long, String>> = emptyList(),
    val isSystem: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val proposal: ToolProposal? = null
)

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New chat",
    val updatedAt: Long = System.currentTimeMillis(),
    val messages: List<GlobalChatMessage> = emptyList()
)

/** Keeps the most recent [maxSessions] chats in one small JSON file. */
class CopilotChatStore(private val file: File, private val maxSessions: Int = 3) {
    private val gson = Gson()
    private val type = object : TypeToken<List<ChatSession>>() {}.type
    private val lock = Any()

    fun load(): List<ChatSession> = synchronized(lock) { read() }

    fun save(session: ChatSession): List<ChatSession> = synchronized(lock) {
        val updated = (listOf(session) + read().filter { it.id != session.id })
            .sortedByDescending { it.updatedAt }
            .take(maxSessions)
        write(updated)
        updated
    }

    fun delete(id: String): List<ChatSession> = synchronized(lock) {
        read().filter { it.id != id }.also { write(it) }
    }

    private fun read(): List<ChatSession> = runCatching {
        if (!file.exists()) emptyList() else gson.fromJson<List<ChatSession>>(file.readText(), type) ?: emptyList()
    }.getOrDefault(emptyList())

    private fun write(sessions: List<ChatSession>) {
        runCatching {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(gson.toJson(sessions))
            if (!tmp.renameTo(file)) { file.writeText(tmp.readText()); tmp.delete() }
        }
    }

    companion object {
        fun titleFor(messages: List<GlobalChatMessage>): String =
            messages.firstOrNull { it.isUser }?.text?.trim()?.replace(Regex("\\s+"), " ")?.take(48)?.ifBlank { null } ?: "New chat"
    }
}
