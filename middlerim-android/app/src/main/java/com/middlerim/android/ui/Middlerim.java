package com.middlerim.android.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;

import com.middlerim.client.Config;
import com.middlerim.client.central.CentralEvents;
import com.middlerim.client.view.ViewEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Middlerim extends AppCompatActivity {
    public static final String TAG = Config.INTERNAL_APP_NAME;
    private static final Logger LOG = LoggerFactory.getLogger(Config.INTERNAL_APP_NAME);

    private Toolbar toolbar;
    private AndroidContext androidContext;
    private int actionBarHeight;

    static {
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
        @Override
        public void handle(CentralEvents.ReceivedTextEvent event) {
            androidContext.buttonQueueManager().removeButton(event.tag);
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

        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }
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
        ViewEvents.fireResume();
    }

    @Override
    protected void onPause() {
        ViewEvents.firePause();
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


    public void showToolbar() {
        toolbar.setTop(0);
        toolbar.setBottom(actionBarHeight);
        toolbar.setVisibility(View.VISIBLE);
    }

    public Toolbar getToolbar() {
        return toolbar;
    }
}
