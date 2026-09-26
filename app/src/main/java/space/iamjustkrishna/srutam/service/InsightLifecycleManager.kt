package space.iamjustkrishna.srutam.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.iamjustkrishna.srutam.data.InsightDao
import space.iamjustkrishna.srutam.data.ReminderDao

/**
 * Performs time-based lifecycle maintenance for action items and reminders.
 * Called once on Application.onCreate().
 *
 * Responsibilities:
 * 1. Auto-archive completed actions older than [AUTO_ARCHIVE_AFTER_DAYS] days
 * 2. Auto-dismiss ACTIVE reminders whose event time has passed (with 1-hour grace)
 * 3. Purge very old archived actions (30 days) and old reminders (60 days)
 */
object InsightLifecycleManager {

    private const val TAG = "InsightLifecycleManager"

    /** Completed actions are auto-archived after 3 days. */
    private const val AUTO_ARCHIVE_AFTER_DAYS = 3L
    /** Archived items are hard-deleted after 30 days. */
    private const val PURGE_ARCHIVED_AFTER_DAYS = 30L
    /** Old completed/dismissed reminders are purged after 60 days. */
    private const val PURGE_REMINDERS_AFTER_DAYS = 60L
    /** Grace period: reminders are auto-dismissed 1 hour after their event time. */
    private const val REMINDER_GRACE_PERIOD_MS = 60 * 60 * 1000L

    /**
     * Run all lifecycle cleanup tasks. Safe to call from Application.onCreate()
     * within a coroutine scope.
     */
    suspend fun runStartupCleanup(
        insightDao: InsightDao,
        reminderDao: ReminderDao,
        context: Context
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        try {
            // 1. Auto-archive completed actions older than 3 days
            val archiveCutoff = now - AUTO_ARCHIVE_AFTER_DAYS * 24 * 60 * 60 * 1000L
            insightDao.autoArchiveStaleCompleted(cutoffMs = archiveCutoff, now = now)
            Log.d(TAG, "Auto-archived stale completed actions (cutoff: $archiveCutoff)")

            // 2. Hard-delete very old archived items (30+ days)
            val purgeCutoff = now - PURGE_ARCHIVED_AFTER_DAYS * 24 * 60 * 60 * 1000L
            insightDao.purgeOldArchived(cutoffMs = purgeCutoff)
            Log.d(TAG, "Purged old archived actions (cutoff: $purgeCutoff)")

            // 3. Auto-dismiss overdue ACTIVE reminders (event time + 1hr grace)
            val reminderCutoff = now - REMINDER_GRACE_PERIOD_MS
            reminderDao.autoDismissOverdue(cutoffMs = reminderCutoff)
            Log.d(TAG, "Auto-dismissed overdue reminders (cutoff: $reminderCutoff)")

            // 4. Purge old completed/dismissed reminders (60+ days old)
            val reminderPurgeCutoff = now - PURGE_REMINDERS_AFTER_DAYS * 24 * 60 * 60 * 1000L
            reminderDao.purgeOldReminders(cutoffMs = reminderPurgeCutoff)
            Log.d(TAG, "Purged old completed/dismissed reminders (cutoff: $reminderPurgeCutoff)")

        } catch (e: Exception) {
            Log.e(TAG, "Lifecycle cleanup failed", e)
        }
    }
}
