package space.iamjustkrishna.srutam.ai.provider

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.iamjustkrishna.srutam.BuildConfig
import space.iamjustkrishna.srutam.utils.AppPreferences

class SrutamCloudRouter(
    private val context: Context,
    private val geminiClient1: LlmClient = GeminiLlmClient(
        context,
        overrideApiKey = BuildConfig.GEMINI_API_KEY.ifBlank { null }
    ),
    private val geminiClient2: LlmClient = GeminiLlmClient(
        context,
        overrideApiKey = BuildConfig.GEMINI_API_KEY2.ifBlank { null }
    ),
    private val groqClient: LlmClient = GroqLlmClient(
        context,
        overrideApiKey = BuildConfig.GROQ_API_KEY.ifBlank { null }
    )
) : LlmClient {

    override val providerId: String = AppPreferences.PROVIDER_SRUTAM_DEFAULT

    override suspend fun generateText(prompt: String, timeoutMs: Long): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        val tiers = listOf(
            Tier(name = "Gemini-1 (Primary)", client = geminiClient1, isCooling = { now < gemini1CoolingUntil }, setCooling = { gemini1CoolingUntil = it }),
            Tier(name = "Gemini-2 (Secondary)", client = geminiClient2, isCooling = { now < gemini2CoolingUntil }, setCooling = { gemini2CoolingUntil = it }),
            Tier(name = "Groq (Backup)", client = groqClient, isCooling = { now < groqCoolingUntil }, setCooling = { groqCoolingUntil = it })
        )

        // First pass: try tiers that are NOT currently in cooldown
        val activeTiers = tiers.filter { !it.isCooling() }
        val tiersToAttempt = if (activeTiers.isNotEmpty()) activeTiers else tiers

        val failures = mutableListOf<String>()

        for (tier in tiersToAttempt) {
            try {
                Log.d(TAG, "Attempting cloud query via ${tier.name}...")
                val response = tier.client.generateText(prompt, timeoutMs)
                if (response.isNotBlank() && response != HIGH_TRAFFIC_MESSAGE) {
                    Log.i(TAG, "Query succeeded via ${tier.name}")
                    return@withContext response
                }
            } catch (e: RateLimitException) {
                tier.setCooling(System.currentTimeMillis() + COOLDOWN_MS)
                Log.w(TAG, "${tier.name} rate limit reached: ${e.message}. Failing over to next provider...")
                failures.add("${tier.name}: 429/Quota exceeded")
            } catch (e: Exception) {
                tier.setCooling(System.currentTimeMillis() + COOLDOWN_MS)
                Log.w(TAG, "${tier.name} failed: ${e.message}. Failing over to next provider...")
                failures.add("${tier.name}: ${e.message}")
            }
        }

        // If activeTiers was a subset and all failed, try any remaining cooling tiers as a last resort
        val remainingTiers = tiers.filter { it !in tiersToAttempt }
        for (tier in remainingTiers) {
            try {
                Log.d(TAG, "Attempting last-resort query via cooling ${tier.name}...")
                val response = tier.client.generateText(prompt, timeoutMs)
                if (response.isNotBlank() && response != HIGH_TRAFFIC_MESSAGE) {
                    Log.i(TAG, "Last-resort query succeeded via ${tier.name}")
                    return@withContext response
                }
            } catch (e: Exception) {
                Log.w(TAG, "Last-resort ${tier.name} failed: ${e.message}")
                failures.add("${tier.name}: ${e.message}")
            }
        }

        Log.e(TAG, "All Srutam Cloud providers failed. Errors: ${failures.joinToString("; ")}")
        HIGH_TRAFFIC_MESSAGE
    }

    private data class Tier(
        val name: String,
        val client: LlmClient,
        val isCooling: () -> Boolean,
        val setCooling: (Long) -> Unit
    )

    companion object {
        private const val TAG = "SrutamCloudRouter"
        const val COOLDOWN_MS = 60_000L // 60 seconds cooldown for exhausted keys
        const val HIGH_TRAFFIC_MESSAGE =
            "Srutam AI is currently experiencing high traffic across all servers. Please wait a moment and try again."

        @Volatile var gemini1CoolingUntil: Long = 0L
        @Volatile var gemini2CoolingUntil: Long = 0L
        @Volatile var groqCoolingUntil: Long = 0L

        fun resetCooldowns() {
            gemini1CoolingUntil = 0L
            gemini2CoolingUntil = 0L
            groqCoolingUntil = 0L
        }
    }
}
