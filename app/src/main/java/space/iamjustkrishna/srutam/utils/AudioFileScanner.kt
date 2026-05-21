package space.iamjustkrishna.srutam.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import space.iamjustkrishna.srutam.SrutamApplication
import space.iamjustkrishna.srutam.ai.AIProcessor
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object AudioFileScanner {
    private const val TAG = "AudioFileScanner"

    suspend fun scanAndImportAudioFiles(context: Context) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting audio file scan...")

            // Get Srutam recordings directory
            val recordingsDir = AudioFileReader.getRecordingsDirectory()

            if (!recordingsDir.exists()) {
                Log.d(TAG, "Recordings directory doesn't exist")
                return@withContext
            }

            // Get all audio files
            val audioFiles = recordingsDir.listFiles { file ->
                file.isFile && file.extension.lowercase() in listOf("m4a", "mp3", "wav", "aac")
            } ?: emptyArray()

            Log.d(TAG, "Found ${audioFiles.size} audio files")

            if (audioFiles.isEmpty()) {
                return@withContext
            }

            val database = SrutamApplication.getInstance().database
            val existingPaths = database.recordingDao().getAllRecordingPaths()

            // Import files that don't exist in database
            val filesToImport = audioFiles.filter { file ->
                file.absolutePath !in existingPaths
            }

            Log.d(TAG, "Importing ${filesToImport.size} new files")

            for (file in filesToImport) {
                try {
                    importAudioFile(context, file, database)
                } catch (e: Exception) {
                    Log.e(TAG, "Error importing ${file.name}", e)
                }
            }

            Log.d(TAG, "Audio file scan complete. Imported ${filesToImport.size} files")
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning audio files", e)
        }
    }

    private suspend fun importAudioFile(context: Context, file: File, database: space.iamjustkrishna.srutam.data.AppDatabase) {
        try {
            // Get audio duration
            val duration = getAudioDuration(file)

            // Get file timestamp (last modified or creation time)
            val timestamp = file.lastModified()

            // Create recording entry - NO AI PROCESSING
            val recording = Recording(
                audioFilePath = file.absolutePath,
                duration = duration,
                timestamp = timestamp,
                name = RecordingNameFormatter.displayName(
                    fileName = file.name,
                    timestamp = timestamp
                ),
                isProcessing = false,
                aiStatus = RecordingAiStatus.NOT_REQUESTED
            )

            val recordingId = database.recordingDao().insertRecording(recording)
            Log.d(TAG, "Imported: ${file.name} with ID: $recordingId")

            // Skip AI processing for now
            // processRecordingWithAI(context, recordingId, file, database)
        } catch (e: Exception) {
            Log.e(TAG, "Error importing file: ${file.name}", e)
        }
    }

    private fun getAudioDuration(audioFile: File): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(audioFile.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            duration?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting audio duration for ${audioFile.name}", e)
            0L
        }
    }

    private suspend fun processRecordingWithAI(
        context: Context,
        recordingId: Long,
        audioFile: File,
        database: space.iamjustkrishna.srutam.data.AppDatabase
    ) = withContext(Dispatchers.IO) {
        try {
            val aiProcessor = AIProcessor(context)
            val result = aiProcessor.processRecording(audioFile)

            val recording = database.recordingDao().getRecordingById(recordingId)
            if (recording != null) {
                val updatedRecording = recording.copy(
                    transcript = result.transcript,
                    summary = result.summary,
                    keyPoints = result.keyPoints,
                    actionItems = result.actionItems,
                    wiifm = result.wiifm,
                    isProcessing = false,
                    aiStatus = RecordingAiStatus.READY
                )
                database.recordingDao().updateRecording(updatedRecording)
                Log.d(TAG, "AI processing completed for recording $recordingId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing recording with AI", e)
            try {
                val recording = database.recordingDao().getRecordingById(recordingId)
                if (recording != null) {
                    database.recordingDao().updateRecording(
                        recording.copy(
                            isProcessing = false,
                            aiStatus = RecordingAiStatus.ERROR,
                            processingError = e.message ?: "Unknown error"
                        )
                    )
                }
            } catch (dbError: Exception) {
                Log.e(TAG, "Error updating recording with error", dbError)
            }
        }
    }
}
