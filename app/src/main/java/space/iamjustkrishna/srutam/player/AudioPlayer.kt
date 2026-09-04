package space.iamjustkrishna.srutam.player

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPosition: Int = 0,
    val duration: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentFilePath: String? = null,
    val speed: Float = 1.0f
)

class AudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var progressUpdateJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var shouldAutoPlayOnPrepared = false

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var currentFilePath: String? = null

    fun prepare(audioFile: File) {
        prepareInternal(audioFile, autoPlay = false)
    }

    fun prepareAndPlay(audioFile: File) {
        prepareInternal(audioFile, autoPlay = true)
    }

    private fun prepareInternal(audioFile: File, autoPlay: Boolean) {
        try {
            release()
            shouldAutoPlayOnPrepared = autoPlay

            currentFilePath = audioFile.absolutePath
            _playbackState.value = PlaybackState(
                isLoading = true,
                error = null,
                currentFilePath = currentFilePath
            )

            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                setOnPreparedListener {
                    _playbackState.value = _playbackState.value.copy(
                        isLoading = false,
                        duration = it.duration,
                        currentPosition = 0,
                        currentFilePath = currentFilePath
                    )
                    if (shouldAutoPlayOnPrepared) {
                        shouldAutoPlayOnPrepared = false
                        play()
                    }
                    Log.d(TAG, "Audio prepared: duration=${it.duration}ms")
                }
                setOnCompletionListener {
                    pause()
                    seekTo(0)
                    Log.d(TAG, "Playback completed")
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    _playbackState.value = _playbackState.value.copy(
                        isLoading = false,
                        isPlaying = false,
                        error = "Playback error: $what"
                    )
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing audio", e)
            _playbackState.value = _playbackState.value.copy(
                isLoading = false,
                error = "Failed to load audio: ${e.message}"
            )
        }
    }

    fun play() {
        try {
            mediaPlayer?.let { player ->
                if (!player.isPlaying) {
                    player.start()
                    _playbackState.value = _playbackState.value.copy(isPlaying = true)
                    startProgressUpdates()
                    Log.d(TAG, "Playback started")
                }
            } ?: run {
                Log.w(TAG, "MediaPlayer not initialized")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting playback", e)
            _playbackState.value = _playbackState.value.copy(
                isPlaying = false,
                error = "Playback failed: ${e.message}"
            )
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    Log.d(TAG, "Playback paused")
                }
                _playbackState.value = _playbackState.value.copy(isPlaying = false)
                stopProgressUpdates()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing playback", e)
        }
    }

    fun seekTo(position: Int) {
        try {
            mediaPlayer?.let { player ->
                val safePosition = position.coerceIn(0, player.duration)
                player.seekTo(safePosition)
                _playbackState.value = _playbackState.value.copy(currentPosition = safePosition)
                Log.d(TAG, "Seeked to position: $safePosition ms")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking", e)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                mediaPlayer?.let { player ->
                    val params = player.playbackParams
                    params.speed = speed
                    player.playbackParams = params
                }
            }
            _playbackState.value = _playbackState.value.copy(speed = speed)
            Log.d(TAG, "Playback speed set to: $speed")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting playback speed", e)
        }
    }

    fun togglePlayPause() {
        if (_playbackState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressUpdateJob = scope.launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                try {
                    val currentPos = mediaPlayer?.currentPosition ?: 0
                    _playbackState.value = _playbackState.value.copy(currentPosition = currentPos)
                    delay(100) // Update every 100ms
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating progress", e)
                    break
                }
            }
        }
    }

    private fun stopProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }

    fun release() {
        try {
            stopProgressUpdates()
            shouldAutoPlayOnPrepared = false
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            _playbackState.value = PlaybackState()
            Log.d(TAG, "AudioPlayer released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing player", e)
        }
    }

    companion object {
        private const val TAG = "AudioPlayer"
    }
}
