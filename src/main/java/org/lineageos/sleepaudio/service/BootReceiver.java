package org.lineageos.sleepaudio.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            context.startForegroundService(new Intent(context, AudioService.class));
            Log.d("SleepAudio", "Boot complete, service requested.");
        }
    }
}
