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
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.advanceDemo.VideoPlayerActivity;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.TwoVideoLayer;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.AudioEditor;
import com.lansosdk.videoeditor.CopyFileFromAssets;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.LanSongMergeAV;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.LanSongFileUtil;

import java.io.IOException;

import com.lansosdk.LanSongFilter.Rotation;

/**
 * 演示<双视频图层>的功能.
 */

public class TwoVideoLayerActivity extends Activity {
    private static final String TAG = "TwoVideoLayerActivity";

    private String mVideoPath;

    private DrawPadView mDrawPadView;

    private MediaPlayer mplayer = null;
    private MediaPlayer mplayer2 = null;
    private TwoVideoLayer twoVideoLayer = null;

    private String editTmpPath = null;
    private String dstPath = null;
    private LinearLayout playVideo;
    private MediaInfo mInfo;
    private boolean isDisplayed = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.twovideolayer_demo_layout);

        mVideoPath = getIntent().getStringExtra("videopath");

        mDrawPadView = (DrawPadView) findViewById(R.id.id_twovideolayer_view);
        mInfo = new MediaInfo(mVideoPath);
        if (!mInfo.prepare()) {
            Toast.makeText(TwoVideoLayerActivity.this, "视频源文件错误!",
                    Toast.LENGTH_SHORT).show();
            this.finish();
        }

        playVideo = (LinearLayout) findViewById(R.id.id_twovideolayer_saveplay);
        playVideo.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (LanSongFileUtil.fileExist(dstPath)) {
                    Intent intent = new Intent(TwoVideoLayerActivity.this,
                            VideoPlayerActivity.class);
                    intent.putExtra("videopath", dstPath);
                    startActivity(intent);
                } else {
                    Toast.makeText(TwoVideoLayerActivity.this, "目标文件不存在",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        playVideo.setVisibility(View.GONE);
        findViewById(R.id.id_twovideolayer_testbutton).setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // TODO Auto-generated method stub
                        if (twoVideoLayer != null) {
                            isDisplayed = !isDisplayed;
                            twoVideoLayer.setDisplayTexture2(isDisplayed);
                        }
                    }
                });
        // 在手机的默认路径下创建一个文件名,用来保存生成的视频文件,(在onDestroy中删除)
        editTmpPath = LanSongFileUtil.newMp4PathInBox();
        dstPath = LanSongFileUtil.newMp4PathInBox();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                startPlayVideo();
            }
        }, 500);
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
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mplayer.setOnPreparedListener(new OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    // TODO Auto-generated method stub
                    initDrawPad(mp);
                }
            });
            mplayer.setOnCompletionListener(new OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    // TODO Auto-generated method stub
                    stopDrawPad();
                }
            });
            mplayer.prepareAsync();
        } else {
            finish();
            return;
        }
    }

    /**
     * Step1: init DrawPad 初始化
     *
     * @param mp
     */
    private void initDrawPad(MediaPlayer mp) {
        int padWidth = 480;
        int padHeight = 480;

        /**
         * 设置使能 实时录制, 即把正在DrawPad中呈现的画面实时的保存下来,实现所见即所得的模式
         */
        mDrawPadView.setRealEncodeEnable(padWidth, padHeight, 1200 * 1000,
                (int) mInfo.vFrameRate, editTmpPath);

        mDrawPadView.setUseMainVideoPts(true);

        mDrawPadView.setDrawPadSize(padWidth, padHeight,
                new onDrawPadSizeChangedListener() {

                    @Override
                    public void onSizeChanged(int viewWidth, int viewHeight) {
                        // TODO Auto-generated method stub
                        startDrawPad();
                    }
                });
    }

    /**
     * Step2: 开始运行 Drawpad
     */
    private void startDrawPad() {
        // 开始DrawPad的渲染线程.
        mDrawPadView.pauseDrawPad();
        if (mDrawPadView.startDrawPad()) {
            // 增加一个主视频的 VideoLayer
            twoVideoLayer = mDrawPadView.addTwoVideoLayer(
                    mplayer.getVideoWidth(), mplayer.getVideoHeight());
            if (twoVideoLayer != null) {
                mplayer.setSurface(new Surface(twoVideoLayer.getVideoTexture()));
            }
            mplayer.start();

            // 增加第二个视频.
            mplayer2 = new MediaPlayer();
            try {
                String video = CopyFileFromAssets.copyAssets(
                        getApplicationContext(), "taohua.mp4");
                mplayer2.setDataSource(video);
                mplayer2.prepare();
                mplayer2.setLooping(true);
                mplayer2.setSurface(new Surface(twoVideoLayer
                        .getVideoTexture2()));
                mplayer2.start();

                if (mInfo.vRotateAngle == 90) { // mInfo是主视频的MediaInfo
                    twoVideoLayer.setSecondVideoMirror(Rotation.ROTATION_270,
                            true, false);
                } else if (mInfo.vRotateAngle == 270) {
                    twoVideoLayer.setSecondVideoMirror(Rotation.ROTATION_90,
                            true, false);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mDrawPadView.resumeDrawPad();
        }
    }

    /**
     * Step3: stop DrawPad
     */
    private void stopDrawPad() {
        if (mDrawPadView != null && mDrawPadView.isRunning()) {

            mDrawPadView.stopDrawPad();
            Toast.makeText(getApplicationContext(), "录制已停止!!",
                    Toast.LENGTH_SHORT).show();

            // 增加音频
            if (LanSongFileUtil.fileExist(editTmpPath)) {

                dstPath= AudioEditor.mergeAudioNoCheck(mVideoPath, editTmpPath, true);
                playVideo.setVisibility(View.VISIBLE);
            } else {
                Log.e(TAG, " player completion, but file:" + editTmpPath
                        + " is not exist!!!");
            }
        }
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
        // TODO Auto-generated method stub
        super.onDestroy();
        LanSongFileUtil.deleteFile(dstPath);
        LanSongFileUtil.deleteFile(editTmpPath);
    }
}
