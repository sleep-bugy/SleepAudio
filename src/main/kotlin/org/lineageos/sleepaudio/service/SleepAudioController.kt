package org.lineageos.sleepaudio.service

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.DynamicsProcessing.*
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.util.Log
import androidx.preference.PreferenceManager
import org.lineageos.sleepaudio.utils.Constants
import kotlin.math.pow

class SleepAudioController private constructor(private val context: Context) {

    private val globalSessionId = 0
    private var dynamicsProcessing: DynamicsProcessing? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    private val audioManager = context.getSystemService(android.media.AudioManager::class.java)
    private var currentDeviceType = 0 // 0: Speaker, 1: Headset/BT

    companion object {
        const val TAG = "SleepAudioController"
        @Volatile private var instance: SleepAudioController? = null

        fun getInstance(context: Context): SleepAudioController =
            instance ?: synchronized(this) {
                instance ?: SleepAudioController(context).also { instance = it }
            }
    }

    @Synchronized
    fun initEngine() {
        if (!prefs.getBoolean(Constants.KEY_ENABLE, false)) {
            releaseEffects()
            return
        }
        
        // Register Device Callback
        audioManager.registerAudioDeviceCallback(deviceCallback, null)
        refreshOutputDevice()

        try {
            if (dynamicsProcessing == null) {
                // ... (Existing Init Logic) ...
                val builder = Config.Builder(
                    Config.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2, true, true, true, true
                )
                builder.setPreferredFrameDuration(10.0f)
                builder.setMbcBandCount(2)
                builder.setPostEqBandCount(10)
                builder.setPreEqBandCount(2)

                dynamicsProcessing = DynamicsProcessing(0, globalSessionId, builder.build()).apply {
                    enabled = true
                }
            }
            // ... (Virtualizer Init) ...
             if (virtualizer == null) {
                try {
                    virtualizer = Virtualizer(0, globalSessionId).apply {
                        enabled = true
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Virtualizer init failed: ${e.message}")
                }
            }
            
            if (presetReverb == null) {
                try {
                    presetReverb = PresetReverb(0, globalSessionId).apply {
                        enabled = true
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Reverb init failed: ${e.message}")
                }
            }

            checkAndApplyAll()
            Log.i(TAG, "Engine Initialized")

        } catch (e: Exception) {
            Log.e(TAG, "Critical: Engine start failed", e)
            releaseEffects()
        }
    }

    private val deviceCallback = object : android.media.AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<android.media.AudioDeviceInfo>?) = refreshOutputDevice()
        override fun onAudioDevicesRemoved(removedDevices: Array<android.media.AudioDeviceInfo>?) = refreshOutputDevice()
    }

    private fun refreshOutputDevice() {
        val devices = audioManager.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)
        var isHeadset = false
        for (device in devices) {
            val type = device.type
            if (type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                isHeadset = true
                break
            }
        }
        
        val newType = if (isHeadset) 1 else 0
        if (newType != currentDeviceType) {
            currentDeviceType = newType
            // Auto-Switch Profile Logic could go here
            // For now, we just log. Future: loadProfile(currentDeviceType)
            Log.d(TAG, "Audio Device Changed: ${if(isHeadset) "Headset" else "Speaker"}")
            
            // Re-apply to adjust for device (e.g. disable Virtualizer on speaker if desired)
            checkAndApplyAll()
        }
    }

    fun checkAndApplyAll() {
        if (dynamicsProcessing == null) return
        
        // Safety: Disable Virtualizer on Speaker (common practice)
        // val forceDisableVirt = currentDeviceType == 0 
        
        applyBass()
        applyVirtualizer()
        applyReverb()
        applyCompressor() // Replaces simple Volume Leveler
        applyEqAndTone()
    }
    
    // --- Advanced Features ---
    
    private fun applyReverb() {
        val reverb = presetReverb ?: return
        val enabled = prefs.getBoolean(Constants.KEY_REVERB_ENABLE, false)
        if (!enabled) {
            reverb.enabled = false
            return
        }
        
        reverb.enabled = true
        // Map 0-5 to PresetReverb Constants
        // 0: None, 1: SmallRoom, 2: MedRoom, 3: LargeRoom, 4: Hall, 5: Plate
        val preset = prefs.getString(Constants.KEY_REVERB_PRESET, "0")?.toShort() ?: 0
        try {
            if (preset > 0) {
                 reverb.preset = preset 
            } else {
                 reverb.enabled = false
            }
        } catch (e: Exception) {}
    }

    private fun applyCompressor() {
        // Advanced DRC implementing Limiter stage
        val dp = dynamicsProcessing ?: return
        val limiter = dp.limiter ?: return
        val enabled = prefs.getBoolean(Constants.KEY_COMPRESSOR_ENABLE, false)
        
        limiter.enabled = enabled
        if (enabled) {
            val attack = prefs.getInt(Constants.KEY_COMPRESSOR_ATTACK, 50).toFloat() // ms
            val release = prefs.getInt(Constants.KEY_COMPRESSOR_RELEASE, 500).toFloat() // ms
            val ratio = prefs.getInt(Constants.KEY_COMPRESSOR_RATIO, 40) / 10.0f // stored x10
            val threshold = prefs.getInt(Constants.KEY_COMPRESSOR_THRESHOLD, -10).toFloat() // dB
            
            limiter.attackTime = attack
            limiter.releaseTime = release
            limiter.ratio = ratio
            limiter.threshold = threshold
            limiter.postGain = 0.0f - threshold // Auto-makeup gain approximation
        }
        dp.limiter = limiter
    }

    // --- Realtime Setters ---

    fun setBassStrength(strength: Int) {
        val dp = dynamicsProcessing ?: return
        val mbc = dp.mbc ?: return
        val band0 = mbc.getBand(0)
        
        // Map 0-100 -> 0-12dB
        val gainDb = (strength / 100f) * 12.0f
        band0.postGain = gainDb
        band0.ratio = 4.0f + (strength / 20f)
        mbc.setBand(0, band0)
        dp.mbc = mbc
    }

    fun setVirtualizerStrength(strength: Int) {
        val virt = virtualizer ?: return
        if (virt.strengthSupported) {
             val param = (strength * 10).toShort()
             try { virt.setStrength(param) } catch (e: Exception) {}
        }
    }

    fun setDialogueAmount(amount: Int) {
        // Dialogue is complex (PostEQ), easier to just trigger full re-calc since it's not super realtime critical
        // But for smooth slider, we can try. 
        // Optimized: just re-apply Eq/Tone logic
        checkAndApplyAll() 
    }

    fun setGeqBand(bandIndex: Int, gainInt: Int) {
        // gainInt is -150 to +150 (representing -15.0 to +15.0 dB)
        val dp = dynamicsProcessing ?: return
        val eq = dp.postEq ?: return
        val band = eq.getBand(bandIndex)
        band.gain = gainInt / 10.0f
        eq.setBand(bandIndex, band)
        dp.postEq = eq
    }

    // --- Feature Implementations ---

    private fun applyBass() {
        val dp = dynamicsProcessing ?: return
        val mbc = dp.mbc ?: return
        
        val enabled = prefs.getBoolean(Constants.KEY_BASS_ENABLE, false)
        val strength = prefs.getInt(Constants.KEY_BASS_STRENGTH, 50) // 0-100
        val isHiFi = isHiFiMode()

        mbc.enabled = !isHiFi
        val band0 = mbc.getBand(0) // Bass Band
        
        if (enabled && !isHiFi) {
            // Map 0-100 -> 0-12dB gain
            val gainDb = (strength / 100f) * 12.0f
            
            band0.cutoffFrequency = 120.0f 
            band0.attackTime = 10.0f
            band0.releaseTime = 80.0f
            band0.ratio = 4.0f + (strength / 20f) // Dynamic ratio
            band0.threshold = -25.0f
            band0.kneeWidth = 6.0f
            band0.postGain = gainDb
        } else {
            band0.postGain = 0.0f
        }
        mbc.setBand(0, band0)
        
        // Ensure high band is flat
        val band1 = mbc.getBand(1)
        band1.cutoffFrequency = 20000.0f
        band1.postGain = 0.0f
        mbc.setBand(1, band1)
        
        dp.mbc = mbc
    }

    private fun applyVirtualizer() {
        val virt = virtualizer ?: return
        val enabled = prefs.getBoolean(Constants.KEY_VIRTUALIZER_ENABLE, false)
        val strength = prefs.getInt(Constants.KEY_VIRTUALIZER_STRENGTH, 25) // 0-100
        val isHiFi = isHiFiMode()
        
        if (isHiFi) {
            virt.enabled = false
            return
        }

        virt.enabled = true
        if (virt.strengthSupported) {
            // Map 0-100 -> 0-1000
            val param = if (enabled) (strength * 10).toShort() else 0
            try { virt.setStrength(param) } catch (e: Exception) {}
        }
    }

    private fun applyVolumeLeveler() {
        val dp = dynamicsProcessing ?: return
        val limiter = dp.limiter ?: return
        val enabled = prefs.getBoolean(Constants.KEY_VOLUME_LEVELER, false)
        val isHiFi = isHiFiMode()

        limiter.enabled = enabled && !isHiFi
        if (limiter.enabled) {
            limiter.attackTime = 1.0f
            limiter.releaseTime = 60.0f
            limiter.threshold = -2.0f
            limiter.ratio = 10.0f
            limiter.postGain = 1.5f
        }
        dp.limiter = limiter
    }

    private fun applyEqAndTone() {
        val dp = dynamicsProcessing ?: return
        val eq = dp.postEq ?: return
        eq.enabled = true
        
        val profile = prefs.getInt(Constants.KEY_PROFILE, Constants.PROFILE_DYNAMIC)
        val bandCount = eq.bandCount
        val gains = FloatArray(bandCount)

        // 1. Base Profile Curve
        when (profile) {
            Constants.PROFILE_MUSIC -> { gains[0]=3f; gains[1]=2f; gains[8]=2f; gains[9]=3f }
            Constants.PROFILE_MOVIE -> { gains[0]=5f; gains[1]=3f; gains[9]=2f }
            Constants.PROFILE_GAME -> { gains[5]=4f; gains[6]=5f; gains[7]=4f }
            Constants.PROFILE_HIFI -> { gains[9]=2f } // Tiny air
            Constants.PROFILE_CUSTOM -> {
                // Read manual GEQ bands
                for (i in 0 until bandCount) {
                    val key = "${Constants.KEY_GEQ_PREFIX}$i"
                    gains[i] = prefs.getInt(key, 0) / 10.0f // Store as int * 10
                }
            }
        }

        // 2. Dialogue Enhancer Overlay
        val dialogEnabled = prefs.getBoolean(Constants.KEY_DIALOGUE_ENABLE, false)
        if (dialogEnabled && !isHiFiMode()) {
            val amount = prefs.getInt(Constants.KEY_DIALOGUE_AMOUNT, 50) // 0-100
            val boost = (amount / 100f) * 6.0f // Max 6dB
            // Vocal range bands (4,5,6 roughly)
            gains[4] += boost * 0.5f
            gains[5] += boost
            gains[6] += boost * 0.8f
        }

        // 3. Apply
        for (i in 0 until bandCount) {
            val band = eq.getBand(i)
            val freq = 32.0f * 2.0.pow(i).toFloat()
            band.cutoffFrequency = freq
            
            // Soft Clamp (-15 to +15 dB)
            var finalGain = gains[i]
            if (finalGain > 15f) finalGain = 15f
            if (finalGain < -15f) finalGain = -15f
            
            band.gain = finalGain
            eq.setBand(i, band)
        }
        dp.postEq = eq
    }

    private fun isHiFiMode(): Boolean {
        return prefs.getInt(Constants.KEY_PROFILE, 0) == Constants.PROFILE_HIFI
    }

    fun releaseEffects() {
        dynamicsProcessing?.release()
        dynamicsProcessing = null
        virtualizer?.release()
        virtualizer = null
    }
}
