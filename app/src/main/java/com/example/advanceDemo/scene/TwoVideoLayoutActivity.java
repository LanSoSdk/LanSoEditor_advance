package com.example.advanceDemo.scene;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.advanceDemo.VideoPlayerActivity;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.DrawPadUpdateMode;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.AudioEditor;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.LanSongFileUtil;

import java.io.IOException;

import static com.example.advanceDemo.utils.CopyFileFromAssets.copyAssets;


public class TwoVideoLayoutActivity extends Activity {
    private static final String TAG = "TwoVideoLayoutActivity";
    boolean isDestorying = false; // 是否正在销毁, 因为销毁会停止DrawPad
    private String mVideoPath;
    private String videoPath2;
    private DrawPadView drawPadView;
    private MediaPlayer mplayer1 = null;
    private MediaPlayer mplayer2 = null;
    private boolean mplayerReady = false;
    private boolean mplayer2Ready = false;
    private VideoLayer videoLayer1 = null;
    private VideoLayer videoLayer2 = null;
    private String editTmpPath = null;
    private String dstPath = null;
    private LinearLayout playVideo;
    private MediaInfo mInfo;
    private TextView tvHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.videopicture_layout);
        initView();

        mVideoPath = getIntent().getStringExtra("videopath");
        drawPadView = (DrawPadView) findViewById(R.id.id_videopicture_drawpadview);

        /**
         * 在手机的默认路径下创建一个文件名, 用来保存生成的视频文件,(在onDestroy中删除)
         */
        editTmpPath = LanSongFileUtil.newMp4PathInBox();
        dstPath = LanSongFileUtil.newMp4PathInBox();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                startPlayVideo12();
            }
        }, 200);
    }

    /**
     * 把视频1,2 都准备好.
     */
    private void startPlayVideo12() {
        videoPath2 = copyAssets(getApplicationContext(),"ping5s.mp4");

        if (mVideoPath != null && videoPath2 != null) {
            mplayer1 = new MediaPlayer();
            mplayer2 = new MediaPlayer();
            try {
                mplayer1.setDataSource(mVideoPath);
                mplayer2.setDataSource(videoPath2);
                mplayer2.setVolume(0.0f,0.0f);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mplayer1.setOnPreparedListener(new OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    mplayerReady = true;
                    initDrawPad();
                }
            });
            mplayer2.setOnPreparedListener(new OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    mplayer2Ready = true;
                    initDrawPad();
                }
            });
            mplayer1.setOnCompletionListener(new OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopDrawPad();
                }
            });
            mplayer1.prepareAsync();
            mplayer2.prepareAsync();
        } else {
            finish();
            return;
        }
    }

    /**
     * Step1: init Drawpad 初始化DrawPad
     */
    private void initDrawPad() {
        if (mplayerReady && mplayer2Ready) {
            mInfo = new MediaInfo(mVideoPath);
            if (mInfo.prepare()) {
                drawPadView.setUpdateMode(DrawPadUpdateMode.AUTO_FLUSH,  (int) mInfo.vFrameRate);
                drawPadView.setRealEncodeEnable(640, 640, (int) mInfo.vFrameRate, editTmpPath);
                drawPadView.setDrawPadSize(640,640, new onDrawPadSizeChangedListener() {
                    @Override
                    public void onSizeChanged(int viewWidth,int viewHeight) {
                        startDrawPad();
                    }
                });
            }
        }
    }

    /**
     * Step2: start drawPad 开始运行这个容器.
     */
    private void startDrawPad() {
        drawPadView.pauseDrawPad(); // 先标志线程在开启后,暂停.
        if (drawPadView.startDrawPad()) {

            /**
             * 开始增加视频图层.
             */
            BitmapLayer layer = drawPadView.addBitmapLayer(BitmapFactory.decodeResource(getResources(), R.drawable.videobg));
            layer.setScaledValue(layer.getPadWidth(), layer.getPadHeight()); // 增加一个背景,填充整个屏幕.

            // 增加一个主视频.
            videoLayer1 = drawPadView.addMainVideoLayer(
                    mplayer1.getVideoWidth(), mplayer1.getVideoHeight(), null);
            if (videoLayer1 != null) {
                mplayer1.setSurface(new Surface(videoLayer1.getVideoTexture()));
                mplayer1.start();
            }

            // 增加另一个视频.
            videoLayer2 = drawPadView.addVideoLayer(mplayer2.getVideoWidth(), mplayer2.getVideoHeight(), null);
            mplayer2.setSurface(new Surface(videoLayer2.getVideoTexture())); // 视频
            mplayer2.start();

            drawPadView.resumeDrawPad();

            // 对两个视频布局一下.
            videoLayer1.setScale(0.5f, 0.5f);
            videoLayer1.setPosition(videoLayer1.getPadWidth() / 4.0f,videoLayer1.getPositionY());
            videoLayer2.setScale(0.5f, 0.5f);
            videoLayer2.setPosition(videoLayer2.getPadWidth() * 3.0f / 4.0f,videoLayer2.getPositionY());
        }
    }

    private void stopDrawPad() {
        if (drawPadView != null && drawPadView.isRunning()) {
            drawPadView.stopDrawPad();

            if (mplayer2 != null) {
                mplayer2.stop();
                mplayer2.release();
                mplayer2 = null;
            }
            Toast.makeText(getApplicationContext(), "录制已停止!!",Toast.LENGTH_SHORT).show();
            if (LanSongFileUtil.fileExist(editTmpPath)) {
                dstPath = AudioEditor.mergeAudioNoCheck(mVideoPath, editTmpPath, true);
                playVideo.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        isDestorying = true;
        if (mplayer1 != null) {
            mplayer1.stop();
            mplayer1.release();
            mplayer1 = null;
        }

        if (drawPadView != null) {
            drawPadView.stopDrawPad();
            drawPadView = null;
        }
        LanSongFileUtil.deleteFile(dstPath);
        LanSongFileUtil.deleteFile(editTmpPath);
    }

    private void initView() {
        tvHint = (TextView) findViewById(R.id.id_videopicture_hint);
        tvHint.setText(R.string.vdieo2_layout);

        playVideo = (LinearLayout) findViewById(R.id.id_videopicture_saveplay);
        playVideo.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (LanSongFileUtil.fileExist(dstPath)) {
                    Intent intent = new Intent(TwoVideoLayoutActivity.this,VideoPlayerActivity.class);
                    intent.putExtra("videopath", dstPath);
                    startActivity(intent);
                } else {
                    Toast.makeText(TwoVideoLayoutActivity.this, "目标文件不存在",Toast.LENGTH_SHORT).show();
                }
            }
        });
        playVideo.setVisibility(View.GONE);
    }

}
