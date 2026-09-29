package space.iamjustkrishna.srutam.ai.copilot

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CopilotToolsTest {
    @Test fun parsesToolCallInsideCodeFence() {
        val reply = CopilotTools.parse("```json\n{\"type\":\"tool\",\"name\":\"create_reminder\",\"args\":{\"title\":\"Call Rahul\",\"date\":\"2027-01-16\",\"time\":\"09:00\"},\"text\":\"Sure\"}\n```")
        reply as AgentReply.Tool
        assertEquals("create_reminder", reply.name)
        assertEquals("Call Rahul", reply.args["title"])
        assertEquals("Sure", reply.preface)
    }

    @Test fun numbersAndBooleansBecomeStrings() {
        val reply = CopilotTools.parse("{\"type\":\"tool\",\"name\":\"rename_note\",\"args\":{\"id\":42,\"name\":\"Plan\"}}") as AgentReply.Tool
        assertEquals("42", reply.args["id"])
    }

    @Test fun answerJsonAndPlainTextBothWork() {
        assertEquals("Hello", (CopilotTools.parse("{\"type\":\"answer\",\"text\":\"Hello\"}") as AgentReply.Answer).text)
        assertEquals("Just text", (CopilotTools.parse("Just text") as AgentReply.Answer).text)
    }

    @Test fun unknownToolAndBrokenJsonFallBackToAnswer() {
        assertTrue(CopilotTools.parse("{\"type\":\"tool\",\"name\":\"format_disk\",\"args\":{}}") is AgentReply.Answer)
        assertTrue(CopilotTools.parse("{\"type\":\"tool\",\"name\":") is AgentReply.Answer)
    }

    @Test fun catalogListsEveryToolOnce() {
        val names = CopilotTools.all.map { it.name }
        assertEquals(names.size, names.toSet().size)
        names.forEach { assertTrue(CopilotTools.catalog().contains(it)) }
    }

    @Test fun chatStoreKeepsOnlyTheThreeNewestSessions() {
        val file = File.createTempFile("chats", ".json").apply { delete() }
        val store = CopilotChatStore(file)
        (1..5).forEach { i ->
            store.save(ChatSession(id = "s$i", title = "Chat $i", updatedAt = i.toLong(),
                messages = listOf(GlobalChatMessage("hi $i", true))))
        }
        assertEquals(listOf("s5", "s4", "s3"), store.load().map { it.id })
        store.save(ChatSession(id = "s3", title = "Chat 3", updatedAt = 99, messages = emptyList()))
        assertEquals(listOf("s3", "s5", "s4"), store.load().map { it.id })
        assertEquals(listOf("s5", "s4"), store.delete("s3").map { it.id })
        file.delete()
    }

    @Test fun chatStoreKeepsProposalStateAcrossRestart() {
        val file = File.createTempFile("chats", ".json").apply { delete() }
        val proposal = ToolProposal(tool = "create_next_step", args = mapOf("text" to "Send deck"), summary = "Add next step",
            status = ProposalStatus.APPLIED, undo = UndoInfo("delete_task", mapOf("id" to "x")))
        CopilotChatStore(file).save(ChatSession(id = "a", messages = listOf(
            GlobalChatMessage("ok", false, citedNotes = listOf(7L to "Kitchen"), proposal = proposal))))
        val loaded = CopilotChatStore(file).load().single().messages.single()
        assertEquals(proposal, loaded.proposal)
        assertEquals(listOf(7L to "Kitchen"), loaded.citedNotes)
        file.delete()
    }

    @Test fun titleUsesTheFirstUserMessage() {
        assertEquals("Remind me tomorrow", CopilotChatStore.titleFor(listOf(
            GlobalChatMessage("Welcome", false), GlobalChatMessage("  Remind   me tomorrow ", true))))
        assertEquals("New chat", CopilotChatStore.titleFor(emptyList()))
    }
}
