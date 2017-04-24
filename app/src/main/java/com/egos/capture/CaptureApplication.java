package com.egos.capture;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import com.squareup.leakcanary.LeakCanary;

/**
 * Created by Egos on 2017/4/13.
 */

public class CaptureApplication extends Application {

    private CaptureListener mCaptureListener;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mCaptureListener = (CaptureListener) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mCaptureListener = null;
        }
    };

    public CaptureListener getCaptureListener() {
        return mCaptureListener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initLeakCanary();

        bindService(new Intent(this, CaptureService.class), connection, Context.BIND_AUTO_CREATE);
    }

    private void initLeakCanary() {
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(this);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        unbindCaptureService();
    }

    public void unbindCaptureService(){
        unbindService(connection);
    }
}
