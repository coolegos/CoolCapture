package com.egos.capture.util;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.Display;
import java.lang.reflect.Method;

/**
 * Created by Egos on 2017/4/13.
 */

public class ScreenUtil {

    public static int screentHeight;
    public static int screentWidth;

    public static int getScreenWidth(@NonNull Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight(@NonNull Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    public static int getScreenDensityDpi(@NonNull Context context) {
        return context.getResources().getDisplayMetrics().densityDpi;
    }

    public static void setScreenSize(Activity activity) {
        try {
            final DisplayMetrics metrics = new DisplayMetrics();
            Display display = activity.getWindowManager().getDefaultDisplay();
            Method mGetRawH, mGetRawW;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                display.getRealMetrics(metrics);

                screentWidth = metrics.widthPixels;
                screentHeight = metrics.heightPixels;
            } else {
                mGetRawH = Display.class.getMethod("getRawHeight");
                mGetRawW = Display.class.getMethod("getRawWidth");
                screentWidth = (Integer) mGetRawW.invoke(display);
                screentHeight = (Integer) mGetRawH.invoke(display);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
