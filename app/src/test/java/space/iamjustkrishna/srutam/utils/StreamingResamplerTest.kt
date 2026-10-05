package space.iamjustkrishna.srutam.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

class StreamingResamplerTest {
    private fun tone(rate: Int, hz: Double, seconds: Double): FloatArray =
        FloatArray((rate * seconds).toInt()) { sin(2 * PI * hz * it / rate).toFloat() }

    private fun resample(input: FloatArray, from: Int, to: Int, chunk: Int = input.size): FloatArray {
        val resampler = StreamingResampler(from, to)
        val pieces = ArrayList<FloatArray>()
        var start = 0
        while (start < input.size) {
            val end = minOf(start + chunk, input.size)
            pieces += resampler.process(input.copyOfRange(start, end))
            start = end
        }
        pieces += resampler.flush()
        val result = FloatArray(pieces.sumOf { it.size })
        var position = 0
        for (piece in pieces) {
            piece.copyInto(result, position)
            position += piece.size
        }
        return result
    }

    // Peak gain of a sine, measured from RMS while ignoring the filter's start-up and tail.
    private fun gain(output: FloatArray, rate: Int): Double {
        val skip = rate / 5
        val middle = output.copyOfRange(skip, output.size - skip)
        return sqrt(middle.sumOf { it.toDouble() * it } / middle.size) * sqrt(2.0)
    }

    @Test fun speechBandTonesKeepTheirLevel() {
        for (hz in listOf(300.0, 1000.0, 4000.0, 6000.0, 7000.0)) {
            val output = resample(tone(44100, hz, 2.0), 44100, 16000)
            assertEquals("gain at $hz Hz", 1.0, gain(output, 16000), 0.03)
        }
    }

    @Test fun tonesAboveTheTargetNyquistAreRemoved() {
        // 10 and 12 kHz would fold to 6 and 4 kHz with plain interpolation.
        for (hz in listOf(8500.0, 10000.0, 12000.0, 15000.0)) {
            val output = resample(tone(44100, hz, 2.0), 44100, 16000)
            assertTrue("$hz Hz leaked through with gain ${gain(output, 16000)}", gain(output, 16000) < 0.01)
        }
    }

    @Test fun outputLengthFollowsTheRatio() {
        val output = resample(tone(44100, 1000.0, 3.0), 44100, 16000)
        assertEquals(48000.0, output.size.toDouble(), 2.0)
    }

    @Test fun chunkSizeDoesNotChangeTheResult() {
        val random = Random(1)
        val input = FloatArray(44100 * 3) { (random.nextGaussian() * 0.1).toFloat() + sin(it * 0.05).toFloat() * 0.3f }
        val whole = resample(input, 44100, 16000)
        for (chunk in listOf(1, 7, 1000, 4096)) {
            val pieces = resample(input, 44100, 16000, chunk)
            assertEquals("length with chunk $chunk", whole.size, pieces.size)
            var worst = 0f
            for (i in whole.indices) worst = maxOf(worst, abs(whole[i] - pieces[i]))
            assertTrue("chunk $chunk differs by $worst", worst < 1e-5f)
        }
    }

    @Test fun constantSignalStaysConstantAwayFromTheEdges() {
        val output = resample(FloatArray(44100) { 0.5f }, 44100, 16000)
        for (i in 4000 until output.size - 4000) {
            assertEquals(0.5f, output[i], 0.005f)
        }
    }

    @Test fun otherCommonRatesWork() {
        for ((from, to) in listOf(48000 to 16000, 22050 to 16000, 8000 to 16000, 16000 to 44100)) {
            val output = resample(tone(from, 1000.0, 2.0), from, to)
            assertEquals("gain for $from -> $to", 1.0, gain(output, to), 0.03)
            assertEquals(2.0 * to, output.size.toDouble(), 3.0)
        }
    }

    @Test fun oneMinuteOfAudioResamplesQuickly() {
        val input = tone(44100, 440.0, 60.0)
        val start = System.nanoTime()
        resample(input, 44100, 16000, 1024)
        val seconds = (System.nanoTime() - start) / 1e9
        assertTrue("took $seconds s", seconds < 2.0)
    }
}
