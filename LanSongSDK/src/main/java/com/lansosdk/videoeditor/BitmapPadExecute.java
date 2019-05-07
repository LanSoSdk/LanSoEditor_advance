package com.lansosdk.videoeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.LSLog;
import com.lansosdk.box.onDrawPadOutFrameListener;

import java.util.List;

import com.lansosdk.LanSongFilter.LanSongFilter;
import com.lansosdk.LanSongFilter.LanSongScreenBlendFilter;

/**
 * 一帧操作,
 * <p>
 * 是对DrawPad后台的再次封装, 用来把图片, 文字, 等生成一张图片,然后返回.
 * <p>
 * 如果您有特殊的需求, 想修改代码,来实现不同的需求, 可以拷贝这个类, 在其中做修改. 或者继承这个类, 扩展相关的方法.
 * <p>
 * 代码调用:
 * <p>
 * init()
 * <p>
 * {中间多种形式的 图片混合方法, 可以多次调用, }
 * <p>
 * <p>
 * release(); 使用完毕后, release掉.
 */
public class BitmapPadExecute {
    private static final String TAG = LSLog.TAG;
    private final Object mLock = new Object();
    protected DrawPadPictureExecute mDrawPad = null;
    private Bitmap OutBmp;
    private Object OutBmpLock = new Object();
    private Context mContext;

    public BitmapPadExecute(Context ct) {
        mContext = ct;
    }

    /**
     * 准备一下, 执行建立DrawPad, 配置各种选项.
     *
     * @param width  读取图片时的宽度,
     * @param height 读取图片时的高度.
     * @return
     */
    public boolean init(int width, int height) {
        // 创建pad,并设置禁止编码, outframe回调.
        mDrawPad = new DrawPadPictureExecute(mContext, width, height, -1, 25,
                1000000, null);
        mDrawPad.setDisableEncode(true);
        mDrawPad.setOutFrameInDrawPad(true);

        mDrawPad.setCheckDrawPadSize(true);

        mDrawPad.setDrawPadOutFrameListener(true,
                new onDrawPadOutFrameListener() {

                    @Override
                    public void onDrawPadOutFrame(DrawPad v, Object obj,
                                                  int type, long ptsUs) {
                        // TODO Auto-generated method stub
                        Bitmap bmp = (Bitmap) obj;

                        if (bmp != null && mDrawPad != null) {
                            mDrawPad.pauseRecord();
                            mDrawPad.resetOutFrames();
                            OutBmp = bmp;
                            notifyReady();
                        }
                    }
                });
        /**
         * 设置暂停标志, 然后开启DrawPad
         */
        mDrawPad.pauseRecord();
        return mDrawPad.startDrawPad();
    }

    /**
     * 投进去两个图片, 返回融合好 的图片. (可以多次执行) 返回的图片大小等于在prepare的时候设置的大小.
     *
     * @param bmp1 主图片, (可以来自提取的视频帧等)
     * @param bmp2 效果图片
     * @return 注意:如果图片很大, 读取很慢, 则可能返回null;
     */
    public synchronized Bitmap getBlendBitmap(Bitmap bmp1, Bitmap bmp2) {
        mDrawPad.pauseRecord();
        mDrawPad.removeAllLayer();
        // 放进去.
        LanSongScreenBlendFilter filter = new LanSongScreenBlendFilter();
        filter.setBitmap(bmp2);

        BitmapLayer layer = mDrawPad.addBitmapLayer(bmp1, filter);
        layer.setScaledValue(layer.getPadWidth(), layer.getPadHeight());
        mDrawPad.resumeRecord();

        // 等待.
        OutBmp = null;
        waitUntilReady();

        return OutBmp;
    }

    /**
     * 给图片增加滤镜
     * <p>
     * 此方法,只能给一张图片增加一个滤镜, 如果你要给一张图片增加多个滤镜, 请参考此文件的最后几行代码.
     *
     * @param bmp1
     * @param filter
     * @return 滤镜处理后的图片.
     */
    public synchronized Bitmap getFilterBitmap(Bitmap bmp1,
                                               LanSongFilter filter) {
        if (bmp1 != null && !bmp1.isRecycled() && filter != null) {
            mDrawPad.pauseRecord();
            mDrawPad.removeAllLayer();
            // 放进去.
            BitmapLayer layer = mDrawPad.addBitmapLayer(bmp1, filter);
            layer.setScaledValue(layer.getPadWidth(), layer.getPadHeight());
            mDrawPad.resumeRecord();

            // 等待.
            OutBmp = null;
            waitUntilReady();
            return OutBmp;
        } else {
            Log.e(TAG, "get Filter Bitmap 错误!");
            return null;
        }
    }

    /**
     * 多个图片叠加在一起.
     *
     * @param bmps
     * @return
     */
    public synchronized Bitmap bitmapOverlayer(List<Bitmap> bmps) {
        if (bmps != null && bmps.size() > 0) {
            mDrawPad.pauseRecord();
            mDrawPad.removeAllLayer();
            BitmapLayer layerFirst = null;
            // 放进去.
            for (Bitmap bmp : bmps) {
                BitmapLayer layer2 = mDrawPad.addBitmapLayer(bmp, null);
                if (layerFirst == null) {
                    layerFirst = layer2;
                }
            }
            // 等于第一个的大小.
            layerFirst.setScaledValue(layerFirst.getPadWidth(),
                    layerFirst.getPadHeight());

            mDrawPad.resumeRecord();

            // 等待.
            OutBmp = null;
            waitUntilReady();
            return OutBmp;
        } else {
            Log.e(TAG, "get Filter Bitmap 错误!");
            return null;
        }
    }

    /**
     * 最后使用完毕后,释放
     */
    public void release() {
        if (mDrawPad != null) {
            mDrawPad.release();
            mDrawPad = null;
        }
    }

    private void waitUntilReady() {
        synchronized (mLock) {
            try {
                mLock.wait(1000);
            } catch (InterruptedException ie) { /* not expected */
            }
        }
    }

    private void notifyReady() {
        synchronized (mLock) {
            mLock.notify();
        }
    }
    /**
     * 测试代码 private void testDrawPadExecute(Bitmap bmp1,Bitmap bmp2) { BitmapPad
     * bendBmp; bendBmp=new BitmapPad(getApplicationContext());
     *
     * if(bendBmp.init(bmp1.getWidth()/2,bmp1.getHeight()/2)) { Bitmap
     * bmp=bendBmp.getBlendBitmap(bmp1, bmp2); TestFrames.saveBitmap(bmp);
     *
     * bmp=bendBmp.getFilterBitmap(bmp1, new LanSongSwirlFilter());
     * TestFrames.saveBitmap(bmp); bmp=bendBmp.getFilterBitmap(bmp1, new
     * LanSongSepiaFilter()); TestFrames.saveBitmap(bmp); } bendBmp.release(); } }
     */
    /**
     * 给一个图片, 增加多个滤镜的方法演示. private void testFile() {
     *
     * testGetFilters(); //执行一次; new Thread(new Runnable() {
     *
     * @Override public void run() { testGetFilters(); //放到另一个线程执行一次. }
     *           }).start(); }
     *
     *           给一张图片, 增加多个滤镜. private void testGetFilters() {
     *           ArrayList<LanSongFilter> filters=new
     *           ArrayList<LanSongFilter>(); filters.add(new
     *           LanSongSepiaFilter()); filters.add(new LanSongSwirlFilter());
     *           filters.add(new LanSongBulgeDistortionFilter());
     *
     *           Bitmap
     *           bmp=BitmapFactory.decodeFile(CopyFileFromAssets.copyAssets
     *           (getApplicationContext(), "t14.jpg"));
     *
     *           //------------------------一下是调用流程. //创建对象,传递参数 (此类可被多个线程同时开启执行)
     *           BitmapGetFilters getFilter=new
     *           BitmapGetFilters(getApplicationContext(), bmp,filters); //设置回调,
     *           注意:回调是在 getFilter.setDrawpadOutFrameListener(new
     *           onGetFiltersOutFrameListener() {
     * @Override public void onOutFrame(BitmapGetFilters v, Object obj) { //
     *           TODO Auto-generated method stub Bitmap bmp2=(Bitmap)obj;
     *           Log.i(TAG,"DrawPad is:"+bmp2.getWidth()+ bmp2.getHeight());
     *           TestFrames.saveBitmap(bmp2); } }); getFilter.start(); }
     */
}
