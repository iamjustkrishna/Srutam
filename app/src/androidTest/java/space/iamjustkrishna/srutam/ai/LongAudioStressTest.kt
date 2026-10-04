package space.iamjustkrishna.srutam.ai

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadataRetriever
import android.os.BatteryManager
import android.os.Debug
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import space.iamjustkrishna.srutam.MainActivity
import space.iamjustkrishna.srutam.utils.AudioDecoder
import java.io.File

/**
 * Stress test for long recordings. Not part of the normal run: it needs `stress_<name>.m4a` plus a
 * same-named `.txt` reference pushed into the app's external files directory, and picks the file
 * with `-e stressFile stress_30min.m4a` (the largest one when unset).
 */
@RunWith(AndroidJUnit4::class)
class LongAudioStressTest {
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val inputDir = appContext.getExternalFilesDir(null)!!
    private val transcriber = LocalTranscriber(appContext)

    private var activity: Activity? = null

    // Some OEM ROMs (seen on a Realme) freeze a process that has no visible UI after a few
    // minutes, which would stall this test, so keep the app on screen while it runs.
    @Before
    fun keepAppInForeground() {
        val intent = Intent(appContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity = InstrumentationRegistry.getInstrumentation().startActivitySync(intent)
    }

    @After
    fun tearDown() {
        transcriber.release()
        activity?.finish()
    }

    @Test
    fun longRecordingCompletesWithoutLosingContent() {
        val file = stressFile()
        val reference = File(inputDir, file.nameWithoutExtension + ".txt").readText()
        val audioMs = durationMs(file)
        val monitor = ResourceMonitor().also { it.start() }
        val startNanos = System.nanoTime()
        val progress = ArrayList<Pair<Long, Long>>()
        var lastLoggedBucket = -1L

        val text = try {
            runBlocking {
                transcriber.transcribe(file) { audioPosition ->
                    val elapsed = elapsedMs(startNanos)
                    synchronized(progress) { progress += elapsed to audioPosition }
                    if (audioPosition / PROGRESS_LOG_MS > lastLoggedBucket) {
                        lastLoggedBucket = audioPosition / PROGRESS_LOG_MS
                        log("progress", "${audioPosition / 1000}s of audio done at ${elapsed / 1000}s")
                    }
                }
            }
        } finally {
            monitor.stop()
        }
        val elapsedMs = elapsedMs(startNanos)

        val hypothesisWords = words(text)
        val referenceWords = words(reference)
        val overallRecall = recall(referenceWords, hypothesisWords)
        val worstBinRecall = binnedRecalls(referenceWords, hypothesisWords).min()
        val rssAtTenPercent = monitor.rssKbAt(startNanos, progress.firstOrNull { it.second >= audioMs / 10 }?.first)
        val rssGrowthMb = ((monitor.peakRssKb - rssAtTenPercent) / 1024).coerceAtLeast(0)

        log("file", file.name)
        log("audio_s", audioMs / 1000)
        log("elapsed_s", elapsedMs / 1000)
        log("rtf", "%.3f".format(elapsedMs.toDouble() / audioMs))
        log("rtf_by_fifth", rtfByFifth(progress, audioMs).joinToString(" ") { "%.3f".format(it) })
        log("peak_rss_mb", monitor.peakRssKb / 1024)
        log("rss_growth_after_10pct_mb", rssGrowthMb)
        log("peak_native_heap_mb", monitor.peakNativeKb / 1024)
        log("peak_java_heap_mb", monitor.peakJavaKb / 1024)
        log("temp_c_start_max_end", "${monitor.startTempC} ${monitor.maxTempC} ${monitor.endTempC}")
        log("words_ref_hyp", "${referenceWords.size} ${hypothesisWords.size}")
        log("recall_overall", "%.3f".format(overallRecall))
        log("recall_worst_tenth", "%.3f".format(worstBinRecall))

        assertTrue("content lost: overall recall $overallRecall", overallRecall >= 0.8)
        assertTrue("later part of the recording degraded: worst tenth recall $worstBinRecall", worstBinRecall >= 0.7)
        assertTrue("memory kept growing: +$rssGrowthMb MB after the first 10%", rssGrowthMb < 400)
    }

    @Test
    fun decodingAloneKeepsTheWholeRecording() {
        val file = stressFile()
        val audioMs = durationMs(file)
        var decodedSamples = 0L
        val startNanos = System.nanoTime()

        val ok = AudioDecoder().decodeAudioFileInChunks(file, 16000) { decodedSamples += it.size }
        val elapsedMs = elapsedMs(startNanos)

        log("decode_only_audio_s", audioMs / 1000)
        log("decode_only_elapsed_s", elapsedMs / 1000)
        log("decode_only_rtf", "%.3f".format(elapsedMs.toDouble() / audioMs))
        assertTrue("decoder failed", ok)
        val decodedMs = decodedSamples * 1000 / 16000
        assertTrue("decoded $decodedMs ms of $audioMs ms", decodedMs >= audioMs * 0.98)
    }

    @Test
    fun cancellingStopsPromptlyAndTheTranscriberStillWorks() {
        val file = stressFile()
        val cancelAfterMs = 15_000L

        val cancelLatencyMs = runBlocking {
            val job = async(Dispatchers.Default) { transcriber.transcribe(file) }
            delay(cancelAfterMs)
            val cancelStart = System.nanoTime()
            job.cancelAndJoin()
            val latency = elapsedMs(cancelStart)
            assertTrue("job should have been cancelled", job.isCancelled)
            runCatching { job.await() }.onFailure { assertTrue(it is CancellationException) }
            latency
        }
        log("cancel_latency_ms", cancelLatencyMs)
        assertTrue("cancel took $cancelLatencyMs ms", cancelLatencyMs < 10_000)

        // The same transcriber must still work after a cancelled run.
        val short = pushedFiles().minByOrNull { durationMs(it) }!!
        val text = runBlocking { transcriber.transcribe(short) }
        assertTrue("no text after a cancelled run", text.isNotBlank())
    }

    private fun pushedFiles(): List<File> =
        inputDir.listFiles { f -> f.name.startsWith("stress_") && f.extension == "m4a" }?.toList().orEmpty()

    private fun stressFile(): File {
        val files = pushedFiles()
        assumeTrue("no stress_*.m4a pushed to $inputDir", files.isNotEmpty())
        val requested = InstrumentationRegistry.getArguments().getString("stressFile")
        return files.firstOrNull { it.name == requested } ?: files.maxByOrNull { it.length() }!!
    }

    private fun durationMs(file: File): Long {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
        } finally {
            retriever.release()
        }
    }

    private fun elapsedMs(startNanos: Long) = (System.nanoTime() - startNanos) / 1_000_000

    private fun log(key: String, value: Any) = Log.i(TAG, "$key=$value")

    private fun words(text: String): List<String> =
        text.lowercase().replace(Regex("[^a-z0-9' ]"), " ").split(Regex("\\s+")).filter { it.isNotEmpty() }

    private fun recall(reference: List<String>, hypothesis: List<String>): Double {
        if (reference.isEmpty()) return 1.0
        val available = hypothesis.groupingBy { it }.eachCount().toMutableMap()
        var hits = 0
        for (word in reference) {
            val count = available[word] ?: 0
            if (count > 0) {
                hits++
                available[word] = count - 1
            }
        }
        return hits.toDouble() / reference.size
    }

    // Splits the reference into tenths and scores each against the matching, slightly widened, part
    // of the hypothesis, so truncation or late-file degradation shows up as one bad tenth.
    private fun binnedRecalls(reference: List<String>, hypothesis: List<String>): List<Double> {
        val bins = 10
        return (0 until bins).map { bin ->
            val refPart = reference.subList(reference.size * bin / bins, reference.size * (bin + 1) / bins)
            val margin = hypothesis.size / (bins * 5)
            val from = (hypothesis.size * bin / bins - margin).coerceAtLeast(0)
            val to = (hypothesis.size * (bin + 1) / bins + margin).coerceAtMost(hypothesis.size)
            recall(refPart, hypothesis.subList(from, to))
        }
    }

    // Real-time factor for each fifth of the audio; a rising trend means slowdown or throttling.
    private fun rtfByFifth(progress: List<Pair<Long, Long>>, audioMs: Long): List<Double> {
        val points = synchronized(progress) { progress.toList() }
        var previousElapsed = 0L
        var previousAudio = 0L
        return (1..5).map { fifth ->
            val target = audioMs * fifth / 5
            val point = points.firstOrNull { it.second >= target } ?: points.lastOrNull() ?: (0L to 0L)
            val rtf = (point.first - previousElapsed).toDouble() / (point.second - previousAudio).coerceAtLeast(1)
            previousElapsed = point.first
            previousAudio = point.second
            rtf
        }
    }

    private inner class ResourceMonitor {
        @Volatile private var running = false
        private var thread: Thread? = null
        private val samples = ArrayList<Pair<Long, Long>>() // elapsed ms to RSS kB
        private var begin = 0L

        @Volatile var peakRssKb = 0L; private set
        @Volatile var peakNativeKb = 0L; private set
        @Volatile var peakJavaKb = 0L; private set
        var startTempC = 0f; private set
        @Volatile var maxTempC = 0f; private set
        @Volatile var endTempC = 0f; private set

        fun start() {
            running = true
            begin = System.nanoTime()
            startTempC = batteryTempC()
            thread = Thread {
                while (running) {
                    sample()
                    Thread.sleep(SAMPLE_INTERVAL_MS)
                }
            }.also { it.start() }
        }

        fun stop() {
            running = false
            thread?.join()
            sample()
            endTempC = batteryTempC()
        }

        fun rssKbAt(startNanos: Long, elapsedMs: Long?): Long {
            if (elapsedMs == null) return peakRssKb
            val offset = (startNanos - begin) / 1_000_000
            return synchronized(samples) { samples.firstOrNull { it.first >= elapsedMs + offset }?.second } ?: peakRssKb
        }

        private fun sample() {
            val rss = readStatusKb("VmRSS")
            synchronized(samples) { samples += elapsedMs(begin) to rss }
            peakRssKb = maxOf(peakRssKb, rss)
            peakNativeKb = maxOf(peakNativeKb, Debug.getNativeHeapAllocatedSize() / 1024)
            val runtime = Runtime.getRuntime()
            peakJavaKb = maxOf(peakJavaKb, (runtime.totalMemory() - runtime.freeMemory()) / 1024)
            maxTempC = maxOf(maxTempC, batteryTempC())
        }

        private fun readStatusKb(key: String): Long =
            File("/proc/self/status").readLines().firstOrNull { it.startsWith("$key:") }
                ?.filter { it.isDigit() }?.toLongOrNull() ?: 0L

        private fun batteryTempC(): Float {
            val intent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            return (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        }
    }

    private companion object {
        const val TAG = "LongAudioStress"
        const val SAMPLE_INTERVAL_MS = 3_000L
        const val PROGRESS_LOG_MS = 30_000L
    }
}
