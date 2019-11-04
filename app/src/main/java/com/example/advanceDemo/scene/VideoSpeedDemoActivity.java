package com.example.advanceDemo.scene;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.DrawPadUpdateMode;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoplayer.OnLSOPlayerCompletionListener;
import com.lansosdk.videoplayer.OnLSOPlayerPreparedListener;
import com.lansosdk.videoplayer.OnLSOPlayerSeekCompleteListener;
import com.lansosdk.videoplayer.VPlayer;
import com.lansosdk.videoplayer.VideoPlayer;

import java.io.FileNotFoundException;

public class VideoSpeedDemoActivity extends Activity implements
        OnClickListener, OnSeekBarChangeListener {
    private static final String TAG ="VideoSpeedDemoActivity";
    private final static int GET_VIDEO_PROGRESS = 101;
    private final static int UPDATE_PROGRESS_TIME = 100;
    boolean isDestorying = false; // 是否正在销毁, 因为销毁会停止DrawPad
    private String mVideoPath;
    private DrawPadView drawPadView;
    private VPlayer mplayer = null;
    private VideoLayer videoLayer = null;
    private MediaInfo mInfo;
    private SeekBar skProgress;
    private EventHandler mhandler = new EventHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_speed_demo_layout);
        drawPadView = (DrawPadView) findViewById(R.id.id_speed_playerview);

        findViewById(R.id.id_speed_btn_pause).setOnClickListener(this);
        findViewById(R.id.id_speed_btn_speedslow).setOnClickListener(this);
        findViewById(R.id.id_speed_btn_speedfast).setOnClickListener(this);
        findViewById(R.id.id_speed_btn_speednormal).setOnClickListener(this);

        skProgress = (SeekBar) findViewById(R.id.id_speed_seekbar_play);
        skProgress.setOnSeekBarChangeListener(this);

        SeekBar speed = (SeekBar) findViewById(R.id.id_speed_seekbar_speed);
        speed.setOnSeekBarChangeListener(this);

        mVideoPath = getIntent().getStringExtra("videopath");
        mInfo = new MediaInfo(mVideoPath);
        if (mInfo.prepare()) {
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    startPlayVideo();
                }
            }, 500);
        }
    }

    private void startPlayVideo() {
        mplayer = new VPlayer(getApplicationContext());
        try {
            mplayer.setVideoPath(mVideoPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        mplayer.setOnPreparedListener(new OnLSOPlayerPreparedListener() {

            @Override
            public void onPrepared(VideoPlayer mp) {
                initDrawPad();
            }
        });
        mplayer.setOnSeekCompleteListener(new OnLSOPlayerSeekCompleteListener() {

            @Override
            public void onSeekComplete(VideoPlayer mp) {
                Log.i(TAG, "onseekcompleted---------------");

            }
        });

        mplayer.setOnCompletionListener(new OnLSOPlayerCompletionListener() {

            @Override
            public void onCompletion(VideoPlayer mp) {
                if (drawPadView != null && drawPadView.isRunning()) {
                    drawPadView.stopDrawPad();
                }
            }
        });
        mplayer.prepareAsync();

    }

    private void initDrawPad() {
        MediaInfo info = new MediaInfo(mVideoPath);
        if (info.prepare()) {
            drawPadView.setUpdateMode(DrawPadUpdateMode.ALL_VIDEO_READY, 25);
            drawPadView.setDrawPadSize(480, 480, new onDrawPadSizeChangedListener() {

                @Override
                public void onSizeChanged(int viewWidth, int viewHeight) {
                    if (drawPadView.startDrawPad()) {
                        addVideoLayer(); // 增加图层,开始播放呢.
                    }
                }
            });
        }
    }

    private void addVideoLayer() {
        BitmapLayer layer = drawPadView.addBitmapLayer(BitmapFactory.decodeResource(getResources(), R.drawable.videobg));
        layer.setScaledValue(layer.getPadWidth(), layer.getPadHeight());

        int videoW = mplayer.getVideoWidth();
        int videoH = mplayer.getVideoHeight();

        videoLayer = drawPadView.addMainVideoLayer(videoW, videoH, null);

        if (videoLayer != null) {
            mplayer.setSurface(new Surface(videoLayer.getVideoTexture()));
            mplayer.start();
            mplayer.setLooping(true);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mplayer.setSpeedEnable();
                    mplayer.setSpeed(2.0f);
                }
            },1000);
            getTime();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mplayer != null) {
            mplayer.stop();
            mplayer.release();
            mplayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestorying = true;
        if (drawPadView != null) {
            drawPadView.stopDrawPad();
            drawPadView = null;
        }
        if (mplayer != null) {
            mplayer.stop();
            mplayer.release();
            mplayer = null;
        }
    }

    @Override
    public void onClick(View v) {
        if (mplayer == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.id_speed_btn_pause:
                if (mplayer.isPlaying()) {
                    mplayer.pause();
                } else {
                    mplayer.start();
                    mplayer.setSpeedEnable();
                    mplayer.setSpeed(1.0f);
                }

                break;
            case R.id.id_speed_btn_speednormal:
                mplayer.setSpeedEnable();
                mplayer.setSpeed(1.0f);
                break;
            case R.id.id_speed_btn_speedslow:
                mplayer.setSpeedEnable();
                mplayer.setSpeed(0.5f);
                break;
            case R.id.id_speed_btn_speedfast:
                mplayer.setSpeedEnable();
                mplayer.setSpeed(2.0f);
                break;
            default:
                break;
        }
    }

    private void getTime() {
        mhandler.sendEmptyMessageDelayed(GET_VIDEO_PROGRESS, UPDATE_PROGRESS_TIME);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {

        if (seekBar.getId() == R.id.id_speed_seekbar_play) {
            if (fromUser && mplayer != null) {
                float percent = (float) progress / 100f;
                int time = (int) (percent * mplayer.getDuration());
                mplayer.seekTo(time);
            }
        } else if (seekBar.getId() == R.id.id_speed_seekbar_speed) {
            if (fromUser && mplayer != null) {
                float percent = (float) progress / 100f;
                mplayer.setSpeedEnable();
                mplayer.setSpeed(percent);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    protected class EventHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GET_VIDEO_PROGRESS:

                    if (mplayer != null) {
                        // Log.i(TAG,"get video progress------当前进度是:"+mplayer.setVF());
                        float progress = (float) (mplayer.getCurrentPosition())
                                / (float) mplayer.getDuration();
                        skProgress.setProgress((int) (progress * 100));
                    }
                    if (!isDestorying) {
                        mhandler.sendEmptyMessageDelayed(GET_VIDEO_PROGRESS,
                                UPDATE_PROGRESS_TIME);
                    }
                    break;
            }
        }
    }

    ;
}
