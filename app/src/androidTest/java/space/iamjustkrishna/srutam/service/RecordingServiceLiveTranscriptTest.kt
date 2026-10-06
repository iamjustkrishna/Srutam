package space.iamjustkrishna.srutam.service

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import space.iamjustkrishna.srutam.MainActivity
import space.iamjustkrishna.srutam.data.AppDatabase
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import space.iamjustkrishna.srutam.utils.AppPreferences
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Drives the real recording service with a recorded clip standing in for the microphone. */
@RunWith(AndroidJUnit4::class)
class RecordingServiceLiveTranscriptTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val testContext = instrumentation.context
    private val appContext = instrumentation.targetContext
    private val dao by lazy { AppDatabase.getDatabase(appContext).recordingDao() }
    private var activity: Activity? = null
    private val createdPaths = ArrayList<String>()
    private var autoAiBefore = false

    @Before
    fun setUp() {
        // Auto AI is on for new installs; these tests are about the transcript, not the AI step.
        autoAiBefore = AppPreferences.isAutoAiEnabled(appContext)
        AppPreferences.setAutoAiEnabled(appContext, false)
        for (permission in listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )) {
            runCatching { instrumentation.uiAutomation.grantRuntimePermission(appContext.packageName, permission) }
        }
        // Keep the app on screen: some OEM ROMs freeze background processes.
        val intent = Intent(appContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity = instrumentation.startActivitySync(intent)
    }

    @After
    fun tearDown() {
        RecordingForegroundService.pcmSourceFactory = { AudioRecordPcmSource() }
        if (RecordingCoordinator.isRecording) RecordingCoordinator.requestCancel(appContext)
        waitFor(20_000) { RecordingCoordinator.isIdle }
        runBlocking {
            for (path in createdPaths) {
                dao.getRecordingByPath(path)?.let { dao.deleteRecording(it) }
                File(path).delete()
            }
        }
        activity?.finish()
        AppPreferences.setAutoAiEnabled(appContext, autoAiBefore)
    }

    @Test
    fun speechIsTranscribedWhileRecordingAndStoredOnTheNote() {
        val source = FilePcmSource(upsample(readWavPcm(MEMO_1), 16000, RECORDING_SAMPLE_RATE), RECORDING_SAMPLE_RATE, realTime = true)
        RecordingForegroundService.pcmSourceFactory = { source }

        assertTrue(RecordingCoordinator.requestStart(appContext))
        assertTrue("recording did not start", waitFor(30_000) { RecordingCoordinator.state.value is RecordingCoordinator.RecordingSessionState.Recording })
        val path = (RecordingCoordinator.state.value as RecordingCoordinator.RecordingSessionState.Recording).filePath
        createdPaths += path

        assertTrue("clip never finished playing", waitFor(60_000) { source.drained })
        Thread.sleep(300)
        val stopAt = System.nanoTime()
        RecordingCoordinator.requestStop(appContext)

        var note: Recording? = null
        assertTrue("no transcript was stored on the note", waitFor(90_000) {
            note = runBlocking { dao.getRecordingByPath(path) }?.takeIf { !it.transcript.isNullOrBlank() }
            note != null
        })
        val stopToTranscriptMs = (System.nanoTime() - stopAt) / 1_000_000
        Log.i(TAG, "stop_to_stored_transcript_ms=$stopToTranscriptMs transcript=${note!!.transcript}")

        assertTrue("recall ${recall(MEMO_1_TEXT, note!!.transcript!!)}", recall(MEMO_1_TEXT, note!!.transcript!!) >= 0.85)
        assertEquals("auto-AI is off in this test, so no AI step runs", RecordingAiStatus.NOT_REQUESTED, note!!.aiStatus)
        assertEquals(24.6, note!!.duration / 1000.0, 0.5)
        assertEquals(24.6, durationOf(File(path)) / 1000.0, 0.5)

        assertTrue("the service did not shut itself down", waitFor(30_000) { !isRecordingServiceRunning() })
        assertTrue(RecordingCoordinator.isIdle)
    }

    @Test
    fun renamingTheFileInTheSaveDialogMovesTheStoredTranscriptWithIt() {
        val path = recordClipUntilDrained()
        RecordingCoordinator.requestStop(appContext, deferAutoAi = true)
        assertTrue("no transcript was stored on the note", waitFor(90_000) {
            !runBlocking { dao.getRecordingByPath(path) }?.transcript.isNullOrBlank()
        })
        val idBefore = runBlocking { dao.getRecordingByPath(path) }!!.id

        // What the in-app Save dialog does when the user types a name.
        val renamed = renameLikeTheSaveDialog(path, "Standup notes")
        assertEquals(1, runBlocking { dao.movePath(path, renamed, "Standup notes") })

        assertEquals("nothing may be left under the old name", null, runBlocking { dao.getRecordingByPath(path) })
        val moved = runBlocking { dao.getRecordingByPath(renamed) }!!
        assertEquals(idBefore, moved.id)
        assertEquals("Standup notes", moved.name)
        assertTrue("the transcript must follow the note", recall(MEMO_1_TEXT, moved.transcript!!) >= 0.85)
        assertTrue("the service did not shut itself down", waitFor(30_000) { !isRecordingServiceRunning() })
    }

    @Test
    fun renamingTheFileBeforeTheTranscriptArrivesLeavesNoGhostNote() {
        val path = recordClipUntilDrained()
        RecordingCoordinator.requestStop(appContext, deferAutoAi = true)
        // The recording is handed off at once; the last phrase is still being transcribed for about a second.
        assertTrue("recording did not stop", waitFor(30_000) { RecordingCoordinator.isIdle })

        val renamed = renameLikeTheSaveDialog(path, "Quick rename")
        runBlocking { dao.movePath(path, renamed, "Quick rename") }

        assertTrue("the service did not shut itself down", waitFor(60_000) { !isRecordingServiceRunning() })
        assertEquals("a renamed note must never leave a row under its old name", null, runBlocking { dao.getRecordingByPath(path) })
        // Either the transcript arrived first and moved with the note, or it was dropped; never a transcript-less ghost.
        val atNewName = runBlocking { dao.getRecordingByPath(renamed) }
        assertTrue("unexpected row: $atNewName", atNewName == null || !atNewName.transcript.isNullOrBlank())
    }

    @Test
    fun theFileBeingRecordedIsNotListedAsANoteUntilItIsFinished() {
        val path = recordClipUntilDrained()

        val whileRecording = space.iamjustkrishna.srutam.utils.AudioFileReader.getAudioFiles().map { it.filePath }
        assertTrue("an unfinished recording must not look like a note", path !in whileRecording)

        RecordingCoordinator.requestStop(appContext, deferAutoAi = true)
        assertTrue("recording did not stop", waitFor(30_000) { RecordingCoordinator.isIdle })

        val afterStop = space.iamjustkrishna.srutam.utils.AudioFileReader.getAudioFiles().map { it.filePath }
        assertTrue("the finished recording must be listed", path in afterStop)
    }

    @Test
    fun fallsBackToMediaRecorderWhenThePipelineCannotStart() {
        RecordingForegroundService.pcmSourceFactory = { throw IllegalStateException("no microphone pipeline") }

        assertTrue(RecordingCoordinator.requestStart(appContext))
        // Either MediaRecorder takes over, or (no microphone on this device) the service gives up cleanly.
        val started = waitFor(15_000) { RecordingCoordinator.state.value is RecordingCoordinator.RecordingSessionState.Recording }
        if (started) {
            val path = (RecordingCoordinator.state.value as RecordingCoordinator.RecordingSessionState.Recording).filePath
            createdPaths += path
            Thread.sleep(2_000)
            RecordingCoordinator.requestStop(appContext)
            assertTrue("service did not return to idle", waitFor(30_000) { RecordingCoordinator.isIdle })
            Log.i(TAG, "MediaRecorder fallback recorded ${File(path).length()} bytes")
            assertTrue("fallback produced no file", File(path).length() > 0)
        } else {
            Log.i(TAG, "MediaRecorder unavailable here; the service must still end up idle")
            assertTrue("service stuck after a failed start", waitFor(30_000) { RecordingCoordinator.isIdle })
        }
        assertNotNull(RecordingCoordinator.state.value)
    }

    /** Records the whole sample clip through the real service and returns the file's path. */
    private fun recordClipUntilDrained(): String {
        val source = FilePcmSource(upsample(readWavPcm(MEMO_1), 16000, RECORDING_SAMPLE_RATE), RECORDING_SAMPLE_RATE, realTime = true)
        RecordingForegroundService.pcmSourceFactory = { source }
        assertTrue(RecordingCoordinator.requestStart(appContext))
        assertTrue("recording did not start", waitFor(30_000) { RecordingCoordinator.state.value is RecordingCoordinator.RecordingSessionState.Recording })
        val path = (RecordingCoordinator.state.value as RecordingCoordinator.RecordingSessionState.Recording).filePath
        createdPaths += path
        assertTrue("clip never finished playing", waitFor(60_000) { source.drained })
        Thread.sleep(300)
        return path
    }

    private fun renameLikeTheSaveDialog(path: String, name: String): String {
        val target = File(File(path).parentFile, "$name.m4a")
        assertTrue("could not rename $path", File(path).renameTo(target))
        createdPaths += target.absolutePath
        return target.absolutePath
    }

    private fun waitFor(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(100)
        }
        return condition()
    }

    @Suppress("DEPRECATION")
    private fun isRecordingServiceRunning(): Boolean {
        val manager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(100).any { it.service.className == RecordingForegroundService::class.java.name }
    }

    private fun durationOf(file: File): Long {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
        } finally {
            retriever.release()
        }
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
        const val TAG = "RecordingServiceLiveTest"
        const val MEMO_1 = "sample_voice_memo_01"
        const val MEMO_1_TEXT = "Good morning team. Here are the three action items for today. First, we need to " +
            "finish the offline speech recognition benchmarks and confirm memory usage. Second, sync with Krishna " +
            "regarding the Play Store feature graphic design. Third, ship the release build to internal testers " +
            "by five PM. Let me know if you run into any blockers."
    }
}
