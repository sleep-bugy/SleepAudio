package org.lineageos.sleepaudio.ui;

import android.os.Bundle;
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

public class MainActivity extends CollapsingToolbarBaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame, new SleepAudioFragment())
                .commit();
        }
    }
}
