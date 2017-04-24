package com.egos.capture;

import android.content.Intent;

/**
 * Created by Egos on 2017/4/13.
 */

public interface CaptureListener {

    void showCaptureWindow();

    void hideCaptureWindow();

    void create(Intent intent, int width, int height);
}