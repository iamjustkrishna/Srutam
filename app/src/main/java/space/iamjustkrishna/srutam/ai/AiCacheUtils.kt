package space.iamjustkrishna.srutam.ai

import java.security.MessageDigest

object AiCacheUtils {

    /**
     * Normalizes a user query: trims, lowercases, removes common punctuation, collapses whitespace.
     * Example: "  What was the budget for the party?  " -> "what was the budget for the party"
     */
    fun normalizeQuery(query: String): String {
        return query.trim()
            .lowercase()
            .replace(Regex("[?!.,;:\"']"), "")
            .replace(Regex("\\s+"), " ")
    }

    /**
     * Computes a SHA-256 hash string for caching keys.
     */
    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
