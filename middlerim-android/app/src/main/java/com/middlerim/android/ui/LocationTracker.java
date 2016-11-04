package com.middlerim.android.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;

import com.middlerim.client.Config;
import com.middlerim.client.view.ViewEvents;
import com.middlerim.location.Coordinate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocationTracker {
    private static final Logger LOG = LoggerFactory.getLogger(Config.INTERNAL_APP_NAME);

    private static final int LOCATION_INTERVAL_FG = Config.SESSION_TIMEOUT_MILLIS / 2;
    private static final float LOCATION_DISTANCE_FG = 50f;

    private static final int LOCATION_INTERVAL_BG = Config.SESSION_TIMEOUT_MILLIS - 5000;
    private static final float LOCATION_DISTANCE_BG = LOCATION_DISTANCE_FG * 2;

    private LocationManager locationManager;
    private AndroidContext androidContext;
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
        if (isForeground == this.isForeground) {
            return;
        }
        this.isForeground = isForeground;
        stop();
        initLocationManager();
        LOG.debug("Changed to " + (isForeground ? "forground" : "background") + " mode.");
    }

    public void start(final AndroidContext androidContext, final boolean isForeground) {
        if (isStarted) {
            return;
        }
        isStarted = true;
        this.androidContext = androidContext;
        this.isForeground = isForeground;
        initLocationManager();
    }

    private void initLocationManager() {
        if (!checkPermission()) {
            return;
        }
        new Handler(androidContext.getContext().getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!checkPermission()) {
                    return;
                }
                if (locationManager == null) {
                    locationManager = (LocationManager) androidContext.getContext().getSystemService(Context.LOCATION_SERVICE);
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
        return ActivityCompat.checkSelfPermission(androidContext.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}