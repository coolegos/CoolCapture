package com.egos.capture.view;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

/**
 * Created by Egos on 2017/4/13.
 */
public class CaptureWindowView extends FrameLayout {
    public CaptureWindowView(Context context) {
        super(context);
    }

    public CaptureWindowView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CaptureWindowView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CaptureWindowView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {

        Log.e("CaptureWindowView", "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
    }
}
