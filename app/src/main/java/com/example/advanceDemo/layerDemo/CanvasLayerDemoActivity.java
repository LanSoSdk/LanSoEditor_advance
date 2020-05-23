package com.example.advanceDemo.layerDemo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
import com.example.advanceDemo.utils.DemoUtil;
import com.example.advanceDemo.view.ShowHeart;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.CanvasLayer;
import com.lansosdk.box.CanvasRunnable;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.AudioEditor;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.LanSongFileUtil;

import java.io.IOException;

/**
 * 演示: 使用DrawPad来实现 视频和图片的实时叠加.
 * <p>
 * 流程是: 先创建一个DrawPad,然后在视频播放过程中,从DrawPad中增加一个CanvasLayer,然后可以调节SeekBar来对Layer的每个
 * 参数进行调节.
 */

public class CanvasLayerDemoActivity extends Activity {
    private static final String TAG = "CanvasLayerDemo";
    ShowHeart mShowHeart;
    private String mVideoPath;
    private DrawPadView drawPadView;
    private MediaPlayer mplayer = null;
    private VideoLayer mLayerMain = null;
    private CanvasLayer mCanvasLayer = null;
    private String editTmpPath = null;
    private String dstPath = null;
    private LinearLayout playVideo;
    private MediaInfo mInfo = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.canvaslayer_demo_layout);

        mVideoPath = getIntent().getStringExtra("videopath");

        mInfo = new MediaInfo(mVideoPath);
        if (!mInfo.prepare()) {
            Log.e(TAG, " video path is error.finish\n");
            finish();
        }
        drawPadView = (DrawPadView) findViewById(R.id.id_canvaslayer_drawpadview);

        playVideo = (LinearLayout) findViewById(R.id.id_canvasLayer_saveplay);
        playVideo.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (LanSongFileUtil.fileExist(dstPath)) {
                    DemoUtil.startPreviewVideo(CanvasLayerDemoActivity.this,dstPath);
                } else {
                    Toast.makeText(CanvasLayerDemoActivity.this, "目标文件不存在", Toast.LENGTH_SHORT).show();
                }
            }
        });
        playVideo.setVisibility(View.GONE);

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
                e.printStackTrace();
            }
            mplayer.setOnPreparedListener(new OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    // TODO Auto-generated method stub
                    initDrawPad();
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
            Log.e(TAG, "Null Data Source\n");
            finish();
            return;
        }
    }

    /**
     * Step1: 初始化 drawPad 容器
     */
    private void initDrawPad() {

        int padWidth=640;
        int padHeight=640;

        drawPadView.setUseMainVideoPts(true);

        // 设置使能 实时录制, 即把正在DrawPad中呈现的画面实时的保存下来,实现所见即所得的模式
        drawPadView.setRealEncodeEnable(padWidth, padHeight, (int) mInfo.vFrameRate, editTmpPath);
        // 设置当前DrawPad的宽度和高度,并把宽度自动缩放到父view的宽度,然后等比例调整高度.
        drawPadView.setDrawPadSize(padWidth, padHeight, new onDrawPadSizeChangedListener() {
            @Override
            public void onSizeChanged(int viewWidth, int viewHeight) {
                // TODO Auto-generated method stub
                // 开始DrawPad的渲染线程.
                startDrawPad();
            }
        });

        drawPadView.setOnDrawPadProgressListener(new onDrawPadProgressListener() {
            @Override
            public void onProgress(DrawPad v, long currentTimeUs) {
                // TODO Auto-generated method stub
                if (currentTimeUs > 20 * 1000 * 1000 && mCanvasLayer != null) {
                    drawPadView.removeLayer(mCanvasLayer);
                    mCanvasLayer = null;
                }
            }
        });
    }

    /**
     * Step2: 开始运行容器
     */
    private void startDrawPad() {
        drawPadView.pauseDrawPad();
        if (drawPadView.startDrawPad()) {
            // 增加一个主视频的 VideoLayer
            mLayerMain = drawPadView.addMainVideoLayer(mplayer.getVideoWidth(), mplayer.getVideoHeight(), null);
            if (mLayerMain != null) {
                mplayer.setSurface(new Surface(mLayerMain.getVideoTexture()));
            }
            mplayer.start();
            addCanvasLayer(); // 增加一个CanvasLayer
            drawPadView.resumeDrawPad();
        }
    }

    /**
     * Step3: 停止容器,停止后,为新的视频文件增加上音频部分.
     */
    private void stopDrawPad() {
        if (drawPadView != null && drawPadView.isRunning()) {

            drawPadView.stopDrawPad();
            toastStop();
            if (LanSongFileUtil.fileExist(editTmpPath)) {
                dstPath = AudioEditor.mergeAudioNoCheck(mVideoPath, editTmpPath, true);
                playVideo.setVisibility(View.VISIBLE);
            } else {
                Log.e(TAG, " player completion, but file:" + editTmpPath
                        + " is not exist!!!");
            }
        }
    }

    private void addCanvasLayer() {
        if (drawPadView == null)
            return;

        mCanvasLayer = drawPadView.addCanvasLayer();
        if (mCanvasLayer != null) {
            /**
             * 在绘制一帧的时候, 是否清除上一帧绘制的 内容.
             */
            mCanvasLayer.setClearCanvas(false);
            mShowHeart = new ShowHeart(CanvasLayerDemoActivity.this,mCanvasLayer.getPadWidth(), mCanvasLayer.getPadHeight());
            /**
             * 这里增加两个 CanvasRunnable CanvasRunnable是把当前的一段代码放到 DrawPad线程中运行的一个类.
             * 类似GLSurfaceView的queueEvent
             *addCanvasRunnable里面是一个List数组, 先放入的, 在绘制的时候, 先绘制;
             */
            mCanvasLayer.addCanvasRunnable(new CanvasRunnable() {
                @Override
                public void onDrawCanvas(CanvasLayer layer, Canvas canvas,long currentTimeUs) {
                    Paint paint = new Paint();
                    paint.setColor(Color.RED);
                    paint.setAntiAlias(true);
                    paint.setTextSize(50);
                    canvas.drawText("蓝松短视频演示之<任意绘制>", 20,mCanvasLayer.getPadHeight() - 200, paint);
                }
            });
            /**
             * 增加另一个CanvasRunnable
             */
            mCanvasLayer.addCanvasRunnable(new CanvasRunnable() {

                @Override
                public void onDrawCanvas(CanvasLayer layer, Canvas canvas,long currentTimeUs) {
                    mShowHeart.drawTrack(canvas);
                }
            });
            // 以下是测试:Canvas图层的移动缩放旋转.
            // mCanvasLayer.setScale(0.5f);
            // mCanvasLayer.setRotate(60);
            // mCanvasLayer.setPosition(mCanvasLayer.getPadWidth()-mCanvasLayer.getLayerWidth()/4,mCanvasLayer.getPositionY()/4);
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

        if (drawPadView != null) {
            drawPadView.stopDrawPad();
        }
    }

    private void toastStop() {
        Toast.makeText(getApplicationContext(), "录制已停止!!", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "录制已停止!!");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LanSongFileUtil.deleteFile(dstPath);
        LanSongFileUtil.deleteFile(editTmpPath);
    }
}
