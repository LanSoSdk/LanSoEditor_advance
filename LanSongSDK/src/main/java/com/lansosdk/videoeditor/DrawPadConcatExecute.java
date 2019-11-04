package com.lansosdk.videoeditor;

import android.content.Context;
import android.util.Log;

import com.lansosdk.box.BitmapAnimationLayer;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.CanvasLayer;
import com.lansosdk.box.DrawPadConcatExeRender;
import com.lansosdk.box.DrawPadUpdateMode;
import com.lansosdk.box.GifLayer;
import com.lansosdk.box.LSOAeAnimation;
import com.lansosdk.box.LSOBitmapAsset;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LSOVideoAsset;
import com.lansosdk.box.MVCacheLayer;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.OnLanSongSDKThreadProgressListener;
import com.lansosdk.box.VideoConcatLayer;

/**
 * 自动执行容器.
 */
public class DrawPadConcatExecute {

    private boolean startSuccess;
    private int padWidth;
    private int padHeight;

    private DrawPadConcatExeRender runnable;



    public DrawPadConcatExecute(Context ctx, int padWidth, int padHeight) {



        runnable=new DrawPadConcatExeRender(ctx,padWidth,padHeight);

        this.padWidth=padWidth;
        this.padHeight=padHeight;
    }

    /**
     * 设置帧率
     * [不建议使用]
     * @param rate
     */
    public void setFrameRate(int rate) {
        if(runnable!=null){
            runnable.setFrameRate(rate);
        }
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

    //-----------------------拼接类--------
    /**
     * 拼接一个图片图层,
     * 拼接动画
     * @param asset
     * @param durationUs
     * @return
     */
    public BitmapLayer concatBitmapLayer(LSOBitmapAsset asset, long durationUs) {
        if (runnable != null && asset!=null && setup()) {
            return runnable.concatBitmapLayer(asset,durationUs);
        }else{
            return null;
        }
    }
    /**
     * 拼接一个视频
     * 先增加的第一个位置, 后增加的,依次向后排列;
     * @param asset 视频, 如果你要增加拼接动画,则拿到VideoConcatLayer对象后, 可以在指定点增加LanSongAnimation对象;
     * @return 图层对象;
     */
    public VideoConcatLayer concatVideoLayer(LSOVideoAsset asset) {
        if (runnable != null && asset!=null && setup()) {
            return runnable.concatVideoLayer(asset);
        }else{
            return null;
        }
    }
    //----------------------在拼接层的上面增加别的图层,比如增加logo等-------


    public BitmapLayer addBitmapLayer(LSOBitmapAsset asset) {
        if (runnable != null && asset!=null && setup()) {
            return runnable.addBitmapLayer(asset,0, Long.MAX_VALUE);
        }else{
            return null;
        }
    }


    public BitmapLayer addBitmapLayer(LSOBitmapAsset asset, long startTimeUs, long endTimeUs) {
        if (runnable != null && asset!=null && setup()) {
            return runnable.addBitmapLayer(asset,startTimeUs,endTimeUs);
        }else{
            return null;
        }
    }
    public CanvasLayer addCanvasLayer() {
        if (runnable != null && setup()) {
            return runnable.addCanvasLayer();
        }else{
            return null;
        }
    }
    public GifLayer addGifLayer(String gifPath, long startTimeUs, long endTimeUs) {
        if (runnable != null && setup()) {
            return runnable.addGifLayer(gifPath,startTimeUs,endTimeUs);
        }else{
            return null;
        }
    }
    public MVCacheLayer addMVLayer(String colorPath, String maskPath, long startTimeUs, long endTimeUs, boolean isMute){
        if (runnable != null && setup()) {
            return runnable.addMVLayer(colorPath,maskPath,startTimeUs,endTimeUs,isMute);
        }else{
            return null;
        }
    }
//-------------------------listener 监听设置 -------------------------------------------
    /**
     * 设置进度监听,  ---经过handle机制,
     * 进度返回的两个参数 long类型是:当前时间戳,单位微秒; int类型是:当前处理的百分比;
     */
    public void setOnLanSongSDKProgressListener(OnLanSongSDKProgressListener listener) {
        if(runnable!=null){
            runnable.setOnLanSongSDKProgressListener(listener);
        }
    }
    /**
     * 设置进度监听 ----不经过handle机制
     * 进度返回的两个参数 long类型是:当前时间戳,单位微秒; int类型是:当前处理的百分比;
     */
    public void setOnLanSongSDKThreadProgressListener(OnLanSongSDKThreadProgressListener listener) {
        if(runnable!=null){
            runnable.setOnLanSongSDKThreadProgressListener(listener);
        }
    }

    /**
     * 完成回调
     */
    public void setOnLanSongSDKCompletedListener(OnLanSongSDKCompletedListener listener) {
        if(runnable!=null){
            runnable.setOnLanSongSDKCompletedListener(listener);
        }
    }

    /**
     * 错误回调
     */
    public void setOnLanSongSDKErrorListener(OnLanSongSDKErrorListener listener) {
        if(runnable!=null){
            runnable.setOnLanSongSDKErrorListener(listener);
        }
    }
//-------------------------listener end -------------------------------------------
    public boolean isRunning() {
        if(runnable!=null){
            return runnable.isRunning();
        }else {
            return false;
        }
    }
    /**
     * 开始执行
     * @return
     */
    public boolean startExport() {
        if(runnable!=null){
            runnable.startExport();
            return startSuccess;
        }else{
            return false;
        }
    }
    /**
     * 取消执行
     */
    public void cancel() {
        if(runnable!=null){
            runnable.cancel();
            runnable.release();
            runnable=null;
            startSuccess=false;
        }

    }
    /**
     * 释放;
     */
    public void release() {
        if(runnable!=null){
            runnable.release();
            runnable=null;
            startSuccess=false;
        }
    }
    /**
     * 不检查容器尺寸.
     * 我们默认内部会16字节对齐; 如果调用此方法,则以您设置的宽高为准;
     * [不建议使用]
     */
    public void setNotCheckDrawPadSize() {
        if(runnable!=null){
            runnable.setNotCheckDrawPadSize();
        }
    }

    /**
     * 设置是否检查码率
     */
    public void setNotCheckBitRate() {
        if(runnable!=null){
            runnable.setNotCheckBitRate();
        }
    }
    //---------------------------------------------------------------------------
    private synchronized boolean setup(){
        if(runnable!=null && !runnable.isRunning() && !startSuccess){
            runnable.setup();
            startSuccess=true;
        }
        return startSuccess;
    }
    //---------------------------test Demo测试例子------------------------------------------------
    /**

     */
}
