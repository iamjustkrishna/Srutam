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
import space.iamjustkrishna.srutam.R

class FloatingButtonService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FloatingButtonService created")
        showFloatingButton()
    }

    private fun showFloatingButton() {
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            // Inflate the floating button layout
            floatingView = LayoutInflater.from(this).inflate(
                R.layout.floating_record_button,
                null
            )

            // Set up window parameters
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 100
            }

            windowManager?.addView(floatingView, params)

            // Set up click and drag listeners
            val recordButton = floatingView?.findViewById<ImageView>(R.id.floating_record_button)
            recordButton?.setOnTouchListener(object : View.OnTouchListener {
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
                            params.x = initialX + (event.rawX - initialTouchX).toInt()
                            params.y = initialY + (event.rawY - initialTouchY).toInt()
                            windowManager?.updateViewLayout(floatingView, params)
                            moved = true
                            lastAction = MotionEvent.ACTION_MOVE
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (!moved && lastAction == MotionEvent.ACTION_DOWN) {
                                // It was a click, not a drag
                                onFloatingButtonClicked()
                            }
                            return true
                        }
                    }
                    return false
                }
            })

            updateButtonAppearance()
            Log.d(TAG, "Floating button displayed")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing floating button", e)
        }
    }

    private fun onFloatingButtonClicked() {
        val isRecording = RecordingForegroundService.isRecording
        Log.d(TAG, "Floating button clicked, isRecording: $isRecording")

        val intent = Intent(this, RecordingForegroundService::class.java).apply {
            action = if (isRecording) {
                RecordingForegroundService.ACTION_STOP_RECORDING
            } else {
                RecordingForegroundService.ACTION_START_RECORDING
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            // Update button appearance after a short delay
            floatingView?.postDelayed({
                updateButtonAppearance()
            }, 300)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording service", e)
        }
    }

    private fun updateButtonAppearance() {
        val recordButton = floatingView?.findViewById<ImageView>(R.id.floating_record_button)
        recordButton?.let {
            if (RecordingForegroundService.isRecording) {
                it.setImageResource(android.R.drawable.ic_media_pause)
                it.setBackgroundResource(android.R.drawable.presence_busy)
            } else {
                it.setImageResource(android.R.drawable.ic_btn_speak_now)
                it.setBackgroundResource(android.R.drawable.presence_online)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let {
            windowManager?.removeView(it)
        }
        Log.d(TAG, "FloatingButtonService destroyed")
    }

    companion object {
        private const val TAG = "FloatingButtonService"
    }
}
