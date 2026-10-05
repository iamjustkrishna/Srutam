package space.iamjustkrishna.srutam.utils

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Streaming sample-rate converter built on a Kaiser-windowed sinc low-pass filter, so content above
 * the target Nyquist frequency is removed instead of folding back into the speech band (plain
 * linear interpolation does the latter). Output sample m corresponds to input position m * (in / out),
 * so there is no delay. Feed audio with [process] in any chunk sizes, then call [flush] once.
 *
 * The filter is precomputed per fractional position (polyphase), so converting is a plain
 * multiply-accumulate over [taps] input samples per output sample.
 */
class StreamingResampler(inputRate: Int, outputRate: Int) {
    private val step = inputRate.toDouble() / outputRate
    private val numerator: Long
    private val denominator: Long
    private val exact: Boolean
    private val phases: Int
    private val reach: Int
    private val taps: Int
    private val coefficients: FloatArray

    private var buffer = FloatArray(0)
    private var size = 0
    private var baseIndex = 0L
    private var realCount = 0L
    private var outputCount = 0L

    init {
        val divisor = gcd(inputRate, outputRate)
        numerator = (inputRate / divisor).toLong()
        denominator = (outputRate / divisor).toLong()
        exact = denominator <= MAX_PHASES
        phases = if (exact) denominator.toInt() else MAX_PHASES

        val transition = TRANSITION_HZ / inputRate
        val cutoff = (minOf(inputRate, outputRate) / 2.0 - TRANSITION_HZ / 2) / inputRate
        val halfWidth = KAISER_TRANSITION_TAPS / transition / 2
        reach = ceil(halfWidth).toInt()
        taps = 2 * reach
        coefficients = buildCoefficients(cutoff, halfWidth, reach, phases)
    }

    fun process(samples: FloatArray): FloatArray {
        append(samples, real = true)
        return produce(flushing = false)
    }

    fun flush(): FloatArray {
        append(FloatArray(reach + 2), real = false)
        return produce(flushing = true)
    }

    private fun append(samples: FloatArray, real: Boolean) {
        if (size + samples.size > buffer.size) {
            buffer = buffer.copyOf(max(buffer.size * 2, size + samples.size))
        }
        System.arraycopy(samples, 0, buffer, size, samples.size)
        size += samples.size
        if (real) realCount += samples.size
    }

    private fun produce(flushing: Boolean): FloatArray {
        var out = FloatArray(((baseIndex + size - outputCount * step) / step).toInt().coerceAtLeast(0) + 2)
        var count = 0

        while (true) {
            val n0 = integerPart(outputCount)
            val phase = phase(outputCount)
            if (flushing && n0 + phase.toDouble() / phases >= realCount) break
            if (n0 + reach >= baseIndex + size) break // not enough input yet

            var index = (n0 - reach + 1 - baseIndex).toInt()
            var tap = 0
            if (index < 0) { // before the start of the stream: silence
                tap = -index
                index = 0
            }
            val row = phase * taps
            var sum = 0f
            while (tap < taps) {
                sum += buffer[index++] * coefficients[row + tap]
                tap++
            }

            if (count == out.size) out = out.copyOf(out.size * 2)
            out[count++] = sum
            outputCount++
        }

        discardConsumedInput()
        return if (count == out.size) out else out.copyOf(count)
    }

    // Input index at or just below the position of output m.
    private fun integerPart(m: Long): Long {
        if (exact) return m * numerator / denominator
        val position = m * step
        val n0 = floor(position).toLong()
        return if (quantizedPhase(position, n0) == phases) n0 + 1 else n0
    }

    // Index of the filter row for output m (its fractional input position, in 1/phases steps).
    private fun phase(m: Long): Int {
        if (exact) return (m * numerator % denominator).toInt()
        val position = m * step
        val phase = quantizedPhase(position, floor(position).toLong())
        return if (phase == phases) 0 else phase
    }

    private fun quantizedPhase(position: Double, n0: Long): Int = ((position - n0) * phases).roundToInt()

    private fun discardConsumedInput() {
        val needFrom = integerPart(outputCount) - reach + 1
        val drop = (needFrom - baseIndex).coerceIn(0L, size.toLong()).toInt()
        if (drop > 0) {
            System.arraycopy(buffer, drop, buffer, 0, size - drop)
            size -= drop
            baseIndex += drop
        }
    }

    private companion object {
        // Width of the filter's transition band at the output rate: passband up to ~7.6 kHz and
        // >= 80 dB attenuation from 8 kHz up for a 16 kHz output.
        const val TRANSITION_HZ = 800.0
        const val KAISER_BETA = 8.0
        // Kaiser design rule: taps ~ 5.0 / transition (normalized) for ~80 dB stopband attenuation.
        const val KAISER_TRANSITION_TAPS = 5.0
        // All common rates resolve to far fewer phases (44.1 kHz -> 16 kHz needs 160); odd ratios
        // fall back to quantized positions, which are accurate to better than 1/2048 of a sample.
        const val MAX_PHASES = 1024

        fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

        // coefficients[phase * taps + tap] weighs input index (n0 - reach + 1 + tap) for an output at
        // n0 + phase / phases. Each row is normalized to unit gain so DC passes unchanged.
        fun buildCoefficients(cutoff: Double, halfWidth: Double, reach: Int, phases: Int): FloatArray {
            val taps = 2 * reach
            val kaiserDenominator = besselI0(KAISER_BETA)
            val result = FloatArray(phases * taps)
            for (phase in 0 until phases) {
                val fraction = phase.toDouble() / phases
                val row = DoubleArray(taps) { tap ->
                    val x = abs((tap - reach + 1) - fraction)
                    val u = x / halfWidth
                    if (u >= 1.0) {
                        0.0
                    } else {
                        val t = 2 * cutoff * x
                        val sinc = if (t == 0.0) 1.0 else sin(PI * t) / (PI * t)
                        2 * cutoff * sinc * besselI0(KAISER_BETA * sqrt(1 - u * u)) / kaiserDenominator
                    }
                }
                val gain = row.sum()
                for (tap in 0 until taps) result[phase * taps + tap] = (row[tap] / gain).toFloat()
            }
            return result
        }

        fun besselI0(x: Double): Double {
            var sum = 1.0
            var term = 1.0
            var k = 1
            while (term > 1e-12 * sum) {
                term *= (x * x) / (4.0 * k * k)
                sum += term
                k++
            }
            return sum
        }
    }
}
