package space.iamjustkrishna.srutam.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import space.iamjustkrishna.srutam.MainActivity
import space.iamjustkrishna.srutam.R

/**
 * Persistent notification service that shows a "Start Recording" button
 * visible even when the app is closed. Tapping the button starts recording.
 */
class PersistentRecordingNotificationService : Service() {

    companion object {
        private const val TAG = "PersistentRecordingNotif"
        private const val NOTIFICATION_ID = 999
        private const val CHANNEL_ID = "srutam_recording_channel"
        const val ACTION_START_RECORDING = "space.iamjustkrishna.srutam.action.START_RECORDING"
        const val ACTION_STOP_NOTIFICATION = "space.iamjustkrishna.srutam.action.STOP_NOTIFICATION"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PersistentRecordingNotificationService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}")

        when (intent?.action) {
            ACTION_START_RECORDING -> {
                Log.d(TAG, "Starting foreground service with persistent notification")
                // Start as foreground service to keep notification persistent
                startForeground(NOTIFICATION_ID, createNotification())
            }
            ACTION_STOP_NOTIFICATION -> {
                Log.d(TAG, "Stopping persistent notification")
                space.iamjustkrishna.srutam.utils.AppPreferences.setPersistentNotificationEnabled(this, false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                // Default: show persistent notification
                startForeground(NOTIFICATION_ID, createNotification())
            }
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Srutam Recording",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Srutam quick recording notification"
                setShowBadge(true)
                enableVibration(false)
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        // Intent to start recording
        val recordingIntent = Intent(this, RecordingForegroundService::class.java).apply {
            action = RecordingForegroundService.ACTION_START_RECORDING
        }
        val recordingPendingIntent = PendingIntent.getService(
            this,
            1,
            recordingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to open app
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val appPendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to close notification
        val closeIntent = Intent(this, PersistentRecordingNotificationService::class.java).apply {
            action = ACTION_STOP_NOTIFICATION
        }
        val closePendingIntent = PendingIntent.getService(
            this,
            2,
            closeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Srutam Recording")
            .setContentText("Tap to start recording")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(appPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_media_play,
                "Start Recording",
                recordingPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dismiss",
                closePendingIntent
            )
            .setAutoCancel(false)
            .build()
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        super.onDestroy()
        space.iamjustkrishna.srutam.utils.AppPreferences.setPersistentNotificationEnabled(this, false)
        Log.d(TAG, "PersistentRecordingNotificationService destroyed")
    }
}
