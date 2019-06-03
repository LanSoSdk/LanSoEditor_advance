package com.example.advanceDemo.layerDemo;

import android.app.Activity;
import android.content.Context;
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
import com.example.advanceDemo.view.MarkArrowView;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.AudioEditor;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.LanSongFileUtil;

import java.io.IOException;

/**
 *
 */
public class BitmapLayerMarkActivity extends Activity {
    private static final String TAG = "BitmapLayerDemoActivity";

    private String mVideoPath;

    private MarkArrowView markView;

    private MediaPlayer mplayer = null;

    private VideoLayer mLayerMain = null;

    private String editTmpPath = null;
    private String dstPath = null;
    private LinearLayout playVideo = null;
    private Context mContext = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawpad_touch_layout);
        mContext = getApplicationContext();

        mVideoPath = getIntent().getStringExtra("videopath");
        markView = (MarkArrowView) findViewById(R.id.markarrow_view);
        playVideo = (LinearLayout) findViewById(R.id.id_markarrow_saveplay);
        playVideo.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (LanSongFileUtil.fileExist(dstPath)) {
                    Intent intent = new Intent(mContext,
                            VideoPlayerActivity.class);
                    intent.putExtra("videopath", dstPath);
                    startActivity(intent);
                } else {
                    Toast.makeText(mContext, "目标文件不存在", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
        playVideo.setVisibility(View.GONE);

        /**
         * 在手机的默认路径下创建一个文件名, 用来保存生成的视频文件,(在onDestroy中删除)
         */
        editTmpPath = LanSongFileUtil.createMp4FileInBox();
        dstPath = LanSongFileUtil.createMp4FileInBox();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                startPlayVideo();
            }
        }, 200);
    }

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
     * Step1: 初始化 drawPad 容器
     */
    private void initDrawPad() {
        MediaInfo info = new MediaInfo(mVideoPath);
        if (info.prepare()) {
            markView.setRealEncodeEnable(480, 480,(int) info.vFrameRate, editTmpPath);
            markView.setDrawPadSize(480, 480,new onDrawPadSizeChangedListener() {
                        @Override
                        public void onSizeChanged(int viewWidth, int viewHeight) {
                            // TODO Auto-generated method stub
                            startDrawPad();
                        }
                    });
        }
    }

    /**
     * Step2:增加一个BitmapLayer到容器上.已经在MarkArrowView中实现了.
     */
    private void startDrawPad() {
        markView.startDrawPad();
        mLayerMain = markView.addMainVideoLayer(mplayer.getVideoWidth(),mplayer.getVideoHeight(), null);
        if (mLayerMain != null) {
            mplayer.setSurface(new Surface(mLayerMain.getVideoTexture()));
        }
        mplayer.start();
    }

    /**
     * Step3: 增加完成后, 停止容器DrawPad 停止后,为新的视频文件增加上音频部分.
     */
    private void stopDrawPad() {
        if (markView != null && markView.isRunning()) {
            markView.stopDrawPad();
            toastStop();

            if (LanSongFileUtil.fileExist(editTmpPath)) {
                dstPath= AudioEditor.mergeAudioNoCheck(mVideoPath, editTmpPath, true);
                playVideo.setVisibility(View.VISIBLE);
            }
        }
    }

    private void toastStop() {
        Toast.makeText(getApplicationContext(), "录制已停止!!", Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        if (mplayer != null) {
            mplayer.stop();
            mplayer.release();
            mplayer = null;
        }
        if (markView != null) {
            markView.stopDrawPad();
            markView = null;
        }
        if (LanSongFileUtil.fileExist(editTmpPath)) {
            LanSongFileUtil.deleteFile(editTmpPath);
            editTmpPath = null;
        }
        if (LanSongFileUtil.fileExist(dstPath)) {
            LanSongFileUtil.deleteFile(dstPath);
            dstPath = null;
        }
    }
}
