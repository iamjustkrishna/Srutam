package space.iamjustkrishna.srutam.navigation

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import space.iamjustkrishna.srutam.data.AppDatabase
import space.iamjustkrishna.srutam.utils.AudioFileReader

/** The note made by the first recording, recorded from the welcome screen. */
internal object FirstNote {

    /**
     * The id of the note for the recording at [path] (the newest recording when unknown), once
     * the recording service has saved it, or null if it doesn't appear within [timeoutMs] (the
     * recording was discarded, for example).
     */
    suspend fun awaitSavedId(context: Context, path: String?, timeoutMs: Long = 8_000L): Long? = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getDatabase(context).recordingDao()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val target = path ?: AudioFileReader.getAudioFiles().maxByOrNull { it.timestamp }?.filePath
            if (target != null) {
                dao.getRecordingByPath(target)?.let { return@withContext it.id }
            }
            delay(250)
        }
        null
    }
}
