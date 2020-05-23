package com.example.advanceDemo.scene;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.advanceDemo.VideoPlayerActivity;
import com.example.advanceDemo.view.StereoView;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.DrawPadUpdateMode;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.ViewLayer;
import com.lansosdk.box.ViewLayerRelativeLayout;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.AudioEditor;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.LanSongFileUtil;
import com.lansosdk.videoeditor.MediaInfo;

import java.io.IOException;

/**
 * 演示: 视频和UI界面的 实时叠加.
 * <p>
 * 把一个立体图形转化为
 */
public class VViewImage3DDemoActivity extends Activity {
    private static final String TAG = "VideoActivity";

    private String mVideoPath;

    private DrawPadView drawpadView;

    private MediaPlayer mplayer = null;

    private VideoLayer videoLayer = null;
    private ViewLayer viewLayer = null;

    //
    private String editTmpPath = null;
    private String dstPath = null;
    private MediaInfo mInfo;
    private ViewLayerRelativeLayout mGLRelativeLayout;
    private StereoView mStereoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vview_image3d_demo_layout);

        mVideoPath = getIntent().getStringExtra("videopath");
        mInfo = new MediaInfo(mVideoPath);
        if (!mInfo.prepare()) {
            Toast.makeText(this, "传递过来的视频文件错误", Toast.LENGTH_SHORT).show();
            this.finish();
        }

        drawpadView = (DrawPadView) findViewById(R.id.id_image3d_drawpadview);
        mGLRelativeLayout = (ViewLayerRelativeLayout) findViewById(R.id.id_image3d_viewlayout);
        mStereoView = (StereoView) findViewById(R.id.id_image3d_stereoView);
        mStereoView.setVisibility(View.INVISIBLE);

        findViewById(R.id.id_image3d_saveplay).setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (LanSongFileUtil.fileExist(dstPath)) {
                            Intent intent = new Intent(
                                    VViewImage3DDemoActivity.this,
                                    VideoPlayerActivity.class);
                            intent.putExtra("videopath", dstPath);
                            startActivity(intent);
                        } else {
                            Toast.makeText(VViewImage3DDemoActivity.this,
                                    "目标文件不存在", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        findViewById(R.id.id_image3d_saveplay).setVisibility(View.GONE);

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
        super.onResume();
    }

    private void startPlayVideo() {
        mplayer = new MediaPlayer();
        try {
            mplayer.setDataSource(mVideoPath);
            mplayer.setOnPreparedListener(new OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    initDrawPad(mp);
                }
            });
            mplayer.setOnCompletionListener(new OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    completeDrawPad();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        mplayer.prepareAsync();
    }

    private void initDrawPad(MediaPlayer mp) {
        drawpadView.setUpdateMode(DrawPadUpdateMode.ALL_VIDEO_READY, 25);
        drawpadView.setRealEncodeEnable(480, 480, (int) mInfo.vFrameRate, editTmpPath);
        drawpadView.setDrawPadSize(480, 480,
                new onDrawPadSizeChangedListener() {

                    @Override
                    public void onSizeChanged(int viewWidth, int viewHeight) {
                        startDrawPad();
                    }
                });
    }

    private void startDrawPad() {
        drawpadView.pauseDrawPad();
        if (drawpadView.startDrawPad()) {
            videoLayer = drawpadView.addMainVideoLayer(mplayer.getVideoWidth(),
                    mplayer.getVideoHeight(), null);
            if (videoLayer != null) {
                mplayer.setSurface(new Surface(videoLayer.getVideoTexture()));
            }
            mplayer.start();
            addViewLayer();
            mStereoView.setVisibility(View.VISIBLE);
            drawpadView.resumeDrawPad();
        }
    }

    private void completeDrawPad() {
        if (drawpadView != null && drawpadView.isRunning()) {
            drawpadView.stopDrawPad();

            toastStop();

            if (LanSongFileUtil.fileExist(editTmpPath)) {
                dstPath = AudioEditor.mergeAudioNoCheck(mVideoPath,editTmpPath,true);
                findViewById(R.id.id_image3d_saveplay).setVisibility(View.VISIBLE);
            }
        }
    }

    private void addViewLayer() {
        if (drawpadView != null && drawpadView.isRunning()) {
            viewLayer = drawpadView.addViewLayer();
            mGLRelativeLayout.bindViewLayer(viewLayer);
            mGLRelativeLayout.invalidate();

            ViewGroup.LayoutParams params = mGLRelativeLayout.getLayoutParams();
            params.height = viewLayer.getPadHeight(); // 因为布局时, 宽度一致,
            // 这里调整高度,让他们一致.
            mGLRelativeLayout.setLayoutParams(params);

            // UI图层的移动缩放旋转. 如果您需要,可以增加.
            // mViewLayer.setScale(0.5f);
            // mViewLayer.setRotate(60);
            // mViewLayer.setPosition(mViewLayer.getPadWidth()-mViewLayer.getLayerWidth()/4,mViewLayer.getPositionY()/4);
        }

    }

    private void toastStop() {
        Toast.makeText(getApplicationContext(), "录制已停止!!", Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mplayer != null) {
            mplayer.stop();
            mplayer.release();
            mplayer = null;
        }
        if (drawpadView != null) {
            drawpadView.stopDrawPad();
            drawpadView = null;
        }
        LanSongFileUtil.deleteFile(dstPath);
        LanSongFileUtil.deleteFile(editTmpPath);
    }

}
