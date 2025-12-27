package org.lineageos.sleepaudio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.preference.PreferenceManager
import org.lineageos.sleepaudio.R
import org.lineageos.sleepaudio.utils.Constants

class AudioService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var controller: SleepAudioController
    private lateinit var prefs: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        controller = SleepAudioController.getInstance(this)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())
        
        // Delayed init safe-guard
        handler.postDelayed({ 
            controller.initEngine()
        }, 2000)
        
        return START_STICKY
    }

    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        controller.releaseEffects()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == Constants.KEY_ENABLE) {
            controller.initEngine()
        } else {
            controller.checkAndApplyAll()
        }
    }

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
}
