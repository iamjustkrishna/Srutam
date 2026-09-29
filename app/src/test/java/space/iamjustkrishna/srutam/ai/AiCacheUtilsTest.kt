package space.iamjustkrishna.srutam.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AiCacheUtilsTest {

    @Test
    fun normalizeQuery_removesPunctuationAndTrimsAndLowercases() {
        val q1 = "  What was the budget for the party?  "
        val q2 = "what was the budget for the party"
        val q3 = "What was the budget for the party??!"

        val n1 = AiCacheUtils.normalizeQuery(q1)
        val n2 = AiCacheUtils.normalizeQuery(q2)
        val n3 = AiCacheUtils.normalizeQuery(q3)

        assertEquals("what was the budget for the party", n1)
        assertEquals(n1, n2)
        assertEquals(n2, n3)
    }

    @Test
    fun sha256_generatesDeterministicHash() {
        val hash1 = AiCacheUtils.sha256("test_query_string")
        val hash2 = AiCacheUtils.sha256("test_query_string")
        val hash3 = AiCacheUtils.sha256("different_query")

        assertEquals(64, hash1.length)
        assertEquals(hash1, hash2)
        assertNotEquals(hash1, hash3)
    }
}
