package com.example.advanceDemo.layerDemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.example.advanceDemo.VideoPlayerActivity;
import com.example.advanceDemo.view.ShowHeart;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.CanvasLayer;
import com.lansosdk.box.CanvasRunnable;
import com.lansosdk.box.DataLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.GifLayer;
import com.lansosdk.box.MVLayer;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadErrorListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.box.onDrawPadThreadProgressListener;
import com.example.advanceDemo.utils.CopyFileFromAssets;
import com.lansosdk.videoeditor.DrawPadVideoExecute;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.LanSongFileUtil;

import java.nio.IntBuffer;

/**
 * 演示: 使用DrawPad在后台执行视频和视频的叠加处理.
 * <p>
 * 适用在 一些UI界面需要用户手动操作UI界面,比如旋转叠加的视频等,增加图片后旋转图片等,这些UI交互完成后,
 * 记录下用户的操作信息,但需要统一处理时,通过此类来在后台执行.
 * <p>
 * 流程:通过DrawPadVideoExecute来实现视频的编辑处理,
 * 效果:建立一个DrawPad后,增加VideoLayer让其播放,在播放过程中,向里面增加两个图片和一个UI,
 * 其中给一个图片移动位置,并在3秒处放大一倍,在6秒处消失,处理中实时的形成视频等
 */
public class ExecuteVideoLayerActivity extends Activity {

    private static final String TAG = "ExecuteVideoLayer";
    GifLayer gifLayer;
    private String videoPath = null;
    private ProgressDialog mProgressDialog;
    private int videoDuration;
    private boolean isRuned = false;
    private MediaInfo mInfo;
    private TextView tvProgressHint;
    private TextView tvHint;
    private String dstPath = null;
    /**
     * 图片图层
     */
    private BitmapLayer bitmapLayer = null;
    /**
     * Canvas 图层.
     */
    private CanvasLayer mCanvasLayer = null;
    /**
     * drawPad, 用来执行图像处理的对象.
     */
    private DrawPadVideoExecute execute2 = null;
    /**
     * 用来显示一个心形.
     */
    private ShowHeart mShowHeart;
    private boolean isExecuting = false;
    private Context mContext = null;
    private MVLayer mvlayer;
    private MediaInfo gifInfo;
    private long decoderHandler;
    private IntBuffer mGLRgbBuffer;
    private int gifInterval = 0;
    private int frameCount = 0;
    private DataLayer dataLayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mContext = getApplicationContext();
        videoPath = getIntent().getStringExtra("videopath");

        mInfo = new MediaInfo(videoPath);
        mInfo.prepare();

        setContentView(R.layout.execute_edit_demo_layout);

        initUI();

        // 在手机的默认路径下创建一个文件名,用来保存生成的视频文件,(在onDestroy中删除)
        dstPath = LanSongFileUtil.newMp4PathInBox();
    }

    /**
     * 从这里开始演示.
     */
    private void startDrawPad() {
        if (isExecuting)
            return;

        isExecuting = true;
        execute2 = new DrawPadVideoExecute(mContext, videoPath,dstPath);

        execute2.setDrawPadErrorListener(new onDrawPadErrorListener() {

            @Override
            public void onError(DrawPad d, int what) {
                execute2.stopDrawPad();
                Log.e(TAG, "后台容器线程 运行失败,您请检查下是否码率分辨率设置过大,或者联系我们!...");
            }
        });
        execute2.setDrawPadProgressListener(new onDrawPadProgressListener() {

            @Override
            public void onProgress(DrawPad v, long currentTimeUs) {
                tvProgressHint.setText(String.valueOf(currentTimeUs));
                // 6秒后消失
                if (currentTimeUs > 6000000 && bitmapLayer != null)
                    v.removeLayer(bitmapLayer);
                else if (currentTimeUs > 3000000 && bitmapLayer != null) // 3秒的时候,放大一倍.
                    bitmapLayer.setScale(2.0f);
            }
        });
        execute2.setDrawPadCompletedListener(new onDrawPadCompletedListener() {

            @Override
            public void onCompleted(DrawPad v) {
                tvProgressHint.setText("DrawPadExecute Completed!!!");
                isExecuting = false;
                findViewById(R.id.id_video_edit_btn2).setEnabled(true);
            }
        });

        execute2.setDrawPadThreadProgressListener(new onDrawPadThreadProgressListener() {

            @Override
            public void onThreadProgress(DrawPad v, long currentTimeUs) {
            }
        });
        addOtherAudio();

        // 在开启前,先设置为暂停录制,因为要增加一些图层.
        execute2.pauseRecord();
        //开始执行这个DrawPad
        if (execute2.startDrawPad()) {
            // 增加一些图层.
            addLayers();
        } else {
            Log.e(TAG,"后台容器线程  运行失败,您请检查下是否是路径设置有无, 请用MediaInfo.checkFile执行查看下....");
        }
    }

    /**
     * 增加各种图层.
     */
    private void addLayers() {
        if (execute2.isRunning()) {


            /**
             * 一下是在处理过程中, 增加的几个Layer, 来实现视频在播放过程中叠加别的一些媒体, 像图片, 文字等.
             */
            bitmapLayer = execute2.addBitmapLayer(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
            bitmapLayer.setPosition(300, 200);

            // 增加一个笑脸, add a byteBuffer
            execute2.addBitmapLayer(BitmapFactory.decodeResource(getResources(), R.drawable.xiaolian));

            // 你可以增加其他图层.
            // addCanvasLayer();
            // addDataLayer();
            addMVLayer();
            // addGifLayer();

            // 增加完图层, 恢复运行.
            execute2.resumeRecord();
        }
    }

    /**
     * 可以插入一段声音. 注意, 需要在drawpad开始前调用. 注意,当增加成功后, 声音会在DrawPad内部合成,
     */
    private void addOtherAudio() {
        String audio = CopyFileFromAssets.copyAssets(getApplicationContext(), "chongjibo_a_music.mp3");
        String audio2 = CopyFileFromAssets.copyAssets(getApplicationContext(), "hongdou10s.mp3");

       execute2.addAudioLayer(audio,3*1000*1000);
       execute2.addAudioLayer(audio2);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (execute2 != null) {
            execute2.releaseDrawPad();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            execute2 = null;
        }
        LanSongFileUtil.deleteFile(dstPath);
    }

    private void initUI() {
        tvHint = (TextView) findViewById(R.id.id_video_editor_hint);

        tvHint.setText(R.string.drawpadexecute_demo_hint);
        tvProgressHint = (TextView) findViewById(R.id.id_video_edit_progress_hint);

        findViewById(R.id.id_video_edit_btn).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (mInfo.vDuration >= 60 * 1000) {// 大于60秒
                            showHintDialog();
                        } else {
                            startDrawPad();
                        }
                    }
                });
        findViewById(R.id.id_video_edit_btn2).setEnabled(false);
        findViewById(R.id.id_video_edit_btn2).setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (LanSongFileUtil.fileExist(dstPath)) {
                            Intent intent = new Intent(mContext,
                                    VideoPlayerActivity.class);
                            intent.putExtra("videopath", dstPath);
                            startActivity(intent);
                        } else {
                            Toast.makeText(ExecuteVideoLayerActivity.this,
                                    "目标文件不存在", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }

    private void showHintDialog() {
        new AlertDialog.Builder(this).setTitle("提示")
                .setMessage("视频过大,可能会需要一段时间,您确定要处理吗?")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startDrawPad();
                    }
                }).setNegativeButton("取消", null).show();
    }

    /**
     * 增加一个CanvasLayer,
     */
    private void addCanvasLayer() {
        mCanvasLayer = execute2.addCanvasLayer();
        if (mCanvasLayer != null) {

            mCanvasLayer.setClearCanvas(false);
            mShowHeart = new ShowHeart(ExecuteVideoLayerActivity.this,
                    mCanvasLayer.getPadWidth(), mCanvasLayer.getPadHeight());
            mCanvasLayer.addCanvasRunnable(new CanvasRunnable() {

                @Override
                public void onDrawCanvas(CanvasLayer pen, Canvas canvas,
                                         long currentTimeUs) {
                    mShowHeart.drawTrack(canvas);
                }
            });
        }
    }

    private void addMVLayer() {
        String colorMVPath = CopyFileFromAssets.copyAssets(ExecuteVideoLayerActivity.this, "mei.mp4");
        String maskMVPath = CopyFileFromAssets.copyAssets(ExecuteVideoLayerActivity.this, "mei_b.mp4");
        mvlayer = execute2.addMVLayer(colorMVPath, maskMVPath, true);
    }

    private void addGifLayer() {
        gifLayer = execute2.addGifLayer(R.drawable.g06);
    }

    /**
     * 用来计算, 在视频走动过程中, 几秒钟插入一个gif图片
     *
     * @return
     */
    private boolean canDrawNext() {
        if (frameCount % gifInterval == 0) { // 能被整除则说明间隔到了.
            frameCount++;
            return true;
        } else {
            frameCount++;
            return false;
        }
    }
}