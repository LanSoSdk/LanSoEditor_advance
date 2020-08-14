package com.lansosdk.videoplayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.view.Surface;

import com.lansosdk.box.LSOLog;
import com.lansosdk.videoeditor.MediaInfo;

import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * 已经废弃,请不要使用
 */
@Deprecated
public class VPlayer {

    MediaPlayer mediaPlayer;
    MediaInfo mediaInfo;
    public VPlayer(Context context) {
    }

    public void setVideoPath(String path) throws FileNotFoundException {

        mediaInfo =new MediaInfo(path);
        if(mediaInfo.prepare()){

                LSOLog.d("VPlayer:: MediaPlayer used to Play Video. video size is :"+ mediaInfo.getWidth() +" x " + mediaInfo.getHeight());
                mediaPlayer =new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(path);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new FileNotFoundException(" input videoPath is not found.mediaInfo is:" + mediaInfo.toString());
                }
            }
    }
    public void setVideoAsset(String path) {

        MediaInfo info=new MediaInfo(path);
        if(info.prepare()){

                mediaPlayer =new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(info.getVideoPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
    }


    public void setSurface(Surface surface) {
        if(mediaPlayer!=null){
            mediaPlayer.setSurface(surface);
        }
    }

    private  OnLSOPlayerPreparedListener onLSOPlayerPreparedListener=null;
    private OnLSOPlayerCompletionListener onLSOPlayerCompletionListener=null;
    private OnLSOPlayerErrorListener onLSOPlayerErrorListener=null;
    private OnLSOPlayerSeekCompleteListener onLSOPlayerSeekCompleteListener=null;


    public void setOnPreparedListener(OnLSOPlayerPreparedListener l) {
            if(mediaPlayer!=null){

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
       if(mediaPlayer!=null){
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
       if(mediaPlayer!=null){
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
        if(mediaPlayer!=null){
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
        if(mediaPlayer!=null){
            LSOLog.e(" MediaPlayer  is not set setOnFrameUpdateListener...LSTODO");
        }
    }


    public boolean isPlaying(){
        if(mediaPlayer!=null){
            return mediaPlayer.isPlaying();
        }else{
            return false;
        }
    }
    public boolean isLooping(){
        if(mediaPlayer!=null){
            return mediaPlayer.isLooping();
        }else{
            return false;
        }
    }

    public void prepareAsync(){
        if(mediaPlayer!=null){
            mediaPlayer.prepareAsync();
        }
    }

    public int getVideoWidth(){
        return mediaInfo.getWidth();
    }

    public int getVideoHeight(){
        return mediaInfo.getHeight();
    }

    public  int getDuration(){
            if(mediaPlayer!=null){
            return mediaPlayer.getDuration();
        }else{
            LSOLog.e("VPlayer getDuration ERROR, vPlayer and mediaPlayer is null");
            return 1000;
        }
    }
    public void setLooping(boolean is){
        if(mediaPlayer!=null){
            mediaPlayer.setLooping(is);
        }
    }

    public void setVolume(float leftVolume, float rightVolume) {
        if(mediaPlayer!=null){
            mediaPlayer.setVolume(leftVolume,rightVolume);
        }
    }

    /**
     * 设置音量, 范围 0--8.0;  1.0为正常, 0.0是无声, 8.0是放大8倍
     * @param volume
     */
    public void setVolume(float volume) {

        if(mediaPlayer!=null){
            mediaPlayer.setVolume(volume,volume);
        }
    }


    public void setSpeed(float speed){
        if(mediaPlayer!=null){
           LSOLog.e(" MediaPlayer  is not set speed...LSTODO");
        }
    }

    /**
     * 获取当前位置,单位MS; 毫秒;
     * 1秒等于1000ms;
     * @return
     */
    public int getCurrentPosition(){
        if(mediaPlayer!=null){
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
        if(mediaPlayer!=null){
            mediaPlayer.start();
        }
    }
    public void pause(){
        if(mediaPlayer!=null){
            mediaPlayer.pause();
        }
    }
    public void seekTo(int  ms){
        if(mediaPlayer!=null){
            mediaPlayer.seekTo(ms);
        }
    }

    public void stop(){
        if(mediaPlayer!=null){
            mediaPlayer.stop();
        }
    }

    public void release(){
        if(mediaPlayer!=null){
            mediaPlayer.release();
            mediaPlayer=null;
        }
    }
    //----------------------

    @Deprecated
    public void setSpeedEnable() {  //废弃;

    }

























    public boolean canPause() {
        if(mediaPlayer!=null){
            return true;
        }else{
            return false;
        }

    }

    public boolean canSeekBackward() {
        if(mediaPlayer!=null){
            return true;
        }else{
            return false;
        }
    }

    public boolean canSeekForward() {
       if(mediaPlayer!=null){
            return true;
        }else{
            return false;
        }
    }

    public int getAudioSessionId() {
        return 0;
    }


}

