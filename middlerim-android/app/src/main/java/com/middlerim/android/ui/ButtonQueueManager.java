package com.middlerim.android.ui;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.DrawableRes;
import android.view.View;

import com.middlerim.client.view.ViewEvents;

public class ButtonQueueManager {

    public static final int MAX_BUTTON_QUEUE = 20;
    private static int index = 0;
    private static View[] buttons;

    private Adopter adopter;

    private Handler mainLoop;

    public ButtonQueueManager(Adopter adopter) {
        this.adopter = adopter;
        this.mainLoop = new Handler(Looper.getMainLooper());
        synchronized (ButtonQueueManager.class) {
            if (buttons == null) {
                buttons = new View[MAX_BUTTON_QUEUE];
                ViewEvents.onPause("ButtonQueueManager.ViewEvents.Listener<ViewEvents.PauseEvent>", new ViewEvents.Listener<ViewEvents.PauseEvent>() {
                    @Override
                    public void handle(ViewEvents.PauseEvent event) {
                        index = 0;
                        buttons = null;
                    }
                });
            }
        }
    }

    public int addButton(@DrawableRes int id, ButtonCallback callback) {
        index++;
        if (index >= MAX_BUTTON_QUEUE) {
            index = 0;
        }
        final View button = adopter.createNewButton(id);
        button.setOnClickListener(callback);
        buttons[index] = button;
        mainLoop.post(new Runnable() {
            @Override
            public void run() {
                adopter.addButton(button);
            }
        });
        return index;
    }

    public void removeButton(int index) {
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
            removeButton(i);
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
