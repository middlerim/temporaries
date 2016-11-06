package com.middlerim.android.ui;

import android.view.MotionEvent;
import android.view.View;

public class SynchronisedScrollTouchListener implements View.OnTouchListener {
    private boolean visible;
    private float initialY = Float.MIN_VALUE;

    private final View view;
    private int originalHeight = -1;

    public SynchronisedScrollTouchListener(View view) {
        this.view = view;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (initialY == Float.MIN_VALUE) {
                    if (originalHeight <= 0) {
                        originalHeight = view.getHeight();
                    }
                    visible = view.getVisibility() == View.VISIBLE;
                    if (!visible) {
                        view.setVisibility(View.VISIBLE);
                        initialY = event.getY();
                    } else {
                        initialY = event.getY() - originalHeight;
                    }
                }
                int delta = (int) (event.getY() - initialY);
                if (delta <= originalHeight) {
                    view.setTop(delta - originalHeight);
                    view.setBottom(delta);
                } else if (delta >= originalHeight && view.getTop() != 0) {
                    view.setTop(0);
                    view.setBottom(originalHeight);
                }
                break;
            case MotionEvent.ACTION_UP:
                delta = (int) (event.getY() - initialY);
                if (!visible && delta >= originalHeight / 2) {
                    view.setTop(0);
                    view.setBottom(originalHeight);
                } else if (visible && delta <= originalHeight / 2) {
                    view.setBottom(0);
                    view.setVisibility(View.INVISIBLE);
                } else if (visible) {
                    view.setTop(0);
                    view.setBottom(originalHeight);
                } else {
                    view.setBottom(0);
                    view.setVisibility(View.INVISIBLE);
                }
                initialY = Float.MIN_VALUE;
        }
        return false;
    }
}
