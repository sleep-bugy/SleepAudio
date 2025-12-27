package org.lineageos.sleepaudio.ui

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import org.lineageos.sleepaudio.R
import org.lineageos.sleepaudio.service.SleepAudioController
import org.lineageos.sleepaudio.utils.Constants

class EqualizerActivity : AppCompatActivity() {

    private lateinit var controller: SleepAudioController
    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_equalizer)
        controller = SleepAudioController.getInstance(this)

        setupEnhancementSliders()
        setupEqualizer()
    }

    private fun setupEnhancementSliders() {
        // Bass
        val seekBass = findViewById<SeekBar>(R.id.seek_bass)
        seekBass.progress = prefs.getInt(Constants.KEY_BASS_STRENGTH, 50)
        seekBass.setOnSeekBarChangeListener(object : SimpleSeekBarListener() {
            override fun onProgressChanged(p0: SeekBar?, val: Int, p2: Boolean) {
                controller.setBassStrength(val)
            }
            override fun onStopTrackingTouch(p0: SeekBar?) {
                prefs.edit().putInt(Constants.KEY_BASS_STRENGTH, p0?.progress ?: 50).apply()
            }
        })

        // Virtualizer
        val seekVirt = findViewById<SeekBar>(R.id.seek_virt)
        seekVirt.progress = prefs.getInt(Constants.KEY_VIRTUALIZER_STRENGTH, 25)
        seekVirt.setOnSeekBarChangeListener(object : SimpleSeekBarListener() {
            override fun onProgressChanged(p0: SeekBar?, val: Int, p2: Boolean) {
                controller.setVirtualizerStrength(val)
            }
            override fun onStopTrackingTouch(p0: SeekBar?) {
                prefs.edit().putInt(Constants.KEY_VIRTUALIZER_STRENGTH, p0?.progress ?: 25).apply()
            }
        })

        // Dialogue
        val seekDialog = findViewById<SeekBar>(R.id.seek_dialog)
        seekDialog.progress = prefs.getInt(Constants.KEY_DIALOGUE_AMOUNT, 50)
        seekDialog.setOnSeekBarChangeListener(object : SimpleSeekBarListener() {
            override fun onProgressChanged(p0: SeekBar?, val: Int, p2: Boolean) {
                // Dialog uses complex recalc
                prefs.edit().putInt(Constants.KEY_DIALOGUE_AMOUNT, val).apply()
                controller.checkAndApplyAll()
            }
        })
    }

    private fun setupEqualizer() {
        val container = findViewById<LinearLayout>(R.id.eq_container)
        val freqs = arrayOf("32", "64", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

        for (i in freqs.indices) {
            val view = layoutInflater.inflate(R.layout.item_eq_band, container, false)
            val text = view.findViewById<TextView>(R.id.band_freq)
            val seek = view.findViewById<SeekBar>(R.id.band_seek)
            
            text.text = "${freqs[i]} Hz"
            
            // Read saved gain (stored as int * 10)
            // Range: -150 to +150. Center at 150 for SeekBar (0-300)
            val savedGain = prefs.getInt("${Constants.KEY_GEQ_PREFIX}$i", 0)
            seek.progress = savedGain + 150

            seek.setOnSeekBarChangeListener(object : SimpleSeekBarListener() {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    val gain = p1 - 150 // Convert back to -150..150
                    controller.setGeqBand(i, gain)
                }
                
                override fun onStopTrackingTouch(p0: SeekBar?) {
                     val gain = (p0?.progress ?: 150) - 150
                     prefs.edit().putInt("${Constants.KEY_GEQ_PREFIX}$i", gain).apply()
                }
            })
            
            container.addView(view)
        }
    }

    open class SimpleSeekBarListener : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {}
        override fun onStartTrackingTouch(p0: SeekBar?) {}
        override fun onStopTrackingTouch(p0: SeekBar?) {}
    }
}
