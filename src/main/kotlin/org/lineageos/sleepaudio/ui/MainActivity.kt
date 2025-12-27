package org.lineageos.sleepaudio.ui

import android.os.Bundle
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.android.settingslib.collapsingtoolbar.R as CollapsingR

class MainActivity : CollapsingToolbarBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (savedInstanceState == null) {
            fragmentManager.beginTransaction()
                .replace(CollapsingR.id.content_frame, SleepAudioFragment())
                .commit()
        }
    }
}
