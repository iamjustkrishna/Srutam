package space.iamjustkrishna.srutam.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.InsightKind
import space.iamjustkrishna.srutam.data.InsightStatus
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.ReminderEntity
import space.iamjustkrishna.srutam.data.ReminderStatus
import java.time.ZoneId
import java.time.ZonedDateTime

class NoteShareTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val at = ZonedDateTime.of(2026, 10, 21, 17, 40, 0, 0, zone).toInstant().toEpochMilli()

    private val note = Recording(
        id = 7, audioFilePath = "/music/a.m4a", timestamp = at, name = "Team sync",
        summary = "The new onboarding launches Friday.", keyPoints = "[\"Launch on Friday\"]",
        transcript = "Okay, quick note before the team sync."
    )

    private val insights = listOf(
        InsightEntity(id = "t1", recordingId = 7, kind = InsightKind.ACTION, text = "Send the release notes", sourceOrder = 0),
        InsightEntity(id = "t2", recordingId = 7, kind = InsightKind.ACTION, text = "Book a call", status = InsightStatus.COMPLETED, sourceOrder = 1),
        InsightEntity(id = "t3", recordingId = 7, kind = InsightKind.ACTION, text = "Archived task", status = InsightStatus.ARCHIVED),
        InsightEntity(id = "i1", recordingId = 7, kind = InsightKind.IDEA, text = "Show a sample note"),
        InsightEntity(id = "d1", recordingId = 7, kind = InsightKind.DECISION, text = "Launch Friday", rationale = "Design is ready")
    )

    private val reminders = listOf(
        ReminderEntity(id = "r1", recordingId = 7, title = "Check installs", originalText = "", eventTimeMs = at + 3 * 86_400_000L),
        ReminderEntity(id = "r2", recordingId = 7, title = "Old one", originalText = "", eventTimeMs = at, status = ReminderStatus.DISMISSED)
    )

    private fun text(content: NoteShare.Content, footer: Boolean = true) =
        NoteShare.text(note, insights, reminders, NoteShare.Options(content = content, footer = footer), zone, java.util.Locale.US)

    @Test fun theSummaryHasTheInsightsButNotTheTranscript() {
        val text = text(NoteShare.Content.SUMMARY)

        assertTrue(text.startsWith("*Team sync* — Oct 21, 5:40 PM"))
        assertTrue(text.contains("The new onboarding launches Friday."))
        assertTrue(text.contains("• Launch on Friday"))
        assertTrue(text.contains("☐ Send the release notes"))
        assertTrue(text.contains("☑ Book a call"))
        assertFalse(text.contains("Archived task"))
        assertTrue(text.contains("💡 Show a sample note"))
        assertTrue(text.contains("✔ Launch Friday (Design is ready)"))
        assertTrue(text.contains("🔔 Check installs · Sat, Oct 24, 5:40 PM"))
        assertFalse(text.contains("Old one"))
        assertFalse(text.contains("quick note before"))
        assertTrue(text.endsWith(NoteShare.FOOTER))
    }

    @Test fun theTranscriptIsJustTheTranscript() {
        val text = text(NoteShare.Content.TRANSCRIPT, footer = false)
        assertEquals("*Team sync* — Oct 21, 5:40 PM\n\nOkay, quick note before the team sync.", text)
    }

    @Test fun everythingHasBothWithATranscriptHeading() {
        val text = text(NoteShare.Content.EVERYTHING)
        assertTrue(text.contains("☐ Send the release notes"))
        assertTrue(text.contains("Transcript\nOkay, quick note before the team sync."))
    }

    @Test fun olderNotesShareTheirTasksFromActionItems() {
        val older = note.copy(actionItems = "[\"[ ] Email Priya\",\"[x] Done thing\"]")
        val text = NoteShare.text(older, emptyList(), emptyList(), NoteShare.Options(), zone, java.util.Locale.US)
        assertTrue(text.contains("☐ Email Priya"))
        assertTrue(text.contains("☑ Done thing"))
    }
}
