package com.middlerim.android.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.DrawableRes;
import android.util.SparseIntArray;
import android.view.View;

import com.middlerim.client.Config;
import com.middlerim.client.view.ViewEvents;

import java.util.HashMap;
import java.util.Map;

public class ButtonQueueManager {

    public static final int MAX_BUTTON_QUEUE = Config.MAX_MESSAGE_QUEUE + 10;

    private static int index;
    private static View[] buttons;
    private static SparseIntArray tagMap;

    private AndroidContext context;
    private Adopter adopter;
    private Handler mainLoop;

    static {
        ViewEvents.onPause("ButtonQueueManager.ViewEvents.Listener<ViewEvents.PauseEvent>", new ViewEvents.Listener<ViewEvents.PauseEvent>() {
            @Override
            public void handle(ViewEvents.PauseEvent event) {
                index = 0;
                buttons = null;
                tagMap = null;
            }
        });
    }

    public ButtonQueueManager(Adopter adopter, AndroidContext context) {
        this.context = context;
        this.adopter = adopter;
        this.mainLoop = new Handler(Looper.getMainLooper());
    }

    public int addButton(int tag, @DrawableRes int id, final FragmentManager.Page callbackPage, final Bundle args) {
        if (buttons == null) {
            index = 0;
            buttons = new View[MAX_BUTTON_QUEUE];
            tagMap = new SparseIntArray(MAX_BUTTON_QUEUE);
        }

        index++;
        if (index >= MAX_BUTTON_QUEUE) {
            index = 0;
        }
        final View button = adopter.createNewButton(id);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                context.fragmentManager().open(callbackPage, args);
            }
        });
        buttons[index] = button;
        mainLoop.post(new Runnable() {
            @Override
            public void run() {
                adopter.addButton(button);
            }
        });
        if (tag == -1) {
            tagMap.append(index, index);
        } else {
            tagMap.append(tag, index);
        }
        return index;
    }

    public void removeButton(int tag) {
        int index = tagMap.get(tag);
        removeButtonByIndex(index);
    }

    private void removeButtonByIndex(int index) {
        final View button = buttons[index];
        if (button == null) {
            return;
        }
        buttons[index] = null;
        mainLoop.post(new Runnable() {
            @Override
            public void run() {
                adopter.removeButton(button);
            }
        });
    }

    public void removeAllButtons() {
        for (int i = 0; i < buttons.length; i++) {
            removeButtonByIndex(i);
        }
    }

    public interface Adopter {
        View createNewButton(@DrawableRes int id);

        void addButton(View button);

        void removeButton(View button);
    }

    public interface ButtonCallback extends View.OnClickListener {
    }
}
