package com.egos.capture;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.os.AsyncTaskCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import com.egos.capture.util.FileUtil;
import com.egos.capture.util.ScreenUtil;
import com.egos.capture.view.CropView;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Egos on 2017/4/13.
 *
 * 管理、展示截图界面。
 */
public class CaptureService extends Service {

    private final static String TAG = "CaptureService";

    private final static int DEFAULT_SIZE = 200;

    private WindowManager mWindowManager;
    private View mWindowView;
    private CropView mCropView;

    private boolean mIsShowing;

    private SensorManager mSensorManager;

    private MediaProjection mMediaProjection;
    private ImageReader mImageReader;
    private VirtualDisplay mVirtualDisplay;

    private int mScreenWidth;
    private int mScreenHeight;
    private int mScreenDensity;
    private int mScreenPortraitWidth = DEFAULT_SIZE;
    private int mScreenPortraitHeight = DEFAULT_SIZE;

    private Handler mMainHandler = new Handler(Looper.getMainLooper());

    private MyBinder mBinder = new MyBinder();

    private class MyBinder extends Binder implements CaptureListener {

        @Override
        public void showCaptureWindow() {
            showWindow();
        }

        @Override
        public void hideCaptureWindow() {
            hideCaptureWindow();
        }

        @Override
        public void create(Intent intent, int width, int height) {
            initData(intent, width, height);
        }
    }

    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if ((event.values[0] > 17 || event.values[1] > 17 || event.values[2] > 17) && !mIsShowing) {
                if (initImageReader()) {
                    showWindow();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.e(TAG, "onAccuracyChanged");
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Log.e(TAG, "onCreate");
    }

    private boolean initImageReader() {
        if (mScreenPortraitWidth == DEFAULT_SIZE && mScreenPortraitWidth == DEFAULT_SIZE) {
            return false;
        }

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mScreenWidth = mScreenPortraitWidth;
            mScreenHeight = mScreenPortraitHeight;
        } else {
            mScreenWidth = mScreenPortraitHeight;
            mScreenHeight = mScreenPortraitWidth;
        }
        mScreenDensity = ScreenUtil.getScreenDensityDpi(getApplicationContext());
        mImageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, PixelFormat.RGBA_8888, 1);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {

            // 每当生成一个新的Image 的时候都会回调onImageAvailable
            // Native 层调用的代码
            @Override
            public void onImageAvailable(ImageReader reader) {
                if (reader == null) {
                    return;
                }
                Image image = reader.acquireLatestImage();
                if (image == null) {
                    startScreenShot();
                } else {
                    if (mCropView != null) {
                        Bitmap bitmap = createScreenBitmap(image);
                        SaveScreenTask mSaveScreenTask = new SaveScreenTask(mCropView.getCropLeft(), mCropView.getCropTop(),
                                mCropView.getCropWidth(), mCropView.getCropHeight());
                        AsyncTaskCompat.executeParallel(mSaveScreenTask, bitmap);
                    }
                }

                if (image != null) {
                    image.close();
                }

                reader.close();

                Log.e(TAG, "onImageAvailable " + image + " " + reader);
            }
        }, mMainHandler);

        return true;
    }

    @NonNull
    private Bitmap createScreenBitmap(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Bitmap bitmap = Bitmap.createBitmap(width + (pixelStride == 0 ? 0 : rowPadding / pixelStride), height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        return bitmap;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.e(TAG, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
    }

    /**
     * 展示Window
     */
    private void showWindow() {
        if (mScreenHeight == 0 || mScreenWidth == 0) {
            return;
        }

        mWindowView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.window_capture, null);
        mWindowView.setFocusable(true);
        mWindowView.setFocusableInTouchMode(true);
        mWindowView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    hideWindow();
                }
                return false;
            }
        });
        mWindowView.findViewById(R.id.crop_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takeScreenShot();
            }
        });
        mCropView = (CropView) mWindowView.findViewById(R.id.crop_view);
        mCropView.setDefaultSize(mScreenWidth, mScreenHeight);
        mWindowView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.type = WindowManager.LayoutParams.TYPE_TOAST; // 使用什么
        params.flags &= ~(WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM |
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SPLIT_TOUCH);
        params.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        params.height = mScreenHeight;
        params.width = mScreenWidth;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        } else {
            params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        }
        params.format = PixelFormat.RGBA_8888;
        params.setTitle("FloatingWindow");
        mWindowManager.addView(mWindowView, params);

        mIsShowing = true;
    }

    private boolean hideWindow() {
        if (mWindowManager != null && mWindowView != null && mIsShowing) {
            mWindowManager.removeView(mWindowView);
            mIsShowing = false;
            return true;
        }
        return false;
    }

    public void takeScreenShot() {
        startScreenShot();
    }

    private void startScreenShot() {
        hideWindow();

        mMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startVirtual();
            }
        }, 10); // 保证hideWindow 以后startVirtual()
    }

    public void startVirtual() {
        if (mMediaProjection != null) {
            virtualDisplay();
        }
    }

    private void virtualDisplay() {
        releaseImageReader();
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-shot",
                mScreenWidth, mScreenHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(), null, null);
    }

    private void releaseImageReader() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind intent = " + intent);

        Sensor accelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometerSensor != null) {
            mSensorManager.registerListener(mSensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);
        }

        return mBinder;
    }

    private void initData(Intent data, int width, int height) {
        mMediaProjection = ((MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE))
                .getMediaProjection(Activity.RESULT_OK, data);

        mScreenPortraitWidth = width <= 0 ? DEFAULT_SIZE : width;
        mScreenPortraitHeight = height <= 0 ? DEFAULT_SIZE : height;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mSensorEventListener);
        }
        releaseImageReader();

        Log.e(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    public class SaveScreenTask extends AsyncTask<Bitmap, Void, String> {

        private int mLeft;
        private int mTop;
        private int mWidth;
        private int mHeight;

        public SaveScreenTask(int left, int top, int width, int height) {
            mLeft = left;
            mTop = top;
            mWidth = width;
            mHeight = height;
        }

        @Override
        protected String doInBackground(Bitmap... params) {

            if (params == null || params.length < 1 || params[0] == null) {

                return null;
            }

            Bitmap bitmap = params[0];
            bitmap = Bitmap.createBitmap(bitmap, mLeft, mTop, mWidth, mHeight);
            FileOutputStream out = null;
            File fileImage = null;
            if (bitmap != null) {
                try {
                    fileImage = new File(FileUtil.getScreenShotsName(getApplicationContext()));
                    out = new FileOutputStream(fileImage);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    fileImage = null;
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            return fileImage != null ? fileImage.getAbsolutePath() : null;
        }

        @Override
        protected void onPostExecute(String uri) {
            super.onPostExecute(uri);
            if (uri != null) {
                Intent intent = new Intent();
                intent.setClassName("com.egos.capture", "com.egos.capture.PreviewPictureActivity");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(PreviewPictureActivity.EXTRA_URI, uri);
                startActivity(intent);
            }

        }
    }
}
