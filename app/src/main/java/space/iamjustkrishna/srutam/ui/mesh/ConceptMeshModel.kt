package space.iamjustkrishna.srutam.ui.mesh

import androidx.compose.ui.graphics.Color
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.InsightKind
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.ui.theme.*
import space.iamjustkrishna.srutam.viewmodel.ThemeCluster
import kotlin.math.*

enum class MeshNodeKind(val displayName: String) {
    RECORDING("Voice Note"),
    ACTION("Action Item"),
    IDEA("Idea"),
    DECISION("Decision"),
    THEME("Recurring Theme")
}

data class MeshNode(
    val id: String,
    val kind: MeshNodeKind,
    val label: String,
    val fullText: String,
    val evidence: String? = null,
    val rationale: String? = null,
    val status: String? = null,
    val recordingId: Long? = null,
    val recordingName: String? = null,
    val timestamp: Long = 0L,
    val rawInsight: InsightEntity? = null,
    val keywords: Set<String> = emptySet(),
    var x: Float = 0f,
    var y: Float = 0f,
    val radius: Float = 26f,
    val color: Color = CobaltBlue
)

data class MeshEdge(
    val id: String,
    val fromId: String,
    val toId: String,
    val relation: String,
    val label: String,
    val isSimilar: Boolean = false
)

data class MeshGraphData(
    val nodes: List<MeshNode> = emptyList(),
    val edges: List<MeshEdge> = emptyList()
) {
    val nodeMap: Map<String, MeshNode> = nodes.associateBy { it.id }

    val adjacency: Map<String, Set<String>> = buildMap {
        edges.forEach { edge ->
            val setFrom = getOrPut(edge.fromId) { mutableSetOf() } as MutableSet
            setFrom.add(edge.toId)
            val setTo = getOrPut(edge.toId) { mutableSetOf() } as MutableSet
            setTo.add(edge.fromId)
        }
        // Also ensure self is present
        nodes.forEach { node ->
            val set = getOrPut(node.id) { mutableSetOf() } as MutableSet
            set.add(node.id)
        }
    }

    fun getConnectedNodeIds(selectedId: String): Set<String> {
        val direct = adjacency[selectedId] ?: setOf(selectedId)
        val selectedNode = nodeMap[selectedId] ?: return direct

        val result = direct.toMutableSet()
        result.add(selectedId)

        // If selected is an insight, also include sibling insights from the same parent recording
        if (selectedNode.recordingId != null) {
            val parentRecId = "rec_${selectedNode.recordingId}"
            result.add(parentRecId)
            nodes.forEach { other ->
                if (other.recordingId == selectedNode.recordingId) {
                    result.add(other.id)
                }
            }
        }

        // If selected is a recording, include all its child insights
        if (selectedNode.kind == MeshNodeKind.RECORDING && selectedNode.recordingId != null) {
            nodes.forEach { other ->
                if (other.recordingId == selectedNode.recordingId) {
                    result.add(other.id)
                }
            }
        }

        // Also illuminate parent recordings of directly connected cross-note nodes
        direct.forEach { otherId ->
            val otherNode = nodeMap[otherId]
            if (otherNode?.recordingId != null) {
                result.add("rec_${otherNode.recordingId}")
            }
        }

        return result
    }
}

object ConceptMeshBuilder {

    private val STOP_WORDS = setOf(
        "the", "and", "this", "that", "with", "from", "have", "will", "your",
        "what", "then", "into", "about", "more", "make", "when", "time", "just",
        "note", "need", "should", "some", "like", "also", "been", "were", "they",
        "them", "there", "their", "here", "could", "would", "shall", "each", "does",
        "done", "very", "much", "many", "such", "only", "other", "into", "over"
    )

    // Semantic concept clusters to group related thoughts across voice notes
    private val CONCEPT_STEMS = mapOf(
        "priva" to "privacy",
        "secur" to "security",
        "encry" to "security",
        "offli" to "offline",
        "local" to "offline",
        "trans" to "speech",
        "audio" to "speech",
        "voice" to "speech",
        "speak" to "speech",
        "sync" to "cloud",
        "cloud" to "cloud",
        "backu" to "cloud",
        "share" to "export",
        "expor" to "export",
        "desig" to "interface",
        "inter" to "interface",
        "layout" to "interface",
        "mesh" to "interface",
        "task" to "action",
        "actio" to "action",
        "decis" to "decision",
        "strat" to "strategy",
        "model" to "ai",
        "copil" to "ai",
        "intel" to "ai",
        "batte" to "performance",
        "memor" to "performance",
        "speed" to "performance"
    )

    fun extractKeywords(text: String): Set<String> {
        val words = text.lowercase()
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.length >= 3 && it !in STOP_WORDS }

        val set = mutableSetOf<String>()
        words.forEach { w ->
            if (w.length >= 4) set.add(w)
            // Match stems
            for ((stem, cluster) in CONCEPT_STEMS) {
                if (w.startsWith(stem)) {
                    set.add(cluster)
                    break
                }
            }
        }
        return set
    }

    private fun truncateLabel(text: String, maxWords: Int = 4, maxChars: Int = 22): String {
        val words = text.trim().split(Regex("\\s+"))
        val candidate = words.take(maxWords).joinToString(" ")
        return if (candidate.length > maxChars) {
            candidate.take(maxChars - 1).trimEnd() + "…"
        } else if (words.size > maxWords) {
            candidate + "…"
        } else {
            candidate
        }
    }

    fun buildGraph(
        recordings: List<Recording>,
        insights: List<InsightEntity>,
        themes: List<ThemeCluster>
    ): MeshGraphData {
        if (recordings.isEmpty() && insights.isEmpty()) {
            return MeshGraphData()
        }

        val nodes = mutableListOf<MeshNode>()
        val edges = mutableListOf<MeshEdge>()

        // 1. Arrange Recording Nodes on Primary Circle
        val validRecordings = recordings.filter { it.id > 0 }
        val numRec = validRecordings.size.coerceAtLeast(1)
        val recOrbitRadius = if (numRec <= 2) 280f else 380f

        val recordingMap = validRecordings.associateBy { it.id }

        validRecordings.forEachIndexed { index, rec ->
            val angle = (2.0 * Math.PI * index / numRec - Math.PI / 2.0)
            val rx = (cos(angle) * recOrbitRadius).toFloat()
            val ry = (sin(angle) * recOrbitRadius).toFloat()

            val recNode = MeshNode(
                id = "rec_${rec.id}",
                kind = MeshNodeKind.RECORDING,
                label = truncateLabel(rec.name, maxWords = 3, maxChars = 20),
                fullText = rec.name,
                evidence = rec.summary,
                rationale = if (rec.duration > 0) "Duration: ${rec.duration / 1000}s" else null,
                recordingId = rec.id,
                recordingName = rec.name,
                timestamp = rec.timestamp,
                keywords = extractKeywords(rec.name + " " + (rec.summary ?: "")),
                x = rx,
                y = ry,
                radius = 32f,
                color = Color(0xFF334155) // Slate Dark
            )
            nodes.add(recNode)

            // 2. Child Insights orbiting this Recording
            val recInsights = insights.filter { it.recordingId == rec.id }
            val count = recInsights.size
            if (count > 0) {
                val insightOrbitRadius = 145f
                val arcSpan = if (count == 1) 0.0 else Math.min(Math.PI * 1.2, count * 0.45)
                val startAngle = angle - arcSpan / 2.0

                recInsights.forEachIndexed { iIdx, insight ->
                    val insAngle = if (count == 1) angle else startAngle + (arcSpan * iIdx / (count - 1).toDouble())
                    val ix = rx + (cos(insAngle) * insightOrbitRadius).toFloat()
                    val iy = ry + (sin(insAngle) * insightOrbitRadius).toFloat()

                    val (kind, color) = when (insight.kind) {
                        InsightKind.ACTION -> MeshNodeKind.ACTION to CobaltBlue
                        InsightKind.IDEA -> MeshNodeKind.IDEA to Color(0xFFF59E0B) // Amber
                        InsightKind.DECISION -> MeshNodeKind.DECISION to Color(0xFF10B981) // Emerald
                        else -> MeshNodeKind.ACTION to CobaltBlue
                    }

                    val insNode = MeshNode(
                        id = "ins_${insight.id}",
                        kind = kind,
                        label = truncateLabel(insight.text, maxWords = 3, maxChars = 18),
                        fullText = insight.text,
                        evidence = insight.evidence,
                        rationale = insight.rationale,
                        status = insight.status,
                        recordingId = rec.id,
                        recordingName = rec.name,
                        timestamp = insight.createdAt,
                        rawInsight = insight,
                        keywords = extractKeywords(insight.text + " " + (insight.rationale ?: "") + " " + (insight.evidence ?: "")),
                        x = ix,
                        y = iy,
                        radius = 24f,
                        color = color
                    )
                    nodes.add(insNode)

                    // Connect Insight to its Origin Recording with Flexible Wire
                    edges.add(
                        MeshEdge(
                            id = "edge_from_${rec.id}_${insight.id}",
                            fromId = "rec_${rec.id}",
                            toId = "ins_${insight.id}",
                            relation = "FROM_NOTE",
                            label = "Extracted from"
                        )
                    )
                }
            }
        }

        // 3. Theme Clusters (positioned near centroid of participating notes)
        themes.forEachIndexed { tIdx, theme ->
            val linkedRecs = theme.noteIds.mapNotNull { recordingMap[it] }
            val avgX = if (linkedRecs.isNotEmpty()) linkedRecs.map { rec ->
                val rIdx = validRecordings.indexOfFirst { it.id == rec.id }
                val angle = (2.0 * Math.PI * rIdx / numRec - Math.PI / 2.0)
                (cos(angle) * recOrbitRadius * 0.45f).toFloat()
            }.average().toFloat() else 0f

            val avgY = if (linkedRecs.isNotEmpty()) linkedRecs.map { rec ->
                val rIdx = validRecordings.indexOfFirst { it.id == rec.id }
                val angle = (2.0 * Math.PI * rIdx / numRec - Math.PI / 2.0)
                (sin(angle) * recOrbitRadius * 0.45f).toFloat()
            }.average().toFloat() else (tIdx * 40f)

            val themeNode = MeshNode(
                id = "theme_${theme.key}",
                kind = MeshNodeKind.THEME,
                label = truncateLabel(theme.title, maxWords = 3, maxChars = 18),
                fullText = theme.title,
                evidence = theme.sampleSnippets.firstOrNull(),
                rationale = "Recurring pattern across ${theme.noteCount} voice notes",
                recordingName = "${theme.noteCount} Notes",
                keywords = extractKeywords(theme.title),
                x = avgX,
                y = avgY,
                radius = 28f,
                color = Color(0xFF8B5CF6) // Royal Violet
            )
            nodes.add(themeNode)

            // Connect Theme to Participating Recording Nodes
            theme.noteIds.forEach { recId ->
                if (recordingMap.containsKey(recId)) {
                    edges.add(
                        MeshEdge(
                            id = "edge_theme_${theme.key}_$recId",
                            fromId = "theme_${theme.key}",
                            toId = "rec_$recId",
                            relation = "THEME_CLUSTER",
                            label = "Recurring theme"
                        )
                    )
                }
            }
        }

        // 4. Compute Cross-Note Similarity Edges (Special priority for Ideas across distinct notes)
        val ideaNodes = nodes.filter { it.kind == MeshNodeKind.IDEA }
        val maxSimEdgesPerNode = 3
        val simEdgeCount = mutableMapOf<String, Int>()

        // 4a. Connect Similar Ideas Across Notes with dedicated Amber-Violet Flexible Wires
        for (i in 0 until ideaNodes.size) {
            val a = ideaNodes[i]
            for (j in i + 1 until ideaNodes.size) {
                val b = ideaNodes[j]
                if (a.recordingId != b.recordingId && a.keywords.isNotEmpty() && b.keywords.isNotEmpty()) {
                    val shared = a.keywords.intersect(b.keywords)
                    // Connect if sharing any concept keyword/stem
                    if (shared.isNotEmpty()) {
                        val countA = simEdgeCount.getOrDefault(a.id, 0)
                        val countB = simEdgeCount.getOrDefault(b.id, 0)
                        if (countA < maxSimEdgesPerNode && countB < maxSimEdgesPerNode) {
                            val topic = shared.first().replaceFirstChar { it.uppercase() }
                            edges.add(
                                MeshEdge(
                                    id = "sim_idea_${a.id}_${b.id}",
                                    fromId = a.id,
                                    toId = b.id,
                                    relation = "SIMILAR_IDEA",
                                    label = "Connected Idea: $topic",
                                    isSimilar = true
                                )
                            )
                            simEdgeCount[a.id] = countA + 1
                            simEdgeCount[b.id] = countB + 1
                        }
                    }
                }
            }
        }

        // 4b. Connect other insights (Actions & Decisions) across notes if strong overlap (>= 2 keywords)
        val otherInsights = nodes.filter { it.rawInsight != null && it.kind != MeshNodeKind.IDEA }
        for (i in 0 until otherInsights.size) {
            val a = otherInsights[i]
            for (j in i + 1 until otherInsights.size) {
                val b = otherInsights[j]
                if (a.recordingId != b.recordingId && a.keywords.isNotEmpty() && b.keywords.isNotEmpty()) {
                    val shared = a.keywords.intersect(b.keywords)
                    if (shared.size >= 2) {
                        val countA = simEdgeCount.getOrDefault(a.id, 0)
                        val countB = simEdgeCount.getOrDefault(b.id, 0)
                        if (countA < maxSimEdgesPerNode && countB < maxSimEdgesPerNode) {
                            edges.add(
                                MeshEdge(
                                    id = "sim_${a.id}_${b.id}",
                                    fromId = a.id,
                                    toId = b.id,
                                    relation = "SIMILAR_TO",
                                    label = "Similar: " + shared.take(2).joinToString(", "),
                                    isSimilar = true
                                )
                            )
                            simEdgeCount[a.id] = countA + 1
                            simEdgeCount[b.id] = countB + 1
                        }
                    }
                }
            }
        }

        return MeshGraphData(nodes = nodes, edges = edges)
    }
}
