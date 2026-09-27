package space.iamjustkrishna.srutam.service

import android.content.Context
import space.iamjustkrishna.srutam.data.InsightDao
import space.iamjustkrishna.srutam.data.ReminderDao
import space.iamjustkrishna.srutam.repository.InsightsRepository

/** Archives completed tasks without purging history; reconciles confirmed reminders only. */
object InsightLifecycleManager {
    suspend fun runStartupCleanup(insightDao: InsightDao, reminderDao: ReminderDao, context: Context) {
        InsightsRepository.from(context).reconcile()
    }
}
