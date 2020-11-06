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
import com.lansosdk.videoeditor.archApi.LanSongFileUtil;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.VideoEditor;

import java.util.ArrayList;



@Deprecated
public class AECompositionExecute {

    private boolean secondLayerAdd=false;

    public AERenderRunnable aeRenderer;

    public DrawPadAERunnable drawPadRenderer;

    public static  boolean forceUseDrawPad=false;

    private String drawPadOutPath;

    private BitmapLayer drawPadLogoLayer =null;
    private LSOLayerPosition drawPadLogoPosition;

    public AECompositionExecute(Context context) throws Exception{
        if(VideoEditor.isSupportNV21ColorFormat() &&!forceUseDrawPad){
            aeRenderer =new AERenderRunnable(context);
            LSOLog.d("AERenderExecute use AERenderRunnable...");
        }else{
            drawPadOutPath= LanSongFileUtil.createMp4FileInBox();
            drawPadRenderer=new DrawPadAERunnable(context,drawPadOutPath);
            LSOLog.d("AERenderExecute use DrawPadAERunnable...");
        }
    }

    public void setExportBitrate(int bitrate){
        if(aeRenderer!=null){
            aeRenderer.setEncodeBitrate(bitrate);
        }else if(drawPadRenderer!=null){
            drawPadRenderer.setEncodeBitrate(bitrate);
        }
    }

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


    public AEMVLayer addThirdLayer(String colorPath, String maskPath){
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

    public AEMVLayer addFifthLayer(String colorPath, String maskPath){
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
        LSOLog.d("AECompositionExecute released....");
    }
}
