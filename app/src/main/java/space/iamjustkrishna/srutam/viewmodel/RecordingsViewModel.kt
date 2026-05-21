package space.iamjustkrishna.srutam.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import space.iamjustkrishna.srutam.SrutamApplication
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.repository.RecordingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecordingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RecordingRepository

    val recordings: StateFlow<List<Recording>>

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        val database = (application as SrutamApplication).database
        repository = RecordingRepository(application.applicationContext, database.recordingDao())

        recordings = repository.allRecordings
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        viewModelScope.launch {
            recordings.collect {
                _isLoading.value = false
            }
        }
    }

    fun deleteRecording(recording: Recording) {
        viewModelScope.launch {
            repository.deleteRecording(recording)
        }
    }

    fun deleteRecordingById(recordingId: Long) {
        viewModelScope.launch {
            repository.deleteRecordingById(recordingId)
        }
    }
}
