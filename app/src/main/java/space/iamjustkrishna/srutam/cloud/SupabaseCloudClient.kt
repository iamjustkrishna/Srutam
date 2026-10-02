package space.iamjustkrishna.srutam.cloud

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

/**
 * @param baseUrlOverride points the client at a local test server instead of the
 *   real project. Production always passes null and reads BuildConfig, so this
 *   cannot change shipped behaviour; it exists so the upload paths can be tested
 *   against real HTTP responses, including the failure codes that used to be
 *   silently discarded.
 */
class SupabaseCloudClient(
    private val context: Context,
    private val baseUrlOverride: String? = null
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private companion object {
        /** Shared by every SupabaseCloudClient instance so key creations can never overlap. */
        val createKeyMutex = Mutex()
    }

    /** Turns a PostgREST error into a typed exception the UI can show as a friendly message. */
    private fun mapCreateKeyError(httpCode: Int, body: String): Exception = when {
        body.contains("KEY_LIMIT_REACHED") -> ApiKeyLimitException()
        httpCode == 409 || body.contains("23505") || body.contains("uq_api_keys_active_name") ->
            ApiKeyNameException("You already have a key with that name. Pick a different name.")
        body.contains("api_keys_active_name_len") || body.contains("23514") ->
            ApiKeyNameException("Name must be 1 to ${ApiKeyRules.MAX_NAME_LENGTH} characters.")
        else -> Exception("Failed to generate API Key: $body")
    }

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val baseUrl: String by lazy {
        (baseUrlOverride ?: BuildConfig.SUPABASE_URL).trimEnd('/')
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
     * Runs one child-table upsert and fails loudly.
     *
     * The three child uploads below used to discard their response entirely
     * (`.execute().close()`), so a missing table or column returned 404/400 in
     * silence and uploadNote() still reported success - the note was marked
     * SYNCED with none of its action items, insights or reminders attached.
     * Throwing here lets the outer catch turn it into Result.failure, which
     * CloudSyncWorker already handles by resetting the note to PENDING for retry.
     *
     * The response body is included in the message because that is what names
     * the missing relation or column when a migration has not been applied.
     */
    private fun executeChildUpsert(request: Request, label: String) {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string()?.takeIf { it.isNotBlank() } ?: ""
                throw Exception("Failed to upload $label: HTTP ${response.code} $err".trim())
            }
        }
    }

    /**
     * Uploads or syncs a voice recording and its action items to Supabase.
     * Returns the Supabase UUID string of the note.
     */
    suspend fun uploadNote(
        recording: Recording,
        insights: List<InsightEntity> = emptyList(),
        reminders: List<space.iamjustkrishna.srutam.data.ReminderEntity> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
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

            // Upload action items, ideas/decisions and reminders as UPSERTS keyed by the stable
            // local id (client_insight_id / client_reminder_id), not delete-then-reinsert. The old
            // delete+reinsert approach gave every cloud row a fresh id on each resync, which silently
            // wiped any completion an agent made via update_action_item between syncs (see migration 07).
            val actionItems = insights.filter { it.kind == InsightKind.ACTION }
            if (actionItems.isNotEmpty()) {
                val itemsArray = JsonArray()
                for (item in actionItems) {
                    val isDone = item.status == space.iamjustkrishna.srutam.data.InsightStatus.COMPLETED
                    val itemJson = JsonObject().apply {
                        addProperty("note_id", noteId)
                        addProperty("user_id", userId)
                        addProperty("client_insight_id", item.id)
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
                    .url("$baseUrl/rest/v1/action_items?on_conflict=note_id,client_insight_id")
                    .addHeader("Prefer", "resolution=merge-duplicates")
                    .post(itemsArray.toString().toRequestBody(jsonMediaType))
                getAuthHeaders(itemsRequest)

                executeChildUpsert(itemsRequest.build(), "action items")
            }

            // Ideas and decisions: same upsert shape, separate table so existing action-item
            // tooling (and the MCP tools built on it) is untouched.
            val noteInsights = insights.filter { it.kind == InsightKind.IDEA || it.kind == InsightKind.DECISION }
            if (noteInsights.isNotEmpty()) {
                val insightsArray = JsonArray()
                for (item in noteInsights) {
                    val insightJson = JsonObject().apply {
                        addProperty("note_id", noteId)
                        addProperty("user_id", userId)
                        addProperty("client_insight_id", item.id)
                        addProperty("kind", item.kind.lowercase())
                        addProperty("text", item.text)
                        addProperty("evidence", item.evidence)
                        addProperty("rationale", item.rationale)
                        // Lifecycle, so an archived or completed idea does not read as open
                        // to an agent (migration 08).
                        addProperty("status", item.status)
                        item.completedAt?.let { addProperty("completed_at", formatIso8601(it)) }
                        item.archivedAt?.let { addProperty("archived_at", formatIso8601(it)) }
                        // Provenance: "convert to next step" creates a NEW action row linked
                        // by sourceInsightId rather than mutating kind, so without these an
                        // agent sees the idea and its derived task as unrelated rows.
                        addProperty("source_insight_id", item.sourceInsightId)
                        addProperty("source_reminder_id", item.sourceReminderId)
                    }
                    insightsArray.add(insightJson)
                }

                val insightsRequest = Request.Builder()
                    .url("$baseUrl/rest/v1/note_insights?on_conflict=note_id,client_insight_id")
                    .addHeader("Prefer", "resolution=merge-duplicates")
                    .post(insightsArray.toString().toRequestBody(jsonMediaType))
                getAuthHeaders(insightsRequest)

                executeChildUpsert(insightsRequest.build(), "ideas and decisions")
            }

            // Reminders: read-only through MCP, but still worth syncing for visibility.
            if (reminders.isNotEmpty()) {
                val remindersArray = JsonArray()
                for (reminder in reminders) {
                    val reminderJson = JsonObject().apply {
                        addProperty("note_id", noteId)
                        addProperty("user_id", userId)
                        addProperty("client_reminder_id", reminder.id)
                        addProperty("title", reminder.title)
                        if (reminder.eventTimeMs != null) {
                            addProperty("event_time", formatIso8601(reminder.eventTimeMs))
                        }
                        addProperty("original_text", reminder.originalText)
                        addProperty("person", reminder.person)
                        addProperty("location", reminder.location)
                        addProperty("type", reminder.type)
                        addProperty("status", reminder.status)
                        // Review state: without this an agent cannot tell a freshly
                        // extracted guess from a reminder the user reviewed and locked in.
                        addProperty("needs_review", reminder.needsReview)
                        reminder.confirmedAt?.let { addProperty("confirmed_at", formatIso8601(it)) }
                        // Resolved local time, so an agent can render "May 20" without a
                        // clock time rather than inventing midnight UTC.
                        addProperty("time_precision", reminder.timePrecision)
                        addProperty("local_date", reminder.localDate)
                        addProperty("local_time", reminder.localTime)
                        addProperty("zone_id", reminder.zoneId)
                        addProperty("linked_task_id", reminder.linkedTaskId)
                    }
                    remindersArray.add(reminderJson)
                }

                val remindersRequest = Request.Builder()
                    .url("$baseUrl/rest/v1/reminders?on_conflict=note_id,client_reminder_id")
                    .addHeader("Prefer", "resolution=merge-duplicates")
                    .post(remindersArray.toString().toRequestBody(jsonMediaType))
                getAuthHeaders(remindersRequest)

                executeChildUpsert(remindersRequest.build(), "reminders")
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
        // One creation at a time per process: overlapping calls (double taps, other screens) queue here,
        // and each re-checks the limit and names against a fresh list before inserting.
        createKeyMutex.withLock {
            try {
                val cleanName = ApiKeyRules.normalizeName(name)
                val active = listApiKeys().getOrElse { return@withLock Result.failure(it) }
                if (ApiKeyRules.limitReached(active.size)) {
                    return@withLock Result.failure(ApiKeyLimitException())
                }
                ApiKeyRules.validateName(cleanName, active.map { it.name })?.let {
                    return@withLock Result.failure(ApiKeyNameException(it))
                }
                insertApiKey(cleanName)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun insertApiKey(name: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
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
                addProperty("name", name)
            }

            val requestBuilder = Request.Builder()
                .url("$baseUrl/rest/v1/api_keys")
                .addHeader("Prefer", "return=representation")
                .post(body.toString().toRequestBody(jsonMediaType))
            getAuthHeaders(requestBuilder)

            var response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful && (response.code == 401 || response.code == 403)) {
                response.close()
                val refreshRes = SupabaseAuthManager(context).refreshSession()
                if (refreshRes.isSuccess) {
                    val retryBuilder = Request.Builder()
                        .url("$baseUrl/rest/v1/api_keys")
                        .addHeader("Prefer", "return=representation")
                        .post(body.toString().toRequestBody(jsonMediaType))
                    getAuthHeaders(retryBuilder)
                    response = client.newCall(retryBuilder.build()).execute()
                }
            }

            response.use { res ->
                if (!res.isSuccessful) {
                    val err = res.body?.string() ?: "HTTP ${res.code}"
                    return@withContext Result.failure(mapCreateKeyError(res.code, err))
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

            var response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful && (response.code == 401 || response.code == 403)) {
                response.close()
                val refreshRes = SupabaseAuthManager(context).refreshSession()
                if (refreshRes.isSuccess) {
                    val retryBuilder = Request.Builder()
                        .url("$baseUrl/rest/v1/api_keys?select=id,name,key_prefix,created_at,last_used_at&revoked_at=is.null&order=created_at.desc")
                        .get()
                    getAuthHeaders(retryBuilder)
                    response = client.newCall(retryBuilder.build()).execute()
                }
            }

            response.use { res ->
                if (!res.isSuccessful) {
                    val err = res.body?.string() ?: "HTTP ${res.code}"
                    return@withContext Result.failure(Exception("Failed to fetch API keys: $err"))
                }

                val responseBody = res.body?.string() ?: "[]"
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

    // ---- QR pairing -------------------------------------------------------------------------
    //
    // The computer generates its own key and only ever sends sha256(key). These two calls let the
    // phone look at a pending request and, once the user confirms, register that hash under this
    // account. Both run as `authenticated`: the server takes the user from auth.uid(), never from
    // a parameter we could get wrong.

    /** Calls an RPC with the signed-in user's token, refreshing the session once on 401/403. */
    private suspend fun callAuthedRpc(fn: String, body: JsonObject): Pair<Int, String> {
        fun build(): Request = Request.Builder()
            .url("$baseUrl/rest/v1/rpc/$fn")
            .post(body.toString().toRequestBody(jsonMediaType))
            .also { getAuthHeaders(it) }
            .build()

        var response = client.newCall(build()).execute()
        if (!response.isSuccessful && (response.code == 401 || response.code == 403)) {
            response.close()
            if (SupabaseAuthManager(context).refreshSession().isSuccess) {
                response = client.newCall(build()).execute()
            } else {
                return 401 to ""
            }
        }
        return response.use { it.code to (it.body?.string() ?: "") }
    }

    /**
     * Looks up a scanned code without changing anything, so the user can see which computer is
     * asking before approving. A wrong or expired code counts towards the server's lockout.
     */
    suspend fun pairPreview(code: String): Result<PairingPreview> = withContext(Dispatchers.IO) {
        try {
            val normalized = PairingRules.normalizeCode(code)
            val (status, body) = callAuthedRpc("mcp_pair_preview", JsonObject().apply {
                addProperty("p_code", normalized)
            })
            if (status !in 200..299) return@withContext Result.failure(PairingRules.errorForHttp(status, body))

            val json = JsonParser.parseString(body).asJsonObject
            if (!json.get("ok").asBoolean) {
                val retry = json.get("retryAfterSeconds")?.takeIf { !it.isJsonNull }?.asInt
                return@withContext Result.failure(
                    PairingRules.errorFor(json.get("error")?.takeIf { !it.isJsonNull }?.asString, retry)
                )
            }
            Result.success(
                PairingPreview(
                    label = json.get("label").asString,
                    platform = json.get("platform")?.takeIf { !it.isJsonNull }?.asString,
                    clientVersion = json.get("clientVersion")?.takeIf { !it.isJsonNull }?.asString,
                    keyPrefix = json.get("keyPrefix")?.takeIf { !it.isJsonNull }?.asString,
                    expiresAt = json.get("expiresAt")?.takeIf { !it.isJsonNull }?.asString
                )
            )
        } catch (e: PairingException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(PairingException(PairingError.OFFLINE, "No connection. Check your network and try again."))
        }
    }

    /**
     * Approves a pairing: the server creates the API key for this account from the hash the
     * computer registered. The 3-key quota and unique-name rules still apply and surface here.
     */
    suspend fun pairApprove(code: String, name: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val normalized = PairingRules.normalizeCode(code)
            val (status, body) = callAuthedRpc("mcp_pair_approve", JsonObject().apply {
                addProperty("p_code", normalized)
                addProperty("p_name", ApiKeyRules.normalizeName(name))
            })
            if (status !in 200..299) return@withContext Result.failure(PairingRules.errorForHttp(status, body))

            val json = JsonParser.parseString(body).asJsonObject
            if (!json.get("ok").asBoolean) {
                val retry = json.get("retryAfterSeconds")?.takeIf { !it.isJsonNull }?.asInt
                return@withContext Result.failure(
                    PairingRules.errorFor(json.get("error")?.takeIf { !it.isJsonNull }?.asString, retry)
                )
            }
            Result.success(json.get("keyName")?.takeIf { !it.isJsonNull }?.asString ?: name)
        } catch (e: PairingException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(PairingException(PairingError.OFFLINE, "No connection. Check your network and try again."))
        }
    }
}
