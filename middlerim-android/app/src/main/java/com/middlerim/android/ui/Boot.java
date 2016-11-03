package com.middlerim.android.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.middlerim.client.view.ViewEvents;


public class Boot extends BroadcastReceiver {
    public static final String ACTION_START_FROM_ACTIVITY = "ACTION_START_FROM_ACTIVITY";

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case ACTION_START_FROM_ACTIVITY:
            case Intent.ACTION_BOOT_COMPLETED:
            case "android.intent.action.QUICKBOOT_POWERON":
                Intent i = new Intent(context, BackgroundService.class);
                context.startService(i);
                return;
            case Intent.ACTION_SHUTDOWN:
            case "android.intent.action.QUICKBOOT_POWEROFF":
                ViewEvents.fireDestroy();
                return;
        }
    }
}
