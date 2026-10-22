package space.iamjustkrishna.srutam.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import space.iamjustkrishna.srutam.data.signature.NoteBlock
import java.io.File

/** Shares a recording's audio without the note block (transcript, insights) stored inside it. */
object AudioShare {

    /** Throws if the file can't be copied or shared; callers show their own message. */
    fun share(context: Context, file: File) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, cleanUri(context, file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Voice Note"))
    }

    /** A shareable URI for a copy of [file] with the note block removed, so it carries only the sound. */
    fun cleanUri(context: Context, file: File): Uri {
        val dir = File(context.cacheDir, "shared_audio").apply {
            mkdirs()
            listFiles()?.forEach { it.delete() }
        }
        val copy = File(dir, file.name)
        file.copyTo(copy, overwrite = true)
        NoteBlock.strip(copy)
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", copy)
    }
}
