package space.iamjustkrishna.srutam.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiKeyRulesTest {

    @Test
    fun acceptsFreshName() {
        assertNull(ApiKeyRules.validateName("Cursor IDE", emptyList()))
        assertNull(ApiKeyRules.validateName("Windsurf", listOf("Cursor IDE", "Zed")))
    }

    @Test
    fun rejectsBlankAndWhitespaceOnly() {
        assertNotNull(ApiKeyRules.validateName("", emptyList()))
        assertNotNull(ApiKeyRules.validateName("   \t ", emptyList()))
    }

    @Test
    fun rejectsOverlongName() {
        assertNull(ApiKeyRules.validateName("x".repeat(ApiKeyRules.MAX_NAME_LENGTH), emptyList()))
        assertNotNull(ApiKeyRules.validateName("x".repeat(ApiKeyRules.MAX_NAME_LENGTH + 1), emptyList()))
    }

    @Test
    fun duplicateCheckIgnoresCaseAndSurroundingAndInnerSpaces() {
        val existing = listOf("Cursor IDE")
        assertNotNull(ApiKeyRules.validateName("Cursor IDE", existing))
        assertNotNull(ApiKeyRules.validateName("cursor ide", existing))
        assertNotNull(ApiKeyRules.validateName("  CURSOR   IDE  ", existing))
        assertNull(ApiKeyRules.validateName("Cursor IDE 2", existing))
    }

    @Test
    fun duplicateMessageNamesTheKey() {
        val message = ApiKeyRules.validateName("  Zed ", listOf("zed"))
        assertTrue(message!!.contains("\"Zed\""))
    }

    @Test
    fun limitIsThree() {
        assertEquals(3, ApiKeyRules.MAX_ACTIVE_KEYS)
        assertFalse(ApiKeyRules.limitReached(0))
        assertFalse(ApiKeyRules.limitReached(2))
        assertTrue(ApiKeyRules.limitReached(3))
        assertTrue(ApiKeyRules.limitReached(4)) // accounts already over the limit stay blocked
    }

    @Test
    fun normalizeCollapsesWhitespace() {
        assertEquals("My Laptop", ApiKeyRules.normalizeName("  My \n  Laptop  "))
    }
}
