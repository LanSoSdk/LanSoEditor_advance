package com.lansosdk.videoeditor;

import android.content.Context;
import android.graphics.Bitmap;
import com.lansosdk.LanSongFilter.LanSongFilter;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.CanvasLayer;
import com.lansosdk.box.DataLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.DrawPadAllRunnable;
import com.lansosdk.box.GifLayer;
import com.lansosdk.box.Layer;
import com.lansosdk.box.MVLayer;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.OnLanSongSDKThreadProgressListener;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.YUVLayer;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadErrorListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.box.onDrawPadThreadProgressListener;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 自动执行容器.
 * 可以增加视频, 图片等.
 * 与DrawPadAutoExecute相对比,有增加视频的功能.
 *
 * 注意1: 此类暂时不做音频处理, 如果您有声音处理,则需要另外用AudioPadExecute处理后,再合并;
 * 注意2: 此类暂时没有做视频的帧率自适应算法,当视频帧率和容器帧率不同时,会导致视频画面少许的放慢或加快.
 */
public class DrawPadAllExecute  {

    private AtomicBoolean isPausing;
    boolean startSuccess;
    private DrawPadAllRunnable runnable;
    private String padDstPath;
    //4个回调;
    private OnLanSongSDKProgressListener onLanSongSDKProgressListener;
    private OnLanSongSDKCompletedListener onLanSongSDKCompletedListener;
    private OnLanSongSDKErrorListener onLanSongSDKErrorListener;
    private OnLanSongSDKThreadProgressListener onLanSongSDKThreadProgressListener;
    private long durationUs;
    private int padWidth;
    private int padHeight;
    private AtomicBoolean isCanceling=new AtomicBoolean(false);

    /**
     * 构造方法
     * @param ctx
     * @param padwidth 容器的宽度, 即最后生成视频的宽度
     * @param padheight 容器的高度,即最后生成视频的高度
     * @param durationUS 容器的长度,  最后生成视频的长度
     * @param frameRate 容器的帧率, 建议25/30这两种.
     */
    public DrawPadAllExecute(Context ctx, int padwidth, int padheight,long  durationUS, int frameRate) {


        LanSongFileUtil.deleteFile(padDstPath);
        padDstPath=LanSongFileUtil.createMp4FileInBox();
        runnable=new DrawPadAllRunnable(ctx,padwidth,padheight,frameRate,0,padDstPath);

        this.durationUs=durationUS;
        this.padWidth=padwidth;
        this.padHeight=padheight;
        isPausing=new AtomicBoolean(false);

        runnable.setDrawPadCompletedListener(new onDrawPadCompletedListener() {
            @Override
            public void onCompleted(DrawPad v) {
                if(!isCancelSatus() && onLanSongSDKCompletedListener!=null){

//                    String dstPath2=dstPath;
//                    if(audioPad!=null && audioPad.getAudioCount()>0){
//                        String audioPath=audioPad.waitComplete();
//                        dstPath2=AudioPadExecute.mergeAudioVideo(dstPath,audioPath,true);
//                        LanSongFileUtil.deleteFile(dstPath);
//                    }
                    onLanSongSDKCompletedListener.onLanSongSDKCompleted(padDstPath);
                }
            }
        });
        runnable.setDrawPadErrorListener(new onDrawPadErrorListener() {
            @Override
            public void onError(DrawPad d, int what) {
//                if(audioPad!=null && audioPad.getAudioCount()>0){
//                    audioPad.stop();
//                    audioPad=null;
//                }
                if(!isCancelSatus() && onLanSongSDKErrorListener!=null){
                    onLanSongSDKErrorListener.onLanSongSDKError(what);
                }
            }
        });
        runnable.setDrawPadProgressListener(new onDrawPadProgressListener() {
            @Override
            public void onProgress(DrawPad v, long currentTimeUs) {
                if(!isCancelSatus() && onLanSongSDKProgressListener!=null){
                    if(currentTimeUs>=durationUs){
                        runnable.stopDrawPad();
                        if(onLanSongSDKCompletedListener!=null){
                            onLanSongSDKCompletedListener.onLanSongSDKCompleted(padDstPath);
                        }
                    }else{
                        int percent=(int)(currentTimeUs *100/durationUs);
                        onLanSongSDKProgressListener.onLanSongSDKProgress(currentTimeUs,percent);
                    }
                }
            }
        });
        runnable.setDrawPadThreadProgressListener(new onDrawPadThreadProgressListener() {
            @Override
            public void onThreadProgress(DrawPad v, long currentTimeUs) {
                if(!isCancelSatus() && onLanSongSDKThreadProgressListener!=null){
                    int percent=(int)(currentTimeUs *100/durationUs);
                    onLanSongSDKThreadProgressListener.onLanSongSDKProgress(currentTimeUs,percent);
                }
            }
        });
    }

    /**
     * 获取容器的宽度
     * @return
     */
    public int getPadWidth(){
        return padWidth;
    }

    /**
     * 获取容器的高度;
     * @return
     */
    public int getPadHeight(){
        return padHeight;
    }
    /**
     * 设置码率
     * [可选,不建议设置]
     * @param bitrate 码率;
     */
    public void setEncodeBitrate(int bitrate) {
        if(runnable!=null){
            runnable.setEncodeBitrate(bitrate);
        }
    }

    /**
     * 开始执行
     * @return
     */
    public boolean start() {
        boolean ret=false;
        if (runnable != null) {
            if (isPausing.get()) {
                runnable.resumeDrawPad();
                ret = true;
            } else {
                ret = runnable.startDrawPad();
            }
        }
        return ret;
    }
    /**
     * 取消执行
     */
    public void cancel() {
        if(runnable!=null){
            isCanceling.set(true);
            runnable.releaseDrawPad();
        }
    }

    /**
     * 释放;
     */
    public void release() {
        if(runnable!=null){
            runnable.release();
        }
    }

    /**
     * 增加图片图层
     * @param bmp 图片
     * @param filter 滤镜
     * @return
     */
    public BitmapLayer addBitmapLayer(Bitmap bmp, LanSongFilter filter) {
        if (runnable != null && setupDrawPad()) {
            return runnable.addBitmapLayer(bmp,filter);
        }else{
            return null;
        }
    }

    /**
     * 增加数据图层
     * @param dataWidth
     * @param dataHeight
     * @return
     */
    public DataLayer addDataLayer(int dataWidth, int dataHeight) {
        if (runnable != null && setupDrawPad()) {
            return runnable.addDataLayer(dataWidth,dataHeight);
        }else{
            return null;
        }
    }

    /**
     * 增加视频图层
     * @param videoPath
     * @param filter
     * @return
     */
    public VideoLayer addVideoLayer(String videoPath, LanSongFilter filter) {
        if (runnable != null && setupDrawPad()) {
            return runnable.addVideoLayer(videoPath,filter);
        }else{
            return null;
        }
    }

    /**
     * 增加MV图层
     * @param colorPath
     * @param maskPath
     * @param isAsync 是否异步执行. 当mv太大时, 视频处理慢, 则让mv异步执行,会加快视频执行;
     * @return
     */
    public MVLayer addMVLayer(String colorPath, String maskPath, boolean isAsync) {
        if (runnable != null && setupDrawPad()) {
            return runnable.addMVLayer(colorPath,maskPath,isAsync);
        }else{
            return null;
        }
    }

    /**
     * 增加mv图层
     * @param srcPath
     * @param maskPath
     * @return
     */
    public MVLayer addMVLayer(String srcPath, String maskPath) {
        if (runnable != null && setupDrawPad()) {
            return runnable.addMVLayer(srcPath,maskPath);
        }else{
            return null;
        }
    }

    /**
     * 增加gif图层
     * @param gifPath
     * @return
     */
    public GifLayer addGifLayer(String gifPath) {
        if (runnable != null && setupDrawPad()) {
            return runnable.addGifLayer(gifPath);
        }else{
            return null;
        }
    }

    /**
     * 增加Gif图层
     * @param resId
     * @return
     */
    public GifLayer addGifLayer(int resId) {
        if (runnable != null && setupDrawPad()) {
            return runnable.addGifLayer(resId);
        }else{
            return null;
        }
    }

    /**
     * 增加canvas图层
     * @return
     */
    public CanvasLayer addCanvasLayer() {
        if (runnable != null && setupDrawPad()) {
            return runnable.addCanvasLayer();
        }else{
            return null;
        }
    }

    /**
     * 增加yuv图层
     * @param width
     * @param height
     * @return
     */
    public YUVLayer addYUVLayer(int width, int height) {
        if (runnable != null && setupDrawPad()) {
            return runnable.addYUVLayer(width,height);
        }else{
            return null;
        }
    }

    /**
     * 把图层放到容器的最底部
     * @param layer
     */
    public void bringToBack(Layer layer) {
        if(runnable!=null){
            runnable.bringToBack(layer);
        }
    }

    /**
     * 把图层放到容器的最外层,
     * @param layer
     */
    public void bringToFront(Layer layer) {
        if(runnable!=null){
            runnable.bringToFront(layer);
        }
    }

    /**
     * 设置图层在容器中的第几层;
     * @param layer
     * @param position
     */
    public void changeLayerPosition(Layer layer, int position) {
        if(runnable!=null){
            runnable.changeLayerPosition(layer,position);
        }
    }

    /**
     * 交互两个图层的上下层关系;
     * @param first
     * @param second
     */
    public void swapTwoLayerPosition(Layer first, Layer second) {
        if(runnable!=null){
            runnable.swapTwoLayerPosition(first,second);
        }
    }
    /**
     * 设置进度监听,  ---经过handle机制,
     * 进度返回的两个参数 long类型是:当前时间戳,单位微秒; int类型是:当前处理的百分比;
     * @param listener
     */
    public void setOnLanSongSDKProgressListener(OnLanSongSDKProgressListener listener) {
        onLanSongSDKProgressListener=listener;
    }

    /**
     * 设置进度监听 ----不经过handle机制
     * 进度返回的两个参数 long类型是:当前时间戳,单位微秒; int类型是:当前处理的百分比;
     * @param listener
     */
    public void setOnLanSongSDKThreadProgressListener(OnLanSongSDKThreadProgressListener listener) {
        onLanSongSDKThreadProgressListener=listener;
    }

    /**
     * 完成回调
     * @param listener
     */
    public void setOnLanSongSDKCompletedListener(OnLanSongSDKCompletedListener listener) {
        onLanSongSDKCompletedListener=listener;
    }

    /**
     * 错误回调
     * @param listener
     */
    public void setOnLanSongSDKErrorListener(OnLanSongSDKErrorListener listener) {
        onLanSongSDKErrorListener=listener;
    }

    public void removeLayer(Layer layer) {
        if(runnable!=null){
            runnable.removeLayer(layer);
        }
    }

    public boolean isRunning() {
        if(runnable!=null){
            return runnable.isRunning();
        }else{
            return false;
        }
    }

    //---------------------------------------------------------------------------
    private boolean isCancelSatus(){
        if(isCanceling.get()){
            LanSongFileUtil.deleteFile(padDstPath);
            padDstPath=null;
        }
        return isCanceling.get();
    }
    private boolean setupDrawPad(){
        if(runnable!=null && !runnable.isRunning() && !isPausing.get()){
            startSuccess =runnable.startDrawPad(true);
            isPausing.set(true);
        }
        return startSuccess;
    }
}
