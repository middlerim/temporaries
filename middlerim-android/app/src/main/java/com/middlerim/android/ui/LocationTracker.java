package com.middlerim.android.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;

import com.middlerim.client.view.ViewEvents;
import com.middlerim.location.Coordinate;

public class LocationTracker {
    private static final String TAG = Middlerim.TAG + ".LOCTRAC";

    private static final int LOCATION_INTERVAL_FG = 5000;
    private static final float LOCATION_DISTANCE_FG = 200f;

    private static final int LOCATION_INTERVAL_BG = LOCATION_INTERVAL_FG * 20;
    private static final float LOCATION_DISTANCE_BG = LOCATION_DISTANCE_FG * 2;

    private LocationManager locationManager;
    private AndroidContext ctx;
    private boolean isForeground;
    private boolean isStarted;

    private class LocationListener implements android.location.LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            ViewEvents.fireLocationUpdate(new Coordinate(location.getLatitude(), location.getLongitude()));
        }

        @Override
        public void onProviderDisabled(String provider) {
            if (LocationManager.GPS_PROVIDER.equals(provider)) {
                ViewEvents.fireStatusChange(Codes.STATUS_CHANGE_GPS_DISABLED);
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
            if (LocationManager.GPS_PROVIDER.equals(provider)) {
                ViewEvents.fireStatusChange(Codes.STATUS_CHANGE_GPS_ENABLED);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

    private final LocationListener locationListener = new LocationListener();

    public void switchTrackingMode(boolean isForeground) {
        if (!(isForeground ^ this.isForeground)) {
            return;
        }
        this.isForeground = isForeground;
        stop();
        initLocationManager();
    }

    public void start(final AndroidContext ctx, final boolean isForeground) {
        if (isStarted) {
            return;
        }
        isStarted = true;
        this.ctx = ctx;
        this.isForeground = isForeground;
        initLocationManager();
    }

    private void initLocationManager() {
        if (!checkPermission()) {
            return;
        }
        new Handler(ctx.getContext().getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!checkPermission()) {
                    return;
                }
                if (locationManager == null) {
                    locationManager = (LocationManager) ctx.getContext().getSystemService(Context.LOCATION_SERVICE);
                }
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation == null) {
                    lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (lastKnownLocation != null) {
                    ViewEvents.fireLocationUpdate(new Coordinate(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));
                }
                int locationInterval = isForeground ? LOCATION_INTERVAL_FG : LOCATION_INTERVAL_BG;
                float locationDistance = isForeground ? LOCATION_DISTANCE_FG : LOCATION_DISTANCE_BG;
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, locationInterval, locationDistance, locationListener);
            }
        }, 2000);
    }

    public void stop() {
        isStarted = false;
        if (!checkPermission()) {
            return;
        }
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    private boolean checkPermission() {
        return ActivityCompat.checkSelfPermission(ctx.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}