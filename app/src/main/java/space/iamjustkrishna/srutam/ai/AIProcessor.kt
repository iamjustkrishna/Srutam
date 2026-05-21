package space.iamjustkrishna.srutam.ai

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.content
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

    private fun createModel(modelName: String, timeoutMs: Long): GenerativeModel {
        val apiKey = AppPreferences.getGeminiApiKey(context)
            .takeIf { it.isNotBlank() }
            ?: space.iamjustkrishna.srutam.BuildConfig.GEMINI_API_KEY.trim()
        return GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            requestOptions = RequestOptions(timeout = timeoutMs)
        )
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
            val response = withTimeout(INSIGHTS_TIMEOUT_MS) {
                createModel(
                    modelName = "gemini-2.5-flash-lite",
                    timeoutMs = INSIGHTS_TIMEOUT_MS
                ).generateContent(prompt)
            }
            val responseText = response.text ?: throw Exception("Empty response from AI")

            // Parse JSON response
            parseAIResponse(responseText)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating insights, using fallback", e)
            // Fallback to basic analysis
            generateFallbackInsights(transcript)
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

            val response = withTimeout(CHUNK_SUMMARY_TIMEOUT_MS) {
                createModel(
                    modelName = "gemini-2.5-flash-lite",
                    timeoutMs = CHUNK_SUMMARY_TIMEOUT_MS
                ).generateContent(prompt)
            }

            val summary = response.text?.trim().orEmpty()
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
              "summary": "Executive Summary in exactly 2 sentences maximum. First sentence states the main topic. Second sentence highlights the key takeaway.",
              "keyPoints": [
                "Key insight 1 - specific and actionable",
                "Key insight 2 - includes important details",
                "Key insight 3 - highlights critical information"
              ],
              "actionItems": [
                "[ ] Specific action task 1 with clear next step",
                "[ ] Specific action task 2 with measurable outcome",
                "[ ] Specific action task 3 with deadline or priority"
              ],
              "wiifm": "What's In It For Me: This recording helps you by [specific personal benefit]. You can use this to [concrete application or value]."
            }

            Rules:
            - Summary: EXACTLY 2 sentences, no more
            - Key Insights: 3-5 bullet points, specific and detailed
            - Action Items: Format as checklist with [ ] prefix, be specific and actionable
            - WIIFM: Must start with "What's In It For Me:", explain personal utility and value
            - All fields must be present and non-empty
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

            AIInsights(
                summary = jsonObject["summary"] as? String ?: "Summary not available",
                keyPoints = (jsonObject["keyPoints"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                actionItems = (jsonObject["actionItems"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                wiifm = jsonObject["wiifm"] as? String ?: "Value not specified"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing AI response", e)
            generateFallbackInsights(responseText)
        }
    }

    private fun generateFallbackInsights(transcript: String): AIInsights {
        // Basic fallback when AI is not available
        val sentences = transcript.split(". ").filter { it.isNotBlank() }
        val wordCount = transcript.split(" ").size

        return AIInsights(
            summary = "This recording contains approximately $wordCount words across ${sentences.size} sentences. " +
                    "The content has been captured and is ready for detailed review.",
            keyPoints = listOf(
                "Audio successfully transcribed with ${sentences.size} distinct segments",
                "Recording captured $wordCount words of content",
                "Full transcript available for detailed analysis and reference"
            ),
            actionItems = listOf(
                "[ ] Review the complete transcript for key information",
                "[ ] Extract specific action items and decisions manually",
                "[ ] Share or export relevant sections as needed"
            ),
            wiifm = "What's In It For Me: This recording preserves your spoken thoughts and conversations for future reference. " +
                    "You can use this to review important details, extract action items, and ensure nothing gets forgotten."
        )
    }

    suspend fun queryRecording(transcript: String, question: String): String = withContext(Dispatchers.IO) {
        try {
            val generativeModel = createModel(
                modelName = "gemini-2.5-flash",
                timeoutMs = QUERY_TIMEOUT_MS
            )

            val prompt = """
                Based on the following transcript, please answer this question:

                Question: $question

                Transcript:
                $transcript

                Provide a clear, concise answer based only on the information in the transcript.
            """.trimIndent()

            val response = withTimeout(QUERY_TIMEOUT_MS) {
                generativeModel.generateContent(prompt)
            }
            response.text ?: "I couldn't generate an answer. Please try again."
        } catch (e: Exception) {
            Log.e(TAG, "Error querying recording", e)
            "Error: ${e.message ?: "Unable to process query"}"
        }
    }

    data class AIInsights(
        val summary: String,
        val keyPoints: List<String>,
        val actionItems: List<String>,
        val wiifm: String
    )

    companion object {
        private const val TAG = "AIProcessor"
        private const val INSIGHTS_TIMEOUT_MS = 120_000L
        private const val CHUNK_SUMMARY_TIMEOUT_MS = 90_000L
        private const val QUERY_TIMEOUT_MS = 60_000L
        private const val TRANSCRIPT_CHUNK_THRESHOLD_CHARS = 12_000
    }
}
