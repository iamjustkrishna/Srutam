package space.iamjustkrishna.srutam.ai

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import space.iamjustkrishna.srutam.ai.provider.LlmClient
import space.iamjustkrishna.srutam.ai.provider.RateLimitException
import space.iamjustkrishna.srutam.ai.provider.SrutamCloudRouter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SrutamCloudRouterTest {

    @Before
    fun setUp() {
        SrutamCloudRouter.resetCooldowns()
    }

    private class FakeLlmClient(
        override val providerId: String,
        var shouldFailWithRateLimit: Boolean = false,
        var responseText: String = "Default answer"
    ) : LlmClient {
        var callCount = 0
        override suspend fun generateText(prompt: String, timeoutMs: Long): String {
            callCount++
            if (shouldFailWithRateLimit) {
                throw RateLimitException("Rate limit hit", provider = providerId)
            }
            return responseText
        }
    }

    @Test
    fun router_usesPrimaryGemini1WhenHealthy() = runBlocking {
        val fakeGemini1 = FakeLlmClient("GEMINI_1", shouldFailWithRateLimit = false, responseText = "Gemini 1 answer")
        val fakeGemini2 = FakeLlmClient("GEMINI_2", shouldFailWithRateLimit = false, responseText = "Gemini 2 answer")
        val fakeGroq = FakeLlmClient("GROQ", shouldFailWithRateLimit = false, responseText = "Groq answer")

        val router = SrutamCloudRouter(
            context = ApplicationProvider.getApplicationContext(),
            geminiClient1 = fakeGemini1,
            geminiClient2 = fakeGemini2,
            groqClient = fakeGroq
        )

        val result = router.generateText("Hello", 5000L)
        assertEquals("Gemini 1 answer", result)
        assertEquals(1, fakeGemini1.callCount)
        assertEquals(0, fakeGemini2.callCount)
        assertEquals(0, fakeGroq.callCount)
    }

    @Test
    fun router_failsOverToGemini2WhenGemini1Fails() = runBlocking {
        val fakeGemini1 = FakeLlmClient("GEMINI_1", shouldFailWithRateLimit = true)
        val fakeGemini2 = FakeLlmClient("GEMINI_2", shouldFailWithRateLimit = false, responseText = "Gemini 2 answer")
        val fakeGroq = FakeLlmClient("GROQ", shouldFailWithRateLimit = false, responseText = "Groq answer")

        val router = SrutamCloudRouter(
            context = ApplicationProvider.getApplicationContext(),
            geminiClient1 = fakeGemini1,
            geminiClient2 = fakeGemini2,
            groqClient = fakeGroq
        )

        val result = router.generateText("Hello", 5000L)
        assertEquals("Gemini 2 answer", result)
        assertEquals(1, fakeGemini1.callCount)
        assertEquals(1, fakeGemini2.callCount)
        assertEquals(0, fakeGroq.callCount)
    }

    @Test
    fun router_failsOverToGroqWhenBothGeminisFail() = runBlocking {
        val fakeGemini1 = FakeLlmClient("GEMINI_1", shouldFailWithRateLimit = true)
        val fakeGemini2 = FakeLlmClient("GEMINI_2", shouldFailWithRateLimit = true)
        val fakeGroq = FakeLlmClient("GROQ", shouldFailWithRateLimit = false, responseText = "Groq answer")

        val router = SrutamCloudRouter(
            context = ApplicationProvider.getApplicationContext(),
            geminiClient1 = fakeGemini1,
            geminiClient2 = fakeGemini2,
            groqClient = fakeGroq
        )

        val result = router.generateText("Hello", 5000L)
        assertEquals("Groq answer", result)
        assertEquals(1, fakeGemini1.callCount)
        assertEquals(1, fakeGemini2.callCount)
        assertEquals(1, fakeGroq.callCount)
    }

    @Test
    fun router_returnsHighTrafficMessageWhenAllTiersFail() = runBlocking {
        val fakeGemini1 = FakeLlmClient("GEMINI_1", shouldFailWithRateLimit = true)
        val fakeGemini2 = FakeLlmClient("GEMINI_2", shouldFailWithRateLimit = true)
        val fakeGroq = FakeLlmClient("GROQ", shouldFailWithRateLimit = true)

        val router = SrutamCloudRouter(
            context = ApplicationProvider.getApplicationContext(),
            geminiClient1 = fakeGemini1,
            geminiClient2 = fakeGemini2,
            groqClient = fakeGroq
        )

        val result = router.generateText("Hello", 5000L)
        assertEquals(SrutamCloudRouter.HIGH_TRAFFIC_MESSAGE, result)
        assertEquals(1, fakeGemini1.callCount)
        assertEquals(1, fakeGemini2.callCount)
        assertEquals(1, fakeGroq.callCount)
    }

    @Test
    fun router_skipsCoolingTierOnSubsequentQuery() = runBlocking {
        val fakeGemini1 = FakeLlmClient("GEMINI_1", shouldFailWithRateLimit = true)
        val fakeGemini2 = FakeLlmClient("GEMINI_2", shouldFailWithRateLimit = false, responseText = "Gemini 2 answer")
        val fakeGroq = FakeLlmClient("GROQ", shouldFailWithRateLimit = false, responseText = "Groq answer")

        val router = SrutamCloudRouter(
            context = ApplicationProvider.getApplicationContext(),
            geminiClient1 = fakeGemini1,
            geminiClient2 = fakeGemini2,
            groqClient = fakeGroq
        )

        // Query 1: Gemini 1 fails, Gemini 2 succeeds
        val res1 = router.generateText("Q1", 5000L)
        assertEquals("Gemini 2 answer", res1)
        assertEquals(1, fakeGemini1.callCount)
        assertEquals(1, fakeGemini2.callCount)

        // Query 2: Gemini 1 is still cooling down! Router skips directly to Gemini 2 without re-calling Gemini 1
        val res2 = router.generateText("Q2", 5000L)
        assertEquals("Gemini 2 answer", res2)
        assertEquals(1, fakeGemini1.callCount) // callCount did not increase!
        assertEquals(2, fakeGemini2.callCount)
    }
}
