package space.iamjustkrishna.srutam.repository

import android.content.Context
import android.util.Log
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingDao
import space.iamjustkrishna.srutam.utils.AudioStorage
import kotlinx.coroutines.flow.Flow
import java.io.IOException

class RecordingRepository(
    private val context: Context,
    private val recordingDao: RecordingDao
) {

    companion object {
        private const val TAG = "RecordingRepository"
    }

    val allRecordings: Flow<List<Recording>> = recordingDao.getAllRecordings()

    suspend fun getRecordingById(recordingId: Long): Recording? {
        return recordingDao.getRecordingById(recordingId)
    }

    fun getRecordingByIdFlow(recordingId: Long): Flow<Recording?> {
        return recordingDao.getRecordingByIdFlow(recordingId)
    }

    suspend fun insertRecording(recording: Recording): Long {
        return recordingDao.insertRecording(recording)
    }

    suspend fun updateRecording(recording: Recording) {
        recordingDao.updateRecording(recording)
    }

    suspend fun deleteRecording(recording: Recording) {
        val deleted = AudioStorage.deleteAudioFile(context, recording.audioFilePath)
        if (!deleted) {
            Log.w(TAG, "Failed to delete audio file: ${recording.audioFilePath}")
            throw IOException("Could not delete audio file")
        }

        recordingDao.deleteRecording(recording)
    }

    suspend fun deleteRecordingById(recordingId: Long) {
        // Get recording first to delete the audio file
        val recording = recordingDao.getRecordingById(recordingId)
        if (recording != null) {
            deleteRecording(recording)
        } else {
            // If recording not found in DB, just delete from DB
            recordingDao.deleteRecordingById(recordingId)
        }
    }

    suspend fun getRecordingByPath(audioFilePath: String): Recording? {
        return recordingDao.getRecordingByPath(audioFilePath)
    }
}
