package space.iamjustkrishna.srutam.service

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.Gson
import space.iamjustkrishna.srutam.ai.AIProcessor
import space.iamjustkrishna.srutam.data.AppDatabase
import space.iamjustkrishna.srutam.data.signature.NoteSignatureWorker
import space.iamjustkrishna.srutam.utils.AppPreferences

/**
 * One-time job that gives notes processed before topics existed their 2-4 topics, so recurring
 * themes cover old notes too. Ten notes per AI call, summaries and key points only. Runs only
 * while automatic AI is on, and keeps going until no note is left without topics.
 */
class TopicBackfillWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!AppPreferences.isAutoAiEnabled(applicationContext)) return Result.success()
        val dao = AppDatabase.getDatabase(applicationContext).recordingDao()
        val gson = Gson()
        val ai = AIProcessor(applicationContext)
        try {
            repeat(MAX_BATCHES_PER_RUN) {
                val batch = dao.getNotesWithoutTopics(BATCH_SIZE)
                if (batch.isEmpty()) return Result.success()

                val (fallback, real) = batch.partition { it.summary.orEmpty().startsWith(FALLBACK_SUMMARY) }
                // Offline placeholder summaries have nothing to tag; mark them so they aren't asked again.
                fallback.forEach { dao.setTopics(it.id, "[]") }
                if (real.isEmpty()) return@repeat

                val topics = ai.suggestTopics(real.associate { note ->
                    note.id to listOf(note.name, note.summary.orEmpty(), note.keyPoints.orEmpty()).joinToString("\n")
                })
                for (note in real) {
                    // A note the reply skipped gets an empty list, so a bad note can't block the rest.
                    dao.setTopics(note.id, gson.toJson(topics[note.id].orEmpty()))
                    NoteSignatureWorker.enqueue(applicationContext, note.id)
                }
            }
            // More left: carry on in a fresh run after this one.
            enqueue(applicationContext, continuation = true)
            return Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Topic backfill failed; will retry", e)
            return if (runAttemptCount < 3) Result.retry() else Result.success()
        } finally {
            runCatching { ai.close() }
        }
    }

    companion object {
        private const val TAG = "TopicBackfillWorker"
        private const val WORK_NAME = "topic_backfill"
        private const val BATCH_SIZE = 10
        private const val MAX_BATCHES_PER_RUN = 5
        private const val FALLBACK_SUMMARY = "This recording contains approximately"

        /** [continuation] queues behind a running backfill; otherwise a running or queued one is kept. */
        fun enqueue(context: Context, continuation: Boolean = false) {
            val request = OneTimeWorkRequestBuilder<TopicBackfillWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            runCatching {
                WorkManager.getInstance(context.applicationContext)
                    .enqueueUniqueWork(
                        WORK_NAME,
                        if (continuation) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.KEEP,
                        request
                    )
            }
        }
    }
}
