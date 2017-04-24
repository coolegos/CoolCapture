package com.egos.capture.util;

import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Egos on 2017/4/13.
 */
public class FileUtil {

    private static final String SCREEN_CAPTURE_PATH = "coolcapture" + File.separator + "screenshots" + File.separator;

    private static final String SCREEN_CAPTURE_NAME = "screenshot";

    private static String getPath(Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return Environment.getExternalStorageDirectory().toString();
        } else {
            return context.getFilesDir().toString();
        }
    }


    private static String getScreenShots(Context context) {
        StringBuilder stringBuilder = new StringBuilder(getPath(context));
        stringBuilder.append(File.separator);
        stringBuilder.append(SCREEN_CAPTURE_PATH);
        File file = new File(stringBuilder.toString());
        if (!file.exists()) {
            file.mkdirs();
        }
        return stringBuilder.toString();

    }

    public static String getScreenShotsName(Context context) {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        String date = simpleDateFormat.format(new Date());
        StringBuilder stringBuilder = new StringBuilder(getScreenShots(context));
        stringBuilder.append(SCREEN_CAPTURE_NAME);
        stringBuilder.append("_");
        stringBuilder.append(date);
        stringBuilder.append(".png");
        return stringBuilder.toString();
    }

}
