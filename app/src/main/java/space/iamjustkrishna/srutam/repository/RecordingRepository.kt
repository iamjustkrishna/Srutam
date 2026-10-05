package space.iamjustkrishna.srutam.repository

import android.content.Context
import android.util.Log
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingDao
import space.iamjustkrishna.srutam.utils.AudioStorage
import space.iamjustkrishna.srutam.utils.RecordingFileNames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** How a rename ended. [Rejected] carries a message that is fine to show the user. */
sealed interface RenameResult {
    data class Renamed(val newPath: String, val title: String) : RenameResult
    data class Rejected(val message: String) : RenameResult
    data object Failed : RenameResult
}

class RecordingRepository(
    private val context: Context,
    private val recordingDao: RecordingDao,
    private val deleteAudio: (Context, String) -> Boolean = AudioStorage::deleteAudioFile
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
        val deleted = deleteAudio(context, recording.audioFilePath)
        if (!deleted) {
            Log.w(TAG, "Failed to delete audio file: ${recording.audioFilePath}")
            throw IOException("Could not delete audio file")
        }

        InsightsRepository.from(context).deleteRecordingData(recording.id)
    }

    /**
     * Renames a note: its file and its title together, so they can never drift apart. The name is checked
     * against the real folder here (the dialog's check is only a hint), and the stored note changes only
     * after the file was renamed. A file with no stored note yet gets one with [duration] and [timestamp].
     */
    suspend fun renameRecording(
        currentPath: String,
        newTitle: String,
        duration: Long = 0L,
        timestamp: Long = System.currentTimeMillis()
    ): RenameResult = withContext(Dispatchers.IO) {
        val current = File(currentPath)
        if (!current.exists()) return@withContext RenameResult.Rejected("File not found")

        val folder = current.parentFile
        RecordingFileNames.validate(newTitle, RecordingFileNames.namesIn(folder), current.name)
            ?.let { return@withContext RenameResult.Rejected(it) }

        val title = RecordingFileNames.titleOf(newTitle)
        val target = File(folder, RecordingFileNames.fileNameFor(title))
        val sameFile = target.absolutePath == current.absolutePath
        if (!sameFile && !current.renameTo(target)) {
            Log.w(TAG, "Could not rename $currentPath to ${target.name}")
            return@withContext RenameResult.Failed
        }

        try {
            val moved = recordingDao.movePath(currentPath, target.absolutePath, title) > 0
            if (!moved && recordingDao.getRecordingByPath(target.absolutePath) == null) {
                recordingDao.insertRecording(
                    Recording(
                        audioFilePath = target.absolutePath,
                        name = title,
                        duration = duration,
                        timestamp = timestamp
                    )
                )
            }
        } catch (e: Exception) {
            // Keep file and title together: put the file back if the stored note could not follow.
            Log.e(TAG, "Could not update the stored note; undoing the file rename", e)
            if (!sameFile) target.renameTo(current)
            return@withContext RenameResult.Failed
        }
        RenameResult.Renamed(target.absolutePath, title)
    }

    /** Deletes only the audio. The note's transcript, insights, tasks and reminders stay. */
    fun deleteAudioKeepingContent(audioFilePath: String) {
        if (!deleteAudio(context, audioFilePath)) {
            Log.w(TAG, "Failed to delete audio file: $audioFilePath")
            throw IOException("Could not delete audio file")
        }
    }

    suspend fun deleteRecordingById(recordingId: Long) {
        // Get recording first to delete the audio file
        val recording = recordingDao.getRecordingById(recordingId)
        if (recording != null) {
            deleteRecording(recording)
        } else {
            // If recording not found in DB, just delete from DB
            InsightsRepository.from(context).deleteRecordingData(recordingId)
        }
    }

    suspend fun getRecordingByPath(audioFilePath: String): Recording? {
        return recordingDao.getRecordingByPath(audioFilePath)
    }

    suspend fun setTranscriptIfBlank(id: Long, text: String) {
        recordingDao.setTranscriptIfBlank(id, text)
    }

    /** Points a note at its renamed file. Returns whether a note existed for [oldPath]. */
    suspend fun moveRecordingPath(oldPath: String, newPath: String, name: String): Boolean {
        return recordingDao.movePath(oldPath, newPath, name) > 0
    }
}
