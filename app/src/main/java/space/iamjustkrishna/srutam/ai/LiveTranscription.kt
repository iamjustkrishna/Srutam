package space.iamjustkrishna.srutam.ai

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicLong

/**
 * Transcribes speech while it is still being recorded, so most of the text is ready when the user
 * stops. The capture thread calls [feed] with 16 kHz mono float audio and must never block, so that
 * only queues the audio; a background worker runs the voice detector and the model on it.
 *
 * Live transcription is an optimisation, never a requirement: if the model cannot keep up (the queue
 * grows past [MAX_BACKLOG_SECONDS]) or anything fails, it gives up quietly and [finish] returns null,
 * and the caller transcribes the saved file instead.
 */
internal class LiveTranscription(private val transcriber: LocalTranscriber) {
    class Result(val text: String, val audioMs: Long)

    private sealed interface Message {
        class Audio(val samples: FloatArray) : Message
        object Break : Message
        class Finish(val result: CompletableDeferred<Result?>) : Message
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val messages = Channel<Message>(Channel.UNLIMITED)
    private val queuedSamples = AtomicLong(0)

    @Volatile
    var gaveUp = false
        private set

    @Volatile
    private var pendingFinish: CompletableDeferred<Result?>? = null

    init {
        // Starts loading the model right away, while the user is still starting to talk.
        scope.launch { run() }
    }

    /** Queues audio for recognition. Never blocks. */
    fun feed(samples: FloatArray) {
        if (gaveUp || samples.isEmpty()) return
        if (queuedSamples.addAndGet(samples.size.toLong()) > MAX_BACKLOG_SAMPLES) {
            giveUp("the model is not keeping up with the recording")
            return
        }
        messages.trySend(Message.Audio(samples))
    }

    /** Marks a pause in the audio (the user paused recording), so words before it are recognized now. */
    fun markBreak() {
        if (!gaveUp) messages.trySend(Message.Break)
    }

    /**
     * Recognizes whatever is still queued and returns the transcript, or null when live transcription
     * gave up, failed, or did not finish within [timeoutMs].
     */
    suspend fun finish(timeoutMs: Long): Result? {
        if (gaveUp) return null
        val result = CompletableDeferred<Result?>()
        pendingFinish = result
        if (!messages.trySend(Message.Finish(result)).isSuccess) return null
        val value = withTimeoutOrNull(timeoutMs) { result.await() }
        if (value == null) giveUp("did not finish within $timeoutMs ms")
        return value
    }

    /** Stops work and frees the model session. Safe to call more than once. */
    fun cancel() {
        gaveUp = true
        messages.close()
        pendingFinish?.complete(null)
        scope.cancel()
    }

    private fun giveUp(reason: String) {
        if (!gaveUp) Log.w(TAG, "Live transcription gave up: $reason")
        cancel()
    }

    private suspend fun run() {
        var session: TranscriptionSession? = null
        try {
            session = transcriber.openSession(currentCoroutineContext()[Job])
            for (message in messages) {
                when (message) {
                    is Message.Audio -> {
                        session.accept(message.samples)
                        queuedSamples.addAndGet(-message.samples.size.toLong())
                    }
                    Message.Break -> session.finish()
                    is Message.Finish -> {
                        session.finish()
                        message.result.complete(Result(session.text, session.totalSamples * 1000 / SPEECH_SAMPLE_RATE))
                        return
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "Live transcription failed", e)
            gaveUp = true
            messages.close()
            pendingFinish?.complete(null)
        } finally {
            session?.close()
        }
    }

    private companion object {
        const val TAG = "LiveTranscription"
        const val MAX_BACKLOG_SECONDS = 120
        const val MAX_BACKLOG_SAMPLES = MAX_BACKLOG_SECONDS.toLong() * SPEECH_SAMPLE_RATE
    }
}
