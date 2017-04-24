package com.egos.capture;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import com.egos.capture.util.ScreenUtil;

/**
 * Created by Egos on 2017/4/13.
 */

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_MEDIA_PROJECTION = 1;
    private static final int REQUEST_STORAGE = 2;

    private Intent mCapturePermission;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ScreenUtil.setScreenSize(this);

        requestCapturePermission();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_MEDIA_PROJECTION:
                if (data != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            mCapturePermission = data;
                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE);
                        } else {
                            buildCaptureData(data);
                        }
                    } else {
                        buildCaptureData(data);
                    }
                }
                break;
        }
    }

    private void buildCaptureData(@NonNull Intent data) {
        if ((((CaptureApplication) getApplicationContext()).getCaptureListener()) != null) {
            (((CaptureApplication) getApplicationContext()).getCaptureListener()).create(data, ScreenUtil.screentWidth, ScreenUtil.screentHeight);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (mCapturePermission != null) {
                        buildCaptureData(mCapturePermission);
                    } else {
                        unbindCaptureService();
                    }
                } else {
                    unbindCaptureService();
                }
                break;
        }
    }

    private void unbindCaptureService() {
        ((CaptureApplication) getApplicationContext()).unbindCaptureService();
        finish();
    }

    @Override
    public boolean shouldShowRequestPermissionRationale(String permission) {
        return super.shouldShowRequestPermissionRationale(permission);
    }

    private void requestCapturePermission() {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION);
    }

    public void showWindow(View view) {
    }


    public void screenShot(View view) {
    }

}
