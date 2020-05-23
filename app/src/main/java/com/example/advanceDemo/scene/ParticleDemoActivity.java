package com.example.advanceDemo.scene;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.advanceDemo.VideoPlayerActivity;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.ViewLayer;
import com.lansosdk.box.ViewLayerRelativeLayout;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.AudioEditor;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.LanSongFileUtil;
import com.lansosdk.videoeditor.MediaInfo;
import com.plattysoft.leonids.ParticleSystem;
import com.plattysoft.leonids.modifiers.AlphaModifier;
import com.plattysoft.leonids.modifiers.ScaleModifier;

import java.io.IOException;

/**
 * 演示:视频增加粒子效果
 */
public class ParticleDemoActivity extends Activity implements OnClickListener {
    private static final String TAG = "ViewLayerDemoActivity";
    RelativeLayout particleLayout;
    ParticleSystem ps;
    long lastTimeUs = 0;
    private String mVideoPath;
    private DrawPadView drawPadView;
    private MediaPlayer mplayer = null;
    private VideoLayer mainVideoLayer = null;
    private ViewLayer mViewLayer = null;
    //
    private String editTmpPath = null; // 用来保存容器录制的目标文件路径.
    private String dstPath = null;
    private ViewLayerRelativeLayout mLayerRelativeLayout;
    private MediaInfo mInfo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.particel_demo_layout);

        mVideoPath = getIntent().getStringExtra("videopath");
        mInfo = new MediaInfo(mVideoPath);
        if (!mInfo.prepare()) {
            Log.e(TAG, " video path is error.finish\n");
            finish();
        }

        drawPadView = (DrawPadView) findViewById(R.id.id_particle_drawpadview);
        mLayerRelativeLayout = (ViewLayerRelativeLayout) findViewById(R.id.id_particle_viewlayerlayout);
        particleLayout = (RelativeLayout) findViewById(R.id.id_particle_layout);
        initView();

        editTmpPath = LanSongFileUtil.newMp4PathInBox();
        dstPath = LanSongFileUtil.newMp4PathInBox();

        touchShot();
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                startPlayVideo();
            }
        }, 500);
    }

    private void startPlayVideo() {
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
    }

    private void initDrawPad() {
        drawPadView.setRealEncodeEnable(640, 640,(int) mInfo.vFrameRate, editTmpPath);
        drawPadView.setDrawPadSize(640, 640, new onDrawPadSizeChangedListener() {
            @Override
            public void onSizeChanged(int viewWidth, int viewHeight) {
                startDrawPad();
            }
        });
    }

    private void startDrawPad() {
        if (drawPadView.startDrawPad()) {
            mainVideoLayer = drawPadView.addMainVideoLayer(mplayer.getVideoWidth(), mplayer.getVideoHeight(), null);
            if (mainVideoLayer != null) {
                mplayer.setSurface(new Surface(mainVideoLayer.getVideoTexture()));
            }
            mplayer.start();
            addViewLayer();
        }
    }

    private void stopDrawPad() {
        if (drawPadView != null && drawPadView.isRunning()) {
            drawPadView.stopDrawPad();
            toastStop();
            if (LanSongFileUtil.fileExist(editTmpPath)) {
                dstPath=AudioEditor.mergeAudioNoCheck(mVideoPath, editTmpPath, true);
                findViewById(R.id.id_particle_saveplay).setVisibility(
                        View.VISIBLE);
            }
        }
    }

    /**
     * 增加一个UI图层: ViewLayer
     */
    private void addViewLayer() {
        if (drawPadView != null && drawPadView.isRunning()) {
            mViewLayer = drawPadView.addViewLayer();

            // 把这个图层绑定到LayerRelativeLayout中.从而LayerRelativeLayout中的各种UI界面会被绘制到Drawpad上.
            mLayerRelativeLayout.bindViewLayer(mViewLayer);

            mLayerRelativeLayout.invalidate();// 刷新一下.

            ViewGroup.LayoutParams params = mLayerRelativeLayout.getLayoutParams();
            params.height = mViewLayer.getPadHeight(); // 因为布局时, 宽度一致,
            // 这里调整高度,让他们一致.
            mLayerRelativeLayout.setLayoutParams(params);
        }
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_particle_touch:
                touchShot();
                break;
            case R.id.id_particle_oneshot:
                oneShot();
                break;
            case R.id.id_particle_baoza:
                baozhaShot();
                break;
            case R.id.id_particle_yunduo:
                yunduoShot();
                break;
            default:
                break;
        }
    }

    private void touchShot() {
        particleLayout.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        // 开始粒子效果.
                        Drawable drawable = getResources().getDrawable(
                                R.drawable.star_pink);
                        ps = new ParticleSystem(particleLayout, 100, drawable, 800);
                        ps.setScaleRange(0.7f, 1.3f);
                        ps.setSpeedRange(0.05f, 0.1f);
                        ps.setRotationSpeedRange(90, 180);
                        ps.setFadeOut(200, new AccelerateInterpolator());
                        ps.emit((int) event.getX(), (int) event.getY() - 10, 40);

                        break;
                    case MotionEvent.ACTION_MOVE:
                        ps.updateEmitPoint((int) event.getX(), (int) event.getY());
                        break;
                    case MotionEvent.ACTION_UP:
                        ps.stopEmitting();
                        break;
                }
                return true;
            }
        });
    }

    /**
     * 单点触发
     */
    private void oneShot() {
        Drawable drawable = getResources().getDrawable(R.drawable.star_pink);
        ps = new ParticleSystem(particleLayout, 100, drawable, 800);
        ps.setSpeedRange(0.1f, 0.25f);
        ps.oneShot(particleLayout, 100);
    }

    private void baozhaShot() {
        Drawable drawable = getResources().getDrawable(
                R.drawable.animated_confetti);
        ps = new ParticleSystem(particleLayout, 100, drawable, 5000);
        ps.setSpeedRange(0.1f, 0.25f);
        ps.setRotationSpeedRange(90, 180);
        ps.setInitialRotationRange(0, 360);
        ps.oneShot(particleLayout, 100);
    }

    private void yunduoShot() {
        Drawable drawable = getResources().getDrawable(R.drawable.dust);
        ps = new ParticleSystem(particleLayout, 4, drawable, 3000);
        ps.setSpeedByComponentsRange(-0.025f, 0.025f, -0.06f, -0.08f);
        ps.setAcceleration(0.00001f, 30);
        ps.setInitialRotationRange(0, 360);
        ps.addModifier(new AlphaModifier(255, 0, 1000, 3000));
        ps.addModifier(new ScaleModifier(0.5f, 2f, 0, 1000));
        ps.oneShot(particleLayout, 4);
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
        if (drawPadView != null) {
            drawPadView.stopDrawPad();
            drawPadView = null;
        }
        LanSongFileUtil.deleteFile(dstPath);
        LanSongFileUtil.deleteFile(editTmpPath);
    }

    // --------------------------------------一下为UI界面-----------------------------------------------------------
    private void initView() {
        findViewById(R.id.id_particle_yunduo).setOnClickListener(this);
        findViewById(R.id.id_particle_touch).setOnClickListener(this);
        findViewById(R.id.id_particle_oneshot).setOnClickListener(this);
        findViewById(R.id.id_particle_baoza).setOnClickListener(this);

        findViewById(R.id.id_particle_saveplay).setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (LanSongFileUtil.fileExist(dstPath)) {
                            Intent intent = new Intent(
                                    ParticleDemoActivity.this,
                                    VideoPlayerActivity.class);
                            intent.putExtra("videopath", dstPath);
                            startActivity(intent);
                        } else {
                            Toast.makeText(ParticleDemoActivity.this,
                                    "目标文件不存在", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        findViewById(R.id.id_particle_saveplay).setVisibility(View.GONE);
    }
}
