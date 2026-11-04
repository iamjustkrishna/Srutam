package space.iamjustkrishna.srutam.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Outline
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.ViewConfiguration
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import space.iamjustkrishna.srutam.MainActivity
import space.iamjustkrishna.srutam.R
import space.iamjustkrishna.srutam.utils.AppPreferences

class FloatingButtonService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isExpanded = false
    /** When the controls were last opened or used, for folding them away after a quiet spell. */
    private var lastTouchAt = 0L
    private var shown = DockLayout.COLLAPSED
    private var moveAnimator: android.animation.ValueAnimator? = null
    private var isDockedLeft = true

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FloatingButtonService created")
        isRunning = true
        startForegroundWithNotification()
        isDockedLeft = AppPreferences.isFloatingDockOnLeft(this)
        showFloatingButton()
        startStateWatcher()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // ACTION_SHOW_IDLE (after a recording) and a plain start both re-attach this service to the one
        // recording notification, drawn for whatever state recording is in.
        startForegroundWithNotification()
        if (floatingView == null) {
            showFloatingButton()
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = QuickRecordNotification.buildCurrent(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                QuickRecordNotification.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(QuickRecordNotification.NOTIFICATION_ID, notification)
        }
    }

    private fun showFloatingButton() {
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            floatingView = LayoutInflater.from(this).inflate(
                R.layout.floating_record_button,
                null
            )

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val screenHeight = resources.displayMetrics.heightPixels
            val density = resources.displayMetrics.density
            val embedOffsetPx = (8 * density).toInt()
            isDockedLeft = AppPreferences.isFloatingDockOnLeft(this)
            val defaultY = (screenHeight * 0.35f).toInt()
            val initialY = AppPreferences.getFloatingDockY(this, defaultY)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or (if (isDockedLeft) Gravity.START else Gravity.END)
                x = -embedOffsetPx
                y = initialY
            }
            windowLayoutParams = params

            windowManager?.addView(floatingView, params)

            setupDragAndClick(params)
            setupExpandedOptions()
            renderCurrentState()

            Log.d(TAG, "Floating button displayed")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing floating button", e)
        }
    }

    private fun setupDragAndClick(params: WindowManager.LayoutParams) {
        val collapsedBtn = floatingView?.findViewById<View>(R.id.floating_record_button)
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop.coerceAtLeast(36)

        val touchListener = object : View.OnTouchListener {
            private var isDragging = false
            private var downX = 0f
            private var downY = 0f
            private var startX = 0
            private var startY = 0

            override fun onTouch(view: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        moveAnimator?.cancel()
                        downX = event.rawX
                        downY = event.rawY
                        val screenWidth = resources.displayMetrics.widthPixels
                        val density = resources.displayMetrics.density
                        val tabWidth = (52 * density).toInt()
                        val embedOffsetPx = (8 * density).toInt()

                        // Temporarily switch to TOP | START coordinates for continuous screen dragging
                        params.gravity = Gravity.TOP or Gravity.START
                        params.x = if (isDockedLeft) -embedOffsetPx else (screenWidth - tabWidth + embedOffsetPx)
                        startX = params.x
                        startY = params.y
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - downX).toInt()
                        val dy = (event.rawY - downY).toInt()
                        val distance = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()

                        if (!isDragging && distance > touchSlop) {
                            isDragging = true
                        }

                        if (isDragging) {
                            val screenHeight = resources.displayMetrics.heightPixels
                            params.x = startX + dx
                            params.y = (startY + dy).coerceIn(40, screenHeight - 200)
                            windowManager?.updateViewLayout(floatingView, params)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val density = resources.displayMetrics.density
                        val embedOffsetPx = (8 * density).toInt()
                        if (isDragging) {
                            val screenWidth = resources.displayMetrics.widthPixels
                            val tabWidth = (52 * density).toInt()
                            isDockedLeft = (params.x + tabWidth / 2) < (screenWidth / 2)
                            AppPreferences.setFloatingDockOnLeft(this@FloatingButtonService, isDockedLeft)
                            AppPreferences.setFloatingDockY(this@FloatingButtonService, params.y)

                            params.gravity = Gravity.TOP or (if (isDockedLeft) Gravity.START else Gravity.END)
                            params.x = -embedOffsetPx
                            windowManager?.updateViewLayout(floatingView, params)
                            renderCurrentState()
                        } else {
                            // Tap event on collapsed dock tab
                            params.gravity = Gravity.TOP or (if (isDockedLeft) Gravity.START else Gravity.END)
                            params.x = -embedOffsetPx
                            toggleExpanded()
                        }
                        return true
                    }
                }
                return false
            }
        }

        collapsedBtn?.setOnTouchListener(touchListener)
    }

    private fun setupExpandedOptions() {
        // Direct record button (circular icon only)
        floatingView?.findViewById<View>(R.id.btn_dock_start_record)?.setOnClickListener {
            Log.d(TAG, "Record icon clicked from dock")
            startRecording()
        }

        // Open app button
        floatingView?.findViewById<View>(R.id.btn_dock_open_app)?.setOnClickListener {
            Log.d(TAG, "Open app clicked from dock")
            openMainActivity()
            isExpanded = false
            adjustPositionForExpandedState()
            renderCurrentState()
        }

        // Collapse button
        floatingView?.findViewById<View>(R.id.btn_dock_collapse)?.setOnClickListener {
            Log.d(TAG, "Collapse button clicked from dock")
            isExpanded = false
            adjustPositionForExpandedState()
            renderCurrentState()
        }

        // Recording controls: fold back into the edge tab
        floatingView?.findViewById<View>(R.id.btn_dock_collapse_recording)?.setOnClickListener {
            collapse()
        }

        // Recording controls: Pause / Resume
        floatingView?.findViewById<View>(R.id.btn_dock_pause_resume)?.setOnClickListener {
            lastTouchAt = SystemClock.elapsedRealtime()
            togglePauseResume()
        }

        // Recording controls: Save & Stop
        floatingView?.findViewById<View>(R.id.btn_dock_stop_save)?.setOnClickListener {
            stopRecording()
        }

        // Recording controls: Cancel & Discard
        floatingView?.findViewById<View>(R.id.btn_dock_cancel)?.setOnClickListener {
            cancelRecording()
        }
    }

    private fun toggleExpanded() {
        isExpanded = !isExpanded
        lastTouchAt = SystemClock.elapsedRealtime()
        adjustPositionForExpandedState()
        renderCurrentState()
    }

    private fun collapse() {
        if (!isExpanded) return
        isExpanded = false
        adjustPositionForExpandedState()
        renderCurrentState()
    }

    private fun adjustPositionForExpandedState() {
        val params = windowLayoutParams ?: return
        val density = resources.displayMetrics.density
        val embedOffsetPx = (8 * density).toInt()

        params.gravity = Gravity.TOP or (if (isDockedLeft) Gravity.START else Gravity.END)
        val targetX = if (isExpanded) (8 * density).toInt() else -embedOffsetPx
        moveAnimator?.cancel()
        if (params.x == targetX) {
            windowManager?.updateViewLayout(floatingView, params)
            return
        }
        // Glide to the new spot instead of jumping there.
        moveAnimator = android.animation.ValueAnimator.ofInt(params.x, targetX).apply {
            duration = 320L
            interpolator = android.view.animation.PathInterpolator(0.2f, 0f, 0f, 1f)
            addUpdateListener {
                params.x = it.animatedValue as Int
                runCatching { windowManager?.updateViewLayout(floatingView, params) }
            }
            start()
        }
    }

    private fun viewFor(layout: DockLayout): View? = floatingView?.findViewById(
        when (layout) {
            DockLayout.COLLAPSED -> R.id.dock_collapsed
            DockLayout.IDLE -> R.id.dock_expanded_idle
            DockLayout.RECORDING -> R.id.dock_expanded_recording
        }
    )

    /** How wide [v] will be once shown, even before it has been laid out. */
    private fun widthOf(v: View): Float {
        v.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        return v.measuredWidth.toFloat()
    }

    /**
     * Moves to [target]. A bar slides out of the screen edge, all of it, and goes back into the edge the
     * same way, leaving the tab; two bars (the menu and the recording controls) cross-fade in place.
     */
    private fun show(target: DockLayout) {
        if (target == shown) return
        val from = viewFor(shown) ?: return
        val to = viewFor(target) ?: return
        val fromLayout = shown
        shown = target
        val towardEdge = if (isDockedLeft) -1f else 1f

        from.animate().cancel()
        to.animate().cancel()
        val reset = {
            if (shown != fromLayout) from.visibility = View.GONE
            from.alpha = 1f
            from.translationX = 0f
        }
        val slide = android.view.animation.PathInterpolator(0.2f, 0f, 0f, 1f)

        when {
            // A bar goes back into the edge; the tab fades in where it was.
            target == DockLayout.COLLAPSED -> {
                from.animate().translationX(towardEdge * widthOf(from)).alpha(0.2f).setDuration(280L)
                    .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator()).withEndAction(reset).start()
                to.alpha = 0f
                to.translationX = 0f
                to.visibility = View.VISIBLE
                to.animate().alpha(1f).setStartDelay(160L).setDuration(180L).start()
            }
            // A bar comes out of the edge over the tab, which fades away behind it.
            shown != DockLayout.COLLAPSED && fromLayout == DockLayout.COLLAPSED -> {
                from.animate().alpha(0f).setStartDelay(100L).setDuration(160L).withEndAction(reset).start()
                to.alpha = 1f
                to.translationX = towardEdge * widthOf(to)
                to.visibility = View.VISIBLE
                to.animate().translationX(0f).setDuration(340L).setInterpolator(slide).start()
            }
            // Menu to recording controls, or back: a quiet cross-fade.
            else -> {
                from.animate().alpha(0f).setDuration(120L).withEndAction(reset).start()
                to.alpha = 0f
                to.translationX = 0f
                to.visibility = View.VISIBLE
                to.animate().alpha(1f).setStartDelay(80L).setDuration(180L).start()
            }
        }
    }

    private fun renderCurrentState() {
        val collapsedLayout = floatingView?.findViewById<View>(R.id.dock_collapsed)
        val expandedIdleLayout = floatingView?.findViewById<View>(R.id.dock_expanded_idle)
        val expandedRecLayout = floatingView?.findViewById<View>(R.id.dock_expanded_recording)
        val collapsedBtn = floatingView?.findViewById<ImageView>(R.id.floating_record_button)
        val pauseResumeBtn = floatingView?.findViewById<ImageView>(R.id.btn_dock_pause_resume)

        val isRec = RecordingCoordinator.isRecording || RecordingForegroundService.isRecording
        val isPaused = RecordingCoordinator.isPaused || RecordingForegroundService.isPaused

        // Mirror layout direction for right docked orientation so actions flow naturally from edge
        val layoutDir = if (isDockedLeft) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL
        expandedIdleLayout?.layoutDirection = layoutDir
        expandedRecLayout?.layoutDirection = layoutDir

        if (!isExpanded) {
            val density = resources.displayMetrics.density
            val padEmbedded = (11 * density).toInt()
            val padFree = (6 * density).toInt()
            val padY = (7 * density).toInt()
            val radius = 22 * density

            if (isDockedLeft) {
                collapsedBtn?.setPadding(padEmbedded, padY, padFree, padY)
                collapsedBtn?.setBackgroundResource(
                    if (isRec) R.drawable.bg_floating_dock_recording_edge_left
                    else R.drawable.bg_floating_dock_edge_left
                )
                collapsedBtn?.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect((-radius).toInt(), 0, view.width, view.height, radius)
                    }
                }
            } else {
                collapsedBtn?.setPadding(padFree, padY, padEmbedded, padY)
                collapsedBtn?.setBackgroundResource(
                    if (isRec) R.drawable.bg_floating_dock_recording_edge_right
                    else R.drawable.bg_floating_dock_edge_right
                )
                collapsedBtn?.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, (view.width + radius).toInt(), view.height, radius)
                    }
                }
            }
            collapsedBtn?.clipToOutline = true
            collapsedBtn?.elevation = 0f
            show(DockLayout.COLLAPSED)
        } else {
            val density = resources.displayMetrics.density
            val radius = 22 * density
            val cardOutlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, radius)
                }
            }
            expandedIdleLayout?.outlineProvider = cardOutlineProvider
            expandedIdleLayout?.clipToOutline = true
            expandedIdleLayout?.elevation = 0f

            expandedRecLayout?.outlineProvider = cardOutlineProvider
            expandedRecLayout?.clipToOutline = true
            expandedRecLayout?.elevation = 0f

            if (isRec) {
                show(DockLayout.RECORDING)

                if (isPaused) {
                    pauseResumeBtn?.setImageResource(R.drawable.ic_floating_play)
                } else {
                    pauseResumeBtn?.setImageResource(R.drawable.ic_floating_pause)
                }
            } else {
                show(DockLayout.IDLE)
            }
        }

        windowLayoutParams?.let { params ->
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            params.gravity = Gravity.TOP or (if (isDockedLeft) Gravity.START else Gravity.END)
            try {
                windowManager?.updateViewLayout(floatingView, params)
            } catch (e: Exception) {
                Log.w(TAG, "Error updating window layout", e)
            }
        }
    }

    private fun startRecording() {
        if (!MicPermission.isGranted(this)) {
            // The app asks, or explains how to turn the mic on if it's blocked.
            MicPermission.openAppToAsk(this, "dock")
            return
        }

        val started = RecordingCoordinator.requestStart(this, "dock")
        if (!started) {
            showElevatedToast("Recording already in progress")
            return
        }
        // Show the recording controls first. They tuck into the edge tab on their own after a few quiet
        // seconds (see DockCollapse), where a ring and the elapsed time carry on showing it records.
        isExpanded = true
        lastTouchAt = SystemClock.elapsedRealtime()
        adjustPositionForExpandedState()
        renderCurrentState()
    }

    private fun togglePauseResume() {
        val isPaused = RecordingCoordinator.isPaused
        if (isPaused) {
            RecordingCoordinator.requestResume(this)
            showElevatedToast("Recording resumed")
        } else {
            RecordingCoordinator.requestPause(this)
            showElevatedToast("Recording paused")
        }
        renderCurrentState()
    }

    private fun stopRecording() {
        RecordingCoordinator.requestStop(this)
        showElevatedToast("Voice note saved")
        isExpanded = false
        adjustPositionForExpandedState()
        renderCurrentState()
    }

    private fun cancelRecording() {
        RecordingCoordinator.requestCancel(this)
        showElevatedToast("Recording discarded")
        isExpanded = false
        adjustPositionForExpandedState()
        renderCurrentState()
    }

    private fun showElevatedToast(message: String) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        val yOffsetPx = (140 * resources.displayMetrics.density).toInt()
        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, yOffsetPx)
        toast.show()
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    private fun startStateWatcher() {
        // Collect reactive state transitions from RecordingCoordinator
        serviceScope.launch {
            RecordingCoordinator.state.collect { state ->
                renderCurrentState()
                if (state is RecordingCoordinator.RecordingSessionState.Idle && isExpanded) {
                    isExpanded = false
                    adjustPositionForExpandedState()
                    renderCurrentState()
                }
            }
        }

        // Live timer updater when recording
        serviceScope.launch {
            while (isActive) {
                val isRec = RecordingCoordinator.isRecording || RecordingForegroundService.isRecording
                val isPaused = RecordingCoordinator.isPaused || RecordingForegroundService.isPaused

                if (DockCollapse.shouldCollapse(isExpanded, isRec, SystemClock.elapsedRealtime() - lastTouchAt)) {
                    collapse()
                }
                updateCollapsedTimer(isRec && !isExpanded, isPaused)
                if (isRec && isExpanded) {
                    val durationMs = RecordingForegroundService.elapsedDurationMs
                    val seconds = (durationMs / 1000).toInt()
                    val minutes = seconds / 60
                    val remSeconds = seconds % 60
                    val formatted = String.format("%d:%02d", minutes, remSeconds)
                    floatingView?.findViewById<TextView>(R.id.txt_dock_timer)?.text =
                        if (isPaused) "PAUSED $formatted" else "REC $formatted"
                }

                delay(200)
            }
        }
    }

    /** The elapsed time, curved around the red edge tab, while it records with the controls tucked away. */
    private fun updateCollapsedTimer(visible: Boolean, paused: Boolean) {
        val tab = floatingView?.findViewById<ImageView>(R.id.floating_record_button) ?: return
        val arc = floatingView?.findViewById<DockTimeArcView>(R.id.dock_time_arc) ?: return
        if (!visible) {
            arc.visibility = View.GONE
            return
        }
        // The tab and the text sit at the screen-edge end of the same frame.
        val gravity = if (isDockedLeft) Gravity.START else Gravity.END
        (tab.layoutParams as? android.widget.FrameLayout.LayoutParams)?.let { if (it.gravity != gravity) { it.gravity = gravity; tab.layoutParams = it } }
        (arc.layoutParams as? android.widget.FrameLayout.LayoutParams)?.let { if (it.gravity != gravity) { it.gravity = gravity; arc.layoutParams = it } }
        val seconds = (RecordingForegroundService.elapsedDurationMs / 1000).toInt()
        arc.leftDocked = isDockedLeft
        arc.dimmed = paused
        arc.text = String.format("%d:%02d", seconds / 60, seconds % 60)
        arc.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        moveAnimator?.cancel()
        serviceScope.cancel()
        floatingView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.w(TAG, "Error removing floating view on destroy", e)
            }
        }
        isRunning = false
        // A recording that outlives the dock keeps its notification; otherwise the notification goes with
        // the service, and the Start notification comes back if the quick-record setting is on.
        if (RecordingForegroundService.isRecording) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            QuickRecordNotification.restoreIdle(applicationContext)
        }
        Log.d(TAG, "FloatingButtonService destroyed")
    }

    companion object {
        private const val TAG = "FloatingButtonService"
        const val ACTION_SHOW_IDLE = "space.iamjustkrishna.srutam.action.SHOW_IDLE_NOTIFICATION"

        /** True while the dock service is alive (it is then the foreground owner of the one notification). */
        @Volatile
        var isRunning = false
            private set
    }
}

/** Which of the dock's three looks is on screen. */
private enum class DockLayout { COLLAPSED, IDLE, RECORDING }
