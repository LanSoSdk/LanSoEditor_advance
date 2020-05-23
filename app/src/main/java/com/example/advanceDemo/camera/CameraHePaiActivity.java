package com.example.advanceDemo.camera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.example.advanceDemo.VideoPlayerActivity;
import com.example.advanceDemo.view.CameraProgressBar;
import com.example.advanceDemo.view.FocusImageView;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.CameraLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.MVLayer;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.onDrawPadErrorListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.videoeditor.AudioEditor;
import com.lansosdk.videoeditor.BeautyManager;
import com.example.advanceDemo.utils.CopyFileFromAssets;
import com.lansosdk.videoeditor.DrawPadCameraView;
import com.lansosdk.videoeditor.DrawPadCameraView.doFousEventListener;
import com.lansosdk.videoeditor.DrawPadCameraView.onViewAvailable;
import com.lansosdk.videoeditor.FilterLibrary;
import com.lansosdk.videoeditor.FilterLibrary.OnLanSongFilterChosenListener;
import com.lansosdk.videoeditor.LanSongFileUtil;
import com.lansosdk.videoeditor.LanSongUtil;
import com.lansosdk.videoeditor.MediaInfo;

import java.io.IOException;

import com.lansosdk.LanSongFilter.LanSongFilter;

public class CameraHePaiActivity extends Activity implements
        OnClickListener {

    private static final int RECORD_CAMERA_MAX = 15 * 1000 * 1000; // 定义录制的时间为30s

    private static final int RECORD_CAMERA_MIN = 2 * 1000 * 1000; // 定义最小2秒

    private static final String TAG = "CameraHePaiActivity";
    // ------------------------------------------一下是UI界面和控制部分.---------------------------------------------------
    private DrawPadCameraView drawPadCamera;
    private CameraLayer cameraLayer = null;
    private String dstPath = null; // 用于录制完成后的目标视频路径.
    private FocusImageView focusView;
    private PowerManager.WakeLock mWakeLock;

    private TextView tvTime;
    private CameraProgressBar mProgressBar = null;
    private BitmapLayer bmpLayer;
    private MVLayer mvLayer;
    private BeautyManager mBeautyMng;
    private float beautyLevel = 0.0f;
    private boolean isFirstRecord = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LanSongUtil.hideBottomUIMenu(this);
        if (!LanSongUtil.checkRecordPermission(getBaseContext())) {
            Toast.makeText(getApplicationContext(), "当前无权限,请打开权限后,重试!!!", Toast.LENGTH_LONG).show();
            finish();
        }

        setContentView(R.layout.camera_full_record_layout);
        drawPadCamera = (DrawPadCameraView) findViewById(R.id.id_fullrecord_padview);

        initView();
        initBeautyView();
        mProgressBar.setMaxProgress(RECORD_CAMERA_MAX / 1000);
        mProgressBar.setOnProgressTouchListener(new CameraProgressBar.OnProgressTouchListener() {
            @Override
            public void onClick(CameraProgressBar progressBar) {

                if (drawPadCamera != null) {
                    if (drawPadCamera.isRecording()) {
                        drawPadCamera.pauseRecord();
                        mediaPlayer.pause();
                    } else {
                        drawPadCamera.startRecord();
                        if (isFirstRecord) {
                            isFirstRecord = false;
                            mediaPlayer.setLooping(false);
                            mediaPlayer.seekTo(0);
                        } else {
                            mediaPlayer.start();
                        }
                    }
                }
            }
        });

        dstPath = LanSongFileUtil.newMp4PathInBox();
        initDrawPad(); // 开始录制.
    }

    @Override
    protected void onResume() {
        LanSongUtil.hideBottomUIMenu(this);
        super.onResume();
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
            mWakeLock.acquire();
        }
        drawPadCamera.setOnViewAvailable(new onViewAvailable() {
            @Override
            public void viewAvailable(DrawPadCameraView v) {
                startDrawPad();
            }
        });
    }

    /**
     * 初始化容器;
     */
    private void initDrawPad() {
        int padWidth = 544;
        int padHeight = 960;
        int bitrate = 3000 * 1024;
        /**
         * 设置录制时的一些监听和参数.
         */
        drawPadCamera.setRealEncodeEnable(padWidth, padHeight, bitrate, (int) 25, dstPath);
        drawPadCamera.setOnDrawPadProgressListener(drawPadProgressListener);

        drawPadCamera.setCameraParam(true, null);
        drawPadCamera.setCameraFocusListener(new doFousEventListener() {

            @Override
            public void onFocus(int x, int y) {
                focusView.startFocus(x, y);
            }
        });
        drawPadCamera.setOnDrawPadErrorListener(new onDrawPadErrorListener() {

            @Override
            public void onError(DrawPad d, int what) {
                Log.e(TAG, "DrawPad容器线程运行出错!!!" + what);
            }
        });
    }

    /**
     * 开始运行容器;
     */
    private void startDrawPad() {
        if (LanSongUtil.isFullScreenRatio(drawPadCamera.getViewWidth(), drawPadCamera.getViewHeight())) {
            drawPadCamera.setRealEncodeEnable(544, 1088, 3500 * 1024, (int) 25, dstPath);
        }
        if (drawPadCamera.setupDrawPad()) // 建立容器
        {
            cameraLayer = drawPadCamera.getCameraLayer();
            if (cameraLayer != null) {
                drawPadCamera.startPreview();

                cameraLayer.setScale(0.5f);
                cameraLayer.setPosition(cameraLayer.getLayerWidth() / 2, cameraLayer.getLayerHeight());

                //增加视频图层, 并放右边;
                addVideoLayer();
            }
        } else {
            Log.i(TAG, "建立drawpad线程失败.");
        }
    }



    private onDrawPadProgressListener drawPadProgressListener = new onDrawPadProgressListener() {

        @Override
        public void onProgress(DrawPad v, long currentTimeUs) {
            if (currentTimeUs >= RECORD_CAMERA_MAX) {
                stopDrawPad();
                playVideo();
            }
            if (tvTime != null) {
                float timeF = ((float) currentTimeUs / 1000000);
                float b = (float) (Math.round(timeF * 10)) / 10; // 保留一位小数.

                if (b >= 0)
                    tvTime.setText(String.valueOf(b));
            }
            if (mProgressBar != null) {
                mProgressBar.setProgress((int) (currentTimeUs / 1000));
            }
        }
    };
//--------------------

    /**
     * 停止容器
     */
    private void stopDrawPad() {
        if (drawPadCamera != null && drawPadCamera.isRunning()) {
            drawPadCamera.stopDrawPad();
            cameraLayer = null;
        }
    }

    /**
     * 选择滤镜效果,
     */
    private void selectFilter() {
        if (drawPadCamera != null && drawPadCamera.isRunning()) {
            FilterLibrary.showDialog(this, new OnLanSongFilterChosenListener() {
                @Override
                public void onLanSongFilterChosenListener(
                        final LanSongFilter filter, String name) {
                    if (cameraLayer != null) {
                        cameraLayer.switchFilterTo(filter);
                    }
                }
            });
        }
    }
    MediaPlayer mediaPlayer;
    VideoLayer videoLayer;
    String videoPath;

    private void addVideoLayer() {
        videoPath = CopyFileFromAssets.copyAssets(getApplicationContext(), "dy_xialu2.mp4");
        if (LanSongFileUtil.fileExist(videoPath) && drawPadCamera != null && drawPadCamera.isRunning()) {
            if(mediaPlayer!=null){
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer=null;
            }

            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(videoPath);
                mediaPlayer.prepare();
                mediaPlayer.setLooping(true);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        if (drawPadCamera.isRecording()) {
                            stopDrawPad();
                            playVideo();
                        }
                    }
                });
                //增加图层
                videoLayer = drawPadCamera.addVideoLayer(mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight(), null);
                mediaPlayer.setSurface(new Surface(videoLayer.getVideoTexture()));
                //缩放移动到容器的右边;
                videoLayer.setScaledValue(cameraLayer.getLayerWidth(), cameraLayer.getLayerHeight());
                videoLayer.setPosition(drawPadCamera.getDrawPadWidth() - cameraLayer.getLayerWidth() / 2, cameraLayer.getLayerHeight());
                mediaPlayer.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (drawPadCamera != null) {
            drawPadCamera.stopDrawPad();
        }
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
        if(mediaPlayer!=null){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer=null;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopDrawPad();
    }

    private void initView() {
        findViewById(R.id.id_fullrecord_cancel).setOnClickListener(this);

        tvTime = (TextView) findViewById(R.id.id_fullscreen_timetv);

        findViewById(R.id.id_fullrecord_ok).setVisibility(View.INVISIBLE);

        focusView = (FocusImageView) findViewById(R.id.id_fullrecord_focusview);

        findViewById(R.id.id_fullrecord_flashlight).setOnClickListener(this);
        findViewById(R.id.id_fullrecord_frontcamera).setOnClickListener(this);
        findViewById(R.id.id_fullrecord_filter).setOnClickListener(this);
        mProgressBar = (CameraProgressBar) findViewById(R.id.id_fullrecord_progress);
    }

    private void initBeautyView() {
        mBeautyMng = new BeautyManager(getApplicationContext());
        findViewById(R.id.id_camerabeauty_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if (beautyLevel == 0.0f) {  //美颜加美颜;
                    mBeautyMng.addBeauty(drawPadCamera
                            .getCameraLayer());
                    beautyLevel += 0.22f;
                } else {
                    beautyLevel += 0.1f;
                    mBeautyMng.setWarmCool(beautyLevel);
                    Log.i(TAG, "调色, 数值是:" + beautyLevel);

                    if (beautyLevel >= 1.0f) {
                        mBeautyMng.deleteBeauty(drawPadCamera.getCameraLayer());
                        beautyLevel = 0.0f;
                    }
                }
            }
        });
        findViewById(R.id.id_camerabeauty_brightadd_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mBeautyMng.increaseBrightness(drawPadCamera.getCameraLayer());
            }
        });
        findViewById(R.id.id_camerabeaty_brightsub_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mBeautyMng.discreaseBrightness(drawPadCamera.getCameraLayer());
            }
        });
    }

    private void playVideo() {
        if (LanSongFileUtil.fileExist(dstPath)) {
            Intent intent = new Intent(this, VideoPlayerActivity.class);

            AudioEditor editor = new AudioEditor();
            MediaInfo.checkFile(dstPath);
            MediaInfo.checkFile(videoPath);

            String str = editor.executeVideoReplaceAudio(dstPath, videoPath);
            intent.putExtra("videopath", str);
            startActivity(intent);
            LanSongFileUtil.deleteFile(dstPath);
        } else {
            Toast.makeText(this, "目标文件不存在", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_fullrecord_cancel:
                this.finish();
                break;
            case R.id.id_fullrecord_ok:
                stopDrawPad();
                playVideo();
                break;
            case R.id.id_fullrecord_frontcamera:
                if (cameraLayer != null) {
                    if (drawPadCamera.isRunning() && CameraLayer.isSupportFrontCamera()) {
                        // 先把DrawPad暂停运行.
                        drawPadCamera.pausePreview();
                        cameraLayer.changeCamera();
                        drawPadCamera.resumePreview(); // 再次开启.
                    }
                }
                break;
            case R.id.id_fullrecord_flashlight:
                if (cameraLayer != null) {
                    cameraLayer.changeFlash();
                }
                break;
            case R.id.id_fullrecord_filter:
                selectFilter();
                break;
            default:
                break;
        }
    }
}