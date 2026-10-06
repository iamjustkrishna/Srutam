package space.iamjustkrishna.srutam.ai

import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.SpeechSegment
import com.k2fsa.sherpa.onnx.Vad
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive

/** The sample rate the speech model and voice detector work at. */
internal const val SPEECH_SAMPLE_RATE = 16000

/**
 * One transcription: takes 16 kHz mono float audio in any chunk sizes, finds the speech with the voice
 * detector, and recognizes each speech segment. Used for audio files and for live capture while
 * recording. Not thread-safe: call it from one thread at a time.
 */
internal class TranscriptionSession(
    private val recognizer: OfflineRecognizer,
    private val vad: Vad,
    private val job: Job?,
    private val onProgress: ((audioMs: Long) -> Unit)?
) : AutoCloseable {
    private val texts = ArrayList<String>()

    /** Everything recognized so far, joined with spaces; empty when no speech was found. */
    val text: String
        get() = texts.joinToString(" ")

    val segmentCount: Int
        get() = texts.size

    var totalSamples = 0L
        private set

    /** True while the voice detector hears someone speaking in the audio it was last given. */
    val isSpeechDetected: Boolean
        get() = vad.isSpeechDetected()

    private val recentAudio = RecentAudio(RECENT_AUDIO_SECONDS * SPEECH_SAMPLE_RATE)
    private var previousSegmentEnd = 0L

    fun accept(chunk: FloatArray) {
        totalSamples += chunk.size
        recentAudio.append(chunk)
        vad.acceptWaveform(chunk)
        recognizeReadySegments()
    }

    /** Ends the current utterance so its words are recognized now; more audio can still follow. */
    fun finish() {
        vad.flush()
        recognizeReadySegments()
    }

    override fun close() {
        vad.release()
    }

    private fun recognizeReadySegments() {
        while (!vad.empty()) {
            job?.ensureActive()
            val segment = vad.front()
            vad.pop()
            val text = recognize(withPreRoll(segment))
            if (text.isNotBlank()) texts.add(text)
            onProgress?.invoke((segment.start.toLong() + segment.samples.size) * 1000 / SPEECH_SAMPLE_RATE)
        }
    }

    // The VAD reports speech slightly after it begins, which clips the first word. Prepend a
    // little of the audio before the segment, without reaching back into the previous one.
    private fun withPreRoll(segment: SpeechSegment): FloatArray {
        val start = segment.start.toLong()
        val preRollStart = maxOf(start - PRE_ROLL_SAMPLES, previousSegmentEnd)
        previousSegmentEnd = start + segment.samples.size
        return recentAudio.slice(preRollStart, start) + segment.samples
    }

    private fun recognize(samples: FloatArray): String {
        val stream = recognizer.createStream()
        try {
            stream.acceptWaveform(samples, SPEECH_SAMPLE_RATE)
            recognizer.decode(stream)
            return recognizer.getResult(stream).text.trim()
        } finally {
            stream.release()
        }
    }

    private companion object {
        const val PRE_ROLL_SAMPLES = (0.4f * SPEECH_SAMPLE_RATE).toInt()

        // Must outlast the longest segment plus the silence the VAD waits for before reporting it.
        const val RECENT_AUDIO_SECONDS = 40
    }
}

/** Ring buffer of the most recent samples, addressed by their absolute position in the stream. */
private class RecentAudio(private val capacity: Int) {
    private val samples = FloatArray(capacity)
    private var total = 0L

    fun append(chunk: FloatArray) {
        var offset = 0
        while (offset < chunk.size) {
            val position = (total % capacity).toInt()
            val count = minOf(chunk.size - offset, capacity - position)
            System.arraycopy(chunk, offset, samples, position, count)
            offset += count
            total += count
        }
    }

    fun slice(from: Long, to: Long): FloatArray {
        val begin = maxOf(from, total - capacity, 0L)
        val end = minOf(to, total)
        if (end <= begin) return FloatArray(0)
        return FloatArray((end - begin).toInt()) { samples[((begin + it) % capacity).toInt()] }
    }
}
