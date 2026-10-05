package space.iamjustkrishna.srutam.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import space.iamjustkrishna.srutam.SrutamApplication
import space.iamjustkrishna.srutam.ai.AIProcessor
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import space.iamjustkrishna.srutam.player.AudioPlayer
import space.iamjustkrishna.srutam.repository.RecordingRepository
import space.iamjustkrishna.srutam.repository.RenameResult
import space.iamjustkrishna.srutam.service.AiProcessingWorker
import space.iamjustkrishna.srutam.utils.NetworkUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RecordingRepository
    private val aiProcessor: AIProcessor
    private val audioPlayer: AudioPlayer
    private val gson = Gson()

    private val _recording = MutableStateFlow<Recording?>(null)
    val recording: StateFlow<Recording?> = _recording.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isQueryLoading = MutableStateFlow(false)
    val isQueryLoading: StateFlow<Boolean> = _isQueryLoading.asStateFlow()

    // Expose audio player state
    val playbackState: StateFlow<space.iamjustkrishna.srutam.player.PlaybackState>
        get() = audioPlayer.playbackState

    private val database = (application as SrutamApplication).database
    private val insightDao = database.insightDao()

    private val _noteInsights = MutableStateFlow<List<space.iamjustkrishna.srutam.data.InsightEntity>>(emptyList())
    val noteInsights: StateFlow<List<space.iamjustkrishna.srutam.data.InsightEntity>> = _noteInsights.asStateFlow()

    init {
        repository = RecordingRepository(application.applicationContext, database.recordingDao())
        aiProcessor = AIProcessor(application)
        audioPlayer = AudioPlayer(application)
    }

    fun loadRecording(recordingId: Long) {
        viewModelScope.launch {
            repository.getRecordingByIdFlow(recordingId).collect { recording ->
                _recording.value = recording
                // Prepare audio player when recording is loaded
                recording?.audioFilePath?.let { filePath ->
                    val audioFile = File(filePath)
                    if (audioFile.exists()) {
                        audioPlayer.prepare(audioFile)
                    }
                }
            }
        }
        viewModelScope.launch {
            insightDao.getInsightsByRecordingIdFlow(recordingId).collect { insights ->
                _noteInsights.value = insights
            }
        }
    }

    fun toggleActionComplete(insight: space.iamjustkrishna.srutam.data.InsightEntity) {
        viewModelScope.launch {
            val newStatus = if (insight.status == space.iamjustkrishna.srutam.data.InsightStatus.COMPLETED) {
                space.iamjustkrishna.srutam.data.InsightStatus.OPEN
            } else {
                space.iamjustkrishna.srutam.data.InsightStatus.COMPLETED
            }
            val completedAt = if (newStatus == space.iamjustkrishna.srutam.data.InsightStatus.COMPLETED) System.currentTimeMillis() else null
            insightDao.updateActionStatus(insight.id, newStatus, completedAt)
        }
    }

    // Audio playback controls
    fun play() = audioPlayer.play()
    fun pause() = audioPlayer.pause()
    fun togglePlayPause() = audioPlayer.togglePlayPause()
    fun seekTo(position: Int) = audioPlayer.seekTo(position)
    fun setPlaybackSpeed(speed: Float) = audioPlayer.setPlaybackSpeed(speed)

    /** Renames the file and the title together; [onError] gets a message to show if that was not possible. */
    fun renameRecording(newName: String, onError: (String) -> Unit = {}) {
        val current = _recording.value ?: return
        viewModelScope.launch {
            when (val result = repository.renameRecording(current.audioFilePath, newName, current.duration, current.timestamp)) {
                is RenameResult.Renamed ->
                    _recording.value = current.copy(name = result.title, audioFilePath = result.newPath)
                is RenameResult.Rejected -> onError(result.message)
                RenameResult.Failed -> onError("Could not rename the file. Please try again.")
            }
        }
    }

    fun askQuestion(question: String) {
        val currentRecording = _recording.value
        val transcript = currentRecording?.transcript

        if (transcript.isNullOrBlank()) {
            addChatMessage(
                ChatMessage(
                    text = "Transcript not available yet. Please wait for processing to complete.",
                    isUser = false
                )
            )
            return
        }

        if (!NetworkUtils.isInternetAvailable(getApplication())) {
            addChatMessage(
                ChatMessage(
                    text = "AI Q&A requires an internet connection. The transcript is still available locally.",
                    isUser = false
                )
            )
            return
        }

        // Add user question
        addChatMessage(ChatMessage(text = question, isUser = true))
        _isQueryLoading.value = true

        viewModelScope.launch {
            try {
                val answer = aiProcessor.queryRecording(transcript, question, recordingId = _recording.value?.id)
                addChatMessage(ChatMessage(text = answer, isUser = false))
            } catch (e: Exception) {
                addChatMessage(
                    ChatMessage(
                        text = "Try again, or try using your own API key from Settings.",
                        isUser = false
                    )
                )
            } finally {
                _isQueryLoading.value = false
            }
        }
    }

    private fun addChatMessage(message: ChatMessage) {
        _chatMessages.value = _chatMessages.value + message
    }

    fun clearChat() {
        _chatMessages.value = emptyList()
    }

    fun generateAiSummary() {
        val currentRecording = _recording.value ?: return
        val audioFile = File(currentRecording.audioFilePath)
        if (!audioFile.exists()) {
            updateRecordingError("Audio file not found")
            return
        }

        AiProcessingWorker.enqueueProcessing(getApplication(), listOf(currentRecording.id))
    }

    fun deleteRecording() {
        val currentRecording = _recording.value ?: return
        viewModelScope.launch {
            try {
                if (audioPlayer.playbackState.value.currentFilePath == currentRecording.audioFilePath) {
                    audioPlayer.release()
                }
                repository.deleteRecording(currentRecording)
            } catch (e: Exception) {
                updateRecordingError("Failed to delete file. Try again.")
            }
        }
    }

    fun retryAiProcessing() {
        val currentRecording = _recording.value ?: return
        val audioFile = File(currentRecording.audioFilePath)
        if (!audioFile.exists()) {
            updateRecordingError("Audio file not found")
            return
        }

        AiProcessingWorker.enqueueProcessing(getApplication(), listOf(currentRecording.id))
    }

    fun reprocessWithAi() {
        val currentRecording = _recording.value ?: return
        val audioFile = File(currentRecording.audioFilePath)
        if (!audioFile.exists()) {
            updateRecordingError("Audio file not found")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val updated = currentRecording.copy(
                summary = null,
                wiifm = null,
                keyPoints = null,
                actionItems = null,
                isProcessing = true,
                aiStatus = if (currentRecording.transcript.isNullOrBlank()) {
                    RecordingAiStatus.TRANSCRIBING
                } else {
                    RecordingAiStatus.SUMMARY_PROCESSING
                },
                processingError = null
            )
            repository.updateRecording(updated)
            _recording.value = updated
            AiProcessingWorker.enqueueProcessing(getApplication(), listOf(currentRecording.id))
        }
    }

    fun updatePrivacy(recordingId: Long, isPrivate: Boolean) {
        viewModelScope.launch {
            database.recordingDao().updatePrivacy(recordingId, isPrivate)
            _recording.value = _recording.value?.copy(isPrivate = isPrivate)
            space.iamjustkrishna.srutam.cloud.CloudSyncManager.enqueueSyncForRecording(
                getApplication<Application>().applicationContext,
                recordingId
            )
        }
    }
    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }

    private fun updateRecordingError(message: String) {
        val currentRecording = _recording.value ?: return
        viewModelScope.launch {
            val updated = currentRecording.copy(
                isProcessing = false,
                aiStatus = RecordingAiStatus.ERROR,
                processingError = message
            )
            repository.updateRecording(updated)
            _recording.value = updated
        }
    }
}
