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
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import space.iamjustkrishna.srutam.MainActivity
import space.iamjustkrishna.srutam.R
import space.iamjustkrishna.srutam.SrutamApplication
import space.iamjustkrishna.srutam.ai.LiveTranscription
import space.iamjustkrishna.srutam.ai.LocalTranscriber
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import space.iamjustkrishna.srutam.repository.RecordingRepository
import space.iamjustkrishna.srutam.utils.AppPreferences
import space.iamjustkrishna.srutam.utils.AudioFileReader
import space.iamjustkrishna.srutam.utils.AudioStorage
import space.iamjustkrishna.srutam.utils.RecordingNameFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class RecordingForegroundService : Service() {

    private var mediaRecorder: MediaRecorder? = null

    // Records through the microphone directly so speech can be transcribed while the user talks;
    // MediaRecorder stays as the fallback if that cannot be opened.
    private var pipeline: AacRecordingPipeline? = null
    private var liveSession: LiveSession? = null
    private val pendingFinalizations = AtomicInteger(0)

    /** The speech model and live transcription of the note being recorded. */
    private class LiveSession(val transcriber: LocalTranscriber, val live: LiveTranscription)
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
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
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
            ACTION_PAUSE_RECORDING -> {
                if (!isRecording) {
                    cleanUpStaleNotification()
                } else {
                    pauseRecording()
                }
            }
            ACTION_RESUME_RECORDING -> {
                if (!isRecording) {
                    cleanUpStaleNotification()
                } else {
                    resumeRecording()
                }
            }
            ACTION_STOP_RECORDING -> {
                if (!isRecording) {
                    cleanUpStaleNotification()
                } else {
                    val deferAutoAi = intent?.getBooleanExtra(EXTRA_DEFER_AUTO_AI, false) ?: false
                    stopRecording(deleteAfterStop = false, deferAutoAi = deferAutoAi)
                }
            }
            ACTION_DELETE_RECORDING -> {
                if (!isRecording) {
                    cleanUpStaleNotification()
                } else {
                    stopRecording(deleteAfterStop = true)
                }
            }
            else -> {
                Log.w(TAG, "Unknown action: ${intent?.action}")
                cleanUpStaleNotification()
            }
        }
        return START_NOT_STICKY
    }

    private fun cleanUpStaleNotification() {
        Log.d(TAG, "Cleaning up stale/orphaned recording notification")
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        notificationManager?.cancel(NOTIFICATION_ID)
        RecordingCoordinator.notifyRecordingEnded()
        stopSelf()
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

            val file = currentRecordingFile!!
            if (!startPipelineRecording(file)) {
                startMediaRecorderRecording(file)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            cleanUpStaleNotification()
        }
    }

    /** Records through the microphone directly, transcribing live. Returns false if that is unavailable. */
    private fun startPipelineRecording(file: File): Boolean {
        var session: LiveSession? = null
        try {
            val transcriber = LocalTranscriber(applicationContext)
            session = LiveSession(transcriber, LiveTranscription(transcriber))
            val newPipeline = AacRecordingPipeline(file, pcmSourceFactory(), session?.live)
            newPipeline.start()
            pipeline = newPipeline
            liveSession = session
            Log.d(TAG, "Recording started with live transcription: ${file.absolutePath}")

            RecordingCoordinator.notifyRecordingStarted(lastResumeTimeMs, file.absolutePath)
            startDurationUpdates()
            return true
        } catch (e: Throwable) {
            Log.w(TAG, "Direct recording unavailable, falling back to MediaRecorder", e)
            session?.live?.cancel()
            session?.transcriber?.release()
            pipeline = null
            liveSession = null
            file.delete()
            return false
        }
    }

    private fun startMediaRecorderRecording(file: File) {
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
            setOutputFile(file.absolutePath)

            try {
                prepare()
                start()
                Log.d(TAG, "Recording started: ${file.absolutePath}")

                RecordingCoordinator.notifyRecordingStarted(
                    lastResumeTimeMs,
                    file.absolutePath
                )
                startDurationUpdates()
            } catch (e: IOException) {
                Log.e(TAG, "Failed to start recording", e)
                cleanUpStaleNotification()
            }
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
            val activePipeline = pipeline
            if (activePipeline != null) activePipeline.pause() else mediaRecorder?.pause()
            accumulatedDurationMs = currentRecordedDurationMs()
            elapsedDurationMs = accumulatedDurationMs
            isPaused = true
            RecordingCoordinator.notifyRecordingPaused(
                elapsedDurationMs,
                currentRecordingFile?.absolutePath ?: ""
            )
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
            val activePipeline = pipeline
            if (activePipeline != null) activePipeline.resume() else mediaRecorder?.resume()
            lastResumeTimeMs = System.currentTimeMillis()
            isPaused = false
            RecordingCoordinator.notifyRecordingResumed(
                lastResumeTimeMs,
                currentRecordingFile?.absolutePath ?: ""
            )
            updateNotification(currentRecordedDurationMs())
            Log.d(TAG, "Recording resumed")
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming recording", e)
        }
    }

    private fun stopRecording(deleteAfterStop: Boolean = false, deferAutoAi: Boolean = false) {
        if (!isRecording) {
            Log.w(TAG, "Not recording")
            cleanUpStaleNotification()
            return
        }

        var orphanedLive: LiveSession? = null
        try {
            var duration = currentRecordedDurationMs()
            val finishedPipeline = pipeline
            val finishedLive = liveSession
            orphanedLive = finishedLive
            pipeline = null
            liveSession = null
            if (finishedPipeline != null) {
                finishedPipeline.stop()?.let { duration = it }
            } else {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
            }
            elapsedDurationMs = duration
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
            var liveHandedOff = false

            if (file != null && file.exists()) {
                if (deleteAfterStop) {
                    AudioStorage.deleteAudioFile(applicationContext, file.absolutePath)
                    Log.d(TAG, "Recording deleted: ${file.absolutePath}")
                } else {
                    Log.d(TAG, "Recording stopped: ${file.absolutePath}, duration: $duration ms")
                    saveRecordingToDatabase(file, duration)
                    RecordingCoordinator.clearActiveFile()
                    _recordingSavedEvents.tryEmit(file)
                    val runAutoAi = !deferAutoAi && AppPreferences.isAutoAiEnabled(applicationContext)
                    if (finishedLive != null) {
                        // The transcript is nearly done; store it first so auto-AI can skip transcribing.
                        finalizeLiveTranscription(finishedLive, file, duration, runAutoAi)
                        liveHandedOff = true
                    } else if (runAutoAi) {
                        triggerAutoAiForFile(file, duration)
                    }
                }
            }
            if (!liveHandedOff && finishedLive != null) {
                finishedLive.live.cancel()
                finishedLive.transcriber.release()
            }
            orphanedLive = null

            currentRecordingFile = null
            accumulatedDurationMs = 0L
            lastResumeTimeMs = 0L
            elapsedDurationMs = 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            orphanedLive?.let {
                it.live.cancel()
                it.transcriber.release()
            }
        } finally {
            if (pendingFinalizations.get() > 0 && !isRecording) {
                // Stay a foreground service until the transcript is stored, or the system may freeze us mid-way.
                notificationManager?.notify(NOTIFICATION_ID, createFinalizingNotification())
                RecordingCoordinator.notifyRecordingEnded()
            } else {
                stopForegroundAndService()
                RecordingCoordinator.notifyRecordingEnded()
            }
        }
    }

    private fun stopForegroundAndService() {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        notificationManager?.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    /**
     * Waits for the live transcript to finish (usually about a second: only the last phrase is left),
     * stores it on the note so the AI step can skip transcribing, then starts auto-AI if it is on.
     * If live transcription gave up or failed, nothing is stored and the saved file is transcribed
     * later exactly as before.
     */
    private fun finalizeLiveTranscription(session: LiveSession, file: File, duration: Long, runAutoAi: Boolean) {
        pendingFinalizations.incrementAndGet()
        serviceScope.launch(Dispatchers.Default) {
            try {
                val text = session.live.finish(LIVE_FINISH_TIMEOUT_MS)?.text?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    storeLiveTranscript(file, duration, text)
                    Log.d(TAG, "Stored live transcript for ${file.name} (${text.length} chars)")
                } else {
                    Log.d(TAG, "No live transcript for ${file.name}; it will be transcribed from the file when needed")
                }
            } finally {
                session.live.cancel()
                session.transcriber.release()
            }
            if (runAutoAi) withContext(Dispatchers.IO) { enqueueAutoAi(file, duration) }
            if (pendingFinalizations.decrementAndGet() == 0) {
                withContext(Dispatchers.Main) {
                    if (!isRecording) stopForegroundAndService()
                }
            }
        }
    }

    private suspend fun storeLiveTranscript(file: File, duration: Long, text: String) {
        withContext(Dispatchers.IO) {
            // The in-app Save dialog may already have renamed or discarded the file; then the transcript
            // is dropped and the AI step simply transcribes the saved file, as it always did.
            if (!file.exists()) {
                Log.d(TAG, "${file.name} was renamed or discarded first; not storing its live transcript")
                return@withContext
            }
            val repository = RecordingRepository(applicationContext, SrutamApplication.getInstance().database.recordingDao())
            val existing = repository.getRecordingByPath(file.absolutePath)
            if (existing == null) {
                val id = repository.insertRecording(
                    Recording(
                        audioFilePath = file.absolutePath,
                        duration = duration,
                        name = RecordingNameFormatter.displayName(fileName = file.name, timestamp = file.lastModified()),
                        transcript = text
                    )
                )
                // Renamed or discarded while we were inserting: do not leave a note for a file that is gone.
                if (!file.exists()) repository.deleteRecordingById(id)
            } else {
                repository.setTranscriptIfBlank(existing.id, text)
            }
        }
    }

    private fun createFinalizingNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(getActivityPendingIntent())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentTitle("Finishing transcript")
            .setContentText("Tap to open Srutam")
            .build()

    private fun saveRecordingToDatabase(file: File, duration: Long) {
        Log.d(TAG, "Recording saved to file: ${file.absolutePath}")
    }

    private fun triggerAutoAiForFile(file: File, duration: Long) {
        serviceScope.launch(Dispatchers.IO) { enqueueAutoAi(file, duration) }
    }

    private suspend fun enqueueAutoAi(file: File, duration: Long) {
        try {
            val database = SrutamApplication.getInstance().database
            val repository = RecordingRepository(applicationContext, database.recordingDao())
            var recording = repository.getRecordingByPath(file.absolutePath)
            if (recording == null) {
                val newRecording = Recording(
                    audioFilePath = file.absolutePath,
                    duration = duration,
                    name = RecordingNameFormatter.displayName(
                        fileName = file.name,
                        timestamp = file.lastModified()
                    ),
                    isProcessing = true,
                    aiStatus = RecordingAiStatus.TRANSCRIBING
                )
                val id = repository.insertRecording(newRecording)
                recording = newRecording.copy(id = id)
            } else {
                repository.updateRecording(
                    recording.copy(
                        isProcessing = true,
                        aiStatus = if (recording.transcript.isNullOrBlank()) {
                            RecordingAiStatus.TRANSCRIBING
                        } else {
                            RecordingAiStatus.SUMMARY_PROCESSING
                        },
                        processingError = null
                    )
                )
            }
            AiProcessingWorker.enqueueProcessing(applicationContext, listOf(recording.id))
            Log.d(TAG, "Auto-AI enqueued for recording ID: ${recording.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering auto-AI in RecordingForegroundService", e)
        }
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
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.recording_notification_channel_desc)
            setShowBadge(true)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.deleteNotificationChannel(OLD_CHANNEL_ID)
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
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
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
                val activePipeline = pipeline
                if (activePipeline != null) {
                    activePipeline.stop()
                } else {
                    mediaRecorder?.apply {
                        stop()
                        release()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing recorder", e)
            }
        }
        pipeline = null
        liveSession?.let {
            it.live.cancel()
            it.transcriber.release()
        }
        liveSession = null
        mediaRecorder = null
        isRecording = false
        isPaused = false
        accumulatedDurationMs = 0L
        lastResumeTimeMs = 0L
        elapsedDurationMs = 0L

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        notificationManager?.cancel(NOTIFICATION_ID)
        RecordingCoordinator.notifyRecordingEnded()
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
        // v2: a channel's importance cannot change once created, and the old one was low (hidden on many lock screens).
        private const val CHANNEL_ID = "recording_channel_v2"
        private const val OLD_CHANNEL_ID = "recording_channel"
        private const val LIVE_FINISH_TIMEOUT_MS = 30_000L

        /** Where the recording pipeline gets its audio. Tests replace it with a recorded clip. */
        @Volatile
        internal var pcmSourceFactory: () -> PcmSource = { AudioRecordPcmSource() }
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_RECORDING = "space.iamjustkrishna.srutam.START_RECORDING"
        const val ACTION_PAUSE_RECORDING = "space.iamjustkrishna.srutam.PAUSE_RECORDING"
        const val ACTION_RESUME_RECORDING = "space.iamjustkrishna.srutam.RESUME_RECORDING"
        const val ACTION_STOP_RECORDING = "space.iamjustkrishna.srutam.STOP_RECORDING"
        const val ACTION_DELETE_RECORDING = "space.iamjustkrishna.srutam.DELETE_RECORDING"
        const val EXTRA_DEFER_AUTO_AI = "space.iamjustkrishna.srutam.DEFER_AUTO_AI"

        private val _recordingSavedEvents = MutableSharedFlow<File>(extraBufferCapacity = 1)
        val recordingSavedEvents: SharedFlow<File> = _recordingSavedEvents.asSharedFlow()

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
