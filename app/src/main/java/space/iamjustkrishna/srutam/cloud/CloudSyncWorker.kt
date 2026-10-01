package space.iamjustkrishna.srutam.cloud

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.iamjustkrishna.srutam.data.AppDatabase
import space.iamjustkrishna.srutam.data.InsightKind
import space.iamjustkrishna.srutam.data.InsightStatus
import space.iamjustkrishna.srutam.data.SyncStatus
import space.iamjustkrishna.srutam.utils.AppPreferences

class CloudSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!AppPreferences.isCloudSignedIn(applicationContext)) {
            // User has not opted into Cloud & MCP sync yet
            return@withContext Result.success()
        }

        val database = AppDatabase.getDatabase(applicationContext)
        val recordingDao = database.recordingDao()
        val insightDao = database.insightDao()
        val reminderDao = database.reminderDao()
        val client = SupabaseCloudClient(applicationContext)

        var hasFailures = false

        // 1. Push pending recordings & action items to cloud
        try {
            val pending = recordingDao.getPendingSyncRecordings()
            for (recording in pending) {
                try {
                    recordingDao.updateSyncStatus(
                        id = recording.id,
                        status = SyncStatus.SYNCING,
                        cloudId = recording.cloudId,
                        syncedAt = System.currentTimeMillis()
                    )

                    val insights = insightDao.getInsightsByRecordingId(recording.id)
                    val reminders = reminderDao.getRemindersByRecordingId(recording.id)
                    val uploadResult = client.uploadNote(recording, insights, reminders)

                    if (uploadResult.isSuccess) {
                        val cloudId = uploadResult.getOrThrow()
                        recordingDao.updateSyncStatus(
                            id = recording.id,
                            status = SyncStatus.SYNCED,
                            cloudId = cloudId,
                            syncedAt = System.currentTimeMillis()
                        )
                    } else {
                        hasFailures = true
                        recordingDao.updateSyncStatus(
                            id = recording.id,
                            status = SyncStatus.PENDING,
                            cloudId = recording.cloudId,
                            syncedAt = System.currentTimeMillis()
                        )
                    }
                } catch (e: Exception) {
                    hasFailures = true
                    recordingDao.updateSyncStatus(
                        id = recording.id,
                        status = SyncStatus.PENDING,
                        cloudId = recording.cloudId,
                        syncedAt = System.currentTimeMillis()
                    )
                }
            }
        } catch (e: Exception) {
            hasFailures = true
        }

        // 2. Pull remote updates made by external AI agents (e.g. Cursor / Antigravity completing action items)
        try {
            val pullResult = client.pullCompletedActionItems()
            if (pullResult.isSuccess) {
                val remoteUpdates = pullResult.getOrThrow()
                for (remote in remoteUpdates) {
                    if (remote.isCompleted) {
                        // Find local insight matching description and mark it completed
                        val localInsights = insightDao.getAllInsights()
                        for (local in localInsights) {
                            if (local.kind == InsightKind.ACTION &&
                                local.status != InsightStatus.COMPLETED &&
                                (local.text.trim().equals(remote.description.trim(), ignoreCase = true) ||
                                 local.text.contains(remote.description.take(30), ignoreCase = true))
                            ) {
                                insightDao.updateActionStatus(local.id, InsightStatus.COMPLETED, System.currentTimeMillis())
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Non-critical pull failure
        }

        AppPreferences.setCloudLastSyncTime(applicationContext, System.currentTimeMillis())

        if (hasFailures && runAttemptCount < 3) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}
