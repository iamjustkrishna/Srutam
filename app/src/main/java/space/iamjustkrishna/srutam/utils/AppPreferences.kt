package space.iamjustkrishna.srutam.utils

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppPreferences {
    private const val PREFS_NAME = "srutam_prefs"

    // Keys
    private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    private const val KEY_CUSTOM_API_KEY = "custom_api_key"
    private const val KEY_CUSTOM_MODEL = "custom_model"
    private const val KEY_AI_PROVIDER = "ai_provider"
    private const val KEY_PERSISTENT_NOTIFICATION = "persistent_recording_notification"
    private const val KEY_FLOATING_DOCK = "floating_screen_dock"
    private const val KEY_COMPLETED_TASKS = "completed_action_items"
    private const val KEY_ARCHIVED_TASKS = "archived_action_item_ids"
    private const val KEY_AUTO_AI_ENABLED = "auto_ai_enabled"
    private const val KEY_BYOK_ONBOARDING_COMPLETED = "byok_onboarding_completed"
    private const val KEY_THEME_MODE = "theme_mode"

    // Theme Modes
    const val THEME_LIGHT = "LIGHT"
    const val THEME_COSMIC_DARK = "COSMIC_DARK"
    const val THEME_SYSTEM = "SYSTEM"

    private val _themeModeFlow = MutableStateFlow<String?>(null)
    val themeModeFlow: StateFlow<String?> = _themeModeFlow.asStateFlow()

    fun getThemeMode(context: Context): String {
        val mode = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM
        if (_themeModeFlow.value == null) {
            _themeModeFlow.value = mode
        }
        return mode
    }

    fun setThemeMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode)
            .apply()
        _themeModeFlow.value = mode
    }

    // AI Providers
    const val PROVIDER_SRUTAM_DEFAULT = "SRUTAM_DEFAULT"
    const val PROVIDER_OPENAI = "OPENAI"
    const val PROVIDER_ANTHROPIC = "ANTHROPIC"
    const val PROVIDER_GEMINI = "GEMINI"
    const val PROVIDER_GROQ = "GROQ"

    fun getCustomModel(context: Context, provider: String = getAIProvider(context)): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val providerModel = prefs.getString("${KEY_CUSTOM_MODEL}_$provider", "")
        if (!providerModel.isNullOrBlank()) return providerModel

        val legacySaved = prefs.getString(KEY_CUSTOM_MODEL, "").orEmpty()
        if (legacySaved.isNotBlank() && isModelValidForProvider(legacySaved, provider)) {
            return legacySaved
        }
        return getDefaultModelForProvider(provider)
    }

    fun getDefaultModelForProvider(provider: String): String {
        return when (provider) {
            PROVIDER_OPENAI -> "gpt-5.6-sol"
            PROVIDER_ANTHROPIC -> "claude-sonnet-4.6"
            PROVIDER_GEMINI -> "gemini-2.5-flash-lite"
            PROVIDER_GROQ -> "qwen3.6-27b"
            else -> "gemini-2.5-flash-lite"
        }
    }

    private fun isModelValidForProvider(model: String, provider: String): Boolean {
        return when (provider) {
            PROVIDER_OPENAI -> model.startsWith("gpt-") || model.startsWith("o")
            PROVIDER_ANTHROPIC -> model.startsWith("claude-")
            PROVIDER_GEMINI -> model.startsWith("gemini-")
            PROVIDER_GROQ -> model.startsWith("qwen") || model.startsWith("minimax") || model.startsWith("whisper") || model.startsWith("llama") || model.startsWith("deepseek")
            else -> false
        }
    }

    fun setCustomModel(context: Context, model: String, provider: String = getAIProvider(context)) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_MODEL, model.trim())
            .putString("${KEY_CUSTOM_MODEL}_$provider", model.trim())
            .apply()
    }

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

    private val _autoAiEnabledFlow = MutableStateFlow<Boolean?>(null)
    val autoAiEnabledFlow: StateFlow<Boolean?> = _autoAiEnabledFlow.asStateFlow()

    fun isAutoAiEnabled(context: Context): Boolean {
        val enabled = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_AI_ENABLED, false)
        if (_autoAiEnabledFlow.value == null) {
            _autoAiEnabledFlow.value = enabled
        }
        return enabled
    }

    fun setAutoAiEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_AI_ENABLED, enabled)
            .apply()
        _autoAiEnabledFlow.value = enabled
    }

    fun isByokOnboardingCompleted(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_BYOK_ONBOARDING_COMPLETED, false)
    }

    fun setByokOnboardingCompleted(context: Context, completed: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_BYOK_ONBOARDING_COMPLETED, completed)
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

    private const val KEY_FLOATING_DOCK_IS_LEFT = "floating_dock_is_left"
    private const val KEY_FLOATING_DOCK_Y = "floating_dock_y"

    fun isFloatingDockOnLeft(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_FLOATING_DOCK_IS_LEFT, true)
    }

    fun setFloatingDockOnLeft(context: Context, isLeft: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FLOATING_DOCK_IS_LEFT, isLeft)
            .apply()
    }

    fun getFloatingDockY(context: Context, defaultY: Int): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_FLOATING_DOCK_Y, defaultY)
    }

    fun setFloatingDockY(context: Context, y: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_FLOATING_DOCK_Y, y)
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
