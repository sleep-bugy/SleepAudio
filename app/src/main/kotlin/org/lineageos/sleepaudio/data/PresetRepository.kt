package org.lineageos.sleepaudio.data

import android.content.Context
import android.content.SharedPreferences
// Gson removed
// To avoid adding heavy dependencies like Gson/Room for a simple mod, 
// we'll use simple SharedPrefs serialization for now.

class PresetRepository(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("sleepaudio_presets", Context.MODE_PRIVATE)

    fun savePreset(preset: Preset) {
        // Format: "gain1,gain2,...|bass|virt"
        val serialized = "${preset.bandGains.joinToString(",")}|${preset.bassStrength}|${preset.virtStrength}"
        prefs.edit().putString(preset.name, serialized).apply()
    }

    fun getPreset(name: String): Preset? {
        val raw = prefs.getString(name, null) ?: return null
        return try {
            val parts = raw.split("|")
            val gains = parts[0].split(",").map { it.toInt() }
            val bass = parts[1].toInt()
            val virt = parts[2].toInt()
            Preset(name, gains, bass, virt)
        } catch (e: Exception) {
            null
        }
    }
    
    fun getAllPresetNames(): List<String> {
        return prefs.all.keys.toList()
    }
}
