package com.example.advanceDemo.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class BitmapUtils {

    /**
     * 缩放图片
     */
    public static void bitmapScale(Bitmap baseBitmap, Paint paint, float x,
                                   float y) {
        // 因为要将图片放大，所以要根据放大的尺寸重新创建Bitmap
        Bitmap scaleBitmap = Bitmap.createBitmap(
                (int) (baseBitmap.getWidth() * x),
                (int) (baseBitmap.getHeight() * y), baseBitmap.getConfig());
        Canvas canvas = new Canvas(scaleBitmap);
        // 初始化Matrix对象
        Matrix matrix = new Matrix();
        // 根据传入的参数设置缩放比例
        matrix.setScale(x, y);
        // 根据缩放比例，把图片draw到Canvas上
        canvas.drawBitmap(baseBitmap, matrix, paint);
    }

    /**
     * 图片旋转
     */
    public static void bitmapRotate(Bitmap baseBitmap, Paint paint,
                                    float degrees) {
        // 创建一个和原图一样大小的图片
        Bitmap afterBitmap = Bitmap.createBitmap(baseBitmap.getWidth(),
                baseBitmap.getHeight(), baseBitmap.getConfig());
        Canvas canvas = new Canvas(afterBitmap);
        Matrix matrix = new Matrix();
        // 根据原图的中心位置旋转
        matrix.setRotate(degrees, baseBitmap.getWidth() / 2,
                baseBitmap.getHeight() / 2);
        canvas.drawBitmap(baseBitmap, matrix, paint);
    }

    /**
     * 图片移动
     */
    public static void bitmapTranslate(Bitmap baseBitmap, Paint paint,
                                       float dx, float dy) {
        // 需要根据移动的距离来创建图片的拷贝图大小
        Bitmap afterBitmap = Bitmap.createBitmap(
                (int) (baseBitmap.getWidth() + dx),
                (int) (baseBitmap.getHeight() + dy), baseBitmap.getConfig());
        Canvas canvas = new Canvas(afterBitmap);
        Matrix matrix = new Matrix();
        // 设置移动的距离
        matrix.setTranslate(dx, dy);
        canvas.drawBitmap(baseBitmap, matrix, paint);
    }

    /**
     * 倾斜图片
     */
    public static void bitmapSkew(Bitmap baseBitmap, Paint paint, float dx,
                                  float dy) {
        // 根据图片的倾斜比例，计算变换后图片的大小，
        Bitmap afterBitmap = Bitmap.createBitmap(baseBitmap.getWidth()
                + (int) (baseBitmap.getWidth() * dx), baseBitmap.getHeight()
                + (int) (baseBitmap.getHeight() * dy), baseBitmap.getConfig());
        Canvas canvas = new Canvas(afterBitmap);
        Matrix matrix = new Matrix();
        // 设置图片倾斜的比例
        matrix.setSkew(dx, dy);
        canvas.drawBitmap(baseBitmap, matrix, paint);
    }

    public static Bitmap decodeFromResource(Context context, int id) {
        Resources res = context.getResources();
        Bitmap bitmap = BitmapFactory.decodeResource(res, id).copy(
                Bitmap.Config.ARGB_8888, true);
        return bitmap;
    }

    /**
     * 保存图片到SD卡
     */
    public static void saveToSdCard(String path, Bitmap bitmap) {
        if (null != bitmap && null != path && !path.equalsIgnoreCase("")) {
            try {
                File file = new File(path);
                FileOutputStream outputStream = null;
                // 创建文件，并写入内容
                outputStream = new FileOutputStream(new File(path), true);
                bitmap.compress(Bitmap.CompressFormat.PNG, 30, outputStream);
                outputStream.flush();
                outputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 复制bitmap
     */
    public static Bitmap duplicateBitmap(Bitmap bmpSrc, int width, int height) {
        if (null == bmpSrc) {
            return null;
        }

        int bmpSrcWidth = bmpSrc.getWidth();
        int bmpSrcHeight = bmpSrc.getHeight();

        Bitmap bmpDest = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        if (null != bmpDest) {
            Canvas canvas = new Canvas(bmpDest);
            Rect viewRect = new Rect();
            final Rect rect = new Rect(0, 0, bmpSrcWidth, bmpSrcHeight);
            if (bmpSrcWidth <= width && bmpSrcHeight <= height) {
                viewRect.set(rect);
            } else if (bmpSrcHeight > height && bmpSrcWidth <= width) {
                viewRect.set(0, 0, bmpSrcWidth, height);
            } else if (bmpSrcHeight <= height && bmpSrcWidth > width) {
                viewRect.set(0, 0, width, bmpSrcWidth);
            } else if (bmpSrcHeight > height && bmpSrcWidth > width) {
                viewRect.set(0, 0, width, height);
            }
            canvas.drawBitmap(bmpSrc, rect, viewRect, null);
        }

        return bmpDest;
    }

    /**
     * 复制bitmap
     */
    public static Bitmap duplicateBitmap(Bitmap bmpSrc) {
        if (null == bmpSrc) {
            return null;
        }

        int bmpSrcWidth = bmpSrc.getWidth();
        int bmpSrcHeight = bmpSrc.getHeight();

        Bitmap bmpDest = Bitmap.createBitmap(bmpSrcWidth, bmpSrcHeight,
                Config.ARGB_8888);
        if (null != bmpDest) {
            Canvas canvas = new Canvas(bmpDest);
            final Rect rect = new Rect(0, 0, bmpSrcWidth, bmpSrcHeight);

            canvas.drawBitmap(bmpSrc, rect, rect, null);
        }

        return bmpDest;
    }

    /**
     * bitmap转字节码
     */
    public static byte[] bitampToByteArray(Bitmap bitmap) {
        byte[] array = null;
        try {
            if (null != bitmap) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                array = os.toByteArray();
                os.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return array;
    }
}
