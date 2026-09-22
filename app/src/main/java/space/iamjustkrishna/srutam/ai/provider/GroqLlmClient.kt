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
import space.iamjustkrishna.srutam.BuildConfig
import space.iamjustkrishna.srutam.utils.AppPreferences
import java.io.IOException
import java.util.concurrent.TimeUnit

class GroqLlmClient(
    private val context: Context,
    private val overrideModel: String? = null,
    private val overrideApiKey: String? = null
) : LlmClient {

    override val providerId: String = AppPreferences.PROVIDER_GROQ
    private val gson = Gson()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun generateText(prompt: String, timeoutMs: Long): String = withContext(Dispatchers.IO) {
        val apiKey = overrideApiKey?.takeIf { it.isNotBlank() }
            ?: AppPreferences.getCustomApiKey(context).takeIf { it.isNotBlank() }
            ?: BuildConfig.GROQ_API_KEY.trim()

        if (apiKey.isBlank()) {
            throw IllegalStateException("No Groq API key configured. Please add your Groq API key in Settings.")
        }

        val modelName = overrideModel?.takeIf { it.isNotBlank() }
            ?: AppPreferences.getCustomModel(context, AppPreferences.PROVIDER_GROQ).takeIf { it.isNotBlank() }
            ?: "qwen/qwen3.8-27b"

        Log.d(TAG, "Executing Groq query with model: $modelName")

        val payload = JsonObject().apply {
            addProperty("model", modelName)
            val messagesArray = com.google.gson.JsonArray().apply {
                val messageObj = JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", prompt)
                }
                add(messageObj)
            }
            add("messages", messagesArray)
            addProperty("temperature", 0.2)
        }

        val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(GROQ_ENDPOINT)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", "SrutamAndroid/2.0")
            .post(requestBody)
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.code == 429) {
                Log.w(TAG, "Groq HTTP 429 Rate Limit: $responseBody")
                throw RateLimitException("Groq rate limit exceeded", provider = providerId)
            }

            if (!response.isSuccessful) {
                Log.e(TAG, "Groq HTTP error code ${response.code}: $responseBody")
                throw IOException("Groq API error (code ${response.code}): $responseBody")
            }

            val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
            val choices = jsonObject.getAsJsonArray("choices")
            if (choices == null || choices.size() == 0) {
                throw IllegalStateException("Invalid response structure from Groq: $responseBody")
            }

            val content = choices[0].asJsonObject
                .getAsJsonObject("message")
                ?.get("content")
                ?.asString
                .orEmpty()

            if (content.isBlank()) {
                throw IllegalStateException("Empty content returned from Groq")
            }

            content
        } catch (e: RateLimitException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Groq request failed", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "GroqLlmClient"
        private const val GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
