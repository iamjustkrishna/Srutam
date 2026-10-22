package space.iamjustkrishna.srutam.utils

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * Whether Srutam was installed on this phone before. Android lets an app see folders in shared
 * storage without any permission, just not other installs' files, so a Music/Srutam folder that
 * already exists on the very first launch means an earlier install left recordings there.
 * New users are never asked about earlier recordings; returning ones are offered to restore.
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

    fun wasPrompted(context: Context): Boolean = prefs(context).getBoolean(KEY_PROMPTED, false)

    fun markPrompted(context: Context) = prefs(context).edit().putBoolean(KEY_PROMPTED, true).apply()
}
