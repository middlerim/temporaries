package com.middlerim.android.ui;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.middlerim.client.CentralEvents;
import com.middlerim.client.CentralServer;
import com.middlerim.client.view.ViewEvents;

public class BackgroundService extends IntentService {
    private static final String TAG = Middlerim.TAG + ".BGSERV";
    private static BackgroundService instance;
    private static boolean isRunningForground;
    private LocationTracker locationTracker;

    private final CentralEvents.Listener<CentralEvents.ReceiveMessageEvent> receiveMessageEventListener = new CentralEvents.Listener<CentralEvents.ReceiveMessageEvent>() {
        @Override
        public void handle(CentralEvents.ReceiveMessageEvent receiveMessageEvent) {
            NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotifyMgr.notify(Codes.NOTIFYCATION_RECEIVE_MESSAGE,
                    new NotificationCompat.Builder(BackgroundService.this)
                            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                            .setContentTitle(BackgroundService.this.getString(R.string.title_receiving_message))
                            .setContentText("Hello World!").build());
        }
    };

    private static BackgroundService getInstance() {
        return instance;
    }

    public BackgroundService() {
        super(TAG);
    }


    private static boolean isStarted() {
        return instance != null;
    }

    public static void onResumeForground(Context ctx) {
        isRunningForground = true;
        if (isStarted()) {
            instance.onResumeForground();
        } else {
            run(ctx);
        }
    }

    private void onResumeForground() {
        CentralEvents.removeListener(receiveMessageEventListener);
        switchLocationTracker();
    }

    private static void run(Context ctx) {
        Intent intent = new Intent(ctx, Boot.class);
        intent.setAction(Boot.ACTION_START_FROM_ACTIVITY);
        ctx.sendBroadcast(intent);
    }

    public static void onPauseForground(Context ctx) {
        isRunningForground = false;
        if (isStarted()) {
            instance.onPauseForground();
        }
    }

    private void onPauseForground() {
        switchLocationTracker();
        CentralEvents.onReceiveMessage(receiveMessageEventListener);

    }

    private void switchLocationTracker() {
        if (locationTracker == null) {
            return;
        }
        locationTracker.switchTrackingMode(isRunningForground);
    }


    @Override
    public void onStart(Intent intent, int startId) {
        if (isStarted()) {
            return;
        }
        instance = this;
        super.onStart(intent, startId);
        if (!isRunningForground) {
            CentralEvents.onReceiveMessage(receiveMessageEventListener);
        }

        CentralEvents.onStarted(new CentralEvents.Listener<CentralEvents.StartedEvent>() {
            @Override
            public void handle(CentralEvents.StartedEvent startedEvent) {
                if (locationTracker == null) {
                    locationTracker = new LocationTracker();
                }
                locationTracker.start(AndroidContext.get(BackgroundService.this), isRunningForground);
            }
        });
        ViewEvents.onDestroy(new ViewEvents.Listener<ViewEvents.DestroyEvent>() {
            @Override
            public void handle(ViewEvents.DestroyEvent destroyEvent) {
                CentralEvents.removeListener(receiveMessageEventListener);
                locationTracker.stop();
            }
        });
        CentralServer.run(AndroidContext.get(getApplication()));
        ViewEvents.fireCreate();
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
    }
}
