package com.example.advanceDemo.layerDemo;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.example.advanceDemo.VideoPlayerActivity;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.Layer;
import com.lansosdk.box.MVLayer;
import com.lansosdk.box.MVLayerENDMode;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.box.onLayerAvailableListener;
import com.lansosdk.videoeditor.AudioEditor;
import com.lansosdk.videoeditor.CopyFileFromAssets;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.DrawPadView.onViewAvailable;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.LanSongFileUtil;

import java.io.IOException;

public class MVLayerDemoActivity extends Activity {
    private static final String TAG = "MVLayerDemoActivity";

    private String mVideoPath;

    private DrawPadView mDrawPadView;

    private MediaPlayer mplayer = null;
    private MediaPlayer mplayer2 = null;
    private VideoLayer mLayerMain = null;
    private MVLayer mvLayer = null;

    private String editTmpPath = null;
    private String dstPath = null;
    private MediaInfo mInfo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mvlayer_demo_layout);
        initView();

        mVideoPath = getIntent().getStringExtra("videopath");
        mDrawPadView = (DrawPadView) findViewById(R.id.id_mvlayer_padview);
        mInfo = new MediaInfo(mVideoPath);
        if (!mInfo.prepare()) {
            Log.e(TAG, "视频源文件错误!");
            this.finish();
        }
        // 在手机的默认路径下创建一个文件名,用来保存生成的视频文件,(在onDestroy中删除)
        editTmpPath = LanSongFileUtil.newMp4PathInBox();
        dstPath = LanSongFileUtil.newMp4PathInBox();


        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                startPlayVideo();
            }
        }, 300);
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        findViewById(R.id.id_mvlayer_saveplay).setVisibility(View.INVISIBLE);

    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        if (mplayer != null) {
            mplayer.stop();
            mplayer.release();
            mplayer = null;
        }
        if (mplayer2 != null) {
            mplayer2.stop();
            mplayer2.release();
            mplayer2 = null;
        }
        if (mDrawPadView != null) {
            mDrawPadView.stopDrawPad();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LanSongFileUtil.deleteFile(dstPath);
        LanSongFileUtil.deleteFile(editTmpPath);
    }

    /**
     * VideoLayer是外部提供画面来源,
     * 您可以用你们自己的播放器作为画面输入源,也可以用原生的MediaPlayer,只需要视频播放器可以设置surface即可.
     * 一下举例是采用MediaPlayer作为视频输入源.
     */
    private void startPlayVideo() {
        if (mVideoPath != null) {
            mplayer = new MediaPlayer();
            try {
                mplayer.setDataSource(mVideoPath);

            } catch (IOException e) {
                e.printStackTrace();
            }
            mplayer.setOnPreparedListener(new OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    initDrawPad();
                }
            });
            mplayer.setOnCompletionListener(new OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopDrawPad();
                }
            });
            mplayer.prepareAsync();
        } else {
            Log.e(TAG, "Null Data Source\n");
            finish();
            return;
        }
    }

    /**
     * Step1: 开始运行 drawPad 容器
     */
    private void initDrawPad() {
        // 设置使能 实时录制, 即把正在DrawPad中呈现的画面实时的保存下来,实现所见即所得的模式
        mDrawPadView.setRealEncodeEnable(480, 480,(int) mInfo.vFrameRate, editTmpPath);
        mDrawPadView.setOnDrawPadProgressListener(new onDrawPadProgressListener() {

            @Override
            public void onProgress(DrawPad v, long currentTimeUs) {
                // TODO Auto-generated method stub
                // Log.i(TAG,"MV当前时间戳是"+currentTimeUs);
            }
        });

        // 设置当前DrawPad的宽度和高度,并把宽度自动缩放到父view的宽度,然后等比例调整高度.
        mDrawPadView.setDrawPadSize(480, 480, new onDrawPadSizeChangedListener() {
            @Override
            public void onSizeChanged(int viewWidth, int viewHeight) {
                // 开始DrawPad的渲染线程.
                startDrawPad();
            }
        });
        mDrawPadView.setOnViewAvailable(new onViewAvailable() {

            @Override
            public void viewAvailable(DrawPadView v) {
                startPlayVideo();
            }
        });
    }

    /**
     * Step2: 开始运行 Drawpad线程.
     */
    private void startDrawPad() {
        if (mDrawPadView.startDrawPad()) {
            // 增加一个主视频的 VideoLayer
            mLayerMain = mDrawPadView.addMainVideoLayer(
                    mplayer.getVideoWidth(), mplayer.getVideoHeight(), null);
            if (mLayerMain != null) {
                mplayer.setSurface(new Surface(mLayerMain.getVideoTexture()));
            }
            mplayer.start();

            addMVLayer();
        }
    }

    /**
     * 增加一个MV图层.
     */
    private void addMVLayer() {
        String colorMVPath = CopyFileFromAssets.copyAssets(MVLayerDemoActivity.this, "mei.mp4");
        String maskMVPath = CopyFileFromAssets.copyAssets(MVLayerDemoActivity.this, "mei_b.mp4");

        mvLayer = mDrawPadView.addMVLayer(colorMVPath, maskMVPath, false); // <-----增加MVLayer
        if (mvLayer != null) {
            mvLayer.setOnLayerAvailableListener(new onLayerAvailableListener() {

                @Override
                public void onAvailable(Layer layer) {
                    mvLayer.setPlayEnable();
                }
            });
//            mvLayer.setScaledValue(mvLayer.getPadWidth(),mvLayer.getPadHeight());
//            // 设置它为满屏.
//            float scaleW = (float) mvLayer.getPadWidth() / (float) mvLayer.getLayerWidth();
//            float scaleH = mvLayer.getPadHeight() / (float) mvLayer.getLayerHeight();
//            mvLayer.setScale(scaleW, scaleH);
            // 可以设置当前的MV是否要录制到
            // mvLayer.setVisibility(AeLayer.VISIBLE_ONLY_PREVIEW);
            // 可以增加mv在播放结束后的三种模式, 停留在最后一帧/循环/消失/
            mvLayer.setEndMode(MVLayerENDMode.LOOP);
        }
    }

    /**
     * Step3: 停止容器,停止后,为新的视频文件增加上音频部分.
     */
    private void stopDrawPad() {
        if (mDrawPadView != null && mDrawPadView.isRunning()) {

            mDrawPadView.stopDrawPad();
            toastStop();
            if (LanSongFileUtil.fileExist(editTmpPath)) {
                dstPath= AudioEditor.mergeAudioNoCheck(mVideoPath, editTmpPath,true);
                findViewById(R.id.id_mvlayer_saveplay).setVisibility(View.VISIBLE);
            } else {
                Log.e(TAG, " player completion, but file:" + editTmpPath
                        + " is not exist!!!");
            }
        }
    }

    private void initView() {
        findViewById(R.id.id_mvlayer_saveplay).setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (LanSongFileUtil.fileExist(dstPath)) {
                            Intent intent = new Intent(
                                    MVLayerDemoActivity.this,
                                    VideoPlayerActivity.class);
                            intent.putExtra("videopath", dstPath);
                            startActivity(intent);
                        } else {
                            Toast.makeText(MVLayerDemoActivity.this, "目标文件不存在",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        findViewById(R.id.id_mvlayer_saveplay).setVisibility(View.GONE);
    }

    private void toastStop() {
        Toast.makeText(getApplicationContext(), "录制已停止!!", Toast.LENGTH_SHORT)
                .show();
        Log.i(TAG, "录制已停止!!");
    }
}
