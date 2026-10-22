package space.iamjustkrishna.srutam.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * How loud the microphone is right now, 0..1, about ten times a second while recording, so a
 * screen can draw a waveform that moves with the voice. 0 when nothing is recording.
 */
object LiveLevel {
    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level.asStateFlow()

    /** Quieter than this reads 0 (room hum); this much louder reads full. In dBFS. */
    private const val FLOOR_DB = -50.0
    private const val RANGE_DB = 38.0

    /**
     * [rms] of a chunk of 16-bit PCM. Mapped on a decibel scale, like a level meter: a quiet voice
     * (about -38 dBFS) reads a third, normal speech half to three quarters. It rises at once and
     * falls back gently, so the bars follow speech without flickering.
     */
    internal fun update(rms: Double) {
        val db = 20 * Math.log10((rms / 32768.0).coerceAtLeast(1e-6))
        val target = ((db - FLOOR_DB) / RANGE_DB).coerceIn(0.0, 1.0).toFloat()
        val current = _level.value
        _level.value = if (target >= current) target else current + (target - current) * 0.35f
    }

    internal fun reset() {
        _level.value = 0f
    }
}
