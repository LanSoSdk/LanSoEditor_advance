package com.example.advanceDemo;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;

import com.lansosdk.videoeditor.CopyFileFromAssets;
import com.lansosdk.videoeditor.MediaInfo;

public class DemoApplication extends Application {

    private static DemoApplication instance;

    public static DemoApplication getInstance() {
        if (instance == null) {
            throw new NullPointerException("DemoApplication instance is null");
        }
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public Context getContext() {
        return getBaseContext();
    }

    public Resources getResources() {
        return getBaseContext().getResources();
    }





}
