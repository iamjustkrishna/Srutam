package space.iamjustkrishna.srutam.ai.copilot

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import space.iamjustkrishna.srutam.data.*
import space.iamjustkrishna.srutam.repository.InsightsRepository
import space.iamjustkrishna.srutam.service.ReminderAlarmService
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class CopilotToolExecutorTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: InsightsRepository
    private lateinit var executor: CopilotToolExecutor
    private val now = 1_800_000_000_000L // 2027-01-15 08:00 UTC
    private val clock = Clock.fixed(Instant.ofEpochMilli(now), ZoneOffset.UTC)
    private val scheduled = mutableListOf<ReminderEntity>()
    private val alarms = object : ReminderAlarmService {
        override fun schedule(reminder: ReminderEntity): String? { scheduled += reminder; return null }
        override fun cancel(reminder: ReminderEntity) {}
    }
    private val titles = mutableMapOf(1L to "Product roadmap")
    private val notes = object : NotesGateway {
        override suspend fun search(query: String, limit: Int) = emptyList<NoteRef>()
        override suspend fun inRange(fromMs: Long, toMs: Long, limit: Int) = emptyList<NoteRef>()
        override suspend fun titleOf(id: Long) = titles[id]
        override suspend fun rename(id: Long, newName: String): Boolean { titles[id] = newName; return true }
    }

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = InsightsRepository(db, alarms, clock)
        executor = CopilotToolExecutor(repo, notes, ZoneOffset.UTC, clock)
    }

    @After fun close() { db.close() }

    private suspend fun run(tool: String, vararg args: Pair<String, String>): ToolProposal {
        val proposal = executor.prepare(tool, args.toMap()).getOrThrow()
        assertEquals(ProposalStatus.PENDING, proposal.status)
        val applied = executor.apply(proposal)
        assertEquals(applied.resultNote, ProposalStatus.APPLIED, applied.status)
        return applied
    }

    private suspend fun onlyInsight() = db.insightDao().getAllInsightsFlow().first().single()

    @Test fun reminderWithoutANoteIsSavedAndScheduled() = runBlocking {
        run("create_reminder", "title" to "Call Rahul", "date" to "2027-01-16", "time" to "09:00")
        val saved = db.reminderDao().getAll().single()
        assertEquals(SourceIds.CHAT, saved.recordingId)
        assertTrue(saved.notificationEnabled)
        assertFalse(saved.needsReview)
        assertEquals(1, scheduled.size)
    }

    @Test fun chatReminderIsScheduledAgainAfterReconcile() = runBlocking {
        run("create_reminder", "title" to "Call Rahul", "date" to "2027-01-16", "time" to "09:00")
        scheduled.clear()
        repo.reconcile()
        assertEquals("Call Rahul", scheduled.single().title)
    }

    @Test fun pastTimesAreRejected() = runBlocking {
        val result = executor.prepare("create_reminder", mapOf("title" to "Old", "date" to "2027-01-14", "time" to "09:00"))
        assertTrue(result.isFailure)
        assertTrue(db.reminderDao().getAll().isEmpty())
    }

    @Test fun targetDateHasNoAlarm() = runBlocking {
        run("create_reminder", "title" to "Launch", "date" to "2027-02-01", "kind" to "target_date")
        val saved = db.reminderDao().getAll().single()
        assertFalse(saved.notificationEnabled)
        assertEquals(ReminderType.MILESTONE, saved.type)
        assertTrue(scheduled.isEmpty())
    }

    @Test fun undoRemovesACreatedReminder() = runBlocking {
        val applied = run("create_reminder", "title" to "Call Rahul", "date" to "2027-01-16", "time" to "09:00")
        val undone = executor.undo(applied)
        assertEquals(ProposalStatus.UNDONE, undone.status)
        assertTrue(db.reminderDao().getAll().isEmpty())
    }

    @Test fun completeAndUndoReopensTheReminder() = runBlocking {
        run("create_reminder", "title" to "Call Rahul", "date" to "2027-01-16", "time" to "09:00")
        val id = db.reminderDao().getAll().single().id
        val applied = run("complete_reminder", "id" to id)
        assertEquals(ReminderStatus.COMPLETED, db.reminderDao().getReminderById(id)!!.status)
        executor.undo(applied)
        val reopened = db.reminderDao().getReminderById(id)!!
        assertEquals(ReminderStatus.ACTIVE, reopened.status)
        assertTrue(reopened.notificationEnabled)
    }

    @Test fun rescheduleMovesTheAlarmAndUndoRestoresIt() = runBlocking {
        run("create_reminder", "title" to "Call Rahul", "date" to "2027-01-16", "time" to "09:00")
        val original = db.reminderDao().getAll().single()
        val applied = run("reschedule_reminder", "id" to original.id, "date" to "2027-01-18", "time" to "10:30")
        assertNotEquals(original.eventTimeMs, db.reminderDao().getReminderById(original.id)!!.eventTimeMs)
        executor.undo(applied)
        assertEquals(original.eventTimeMs, db.reminderDao().getReminderById(original.id)!!.eventTimeMs)
    }

    @Test fun nextStepLifecycle() = runBlocking {
        val created = run("create_next_step", "text" to "Send deck")
        val task = onlyInsight()
        assertEquals(SourceIds.CHAT, task.recordingId)
        val done = run("complete_next_step", "id" to task.id)
        assertEquals(InsightStatus.COMPLETED, db.insightDao().getById(task.id)!!.status)
        executor.undo(done)
        assertEquals(InsightStatus.OPEN, db.insightDao().getById(task.id)!!.status)
        val edited = run("edit_next_step", "id" to task.id, "text" to "Send the deck")
        assertEquals("Send the deck", db.insightDao().getById(task.id)!!.text)
        executor.undo(edited)
        assertEquals("Send deck", db.insightDao().getById(task.id)!!.text)
        val archived = run("archive_next_step", "id" to task.id)
        assertEquals(InsightStatus.ARCHIVED, db.insightDao().getById(task.id)!!.status)
        executor.undo(archived)
        assertEquals(InsightStatus.OPEN, db.insightDao().getById(task.id)!!.status)
        executor.undo(created)
        assertNull(db.insightDao().getById(task.id))
    }

    @Test fun promoteIdeaCreatesLinkedNextStep() = runBlocking {
        run("add_idea", "text" to "Offline mode")
        val idea = onlyInsight()
        assertEquals(InsightKind.IDEA, idea.kind)
        run("promote_idea_to_next_step", "id" to idea.id)
        assertEquals(idea.id, db.insightDao().getDerivedTask(idea.id)!!.sourceInsightId)
    }

    @Test fun renameNoteAndUndo() = runBlocking {
        val applied = run("rename_note", "id" to "1", "name" to "Kitchen plan")
        assertEquals("Kitchen plan", titles[1L])
        executor.undo(applied)
        assertEquals("Product roadmap", titles[1L])
    }

    @Test fun agentRunsReadToolsThenReturnsAWriteProposal() = runBlocking {
        run("create_reminder", "title" to "Call Rahul", "date" to "2027-01-16", "time" to "09:00")
        val id = db.reminderDao().getAll().single().id
        val prompts = mutableListOf<String>()
        val replies = ArrayDeque(listOf(
            """{"type":"tool","name":"list_reminders","args":{"scope":"upcoming"}}""",
            """{"type":"tool","name":"dismiss_reminder","args":{"id":"$id"},"text":"I can dismiss it."}"""
        ))
        val agent = CopilotAgent({ prompt -> prompts += prompt; replies.removeFirst() }, executor, ZoneOffset.UTC, clock)
        val result = agent.run(emptyList(), "dismiss my call reminder", emptyList(), emptyList())
        assertEquals("dismiss_reminder", result.proposal!!.tool)
        assertEquals("I can dismiss it.", result.text)
        assertTrue(prompts[1].contains("Call Rahul"))
        // Nothing is written until the user confirms.
        assertEquals(ReminderStatus.ACTIVE, db.reminderDao().getReminderById(id)!!.status)
    }

    @Test fun agentStillAnswersWhenTheModelKeepsCallingTools() = runBlocking {
        val agent = CopilotAgent({ prompt ->
            if (prompt.contains("Do not call tools")) "You talked about the kitchen leak and the roadmap."
            else """{"type":"tool","name":"find_notes","args":{"query":"recent"}}"""
        }, executor, ZoneOffset.UTC, clock)
        val result = agent.run(emptyList(), "what did I talk about recently?", listOf("Recent note: Kitchen"), emptyList())
        assertNull(result.proposal)
        assertEquals("You talked about the kitchen leak and the roadmap.", result.text)
    }

    @Test fun agentAnswersDirectlyFromRecentNotes() = runBlocking {
        val prompts = mutableListOf<String>()
        val agent = CopilotAgent({ prompt -> prompts += prompt; """{"type":"answer","text":"You discussed the kitchen."}""" }, executor, ZoneOffset.UTC, clock)
        val result = agent.run(emptyList(), "what did I talk about recently?", listOf("Recent note: Kitchen (Sep 28)"), emptyList())
        assertEquals("You discussed the kitchen.", result.text)
        assertEquals(1, prompts.size)
        assertTrue(prompts.single().contains("Recent note: Kitchen"))
    }

    @Test fun liveStateFollowsChangesMadeElsewhere() = runBlocking {
        val created = run("create_reminder", "title" to "Call Rahul", "date" to "2027-01-16", "time" to "09:00")
        assertNull(executor.liveState(created))
        val id = db.reminderDao().getAll().single().id
        repo.setReminderStatus(id, ReminderStatus.COMPLETED)
        val live = executor.liveState(created)!!
        assertEquals("Completed", live.label)
        assertFalse(live.canUndo)
    }

    @Test fun liveStateNoticesRemovedNextStep() = runBlocking {
        val created = run("create_next_step", "text" to "Send deck")
        val id = onlyInsight().id
        assertNull(executor.liveState(created))
        repo.toggleTask(id)
        assertEquals("Completed", executor.liveState(created)!!.label)
        repo.deleteTask(id)
        assertEquals("Removed", executor.liveState(created)!!.label)
    }

    @Test fun unknownIdsAndToolsFailValidation() = runBlocking {
        assertTrue(executor.prepare("complete_reminder", mapOf("id" to "nope")).isFailure)
        assertTrue(executor.prepare("edit_next_step", mapOf("id" to "nope", "text" to "x")).isFailure)
        assertTrue(executor.prepare("drop_database", emptyMap()).isFailure)
        assertTrue(executor.prepare("create_next_step", mapOf("text" to " ")).isFailure)
    }
}
