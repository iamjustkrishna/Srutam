package space.iamjustkrishna.srutam.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import space.iamjustkrishna.srutam.MainActivity
import space.iamjustkrishna.srutam.R

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val title = intent.getStringExtra(EXTRA_REMINDER_TITLE) ?: "Upcoming Event"
        val recordingId = intent.getLongExtra(EXTRA_RECORDING_ID, -1L)
        val person = intent.getStringExtra(EXTRA_PERSON)
        val location = intent.getStringExtra(EXTRA_LOCATION)
        val isHeadsUp = intent.getBooleanExtra(EXTRA_IS_HEADS_UP, false)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (recordingId > 0L) {
                putExtra("extra_open_recording_id", recordingId)
            }
        }
        val pendingOpen = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentBuilder = StringBuilder()
        if (isHeadsUp) {
            contentBuilder.append("Starts in 15 minutes. ")
        }
        if (!person.isNullOrBlank()) {
            contentBuilder.append("With: $person. ")
        }
        if (!location.isNullOrBlank()) {
            contentBuilder.append("Location: $location.")
        }
        val contentText = contentBuilder.toString().trim().ifEmpty {
            if (isHeadsUp) "Event scheduled in 15 minutes." else "Scheduled event starting now."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(pendingOpen)
            .setAutoCancel(true)
            .build()

        val notificationId = (reminderId.hashCode() + (if (isHeadsUp) 1 else 2)) and 0x7FFFFFFF
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Srutam Event & Meeting Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority notifications for scheduled meetings, tasks, and deadlines detected from your voice notes"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "srutam_reminders_channel"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
        const val EXTRA_RECORDING_ID = "extra_recording_id"
        const val EXTRA_PERSON = "extra_person"
        const val EXTRA_LOCATION = "extra_location"
        const val EXTRA_IS_HEADS_UP = "extra_is_heads_up"
    }
}
