package space.iamjustkrishna.srutam.ai

/**
 * Cleans raw Whisper output: drops non-speech annotations it invents for noise
 * ("(cough)", "[BLANK_AUDIO]", "*music*"), collapses decoder repetition loops, and
 * tidies the spacing that removing those leaves behind.
 */
object TranscriptPostProcessor {
    private val NON_SPEECH_TAG = Regex("""\[[^\]]{0,40}]|\([^)]{0,40}\)|\*[^*]{1,40}\*|♪+""")
    private val WHITESPACE = Regex("""\s+""")
    private val SPACE_BEFORE_PUNCTUATION = Regex("""\s+([,.!?;:])""")

    private const val MAX_LOOP_PHRASE_WORDS = 8
    private const val MIN_LOOP_REPEATS = 5

    fun clean(raw: String): String {
        val withoutTags = NON_SPEECH_TAG.replace(raw, " ")
        val words = WHITESPACE.split(withoutTags.trim()).filter { it.isNotEmpty() }
        val joined = collapseRepeatedPhrases(words).joinToString(" ")
        return SPACE_BEFORE_PUNCTUATION.replace(joined, "$1").trim()
    }

    // A phrase of 1-8 words repeated 5+ times back to back is a decoder loop, not speech
    // (genuine stutters like "no, no, no" stay under the threshold).
    private fun collapseRepeatedPhrases(words: List<String>): List<String> {
        val keys = words.map { word -> word.lowercase().filter { it.isLetterOrDigit() } }
        val result = ArrayList<String>(words.size)
        var i = 0
        while (i < words.size) {
            val loop = findLoop(keys, i)
            if (loop != null) {
                result.addAll(words.subList(i, i + loop.phraseWords))
                i += loop.phraseWords * loop.repeats
            } else {
                result.add(words[i])
                i++
            }
        }
        return result
    }

    private class Loop(val phraseWords: Int, val repeats: Int)

    private fun findLoop(keys: List<String>, start: Int): Loop? {
        for (phraseWords in 1..MAX_LOOP_PHRASE_WORDS) {
            if (start + phraseWords * MIN_LOOP_REPEATS > keys.size) break
            var repeats = 1
            while (start + (repeats + 1) * phraseWords <= keys.size &&
                sameRange(keys, start, start + repeats * phraseWords, phraseWords)
            ) {
                repeats++
            }
            if (repeats >= MIN_LOOP_REPEATS) return Loop(phraseWords, repeats)
        }
        return null
    }

    private fun sameRange(keys: List<String>, a: Int, b: Int, length: Int): Boolean {
        for (offset in 0 until length) {
            if (keys[a + offset] != keys[b + offset]) return false
        }
        return true
    }
}
