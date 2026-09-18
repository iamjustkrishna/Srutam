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

        if (RecordingCoordinator.isRecording || RecordingForegroundService.isRecording) {
            RecordingCoordinator.requestStop(this)
        } else {
            RecordingCoordinator.requestStart(this)
        }

        updateTileState()
    }

    private fun updateTileState() {
        val isRec = RecordingCoordinator.isRecording || RecordingForegroundService.isRecording
        qsTile?.apply {
            state = if (isRec) {
                Tile.STATE_ACTIVE
            } else {
                Tile.STATE_INACTIVE
            }

            label = if (isRec) {
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
