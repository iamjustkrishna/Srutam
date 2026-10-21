package space.iamjustkrishna.srutam.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import space.iamjustkrishna.srutam.data.InsightStatus
import space.iamjustkrishna.srutam.data.NoteSource
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import space.iamjustkrishna.srutam.data.ReminderStatus

class RestoreMergeTest {

    private fun cloud(
        id: String = "cloud-1",
        noteUuid: String? = "uuid-1",
        audioHash: String? = null,
        title: String = "Standup",
        timestampMs: Long = 1_000_000L,
        durationMs: Long = 60_000L,
        updatedAtMs: Long = 2_000_000L
    ) = CloudNote(
        id = id, noteUuid = noteUuid, audioHash = audioHash, title = title, transcript = "t", summary = "s",
        keyPoints = null, wiifm = null, aiStatus = RecordingAiStatus.READY, durationMs = durationMs,
        timestampMs = timestampMs, isPrivate = false, topics = null, updatedAtMs = updatedAtMs
    )

    private fun local(
        id: Long = 1,
        noteUuid: String = "local-uuid",
        cloudId: String? = null,
        audioHash: String? = null,
        name: String = "Standup",
        timestamp: Long = 1_000_000L,
        duration: Long = 60_000L,
        aiStatus: String = RecordingAiStatus.READY,
        isProcessing: Boolean = false,
        source: String = NoteSource.AI,
        updatedAt: Long = 1_500_000L
    ) = Recording(
        id = id, audioFilePath = "/music/$id.m4a", noteUuid = noteUuid, cloudId = cloudId, audioHash = audioHash,
        name = name, timestamp = timestamp, duration = duration, aiStatus = aiStatus, isProcessing = isProcessing,
        contentSource = source, updatedAt = updatedAt
    )

    // ---- Matching: cloud id -> note id -> audio hash -> name and time ----

    @Test fun matchesByCloudIdFirst() {
        val byCloudId = local(id = 1, cloudId = "cloud-1", noteUuid = "other")
        val byUuid = local(id = 2, noteUuid = "uuid-1")
        assertSame(byCloudId, RestoreMerge.match(listOf(byUuid, byCloudId), cloud()))
    }

    @Test fun matchesByNoteIdThenAudioHash() {
        val byUuid = local(id = 2, noteUuid = "uuid-1")
        assertSame(byUuid, RestoreMerge.match(listOf(byUuid), cloud()))

        val byHash = local(id = 3, audioHash = "abc")
        assertSame(byHash, RestoreMerge.match(listOf(byHash), cloud(noteUuid = null, audioHash = "abc")))
    }

    @Test fun fallsBackToTimeAndLengthForNotesWithoutIds() {
        val sameRecording = local(timestamp = 1_002_000L, duration = 60_500L)
        assertSame(sameRecording, RestoreMerge.match(listOf(sameRecording), cloud(noteUuid = null)))
    }

    @Test fun aDifferentRecordingIsNotMatched() {
        val other = local(timestamp = 9_000_000L)
        assertNull(RestoreMerge.match(listOf(other), cloud(noteUuid = null)))
    }

    @Test fun anAudioFileWithoutANoteIsMatchedByTime() {
        val file = LocalAudio("/music/old.m4a", "recording_1000", lastModifiedMs = 1_001_000L, durationMs = 60_000L)
        assertSame(file, RestoreMerge.matchAudio(listOf(file), cloud(noteUuid = null)))
    }

    // ---- Who wins ----

    @Test fun cloudWinsWhileAiIsQueuedOrRunning() {
        val running = local(aiStatus = RecordingAiStatus.SUMMARY_PROCESSING, isProcessing = true, updatedAt = 9_000_000L)
        assertEquals(RestoreMerge.Winner.CLOUD, RestoreMerge.winner(running, cloud()))

        val queued = local(aiStatus = RecordingAiStatus.NOT_REQUESTED, updatedAt = 9_000_000L)
        assertEquals(RestoreMerge.Winner.CLOUD, RestoreMerge.winner(queued, cloud()))
    }

    @Test fun cloudWinsOverANoteReprocessedAfterTheReinstall() {
        val reprocessed = local(source = NoteSource.AI, updatedAt = 9_000_000L)
        assertEquals(RestoreMerge.Winner.CLOUD, RestoreMerge.winner(reprocessed, cloud(updatedAtMs = 1L)))
    }

    @Test fun aNoteRestoredFromItsFileKeepsWhicheverIsNewer() {
        val newerLocal = local(source = NoteSource.FILE, updatedAt = 3_000_000L)
        assertEquals(RestoreMerge.Winner.LOCAL, RestoreMerge.winner(newerLocal, cloud(updatedAtMs = 2_000_000L)))

        val olderLocal = local(source = NoteSource.FILE, updatedAt = 1_000_000L)
        assertEquals(RestoreMerge.Winner.CLOUD, RestoreMerge.winner(olderLocal, cloud(updatedAtMs = 2_000_000L)))
    }

    // ---- Statuses only move forward ----

    @Test fun theMostProgressedTaskStatusWins() {
        assertEquals(InsightStatus.COMPLETED, RestoreMerge.mostProgressedInsight(InsightStatus.OPEN, InsightStatus.COMPLETED))
        assertEquals(InsightStatus.COMPLETED, RestoreMerge.mostProgressedInsight(InsightStatus.COMPLETED, InsightStatus.OPEN))
        assertEquals(InsightStatus.ARCHIVED, RestoreMerge.mostProgressedInsight(InsightStatus.COMPLETED, InsightStatus.ARCHIVED))
    }

    @Test fun theMostProgressedReminderStatusWins() {
        assertEquals(ReminderStatus.COMPLETED, RestoreMerge.mostProgressedReminder(ReminderStatus.ACTIVE, ReminderStatus.COMPLETED))
        assertEquals(ReminderStatus.DISMISSED, RestoreMerge.mostProgressedReminder(ReminderStatus.ACTIVE, ReminderStatus.DISMISSED))
        assertEquals(ReminderStatus.COMPLETED, RestoreMerge.mostProgressedReminder(ReminderStatus.COMPLETED, ReminderStatus.DISMISSED))
    }
}
