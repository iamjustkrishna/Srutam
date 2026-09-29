package space.iamjustkrishna.srutam.ai.provider

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import space.iamjustkrishna.srutam.BuildConfig
import space.iamjustkrishna.srutam.utils.AppPreferences

class GeminiLlmClient(
    private val context: Context,
    private val overrideModel: String? = null,
    private val overrideApiKey: String? = null
) : LlmClient {

    override val providerId: String = AppPreferences.PROVIDER_GEMINI

    override suspend fun generateText(prompt: String, timeoutMs: Long): String = withContext(Dispatchers.IO) {
        val apiKey = overrideApiKey?.takeIf { it.isNotBlank() }
            ?: AppPreferences.getGeminiApiKey(context).takeIf { it.isNotBlank() }
            ?: AppPreferences.getCustomApiKey(context).takeIf { it.isNotBlank() }
            ?: BuildConfig.GEMINI_API_KEY.trim()

        if (apiKey.isBlank()) {
            throw IllegalStateException("No Gemini API key configured. Please add an API key in Settings.")
        }

        val primaryModel = overrideModel?.takeIf { it.isNotBlank() }
            ?: AppPreferences.getCustomModel(context, AppPreferences.PROVIDER_GEMINI).takeIf { it.isNotBlank() }
            ?: "gemini-3-flash-preview"

        Log.d(TAG, "Executing Gemini query with model: $primaryModel")

        try {
            executeModelCall(primaryModel, apiKey, prompt, timeoutMs)
        } catch (e: Exception) {
            val message = e.message.orEmpty()
            if (message.contains("404") || message.contains("NOT_FOUND", ignoreCase = true)) {
                val fallbackModel = if (primaryModel == "gemini-3-flash-preview") "gemini-2.5-flash-lite" else "gemini-3-flash-preview"
                Log.w(TAG, "Gemini model $primaryModel returned 404. Attempting fallback to $fallbackModel...")
                try {
                    return@withContext executeModelCall(fallbackModel, apiKey, prompt, timeoutMs)
                } catch (fallbackEx: Exception) {
                    handleGeminiException(fallbackEx)
                }
            }
            handleGeminiException(e)
        }
    }

    private suspend fun executeModelCall(modelName: String, apiKey: String, prompt: String, timeoutMs: Long): String {
        val generativeModel = GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            requestOptions = RequestOptions(timeout = timeoutMs)
        )
        val response = withTimeout(timeoutMs) {
            generativeModel.generateContent(prompt)
        }
        val text = response.text
        if (text.isNullOrBlank()) {
            throw IllegalStateException("Empty response from Gemini")
        }
        return text
    }

    private fun handleGeminiException(e: Exception): Nothing {
        val message = e.message.orEmpty()
        if (message.contains("429") || message.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || message.contains("Quota exceeded", ignoreCase = true)) {
            Log.w(TAG, "Gemini rate limited: $message")
            throw RateLimitException("Gemini quota/rate limit exceeded", provider = providerId, cause = e)
        }
        Log.e(TAG, "Gemini API error: $message", e)
        throw e
    }

    companion object {
        private const val TAG = "GeminiLlmClient"
    }
}
