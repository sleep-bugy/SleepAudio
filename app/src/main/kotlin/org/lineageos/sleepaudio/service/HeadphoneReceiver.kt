package org.lineageos.sleepaudio.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.widget.Toast
import org.lineageos.sleepaudio.presets.PresetManager
import org.lineageos.sleepaudio.utils.Constants

class HeadphoneReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_HEADSET_PLUG -> {
                val state = intent.getIntExtra("state", -1)
                handleHeadphoneState(context, state == 1)
            }
            BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                handleHeadphoneState(context, state == BluetoothProfile.STATE_CONNECTED)
            }
        }
    }
    
    private fun handleHeadphoneState(context: Context, isConnected: Boolean) {
        val prefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
        val autoDetectEnabled = prefs.getBoolean("headphone_auto_detect", false)
        
        if (!autoDetectEnabled) return
        
        val headphonePreset = prefs.getString("headphone_preset", "Music") ?: "Music"
        val speakerPreset = prefs.getString("speaker_preset", "Music") ?: "Music"
        
        if (isConnected) {
            // Headphone connected
            PresetManager.setCurrentPreset(context, headphonePreset)
            Toast.makeText(context, "Headphone detected: $headphonePreset", Toast.LENGTH_SHORT).show()
            
            // Auto-enable SleepAudio if disabled
            val enabled = prefs.getBoolean(Constants.KEY_ENABLE, false)
            if (!enabled) {
                prefs.edit().putBoolean(Constants.KEY_ENABLE, true).apply()
            }
            
            // Start service
            val serviceIntent = Intent(context, AudioService::class.java)
            context.startForegroundService(serviceIntent)
        } else {
            // Headphone disconnected
            PresetManager.setCurrentPreset(context, speakerPreset)
            Toast.makeText(context, "Speaker mode: $speakerPreset", Toast.LENGTH_SHORT).show()
            
            // Restart service to apply changes
            val serviceIntent = Intent(context, AudioService::class.java)
            context.stopService(serviceIntent)
            
            val enabled = prefs.getBoolean(Constants.KEY_ENABLE, false)
            if (enabled) {
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
