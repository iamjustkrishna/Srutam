package space.iamjustkrishna.srutam.cloud

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class SignInDiagnosisTest {

    @Test fun aCancelThatComesBackAtOnceIsASetupProblem() {
        val why = SignInDiagnosis.of(GetCredentialCancellationException("activity is cancelled by the user."), elapsedMs = 400)
        assertEquals("cancelled_fast", why.code)
        assertTrue(why.show)
        assertTrue(why.message.contains("SHA-1"))
    }

    @Test fun aSlowCancelIsSomeoneClosingTheSheetAndStaysQuiet() {
        val why = SignInDiagnosis.of(GetCredentialCancellationException("cancelled"), elapsedMs = 6_000)
        assertEquals("cancelled", why.code)
        assertFalse(why.show)
    }

    @Test fun theBoundaryIsFifteenHundredMilliseconds() {
        assertEquals("cancelled_fast", SignInDiagnosis.of(GetCredentialCancellationException("x"), 1_499).code)
        assertEquals("cancelled", SignInDiagnosis.of(GetCredentialCancellationException("x"), 1_500).code)
    }

    @Test fun credentialProblemsGetTheirOwnCodes() {
        assertEquals("no_credential", SignInDiagnosis.of(NoCredentialException(), 300).code)
        assertEquals("provider_config", SignInDiagnosis.of(GetCredentialProviderConfigurationException(), 300).code)
        assertEquals("interrupted", SignInDiagnosis.of(GetCredentialInterruptedException(), 300).code)
    }

    @Test fun supabaseRepliesAreToldApart() {
        assertEquals(
            "supabase_audience",
            SignInDiagnosis.of(SupabaseSignInException(400, """{"msg":"Unacceptable audience in id_token: [123.apps.googleusercontent.com]"}"""), 900).code
        )
        assertEquals("supabase_nonce", SignInDiagnosis.of(SupabaseSignInException(400, """{"msg":"Nonces mismatch"}"""), 900).code)
        assertEquals(
            "supabase_provider_off",
            SignInDiagnosis.of(SupabaseSignInException(400, """{"msg":"Unsupported provider: provider is not enabled"}"""), 900).code
        )
        assertEquals("supabase_500", SignInDiagnosis.of(SupabaseSignInException(500, "oops"), 900).code)
    }

    @Test fun missingClientIdAndNoNetworkAreRecognised() {
        assertEquals(
            "not_configured",
            SignInDiagnosis.of(IllegalStateException("GOOGLE_WEB_CLIENT_ID is not configured in local.properties"), 5).code
        )
        assertEquals("network", SignInDiagnosis.of(UnknownHostException("x"), 100).code)
        assertEquals("unknown", SignInDiagnosis.of(RuntimeException("boom"), 100).code)
    }

    @Test fun codesNeverCarryTheServerReply() {
        // The reply can hold tokens or ids; only the short code and fixed sentences are logged or shown.
        val why = SignInDiagnosis.of(SupabaseSignInException(400, "id_token abc.def.ghi rejected"), 100)
        assertFalse(why.message.contains("abc.def.ghi"))
        assertFalse(why.code.contains("abc"))
    }
}
