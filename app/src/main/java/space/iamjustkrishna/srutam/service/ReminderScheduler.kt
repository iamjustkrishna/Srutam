package space.iamjustkrishna.srutam.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import space.iamjustkrishna.srutam.data.ReminderEntity

object ReminderScheduler {

    private const val TAG = "ReminderScheduler"
    private const val FIFTEEN_MINUTES_MS = 15 * 60 * 1000L

    fun scheduleReminder(context: Context, reminder: ReminderEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()

        // 1. Schedule 15-minute heads-up alarm if still in future
        val headsUpTime = reminder.eventTimeMs - FIFTEEN_MINUTES_MS
        if (headsUpTime > now) {
            val headsUpPending = createPendingIntent(context, reminder, isHeadsUp = true)
            setAlarm(alarmManager, headsUpTime, headsUpPending)
            Log.d(TAG, "Scheduled 15-min heads-up alarm for reminder: ${reminder.title} at $headsUpTime")
        }

        // 2. Schedule exact event time alarm
        if (reminder.eventTimeMs > now) {
            val exactPending = createPendingIntent(context, reminder, isHeadsUp = false)
            setAlarm(alarmManager, reminder.eventTimeMs, exactPending)
            Log.d(TAG, "Scheduled event time alarm for reminder: ${reminder.title} at ${reminder.eventTimeMs}")
        }
    }

    fun cancelReminder(context: Context, reminder: ReminderEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val headsUpPending = createPendingIntent(context, reminder, isHeadsUp = true)
        alarmManager.cancel(headsUpPending)
        headsUpPending.cancel()

        val exactPending = createPendingIntent(context, reminder, isHeadsUp = false)
        alarmManager.cancel(exactPending)
        exactPending.cancel()

        Log.d(TAG, "Cancelled alarms for reminder: ${reminder.id}")
    }

    private fun setAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission not granted, falling back to inexact alarm", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed scheduling alarm for $triggerAtMillis", e)
        }
    }

    private fun createPendingIntent(context: Context, reminder: ReminderEntity, isHeadsUp: Boolean): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_TITLE, reminder.title)
            putExtra(ReminderAlarmReceiver.EXTRA_RECORDING_ID, reminder.recordingId)
            putExtra(ReminderAlarmReceiver.EXTRA_PERSON, reminder.person)
            putExtra(ReminderAlarmReceiver.EXTRA_LOCATION, reminder.location)
            putExtra(ReminderAlarmReceiver.EXTRA_IS_HEADS_UP, isHeadsUp)
        }
        val requestCode = (reminder.id.hashCode() + (if (isHeadsUp) 100000 else 200000)) and 0x7FFFFFFF
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
