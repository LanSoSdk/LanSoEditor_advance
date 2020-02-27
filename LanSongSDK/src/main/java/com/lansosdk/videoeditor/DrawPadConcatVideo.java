package com.lansosdk.videoeditor;

import android.content.Context;
import android.graphics.Bitmap;

import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.DrawPadConcatVideoRunnable;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LSOVideoBody;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadErrorListener;
import com.lansosdk.box.onDrawPadProgressListener;

import java.util.List;

/**
 * 多个视频拼接[后台执行]
 *
 * 1. 内部会对每个视频进行编码,尽管是硬件编码,但速度还是没有不解码快.
 * 2. 使用场景是: 多个不同来源,不同分辨率的视频拼接.
 * 3, 如果是相同分辨率,都是我们SDK生成的视频, 请直接用VideoEditor.executeConcatVideo;
 *
 */
public class DrawPadConcatVideo {
    DrawPadConcatVideoRunnable renderer;

    /**
     *  构造方法
     *  有定义的拼接后的视频宽高, 当要拼接的视频不是这个宽高时,自动缩放并居中显示.
     *  视频会默认以最大的宽或高和padWidth/padHeight对齐,然后调整另一边;
     *
     * @param ctx
     * @param padWidth 拼接后的视频宽高. 注意请一定保证此宽高是16的倍数; 如果不是16的倍数,在低端手机上可能会返回错误回调;
     * @param padHeight
     * @param dstPath 拼接后保存到的目标视频路径
     */
    public DrawPadConcatVideo(Context ctx, int padWidth, int padHeight, String dstPath) {
        if(renderer==null){
            renderer=new DrawPadConcatVideoRunnable(ctx,padWidth,padHeight,dstPath);
        }
    }

    /**
     * 构造方法
     *  直接输入多个视频路径. 默认以第一个视频的宽高为容器宽高, 也是输出视频宽高.
     * @param ctx
     * @param videoArray 多个视频文件
     * @param dstPath  执行后保存到的目标视频路径;
     */
    public DrawPadConcatVideo(Context ctx, List<LSOVideoBody> videoArray, String dstPath){
        if(renderer==null){
            renderer=new DrawPadConcatVideoRunnable(ctx,videoArray,dstPath);
        }
    }

    /**
     * 获取处理多个视频时长之和. 总时长
     * @return
     */
    public long getTotalDurationUs()
    {
        if(renderer!=null){
            return renderer.getTotalDurationUs();
        }else{
            LSOLog.e("concatVideo getTotalDurationUs Error.");
            return 1000L;
        }
    }

    /**
     * 增加视频;
     *  [可选]
     * @param body
     */
    public void addVideo(LSOVideoBody body) {
        if(renderer!=null && !renderer.isRunning()){
            renderer.addVideo(body);
        }
    }

    /**
     * 设置一个logo等.
     * 只能设置一次;
     *  [可选]
     * @param bmp
     * @return
     */
    public BitmapLayer addBitmapLayer(Bitmap bmp) {
        if(renderer!=null&& bmp!=null){
            return renderer.addBitmapLayer(bmp);
        }
        return null;
    }

    /**
     * 增加背景图片
     * [可选]
     * 如果不增加, 则默认是黑色背景,图片会被自动缩放到容器大小;
     * @param bmp 图片对象
     * @return
     */
    public BitmapLayer  addBackGroundBitmapLayer(Bitmap bmp){
        if(renderer!=null&& bmp!=null){
            return renderer.addBackGroundBitmapLayer(bmp);
        }
        return null;
    }

    /**
     * 设置编码时的码率;
     * [可选]
     * @param bitrate
     */
    public void setEncoderBitrate(int bitrate) {
        if(renderer!=null){
            renderer.setEncoderBitrate(bitrate);
        }
    }
    public  void setCheckDrawPadSize(){
        if(renderer!=null){
            renderer.setCheckDrawPadSize(true);
        }
    }
    /**
     * 是否忽略声音
     */
    public void setIngoreAudio() {
        if(renderer!=null){
            renderer.setIngoreAudio();
        }
    }

    /**
     * 进度
     * @param listener
     */
    public void setDrawPadProgressListener(onDrawPadProgressListener listener) {
        if (renderer != null) {
            renderer.setDrawPadProgressListener(listener);
        }
    }

    /**
     * 完成监听
     * @param listener
     */
    public void setDrawPadCompletedListener(onDrawPadCompletedListener listener) {
        if (renderer != null) {
            renderer.setDrawPadCompletedListener(listener);
        }
    }

    /**
     * 错误监听
     * @param listener
     */
    public void setDrawPadErrorListener(onDrawPadErrorListener listener) {
        if (renderer != null) {
            renderer.setDrawPadErrorListener(listener);
        }
    }

    /**
     * 开始
     */
    public boolean start(){
        if(renderer!=null){
            return renderer.startDrawPad();
        }else{
            LSOLog.e("DrawPadConcatvideo render is null. error!");
            return false;
        }
    }

    /**
     * 取消
     */
    public void cancel(){
        if(renderer!=null){
            renderer.cancelDrawPad();
            renderer=null;
        }
    }

    /**
     * 释放
     */
    public void release(){
        if(renderer!=null){
            renderer.release();
            renderer=null;
        }
    }

//---------------测试代码.
//    String dst="/sdcard/shanchu2.mp4";
//
//    public void testFile2() {
//        try {
//            LSOVideoBody body=new LSOVideoBody(SDCARD.file("huaweiP10_4k.mp4"));
//            DrawPadConcatVideo concatVideo=new DrawPadConcatVideo(getApplicationContext(),
//                    Arrays.asList(body),dst);
//
//            concatVideo.setDrawPadCompletedListener(new onDrawPadCompletedListener() {
//                @Override
//                public void onCompleted(DrawPad v) {
//
//                    MediaInfo.checkFileReturnString(dst);
//                }
//            });
//            concatVideo.setDrawPadProgressListener(new onDrawPadProgressListener() {
//                @Override
//                public void onProgress(DrawPad v, long currentTimeUs) {
//                    Log.e("tag", "---currentTimeUs---: "+ currentTimeUs);
//                }
//            });
//            concatVideo.startPreview();
//
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}
