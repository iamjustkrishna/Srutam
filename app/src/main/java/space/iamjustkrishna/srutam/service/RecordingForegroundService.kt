package space.iamjustkrishna.srutam.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import space.iamjustkrishna.srutam.MainActivity
import space.iamjustkrishna.srutam.R
import space.iamjustkrishna.srutam.utils.AudioFileReader
import space.iamjustkrishna.srutam.utils.AudioStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class RecordingForegroundService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var accumulatedDurationMs = 0L
    private var lastResumeTimeMs = 0L
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var powerButtonPressTime = 0L
    private val powerButtonDoubleClickThreshold = 1000L
    private var notificationManager: NotificationManager? = null

    // WakeLock for screen handling
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null

    // Cached PendingIntents to avoid recreating on every notification update
    private var cachedActivityPendingIntent: PendingIntent? = null
    private var cachedPausePendingIntent: PendingIntent? = null
    private var cachedResumePendingIntent: PendingIntent? = null
    private var cachedStopPendingIntent: PendingIntent? = null

    private val powerButtonReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF && isRecording) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - powerButtonPressTime < powerButtonDoubleClickThreshold) {
                    Log.d(TAG, "Power button double-press detected while recording, stopping")
                    stopRecording()
                } else {
                    powerButtonPressTime = currentTime
                    Log.d(TAG, "Power button pressed once, waiting for second press")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        AudioFileReader.init(this)
        notificationManager = getSystemService(NotificationManager::class.java)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Srutam:RecordingWakeLock"
        )?.apply {
            setReferenceCounted(false)
        }

        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(powerButtonReceiver, filter)
        Log.d(TAG, "Power button receiver registered")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START_RECORDING -> startRecording()
            ACTION_PAUSE_RECORDING -> pauseRecording()
            ACTION_RESUME_RECORDING -> resumeRecording()
            ACTION_STOP_RECORDING -> stopRecording()
            ACTION_DELETE_RECORDING -> stopRecording(deleteAfterStop = true)
            else -> {
                Log.w(TAG, "Unknown action: ${intent?.action}")
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return
        }

        try {
            wakeLock?.acquire(60 * 60 * 1000L)
            Log.d(TAG, "WakeLock acquired for recording")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(100, 50))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100)
            }

            accumulatedDurationMs = 0L
            lastResumeTimeMs = System.currentTimeMillis()
            elapsedDurationMs = 0L
            isPaused = false
            isRecording = true

            val notification = createNotification(0L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
                Log.d(TAG, "Started foreground service with microphone type")
            } else {
                startForeground(NOTIFICATION_ID, notification)
                Log.d(TAG, "Started foreground service")
            }

            currentRecordingFile = createAudioFile()

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(currentRecordingFile?.absolutePath)

                try {
                    prepare()
                    start()
                    Log.d(TAG, "Recording started: ${currentRecordingFile?.absolutePath}")

                    startDurationUpdates()
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to start recording", e)
                    stopSelf()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            stopSelf()
        }
    }

    /**
     * Updates in-memory duration state without repeatedly calling notificationManager.notify().
     * Android's native Chronometer handles the on-screen notification timer ticking,
     * eliminating the 1-second button flicker completely.
     */
    private fun startDurationUpdates() {
        serviceScope.launch(Dispatchers.Main) {
            while (isRecording) {
                if (!isPaused) {
                    elapsedDurationMs = currentRecordedDurationMs()
                }
                delay(100)
            }
        }
    }

    private fun updateNotification(duration: Long) {
        val notification = createNotification(duration)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun pauseRecording() {
        if (!isRecording || isPaused) {
            return
        }

        try {
            mediaRecorder?.pause()
            accumulatedDurationMs = currentRecordedDurationMs()
            elapsedDurationMs = accumulatedDurationMs
            isPaused = true
            updateNotification(elapsedDurationMs)
            Log.d(TAG, "Recording paused")
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing recording", e)
        }
    }

    private fun resumeRecording() {
        if (!isRecording || !isPaused) {
            return
        }

        try {
            mediaRecorder?.resume()
            lastResumeTimeMs = System.currentTimeMillis()
            isPaused = false
            updateNotification(currentRecordedDurationMs())
            Log.d(TAG, "Recording resumed")
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming recording", e)
        }
    }

    private fun stopRecording(deleteAfterStop: Boolean = false) {
        if (!isRecording) {
            Log.w(TAG, "Not recording")
            stopSelf()
            return
        }

        try {
            val duration = currentRecordedDurationMs()
            elapsedDurationMs = duration
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            isPaused = false

            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                    Log.d(TAG, "WakeLock released")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing WakeLock", e)
            }

            val file = currentRecordingFile

            if (file != null && file.exists()) {
                if (deleteAfterStop) {
                    AudioStorage.deleteAudioFile(applicationContext, file.absolutePath)
                    Log.d(TAG, "Recording deleted: ${file.absolutePath}")
                } else {
                    Log.d(TAG, "Recording stopped: ${file.absolutePath}, duration: $duration ms")
                    saveRecordingToDatabase(file, duration)
                }
            }

            currentRecordingFile = null
            accumulatedDurationMs = 0L
            lastResumeTimeMs = 0L
            elapsedDurationMs = 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        } finally {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun saveRecordingToDatabase(file: File, duration: Long) {
        Log.d(TAG, "Recording saved to file: ${file.absolutePath}")
    }

    private fun createAudioFile(): File {
        val recordingsDir = AudioFileReader.getRecordingsDirectory().apply {
            if (!exists()) {
                mkdirs()
                Log.d(TAG, "Created recordings directory: $absolutePath")
            }
        }
        val fileName = "recording_${System.currentTimeMillis()}.m4a"
        val file = File(recordingsDir, fileName)
        Log.d(TAG, "Recording will be saved to: ${file.absolutePath}")
        return file
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.recording_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.recording_notification_channel_desc)
            setShowBadge(true)
            enableVibration(false)
            setSound(null, null)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun getActivityPendingIntent(): PendingIntent {
        if (cachedActivityPendingIntent == null) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            cachedActivityPendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }
        return cachedActivityPendingIntent!!
    }

    private fun getPausePendingIntent(): PendingIntent {
        if (cachedPausePendingIntent == null) {
            val intent = Intent(this, RecordingForegroundService::class.java).apply {
                action = ACTION_PAUSE_RECORDING
            }
            cachedPausePendingIntent = PendingIntent.getService(
                this,
                2,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }
        return cachedPausePendingIntent!!
    }

    private fun getResumePendingIntent(): PendingIntent {
        if (cachedResumePendingIntent == null) {
            val intent = Intent(this, RecordingForegroundService::class.java).apply {
                action = ACTION_RESUME_RECORDING
            }
            cachedResumePendingIntent = PendingIntent.getService(
                this,
                3,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }
        return cachedResumePendingIntent!!
    }

    private fun getStopPendingIntent(): PendingIntent {
        if (cachedStopPendingIntent == null) {
            val intent = Intent(this, RecordingForegroundService::class.java).apply {
                action = ACTION_STOP_RECORDING
            }
            cachedStopPendingIntent = PendingIntent.getService(
                this,
                1,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }
        return cachedStopPendingIntent!!
    }

    private fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }

    private fun createNotification(durationMs: Long): Notification {
        val pendingIntent = getActivityPendingIntent()
        val stopPendingIntent = getStopPendingIntent()

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOnlyAlertOnce(true)

        if (isPaused) {
            val durationText = formatDuration(durationMs)
            builder.setContentTitle("Recording paused ($durationText)")
                .setContentText("Resume or save your recording")
                .setUsesChronometer(false)
                .setShowWhen(false)
                .addAction(
                    android.R.drawable.ic_media_play,
                    "Resume",
                    getResumePendingIntent()
                )
        } else {
            builder.setContentTitle("Recording in progress")
                .setContentText("Tap to open Srutam")
                .setUsesChronometer(true)
                .setWhen(System.currentTimeMillis() - durationMs)
                .setShowWhen(true)
                .addAction(
                    android.R.drawable.ic_media_pause,
                    "Pause",
                    getPausePendingIntent()
                )
        }

        builder.addAction(
            android.R.drawable.ic_menu_save,
            "Save",
            stopPendingIntent
        )

        return builder.build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()

        try {
            unregisterReceiver(powerButtonReceiver)
            Log.d(TAG, "Power button receiver unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering power button receiver", e)
        }

        if (isRecording) {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing media recorder", e)
            }
        }
        mediaRecorder = null
        isRecording = false
        isPaused = false
        accumulatedDurationMs = 0L
        lastResumeTimeMs = 0L
        elapsedDurationMs = 0L
    }

    private fun currentRecordedDurationMs(): Long {
        return accumulatedDurationMs + if (isRecording && !isPaused) {
            System.currentTimeMillis() - lastResumeTimeMs
        } else {
            0L
        }
    }

    companion object {
        private const val TAG = "RecordingService"
        private const val CHANNEL_ID = "recording_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START_RECORDING = "space.iamjustkrishna.srutam.START_RECORDING"
        const val ACTION_PAUSE_RECORDING = "space.iamjustkrishna.srutam.PAUSE_RECORDING"
        const val ACTION_RESUME_RECORDING = "space.iamjustkrishna.srutam.RESUME_RECORDING"
        const val ACTION_STOP_RECORDING = "space.iamjustkrishna.srutam.STOP_RECORDING"
        const val ACTION_DELETE_RECORDING = "space.iamjustkrishna.srutam.DELETE_RECORDING"

        @Volatile
        var isRecording = false
            private set

        @Volatile
        var isPaused = false
            private set

        @Volatile
        var elapsedDurationMs = 0L
            private set
    }
}
