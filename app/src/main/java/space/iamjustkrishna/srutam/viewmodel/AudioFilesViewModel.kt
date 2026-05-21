package space.iamjustkrishna.srutam.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import space.iamjustkrishna.srutam.SrutamApplication
import space.iamjustkrishna.srutam.ai.AIProcessor
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import space.iamjustkrishna.srutam.repository.RecordingRepository
import space.iamjustkrishna.srutam.utils.AudioFileInfo
import space.iamjustkrishna.srutam.utils.AudioFileReader
import space.iamjustkrishna.srutam.utils.NetworkUtils
import space.iamjustkrishna.srutam.utils.RecordingNameFormatter
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AudioFilesViewModel(application: Application) : AndroidViewModel(application) {

    private val _audioFiles = MutableStateFlow<List<AudioFileInfo>>(emptyList())
    val audioFiles: StateFlow<List<AudioFileInfo>> = _audioFiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _processingError = MutableStateFlow<String?>(null)
    val processingError: StateFlow<String?> = _processingError.asStateFlow()

    private val _recordingsByPath = MutableStateFlow<Map<String, Recording>>(emptyMap())
    val recordingsByPath: StateFlow<Map<String, Recording>> = _recordingsByPath.asStateFlow()

    private val aiProcessor: AIProcessor
    private val repository: RecordingRepository
    private val gson = Gson()

    init {
        val database = (application as SrutamApplication).database
        repository = RecordingRepository(application.applicationContext, database.recordingDao())
        aiProcessor = AIProcessor(application)

        observeRecordings()
        loadAudioFiles()
    }

    val audioPlayer = space.iamjustkrishna.srutam.player.AudioPlayer(application)

    fun playAudio(audioFile: AudioFileInfo) {
        audioPlayer.prepareAndPlay(File(audioFile.filePath))
    }

    fun loadAudioFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val files = AudioFileReader.getAudioFiles()
            _audioFiles.value = files
            _isLoading.value = false
        }
    }

    private fun observeRecordings() {
        viewModelScope.launch {
            repository.allRecordings.collectLatest { recordings ->
                _recordingsByPath.value = recordings.associateBy { it.audioFilePath }
            }
        }
    }

    fun deleteAudioFile(audioFile: AudioFileInfo) {
        // Optimistic UI update to remove item immediately
        _audioFiles.value = _audioFiles.value.filterNot { it.filePath == audioFile.filePath }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (audioPlayer.playbackState.value.currentFilePath == audioFile.filePath) {
                    withContext(Dispatchers.Main) {
                        audioPlayer.release()
                    }
                }

                // Delete storage and associated DB record together
                val recording = repository.getRecordingByPath(audioFile.filePath)
                if (recording != null) {
                    try {
                        repository.deleteRecording(recording)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting audio file: ${audioFile.filePath}", e)
                        _processingError.value = "Failed to delete file. Try again."
                    }
                } else {
                    val deleted = space.iamjustkrishna.srutam.utils.AudioStorage
                        .deleteAudioFile(getApplication(), audioFile.filePath)
                    if (!deleted) {
                        Log.w(TAG, "Could not delete audio file: ${audioFile.filePath}")
                        _processingError.value = "Failed to delete file. Try again."
                    }
                }

                // Reload files to ensure consistency with storage
                loadAudioFiles()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting audio file", e)
                _processingError.value = "Failed to delete file. Try again."
                loadAudioFiles()
            }
        }
    }

    fun renameRecording(audioFile: AudioFileInfo, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val trimmedName = newName.trim()
                if (trimmedName.isBlank()) {
                    _processingError.value = "Failed to rename: name cannot be empty"
                    return@launch
                }

                val currentFile = File(audioFile.filePath)
                if (!currentFile.exists()) {
                    _processingError.value = "Failed to rename: file not found"
                    return@launch
                }

                val newFileName = if (trimmedName.endsWith(".m4a", ignoreCase = true)) {
                    trimmedName
                } else {
                    "$trimmedName.m4a"
                }
                val newFile = File(currentFile.parentFile, newFileName)
                if (newFile.exists()) {
                    _processingError.value = "Failed to rename: a file with that name already exists"
                    return@launch
                }

                val renamed = currentFile.renameTo(newFile)
                if (!renamed) {
                    _processingError.value = "Failed to rename: could not rename file"
                    return@launch
                }

                // Update DB record if present
                val recording = repository.getRecordingByPath(audioFile.filePath)
                if (recording != null) {
                    repository.updateRecording(
                        recording.copy(
                            name = trimmedName,
                            audioFilePath = newFile.absolutePath
                        )
                    )
                } else {
                    Log.w(TAG, "Recording not found in database: ${audioFile.filePath}")
                }

                // Optimistic UI update for immediate feedback
                _audioFiles.value = _audioFiles.value.map { file ->
                    if (file.filePath == audioFile.filePath) {
                        file.copy(
                            filePath = newFile.absolutePath,
                            fileName = newFile.name,
                            timestamp = newFile.lastModified(),
                            sizeBytes = newFile.length()
                        )
                    } else {
                        file
                    }
                }

                // Reload to ensure consistency with storage
                loadAudioFiles()
            } catch (e: Exception) {
                Log.e(TAG, "Error renaming recording", e)
                _processingError.value = "Failed to rename: ${e.message}"
            }
        }
    }

    /**
     * Process a recording with AI.
     * Transcription is always local. Summary is only generated when internet is available.
     */
    fun processRecordingForAI(audioFile: AudioFileInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            var recording: Recording? = null
            
            try {
                Log.d(TAG, "Starting AI processing for: ${audioFile.fileName}")
                
                // Step 1: Get or create database recording
                // First try to find by file path
                recording = repository.getRecordingByPath(audioFile.filePath)
                
                if (recording == null) {
                    // Create new recording in database
                    val newRecording = Recording(
                        audioFilePath = audioFile.filePath,
                        duration = audioFile.duration,
                        name = RecordingNameFormatter.displayName(
                            fileName = audioFile.fileName,
                            timestamp = audioFile.timestamp
                        )
                    )
                    val recordingId = repository.insertRecording(newRecording)
                    recording = newRecording.copy(id = recordingId)
                    Log.d(TAG, "Created new recording in database with ID: $recordingId")
                }
                
                // Step 2: Mark as processing
                Log.d(TAG, "Marking recording as processing...")
                repository.updateRecording(
                    recording.copy(
                        isProcessing = true,
                        aiStatus = RecordingAiStatus.TRANSCRIBING,
                        summary = null,
                        keyPoints = null,
                        actionItems = null,
                        wiifm = null,
                        processingError = null
                    )
                )
                
                // Step 3: Transcribe locally
                Log.d(TAG, "Processing audio file: ${audioFile.filePath}")
                val audioFileObj = File(audioFile.filePath)
                if (!audioFileObj.exists()) {
                    throw Exception("Audio file not found: ${audioFileObj.absolutePath}")
                }
                
                val transcript = aiProcessor.transcribeAudio(audioFileObj)
                Log.d(TAG, "Local transcription complete. Transcript length: ${transcript.length}")
                recording = recording.copy(transcript = transcript)
                
                val hasInternet = NetworkUtils.isInternetAvailable(getApplication())
                if (hasInternet) {
                    Log.d(TAG, "Internet available, generating AI summary...")
                    repository.updateRecording(
                        recording.copy(
                            isProcessing = true,
                            aiStatus = RecordingAiStatus.SUMMARY_PROCESSING,
                            summary = null,
                            keyPoints = null,
                            actionItems = null,
                            wiifm = null,
                            processingError = null
                        )
                    )

                    val insights = aiProcessor.generateInsights(transcript)
                    repository.updateRecording(
                        recording.copy(
                            summary = insights.summary,
                            keyPoints = gson.toJson(insights.keyPoints),
                            actionItems = gson.toJson(insights.actionItems),
                            wiifm = insights.wiifm,
                            isProcessing = false,
                            aiStatus = RecordingAiStatus.READY,
                            processingError = null
                        )
                    )
                } else {
                    Log.d(TAG, "Internet unavailable. Transcript stored and summary marked pending.")
                    repository.updateRecording(
                        recording.copy(
                            summary = null,
                            keyPoints = null,
                            actionItems = null,
                            wiifm = null,
                            isProcessing = false,
                            aiStatus = RecordingAiStatus.SUMMARY_PENDING_OFFLINE,
                            processingError = null
                        )
                    )
                }
                
                Log.d(TAG, "AI processing complete!")
                _processingError.value = null
                
                // Reload files to reflect UI changes
                loadAudioFiles()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing recording", e)
                _processingError.value = "Processing failed: ${e.message}"
                
                // Try to update database with error state
                try {
                    if (recording != null) {
                        repository.updateRecording(recording.copy(
                            isProcessing = false,
                            aiStatus = RecordingAiStatus.ERROR,
                            processingError = e.message ?: "Unknown error"
                        ))
                    }
                } catch (dbError: Exception) {
                    Log.e(TAG, "Failed to update error state in database", dbError)
                }
                
                loadAudioFiles()
            }
        }
    }

    fun generateSummaryForRecording(audioFile: AudioFileInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val recording = repository.getRecordingByPath(audioFile.filePath)
                if (recording == null) {
                    processRecordingForAI(audioFile)
                    return@launch
                }

                val transcript = recording.transcript
                if (transcript.isNullOrBlank()) {
                    processRecordingForAI(audioFile)
                    return@launch
                }

                if (!NetworkUtils.isInternetAvailable(getApplication())) {
                    repository.updateRecording(
                        recording.copy(
                            aiStatus = RecordingAiStatus.SUMMARY_PENDING_OFFLINE,
                            processingError = null
                        )
                    )
                    loadAudioFiles()
                    return@launch
                }

                repository.updateRecording(
                    recording.copy(
                        isProcessing = true,
                        aiStatus = RecordingAiStatus.SUMMARY_PROCESSING,
                        summary = null,
                        keyPoints = null,
                        actionItems = null,
                        wiifm = null,
                        processingError = null
                    )
                )

                val insights = aiProcessor.generateInsights(transcript)
                repository.updateRecording(
                    recording.copy(
                        summary = insights.summary,
                        keyPoints = gson.toJson(insights.keyPoints),
                        actionItems = gson.toJson(insights.actionItems),
                        wiifm = insights.wiifm,
                        isProcessing = false,
                        aiStatus = RecordingAiStatus.READY,
                        processingError = null
                    )
                )
                loadAudioFiles()
            } catch (e: Exception) {
                Log.e(TAG, "Error generating summary", e)
                val recording = repository.getRecordingByPath(audioFile.filePath)
                if (recording != null) {
                    repository.updateRecording(
                        recording.copy(
                            isProcessing = false,
                            aiStatus = RecordingAiStatus.ERROR,
                            processingError = e.message ?: "Failed to generate summary"
                        )
                    )
                }
                loadAudioFiles()
            }
        }
    }

    fun getOrCreateRecordingId(audioFile: AudioFileInfo, onResult: (Long) -> Unit) {
        viewModelScope.launch {
            try {
                val recordingId = withContext(Dispatchers.IO) {
                    val existingRecording = repository.getRecordingByPath(audioFile.filePath)
                    if (existingRecording != null) {
                        existingRecording.id
                    } else {
                        repository.insertRecording(
                            Recording(
                                audioFilePath = audioFile.filePath,
                                duration = audioFile.duration,
                                name = RecordingNameFormatter.displayName(
                                    fileName = audioFile.fileName,
                                    timestamp = audioFile.timestamp
                                )
                            )
                        )
                    }
                }
                onResult(recordingId)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting recording ID", e)
            }
        }
    }

    fun retryAiProcessing(recording: Recording) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Retrying AI processing for recording ID: ${recording.id}")
                
                val audioFile = File(recording.audioFilePath)
                if (!audioFile.exists()) {
                    throw Exception("Audio file not found: ${recording.audioFilePath}")
                }

                // If transcript is missing, redo full pipeline
                if (recording.transcript.isNullOrBlank()) {
                    Log.d(TAG, "No transcript found, restarting full AI pipeline")
                    repository.updateRecording(
                        recording.copy(
                            isProcessing = true,
                            aiStatus = RecordingAiStatus.TRANSCRIBING,
                            summary = null,
                            keyPoints = null,
                            actionItems = null,
                            wiifm = null,
                            processingError = null
                        )
                    )

                    val transcript = aiProcessor.transcribeAudio(audioFile)
                    Log.d(TAG, "Transcription complete. Transcript length: ${transcript.length}")

                    val hasInternet = NetworkUtils.isInternetAvailable(getApplication())
                    if (hasInternet) {
                        Log.d(TAG, "Internet available, generating AI summary...")
                        repository.updateRecording(
                            recording.copy(
                                isProcessing = true,
                                aiStatus = RecordingAiStatus.SUMMARY_PROCESSING,
                                transcript = transcript,
                                summary = null,
                                keyPoints = null,
                                actionItems = null,
                                wiifm = null,
                                processingError = null
                            )
                        )

                        val insights = aiProcessor.generateInsights(transcript)
                        repository.updateRecording(
                            recording.copy(
                                transcript = transcript,
                                summary = insights.summary,
                                keyPoints = gson.toJson(insights.keyPoints),
                                actionItems = gson.toJson(insights.actionItems),
                                wiifm = insights.wiifm,
                                isProcessing = false,
                                aiStatus = RecordingAiStatus.READY,
                                processingError = null
                            )
                        )
                    } else {
                        Log.d(TAG, "Internet unavailable. Transcript stored, summary marked pending.")
                        repository.updateRecording(
                            recording.copy(
                                transcript = transcript,
                                summary = null,
                                keyPoints = null,
                                actionItems = null,
                                wiifm = null,
                                isProcessing = false,
                                aiStatus = RecordingAiStatus.SUMMARY_PENDING_OFFLINE,
                                processingError = null
                            )
                        )
                    }
                } else {
                    // Transcript exists, just retry summary generation
                    Log.d(TAG, "Transcript exists, retrying summary generation only")
                    val hasInternet = NetworkUtils.isInternetAvailable(getApplication())
                    if (!hasInternet) {
                        Log.w(TAG, "Internet not available, cannot retry summary generation")
                        _processingError.value = "Internet connection required to generate AI summary"
                        return@launch
                    }

                    repository.updateRecording(
                        recording.copy(
                            isProcessing = true,
                            aiStatus = RecordingAiStatus.SUMMARY_PROCESSING,
                            summary = null,
                            keyPoints = null,
                            actionItems = null,
                            wiifm = null,
                            processingError = null
                        )
                    )

                    val insights = aiProcessor.generateInsights(recording.transcript!!)
                    repository.updateRecording(
                        recording.copy(
                            summary = insights.summary,
                            keyPoints = gson.toJson(insights.keyPoints),
                            actionItems = gson.toJson(insights.actionItems),
                            wiifm = insights.wiifm,
                            isProcessing = false,
                            aiStatus = RecordingAiStatus.READY,
                            processingError = null
                        )
                    )
                }

                Log.d(TAG, "AI retry processing complete!")
                _processingError.value = null
                loadAudioFiles()

            } catch (e: Exception) {
                Log.e(TAG, "Error retrying AI processing", e)
                _processingError.value = "Retry failed: ${e.message}"

                try {
                    repository.updateRecording(
                        recording.copy(
                            isProcessing = false,
                            aiStatus = RecordingAiStatus.ERROR,
                            processingError = e.message ?: "Unknown error during retry"
                        )
                    )
                } catch (dbError: Exception) {
                    Log.e(TAG, "Failed to update error state in database", dbError)
                }

                loadAudioFiles()
            }
        }
    }

    fun deleteMultipleAudioFiles(audioFiles: List<AudioFileInfo>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                for (audioFile in audioFiles) {
                    try {
                        if (audioPlayer.playbackState.value.currentFilePath == audioFile.filePath) {
                            withContext(Dispatchers.Main) {
                                audioPlayer.release()
                            }
                        }

                        // Delete storage and associated DB record together
                        val recording = repository.getRecordingByPath(audioFile.filePath)
                        if (recording != null) {
                            repository.deleteRecording(recording)
                        } else {
                            val deleted = space.iamjustkrishna.srutam.utils.AudioStorage
                                .deleteAudioFile(getApplication(), audioFile.filePath)
                            if (!deleted) {
                                Log.w(TAG, "Could not delete audio file: ${audioFile.filePath}")
                                _processingError.value = "Failed to delete some files."
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting audio file: ${audioFile.filePath}", e)
                        _processingError.value = "Failed to delete some files."
                    }
                }

                // Reload files to ensure consistency with storage
                loadAudioFiles()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting multiple audio files", e)
                _processingError.value = "Failed to delete files. Try again."
                loadAudioFiles()
            }
        }
    }

    fun undoDeleteMultipleAudioFiles(audioFiles: List<AudioFileInfo>) {
        // Optimistically add items back to the list
        _audioFiles.value = (_audioFiles.value + audioFiles).distinctBy { it.filePath }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Reload to ensure consistency
                loadAudioFiles()
            } catch (e: Exception) {
                Log.e(TAG, "Error undoing delete", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }

    companion object {
        private const val TAG = "AudioFilesViewModel"
    }
}
