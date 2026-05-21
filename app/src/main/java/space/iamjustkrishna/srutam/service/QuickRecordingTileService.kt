package space.iamjustkrishna.srutam.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class QuickRecordingTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        Log.d(TAG, "Quick Settings Tile clicked")

        if (RecordingForegroundService.isRecording) {
            stopRecording()
        } else {
            startRecording()
        }

        updateTileState()
    }

    private fun startRecording() {
        val intent = Intent(this, RecordingForegroundService::class.java).apply {
            action = RecordingForegroundService.ACTION_START_RECORDING
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
                Log.d(TAG, "Started recording via Quick Settings")
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording service", e)
        }
    }

    private fun stopRecording() {
        val intent = Intent(this, RecordingForegroundService::class.java).apply {
            action = RecordingForegroundService.ACTION_STOP_RECORDING
        }

        try {
            startService(intent)
            Log.d(TAG, "Stopped recording via Quick Settings")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording service", e)
        }
    }

    private fun updateTileState() {
        qsTile?.apply {
            state = if (RecordingForegroundService.isRecording) {
                Tile.STATE_ACTIVE
            } else {
                Tile.STATE_INACTIVE
            }

            label = if (RecordingForegroundService.isRecording) {
                "Recording..."
            } else {
                "Voice Record"
            }

            updateTile()
        }
    }

    companion object {
        private const val TAG = "QuickRecordingTile"
    }
}
