package space.iamjustkrishna.srutam.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Authoritative singleton coordinator for audio recording sessions.
 * Guarantees that strictly one recording session can ever exist in the app at any given time.
 * All entry points (Floating Dock, Feed FAB, Tablet Shutter, Quick Settings Tile, Volume Buttons)
 * interact through this coordinator or have their actions synchronized through it.
 */
object RecordingCoordinator {

    private const val TAG = "RecordingCoordinator"

    sealed interface RecordingSessionState {
        object Idle : RecordingSessionState
        object Starting : RecordingSessionState
        data class Recording(val startTimeMs: Long, val filePath: String) : RecordingSessionState
        data class Paused(val durationMs: Long, val filePath: String) : RecordingSessionState
        object Stopping : RecordingSessionState
    }

    private val _state = MutableStateFlow<RecordingSessionState>(RecordingSessionState.Idle)
    val state: StateFlow<RecordingSessionState> = _state.asStateFlow()

    private val sessionLock = Any()

    private fun logD(msg: String) {
        try {
            Log.d(TAG, msg)
        } catch (_: Throwable) {}
    }

    private fun logW(msg: String) {
        try {
            Log.w(TAG, msg)
        } catch (_: Throwable) {}
    }

    private fun logE(msg: String, tr: Throwable? = null) {
        try {
            if (tr != null) Log.e(TAG, msg, tr) else Log.e(TAG, msg)
        } catch (_: Throwable) {}
    }

    /**
     * True if the coordinator is completely idle and ready to initiate a new recording session.
     */
    val isIdle: Boolean
        get() = _state.value is RecordingSessionState.Idle

    /**
     * True if a recording session is currently either active or paused.
     */
    val isRecording: Boolean
        get() = _state.value is RecordingSessionState.Recording || _state.value is RecordingSessionState.Paused

    /**
     * True if an active recording session is currently paused.
     */
    val isPaused: Boolean
        get() = _state.value is RecordingSessionState.Paused

    /**
     * Checks if a new recording session can be started.
     */
    fun canStart(): Boolean {
        synchronized(sessionLock) {
            return _state.value is RecordingSessionState.Idle
        }
    }

    /**
     * Safely requests starting a new recording session.
     * Returns true if the request was accepted and dispatched, false if rejected due to an existing session.
     */
    fun requestStart(context: Context): Boolean {
        synchronized(sessionLock) {
            if (_state.value !is RecordingSessionState.Idle) {
                logW("Rejecting requestStart: current state is ${_state.value}")
                return false
            }
            _state.value = RecordingSessionState.Starting
        }

        val intent = Intent(context, RecordingForegroundService::class.java).apply {
            action = RecordingForegroundService.ACTION_START_RECORDING
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            logD("Dispatched ACTION_START_RECORDING to RecordingForegroundService")
            return true
        } catch (e: Exception) {
            logE("Failed to start RecordingForegroundService", e)
            synchronized(sessionLock) {
                _state.value = RecordingSessionState.Idle
            }
            return false
        }
    }

    /**
     * Safely requests pausing the active recording session.
     */
    fun requestPause(context: Context) {
        synchronized(sessionLock) {
            if (_state.value !is RecordingSessionState.Recording) {
                logW("Cannot pause: current state is ${_state.value}")
                return
            }
        }
        val intent = Intent(context, RecordingForegroundService::class.java).apply {
            action = RecordingForegroundService.ACTION_PAUSE_RECORDING
        }
        context.startService(intent)
    }

    /**
     * Safely requests resuming a paused recording session.
     */
    fun requestResume(context: Context) {
        synchronized(sessionLock) {
            if (_state.value !is RecordingSessionState.Paused) {
                logW("Cannot resume: current state is ${_state.value}")
                return
            }
        }
        val intent = Intent(context, RecordingForegroundService::class.java).apply {
            action = RecordingForegroundService.ACTION_RESUME_RECORDING
        }
        context.startService(intent)
    }

    /**
     * Safely requests stopping and saving the current recording session.
     */
    fun requestStop(context: Context, deferAutoAi: Boolean = false) {
        synchronized(sessionLock) {
            if (_state.value is RecordingSessionState.Idle || _state.value is RecordingSessionState.Stopping) {
                logW("Cannot stop: session is already ${_state.value}")
                return
            }
            _state.value = RecordingSessionState.Stopping
        }
        val intent = Intent(context, RecordingForegroundService::class.java).apply {
            action = RecordingForegroundService.ACTION_STOP_RECORDING
            putExtra(RecordingForegroundService.EXTRA_DEFER_AUTO_AI, deferAutoAi)
        }
        context.startService(intent)
    }

    /**
     * Safely requests cancelling and discarding the current recording session.
     */
    fun requestCancel(context: Context) {
        synchronized(sessionLock) {
            if (_state.value is RecordingSessionState.Idle || _state.value is RecordingSessionState.Stopping) {
                logW("Cannot cancel: session is already ${_state.value}")
                return
            }
            _state.value = RecordingSessionState.Stopping
        }
        val intent = Intent(context, RecordingForegroundService::class.java).apply {
            action = RecordingForegroundService.ACTION_DELETE_RECORDING
        }
        context.startService(intent)
    }

    // =========================================================================
    // Callbacks invoked strictly by RecordingForegroundService
    // =========================================================================

    internal fun notifyRecordingStarted(startTimeMs: Long, filePath: String) {
        synchronized(sessionLock) {
            _state.value = RecordingSessionState.Recording(startTimeMs, filePath)
            logD("State transition -> Recording (file: $filePath)")
        }
    }

    internal fun notifyRecordingPaused(durationMs: Long, filePath: String) {
        synchronized(sessionLock) {
            _state.value = RecordingSessionState.Paused(durationMs, filePath)
            logD("State transition -> Paused (duration: $durationMs ms)")
        }
    }

    internal fun notifyRecordingResumed(startTimeMs: Long, filePath: String) {
        synchronized(sessionLock) {
            _state.value = RecordingSessionState.Recording(startTimeMs, filePath)
            logD("State transition -> Recording (resumed)")
        }
    }

    internal fun notifyRecordingEnded() {
        synchronized(sessionLock) {
            _state.value = RecordingSessionState.Idle
            logD("State transition -> Idle")
        }
    }
}
