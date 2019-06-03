package com.example.advanceDemo.scene;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

import com.example.advanceDemo.utils.DemoUtil;
import com.example.advanceDemo.utils.LSOProgressDialog;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.LSOVideoBody;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadErrorListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.CopyFileFromAssets;
import com.lansosdk.videoeditor.DrawPadConcatVideo;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.LanSongFileUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 多个视频拼接演示
 */
public class VideoConcatAcvivity extends Activity {
    private MediaPlayer firstPlayer,
            cachePlayer,
            currentPlayer;

    private Surface playerSurface;
    private ArrayList<String> videoList = new ArrayList<>();
    private HashMap<String, MediaPlayer> playersCache = new HashMap<>();
    private int currentVideoIndex = 0;
    private DrawPadView drawPadView;
    private  VideoLayer videoLayer;
    private int padWidth,padHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_concat_layout);
        drawPadView = findViewById(R.id.id_videoconcat_drawpadview);
        getVideoUrls();

        findViewById(R.id.id_videoconcat_export).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                export();
            }
        });
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                initFirstPlayer();
            }
        }, 100);
    }

    private void initFirstPlayer() {
        firstPlayer = new MediaPlayer();
        firstPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        //设置cachePlayer为该player对象
        cachePlayer = firstPlayer;
        initNexttPlayer();
        try {
            firstPlayer.setDataSource(videoList.get(currentVideoIndex));
            firstPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    initDrawPad();
                }
            });
            firstPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    onVideoPlayCompleted(mp);
                }
            });
            firstPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 建立当前容器,
     */
    private void initDrawPad() {
        padWidth=firstPlayer.getVideoWidth();  //这里设置容器的宽高, 也是生成最终视频的宽高;建议参考我们的F2文档;
        padHeight=firstPlayer.getVideoHeight();

        drawPadView.setDrawPadSize(padWidth,padHeight, new onDrawPadSizeChangedListener() {
            @Override
            public void onSizeChanged(int viewWidth, int viewHeight) {
                startDrawPad();
            }
        });
    }
    /**
     * Step2: 开始运行 Drawpad
     */
    private void startDrawPad() {
        drawPadView.pauseDrawPad();
        if (!drawPadView.isRunning() && drawPadView.startDrawPad()) {

            addBitmapLayer();
            videoLayer = drawPadView.addVideoLayer(firstPlayer.getVideoWidth(), firstPlayer.getVideoHeight(), null);
            if (videoLayer != null) {
                playerSurface=new Surface(videoLayer.getVideoTexture());
                firstPlayer.setSurface(playerSurface);
                firstPlayer.start();
            }
            drawPadView.resumeDrawPad();
        }
    }

    /**
     * 增加一个背景图层
     */
    public void addBitmapLayer(){
        BitmapLayer layer = drawPadView.addBitmapLayer(BitmapFactory.decodeResource(getResources(), R.drawable.videobg));
        layer.setScaledValue(layer.getPadWidth(), layer.getPadHeight()); // 填充整个屏幕.
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMediaPlayer();
        LanSongFileUtil.deleteFile(exportPath);
    }
    private void stopMediaPlayer()
    {
        if (firstPlayer != null) {
            if (firstPlayer.isPlaying()) {
                firstPlayer.stop();
            }
            firstPlayer.release();
            firstPlayer=null;
        }
        if (currentPlayer != null) {
            if (currentPlayer.isPlaying()) {
                currentPlayer.stop();
            }
            currentPlayer.release();
            currentPlayer=null;
        }
    }
    /**
     * 新开线程负责初始化负责播放剩余视频分段的player对象,避免UI线程做过多耗时操作
     */
    private void initNexttPlayer() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                for (int i = 1; i < videoList.size(); i++) {
                    MediaPlayer   nextMediaPlayer = new MediaPlayer();
                    nextMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    nextMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            onVideoPlayCompleted(mp);
                        }
                    });
                    try {
                        nextMediaPlayer.setDataSource(videoList.get(i));
                        nextMediaPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    cachePlayer.setNextMediaPlayer(nextMediaPlayer);
                    cachePlayer = nextMediaPlayer;
                    playersCache.put(String.valueOf(i), nextMediaPlayer);
                }
                cachePlayer=null;
            }
        }).start();
    }

    /**
     * 每个播放器播放完毕后的
     * @param mp
     */
    private void onVideoPlayCompleted(MediaPlayer mp) {
        mp.setDisplay(null);
        currentPlayer = playersCache.get(String.valueOf(++currentVideoIndex));
        if (currentPlayer != null) {
            videoLayer.updateVideoSize(currentPlayer.getVideoWidth(),currentPlayer.getVideoHeight());
            currentPlayer.setSurface(playerSurface);
        } else{
            Toast.makeText(VideoConcatAcvivity.this, "视频播放完毕..", Toast.LENGTH_SHORT).show();
        }
    }
    DrawPadConcatVideo concatVideo;
    String exportPath;
    LSOProgressDialog progressDialog=new LSOProgressDialog();

    /**
     * 后台执行;
     */
    private void export()
    {
        stopMediaPlayer();
        progressDialog.show(VideoConcatAcvivity.this);
        ArrayList<LSOVideoBody> bodys=new ArrayList<>();
        for(int i = 0; i< videoList.size(); i++) {
            LSOVideoBody body = new LSOVideoBody(videoList.get(i));
            bodys.add(body);
        }

        exportPath= LanSongFileUtil.createMp4FileInBox();

        if(padWidth>0 && padHeight>0){
            concatVideo=new DrawPadConcatVideo(getApplicationContext(),padWidth,padHeight,exportPath);
            for (LSOVideoBody body: bodys){
                concatVideo.addVideo(body);
            }
        }else{
            concatVideo=new DrawPadConcatVideo(getApplicationContext(),bodys,exportPath);
        }
        concatVideo.addBackGroundBitmapLayer(BitmapFactory.decodeResource(getResources(), R.drawable.videobg));
        concatVideo.setDrawPadCompletedListener(new onDrawPadCompletedListener() {
            @Override
            public void onCompleted(DrawPad v) {
                progressDialog.release();
                DemoUtil.startPlayDstVideo(VideoConcatAcvivity.this,exportPath);
            }
        });
        concatVideo.setDrawPadProgressListener(new onDrawPadProgressListener() {
            @Override
            public void onProgress(DrawPad v, long currentTimeUs) {
                int percent=(int)((currentTimeUs*100)/concatVideo.getTotalDurationUs());
                progressDialog.setProgress(percent);
            }
        });
        concatVideo.setDrawPadErrorListener(new onDrawPadErrorListener() {
            @Override
            public void onError(DrawPad d, int what) {
                progressDialog.release();
            }
        });
        concatVideo.start();
    }
    private void getVideoUrls() {
        videoList.add(CopyFileFromAssets.copyAssets(getApplicationContext(),"dy_xialu2.mp4"));
        videoList.add(CopyFileFromAssets.copyAssets(getApplicationContext(),"ku7s.mp4"));
        videoList.add(CopyFileFromAssets.copyAssets(getApplicationContext(),"d3_5s.mp4"));
    }
}