package space.iamjustkrishna.srutam.ai

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineNemoEncDecCtcModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.SpeechSegment
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import space.iamjustkrishna.srutam.utils.AudioDecoder
import java.io.File
import java.io.IOException

class LocalTranscriber(private val context: Context) {
    private val audioDecoder = AudioDecoder()

    @Volatile
    private var recognizer: OfflineRecognizer? = null

    /** [onProgress] receives how many ms of audio have been fully transcribed so far. */
    suspend fun transcribe(audioFile: File, onProgress: ((audioMs: Long) -> Unit)? = null): String {
        verifyModelAssets()
        val job = currentCoroutineContext()[Job]
        val recognizer = getOrCreateRecognizer()
        val vad = createVad()
        val startNanos = System.nanoTime()

        val session = Session(recognizer, vad, job, onProgress)
        try {
            val decoded = audioDecoder.decodeAudioFileInChunks(audioFile, SAMPLE_RATE, session::accept)
            job?.ensureActive()
            require(decoded && session.totalSamples > 0) { "Failed to decode audio file for local transcription" }

            session.finish()
        } finally {
            vad.release()
        }

        Log.d(
            TAG,
            "Transcribed ${session.totalSamples * 1000 / SAMPLE_RATE} ms of audio in " +
                "${(System.nanoTime() - startNanos) / 1_000_000} ms (${session.texts.size} speech segments)"
        )
        // No speech segments means no text, which lets the caller report a silent recording
        // instead of the model inventing words for noise.
        return session.texts.joinToString(" ")
    }

    @Synchronized
    fun release() {
        recognizer?.release()
        recognizer = null
    }

    /** State for one transcription: feeds audio to the VAD and recognizes each speech segment. */
    private class Session(
        private val recognizer: OfflineRecognizer,
        private val vad: Vad,
        private val job: Job?,
        private val onProgress: ((audioMs: Long) -> Unit)?
    ) {
        val texts = ArrayList<String>()
        var totalSamples = 0L
            private set

        private val recentAudio = RecentAudio(RECENT_AUDIO_SECONDS * SAMPLE_RATE)
        private var previousSegmentEnd = 0L

        fun accept(chunk: FloatArray) {
            totalSamples += chunk.size
            recentAudio.append(chunk)
            vad.acceptWaveform(chunk)
            recognizeReadySegments()
        }

        fun finish() {
            vad.flush()
            recognizeReadySegments()
        }

        private fun recognizeReadySegments() {
            while (!vad.empty()) {
                job?.ensureActive()
                val segment = vad.front()
                vad.pop()
                val text = recognize(withPreRoll(segment))
                if (text.isNotBlank()) texts.add(text)
                onProgress?.invoke((segment.start.toLong() + segment.samples.size) * 1000 / SAMPLE_RATE)
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
                stream.acceptWaveform(samples, SAMPLE_RATE)
                recognizer.decode(stream)
                return recognizer.getResult(stream).text.trim()
            } finally {
                stream.release()
            }
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

    // sherpa-onnx hard-exits the whole process when a model file is missing, so check first and
    // fail with a normal exception that the worker can turn into an ERROR state.
    private fun verifyModelAssets() {
        for (name in REQUIRED_ASSETS) {
            try {
                context.assets.open(name).close()
            } catch (e: IOException) {
                throw IllegalStateException("Speech model file missing from app assets: $name", e)
            }
        }
    }

    private fun createVad(): Vad = Vad(
        assetManager = context.assets,
        config = VadModelConfig(
            sileroVadModelConfig = SileroVadModelConfig(
                model = VAD_ASSET,
                threshold = VAD_THRESHOLD,
                minSilenceDuration = VAD_MIN_SILENCE_SECONDS,
                minSpeechDuration = VAD_MIN_SPEECH_SECONDS,
                maxSpeechDuration = MAX_SEGMENT_SECONDS
            ),
            sampleRate = SAMPLE_RATE,
            numThreads = 1,
            provider = "cpu"
        )
    )

    @Synchronized
    private fun getOrCreateRecognizer(): OfflineRecognizer {
        recognizer?.let { return it }

        val config = OfflineRecognizerConfig(
            featConfig = FeatureConfig(
                sampleRate = SAMPLE_RATE,
                featureDim = FEATURE_DIM
            ),
            modelConfig = OfflineModelConfig(
                nemo = OfflineNemoEncDecCtcModelConfig(model = MODEL_ASSET),
                tokens = TOKENS_ASSET,
                numThreads = NUM_THREADS,
                debug = false,
                provider = "cpu"
            ),
            decodingMethod = "greedy_search"
        )

        return OfflineRecognizer(
            assetManager = context.assets,
            config = config
        ).also { recognizer = it }
    }

    companion object {
        private const val TAG = "LocalTranscriber"

        private const val SAMPLE_RATE = 16000
        private const val FEATURE_DIM = 80
        private const val NUM_THREADS = 4

        // Cap on one decode so its memory stays bounded however long the recording is.
        // The library default (5 s) would split continuous speech far too often.
        private const val MAX_SEGMENT_SECONDS = 25f
        private const val VAD_THRESHOLD = 0.5f
        private const val VAD_MIN_SILENCE_SECONDS = 0.4f
        // Short, so single-word utterances are not dropped.
        private const val VAD_MIN_SPEECH_SECONDS = 0.1f
        private const val PRE_ROLL_SAMPLES = (0.4f * SAMPLE_RATE).toInt()
        // Must outlast the longest segment plus the silence the VAD waits for before reporting it.
        private const val RECENT_AUDIO_SECONDS = 40

        private const val MODEL_ASSET = "parakeet-110m.int8.onnx"
        private const val TOKENS_ASSET = "parakeet-110m-tokens.txt"
        private const val VAD_ASSET = "silero_vad.onnx"
        private val REQUIRED_ASSETS = listOf(MODEL_ASSET, TOKENS_ASSET, VAD_ASSET)
    }
}
