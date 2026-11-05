package space.iamjustkrishna.srutam.service

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * The floating dock: one pill. It rests in the screen edge with only its logo end showing (red while
 * recording) and slides out whole when tapped, then back in. Nothing swaps or fades in its place, so the
 * motion is one piece. Tucked away while recording, the elapsed time is written along the visible end.
 *
 * Geometry for a dock on the left (the right is the mirror image): the pill is laid out at the left of a
 * window that is only as wide as what shows. Tucked, the pill is translated left until just its logo end
 * (PEEK) is inside the window; out, it is translated right by a small gap. The window is widened before
 * the pill slides out and narrowed after it has slid back, so a tucked dock never blocks taps next to it.
 */
class FloatingButtonService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null

    private var isDockedLeft = true
    private var isExpanded = false

    /** True once a slide back into the edge has finished (the time may then be written beside it). */
    private var settled = true
    private var lastRecording = false

    /** When the pill was last opened or used, for tucking it away after a quiet spell. */
    private var lastTouchAt = 0L

    /** The pill's current width in px: wider with the recording controls than with the start menu. */
    private var pillWidth = 0

    private var morphAnimator: ValueAnimator? = null
    private var colorAnimator: ValueAnimator? = null
    private var arcShown = false

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val density get() = resources.displayMetrics.density
    private fun dp(v: Float) = (v * density).roundToInt()
    private val peekPx get() = dp(PEEK_DP)
    private val gapPx get() = dp(OUT_GAP_DP)
    private val arcExtraPx get() = dp(ARC_EXTRA_DP)

    private fun <T : View> view(id: Int): T? = floatingView?.findViewById(id)

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

    private fun isRecordingNow() = RecordingCoordinator.isRecording || RecordingForegroundService.isRecording
    private fun isPausedNow() = RecordingCoordinator.isPaused || RecordingForegroundService.isPaused

    // ---- the window --------------------------------------------------------------------------

    private fun showFloatingButton() {
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val root = LayoutInflater.from(this).inflate(R.layout.floating_record_button, null)
            floatingView = root

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            isDockedLeft = AppPreferences.isFloatingDockOnLeft(this)
            val defaultY = (resources.displayMetrics.heightPixels * 0.35f).toInt()

            val params = WindowManager.LayoutParams(
                peekPx,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or (if (isDockedLeft) Gravity.START else Gravity.END)
                x = 0
                y = AppPreferences.getFloatingDockY(this@FloatingButtonService, defaultY)
            }
            windowLayoutParams = params
            windowManager?.addView(root, params)

            setupButtons()
            setupLogoTouch()
            lastRecording = isRecordingNow()
            styleFor(lastRecording, animate = false)
            reflow()
            Log.d(TAG, "Floating dock displayed")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing floating dock", e)
        }
    }

    private fun setWindowWidth(px: Int) {
        val params = windowLayoutParams ?: return
        if (params.width == px) return
        params.width = px
        try {
            windowManager?.updateViewLayout(floatingView, params)
        } catch (e: Exception) {
            Log.w(TAG, "Error updating the dock window", e)
        }
    }

    // ---- the pill's layout -------------------------------------------------------------------

    /** How wide the pill is with the recording controls ([recording]) or the start menu. */
    private fun widthFor(recording: Boolean): Int {
        val pill = view<View>(R.id.dock_pill) ?: return 0
        val idle = view<View>(R.id.pill_idle) ?: return 0
        val rec = view<View>(R.id.pill_rec) ?: return 0
        val idleVisibility = idle.visibility
        val recVisibility = rec.visibility
        idle.visibility = if (recording) View.GONE else View.VISIBLE
        rec.visibility = if (recording) View.VISIBLE else View.GONE
        val lp = pill.layoutParams
        val oldWidth = lp.width
        lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
        pill.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.makeMeasureSpec(dp(52f), View.MeasureSpec.EXACTLY))
        val width = pill.measuredWidth
        lp.width = oldWidth
        idle.visibility = idleVisibility
        rec.visibility = recVisibility
        return width
    }

    private fun setPillContent(recording: Boolean) {
        val pill = view<View>(R.id.dock_pill) ?: return
        view<View>(R.id.pill_idle)?.apply { visibility = if (recording) View.GONE else View.VISIBLE; alpha = 1f }
        view<View>(R.id.pill_rec)?.apply { visibility = if (recording) View.VISIBLE else View.GONE; alpha = 1f }
        pillWidth = widthFor(recording)
        val lp = pill.layoutParams
        lp.width = pillWidth
        pill.layoutParams = lp
    }

    /** The side the dock is on: which way the pill is laid out, where it is anchored and which way the time curves. */
    private fun applySide() {
        val gravity = if (isDockedLeft) Gravity.START else Gravity.END
        view<View>(R.id.dock_pill)?.let { pill ->
            pill.layoutDirection = if (isDockedLeft) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL
            (pill.layoutParams as? FrameLayout.LayoutParams)?.let { it.gravity = gravity; pill.layoutParams = it }
        }
        view<DockTimeArcView>(R.id.dock_time_arc)?.let { arc ->
            arc.leftDocked = isDockedLeft
            (arc.layoutParams as? FrameLayout.LayoutParams)?.let { it.gravity = gravity; arc.layoutParams = it }
        }
    }

    private fun tuckedTranslation(): Float = if (isDockedLeft) (peekPx - pillWidth).toFloat() else (pillWidth - peekPx).toFloat()
    private fun outTranslation(): Float = if (isDockedLeft) gapPx.toFloat() else -gapPx.toFloat()
    private fun tuckedWindowWidth(recording: Boolean) = peekPx + if (recording) arcExtraPx else 0

    /** Lays everything out again for the current side, state and open or tucked position, with no animation. */
    private fun reflow() {
        val recording = isRecordingNow()
        applySide()
        setPillContent(recording)
        val pill = view<View>(R.id.dock_pill) ?: return
        if (isExpanded) {
            pill.translationX = outTranslation()
            setWindowWidth(pillWidth + gapPx)
        } else {
            pill.translationX = tuckedTranslation()
            setWindowWidth(tuckedWindowWidth(recording))
        }
    }

    // ---- opening, tucking and the state of recording -------------------------------------------

    /** The whole pill slides out of the edge. */
    private fun expand() {
        if (isExpanded) return
        isExpanded = true
        settled = false
        lastTouchAt = SystemClock.elapsedRealtime()
        morphAnimator?.cancel()
        val pill = view<View>(R.id.dock_pill) ?: return
        hideArc()
        setWindowWidth(pillWidth + gapPx)
        pill.animate().cancel()
        pill.animate().translationX(outTranslation()).setDuration(SLIDE_OUT_MS)
            .setInterpolator(PathInterpolator(0.2f, 0f, 0f, 1f)).start()
    }

    /** The whole pill slides back into the edge, leaving only its logo end. */
    private fun collapse() {
        if (!isExpanded) return
        isExpanded = false
        finishMorph()
        slideIntoEdge(SLIDE_IN_MS)
    }

    /** Slides the pill to where it rests tucked away, for the width and state it has right now. */
    private fun slideIntoEdge(durationMs: Long) {
        val pill = view<View>(R.id.dock_pill) ?: return
        pill.animate().cancel()
        pill.animate().translationX(tuckedTranslation()).setDuration(durationMs)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                if (!isExpanded) {
                    settled = true
                    setWindowWidth(tuckedWindowWidth(isRecordingNow()))
                }
            }.start()
    }

    /** Stops a widening or narrowing where it is and lays the pill out at the width its buttons need. */
    private fun finishMorph() {
        if (morphAnimator?.isRunning != true) return
        morphAnimator?.cancel()
        setPillContent(isRecordingNow())
    }

    private fun toggle() {
        if (isExpanded) collapse() else expand()
    }

    /** Recording started or ended: the pill turns red (or back) and its buttons change. */
    private fun onRecordingChanged(recording: Boolean) {
        styleFor(recording, animate = true)
        if (!isExpanded) {
            // Tucked away, or sliding there (a discard or a save slides it in just as the recording ends): the
            // buttons change unseen, and the pill carries on to where it rests for its new width. Aiming
            // for the old width would leave it entirely off screen.
            applySide()
            setPillContent(recording)
            if (settled) {
                view<View>(R.id.dock_pill)?.apply { animate().cancel(); translationX = tuckedTranslation() }
                setWindowWidth(tuckedWindowWidth(recording))
            } else {
                slideIntoEdge(SLIDE_IN_MS / 2)
            }
            return
        }
        morphTo(recording)
    }

    /** Open pill, new buttons: the pill widens or narrows while the old buttons fade out and the new ones in. */
    private fun morphTo(recording: Boolean) {
        val pill = view<View>(R.id.dock_pill) ?: return
        val idle = view<View>(R.id.pill_idle) ?: return
        val rec = view<View>(R.id.pill_rec) ?: return
        val oldView = if (recording) idle else rec
        val newView = if (recording) rec else idle
        val fromWidth = pillWidth
        val toWidth = widthFor(recording)
        pillWidth = toWidth

        morphAnimator?.cancel()
        morphAnimator = ValueAnimator.ofInt(fromWidth, toWidth).apply {
            duration = 240L
            interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
            addUpdateListener {
                val w = it.animatedValue as Int
                pill.layoutParams = pill.layoutParams.apply { width = w }
                setWindowWidth(w + gapPx)
            }
            start()
        }
        oldView.animate().cancel()
        newView.animate().cancel()
        oldView.animate().alpha(0f).setDuration(90L).withEndAction {
            oldView.visibility = View.GONE
            oldView.alpha = 1f
            newView.alpha = 0f
            newView.visibility = View.VISIBLE
            newView.animate().alpha(1f).setDuration(140L).start()
        }.start()
        lastTouchAt = SystemClock.elapsedRealtime()
    }

    /** The fill of the pill and the logo disc: dark while idle, red while recording. */
    private fun styleFor(recording: Boolean, animate: Boolean) {
        val pillBg = view<View>(R.id.dock_pill)?.background?.mutate() as? GradientDrawable ?: return
        val logoBg = view<View>(R.id.floating_record_button)?.background?.mutate() as? GradientDrawable
        val toPill = if (recording) COLOR_RED else COLOR_IDLE
        val toLogo = if (recording) COLOR_LOGO_RED else COLOR_LOGO_IDLE
        // The stop button is white with a red icon; the bin is white so it reads on red.
        view<ImageView>(R.id.btn_dock_stop_save)?.imageTintList = ColorStateList.valueOf(COLOR_RED)
        view<ImageView>(R.id.btn_dock_cancel)?.imageTintList = ColorStateList.valueOf(Color.WHITE)

        colorAnimator?.cancel()
        if (!animate) {
            pillBg.setColor(toPill)
            logoBg?.setColor(toLogo)
            return
        }
        val fromPill = (if (recording) COLOR_IDLE else COLOR_RED)
        val fromLogo = (if (recording) COLOR_LOGO_IDLE else COLOR_LOGO_RED)
        val evaluator = ArgbEvaluator()
        colorAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 160L
            addUpdateListener {
                val f = it.animatedFraction
                pillBg.setColor(evaluator.evaluate(f, fromPill, toPill) as Int)
                logoBg?.setColor(evaluator.evaluate(f, fromLogo, toLogo) as Int)
            }
            start()
        }
    }

    // ---- the buttons -------------------------------------------------------------------------

    private fun setupButtons() {
        view<View>(R.id.btn_dock_start_record)?.setOnClickListener {
            Log.d(TAG, "Record clicked from dock")
            startRecording()
        }
        view<View>(R.id.btn_dock_open_app)?.setOnClickListener {
            openMainActivity()
            collapse()
        }
        view<View>(R.id.btn_dock_collapse)?.setOnClickListener { collapse() }
        view<View>(R.id.btn_dock_collapse_recording)?.setOnClickListener { collapse() }
        view<View>(R.id.btn_dock_pause_resume)?.setOnClickListener {
            lastTouchAt = SystemClock.elapsedRealtime()
            togglePauseResume()
        }
        view<View>(R.id.btn_dock_stop_save)?.setOnClickListener { stopRecording() }
        view<View>(R.id.btn_dock_cancel)?.setOnClickListener { cancelRecording() }
    }

    /** Tap the logo end to open or tuck the pill; drag it to move the dock up, down or to the other edge. */
    private fun setupLogoTouch() {
        val logo = view<View>(R.id.floating_record_button) ?: return
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop.coerceAtLeast(36)

        logo.setOnTouchListener(object : View.OnTouchListener {
            private var dragging = false
            private var downX = 0f
            private var downY = 0f
            private var startX = 0
            private var startY = 0

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val params = windowLayoutParams ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        // Switch to TOP | START coordinates, so the window can be dragged across the screen.
                        val width = floatingView?.width ?: peekPx
                        params.gravity = Gravity.TOP or Gravity.START
                        params.x = if (isDockedLeft) 0 else resources.displayMetrics.widthPixels - width
                        startX = params.x
                        startY = params.y
                        dragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - downX).toInt()
                        val dy = (event.rawY - downY).toInt()
                        if (!dragging && hypot(dx.toDouble(), dy.toDouble()) > touchSlop) dragging = true
                        if (dragging) {
                            params.x = startX + dx
                            params.y = (startY + dy).coerceIn(40, resources.displayMetrics.heightPixels - 200)
                            try {
                                windowManager?.updateViewLayout(floatingView, params)
                            } catch (e: Exception) {
                                Log.w(TAG, "Error moving the dock", e)
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val width = floatingView?.width ?: peekPx
                        if (dragging) {
                            val screenWidth = resources.displayMetrics.widthPixels
                            val left = (params.x + width / 2) < (screenWidth / 2)
                            AppPreferences.setFloatingDockY(this@FloatingButtonService, params.y)
                            if (left != isDockedLeft) {
                                isDockedLeft = left
                                AppPreferences.setFloatingDockOnLeft(this@FloatingButtonService, left)
                            }
                        }
                        params.gravity = Gravity.TOP or (if (isDockedLeft) Gravity.START else Gravity.END)
                        params.x = 0
                        try {
                            windowManager?.updateViewLayout(floatingView, params)
                        } catch (e: Exception) {
                            Log.w(TAG, "Error docking the dock", e)
                        }
                        if (dragging) reflow() else toggle()
                        return true
                    }
                }
                return false
            }
        })
    }

    // ---- recording ---------------------------------------------------------------------------

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
        // The pill stays out and turns into the recording controls (the state watcher does that); they tuck
        // into the edge by themselves after a few quiet seconds (see DockCollapse).
        lastTouchAt = SystemClock.elapsedRealtime()
    }

    private fun togglePauseResume() {
        if (RecordingCoordinator.isPaused) {
            RecordingCoordinator.requestResume(this)
            showElevatedToast("Recording resumed")
        } else {
            RecordingCoordinator.requestPause(this)
            showElevatedToast("Recording paused")
        }
        updatePauseIcon()
    }

    private fun updatePauseIcon() {
        view<ImageView>(R.id.btn_dock_pause_resume)?.setImageResource(
            if (isPausedNow()) R.drawable.ic_floating_play else R.drawable.ic_floating_pause
        )
    }

    private fun stopRecording() {
        RecordingCoordinator.requestStop(this)
        showElevatedToast("Voice note saved")
        collapse()
    }

    private fun cancelRecording() {
        RecordingCoordinator.requestCancel(this)
        showElevatedToast("Recording discarded")
        collapse()
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

    // ---- the time ----------------------------------------------------------------------------

    private fun hideArc() {
        val arc = view<DockTimeArcView>(R.id.dock_time_arc) ?: return
        arc.animate().cancel()
        arc.visibility = View.GONE
        arcShown = false
    }

    private fun formatTime(ms: Long): String {
        val seconds = (ms / 1000).toInt()
        return String.format("%d:%02d", seconds / 60, seconds % 60)
    }

    private fun startStateWatcher() {
        // Collect reactive state transitions from RecordingCoordinator
        serviceScope.launch {
            RecordingCoordinator.state.collect { state ->
                val recording = isRecordingNow()
                if (recording != lastRecording) {
                    lastRecording = recording
                    onRecordingChanged(recording)
                }
                updatePauseIcon()
                if (state is RecordingCoordinator.RecordingSessionState.Idle && isExpanded) collapse()
            }
        }

        // The time, and tucking the pill away after a quiet spell
        serviceScope.launch {
            while (isActive) {
                val recording = isRecordingNow()
                val paused = isPausedNow()
                val now = SystemClock.elapsedRealtime()

                if (DockCollapse.shouldCollapse(isExpanded, recording, now - lastTouchAt)) collapse()
                if (!isExpanded && settled) {
                    val pill = view<View>(R.id.dock_pill)
                    if (pill != null && kotlin.math.abs(pill.translationX - tuckedTranslation()) > 1f) reflow()
                }

                val elapsed = RecordingForegroundService.elapsedDurationMs
                if (recording && isExpanded) {
                    view<TextView>(R.id.txt_dock_timer)?.apply {
                        text = formatTime(elapsed)
                        alpha = if (paused) 0.6f else 1f
                    }
                }

                val arc = view<DockTimeArcView>(R.id.dock_time_arc)
                val showArc = recording && !isExpanded && settled
                if (arc != null) {
                    if (showArc) {
                        arc.text = formatTime(elapsed)
                        arc.dimmed = paused
                        if (!arcShown) {
                            arcShown = true
                            setWindowWidth(tuckedWindowWidth(true))
                            arc.alpha = 0f
                            arc.visibility = View.VISIBLE
                            arc.animate().alpha(1f).setDuration(160L).start()
                        }
                    } else if (arcShown) {
                        hideArc()
                        if (!isExpanded && settled) setWindowWidth(tuckedWindowWidth(false))
                    }
                }
                delay(200)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        morphAnimator?.cancel()
        colorAnimator?.cancel()
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

        /** How much of the pill shows when it is tucked away: its logo end. */
        private const val PEEK_DP = 50f

        /** The gap between the screen edge and the pill when it is out. */
        private const val OUT_GAP_DP = 6f

        /** Room beside the visible end for the elapsed time. */
        private const val ARC_EXTRA_DP = 20f

        private const val SLIDE_OUT_MS = 300L
        private const val SLIDE_IN_MS = 300L

        private val COLOR_IDLE = Color.parseColor("#0F172A")
        private val COLOR_RED = Color.parseColor("#DC2626")
        private val COLOR_LOGO_IDLE = Color.parseColor("#334155")
        private val COLOR_LOGO_RED = Color.parseColor("#7F1D1D")

        /** True while the dock service is alive (it is then the foreground owner of the one notification). */
        @Volatile
        var isRunning = false
            private set
    }
}
