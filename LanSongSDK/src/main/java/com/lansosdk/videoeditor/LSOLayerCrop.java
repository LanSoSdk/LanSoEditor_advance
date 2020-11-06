package com.lansosdk.videoeditor;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.Surface;

import com.lansosdk.box.LSOFrameLayout;
import com.lansosdk.box.LSOLayer;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.OnCreateListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKPlayCompletedListener;
import com.lansosdk.box.OnLanSongSDKPlayProgressListener;
import com.lansosdk.box.OnLanSongSDKTextureUpdateListener;
import com.lansosdk.box.OnResumeListener;

import java.io.IOException;


public class LSOLayerCrop extends LSOFrameLayout {

    private MediaPlayer mediaPlayer;
    // ----------------------------------------------
    public LSOLayerCrop(Context context) {
        super(context);
    }

    public LSOLayerCrop(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LSOLayerCrop(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LSOLayerCrop(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    //-----------copy code
    protected void sendOnCreateListener() {
        setup();
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


    private int inputWidth,inputHeight;
    private LSOLayer videoLayer;
    private long durationUs;
    private final  static  int GET_POSITION_INTERVAL_MS =100;


    private Handler positionHandler=new Handler();
    private Runnable positionRunnable=null;
    private long lastPtsUs=-1;

    private boolean needPauseFirstFrame =true;


    public void onCreateAsync(LSOLayer layer, OnCreateListener listener) {

        videoLayer=layer;
        mediaPlayer=new MediaPlayer();
        setOnLanSongSDKTextureUpdateListener(new OnLanSongSDKTextureUpdateListener() {
            @Override
            public void onTextureUpdate() {
                if(mediaPlayer!=null && needPauseFirstFrame){
                    mediaPlayer.pause();
                    needPauseFirstFrame =false;
                }
            }
        });
        try {
            mediaPlayer.setDataSource(layer.getOriginalPath());
            mediaPlayer.prepare(); //同步;

            durationUs=(long)(mediaPlayer.getDuration()*1000L);

            inputWidth =mediaPlayer.getVideoWidth();
            inputHeight =mediaPlayer.getVideoHeight();

            if (inputWidth * inputHeight > 1088 * 1920) {
                LSOLog.e("setCompositionSize too bigger divide by 2 :"+ inputWidth+ " x "+ inputHeight);
                inputWidth/=2;
                inputHeight/=2;
            }

            if(layer.getCutStartTimeUs()>0 &&  layer.getCutStartTimeUs() < mediaPlayer.getDuration()*1000L){
                int position=(int)(layer.getCutStartTimeUs()/1000);
                mediaPlayer.seekTo(position);
            }


            setCompositionSizeAsync(inputWidth,inputHeight,listener);
            mediaPlayer.setVolume(0.0f,0.0f);
            mediaPlayer.start();
            needPauseFirstFrame =true;

            positionRunnable=new Runnable() {
                @Override
                public void run() {
                    if(mediaPlayer!=null){
                        long ptsUs=mediaPlayer.getCurrentPosition()*1000L;
                        if(ptsUs!= lastPtsUs && playProgressListener!=null && !isPause){
                            int percent=(int)(ptsUs*100/getDurationUs());
                            playProgressListener.onLanSongSDKPlayProgress(ptsUs,percent);
                            lastPtsUs=ptsUs;
                        }
                        if(positionHandler!=null){
                            positionHandler.postDelayed(positionRunnable, GET_POSITION_INTERVAL_MS);
                        }
                    }
                }
            };
            if(positionHandler!=null){
                positionHandler.postDelayed(positionRunnable, GET_POSITION_INTERVAL_MS);
            }
        } catch (IOException e) {
            e.printStackTrace();
            mediaPlayer=null;
            videoLayer=null;
        }
    }

    public void onResumeAsync(OnResumeListener listener) {
        super.onResumeAsync(listener);
    }

    public void onPause(){
        super.onPause();
        pause();
    }

    public void onDestroy(){
        super.onDestroy();
        release();
    }
//------------------------------------player code---------------------------------------------------


    private OnLanSongSDKPlayProgressListener playProgressListener=null;
    /**
     *
     * play  progress listener
     * @param listener
     */
    public void setOnLanSongSDKPlayProgressListener(OnLanSongSDKPlayProgressListener listener) {
        playProgressListener=listener;
    }

    public void setOnLanSongSDKPlayCompletedListener(final OnLanSongSDKPlayCompletedListener listener) {
        if(mediaPlayer!=null){
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    listener.onLanSongSDKPlayCompleted();
                }
            });
        }
    }
    private OnLanSongSDKErrorListener userErrorListener;
    /**
     * error listener.
     */
    public void setOnLanSongSDKErrorListener(OnLanSongSDKErrorListener listener) {
        userErrorListener=listener;
    }
    /**
     * 设置裁剪区域;
     *
     * crop rect in percent;
     * 范围是0--1.0f;
     * 左上角xy坐标是0,0;
     *
     * @param x  裁剪开始X坐标的百分比. 范围是0.0--1.0f;
     * @param y 裁剪开始Y坐标的百分比. 范围是0.0--1.0f;
     * @param width 裁剪宽度 百分比. 范围是0.0--1.0f;
     * @param height 裁剪高度 百分比.  范围是0.0--1.0f;
     */
    public void setCropRectPercent(float x, float y,float width,float height){
        if(videoLayer!=null){
            videoLayer.setCropRectPercent(x,y,width,height);
        }
    }

    /**
     * set to original size
     */
    public void setCropRectToOriginal(){
        if(videoLayer!=null){
            videoLayer.setCropRectToOriginal();
        }
    }
    public void setVolume(float volume){
        if(mediaPlayer!=null){
            mediaPlayer.setVolume(volume,volume);
        }
    }

    /**
     * 开始预览
     */
    public boolean start() {
        super.start();
        if (mediaPlayer != null && videoLayer!=null) {
            mediaPlayer.start();
            isPause=false;
            return true;
        }else{
            LSOLog.d("Layer Crop error. start error media player is null");
            return false;
        }
    }
    private boolean isPause=false;
    /**
     * 定位到某个位置.
     *
     * @param timeUs time时间
     */
    public void seekToTimeUs(long timeUs) {
        pause();
        if (mediaPlayer != null) {
            mediaPlayer.seekTo((int)(timeUs/1000L));
        }
    }
    public boolean isPlaying(){
        return mediaPlayer!=null && mediaPlayer.isPlaying();
    }
    /**
     * 暂停
     */
    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            isPause=true;
        }
    }

    public long getDurationUs() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration()*1000L;
        } else {
            return 1000;
        }
    }
    //内部使用;
    private void setup() {
        if (mediaPlayer != null  && getSurfaceTexture() != null) {
            mediaPlayer.setSurface(new Surface(getSurfaceTexture()));
        }
    }
    /**
     * 释放
     * 如果已经收到完成监听, 则不需要调用;
     */
    private void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}