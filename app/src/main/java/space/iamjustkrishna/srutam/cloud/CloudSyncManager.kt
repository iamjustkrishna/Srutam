package space.iamjustkrishna.srutam.cloud

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import space.iamjustkrishna.srutam.data.AppDatabase
import space.iamjustkrishna.srutam.utils.AppPreferences
import java.util.concurrent.TimeUnit

object CloudSyncManager {

    private const val WORK_NAME = "srutam_cloud_sync_worker"

    /**
     * Triggers a cloud sync job if the user is signed in.
     */
    fun enqueueSync(context: Context) {
        if (!AppPreferences.isCloudSignedIn(context)) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<CloudSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    /**
     * Marks a specific recording for sync and triggers the background worker.
     */
    fun enqueueSyncForRecording(context: Context, recordingId: Long) {
        if (!AppPreferences.isCloudSignedIn(context)) return

        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            database.recordingDao().markForSync(recordingId)
            enqueueSync(context)
        }
    }
}
