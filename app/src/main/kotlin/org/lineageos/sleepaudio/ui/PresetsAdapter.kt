package org.lineageos.sleepaudio.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.lineageos.sleepaudio.R
import org.lineageos.sleepaudio.presets.Preset

class PresetsAdapter(
    private val presets: List<Preset>,
    private val currentPreset: String,
    private val onPresetClick: (Preset) -> Unit,
    private val onPresetDelete: (Preset) -> Unit
) : RecyclerView.Adapter<PresetsAdapter.PresetViewHolder>() {
    
    class PresetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.preset_name)
        val statusText: TextView = view.findViewById(R.id.preset_status)
        val deleteButton: ImageButton = view.findViewById(R.id.btn_delete_preset)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_preset, parent, false)
        return PresetViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: PresetViewHolder, position: Int) {
        val preset = presets[position]
        val isActive = preset.name == currentPreset
        
        holder.nameText.text = preset.name
        holder.statusText.text = when {
            isActive -> "Active"
            preset.isBuiltIn -> "Built-in"
            else -> "Custom"
        }
        
        holder.statusText.setTextColor(
            if (isActive) holder.itemView.context.getColor(android.R.color.holo_green_light)
            else holder.itemView.context.getColor(android.R.color.darker_gray)
        )
        
        holder.itemView.setOnClickListener {
            onPresetClick(preset)
        }
        
        if (preset.isBuiltIn) {
            holder.deleteButton.visibility = View.GONE
        } else {
            holder.deleteButton.visibility = View.VISIBLE
            holder.deleteButton.setOnClickListener {
                onPresetDelete(preset)
            }
        }
    }
    
    override fun getItemCount() = presets.size
}
