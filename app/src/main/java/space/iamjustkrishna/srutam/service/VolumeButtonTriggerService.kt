package space.iamjustkrishna.srutam.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class VolumeButtonTriggerService : AccessibilityService() {

    private var lastVolumeDownTime = 0L
    private var volumeDownPressCount = 0
    private val doublePressThreshold = 500L // milliseconds

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "=== VolumeButtonTriggerService CONNECTED ===")
        Log.d(TAG, "Service info: $serviceInfo")
        Log.d(TAG, "Can filter key events: ${serviceInfo?.flags?.and(android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS) != 0}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not needed for key event interception but log for debugging
        Log.v(TAG, "Accessibility event: ${event?.eventType}")
    }

    override fun onInterrupt() {
        Log.w(TAG, "=== VolumeButtonTriggerService INTERRUPTED ===")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        Log.v(TAG, "Key event received: keyCode=${event.keyCode}, action=${event.action}, scanCode=${event.scanCode}")

        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                val currentTime = System.currentTimeMillis()
                val timeSinceLastPress = currentTime - lastVolumeDownTime

                Log.d(TAG, "Volume DOWN pressed! Time since last press: ${timeSinceLastPress}ms")

                if (timeSinceLastPress < doublePressThreshold && lastVolumeDownTime > 0) {
                    volumeDownPressCount++
                    Log.d(TAG, "Within double-press threshold! Press count: $volumeDownPressCount")
                    if (volumeDownPressCount == 1) { // Second press detected (double press)
                        Log.d(TAG, "=== DOUBLE PRESS DETECTED ===")
                        toggleRecording()
                        volumeDownPressCount = 0
                        lastVolumeDownTime = 0
                        return true // Consume the event
                    }
                } else {
                    Log.d(TAG, "First press or timeout exceeded, resetting counter")
                    volumeDownPressCount = 0
                }

                lastVolumeDownTime = currentTime
            } else if (event.action == KeyEvent.ACTION_UP) {
                Log.v(TAG, "Volume DOWN released")
            }
        }

        return super.onKeyEvent(event)
    }

    private fun toggleRecording() {
        val intent = Intent(this, RecordingForegroundService::class.java).apply {
            action = if (RecordingForegroundService.isRecording) {
                RecordingForegroundService.ACTION_STOP_RECORDING
            } else {
                RecordingForegroundService.ACTION_START_RECORDING
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
                Log.d(TAG, "Started foreground service with action: ${intent.action}")
            } else {
                startService(intent)
                Log.d(TAG, "Started service with action: ${intent.action}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording service", e)
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.w(TAG, "=== VolumeButtonTriggerService UNBOUND ===")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "=== VolumeButtonTriggerService DESTROYED ===")
    }

    companion object {
        private const val TAG = "VolumeButtonTrigger"
    }
}
