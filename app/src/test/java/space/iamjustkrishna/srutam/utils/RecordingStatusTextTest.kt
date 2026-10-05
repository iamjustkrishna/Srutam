package space.iamjustkrishna.srutam.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus

class RecordingStatusTextTest {
    private fun note(
        status: String = RecordingAiStatus.NOT_REQUESTED,
        processing: Boolean = false,
        transcript: String? = null,
        summary: String? = null
    ) = Recording(
        audioFilePath = "/rec/a.m4a",
        aiStatus = status,
        isProcessing = processing,
        transcript = transcript,
        summary = summary
    )

    @Test fun aFileWithNoStoredNoteIsNotProcessed() {
        assertEquals("Not processed", RecordingStatusText.label(null))
    }

    @Test fun aNoteNothingHasRunOnIsNotProcessed() {
        assertEquals("Not processed", RecordingStatusText.label(note()))
    }

    @Test fun aNoteWithOnlyItsLiveTranscriptIsTranscribedNotReady() {
        assertEquals("Transcribed", RecordingStatusText.label(note(transcript = "hello")))
    }

    @Test fun progressIsShownWhileTheWorkerRuns() {
        assertEquals("Transcribing...", RecordingStatusText.label(note(status = RecordingAiStatus.TRANSCRIBING)))
        assertEquals("Transcribing...", RecordingStatusText.label(note(processing = true)))
        assertEquals("Analyzing insights...", RecordingStatusText.label(note(status = RecordingAiStatus.SUMMARY_PROCESSING, processing = false)))
    }

    @Test fun anOfflineNoteSaysItIsWaitingForInternet() {
        assertEquals("Waiting for internet", RecordingStatusText.label(note(status = RecordingAiStatus.SUMMARY_PENDING_OFFLINE, transcript = "hi")))
    }

    @Test fun aFinishedNoteSaysItsInsightsAreReady() {
        assertEquals("Insights ready", RecordingStatusText.label(note(status = RecordingAiStatus.READY, transcript = "hi", summary = "A summary")))
    }

    @Test fun anErrorIsShown() {
        assertEquals("Error processing", RecordingStatusText.label(note(status = RecordingAiStatus.ERROR)))
    }
}
