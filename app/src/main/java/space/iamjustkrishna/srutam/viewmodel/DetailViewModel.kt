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

    init {
        val database = (application as SrutamApplication).database
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
    }

    // Audio playback controls
    fun play() = audioPlayer.play()

    fun pause() = audioPlayer.pause()

    fun togglePlayPause() = audioPlayer.togglePlayPause()

    fun seekTo(position: Int) = audioPlayer.seekTo(position)

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
                val answer = aiProcessor.queryRecording(transcript, question)
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

        viewModelScope.launch {
            try {
                val hasInternet = NetworkUtils.isInternetAvailable(getApplication())
                if (!hasInternet) {
                    val pendingRecording = currentRecording.copy(
                        isProcessing = false,
                        aiStatus = RecordingAiStatus.SUMMARY_PENDING_OFFLINE,
                        processingError = null
                    )
                    repository.updateRecording(pendingRecording)
                    _recording.value = pendingRecording
                    return@launch
                }

                val processingRecording = currentRecording.copy(
                    isProcessing = true,
                    aiStatus = if (currentRecording.transcript.isNullOrBlank()) {
                        RecordingAiStatus.TRANSCRIBING
                    } else {
                        RecordingAiStatus.SUMMARY_PROCESSING
                    },
                    summary = null,
                    keyPoints = null,
                    actionItems = null,
                    wiifm = null,
                    processingError = null
                )
                repository.updateRecording(processingRecording)
                _recording.value = processingRecording

                val updated = withContext(Dispatchers.IO) {
                    val transcript = currentRecording.transcript
                        ?: aiProcessor.transcribeAudio(audioFile)

                    val workingRecording = processingRecording.copy(
                        transcript = transcript,
                        isProcessing = true,
                        aiStatus = RecordingAiStatus.SUMMARY_PROCESSING,
                        summary = null,
                        keyPoints = null,
                        actionItems = null,
                        wiifm = null,
                        processingError = null
                    )
                    repository.updateRecording(workingRecording)
                    _recording.value = workingRecording

                    val insights = aiProcessor.generateInsights(transcript)
                    workingRecording.copy(
                        transcript = transcript,
                        summary = insights.summary,
                        keyPoints = gson.toJson(insights.keyPoints),
                        actionItems = gson.toJson(insights.actionItems),
                        wiifm = insights.wiifm,
                        isProcessing = false,
                        aiStatus = RecordingAiStatus.READY,
                        processingError = null
                    )
                }

                repository.updateRecording(updated)
                _recording.value = updated
            } catch (e: Exception) {
                updateRecordingError(e.message ?: "Failed to generate summary")
            }
        }
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

        viewModelScope.launch {
            try {
                val hasInternet = NetworkUtils.isInternetAvailable(getApplication())
                if (!hasInternet) {
                    updateRecordingError("Internet connection required to retry AI processing")
                    return@launch
                }

                // If transcript is missing, redo full pipeline
                if (currentRecording.transcript.isNullOrBlank()) {
                    val processingRecording = currentRecording.copy(
                        isProcessing = true,
                        aiStatus = RecordingAiStatus.TRANSCRIBING,
                        summary = null,
                        keyPoints = null,
                        actionItems = null,
                        wiifm = null,
                        processingError = null
                    )
                    repository.updateRecording(processingRecording)
                    _recording.value = processingRecording

                    val updated = withContext(Dispatchers.IO) {
                        val transcript = aiProcessor.transcribeAudio(audioFile)

                        val withTranscript = processingRecording.copy(
                            transcript = transcript,
                            isProcessing = true,
                            aiStatus = RecordingAiStatus.SUMMARY_PROCESSING
                        )
                        repository.updateRecording(withTranscript)
                        _recording.value = withTranscript

                        val insights = aiProcessor.generateInsights(transcript)
                        withTranscript.copy(
                            summary = insights.summary,
                            keyPoints = gson.toJson(insights.keyPoints),
                            actionItems = gson.toJson(insights.actionItems),
                            wiifm = insights.wiifm,
                            isProcessing = false,
                            aiStatus = RecordingAiStatus.READY,
                            processingError = null
                        )
                    }

                    repository.updateRecording(updated)
                    _recording.value = updated
                } else {
                    // Transcript exists, just retry summary generation
                    val processingRecording = currentRecording.copy(
                        isProcessing = true,
                        aiStatus = RecordingAiStatus.SUMMARY_PROCESSING,
                        summary = null,
                        keyPoints = null,
                        actionItems = null,
                        wiifm = null,
                        processingError = null
                    )
                    repository.updateRecording(processingRecording)
                    _recording.value = processingRecording

                    val updated = withContext(Dispatchers.IO) {
                        val insights = aiProcessor.generateInsights(currentRecording.transcript!!)
                        processingRecording.copy(
                            summary = insights.summary,
                            keyPoints = gson.toJson(insights.keyPoints),
                            actionItems = gson.toJson(insights.actionItems),
                            wiifm = insights.wiifm,
                            isProcessing = false,
                            aiStatus = RecordingAiStatus.READY,
                            processingError = null
                        )
                    }

                    repository.updateRecording(updated)
                    _recording.value = updated
                }
            } catch (e: Exception) {
                updateRecordingError("Retry failed: ${e.message ?: "Unknown error"}")
            }
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
