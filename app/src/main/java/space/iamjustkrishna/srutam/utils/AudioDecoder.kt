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

/**
 * Decodes M4A audio files to 16kHz mono PCM float array for speech recognition.
 * Uses MediaCodec for hardware-accelerated decoding when available.
 */
class AudioDecoder {
    companion object {
        private const val TAG = "AudioDecoder"
        private const val TARGET_SAMPLE_RATE = 16000
        private const val BUFFER_SIZE = 4096
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

    fun decodeAudioFileInChunks(
        audioFile: File,
        targetSampleRate: Int = TARGET_SAMPLE_RATE,
        onChunk: (FloatArray) -> Unit,
    ): Boolean {
        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(audioFile.absolutePath)

            val trackIndex = findAudioTrack(extractor)
            if (trackIndex < 0) {
                Log.e(TAG, "No audio track found in file: ${audioFile.name}")
                return false
            }

            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            Log.d(TAG, "Audio format - Sample Rate: $sampleRate Hz, Channels: $channelCount")

            val mimeType = format.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"
            val codec = MediaCodec.createDecoderByType(mimeType)
            codec.configure(format, null, null, 0)
            codec.start()

            val inputBuffers = codec.inputBuffers
            val outputBuffers = codec.outputBuffers
            val info = MediaCodec.BufferInfo()
            val resampler = if (sampleRate != targetSampleRate) {
                StreamingResampler(sampleRate, targetSampleRate)
            } else {
                null
            }

            var decodingComplete = false

            while (!decodingComplete) {
                val inputIndex = codec.dequeueInputBuffer(10000)
                if (inputIndex >= 0) {
                    val inputBuffer = inputBuffers[inputIndex]
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)

                    if (sampleSize < 0) {
                        codec.queueInputBuffer(
                            inputIndex,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
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

                    var chunk = bytesToFloatSamples(pcmSamples)
                    if (channelCount > 1) {
                        chunk = stereoToMono(chunk, channelCount)
                    }

                    if (chunk.isNotEmpty()) {
                        val outputChunk = resampler?.process(chunk) ?: chunk
                        if (outputChunk.isNotEmpty()) {
                            onChunk(outputChunk)
                        }
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

            resampler?.flush()?.takeIf { it.isNotEmpty() }?.let(onChunk)

            codec.stop()
            codec.release()
            extractor.release()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding audio file in chunks: ${e.message}", e)
            return false
        }
    }

    private fun bytesToFloatSamples(pcmSamples: ByteArray): FloatArray {
        val shorts = ByteBuffer.wrap(pcmSamples)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()

        val samples = FloatArray(shorts.limit())
        for (i in 0 until shorts.limit()) {
            samples[i] = shorts[i].toFloat() / 32768.0f
        }
        return samples
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

    private class StreamingResampler(
        private val inputRate: Int,
        private val outputRate: Int,
    ) {
        private val step = inputRate.toDouble() / outputRate.toDouble()
        private val source = ArrayList<Float>(TARGET_SAMPLE_RATE)
        private var baseIndex = 0
        private var nextPos = 0.0

        fun process(samples: FloatArray): FloatArray {
            for (sample in samples) {
                source.add(sample)
            }

            return drain(false)
        }

        fun flush(): FloatArray {
            return drain(true)
        }

        private fun drain(flush: Boolean): FloatArray {
            if (source.isEmpty()) {
                return FloatArray(0)
            }

            val output = ArrayList<Float>()

            while (true) {
                val relativePos = nextPos - baseIndex
                val leftIndex = relativePos.toInt()
                val rightIndex = leftIndex + 1

                if (leftIndex < 0) {
                    break
                }

                if (rightIndex >= source.size) {
                    if (flush && leftIndex < source.size) {
                        output.add(source[leftIndex])
                        nextPos += step
                        trimBuffer()
                        continue
                    }
                    break
                }

                val frac = relativePos - leftIndex
                val value = source[leftIndex] * (1 - frac).toFloat() + source[rightIndex] * frac.toFloat()
                output.add(value)
                nextPos += step
                trimBuffer()
            }

            return output.toFloatArray()
        }

        private fun trimBuffer() {
            val keepFrom = maxOf(0, nextPos.toInt() - baseIndex - 1)
            if (keepFrom > 0) {
                repeat(keepFrom) {
                    if (source.isNotEmpty()) {
                        source.removeAt(0)
                        baseIndex++
                    }
                }
            }
        }
    }
}
