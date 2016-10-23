package com.middlerim.android.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.middlerim.client.CentralEvents;
import com.middlerim.client.view.ViewEvents;

public class Middlerim extends AppCompatActivity {
    public static final String TAG = "Middlerim";

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.middlerim);
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
        CentralEvents.onError(errorEventListener);
        BackgroundService.onResumeForground(this);
        ViewEvents.onStatusChange(statusChangeEventListener);
    }

    @Override
    protected void onPause() {
        ViewEvents.removeListener(statusChangeEventListener);
        BackgroundService.onPauseForground(this);
        CentralEvents.removeListener(errorEventListener);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                GeneralPreferenceFragment.open(this);
                return true;
            case R.id.action_signIn:
                SignInFragment.open(this);
                return true;
            case R.id.action_search:
                System.out.println(item);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
            } else {
                finish();
            }
        }
    }

    private void requestGps() {
        final GpsDisabledAlertDialogFragment alertDialog = new GpsDisabledAlertDialogFragment();
        alertDialog.show(getSupportFragmentManager(), TAG);
    }
}
