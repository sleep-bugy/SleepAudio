package org.lineageos.sleepaudio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.DynamicsProcessing.*
import android.media.audiofx.Virtualizer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.preference.PreferenceManager
import org.lineageos.sleepaudio.R
import org.lineageos.sleepaudio.utils.Constants
import kotlin.math.pow

class AudioService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val globalSessionId = 0
    private lateinit var prefs: SharedPreferences
    private var dynamicsProcessing: DynamicsProcessing? = null
    private var virtualizer: Virtualizer? = null
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            startForeground(1, createNotification())
            isRunning = true
        }
        // Delayed init to avoid conflicts with Dolby/Viper
        handler.postDelayed({ initEngine() }, 2000)
        return START_STICKY
    }

    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        releaseEffects()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            Constants.CHANNEL_ID,
            "SleepAudio Engine",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return Notification.Builder(this, Constants.CHANNEL_ID)
            .setContentTitle("SleepAudio Active")
            .setContentText("Enhancing audio stream")
            .setSmallIcon(R.drawable.ic_qs_tile)
            .build()
    }

    @Synchronized
    private fun initEngine() {
        if (!prefs.getBoolean(Constants.KEY_ENABLE, false)) {
            releaseEffects()
            return
        }

        try {
            // 1. DynamicsProcessing (Core Engine)
            if (dynamicsProcessing == null) {
                // Config: 2 Channels, PreEq, MBC, PostEq, Limiter
                val builder = Config.Builder(
                    Config.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2, true, true, true, true
                )
                builder.setPreferredFrameDuration(10.0f)
                builder.setMbcBandCount(2) // Low / High
                builder.setPostEqBandCount(10) // 10-Band GEQ
                builder.setPreEqBandCount(2) // Tone shaping

                dynamicsProcessing = DynamicsProcessing(0, globalSessionId, builder.build()).apply {
                    enabled = true
                }
            }

            // 2. Virtualizer (Spatial)
            if (virtualizer == null) {
                try {
                    virtualizer = Virtualizer(0, globalSessionId).apply {
                        enabled = true
                    }
                } catch (e: Exception) {
                    Log.w(Constants.TAG, "Virtualizer init failed: ${e.message}")
                }
            }

            Log.i(Constants.TAG, "Engine Initialized successfully")
            applyAllSettings()

        } catch (e: Exception) {
            Log.e(Constants.TAG, "Critical: Engine start failed", e)
            releaseEffects()
        }
    }

    private fun applyAllSettings() {
        if (dynamicsProcessing == null) return

        val profile = prefs.getInt(Constants.KEY_PROFILE, Constants.PROFILE_DYNAMIC)
        val isHiFi = profile == Constants.PROFILE_HIFI

        applyDynamics(isHiFi)
        applyVirtualizer(isHiFi)
    }

    private fun applyVirtualizer(isHiFi: Boolean) {
        if (isHiFi) {
            virtualizer?.enabled = false
            return
        }
        
        virtualizer?.enabled = true
        val enabled = prefs.getBoolean(Constants.KEY_VIRTUALIZER_ENABLE, false)
        val strength = if (enabled) 1000.toShort() else 0.toShort()
        
        try {
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(strength)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun applyDynamics(isHiFi: Boolean) {
        val dp = dynamicsProcessing ?: return

        // 1. MBC (Bass Enhancer)
        // Band 0: Bass (< 150Hz), Band 1: Rest
        val mbc = dp.mbc
        if (mbc != null) {
            mbc.enabled = !isHiFi // Disable compression in Hi-Fi mode
            
            val bassEnabled = prefs.getBoolean(Constants.KEY_BASS_ENABLE, false)
            val bassStrength = prefs.getInt(Constants.KEY_BASS_STRENGTH, 50) 
            // Map 0-100 strength to dB (0 to 9dB)
            val bassGain = if (bassEnabled) (bassStrength / 100f) * 9.0f else 0.0f

            // Bass Band
            val bassBand = mbc.getBand(0)
            bassBand.cutoffFrequency = 150.0f
            if (bassEnabled) {
                bassBand.attackTime = 10.0f
                bassBand.releaseTime = 100.0f
                bassBand.ratio = 4.0f // Compress for punch
                bassBand.threshold = -20.0f
                bassBand.kneeWidth = 6.0f
                bassBand.postGain = bassGain
            } else {
                bassBand.postGain = 0.0f
            }
            mbc.setBand(0, bassBand)

            // High Band (Flat)
            val highBand = mbc.getBand(1)
            highBand.cutoffFrequency = 20000.0f
            highBand.postGain = 0.0f
            mbc.setBand(1, highBand)

            dp.mbc = mbc
        }

        // 2. Limiter (Volume Leveler)
        val limiter = dp.limiter
        if (limiter != null) {
            val volLeveler = prefs.getBoolean(Constants.KEY_VOLUME_LEVELER, false)
            limiter.enabled = volLeveler && !isHiFi
            
            if (limiter.enabled) {
                limiter.attackTime = 1.0f
                limiter.releaseTime = 60.0f
                limiter.threshold = -2.0f
                limiter.ratio = 10.0f
                limiter.postGain = 2.0f // Makeup
            }
            dp.limiter = limiter
        }

        // 3. PostEq (Graphic EQ + Profile Tone)
        applyPostEq(dp, profile = prefs.getInt(Constants.KEY_PROFILE, Constants.PROFILE_DYNAMIC))
    }

    private fun applyPostEq(dp: DynamicsProcessing, profile: Int) {
        val postEq = dp.postEq ?: return
        postEq.enabled = true
        
        val bandCount = postEq.bandCount
        // Target Gains
        val gains = FloatArray(bandCount)

        // A. Profile Base Curve
        when (profile) {
            Constants.PROFILE_MUSIC -> {
                // V-Shape
                gains[0] = 3f; gains[1] = 2f; gains[8] = 2f; gains[9] = 3f
            }
            Constants.PROFILE_GAME -> {
                // Footsteps (High Mids)
                gains[5] = 4f; gains[6] = 5f; gains[7] = 4f
            }
            Constants.PROFILE_MOVIE -> {
                // Cinematic (Sub-bass + Air)
                gains[0] = 5f; gains[9] = 2f
            }
            Constants.PROFILE_HIFI -> {
                // Flat + Air
                gains[9] = 2f
            }
        }

        // B. Dialogue Enhancer Overlay
        if (prefs.getBoolean(Constants.KEY_DIALOGUE_ENABLE, false) && profile != Constants.PROFILE_HIFI) {
            // Boost Vocal Range (1k - 3k)
            gains[5] += 3f // ~1k
            gains[6] += 2f // ~2k
        }

        // C. Apply to Bands
        for (i in 0 until bandCount) {
            val band = postEq.getBand(i)
            // Logarithmic Freq: 32, 64, 125, 250, 500, 1k, 2k, 4k, 8k, 16k
            val freq = 32.0f * 2.0.pow(i).toFloat()
            band.cutoffFrequency = freq
            
            // Clamp gains
            var finalGain = gains[i]
            if (finalGain > 12.0f) finalGain = 12.0f
            if (finalGain < -12.0f) finalGain = -12.0f
            
            band.gain = finalGain
            postEq.setBand(i, band)
        }
        dp.postEq = postEq
    }

    private fun releaseEffects() {
        dynamicsProcessing?.release()
        dynamicsProcessing = null
        virtualizer?.release()
        virtualizer = null
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == Constants.KEY_ENABLE) {
            initEngine()
        } else {
            applyAllSettings()
        }
    }
}
