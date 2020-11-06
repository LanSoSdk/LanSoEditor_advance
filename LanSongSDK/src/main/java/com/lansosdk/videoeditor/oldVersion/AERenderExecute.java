package com.lansosdk.videoeditor.oldVersion;

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
import com.lansosdk.box.LSOLayerPosition;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.OnAERenderCompletedListener;
import com.lansosdk.box.OnAERenderErrorListener;
import com.lansosdk.box.OnAERenderProgressListener;
import com.lansosdk.box.OnDrawPadCancelAsyncListener;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadErrorListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.videoeditor.archApi.LanSongFileUtil;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.VideoEditor;


@Deprecated
public class AERenderExecute {
    public AERenderRunnable aeRenderer;

    public DrawPadAERunnable drawPadRenderer;

    public static  boolean forceUseDrawPad=false;

    private String drawPadOutPath;

    public AERenderExecute(Context ctx) throws  Exception{
        if(!forceUseDrawPad && VideoEditor.isSupportNV21ColorFormat()){
            aeRenderer =new AERenderRunnable(ctx);
            LSOLog.d("AERenderExecute use AERenderRunnable...");
        }else{
            drawPadOutPath= LanSongFileUtil.createMp4FileInBox();
            drawPadRenderer=new DrawPadAERunnable(ctx,drawPadOutPath);
            LSOLog.d("AERenderExecute use DrawPadAERunnable...");
        }
    }


    public boolean isUseAERenderClass()
    {
        return aeRenderer!=null;
    }


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

    public AEMVLayer addMVLayer(String colorPath, String maskPath){
        AEMVLayer aemvLayer=null;
        if(aeRenderer !=null){
            aemvLayer= aeRenderer.addMVLayer(colorPath,maskPath);
        }else if(drawPadRenderer!=null) {
            aemvLayer = drawPadRenderer.addMVLayer(colorPath, maskPath);
        }
        if(aemvLayer==null){
            LSOLog.e("AERenderExecute addMVLayer error color videoPath:"+colorPath+ " mask videoPath:"+maskPath);
        }
        return aemvLayer;
    }

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

    public void setbackgroundColor(float red,float green,float blue){
        if (aeRenderer != null) {
            aeRenderer.setbackgroundColor(red,green,blue);
        }else if(drawPadRenderer!=null){
            drawPadRenderer.setBackGroundColor(red,green,blue);
        } else {
            LSOLog.e("setBackGroundColor error.");
        }
    }

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


    public void setOnAERenderProgressListener(OnAERenderProgressListener listener) {
        if (aeRenderer != null) {
            aeRenderer.setOnAERenderProgressListener(listener);
        }else if(drawPadRenderer!=null){
            onAERenderProgressListener=listener;
        }
    }

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

    public void setEncodeBitrate(int bitrate){
        if(aeRenderer!=null){
            aeRenderer.setEncodeBitrate(bitrate);
        }else if(drawPadRenderer!=null){
            drawPadRenderer.setEncodeBitrate(bitrate);
        }
    }

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
