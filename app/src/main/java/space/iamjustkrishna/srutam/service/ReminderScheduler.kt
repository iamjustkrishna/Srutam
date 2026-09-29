package space.iamjustkrishna.srutam.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import space.iamjustkrishna.srutam.data.ReminderEntity

class AndroidReminderAlarms(private val context: Context) : ReminderAlarmService {
    override fun schedule(reminder: ReminderEntity) = ReminderScheduler.scheduleReminder(context, reminder)
    override fun cancel(reminder: ReminderEntity) = ReminderScheduler.cancelReminder(context, reminder)
}

object ReminderScheduler {
    fun scheduleReminder(context: Context, reminder: ReminderEntity): String? {
        val now = System.currentTimeMillis()
        if (!ReminderPolicy.canSchedule(reminder, now)) return null
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return "Notifications are disabled. Enable them in system settings, then retry."
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return "Could not access reminder scheduling. Retry."
        return try {
            val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
            for (headsUp in listOf(false, true)) {
                if (headsUp && !reminder.advanceNotification) continue
                val trigger = reminder.eventTimeMs!! - if (headsUp) ReminderPolicy.ADVANCE_MS else 0L
                if (trigger <= now) continue
                val pending = pendingIntent(context, reminder, headsUp, trigger) ?: error("Could not create alarm")
                if (exact) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
                else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
            }
            if (exact) null else "Scheduled; timing may vary because exact alarms are unavailable."
        } catch (e: Exception) {
            cancelReminder(context, reminder)
            "Could not schedule notification. Review and retry."
        }
    }

    fun cancelReminder(context: Context, reminder: ReminderEntity) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (headsUp in listOf(false, true)) {
            // Old releases used the same component/request code without a data URI.
            for (legacy in listOf(false, true)) {
                val pending = pendingIntent(context, reminder, headsUp, 0, legacy, noCreate = true) ?: continue
                manager.cancel(pending)
                pending.cancel()
            }
        }
    }

    private fun pendingIntent(
        context: Context, reminder: ReminderEntity, headsUp: Boolean, trigger: Long,
        legacy: Boolean = false, noCreate: Boolean = false
    ): PendingIntent? {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            if (!legacy) data = Uri.Builder().scheme("srutam").authority("reminder")
                .appendPath(reminder.id).appendPath(if (headsUp) "advance" else "event").build()
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderAlarmReceiver.EXTRA_IS_HEADS_UP, headsUp)
            putExtra(ReminderAlarmReceiver.EXTRA_REVISION, reminder.scheduleRevision)
            putExtra(ReminderAlarmReceiver.EXTRA_TRIGGER, trigger)
        }
        val code = (reminder.id.hashCode() + if (headsUp) 100000 else 200000) and 0x7FFFFFFF
        return PendingIntent.getBroadcast(context, code, intent,
            (if (noCreate) PendingIntent.FLAG_NO_CREATE else PendingIntent.FLAG_UPDATE_CURRENT) or PendingIntent.FLAG_IMMUTABLE)
    }
}
