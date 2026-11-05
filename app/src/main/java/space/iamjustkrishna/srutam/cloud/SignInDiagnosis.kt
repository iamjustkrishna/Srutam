package space.iamjustkrishna.srutam.cloud

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.io.IOException

/** Srutam Cloud (Supabase) answered the Google ID token with an error. [body] is its reply. */
class SupabaseSignInException(val httpCode: Int, val body: String) : Exception("Google Sign-In failed: $body")

/**
 * Turns a failed Google sign-in into a short code (for the event log) and a plain sentence (for the
 * person). Android reports several different setup problems as a bare "cancelled", so a cancel that
 * comes straight back is treated as a setup problem, not as someone backing out.
 */
object SignInDiagnosis {
    /** A cancel faster than this was not a person deciding to back out. */
    const val FAST_CANCEL_MS = 1_500L

    /**
     * [code] goes to analytics; [message] is shown under the button (empty when [show] is false,
     * as when someone simply closed the sheet).
     */
    data class Result(val code: String, val message: String, val show: Boolean)

    fun of(error: Throwable?, elapsedMs: Long): Result = when {
        error == null -> Result("unknown", "Sign-in failed. Try again.", true)

        error is IllegalStateException && error.message?.contains("GOOGLE_WEB_CLIENT_ID") == true ->
            Result("not_configured", "This build of Srutam has no Google client ID set.", true)

        error is GetCredentialCancellationException ->
            if (elapsedMs < FAST_CANCEL_MS) {
                Result(
                    "cancelled_fast",
                    "Google sign-in closed right away. This usually means this app build isn't registered with Google (its SHA-1 is missing), or no Google account is on the phone.",
                    true
                )
            } else {
                Result("cancelled", "", false)
            }

        error is NoCredentialException ->
            Result("no_credential", "No Google account found on this phone. Add one in Settings > Accounts, then try again.", true)

        error is GetCredentialProviderConfigurationException ->
            Result("provider_config", "Google Play services isn't ready on this phone. Update Google Play services and try again.", true)

        error is GetCredentialInterruptedException ->
            Result("interrupted", "Sign-in was interrupted. Try again.", true)

        error is GetCredentialUnsupportedException ->
            Result("unsupported", "This phone can't use Google sign-in here.", true)

        error is GetCredentialException ->
            Result("credential_error", "Google sign-in couldn't finish.", true)

        error is GoogleIdTokenParsingException ->
            Result("token_parse", "Google's reply couldn't be read.", true)

        error is SupabaseSignInException -> supabase(error)

        error is IOException ->
            Result("network", "No internet connection. Check it and try again.", true)

        else -> Result("unknown", "Sign-in failed. Try again.", true)
    }

    private fun supabase(e: SupabaseSignInException): Result {
        val body = e.body.lowercase()
        return when {
            "audience" in body ->
                Result("supabase_audience", "Srutam Cloud doesn't accept this Google client ID. The Google provider in Supabase must use the same Web client ID as the app.", true)
            "nonce" in body ->
                Result("supabase_nonce", "Srutam Cloud rejected the Google sign-in (nonce check).", true)
            "provider" in body && ("not enabled" in body || "unsupported" in body || "disabled" in body) ->
                Result("supabase_provider_off", "Google sign-in is switched off in Srutam Cloud.", true)
            else ->
                Result("supabase_${e.httpCode}", "Srutam Cloud rejected the Google sign-in (error ${e.httpCode}).", true)
        }
    }
}
