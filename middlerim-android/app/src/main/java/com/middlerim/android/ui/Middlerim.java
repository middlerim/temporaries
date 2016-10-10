package com.middlerim.android.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.middlerim.client.CentralEvents;
import com.middlerim.client.view.ViewEvents;
import com.middlerim.location.Point;

public class Middlerim extends AppCompatActivity {

    public static final String NAME = "Middlerim";
    private AndroidContext androidContext;

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
                onPause();
                requestGps();
            } else if (statusChangeEvent.statusCode == Codes.STATUS_CHANGE_GPS_ENABLED) {
                onResume();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidContext = AndroidContext.get(this);
        setContentView(R.layout.middlerim);
    }

    @Override
    protected void onResume() {
        super.onResume();

        requestPermission();
        if (!BackgroundService.isStarted()) {
            Intent intent = new Intent(this, Boot.class);
            intent.setAction(Boot.ACTION_START_FROM_ACTIVITY);
            sendBroadcast(intent);
        }

        CentralEvents.onError(errorEventListener);
        ViewEvents.onStatusChange(statusChangeEventListener);
        Location lastKnownLocation = androidContext.getLastKnownLocation();
        if (lastKnownLocation != null) {
            ViewEvents.fireLocationUpdate(new Point(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));
        }
    }


    @Override
    protected void onPause() {
        ViewEvents.removeListener(statusChangeEventListener);
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

    private void onError(String message, Throwable e) {
        Log.e(NAME, message, e);
        try {
            Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
            toast.show();
            onPause();
        } catch (Exception e2) {
            e2.printStackTrace();
            onStop();
        }
    }

    private boolean checkPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (checkPermission()) {
            return;
        }
        ActivityCompat.requestPermissions(this, new String[]
                {Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.INTERNET
                }, Codes.PERMISSION_REQUEST_CODE_MAP);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == Codes.PERMISSION_REQUEST_CODE_MAP) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                return;
            } else {
                final Toast toast = Toast.makeText(this, R.string.error_permission_needed, Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }


    private void requestGps() {
        final GpsDisabledAlertDialogFragment alertDialog = new GpsDisabledAlertDialogFragment();
        alertDialog.show(getSupportFragmentManager(), NAME + ".GpsDisabledAlert");
    }
}
