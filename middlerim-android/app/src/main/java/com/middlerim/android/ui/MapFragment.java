package com.middlerim.android.ui;

import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.appindexing.AndroidAppUri;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.middlerim.client.view.ViewEvents;
import com.middlerim.location.Point;

public class MapFragment extends SupportMapFragment implements OnMapReadyCallback {
    private static final String NAME = Middlerim.NAME + ".MAP";

    private GoogleMap map;
    private AndroidContext androidContext;
    private ViewEvents.Listener<ViewEvents.LocationUpdateEvent> locationChangeListener = new ViewEvents.Listener<ViewEvents.LocationUpdateEvent>() {
        @Override
        public void handle(ViewEvents.LocationUpdateEvent locationUpdateEvent) {
            movePosition(locationUpdateEvent.location, true);
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        androidContext = AndroidContext.get(this.getContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        getMapAsync(this);
        ViewEvents.onLocationUpdate(locationChangeListener);
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        map = googleMap;

        // https://mapstyle.withgoogle.com
        boolean success = map.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.map_style));

        if (!success) {
            Log.e("MapsActivityRaw", "Style parsing failed.");
        }
        Location lastKnownLocation = androidContext.getLastKnownLocation();
        if (lastKnownLocation != null) {
            movePosition(new Point(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()), false);
        }
    }

    private void movePosition(Point location, boolean animate) {
        if (map == null) {
            return;
        }
        final LatLng me = new LatLng(location.latitude, location.longitude);
        if (animate) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(me, 15));
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(me, 15));
        }
    }

    @Override
    public void onPause() {
        ViewEvents.removeListener(locationChangeListener);
        super.onPause();
    }
}
