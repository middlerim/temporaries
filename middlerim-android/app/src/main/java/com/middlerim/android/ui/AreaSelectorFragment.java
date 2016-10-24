package com.middlerim.android.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class AreaSelectorFragment extends Fragment {
    public static final String TAG = Middlerim.TAG + ".Area";

    private MapFragment map;
    private AreaSelector areaSelector;
    private AndroidContext androidContext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_area_selector, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        androidContext = AndroidContext.get(getActivity());
        map = androidContext.fragmentManager().getMapFragment();
        areaSelector = (AreaSelector) getView().findViewById(R.id.area_selector);
    }

    @Override
    public void onResume() {
        super.onResume();
        map.showAreaCircle();
        areaSelector.setProgress(androidContext.preferences().getInt(Codes.PREF_AREA_PROGRESS, 0));
    }

    @Override
    public void onPause() {
        super.onPause();
        map.hideAreaCircle();
        SharedPreferences.Editor prefEditor = androidContext.preferences().edit();
        prefEditor.putInt(Codes.PREF_AREA_PROGRESS, areaSelector.getProgress());
        prefEditor.putInt(Codes.PREF_AREA_RADIUS, areaSelector.progressToRadius(areaSelector.getProgress()));
        prefEditor.apply();
    }
}
