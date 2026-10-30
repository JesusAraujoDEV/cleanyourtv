package com.gathin.tvhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Process;

/**
 * Restarts Home after a companion app wrote new theme files through ThemeProvider: the launcher reads
 * its theme when the process starts, and Android brings Home straight back. Only callers holding
 * WRITE_SECURE_SETTINGS, as for ThemeProvider itself.
 */
public class ThemeReloadReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Process.killProcess(Process.myPid());
    }
}
