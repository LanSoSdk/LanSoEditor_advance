package com.lansosdk.videoeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.CanvasLayer;
import com.lansosdk.box.DataLayer;
import com.lansosdk.box.DrawPadBitmapRunnable;
import com.lansosdk.box.GifLayer;
import com.lansosdk.box.LSLog;
import com.lansosdk.box.Layer;
import com.lansosdk.box.MVLayer;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadErrorListener;
import com.lansosdk.box.onDrawPadOutFrameListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.box.onDrawPadThreadProgressListener;

import java.util.ArrayList;

import com.lansosdk.LanSongFilter.LanSongFilter;

public class DrawPadPictureExecute {

    private static final String TAG = LSLog.TAG;
    DrawPadBitmapRunnable renderer;
    private int padWidth, padHeight;
    private boolean mPauseRecord = false;

    /**
     * DrawPad的图片转换为视频的后台执行
     *
     * @param ctx        语境,android的Context
     * @param padwidth   DrawPad的的宽度
     * @param padheight  DrawPad的的高度
     * @param durationMs 视频时长, 单位毫秒
     * @param framerate  帧率
     * @param bitrate    编码视频所希望的码率,比特率,设置的越大,则文件越大, 设置小一些会起到视频压缩的效果.
     * @param dstPath    编码视频保存的路径.
     */
    public DrawPadPictureExecute(Context ctx, int padwidth, int padheight,
                                 int durationMs, int framerate, int bitrate, String dstPath) {
        if (renderer == null) {
            renderer = new DrawPadBitmapRunnable(ctx, padwidth, padheight,
                    durationMs, framerate, bitrate, dstPath);
        }
        this.padWidth = padwidth;
        this.padHeight = padheight;
    }

    protected static int make32Multi(int value) {
        if (value < 32) {
            return 32;
        } else {
            value += 16;
            int val2 = value / 32;
            val2 *= 32;
            return val2;
        }
    }

    public void setCheckDrawPadSize(boolean check) {
        if (renderer != null) {
            renderer.setCheckDrawPadSize(check);
        }
    }

    /**
     * 在您配置了 OutFrame, 要输出每一帧的时候, 是否要禁止编码器. 当你只想要处理后的 数据, 而暂时不需要编码成最终的目标文件时,
     * 把这里设置为true. 默认是false;
     *
     * @param dis
     */
    public void setDisableEncode(boolean dis) {
        if (renderer != null && !renderer.isRunning()) {
            renderer.setDisableEncode(dis);
        }
    }

    public void setLanSongVideoMode(boolean is) {
        if (renderer != null) {
            renderer.setEditModeVideo(is);
        }
    }

    public boolean startDrawPad() {
        boolean ret = false;
        if (renderer != null && !renderer.isRunning()) {
            ret = renderer.startDrawPad();
        }
        if (!ret) {
            Log.e(TAG, "开启DrawPad 后台执行失败");
        }
        return ret;
    }

    public boolean startDrawPad(boolean pause) {

        if (renderer != null && !renderer.isRunning()) {
            return renderer.startDrawPad(pause);
        } else {
            return false;
        }
    }

    public void stopDrawPad() {
        release();
    }

    public void release() {
        if (renderer != null) {
            renderer.release();
            renderer = null;
        }
    }

    public void bringToBack(Layer lay) {
        if (renderer != null && renderer.isRunning()) {
            renderer.bringToBack(lay);
        }
    }

    public void bringToFront(Layer lay) {
        if (renderer != null && renderer.isRunning()) {
            renderer.bringToFront(lay);
        }
    }

    public BitmapLayer addBitmapLayer(Bitmap bmp, LanSongFilter filter) {
        if (renderer != null && renderer.isRunning()) {
            return renderer.addBitmapLayer(bmp, filter);
        } else {
            Log.i(TAG, "add------------eeee" + renderer.isRunning());
            return null;
        }
    }

    public DataLayer addDataLayer(int dataWidth, int dataHeight) {
        if (renderer != null && renderer.isRunning()) {
            return renderer.addDataLayer(dataWidth, dataHeight);
        } else {
            return null;
        }
    }

    public CanvasLayer addCanvasLayer() {
        if (renderer != null && renderer.isRunning()) {
            return renderer.addCanvasLayer();
        } else {
            return null;
        }
    }

    public MVLayer addMVLayer(String srcPath, String maskPath, boolean isAsync) {
        if (renderer != null && renderer.isRunning()) {
            return renderer.addMVLayer(srcPath, maskPath, isAsync);
        } else {
            return null;
        }
    }

    public MVLayer addMVLayer(String srcPath, String maskPath) {
        if (renderer != null && renderer.isRunning()) {
            return renderer.addMVLayer(srcPath, maskPath);
        } else {
            return null;
        }
    }

    public GifLayer addGifLayer(String gifPath) {

        if (renderer != null && renderer.isRunning()) {
            return renderer.addGifLayer(gifPath);
        } else {
            return null;
        }
    }

    public GifLayer addGifLayer(int resId) {
        if (renderer != null && renderer.isRunning()) {
            return renderer.addGifLayer(resId);
        } else {
            return null;
        }
    }

    public void removeLayer(Layer lay) {
        if (renderer != null && renderer.isRunning() && lay != null) {
            renderer.removeLayer(lay);
        }
    }

    public void removeAllLayer() {
        if (renderer != null && renderer.isRunning()) {
            renderer.removeAllLayer();
        }
    }

    /**
     * 切换滤镜
     *
     * @param layer  为哪个图层
     * @param filter 滤镜对象, 无滤镜则可以为null
     */
    public void switchFilterTo(Layer layer, LanSongFilter filter) {
        if (renderer != null && renderer.isRunning()) {
            renderer.switchFilterTo(layer, filter);
        }
    }

    /**
     * 切换滤镜 为一个图层切换多个滤镜. 即一个滤镜处理完后的输出, 作为下一个滤镜的输入.
     * <p>
     * filter的列表, 是先add进去,最新渲染, 把第一个渲染的结果传递给第二个,第二个传递给第三个,以此类推.
     * <p>
     * 注意: 这里内部会在切换的时候, 会销毁 之前的列表中的所有滤镜对象, 然后重新增加, 故您不可以把同一个滤镜对象再次放到进来,
     * 您如果还想使用之前的滤镜,则应该重新创建一个对象.
     *
     * @param layer
     * @param filters
     */
    public void switchFilterList(Layer layer, ArrayList<LanSongFilter> filters) {
        if (renderer != null && renderer.isRunning()) {
            renderer.switchFilterList(layer, filters);
        }
    }

    /**
     * DrawPad每执行完一帧画面,会调用这个Listener,返回的timeUs是当前画面的时间戳(微妙),
     * 可以利用这个时间戳来做一些变化,比如在几秒处缩放, 在几秒处平移等等.从而实现一些动画效果.
     */
    public void setDrawPadProgressListener(onDrawPadProgressListener listener) {
        if (renderer != null) {
            renderer.setDrawPadProgressListener(listener);
        }
    }

    /**
     * 方法与 onDrawPadProgressListener不同的地方在于: 即将开始一帧渲染的时候,
     * 直接执行这个回调中的代码,不通过Handler传递出去,你可以精确的执行一些这一帧的如何操作.
     * <p>
     * 故不能在回调 内增加各种UI相关的代码.
     */
    public void setDrawPadThreadProgressListener(
            onDrawPadThreadProgressListener listener) {
        if (renderer != null) {
            renderer.setDrawPadThreadProgressListener(listener);
        }
    }

    /**
     * DrawPad执行完成后的回调.
     *
     * @param listener
     */
    public void setDrawPadCompletedListener(onDrawPadCompletedListener listener) {
        if (renderer != null) {
            renderer.setDrawPadCompletedListener(listener);
        }
    }

    /**
     * 设置当前DrawPad运行错误的回调监听.
     *
     * @param listener
     */
    public void setDrawPadErrorListener(onDrawPadErrorListener listener) {
        if (renderer != null) {
            renderer.setDrawPadErrorListener(listener);
        }
    }

    /**
     * 设置每处理一帧的数据预览监听, 等于把当前处理的这一帧的画面拉出来, 您可以根据这个画面来自行的编码保存, 或网络传输.
     * <p>
     * 建议在这里拿到数据后, 放到queue中, 然后在其他线程中来异步读取queue中的数据, 请注意queue中数据的总大小, 要及时处理和释放,
     * 以免内存过大,造成OOM问题
     *
     * @param listener 监听对象
     */
    public void setDrawPadOutFrameListener(onDrawPadOutFrameListener listener) {
        if (renderer != null) {
            renderer.setDrawpadOutFrameListener(padWidth, padHeight, 1,
                    listener);
        }
    }

    public void setDrawPadOutFrameListener(boolean isMulti,
                                           onDrawPadOutFrameListener listener) {
        if (renderer != null) {
            int w = padWidth;
            int h = padHeight;
            if (isMulti) {
                w = make32Multi(w);
                h = make32Multi(h);
            }
            renderer.setDrawpadOutFrameListener(w, h, 1, listener);
        }
    }

    public void setDrawPadOutFrameListener(int width, int height,
                                           onDrawPadOutFrameListener listener) {
        if (renderer != null) {
            renderer.setDrawpadOutFrameListener(width, height, 1, listener);
        }
    }

    /**
     * 设置setOnDrawPadOutFrameListener后, 你可以设置这个方法来让listener是否运行在Drawpad线程中.
     * 如果你要直接使用里面的数据, 则不用设置, 如果你要开启另一个线程, 把listener传递过来的数据送过去,则建议设置为true;
     *
     * @param en
     */
    public void setOutFrameInDrawPad(boolean en) {
        if (renderer != null) {
            renderer.setOutFrameInDrawPad(en);
        }
    }

    /**
     * 已废弃.请用pauseRecord();
     */
    @Deprecated
    public void pauseRecordDrawPad() {
        pauseRecord();
    }

    /**
     * 已废弃,请用resumeRecord();
     */
    @Deprecated
    public void resumeRecordDrawPad() {
        resumeRecord();
    }

    /**
     * 暂停录制, 使用在 : 开始DrawPad后, 需要暂停录制, 来增加一些图层, 然后恢复录制的场合. 此方法使用在DrawPad线程中的
     * 暂停和恢复的作用, 不能用在一个Activity的onPause和onResume中.
     */
    public void pauseRecord() {
        if (renderer != null) {
            renderer.pauseRecordDrawPad();
        } else {
            mPauseRecord = true;
        }
    }

    /**
     * 恢复录制. 此方法使用在DrawPad线程中的 暂停和恢复的作用, 不能用在一个Activity的onPause和onResume中.
     */
    public void resumeRecord() {
        if (renderer != null && renderer.isRunning()) {
            renderer.resumeRecordDrawPad();
        } else {
            mPauseRecord = false;
        }
    }

    public void pauseRefreshDrawPad() {
        pauseRecord();
    }

    public void resumeRefreshDrawPad() {
        resumeRecord();
    }

    /**
     * 内部使用
     */
    public void resetOutFrames() {
        if (renderer != null && renderer.isRunning()) {
            renderer.resetOutFrames();
        }
    }
}
