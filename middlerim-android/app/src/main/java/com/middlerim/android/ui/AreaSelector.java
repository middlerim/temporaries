package com.middlerim.android.ui;


import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class AreaSelector extends SeekBar {

    public AreaSelector(Context context) {
        super(context);
    }

    public AreaSelector(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public AreaSelector(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldh, oldw);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    protected void onDraw(Canvas c) {
        c.rotate(-90);
        c.translate(-getHeight(), 0);

        super.onDraw(c);
    }

    public int progressToRadius(int progress) {
        if (progress <= 0) {
            return 16;
        }
        double d = (double) progress / getMax();
        if (d > 0.75) {
            // From 20KM to 100KM
            return 20000 + (int) (80000 * (d - 0.75) / 0.25);
        } else if (d > 0.4) {
            // From 2KM to 10KM
            return 2000 + (int) (8000 * (d - 0.4) / 0.35);
        } else {
            // From 16m to 1000m
            return 16 + (int) (984 * d / 0.4);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                setProgress(getMax() - (int) (getMax() * event.getY() / getHeight()));
                onSizeChanged(getWidth(), getHeight(), 0, 0);
                break;

            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return true;
    }
}
