package com.middlerim.android.ui;

import android.os.StrictMode;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.middlerim.client.CentralEvents;
import com.middlerim.client.view.ViewEvents;

public class Middlerim extends AppCompatActivity {
    public static final String TAG = "Middlerim";

    private Toolbar toolbar;
    private int originalToolbarHeight = -1;
    private AndroidContext androidContext;
    private SparseIntArray buttonQueueIds = new SparseIntArray(5);

    {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);

    }
    private CentralEvents.Listener<CentralEvents.ErrorEvent> errorEventListener = new CentralEvents.Listener<CentralEvents.ErrorEvent>() {
        @Override
        public void handle(CentralEvents.ErrorEvent event) {
            onError(event.message, event.cause);
        }
    };

    private ViewEvents.Listener<ViewEvents.StatusChangeEvent> statusChangeEventListener = new ViewEvents.Listener<ViewEvents.StatusChangeEvent>() {
        @Override
        public void handle(ViewEvents.StatusChangeEvent statusChangeEvent) {
            if (statusChangeEvent.statusCode == Codes.STATUS_CHANGE_GPS_DISABLED) {
                requestGps();
            } else if (statusChangeEvent.statusCode == Codes.STATUS_CHANGE_GPS_ENABLED) {
                resume();
            }
        }
    };


    private CentralEvents.Listener<CentralEvents.SendMessageEvent> sendMessageListener = new CentralEvents.Listener<CentralEvents.SendMessageEvent>() {
        @Override
        public void handle(CentralEvents.SendMessageEvent event) {
            buttonQueueIds.put(event.clientSequenceNo, event.tag);
        }
    };


    private CentralEvents.Listener<CentralEvents.LostMessageEvent> lostMessageListener = new CentralEvents.Listener<CentralEvents.LostMessageEvent>() {
        @Override
        public void handle(final CentralEvents.LostMessageEvent event) {
            final int tag = event.message.tag();
            new Handler(getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    androidContext.buttonQueueManager().removeButton(tag);
                    final Toast toast = Toast.makeText(Middlerim.this, R.string.error_lost_message, Toast.LENGTH_SHORT);
                    toast.show();
                }
            });
        }
    };

    private CentralEvents.Listener<CentralEvents.ReceivedTextEvent> receivedTextListener = new CentralEvents.Listener<CentralEvents.ReceivedTextEvent>() {
        private byte lastReveicedSequenceNo = Byte.MIN_VALUE;

        @Override
        public void handle(CentralEvents.ReceivedTextEvent event) {
            int tag = buttonQueueIds.get(event.clientSequenceNo, -1);
            if (tag == -1) {
                return;
            }
            buttonQueueIds.delete(event.clientSequenceNo);
            androidContext.buttonQueueManager().removeButton(tag);
            if ((lastReveicedSequenceNo + 1) != event.clientSequenceNo) {
                // Despite clientSequenceNo must be sequencial, it isn't. Some messages might be lost.
                for (int i = lastReveicedSequenceNo; i < event.clientSequenceNo; i++) {
                    int leftOverTag = buttonQueueIds.get(i, -1);
                    if (leftOverTag == -1) {
                        continue;
                    }
                    buttonQueueIds.delete(i);
                    androidContext.buttonQueueManager().removeButton(leftOverTag);
                }
                androidContext.logger().warn(TAG, "The message which sent just before the message which sent the time are not match. prev: "
                        + lastReveicedSequenceNo + ", curr: " + event.clientSequenceNo);
            }
            lastReveicedSequenceNo = event.clientSequenceNo;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.middlerim);
        initToolbar();
        androidContext = AndroidContext.get(this);
        androidContext.fragmentManager().openWelcomeSet();
    }

    private void initToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (requestPermissionIfNeeded()) {
            return;
        }
        resume();
    }

    private void resume() {
        CentralEvents.onError(TAG + ".errorEventListener", errorEventListener);
        CentralEvents.onLostMessage(TAG + ".lostMessageListener", lostMessageListener);
        BackgroundService.onResumeForground(this);
        ViewEvents.onStatusChange(TAG + ".statusChangeEventListener", statusChangeEventListener);
        CentralEvents.onReceivedText(TAG + ".receivedTextListener", receivedTextListener);
        CentralEvents.onSendMessage(TAG + ".sendMessageListener", sendMessageListener);
        ViewEvents.fireResume();
    }

    @Override
    protected void onPause() {
        ViewEvents.firePause();
        CentralEvents.removeListener(TAG + ".sendMessageListener");
        CentralEvents.removeListener(TAG + ".receivedTextListener");
        ViewEvents.removeListener(TAG + ".statusChangeEventListener");
        BackgroundService.onPauseForground(this);
        CentralEvents.removeListener(TAG + ".lostMessageListener");
        CentralEvents.removeListener(TAG + ".errorEventListener");
        super.onPause();
    }

    private void onError(final String message, Throwable e) {
        Log.e(TAG, message, e);

        new Handler(getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {

                    Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
                    toast.show();
                    onPause();
                } catch (Exception e2) {
                    e2.printStackTrace();
                    onStop();
                }
            }
        }, 0);
    }

    private boolean checkPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean requestPermissionIfNeeded() {
        if (checkPermission()) {
            return false;
        }
        final Toast toast = Toast.makeText(this, R.string.error_permission_needed, Toast.LENGTH_SHORT);
        toast.show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ActivityCompat.requestPermissions(Middlerim.this, new String[]
                        {Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_NETWORK_STATE,
                                Manifest.permission.INTERNET
                        }, Codes.PERMISSION_REQUEST);
            }
        }, 3000);

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == Codes.PERMISSION_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Don't call resume() here. It's going to be called by statusChangeEventListener.
                return;
            }
            finish();
        }
    }

    private void requestGps() {
        final GpsDisabledAlertDialogFragment alertDialog = new GpsDisabledAlertDialogFragment();
        alertDialog.show(getSupportFragmentManager(), TAG);
    }

    public void setToolbarHandler(View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            private boolean visible;
            private float initialY = Float.MIN_VALUE;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        if (initialY == Float.MIN_VALUE) {
                            if (originalToolbarHeight <= 0) {
                                originalToolbarHeight = toolbar.getHeight();
                            }
                            visible = toolbar.getVisibility() == View.VISIBLE;
                            if (!visible) {
                                toolbar.setVisibility(View.VISIBLE);
                                initialY = event.getY();
                            } else {
                                initialY = event.getY() - originalToolbarHeight;
                            }
                        }
                        int delta = (int) (event.getY() - initialY);
                        if (delta <= originalToolbarHeight) {
                            toolbar.setTop(delta - originalToolbarHeight);
                            toolbar.setBottom(delta);
                        } else if (delta >= originalToolbarHeight && toolbar.getTop() != 0) {
                            toolbar.setTop(0);
                            toolbar.setBottom(originalToolbarHeight);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        delta = (int) (event.getY() - initialY);
                        if (!visible && delta >= originalToolbarHeight / 2) {
                            toolbar.setTop(0);
                            toolbar.setBottom(originalToolbarHeight);
                        } else if (visible && delta <= originalToolbarHeight / 2) {
                            toolbar.setBottom(0);
                            toolbar.setVisibility(View.INVISIBLE);
                        } else if (visible) {
                            toolbar.setTop(0);
                            toolbar.setBottom(originalToolbarHeight);
                        } else {
                            toolbar.setBottom(0);
                            toolbar.setVisibility(View.INVISIBLE);
                        }
                        initialY = Float.MIN_VALUE;
                }
                return false;
            }
        });
    }
}
