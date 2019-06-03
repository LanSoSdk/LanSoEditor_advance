package com.example.advanceDemo.camera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.example.advanceDemo.VideoPlayerActivity;
import com.example.advanceDemo.utils.DemoUtil;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.CameraLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.LanSongAlphaPixelFilter;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.ViewLayer;
import com.lansosdk.box.ViewLayerRelativeLayout;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.videoeditor.CopyFileFromAssets;
import com.lansosdk.videoeditor.DrawPadCameraView;
import com.lansosdk.videoeditor.DrawPadCameraView.onViewAvailable;
import com.lansosdk.videoeditor.LanSongFileUtil;
import com.lansosdk.videoeditor.LanSongUtil;

/**
 * 在部分华为手机上出现前置摄像头, 画面倒置的问题, 解决如下:
 * <p>
 * 方案1, 如果当前Activity继承自原生Activity; 则manifest.xml中的代码如下:
 * <activity android:name="com.example.advanceDemo.CameraLayerFullLandscapeActivity"
 * android:screenOrientation="landscape"
 * android:theme="@android:style/Theme.NoTitleBar.Fullscreen" > </activity>
 * <p>
 * 方案2,
 * 如果当前Activity继承自v7包的 AppCompatActivity,则manifest.xml的代码如下:
 * <activity
 * android:name="com.example.advanceDemo.CameraLayerFullLandscapeActivity"
 * android:screenOrientation="landscape"
 * android:theme="@style/Theme.AppCompat.Light.NoActionBar.FullScreen" >
 * </activity>
 * 其中theme需要定义在styles.xml中如下:
 * <style
 * name="Theme.AppCompat.Light.NoActionBar.FullScreen"
 * parent="@style/Theme.AppCompat.Light"> <item name="windowNoTitle">true</item>
 * <item name="windowActionBar">false</item> <item
 * name="android:windowFullscreen">true</item> <item
 * name="android:windowContentOverlay">@null</item>
 * </style>
 */
public class CameraLayerKTVDemoActivity extends Activity implements
        OnClickListener, OnSeekBarChangeListener {
    // public class CameraLayerKTVDemoActivity extends AppCompatActivity
    // implements OnClickListener,OnSeekBarChangeListener{
    private static final long RECORD_CAMERA_TIME = 60 * 1000 * 1000; // 300秒.
    private static final String TAG = "CameraKTVDemo";
    int zoomCnt = 0;
    private DrawPadCameraView drawPadCamera;
    private CameraLayer cameraLayer = null;
    private String dstPath = null;
    private PowerManager.WakeLock mWakeLock;
    private ViewLayer mViewLayer = null;
    private ViewLayerRelativeLayout mLayerRelativeLayout;
    private LanSongAlphaPixelFilter alphaPixelFilter;
    private MediaPlayer mediaplayer = null;
    private VideoLayer videoLayer = null;
    private String srcVideoPath;
    // -------------------------------------------一下是UI界面和控制部分.---------------------------------------------------
    private LinearLayout playVideo;
    private TextView tvTime;
    private onDrawPadProgressListener drawPadProgressListener = new onDrawPadProgressListener() {

        @Override
        public void onProgress(DrawPad v, long currentTimeUs) {
            if (currentTimeUs >= RECORD_CAMERA_TIME) {
                stopDrawPad();
            }
            if (tvTime != null) {
                long left = RECORD_CAMERA_TIME - currentTimeUs;

                float leftF = ((float) left / 1000000);
                float b = (float) (Math.round(leftF * 10)) / 10; // 保留一位小数.

                if (b >= 0)
                    tvTime.setText(String.valueOf(b));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        srcVideoPath = getIntent().getStringExtra("videopath");
        LanSongUtil.hideBottomUIMenu(this);

        setContentView(R.layout.cameralayer_ktv_demo_layout);

        if (!LanSongUtil.checkRecordPermission(getBaseContext())) {
            Toast.makeText(getApplicationContext(), "请打开权限后,重试!!!",
                    Toast.LENGTH_LONG).show();
            finish();
        }

        drawPadCamera = (DrawPadCameraView) findViewById(R.id.id_ktvdemo_drawpadcameraview);
        dstPath = LanSongFileUtil.newMp4PathInBox();

        initView();
        initDrawPad();
        DemoUtil.showDialog(CameraLayerKTVDemoActivity.this,
                "此功能 需要对着绿背景拍摄,类似演员在绿幕前表演,共3个图层, 最底层是场景视频,中间层是摄像机,上层是UI");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
            mWakeLock.acquire();
        }
        playVideo.setVisibility(View.GONE);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startDrawPad();
            }
        }, 200);
    }

    /**
     * Step1: 开始运行 drawPad 容器
     */
    private void initDrawPad() {
        // 因手机屏幕是16:9;全屏模式,建议分辨率设置为960x544;
        int padWidth = 544;
        int padHeight = 960;
        int bitrate = 3000 * 1024;
        drawPadCamera.setRealEncodeEnable(padWidth, padHeight, bitrate, 25, dstPath);
        /**
         * 设置进度回调
         */
        drawPadCamera.setOnDrawPadProgressListener(drawPadProgressListener);
        drawPadCamera.setRecordMic(true);

        alphaPixelFilter = new LanSongAlphaPixelFilter();
        drawPadCamera.setCameraParam(true, alphaPixelFilter, true); // 设置是否前置.

        drawPadCamera.setOnViewAvailable(new onViewAvailable() {

            @Override
            public void viewAvailable(DrawPadCameraView v) {
                startDrawPad();
            }
        });
    }

    /**
     * Step2: 开始运行 Drawpad线程.
     */
    private void startDrawPad() {
        // 如果是屏幕比例大于16:9,则需要重新设置编码参数, 从而画面不变形
        if (LanSongUtil.isFullScreenRatio(drawPadCamera.getViewWidth(), drawPadCamera.getViewHeight())) {
            drawPadCamera.setRealEncodeEnable(1088, 544, 3500 * 1024, (int) 25, dstPath);
        }
        if (drawPadCamera.setupDrawpad()) {
            cameraLayer = drawPadCamera.getCameraLayer();
            addVideoLayer();

            addViewLayer();
            drawPadCamera.startPreview();
            drawPadCamera.startRecord();
        }
    }

    /**
     * Step3: 停止容器, 停止后,为新的视频文件增加上音频部分.
     */
    private void stopDrawPad() {
        if (drawPadCamera != null && drawPadCamera.isRunning()) {
            drawPadCamera.stopDrawPad();
            Log.i(TAG, "onViewAvaiable  drawPad停止工作.");
            toastStop();
            cameraLayer = null;

            if (mediaplayer != null) {
                mediaplayer.stop();
                mediaplayer.release();
                mediaplayer = null;
            }
        }
        playVideo.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (drawPadCamera != null) {
            drawPadCamera.stopDrawPad();
        }
        if (mediaplayer != null) {
            mediaplayer.stop();
            mediaplayer.release();
            mediaplayer = null;
        }
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LanSongFileUtil.deleteFile(dstPath);
        dstPath = null;
    }

    /**
     * 增加一个UI图层: ViewLayer
     */
    private void addViewLayer() {
        mLayerRelativeLayout = (ViewLayerRelativeLayout) findViewById(R.id.id_ktvdemo_viewlaylayout);
        mLayerRelativeLayout.setVisibility(View.VISIBLE);

        if (drawPadCamera != null && drawPadCamera.isRunning()) {
            mViewLayer = drawPadCamera.addViewLayer();
            mLayerRelativeLayout.bindViewLayer(mViewLayer);
            mLayerRelativeLayout.invalidate();// 刷新一下.

            ViewGroup.LayoutParams params = mLayerRelativeLayout.getLayoutParams();

            params.height = mViewLayer.getPadHeight(); // 因为布局时, 宽度一致,这里调整高度,让他们一致.
            mLayerRelativeLayout.setLayoutParams(params);
        }
    }

    /**
     * 增加一个视频图层.
     */
    private void addVideoLayer() {
        String videoBG = CopyFileFromAssets.copyAssets(getApplicationContext(), "bg10s.mp4");
        if (srcVideoPath != null && drawPadCamera != null
                && drawPadCamera.isRunning() && videoBG != null) {

            mediaplayer = new MediaPlayer();
            try {
                mediaplayer.setDataSource(videoBG);

            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaplayer.setOnPreparedListener(new OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {

                    if (drawPadCamera != null && drawPadCamera.isRunning()) {

                        videoLayer = drawPadCamera.addVideoLayer(
                                mediaplayer.getVideoWidth(),
                                mediaplayer.getVideoHeight(), null);

                        videoLayer.setScaledValue(videoLayer.getLayerWidth(), videoLayer.getPadHeight() * 2);

                        mediaplayer.setSurface(new Surface(videoLayer.getVideoTexture()));
                        mediaplayer.start();
                        mediaplayer.setLooping(true);

                        drawPadCamera.changeLayerPosition(videoLayer, 0);
                    }
                }
            });
            mediaplayer.prepareAsync();
        }
    }

    private void initView() {
        tvTime = (TextView) findViewById(R.id.id_ktvdemo_timetv);

        playVideo = (LinearLayout) findViewById(R.id.id_ktvdemo_saveplay);
        playVideo.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (LanSongFileUtil.fileExist(dstPath)) {
                    Intent intent = new Intent(CameraLayerKTVDemoActivity.this,
                            VideoPlayerActivity.class);
                    intent.putExtra("videopath", dstPath);
                    startActivity(intent);
                } else {
                    Toast.makeText(CameraLayerKTVDemoActivity.this, "目标文件不存在",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        playVideo.setVisibility(View.GONE);

        SeekBar skbar = (SeekBar) findViewById(R.id.id_ktvdemo_skbar_fine);
        skbar.setOnSeekBarChangeListener(this);
        skbar.setMax(50); // 细调最大定为0.5

        skbar = (SeekBar) findViewById(R.id.id_ktvdemo_skbar_sketchy);
        skbar.setOnSeekBarChangeListener(this);
        skbar.setMax(20); // 粗调最大定为0.2;

        findViewById(R.id.id_ktvdemo_flashlight).setOnClickListener(this);
        findViewById(R.id.id_ktvdemo_frontcamera).setOnClickListener(this);
        findViewById(R.id.id_ktvdemo_filter).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_ktvdemo_frontcamera:
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
            case R.id.id_ktvdemo_flashlight:
                if (cameraLayer != null) {
                    cameraLayer.changeFlash();
                }
                break;
            case R.id.id_ktvdemo_filter:
                break;
            default:
                break;
        }
    }

    private void toastStop() {
        Toast.makeText(getApplicationContext(), "录制已停止!!", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "录制已停止!!");
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.id_ktvdemo_skbar_fine:
                if (alphaPixelFilter != null) {
                    float fine = (float) progress / 100f;
                    alphaPixelFilter.setFineAdjust(fine);
                }
                break;
            case R.id.id_ktvdemo_skbar_sketchy:
                if (alphaPixelFilter != null) {
                    float fine = (float) progress / 100f;
                    alphaPixelFilter.setSketchAdjust(fine);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
