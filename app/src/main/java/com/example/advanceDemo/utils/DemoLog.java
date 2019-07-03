package com.example.advanceDemo.utils;

import android.util.Log;

public class DemoLog {

    private static final int VERBOSE = 2;

    private static final int DEBUG = 3;
    private static final int INFO = 4;
    private static final int WARN = 5;
    private static final int ERROR = 6;


    private static String TAG = "LanSongDemo";

    public static void i(String var2) {
        Log.i(TAG, var2);
    }

    public static void d(String var2) {
        Log.d(TAG, var2);
    }

    public static void w(String var2) {
        Log.w(TAG, var2);
    }

    public static void e(String var2) {
        Log.e(TAG, var2);
    }

    public static void e(String msg, Throwable tr) {
        Log.e(TAG, msg, tr);
    }
}
