package space.iamjustkrishna.srutam.cloud

import android.content.Context
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import space.iamjustkrishna.srutam.BuildConfig
import space.iamjustkrishna.srutam.utils.AppPreferences
import java.util.concurrent.TimeUnit

class SupabaseAuthManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val baseUrl: String by lazy {
        BuildConfig.SUPABASE_URL.trimEnd('/')
    }

    private val anonKey: String by lazy {
        BuildConfig.SUPABASE_ANON_KEY
    }

    /**
     * Signs in using a Google ID Token obtained from Android Credential Manager.
     */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("provider", "google")
                addProperty("id_token", idToken)
            }

            val request = Request.Builder()
                .url("$baseUrl/auth/v1/token?grant_type=id_token")
                .addHeader("apikey", anonKey)
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val responseString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Google Sign-In failed: $responseString"))
                }

                val json = JsonParser.parseString(responseString).asJsonObject
                val accessToken = json.get("access_token").asString
                val refreshToken = if (json.has("refresh_token")) json.get("refresh_token").asString else null
                val userObj = json.get("user").asJsonObject
                val userId = userObj.get("id").asString
                val email = if (userObj.has("email") && !userObj.get("email").isJsonNull) userObj.get("email").asString else null

                AppPreferences.saveCloudSession(
                    context = context,
                    userId = userId,
                    email = email,
                    accessToken = accessToken,
                    refreshToken = refreshToken
                )

                Result.success(userId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sends an Email Magic Link for passwordless authentication.
     */
    suspend fun sendMagicLink(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("email", email)
            }

            val request = Request.Builder()
                .url("$baseUrl/auth/v1/magiclink")
                .addHeader("apikey", anonKey)
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: "HTTP ${response.code}"
                    return@withContext Result.failure(Exception("Magic link request failed: $err"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Quick Developer / Sandbox sign-in with email & password (or sign-up).
     * Makes it effortless to test in emulators or without Google Web Client ID setup.
     */
    suspend fun signInWithEmailPassword(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("email", email)
                addProperty("password", password)
            }

            val request = Request.Builder()
                .url("$baseUrl/auth/v1/token?grant_type=password")
                .addHeader("apikey", anonKey)
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val responseString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    if (responseString.contains("Email not confirmed", ignoreCase = true)) {
                        return@withContext Result.failure(
                            Exception("Email not confirmed. Please check your inbox or confirm your email in Supabase Authentication settings.")
                        )
                    }
                    // Try auto sign-up if user doesn't exist
                    return@withContext signUpWithEmailPassword(email, password)
                }

                val json = JsonParser.parseString(responseString).asJsonObject
                val accessToken = json.get("access_token").asString
                val refreshToken = if (json.has("refresh_token")) json.get("refresh_token").asString else null
                val userObj = json.get("user").asJsonObject
                val userId = userObj.get("id").asString

                AppPreferences.saveCloudSession(
                    context = context,
                    userId = userId,
                    email = email,
                    accessToken = accessToken,
                    refreshToken = refreshToken
                )

                Result.success(userId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun signUpWithEmailPassword(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("email", email)
                addProperty("password", password)
            }

            val request = Request.Builder()
                .url("$baseUrl/auth/v1/signup")
                .addHeader("apikey", anonKey)
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val responseString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Sign up failed: $responseString"))
                }

                val json = JsonParser.parseString(responseString).asJsonObject
                if (!json.has("access_token")) {
                    return@withContext Result.failure(
                        Exception("Email confirmation is required. Please check your inbox or disable 'Confirm email' in Supabase Authentication settings.")
                    )
                }

                val userObj = json.get("user")?.asJsonObject ?: json
                val userId = userObj.get("id").asString
                val accessToken = json.get("access_token").asString
                val refreshToken = if (json.has("refresh_token")) json.get("refresh_token").asString else null

                AppPreferences.saveCloudSession(
                    context = context,
                    userId = userId,
                    email = email,
                    accessToken = accessToken,
                    refreshToken = refreshToken
                )

                Result.success(userId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signs out and clears local session tokens.
     */
    fun signOut() {
        AppPreferences.clearCloudSession(context)
    }
}
