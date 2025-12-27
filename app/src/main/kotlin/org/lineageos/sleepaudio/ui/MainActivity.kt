package org.lineageos.sleepaudio.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.lineageos.sleepaudio.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, SleepAudioFragment())
                .commit()
        }
        
        checkRootAccess()
        
        // Critical: Start service if enabled
        val prefs = getSharedPreferences("${packageName}_preferences", MODE_PRIVATE)
        if (prefs.getBoolean("sleep_audio_enable", false)) {
            startForegroundService(android.content.Intent(this, org.lineageos.sleepaudio.service.AudioService::class.java))
        }
    }

    private fun checkRootAccess() {
        val hasPerm = checkCallingOrSelfPermission("android.permission.MODIFY_AUDIO_SETTINGS_PRIVILEGED") == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasPerm) {
             androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Root Access Required")
                .setMessage("SleepAudio requires System privileges to modify global audio (Session 0). \n\nPlease install this APK as a System App module via Magisk/KernelSU to function correctly.")
                .setPositiveButton("I Understand", null)
                .setCancelable(false)
                .show()
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_backup_restore -> {
                startActivity(android.content.Intent(this, BackupRestoreActivity::class.java))
                true
            }
            R.id.action_about -> {
                startActivity(android.content.Intent(this, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
