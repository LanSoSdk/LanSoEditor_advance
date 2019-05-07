package com.example.advanceDemo.utils;

import android.media.MediaPlayer;
import android.view.Surface;

import com.lansosdk.box.LSLog;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.LanSongFileUtil;
import com.lansosdk.box.onDrawPadSizeChangedListener;

import java.io.IOException;

/**
 * 把公共的代码放到整理来;
 *
 */
@Deprecated
public class DrawPadViewWapper {
    public final  static  String TAG= LSLog.TAG;
    DrawPadView drawPadView;
    String videopath;
    public MediaPlayer mediaPlayer;
    public MediaInfo mediaInfo;
    public String editTmpPath;

    public DrawPadViewWapper(DrawPadView view){
        drawPadView=view;
    }
    private onDrawPadViewSetupedListener setupedListener;

    public void setup(String video, onDrawPadViewSetupedListener listener) {
        videopath = video;
        setupedListener = listener;
        mediaInfo = new MediaInfo(videopath);
        if (mediaInfo.prepare()) {
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(video);

            } catch (IOException e) {
                e.printStackTrace();
            }
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    initDrawPad(mp);
                }
            });
            mediaPlayer.prepareAsync();
        } else if (setupedListener != null) {
            setupedListener.onSetup(false);
            return;
        }
    }


    /**
     * Step1: init Drawpad 初始化DrawPad
     */
    private void initDrawPad(MediaPlayer mp) {

            int width=mediaInfo.getWidth();
            int height=mediaInfo.getHeight();
            editTmpPath=LanSongFileUtil.createMp4FileInBox();

            drawPadView.setRealEncodeEnable(width, height,  (int)(mediaInfo.vBitRate*1.5f),(int) mediaInfo.vFrameRate, editTmpPath);
            drawPadView.setDrawPadSize(width, height,new onDrawPadSizeChangedListener() {
                @Override
                public void onSizeChanged(int viewWidth, int viewHeight) {
                    startDrawPad();
                }
            });
    }

    /**
     * Step2: start DrawPad 开始运行这个容器.
     */
    private void startDrawPad() {

        drawPadView.pauseDrawPad();
        if (drawPadView.startDrawPad()) {
            /**
             *  增加视频图层;
             */
            VideoLayer videoLayer = drawPadView.addVideoLayer(mediaPlayer.getVideoWidth(),mediaPlayer.getVideoHeight(), null);
            if (videoLayer != null) {
                mediaPlayer.setSurface(new Surface(videoLayer.getVideoTexture()));
            }
            isSetuped=true;
            if(setupedListener!=null){
                setupedListener.onSetup(true);
            }
        }
    }
    boolean isSetuped=false;
    public  void  start(){
        if(isSetuped && mediaPlayer!=null){
            drawPadView.resumeDrawPad();
            mediaPlayer.start();
        }
    }
    public  void stop(){
        if(mediaPlayer!=null){
            drawPadView.stopDrawPad();
            mediaPlayer.stop();
            isSetuped=false;
        }
    }
}
