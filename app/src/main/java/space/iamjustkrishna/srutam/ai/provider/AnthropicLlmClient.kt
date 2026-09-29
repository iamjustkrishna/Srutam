package space.iamjustkrishna.srutam.ai.provider

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import space.iamjustkrishna.srutam.utils.AppPreferences
import java.io.IOException
import java.util.concurrent.TimeUnit

class AnthropicLlmClient(
    private val context: Context,
    private val overrideModel: String? = null,
    private val overrideApiKey: String? = null
) : LlmClient {

    override val providerId: String = AppPreferences.PROVIDER_ANTHROPIC
    private val gson = Gson()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun generateText(prompt: String, timeoutMs: Long): String = withContext(Dispatchers.IO) {
        val apiKey = overrideApiKey?.takeIf { it.isNotBlank() }
            ?: AppPreferences.getCustomApiKey(context).takeIf { it.isNotBlank() }

        if (apiKey.isNullOrBlank()) {
            throw IllegalStateException("No Anthropic API key configured. Please add your key in Settings.")
        }

        val modelName = overrideModel?.takeIf { it.isNotBlank() }
            ?: AppPreferences.getCustomModel(context, AppPreferences.PROVIDER_ANTHROPIC).takeIf { it.isNotBlank() }
            ?: "claude-sonnet-4.6"

        Log.d(TAG, "Executing Anthropic query with model: $modelName")

        val payload = JsonObject().apply {
            addProperty("model", modelName)
            addProperty("max_tokens", 4096)
            val messagesArray = com.google.gson.JsonArray().apply {
                val messageObj = JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", prompt)
                }
                add(messageObj)
            }
            add("messages", messagesArray)
        }

        val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(ANTHROPIC_ENDPOINT)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(requestBody)
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.code == 429) {
                Log.w(TAG, "Anthropic HTTP 429 Rate Limit: $responseBody")
                throw RateLimitException("Anthropic rate limit exceeded", provider = providerId)
            }

            if (!response.isSuccessful) {
                Log.e(TAG, "Anthropic HTTP error code ${response.code}: $responseBody")
                throw IOException("Anthropic API error (code ${response.code}): $responseBody")
            }

            val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
            val contentArray = jsonObject.getAsJsonArray("content")
            if (contentArray == null || contentArray.size() == 0) {
                throw IllegalStateException("Invalid response structure from Anthropic: $responseBody")
            }

            val text = contentArray[0].asJsonObject.get("text")?.asString.orEmpty()
            if (text.isBlank()) {
                throw IllegalStateException("Empty content returned from Anthropic")
            }

            text
        } catch (e: RateLimitException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Anthropic request failed", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "AnthropicLlmClient"
        private const val ANTHROPIC_ENDPOINT = "https://api.anthropic.com/v1/messages"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
