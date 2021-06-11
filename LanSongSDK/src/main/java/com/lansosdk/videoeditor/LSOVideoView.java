package com.lansosdk.videoeditor;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.Surface;

import com.lansosdk.box.LSOFrameLayout;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.OnCreateListener;
import com.lansosdk.videoeditor.archApi.LanSongFileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;


public class LSOVideoView extends LSOFrameLayout {

    private MediaPlayer mediaPlayer;
    // ----------------------------------------------
    public LSOVideoView(Context context) {
        super(context);
    }

    public LSOVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LSOVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LSOVideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
    protected void sendOnCreateListener() {
        if (mediaPlayer != null  && getSurfaceTexture() != null) {
            mediaPlayer.setSurface(new Surface(getSurfaceTexture()));
        }
        super.sendOnCreateListener();
    }

    public void sendOnResumeListener(){
        super.sendOnResumeListener();
    }



    //旋转移动缩放
    public boolean onTextureViewTouchEvent(MotionEvent event) {
        super.onTextureViewTouchEvent(event);
        return true;
    }



    private   String inputPath;
    private   String copyVideoPath;
    private int inputWidth,inputHeight;

    public void start(String path, final OnCreateListener onCreateListener) {
        inputPath=path;
        mediaPlayer=new MediaPlayer();

            Thread thread=new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        copyVideoPath=copyFile(inputPath);
                        post(new Runnable() {
                            @Override
                            public void run() {
                                startPlayer(onCreateListener);
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
    }
    private void startPlayer(OnCreateListener onCreateListener){
        try {
            mediaPlayer.setDataSource(copyVideoPath);
            mediaPlayer.prepare(); //同步;
            mediaPlayer.setLooping(true);
            inputWidth =mediaPlayer.getVideoWidth();
            inputHeight =mediaPlayer.getVideoHeight();

            if (inputWidth * inputHeight > 1088 * 1920) {
                LSOLog.e("setPlayerSizeAsync too bigger divide by 2 :"+ inputWidth+ " x "+ inputHeight);
                inputWidth/=2;
                inputHeight/=2;
            }
            setPlayerSizeAsync(inputWidth,inputHeight,onCreateListener);
            mediaPlayer.setVolume(0.0f,0.0f);
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
            mediaPlayer=null;
        }
    }


    public void pause(){
        if (mediaPlayer != null){
            mediaPlayer.pause();
        }
    }

    public long getDurationUs(){
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration()*1000;
        }
        return -1;
    }

    public void seekTo(long timeUs){
        if (mediaPlayer != null) {
            mediaPlayer.seekTo((int) (timeUs/1000));
        }
    }

    private static Object copyLock=new Object();

    private static String  copyFile(String path)throws IOException {

        synchronized (copyLock){
            File source=new File(path);
            String path2= LanSongFileUtil.createMp4FileInBox();
            File dstPath=new File(path2);

            FileChannel inputChannel = null;
            FileChannel outputChannel = null;
            try {
                inputChannel = new FileInputStream(source).getChannel();
                outputChannel = new FileOutputStream(dstPath).getChannel();
                outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
            } finally {
                inputChannel.close();
                outputChannel.close();
            }
            return path2;
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        release();
    }

    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        LanSongFileUtil.deleteFile(copyVideoPath);
    }
}