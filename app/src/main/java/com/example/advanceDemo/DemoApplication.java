package com.example.advanceDemo;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;

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


    /**
     * 当前正在处理的视频;
     */
    public String currentEditVideo;


}
