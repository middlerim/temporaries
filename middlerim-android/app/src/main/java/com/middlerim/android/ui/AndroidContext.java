package com.middlerim.android.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentActivity;
import android.text.SpannableStringBuilder;

import com.middlerim.client.Config;
import com.middlerim.client.view.MessagePool;
import com.middlerim.client.view.ViewContext;
import com.middlerim.client.view.ViewEvents;
import com.middlerim.location.Coordinate;

import java.io.File;
import java.nio.ByteBuffer;

class AndroidContext extends ViewContext {

    private static int messageTag = 0;
    private static MessagePool<Message> messagePool;

    private Context ctx;
    private ButtonQueueManager buttonQueueManager;
    private FragmentManager fragmentManager;

    private AndroidContext() {
    }

    public static AndroidContext get(Context appContext) {
        synchronized (AndroidContext.class) {
            AndroidContext instance = new AndroidContext();
            instance.ctx = appContext;
            initializeMessagePool(instance);
            return instance;
        }
    }

    private static void initializeMessagePool(final AndroidContext context) {
        if (messagePool != null) {
            return;
        }
        final File storage = context.ctx.getFilesDir();
        messagePool = new MessagePool<>(10, new MessagePool.Adapter<Message>() {
            @Override
            public Message onReceive(long userId, Coordinate location, String displayName, ByteBuffer message, int numberOfDelivery) {
                ByteBuffer buf = message.duplicate();
                buf.position(0);
                byte[] bs = new byte[buf.remaining()];
                buf.get(bs);
                SpannableStringBuilder sb = new SpannableStringBuilder(new String(bs, Config.MESSAGE_ENCODING));
                Message msg = new Message(userId, location, displayName, sb, numberOfDelivery);
                return msg;
            }

            @Override
            public File storage() {
                return new File(storage, "messages");
            }
        });
        int latestMessageSize = context.preferences().getInt(Codes.PREF_LATEST_MESSAGE_SIZE, -1);
        if (latestMessageSize > 0) {
            messagePool.loadLatestMessages(latestMessageSize);
        }
        ViewEvents.onPause("AndroidContext.ViewEvents.Listener<ViewEvents.PauseEvent>", new ViewEvents.Listener<ViewEvents.PauseEvent>() {
            @Override
            public void handle(ViewEvents.PauseEvent event) {
                context.preferences().edit().putInt(Codes.PREF_LATEST_MESSAGE_SIZE, messagePool.size()).apply();
            }
        });
        ViewEvents.onDestroy("AndroidContext.ViewEvents.Listener<ViewEvents.DestroyEvent>", new ViewEvents.Listener<ViewEvents.DestroyEvent>() {
            @Override
            public void handle(ViewEvents.DestroyEvent event) {
                messagePool.stopListen();
            }
        });
        messagePool.startListen();
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

    public synchronized int nextMessageTag() {
        return ++messageTag;
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

    @Override
    public MessagePool<Message> getMessagePool() {
        return messagePool;
    }
}
