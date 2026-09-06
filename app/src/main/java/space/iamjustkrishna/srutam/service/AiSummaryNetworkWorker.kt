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
import kotlinx.coroutines.flow.first
import space.iamjustkrishna.srutam.data.AppDatabase
import space.iamjustkrishna.srutam.data.RecordingAiStatus

class AiSummaryNetworkWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "AiSummaryNetworkWorker triggered with active internet connection")
        val database = AppDatabase.getDatabase(applicationContext)
        val recordingDao = database.recordingDao()

        try {
            val allRecordings = recordingDao.getAllRecordings().first()
            val pendingRecordings = allRecordings.filter { rec ->
                rec.aiStatus == RecordingAiStatus.SUMMARY_PENDING_OFFLINE ||
                    (!rec.transcript.isNullOrBlank() && rec.summary.isNullOrBlank() && !rec.isProcessing)
            }

            if (pendingRecordings.isEmpty()) {
                Log.d(TAG, "No pending offline recordings found to process")
                return Result.success()
            }

            val pendingIds = pendingRecordings.map { it.id }
            Log.d(TAG, "Resuming AI summary generation for ${pendingIds.size} pending recordings")

            // Re-enqueue in AiProcessingWorker which shows foreground notification and completion alert
            AiProcessingWorker.enqueueProcessing(applicationContext, pendingIds)

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in AiSummaryNetworkWorker", e)
            return Result.retry()
        }
    }

    companion object {
        private const val TAG = "AiSummaryNetworkWorker"
        const val UNIQUE_WORK_NAME = "srutam_ai_summary_network_work"

        fun enqueueWhenConnected(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<AiSummaryNetworkWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Log.d(TAG, "Scheduled AiSummaryNetworkWorker to resume when internet connects")
        }
    }
}
