package com.middlerim.android.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.middlerim.client.CentralEvents;
import com.middlerim.client.view.ViewEvents;
import com.middlerim.location.Coordinate;

public class MapFragment extends SupportMapFragment implements OnMapReadyCallback {
    public static final String TAG = Middlerim.TAG + ".MAP";
    private GoogleMap map;
    private Circle area;
    private boolean showCircle = false;
    private int lastSelectedRadiusMeter;
    private Coordinate lastLocation;
    private BitmapDescriptor messageMarkerImage;

    private AndroidContext androidContext;
    private ViewEvents.Listener<ViewEvents.LocationUpdateEvent> locationChangeListener = new ViewEvents.Listener<ViewEvents.LocationUpdateEvent>() {
        @Override
        public void handle(ViewEvents.LocationUpdateEvent event) {
            movePosition(event.location, lastSelectedRadiusMeter);
            lastLocation = event.location;
        }
    };
    private ViewEvents.Listener<ViewEvents.ChangeAreaEvent> changeAreaListener = new ViewEvents.Listener<ViewEvents.ChangeAreaEvent>() {
        @Override
        public void handle(ViewEvents.ChangeAreaEvent event) {
            if (lastLocation == null) {
                return;
            }
            movePosition(lastLocation, event.radiusMeter);
        }
    };
    private CentralEvents.Listener<CentralEvents.ReceiveMessageEvent> receiveMessageEventListener = new CentralEvents.Listener<CentralEvents.ReceiveMessageEvent>() {
        @Override
        public void handle(CentralEvents.ReceiveMessageEvent event) {
            final Coordinate location = event.location;
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    lightUp(location);
                }
            });
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        androidContext = AndroidContext.get(getContext());
        ViewEvents.onChangeArea(TAG + ".changeAreaListener", changeAreaListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        getMapAsync(this);
        lastSelectedRadiusMeter = androidContext.preferences().getInt(Codes.PREF_AREA_RADIUS, 16);
        ViewEvents.onLocationUpdate(TAG + ".locationChangeListener", locationChangeListener);
        CentralEvents.onReceiveMessage(TAG + ".receiveMessageEventListener", receiveMessageEventListener);
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        map = googleMap;
        if (messageMarkerImage == null) {
            // Needed to call BitmapDescriptorFactory after googleMap is initialized.
            messageMarkerImage = BitmapDescriptorFactory.fromResource(R.drawable.scrubber_control_normal_holo);
        }

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
        LatLng me = new LatLng(location.latitude, location.longitude);
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

    private void lightUp(Coordinate location) {
        if (map == null) {
            return;
        }
        LatLng latlng = new LatLng(location.latitude, location.longitude);
        final Marker marker = map.addMarker(new MarkerOptions()
                .position(latlng)
                .alpha(0)
                .icon(messageMarkerImage));

        final Handler handler = new Handler();
        handler.post(new Runnable() {
            float sign = 1;
            float alfa = 0;

            @Override
            public void run() {
                if (alfa < 0) {
                    marker.remove();
                } else if (alfa < 1) {
                    marker.setAlpha(alfa += (0.1 * sign));
                    handler.postDelayed(this, 100);
                } else {
                    sign = -1;
                    marker.setAlpha(alfa = 0.9f);
                    handler.postDelayed(this, 500);
                }
            }
        });
    }

    @Override
    public void onPause() {
        CentralEvents.removeListener(TAG + ".receiveMessageEventListener");
        ViewEvents.removeListener(TAG + ".locationChangeListener");
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
}
