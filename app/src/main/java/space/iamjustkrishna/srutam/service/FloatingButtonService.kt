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
import android.widget.ImageView
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import space.iamjustkrishna.srutam.MainActivity
import space.iamjustkrishna.srutam.R

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
        showFloatingButton()
        startStateWatcher()
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
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = (screenHeight * 0.35f).toInt()
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

        val touchListener = object : View.OnTouchListener {
            private var lastAction = 0
            private var moved = false

            override fun onTouch(view: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        lastAction = MotionEvent.ACTION_DOWN
                        moved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            moved = true
                        }
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager?.updateViewLayout(floatingView, params)
                        lastAction = MotionEvent.ACTION_MOVE
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (moved) {
                            val screenWidth = resources.displayMetrics.widthPixels
                            val density = resources.displayMetrics.density
                            val tabWidth = (44 * density).toInt()
                            isDockedLeft = (params.x + tabWidth / 2) < (screenWidth / 2)
                            params.x = if (isDockedLeft) 0 else (screenWidth - tabWidth)
                            windowManager?.updateViewLayout(floatingView, params)
                            renderCurrentState()
                        } else if (lastAction == MotionEvent.ACTION_DOWN) {
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
        // Direct record button
        floatingView?.findViewById<View>(R.id.btn_dock_start_record)?.setOnClickListener {
            Log.d(TAG, "Record button clicked from dock")
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
        val screenWidth = resources.displayMetrics.widthPixels
        val density = resources.displayMetrics.density
        val tabWidth = (44 * density).toInt()

        if (isExpanded) {
            val estimatedWidthPx = (240 * density).toInt()
            if (isDockedLeft) {
                params.x = (8 * density).toInt()
            } else {
                params.x = Math.max((8 * density).toInt(), screenWidth - estimatedWidthPx - (8 * density).toInt())
            }
        } else {
            params.x = if (isDockedLeft) 0 else (screenWidth - tabWidth)
        }
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

        if (!isExpanded) {
            if (isDockedLeft) {
                collapsedBtn?.setBackgroundResource(
                    if (isRec) R.drawable.bg_floating_dock_recording_edge_left
                    else R.drawable.bg_floating_dock_edge_left
                )
            } else {
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
            try {
                windowManager?.updateViewLayout(floatingView, params)
            } catch (e: Exception) {
                Log.w(TAG, "Error updating window layout", e)
            }
        }
    }

    private fun startRecording() {
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
        renderCurrentState()
    }

    private fun stopRecording() {
        val intent = Intent(this, RecordingForegroundService::class.java).apply {
            action = RecordingForegroundService.ACTION_STOP_RECORDING
        }
        startService(intent)
        isExpanded = false
        adjustPositionForExpandedState()
        renderCurrentState()
    }

    private fun cancelRecording() {
        val intent = Intent(this, RecordingForegroundService::class.java).apply {
            action = RecordingForegroundService.ACTION_DELETE_RECORDING
        }
        startService(intent)
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
