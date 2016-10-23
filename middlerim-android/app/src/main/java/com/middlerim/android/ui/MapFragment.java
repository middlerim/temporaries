package com.middlerim.android.ui;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.SeekBar;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.middlerim.client.view.ViewEvents;
import com.middlerim.location.Coordinate;

public class MapFragment extends SupportMapFragment implements OnMapReadyCallback, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = Middlerim.TAG + ".MAP";
    private GoogleMap map;
    private Circle area;
    private boolean showCircle = false;
    private int viewportMinSize = 320;
    private int lastSelectedRadiusMeter;
    private Coordinate lastLocation;

    private AndroidContext androidContext;
    private ViewEvents.Listener<ViewEvents.LocationUpdateEvent> locationChangeListener = new ViewEvents.Listener<ViewEvents.LocationUpdateEvent>() {
        @Override
        public void handle(ViewEvents.LocationUpdateEvent locationUpdateEvent) {
            movePosition(locationUpdateEvent.location, lastSelectedRadiusMeter);
            lastLocation = locationUpdateEvent.location;
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point p = new Point();
        display.getSize(p);
        viewportMinSize = Math.min(p.x, p.y);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        androidContext = AndroidContext.get(getContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        getMapAsync(this);
        lastSelectedRadiusMeter = androidContext.preferences().getInt(Codes.PREF_AREA_RADIUS, 80000);
        ViewEvents.onLocationUpdate(locationChangeListener);
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        map = googleMap;
        // https://mapstyle.withgoogle.com
        boolean success = map.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.map_style));

        if (!success) {
            Log.e(TAG, "Style parsing failed.");
        }
    }

    private void movePosition(Coordinate location, int radiusMeter) {
        if (map == null) {
            return;
        }
        final LatLng me = new LatLng(location.latitude, location.longitude);
        if (area == null) {
            area = createCircle(me, radiusMeter);
        } else {
            area.setCenter(me);
            if (lastSelectedRadiusMeter != radiusMeter) {
                area.setRadius(radiusMeter);
            }
        }
        float currentZoomLevel = map.getCameraPosition().zoom;
        float zoomLevel = calculateZoomLevel(area);
        if (currentZoomLevel < zoomLevel && zoomLevel - currentZoomLevel <= 5) {
            zoomLevel = currentZoomLevel;
        }
        if (zoomLevel >= 13 || Math.abs(zoomLevel - currentZoomLevel) > 5) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(me, zoomLevel));
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(me, zoomLevel));
        }

        lastSelectedRadiusMeter = radiusMeter;
    }

    private Circle createCircle(LatLng me, int radiusMeter) {
        return map.addCircle(new CircleOptions().center(me).radius(radiusMeter).strokeColor(Color.parseColor("#88FF9900")).visible(showCircle));
    }

    private float calculateZoomLevel(Circle circle) {
        double radius = circle.getRadius();
        double scale = radius / 200;
        return (float) (16 - Math.log(scale) / Math.log(2));
    }

    @Override
    public void onPause() {
        ViewEvents.removeListener(locationChangeListener);
        SharedPreferences.Editor prefEditor = androidContext.preferences().edit();
        prefEditor.putInt(Codes.PREF_AREA_RADIUS, lastSelectedRadiusMeter);
        prefEditor.apply();
        super.onPause();
    }

    public void showAreaCircle() {
        showCircle = true;
        if (area != null) {
            area.setVisible(true);
        }
    }

    public void hideAreaCircle() {
        showCircle = false;
        if (area != null) {
            area.setVisible(false);
        }
    }

    @Override
    public void onProgressChanged(SeekBar areaSelector, int progress, boolean fromUser) {
        if (lastLocation == null) {
            return;
        }
        int radius = ((AreaSelector) areaSelector).progressToRadius(progress);
        movePosition(lastLocation, radius);
    }

    @Override
    public void onStartTrackingTouch(SeekBar areaSelector) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar areaSelector) {

    }
}
