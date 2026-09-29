package space.iamjustkrishna.srutam.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.room.withTransaction
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.iamjustkrishna.srutam.MainActivity
import space.iamjustkrishna.srutam.R
import space.iamjustkrishna.srutam.data.AppDatabase
import space.iamjustkrishna.srutam.data.InsightStatus
import space.iamjustkrishna.srutam.data.ReminderEntity
import space.iamjustkrishna.srutam.data.ReminderStatus
import space.iamjustkrishna.srutam.data.SourceIds

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val id = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "srutam:reminder_alert"
        )?.apply {
            setReferenceCounted(false)
            acquire(15_000L) // 15 seconds max safety timeout for background DB + notification work
        }

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                when (action) {
                    ACTION_MARK_DONE -> {
                        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                        if (notificationId != -1) {
                            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            manager.cancel(notificationId)
                        }
                        db.withTransaction {
                            val reminder = db.reminderDao().getReminderById(id) ?: return@withTransaction
                            db.reminderDao().update(
                                reminder.copy(
                                    status = ReminderStatus.COMPLETED,
                                    notificationEnabled = false,
                                    confirmedAt = System.currentTimeMillis(),
                                    scheduleRevision = reminder.scheduleRevision + 1
                                )
                            )
                            if (reminder.linkedTaskId != null) {
                                val task = db.insightDao().getById(reminder.linkedTaskId)
                                if (task != null) {
                                    db.insightDao().updateInsight(
                                        task.copy(
                                            status = InsightStatus.COMPLETED,
                                            completedAt = System.currentTimeMillis()
                                        )
                                    )
                                }
                            }
                        }
                    }
                    ACTION_SNOOZE -> {
                        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                        if (notificationId != -1) {
                            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            manager.cancel(notificationId)
                        }
                        val reminder = db.reminderDao().getReminderById(id)
                        if (reminder != null) {
                            // Cancel any prior pending alarms before scheduling the snoozed alarm
                            ReminderScheduler.cancelReminder(context, reminder)
                            val snoozedTime = System.currentTimeMillis() + SNOOZE_DURATION_MS
                            val updated = reminder.copy(
                                eventTimeMs = snoozedTime,
                                timePrecision = "EXACT",
                                needsReview = false,
                                advanceNotification = false,
                                status = ReminderStatus.ACTIVE,
                                notificationEnabled = true,
                                confirmedAt = System.currentTimeMillis(),
                                scheduleRevision = reminder.scheduleRevision + 1,
                                scheduleError = null
                            )
                            db.reminderDao().update(updated)
                            val err = ReminderScheduler.scheduleReminder(context, updated)
                            if (err != null) {
                                db.reminderDao().update(updated.copy(scheduleError = err))
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Snoozed for 10 minutes", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    else -> {
                        // Standard alarm delivery trigger
                        if (!intent.hasExtra(EXTRA_REVISION) || !intent.hasExtra(EXTRA_TRIGGER)) return@launch
                        db.withTransaction {
                            val reminder = db.reminderDao().getReminderById(id) ?: return@withTransaction
                            val headsUp = intent.getBooleanExtra(EXTRA_IS_HEADS_UP, false)
                            val sourceExists = SourceIds.isChat(reminder.recordingId) ||
                                db.recordingDao().getRecordingById(reminder.recordingId) != null

                            if (ReminderPolicy.canDeliver(
                                    reminder = reminder,
                                    revision = intent.getIntExtra(EXTRA_REVISION, -1),
                                    trigger = intent.getLongExtra(EXTRA_TRIGGER, -1),
                                    headsUp = headsUp,
                                    now = System.currentTimeMillis(),
                                    sourceExists = sourceExists
                                ) && NotificationManagerCompat.from(context).areNotificationsEnabled()
                            ) {
                                notify(context, reminder, headsUp)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("ReminderAlarmReceiver", "Reminder processing failed", e)
            } finally {
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                } catch (e: Exception) {
                    Log.w("ReminderAlarmReceiver", "Error releasing WakeLock", e)
                }
                pending.finish()
            }
        }
    }

    private fun notify(context: Context, reminder: ReminderEntity, headsUp: Boolean) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Clean up legacy channel if present
        runCatching { manager.deleteNotificationChannel(LEGACY_CHANNEL_ID) }

        // Setup sound and attributes pointing to Srutam signature chime
        val soundUri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.srutam_chime}")
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Srutam Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts for confirmed tasks and scheduled reminders in Srutam"
            setSound(soundUri, audioAttributes)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 150, 300)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)

        val notificationId = ((reminder.id.hashCode() + if (headsUp) 1 else 2) and 0x7FFFFFFF)

        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_RECORDING_ID, reminder.recordingId)
            putExtra("extra_open_reminder_id", reminder.id)
        }
        val pendingOpen = PendingIntent.getActivity(
            context,
            reminder.id.hashCode(),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Mark Done
        val doneIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            data = Uri.parse("srutam://reminder/${reminder.id}/done")
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val pendingDone = PendingIntent.getBroadcast(
            context,
            (reminder.id.hashCode() + 10) and 0x7FFFFFFF,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze 10m
        val snoozeIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE
            data = Uri.parse("srutam://reminder/${reminder.id}/snooze")
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val pendingSnooze = PendingIntent.getBroadcast(
            context,
            (reminder.id.hashCode() + 20) and 0x7FFFFFFF,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = if (headsUp) "Starts in 15 minutes." else "Your reminder is due."
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(reminder.title)
            .setContentText(text)
            .setContentIntent(pendingOpen)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, "Mark Done", pendingDone)
            .addAction(0, "Snooze 10m", pendingSnooze)
            .build()

        manager.notify(notificationId, notification)
    }

    companion object {
        const val CHANNEL_ID = "srutam_reminders_v2"
        private const val LEGACY_CHANNEL_ID = "srutam_reminders_channel"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_IS_HEADS_UP = "extra_is_heads_up"
        const val EXTRA_REVISION = "extra_schedule_revision"
        const val EXTRA_TRIGGER = "extra_trigger_at"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        const val ACTION_MARK_DONE = "space.iamjustkrishna.srutam.ACTION_REMINDER_DONE"
        const val ACTION_SNOOZE = "space.iamjustkrishna.srutam.ACTION_REMINDER_SNOOZE"
        const val SNOOZE_DURATION_MS = 10 * 60 * 1000L
    }
}
