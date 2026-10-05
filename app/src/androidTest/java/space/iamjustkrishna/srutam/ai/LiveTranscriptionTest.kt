package space.iamjustkrishna.srutam.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveTranscriptionTest {
    private val transcriber = LocalTranscriber(InstrumentationRegistry.getInstrumentation().targetContext)

    @After
    fun tearDown() {
        transcriber.release()
    }

    @Test
    fun givesUpWhenTheModelCannotKeepUp() {
        val live = LiveTranscription(transcriber)
        val eightSeconds = FloatArray(SPEECH_SAMPLE_RATE * 8)

        // The model is still loading, so 160 s of audio queued instantly is far past the backlog limit.
        repeat(20) { live.feed(eightSeconds) }

        assertTrue("should have given up", live.gaveUp)
        assertNull("a transcript of an incomplete recording must not be offered", runBlocking { live.finish(5_000) })
        live.cancel()
    }

    @Test
    fun silenceGivesAnEmptyTranscriptNotAnError() {
        val live = LiveTranscription(transcriber)
        repeat(5) { live.feed(FloatArray(SPEECH_SAMPLE_RATE)) }

        val result = runBlocking { live.finish(60_000) }

        assertNotNull(result)
        assertEquals("", result!!.text)
        assertEquals(5_000L, result.audioMs)
        live.cancel()
    }

    @Test
    fun finishAfterCancelReturnsImmediately() {
        val live = LiveTranscription(transcriber)
        live.cancel()

        val start = System.nanoTime()
        val result = runBlocking { live.finish(30_000) }

        assertNull(result)
        assertTrue("took too long to report cancellation", (System.nanoTime() - start) / 1_000_000 < 2_000)
    }
}
