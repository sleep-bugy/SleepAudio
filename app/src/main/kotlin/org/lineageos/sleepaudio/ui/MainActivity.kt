package org.lineageos.sleepaudio.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.lineageos.sleepaudio.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, SleepAudioFragment())
                .commit()
        }
    }
}
