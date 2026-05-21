package space.iamjustkrishna.srutam.utils

import android.content.Context

object AppPreferences {
    private const val PREFS_NAME = "srutam_prefs"
    private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    private const val KEY_PERSISTENT_NOTIFICATION = "persistent_recording_notification"

    fun getGeminiApiKey(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_GEMINI_API_KEY, "")
            .orEmpty()
    }

    fun setGeminiApiKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_GEMINI_API_KEY, key.trim())
            .apply()
    }

    fun isPersistentNotificationEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PERSISTENT_NOTIFICATION, false)
    }

    fun setPersistentNotificationEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PERSISTENT_NOTIFICATION, enabled)
            .apply()
    }
}
