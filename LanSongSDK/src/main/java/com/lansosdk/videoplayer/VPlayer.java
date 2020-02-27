package com.lansosdk.videoplayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.Surface;

import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LSOVideoAsset;
import com.lansosdk.videoeditor.MediaInfo;

import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * SDK提供的视频播放器;
 */
public class VPlayer {

    VPlayerWrapper vPlayer;
    MediaPlayer mediaPlayer;
    MediaInfo mediaInfo;
    public VPlayer(Context context) {
        vPlayer=new VPlayerWrapper(context);
    }

    public void setVideoPath(String path) throws FileNotFoundException {

        mediaInfo =new MediaInfo(path);
        if(mediaInfo.prepare()){
            if( mediaInfo.getWidth() * mediaInfo.getHeight()<=1088*1920){
                vPlayer.setVideoPath(mediaInfo);
                LSOLog.d("VPlayer:: VPlayer2 used to Play Video.");
            }else{
                //大于1080P,则用MediaPlayer播放;
                vPlayer=null;
                LSOLog.d("VPlayer:: MediaPlayer used to Play Video. video size is :"+ mediaInfo.getWidth() +" x " + mediaInfo.getHeight());
                mediaPlayer =new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(path);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new FileNotFoundException(" input path is not found.mediaInfo is:" + mediaInfo.toString());
                }
            }
        }else{
            throw new FileNotFoundException(" input path is not found.mediaInfo is:" + mediaInfo.toString());
        }
    }
    public void setVideoAsset(LSOVideoAsset asset) {

        if(asset!=null){
            if( asset.getWidth() * asset.getHeight()<=1088*1920){
                try {
                    vPlayer.setVideoPath(asset.getVideoPath());
                    mediaPlayer=null;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }else{
                //大于1080P,则用MediaPlayer播放;
                //大于1080P,用videoPlayer播放,会导致内存大量上涨, 不安全. 用系统默认播放器;
                vPlayer=null;
                mediaPlayer =new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(asset.getVideoPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void setSurface(Surface surface) {
        if(vPlayer!=null){
            vPlayer.setSurface(surface);
        }else if(mediaPlayer!=null){
            mediaPlayer.setSurface(surface);
        }
    }

    private  OnLSOPlayerPreparedListener onLSOPlayerPreparedListener=null;
    private OnLSOPlayerCompletionListener onLSOPlayerCompletionListener=null;
    private OnLSOPlayerErrorListener onLSOPlayerErrorListener=null;
    private OnLSOPlayerSeekCompleteListener onLSOPlayerSeekCompleteListener=null;


    public void setOnPreparedListener(OnLSOPlayerPreparedListener l) {
        if(vPlayer!=null){
            vPlayer.setOnPreparedListener(l);
        }else if(mediaPlayer!=null){

            onLSOPlayerPreparedListener=l;

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    if(onLSOPlayerPreparedListener!=null){
                        onLSOPlayerPreparedListener.onPrepared(null);
                    }
                }
            });
        }
    }

    public void setOnCompletionListener(OnLSOPlayerCompletionListener l) {
        if(vPlayer!=null){
            vPlayer.setOnCompletionListener(l);
        }else if(mediaPlayer!=null){
            onLSOPlayerCompletionListener=l;

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if(onLSOPlayerCompletionListener!=null){
                        onLSOPlayerCompletionListener.onCompletion(null);
                    }
                }
            });
        }
    }

    public void setOnErrorListener(OnLSOPlayerErrorListener l) {
        if(vPlayer!=null){
            vPlayer.setOnErrorListener(l);
        }else if(mediaPlayer!=null){
            onLSOPlayerErrorListener=l;
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {

                    if(onLSOPlayerErrorListener!=null){
                        onLSOPlayerErrorListener.onError(null,what,extra);
                        return true;
                    }
                    return false;
                }
            });
        }
    }
    public void setOnSeekCompleteListener(OnLSOPlayerSeekCompleteListener l) {
        if(vPlayer!=null){
            vPlayer.setOnSeekCompleteListener(l);
        }else if(mediaPlayer!=null){
            onLSOPlayerSeekCompleteListener=l;
            mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(MediaPlayer mp) {
                    if(onLSOPlayerSeekCompleteListener!=null){
                        onLSOPlayerSeekCompleteListener.onSeekComplete(null);
                    }
                }
            });
        }
    }

    /**
     * 当播放器的每一帧播放后, 会调用此方法, 返回给你当前播放的精确精度;
     *
     * @param listener 监听,里面有两个方法, VideoPlayer对象和 currentMs当前播放的进度,单位是毫秒;
     */
    public void setOnFrameUpdateListener(OnLSOPlayeFrameUpdateListener listener) {
        if(vPlayer!=null){
            vPlayer.setOnFrameUpateListener(listener);
        }else if(mediaPlayer!=null){
            LSOLog.e(" MediaPlayer  is not set setOnFrameUpdateListener...LSTODO");
        }
    }


    public boolean isPlaying(){
        if(vPlayer!=null){
            return vPlayer.isPlaying();
        }else if(mediaPlayer!=null){
            return mediaPlayer.isPlaying();
        }else{
            return false;
        }
    }
    public boolean isLooping(){
        if(vPlayer!=null){
            return vPlayer.isLooping();
        }else if(mediaPlayer!=null){
            return mediaPlayer.isLooping();
        }else{
            return false;
        }
    }

    public void prepareAsync(){
        if(vPlayer!=null){
            vPlayer.prepareAsync();
        }else if(mediaPlayer!=null){
            mediaPlayer.prepareAsync();
        }
    }

    public int getVideoWidth(){
//        if(vPlayer!=null){
//            return vPlayer.getVideoWidth();
//        }else if(mediaPlayer!=null){
//            return mediaPlayer.getVideoWidth();
//        }else {
//            LSOLog.e("VPlayer getVideoWidth ERROR, vPlayer and mediaPlayer is null");
//            return 320;
//        }
        return mediaInfo.getWidth();
    }

    public int getVideoHeight(){
//        if(vPlayer!=null){
//            return vPlayer.getVideoHeight();
//        }else if(mediaPlayer!=null){
//            return mediaPlayer.getVideoHeight();
//        }else{
//            LSOLog.e("VPlayer getVideoHeight ERROR, vPlayer and mediaPlayer is null");
//            return 320;
//        }
        return mediaInfo.getHeight();
    }

    public  int getDuration(){
        if(vPlayer!=null){
            return vPlayer.getDuration();
        }else if(mediaPlayer!=null){
            return mediaPlayer.getDuration();
        }else{
            LSOLog.e("VPlayer getDuration ERROR, vPlayer and mediaPlayer is null");
            return 1000;
        }
    }
    public void setLooping(boolean is){
        if(vPlayer!=null){
            vPlayer.setLooping(is);
        }else if(mediaPlayer!=null){
            mediaPlayer.setLooping(is);
        }
    }

    public void setVolume(float leftVolume, float rightVolume) {
        if (vPlayer != null){
            vPlayer.setVolume(leftVolume, rightVolume);
        }else if(mediaPlayer!=null){
            mediaPlayer.setVolume(leftVolume,rightVolume);
        }
    }

    /**
     * 设置音量, 范围 0--8.0;  1.0为正常, 0.0是无声, 8.0是放大8倍
     * @param volume
     */
    public void setVolume(float volume) {
        if (vPlayer != null){
            vPlayer.setVolume(volume, volume);
        }else if(mediaPlayer!=null){
            mediaPlayer.setVolume(volume,volume);
        }
    }


    public void setSpeed(float speed){
        if(vPlayer!=null){
            vPlayer.setSpeed(speed);
        }else if(mediaPlayer!=null){
           LSOLog.e(" MediaPlayer  is not set speed...LSTODO");
        }
    }

    /**
     * 获取当前位置,单位MS; 毫秒;
     * 1秒等于1000ms;
     * @return
     */
    public int getCurrentPosition(){
        if(vPlayer!=null){
            return vPlayer.getCurrentPosition();
        }else if(mediaPlayer!=null){
            return mediaPlayer.getCurrentPosition();
        }else{
            LSOLog.e("VPlayer getCurrentPositionMS ERROR, vPlayer and mediaPlayer is null");
            return 1000;
        }
    }
    /**
     * 获取当前位置,单位MS; 毫秒;
     * 1秒等于1000ms;
     * @return
     */
    public int getCurrentFramePosition(){
        return getCurrentPosition();
    }
    public void start(){
        if(vPlayer!=null){
            vPlayer.start();
        }else if(mediaPlayer!=null){
            mediaPlayer.start();
        }
    }
    public void pause(){
        if(vPlayer!=null){
            vPlayer.pause();
        }else if(mediaPlayer!=null){
            mediaPlayer.pause();
        }
    }
    public void seekTo(int  ms){
        if(vPlayer!=null){
            vPlayer.seekTo(ms);
        }else if(mediaPlayer!=null){
            mediaPlayer.seekTo(ms);
        }
    }

    public void stop(){
        if(vPlayer!=null){
            vPlayer.stop();
        }else if(mediaPlayer!=null){
            mediaPlayer.stop();
        }
    }

    public void release(){
        if(vPlayer!=null){
            vPlayer.release();
        }else if(mediaPlayer!=null){
            mediaPlayer.release();
        }
    }
    //----------------------

    @Deprecated
    public void setSpeedEnable() {  //废弃;

    }
    /**
     * 调节变声;
     * 最低:-1.0; (低沉的男声)
     * 最高: 1.0; (尖锐的女声);
     *
     * @param pitch 范围是-1.0 ---1.0;
     */
    public void setAudioPitch(float pitch) {
        if (pitch > 1.0 || pitch < -1.0) {
            return;
        }

        if (vPlayer != null) {
            vPlayer.setAudioPitch(pitch);
        }
    }

    public void setAudioPitchPercent(int percent) {
        if(percent>=0 && percent<=100){
            float percentF=(float)percent/100f;

            float value = 2 * percentF - 1;

            if (vPlayer != null) {
                vPlayer.setAudioPitch(value * 12);
            }
        }
    }
    /**
     * 当设置seek的时候, 是否要精确定位;
     *
     * @param is
     */
    public void setExactlySeekEnable(boolean is) {
        if (vPlayer != null) {
            vPlayer.setExactlySeekEnable(is);
        }
    }








    public void setOnInfoListener(OnLSOPlayerInfoListener l) {
       if(vPlayer!=null){
           vPlayer.setOnInfoListener(l);
       }
    }


















    public boolean canPause() {
        if(vPlayer!=null){
            return vPlayer.canPause();
        }else if(mediaPlayer!=null){
            return true;
        }else{
            return false;
        }

    }

    public boolean canSeekBackward() {
        if(vPlayer!=null){
            return vPlayer.canSeekBackward();
        }else if(mediaPlayer!=null){
            return true;
        }else{
            return false;
        }
    }

    public boolean canSeekForward() {
        if(vPlayer!=null){
            return vPlayer.canSeekForward();
        }else if(mediaPlayer!=null){
            return true;
        }else{
            return false;
        }
    }

    public int getAudioSessionId() {
        return 0;
    }


}

