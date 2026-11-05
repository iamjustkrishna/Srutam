package space.iamjustkrishna.srutam.utils

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * Whether Srutam probably left recordings on this phone before. Android lets an app see folders in
 * shared storage without any permission, but not what is inside them, so a Music/Srutam folder that
 * already exists on the very first launch is the only sign there is, and it can't tell a folder full
 * of recordings from an empty one. The app only creates the folder when it saves a recording, so
 * an empty one is rare (every recording deleted, or an install from before this was fixed).
 * New users are never asked about earlier recordings; returning ones are offered to look.
 */
object PriorInstall {
    private const val PREFS = "srutam_prior_install"
    private const val KEY_CHECKED = "checked"
    private const val KEY_DETECTED = "detected"
    private const val KEY_PROMPTED = "prompted"

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Runs on the first launch, before anything creates the folder. Later calls do nothing. */
    fun checkOnce(context: Context, musicDir: File? = null) {
        val prefs = prefs(context)
        if (prefs.getBoolean(KEY_CHECKED, false)) return
        val dir = File(musicDir ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Srutam")
        val existed = runCatching { dir.isDirectory }.getOrDefault(false)
        prefs.edit().putBoolean(KEY_CHECKED, true).putBoolean(KEY_DETECTED, existed).apply()
    }

    /** An earlier install left a Music/Srutam folder. */
    fun recordingsFromBefore(context: Context): Boolean = prefs(context).getBoolean(KEY_DETECTED, false)

    /** Recordings from before exist, and Srutam can't see them yet. */
    fun shouldOfferRestore(context: Context): Boolean =
        recordingsFromBefore(context) && !StoragePermission.isGranted(context)

    /**
     * Time to ask for access to Music and audio: an earlier install left a Music/Srutam folder, the access
     * isn't there yet, and it hasn't been asked for already. People with no such folder (new users) are never
     * asked; the folder is the only reason to.
     */
    fun shouldAskForAccess(context: Context): Boolean =
        recordingsFromBefore(context) && !StoragePermission.isGranted(context) && !wasPrompted(context)

    fun wasPrompted(context: Context): Boolean = prefs(context).getBoolean(KEY_PROMPTED, false)

    fun markPrompted(context: Context) = prefs(context).edit().putBoolean(KEY_PROMPTED, true).apply()
}
