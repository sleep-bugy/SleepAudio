package org.lineageos.sleepaudio.ui

import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import org.lineageos.sleepaudio.R
import org.lineageos.sleepaudio.service.AudioService
import org.lineageos.sleepaudio.utils.Constants

class SleepAudioFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.sleep_audio_settings, rootKey)
        
        // Check permission and disable toggle if no root
        val hasPerm = requireContext().checkCallingOrSelfPermission("android.permission.MODIFY_AUDIO_SETTINGS_PRIVILEGED") == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        val enableSwitch = findPreference<SwitchPreference>(Constants.KEY_ENABLE)
        
        if (!hasPerm) {
            // Disable toggle and show why
            enableSwitch?.isEnabled = false
            enableSwitch?.summary = "Requires System App installation (Root/Magisk)"
        } else {
            // Auto-start service when "Use SleepAudio" is toggled (only if permission granted)
            enableSwitch?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue == true) {
                    requireContext().startForegroundService(Intent(requireContext(), AudioService::class.java))
                }
                true
            }
        }
    }
    
    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.setPadding(32, 0, 32, 0) // Explicit 32px padding (approx 12dp) to prevent flush left
        listView.clipToPadding = false
    }
    
    override fun onResume() {
        super.onResume()
        // Ensure service is running if enabled
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        if (prefs.getBoolean(Constants.KEY_ENABLE, false)) {
            requireContext().startForegroundService(Intent(requireContext(), AudioService::class.java))
        }
    }
}
