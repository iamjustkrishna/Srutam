package space.iamjustkrishna.srutam.ai.copilot

import space.iamjustkrishna.srutam.ai.ReminderTimeResolver
import space.iamjustkrishna.srutam.ai.ResolvedReminderTime
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.InsightKind
import space.iamjustkrishna.srutam.data.InsightStatus
import space.iamjustkrishna.srutam.data.ReminderEntity
import space.iamjustkrishna.srutam.data.ReminderStatus
import space.iamjustkrishna.srutam.data.ReminderType
import space.iamjustkrishna.srutam.repository.InsightsRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class NoteRef(val id: Long, val title: String, val timestamp: Long, val summary: String)

/** The part of the notes library the AI may touch. Implemented by the view model. */
interface NotesGateway {
    suspend fun search(query: String, limit: Int): List<NoteRef>
    suspend fun inRange(fromMs: Long, toMs: Long, limit: Int): List<NoteRef>
    suspend fun titleOf(id: Long): String?
    suspend fun rename(id: Long, newName: String): Boolean
}

/** What the chat should show for an applied change once the item has been changed elsewhere. */
data class LiveState(val label: String, val canUndo: Boolean = false)

data class ReadResult(val text: String, val cited: List<Pair<Long, String>> = emptyList())

class CopilotToolExecutor(
    private val repo: InsightsRepository,
    private val notes: NotesGateway,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val clock: Clock = Clock.systemDefaultZone()
) {
    private val whenFormat = DateTimeFormatter.ofPattern("EEE, MMM d 'at' h:mm a", Locale.getDefault())
    private val dateFormat = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

    // ---------------------------------------------------------------- read tools

    suspend fun read(name: String, args: Map<String, String>): ReadResult = when (name) {
        "find_notes" -> {
            val found = notes.search(args["query"].orEmpty(), 5)
            ReadResult(
                if (found.isEmpty()) "No matching notes." else found.joinToString("\n") { "id=${it.id} | ${it.title} | ${day(it.timestamp)}" },
                found.map { it.id to it.title }
            )
        }
        "summarize_range" -> {
            val from = parseDate(args["from"]) ?: return ReadResult("Invalid 'from' date.")
            val to = parseDate(args["to"]) ?: from
            val found = notes.inRange(
                from.atStartOfDay(zone).toInstant().toEpochMilli(),
                to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), 8
            )
            ReadResult(
                if (found.isEmpty()) "No notes in that range." else found.joinToString("\n\n") {
                    "${it.title} (${day(it.timestamp)}): ${it.summary.take(500)}"
                },
                found.map { it.id to it.title }
            )
        }
        "list_reminders" -> {
            val now = clock.millis()
            val scope = args["scope"]?.lowercase() ?: "upcoming"
            val rows = repo.allReminders().filter { it.status == ReminderStatus.ACTIVE && !it.needsReview }.filter {
                val at = sortTime(it)
                when (scope) {
                    "overdue" -> at < now
                    "all" -> true
                    else -> at >= now
                }
            }.sortedBy { sortTime(it) }.take(15)
            ReadResult(if (rows.isEmpty()) "No reminders." else rows.joinToString("\n") {
                "id=${it.id} | ${it.title} | ${describeTime(it)} | ${if (it.type == ReminderType.MILESTONE || !it.notificationEnabled) "target date" else "reminder"}"
            })
        }
        "list_next_steps" -> {
            val status = if (args["status"]?.lowercase() == "completed") InsightStatus.COMPLETED else InsightStatus.OPEN
            val rows = repo.allInsights().filter { it.kind == InsightKind.ACTION && it.status == status }.take(15)
            ReadResult(if (rows.isEmpty()) "No next steps." else rows.joinToString("\n") { "id=${it.id} | ${it.text}" })
        }
        "list_insights" -> {
            val kind = if (args["kind"]?.lowercase() == "decision") InsightKind.DECISION else InsightKind.IDEA
            val rows = repo.allInsights().filter { it.kind == kind && it.status != InsightStatus.ARCHIVED }.take(15)
            ReadResult(if (rows.isEmpty()) "None saved." else rows.joinToString("\n") { "id=${it.id} | ${it.text}" })
        }
        else -> ReadResult("Unknown tool.")
    }

    // ---------------------------------------------------------------- write tools: validate and describe

    /** Returns a proposal to show the user, or an error message the model can react to. */
    suspend fun prepare(name: String, args: Map<String, String>): Result<ToolProposal> = runCatching {
        fun proposal(summary: String, a: Map<String, String> = args) = ToolProposal(tool = name, args = a, summary = summary)
        when (name) {
            "create_reminder" -> {
                val title = args.required("title")
                val target = args["kind"]?.lowercase() == "target_date"
                val date = parseDate(args["date"]) ?: error("Give the date as yyyy-MM-dd.")
                if (target) {
                    proposal("Add target date ${date.format(dateFormat)}: $title",
                        mapOf("title" to title, "date" to date.toString(), "kind" to "target_date"))
                } else {
                    val time = parseTime(args["time"]) ?: DEFAULT_TIME
                    val resolved = ReminderTimeResolver.resolveLocal(date, time, zone)
                    check(ReminderTimeResolver.isFuture(resolved, clock)) { "That time is not in the future." }
                    val note = if (parseTime(args["time"]) == null) " (default time)" else ""
                    proposal("Remind you ${describe(resolved)}$note: $title",
                        mapOf("title" to title, "date" to date.toString(), "time" to time.toString(), "kind" to "reminder"))
                }
            }
            "reschedule_reminder" -> {
                val item = reminder(args)
                val date = parseDate(args["date"]) ?: error("Give the new date as yyyy-MM-dd.")
                val time = parseTime(args["time"]) ?: item.localTime?.let(::parseTime) ?: DEFAULT_TIME
                val resolved = ReminderTimeResolver.resolveLocal(date, time, zone)
                check(ReminderTimeResolver.isFuture(resolved, clock)) { "That time is not in the future." }
                proposal("Move \"${item.title}\" from ${describeTime(item)} to ${describe(resolved)}",
                    mapOf("id" to item.id, "date" to date.toString(), "time" to time.toString()))
            }
            "complete_reminder" -> reminder(args).let { proposal("Mark done: \"${it.title}\"") }
            "dismiss_reminder" -> reminder(args).let { proposal("Dismiss: \"${it.title}\"") }
            "create_next_step" -> args.required("text").let { proposal("Add next step: \"$it\"", mapOf("text" to it)) }
            "complete_next_step" -> insight(args, InsightKind.ACTION).let { proposal("Mark next step done: \"${it.text}\"") }
            "edit_next_step" -> insight(args, InsightKind.ACTION).let {
                proposal("Change next step\n\"${it.text}\" to\n\"${args.required("text")}\"")
            }
            "archive_next_step" -> insight(args, InsightKind.ACTION).let { proposal("Archive next step: \"${it.text}\"") }
            "add_idea" -> args.required("text").let { proposal("Save idea: \"$it\"", mapOf("text" to it)) }
            "add_decision" -> args.required("text").let {
                proposal("Save decision: \"$it\"", mapOf("text" to it, "rationale" to args["rationale"].orEmpty()))
            }
            "edit_insight" -> repo.getInsight(args.required("id")).let { item ->
                check(item != null && item.kind != InsightKind.ACTION) { "No idea or decision with that id." }
                proposal("Change ${item.kind.lowercase()}\n\"${item.text}\" to\n\"${args.required("text")}\"")
            }
            "promote_idea_to_next_step" -> insight(args, InsightKind.IDEA).let { proposal("Turn idea into a next step: \"${it.text}\"") }
            "rename_note" -> {
                val id = args.required("id").toLongOrNull() ?: error("Invalid note id.")
                val old = notes.titleOf(id) ?: error("No note with that id.")
                proposal("Rename note \"$old\" to \"${args.required("name")}\"", mapOf("id" to id.toString(), "name" to args.required("name")))
            }
            else -> error("Unknown tool.")
        }
    }

    // ---------------------------------------------------------------- write tools: apply and undo

    suspend fun apply(p: ToolProposal): ToolProposal = try {
        val a = p.args
        var note: String? = null
        val undo: UndoInfo = when (p.tool) {
            "create_reminder" -> {
                val date = LocalDate.parse(a.getValue("date"))
                val target = a["kind"] == "target_date"
                val resolved = if (target) ResolvedReminderTime(localDate = date.toString(), zoneId = zone.id, precision = "DATE_ONLY")
                else ReminderTimeResolver.resolveLocal(date, LocalTime.parse(a.getValue("time")), zone)
                val saved = repo.createChatReminder(
                    a.getValue("title"), if (target) ReminderType.MILESTONE else ReminderType.REMINDER, resolved, notify = !target
                )
                note = saved.scheduleError
                UndoInfo("delete_reminder", mapOf("id" to saved.id))
            }
            "reschedule_reminder" -> {
                val item = repo.getReminder(a.getValue("id")) ?: error("Reminder no longer exists.")
                val resolved = ReminderTimeResolver.resolveLocal(LocalDate.parse(a.getValue("date")), LocalTime.parse(a.getValue("time")), zone)
                val prev = mapOf(
                    "id" to item.id, "eventTimeMs" to (item.eventTimeMs?.toString() ?: ""), "precision" to item.timePrecision,
                    "date" to item.localDate.orEmpty(), "time" to item.localTime.orEmpty(), "zone" to item.zoneId.orEmpty(),
                    "notify" to item.notificationEnabled.toString()
                )
                repo.saveReminder(item.withTime(resolved, item.notificationEnabled), true)
                UndoInfo("restore_reminder_time", prev)
            }
            "complete_reminder" -> { repo.setReminderStatus(a.getValue("id"), ReminderStatus.COMPLETED); UndoInfo("reopen_reminder", mapOf("id" to a.getValue("id"))) }
            "dismiss_reminder" -> { repo.setReminderStatus(a.getValue("id"), ReminderStatus.DISMISSED); UndoInfo("reopen_reminder", mapOf("id" to a.getValue("id"))) }
            "create_next_step" -> UndoInfo("delete_task", mapOf("id" to repo.createChatInsight(InsightKind.ACTION, a.getValue("text"))))
            "add_idea" -> UndoInfo("delete_task", mapOf("id" to repo.createChatInsight(InsightKind.IDEA, a.getValue("text"))))
            "add_decision" -> UndoInfo("delete_task", mapOf("id" to repo.createChatInsight(InsightKind.DECISION, a.getValue("text"), a["rationale"])))
            "complete_next_step" -> { repo.toggleTask(a.getValue("id")); UndoInfo("toggle_task", mapOf("id" to a.getValue("id"))) }
            "edit_next_step", "edit_insight" -> {
                val item = repo.getInsight(a.getValue("id")) ?: error("Item no longer exists.")
                repo.editInsightText(item.id, a.getValue("text"))
                UndoInfo("edit_text", mapOf("id" to item.id, "text" to item.text))
            }
            "archive_next_step" -> {
                val item = repo.getInsight(a.getValue("id")) ?: error("Item no longer exists.")
                repo.archiveTask(item.id)
                UndoInfo("set_status", mapOf("id" to item.id, "status" to item.status))
            }
            "promote_idea_to_next_step" -> {
                val idea = repo.getInsight(a.getValue("id")) ?: error("Idea no longer exists.")
                UndoInfo("delete_task", mapOf("id" to repo.createTask(idea.id, false, idea.text)))
            }
            "rename_note" -> {
                val id = a.getValue("id").toLong()
                val old = notes.titleOf(id) ?: error("Note no longer exists.")
                check(notes.rename(id, a.getValue("name"))) { "Could not rename the note." }
                UndoInfo("rename_note", mapOf("id" to id.toString(), "name" to old))
            }
            else -> error("Unknown tool.")
        }
        p.copy(status = ProposalStatus.APPLIED, undo = undo, resultNote = note)
    } catch (e: Exception) {
        p.copy(status = ProposalStatus.FAILED, resultNote = e.message ?: "Something went wrong.")
    }

    suspend fun undo(p: ToolProposal): ToolProposal {
        val u = p.undo ?: return p
        return try {
            val a = u.args
            when (u.action) {
                "delete_reminder" -> repo.deleteReminder(a.getValue("id"))
                "reopen_reminder" -> repo.undoReminderStatus(a.getValue("id"))
                "restore_reminder_time" -> {
                    val item = repo.getReminder(a.getValue("id")) ?: error("Reminder no longer exists.")
                    val prev = ResolvedReminderTime(
                        a["eventTimeMs"]?.toLongOrNull(), a["date"]?.ifBlank { null }, a["time"]?.ifBlank { null },
                        a["zone"].orEmpty().ifBlank { zone.id }, a["precision"].orEmpty().ifBlank { "UNKNOWN" }
                    )
                    repo.saveReminder(item.withTime(prev, a["notify"] == "true"), true)
                }
                "delete_task" -> repo.deleteTask(a.getValue("id"))
                "toggle_task" -> repo.toggleTask(a.getValue("id"))
                "edit_text" -> repo.editInsightText(a.getValue("id"), a.getValue("text"))
                "set_status" -> repo.setInsightStatus(a.getValue("id"), a.getValue("status"))
                "rename_note" -> check(notes.rename(a.getValue("id").toLong(), a.getValue("name"))) { "Could not rename the note." }
            }
            p.copy(status = ProposalStatus.UNDONE, undo = null, resultNote = null)
        } catch (e: Exception) {
            p.copy(resultNote = "Could not undo: ${e.message}")
        }
    }

    /** Null means the change is still exactly as applied, so the card keeps its Done + Undo look. */
    suspend fun liveState(p: ToolProposal): LiveState? {
        val u = p.undo?.takeIf { p.status == ProposalStatus.APPLIED } ?: return null
        val a = u.args
        return when (u.action) {
            "delete_reminder" -> {
                val r = repo.getReminder(a.getValue("id")) ?: return LiveState("Removed")
                when {
                    r.status == ReminderStatus.COMPLETED -> LiveState("Completed")
                    r.status == ReminderStatus.DISMISSED -> LiveState("Dismissed")
                    r.status != ReminderStatus.ACTIVE -> LiveState("Closed")
                    !isTargetDate(r) && sortTime(r) < clock.millis() -> LiveState("Time has passed")
                    else -> null
                }
            }
            "reopen_reminder" -> repo.getReminder(a.getValue("id"))?.takeIf { it.status == ReminderStatus.ACTIVE }?.let { LiveState("Reopened") }
            "restore_reminder_time" -> {
                val r = repo.getReminder(a.getValue("id")) ?: return LiveState("Removed")
                when (r.status) {
                    ReminderStatus.COMPLETED -> LiveState("Completed")
                    ReminderStatus.DISMISSED -> LiveState("Dismissed")
                    else -> null
                }
            }
            "delete_task" -> {
                val i = repo.getInsight(a.getValue("id")) ?: return LiveState("Removed")
                when (i.status) {
                    InsightStatus.COMPLETED -> LiveState("Completed")
                    InsightStatus.ARCHIVED -> LiveState("Archived")
                    else -> null
                }
            }
            "toggle_task" -> repo.getInsight(a.getValue("id"))?.let { i ->
                when (i.status) {
                    InsightStatus.OPEN -> LiveState("Reopened")
                    InsightStatus.ARCHIVED -> LiveState("Archived")
                    else -> null
                }
            }
            "set_status" -> repo.getInsight(a.getValue("id"))?.takeIf { it.status != InsightStatus.ARCHIVED }?.let { LiveState("Restored") }
            "edit_text" -> repo.getInsight(a.getValue("id"))?.takeIf { it.text != p.args["text"]?.trim() }?.let { LiveState("Changed since") }
            "rename_note" -> notes.titleOf(a.getValue("id").toLong())?.takeIf { it != p.args["name"]?.trim() }?.let { LiveState("Changed since") }
            else -> null
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun ReminderEntity.withTime(t: ResolvedReminderTime, notify: Boolean) = copy(
        eventTimeMs = t.eventTimeMs, timePrecision = t.precision, localDate = t.localDate, localTime = t.localTime,
        zoneId = t.zoneId, notificationEnabled = notify
    )

    private suspend fun reminder(args: Map<String, String>): ReminderEntity {
        val item = repo.getReminder(args.required("id"))
        check(item != null && item.status == ReminderStatus.ACTIVE) { "No active reminder with that id." }
        return item
    }

    private suspend fun insight(args: Map<String, String>, kind: String): InsightEntity {
        val item = repo.getInsight(args.required("id"))
        check(item != null && item.kind == kind) { "No ${kind.lowercase()} with that id." }
        return item
    }

    private fun Map<String, String>.required(key: String): String =
        this[key]?.trim()?.takeIf { it.isNotEmpty() } ?: error("Missing '$key'.")

    private fun parseDate(s: String?): LocalDate? = s?.trim()?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    private fun parseTime(s: String?): LocalTime? = s?.trim()?.takeIf { it.isNotEmpty() }?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

    private fun day(ms: Long) = Instant.ofEpochMilli(ms).atZone(zone).format(dateFormat)

    private fun describe(t: ResolvedReminderTime): String = t.eventTimeMs
        ?.let { Instant.ofEpochMilli(it).atZone(zone).format(whenFormat) } ?: (t.localDate ?: "date not set")

    private fun describeTime(r: ReminderEntity): String = r.eventTimeMs
        ?.let { Instant.ofEpochMilli(it).atZone(runCatching { ZoneId.of(r.zoneId) }.getOrDefault(zone)).format(whenFormat) }
        ?: (r.localDate?.let { runCatching { LocalDate.parse(it).format(dateFormat) }.getOrNull() } ?: "date not set")

    private fun isTargetDate(r: ReminderEntity) = r.type == ReminderType.MILESTONE || !r.notificationEnabled

    private fun sortTime(r: ReminderEntity): Long = r.eventTimeMs
        ?: r.localDate?.let { runCatching { LocalDate.parse(it).atStartOfDay(zone).toInstant().toEpochMilli() }.getOrNull() }
        ?: Long.MAX_VALUE

    companion object {
        private val DEFAULT_TIME: LocalTime = LocalTime.of(9, 0)
    }
}
