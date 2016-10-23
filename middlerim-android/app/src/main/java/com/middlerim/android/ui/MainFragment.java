package com.middlerim.android.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

public class MainFragment extends Fragment {

    private AndroidContext androidContext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        androidContext = AndroidContext.get(getActivity());
        view.findViewById(R.id.button_new_message).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                androidContext.fragmentManager().openNewMessage(MainFragment.this);
            }
        });
        view.findViewById(R.id.button_edit_area).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                androidContext.fragmentManager().openMap(MainFragment.this);
            }
        });
        return view;
    }
}
