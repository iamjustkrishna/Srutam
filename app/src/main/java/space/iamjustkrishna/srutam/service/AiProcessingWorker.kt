package space.iamjustkrishna.srutam.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.gson.Gson
import space.iamjustkrishna.srutam.MainActivity
import space.iamjustkrishna.srutam.R
import space.iamjustkrishna.srutam.ai.AIProcessor
import space.iamjustkrishna.srutam.data.AppDatabase
import space.iamjustkrishna.srutam.data.InsightDao
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.InsightKind
import space.iamjustkrishna.srutam.data.InsightStatus
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.RecordingAiStatus
import space.iamjustkrishna.srutam.utils.NetworkUtils
import java.io.File

class AiProcessingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val gson = Gson()

    override suspend fun doWork(): Result {
        val recordingIds = inputData.getLongArray(KEY_RECORDING_IDS)?.toList() ?: emptyList()
        if (recordingIds.isEmpty()) {
            Log.d(TAG, "No recording IDs supplied to AiProcessingWorker")
            return Result.success()
        }

        createNotificationChannels()

        val initialNotification = buildProgressNotification(
            title = "Processing AI Insights",
            content = "Preparing voice notes...",
            progress = 0,
            max = recordingIds.size * 2
        )

        try {
            setForeground(createForegroundInfo(initialNotification))
        } catch (e: Exception) {
            Log.w(TAG, "Could not set foreground info", e)
        }

        val database = AppDatabase.getDatabase(applicationContext)
        val recordingDao = database.recordingDao()
        val insightDao = database.insightDao()
        val aiProcessor = AIProcessor(applicationContext)

        val totalCount = recordingIds.size
        var completedCount = 0
        var lastCompletedRecording: Recording? = null
        var offlinePendingCount = 0

        for ((index, recordingId) in recordingIds.withIndex()) {
            var recording = recordingDao.getRecordingById(recordingId)
            if (recording == null) {
                Log.w(TAG, "Recording ID $recordingId not found in database")
                continue
            }

            val noteName = recording.name.ifBlank { "Voice Note" }
            val baseProgress = index * 2

            // Stage 1: Transcription (Local Sherpa ONNX, works offline)
            var transcript = recording.transcript
            if (transcript.isNullOrBlank()) {
                val transcribingTitle = if (totalCount > 1) {
                    "Analyzing note ${index + 1} of $totalCount"
                } else {
                    "Transcribing: $noteName"
                }
                updateForegroundProgress(
                    title = transcribingTitle,
                    content = "Transcribing audio locally...",
                    progress = baseProgress + 1,
                    max = totalCount * 2
                )

                val audioFile = File(recording.audioFilePath)
                if (!audioFile.exists()) {
                    Log.e(TAG, "Audio file not found: ${recording.audioFilePath}")
                    recordingDao.updateRecording(
                        recording.copy(
                            isProcessing = false,
                            aiStatus = RecordingAiStatus.ERROR,
                            processingError = "Audio file missing"
                        )
                    )
                    continue
                }

                try {
                    recordingDao.updateRecording(
                        recording.copy(
                            isProcessing = true,
                            aiStatus = RecordingAiStatus.TRANSCRIBING,
                            processingError = null
                        )
                    )
                    transcript = aiProcessor.transcribeAudio(audioFile)
                    recording = recording.copy(transcript = transcript)
                    recordingDao.updateRecording(recording)
                    Log.d(TAG, "Local transcription complete for note $recordingId")
                } catch (e: Exception) {
                    Log.e(TAG, "Transcription failed for note $recordingId", e)
                    recordingDao.updateRecording(
                        recording.copy(
                            isProcessing = false,
                            aiStatus = RecordingAiStatus.ERROR,
                            processingError = e.message ?: "Transcription failed"
                        )
                    )
                    continue
                }
            }

            // Stage 2: AI Summary and Insights (Requires internet)
            if (!recording.summary.isNullOrBlank() && recording.aiStatus == RecordingAiStatus.READY) {
                completedCount++
                lastCompletedRecording = recording
                continue
            }

            val hasInternet = NetworkUtils.isInternetAvailable(applicationContext)
            if (hasInternet) {
                val summarizingTitle = if (totalCount > 1) {
                    "Analyzing note ${index + 1} of $totalCount"
                } else {
                    "Generating Insights: $noteName"
                }
                updateForegroundProgress(
                    title = summarizingTitle,
                    content = "Extracting summary, ideas, and action items...",
                    progress = baseProgress + 2,
                    max = totalCount * 2
                )

                try {
                    recordingDao.updateRecording(
                        recording.copy(
                            isProcessing = true,
                            aiStatus = RecordingAiStatus.SUMMARY_PROCESSING,
                            processingError = null
                        )
                    )

                    val insights = aiProcessor.generateInsights(transcript!!)
                    val updatedName = if (recording.name.isBlank() ||
                        recording.name == "Voice Note" ||
                        recording.name.startsWith("Voice note", ignoreCase = true)
                    ) {
                        insights.title?.takeIf { it.isNotBlank() } ?: recording.name
                    } else {
                        recording.name
                    }

                    saveInsightsToRoom(
                        insightDao = insightDao,
                        recordingId = recording.id,
                        recordingName = updatedName,
                        timestamp = recording.timestamp,
                        insights = insights
                    )

                    val finishedRecording = recording.copy(
                        name = updatedName,
                        summary = insights.summary,
                        keyPoints = gson.toJson(insights.keyPoints),
                        actionItems = gson.toJson(insights.actionItems),
                        wiifm = insights.wiifm,
                        isProcessing = false,
                        aiStatus = RecordingAiStatus.READY,
                        processingError = null
                    )
                    recordingDao.updateRecording(finishedRecording)

                    completedCount++
                    lastCompletedRecording = finishedRecording
                    Log.d(TAG, "AI insights generated successfully for note $recordingId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to generate AI insights for note $recordingId", e)
                    // If network dropped mid-request, treat as pending offline rather than hard error
                    if (!NetworkUtils.isInternetAvailable(applicationContext)) {
                        recordingDao.updateRecording(
                            recording.copy(
                                isProcessing = false,
                                aiStatus = RecordingAiStatus.SUMMARY_PENDING_OFFLINE,
                                processingError = null
                            )
                        )
                        offlinePendingCount++
                    } else {
                        recordingDao.updateRecording(
                            recording.copy(
                                isProcessing = false,
                                aiStatus = RecordingAiStatus.ERROR,
                                processingError = e.message ?: "Failed to generate summary"
                            )
                        )
                    }
                }
            } else {
                // Offline: mark pending silently without notifying user about internet wait
                Log.d(TAG, "No internet for note $recordingId. Marking SUMMARY_PENDING_OFFLINE quietly.")
                recordingDao.updateRecording(
                    recording.copy(
                        isProcessing = false,
                        aiStatus = RecordingAiStatus.SUMMARY_PENDING_OFFLINE,
                        processingError = null
                    )
                )
                offlinePendingCount++
            }
        }

        // If any notes were left pending offline, schedule the network worker quietly
        if (offlinePendingCount > 0) {
            AiSummaryNetworkWorker.enqueueWhenConnected(applicationContext)
        }

        // Post completion notification if any notes finished
        if (completedCount > 0) {
            postCompletionNotification(completedCount, totalCount, lastCompletedRecording)
        }

        return Result.success()
    }

    private fun createForegroundInfo(notification: Notification): ForegroundInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(
                NOTIFICATION_ID_FOREGROUND,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID_FOREGROUND, notification)
        }
    }

    private fun updateForegroundProgress(
        title: String,
        content: String,
        progress: Int,
        max: Int
    ) {
        val notification = buildProgressNotification(title, content, progress, max)
        notificationManager.notify(NOTIFICATION_ID_FOREGROUND, notification)
    }

    private fun buildProgressNotification(
        title: String,
        content: String,
        progress: Int,
        max: Int
    ): Notification {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(applicationContext, CHANNEL_ID_PROGRESS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setProgress(max, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun postCompletionNotification(
        completedCount: Int,
        totalCount: Int,
        lastRecording: Recording?
    ) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (completedCount == 1 && lastRecording != null) {
                putExtra(MainActivity.EXTRA_OPEN_RECORDING_ID, lastRecording.id)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "AI Insights Ready"
        val content = if (completedCount == 1 && lastRecording != null) {
            "${lastRecording.name.ifBlank { "Voice note" }} insights are ready."
        } else {
            "AI insights generated for $completedCount voice notes."
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID_COMPLETION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_COMPLETION, notification)
    }

    private fun createNotificationChannels() {
        val progressChannel = NotificationChannel(
            CHANNEL_ID_PROGRESS,
            applicationContext.getString(R.string.ai_processing_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = applicationContext.getString(R.string.ai_processing_channel_desc)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }

        val completionChannel = NotificationChannel(
            CHANNEL_ID_COMPLETION,
            applicationContext.getString(R.string.ai_completion_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = applicationContext.getString(R.string.ai_completion_channel_desc)
            setShowBadge(true)
            enableVibration(true)
        }

        notificationManager.createNotificationChannel(progressChannel)
        notificationManager.createNotificationChannel(completionChannel)
    }

    private suspend fun saveInsightsToRoom(
        insightDao: InsightDao,
        recordingId: Long,
        recordingName: String,
        timestamp: Long,
        insights: AIProcessor.AIInsights
    ) {
        try {
            insightDao.deleteInsightsByRecordingId(recordingId)
            val entities = mutableListOf<InsightEntity>()

            insights.actionItems.forEachIndexed { idx, rawAction ->
                val cleanText = rawAction.removePrefix("[ ]").removePrefix("[]").trim()
                if (cleanText.isNotBlank()) {
                    entities.add(
                        InsightEntity(
                            id = "${recordingId}_action_${idx}_${System.currentTimeMillis()}",
                            recordingId = recordingId,
                            recordingName = recordingName,
                            kind = InsightKind.ACTION,
                            text = cleanText,
                            status = InsightStatus.OPEN,
                            createdAt = timestamp,
                            sourceOrder = idx
                        )
                    )
                }
            }

            insights.ideas.forEachIndexed { idx, ideaText ->
                if (ideaText.isNotBlank()) {
                    entities.add(
                        InsightEntity(
                            id = "${recordingId}_idea_${idx}_${System.currentTimeMillis()}",
                            recordingId = recordingId,
                            recordingName = recordingName,
                            kind = InsightKind.IDEA,
                            text = ideaText.trim(),
                            createdAt = timestamp,
                            sourceOrder = idx
                        )
                    )
                }
            }

            insights.decisions.forEachIndexed { idx, dec ->
                if (dec.text.isNotBlank()) {
                    entities.add(
                        InsightEntity(
                            id = "${recordingId}_decision_${idx}_${System.currentTimeMillis()}",
                            recordingId = recordingId,
                            recordingName = recordingName,
                            kind = InsightKind.DECISION,
                            text = dec.text.trim(),
                            rationale = dec.rationale?.trim(),
                            evidence = dec.evidence?.trim(),
                            createdAt = timestamp,
                            sourceOrder = idx
                        )
                    )
                }
            }

            if (entities.isNotEmpty()) {
                insightDao.insertInsights(entities)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving insights to Room for recording $recordingId", e)
        }
    }

    companion object {
        private const val TAG = "AiProcessingWorker"
        const val CHANNEL_ID_PROGRESS = "ai_processing_channel"
        const val CHANNEL_ID_COMPLETION = "ai_completion_channel"
        const val NOTIFICATION_ID_FOREGROUND = 2001
        const val NOTIFICATION_ID_COMPLETION = 2002
        const val KEY_RECORDING_IDS = "key_recording_ids"
        const val UNIQUE_WORK_NAME = "srutam_ai_processing_work"

        fun enqueueProcessing(context: Context, recordingIds: List<Long>) {
            if (recordingIds.isEmpty()) return

            val data = workDataOf(
                KEY_RECORDING_IDS to recordingIds.toLongArray()
            )
            val request = OneTimeWorkRequestBuilder<AiProcessingWorker>()
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
            Log.d(TAG, "Enqueued AiProcessingWorker for ${recordingIds.size} recordings")
        }
    }
}
