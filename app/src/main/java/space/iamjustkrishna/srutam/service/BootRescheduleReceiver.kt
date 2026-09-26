package space.iamjustkrishna.srutam.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import space.iamjustkrishna.srutam.data.AppDatabase

/**
 * Re-schedules all future ACTIVE reminders after a device reboot.
 * AlarmManager alarms are lost on reboot, so this receiver restores them.
 */
class BootRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "Boot completed — re-scheduling active reminders")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val reminderDao = db.reminderDao()
                val now = System.currentTimeMillis()

                val futureReminders = reminderDao.getFutureActiveReminders(now)
                futureReminders.forEach { reminder ->
                    ReminderScheduler.scheduleReminder(context, reminder)
                }
                Log.d(TAG, "Re-scheduled ${futureReminders.size} reminders after boot")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to re-schedule reminders on boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootRescheduleReceiver"
    }
}
