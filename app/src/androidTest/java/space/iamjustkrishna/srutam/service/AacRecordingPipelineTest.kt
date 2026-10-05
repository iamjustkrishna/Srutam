package space.iamjustkrishna.srutam.service

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import space.iamjustkrishna.srutam.ai.LiveTranscription
import space.iamjustkrishna.srutam.ai.LocalTranscriber
import space.iamjustkrishna.srutam.utils.AudioDecoder
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(AndroidJUnit4::class)
class AacRecordingPipelineTest {
    private val testContext = InstrumentationRegistry.getInstrumentation().context
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val transcriber = LocalTranscriber(appContext)
    private val files = ArrayList<File>()

    @After
    fun tearDown() {
        transcriber.release()
        files.forEach { it.delete() }
    }

    @Test
    fun noteIsSavedAndMostOfTheTextIsReadyWhenRecordingStops() {
        val pcm44 = upsample(readWavPcm(MEMO_1), 16000, RECORDING_SAMPLE_RATE)
        val source = FilePcmSource(pcm44, RECORDING_SAMPLE_RATE, realTime = true)
        val file = newFile("live")
        val live = LiveTranscription(transcriber)
        val pipeline = AacRecordingPipeline(file, source, live)

        pipeline.start()
        while (!source.drained) Thread.sleep(50)
        Thread.sleep(300) // let the last read land

        val stopStart = System.nanoTime()
        val recordedMs = pipeline.stop()
        val result = runBlocking { live.finish(timeoutMs = 120_000) }
        val stopToTextMs = (System.nanoTime() - stopStart) / 1_000_000
        live.cancel()

        Log.i(TAG, "stop_to_text_ms=$stopToTextMs recorded_ms=$recordedMs text=${result?.text}")
        assertNotNull("live transcription should have produced text", result)
        assertEquals(24.6, recordedMs!! / 1000.0, 0.4)
        assertTrue("recall ${recall(MEMO_1_TEXT, result!!.text)} for: ${result.text}", recall(MEMO_1_TEXT, result.text) >= 0.85)

        // The saved file must be a normal, playable AAC file of the right length.
        assertTrue("file is empty", file.length() > 10_000)
        var decoded = 0L
        assertTrue(AudioDecoder().decodeAudioFileInChunks(file, 16000) { decoded += it.size })
        assertEquals(24.6, decoded / 16000.0, 0.5)
    }

    @Test
    fun pausedTimeIsNotRecorded() {
        val pcm44 = upsample(readWavPcm(MEMO_1).copyOf(16000 * 8), 16000, RECORDING_SAMPLE_RATE) // 8 s
        val source = FilePcmSource(pcm44, RECORDING_SAMPLE_RATE, realTime = true)
        val file = newFile("paused")
        val pipeline = AacRecordingPipeline(file, source, live = null)

        pipeline.start()
        Thread.sleep(3_000)
        pipeline.pause()
        Thread.sleep(2_000) // two seconds of wall time that must not end up in the file
        pipeline.resume()
        while (!source.drained) Thread.sleep(50)
        Thread.sleep(300)
        val recordedMs = pipeline.stop()

        assertEquals("8 s of audio were delivered", 8.0, recordedMs!! / 1000.0, 0.3)
        var decoded = 0L
        AudioDecoder().decodeAudioFileInChunks(file, 16000) { decoded += it.size }
        assertEquals(8.0, decoded / 16000.0, 0.5)
    }

    @Test
    fun stoppingBeforeAnyAudioLeavesNoFile() {
        val source = FilePcmSource(ShortArray(0), RECORDING_SAMPLE_RATE, realTime = false)
        val file = newFile("empty")
        val pipeline = AacRecordingPipeline(file, source, live = null)

        pipeline.start()
        Thread.sleep(300)

        assertEquals(null, pipeline.stop())
        assertTrue("an empty recording should not leave a file behind", !file.exists())
    }

    private fun newFile(name: String) = File(appContext.cacheDir, "pipeline_$name.m4a").also {
        it.delete()
        files += it
    }

    private fun readWavPcm(name: String): ShortArray {
        val bytes = testContext.assets.open("asr/$name.wav").use { it.readBytes() }
        var offset = 12
        while (offset + 8 <= bytes.size) {
            val id = String(bytes, offset, 4, Charsets.US_ASCII)
            val size = ByteBuffer.wrap(bytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            if (id == "data") {
                val samples = ShortArray(minOf(size, bytes.size - offset - 8) / 2)
                ByteBuffer.wrap(bytes, offset + 8, samples.size * 2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples)
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

    private fun words(text: String) =
        text.lowercase().replace(Regex("[^a-z0-9' ]"), " ").split(Regex("\\s+")).filter { it.isNotEmpty() }

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

    private companion object {
        const val TAG = "AacRecordingPipelineTest"
        const val MEMO_1 = "sample_voice_memo_01"
        const val MEMO_1_TEXT = "Good morning team. Here are the three action items for today. First, we need to " +
            "finish the offline speech recognition benchmarks and confirm memory usage. Second, sync with Krishna " +
            "regarding the Play Store feature graphic design. Third, ship the release build to internal testers " +
            "by five PM. Let me know if you run into any blockers."
    }
}
