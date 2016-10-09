package com.middlerim.android.ui;

import android.content.Context;
import android.location.Location;

import com.middlerim.client.view.Logger;
import com.middlerim.client.view.ViewContext;

import java.io.File;

public class AndroidContext extends ViewContext {

    private static final Logger logger = new AndroidLogger();
    private static Location lastKnownLocation;

    private Context ctx;

    public static AndroidContext get(Context appContext) {
        AndroidContext instance = new AndroidContext();
        instance.ctx = appContext;
        return instance;
    }

    public Context getContext() {
        return ctx;
    }


    @Override
    public File getCacheDir() {
        return ctx.getCacheDir();
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public boolean isDebug() {
        return true;
    }

    public void setLastKnownLocation(Location lastKnownLocation) {
        this.lastKnownLocation = lastKnownLocation;
    }

    public Location getLastKnownLocation() {
        return this.lastKnownLocation;
    }
}
