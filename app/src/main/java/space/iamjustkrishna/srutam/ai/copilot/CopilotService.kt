package space.iamjustkrishna.srutam.ai.copilot

import android.content.Context
import space.iamjustkrishna.srutam.repository.InsightsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import java.io.File

/** Entry point used by the AI screen: chat storage plus the tool-using agent. */
class CopilotService(
    context: Context,
    notes: NotesGateway,
    llm: suspend (String) -> String,
    private val retrieve: suspend (String) -> Pair<List<String>, List<Pair<Long, String>>>
) {
    val store = CopilotChatStore(File(context.applicationContext.filesDir, "copilot_chats.json"))
    private val executor = CopilotToolExecutor(InsightsRepository.from(context.applicationContext), notes)
    private val agent = CopilotAgent(llm, executor)

    suspend fun ask(history: List<GlobalChatMessage>, question: String): AgentResult {
        val (snippets, cited) = retrieve(question)
        return agent.run(history.toTurns(), question, snippets, cited)
    }

    private val repo = InsightsRepository.from(context.applicationContext)

    /** Emits whenever a reminder or insight changes, anywhere in the app. */
    val changes: Flow<Unit> = merge(repo.reminders, repo.insights).map { }

    suspend fun liveStates(messages: List<GlobalChatMessage>): Map<String, LiveState> =
        messages.mapNotNull { it.proposal }.filter { it.status == ProposalStatus.APPLIED && it.undo != null }
            .mapNotNull { p -> executor.liveState(p)?.let { p.id to it } }.toMap()

    suspend fun apply(proposal: ToolProposal): ToolProposal = executor.apply(proposal)

    suspend fun undo(proposal: ToolProposal): ToolProposal = executor.undo(proposal)

    private fun List<GlobalChatMessage>.toTurns(): List<CopilotTurn> = filterNot { it.isSystem }.map { m ->
        val extra = m.proposal?.let { " [${it.summary}: ${it.status.name.lowercase()}]" }.orEmpty()
        CopilotTurn(m.isUser, m.text + extra)
    }
}
