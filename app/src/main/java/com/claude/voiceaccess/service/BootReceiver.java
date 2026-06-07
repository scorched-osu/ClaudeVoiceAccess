package com.claude.voiceaccess.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.claude.voiceaccess.utils.PreferenceManager;
import com.claude.voiceaccess.utils.RootCommandRunner;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            // Re-enable accessibility service via root if needed
            PreferenceManager prefs = new PreferenceManager(context);
            if (prefs.isServiceEnabled()) {
                RootCommandRunner.enableAccessibilityService(
                        "com.claude.voiceaccess/.service.ClaudeAccessibilityService");
                // Start the voice listener
                Intent serviceIntent = new Intent(context, VoiceListenerService.class);
                context.startForegroundService(serviceIntent);
            }
        }
    }
}
