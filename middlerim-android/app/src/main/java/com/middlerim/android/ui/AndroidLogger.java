package com.middlerim.android.ui;

import android.util.Log;

import com.middlerim.client.view.Logger;


public class AndroidLogger implements Logger {

    @Override
    public void warn(String tag, String message) {
        Log.w(tag, message);
    }

    @Override
    public void debug(String tag, String message) {
        Log.d(tag, message);
    }
}
