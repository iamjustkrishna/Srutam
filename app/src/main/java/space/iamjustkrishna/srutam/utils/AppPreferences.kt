package space.iamjustkrishna.srutam.utils

import android.content.Context

object AppPreferences {
    private const val PREFS_NAME = "srutam_prefs"

    // Keys
    private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    private const val KEY_CUSTOM_API_KEY = "custom_api_key"
    private const val KEY_AI_PROVIDER = "ai_provider"
    private const val KEY_PERSISTENT_NOTIFICATION = "persistent_recording_notification"
    private const val KEY_FLOATING_DOCK = "floating_screen_dock"
    private const val KEY_COMPLETED_TASKS = "completed_action_items"
    private const val KEY_ARCHIVED_TASKS = "archived_action_item_ids"

    // AI Providers
    const val PROVIDER_SRUTAM_DEFAULT = "SRUTAM_DEFAULT"
    const val PROVIDER_OPENAI = "OPENAI"
    const val PROVIDER_ANTHROPIC = "ANTHROPIC"
    const val PROVIDER_GEMINI = "GEMINI"
    const val PROVIDER_GROQ = "GROQ"

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

    fun getCustomApiKey(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_API_KEY, "")
            .orEmpty()
    }

    fun setCustomApiKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_API_KEY, key.trim())
            .apply()
    }

    fun getAIProvider(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_AI_PROVIDER, PROVIDER_SRUTAM_DEFAULT) ?: PROVIDER_SRUTAM_DEFAULT
    }

    fun setAIProvider(context: Context, provider: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_AI_PROVIDER, provider)
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

    fun isFloatingDockEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_FLOATING_DOCK, false)
    }

    fun setFloatingDockEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FLOATING_DOCK, enabled)
            .apply()
    }

    fun getCompletedActionItemIds(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_COMPLETED_TASKS, emptySet()) ?: emptySet()
    }

    fun toggleActionItemCompleted(context: Context, itemId: String): Boolean {
        val current = getCompletedActionItemIds(context).toMutableSet()
        val isNowCompleted = if (current.contains(itemId)) {
            current.remove(itemId)
            false
        } else {
            current.add(itemId)
            true
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_COMPLETED_TASKS, current)
            .apply()
        return isNowCompleted
    }

    fun getArchivedActionItemIds(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_ARCHIVED_TASKS, emptySet()) ?: emptySet()
    }

    fun archiveCompletedActionItems(
        context: Context,
        itemIdsToArchive: Set<String> = getCompletedActionItemIds(context)
    ) {
        val archived = getArchivedActionItemIds(context).toMutableSet()
        archived.addAll(itemIdsToArchive)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_ARCHIVED_TASKS, archived)
            .apply()
    }

    fun unarchiveAllActionItems(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ARCHIVED_TASKS)
            .apply()
    }

    private const val KEY_DISMISSED_THEMES = "dismissed_insights_themes"

    fun getDismissedThemes(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_DISMISSED_THEMES, emptySet()) ?: emptySet()
    }

    fun dismissTheme(context: Context, themeKey: String) {
        val current = getDismissedThemes(context).toMutableSet()
        current.add(themeKey.lowercase().trim())
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_DISMISSED_THEMES, current)
            .apply()
    }

    fun resetDismissedThemes(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_DISMISSED_THEMES)
            .apply()
    }
}
