package space.iamjustkrishna.srutam.ai

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import space.iamjustkrishna.srutam.ai.provider.AnthropicLlmClient
import space.iamjustkrishna.srutam.ai.provider.GeminiLlmClient
import space.iamjustkrishna.srutam.ai.provider.GroqLlmClient
import space.iamjustkrishna.srutam.ai.provider.LlmClient
import space.iamjustkrishna.srutam.ai.provider.OpenAiLlmClient
import space.iamjustkrishna.srutam.ai.provider.SrutamCloudRouter
import space.iamjustkrishna.srutam.data.AiQueryCache
import space.iamjustkrishna.srutam.data.AppDatabase
import com.google.gson.Gson
import space.iamjustkrishna.srutam.utils.AppPreferences
import space.iamjustkrishna.srutam.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

data class AIProcessingResult(
    val transcript: String,
    val summary: String,
    val keyPoints: String, // JSON array
    val actionItems: String, // JSON array
    val wiifm: String
)

class AIProcessor(private val context: Context) {

    private val gson = Gson()
    private val localTranscriber = LocalTranscriber(context)
    private val cacheDao by lazy { AppDatabase.getDatabase(context).aiQueryCacheDao() }

    suspend fun processRecording(audioFile: File): AIProcessingResult = withContext(Dispatchers.IO) {
        try {
            val transcript = transcribeAudio(audioFile)
            val insights = if (NetworkUtils.isInternetAvailable(context)) {
                generateInsights(transcript)
            } else {
                null
            }

            AIProcessingResult(
                transcript = transcript,
                summary = insights?.summary.orEmpty(),
                keyPoints = insights?.let { gson.toJson(it.keyPoints) }.orEmpty(),
                actionItems = insights?.let { gson.toJson(it.actionItems) }.orEmpty(),
                wiifm = insights?.wiifm.orEmpty()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error processing recording", e)
            throw e
        }
    }

    private fun getLlmClient(overrideModel: String? = null): LlmClient {
        val provider = AppPreferences.getAIProvider(context)
        return when (provider) {
            AppPreferences.PROVIDER_SRUTAM_DEFAULT -> SrutamCloudRouter(context)
            AppPreferences.PROVIDER_GROQ -> GroqLlmClient(context, overrideModel = overrideModel)
            AppPreferences.PROVIDER_OPENAI -> OpenAiLlmClient(context, overrideModel = overrideModel)
            AppPreferences.PROVIDER_ANTHROPIC -> AnthropicLlmClient(context, overrideModel = overrideModel)
            AppPreferences.PROVIDER_GEMINI -> GeminiLlmClient(context, overrideModel = overrideModel)
            else -> SrutamCloudRouter(context)
        }
    }

    suspend fun transcribeAudio(audioFile: File): String = withContext(Dispatchers.IO) {
        val duration = getAudioDuration(audioFile)
        Log.d(TAG, "Transcribing audio file: ${audioFile.name}, duration: $duration ms")

        try {
            if (!audioFile.exists()) {
                throw IllegalStateException("Audio file not found: ${audioFile.absolutePath}")
            }
            localTranscriber.transcribe(audioFile)
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed completely", e)
            throw e
        }
    }

    private fun getAudioDuration(audioFile: File): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(audioFile.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            duration?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting audio duration", e)
            0L
        }
    }

    suspend fun generateInsights(transcript: String): AIInsights = withContext(Dispatchers.IO) {
        try {
            val normalizedTranscript = transcript.trim()
            if (normalizedTranscript.isBlank()) {
                throw IllegalStateException("Transcript is empty")
            }

            val transcriptForAnalysis = if (normalizedTranscript.length > TRANSCRIPT_CHUNK_THRESHOLD_CHARS) {
                summarizeTranscriptChunks(normalizedTranscript)
            } else {
                normalizedTranscript
            }

            val prompt = buildStructuredInsightsPrompt(transcriptForAnalysis)
            val responseText = getLlmClient().generateText(prompt, INSIGHTS_TIMEOUT_MS)

            // Parse JSON response
            parseAIResponse(responseText)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating insights", e)
            throw e
        }
    }

    private suspend fun summarizeTranscriptChunks(transcript: String): String = withContext(Dispatchers.IO) {
        val chunks = chunkText(transcript, TRANSCRIPT_CHUNK_THRESHOLD_CHARS)
        val chunkSummaries = mutableListOf<String>()

        for ((index, chunk) in chunks.withIndex()) {
            val prompt = """
                Summarize this transcript chunk for later aggregation.

                Return:
                - 5 concise bullet points with the most important facts
                - action items mentioned
                - unresolved questions or decisions

                Keep it factual. Do not invent details.

                Chunk ${index + 1} of ${chunks.size}:
                $chunk
            """.trimIndent()

            val summary = getLlmClient().generateText(prompt, CHUNK_SUMMARY_TIMEOUT_MS).trim()
            if (summary.isNotBlank()) {
                chunkSummaries += "Chunk ${index + 1} Summary:\n$summary"
            }
        }

        if (chunkSummaries.isEmpty()) {
            throw IllegalStateException("Failed to generate chunk summaries")
        }

        chunkSummaries.joinToString("\n\n")
    }

    private fun buildStructuredInsightsPrompt(transcript: String): String {
        return """
            Analyze the following transcript and provide structured insights in EXACTLY this format:

            Transcript:
            $transcript

            Required output format (JSON):
            {
              "title": "Concise 2 to 4 word topic title (e.g. Design Sync, Budget Review, Apartment Hunt)",
              "summary": "Executive Summary in exactly 2 sentences maximum. First sentence states the main topic. Second sentence highlights the key takeaway.",
              "keyPoints": [
                "Key insight 1 - specific and actionable",
                "Key insight 2 - includes important details",
                "Key insight 3 - highlights critical information"
              ],
              "actionItems": [
                "[ ] Specific action task 1 with clear next step",
                "[ ] Specific action task 2 with measurable outcome"
              ],
              "ideas": [
                "A distinct possibility, proposal, or conceptual thought worth remembering"
              ],
              "decisions": [
                {
                  "text": "Explicit conclusion or choice that was made",
                  "rationale": "Brief reason or context behind the decision"
                }
              ],
              "reminders": [
                {
                  "title": "Concise event or meeting title (e.g. Sync with Alex, Dentist Appointment, Submit Tax Return)",
                  "timeDescription": "Time expression as stated in transcript (e.g. tomorrow at 3pm, next Friday, in 2 hours)",
                  "estimatedTimeOffsetHours": 24,
                  "person": "Name of person or null",
                  "location": "Location or platform or null",
                  "type": "MEETING, DEADLINE, CALL, or REMINDER"
                }
              ],
              "wiifm": "What's In It For Me: This recording helps you by [specific personal benefit]. You can use this to [concrete application or value]."
            }

            Rules:
            - Summary: EXACTLY 2 sentences, no more
            - Key Insights: 3-5 bullet points, specific and detailed
            - Action Items: CRITICAL - Only extract actionable tasks or commitments IF EXPLICITLY MENTIONED in the transcript. Most voice notes (e.g. personal thoughts, diary entries, ideas) do NOT contain any tasks. If no clear action items are explicitly mentioned, you MUST return [] for "actionItems". NEVER invent generic to-dos.
            - Ideas: 0-4 distinct proposals, concepts, or thoughts worth remembering. Empty array [] if none.
            - Decisions: Only include explicit conclusions, choices, or agreements made in the transcript. Empty array [] if none.
            - Reminders: Extract scheduled meetings, events, appointments, or deadlines explicitly mentioned with an estimated future time. Empty array [] if none.
            - WIIFM: Must start with "What's In It For Me:", explain personal utility and value.
            - summary, keyPoints, and wiifm must be present and non-empty. actionItems, ideas, decisions, and reminders may be empty [].
        """.trimIndent()
    }

    private fun chunkText(text: String, maxChars: Int): List<String> {
        if (text.length <= maxChars) return listOf(text)

        val chunks = mutableListOf<String>()
        var start = 0

        while (start < text.length) {
            var end = minOf(start + maxChars, text.length)
            if (end < text.length) {
                val splitAt = text.lastIndexOf('\n', end).takeIf { it > start + (maxChars / 2) }
                    ?: text.lastIndexOf(' ', end).takeIf { it > start + (maxChars / 2) }
                if (splitAt != null) {
                    end = splitAt
                }
            }
            chunks += text.substring(start, end).trim()
            start = end
        }

        return chunks.filter { it.isNotBlank() }
    }

    private fun parseAIResponse(responseText: String): AIInsights {
        return try {
            // Extract JSON from response (it might be wrapped in markdown code blocks)
            val jsonText = if (responseText.contains("```json")) {
                responseText.substringAfter("```json").substringBefore("```").trim()
            } else if (responseText.contains("```")) {
                responseText.substringAfter("```").substringBefore("```").trim()
            } else {
                responseText.trim()
            }

            val jsonObject = gson.fromJson(jsonText, Map::class.java)

            val rawKeyPoints = (jsonObject["keyPoints"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val rawActionItems = (jsonObject["actionItems"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val rawIdeas = (jsonObject["ideas"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val rawDecisionsList = jsonObject["decisions"] as? List<*> ?: emptyList<Any>()

            val parsedDecisions = rawDecisionsList.mapNotNull { item ->
                when (item) {
                    is Map<*, *> -> {
                        val text = item["text"] as? String
                        if (!text.isNullOrBlank()) {
                            AIDecision(
                                text = text.trim(),
                                rationale = item["rationale"] as? String,
                                evidence = item["evidence"] as? String
                            )
                        } else null
                    }
                    is String -> if (item.isNotBlank()) AIDecision(text = item.trim()) else null
                    else -> null
                }
            }

            // Fallback for ideas: if AI didn't return explicit ideas, derive from keyPoints
            val finalIdeas = if (rawIdeas.isNotEmpty()) {
                rawIdeas
            } else {
                rawKeyPoints.filter { pt ->
                    !pt.startsWith("[ ]") && !pt.startsWith("[]")
                }
            }

            // Fallback for decisions: if AI didn't return explicit decisions, detect from keyPoints
            val decisionKeywords = listOf("decid", "agree", "plan", "will ", "chose", "chosen", "approv", "commit", "conclud", "finaliz", "schedule", "deadline", "next step")
            val finalDecisions = if (parsedDecisions.isNotEmpty()) {
                parsedDecisions
            } else {
                rawKeyPoints.filter { pt ->
                    val lower = pt.lowercase()
                    decisionKeywords.any { lower.contains(it) }
                }.map { AIDecision(text = it.trim()) }
            }

            val rawRemindersList = jsonObject["reminders"] as? List<*> ?: emptyList<Any>()
            val parsedReminders = rawRemindersList.mapNotNull { item ->
                when (item) {
                    is Map<*, *> -> {
                        val title = item["title"] as? String
                        if (!title.isNullOrBlank()) {
                            val timeDesc = item["timeDescription"] as? String ?: ""
                            val offsetHours = (item["estimatedTimeOffsetHours"] as? Number)?.toDouble() ?: -1.0
                            val eventTimeMs = if (offsetHours > 0) {
                                System.currentTimeMillis() + (offsetHours * 3600 * 1000).toLong()
                            } else {
                                parseTimeDescription(timeDesc)
                            }
                            AIReminder(
                                title = title.trim(),
                                eventTimeMs = eventTimeMs,
                                originalText = timeDesc,
                                person = (item["person"] as? String)?.trim()?.takeIf { it.isNotBlank() },
                                location = (item["location"] as? String)?.trim()?.takeIf { it.isNotBlank() },
                                type = (item["type"] as? String)?.trim()?.uppercase()?.takeIf {
                                    it in listOf("MEETING", "DEADLINE", "CALL", "REMINDER")
                                } ?: "REMINDER"
                            )
                        } else null
                    }
                    else -> null
                }
            }

            AIInsights(
                title = (jsonObject["title"] as? String)?.trim()?.takeIf { it.isNotBlank() },
                summary = jsonObject["summary"] as? String ?: "Summary not available",
                keyPoints = rawKeyPoints,
                actionItems = rawActionItems,
                ideas = finalIdeas,
                decisions = finalDecisions,
                reminders = parsedReminders,
                wiifm = jsonObject["wiifm"] as? String ?: (jsonObject["whatsInItForMe"] as? String) ?: "Value not specified"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing AI response", e)
            throw e
        }
    }

    private fun parseTimeDescription(timeDesc: String): Long {
        val lower = timeDesc.lowercase()
        val now = System.currentTimeMillis()
        return when {
            lower.contains("tomorrow") -> now + 24 * 3600 * 1000L
            lower.contains("day after") -> now + 48 * 3600 * 1000L
            lower.contains("next week") -> now + 7 * 24 * 3600 * 1000L
            lower.contains("tonight") || lower.contains("today") -> now + 4 * 3600 * 1000L
            lower.contains("in an hour") || lower.contains("1 hour") -> now + 3600 * 1000L
            lower.contains("in 2 hours") || lower.contains("2 hours") -> now + 2 * 3600 * 1000L
            else -> now + 24 * 3600 * 1000L // Default to tomorrow
        }
    }

    fun generateFallbackInsights(transcript: String): AIInsights {
        // Basic fallback when AI is not available
        val sentences = transcript.split(". ").filter { it.isNotBlank() }
        val wordCount = transcript.split(" ").size

        return AIInsights(
            title = null,
            summary = "This recording contains approximately $wordCount words across ${sentences.size} sentences. " +
                    "The content has been captured and is ready for detailed review.",
            keyPoints = listOf(
                "Audio successfully transcribed with ${sentences.size} distinct segments",
                "Recording captured $wordCount words of content",
                "Ready for full analysis when connected to internet"
            ),
            actionItems = emptyList(),
            ideas = listOf("Audio captured offline and ready for AI insights when connected"),
            decisions = emptyList(),
            reminders = emptyList(),
            wiifm = "What's In It For Me: This provides an instant offline overview of your audio length and structural complexity before cloud processing."
        )
    }

    data class AIDecision(
        val text: String,
        val rationale: String? = null,
        val evidence: String? = null
    )

    data class AIReminder(
        val title: String,
        val eventTimeMs: Long,
        val originalText: String = "",
        val person: String? = null,
        val location: String? = null,
        val type: String = "REMINDER" // MEETING, DEADLINE, REMINDER, CALL
    )

    data class AIInsights(
        val title: String? = null,
        val summary: String,
        val keyPoints: List<String>,
        val actionItems: List<String>,
        val ideas: List<String> = emptyList(),
        val decisions: List<AIDecision> = emptyList(),
        val reminders: List<AIReminder> = emptyList(),
        val wiifm: String
    )

    suspend fun queryRecording(transcript: String, question: String, recordingId: Long? = null): String = withContext(Dispatchers.IO) {
        val normalizedQ = AiCacheUtils.normalizeQuery(question)
        val contextKey = if (recordingId != null) "rec_$recordingId" else "tx_${transcript.hashCode()}"
        val cacheKey = AiCacheUtils.sha256("single:$contextKey:$normalizedQ")

        try {
            val cached = cacheDao.get(cacheKey)
            if (cached != null) {
                Log.d(TAG, "Cache HIT for single-note query: $question")
                cacheDao.updateAccessTime(cacheKey)
                return@withContext cached.answer
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading query cache", e)
        }

        try {
            val prompt = """
                Based on the following transcript, please answer this question:

                Question: $question

                Transcript:
                $transcript

                Provide a clear, concise answer based only on the information in the transcript.
            """.trimIndent()

            val answer = getLlmClient().generateText(prompt, QUERY_TIMEOUT_MS).trim()
            val finalAnswer = answer.ifBlank { SrutamCloudRouter.HIGH_TRAFFIC_MESSAGE }

            if (finalAnswer != SrutamCloudRouter.HIGH_TRAFFIC_MESSAGE && !finalAnswer.startsWith("Something went wrong")) {
                try {
                    cacheDao.insert(
                        AiQueryCache(
                            cacheKey = cacheKey,
                            queryType = "SINGLE",
                            normalizedQuery = normalizedQ,
                            contextFingerprint = contextKey,
                            answer = finalAnswer
                        )
                    )
                    cacheDao.pruneOldEntries(500)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to save query cache", e)
                }
            }

            finalAnswer
        } catch (e: Exception) {
            Log.e(TAG, "Error querying recording", e)
            SrutamCloudRouter.HIGH_TRAFFIC_MESSAGE
        }
    }

    suspend fun queryAllRecordings(contextSnippets: List<String>, question: String): String = withContext(Dispatchers.IO) {
        try {
            val notesContext = contextSnippets.joinToString("\n\n---\n\n")

            val prompt = """
                You are Srutam AI, an intelligent personal voice notes companion for Srutam.
                The user is asking a question regarding their voice recordings, transcripts, summaries, and action items.
                Below are the relevant notes and excerpts retrieved from their library:

                $notesContext

                User Question: $question

                STRICT GUARDRAIL RULES:
                1. SCOPE RESTRICTION: You are strictly scoped to the user's voice notes, recordings, ideas, transcriptions, summaries, and action items.
                2. If the user's question is unrelated to their voice notes, recordings, or tasks (e.g. asking general trivia, coding, writing essays, math problems, weather, or general chit-chat outside their notes), you MUST politely refuse to answer:
                   "I am Srutam AI, designed specifically to help you search, summarize, and understand your voice notes and recordings. I can only assist with queries related to your voice notes and action items in Srutam."
                3. Do NOT mention internal search algorithms (such as BM25, embeddings, or technical implementation details).
                4. Answer helpfully, clearly, and concisely based strictly on the retrieved context above.
                5. When referencing information, cite the note name or date (e.g., "[Recording Title]").
                6. If the notes do not contain the answer, say that you couldn't find any mention of it in their voice recordings.
                7. Maintain a crisp, helpful, professional tone.
            """.trimIndent()

            val response = getLlmClient().generateText(prompt, QUERY_TIMEOUT_MS).trim()
            response.ifBlank { SrutamCloudRouter.HIGH_TRAFFIC_MESSAGE }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying all recordings", e)
            SrutamCloudRouter.HIGH_TRAFFIC_MESSAGE
        }
    }

    companion object {
        private const val TAG = "AIProcessor"
        private const val INSIGHTS_TIMEOUT_MS = 120_000L
        private const val CHUNK_SUMMARY_TIMEOUT_MS = 90_000L
        private const val QUERY_TIMEOUT_MS = 60_000L
        private const val TRANSCRIPT_CHUNK_THRESHOLD_CHARS = 12_000
    }
}
