package space.iamjustkrishna.srutam.utils

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.cancellation.CancellationException

/**
 * Decodes M4A audio files to 16kHz mono PCM float array for speech recognition.
 * Uses MediaCodec for hardware-accelerated decoding when available.
 */
class AudioDecoder {
    companion object {
        private const val TAG = "AudioDecoder"
        private const val TARGET_SAMPLE_RATE = 16000
        private const val BUFFER_SIZE = 4096
        private const val CODEC_WAIT_US = 10_000L
        private const val MAX_IDLE_WAITS_AFTER_INPUT = 300
    }

    /**
     * Decodes M4A file to 16kHz mono PCM samples as float array (values -1.0 to 1.0).
     * @return Float array with PCM samples at 16kHz sample rate
     */
    fun decodeAudioFile(audioFile: File): FloatArray {
        val pcmData = mutableListOf<Float>()
        
        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(audioFile.absolutePath)
            
            // Find audio track
            val trackIndex = findAudioTrack(extractor)
            if (trackIndex < 0) {
                Log.e(TAG, "No audio track found in file: ${audioFile.name}")
                return FloatArray(0)
            }
            
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            
            Log.d(TAG, "Audio format - Sample Rate: $sampleRate Hz, Channels: $channelCount")
            
            // Create decoder
            val mimeType = format.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"
            val codec = MediaCodec.createDecoderByType(mimeType)
            codec.configure(format, null, null, 0)
            codec.start()
            
            // Decode
            val inputBuffers = codec.inputBuffers
            val outputBuffers = codec.outputBuffers
            val info = MediaCodec.BufferInfo()
            var decodingComplete = false
            
            while (!decodingComplete) {
                val inputIndex = codec.dequeueInputBuffer(10000)
                if (inputIndex >= 0) {
                    val inputBuffer = inputBuffers[inputIndex]
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        decodingComplete = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
                
                val outputIndex = codec.dequeueOutputBuffer(info, 10000)
                if (outputIndex >= 0) {
                    val outputBuffer = outputBuffers[outputIndex]
                    val pcmSamples = ByteArray(info.size)
                    outputBuffer.get(pcmSamples)
                    outputBuffer.clear()
                    
                    // Convert byte array to float array
                    val shorts = ByteBuffer.wrap(pcmSamples)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .asShortBuffer()
                    
                    for (i in 0 until shorts.limit()) {
                        val sample = shorts[i].toFloat() / 32768.0f
                        pcmData.add(sample)
                    }
                    
                    codec.releaseOutputBuffer(outputIndex, false)
                    
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        decodingComplete = true
                    }
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // Ignore
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // Ignore
                }
            }
            
            codec.stop()
            codec.release()
            extractor.release()
            
            // Resample to 16kHz if needed
            val resampledData = if (sampleRate != TARGET_SAMPLE_RATE) {
                resampleAudio(pcmData.toFloatArray(), sampleRate, TARGET_SAMPLE_RATE)
            } else {
                pcmData.toFloatArray()
            }
            
            // Convert stereo to mono if needed
            return if (channelCount > 1) {
                stereoToMono(resampledData, channelCount)
            } else {
                resampledData
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding audio file: ${e.message}", e)
            return FloatArray(0)
        }
    }

    /**
     * Streams the file's audio to [onChunk] as mono float samples at [targetSampleRate].
     *
     * The codec is driven without blocking: every free input buffer is filled, every finished
     * output buffer is drained, and the thread only waits when there is nothing to feed or take.
     *
     * Speed is limited by Android's AAC decoder, not this loop: it costs a few ms of framework overhead
     * per 23 ms frame (about 0.18x real time on a Realme with a Helio G95; reading the frames from the
     * file takes under 1%), and neither a non-blocking loop nor async callbacks changed that. Callers
     * should therefore overlap decoding with other work (see LocalTranscriber) rather than wait for it.
     */
    fun decodeAudioFileInChunks(
        audioFile: File,
        targetSampleRate: Int = TARGET_SAMPLE_RATE,
        onChunk: (FloatArray) -> Unit,
    ): Boolean {
        val extractor = MediaExtractor()
        var startedCodec: MediaCodec? = null
        try {
            extractor.setDataSource(audioFile.absolutePath)

            val trackIndex = findAudioTrack(extractor)
            if (trackIndex < 0) {
                Log.e(TAG, "No audio track found in file: ${audioFile.name}")
                return false
            }

            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            Log.d(TAG, "Audio format - Sample Rate: $sampleRate Hz, Channels: $channelCount")

            val mimeType = format.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"
            val codec = MediaCodec.createDecoderByType(mimeType)
            startedCodec = codec
            codec.configure(format, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            var resampler = resamplerFor(sampleRate, targetSampleRate)
            var inputDone = false
            var outputDone = false

            // Takes one finished output buffer (or a format change) if there is one; returns whether it did.
            fun drainOutput(timeoutUs: Long): Boolean {
                val outputIndex = codec.dequeueOutputBuffer(info, timeoutUs)
                if (outputIndex >= 0) {
                    val buffer = codec.getOutputBuffer(outputIndex)
                    if (buffer != null && info.size > 0) {
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        val chunk = pcm16ToMonoFloat(buffer, channelCount)
                        val converted = resampler?.process(chunk) ?: chunk
                        if (converted.isNotEmpty()) onChunk(converted)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true
                    return true
                }
                if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // The decoder can output a different rate or channel count than the container declares
                    // (e.g. Opus is always decoded at 48 kHz).
                    val outputFormat = codec.outputFormat
                    val newRate = if (outputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    } else {
                        sampleRate
                    }
                    if (outputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channelCount = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    if (newRate != sampleRate) {
                        resampler?.flush()?.takeIf { it.isNotEmpty() }?.let(onChunk)
                        sampleRate = newRate
                        resampler = resamplerFor(sampleRate, targetSampleRate)
                    }
                    return true
                }
                return false
            }

            var idleWaits = 0
            while (!outputDone) {
                var progressed = false

                while (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(0)
                    if (inputIndex < 0) break
                    val inputBuffer = codec.getInputBuffer(inputIndex)!!
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                    progressed = true
                }

                // Output keeps being drained after end-of-stream is queued: the codec still holds
                // decoded frames then, and stopping early drops the end of the recording.
                while (!outputDone && drainOutput(0)) progressed = true

                if (progressed) {
                    idleWaits = 0
                } else if (!outputDone) {
                    // Nothing to feed or take right now: let the codec work instead of spinning.
                    if (drainOutput(CODEC_WAIT_US)) {
                        idleWaits = 0
                    } else if (inputDone && ++idleWaits >= MAX_IDLE_WAITS_AFTER_INPUT) {
                        break // safety net for a codec that never reports its own end-of-stream
                    }
                }
            }

            resampler?.flush()?.takeIf { it.isNotEmpty() }?.let(onChunk)

            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding audio file in chunks: ${e.message}", e)
            return false
        } finally {
            startedCodec?.let {
                runCatching { it.stop() }
                runCatching { it.release() }
            }
            extractor.release()
        }
    }

    private fun resamplerFor(inputRate: Int, outputRate: Int): StreamingResampler? =
        if (inputRate == outputRate) null else StreamingResampler(inputRate, outputRate)

    private fun pcm16ToMonoFloat(buffer: ByteBuffer, channelCount: Int): FloatArray {
        val shorts = ShortArray(buffer.remaining() / 2)
        buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)

        val frames = shorts.size / channelCount
        val mono = FloatArray(frames)
        if (channelCount == 1) {
            for (i in 0 until frames) mono[i] = shorts[i] / 32768.0f
        } else {
            for (i in 0 until frames) {
                var sum = 0
                for (channel in 0 until channelCount) sum += shorts[i * channelCount + channel]
                mono[i] = sum / channelCount / 32768.0f
            }
        }
        return mono
    }

    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                return i
            }
        }
        return -1
    }

    private fun resampleAudio(samples: FloatArray, inputRate: Int, outputRate: Int): FloatArray {
        if (inputRate == outputRate) {
            return samples
        }

        val ratio = outputRate.toDouble() / inputRate.toDouble()
        val outputLength = (samples.size * ratio).toInt()
        val output = FloatArray(outputLength)

        for (i in output.indices) {
            val pos = i / ratio
            val left = pos.toInt()
            val right = left + 1

            output[i] = when {
                right >= samples.size -> samples[left]
                else -> {
                    val frac = pos - left
                    samples[left] * (1 - frac).toFloat() + samples[right] * frac.toFloat()
                }
            }
        }

        return output
    }

    private fun stereoToMono(samples: FloatArray, channelCount: Int): FloatArray {
        val monoLength = samples.size / channelCount
        val mono = FloatArray(monoLength)

        for (i in 0 until monoLength) {
            var sum = 0.0f
            for (ch in 0 until channelCount) {
                sum += samples[i * channelCount + ch]
            }
            mono[i] = sum / channelCount
        }

        return mono
    }
}
