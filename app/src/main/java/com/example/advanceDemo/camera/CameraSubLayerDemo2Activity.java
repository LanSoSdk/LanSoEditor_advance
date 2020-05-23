package com.example.advanceDemo.camera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.advanceDemo.VideoPlayerActivity;
import com.example.advanceDemo.view.CameraProgressBar;
import com.example.advanceDemo.view.FocusImageView;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.CameraLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.SubLayer;
import com.lansosdk.box.onDrawPadErrorListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.videoeditor.DrawPadCameraView;
import com.lansosdk.videoeditor.DrawPadCameraView.doFousEventListener;
import com.lansosdk.videoeditor.DrawPadCameraView.onViewAvailable;
import com.lansosdk.videoeditor.FilterLibrary;
import com.lansosdk.videoeditor.FilterLibrary.OnLanSongFilterChosenListener;
import com.lansosdk.videoeditor.LanSongUtil;
import com.lansosdk.videoeditor.LanSongFileUtil;

import java.util.ArrayList;

import com.lansosdk.LanSongFilter.LanSongFilter;
import com.lansosdk.LanSongFilter.LanSongBlurFilter;

public class CameraSubLayerDemo2Activity extends Activity implements
        OnClickListener {

    private static final int RECORD_CAMERA_MAX = 15 * 1000 * 1000; // 定义录制的时间为30s

    private static final int RECORD_CAMERA_MIN = 2 * 1000 * 1000; // 定义最小2秒

    private static final String TAG = "CameraSubLayerDemo";

    private DrawPadCameraView mDrawPadCamera;

    private CameraLayer mCamLayer = null;

    private String dstPath = null; // 用于录制完成后的目标视频路径.

    private FocusImageView focusView;

    private PowerManager.WakeLock mWakeLock;
    private TextView tvTime;
    private Context mContext = null;

    private ImageView btnOk;
    private CameraProgressBar mProgressBar = null;

    private TextView tvSubLayerHint;
    private int subLayerAngle; // 角度
    private ArrayList<SubLayer> subLayerArray = new ArrayList<>();
    private onDrawPadProgressListener drawPadProgressListener = new onDrawPadProgressListener() {

        @Override
        public void onProgress(DrawPad v, long currentTimeUs) {
            // TODO Auto-generated method stub

            if (currentTimeUs >= RECORD_CAMERA_MIN && btnOk != null) {
                btnOk.setVisibility(View.VISIBLE);
            }

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LanSongUtil.hideBottomUIMenu(this);
        mContext = getApplicationContext();

        if (!LanSongUtil.checkRecordPermission(getBaseContext())) {
            Toast.makeText(getApplicationContext(), "当前无权限,请打开权限后,重试!!!",
                    Toast.LENGTH_LONG).show();
            finish();
        }

        setContentView(R.layout.camera_sublayerdemo2_layout);
        mDrawPadCamera = (DrawPadCameraView) findViewById(R.id.id_fullrecord_padview);

        initView();
        mProgressBar.setMaxProgress(RECORD_CAMERA_MAX / 1000);
        mProgressBar
                .setOnProgressTouchListener(new CameraProgressBar.OnProgressTouchListener() {
                    @Override
                    public void onClick(CameraProgressBar progressBar) {

                        if (mDrawPadCamera != null) {
                            /**
                             * 这里只是暂停和恢复录制, 可以录制多段,但不可以删除录制好的每一段,
                             *
                             * 如果你要分段录制,并支持回删,则可以采用SegmentStart和SegmentStop;
                             */
                            if (mDrawPadCamera.isRecording()) {
                                mDrawPadCamera.pauseRecord(); // 暂停录制,如果要停止录制
                            } else {
                                mDrawPadCamera.startRecord();
                            }
                        }
                    }
                });
        dstPath = LanSongFileUtil.newMp4PathInBox();
        initDrawPad(); // 开始录制.
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        LanSongUtil.hideBottomUIMenu(this);
        super.onResume();
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                    TAG);
            mWakeLock.acquire();
        }
        startDrawPad();
        tvSubLayerHint.setText("演示增加多个子图层");
        subLayerAngle = 0;
        subLayerArray.clear();
    }

    /**
     * Step1: 开始运行 drawPad 容器
     */
    private void initDrawPad() {
        int padWidth = 544;
        int padHeight = 960;

        /**
         * 设置录制时的一些参数.
         */
        mDrawPadCamera.setRealEncodeEnable(padWidth, padHeight, 3000 * 1024,
                (int) 25, dstPath);
        /**
         * 录制的同时,录制外面的声音.
         */
        mDrawPadCamera.setRecordMic(true);
        /**
         * 设置录制处理进度监听.
         */
        mDrawPadCamera.setOnDrawPadProgressListener(drawPadProgressListener);

        /**
         * 相机前后置.是否设置滤镜.
         */
        mDrawPadCamera.setCameraParam(false, new LanSongBlurFilter());

        /**
         * 当手动聚焦的时候, 返回聚焦点的位置,让focusView去显示一个聚焦的动画.
         */
        mDrawPadCamera.setCameraFocusListener(new doFousEventListener() {

            @Override
            public void onFocus(int x, int y) {
                // TODO Auto-generated method stub
                focusView.startFocus(x, y);
            }
        });
        /**
         *
         * UI界面有效后, 开始开启DrawPad线程, 来预览画面.
         */
        mDrawPadCamera.setOnViewAvailable(new onViewAvailable() {

            @Override
            public void viewAvailable(DrawPadCameraView v) {
                // TODO Auto-generated method stub
                startDrawPad();
            }
        });
        mDrawPadCamera.setOnDrawPadErrorListener(new onDrawPadErrorListener() {

            @Override
            public void onError(DrawPad d, int what) {
                // TODO Auto-generated method stub
                Log.e(TAG, "DrawPad容器线程运行出错!!!" + what);
            }
        });
    }

    /**
     * Step2: 开始运行 Drawpad线程.
     */
    private void startDrawPad() {
        if (mDrawPadCamera.setupDrawPad()) {
            mCamLayer = mDrawPadCamera.getCameraLayer();
            mDrawPadCamera.startPreview(); // 容器开始预览
        } else {
            Log.i(TAG, "建立drawpad线程失败.");
        }
    }

    /**
     * Step3: 停止容器, 停止后,为新的视频文件增加上音频部分.
     */
    private void stopDrawPad() {
        if (mDrawPadCamera != null && mDrawPadCamera.isRunning()) {
            mDrawPadCamera.stopDrawPad();
            mCamLayer = null;
            subLayerArray.clear();
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
                            if (mCamLayer != null) {
                                mCamLayer.switchFilterTo(filter);
                            }
                        }
                    });
        }
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
        stopDrawPad();

        LanSongFileUtil.deleteFile(dstPath);
        dstPath = null;
    }

    // -------------------------------------------一下是UI界面和控制部分.---------------------------------------------------
    private void initView() {
        findViewById(R.id.id_fullrecord_cancel).setOnClickListener(this);

        tvTime = (TextView) findViewById(R.id.id_fullscreen_timetv);

        tvSubLayerHint = (TextView) findViewById(R.id.id_camera_sublayer_hint);
        tvSubLayerHint.setText("(主图层虚化处理)演示增加/删除 多个子图层");

        btnOk = (ImageView) findViewById(R.id.id_fullrecord_ok);
        btnOk.setOnClickListener(this);

        findViewById(R.id.id_camera_sublayer_add).setOnClickListener(this);
        findViewById(R.id.id_camera_sublayer_delete).setOnClickListener(this);

        focusView = (FocusImageView) findViewById(R.id.id_fullrecord_focusview);

        findViewById(R.id.id_fullrecord_flashlight).setOnClickListener(this);
        findViewById(R.id.id_fullrecord_frontcamera).setOnClickListener(this);
        findViewById(R.id.id_fullrecord_filter).setOnClickListener(this);
        mProgressBar = (CameraProgressBar) findViewById(R.id.id_fullrecord_progress);
    }

    private void playVideo() {
        if (LanSongFileUtil.fileExist(dstPath)) {
            Intent intent = new Intent(this, VideoPlayerActivity.class);
            intent.putExtra("videopath", dstPath);
            startActivity(intent);
        } else {
            Toast.makeText(this, "目标文件不存在", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.id_fullrecord_cancel:

                this.finish();
                break;
            case R.id.id_fullrecord_ok:
                stopDrawPad();
                playVideo();
                break;
            case R.id.id_fullrecord_frontcamera:
                if (mCamLayer != null) {
                    if (mDrawPadCamera.isRunning()
                            && CameraLayer.isSupportFrontCamera()) {
                        // 先把DrawPad暂停运行.
                        mDrawPadCamera.pausePreview();
                        mCamLayer.changeCamera();
                        mDrawPadCamera.resumePreview(); // 再次开启.
                    }
                }
                break;
            case R.id.id_fullrecord_flashlight:
                if (mCamLayer != null) {
                    mCamLayer.changeFlash();
                }
                break;
            case R.id.id_fullrecord_filter:
                selectFilter();
                break;
            case R.id.id_camera_sublayer_add:
                if (mCamLayer != null) {
                    SubLayer layer = mCamLayer.addSubLayer();
                    layer.setScale(0.5f);

                    subLayerAngle += 10;
                    layer.setRotate(subLayerAngle);
                    subLayerArray.add(layer);
                    tvSubLayerHint.setText("演示增删多个子图层,个数:" + subLayerArray.size());
                }
                break;
            case R.id.id_camera_sublayer_delete:
                if (mCamLayer != null && subLayerArray.size() > 0) {
                    int index = subLayerArray.size() - 1;
                    SubLayer layer = subLayerArray.get(index);
                    mCamLayer.removeSubLayer(layer);
                    subLayerArray.remove(index);
                    subLayerAngle -= 10;
                    tvSubLayerHint.setText("演示增删多个子图层,个数:" + subLayerArray.size());
                }
                break;
            default:
                break;
        }
    }
}