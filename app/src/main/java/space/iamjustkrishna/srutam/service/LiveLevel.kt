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

    /** RMS of a chunk of 16-bit PCM at which the level reads full: a clearly raised voice. */
    private const val FULL_RMS = 5000.0

    internal fun update(rms: Double) {
        _level.value = (Math.sqrt(rms / FULL_RMS)).coerceIn(0.0, 1.0).toFloat()
    }

    internal fun reset() {
        _level.value = 0f
    }
}
