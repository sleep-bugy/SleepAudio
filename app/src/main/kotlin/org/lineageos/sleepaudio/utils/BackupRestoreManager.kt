package org.lineageos.sleepaudio.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object BackupRestoreManager {
    private const val TAG = "BackupRestore"
    private const val BACKUP_VERSION = "1.0"
    private const val BACKUP_DIR = "SleepAudio"
    
    /**
     * Export all settings to JSON file
     * @return File path if successful, null otherwise
     */
    fun exportSettings(context: Context): File? {
        return try {
            val prefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
            
            // Create backup JSON
            val backup = JSONObject().apply {
                put("version", BACKUP_VERSION)
                put("app_version", getAppVersion(context))
                put("timestamp", System.currentTimeMillis())
                put("settings", prefsToJson(prefs))
            }
            
            // Save to file
            val backupFile = getBackupFile()
            FileOutputStream(backupFile).use { output ->
                output.write(backup.toString(2).toByteArray())
            }
            
            Log.d(TAG, "Settings exported to: ${backupFile.absolutePath}")
            backupFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export settings", e)
            null
        }
    }
    
    /**
     * Import settings from JSON file
     * @return true if successful, false otherwise
     */
    fun importSettings(context: Context, file: File): Boolean {
        return try {
            val json = file.readText()
            val backup = JSONObject(json)
            
            // Validate backup
            if (!validateBackup(backup)) {
                Log.e(TAG, "Invalid backup file")
                return false
            }
            
            // Restore settings
            val prefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
            val settings = backup.getJSONObject("settings")
            
            prefs.edit().apply {
                // Clear existing settings
                clear()
                
                // Restore from backup
                settings.keys().forEach { key ->
                    when (val value = settings.get(key)) {
                        is Boolean -> putBoolean(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Float -> putFloat(key, value)
                        is String -> putString(key, value)
                    }
                }
            }.apply()
            
            Log.d(TAG, "Settings imported from: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import settings", e)
            false
        }
    }
    
    /**
     * Import settings from URI (for file picker)
     */
    fun importSettings(context: Context, uri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val json = inputStream?.bufferedReader()?.use { it.readText() } ?: return false
            
            val backup = JSONObject(json)
            
            // Validate backup
            if (!validateBackup(backup)) {
                Log.e(TAG, "Invalid backup file")
                return false
            }
            
            // Restore settings
            val prefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
            val settings = backup.getJSONObject("settings")
            
            prefs.edit().apply {
                // Clear existing settings
                clear()
                
                // Restore from backup
                settings.keys().forEach { key ->
                    when (val value = settings.get(key)) {
                        is Boolean -> putBoolean(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Float -> putFloat(key, value)
                        is String -> putString(key, value)
                    }
                }
            }.apply()
            
            Log.d(TAG, "Settings imported successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import settings", e)
            false
        }
    }
    
    /**
     * Validate backup JSON structure
     */
    private fun validateBackup(backup: JSONObject): Boolean {
        return try {
            backup.has("version") && 
            backup.has("settings") &&
            backup.getJSONObject("settings").length() > 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Convert SharedPreferences to JSON
     */
    private fun prefsToJson(prefs: SharedPreferences): JSONObject {
        val json = JSONObject()
        prefs.all.forEach { (key, value) ->
            when (value) {
                is Boolean -> json.put(key, value)
                is Int -> json.put(key, value)
                is Long -> json.put(key, value)
                is Float -> json.put(key, value)
                is String -> json.put(key, value)
            }
        }
        return json
    }
    
    /**
     * Get backup file with timestamp
     */
    private fun getBackupFile(): File {
        val backupDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), BACKUP_DIR)
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(backupDir, "sleepaudio_backup_$timestamp.json")
    }
    
    /**
     * Get app version
     */
    private fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    /**
     * Get list of existing backups
     */
    fun getBackupFiles(): List<File> {
        val backupDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), BACKUP_DIR)
        if (!backupDir.exists()) {
            return emptyList()
        }
        
        return backupDir.listFiles { file ->
            file.name.startsWith("sleepaudio_backup_") && file.name.endsWith(".json")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}
