package space.iamjustkrishna.srutam.viewmodel

import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.utils.InsightNames
import java.util.Locale

object ThemeClusterEngine {
    private val generatedStatus = Regex(
        """(?i)\b(?:no audible speech(?: was)? detected|audio successfully transcribed|recording captured \d+ words|ready for full analysis|audio captured offline)\b"""
    )
    private val stop = setOf(
        "the", "and", "this", "that", "with", "from", "have", "were", "they", "will",
        "what", "when", "where", "which", "there", "their", "about", "would", "could",
        "should", "into", "more", "some", "other", "than", "then", "just", "also",
        "your", "been", "each", "like", "very", "make", "made", "doing", "does",
        "done", "going", "think", "need", "want", "voice", "note", "recording",
        "audio", "today", "yesterday", "tomorrow", "really", "maybe", "something",
        "anything", "nothing", "everything", "talk", "talking", "future", "life", "work",
        "for", "are", "was", "not", "but", "you", "our", "can", "has", "had"
    )

    fun build(recordings: List<Recording>, dismissed: Set<String>): List<ThemeCluster> {
        val usable = recordings.filter { !it.transcript.isNullOrBlank() || !it.summary.isNullOrBlank() }
        if (usable.size < 3) return emptyList()
        val byId = usable.associateBy { it.id }
        val phrases = mutableMapOf<String, MutableSet<Long>>()
        for (note in usable) {
            // Preserve original adjacency. Never form phrases across sentences or deleted stop words.
            val content = "${if (InsightNames.isMeaningful(note.name)) note.name else ""}.\n${note.summary.orEmpty()}.\n${note.transcript.orEmpty()}"
            for (sentence in content.split(Regex("""[.!?\n;]+"""))) {
                if (generatedStatus.containsMatchIn(sentence)) continue
                val words = Regex("""[\p{L}\p{M}][\p{L}\p{M}\p{N}]*""")
                    .findAll(sentence.lowercase(Locale.ROOT)).map { it.value }.toList()
                for (size in 2..4) {
                    for (part in words.windowed(size)) {
                        if (part.any { it in stop || it.length < 3 || it.length > 32 }) continue
                        val phrase = part.joinToString(" ")
                        if (phrase !in dismissed) phrases.getOrPut(phrase) { linkedSetOf() }.add(note.id)
                    }
                }
            }
        }
        val ranked = phrases.entries.filter { it.value.size >= 3 }.sortedWith(
            compareByDescending<Map.Entry<String, MutableSet<Long>>> { it.value.size }
                .thenByDescending { it.key.count { char -> char == ' ' } }
                .thenByDescending { entry -> entry.value.maxOf { byId.getValue(it).timestamp } }
                .thenBy { it.key }
        )
        val selected = mutableListOf<ThemeCluster>()
        for ((key, ids) in ranked) {
            if (selected.any {
                    val existing = it.noteIds.toSet()
                    existing.intersect(ids).size.toDouble() / existing.union(ids).size >= 0.8
                }) continue
            val sortedIds = ids.sortedByDescending { byId.getValue(it).timestamp }
            selected += ThemeCluster(
                key, key.replaceFirstChar { it.titlecase(Locale.getDefault()) }, ids.size,
                sortedIds, sortedIds.map { InsightNames.source(it, byId).label },
                sortedIds.take(2).map { byId.getValue(it).summary.orEmpty().take(160) }.filter { it.isNotBlank() }
            )
            if (selected.size == 5) break
        }
        return selected
    }
}
