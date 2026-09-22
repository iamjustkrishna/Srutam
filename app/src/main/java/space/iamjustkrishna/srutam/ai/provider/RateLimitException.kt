package space.iamjustkrishna.srutam.ai.provider

class RateLimitException(
    message: String,
    val provider: String,
    val retryAfterSeconds: Long? = null,
    cause: Throwable? = null
) : Exception(message, cause)
