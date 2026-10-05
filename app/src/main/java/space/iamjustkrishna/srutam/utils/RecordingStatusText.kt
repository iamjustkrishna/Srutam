package space.iamjustkrishna.srutam.utils

import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus

/** The AI status line shown in a note's File Info. */
object RecordingStatusText {
    fun label(recording: Recording?): String = when {
        recording == null -> "Not processed"
        recording.aiStatus == RecordingAiStatus.ERROR -> "Error processing"
        recording.isProcessing || recording.aiStatus == RecordingAiStatus.TRANSCRIBING -> "Transcribing..."
        recording.aiStatus == RecordingAiStatus.SUMMARY_PROCESSING -> "Analyzing insights..."
        recording.aiStatus == RecordingAiStatus.SUMMARY_PENDING_OFFLINE -> "Waiting for internet"
        !recording.summary.isNullOrBlank() -> "Insights ready"
        !recording.transcript.isNullOrBlank() -> "Transcribed"
        else -> "Not processed"
    }
}
