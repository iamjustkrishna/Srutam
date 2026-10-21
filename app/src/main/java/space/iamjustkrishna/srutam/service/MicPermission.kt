package space.iamjustkrishna.srutam.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import space.iamjustkrishna.srutam.MainActivity

/**
 * The microphone is asked for on the first record tap, not during onboarding. Every start path
 * (in-app button, dock, tile, volume keys, notification) checks here first and, when the
 * permission is missing, opens the app to ask; the app starts the recording once it's granted.
 */
object MicPermission {
    private const val TAG = "MicPermission"

    fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    /** Opens MainActivity to ask for the mic, then start a recording tagged with [source]. */
    fun requestIntent(context: Context, source: String): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_REQUEST_MIC_SOURCE, source)
        }

    /** Best effort: background launch limits can block this from some services. */
    fun openAppToAsk(context: Context, source: String) {
        try {
            context.startActivity(requestIntent(context, source))
        } catch (e: Exception) {
            Log.w(TAG, "Could not open the app to ask for the microphone", e)
        }
    }
}
