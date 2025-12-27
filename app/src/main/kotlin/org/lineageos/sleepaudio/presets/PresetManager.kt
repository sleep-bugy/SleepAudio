package org.lineageos.sleepaudio.presets

import android.content.Context
import android.content.SharedPreferences
import org.lineageos.sleepaudio.utils.Constants

object PresetManager {
    private const val PREF_CURRENT_PRESET = "current_preset"
    private const val PREF_CUSTOM_PREFIX = "custom_preset_"
    
    /**
     * Get all built-in presets
     */
    fun getBuiltInPresets(): List<Preset> = listOf(
        // Music: Balanced, slight V-shape EQ
        Preset(
            name = "Music",
            isBuiltIn = true,
            bassEnabled = true,
            bassStrength = 65,
            virtualizerEnabled = true,
            virtualizerStrength = 70,
            eqBands = intArrayOf(3, 2, 1, 0, -1, -1, 0, 1, 2, 3),
            stereoWidening = 60
        ),
        
        // Movies: Enhanced dialogue, wide soundstage
        Preset(
            name = "Movies",
            isBuiltIn = true,
            bassEnabled = true,
            bassStrength = 55,
            virtualizerEnabled = true,
            virtualizerStrength = 85,
            dialogueEnabled = true,
            dialogueAmount = 70,
            eqBands = intArrayOf(2, 1, 0, 2, 3, 3, 2, 0, 1, 2),
            stereoWidening = 75,
            compressorEnabled = true
        ),
        
        // Gaming: High virtualizer for positioning, boosted footsteps
        Preset(
            name = "Gaming",
            isBuiltIn = true,
            bassEnabled = true,
            bassStrength = 45,
            virtualizerEnabled = true,
            virtualizerStrength = 90,
            eqBands = intArrayOf(1, 2, 3, 2, 1, 1, 2, 3, 2, 1),
            stereoWidening = 85
        ),
        
        // Podcast: Enhanced dialogue, reduced bass
        Preset(
            name = "Podcast",
            isBuiltIn = true,
            bassEnabled = false,
            bassStrength = 30,
            virtualizerEnabled = false,
            dialogueEnabled = true,
            dialogueAmount = 85,
            eqBands = intArrayOf(-2, -1, 0, 3, 4, 4, 3, 1, 0, -1),
            stereoWidening = 30,
            volumeLevelerEnabled = true
        ),
        
        // Bass Boost: Maximum bass, V-shaped EQ
        Preset(
            name = "Bass Boost",
            isBuiltIn = true,
            bassEnabled = true,
            bassStrength = 100,
            virtualizerEnabled = true,
            virtualizerStrength = 60,
            eqBands = intArrayOf(6, 5, 4, 2, 0, -2, 0, 2, 4, 5),
            stereoWidening = 70
        ),
        
        // Sleep: Warm filter, reduced treble
        Preset(
            name = "Sleep",
            isBuiltIn = true,
            bassEnabled = true,
            bassStrength = 40,
            virtualizerEnabled = false,
            eqBands = intArrayOf(2, 2, 1, 0, 0, -1, -2, -3, -4, -4),
            stereoWidening = 20,
            volumeLevelerEnabled = true
        )
    )
    
    /**
     * Get all custom presets saved by user
     */
    fun getCustomPresets(context: Context): List<Preset> {
        val prefs = getPrefs(context)
        val customNames = prefs.all.keys
            .filter { it.startsWith(PREF_CUSTOM_PREFIX) && it.endsWith("_name") }
            .map { it.removePrefix(PREF_CUSTOM_PREFIX).removeSuffix("_name") }
        
        return customNames.mapNotNull { id ->
            loadCustomPreset(context, id)
        }
    }
    
    /**
     * Get currently active preset name
     */
    fun getCurrentPreset(context: Context): String {
        return getPrefs(context).getString(PREF_CURRENT_PRESET, "Music") ?: "Music"
    }
    
    /**
     * Set current preset and apply settings
     */
    fun setCurrentPreset(context: Context, presetName: String) {
        val preset = findPreset(context, presetName) ?: return
        applyPreset(context, preset)
        getPrefs(context).edit().putString(PREF_CURRENT_PRESET, presetName).apply()
    }
    
    /**
     * Apply preset settings to SharedPreferences
     */
    fun applyPreset(context: Context, preset: Preset) {
        val editor = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE).edit()
        
        editor.putBoolean(Constants.KEY_ENABLE, preset.enabled)
        editor.putBoolean(Constants.KEY_BASS_ENABLE, preset.bassEnabled)
        editor.putInt(Constants.KEY_BASS_STRENGTH, preset.bassStrength)
        editor.putBoolean(Constants.KEY_VIRTUALIZER_ENABLE, preset.virtualizerEnabled)
        editor.putInt(Constants.KEY_VIRTUALIZER_STRENGTH, preset.virtualizerStrength)
        editor.putBoolean(Constants.KEY_DIALOGUE_ENABLE, preset.dialogueEnabled)
        editor.putInt(Constants.KEY_DIALOGUE_AMOUNT, preset.dialogueAmount)
        editor.putBoolean(Constants.KEY_VOLUME_LEVELER, preset.volumeLevelerEnabled)
        editor.putInt(Constants.KEY_STEREO_WIDENING, preset.stereoWidening)
        
        // EQ Bands
        preset.eqBands.forEachIndexed { index, value ->
            editor.putInt("${Constants.KEY_GEQ_PREFIX}$index", value)
        }
        
        // Reverb
        editor.putBoolean(Constants.KEY_REVERB_ENABLE, preset.reverbEnabled)
        editor.putInt(Constants.KEY_REVERB_PRESET, preset.reverbPreset)
        
        // Compressor
        editor.putBoolean(Constants.KEY_COMPRESSOR_ENABLE, preset.compressorEnabled)
        editor.putInt(Constants.KEY_COMPRESSOR_ATTACK, preset.compressorAttack)
        editor.putInt(Constants.KEY_COMPRESSOR_RELEASE, preset.compressorRelease)
        editor.putInt(Constants.KEY_COMPRESSOR_RATIO, preset.compressorRatio)
        editor.putInt(Constants.KEY_COMPRESSOR_THRESHOLD, preset.compressorThreshold)
        
        editor.apply()
    }
    
    /**
     * Save current settings as a custom preset
     */
    fun saveCustomPreset(context: Context, name: String) {
        val prefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
        val id = System.currentTimeMillis().toString()
        val editor = getPrefs(context).edit()
        
        editor.putString("${PREF_CUSTOM_PREFIX}${id}_name", name)
        editor.putBoolean("${PREF_CUSTOM_PREFIX}${id}_bass_enabled", prefs.getBoolean(Constants.KEY_BASS_ENABLE, false))
        editor.putInt("${PREF_CUSTOM_PREFIX}${id}_bass_strength", prefs.getInt(Constants.KEY_BASS_STRENGTH, 50))
        editor.putBoolean("${PREF_CUSTOM_PREFIX}${id}_virtualizer_enabled", prefs.getBoolean(Constants.KEY_VIRTUALIZER_ENABLE, false))
        editor.putInt("${PREF_CUSTOM_PREFIX}${id}_virtualizer_strength", prefs.getInt(Constants.KEY_VIRTUALIZER_STRENGTH, 50))
        // ... save all other settings
        
        editor.apply()
    }
    
    /**
     * Delete a custom preset
     */
    fun deleteCustomPreset(context: Context, presetName: String) {
        val editor = getPrefs(context).edit()
        val id = getCustomPresets(context).find { it.name == presetName }?.name ?: return
        
        editor.remove("${PREF_CUSTOM_PREFIX}${id}_name")
        // Remove all associated keys
        editor.apply()
    }
    
    private fun findPreset(context: Context, name: String): Preset? {
        return getBuiltInPresets().find { it.name == name }
            ?: getCustomPresets(context).find { it.name == name }
    }
    
    private fun loadCustomPreset(context: Context, id: String): Preset? {
        val prefs = getPrefs(context)
        val name = prefs.getString("${PREF_CUSTOM_PREFIX}${id}_name", null) ?: return null
        
        return Preset(
            name = name,
            isBuiltIn = false,
            bassEnabled = prefs.getBoolean("${PREF_CUSTOM_PREFIX}${id}_bass_enabled", false),
            bassStrength = prefs.getInt("${PREF_CUSTOM_PREFIX}${id}_bass_strength", 50),
            // ... load all settings
        )
    }
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences("sleepaudio_presets", Context.MODE_PRIVATE)
    }
}
