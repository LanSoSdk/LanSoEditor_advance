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
import com.lansosdk.box.LSOAudioAsset;
import com.lansosdk.box.LSOLayerPosition;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.OnAERenderCompletedListener;
import com.lansosdk.box.OnAERenderErrorListener;
import com.lansosdk.box.OnAERenderProgressListener;
import com.lansosdk.box.OnDrawPadCancelAsyncListener;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadErrorListener;
import com.lansosdk.box.onDrawPadProgressListener;

import java.util.ArrayList;


/**
 * Ae的合成类;
 */
public class AECompositionExecute {

    private boolean secondLayerAdd=false;

    public AERenderRunnable aeRenderer;

    public DrawPadAERunnable drawPadRenderer;

    public static  boolean forceUseDrawPad=false;

    private String drawPadOutPath;

    private BitmapLayer drawPadLogoLayer =null;
    private LSOLayerPosition drawPadLogoPosition;

    public AECompositionExecute(Context context){
        if(VideoEditor.isSupportNV21ColorFormat() &&!forceUseDrawPad){
            aeRenderer =new AERenderRunnable(context);
            LSOLog.d("AERenderExecute use AERenderRunnable...");
        }else{
            drawPadOutPath=LanSongFileUtil.createMp4FileInBox();
            drawPadRenderer=new DrawPadAERunnable(context,drawPadOutPath);
            LSOLog.d("AERenderExecute use DrawPadAERunnable...");
        }
    }
    /**
     * 设置在导出时 码率
     * [不建议使用]
     * @param bitrate 码率,最低1*1024*1024;
     */
    public void setExportBitrate(int bitrate){
        if(aeRenderer!=null){
            aeRenderer.setEncodeBitrate(bitrate);
        }else if(drawPadRenderer!=null){
            drawPadRenderer.setEncodeBitrate(bitrate);
        }
    }
    /**
     * 增加第1层
     * 视频图层; 没有则设置为nil
     */
    public AEVideoLayer addFirstLayer(String videoPath){
        AEVideoLayer layer=null;
        if(aeRenderer !=null) {
            layer= aeRenderer.addBgVideoLayer(videoPath);
        }else if(drawPadRenderer!=null){
            layer= drawPadRenderer.addVideoLayer(videoPath);
        }
        if(layer==null){
            LSOLog.e("AERenderExecute addVideoLayer error. input is "+ MediaInfo.checkFile(videoPath,true));
        }
        return layer;
    }

    /**
     * 增加第2层
     * Ae json图层;
     */
    public AEJsonLayer addSecondLayer(LSOAeDrawable drawable){
        if(secondLayerAdd){
            LSOLog.e("已经增加第二层(AE图层). 请确认你的增加顺序.");
            return null;
        }
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
     *
     * 增加第2层
     * Ae json图层;
     *  [重载方法.]
     * @param drawable
     * @param startIndex
     * @param endIndex
     * @return
     * @throws Exception
     */
    public AEJsonLayer addSecondLayer(LSOAeDrawable  drawable, int startIndex, int endIndex) {
        if(secondLayerAdd){
            LSOLog.e("已经增加第二层(AE图层). 请确认你的增加顺序.");
            return null;
        }
        drawable.setCutFrame(startIndex,endIndex);
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
     * 增加第3层
     * mv图层
     * [没有则不调用]
     */
    public AEMVLayer addThirdLayer(String colorPath, String maskPath){
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
     * 增加第4层
     * Ae json图层;
     * [没有则不调用]
     * @return
     */
    public AEJsonLayer addForthLayer(LSOAeDrawable drawable){
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
     * 增加第5层
     * 增加mv图层
     * [没有则不调用]
     * @return
     */
    public AEMVLayer addFifthLayer(String colorPath, String maskPath){
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
     * 增加图片序列,
     * 一般用在动态的透明logo
     * @param bmpList 图片序列
     * @param intervalUs 序列中图片显示间隔,  单位是微秒. 一般建议是40*1000
     * @param loop  是否循环显示, 如果不循环,则停留在最后一帧; 如果您不想循环,也不要最后一帧,则可以设计最后一帧是完全透明的图片;
     * @return 返回图片图层对象
     */
    public BitmapLayer addBitmapLayer(ArrayList<Bitmap> bmpList, long intervalUs,boolean loop) {
        BitmapLayer layer=null;
        if(aeRenderer !=null && bmpList!=null) {
            layer= aeRenderer.addBitmapLayer(bmpList,intervalUs,loop);
        }else if(drawPadRenderer!=null) {
            layer = drawPadRenderer.addBitmapLayer(bmpList,intervalUs,loop);
        }
        if(layer==null){
            LSOLog.e("AERenderExecute addBitmapLayer error bitmap is:"+bmpList);
        }
        return  layer;
    }

    /**
     * 在增加其他音频后,获取Ae模板中的声音.
     * 没有增加其他声音,则无效;
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


    public BitmapLayer addLogoLayer(Bitmap bmp, LSOLayerPosition position){
        BitmapLayer layer=null;
        if(aeRenderer !=null && bmp!=null){
            layer=  aeRenderer.addLogo(bmp,position);
        }else if(drawPadRenderer!=null){
            drawPadLogoLayer = drawPadRenderer.addBitmapLayer(bmp);
            drawPadLogoPosition =position;
            layer= drawPadLogoLayer;
        }

        if(layer==null){
            LSOLog.e("AERenderExecute addLogoLayer error bitmap is:"+bmp);
        }
        return  layer;
    }
    /**
     * 增加声音图层;
     * @param audioAsset 声音文件对象;
     * @return
     */
    public AudioLayer addAudioLayer(LSOAudioAsset audioAsset) {
        if (aeRenderer != null && !aeRenderer.isRunning()) {
            return aeRenderer.addAudioLayer(audioAsset.getAudioPath());
        }else if(drawPadRenderer!=null && !drawPadRenderer.isRunning()){
            return drawPadRenderer.addAudioLayer(audioAsset.getAudioPath());
        } else {
            LSOLog.e("AERenderExecute addAudioLayer error. srcPath  is null or render is running.");
            return null;
        }
    }

    /**
     * 增加音频图层.
     * @param audioAsset 音频资源
     * @param loop 是否循环;
     * @return
     */
    public AudioLayer addAudioLayer(LSOAudioAsset audioAsset, boolean loop) {
        if (aeRenderer != null && !aeRenderer.isRunning()) {
            AudioLayer layer= aeRenderer.addAudioLayer(audioAsset.getAudioPath());
            if(layer!=null){
                layer.setLooping(loop);
            }
            return layer;
        }else if(drawPadRenderer!=null && !drawPadRenderer.isRunning()){
            AudioLayer layer= drawPadRenderer.addAudioLayer(audioAsset.getAudioPath());
            if(layer!=null){
                layer.setLooping(loop);
            }
            return layer;
        } else {
            LSOLog.e("AERenderExecute addAudioLayer error. srcPath  is null or render is running.");
            return null;
        }
    }
    /**
     * 增加音频,
     *
     * 在AE线程开始前 + 所有图层增加后 调用;
     *
     * 音频采样率必须和视频的声音采样率一致
     * @param srcPath
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
     * 增加音频图层,并是否循环
     * 在AE线程开始前 + 所有图层增加后 调用;
     * @return
     */
    public AudioLayer addAudioLayer(String srcPath, boolean loop) {
        if (aeRenderer != null && !aeRenderer.isRunning()) {
            AudioLayer layer= aeRenderer.addAudioLayer(srcPath);
            if(layer!=null){
                layer.setLooping(loop);
            }
            return layer;
        }else if(drawPadRenderer!=null && !drawPadRenderer.isRunning()){
            AudioLayer layer= drawPadRenderer.addAudioLayer(srcPath);
            if(layer!=null){
                layer.setLooping(loop);
            }
            return layer;
        } else {
            LSOLog.e("AERenderExecute addAudioLayer error. srcPath  is null or render is running.");
            return null;
        }
    }

    /**
     * 增加音频图层,;
     *
     * 在AE线程开始前 + 所有图层增加后 调用;
     *
     * 音频采样率必须和视频的声音采样率一致
     * @param srcPath
     * @param startFromPadUs 从Ae模板的什么时间开始增加,
     * @return  返回增加后音频层, 可以用来设置音量,快慢,变声等.
     */
    public AudioLayer addAudioLayer(String srcPath, long startFromPadUs) {
        if (aeRenderer != null && !aeRenderer.isRunning()) {
            return aeRenderer.addAudioLayer(srcPath,startFromPadUs,-1);
        }else if(drawPadRenderer!=null && !drawPadRenderer.isRunning()){
            return drawPadRenderer.addAudioLayer(srcPath,startFromPadUs,-1);
        } else {
            LSOLog.e("AERenderExecute addAudioLayer error. srcPath  is null or render is running.");
            return null;
        }
    }

    /**
     * 增加其他声音;
     *
     *在AE线程开始前 和 所有图层增加后 调用;
     *
     *
     * 音频采样率必须和视频的声音采样率一致
     *
     * @param srcPath        路径, 可以是mp3或m4a或 带有音频的MP4文件;
     * @param startFromPadUs 从主音频的什么时间开始增加
     * @param durationUs     把这段声音多长插入进去.
     * @return 返回一个AudioLayer对象;
     */
    public AudioLayer addAudioLayer(String srcPath, long startFromPadUs, long durationUs) {
        if (aeRenderer != null && !aeRenderer.isRunning()) {
            return aeRenderer.addAudioLayer(srcPath,startFromPadUs,durationUs);
        }else if(drawPadRenderer!=null && !drawPadRenderer.isRunning()){
            return drawPadRenderer.addAudioLayer(srcPath,startFromPadUs,durationUs);
        } else {
            LSOLog.e("AERenderExecute addAudioLayer error. srcPath  is null or render is running.");
            return null;
        }
    }

    /**
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
            return aeRenderer.addAudioLayer(srcPath,startFromPadUs,startAudioTimeUs,endAudioTimeUs);
        }else if(drawPadRenderer!=null && !drawPadRenderer.isRunning()){
            return drawPadRenderer.addAudioLayer(srcPath,startFromPadUs,startAudioTimeUs,endAudioTimeUs);
        } else {
            LSOLog.e("AERenderExecute addAudioLayer error. srcPath  is null or render is running.");
            return null;
        }
    }

//----------------监听 回调;
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

    /**
     * 开始导出Ae模板.
     * 导出后, 会以视频的形式返回给你
     * 你可以不预览,add好各种图层后, 直接调用此方法.
     */
    public boolean startExport(){
            if(aeRenderer!=null&& !aeRenderer.isRunning()) {
                return aeRenderer.start();
            }else if(drawPadRenderer!=null && !drawPadRenderer.isRunning()){


                drawPadRenderer.setDrawPadCompletedListener(new onDrawPadCompletedListener() {
                    @Override
                    public void onCompleted(DrawPad v) {
                        if(onAERenderCompletedListener!=null){
                            LSOLog.d("AEComposition use drawPad completed.");
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
                if(ret && drawPadLogoLayer !=null){
                    drawPadLogoLayer.setPosition(drawPadLogoPosition);
                }
                return ret;
            }else{
                return  false;
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
