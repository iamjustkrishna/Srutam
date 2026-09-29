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
                space.iamjustkrishna.srutam.repository.InsightsRepository.from(context).reconcile()
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
