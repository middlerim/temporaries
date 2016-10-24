package com.middlerim.android.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MinuteMessageFragment extends Fragment {
    public static final String TAG = Middlerim.TAG + ".MinuteMsg";

    private AndroidContext androidContext;


    @Override
    public void onStart() {
        super.onStart();
        androidContext = AndroidContext.get(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_minute_message, container, false);
        return view;
    }
}
