package space.iamjustkrishna.srutam.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import space.iamjustkrishna.srutam.SrutamApplication
import space.iamjustkrishna.srutam.ai.AIProcessor
import space.iamjustkrishna.srutam.ai.BM25SearchEngine
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.InsightKind
import space.iamjustkrishna.srutam.data.InsightStatus
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import space.iamjustkrishna.srutam.repository.RecordingRepository
import space.iamjustkrishna.srutam.ui.screens.formatDate
import space.iamjustkrishna.srutam.utils.AppPreferences
import space.iamjustkrishna.srutam.utils.AudioFileInfo
import space.iamjustkrishna.srutam.utils.AudioFileReader
import space.iamjustkrishna.srutam.utils.NetworkUtils
import space.iamjustkrishna.srutam.utils.RecordingNameFormatter
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ThemeCluster(
    val key: String,
    val title: String,
    val noteCount: Int,
    val noteIds: List<Long>,
    val noteNames: List<String>,
    val sampleSnippets: List<String> = emptyList()
)

class AudioFilesViewModel(application: Application) : AndroidViewModel(application) {

    private val _audioFiles = MutableStateFlow<List<AudioFileInfo>>(emptyList())
    val audioFiles: StateFlow<List<AudioFileInfo>> = _audioFiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _processingError = MutableStateFlow<String?>(null)
    val processingError: StateFlow<String?> = _processingError.asStateFlow()

    private val _recordingsByPath = MutableStateFlow<Map<String, Recording>>(emptyMap())
    val recordingsByPath: StateFlow<Map<String, Recording>> = _recordingsByPath.asStateFlow()

    private val database = (application as space.iamjustkrishna.srutam.SrutamApplication).database
    private val insightDao = database.insightDao()

    val allInsights: StateFlow<List<space.iamjustkrishna.srutam.data.InsightEntity>> = insightDao.getAllInsightsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeActions: StateFlow<List<space.iamjustkrishna.srutam.data.InsightEntity>> = insightDao.getActiveActionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allIdeas: StateFlow<List<space.iamjustkrishna.srutam.data.InsightEntity>> = insightDao.getIdeasFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDecisions: StateFlow<List<space.iamjustkrishna.srutam.data.InsightEntity>> = insightDao.getDecisionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedActionsCount: StateFlow<Int> = insightDao.getArchivedActionsCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _themeClusters = MutableStateFlow<List<ThemeCluster>>(emptyList())
    val themeClusters: StateFlow<List<ThemeCluster>> = _themeClusters.asStateFlow()

    private val aiProcessor: AIProcessor
    private val repository: RecordingRepository
    private val gson = Gson()

    init {
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
                computeThemeClusters(recordings)
            }
        }
        syncExistingRecordingsToInsights()
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
                        insightDao.deleteInsightsByRecordingId(recording.id)
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
                    val updatedName = if (recording.name.isBlank() || recording.name == "Voice Note" || recording.name.startsWith("Voice note", ignoreCase = true)) {
                        insights.title?.takeIf { it.isNotBlank() } ?: recording.name
                    } else {
                        recording.name
                    }
                    saveInsightsToRoom(recording.id, updatedName, recording.timestamp, insights)
                    repository.updateRecording(
                        recording.copy(
                            name = updatedName,
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
                val updatedName = if (recording.name.isBlank() || recording.name == "Voice Note" || recording.name.startsWith("Voice note", ignoreCase = true)) {
                    insights.title?.takeIf { it.isNotBlank() } ?: recording.name
                } else {
                    recording.name
                }
                saveInsightsToRoom(recording.id, updatedName, recording.timestamp, insights)
                repository.updateRecording(
                    recording.copy(
                        name = updatedName,
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
                    val updatedName = if (recording.name.isBlank() || recording.name == "Voice Note" || recording.name.startsWith("Voice note", ignoreCase = true)) {
                        insights.title?.takeIf { it.isNotBlank() } ?: recording.name
                    } else {
                        recording.name
                    }
                    saveInsightsToRoom(recording.id, updatedName, recording.timestamp, insights)
                    repository.updateRecording(
                        recording.copy(
                            name = updatedName,
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

    fun processPendingOfflineRecordings() {
        viewModelScope.launch(Dispatchers.IO) {
            val hasInternet = NetworkUtils.isInternetAvailable(getApplication())
            if (!hasInternet) {
                Log.d(TAG, "Cannot process pending offline recordings: No internet")
                return@launch
            }

            val currentMap = recordingsByPath.value
            val pending = currentMap.values.filter { rec ->
                !rec.transcript.isNullOrBlank() && rec.summary.isNullOrBlank() && !rec.isProcessing
            }

            if (pending.isEmpty()) {
                Log.d(TAG, "No pending offline recordings found")
                return@launch
            }

            Log.d(TAG, "Batch processing ${pending.size} pending offline recordings")
            for (recording in pending) {
                try {
                    retryAiProcessing(recording)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed auto-processing recording ${recording.id}", e)
                }
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
                            insightDao.deleteInsightsByRecordingId(recording.id)
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

    private val bm25Engine = BM25SearchEngine()

    suspend fun queryAllVoiceNotes(question: String): Pair<String, List<Pair<Long, String>>> = withContext(Dispatchers.IO) {
        val allRecs = _recordingsByPath.value.values.toList()
        if (allRecs.isEmpty()) {
            return@withContext Pair("You don't have any processed voice notes in your library yet.", emptyList())
        }

        // Build BM25 index from all available recordings
        val docs = allRecs.map { rec ->
            BM25SearchEngine.createDocument(
                id = rec.id,
                title = rec.name.ifBlank { "Voice Note" },
                transcript = rec.transcript.orEmpty(),
                summary = rec.summary.orEmpty(),
                dateString = formatDate(rec.timestamp)
            )
        }
        bm25Engine.index(docs)

        val searchResults = bm25Engine.search(question, topK = 4)
        val snippets = searchResults.map { res ->
            val doc = res.document
            "Note: ${doc.title} (${doc.dateString})\nContent:\n${doc.text.take(1200)}"
        }

        val citedNotes = searchResults.map { Pair(it.document.id, it.document.title) }

        val answer = aiProcessor.queryAllRecordings(snippets, question)
        Pair(answer, citedNotes)
    }

    fun toggleActionComplete(insight: InsightEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val isNowCompleted = insight.status != InsightStatus.COMPLETED
            val newStatus = if (isNowCompleted) InsightStatus.COMPLETED else InsightStatus.OPEN
            val completedAt = if (isNowCompleted) System.currentTimeMillis() else null
            insightDao.updateActionStatus(insight.id, newStatus, completedAt)
        }
    }

    fun archiveCompletedActions() {
        viewModelScope.launch(Dispatchers.IO) {
            insightDao.archiveCompletedActions()
        }
    }

    fun unarchiveAllActions() {
        viewModelScope.launch(Dispatchers.IO) {
            insightDao.unarchiveAllActions()
        }
    }

    fun dismissTheme(themeKey: String) {
        AppPreferences.dismissTheme(getApplication(), themeKey)
        val currentRecordings = _recordingsByPath.value.values.toList()
        computeThemeClusters(currentRecordings)
    }

    private fun computeThemeClusters(recordings: List<Recording>) {
        viewModelScope.launch(Dispatchers.Default) {
            val dismissed = AppPreferences.getDismissedThemes(getApplication())
            val validRecordings = recordings.filter { !it.transcript.isNullOrBlank() || !it.summary.isNullOrBlank() }
            if (validRecordings.size < 3) {
                _themeClusters.value = emptyList()
                return@launch
            }

            val stopWords = setOf(
                "the", "and", "this", "that", "with", "from", "have", "were", "they", "will", "what",
                "when", "where", "which", "there", "their", "about", "would", "could", "should",
                "into", "more", "some", "other", "than", "then", "just", "also", "your", "mine",
                "been", "each", "like", "very", "make", "made", "doing", "does", "done", "going",
                "went", "gone", "know", "knew", "think", "thought", "need", "want", "wanted",
                "voice", "note", "recording", "audio", "audiofile", "today", "yesterday", "tomorrow",
                "really", "maybe", "something", "anything", "nothing", "everything", "talk", "talking"
            )

            val keywordToNotes = mutableMapOf<String, MutableSet<Long>>()
            val noteIdToName = mutableMapOf<Long, String>()
            val noteIdToSnippet = mutableMapOf<Long, String>()

            for (rec in validRecordings) {
                val name = rec.name.ifBlank { "Voice Note" }
                noteIdToName[rec.id] = name
                val content = "${rec.name} ${rec.summary.orEmpty()} ${rec.transcript.orEmpty()}".lowercase()
                val snippet = rec.summary?.takeIf { it.isNotBlank() } ?: rec.transcript?.take(120).orEmpty()
                noteIdToSnippet[rec.id] = snippet

                val words = content.split(Regex("[^a-zA-Z0-9]+"))
                    .map { it.trim() }
                    .filter { it.length in 4..24 && it !in stopWords }

                // Single keywords
                val distinctWords = words.toSet()
                for (w in distinctWords) {
                    keywordToNotes.getOrPut(w) { mutableSetOf() }.add(rec.id)
                }

                // Two-word phrases
                for (i in 0 until words.size - 1) {
                    val bigram = "${words[i]} ${words[i + 1]}"
                    keywordToNotes.getOrPut(bigram) { mutableSetOf() }.add(rec.id)
                }
            }

            val clusters = keywordToNotes
                .filter { (key, notes) -> notes.size >= 3 && key !in dismissed }
                .map { (key, notes) ->
                    val displayTitle = key.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    val noteNames = notes.take(3).mapNotNull { noteIdToName[it] }
                    val sampleSnippets = notes.take(2).mapNotNull { noteIdToSnippet[it] }.filter { it.isNotBlank() }
                    ThemeCluster(
                        key = key,
                        title = displayTitle,
                        noteCount = notes.size,
                        noteIds = notes.toList(),
                        noteNames = noteNames,
                        sampleSnippets = sampleSnippets
                    )
                }
                .sortedByDescending { it.noteCount }
                .distinctBy { it.title.lowercase() }
                .take(6)

            _themeClusters.value = clusters
        }
    }

    private suspend fun saveInsightsToRoom(
        recordingId: Long,
        recordingName: String,
        timestamp: Long,
        insights: AIProcessor.AIInsights
    ) = withContext(Dispatchers.IO) {
        try {
            insightDao.deleteInsightsByRecordingId(recordingId)
            val entities = mutableListOf<InsightEntity>()

            insights.actionItems.forEachIndexed { idx, rawAction ->
                val cleanText = rawAction.removePrefix("[ ]").removePrefix("[]").trim()
                if (cleanText.isNotBlank()) {
                    entities.add(
                        InsightEntity(
                            id = "${recordingId}_action_${idx}_${System.currentTimeMillis()}",
                            recordingId = recordingId,
                            recordingName = recordingName,
                            kind = InsightKind.ACTION,
                            text = cleanText,
                            status = InsightStatus.OPEN,
                            createdAt = timestamp,
                            sourceOrder = idx
                        )
                    )
                }
            }

            insights.ideas.forEachIndexed { idx, ideaText ->
                if (ideaText.isNotBlank()) {
                    entities.add(
                        InsightEntity(
                            id = "${recordingId}_idea_${idx}_${System.currentTimeMillis()}",
                            recordingId = recordingId,
                            recordingName = recordingName,
                            kind = InsightKind.IDEA,
                            text = ideaText.trim(),
                            createdAt = timestamp,
                            sourceOrder = idx
                        )
                    )
                }
            }

            insights.decisions.forEachIndexed { idx, dec ->
                if (dec.text.isNotBlank()) {
                    entities.add(
                        InsightEntity(
                            id = "${recordingId}_decision_${idx}_${System.currentTimeMillis()}",
                            recordingId = recordingId,
                            recordingName = recordingName,
                            kind = InsightKind.DECISION,
                            text = dec.text.trim(),
                            rationale = dec.rationale?.trim(),
                            evidence = dec.evidence?.trim(),
                            createdAt = timestamp,
                            sourceOrder = idx
                        )
                    )
                }
            }

            if (entities.isNotEmpty()) {
                insightDao.insertInsights(entities)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save insights to Room for recording $recordingId", e)
        }
    }

    private fun syncExistingRecordingsToInsights() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (insightDao.getInsightCount() > 0) return@launch

                val allRecs = repository.allRecordings.first()
                val listType = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type

                for (rec in allRecs) {
                    val rawActions: List<String> = try {
                        if (!rec.actionItems.isNullOrBlank()) {
                            gson.fromJson<List<String>>(rec.actionItems, listType) ?: emptyList()
                        } else emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }

                    val rawKeyPoints: List<String> = try {
                        if (!rec.keyPoints.isNullOrBlank()) {
                            gson.fromJson<List<String>>(rec.keyPoints, listType) ?: emptyList()
                        } else emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }

                    val recName = rec.name.ifBlank { "Voice Note" }
                    val entities = mutableListOf<InsightEntity>()

                    rawActions.forEachIndexed { idx, act ->
                        val clean = act.removePrefix("[ ]").removePrefix("[]").trim()
                        if (clean.isNotBlank()) {
                            entities.add(
                                InsightEntity(
                                    id = "${rec.id}_action_${idx}",
                                    recordingId = rec.id,
                                    recordingName = recName,
                                    kind = InsightKind.ACTION,
                                    text = clean,
                                    status = InsightStatus.OPEN,
                                    createdAt = rec.timestamp,
                                    sourceOrder = idx
                                )
                            )
                        }
                    }

                    rawKeyPoints.forEachIndexed { idx, pt ->
                        val clean = pt.trim()
                        if (clean.isNotBlank()) {
                            entities.add(
                                InsightEntity(
                                    id = "${rec.id}_idea_${idx}",
                                    recordingId = rec.id,
                                    recordingName = recName,
                                    kind = InsightKind.IDEA,
                                    text = clean,
                                    createdAt = rec.timestamp,
                                    sourceOrder = idx
                                )
                            )
                        }
                    }

                    if (entities.isNotEmpty()) {
                        insightDao.insertInsights(entities)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing recordings to insights", e)
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
