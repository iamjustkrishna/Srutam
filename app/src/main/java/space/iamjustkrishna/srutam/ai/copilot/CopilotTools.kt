package space.iamjustkrishna.srutam.ai.copilot

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.UUID

enum class ToolKind { READ, WRITE }

data class ToolSpec(val name: String, val kind: ToolKind, val description: String, val args: String)

enum class ProposalStatus { PENDING, APPLIED, CANCELLED, FAILED, UNDONE }

/** Remembers how to reverse an applied proposal. All values are strings so the chat can be saved as JSON. */
data class UndoInfo(val action: String, val args: Map<String, String> = emptyMap())

/** A change the AI wants to make. Nothing is written until the user confirms it. */
data class ToolProposal(
    val id: String = UUID.randomUUID().toString(),
    val tool: String,
    val args: Map<String, String>,
    val summary: String,
    val status: ProposalStatus = ProposalStatus.PENDING,
    val resultNote: String? = null,
    val undo: UndoInfo? = null
)

sealed interface AgentReply {
    data class Answer(val text: String) : AgentReply
    data class Tool(val name: String, val args: Map<String, String>, val preface: String?) : AgentReply
}

object CopilotTools {
    val all: List<ToolSpec> = listOf(
        // Read tools run immediately and their results go back to the model.
        ToolSpec("find_notes", ToolKind.READ, "Search voice notes by topic.", "query"),
        ToolSpec("summarize_range", ToolKind.READ, "Titles and summaries of notes recorded between two dates.", "from (yyyy-MM-dd), to (yyyy-MM-dd)"),
        ToolSpec("list_reminders", ToolKind.READ, "List reminders and target dates with their ids.", "scope: upcoming | overdue | all"),
        ToolSpec("list_next_steps", ToolKind.READ, "List next steps with their ids.", "status: open | completed"),
        ToolSpec("list_insights", ToolKind.READ, "List saved ideas or decisions with their ids.", "kind: idea | decision"),
        // Write tools are shown to the user as a confirmation card first.
        ToolSpec("create_reminder", ToolKind.WRITE, "Create a reminder (alerts at the time) or a target date (no alert).",
            "title, date (yyyy-MM-dd), time (HH:mm, optional), kind: reminder | target_date"),
        ToolSpec("reschedule_reminder", ToolKind.WRITE, "Move an existing reminder.", "id, date (yyyy-MM-dd), time (HH:mm)"),
        ToolSpec("complete_reminder", ToolKind.WRITE, "Mark a reminder done.", "id"),
        ToolSpec("dismiss_reminder", ToolKind.WRITE, "Dismiss a reminder.", "id"),
        ToolSpec("create_next_step", ToolKind.WRITE, "Add a next step (task).", "text"),
        ToolSpec("complete_next_step", ToolKind.WRITE, "Mark a next step done.", "id"),
        ToolSpec("edit_next_step", ToolKind.WRITE, "Change the text of a next step.", "id, text"),
        ToolSpec("archive_next_step", ToolKind.WRITE, "Archive a next step.", "id"),
        ToolSpec("add_idea", ToolKind.WRITE, "Save an idea.", "text"),
        ToolSpec("add_decision", ToolKind.WRITE, "Save a decision.", "text, rationale (optional)"),
        ToolSpec("edit_insight", ToolKind.WRITE, "Change the text of an idea or decision.", "id, text"),
        ToolSpec("promote_idea_to_next_step", ToolKind.WRITE, "Turn an idea into a next step.", "id"),
        ToolSpec("rename_note", ToolKind.WRITE, "Rename a voice note.", "id, name")
    )

    private val byName = all.associateBy { it.name }

    fun spec(name: String): ToolSpec? = byName[name]

    fun catalog(): String = all.joinToString("\n") { "- ${it.name}(${it.args}) [${it.kind.name.lowercase()}]: ${it.description}" }

    /**
     * Reads the model output. Anything that is not a valid tool call is treated as a plain answer,
     * so a badly formatted reply never breaks the chat.
     */
    fun parse(raw: String): AgentReply {
        val json = extractJson(raw) ?: return AgentReply.Answer(raw.trim())
        val type = json.str("type")?.lowercase()
        val toolName = json.str("tool") ?: json.str("name")
        if ((type == "tool" || type == null) && toolName != null && spec(toolName) != null) {
            val args = mutableMapOf<String, String>()
            (json.get("args") as? JsonObject)?.entrySet()?.forEach { (k, v) ->
                scalar(v)?.let { args[k] = it }
            }
            return AgentReply.Tool(toolName, args, json.str("text")?.takeIf { it.isNotBlank() })
        }
        val text = json.str("text") ?: json.str("answer")
        return AgentReply.Answer(text?.trim().takeUnless { it.isNullOrBlank() } ?: raw.trim())
    }

    private fun JsonObject.str(key: String): String? = get(key)?.let { scalar(it) }

    private fun scalar(e: JsonElement): String? =
        if (e.isJsonPrimitive) e.asJsonPrimitive.asString else null

    private fun extractJson(raw: String): JsonObject? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { JsonParser.parseString(raw.substring(start, end + 1)) }
            .getOrNull()?.takeIf { it.isJsonObject }?.asJsonObject
    }
}
