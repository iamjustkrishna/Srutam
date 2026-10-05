package space.iamjustkrishna.srutam.service

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.util.Log
import space.iamjustkrishna.srutam.ai.LiveTranscription
import space.iamjustkrishna.srutam.utils.StreamingResampler
import java.io.File
import java.nio.ByteOrder

/** A source of 16-bit mono PCM audio. The real one is the microphone; tests use a file. */
internal interface PcmSource {
    val sampleRate: Int

    fun start()

    /** Blocks until audio is available; returns the number of samples read, or a negative number on error. */
    fun read(buffer: ShortArray): Int

    fun stop()

    fun release()
}

internal class AudioRecordPcmSource(override val sampleRate: Int = RECORDING_SAMPLE_RATE) : PcmSource {
    private val record: AudioRecord

    init {
        val minimum = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        check(minimum > 0) { "Microphone does not support $sampleRate Hz mono" }
        record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minimum * 2, sampleRate * 2) // at least a second of headroom
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            error("Microphone could not be opened")
        }
    }

    override fun start() = record.startRecording()

    override fun read(buffer: ShortArray): Int = record.read(buffer, 0, buffer.size)

    override fun stop() = record.stop()

    override fun release() = record.release()
}

internal const val RECORDING_SAMPLE_RATE = 44100

/**
 * Records one note: reads PCM from a [PcmSource] on its own thread, encodes it to the same AAC `.m4a`
 * the app has always written (44.1 kHz mono, 128 kbps), and tees the audio to [live] so it can be
 * transcribed while the user is still talking. Pausing stops the source, so paused time is not
 * recorded, exactly like MediaRecorder's pause.
 *
 * [start] throws if the microphone or encoder cannot be opened; the caller then falls back to
 * MediaRecorder, so a problem here can never cost a recording.
 */
internal class AacRecordingPipeline(
    private val outputFile: File,
    private val source: PcmSource,
    private val live: LiveTranscription?
) {
    private val encoder: MediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
    private var muxer: MediaMuxer? = null
    private var track = -1
    private var muxerStarted = false
    private val info = MediaCodec.BufferInfo()

    private val toSpeechRate: StreamingResampler? =
        if (live != null) StreamingResampler(source.sampleRate, SPEECH_RATE) else null

    private val pauseLock = Object()
    private var thread: Thread? = null

    @Volatile
    private var stopping = false

    @Volatile
    private var paused = false

    /** True if the microphone stopped delivering audio before [stop] was called. */
    @Volatile
    var captureFailed = false
        private set

    private var samplesEncoded = 0L

    fun start() {
        try {
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, source.sampleRate, 1).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MAX_INPUT_BYTES)
            }
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            source.start()
        } catch (e: Throwable) {
            release()
            outputFile.delete()
            throw e
        }
        thread = Thread(::captureLoop, "recording-capture").also { it.start() }
    }

    fun pause() {
        paused = true
    }

    fun resume() {
        synchronized(pauseLock) {
            paused = false
            pauseLock.notifyAll()
        }
    }

    /**
     * Ends the recording and finalizes the file. Returns the recorded duration in ms, or null when
     * nothing was captured (the empty file is deleted).
     */
    fun stop(): Long? {
        stopping = true
        resume()
        thread?.join(JOIN_TIMEOUT_MS)
        try {
            toSpeechRate?.flush()?.let { live?.feed(it) }
            val written = finishEncoding()
            return if (written) samplesEncoded * 1000 / source.sampleRate else null
        } finally {
            release()
            if (samplesEncoded == 0L) outputFile.delete()
        }
    }

    private fun captureLoop() {
        val buffer = ShortArray(source.sampleRate / 10) // 100 ms
        var failures = 0
        var sourceRunning = true
        try {
            while (!stopping) {
                if (paused) {
                    if (sourceRunning) {
                        source.stop()
                        sourceRunning = false
                        live?.markBreak()
                    }
                    synchronized(pauseLock) {
                        while (paused && !stopping) pauseLock.wait()
                    }
                    if (!stopping && !sourceRunning) {
                        source.start()
                        sourceRunning = true
                    }
                    continue
                }

                val count = source.read(buffer)
                if (count > 0) {
                    failures = 0
                    handle(buffer, count)
                } else if (++failures > MAX_READ_FAILURES) {
                    Log.e(TAG, "Microphone stopped delivering audio (read returned $count)")
                    captureFailed = true
                    break
                } else {
                    Thread.sleep(10)
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Capture loop failed", e)
            captureFailed = true
        } finally {
            if (sourceRunning) runCatching { source.stop() }
        }
    }

    private fun handle(buffer: ShortArray, count: Int) {
        encode(buffer, count)
        if (live != null && toSpeechRate != null) {
            val floats = FloatArray(count) { buffer[it] / 32768f }
            live.feed(toSpeechRate.process(floats))
        }
    }

    private fun encode(pcm: ShortArray, count: Int) {
        var offset = 0
        while (offset < count) {
            val index = encoder.dequeueInputBuffer(CODEC_WAIT_US)
            if (index >= 0) {
                val input = encoder.getInputBuffer(index)!!
                input.clear()
                val samples = minOf(count - offset, input.capacity() / 2)
                input.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcm, offset, samples)
                encoder.queueInputBuffer(index, 0, samples * 2, presentationTimeUs(samplesEncoded), 0)
                samplesEncoded += samples
                offset += samples
            }
            drainEncoder(endOfStream = false)
        }
    }

    private fun finishEncoding(): Boolean {
        if (samplesEncoded == 0L) return false
        var queued = false
        var attempts = 0
        while (!queued && attempts++ < MAX_CODEC_WAITS) {
            val index = encoder.dequeueInputBuffer(CODEC_WAIT_US)
            if (index >= 0) {
                encoder.queueInputBuffer(index, 0, 0, presentationTimeUs(samplesEncoded), MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                queued = true
            } else {
                drainEncoder(endOfStream = false)
            }
        }
        if (!queued) return false
        drainEncoder(endOfStream = true)
        return muxerStarted
    }

    private fun drainEncoder(endOfStream: Boolean) {
        var idle = 0
        while (true) {
            val index = encoder.dequeueOutputBuffer(info, if (endOfStream) CODEC_WAIT_US else 0)
            when {
                index == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream || ++idle >= MAX_CODEC_WAITS) return
                }
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val activeMuxer = muxer!!
                    track = activeMuxer.addTrack(encoder.outputFormat)
                    activeMuxer.start()
                    muxerStarted = true
                }
                index >= 0 -> {
                    idle = 0
                    val data = encoder.getOutputBuffer(index)!!
                    if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && info.size > 0 && muxerStarted) {
                        data.position(info.offset)
                        data.limit(info.offset + info.size)
                        muxer!!.writeSampleData(track, data, info)
                    }
                    encoder.releaseOutputBuffer(index, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return
                }
            }
        }
    }

    private fun presentationTimeUs(samples: Long): Long = samples * 1_000_000 / source.sampleRate

    private fun release() {
        runCatching { source.release() }
        runCatching { encoder.stop() }
        runCatching { encoder.release() }
        muxer?.let {
            runCatching { if (muxerStarted) it.stop() }
            runCatching { it.release() }
        }
        muxer = null
    }

    private companion object {
        const val TAG = "AacRecordingPipeline"
        const val BIT_RATE = 128_000
        const val MAX_INPUT_BYTES = 16_384
        const val SPEECH_RATE = 16_000
        const val CODEC_WAIT_US = 10_000L
        const val MAX_CODEC_WAITS = 300
        const val MAX_READ_FAILURES = 100
        const val JOIN_TIMEOUT_MS = 5_000L
    }
}
