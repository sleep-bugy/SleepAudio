package org.lineageos.sleepaudio.ui;

import android.os.Bundle;
import androidx.preference.PreferenceFragment;
import org.lineageos.sleepaudio.R;

public class SleepAudioFragment extends PreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.sleep_audio_settings);
    }
}
