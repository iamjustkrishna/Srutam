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
import android.util.Log
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import space.iamjustkrishna.srutam.MainActivity
import space.iamjustkrishna.srutam.R
import space.iamjustkrishna.srutam.utils.AudioStorage
import space.iamjustkrishna.srutam.utils.AudioFileReader
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
    private val powerButtonDoubleClickThreshold = 1000L // 1 second
    private var notificationManager: NotificationManager? = null
    
    // WakeLock for screen handling
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null

    private val powerButtonReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF && isRecording) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - powerButtonPressTime < powerButtonDoubleClickThreshold) {
                    // Double press detected - stop recording
                    Log.d(TAG, "Power button double-press detected while recording - stopping")
                    stopRecording()
                } else {
                    powerButtonPressTime = currentTime
                    Log.d(TAG, "Power button pressed once - waiting for second press")
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
        
        // Initialize WakeLock
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Srutam:RecordingWakeLock"
        )?.apply {
            setReferenceCounted(false)
        }

        // Register power button receiver
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
            // Acquire WakeLock to keep device awake while recording
            wakeLock?.acquire(60 * 60 * 1000L) // 1 hour max
            Log.d(TAG, "WakeLock acquired for recording")
            
            // Provide haptic feedback
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(100, 50))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100)
            }
            
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
            accumulatedDurationMs = 0L
            lastResumeTimeMs = System.currentTimeMillis()
            elapsedDurationMs = 0L
            isPaused = false

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
                    isRecording = true
                    isPaused = false
                    Log.d(TAG, "Recording started: ${currentRecordingFile?.absolutePath}")

                    // Start duration update loop
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

    private fun startDurationUpdates() {
        serviceScope.launch(Dispatchers.Main) {
            while (isRecording) {
                val duration = currentRecordedDurationMs()
                elapsedDurationMs = duration
                updateNotification(duration)
                delay(1000) // Update every second
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
            
            // Release WakeLock
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
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.recording_notification_channel_desc)
            setShowBadge(true)
            enableVibration(false)
            setSound(null, null)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(durationMs: Long): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val pauseOrResumeIntent = Intent(this, RecordingForegroundService::class.java).apply {
            action = if (isPaused) ACTION_RESUME_RECORDING else ACTION_PAUSE_RECORDING
        }
        val pauseOrResumePendingIntent = PendingIntent.getService(
            this,
            2,
            pauseOrResumeIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, RecordingForegroundService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Format duration
        val seconds = (durationMs / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        val durationText = String.format("%d:%02d", minutes, remainingSeconds)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (isPaused) "Recording paused - $durationText" else "Recording - $durationText")
            .setContentText(if (isPaused) "Resume or save the current recording" else "Recording in progress")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (isPaused) "Resume" else "Pause",
                pauseOrResumePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_save,
                "Save",
                stopPendingIntent
            )
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()

        // Unregister power button receiver
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
