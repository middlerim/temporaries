package com.middlerim.android.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentActivity;

import com.middlerim.client.view.Logger;
import com.middlerim.client.view.ViewContext;

import java.io.File;

class AndroidContext extends ViewContext {

    private static final Logger logger = new AndroidLogger();

    private Context ctx;

    private AndroidContext() {
    }

    public static AndroidContext get(Context appContext) {
        synchronized (AndroidContext.class) {
            AndroidContext instance = new AndroidContext();
            instance.ctx = appContext;
            return instance;
        }
    }

    public Context getContext() {
        return ctx;
    }


    public FragmentManager fragmentManager() {
        if (ctx instanceof FragmentActivity) {
            return new FragmentManager((FragmentActivity) ctx);
        }
        throw new IllegalStateException("The context don't have fragmentManager" + ctx);
    }

    @Override
    public File getCacheDir() {
        return ctx.getCacheDir();
    }

    @Override
    public Logger logger() {
        return logger;
    }

    public SharedPreferences preferences() {
        return ctx.getSharedPreferences(Middlerim.TAG, Context.MODE_PRIVATE);
    }

    @Override
    public boolean isDebug() {
        return true;
    }
}
