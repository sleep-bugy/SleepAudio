package org.lineageos.sleepaudio.service

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.DynamicsProcessing.Config
import android.media.audiofx.DynamicsProcessing.Limiter
import android.media.audiofx.DynamicsProcessing.Mbc
import android.media.audiofx.DynamicsProcessing.Eq
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
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        const val TAG = "SleepAudioController"
        const val CHANNEL_COUNT = 2
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

        // Run initialization on background thread to prevent Main Thread freeze/ANR
        Thread {
            try {
                // Register Device Callback (Safe to call from any thread usually, updates usually happen on Handler)
                // We wrap this too just in case
                audioManager.registerAudioDeviceCallback(deviceCallback, null)
                refreshOutputDevice()

                Log.d(TAG, "Attempting to initialize DynamicsProcessing on Background Thread...")
                
                // DynamicsProcessing Init using Compat
                if (dynamicsProcessing == null) {
                    try {
                        val config = org.lineageos.sleepaudio.utils.DynamicsProcessingCompat.createConfig(
                            0 /* VARIANT_FAVOR_FREQUENCY_RESOLUTION */,
                            CHANNEL_COUNT,
                            true, 2, // PreEq
                            true, 2, // Mbc
                            true, 10, // PostEq
                            true // Limiter
                        )
                        
                        if (config != null) {
                            dynamicsProcessing = DynamicsProcessing(0, globalSessionId, config).apply {
                                enabled = true
                            }
                        } else {
                            Log.e(TAG, "Failed to build DynamicsProcessing Config via Compat")
                        }
                    } catch (sec: SecurityException) {
                        Log.e(TAG, "SECURITY EXCEPTION: Missing Permission to Modify Audio Settings!", sec)
                        // Should notify user via Notification or Toast logic (would need Handler for Toast)
                        return@Thread
                    } catch (e: Exception) {
                        Log.e(TAG, "Error initializing DynamicsProcessing: ${e.message}", e)
                    }
                }
                
                // Virtualizer Init
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
                Log.i(TAG, "Engine Initialized Successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Critical: Engine start failed on background thread", e)
                releaseEffects()
            }
        }.start()
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
            Log.d(TAG, "Audio Device Changed: ${if(isHeadset) "Headset" else "Speaker"}")
            checkAndApplyAll()
        }
    }

    fun checkAndApplyAll() {
        applyBass()
        applyVirtualizer()
        applyReverb()
        applyCompressor() 
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
        val preset = prefs.getString(Constants.KEY_REVERB_PRESET, "0")?.toShortOrNull() ?: 0.toShort()
        try {
            if (preset > 0) {
                 reverb.preset = preset 
            } else {
                 reverb.enabled = false
            }
        } catch (e: Exception) {}
    }

    private fun applyCompressor() {
        val dp = dynamicsProcessing ?: return
        val limiter = org.lineageos.sleepaudio.utils.DynamicsProcessingCompat.getLimiter(dp, 0) ?: return
        val enabled = prefs.getBoolean(Constants.KEY_COMPRESSOR_ENABLE, false)
        val isHiFi = isHiFiMode()
        
        limiter.isEnabled = enabled && !isHiFi
        if (enabled && !isHiFi) {
            val attack = prefs.getInt(Constants.KEY_COMPRESSOR_ATTACK, 50).toFloat()
            val release = prefs.getInt(Constants.KEY_COMPRESSOR_RELEASE, 500).toFloat()
            val ratio = prefs.getInt(Constants.KEY_COMPRESSOR_RATIO, 40) / 10.0f
            val threshold = prefs.getInt(Constants.KEY_COMPRESSOR_THRESHOLD, -10).toFloat()
            
            limiter.attackTime = attack
            limiter.releaseTime = release
            limiter.ratio = ratio
            limiter.threshold = threshold
            limiter.postGain = 0.0f - threshold
        }
        
        // Apply to all channels
        try {
            for (i in 0 until CHANNEL_COUNT) {
                 org.lineageos.sleepaudio.utils.DynamicsProcessingCompat.setLimiter(dp, i, limiter)
            }
        } catch (e: Exception) { Log.w(TAG, "Failed setLimiter loop: $e") }
    }

    // --- Realtime Setters ---

    fun setBassStrength(strength: Int) {
        // Optimized: just re-calc instead of partial update to ensure consistency
        applyBass()
    }

    fun setVirtualizerStrength(strength: Int) {
        val virt = virtualizer ?: return
        val enabled = prefs.getBoolean(Constants.KEY_VIRTUALIZER_ENABLE, false)
        if (enabled && virt.strengthSupported) {
             val param = (strength * 10).toShort()
             try { virt.setStrength(param) } catch (e: Exception) {}
        }
    }

    fun setDialogueAmount(amount: Int) {
        checkAndApplyAll() 
    }

    fun setGeqBand(bandIndex: Int, gainInt: Int) {
        // Just trigger full refresh for safety and simplicity with Compat
        applyEqAndTone()
    }

    // --- Feature Implementations ---

    private fun applyBass() {
        val dp = dynamicsProcessing ?: return
        val mbc = org.lineageos.sleepaudio.utils.DynamicsProcessingCompat.getMbc(dp, 0) ?: return
        
        val enabled = prefs.getBoolean(Constants.KEY_BASS_ENABLE, false)
        val strength = prefs.getInt(Constants.KEY_BASS_STRENGTH, 50) // 0-100
        val isHiFi = isHiFiMode()

        mbc.isEnabled = !isHiFi
        val band0 = mbc.getBand(0) // Bass Band
        
        if (enabled && !isHiFi) {
            val gainDb = (strength / 100f) * 12.0f
            band0.cutoffFrequency = 120.0f
            band0.attackTime = 10.0f
            band0.releaseTime = 80.0f
            band0.ratio = 4.0f + (strength / 20f)
            band0.threshold = -25.0f
            band0.kneeWidth = 6.0f
            band0.postGain = gainDb
        } else {
            band0.postGain = 0.0f
        }
        mbc.setBand(0, band0)
        
        val band1 = mbc.getBand(1)
        band1.cutoffFrequency = 20000.0f
        band1.postGain = 0.0f
        mbc.setBand(1, band1)
        
        try {
            for (i in 0 until CHANNEL_COUNT) {
                 org.lineageos.sleepaudio.utils.DynamicsProcessingCompat.setMbc(dp, i, mbc)
            }
        } catch (e: Exception) { Log.w(TAG, "Failed setMbc loop: $e") }
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
            val param = if (enabled) (strength * 10).toShort() else 0.toShort()
            try { virt.setStrength(param) } catch (e: Exception) {}
        }
    }

    private fun applyEqAndTone() {
        val dp = dynamicsProcessing ?: return
        val eq = org.lineageos.sleepaudio.utils.DynamicsProcessingCompat.getPostEq(dp, 0) ?: return
        eq.isEnabled = true
        
        val profile = prefs.getInt(Constants.KEY_PROFILE, Constants.PROFILE_DYNAMIC)
        val bandCount = eq.bandCount
        val gains = FloatArray(bandCount)

        when (profile) {
            Constants.PROFILE_MUSIC -> { gains[0]=3f; gains[1]=2f; gains[8]=2f; gains[9]=3f }
            Constants.PROFILE_MOVIE -> { gains[0]=5f; gains[1]=3f; gains[9]=2f }
            Constants.PROFILE_GAME -> { gains[5]=4f; gains[6]=5f; gains[7]=4f }
            Constants.PROFILE_SLEEP -> { 
                // Warm Filter: Cut Highs
                if (bandCount >= 10) {
                   gains[6] = -3.0f // 2kHz
                   gains[7] = -6.0f // 4kHz
                   gains[8] = -9.0f // 8kHz
                   gains[9] = -12.0f // 16kHz
                }
            }
            Constants.PROFILE_HIFI -> { gains[9]=2f } 
            Constants.PROFILE_CUSTOM -> {
                for (i in 0 until bandCount) {
                    val key = "${Constants.KEY_GEQ_PREFIX}$i"
                    gains[i] = prefs.getInt(key, 0) / 10.0f 
                }
            }
        }

        val dialogEnabled = prefs.getBoolean(Constants.KEY_DIALOGUE_ENABLE, false)
        if (dialogEnabled && !isHiFiMode()) {
            val amount = prefs.getInt(Constants.KEY_DIALOGUE_AMOUNT, 50)
            val boost = (amount / 100f) * 6.0f 
            if (bandCount >= 7) {
                gains[4] += boost * 0.5f // 500
                gains[5] += boost      // 1k
                gains[6] += boost * 0.8f // 2k
            }
        }

        for (i in 0 until bandCount) {
            val band = eq.getBand(i)
            val freq = 32.0f * 2.0.pow(i).toFloat()
            band.cutoffFrequency = freq
            
            var finalGain = gains[i]
            if (finalGain > 15f) finalGain = 15f
            if (finalGain < -15f) finalGain = -15f
            
            band.gain = finalGain
            eq.setBand(i, band)
        }
        
        try {
            for (i in 0 until CHANNEL_COUNT) {
                 org.lineageos.sleepaudio.utils.DynamicsProcessingCompat.setPostEq(dp, i, eq)
            }
        } catch (e: Exception) { Log.w(TAG, "Failed setPostEq loop: $e") }
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
