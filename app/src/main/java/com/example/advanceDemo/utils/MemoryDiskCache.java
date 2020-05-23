package com.example.advanceDemo.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;
import androidx.collection.LruCache;

import com.example.advanceDemo.utils.DiskLruCache.Snapshot;
import com.lansosdk.videoeditor.LanSongFileUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 图片缓存帮助类 包含内存缓存LruCache和磁盘缓存DiskLruCache
 *
 * @author Javen
 */
public class MemoryDiskCache {

    private final static String KEY_INDEX = "index";
    // 磁盘缓存大小
    private static final int DISKMAXSIZE = 200 * 1024 * 1024;
    private String TAG = MemoryDiskCache.this.getClass().getSimpleName();
    // 缓存类
    private LruCache<String, Bitmap> mLruCache;
    private DiskLruCache mDiskLruCache;
    private int writeIndex = 0;
    private File cacheFile = null;
    private int memorySize;
    /**
     * 最大是2G
     */
    private int sizeCount = 0;

    public MemoryDiskCache(Context context) {
        memorySize = (int) (Runtime.getRuntime().maxMemory() / 5);
        mLruCache = new LruCache<String, Bitmap>(memorySize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getRowBytes() * bitmap.getHeight();
            }
        };
        try {
            // 获取DiskLruCahce对象
            cacheFile = getDiskCacheDir(context.getApplicationContext(),
                    "diskImageCache");
            if (cacheFile.exists()) {
                cacheFile.delete();
            }
            mDiskLruCache = DiskLruCache.open(cacheFile,
                    getAppVersion(context), 1, DISKMAXSIZE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 该方法会判断当前sd卡是否存在，然后选择缓存地址
     *
     * @param context
     * @param uniqueName
     * @return
     */
    public static File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }

    public synchronized void pushBitmap(Bitmap bitmap) {

        String key = KEY_INDEX + writeIndex;
        writeIndex++;
        if (sizeCount < memorySize * 0.8f) {
            mLruCache.put(key, bitmap);
            sizeCount += bitmap.getByteCount();
        } else {
            saveToDiskCache(key, bitmap);
        }
    }
    public synchronized void pushBitmap(String key,Bitmap bitmap) {
        if (sizeCount < memorySize * 0.8f) {
            mLruCache.put(key, bitmap);
            sizeCount += bitmap.getByteCount();
        } else {
            saveToDiskCache(key, bitmap);
        }
    }
    public synchronized Bitmap getBitmap(String key) {
        if (mLruCache.get(key) != null) {
            return mLruCache.get(key);
        } else {
            try {
                if (mDiskLruCache.get(key) != null) {
                    // 从DiskLruCahce取
                    Snapshot snapshot = mDiskLruCache.get(key);
                    Bitmap bitmap = null;
                    if (snapshot != null) {
                        bitmap = BitmapFactory.decodeStream(snapshot
                                .getInputStream(0));
                        mLruCache.put(key, bitmap);
                    }
                    return bitmap;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    /**
     * 从缓存（内存缓存，磁盘缓存）中获取Bitmap
     */
    public synchronized Bitmap getBitmap(int index) {
        String key = KEY_INDEX + index;
        if (mLruCache.get(key) != null) {
            return mLruCache.get(key);
        } else {
            try {
                if (mDiskLruCache.get(key) != null) {
                    // 从DiskLruCahce取
                    Snapshot snapshot = mDiskLruCache.get(key);
                    Bitmap bitmap = null;
                    if (snapshot != null) {
                        bitmap = BitmapFactory.decodeStream(snapshot
                                .getInputStream(0));
                        mLruCache.put(key, bitmap);
                    }
                    return bitmap;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void clear() {
        if (mLruCache != null) {
            mLruCache.evictAll();
            mLruCache = null;
        }
        if (mDiskLruCache != null) {
            try {
                mDiskLruCache.close();
                mDiskLruCache = null;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        LanSongFileUtil.deleteDir(cacheFile);

    }

    private void saveToDiskCache(String key, Bitmap bitmap) {
        // 判断是否存在DiskLruCache缓存，若没有存入
        try {
            // long beforeDraw=System.currentTimeMillis();
            DiskLruCache.Editor editor = mDiskLruCache.edit(key);
            if (editor != null) {
                OutputStream outputStream = editor.newOutputStream(0);
                /**
                 * 2017年7月5日11:36:09: 测试后发现如果换算成png,因为png是无损压缩, 会导致数据过大,
                 * 从而写入到磁盘的速度也很慢 不然先在内存里用jpeg压缩下,然后写入的速度快.
                 */
                if (bitmap.compress(CompressFormat.JPEG, 100, outputStream)) {
                    editor.commit();
                } else {
                    editor.abort();
                }
            }
            mDiskLruCache.flush();
            // Log.i("TIME","putBitmap 消耗时间是:"+ (System.currentTimeMillis() -
            // beforeDraw));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取应用版本号
     *
     * @param context
     * @return
     */
    public int getAppVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            return info.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }

}