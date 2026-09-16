package space.iamjustkrishna.srutam.cloud

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import space.iamjustkrishna.srutam.BuildConfig
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.InsightKind
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.utils.AppPreferences
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class RemoteActionItemUpdate(
    val id: String,
    val noteId: String,
    val description: String,
    val isCompleted: Boolean,
    val completedBy: String?,
    val completedAt: String?
)

class SupabaseCloudClient(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val baseUrl: String by lazy {
        BuildConfig.SUPABASE_URL.trimEnd('/')
    }

    private val anonKey: String by lazy {
        BuildConfig.SUPABASE_ANON_KEY
    }

    private fun getAuthHeaders(requestBuilder: Request.Builder): Request.Builder {
        val accessToken = AppPreferences.getCloudAccessToken(context)
        requestBuilder.addHeader("apikey", anonKey)
        if (!accessToken.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $accessToken")
        }
        return requestBuilder
    }

    private fun formatIso8601(timestampMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(timestampMillis))
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Uploads or syncs a voice recording and its action items to Supabase.
     * Returns the Supabase UUID string of the note.
     */
    suspend fun uploadNote(recording: Recording, insights: List<InsightEntity> = emptyList()): Result<String> = withContext(Dispatchers.IO) {
        try {
            val userId = AppPreferences.getCloudUserId(context)
                ?: return@withContext Result.failure(IllegalStateException("User not signed in to cloud"))

            val noteJson = JsonObject().apply {
                addProperty("user_id", userId)
                addProperty("client_recording_id", recording.id)
                addProperty("title", recording.name)
                addProperty("transcript", recording.transcript)
                addProperty("summary", recording.summary)
                addProperty("wiifm", recording.wiifm)
                addProperty("ai_status", recording.aiStatus)
                addProperty("duration_ms", recording.duration)
                addProperty("is_private", recording.isPrivate)
                addProperty("timestamp", formatIso8601(recording.timestamp))

                // Parse keyPoints JSON array safely
                try {
                    if (!recording.keyPoints.isNullOrBlank()) {
                        add("key_points", JsonParser.parseString(recording.keyPoints))
                    } else {
                        add("key_points", JsonArray())
                    }
                } catch (e: Exception) {
                    add("key_points", JsonArray())
                }
            }

            val isExistingCloudNote = !recording.cloudId.isNullOrBlank()
            val noteId: String

            if (isExistingCloudNote) {
                val cloudId = recording.cloudId!!
                val patchBody = JsonObject().apply {
                    addProperty("title", recording.name)
                    addProperty("transcript", recording.transcript)
                    addProperty("summary", recording.summary)
                    addProperty("wiifm", recording.wiifm)
                    addProperty("ai_status", recording.aiStatus)
                    addProperty("duration_ms", recording.duration)
                    addProperty("is_private", recording.isPrivate)
                    addProperty("updated_at", formatIso8601(System.currentTimeMillis()))
                    try {
                        if (!recording.keyPoints.isNullOrBlank()) {
                            add("key_points", JsonParser.parseString(recording.keyPoints))
                        } else {
                            add("key_points", JsonArray())
                        }
                    } catch (e: Exception) {
                        add("key_points", JsonArray())
                    }
                }

                val patchRequest = Request.Builder()
                    .url("$baseUrl/rest/v1/notes?id=eq.$cloudId")
                    .patch(patchBody.toString().toRequestBody(jsonMediaType))
                getAuthHeaders(patchRequest)

                client.newCall(patchRequest.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        val err = response.body?.string() ?: "HTTP ${response.code}"
                        return@withContext Result.failure(Exception("Failed to update note: $err"))
                    }
                }
                noteId = cloudId

                // Clean existing action items before re-inserting
                val deleteItemsRequest = Request.Builder()
                    .url("$baseUrl/rest/v1/action_items?note_id=eq.$noteId")
                    .delete()
                getAuthHeaders(deleteItemsRequest)
                client.newCall(deleteItemsRequest.build()).execute().close()
            } else {
                val postRequest = Request.Builder()
                    .url("$baseUrl/rest/v1/notes")
                    .addHeader("Prefer", "return=representation")
                    .post(noteJson.toString().toRequestBody(jsonMediaType))
                getAuthHeaders(postRequest)

                val generatedId = client.newCall(postRequest.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        val err = response.body?.string() ?: "HTTP ${response.code}"
                        return@withContext Result.failure(Exception("Failed to upload note: $err"))
                    }
                    val responseBody = response.body?.string() ?: ""
                    val jsonArray = JsonParser.parseString(responseBody).asJsonArray
                    if (jsonArray.isEmpty) {
                        return@withContext Result.failure(Exception("Empty response from Supabase notes table"))
                    }
                    jsonArray[0].asJsonObject.get("id").asString
                }
                noteId = generatedId
            }

            // Now upload any action items / next steps
            val actionItems = insights.filter { it.kind == InsightKind.ACTION }
            if (actionItems.isNotEmpty()) {
                val itemsArray = JsonArray()
                for (item in actionItems) {
                    val isDone = item.status == space.iamjustkrishna.srutam.data.InsightStatus.COMPLETED
                    val itemJson = JsonObject().apply {
                        addProperty("note_id", noteId)
                        addProperty("user_id", userId)
                        addProperty("description", item.text)
                        addProperty("is_completed", isDone)
                        if (isDone) {
                            addProperty("completed_by", "user")
                            addProperty("completed_at", formatIso8601(item.completedAt ?: System.currentTimeMillis()))
                        }
                    }
                    itemsArray.add(itemJson)
                }

                val itemsRequest = Request.Builder()
                    .url("$baseUrl/rest/v1/action_items")
                    .post(itemsArray.toString().toRequestBody(jsonMediaType))
                getAuthHeaders(itemsRequest)

                client.newCall(itemsRequest.build()).execute().close()
            }

            Result.success(noteId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates a new Personal Access Token (API Key) for IDEs / Agents.
     * Returns Pair(plainTextKey, keyPrefix). The plainTextKey is only shown once to user.
     */
    suspend fun createApiKey(name: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val userId = AppPreferences.getCloudUserId(context)
                ?: return@withContext Result.failure(IllegalStateException("User not signed in"))

            val randomBytes = ByteArray(24)
            SecureRandom().nextBytes(randomBytes)
            val randomHex = randomBytes.joinToString("") { "%02x".format(it) }
            val plainKey = "srtm_live_$randomHex"
            val keyHash = sha256(plainKey)
            val keyPrefix = "srtm_live_${randomHex.take(6)}..."

            val body = JsonObject().apply {
                addProperty("user_id", userId)
                addProperty("key_hash", keyHash)
                addProperty("key_prefix", keyPrefix)
                addProperty("name", name.ifBlank { "Developer Key" })
            }

            val requestBuilder = Request.Builder()
                .url("$baseUrl/rest/v1/api_keys")
                .addHeader("Prefer", "return=representation")
                .post(body.toString().toRequestBody(jsonMediaType))

            getAuthHeaders(requestBuilder)

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: "HTTP ${response.code}"
                    return@withContext Result.failure(Exception("Failed to generate API Key: $err"))
                }
                Result.success(Pair(plainKey, keyPrefix))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lists active API Keys for this user.
     */
    suspend fun listApiKeys(): Result<List<ApiKeyItem>> = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder()
                .url("$baseUrl/rest/v1/api_keys?select=id,name,key_prefix,created_at,last_used_at&revoked_at=is.null&order=created_at.desc")
                .get()

            getAuthHeaders(requestBuilder)

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: "HTTP ${response.code}"
                    return@withContext Result.failure(Exception("Failed to fetch API keys: $err"))
                }

                val responseBody = response.body?.string() ?: "[]"
                val jsonArray = JsonParser.parseString(responseBody).asJsonArray
                val list = mutableListOf<ApiKeyItem>()

                for (el in jsonArray) {
                    val obj = el.asJsonObject
                    list.add(
                        ApiKeyItem(
                            id = obj.get("id").asString,
                            name = obj.get("name").asString,
                            keyPrefix = obj.get("key_prefix").asString,
                            createdAt = obj.get("created_at").asString,
                            lastUsedAt = if (obj.has("last_used_at") && !obj.get("last_used_at").isJsonNull) obj.get("last_used_at").asString else null
                        )
                    )
                }

                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Revokes an active API Key.
     */
    suspend fun revokeApiKey(keyId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("revoked_at", formatIso8601(System.currentTimeMillis()))
            }

            val requestBuilder = Request.Builder()
                .url("$baseUrl/rest/v1/api_keys?id=eq.$keyId")
                .patch(body.toString().toRequestBody(jsonMediaType))

            getAuthHeaders(requestBuilder)

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: "HTTP ${response.code}"
                    return@withContext Result.failure(Exception("Failed to revoke API key: $err"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pulls updates made by agents (e.g. action items completed by Cursor/Antigravity).
     */
    suspend fun pullCompletedActionItems(): Result<List<RemoteActionItemUpdate>> = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder()
                .url("$baseUrl/rest/v1/action_items?select=id,note_id,description,is_completed,completed_by,completed_at&is_completed=eq.true")
                .get()

            getAuthHeaders(requestBuilder)

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: "HTTP ${response.code}"
                    return@withContext Result.failure(Exception("Failed to pull remote action items: $err"))
                }

                val responseBody = response.body?.string() ?: "[]"
                val jsonArray = JsonParser.parseString(responseBody).asJsonArray
                val list = mutableListOf<RemoteActionItemUpdate>()

                for (el in jsonArray) {
                    val obj = el.asJsonObject
                    list.add(
                        RemoteActionItemUpdate(
                            id = obj.get("id").asString,
                            noteId = obj.get("note_id").asString,
                            description = obj.get("description").asString,
                            isCompleted = obj.get("is_completed").asBoolean,
                            completedBy = if (obj.has("completed_by") && !obj.get("completed_by").isJsonNull) obj.get("completed_by").asString else null,
                            completedAt = if (obj.has("completed_at") && !obj.get("completed_at").isJsonNull) obj.get("completed_at").asString else null
                        )
                    )
                }

                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
