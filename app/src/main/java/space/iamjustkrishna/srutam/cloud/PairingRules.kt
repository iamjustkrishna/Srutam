package space.iamjustkrishna.srutam.cloud

/**
 * Pure helpers for QR pairing: parsing what the camera read, and turning server replies into
 * messages a person can act on. Kept free of Android and network types so it is unit-testable.
 *
 * The server (migration 06) is always the authority; nothing here grants access.
 */

/** What a computer told us about itself, shown on the confirmation sheet before approving. */
data class PairingPreview(
    val label: String,
    val platform: String?,
    val clientVersion: String?,
    val keyPrefix: String?,
    val expiresAt: String?
)

/** Why a pairing attempt could not continue. Each maps to a specific, non-generic message. */
enum class PairingError {
    /** Expired, already used, cancelled, or simply wrong. The server does not distinguish, on purpose. */
    INVALID_CODE,
    RATE_LIMITED,
    NOT_SIGNED_IN,
    KEY_LIMIT_REACHED,
    DUPLICATE_NAME,
    UNSUPPORTED_VERSION,
    NOT_A_SRUTAM_CODE,
    OFFLINE,
    UNKNOWN
}

class PairingException(val error: PairingError, message: String, val retryAfterSeconds: Int? = null) :
    Exception(message)

object PairingRules {

    /** The only pairing protocol this app understands. A newer code means the app is out of date. */
    const val SUPPORTED_VERSION = 1

    private val CODE_CHARS = Regex("^[0-9A-HJKMNP-TV-Z]{8}$")

    /**
     * Accepts what the scanner or the manual field produced and returns the bare code.
     *
     * Handles the full URI (`srutam://pair?v=1&c=K7QF2M9D`) and a hand-typed code, which people
     * naturally write lowercase and with the dash shown in the terminal ("k7qf-2m9d").
     *
     * @throws PairingException if this is not a Srutam code, or is from a newer protocol.
     */
    fun parseScanned(raw: String?): String {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) throw PairingException(PairingError.NOT_A_SRUTAM_CODE, "Nothing was scanned.")

        if (text.contains("://")) {
            if (!text.startsWith("srutam://pair", ignoreCase = true)) {
                throw PairingException(PairingError.NOT_A_SRUTAM_CODE, "That isn't a Srutam pairing code.")
            }
            val query = text.substringAfter('?', "")
            val params = query.split('&').mapNotNull {
                val key = it.substringBefore('=', "")
                val value = it.substringAfter('=', "")
                if (key.isEmpty()) null else key.lowercase() to value
            }.toMap()

            val version = params["v"]?.toIntOrNull()
            if (version != null && version > SUPPORTED_VERSION) {
                throw PairingException(
                    PairingError.UNSUPPORTED_VERSION,
                    "Update Srutam to connect this computer."
                )
            }
            return normalizeCode(params["c"].orEmpty())
        }

        return normalizeCode(text)
    }

    /** Uppercases and drops separators, so "k7qf-2m9d" and "K7QF 2M9D" both work. */
    fun normalizeCode(raw: String): String {
        val code = raw.filter { it.isLetterOrDigit() }.uppercase()
        if (!CODE_CHARS.matches(code)) {
            throw PairingException(PairingError.NOT_A_SRUTAM_CODE, "That isn't a Srutam pairing code.")
        }
        return code
    }

    /** Groups the code the way the terminal prints it, for the manual-entry field. */
    fun formatCode(code: String): String =
        if (code.length == 8) "${code.take(4)}-${code.drop(4)}" else code

    /**
     * Maps a server reply or a thrown error onto a specific message.
     *
     * The server returns `{ok:false,error:...}` for a bad code rather than raising, because an
     * exception would roll back the failed-attempt row the brute-force counter depends on.
     */
    fun errorFor(serverError: String?, retryAfterSeconds: Int? = null): PairingException = when (serverError) {
        "invalid" -> PairingException(
            PairingError.INVALID_CODE,
            "That code has expired or was already used. Run the command on your computer again."
        )
        "rate_limited" -> PairingException(
            PairingError.RATE_LIMITED,
            retryAfterSeconds?.let { "Too many attempts. Try again in ${humanizeSeconds(it)}." }
                ?: "Too many attempts. Try again in a few minutes.",
            retryAfterSeconds
        )
        else -> PairingException(PairingError.UNKNOWN, "Could not connect that computer. Please try again.")
    }

    /** Translates a raw HTTP failure into something actionable. */
    fun errorForHttp(code: Int, body: String?): PairingException = when {
        code == 401 || code == 403 -> PairingException(PairingError.NOT_SIGNED_IN, "Please sign in again.")
        body?.contains("KEY_LIMIT_REACHED") == true -> PairingException(
            PairingError.KEY_LIMIT_REACHED,
            "You already have ${ApiKeyRules.MAX_ACTIVE_KEYS} active keys. Revoke one, then connect again."
        )
        body?.contains("uq_api_keys_active_name") == true || body?.contains("23505") == true ->
            PairingException(PairingError.DUPLICATE_NAME, "You already have a key with that name. Pick another.")
        else -> PairingException(PairingError.UNKNOWN, "Could not connect that computer. Please try again.")
    }

    fun humanizeSeconds(seconds: Int): String {
        if (seconds < 60) return "$seconds seconds"
        val minutes = (seconds + 59) / 60
        return if (minutes == 1) "a minute" else "$minutes minutes"
    }

    /**
     * Seconds remaining before a code expires, from the server timestamp. Negative or unparseable
     * values mean "treat as expired": the phone clock is never trusted to extend a code.
     */
    fun secondsUntil(expiresAtIso: String?, nowMillis: Long): Long {
        val expiry = parseIsoMillis(expiresAtIso) ?: return -1
        return (expiry - nowMillis) / 1000
    }

    private fun parseIsoMillis(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        return try {
            // Supabase returns ISO-8601; normalise the "+00:00" form Instant rejects on older APIs.
            val cleaned = iso.trim().replace(" ", "T").let { if (it.endsWith("Z")) it else "${it}Z" }
            java.time.Instant.parse(cleaned.replace(Regex("""\+00:00Z?$"""), "Z")).toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}
