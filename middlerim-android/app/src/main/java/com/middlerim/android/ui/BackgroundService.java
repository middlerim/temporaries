package com.middlerim.android.ui;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.middlerim.client.CentralEvents;
import com.middlerim.client.CentralServer;
import com.middlerim.client.view.ViewEvents;

public class BackgroundService extends IntentService {

    private LocationTracker locationTracker;
    private static boolean isStarted;

    private CentralEvents.Listener<CentralEvents.ReceiveMessageEvent> receiveMessageEventListener = new CentralEvents.Listener<CentralEvents.ReceiveMessageEvent>() {
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

    public BackgroundService() {
        super("Middlerim");
    }

    @Override
    public void onStart(Intent intent, int startId) {
        if (isStarted) {
            return;
        }
        isStarted = true;
        super.onStart(intent, startId);
        Log.w("BackgroundService#start", intent.toString());
        CentralEvents.onReceiveMessage(receiveMessageEventListener);
        CentralEvents.onStarted(new CentralEvents.Listener<CentralEvents.StartedEvent>() {
            @Override
            public void handle(CentralEvents.StartedEvent startedEvent) {
                locationTracker = new LocationTracker();
                locationTracker.start(AndroidContext.get(BackgroundService.this.getApplication()));
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

    static boolean isStarted() {
        return isStarted;
    }
}
