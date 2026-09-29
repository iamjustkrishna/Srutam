package space.iamjustkrishna.srutam.ai.copilot

import android.util.Log
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class CopilotTurn(val fromUser: Boolean, val text: String)

data class AgentResult(
    val text: String,
    val cited: List<Pair<Long, String>> = emptyList(),
    val proposal: ToolProposal? = null
)

/**
 * Prompt-level tool loop. Read tools run right away and their output is shown to the model again;
 * the first write tool ends the turn as a [ToolProposal] that the user must confirm.
 * If the model keeps calling tools, one last tool-free request still produces an answer.
 */
class CopilotAgent(
    private val llm: suspend (String) -> String,
    private val executor: CopilotToolExecutor,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val clock: Clock = Clock.systemDefaultZone()
) {
    suspend fun run(
        history: List<CopilotTurn>,
        question: String,
        noteSnippets: List<String>,
        retrievedNotes: List<Pair<Long, String>>
    ): AgentResult {
        val cited = retrievedNotes.toMutableList()
        val scratch = StringBuilder()
        val seenCalls = mutableSetOf<String>()
        repeat(MAX_HOPS) { hop ->
            val raw = llm(buildPrompt(history, question, noteSnippets, scratch.toString(), hop == MAX_HOPS - 1))
            val reply = CopilotTools.parse(raw)
            log("hop $hop reply: ${raw.take(300).replace('\n', ' ')}")
            when (reply) {
                is AgentReply.Answer -> return AgentResult(reply.text, cited.distinct().take(MAX_CITED))
                is AgentReply.Tool -> {
                    val spec = CopilotTools.spec(reply.name)!!
                    if (spec.kind == ToolKind.READ) {
                        val call = reply.name + reply.args.toSortedMap()
                        if (!seenCalls.add(call)) {
                            scratch.append("NOTE: you already called ${reply.name} with these arguments. Use the earlier result and answer now.\n\n")
                        } else {
                            val result = executor.read(reply.name, reply.args)
                            cited += result.cited
                            scratch.append("TOOL RESULT ${reply.name}:\n${result.text}\n\n")
                        }
                    } else {
                        val prepared = executor.prepare(reply.name, reply.args)
                        val proposal = prepared.getOrNull()
                        if (proposal != null) {
                            return AgentResult(reply.preface ?: "Here is what I would do. Confirm to apply it.", cited.distinct().take(MAX_CITED), proposal)
                        }
                        scratch.append("TOOL ERROR ${reply.name}: ${prepared.exceptionOrNull()?.message}\n\n")
                    }
                }
            }
        }
        // The model kept calling tools. Ask once more, with no tools, so the user still gets an answer.
        val closing = CopilotTools.parse(llm(buildFinalPrompt(history, question, noteSnippets, scratch.toString())))
        log("forced final answer: ${(closing as? AgentReply.Answer)?.text?.take(200)}")
        val text = (closing as? AgentReply.Answer)?.text?.takeIf { it.isNotBlank() }
            ?: "I could not complete that. Could you rephrase or give me the exact date and details?"
        return AgentResult(text, cited.distinct().take(MAX_CITED))
    }

    private fun log(message: String) {
        // Unit tests run on the JVM where android.util.Log is not implemented.
        runCatching { Log.d(TAG, message) }
    }

    private fun convoText(history: List<CopilotTurn>): String =
        history.takeLast(MAX_HISTORY_TURNS * 2).joinToString("\n") {
            "${if (it.fromUser) "User" else "Srutam AI"}: ${it.text.take(600)}"
        }.ifBlank { "(new conversation)" }

    private fun buildPrompt(
        history: List<CopilotTurn>, question: String, snippets: List<String>, scratch: String, finalHop: Boolean
    ): String {
        val now = ZonedDateTime.now(clock.withZone(zone))
        return """
            You are Srutam AI, the assistant inside the Srutam voice notes app. You answer questions about the user's voice notes and you can manage their reminders, next steps, ideas and decisions with tools.

            Now: ${now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm, EEEE", Locale.ENGLISH))} (timezone ${zone.id}). Resolve words like "tomorrow" or "next Friday" from this.

            TOOLS:
            ${CopilotTools.catalog()}

            RULES:
            1. Reply with exactly ONE JSON object and nothing else.
               Final answer: {"type":"answer","text":"..."}
               Tool call: {"type":"tool","name":"<tool>","args":{...},"text":"<short sentence for the user>"}
            2. Questions about what the user said or recorded are answered from the notes below. Answer directly and do NOT call a tool when the notes below already contain what you need. Recent notes are included, so "recently" questions can be answered from them.
            3. To change anything, call a write tool. Never say you already changed something: the user confirms first.
            4. You need an id from a list tool before you can change an existing item. Call the matching list tool first, then use the id exactly as shown.
            5. If the date, time or target is unclear, ask ONE short question with a final answer instead of guessing.
            6. Stay within the user's voice notes, reminders, next steps, ideas and decisions. Politely refuse anything else.
            7. Cite note titles when using the notes below. If the notes do not contain the answer, say so.
            8. Never mention tools, JSON, ids or search algorithms in the text the user reads.
            ${if (finalHop) "9. This is your last step: give a final answer now." else ""}

            RELEVANT AND RECENT NOTES:
            ${if (snippets.isEmpty()) "(none found)" else snippets.joinToString("\n\n---\n\n")}

            CONVERSATION SO FAR:
            ${convoText(history)}

            ${if (scratch.isBlank()) "" else "TOOL RESULTS SO FAR:\n$scratch"}
            User: $question
        """.trimIndent()
    }

    private fun buildFinalPrompt(history: List<CopilotTurn>, question: String, snippets: List<String>, scratch: String): String = """
        You are Srutam AI, the assistant inside the Srutam voice notes app.
        Answer the user's question in plain, friendly text using only the information below. Do not call tools and do not output JSON.
        If the information is not enough, say what you could not find in their notes.
        Cite note titles where helpful. Keep it short.

        NOTES:
        ${if (snippets.isEmpty()) "(none found)" else snippets.joinToString("\n\n---\n\n")}

        ${if (scratch.isBlank()) "" else "LOOKUP RESULTS:\n$scratch"}
        CONVERSATION SO FAR:
        ${convoText(history)}

        User: $question
    """.trimIndent()

    companion object {
        private const val TAG = "CopilotAgent"
        const val MAX_HOPS = 3
        const val MAX_HISTORY_TURNS = 3
        private const val MAX_CITED = 4
    }
}
