package space.iamjustkrishna.srutam.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.ViewConfiguration
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
    private var isDockedLeft = true

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FloatingButtonService created")
        isDockedLeft = AppPreferences.isFloatingDockOnLeft(this)
        showFloatingButton()
        startStateWatcher()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (floatingView == null) {
            showFloatingButton()
        }
        return START_STICKY
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

        // Recording controls: Pause / Resume
        floatingView?.findViewById<View>(R.id.btn_dock_pause_resume)?.setOnClickListener {
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
        adjustPositionForExpandedState()
        renderCurrentState()
    }

    private fun adjustPositionForExpandedState() {
        val params = windowLayoutParams ?: return
        val density = resources.displayMetrics.density
        val embedOffsetPx = (8 * density).toInt()

        params.gravity = Gravity.TOP or (if (isDockedLeft) Gravity.START else Gravity.END)
        params.x = if (isExpanded) (8 * density).toInt() else -embedOffsetPx
        windowManager?.updateViewLayout(floatingView, params)
    }

    private fun renderCurrentState() {
        val collapsedLayout = floatingView?.findViewById<View>(R.id.dock_collapsed)
        val expandedIdleLayout = floatingView?.findViewById<View>(R.id.dock_expanded_idle)
        val expandedRecLayout = floatingView?.findViewById<View>(R.id.dock_expanded_recording)
        val collapsedBtn = floatingView?.findViewById<ImageView>(R.id.floating_record_button)
        val pauseResumeBtn = floatingView?.findViewById<ImageView>(R.id.btn_dock_pause_resume)

        val isRec = RecordingForegroundService.isRecording
        val isPaused = RecordingForegroundService.isPaused

        // Mirror layout direction for right docked orientation so actions flow naturally from edge
        val layoutDir = if (isDockedLeft) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL
        expandedIdleLayout?.layoutDirection = layoutDir
        expandedRecLayout?.layoutDirection = layoutDir

        if (!isExpanded) {
            val density = resources.displayMetrics.density
            val padEmbedded = (11 * density).toInt()
            val padFree = (6 * density).toInt()
            val padY = (7 * density).toInt()

            if (isDockedLeft) {
                collapsedBtn?.setPadding(padEmbedded, padY, padFree, padY)
                collapsedBtn?.setBackgroundResource(
                    if (isRec) R.drawable.bg_floating_dock_recording_edge_left
                    else R.drawable.bg_floating_dock_edge_left
                )
            } else {
                collapsedBtn?.setPadding(padFree, padY, padEmbedded, padY)
                collapsedBtn?.setBackgroundResource(
                    if (isRec) R.drawable.bg_floating_dock_recording_edge_right
                    else R.drawable.bg_floating_dock_edge_right
                )
            }
            collapsedLayout?.visibility = View.VISIBLE
            expandedIdleLayout?.visibility = View.GONE
            expandedRecLayout?.visibility = View.GONE
        } else {
            collapsedLayout?.visibility = View.GONE
            if (isRec) {
                expandedIdleLayout?.visibility = View.GONE
                expandedRecLayout?.visibility = View.VISIBLE

                if (isPaused) {
                    pauseResumeBtn?.setImageResource(R.drawable.ic_floating_play)
                } else {
                    pauseResumeBtn?.setImageResource(R.drawable.ic_floating_pause)
                }
            } else {
                expandedIdleLayout?.visibility = View.VISIBLE
                expandedRecLayout?.visibility = View.GONE
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
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Microphone permission required to record", Toast.LENGTH_SHORT).show()
            openMainActivity()
            return
        }

        val intent = Intent(this, RecordingForegroundService::class.java).apply {
            action = RecordingForegroundService.ACTION_START_RECORDING
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        isExpanded = true
        adjustPositionForExpandedState()
        renderCurrentState()
    }

    private fun togglePauseResume() {
        val isPaused = RecordingForegroundService.isPaused
        val intent = Intent(this, RecordingForegroundService::class.java).apply {
            action = if (isPaused) {
                RecordingForegroundService.ACTION_RESUME_RECORDING
            } else {
                RecordingForegroundService.ACTION_PAUSE_RECORDING
            }
        }
        startService(intent)
        Toast.makeText(
            this,
            if (isPaused) "Recording resumed" else "Recording paused",
            Toast.LENGTH_SHORT
        ).show()
        renderCurrentState()
    }

    private fun stopRecording() {
        val intent = Intent(this, RecordingForegroundService::class.java).apply {
            action = RecordingForegroundService.ACTION_STOP_RECORDING
        }
        startService(intent)
        Toast.makeText(this, "Voice note saved", Toast.LENGTH_SHORT).show()
        isExpanded = false
        adjustPositionForExpandedState()
        renderCurrentState()
    }

    private fun cancelRecording() {
        val intent = Intent(this, RecordingForegroundService::class.java).apply {
            action = RecordingForegroundService.ACTION_DELETE_RECORDING
        }
        startService(intent)
        Toast.makeText(this, "Recording discarded", Toast.LENGTH_SHORT).show()
        isExpanded = false
        adjustPositionForExpandedState()
        renderCurrentState()
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    private fun startStateWatcher() {
        serviceScope.launch {
            var lastRecState = false
            var lastPauseState = false

            while (isActive) {
                val isRec = RecordingForegroundService.isRecording
                val isPaused = RecordingForegroundService.isPaused

                if (isRec != lastRecState || isPaused != lastPauseState) {
                    lastRecState = isRec
                    lastPauseState = isPaused
                    renderCurrentState()
                }

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

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        floatingView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.w(TAG, "Error removing floating view on destroy", e)
            }
        }
        Log.d(TAG, "FloatingButtonService destroyed")
    }

    companion object {
        private const val TAG = "FloatingButtonService"
    }
}
