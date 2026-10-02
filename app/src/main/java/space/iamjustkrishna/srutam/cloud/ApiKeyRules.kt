package space.iamjustkrishna.srutam.cloud

/** Client-side mirror of the server rules (trigger + unique index in migration 05). The server is the authority. */
object ApiKeyRules {
    const val MAX_ACTIVE_KEYS = 3
    const val MAX_NAME_LENGTH = 60

    fun normalizeName(raw: String): String = raw.trim().replace(Regex("\\s+"), " ")

    private fun sameName(a: String, b: String): Boolean =
        normalizeName(a).equals(normalizeName(b), ignoreCase = true)

    /** Returns a user-facing error, or null when [rawName] is acceptable given the existing active key names. */
    fun validateName(rawName: String, existingActiveNames: List<String>): String? {
        val name = normalizeName(rawName)
        return when {
            name.isEmpty() -> "Give this key a name."
            name.length > MAX_NAME_LENGTH -> "Name must be $MAX_NAME_LENGTH characters or fewer."
            existingActiveNames.any { sameName(it, name) } -> "You already have a key called \"$name\". Pick a different name."
            else -> null
        }
    }

    fun limitReached(activeKeyCount: Int): Boolean = activeKeyCount >= MAX_ACTIVE_KEYS
}

/** Thrown when the account already has [ApiKeyRules.MAX_ACTIVE_KEYS] active keys. */
class ApiKeyLimitException : Exception("Maximum ${ApiKeyRules.MAX_ACTIVE_KEYS} keys allowed. Revoke one to create a new key.")

/** Thrown when an active key with the same name already exists, or the name is invalid. */
class ApiKeyNameException(message: String) : Exception(message)
