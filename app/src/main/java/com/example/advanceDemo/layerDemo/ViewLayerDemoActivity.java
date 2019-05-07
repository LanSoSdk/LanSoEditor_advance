package com.example.advanceDemo.layerDemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.advanceDemo.utils.DemoUtil;
import com.example.advanceDemo.view.ImageTouchView;
import com.example.advanceDemo.view.PaintConstants;
import com.example.advanceDemo.view.StickerView;
import com.example.advanceDemo.view.TextStickerView;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.DrawPadUpdateMode;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.ViewLayer;
import com.lansosdk.box.ViewLayerRelativeLayout;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.AudioEditor;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.LanSongMergeAV;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.LanSongFileUtil;

import java.io.IOException;

public class ViewLayerDemoActivity extends Activity implements OnClickListener {
    private static final String TAG = "ViewLayerDemoActivity";
    ImageTouchView imgeTouchView;
    long lastTimeUs = 0;
    private String mVideoPath;
    private DrawPadView drawPadView;
    private MediaPlayer mplayer = null;
    private VideoLayer mainVideoLayer = null;
    //
    private String editTmpPath = null;
    private String dstPath = null;
    private ViewLayer mViewLayer = null;
    private ViewLayerRelativeLayout viewLayerRelativeLayout;
    private MediaInfo mInfo = null;
    private StickerView stickView;
    private TextStickerView textStickView;
    private TextView tvWord;
    private int stickCnt = 2;
    private String strInputText = "蓝松文字演示";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vview_drawimage_demo_layout);

        mVideoPath = getIntent().getStringExtra("videopath");
        mInfo = new MediaInfo(mVideoPath);
        if (!mInfo.prepare()) {
            Log.e(TAG, " video path is error.finish\n");
            finish();
        }

        drawPadView = (DrawPadView) findViewById(R.id.id_vview_realtime_drawpadview);

        initView();

        editTmpPath = LanSongFileUtil.newMp4PathInBox();
        dstPath = LanSongFileUtil.newMp4PathInBox();

        // 演示例子用到的.
        PaintConstants.SELECTOR.COLORING = true;
        PaintConstants.SELECTOR.KEEP_IMAGE = true;

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                startPlayVideo();
            }
        }, 200);
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

    /**
     * Step1: 设置DrawPad 容器的尺寸. 并设置是否实时录制容器上的内容.
     */
    private void initDrawPad() {
        MediaInfo info = new MediaInfo(mVideoPath);
        if (info.prepare()) {

            int padWidth = 640;
            int padHeight = 640;
            int frameRate = 25;

            drawPadView.setUpdateMode(DrawPadUpdateMode.AUTO_FLUSH, frameRate);
            drawPadView.setRealEncodeEnable(padWidth, padHeight, 2 * 1024 * 1024, (int) frameRate, editTmpPath);

            drawPadView.setOnDrawPadProgressListener(new onDrawPadProgressListener() {
                @Override
                public void onProgress(DrawPad v, long currentTimeUs) {
                    if (currentTimeUs > 7 * 1000 * 1000) { // 在第7秒的时候,不再显示.
                        hideWord();
                    } else if (currentTimeUs > 3 * 1000 * 1000) { // 在第三秒的时候,显示tvWord
                        showWord();
                    }
                }
            });
            drawPadView.setDrawPadSize(padWidth, padHeight, new onDrawPadSizeChangedListener() {
                @Override
                public void onSizeChanged(int viewWidth, int viewHeight) {
                    startDrawPad();
                }
            });
        }
    }

    /**
     * Step2: 开始容器线程, 并增加一个视频图层和 view图层.
     */
    private void startDrawPad() {
        if (drawPadView.startDrawPad()) {
            // 给容器增加一个背景
            BitmapLayer layer = drawPadView.addBitmapLayer(BitmapFactory
                    .decodeResource(getResources(), R.drawable.videobg));
            layer.setScaledValue(layer.getPadWidth(), layer.getPadHeight()); // 填充整个容器

            mainVideoLayer = drawPadView.addMainVideoLayer(
                    mplayer.getVideoWidth(), mplayer.getVideoHeight(), null);
            if (mainVideoLayer != null) {
                mplayer.setSurface(new Surface(mainVideoLayer.getVideoTexture()));
            }
            mplayer.start();
            addViewLayer();
        }
    }

    /**
     * Step3: 做好后, 停止容器, 因为容器里没有声音, 这里增加上原来的声音.
     */
    private void stopDrawPad() {
        if (drawPadView != null && drawPadView.isRunning()) {
            drawPadView.stopDrawPad();
            toastStop();
            if (LanSongFileUtil.fileExist(editTmpPath)) {
                dstPath = AudioEditor.mergeAudioNoCheck(mVideoPath, editTmpPath, true);
                findViewById(R.id.id_vview_realtime_saveplay).setVisibility(
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

            // 绑定
            viewLayerRelativeLayout.bindViewLayer(mViewLayer);

            viewLayerRelativeLayout.invalidate();// 刷新一下.

            ViewGroup.LayoutParams params = viewLayerRelativeLayout
                    .getLayoutParams();
            params.height = mViewLayer.getPadHeight(); // 因为布局时, 宽度一致,
            // 这里调整高度,让他们一致.
            viewLayerRelativeLayout.setLayoutParams(params);

            // mViewLayer.switchFilterTo(new LanSongSepiaFilter());

            // UI图层的移动缩放旋转.
            // mViewLayer.setScale(0.5f);
            // mViewLayer.setRotate(60);
            // mViewLayer.setPosition(mViewLayer.getPadWidth()-mViewLayer.getLayerWidth()/4,mViewLayer.getPositionY()
            // /4);
        }
    }

    private void toastStop() {
        Toast.makeText(getApplicationContext(), "录制已停止!!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (drawPadView != null) {
            drawPadView.stopDrawPad();
            drawPadView = null;
        }
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
        tvWord = (TextView) findViewById(R.id.id_vview_tvtest);
        findViewById(R.id.id_vview_drawimage_pause).setOnClickListener(this);
        findViewById(R.id.id_vview_drawimage_addstick).setOnClickListener(this);
        findViewById(R.id.id_vview_drawimage_addtext).setOnClickListener(this);

        imgeTouchView = (ImageTouchView) findViewById(R.id.switcher);
        imgeTouchView.setActivity(ViewLayerDemoActivity.this);

        stickView = (StickerView) findViewById(R.id.id_vview_drawimage_stickview);
        textStickView = (TextStickerView) findViewById(R.id.id_vview_drawimage_textstickview);

        viewLayerRelativeLayout = (ViewLayerRelativeLayout) findViewById(R.id.id_vview_realtime_gllayout);

        findViewById(R.id.id_vview_realtime_saveplay).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (LanSongFileUtil.fileExist(dstPath)) {
                            DemoUtil.startPlayDstVideo(ViewLayerDemoActivity.this, dstPath);
                        } else {
                            DemoUtil.showToast(ViewLayerDemoActivity.this, "目标文件不存在");
                        }
                    }
                });
        findViewById(R.id.id_vview_realtime_saveplay).setVisibility(View.GONE);
    }

    private void showWord() {
        if (tvWord != null && tvWord.getVisibility() != View.VISIBLE) {
            tvWord.startAnimation(AnimationUtils.loadAnimation(
                    ViewLayerDemoActivity.this, R.anim.slide_right_in));
            tvWord.setVisibility(View.VISIBLE);
        }
    }

    private void hideWord() {
        if (tvWord != null && tvWord.getVisibility() == View.VISIBLE) {
            tvWord.startAnimation(AnimationUtils.loadAnimation(
                    ViewLayerDemoActivity.this, R.anim.slide_right_out));
            tvWord.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_vview_drawimage_pause:
                if (mplayer != null) {
                    if (mplayer.isPlaying()) {
                        mplayer.pause();
                        drawPadView.pauseDrawPad();

                        // 解除绑定, 让android的UI线程可以更新UI界面;
                        if (viewLayerRelativeLayout != null) {
                            viewLayerRelativeLayout.unBindViewLayer();
                        }
                    } else {
                        mplayer.start();
                        drawPadView.resumeDrawPad();
                        if (viewLayerRelativeLayout != null) {
                            viewLayerRelativeLayout.bindViewLayer(mViewLayer);
                        }
                        // 把贴纸的边框去掉.
                        stickView.disappearIconBorder();
                        textStickView.disappearIconBorder();
                    }
                }
                break;
            case R.id.id_vview_drawimage_addstick:
                if (stickView != null) {
                    Bitmap bmp = null;
                    if (stickCnt == 2) {
                        bmp = BitmapFactory.decodeResource(getResources(),
                                R.drawable.stick2);
                    } else if (stickCnt == 3) {
                        bmp = BitmapFactory.decodeResource(getResources(),
                                R.drawable.stick3);
                    } else if (stickCnt == 4) {
                        bmp = BitmapFactory.decodeResource(getResources(),
                                R.drawable.stick4);
                    } else {
                        bmp = BitmapFactory.decodeResource(getResources(),
                                R.drawable.stick5);
                    }
                    stickCnt++;
                    stickView.addBitImage(bmp);
                }
                break;
            case R.id.id_vview_drawimage_addtext:
                showInputDialog();
                break;
            default:
                break;
        }
    }

    private void showInputDialog() {
        final EditText etInput = new EditText(this);
        new AlertDialog.Builder(this).setTitle("请输入文字").setView(etInput)
                .setPositiveButton("确定", new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String input = etInput.getText().toString();
                        if (input != null && !input.equals("")) {
                            strInputText = input;
                            textStickView.setText(strInputText);
                        }
                    }
                }).show();
    }

}
