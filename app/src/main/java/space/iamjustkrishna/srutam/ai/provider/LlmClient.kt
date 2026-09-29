package space.iamjustkrishna.srutam.ai.provider

interface LlmClient {
    val providerId: String
    suspend fun generateText(prompt: String, timeoutMs: Long = 60_000L): String
}
