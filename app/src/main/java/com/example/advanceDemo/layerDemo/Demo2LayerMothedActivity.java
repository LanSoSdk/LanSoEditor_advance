package com.example.advanceDemo.layerDemo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.example.advanceDemo.VideoPlayerActivity;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.Layer;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.AudioEditor;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.LanSongFileUtil;

import java.io.IOException;

/**
 * 演示: 使用DrawPad来实现 视频和视频的实时叠加.
 * 流程是: 先创建一个DrawPad,增加主VideoLayer,在播放过程中,再次增加一个VideoLayer然后可以调节SeekBar来对
 * Layer的每个参数进行调节.
 * 可以调节的有:平移,旋转,缩放,RGBA值,显示/不显示(闪烁)效果. 实际使用中, 可用这些属性来扩展一些功能.
 * 比如 调节另一个视频的RGBA中的A值来实现透明叠加效果,类似MV的效果.
 * 比如 调节另一个视频的平移,缩放,旋转来实现贴纸的效果.
 */
public class Demo2LayerMothedActivity extends Activity implements OnSeekBarChangeListener {
    private static final String TAG = "Demo2LayerMothedActivity";
    boolean isFirstRemove = false;
    boolean isDestorying = false; // 是否正在销毁, 因为销毁会停止DrawPad
    int RotateCnt = 0;
    private String srcPath;
    private DrawPadView drawPadView;
    private MediaPlayer mplayer = null;
    private VideoLayer videoLayer = null;
    private String editTmpPath = null;
    private String dstPath = null;
    private LinearLayout playVideo;
    private MediaInfo mInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo2_layer_layout);
        initView();

         srcPath = getIntent().getStringExtra("videopath");

        drawPadView = (DrawPadView) findViewById(R.id.id_mothed2_drawpadview);

        /**
         * 在手机的默认路径下创建一个文件名, 用来保存生成的视频文件,(在onDestroy中删除)
         */
        editTmpPath = LanSongFileUtil.newMp4PathInBox();
        dstPath = LanSongFileUtil.newMp4PathInBox();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                startPlayVideo();
            }
        }, 100);
    }

    private void startPlayVideo() {
        if (srcPath != null) {
            mplayer = new MediaPlayer();
            try {
                mplayer.setDataSource(srcPath);

            } catch (IOException e) {
                e.printStackTrace();
            }
            mplayer.setOnPreparedListener(new OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    initDrawPad(mp);
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
            finish();
            return;
        }
    }

    /**
     * Step1: init Drawpad 初始化DrawPad
     */
    private void initDrawPad(MediaPlayer mp) {
        mInfo = new MediaInfo(srcPath);
        if (mInfo.prepare()) {
            int width=mInfo.getWidth();
            int height=mInfo.getHeight();
            drawPadView.setRealEncodeEnable(width, height,(int) mInfo.vFrameRate, editTmpPath);

            /**
             * 设置当前DrawPad的宽度和高度,并把宽度自动缩放到父view的宽度,然后等比例调整高度.
             */
            drawPadView.setDrawPadSize(width, height,new onDrawPadSizeChangedListener() {

                @Override
                public void onSizeChanged(int viewWidth, int viewHeight) {
                    startDrawPad();
                }
            });
        }
    }

    /**
     * Step2: start drawPad 开始运行这个容器.
     */
    private void startDrawPad() {
        if (drawPadView.startDrawPad()) {
            /**
             * 增加一个背景, 用来说明裁剪掉的一部分是透明的
             */
            BitmapLayer layer = drawPadView.addBitmapLayer(BitmapFactory.decodeResource(getResources(), R.drawable.videobg));
            layer.setScaledValue(layer.getPadWidth(), layer.getPadHeight()); // 填充整个屏幕.

            /**
             *  增加视频图层;
             */
            videoLayer = drawPadView.addVideoLayer(mplayer.getVideoWidth(),mplayer.getVideoHeight(), null);
            if (videoLayer != null) {
                mplayer.setSurface(new Surface(videoLayer.getVideoTexture()));
                mplayer.start();
            }

        }
    }

    /**
     * Step3 第三步: 停止运行DrawPad
     */
    private void stopDrawPad() {
        if (drawPadView != null && drawPadView.isRunning()) {
            drawPadView.stopDrawPad();
            Toast.makeText(getApplicationContext(), "录制已停止!!",Toast.LENGTH_SHORT).show();
            if (LanSongFileUtil.fileExist(editTmpPath)) {

                MediaInfo.checkFile(srcPath);

                MediaInfo.checkFile(editTmpPath);


                dstPath= AudioEditor.mergeAudioNoCheck(srcPath, editTmpPath, true);
                playVideo.setVisibility(View.VISIBLE);

            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        isDestorying = true;
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

    private void initView() {
        initSeekBar(R.id.id_mothed2_skbar_rectleft, 100);
        initSeekBar(R.id.id_mothed2_skbar_rectround, 100);
        initSeekBar(R.id.id_mothed2_skbar_rectxy, 100);
        initSeekBar(R.id.id_mothed2_skbar_circle, 100);
        initSeekBar(R.id.id_mothed2_skbar_circle_center, 100);
        playVideo = (LinearLayout) findViewById(R.id.id_mothed2_saveplay);
        playVideo.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (LanSongFileUtil.fileExist(dstPath)) {
                    Intent intent = new Intent(Demo2LayerMothedActivity.this,
                            VideoPlayerActivity.class);
                    intent.putExtra("videopath", dstPath);
                    startActivity(intent);
                } else {
                    Toast.makeText(Demo2LayerMothedActivity.this, "目标文件不存在",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        playVideo.setVisibility(View.GONE);
    }

    private void initSeekBar(int resId, int maxvalue) {
        SeekBar skbar = (SeekBar) findViewById(resId);
        skbar.setOnSeekBarChangeListener(this);
        skbar.setMax(maxvalue);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {

        Layer optionLayer = (Layer) videoLayer; // 为了测试不同图层对象,特意转换一下.

        switch (seekBar.getId()) {

            case R.id.id_mothed2_skbar_rectleft: // 演示从左侧裁剪
                if (optionLayer != null) {
                    float startX = (float) progress / 100f;
                    optionLayer.setVisibleRect(startX, 1.0f, 0.0f, 1.0f);
                }
                break;
            case R.id.id_mothed2_skbar_rectround:
                if (optionLayer != null) {
                    float endX = (float) progress / 100f;
                    float half = endX / 2.0f;
                    optionLayer.setVisibleRect(0.5f - half, 0.5f + half, 0.0f, 1.0f);
                }
                break;
            case R.id.id_mothed2_skbar_rectxy: // 演示宽度和高度同时缩放
                if (videoLayer != null) {
                    float start = (float) progress / 100f;

                    float end = (float) start + 0.5f;
                    /**
                     * 设置可见区域, 区域为四方形, 其他不可见为透明 四方形表示为: 从左到右是0.0f---1.0f; 从上到下 是:
                     * 0.0f---1.0f
                     *
                     * @param startX
                     * @param endX
                     * @param startY
                     * @param endY
                     */
                    optionLayer.setVisibleRect(start, end, start, end);
                    /**
                     * 设置可见矩形四周的边框的宽度和颜色
                     *
                     * @param width
                     *            边框的宽度,最大是1.0, 最小是0.0, 推荐是0.01f
                     * @param r
                     *            RGBA分量中的Red 范围是0.0f---1.0f
                     * @param g
                     * @param b
                     * @param a
                     */
                    optionLayer.setVisibleRectBorder(0.0f, 1.0f, 0.0f, 0.0f, 1.0f);
                }
                break;
            case R.id.id_mothed2_skbar_circle:
                if (optionLayer != null) {
                    float radius = (float) progress / 100f;
                    /**
                     * 画面以圆形裁剪 只显示画面圆形的某一个部分.
                     *
                     * @param radius 圆的半径, 范围0--1.0f
                     * @param center 圆的中心点位置, 范围0--1.0f;, 最上角为0,0,右下角为1,1;, 居中则是new PointF(0.5f,0.5f);
                     */
                    optionLayer.setVisibleCircle(radius, new PointF(0.5f, 0.5f));
                    /**
                     * 设置可见圆形的四周的边框颜色和厚度.
                     *     第一个参数是: 厚度,最大是1.0, 最小是0.0, 推荐是0.01f
                     *     后面4个参数分别是:R,G,B,A 4个颜色分量   RGBA分量中的Red 范围是0.0f---1.0f
                     */
                    optionLayer.setVisibleCircleBorder(0.01f, 1.0f, 0.0f, 0.0f,1.0f);
                }
                break;
            case R.id.id_mothed2_skbar_circle_center:
                if (optionLayer != null) {
                    float xy = (float) progress / 100f;
                    xy /= 2.0f;
                    // 因为是半径, 如果显示画面的一半圆形, 则整个画面是1.0f,则半径0.5;如果显示一半则是0.25;
                    optionLayer.setVisibleCircle(0.25f, new PointF(xy, xy));
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
