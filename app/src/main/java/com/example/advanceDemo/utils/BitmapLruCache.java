package com.example.advanceDemo.utils;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;

public class BitmapLruCache {

    private final static String TAG = "BitmapLruCache";
    private static final int MAX_NUMBER = 8;
    private final LruCache<String, Bitmap> mMemCache;
    private int writeIndex = 0;

    public BitmapLruCache() {

        // Use 20% of the available memory for this memory cache.
        final long cacheSize = Runtime.getRuntime().maxMemory() / MAX_NUMBER;

        mMemCache = new LruCache<String, Bitmap>((int) cacheSize) {

            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
    }

    public static long getMaxCacheSize() {
        return Runtime.getRuntime().maxMemory() / MAX_NUMBER;
    }

    public synchronized Bitmap getBitmap(int index) {

        String key = "index:" + index;
        final Bitmap b = mMemCache.get(key);

        if (b == null) {
            Log.w(TAG, "getNextBitmap  is null. remove from cache");
            mMemCache.remove(key);
            return null;
        }
        return b;
    }

    public synchronized void pushBitmap(Bitmap bitmap) {
        String key = "index:" + writeIndex;
        writeIndex++;
        if (key != null && bitmap != null) {
            mMemCache.put(key, bitmap);
        }
    }

    // public synchronized Bitmap getBitmap(String key) {
    //
    // final Bitmap b = mMemCache.get(key);
    //
    // if (b == null){
    // mMemCache.remove(key);
    // return null;
    // }
    // return b;
    // }

    // public synchronized Bitmap getBitmap(long ptsUs) {
    //
    // String key="pts:" + ptsUs;
    // final Bitmap b = mMemCache.get(key);
    //
    // if (b == null){
    // mMemCache.remove(key);
    // return null;
    // }
    // return b;
    // }
    // public synchronized void pushBitmap(long ptsUs, Bitmap bitmap) {
    // String key="pts:" + ptsUs;
    // if (key != null && bitmap != null && getBitmap(ptsUs) == null) {
    // mMemCache.put(key, bitmap);
    // }
    // }

    public synchronized void clear() {
        Log.i(TAG, "============mMemCache.evictAll()");
        mMemCache.evictAll();
    }

    public synchronized int count() {
        return mMemCache.putCount();
    }

    // private Bitmap getBitmapFromMemCache(long ptsUs) {
    // return getBitmapFromMemCache("pts:" + ptsUs);
    // }
    //
    // private void addBitmapToMemCache(long ptsUs, Bitmap bitmap) {
    // addBitmapToMemCache("pts:" + ptsUs, bitmap);
    // }
    // public static Bitmap getFromResource(Resources res, int resId) {
    // BitmapCache cache = BitmapCache.getInstance();
    // Bitmap bitmap = cache.getBitmapFromMemCache(resId);
    // if (bitmap == null) {
    // bitmap = BitmapFactory.decodeResource(res, resId);
    // cache.addBitmapToMemCache(resId, bitmap);
    // }
    // return bitmap;
    // }
}
