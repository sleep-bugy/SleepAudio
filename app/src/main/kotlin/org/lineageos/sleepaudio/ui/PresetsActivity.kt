package org.lineageos.sleepaudio.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.lineageos.sleepaudio.R
import org.lineageos.sleepaudio.presets.PresetManager

class PresetsActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PresetsAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_presets)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Audio Presets"
        
        recyclerView = findViewById(R.id.presets_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        loadPresets()
        
        findViewById<FloatingActionButton>(R.id.fab_add_preset).setOnClickListener {
            showSavePresetDialog()
        }
    }
    
    private fun loadPresets() {
        val builtIn = PresetManager.getBuiltInPresets()
        val custom = PresetManager.getCustomPresets(this)
        val currentPreset = PresetManager.getCurrentPreset(this)
        
        adapter = PresetsAdapter(
            builtIn + custom,
            currentPreset,
            onPresetClick = { preset ->
                PresetManager.setCurrentPreset(this, preset.name)
                Toast.makeText(this, "Applied preset: ${preset.name}", Toast.LENGTH_SHORT).show()
                loadPresets() // Refresh to update selected state
                
                // Restart audio service to apply changes
                val serviceIntent = android.content.Intent(this, org.lineageos.sleepaudio.service.AudioService::class.java)
                stopService(serviceIntent)
                startForegroundService(serviceIntent)
            },
            onPresetDelete = { preset ->
                if (!preset.isBuiltIn) {
                    PresetManager.deleteCustomPreset(this, preset.name)
                    Toast.makeText(this, "Deleted preset: ${preset.name}", Toast.LENGTH_SHORT).show()
                    loadPresets()
                }
            }
        )
        
        recyclerView.adapter = adapter
    }
    
    private fun showSavePresetDialog() {
        val input = EditText(this)
        input.hint = "Preset name"
        
        AlertDialog.Builder(this)
            .setTitle("Save Current Settings")
            .setMessage("Enter a name for this preset")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    PresetManager.saveCustomPreset(this, name)
                    Toast.makeText(this, "Saved preset: $name", Toast.LENGTH_SHORT).show()
                    loadPresets()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
