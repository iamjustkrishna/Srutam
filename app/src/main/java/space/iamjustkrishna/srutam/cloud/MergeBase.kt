package space.iamjustkrishna.srutam.cloud

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import space.iamjustkrishna.srutam.data.signature.NotePayload
import space.iamjustkrishna.srutam.data.signature.NoteSignature
import java.io.File

/**
 * The common ancestor for merging a note with Srutam Cloud, like git's merge base.
 *
 * When a reinstall restores a note from the block inside its recording, that block is the last
 * state this phone knew. It's kept here, so a later sign-in can tell what changed on the phone
 * since (edits made before signing in) from what changed in the cloud (an agent's edits), and
 * merge the two instead of letting one side overwrite the other. A field both sides changed
 * goes to the newer edit; the other value is kept in [conflictsFile].
 */
class MergeBase(private val dir: File) {

    constructor(context: Context) : this(File(context.applicationContext.filesDir, "merge_base"))

    private fun baseFile(noteUuid: String) = File(dir, "${safe(noteUuid)}.json")

    private fun conflictsFile(noteUuid: String) = File(File(dir, "conflicts"), "${safe(noteUuid)}.json")

    fun save(payload: NotePayload) {
        runCatching {
            dir.mkdirs()
            baseFile(payload.noteUuid).writeText(NoteSignature.toJson(payload))
        }.onFailure { Log.w(TAG, "Could not keep the merge base for ${payload.noteUuid}", it) }
    }

    fun load(noteUuid: String): NotePayload? =
        baseFile(noteUuid).takeIf { it.isFile }?.let { runCatching { NoteSignature.fromJson(it.readText()) }.getOrNull() }

    /** Once merged, this phone and the cloud agree again: there's no base any more. */
    fun clear(noteUuid: String) {
        baseFile(noteUuid).delete()
    }

    /** Keeps the values a merge didn't use, so nothing is silently lost. */
    fun recordConflicts(noteUuid: String, conflicts: List<RestoreMerge.Conflict>) {
        if (conflicts.isEmpty()) return
        runCatching {
            val file = conflictsFile(noteUuid)
            file.parentFile?.mkdirs()
            val earlier = if (file.isFile) {
                runCatching { gson.fromJson(file.readText(), Array<RestoreMerge.Conflict>::class.java)?.toList() }.getOrNull().orEmpty()
            } else {
                emptyList()
            }
            file.writeText(gson.toJson(earlier + conflicts))
        }.onFailure { Log.w(TAG, "Could not keep the merge conflicts for $noteUuid", it) }
    }

    fun conflicts(noteUuid: String): List<RestoreMerge.Conflict> {
        val file = conflictsFile(noteUuid)
        if (!file.isFile) return emptyList()
        return runCatching { gson.fromJson(file.readText(), Array<RestoreMerge.Conflict>::class.java)?.toList() }
            .getOrNull().orEmpty()
    }

    private fun safe(noteUuid: String) = noteUuid.replace(Regex("[^A-Za-z0-9-]"), "_")

    companion object {
        private const val TAG = "MergeBase"
        private val gson = Gson()
    }
}
