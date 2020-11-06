package com.lansosdk.videoeditor.oldVersion;

import android.content.Context;
import android.graphics.Bitmap;

import com.lansosdk.LanSongAe.LSOAeDrawable;
import com.lansosdk.box.AEJsonLayer;
import com.lansosdk.box.AudioLayer;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.DrawPadAERunnable;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadErrorListener;
import com.lansosdk.box.onDrawPadProgressListener;


@Deprecated
public class DrawPadAEExecute {

    public DrawPadAERunnable renderer;


    public DrawPadAEExecute(Context ctx,String inputVideo,String output){
            renderer =new DrawPadAERunnable(ctx,inputVideo,output);
    }

    public DrawPadAEExecute(Context ctx,String output){
        if(renderer ==null){
            renderer =new DrawPadAERunnable(ctx,output);
        }
    }

    public void setEncodeBitrate(int bitrate){
        renderer.setEncodeBitrate(bitrate);
    }


    public void setFrateRate(int rate){
        renderer.setFrateRate(rate);
    }


    public AudioLayer getAEAudioLayer(){
        if (renderer != null) {
            return renderer.getMainAudioLayer();
        }else{
            return null;
        }
    }

    public AudioLayer addAudioLayer(String srcPath) {
        if (renderer != null && !renderer.isRunning()) {
            return renderer.addAudioLayer(srcPath);
        } else {
            return null;
        }
    }

    public AudioLayer addAudioLayer(String srcPath, boolean loop) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer audioLayer= renderer.addAudioLayer(srcPath);
            if(audioLayer!=null){
                audioLayer.setLooping(loop);
            }
            return audioLayer;
        } else {
            return null;
        }
    }

    public AudioLayer addAudioLayer(String srcPath, long startFromPadTime) {
        if (renderer != null && !renderer.isRunning()) {
            return renderer.addAudioLayer(srcPath, startFromPadTime, -1);
        } else {
            return null;
        }
    }

    public AudioLayer addAudioLayer(String srcPath, long startFromPadUs,
                                    long durationUs) {
        if (renderer != null && !renderer.isRunning()) {
            return renderer.addAudioLayer(srcPath, startFromPadUs, durationUs);
        } else {
            return null;
        }
    }

    public AudioLayer addAudioLayer(String srcPath, long startFromPadUs,
                                    long startAudioTimeUs, long endAudioTimeUs) {
        if (renderer != null && !renderer.isRunning()) {
            return renderer.addAudioLayer(srcPath, startFromPadUs,
                    startAudioTimeUs, endAudioTimeUs);
        } else {
            return null;
        }
    }


    public void setNotCheckBitRate() {
        if (renderer != null && !renderer.isRunning()) {
            renderer.setNotCheckBitRate();
        }
    }

    public BitmapLayer addBitmapLayer(Bitmap bmp){
        if(renderer !=null && bmp!=null){
            return  renderer.addBitmapLayer(bmp);
        }else {
            return  null;
        }
    }


    public AEJsonLayer addAeLayer(LSOAeDrawable drawable){
        if(renderer !=null) {
            return renderer.addAeLayer(drawable);
        }else{
            return null;
        }
    }

    public AEJsonLayer addAeLayer(LSOAeDrawable drawable, boolean isDisplayLastFrame, long offsetTimeUs){
        if(renderer !=null) {
            AEJsonLayer ret=renderer.addAeLayer(drawable);
            ret.setOnLastFrame(isDisplayLastFrame);
            ret.setOffsetTimeUs(offsetTimeUs);
            return ret;
        }else{
            return null;
        }
    }

    public void addMVLayer(String colorPath, String maskPath){
        if(renderer !=null){
            renderer.addMVLayer(colorPath,maskPath);
        }
    }

    public long getDuration(){
        if(renderer !=null){
            return renderer.getDuration();
        }else {
            LSOLog.e( "get duration error, aeRenderer==null.here return 1000");
            return  1000;
        }
    }

    public void setDrawPadProgressListener(onDrawPadProgressListener listener) {
        if (renderer != null) {
            renderer.setDrawPadProgressListener(listener);
        }
    }

    public void setDrawPadCompletedListener(onDrawPadCompletedListener listener) {
        if (renderer != null) {
            renderer.setDrawPadCompletedListener(listener);
        }
    }
    public void setDrawPadErrorListener(onDrawPadErrorListener listener) {
        if (renderer != null) {
            renderer.setDrawPadErrorListener(listener);
        }
    }

    public  boolean start(){
        if(!renderer.isRunning()){
            return renderer.startDrawPad();
        }else{
            return  false;
        }
    }


    public void stop(){
        if(renderer.isRunning()){
            renderer.stopDrawPad();
        }
    }


    public void cancel(){
        if(renderer.isRunning()){
            renderer.cancelDrawPad();
        }
    }


    public void release(){
        if(renderer.isRunning()){
            renderer.cancelDrawPad();
        }else{
            renderer.releaseDrawPad();
        }
    }
}
