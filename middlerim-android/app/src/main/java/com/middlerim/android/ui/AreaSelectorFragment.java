package com.middlerim.android.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class AreaSelectorFragment extends Fragment {

    private MapFragment map;
    private AreaSelector areaSelector;
    private AndroidContext androidContext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_area_selector, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        androidContext = AndroidContext.get(getActivity());
        map = androidContext.fragmentManager().findById(R.id.fragment_map);
        areaSelector = (AreaSelector) view.findViewById(R.id.area_selector);
        areaSelector.setOnSeekBarChangeListener(map);
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
        prefEditor.apply();
    }
}
