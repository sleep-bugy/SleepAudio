package org.lineageos.sleepaudio.data

/**
 * Represents a saved user preset.
 * @param name The display name of the preset
 * @param bandGains 10-band EQ gains (in dB)
 * @param bassStrength Bass boost level (0-100)
 * @param virtStrength Virtualizer level (0-100)
 */
data class Preset(
    val name: String,
    val bandGains: List<Int>, // 10 bands, values -15 to +15
    val bassStrength: Int,
    val virtStrength: Int
)
