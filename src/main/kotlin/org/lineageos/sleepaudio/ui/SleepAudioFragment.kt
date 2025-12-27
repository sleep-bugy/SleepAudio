package org.lineageos.sleepaudio.ui

import android.os.Bundle
import androidx.preference.PreferenceFragment
import org.lineageos.sleepaudio.R

class SleepAudioFragment : PreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.sleep_audio_settings)
    }
}
