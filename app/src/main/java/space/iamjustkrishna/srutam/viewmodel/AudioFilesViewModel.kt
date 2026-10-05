package space.iamjustkrishna.srutam.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import space.iamjustkrishna.srutam.SrutamApplication
import space.iamjustkrishna.srutam.ai.AIProcessor
import space.iamjustkrishna.srutam.ai.BM25SearchEngine
import space.iamjustkrishna.srutam.ai.AiCacheUtils
import space.iamjustkrishna.srutam.ai.copilot.CopilotService
import space.iamjustkrishna.srutam.ai.copilot.NoteRef
import space.iamjustkrishna.srutam.ai.copilot.NotesGateway
import space.iamjustkrishna.srutam.data.AiQueryCache
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.InsightKind
import space.iamjustkrishna.srutam.data.InsightStatus
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import space.iamjustkrishna.srutam.repository.RecordingRepository
import space.iamjustkrishna.srutam.repository.RenameResult
import space.iamjustkrishna.srutam.service.AiProcessingWorker
import space.iamjustkrishna.srutam.service.RecordingForegroundService
import space.iamjustkrishna.srutam.ui.screens.formatDate
import space.iamjustkrishna.srutam.ui.screens.FeedFilter
import java.time.LocalDate
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.iamjustkrishna.srutam.analytics.UserActivityAnalytics
import space.iamjustkrishna.srutam.analytics.UserActivityMetrics
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

    private val _selectedFilter = MutableStateFlow(FeedFilter.DEFAULT)
    val selectedFilter: StateFlow<FeedFilter> = _selectedFilter.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    fun setSelectedFilter(filter: FeedFilter) {
        _selectedFilter.value = filter
        _selectedDate.value = null
    }

    fun setSelectedDate(date: LocalDate?) {
        _selectedDate.value = date
        if (date != null) {
            _selectedFilter.value = FeedFilter.DEFAULT
        }
    }

    private val database = (application as space.iamjustkrishna.srutam.SrutamApplication).database
    private val insightDao = database.insightDao()
    private val reminderDao = database.reminderDao()
    private val aiQueryCacheDao = database.aiQueryCacheDao()
    private val repository: RecordingRepository = RecordingRepository(application.applicationContext, database.recordingDao())

    val activityMetrics: StateFlow<UserActivityMetrics> = combine(
        repository.allRecordings,
        insightDao.getAllInsightsFlow()
    ) { recordings, insights ->
        UserActivityAnalytics.computeMetrics(recordings, insights)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserActivityMetrics())

    val allInsights: StateFlow<List<space.iamjustkrishna.srutam.data.InsightEntity>> = insightDao.getAllInsightsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingReminders: StateFlow<List<space.iamjustkrishna.srutam.data.ReminderEntity>> = reminderDao.getAllActiveRemindersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeActions: StateFlow<List<space.iamjustkrishna.srutam.data.InsightEntity>> = insightDao.getActiveActionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allIdeas: StateFlow<List<space.iamjustkrishna.srutam.data.InsightEntity>> = insightDao.getIdeasFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDecisions: StateFlow<List<space.iamjustkrishna.srutam.data.InsightEntity>> = insightDao.getDecisionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedActionsCount: StateFlow<Int> = insightDao.getArchivedActionsCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pastReminders: StateFlow<List<space.iamjustkrishna.srutam.data.ReminderEntity>> = reminderDao.getPastRemindersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _themeClusters = MutableStateFlow<List<ThemeCluster>>(emptyList())
    val themeClusters: StateFlow<List<ThemeCluster>> = _themeClusters.asStateFlow()

    private val aiProcessor: AIProcessor = AIProcessor(application)
    private val gson = Gson()

    init {
        observeRecordings()
        loadAudioFiles()

        viewModelScope.launch {
            RecordingForegroundService.recordingSavedEvents.collect {
                loadAudioFiles()
            }
        }
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
                if (recordings.isNotEmpty() && recordings.size != _audioFiles.value.size) {
                    loadAudioFiles()
                }
            }
        }
        syncExistingRecordingsToInsights()
    }

    /** [deleteContent] false deletes only the audio; the note's transcript, insights, tasks and reminders stay. */
    fun deleteAudioFile(audioFile: AudioFileInfo, deleteContent: Boolean = true) {
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
                if (!deleteContent) {
                    try {
                        repository.deleteAudioKeepingContent(audioFile.filePath)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting audio file: ${audioFile.filePath}", e)
                        _processingError.value = "Failed to delete file. Try again."
                    }
                } else if (recording != null) {
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
                when (val result = repository.renameRecording(
                    currentPath = audioFile.filePath,
                    newTitle = newName,
                    duration = audioFile.duration,
                    timestamp = audioFile.timestamp
                )) {
                    is RenameResult.Renamed -> {
                        // Optimistic UI update for immediate feedback
                        val renamed = File(result.newPath)
                        _audioFiles.value = _audioFiles.value.map { file ->
                            if (file.filePath == audioFile.filePath) {
                                file.copy(
                                    filePath = result.newPath,
                                    fileName = renamed.name,
                                    timestamp = renamed.lastModified().takeIf { it > 0 } ?: file.timestamp,
                                    sizeBytes = renamed.length().takeIf { it > 0 } ?: file.sizeBytes
                                )
                            } else {
                                file
                            }
                        }
                        // Reload to ensure consistency with storage
                        loadAudioFiles()
                    }
                    is RenameResult.Rejected -> _processingError.value = "Failed to rename: ${result.message}"
                    RenameResult.Failed -> _processingError.value = "Failed to rename file. Please try again."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error renaming recording", e)
                _processingError.value = "Failed to rename file. Please try again."
            }
        }
    }

    /**
     * Process a recording with AI using background WorkManager pipeline.
     */
    fun processRecordingForAI(audioFile: AudioFileInfo) {
        viewModelScope.launch(Dispatchers.IO) { startAiProcessing(audioFile) }
    }

    private suspend fun startAiProcessing(audioFile: AudioFileInfo) {
        try {
            Log.d(TAG, "Queueing AI processing for: ${audioFile.fileName}")
            var recording = repository.getRecordingByPath(audioFile.filePath)
            if (recording == null) {
                val newRecording = Recording(
                    audioFilePath = audioFile.filePath,
                    duration = audioFile.duration,
                    name = RecordingNameFormatter.displayName(
                        fileName = audioFile.fileName,
                        timestamp = audioFile.timestamp
                    ),
                    isProcessing = true,
                    aiStatus = RecordingAiStatus.TRANSCRIBING
                )
                val recordingId = repository.insertRecording(newRecording)
                recording = newRecording.copy(id = recordingId)
            } else {
                repository.updateRecording(
                    recording.copy(
                        isProcessing = true,
                        aiStatus = if (recording.transcript.isNullOrBlank()) {
                            RecordingAiStatus.TRANSCRIBING
                        } else {
                            RecordingAiStatus.SUMMARY_PROCESSING
                        },
                        processingError = null
                    )
                )
            }

            AiProcessingWorker.enqueueProcessing(getApplication(), listOf(recording.id))
            loadAudioFiles()
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating AI processing", e)
            _processingError.value = "Failed to start AI processing. Please try again."
        }
    }

    /**
     * The in-app Save dialog is finished with a new note. If it renamed the file, carry the stored note
     * over to the new path (it may already hold the live transcript), then start AI when auto-AI is on.
     * One sequential call, so the AI step always finds the moved note.
     */
    fun onNewRecordingSaved(originalPath: String, savedFile: java.io.File, startAi: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (originalPath != savedFile.absolutePath) {
                    repository.moveRecordingPath(
                        originalPath,
                        savedFile.absolutePath,
                        RecordingNameFormatter.displayName(fileName = savedFile.name, timestamp = savedFile.lastModified())
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Could not move the saved note to its renamed file", e)
            }
            if (startAi) {
                startAiProcessing(
                    AudioFileInfo(
                        filePath = savedFile.absolutePath,
                        fileName = savedFile.name,
                        duration = 0L,
                        timestamp = savedFile.lastModified(),
                        sizeBytes = savedFile.length()
                    )
                )
            } else {
                loadAudioFiles()
            }
        }
    }

    /** The Save dialog's Discard: drop the note that may already hold the live transcript. */
    fun onNewRecordingDiscarded(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.getRecordingByPath(path)?.let { repository.deleteRecording(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Could not remove the discarded note", e)
            }
        }
    }

    fun generateSummaryForRecording(audioFile: AudioFileInfo) {
        processRecordingForAI(audioFile)
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
                repository.updateRecording(
                    recording.copy(
                        isProcessing = true,
                        aiStatus = if (recording.transcript.isNullOrBlank()) {
                            RecordingAiStatus.TRANSCRIBING
                        } else {
                            RecordingAiStatus.SUMMARY_PROCESSING
                        },
                        processingError = null
                    )
                )
                AiProcessingWorker.enqueueProcessing(getApplication(), listOf(recording.id))
                loadAudioFiles()
            } catch (e: Exception) {
                Log.e(TAG, "Error retrying AI processing", e)
                _processingError.value = "Failed to retry processing. Please try again."
            }
        }
    }

    fun processPendingOfflineRecordings(force: Boolean = false, onComplete: ((Int) -> Unit)? = null) {
        if (!force && !AppPreferences.isAutoAiEnabled(getApplication())) {
            Log.d(TAG, "Auto AI processing is disabled in Settings. Skipping automatic pending recordings sync.")
            onComplete?.invoke(0)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentFiles = _audioFiles.value.ifEmpty { AudioFileReader.getAudioFiles() }
                val currentMap = _recordingsByPath.value

                // Find all audio files that either have no summary, are pending offline, or have fallback summary
                val pendingFiles = currentFiles.filter { audioFile ->
                    val rec = currentMap[audioFile.filePath]
                    rec == null ||
                        rec.summary.isNullOrBlank() ||
                        rec.aiStatus == RecordingAiStatus.SUMMARY_PENDING_OFFLINE ||
                        rec.summary?.startsWith("This recording contains approximately") == true
                }

                if (pendingFiles.isEmpty()) {
                    Log.d(TAG, "No pending offline recordings found")
                    withContext(Dispatchers.Main) {
                        onComplete?.invoke(0)
                    }
                    return@launch
                }

                Log.d(TAG, "Batch processing ${pendingFiles.size} pending recordings via WorkManager")
                val ids = mutableListOf<Long>()
                for (audioFile in pendingFiles) {
                    var recording = currentMap[audioFile.filePath] ?: repository.getRecordingByPath(audioFile.filePath)
                    if (recording == null) {
                        val newRecording = Recording(
                            audioFilePath = audioFile.filePath,
                            duration = audioFile.duration,
                            name = RecordingNameFormatter.displayName(
                                fileName = audioFile.fileName,
                                timestamp = audioFile.timestamp
                            ),
                            isProcessing = true,
                            aiStatus = RecordingAiStatus.TRANSCRIBING
                        )
                        val newId = repository.insertRecording(newRecording)
                        ids.add(newId)
                    } else if (!recording.isProcessing) {
                        val isFallback = recording.summary?.startsWith("This recording contains approximately") == true
                        repository.updateRecording(
                            recording.copy(
                                summary = if (isFallback) null else recording.summary,
                                wiifm = if (isFallback) null else recording.wiifm,
                                keyPoints = if (isFallback) null else recording.keyPoints,
                                isProcessing = true,
                                aiStatus = if (recording.transcript.isNullOrBlank()) {
                                    RecordingAiStatus.TRANSCRIBING
                                } else {
                                    RecordingAiStatus.SUMMARY_PROCESSING
                                },
                                processingError = null
                            )
                        )
                        ids.add(recording.id)
                    }
                }

                if (ids.isNotEmpty()) {
                    AiProcessingWorker.enqueueProcessing(getApplication(), ids)
                    loadAudioFiles()
                }

                withContext(Dispatchers.Main) {
                    onComplete?.invoke(ids.size)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing pending offline recordings", e)
                _processingError.value = "Failed to process pending notes. Please try again."
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(0)
                }
            }
        }
    }

    fun processBatchAI(audioFiles: List<AudioFileInfo>) {
        if (audioFiles.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ids = mutableListOf<Long>()
                for (audioFile in audioFiles) {
                    var recording = repository.getRecordingByPath(audioFile.filePath)
                    if (recording == null) {
                        val newRecording = Recording(
                            audioFilePath = audioFile.filePath,
                            duration = audioFile.duration,
                            name = RecordingNameFormatter.displayName(
                                fileName = audioFile.fileName,
                                timestamp = audioFile.timestamp
                            ),
                            isProcessing = true,
                            aiStatus = RecordingAiStatus.TRANSCRIBING
                        )
                        val newId = repository.insertRecording(newRecording)
                        ids.add(newId)
                    } else {
                        repository.updateRecording(
                            recording.copy(
                                isProcessing = true,
                                aiStatus = if (recording.transcript.isNullOrBlank()) {
                                RecordingAiStatus.TRANSCRIBING
                            } else {
                                RecordingAiStatus.SUMMARY_PROCESSING
                            },
                                processingError = null
                            )
                        )
                        ids.add(recording.id)
                    }
                }
                if (ids.isNotEmpty()) {
                    AiProcessingWorker.enqueueProcessing(getApplication(), ids)
                }
                loadAudioFiles()
            } catch (e: Exception) {
                Log.e(TAG, "Error processing batch AI", e)
                _processingError.value = "Failed to start batch processing. Please try again."
            }
        }
    }

    fun deleteMultipleAudioFiles(audioFiles: List<AudioFileInfo>, deleteContent: Boolean = true) {
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
                        if (!deleteContent) {
                            repository.deleteAudioKeepingContent(audioFile.filePath)
                        } else if (recording != null) {


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

        // Context-aware caching using top snippet fingerprint
        val normalizedQ = AiCacheUtils.normalizeQuery(question)
        val snippetFingerprint = searchResults.map { "${it.document.id}:${it.document.text.hashCode()}" }
            .sorted()
            .joinToString(",")
        val cacheKey = AiCacheUtils.sha256("global:$normalizedQ:$snippetFingerprint")

        val cached = try {
            aiQueryCacheDao.get(cacheKey)
        } catch (e: Exception) {
            null
        }

        if (cached != null) {
            Log.d("AudioFilesViewModel", "Global Copilot cache HIT for: $question")
            aiQueryCacheDao.updateAccessTime(cacheKey)
            return@withContext Pair(cached.answer, citedNotes)
        }

        val answer = aiProcessor.queryAllRecordings(snippets, question)

        try {
            aiQueryCacheDao.insert(
                AiQueryCache(
                    cacheKey = cacheKey,
                    queryType = "GLOBAL",
                    normalizedQuery = normalizedQ,
                    contextFingerprint = snippetFingerprint,
                    answer = answer,
                    citedNotesJson = gson.toJson(citedNotes)
                )
            )
            aiQueryCacheDao.pruneOldEntries(500)
        } catch (e: Exception) {
            Log.w("AudioFilesViewModel", "Failed to cache global query answer: ${e.message}")
        }

        Pair(answer, citedNotes)
    }

    private fun indexLibrary(): Map<Long, Recording> {
        val recs = _recordingsByPath.value.values.toList()
        bm25Engine.index(recs.map { rec ->
            BM25SearchEngine.createDocument(
                id = rec.id, title = rec.name.ifBlank { "Voice Note" },
                transcript = rec.transcript.orEmpty(), summary = rec.summary.orEmpty(), dateString = formatDate(rec.timestamp)
            )
        })
        return recs.associateBy { it.id }
    }

    private val copilotNotes = object : NotesGateway {
        private fun ref(rec: Recording) = NoteRef(
            rec.id, rec.name.ifBlank { "Voice Note" }, rec.timestamp,
            rec.summary?.takeIf { it.isNotBlank() } ?: rec.transcript.orEmpty().take(500)
        )

        override suspend fun search(query: String, limit: Int) = withContext(Dispatchers.IO) {
            val byId = indexLibrary()
            bm25Engine.search(query, topK = limit).mapNotNull { byId[it.document.id]?.let(::ref) }
        }

        override suspend fun inRange(fromMs: Long, toMs: Long, limit: Int) = withContext(Dispatchers.IO) {
            _recordingsByPath.value.values.filter { it.timestamp in fromMs until toMs }
                .sortedByDescending { it.timestamp }.take(limit).map(::ref)
        }

        override suspend fun titleOf(id: Long) = getRecordingById(id)?.name?.ifBlank { "Voice Note" }

        override suspend fun rename(id: Long, newName: String): Boolean = withContext(Dispatchers.IO) {
            val rec = getRecordingById(id) ?: return@withContext false
            if (newName.isBlank()) return@withContext false
            repository.updateRecording(rec.copy(name = newName.trim()))
            true
        }
    }

    /** Chat with tools: saved conversations plus the agent that can propose changes. */
    val copilot: CopilotService by lazy {
        CopilotService(getApplication(), copilotNotes, aiProcessor::generateRaw) { question ->
            withContext(Dispatchers.IO) {
                val byId = indexLibrary()
                val results = bm25Engine.search(question, topK = 4)
                val snippets = results.map { "Note: ${it.document.title} (${it.document.dateString})\nContent:\n${it.document.text.take(1200)}" }
                val matched = results.filter { byId.containsKey(it.document.id) }
                // Always include the newest notes so "what did I talk about recently" has something to read.
                val matchedIds = matched.map { it.document.id }.toSet()
                val recent = byId.values.filter { it.id !in matchedIds && (!it.summary.isNullOrBlank() || !it.transcript.isNullOrBlank()) }
                    .sortedByDescending { it.timestamp }.take(5)
                val recentSnippets = recent.map { rec ->
                    val body = rec.summary?.takeIf { it.isNotBlank() } ?: rec.transcript.orEmpty().take(600)
                    "Recent note: ${rec.name.ifBlank { "Voice Note" }} (${formatDate(rec.timestamp)})\nContent:\n$body"
                }
                (snippets + recentSnippets) to
                    (matched.map { it.document.id to it.document.title } + recent.map { it.id to it.name.ifBlank { "Voice Note" } })
            }
        }
    }

    suspend fun getRecordingById(recordingId: Long): Recording? = withContext(Dispatchers.IO) {
        repository.getRecordingById(recordingId)
            ?: _recordingsByPath.value.values.find { it.id == recordingId }
    }

    suspend fun querySpecificRecording(recordingId: Long, question: String): Pair<String, List<Pair<Long, String>>> = withContext(Dispatchers.IO) {
        val rec = repository.getRecordingById(recordingId)
            ?: _recordingsByPath.value.values.find { it.id == recordingId }

        if (rec == null || rec.transcript.isNullOrBlank()) {
            return@withContext Pair("This voice note does not have a transcript available yet. Please transcribe it first.", emptyList())
        }

        val noteTitle = rec.name.ifBlank { "Voice Note" }
        val answer = aiProcessor.queryRecording(rec.transcript, question, recordingId = rec.id)
        Pair(answer, listOf(Pair(rec.id, noteTitle)))
    }

    fun toggleActionComplete(insight: InsightEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            space.iamjustkrishna.srutam.repository.InsightsRepository.from(getApplication()).toggleTask(insight.id)
        }
    }

    fun updateReminderStatus(id: String, status: String) {
        viewModelScope.launch(Dispatchers.IO) {
            space.iamjustkrishna.srutam.repository.InsightsRepository.from(getApplication()).setReminderStatus(id, status)
        }
    }

    fun archiveCompletedActions() {
        viewModelScope.launch(Dispatchers.IO) {
            space.iamjustkrishna.srutam.repository.InsightsRepository.from(getApplication()).archiveCompleted()
        }
    }

    fun unarchiveAllActions() {
        viewModelScope.launch(Dispatchers.IO) { insightDao.unarchiveAllActions() }
    }

    fun dismissTheme(themeKey: String) {
        AppPreferences.dismissTheme(getApplication(), themeKey)
        val currentRecordings = _recordingsByPath.value.values.toList()
        computeThemeClusters(currentRecordings)
    }

    private fun computeThemeClusters(recordings: List<Recording>) {
        viewModelScope.launch(Dispatchers.Default) {
            _themeClusters.value = ThemeClusterEngine.build(recordings, AppPreferences.getDismissedThemes(getApplication()))
        }
    }

    private fun syncExistingRecordingsToInsights() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                space.iamjustkrishna.srutam.repository.InsightsRepository.from(getApplication()).importLegacy()
            } catch (e: Exception) {
                Log.e(TAG, "Legacy insight import failed", e)
            }
        }
    }
    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
    companion object { private const val TAG = "AudioFilesViewModel" }

}
