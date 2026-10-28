package space.iamjustkrishna.srutam.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import space.iamjustkrishna.srutam.MainActivity
import space.iamjustkrishna.srutam.R
import space.iamjustkrishna.srutam.data.AppDatabase
import space.iamjustkrishna.srutam.utils.Analytics
import space.iamjustkrishna.srutam.utils.AppPreferences
import space.iamjustkrishna.srutam.utils.TimeSaved
import space.iamjustkrishna.srutam.utils.WeeklyRecap
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * The Sunday evening note: how much time talking saved over the past week. Sent only when the
 * setting is on, notifications are allowed and the week saved at least a minute, so a quiet week
 * sends nothing. Tapping it opens Your Srutam.
 */
class WeeklyRecapWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (!AppPreferences.isWeeklyRecapEnabled(context)) return Result.success()
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return Result.success()

        val recordings = runCatching { AppDatabase.getDatabase(context).recordingDao().getAllRecordings().first() }
            .getOrElse { Log.w(TAG, "Couldn't read notes for the recap", it); return Result.retry() }
        val weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val (title, text) = WeeklyRecap.message(TimeSaved.period(recordings, weekAgo)) ?: return Result.success()

        val channel = NotificationChannel(CHANNEL_ID, "Weekly recap", NotificationManager.IMPORTANCE_LOW).apply {
            description = "A Sunday note on how much time talking saved you this week"
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)

        val open = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_STATS, System.currentTimeMillis())
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setContentIntent(open)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
            .onSuccess { Analytics.weeklyRecapSent(context) }
        return Result.success()
    }

    companion object {
        private const val TAG = "WeeklyRecapWorker"
        private const val WORK_NAME = "weekly_recap"
        private const val CHANNEL_ID = "weekly_recap_channel"
        private const val NOTIFICATION_ID = 2101
        private const val REQUEST_CODE = 2101

        /** Repeats every seven days, starting at the next Sunday evening. Safe to call on every start. */
        fun schedule(context: Context, now: ZonedDateTime = ZonedDateTime.now()) {
            if (!AppPreferences.isWeeklyRecapEnabled(context)) return
            val delay = Duration.between(now, WeeklyRecap.nextRun(now))
            val request = PeriodicWorkRequestBuilder<WeeklyRecapWorker>(7, TimeUnit.DAYS, 6, TimeUnit.HOURS)
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .build()
            // A recap is a nicety: if scheduling fails, the app still starts.
            runCatching { WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request) }
                .onFailure { Log.w(TAG, "Couldn't schedule the weekly recap", it) }
        }

        fun cancel(context: Context) {
            runCatching { WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME) }
                .onFailure { Log.w(TAG, "Couldn't cancel the weekly recap", it) }
        }
    }
}
