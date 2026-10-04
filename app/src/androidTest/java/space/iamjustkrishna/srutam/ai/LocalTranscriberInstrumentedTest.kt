package space.iamjustkrishna.srutam.ai

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import space.iamjustkrishna.srutam.utils.AudioDecoder
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Runs the real model on the device. Clips are re-encoded to 44.1 kHz AAC/M4A, the same shape the
 * recorder produces, so the decode -> resample -> VAD -> Whisper path matches production.
 */
@RunWith(AndroidJUnit4::class)
class LocalTranscriberInstrumentedTest {
    private val testContext = InstrumentationRegistry.getInstrumentation().context
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val transcriber = LocalTranscriber(appContext)

    @After
    fun tearDown() {
        transcriber.release()
    }

    @Test
    fun shortMemoIsTranscribedWithPunctuation() {
        val text = transcribe(listOf(MEMO_1))

        Log.i(TAG, "short memo: $text")
        assertTrue("recall too low for: $text", recall(reference(MEMO_1), text) >= 0.7)
        assertTrue("expected punctuation in: $text", text.contains(Regex("[.,?!]")))
        // "Third," is a one-word utterance after a pause; it was lost to VAD clipping before.
        assertTrue("lost the start of a segment in: $text", "third" in words(text))
    }

    @Test
    fun memoLongerThanThirtySecondsIsNotTruncated() {
        val text = transcribe(listOf(MEMO_1, MEMO_2, MEMO_4))

        Log.i(TAG, "long memo: $text")
        assertTrue("overall recall too low for: $text", recall(reference(MEMO_1, MEMO_2, MEMO_4), text) >= 0.7)
        // This clip starts about 48 s in, well past Whisper's 30 s window.
        assertTrue("tail missing from: $text", recall(reference(MEMO_4), text) >= 0.6)
    }

    @Test
    fun silentRecordingProducesNoText() {
        val file = File(appContext.cacheDir, "silence.m4a")
        writeM4a(ShortArray(SAMPLE_RATE * 5), file)

        assertEquals("", runBlocking { transcriber.transcribe(file) })
    }

    @Test
    fun decoderKeepsTheEndOfTheRecording() {
        val pcm = readWavPcm(MEMO_4)
        val file = File(appContext.cacheDir, "tail.m4a")
        writeM4a(pcm, file)

        var decodedSamples = 0L
        assertTrue(AudioDecoder().decodeAudioFileInChunks(file, SAMPLE_RATE) { decodedSamples += it.size })

        // AAC adds a little priming/padding, so only losing audio counts as a failure.
        val lostSamples = pcm.size - decodedSamples
        assertTrue("lost $lostSamples samples at the end", lostSamples <= SAMPLE_RATE * 0.05)
    }

    private fun transcribe(memos: List<String>): String {
        val gap = ShortArray(SAMPLE_RATE)
        val pcm = memos.map(::readWavPcm).reduce { acc, memo -> acc + gap + memo }
        val file = File(appContext.cacheDir, "memo.m4a")
        writeM4a(pcm, file)
        return runBlocking { transcriber.transcribe(file) }
    }

    private fun reference(vararg memos: String): String =
        memos.joinToString(" ") { testContext.assets.open("asr/$it.txt").bufferedReader().readText() }

    private fun recall(reference: String, hypothesis: String): Double {
        val available = words(hypothesis).groupingBy { it }.eachCount().toMutableMap()
        val expected = words(reference)
        var hits = 0
        for (word in expected) {
            val count = available[word] ?: 0
            if (count > 0) {
                hits++
                available[word] = count - 1
            }
        }
        return hits.toDouble() / expected.size
    }

    private fun words(text: String): List<String> =
        text.lowercase().replace(Regex("[^a-z0-9' ]"), " ").split(Regex("\\s+")).filter { it.isNotEmpty() }

    private fun readWavPcm(name: String): ShortArray {
        val bytes = testContext.assets.open("asr/$name.wav").use { it.readBytes() }
        var offset = 12
        while (offset + 8 <= bytes.size) {
            val id = String(bytes, offset, 4, Charsets.US_ASCII)
            val size = ByteBuffer.wrap(bytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            if (id == "data") {
                val samples = ShortArray(minOf(size, bytes.size - offset - 8) / 2)
                ByteBuffer.wrap(bytes, offset + 8, samples.size * 2)
                    .order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples)
                return samples
            }
            offset += 8 + size + (size and 1)
        }
        error("No data chunk in $name.wav")
    }

    private fun upsample(pcm: ShortArray, fromRate: Int, toRate: Int): ShortArray {
        val out = ShortArray((pcm.size.toLong() * toRate / fromRate).toInt())
        for (i in out.indices) {
            val position = i.toDouble() * fromRate / toRate
            val index = position.toInt().coerceAtMost(pcm.size - 1)
            val next = (index + 1).coerceAtMost(pcm.size - 1)
            val fraction = position - index
            out[i] = (pcm[index] * (1 - fraction) + pcm[next] * fraction).toInt().toShort()
        }
        return out
    }

    private fun writeM4a(pcm16k: ShortArray, file: File) {
        val pcm = upsample(pcm16k, SAMPLE_RATE, RECORDER_SAMPLE_RATE)
        val pcmBytes = ByteBuffer.allocate(pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            .apply { asShortBuffer().put(pcm) }.array()

        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, RECORDER_SAMPLE_RATE, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16_384)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        val muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var track = -1
        var bytePosition = 0
        var inputDone = false
        var outputDone = false
        val info = MediaCodec.BufferInfo()

        while (!outputDone) {
            if (!inputDone) {
                val inputIndex = codec.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val buffer = codec.getInputBuffer(inputIndex)!!
                    buffer.clear()
                    val count = minOf(pcmBytes.size - bytePosition, buffer.capacity())
                    val timeUs = bytePosition / 2L * 1_000_000 / RECORDER_SAMPLE_RATE
                    if (count <= 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, timeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        buffer.put(pcmBytes, bytePosition, count)
                        codec.queueInputBuffer(inputIndex, 0, count, timeUs, 0)
                        bytePosition += count
                    }
                }
            }

            val outputIndex = codec.dequeueOutputBuffer(info, 10_000)
            if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                track = muxer.addTrack(codec.outputFormat)
                muxer.start()
            } else if (outputIndex >= 0) {
                val buffer = codec.getOutputBuffer(outputIndex)!!
                if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && info.size > 0) {
                    buffer.position(info.offset)
                    buffer.limit(info.offset + info.size)
                    muxer.writeSampleData(track, buffer, info)
                }
                codec.releaseOutputBuffer(outputIndex, false)
                outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
            }
        }

        codec.stop()
        codec.release()
        muxer.stop()
        muxer.release()
    }

    private companion object {
        const val TAG = "LocalTranscriberTest"
        const val SAMPLE_RATE = 16000
        const val RECORDER_SAMPLE_RATE = 44100
        const val MEMO_1 = "sample_voice_memo_01"
        const val MEMO_2 = "sample_voice_memo_02"
        const val MEMO_4 = "sample_voice_memo_04"
    }
}
