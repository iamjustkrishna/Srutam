package space.iamjustkrishna.srutam.cloud

import org.junit.Assert.*
import org.junit.Test

class PairingRulesTest {

    // ---- what the camera / the manual field produced -------------------------------------------

    @Test fun acceptsTheUriTheCliEncodes() {
        assertEquals("K7QF2M9D", PairingRules.parseScanned("srutam://pair?v=1&c=K7QF2M9D"))
    }

    @Test fun acceptsAHandTypedCodeLowercaseAndWithTheDashTheTerminalShows() {
        assertEquals("K7QF2M9D", PairingRules.parseScanned("k7qf-2m9d"))
        assertEquals("K7QF2M9D", PairingRules.parseScanned("  K7QF 2M9D  "))
    }

    @Test fun rejectsAQrFromSomeOtherApp() {
        val error = assertThrows(PairingException::class.java) {
            PairingRules.parseScanned("https://example.com/not-srutam")
        }
        assertEquals(PairingError.NOT_A_SRUTAM_CODE, error.error)
    }

    @Test fun rejectsAnEmptyScan() {
        assertEquals(
            PairingError.NOT_A_SRUTAM_CODE,
            assertThrows(PairingException::class.java) { PairingRules.parseScanned("  ") }.error
        )
    }

    @Test fun tellsTheUserToUpdateWhenTheCodeIsFromANewerProtocol() {
        val error = assertThrows(PairingException::class.java) {
            PairingRules.parseScanned("srutam://pair?v=2&c=K7QF2M9D")
        }
        assertEquals(PairingError.UNSUPPORTED_VERSION, error.error)
        assertTrue(error.message!!.contains("Update Srutam"))
    }

    @Test fun rejectsCodesUsingTheAmbiguousLettersTheAlphabetExcludes() {
        // I, L, O and U are deliberately not in the server's alphabet.
        for (bad in listOf("IIIIIIII", "LLLLLLLL", "OOOOOOOO", "UUUUUUUU")) {
            assertEquals(
                PairingError.NOT_A_SRUTAM_CODE,
                assertThrows(PairingException::class.java) { PairingRules.normalizeCode(bad) }.error
            )
        }
    }

    @Test fun rejectsCodesOfTheWrongLength() {
        for (bad in listOf("K7QF2M9", "K7QF2M9DD", "")) {
            assertEquals(
                PairingError.NOT_A_SRUTAM_CODE,
                assertThrows(PairingException::class.java) { PairingRules.normalizeCode(bad) }.error
            )
        }
    }

    @Test fun formatsCodesTheWayTheTerminalPrintsThem() {
        assertEquals("K7QF-2M9D", PairingRules.formatCode("K7QF2M9D"))
    }

    // ---- turning server replies into messages people can act on --------------------------------

    @Test fun anInvalidCodeTellsTheUserWhatToDoNext() {
        val error = PairingRules.errorFor("invalid")
        assertEquals(PairingError.INVALID_CODE, error.error)
        assertTrue(error.message!!.contains("again"))
    }

    @Test fun rateLimitingReportsHowLongToWait() {
        val error = PairingRules.errorFor("rate_limited", 125)
        assertEquals(PairingError.RATE_LIMITED, error.error)
        assertEquals(125, error.retryAfterSeconds)
        assertTrue(error.message!!.contains("3 minutes"))
    }

    @Test fun rateLimitingStillReadsWellWithoutARetryHint() {
        assertTrue(PairingRules.errorFor("rate_limited").message!!.contains("few minutes"))
    }

    @Test fun theThreeKeyLimitSurfacesAsItsOwnError() {
        val error = PairingRules.errorForHttp(400, """{"message":"KEY_LIMIT_REACHED: Maximum 3 ..."}""")
        assertEquals(PairingError.KEY_LIMIT_REACHED, error.error)
        assertTrue(error.message!!.contains("Revoke one"))
    }

    @Test fun aDuplicateNameSurfacesAsItsOwnError() {
        val error = PairingRules.errorForHttp(409, """{"code":"23505","message":"uq_api_keys_active_name"}""")
        assertEquals(PairingError.DUPLICATE_NAME, error.error)
    }

    @Test fun anExpiredSessionAsksTheUserToSignInAgain() {
        assertEquals(PairingError.NOT_SIGNED_IN, PairingRules.errorForHttp(401, null).error)
        assertEquals(PairingError.NOT_SIGNED_IN, PairingRules.errorForHttp(403, null).error)
    }

    @Test fun humanizesWaitTimes() {
        assertEquals("30 seconds", PairingRules.humanizeSeconds(30))
        assertEquals("a minute", PairingRules.humanizeSeconds(60))
        assertEquals("5 minutes", PairingRules.humanizeSeconds(241))
    }

    // ---- expiry is the server's call, never the phone's ----------------------------------------

    @Test fun countsDownFromTheServerTimestamp() {
        val now = 1_700_000_000_000L
        val inThreeMinutes = java.time.Instant.ofEpochMilli(now + 180_000).toString()
        assertEquals(180, PairingRules.secondsUntil(inThreeMinutes, now))
    }

    @Test fun treatsAMissingOrUnparseableExpiryAsAlreadyExpired() {
        assertTrue(PairingRules.secondsUntil(null, 1_700_000_000_000L) < 0)
        assertTrue(PairingRules.secondsUntil("not a date", 1_700_000_000_000L) < 0)
    }

    @Test fun aPastExpiryIsNegativeSoAStaleCodeIsNeverShownAsLive() {
        val now = 1_700_000_000_000L
        val aMinuteAgo = java.time.Instant.ofEpochMilli(now - 60_000).toString()
        assertTrue(PairingRules.secondsUntil(aMinuteAgo, now) < 0)
    }
}
