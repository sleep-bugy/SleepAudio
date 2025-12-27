package org.lineageos.sleepaudio.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import org.lineageos.sleepaudio.R

class SleepAudioFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.sleep_audio_settings, rootKey)
    }
}
