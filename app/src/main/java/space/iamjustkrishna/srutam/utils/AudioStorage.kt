package space.iamjustkrishna.srutam.utils

import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

object AudioStorage {
    private const val TAG = "AudioStorage"

    fun deleteAudioFile(context: Context, filePath: String): Boolean {
        return deleteAudioFiles(context, listOf(filePath))
    }

    fun deleteAudioFiles(context: Context, filePaths: List<String>): Boolean {
        var allDeleted = true
        filePaths.forEach { filePath ->
            if (!deleteAudioFileDirect(filePath)) {
                allDeleted = false
            }
        }
        return allDeleted
    }

    suspend fun createDeleteRequest(context: Context, filePaths: List<String>) = if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    ) {
        createScopedStorageDeleteRequest(context, filePaths)
    } else {
        null
    }

    private fun deleteAudioFileDirect(filePath: String): Boolean {
        val file = File(filePath)

        if (!file.exists()) {
            Log.d(TAG, "File does not exist: $filePath")
            return true
        }

        return try {
            val deleted = file.delete()
            if (deleted) {
                Log.d(TAG, "Successfully deleted audio file: $filePath")
            } else {
                Log.w(TAG, "Failed to delete audio file: $filePath")
            }
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting audio file: $filePath", e)
            false
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.R)
    private suspend fun createScopedStorageDeleteRequest(
        context: Context,
        filePaths: List<String>
    ) = try {
        val existingFiles = filePaths
            .distinct()
            .map(::File)
            .filter(File::exists)

        val uris = existingFiles.mapNotNull { file ->
            resolveMediaStoreUri(context, file)
        }

        when {
            uris.isEmpty() -> null
            uris.size != existingFiles.size -> {
                Log.w(TAG, "Could not resolve all MediaStore Uris for delete request")
                null
            }
            else -> MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error creating scoped storage delete request", e)
        null
    }

    private suspend fun resolveMediaStoreUri(context: Context, file: File): Uri? {
        findAudioUri(context, file)?.let { return it }
        scanFile(context, file)
        return findAudioUri(context, file)
    }

    private fun findAudioUri(context: Context, file: File): Uri? {
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf(file.name, "Music/Srutam/")

        return context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) {
                null
            } else {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
            }
        }
    }

    private suspend fun scanFile(context: Context, file: File) {
        suspendCancellableCoroutine<Unit> { continuation ->
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                null
            ) { _, _ ->
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
        }
    }
}
