package com.example.advanceDemo.camera;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.advanceDemo.VideoPlayerActivity;
import com.example.advanceDemo.view.FocusImageView;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.CameraLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.ViewLayer;
import com.lansosdk.box.ViewLayerRelativeLayout;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.videoeditor.BeautyManager;
import com.lansosdk.videoeditor.CopyFileFromAssets;
import com.lansosdk.videoeditor.DrawPadCameraView;
import com.lansosdk.videoeditor.DrawPadCameraView.doFousEventListener;
import com.lansosdk.videoeditor.DrawPadCameraView.onViewAvailable;
import com.lansosdk.videoeditor.FilterLibrary;
import com.lansosdk.videoeditor.FilterLibrary.OnLanSongFilterChosenListener;
import com.lansosdk.videoeditor.LanSongUtil;
import com.lansosdk.videoeditor.LanSongFileUtil;

import com.lansosdk.LanSongFilter.LanSongFilter;

/**
 * 在部分华为手机上出现前置摄像头, 画面倒置的问题, 解决如下:
 * <p>
 * 方案1, 如果当前Activity继承自原生Activity; 则manifest.xml中的代码如下: <activity
 * android:name="com.example.advanceDemo.CameraLayerFullLandscapeActivity"
 * android:screenOrientation="landscape"
 * android:theme="@android:style/Theme.NoTitleBar.Fullscreen" > </activity> 方案2,
 * 如果当前Activity继承自v7包的 AppCompatActivity,则manifest.xml的代码如下: <activity
 * android:name="com.example.advanceDemo.CameraLayerFullLandscapeActivity"
 * android:screenOrientation="landscape"
 * android:theme="@style/Theme.AppCompat.Light.NoActionBar.FullScreen" >
 * </activity> 其中theme需要定义在styles.xml中如下: <style
 * name="Theme.AppCompat.Light.NoActionBar.FullScreen"
 * parent="@style/Theme.AppCompat.Light"> <item name="windowNoTitle">true</item>
 * <item name="windowActionBar">false</item> <item
 * name="android:windowFullscreen">true</item> <item
 * name="android:windowContentOverlay">@null</item> </style>
 */
// public class CameraLayerFullLandscapeActivity extends Activity implements
// OnClickListener{
public class CameraLayerFullLandscapeActivity extends AppCompatActivity
        implements OnClickListener {
    private static final long RECORD_CAMERA_TIME = 15 * 1000 * 1000; // 定义录制的时间为20s
    private static final String TAG = "CameraLayerFullLandscapeActivity";

    private DrawPadCameraView mDrawPadCamera;

    private CameraLayer mCameraLayer = null;

    private String dstPath = null;

    private FocusImageView focusView;

    private PowerManager.WakeLock mWakeLock;
    private ViewLayer mViewLayer = null;
    private ViewLayerRelativeLayout mLayerRelativeLayout;
    private BeautyManager mBeautyMng;
    private float beautyLevel = 0.0f;
    /**
     * 增加一个UI图层: ViewLayer
     */
    private TextView tvWord;
    private TextView tvWord2;
    private TextView tvWord3;
    /**
     * 在增加一个UI图层.
     */
    private BitmapLayer bmpLayer;
    // -------------------------------------------一下是UI界面和控制部分.---------------------------------------------------
    private LinearLayout playVideo;
    private TextView tvTime;
    private onDrawPadProgressListener drawPadProgressListener = new onDrawPadProgressListener() {

        @Override
        public void onProgress(DrawPad v, long currentTimeUs) {
            // TODO Auto-generated method stub

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
            if (currentTimeUs > 7000 * 1000) // 在第7秒的时候, 不再显示.
            {
                hideWord();
            } else if (currentTimeUs > 3 * 1000 * 1000) // 在第三秒的时候, 显示tvWord
            {
                showWord();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LanSongUtil.hideBottomUIMenu(this);

        setContentView(R.layout.cameralayer_fullscreen_demo_layout);

        if (!LanSongUtil.checkRecordPermission(getBaseContext())) {
            Toast.makeText(getApplicationContext(), "请打开权限后,重试!!!",
                    Toast.LENGTH_LONG).show();
            finish();
        }

        mDrawPadCamera = (DrawPadCameraView) findViewById(R.id.id_fullscreen_padview);
        dstPath = LanSongFileUtil.newMp4PathInBox();

        initView();
        initBeautyView();
        initDrawPad();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                    TAG);
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
     * Step1: 开始运行 DrawPad 容器
     */
    private void initDrawPad() {
        /**
         * 当前手机屏幕有两种, 全面屏和16:9的屏幕.
         */
        int padWidth = 960;
        int padHeight = 544;
        int bitrate = 3000 * 1024;
        int frameRate = 25;
        mDrawPadCamera.setRealEncodeEnable(padWidth, padHeight, bitrate,
                frameRate, dstPath);
        /**
         * 设置进度回调
         */
        mDrawPadCamera.setOnDrawPadProgressListener(drawPadProgressListener);

        mDrawPadCamera.setRecordMic(true);

        mDrawPadCamera.setCameraParam(true, null, true); // 设置是否前置.
        /**
         * 设置当聚焦时的UI动画.
         */
        mDrawPadCamera.setCameraFocusListener(new doFousEventListener() {

            @Override
            public void onFocus(int x, int y) {
                focusView.startFocus(x, y);
            }
        });
        mDrawPadCamera.setOnViewAvailable(new onViewAvailable() {

            @Override
            public void viewAvailable(DrawPadCameraView v) {
                startDrawPad();
            }
        });
    }

    private void initBeautyView() {
        mBeautyMng = new BeautyManager(getApplicationContext());
        findViewById(R.id.id_camerabeauty_btn).setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (beautyLevel == 0.0f) {  //美颜加美颜;
                            mBeautyMng.addBeauty(mDrawPadCamera.getCameraLayer());
                            beautyLevel += 0.22f;
                        } else {
                            beautyLevel += 0.1f;
                            mBeautyMng.setWarmCool(beautyLevel);
                            Log.i(TAG, "调色, 数值是:" + beautyLevel);

                            if (beautyLevel >= 1.0f) {
                                mBeautyMng.deleteBeauty(mDrawPadCamera.getCameraLayer());
                                beautyLevel = 0.0f;
                            }
                        }
                    }
                });
        findViewById(R.id.id_camerabeauty_brightadd_btn).setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mBeautyMng.increaseBrightness(mDrawPadCamera
                                .getCameraLayer());
                    }
                });
        findViewById(R.id.id_camerabeaty_brightsub_btn).setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mBeautyMng.discreaseBrightness(mDrawPadCamera
                                .getCameraLayer());
                    }
                });
    }

    /**
     * Step2: 开始运行 Drawpad线程.
     */
    private void startDrawPad() {
        // 如果是屏幕比例大于16:9,则需要重新设置编码参数, 从而画面不变形
        if (LanSongUtil.isFullScreenRatio(mDrawPadCamera.getViewWidth(),
                mDrawPadCamera.getViewHeight())) {
            mDrawPadCamera.setRealEncodeEnable(1088, 544, 3500 * 1024,
                    (int) 25, dstPath);
        }
        if (mDrawPadCamera.setupDrawpad()) {
            mCameraLayer = mDrawPadCamera.getCameraLayer();

            // addViewLayer();
            // addBitmapLayer();
            // addMVLayer();
            mDrawPadCamera.startPreview();
            mDrawPadCamera.startRecord();
        }
    }

    /**
     * Step3: 停止容器, 停止后,为新的视频文件增加上音频部分.
     */
    private void stopDrawPad() {
        if (mDrawPadCamera != null && mDrawPadCamera.isRunning()) {
            mDrawPadCamera.stopDrawPad();
            Log.i(TAG, "onViewAvaiable  drawPad停止工作!!!!	");
            toastStop();
            mCameraLayer = null;

            // VHeaderConcat scale=new VHeaderConcat();
            // scale.start(getApplicationContext(), "/sdcard/video_start.mp4",
            // drawpadDstPath);
        }
        playVideo.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        if (mDrawPadCamera != null) {
            mDrawPadCamera.stopDrawPad();
        }
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        if (LanSongFileUtil.fileExist(dstPath)) {
            LanSongFileUtil.deleteFile(dstPath);
            dstPath = null;
        }
    }

    /**
     * 选择滤镜效果,
     */
    private void selectFilter() {
        if (mDrawPadCamera != null && mDrawPadCamera.isRunning()) {
            FilterLibrary.showDialog(this,
                    new OnLanSongFilterChosenListener() {

                        @Override
                        public void onLanSongFilterChosenListener(
                                final LanSongFilter filter, String name) {

                            if (mCameraLayer != null) {
                                mCameraLayer.switchFilterTo(filter);
                            }

                        }
                    });
        }
    }

    private void addViewLayer() {
        mLayerRelativeLayout = (ViewLayerRelativeLayout) findViewById(R.id.id_vview_realtime_gllayout);
        if (mDrawPadCamera != null && mDrawPadCamera.isRunning()) {
            mViewLayer = mDrawPadCamera.addViewLayer();

            // 把这个图层绑定到LayerRelativeLayout中.从而LayerRelativeLayout中的各种UI界面会被绘制到Drawpad上.
            mLayerRelativeLayout.bindViewLayer(mViewLayer);

            mLayerRelativeLayout.invalidate();// 刷新一下.

            ViewGroup.LayoutParams params = mLayerRelativeLayout
                    .getLayoutParams();
            params.height = mViewLayer.getPadHeight(); // 因为布局时, 宽度一致,
            // 这里调整高度,让他们一致.
            mLayerRelativeLayout.setLayoutParams(params);
        }
        tvWord = (TextView) findViewById(R.id.id_vview_tvtest);
        tvWord2 = (TextView) findViewById(R.id.id_vview_tvtest2);
        tvWord3 = (TextView) findViewById(R.id.id_vview_tvtest3);
    }

    private void addBitmapLayer() {
        if (mDrawPadCamera != null && mDrawPadCamera.isRunning()) {
            String bitmapPath = CopyFileFromAssets.copyAssets(
                    getApplicationContext(), "small.png");
            bmpLayer = mDrawPadCamera.addBitmapLayer(BitmapFactory
                    .decodeFile(bitmapPath));

            // 把位置放到中间的右侧, 因为获取的高级是中心点的高度.
            bmpLayer.setPosition(
                    bmpLayer.getPadWidth() - bmpLayer.getLayerWidth() / 2,
                    bmpLayer.getPositionY());
        }
    }

    private void showWord() {
        if (tvWord != null && tvWord.getVisibility() != View.VISIBLE) {
            tvWord.startAnimation(AnimationUtils.loadAnimation(this,
                    R.anim.slide_right_in));
            tvWord.setVisibility(View.VISIBLE);
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    tvWord2.startAnimation(AnimationUtils.loadAnimation(
                            CameraLayerFullLandscapeActivity.this,
                            R.anim.slide_right_in));
                    tvWord2.setVisibility(View.VISIBLE);
                }
            }, 500);

            // 1秒后再显示这个.
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    tvWord3.startAnimation(AnimationUtils.loadAnimation(
                            CameraLayerFullLandscapeActivity.this,
                            R.anim.slide_right_in));
                    tvWord3.setVisibility(View.VISIBLE);
                }
            }, 1000);
        }
    }

    private void hideWord() {
        if (tvWord != null && tvWord.getVisibility() == View.VISIBLE) {
            tvWord.startAnimation(AnimationUtils.loadAnimation(
                    CameraLayerFullLandscapeActivity.this, R.anim.push_up_out));
            tvWord.setVisibility(View.INVISIBLE);
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    tvWord2.startAnimation(AnimationUtils.loadAnimation(
                            CameraLayerFullLandscapeActivity.this,
                            R.anim.push_up_out));
                    tvWord2.setVisibility(View.INVISIBLE);
                }
            }, 500);
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    tvWord3.startAnimation(AnimationUtils.loadAnimation(
                            CameraLayerFullLandscapeActivity.this,
                            R.anim.push_up_out));
                    tvWord3.setVisibility(View.INVISIBLE);
                }
            }, 1000);
        }
    }

    private void initView() {
        tvTime = (TextView) findViewById(R.id.id_fullscreen_timetv);

        playVideo = (LinearLayout) findViewById(R.id.id_fullscreen_saveplay);
        playVideo.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (LanSongFileUtil.fileExist(dstPath)) {
                    Intent intent = new Intent(
                            CameraLayerFullLandscapeActivity.this,
                            VideoPlayerActivity.class);
                    intent.putExtra("videopath", dstPath);
                    startActivity(intent);
                } else {
                    Toast.makeText(CameraLayerFullLandscapeActivity.this,
                            "目标文件不存在", Toast.LENGTH_SHORT).show();
                }
            }
        });
        playVideo.setVisibility(View.GONE);

        focusView = (FocusImageView) findViewById(R.id.id_fullscreen_focus_view);

        findViewById(R.id.id_fullscreen_flashlight).setOnClickListener(this);
        findViewById(R.id.id_fullscreen_frontcamera).setOnClickListener(this);
        findViewById(R.id.id_fullscreen_filter).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.id_fullscreen_frontcamera:
                if (mCameraLayer != null) {
                    if (mDrawPadCamera.isRunning()
                            && CameraLayer.isSupportFrontCamera()) {
                        // 先把DrawPad暂停运行.
                        mDrawPadCamera.pausePreview();
                        mCameraLayer.changeCamera();
                        mDrawPadCamera.resumePreview(); // 再次开启.
                    }
                }
                break;
            case R.id.id_fullscreen_flashlight:
                if (mCameraLayer != null) {
                    mCameraLayer.changeFlash();
                }
                break;
            case R.id.id_fullscreen_filter:
                selectFilter();
                break;
            default:
                break;
        }
    }

    private void toastStop() {
        Toast.makeText(getApplicationContext(), "录制已停止!!", Toast.LENGTH_SHORT)
                .show();
        Log.i(TAG, "录制已停止!!");
    }
}
