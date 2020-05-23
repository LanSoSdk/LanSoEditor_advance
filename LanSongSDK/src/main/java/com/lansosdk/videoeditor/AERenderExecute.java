package com.lansosdk.videoeditor;

import android.content.Context;
import android.graphics.Bitmap;

import com.lansosdk.LanSongAe.LSOAeDrawable;
import com.lansosdk.box.AEJsonLayer;
import com.lansosdk.box.AEMVLayer;
import com.lansosdk.box.AERenderRunnable;
import com.lansosdk.box.AEVideoLayer;
import com.lansosdk.box.AudioLayer;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.DrawPadAERunnable;
import com.lansosdk.box.ILSOHandleInterface;
import com.lansosdk.box.LSOLayerPosition;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.OnAERenderCompletedListener;
import com.lansosdk.box.OnAERenderErrorListener;
import com.lansosdk.box.OnAERenderProgressListener;
import com.lansosdk.box.OnDrawPadCancelAsyncListener;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadErrorListener;
import com.lansosdk.box.onDrawPadProgressListener;


/**
 * 已废弃, 请用AECompositionView 或 AECompositionExecute;
 */
@Deprecated
public class AERenderExecute {
    public AERenderRunnable aeRenderer;

    public DrawPadAERunnable drawPadRenderer;

    public static  boolean forceUseDrawPad=false;

    private String drawPadOutPath;
    /**
     * 构造方法
     * @param ctx
     */
    public AERenderExecute(Context ctx) throws  Exception{
        if(!LanSoEditor.isLoadLanSongSDK.get()){
            throw  new Exception("没有加载SDK, 或你的APP崩溃后,重新启动当前Activity,请查看完整的logcat:(No SDK is loaded, or the current activity is restarted after your app crashes, please see the full logcat)");
        }

        if(!forceUseDrawPad && VideoEditor.isSupportNV21ColorFormat()){
            aeRenderer =new AERenderRunnable(ctx);
            LSOLog.d("AERenderExecute use AERenderRunnable...");
        }else{
            drawPadOutPath=LanSongFileUtil.createMp4FileInBox();
            drawPadRenderer=new DrawPadAERunnable(ctx,drawPadOutPath);
            LSOLog.d("AERenderExecute use DrawPadAERunnable...");
        }
    }

    /**
     * 特定客户使用.
     * @param handleInterface
     */
    public void setILSOHandleInterface(ILSOHandleInterface handleInterface){
        if(aeRenderer!=null){
            aeRenderer.setILSOHandleInterface(handleInterface);
        }
    }

    /**
     * 是否使用了AErender这个类;
     * @return
     */
    public boolean isUseAERenderClass()
    {
        return aeRenderer!=null;
    }

    /**
     * 增加一个视频图层, 增加成功返回图层对象, 增加失败,返回null
     * @param input 视频的完整路径;
     * @return
     */
    public AEVideoLayer addVideoLayer(String input) {
        AEVideoLayer layer=null;
        if(aeRenderer !=null) {
            layer= aeRenderer.addBgVideoLayer(input);
        }else if(drawPadRenderer!=null){
            layer= drawPadRenderer.addVideoLayer(input);
        }
        if(layer==null){
            LSOLog.e("AERenderExecute addVideoLayer error. input is "+ MediaInfo.checkFile(input,true));
        }
        return layer;
    }
    /**
     * 增加图片图层,
     * 在start前调用
     */
    public BitmapLayer addBitmapLayer(Bitmap bmp){
        BitmapLayer layer=null;
        if(aeRenderer !=null && bmp!=null) {
            layer= aeRenderer.addBitmapLayer(bmp);
        }else if(drawPadRenderer!=null) {
            layer = drawPadRenderer.addBitmapLayer(bmp);
        }
        if(layer==null){
            LSOLog.e("AERenderExecute addBitmapLayer error bitmap is:"+bmp);
        }
        return  layer;
    }
    private BitmapLayer drawpadLogoLayer=null;
    private LSOLayerPosition drawpadLogoPosition;
    public BitmapLayer addLogoLayer(Bitmap bmp, LSOLayerPosition position){
        BitmapLayer layer=null;
        if(aeRenderer !=null && bmp!=null){
            layer=  aeRenderer.addLogo(bmp,position);
        }else if(drawPadRenderer!=null){
            drawpadLogoLayer= drawPadRenderer.addBitmapLayer(bmp);
            drawpadLogoPosition=position;
            layer=drawpadLogoLayer;
        }

        if(layer==null){
            LSOLog.e("AERenderExecute addLogoLayer error bitmap is:"+bmp);
        }
        return  layer;
    }

    /**
     * 增加一层Json
     * @param drawable
     * @return
     */
    public AEJsonLayer addAeLayer(LSOAeDrawable drawable){
        AEJsonLayer layer=null;
        if(aeRenderer !=null) {
            layer= aeRenderer.addAeLayer(drawable);
        }else if(drawPadRenderer!=null) {
            layer = drawPadRenderer.addAeLayer(drawable);
        }
        if(layer==null){
            LSOLog.e("AERenderExecute addAeLayer error input is:"+ drawable);
        }
        return layer;
    }
    /**
     * 增加mv图层
     * 在start前调用
     */
    public AEMVLayer addMVLayer(String colorPath, String maskPath){
        AEMVLayer aemvLayer=null;
        if(aeRenderer !=null){
            aemvLayer= aeRenderer.addMVLayer(colorPath,maskPath);
        }else if(drawPadRenderer!=null) {
            aemvLayer = drawPadRenderer.addMVLayer(colorPath, maskPath);
        }
        if(aemvLayer==null){
            LSOLog.e("AERenderExecute addMVLayer error color path:"+colorPath+ " mask path:"+maskPath);
        }
        return aemvLayer;
    }
    /**
     * 获取Ae模板的声音,在AudioPad中的对象;
     * 在 addAudioLayer后, 调用
     * 如果没有增加其他声音, 则返回null;
     * @return
     */
    public AudioLayer getAEAudioLayer(){
        if (aeRenderer != null) {
            return aeRenderer.getMainAudioLayer();
        }else if(drawPadRenderer!=null){
            return drawPadRenderer.getMainAudioLayer();
        }else{
            LSOLog.e("AERenderExecute getAEAudioLayer error.");
            return null;
        }
    }

    /**
     * 删除AE模板中的声音, 在调用一次AddAudioLayer后调用;
     * @param mute 是否删除;
     */
    public void setAEModuleMute(boolean mute){
        AudioLayer layer=null;
        if(aeRenderer !=null){
            layer=aeRenderer.getMainAudioLayer();
        }else if(drawPadRenderer!=null){
            layer=drawPadRenderer.getMainAudioLayer();
        }
        if(layer!=null){
            layer.setMute(mute);
        }
    }
    /**
     * 增加音频图层,
     *
     * 在AE线程开始前 + 所有图层增加后 调用;
     * @return
     */
    public AudioLayer addAudioLayer(String srcPath) {
        if (aeRenderer != null && !aeRenderer.isRunning()) {
            return aeRenderer.addAudioLayer(srcPath);
        }else if(drawPadRenderer!=null && !drawPadRenderer.isRunning()){
            return drawPadRenderer.addAudioLayer(srcPath);
        } else {
            LSOLog.e("AERenderExecute addAudioLayer error. srcPath  is null or render is running.");
            return null;
        }
    }

    /**
     * 增加音频图层,
     * 并设置是否循环
     *
     * 在AE线程开始前 + 所有图层增加后 调用;
     */
    public AudioLayer addAudioLayer(String srcPath, boolean loop) {
        AudioLayer layer=null;
        if (aeRenderer != null && !aeRenderer.isRunning()) {
            layer= aeRenderer.addAudioLayer(srcPath);
        }else if(drawPadRenderer!=null && !drawPadRenderer.isRunning()){
            layer= drawPadRenderer.addAudioLayer(srcPath);
        } else {
            LSOLog.e("AERenderExecute addAudioLayer error. srcPath  is null or render is running.");
            return null;
        }

        if(layer!=null && loop){
            layer.setLooping(loop);
        }
        return layer;
    }

    /**
     * 增加音频图层,;
     *
     * 在AE线程开始前 + 所有图层增加后 调用;
     *
     * @param srcPath
     * @param startFromPadTime 从Ae模板的什么时间开始增加
     * @return  返回增加后音频层, 可以用来设置音量,快慢,变声等.
     */
    public AudioLayer addAudioLayer(String srcPath, long startFromPadTime) {
        if (aeRenderer != null && !aeRenderer.isRunning()) {
            return aeRenderer.addAudioLayer(srcPath, startFromPadTime, -1);
        }else if(drawPadRenderer!=null && !drawPadRenderer.isRunning()){
            return drawPadRenderer.addAudioLayer(srcPath, startFromPadTime, -1);
        } else {
            LSOLog.e("AERenderExecute addAudioLayer error. srcPath  is null or render is running.");
            return null;
        }
    }

    /**
     * 增加音频图层
     *
     *在AE线程开始前 + 所有图层增加后 调用;
     *
     *
     * @param srcPath        路径, 可以是mp3或m4a或 带有音频的MP4文件;
     * @param startFromPadUs 从主音频的什么时间开始增加
     * @param durationUs     把这段声音多长插入进去.
     * @return 返回一个AudioLayer对象;
     */
    public AudioLayer addAudioLayer(String srcPath, long startFromPadUs,
                                    long durationUs) {
        if (aeRenderer != null && !aeRenderer.isRunning()) {
            return aeRenderer.addAudioLayer(srcPath, startFromPadUs, durationUs);
        }else if(drawPadRenderer!=null && !drawPadRenderer.isRunning()){
            return drawPadRenderer.addAudioLayer(srcPath, startFromPadUs, durationUs);
        } else {
            LSOLog.e("AERenderExecute addAudioLayer error. srcPath  is null or render is running.");
            return null;
        }
    }

    /**
     * 增加一个音频图层.
     *
     * 如果要调节音量, 则增加拿到对象后, 开始调节.
     *
     * 在AE线程开始前 + 所有图层增加后 调用;
     *
     * @param srcPath
     * @param startFromPadUs   从容器的什么位置开始增加
     * @param startAudioTimeUs 裁剪声音的开始时间
     * @param endAudioTimeUs   裁剪声音的结束时间;
     * @return
     */
    public AudioLayer addAudioLayer(String srcPath, long startFromPadUs,
                                    long startAudioTimeUs, long endAudioTimeUs) {
        if (aeRenderer != null && !aeRenderer.isRunning()) {
            return aeRenderer.addAudioLayer(srcPath, startFromPadUs,startAudioTimeUs, endAudioTimeUs);
        }else if(drawPadRenderer!=null && !drawPadRenderer.isRunning()){
            return drawPadRenderer.addAudioLayer(srcPath, startFromPadUs,startAudioTimeUs, endAudioTimeUs);
        } else {
            LSOLog.e("AERenderExecute addAudioLayer error. srcPath  is null or render is running.");
            return null;
        }
    }

    /**
     * 设置合成视频的背景颜色
     * [不建议使用]
     * @param red  红色分量--范围0.0--1.0f
     * @param green 绿色分量---范围0.0--1.0f;
     * @param blue  蓝色分量---范围0.0--1.0f;
     */
    public void setbackgroundColor(float red,float green,float blue){
        if (aeRenderer != null) {
            aeRenderer.setbackgroundColor(red,green,blue);
        }else if(drawPadRenderer!=null){
            drawPadRenderer.setBackGroundColor(red,green,blue);
        } else {
            LSOLog.e("setBackGroundColor error.");
        }
    }

    /**
     * 返回处理的时长
     * 单位us
     * @return
     */
    public long getDuration(){
        if(aeRenderer !=null){
            return aeRenderer.getDurationUS();
        }else if(drawPadRenderer!=null){
            return drawPadRenderer.getDuration();
        }else {
            LSOLog.e("get duration error, aeRenderer==null.here return 1000");
            return  1000;
        }
    }
    public int getWidth()
    {
        if(aeRenderer!=null){
            return aeRenderer.getWidth();
        }else if(drawPadRenderer!=null){
            return drawPadRenderer.getPadWidth();
        }else{
            LSOLog.e("AeRenderExecute getWidth error.");
            return 1;
        }
    }
    public int getHeight(){
        if(aeRenderer!=null){
            return aeRenderer.getHeight();
        }else if(drawPadRenderer!=null){
            return drawPadRenderer.getPadHeight();
        }else{
            LSOLog.e("AeRenderExecute getHeight error.");
            return 1;
        }
    }

    private OnAERenderProgressListener onAERenderProgressListener;
    private OnAERenderCompletedListener onAERenderCompletedListener;
    private OnAERenderErrorListener onAERenderErrorListener;

    /**
     * 进度监听, 回调的两个参数分别是, 时间戳, 百分比;
     */
    public void setOnAERenderProgressListener(OnAERenderProgressListener listener) {
        if (aeRenderer != null) {
            aeRenderer.setOnAERenderProgressListener(listener);
        }else if(drawPadRenderer!=null){
            onAERenderProgressListener=listener;
        }
    }

    /**
     * 完成监听
     */
    public void setOnAERenderCompletedListener(OnAERenderCompletedListener listener) {
        if (aeRenderer != null) {
            aeRenderer.setOnAERenderCompletedListener(listener);
        }else if(drawPadRenderer!=null){
            onAERenderCompletedListener=listener;
        }
    }
    public void setOnAERenderErrorListener(OnAERenderErrorListener listener) {
        if (aeRenderer != null) {
            aeRenderer.setOnAERenderErrorListener(listener);
        }else{
            onAERenderErrorListener=listener;
        }
    }
    /**
     * 设置输出视频的码率, 可以不设置
     * @param bitrate
     */
    public void setEncodeBitrate(int bitrate){
        if(aeRenderer!=null){
            aeRenderer.setEncodeBitrate(bitrate);
        }else if(drawPadRenderer!=null){
            drawPadRenderer.setEncodeBitrate(bitrate);
        }
    }
    /**
     * 开始运行
     * @return
     */
    public  boolean start(){
        if(aeRenderer!=null&& !aeRenderer.isRunning()) {
            return aeRenderer.start();
        }else if(drawPadRenderer!=null && !drawPadRenderer.isRunning()){


            drawPadRenderer.setDrawPadCompletedListener(new onDrawPadCompletedListener() {
                @Override
                public void onCompleted(DrawPad v) {
                    if(onAERenderCompletedListener!=null){
                        LSOLog.d("AERenderExecute use drawPad completed.");
                        onAERenderCompletedListener.onCompleted(drawPadOutPath);
                    }
                }
            });
            drawPadRenderer.setDrawPadProgressListener(new onDrawPadProgressListener() {
                @Override
                public void onProgress(DrawPad v, long currentTimeUs) {
                    if(onAERenderProgressListener!=null){
                        int percent=(int)(currentTimeUs*100/getDuration());
                        onAERenderProgressListener.onProgress(currentTimeUs,percent);
                    }
                }
            });
            drawPadRenderer.setDrawPadErrorListener(new onDrawPadErrorListener() {
                @Override
                public void onError(DrawPad d, int what) {
                    if(onAERenderErrorListener!=null){
                        onAERenderErrorListener.onError("code is:"+what);
                    }
                }
            });
            boolean ret= drawPadRenderer.startDrawPad();
            if(ret && drawpadLogoLayer!=null){
                drawpadLogoLayer.setPosition(drawpadLogoPosition);
            }
            return ret;
        }else{
            return  false;
        }
    }

    /**
     * 停止,
     * 如果已经收到完成监听, 则不需要调用;
     */
    public void stop(){
        if(aeRenderer!=null && aeRenderer.isRunning()){
            aeRenderer.cancel();
        }else if(drawPadRenderer!=null && drawPadRenderer.isRunning()){
            drawPadRenderer.cancelDrawPad();
        }
    }

    /**
     * 取消
     * 如果已经收到完成监听, 则不需要调用;
     */
    public void cancel(){
        if(aeRenderer!=null && aeRenderer.isRunning()){
            aeRenderer.cancel();
        }else if(drawPadRenderer!=null && drawPadRenderer.isRunning()){
            drawPadRenderer.cancelDrawPad();
        }
    }

    public void cancelWithAsync(OnDrawPadCancelAsyncListener listener){
        if(aeRenderer!=null && aeRenderer.isRunning()){
            aeRenderer.cancelWithAsync(listener);
        }else if(drawPadRenderer!=null && drawPadRenderer.isRunning()){
            drawPadRenderer.cancelWithAsync(listener);
        }
    }
    /**
     * 释放
     * 如果已经收到完成监听, 则不需要调用;
     */
    public void release(){
        if(aeRenderer!=null) {
            if (aeRenderer.isRunning()) {
                aeRenderer.cancel();
            } else {
                aeRenderer.release();
            }
        }else if(drawPadRenderer!=null){
            if (drawPadRenderer.isRunning()) {
                drawPadRenderer.cancelDrawPad();
            } else {
                drawPadRenderer.release();
            }
        }
        LSOLog.d("AeRenderExecute released....");
    }
}
