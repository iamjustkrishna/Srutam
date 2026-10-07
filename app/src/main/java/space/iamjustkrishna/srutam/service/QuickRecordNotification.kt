package space.iamjustkrishna.srutam.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import space.iamjustkrishna.srutam.MainActivity
import space.iamjustkrishna.srutam.R
import space.iamjustkrishna.srutam.utils.AppPreferences

/**
 * The one recording notification. Its buttons sit in the collapsed view, so Start is visible without
 * expanding it, also on the lock screen. Before recording it shows Start; while recording, the timer
 * with Pause and Save; while paused, Resume and Save. Everything (the quick-record setting, the floating
 * dock and the recording service) uses this one id and builder, so there is never a second notification.
 *
 * Who posts it: [RecordingForegroundService] while recording, [FloatingButtonService] while the dock runs,
 * and a plain ongoing notification (no service needed) when only the quick-record setting is on.
 */
object QuickRecordNotification {
    // v3: high importance (still silent) so the phone shows the full notification, not a folded one-line row.
    const val CHANNEL_ID = "recording_channel_v3"
    const val NOTIFICATION_ID = 1001

    // Notifications and channels of earlier versions, removed on first launch after the update.
    private const val OLD_RECORDING_CHANNEL_ID = "recording_channel"
    private const val OLD_RECORDING_CHANNEL_V2_ID = "recording_channel_v2"
    private const val SAVED_VISIBLE_MS = 3_000L
    private const val FINISHING_MAX_MS = 60_000L
    private const val LEGACY_QUICK_RECORD_ID = 999
    private const val LEGACY_DOCK_ID = 1002
    private const val LEGACY_QUICK_RECORD_CHANNEL = "srutam_recording_channel"
    private const val LEGACY_DOCK_CHANNEL = "srutam_floating_dock_channel"

    private const val REQUEST_START = 20
    private const val REQUEST_PAUSE = 21
    private const val REQUEST_RESUME = 22
    private const val REQUEST_STOP = 23
    private const val REQUEST_OPEN_APP = 24

    sealed interface State {
        data object Idle : State
        data class Recording(val elapsedMs: Long) : State
        data class Paused(val elapsedMs: Long) : State

        /** Just saved: shown for a few seconds on the same notification. [finishing] while the transcript is still being written. */
        data class Saved(val durationMs: Long, val finishing: Boolean) : State
    }

    @Volatile
    private var saved: State.Saved? = null

    @Volatile
    private var savedUntilMs = 0L

    /** The Start notification is wanted while the floating dock runs (it needs a notification) or when asked for. */
    fun idleWanted(dockEnabled: Boolean, quickRecordEnabled: Boolean): Boolean = dockEnabled || quickRecordEnabled

    /**
     * What the "Persistent Recording Notification" switch shows. While the floating dock is on the
     * notification is on too (Android needs it for the dock), so the switch reads ON and is locked;
     * the user's own stored choice is untouched and comes back when the dock is turned off.
     */
    fun switchChecked(dockEnabled: Boolean, stored: Boolean): Boolean = dockEnabled || stored

    fun switchLocked(dockEnabled: Boolean): Boolean = dockEnabled

    fun idleWantedNow(context: Context): Boolean = idleWanted(
        AppPreferences.isFloatingDockEnabled(context),
        AppPreferences.isPersistentNotificationEnabled(context)
    )

    fun currentState(nowMs: Long = SystemClock.elapsedRealtime()): State {
        val justSaved = saved
        return when {
            RecordingForegroundService.isRecording && RecordingForegroundService.isPaused ->
                State.Paused(RecordingForegroundService.elapsedDurationMs)
            RecordingForegroundService.isRecording -> State.Recording(RecordingForegroundService.elapsedDurationMs)
            justSaved != null && nowMs < savedUntilMs -> justSaved
            else -> State.Idle
        }
    }

    /**
     * A recording was just saved: the notification says so for a few seconds, then goes back to Start.
     * While [finishing] (the transcript is still being written) it says so until the transcript is done.
     */
    fun markSaved(context: Context, durationMs: Long, finishing: Boolean, nowMs: Long = SystemClock.elapsedRealtime()) {
        saved = State.Saved(durationMs, finishing)
        savedUntilMs = nowMs + if (finishing) FINISHING_MAX_MS else SAVED_VISIBLE_MS
        if (!finishing) {
            val app = context.applicationContext
            Handler(Looper.getMainLooper()).postDelayed({ restoreIdle(app) }, SAVED_VISIBLE_MS + 100)
        }
    }

    internal fun clearSaved() {
        saved = null
        savedUntilMs = 0L
    }

    /** What the notification still says if the phone shows only its title and text (a folded row, a smart watch). */
    private fun fallbackText(state: State): Pair<String, String> = when (state) {
        State.Idle -> "Srutam" to "Ready to record"
        is State.Recording -> "Recording" to "Tap to open Srutam"
        is State.Paused -> "Paused" to formatDuration(state.elapsedMs)
        is State.Saved -> "Saved" to if (state.finishing) "Finishing transcript..." else "Your note is saved"
    }

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.recording_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.recording_notification_channel_desc)
            setShowBadge(true)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.deleteNotificationChannel(OLD_RECORDING_CHANNEL_ID)
        manager.deleteNotificationChannel(OLD_RECORDING_CHANNEL_V2_ID)
        manager.createNotificationChannel(channel)
    }

    fun buildCurrent(context: Context): Notification = build(context, currentState())

    fun build(context: Context, state: State): Notification {
        ensureChannel(context)
        val views = views(context, state)
        val (title, text) = fallbackText(state)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(views)
            .setCustomBigContentView(views)
            .setContentIntent(openApp(context))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setAutoCancel(false)
            .build()
    }

    /** The notification's layout for [state]; separate from [build] so it can be inflated and clicked in tests. */
    internal fun views(context: Context, state: State): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.notification_quick_record)
        when (state) {
            State.Idle -> {
                views.setTextViewText(R.id.quick_title, "Srutam")
                views.setTextViewText(R.id.quick_text, "Ready to record")
                views.setViewVisibility(R.id.quick_text, View.VISIBLE)
                views.setViewVisibility(R.id.quick_timer, View.GONE)
                views.setViewVisibility(R.id.quick_secondary, View.GONE)
                button(context, views, R.id.quick_primary, "Start", "Start recording", start(context))
            }
            is State.Recording -> {
                views.setTextViewText(R.id.quick_title, "Recording")
                views.setViewVisibility(R.id.quick_text, View.GONE)
                views.setViewVisibility(R.id.quick_timer, View.VISIBLE)
                views.setChronometer(R.id.quick_timer, SystemClock.elapsedRealtime() - state.elapsedMs, null, true)
                button(context, views, R.id.quick_primary, "Pause", "Pause recording", service(context, REQUEST_PAUSE, RecordingForegroundService.ACTION_PAUSE_RECORDING))
                button(context, views, R.id.quick_secondary, "Save", "Save recording", service(context, REQUEST_STOP, RecordingForegroundService.ACTION_STOP_RECORDING))
            }
            is State.Saved -> {
                views.setTextViewText(R.id.quick_title, "Saved")
                views.setTextViewText(
                    R.id.quick_text,
                    if (state.finishing) "Finishing transcript..." else "Your note is saved (${formatDuration(state.durationMs)})"
                )
                views.setViewVisibility(R.id.quick_text, View.VISIBLE)
                views.setViewVisibility(R.id.quick_timer, View.GONE)
                views.setViewVisibility(R.id.quick_secondary, View.GONE)
                if (state.finishing) {
                    views.setViewVisibility(R.id.quick_primary, View.GONE)
                } else {
                    button(context, views, R.id.quick_primary, "Start", "Start recording", start(context))
                }
            }
            is State.Paused -> {
                views.setTextViewText(R.id.quick_title, "Paused")
                views.setTextViewText(R.id.quick_text, formatDuration(state.elapsedMs))
                views.setViewVisibility(R.id.quick_text, View.VISIBLE)
                views.setViewVisibility(R.id.quick_timer, View.GONE)
                button(context, views, R.id.quick_primary, "Resume", "Resume recording", service(context, REQUEST_RESUME, RecordingForegroundService.ACTION_RESUME_RECORDING))
                button(context, views, R.id.quick_secondary, "Save", "Save recording", service(context, REQUEST_STOP, RecordingForegroundService.ACTION_STOP_RECORDING))
            }
        }
        return views
    }

    /** Turns the quick-record notification setting on or off and shows or removes the notification to match. */
    fun setEnabled(context: Context, enabled: Boolean) {
        AppPreferences.setPersistentNotificationEnabled(context, enabled)
        restoreIdle(context)
    }

    /**
     * Brings back the Start notification when it should be there and nothing is recording: after a
     * recording ends, after the dock stops, after a setting changes, at app start.
     */
    fun restoreIdle(context: Context, force: Boolean = false) {
        val app = context.applicationContext
        if (!force && RecordingForegroundService.isRecording) return
        val manager = app.getSystemService(NotificationManager::class.java) ?: return
        val dockEnabled = AppPreferences.isFloatingDockEnabled(app)
        val quickRecordEnabled = AppPreferences.isPersistentNotificationEnabled(app)
        when {
            !idleWanted(dockEnabled, quickRecordEnabled) -> manager.cancel(NOTIFICATION_ID)
            dockEnabled && FloatingButtonService.isRunning -> {
                // The dock is a foreground service; it takes its notification back so the two stay linked.
                ContextCompat.startForegroundService(
                    app,
                    Intent(app, FloatingButtonService::class.java).setAction(FloatingButtonService.ACTION_SHOW_IDLE)
                )
            }
            else -> manager.notify(NOTIFICATION_ID, build(app, currentState()))
        }
    }

    /** Removes the two separate notifications earlier versions showed (quick record and dock). */
    fun cleanUpLegacy(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.cancel(LEGACY_QUICK_RECORD_ID)
        manager.cancel(LEGACY_DOCK_ID)
        manager.deleteNotificationChannel(LEGACY_QUICK_RECORD_CHANNEL)
        manager.deleteNotificationChannel(LEGACY_DOCK_CHANNEL)
    }

    internal fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000).toInt()
        return String.format("%d:%02d", seconds / 60, seconds % 60)
    }

    private fun button(context: Context, views: RemoteViews, id: Int, label: String, description: String, action: PendingIntent) {
        views.setTextViewText(id, label)
        views.setViewVisibility(id, View.VISIBLE)
        views.setContentDescription(id, description)
        views.setOnClickPendingIntent(id, action)
    }

    private fun flags() = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    /** Start promotes the service to the foreground itself, so it goes out as a foreground-service intent. */
    private fun start(context: Context): PendingIntent = PendingIntent.getForegroundService(
        context,
        REQUEST_START,
        Intent(context, RecordingForegroundService::class.java).setAction(RecordingForegroundService.ACTION_START_RECORDING),
        flags()
    )

    private fun service(context: Context, requestCode: Int, action: String): PendingIntent = PendingIntent.getService(
        context,
        requestCode,
        Intent(context, RecordingForegroundService::class.java).setAction(action),
        flags()
    )

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_OPEN_APP,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        },
        PendingIntent.FLAG_IMMUTABLE
    )
}
