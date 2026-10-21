package space.iamjustkrishna.srutam.data.signature

import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import space.iamjustkrishna.srutam.data.AppDatabase
import space.iamjustkrishna.srutam.utils.AudioFileReader
import space.iamjustkrishna.srutam.utils.AudioStorage
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile

/**
 * Recordings left by a previous install can be read but not written, so their notes can't be
 * saved into them. This tracks those notes and asks the user, once for all of them, to let
 * Srutam modify the files (Android's "Allow Srutam to modify N audio files?" dialog). Once
 * allowed, each note's block is written through the file's MediaStore URI, so the next
 * reinstall restores the note from the file instead of running AI again.
 */
object BlockWriteAccess {
    private const val TAG = "BlockWriteAccess"
    private const val PREFS = "srutam_block_access"
    private const val KEY_PENDING = "pending_ids"
    private const val KEY_AUTO_ASKED = "auto_asked"

    private val _pending = MutableStateFlow<Set<Long>?>(null)

    /** Notes whose block couldn't be written because the file belongs to a previous install. */
    fun pending(context: Context): StateFlow<Set<Long>?> {
        if (_pending.value == null) _pending.value = load(context)
        return _pending.asStateFlow()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun load(context: Context): Set<Long> =
        prefs(context).getStringSet(KEY_PENDING, emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()

    @Synchronized
    private fun update(context: Context, change: (Set<Long>) -> Set<Long>) {
        val current = _pending.value ?: load(context)
        val next = change(current)
        if (next == current) {
            _pending.value = current
            return
        }
        prefs(context).edit().putStringSet(KEY_PENDING, next.map { it.toString() }.toSet()).apply()
        _pending.value = next
    }

    fun markPending(context: Context, recordingId: Long) = update(context) { it + recordingId }

    fun clearPending(context: Context, recordingId: Long) = update(context) { it - recordingId }

    /** The automatic prompt shows once; after that the Home card and Settings offer it. */
    fun wasAutoAsked(context: Context) = prefs(context).getBoolean(KEY_AUTO_ASKED, false)

    fun markAutoAsked(context: Context) = prefs(context).edit().putBoolean(KEY_AUTO_ASKED, true).apply()

    /** Android only offers a batch "modify" dialog from Android 11. */
    val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /**
     * Writes [json] into [file] through its MediaStore URI. Works for a previous install's file
     * once the user has allowed it; returns false otherwise.
     */
    fun writeViaUri(context: Context, file: File, json: String): Boolean {
        val uri = runCatching { AudioStorage.findAudioUri(context, file) }.getOrNull() ?: return false
        return try {
            context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                FileInputStream(pfd.fileDescriptor).channel.use { reader ->
                    FileOutputStream(pfd.fileDescriptor).channel.use { writer ->
                        NoteBlock.write(NoteBlock.ChannelSeekable(reader, writer), json)
                    }
                }
                true
            } ?: false
        } catch (e: SecurityException) {
            false
        } catch (e: IOException) {
            Log.w(TAG, "Writing the block through MediaStore failed for ${file.name}", e)
            false
        }
    }

    /** True when this install can write [file] by path (it made the file). Opens it without changing it. */
    fun isWritableByPath(file: File): Boolean = try {
        RandomAccessFile(file, "rw").use { true }
    } catch (e: IOException) {
        false
    } catch (e: SecurityException) {
        false
    }

    /**
     * The system dialog asking to modify the recordings this install can't write: every pending
     * note's file plus every other previous-install file in Music/Srutam, so notes AI fills in
     * later can be saved without asking again. Null when there's nothing to ask for or this
     * Android can't ask.
     */
    suspend fun createRequest(context: Context): IntentSender? = withContext(Dispatchers.IO) {
        if (!isSupported) return@withContext null
        val dao = AppDatabase.getDatabase(context).recordingDao()
        val pendingFiles = pending(context).value.orEmpty().mapNotNull { id ->
            val file = dao.getRecordingById(id)?.audioFilePath?.let(::File)
            if (file == null || !file.isFile) {
                clearPending(context, id)
                null
            } else {
                file
            }
        }
        if (pendingFiles.isEmpty()) return@withContext null
        val otherForeign = AudioFileReader.getAudioFiles()
            .map { File(it.filePath) }
            .filter { it.isFile && !isWritableByPath(it) }
        val uris = (pendingFiles + otherForeign)
            .distinctBy { it.absolutePath }
            .mapNotNull { AudioStorage.findAudioUri(context, it) }
        if (uris.isEmpty()) return@withContext null
        runCatching { MediaStore.createWriteRequest(context.contentResolver, uris).intentSender }
            .onFailure { Log.w(TAG, "Could not create the write request", it) }
            .getOrNull()
    }

    /** After the user allowed it: writes every pending note's block now. */
    fun onAccessGranted(context: Context) {
        pending(context).value.orEmpty().forEach { NoteSignatureWorker.enqueue(context, it) }
    }
}
