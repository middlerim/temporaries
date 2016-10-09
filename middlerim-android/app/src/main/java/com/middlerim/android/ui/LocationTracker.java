package com.middlerim.android.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.middlerim.client.view.ViewEvents;
import com.middlerim.location.Point;

public class LocationTracker {
    private static final String TAG = "BOOMBOOMTESTGPS";
    private static final int LOCATION_INTERVAL = 5000;
    private static final float LOCATION_DISTANCE = 200f;

    private LocationManager locationManager;
    private AndroidContext ctx;

    private class LocationListener implements android.location.LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            ViewEvents.fireLocationUpdate(new Point(location.getLatitude(), location.getLongitude()));
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

    public void start(final AndroidContext ctx) {
        this.ctx = ctx;

        new Handler(ctx.getContext().getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (locationManager == null) {
                    locationManager = (LocationManager) ctx.getContext().getSystemService(Context.LOCATION_SERVICE);
                }
                try {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                            locationListener);
                } catch (java.lang.SecurityException ex) {
                    Log.i(TAG, "fail to request location update, ignore", ex);
                } catch (IllegalArgumentException ex) {
                    Log.d(TAG, "network provider does not exist, " + ex.getMessage());
                }
                try {
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                            locationListener);
                } catch (java.lang.SecurityException ex) {
                    Log.i(TAG, "fail to request location update, ignore", ex);
                } catch (IllegalArgumentException ex) {
                    Log.d(TAG, "gps provider does not exist " + ex.getMessage());
                }

                checkPermission();
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation == null) {
                    lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                ctx.setLastKnownLocation(lastKnownLocation);
            }
        }, 1000);
    }

    public void stop() {
        if (locationManager != null) {
            checkPermission();
            locationManager.removeUpdates(locationListener);
        }
    }

    private boolean checkPermission() {
        return ActivityCompat.checkSelfPermission(ctx.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(ctx.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(ctx.getContext(), Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(ctx.getContext(), Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED;
    }
}