package org.lineageos.sleepaudio.presets

import android.content.Context
import android.content.SharedPreferences
import org.lineageos.sleepaudio.utils.Constants

/**
 * Represents an audio preset with all configurable settings
 */
data class Preset(
    val name: String,
    val isBuiltIn: Boolean = false,
    
    // Master controls
    val enabled: Boolean = true,
    
    // Bass & Virtualizer
    val bassEnabled: Boolean = true,
    val bassStrength: Int = 50,
    val virtualizerEnabled: Boolean = true,
    val virtualizerStrength: Int = 50,
    
    // Dialogue Enhancement
    val dialogueEnabled: Boolean = false,
    val dialogueAmount: Int = 50,
    
    // Volume Leveler
    val volumeLevelerEnabled: Boolean = false,
    
    // Stereo Widening
    val stereoWidening: Int = 50,
    
    // 10-Band Equalizer
    val eqBands: IntArray = IntArray(10) { 0 },
    
    // Reverb
    val reverbEnabled: Boolean = false,
    val reverbPreset: Int = 0,
    
    // Compressor
    val compressorEnabled: Boolean = false,
    val compressorAttack: Int = 50,
    val compressorRelease: Int = 200,
    val compressorRatio: Int = 30,
    val compressorThreshold: Int = -20
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Preset
        return name == other.name && eqBands.contentEquals(other.eqBands)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + eqBands.contentHashCode()
        return result
    }
}
