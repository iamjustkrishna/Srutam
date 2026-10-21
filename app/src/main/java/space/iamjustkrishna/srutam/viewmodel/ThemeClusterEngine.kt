package space.iamjustkrishna.srutam.viewmodel

import com.google.gson.JsonParser
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.utils.InsightNames
import java.util.Locale

/**
 * Recurring themes across notes.
 *
 * Themes come from the 2-4 topics the AI tags each note with ("Apartment hunt", "Team hiring").
 * Notes without topics yet fall back to counting phrases, but only in their summaries and key
 * points (never raw transcripts, where filler like "right now" or "kind of" dominates).
 */
object ThemeClusterEngine {
    private const val MAX_THEMES = 5
    /** A topic on two notes already recurs; counted phrases are noisier and need three. */
    private const val MIN_NOTES_PER_TOPIC = 2
    private const val MIN_NOTES_PER_PHRASE = 3

    private val generatedStatus = Regex(
        """(?i)\b(?:no audible speech(?: was)? detected|audio successfully transcribed|recording captured \d+ words|ready for full analysis|audio captured offline|try speaking closer(?: to)?(?: the)?(?: microphone)?|low audio volume|transcription failed|speaking closer)\b"""
    )
    private val stop = setOf(
        "the", "and", "this", "that", "with", "from", "have", "were", "they", "will",
        "what", "when", "where", "which", "there", "their", "about", "would", "could",
        "should", "into", "more", "some", "other", "than", "then", "just", "also",
        "your", "been", "each", "like", "very", "make", "made", "doing", "does",
        "done", "going", "think", "need", "want", "voice", "note", "recording",
        "audio", "today", "yesterday", "tomorrow", "really", "maybe", "something",
        "anything", "nothing", "everything", "talk", "talking", "future", "life", "work",
        "for", "are", "was", "not", "but", "you", "our", "can", "has", "had",
        // Filler that reads as a "theme" when it is just how people talk.
        "right", "now", "kind", "sort", "lot", "lots", "gonna", "wanna", "okay", "yeah",
        "actually", "basically", "literally", "stuff", "thing", "things", "week", "weeks",
        "time", "times", "day", "days", "morning", "evening", "tonight", "moment", "bit",
        "speaker", "discusses", "discussed", "mentions", "mentioned", "notes", "user"
    )

    /** Topic names the AI sometimes returns that are too generic to be a theme. */
    private val genericTopics = setOf(
        "work", "life", "thoughts", "general", "misc", "miscellaneous", "notes", "personal",
        "ideas", "tasks", "today", "update", "updates", "meeting", "discussion", "other"
    )

    fun build(recordings: List<Recording>, dismissed: Set<String>): List<ThemeCluster> {
        val byId = recordings.associateBy { it.id }
        val selected = mutableListOf<ThemeCluster>()

        // 1. AI topics.
        val topicNotes = mutableMapOf<String, MutableSet<Long>>()
        val topicTitles = mutableMapOf<String, MutableMap<String, Int>>()
        val tagged = mutableSetOf<Long>()
        for (note in recordings) {
            // Once the AI has looked at a note (even if it found no topic), its topics are the answer.
            if (note.topics != null) tagged += note.id
            val topics = parseTopics(note.topics)
            for (topic in topics) {
                val key = topicKey(topic)
                if (key.isEmpty() || key in genericTopics || key in dismissed) continue
                topicNotes.getOrPut(key) { linkedSetOf() }.add(note.id)
                topicTitles.getOrPut(key) { mutableMapOf() }.merge(topic.trim(), 1, Int::plus)
            }
        }
        topicNotes.entries
            .filter { it.value.size >= MIN_NOTES_PER_TOPIC }
            .sortedWith(byNoteCountThenRecency(byId))
            .forEach { (key, ids) ->
                if (selected.size >= MAX_THEMES || overlapsChosen(selected, ids)) return@forEach
                val title = topicTitles.getValue(key).maxByOrNull { it.value }!!.key
                selected += cluster(key, title, ids, byId)
            }

        // 2. Fallback for notes the AI hasn't tagged yet.
        if (selected.size < MAX_THEMES) {
            val untagged = recordings.filter { it.id !in tagged }
            for ((key, ids) in rankedPhrases(untagged, dismissed, byId)) {
                if (selected.size >= MAX_THEMES) break
                if (selected.any { it.key == key } || overlapsChosen(selected, ids)) continue
                selected += cluster(key, key.replaceFirstChar { it.titlecase(Locale.getDefault()) }, ids, byId)
            }
        }
        return selected
    }

    /** Topics as stored on a note: a JSON array of strings. */
    fun parseTopics(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            JsonParser.parseString(json).asJsonArray
                .mapNotNull { element -> element.takeIf { it.isJsonPrimitive }?.asString?.trim() }
                .filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Cleans the AI's topics: 2-4 distinct, short, non-generic names. */
    fun cleanTopics(raw: List<String>): List<String> = raw
        .map { it.trim().trim('#', '"', '.', ',').replace(Regex("\\s+"), " ") }
        .filter { topic ->
            val key = topicKey(topic)
            key.isNotEmpty() && key !in genericTopics && topic.length <= 40 &&
                key.split(' ').size <= 4 && key.split(' ').any { it !in stop }
        }
        .distinctBy { topicKey(it) }
        .take(4)

    internal fun topicKey(topic: String): String =
        topic.lowercase(Locale.ROOT).replace(Regex("""[^\p{L}\p{M}\p{N}]+"""), " ").trim()

    private fun rankedPhrases(
        notes: List<Recording>,
        dismissed: Set<String>,
        byId: Map<Long, Recording>
    ): List<Pair<String, Set<Long>>> {
        val usable = notes.filter { !it.summary.isNullOrBlank() || !it.keyPoints.isNullOrBlank() }
        if (usable.size < MIN_NOTES_PER_PHRASE) return emptyList()
        val phrases = mutableMapOf<String, MutableSet<Long>>()
        for (note in usable) {
            // Preserve original adjacency. Never form phrases across sentences or deleted stop words.
            val keyPoints = parseTopics(note.keyPoints).joinToString(".\n")
            val content = "${if (InsightNames.isMeaningful(note.name)) note.name else ""}.\n${note.summary.orEmpty()}.\n$keyPoints"
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
        return phrases.entries.filter { it.value.size >= MIN_NOTES_PER_PHRASE }
            .sortedWith(
                compareByDescending<Map.Entry<String, MutableSet<Long>>> { it.value.size }
                    .thenByDescending { it.key.count { char -> char == ' ' } }
                    .thenByDescending { entry -> entry.value.maxOf { byId.getValue(it).timestamp } }
                    .thenBy { it.key }
            )
            .map { it.key to it.value }
    }

    private fun byNoteCountThenRecency(byId: Map<Long, Recording>) =
        compareByDescending<Map.Entry<String, MutableSet<Long>>> { it.value.size }
            .thenByDescending { entry -> entry.value.maxOf { byId.getValue(it).timestamp } }
            .thenBy { it.key }

    private fun overlapsChosen(selected: List<ThemeCluster>, ids: Set<Long>) = selected.any {
        val existing = it.noteIds.toSet()
        existing.intersect(ids).size.toDouble() / existing.union(ids).size >= 0.8
    }

    private fun cluster(key: String, title: String, ids: Set<Long>, byId: Map<Long, Recording>): ThemeCluster {
        val sortedIds = ids.sortedByDescending { byId.getValue(it).timestamp }
        return ThemeCluster(
            key, title, ids.size,
            sortedIds, sortedIds.map { InsightNames.source(it, byId).label },
            sortedIds.take(2).map { byId.getValue(it).summary.orEmpty().take(160) }.filter { it.isNotBlank() }
        )
    }
}
