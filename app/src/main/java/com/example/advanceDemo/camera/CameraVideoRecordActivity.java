package com.example.advanceDemo.camera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.Toast;

import com.example.advanceDemo.VideoPlayerActivity;
import com.example.advanceDemo.view.VideoFocusView;
import com.example.advanceDemo.view.VideoProgressView;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.AudioLine;
import com.lansosdk.box.CameraLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.LSLog;
import com.lansosdk.box.VideoConcat;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.box.onDrawPadRecordCompletedListener;
import com.lansosdk.videoeditor.BeautyManager;
import com.lansosdk.videoeditor.CopyFileFromAssets;
import com.lansosdk.videoeditor.DrawPadCameraView;
import com.lansosdk.videoeditor.DrawPadCameraView.onViewAvailable;
import com.lansosdk.videoeditor.FilterLibrary;
import com.lansosdk.videoeditor.FilterLibrary.OnLanSongFilterChosenListener;
import com.lansosdk.videoeditor.LanSongFileUtil;
import com.lansosdk.videoeditor.LanSongUtil;
import com.lansosdk.videoeditor.VideoEditor;

import java.io.IOException;
import java.util.ArrayList;

import com.lansosdk.LanSongFilter.LanSongFilter;

public class CameraVideoRecordActivity extends Activity implements
        OnClickListener {

    public static final long MAX_RECORD_TIME = 15 * 1000 * 1000; // 设置录制的最大时间.
    // 15秒.

    public static final long MIN_RECORD_TIME = 2 * 1000 * 1000; // 录制的最小时间

    private static final String TAG = LSLog.TAG;
    VideoFocusView focusView;
    private DrawPadCameraView drawPadCamera;
    private CameraLayer cameraLayer = null;
    private String dstPath = null;
    private PowerManager.WakeLock mWakeLock;

    private VideoProgressView progressView;
    private Button cancelBtn;
    private Button okBtn;
    private Button recorderVideoBtn;

    private long currentSegDuration; // 当前正在录制段的时间. 单位US
    private long beforeSegDuration; // 正在录制的这一段前的总时间. 单位US
    private ArrayList<String> segmentArray = new ArrayList<String>();
    /**
     * 录制音乐,和录制MIC的外音不同; 录制音乐,则为了保持音乐的完整性, 先单独编码,
     * 然后等视频拼接好后, 再把音频和拼接后的视频合并在一起,
     * 内部已经做了音视频的同步处理;
     */
    private boolean isRecordMp3 = false; // 是否分段录制的是mp3; //LSTODO_MUST一定要增加UI按钮;
    private AudioLine mAudioLine = null;
    /**
     * 视频每处理一帧,则会执行这里的回调, 返回当前处理后的时间戳,单位微秒.
     */
    private onDrawPadProgressListener drawPadProgressListener = new onDrawPadProgressListener() {

        @Override
        public void onProgress(DrawPad v, long currentTimeUs) {
            currentSegDuration = currentTimeUs;

            long totalTime = beforeSegDuration + currentTimeUs;

            if (totalTime < MIN_RECORD_TIME) {
                okBtn.setVisibility(View.INVISIBLE);
            } else if (totalTime >= MIN_RECORD_TIME
                    && totalTime < MAX_RECORD_TIME) {
                okBtn.setVisibility(View.VISIBLE);
            } else if (totalTime >= MAX_RECORD_TIME) {
                stopDrawPad();
            }

            if (progressView != null) {
                progressView.setProgressTime(currentTimeUs / 1000);
            }
        }
    };
    private BeautyManager mBeautyMng;
    private float beautyLevel = 0.0f;
    private volatile boolean isDeleteState = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LanSongUtil.hideBottomUIMenu(this);

        setContentView(R.layout.cameralayer_fullsegment_layout);

        if (!LanSongUtil.checkRecordPermission(getBaseContext())) {
            Toast.makeText(getApplicationContext(), "请打开权限后,重试!!!",
                    Toast.LENGTH_LONG).show();
            finish();
        }

        drawPadCamera = (DrawPadCameraView) findViewById(R.id.id_fullscreen_padview);
        initView();
        initBeautyView();

        recorderVideoBtn.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        segmentStart();
                        break;
                    case MotionEvent.ACTION_UP:
                        segmentStop();
                        break;
                }
                return true;
            }
        });

        dstPath = LanSongFileUtil.newMp4PathInBox();
        initDrawPad(); // 开始录制.
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,TAG);
            mWakeLock.acquire();
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dstPath = null;
    }

    private void initDrawPad() {
        int padWidth = 544;
        int padHeight = 960;
        int bitrate = 3000 * 1024;

        drawPadCamera.setRealEncodeEnable(padWidth, padHeight, bitrate,(int) 25, LanSongFileUtil.newMp4PathInBox());
        drawPadCamera.setCameraParam(true, null);
        drawPadCamera.setOnDrawPadProgressListener(drawPadProgressListener);
        drawPadCamera.setOnViewAvailable(new onViewAvailable() {

            @Override
            public void viewAvailable(DrawPadCameraView v) {
                startDrawPad();
            }
        });
    }

    /**
     * 开始运行 Drawpad线程.
     */
    private void startDrawPad() {
        if (LanSongUtil.isFullScreenRatio(drawPadCamera.getViewWidth(),drawPadCamera.getViewHeight())) {
            drawPadCamera.setRealEncodeEnable(544, 1088, 3500 * 1024, (int) 25,dstPath);
        }

        if (drawPadCamera.setupDrawpad()) {
            cameraLayer = drawPadCamera.getCameraLayer();
            drawPadCamera.startPreview();


            cameraLayer.setScale(0.5f);
            cameraLayer.setPosition(cameraLayer.getLayerWidth()/2, cameraLayer.getLayerHeight());

            //增加视频图层, 并放右边;
            addVideoLayer();
        }
    }

    /**
     * 结束录制, 预览.
     */
    private void stopDrawPad() {
        if (drawPadCamera != null && drawPadCamera.isRunning()) {
            /**
             * 如果正在录制,则把最后一段增加进来.
             */
            if (drawPadCamera.isRecording()) {
               String  path=drawPadCamera.segmentStop();
                segmentArray.add(path);
            }
            String musicPath = null;
            if (isRecordMp3) {
                musicPath = drawPadCamera.getRecordMusicPath();
            }
            /**
             * 停止 容器.
             */
            drawPadCamera.stopDrawPad();
            cameraLayer = null;
            mAudioLine = null;

            /**
             * 开始拼接
             */
            if (segmentArray.size() > 0) {

                if(segmentArray.size()==1){
                    dstPath=segmentArray.get(0);
                }else{
                    VideoEditor editor = new VideoEditor();
                    // 把多段拼接起来
                    if (musicPath != null) { // 录制的是MP3;
                        String tmpVideo = LanSongFileUtil.createMp4FileInBox();
                        tmpVideo=editor.executeConcatMP4(segmentArray);
                        dstPath=editor.executeVideoMergeAudio(tmpVideo, musicPath);
                        LanSongFileUtil.deleteFile(tmpVideo);
                    } else {
                        dstPath=editor.executeConcatMP4(segmentArray);
                    }
                }
            }
            /**
             * 开始播放.
             */
            if (LanSongFileUtil.fileExist(dstPath)) {
                Intent intent = new Intent(CameraVideoRecordActivity.this,VideoPlayerActivity.class);
                intent.putExtra("videopath", dstPath);
                startActivity(intent);
            } else {
                Toast.makeText(CameraVideoRecordActivity.this, "目标文件不存在",Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 开始录制一段视频.
     */
    private void segmentStart() {
        if (!drawPadCamera.isRecording()) {
            drawPadCamera.segmentStart();
            progressView.setCurrentState(VideoProgressView.State.START);
        }
    }

    /**
     * 停止一段的录制
     */
    private void segmentStop() {
        if (drawPadCamera.isRecording()) {
            String path=drawPadCamera.segmentStop(true);
            segmentArray.add(path);

            progressView.setCurrentState(VideoProgressView.State.PAUSE);
            int timeMS = (int) (currentSegDuration / 1000); // 转换为毫秒.
            progressView.putTimeList(timeMS);
            beforeSegDuration += currentSegDuration;
            cancelBtn.setVisibility(View.VISIBLE);
        }
    }

    MediaPlayer  mediaPlayer;
    VideoLayer videoLayer;
    private void addVideoLayer()
    {
        String video=CopyFileFromAssets.copyAssets(getApplicationContext(),"dy_xialu2.mp4");

        if(LanSongFileUtil.fileExist(video) && drawPadCamera!=null && drawPadCamera.isRunning()){
            mediaPlayer=new MediaPlayer();
            try {
                mediaPlayer.setDataSource(video);
                mediaPlayer.prepare();

                //增加图层
                videoLayer=drawPadCamera.addVideoLayer(mediaPlayer.getVideoWidth(),mediaPlayer.getVideoHeight(),null);
                mediaPlayer.setSurface(new Surface(videoLayer.getVideoTexture()));
                //缩放移动到容器的右边;
                videoLayer.setScaledValue(cameraLayer.getLayerWidth(),cameraLayer.getLayerHeight());
                videoLayer.setPosition(drawPadCamera.getDrawPadWidth() - cameraLayer.getLayerWidth()/2,cameraLayer.getLayerHeight());
                mediaPlayer.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // -------------------------------------------一下是UI界面和控制部分.---------------------------------------------------
    private void initView() {
        findViewById(R.id.id_fullscreen_flashlight).setOnClickListener(this);
        findViewById(R.id.id_fullscreen_frontcamera).setOnClickListener(this);
        findViewById(R.id.id_fullscreen_filter).setOnClickListener(this);

        progressView = (VideoProgressView) findViewById(R.id.id_fullsegment_progress);
        progressView.setMinRecordTime(MIN_RECORD_TIME / 1000f);
        progressView.setMaxRecordTime(MAX_RECORD_TIME / 1000f);

        cancelBtn = (Button) findViewById(R.id.id_fullsegment_cancel);
        okBtn = (Button) findViewById(R.id.id_fullsegment_next);
        recorderVideoBtn = (Button) findViewById(R.id.id_fullsegment_video);

        cancelBtn.setOnClickListener(this);
        okBtn.setOnClickListener(this);
    }

    private void initBeautyView() {
        mBeautyMng = new BeautyManager(getApplicationContext());
        findViewById(R.id.id_camerabeauty_btn).setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        if (beautyLevel == 0.0f) {  //美颜加美颜;
                            mBeautyMng.addBeauty(drawPadCamera.getCameraLayer());
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
        findViewById(R.id.id_camerabeauty_brightadd_btn).setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mBeautyMng.increaseBrightness(drawPadCamera.getCameraLayer());
                    }
                });
        findViewById(R.id.id_camerabeaty_brightsub_btn).setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mBeautyMng.discreaseBrightness(drawPadCamera.getCameraLayer());
                    }
                });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_fullsegment_cancel:

                if (segmentArray.size() == 0) {
                    isDeleteState = false;
                    progressView.setCurrentState(VideoProgressView.State.DELETE);
                    cancelBtn.setBackgroundResource(R.drawable.video_record_backspace);
                    break;
                }
                if (isDeleteState) { // 在按一下, 删除.
                    isDeleteState = false;
                    deleteSegment();

                    progressView.setCurrentState(VideoProgressView.State.DELETE);
                    cancelBtn
                            .setBackgroundResource(R.drawable.video_record_backspace);
                } else {
                    isDeleteState = true;
                    progressView.setCurrentState(VideoProgressView.State.BACKSPACE);
                    cancelBtn.setBackgroundResource(R.drawable.video_record_delete);
                }
                break;
            case R.id.id_fullsegment_next:
                stopDrawPad();
                break;
            case R.id.id_fullscreen_frontcamera:
                if (cameraLayer != null) {
                    if (drawPadCamera.isRunning()
                            && CameraLayer.isSupportFrontCamera()) {
                        // 先把DrawPad暂停运行.
                        drawPadCamera.pausePreview();
                        cameraLayer.changeCamera();
                        drawPadCamera.resumePreview(); // 再次开启.
                    }
                }
                break;
            case R.id.id_fullscreen_flashlight:
                if (cameraLayer != null) {
                    cameraLayer.changeFlash();
                }
                break;
            case R.id.id_fullscreen_filter:
                selectFilter();
                break;
            default:
                break;
        }
    }

    /**
     * 从数组里删除最后一段视频
     */
    private void deleteSegment() {
        if (segmentArray.size() > 0) {

            String filePath = segmentArray.get(segmentArray.size() - 1); // 拿到最后一个.

            LanSongFileUtil.deleteFile(filePath);
            segmentArray.remove(segmentArray.size() - 1);

            beforeSegDuration -= progressView.getLastTime() * 1000;
            if (beforeSegDuration <= 0) {
                beforeSegDuration = 0;
            }
        }
    }

    /**
     * 选择滤镜效果,
     */
    private void selectFilter() {
        if (drawPadCamera != null && drawPadCamera.isRunning()) {
            FilterLibrary.showDialog(this,
                    new OnLanSongFilterChosenListener() {

                        @Override
                        public void onLanSongFilterChosenListener(
                                final LanSongFilter filter, String name) {
                            /**
                             * 通过DrawPad线程去切换 filterLayer的滤镜
                             * 有些Filter是可以调节的,这里为了代码简洁,暂时没有演示,
                             * 可以在CameraeLayerDemoActivity中查看.
                             */
                            if (cameraLayer != null) {
                                cameraLayer.switchFilterTo(filter);
                            }
                        }
                    });
        }
    }
}
