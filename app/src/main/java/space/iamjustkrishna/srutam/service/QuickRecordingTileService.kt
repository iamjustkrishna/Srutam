package space.iamjustkrishna.srutam.service

import android.app.PendingIntent
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
        } else if (!MicPermission.isGranted(this)) {
            // A tile may only open an activity through startActivityAndCollapse.
            val intent = MicPermission.requestIntent(this, "tile")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(
                    PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                )
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } else {
            RecordingCoordinator.requestStart(this, "tile")
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
