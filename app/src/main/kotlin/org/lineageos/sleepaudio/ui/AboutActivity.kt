package org.lineageos.sleepaudio.ui

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.lineageos.sleepaudio.R
import org.lineageos.sleepaudio.service.SleepAudioController
import org.lineageos.sleepaudio.utils.Constants
import android.content.pm.PackageManager

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "About SleepAudio"

        val statusText = findViewById<TextView>(R.id.status_text)
        val permText = findViewById<TextView>(R.id.perm_text)
        val versionText = findViewById<TextView>(R.id.version_text)

        // Version Info
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            versionText.text = "Version: ${pInfo.versionName} (${pInfo.versionCode})"
        } catch (e: Exception) {
            versionText.text = "Version: Unknown"
        }

        // Permission Check
        val hasPerm = checkCallingOrSelfPermission("android.permission.MODIFY_AUDIO_SETTINGS_PRIVILEGED") == PackageManager.PERMISSION_GRANTED
        permText.text = if (hasPerm) "Permission: GRANTED (System/Root)" else "Permission: DENIED (Standard User)"
        permText.setTextColor(if (hasPerm) getColor(android.R.color.holo_green_light) else getColor(android.R.color.holo_red_light))

        // Engine Status (Approximation via Prefs)
        val enabled = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE).getBoolean(Constants.KEY_ENABLE, false)
        statusText.text = if (enabled) "Engine Target: ACTIVE" else "Engine Target: DISABLED"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
