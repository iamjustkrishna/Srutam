package space.iamjustkrishna.srutam.ai

import kotlin.math.ln
import kotlin.math.max

/**
 * Fast in-memory Okapi BM25 ranking algorithm for voice notes.
 * Used by the global Srutam AI Copilot to quickly retrieve relevant notes across the entire library.
 */
class BM25SearchEngine(
    private val k1: Float = 1.2f,
    private val b: Float = 0.75f
) {
    data class IndexedDocument(
        val id: Long,
        val title: String,
        val text: String,
        val dateString: String,
        val tokenCounts: Map<String, Int>,
        val length: Int
    )

    data class SearchResult(
        val document: IndexedDocument,
        val score: Float
    )

    private val documents = mutableListOf<IndexedDocument>()
    private val docFrequency = mutableMapOf<String, Int>()
    private var avgDocLength = 0f

    /**
     * Index a collection of recordings or documents.
     */
    fun index(docs: List<IndexedDocument>) {
        documents.clear()
        docFrequency.clear()
        documents.addAll(docs)

        if (documents.isEmpty()) {
            avgDocLength = 0f
            return
        }

        var totalLen = 0L
        for (doc in documents) {
            totalLen += doc.length
            for (term in doc.tokenCounts.keys) {
                docFrequency[term] = (docFrequency[term] ?: 0) + 1
            }
        }
        avgDocLength = totalLen.toFloat() / documents.size.toFloat()
    }

    /**
     * Query the index and return ranked results sorted by BM25 score descending.
     */
    fun search(query: String, topK: Int = 5): List<SearchResult> {
        val queryTerms = tokenize(query)
        if (queryTerms.isEmpty() || documents.isEmpty()) {
            return documents.take(topK).map { SearchResult(it, 0f) }
        }

        val totalDocs = documents.size.toFloat()
        val results = mutableListOf<SearchResult>()

        for (doc in documents) {
            var score = 0f
            for (term in queryTerms) {
                val tf = doc.tokenCounts[term] ?: 0
                if (tf == 0) continue

                val df = (docFrequency[term] ?: 0).toFloat()
                // Standard BM25 IDF with smoothing
                val idf = ln((totalDocs - df + 0.5f) / (df + 0.5f) + 1f)
                val numerator = tf * (k1 + 1f)
                val lenRatio = if (avgDocLength > 0f) doc.length / avgDocLength else 1f
                val denominator = tf + k1 * (1f - b + b * lenRatio)

                score += idf * (numerator / max(0.001f, denominator))
            }

            // Small boost for query terms matching title
            val titleTerms = tokenize(doc.title)
            val titleMatches = queryTerms.count { it in titleTerms }
            if (titleMatches > 0) {
                score += titleMatches * 1.5f
            }

            if (score > 0f) {
                results.add(SearchResult(doc, score))
            }
        }

        // If no strict keyword matches found, return most recent documents
        if (results.isEmpty()) {
            return documents.take(topK).map { SearchResult(it, 0f) }
        }

        return results.sortedByDescending { it.score }.take(topK)
    }

    companion object {
        private val STOP_WORDS = setOf(
            "a", "an", "the", "in", "on", "at", "to", "for", "of", "and", "or",
            "is", "are", "was", "were", "it", "this", "that", "i", "you", "he",
            "she", "we", "they", "my", "your", "what", "how", "when", "where", "why"
        )

        fun tokenize(text: String): List<String> {
            return text.lowercase()
                .replace(Regex("[^a-z0-9\\s]"), " ")
                .split(Regex("\\s+"))
                .filter { it.length > 1 && it !in STOP_WORDS }
        }

        fun createDocument(
            id: Long,
            title: String,
            transcript: String,
            summary: String,
            dateString: String
        ): IndexedDocument {
            val fullText = "$title\n$summary\n$transcript"
            val tokens = tokenize(fullText)
            val counts = mutableMapOf<String, Int>()
            for (token in tokens) {
                counts[token] = (counts[token] ?: 0) + 1
            }
            return IndexedDocument(
                id = id,
                title = title,
                text = fullText,
                dateString = dateString,
                tokenCounts = counts,
                length = tokens.size
            )
        }
    }
}
