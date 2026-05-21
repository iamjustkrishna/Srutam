package space.iamjustkrishna.srutam.ai

import android.content.Context
import space.iamjustkrishna.srutam.utils.AudioDecoder
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import java.io.File

class LocalTranscriber(private val context: Context) {
    private val audioDecoder = AudioDecoder()

    @Volatile
    private var recognizer: OnlineRecognizer? = null

    suspend fun transcribe(audioFile: File): String {
        val recognizer = getOrCreateRecognizer()
        val stream = recognizer.createStream()

        try {
            stream.acceptWaveform(FloatArray((LEFT_PADDING_SECONDS * SAMPLE_RATE).toInt()), SAMPLE_RATE)

            var hadSamples = false
            val decoded = audioDecoder.decodeAudioFileInChunks(audioFile, SAMPLE_RATE) { chunk ->
                hadSamples = true
                var offset = 0
                while (offset < chunk.size) {
                    val end = minOf(offset + CHUNK_SIZE_SAMPLES, chunk.size)
                    stream.acceptWaveform(chunk.copyOfRange(offset, end), SAMPLE_RATE)
                    while (recognizer.isReady(stream)) {
                        recognizer.decode(stream)
                    }
                    offset = end
                }
            }

            require(decoded && hadSamples) { "Failed to decode audio file for local transcription" }

            stream.acceptWaveform(FloatArray((TAIL_PADDING_SECONDS * SAMPLE_RATE).toInt()), SAMPLE_RATE)
            stream.inputFinished()

            while (recognizer.isReady(stream)) {
                recognizer.decode(stream)
            }

            return recognizer.getResult(stream).text.trim()
        } finally {
            stream.release()
        }
    }

    @Synchronized
    private fun getOrCreateRecognizer(): OnlineRecognizer {
        recognizer?.let { return it }

        val config = OnlineRecognizerConfig(
            featConfig = FeatureConfig(
                sampleRate = SAMPLE_RATE,
                featureDim = FEATURE_DIM,
                dither = 0.0f
            ),
            modelConfig = OnlineModelConfig(
                transducer = OnlineTransducerModelConfig(
                    encoder = ENCODER_ASSET,
                    decoder = DECODER_ASSET,
                    joiner = JOINER_ASSET
                ),
                tokens = TOKENS_ASSET,
                numThreads = NUM_THREADS,
                debug = false,
                provider = "cpu",
                modelType = "zipformer2"
            ),
            enableEndpoint = false,
            decodingMethod = "greedy_search",
            maxActivePaths = 4,
            blankPenalty = 0.0f
        )

        return OnlineRecognizer(
            assetManager = context.assets,
            config = config
        ).also { recognizer = it }
    }

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val FEATURE_DIM = 80
        private const val NUM_THREADS = 2
        private const val LEFT_PADDING_SECONDS = 0.3f
        private const val TAIL_PADDING_SECONDS = 0.6f
        private const val CHUNK_SECONDS = 0.5f
        private const val CHUNK_SIZE_SAMPLES = (SAMPLE_RATE * CHUNK_SECONDS).toInt()

        private const val ENCODER_ASSET = "encoder-epoch-99-avg-1-chunk-16-left-128.int8.onnx"
        private const val DECODER_ASSET = "decoder-epoch-99-avg-1-chunk-16-left-128.onnx"
        private const val JOINER_ASSET = "joiner-epoch-99-avg-1-chunk-16-left-128.int8.onnx"
        private const val TOKENS_ASSET = "tokens.txt"
    }
}
