package org.lineageos.sleepaudio.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.lineageos.sleepaudio.R
import org.lineageos.sleepaudio.utils.BackupRestoreManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BackupRestoreActivity : AppCompatActivity() {
    
    private val importLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                importBackup(uri)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup_restore)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Backup & Restore"
        
        findViewById<Button>(R.id.btn_export).setOnClickListener {
            exportSettings()
        }
        
        findViewById<Button>(R.id.btn_import).setOnClickListener {
            selectBackupFile()
        }
        
        // Load existing backups
        loadBackupList()
    }
    
    private fun exportSettings() {
        val file = BackupRestoreManager.exportSettings(this)
        if (file != null) {
            Toast.makeText(
                this,
                "Backup saved to:\n${file.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
            loadBackupList() // Refresh list
        } else {
            Toast.makeText(this, "Failed to create backup", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun selectBackupFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        importLauncher.launch(intent)
    }
    
    private fun importBackup(uri: Uri) {
        if (BackupRestoreManager.importSettings(this, uri)) {
            Toast.makeText(this, "Settings restored successfully!\nRestart the app to apply changes.", Toast.LENGTH_LONG).show()
            
            // Optionally restart service
            val serviceIntent = Intent(this, org.lineageos.sleepaudio.service.AudioService::class.java)
            stopService(serviceIntent)
            
            val prefs = getSharedPreferences("${packageName}_preferences", MODE_PRIVATE)
            if (prefs.getBoolean("sleep_audio_enable", false)) {
                startForegroundService(serviceIntent)
            }
        } else {
            Toast.makeText(this, "Failed to restore settings.\nInvalid backup file.", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadBackupList() {
        val backups = BackupRestoreManager.getBackupFiles()
        val recyclerView = findViewById<RecyclerView>(R.id.backup_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = BackupListAdapter(backups) { file ->
            importBackup(Uri.fromFile(file))
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    // Simple adapter for backup list
    private class BackupListAdapter(
        private val backups: List<File>,
        private val onItemClick: (File) -> Unit
    ) : RecyclerView.Adapter<BackupViewHolder>() {
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): BackupViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return BackupViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: BackupViewHolder, position: Int) {
            val file = backups[position]
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            
            holder.title.text = file.name
            holder.subtitle.text = "Created: ${dateFormat.format(Date(file.lastModified()))}"
            holder.itemView.setOnClickListener {
                onItemClick(file)
            }
        }
        
        override fun getItemCount() = backups.size
    }
    
    private class BackupViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val title: android.widget.TextView = view.findViewById(android.R.id.text1)
        val subtitle: android.widget.TextView = view.findViewById(android.R.id.text2)
    }
}
