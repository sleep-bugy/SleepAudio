package org.lineageos.sleepaudio.ui

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.preference.PreferenceManager
import org.lineageos.sleepaudio.utils.Constants

class SleepAudioTileService : TileService() {

    override fun onClick() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val state = prefs.getBoolean(Constants.KEY_ENABLE, false)
        prefs.edit().putBoolean(Constants.KEY_ENABLE, !state).apply()
        updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val state = prefs.getBoolean(Constants.KEY_ENABLE, false)
        
        tile.state = if (state) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "SleepAudio"
        tile.subtitle = if (state) "Active" else "Off"
        tile.updateTile()
    }
}
