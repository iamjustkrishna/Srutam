package space.iamjustkrishna.srutam.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.InsightKind
import space.iamjustkrishna.srutam.data.InsightStatus
import space.iamjustkrishna.srutam.data.ReminderEntity
import space.iamjustkrishna.srutam.data.ReminderStatus

/** The git-style merge of a note restored from its recording with its Srutam Cloud copy. */
class ThreeWayMergeTest {

    private val base = RestoreMerge.NoteContent(
        title = "Standup", transcript = "t", summary = "Base summary", keyPoints = "[\"a\"]",
        wiifm = null, topics = null, isPrivate = false
    )

    private fun sides(phoneNewer: Boolean = true, cloudMovedOn: Boolean = true) =
        RestoreMerge.Sides(phoneNewer = phoneNewer, cloudMovedOn = cloudMovedOn, now = 0L)

    @Test fun aChangeOnOnlyOneSideIsKeptFromEachSide() {
        val phone = base.copy(title = "Renamed on phone")
        val cloud = base.copy(summary = "Edited by an agent")

        val merged = RestoreMerge.mergeContent(base, phone, cloud, sides())

        assertEquals("Renamed on phone", merged.content.title)
        assertEquals("Edited by an agent", merged.content.summary)
        assertTrue(merged.conflicts.isEmpty())
        assertTrue(merged.changedOnPhone)
    }

    @Test fun aFieldBothSidesChangedGoesToTheNewerEditAndKeepsTheOther() {
        val phone = base.copy(title = "Phone title")
        val cloud = base.copy(title = "Cloud title")

        val phoneWins = RestoreMerge.mergeContent(base, phone, cloud, sides(phoneNewer = true))
        assertEquals("Phone title", phoneWins.content.title)
        assertEquals("Cloud title", phoneWins.conflicts.single().discarded)

        val cloudWins = RestoreMerge.mergeContent(base, phone, cloud, sides(phoneNewer = false))
        assertEquals("Cloud title", cloudWins.content.title)
        assertEquals("Phone title", cloudWins.conflicts.single().discarded)
    }

    @Test fun aCloudCopyOlderThanTheBaseDoesNotUndoThePhonesLastEdits() {
        // The old install's last edit reached its recording but never the cloud.
        val cloud = base.copy(summary = "Older cloud summary")

        val merged = RestoreMerge.mergeContent(base, base, cloud, sides(cloudMovedOn = false))

        assertEquals("Base summary", merged.content.summary)
    }

    @Test fun nothingChangedLeavesTheNoteAsItWas() {
        val merged = RestoreMerge.mergeContent(base, base, base, sides())
        assertEquals(base, merged.content)
        assertFalse(merged.changedOnPhone)
    }

    private fun task(id: String, text: String = "Send notes", status: String = InsightStatus.OPEN) = InsightEntity(
        id = id, recordingId = 1, kind = InsightKind.ACTION, text = text, status = status, createdAt = 0
    )

    @Test fun tasksMergeByIdWithStatusOnlyMovingForward() {
        val base = listOf(task("a"), task("b"))
        val phone = listOf(task("a", status = InsightStatus.COMPLETED), task("b"))
        val cloud = listOf(task("a"), task("b", text = "Send notes to Priya"))

        val merged = RestoreMerge.mergeInsights(base, phone, cloud, sides()).write.associateBy { it.id }

        assertEquals(InsightStatus.COMPLETED, merged.getValue("a").status)
        assertEquals("Send notes to Priya", merged.getValue("b").text)
    }

    @Test fun anAgentsDoneInTheCloudIsKept() {
        val base = listOf(task("a"))
        val merged = RestoreMerge.mergeInsights(base, base, listOf(task("a", status = InsightStatus.COMPLETED)), sides())
        assertEquals(InsightStatus.COMPLETED, merged.write.single().status)
    }

    @Test fun newRowsOnEitherSideAreKept() {
        val base = listOf(task("a"))
        val phone = base + task("phone-new", "Book a call")
        val cloud = base + task("cloud-new", "Check numbers")

        val ids = RestoreMerge.mergeInsights(base, phone, cloud, sides()).write.map { it.id }.toSet()

        assertEquals(setOf("a", "phone-new", "cloud-new"), ids)
    }

    @Test fun aRowDeletedOnOneSideStaysDeletedUnlessTheOtherSideEditedIt() {
        val base = listOf(task("gone"), task("edited"))
        val phone = listOf(task("gone"), task("edited", text = "Edited on phone"))
        val cloud = emptyList<InsightEntity>()

        val merged = RestoreMerge.mergeInsights(base, phone, cloud, sides())

        assertEquals(listOf("gone"), merged.deleteLocal.map { it.id })
        assertEquals(listOf("edited"), merged.write.map { it.id })
    }

    @Test fun aRowDeletedOnThePhoneIsNotBroughtBackByAnUnchangedCloudCopy() {
        val base = listOf(task("a"))
        val merged = RestoreMerge.mergeInsights(base, emptyList(), base, sides())
        assertTrue(merged.write.isEmpty())
    }

    private fun reminder(id: String, date: String = "2026-10-24", status: String = ReminderStatus.ACTIVE) = ReminderEntity(
        id = id, recordingId = 1, title = "Check installs", eventTimeMs = 100, originalText = "",
        localDate = date, localTime = "09:00", zoneId = "Asia/Kolkata", status = status
    )

    @Test fun aRemindersTimeMovesAsOne() {
        val base = listOf(reminder("r"))
        val cloud = listOf(reminder("r", date = "2026-10-25").copy(eventTimeMs = 200))

        val merged = RestoreMerge.mergeReminders(base, base, cloud, sides()).write.single()

        assertEquals("2026-10-25", merged.localDate)
        assertEquals(200L, merged.eventTimeMs)
    }

    @Test fun reprocessedTasksKeepTheirProgressWhenMatchedByText() {
        val phone = listOf(task("new-id", "Send the release notes to Priya", InsightStatus.COMPLETED))
        val cloud = listOf(task("cloud-id", "Send release notes to Priya"), task("other", "Book a call with design"))

        val carried = RestoreMerge.carryProgressByText(phone, cloud).associateBy { it.id }

        assertEquals(InsightStatus.COMPLETED, carried.getValue("cloud-id").status)
        assertEquals(InsightStatus.OPEN, carried.getValue("other").status)
    }

    @Test fun differentTasksDoNotMatchByText() {
        assertFalse(RestoreMerge.sameText("Call the landlord", "Email the design team"))
        assertTrue(RestoreMerge.sameText("Send the release notes to Priya", "Send release notes to Priya"))
    }
}
