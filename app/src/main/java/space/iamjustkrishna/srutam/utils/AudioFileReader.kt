package space.iamjustkrishna.srutam.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.util.Log
import java.io.File

data class AudioFileInfo(
    val filePath: String,
    val fileName: String,
    val duration: Long,
    val timestamp: Long,
    val sizeBytes: Long
)

object AudioFileReader {
    private const val TAG = "AudioFileReader"
    private val supportedExtensions = setOf("m4a", "mp3", "wav", "aac")
    private var context: Context? = null

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    fun getRecordingsDirectory(): File {
        // Points to: /storage/emulated/0/Music/
        val publicMusicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)

        // Points to: /storage/emulated/0/Music/Srutam/
        val srutamDir = File(publicMusicDir, "Srutam")

        // Create the folder if it doesn't exist
        if (!srutamDir.exists()) {
            srutamDir.mkdirs()
        }

        return srutamDir
    }

    fun getAudioFiles(): List<AudioFileInfo> {
        return try {
            val recordingsDir = getRecordingsDirectory()

            if (!recordingsDir.exists()) {
                Log.d(TAG, "Srutam directory doesn't exist")
                return emptyList()
            }

            val audioFiles = recordingsDir.listFiles { file ->
                file.isFile && file.extension.lowercase() in supportedExtensions
            } ?: emptyArray()

            Log.d(TAG, "Found ${audioFiles.size} audio files")

            audioFiles.map { file ->
                AudioFileInfo(
                    filePath = file.absolutePath,
                    fileName = file.name,
                    duration = getAudioDuration(file),
                    timestamp = file.lastModified(),
                    sizeBytes = file.length()
                )
            }.sortedByDescending { it.timestamp } // Newest first

        } catch (e: Exception) {
            Log.e(TAG, "Error reading audio files", e)
            emptyList()
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
            Log.e(TAG, "Error getting duration for ${audioFile.name}", e)
            0L
        }
    }
}
