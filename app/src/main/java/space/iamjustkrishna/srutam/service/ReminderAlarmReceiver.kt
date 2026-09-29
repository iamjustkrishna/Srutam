package space.iamjustkrishna.srutam.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import space.iamjustkrishna.srutam.MainActivity
import space.iamjustkrishna.srutam.R
import space.iamjustkrishna.srutam.data.AppDatabase
import space.iamjustkrishna.srutam.data.ReminderEntity

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        // Legacy payloads never establish permission to notify.
        if (!intent.hasExtra(EXTRA_REVISION) || !intent.hasExtra(EXTRA_TRIGGER)) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                db.withTransaction {
                    val reminder = db.reminderDao().getReminderById(id) ?: return@withTransaction
                    val headsUp = intent.getBooleanExtra(EXTRA_IS_HEADS_UP, false)
                    if (ReminderPolicy.canDeliver(reminder, intent.getIntExtra(EXTRA_REVISION, -1),
                            intent.getLongExtra(EXTRA_TRIGGER, -1), headsUp, System.currentTimeMillis(),
                            (space.iamjustkrishna.srutam.data.SourceIds.isChat(reminder.recordingId) ||
                                db.recordingDao().getRecordingById(reminder.recordingId) != null)) &&
                        NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                        notify(context, reminder, headsUp)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("ReminderAlarmReceiver", "Reminder delivery failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    private fun notify(context: Context, reminder: ReminderEntity, headsUp: Boolean) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Confirmed reminders", NotificationManager.IMPORTANCE_HIGH))
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("extra_open_recording_id", reminder.recordingId)
        }
        val pending = PendingIntent.getActivity(context, reminder.id.hashCode(), open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val text = if (headsUp) "Starts in 15 minutes." else "Your confirmed reminder is due."
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher).setContentTitle(reminder.title)
            .setContentText(text).setContentIntent(pending).setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH).build()
        manager.notify((reminder.id.hashCode() + if (headsUp) 1 else 2) and 0x7FFFFFFF, notification)
    }

    companion object {
        const val CHANNEL_ID = "srutam_reminders_channel"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_IS_HEADS_UP = "extra_is_heads_up"
        const val EXTRA_REVISION = "extra_schedule_revision"
        const val EXTRA_TRIGGER = "extra_trigger_at"
    }
}
