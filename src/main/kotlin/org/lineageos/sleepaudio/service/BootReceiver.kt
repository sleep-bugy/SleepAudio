package org.lineageos.sleepaudio.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.lineageos.sleepaudio.utils.Constants

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(Constants.TAG, "Boot received, starting AudioService...")
            context.startForegroundService(Intent(context, AudioService::class.java))
        }
    }
}
