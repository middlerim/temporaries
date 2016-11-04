package com.middlerim.android.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentActivity;

import com.middlerim.client.view.ViewContext;

import java.io.File;

class AndroidContext extends ViewContext {

    private Context ctx;
    private ButtonQueueManager buttonQueueManager;
    private FragmentManager fragmentManager;

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

    public Middlerim getActivity() {
        if (ctx instanceof Middlerim) {
            return (Middlerim) ctx;
        }
        return null;
    }

    public synchronized FragmentManager fragmentManager() {
        if (ctx instanceof FragmentActivity) {
            if (fragmentManager != null) {
                return this.fragmentManager;
            }
            this.fragmentManager = new FragmentManager((FragmentActivity) ctx);
            return this.fragmentManager;
        }
        throw new IllegalStateException("The context don't have fragmentManager" + ctx);
    }

    public synchronized ButtonQueueManager buttonQueueManager() {
        if (ctx instanceof FragmentActivity) {
            if (buttonQueueManager != null) {
                return buttonQueueManager;
            }
            MainFragment main = fragmentManager().getMainFragment();
            if (main == null) {
                throw new IllegalStateException("MainFragment need to be started.");
            }
            buttonQueueManager = new ButtonQueueManager(main, this);
            return buttonQueueManager;
        }
        throw new IllegalStateException("The context don't have fragmentManager" + ctx);
    }

    @Override
    public File getCacheDir() {
        return ctx.getCacheDir();
    }

    public SharedPreferences preferences() {
        return ctx.getSharedPreferences(Middlerim.TAG, Context.MODE_PRIVATE);
    }

    @Override
    public boolean isDebug() {
        return true;
    }
}
