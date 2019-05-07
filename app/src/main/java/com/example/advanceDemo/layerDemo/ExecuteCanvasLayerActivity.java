package com.example.advanceDemo.layerDemo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
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
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.DrawPadBitmapRunnable;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadOutFrameListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.videoeditor.CopyFileFromAssets;
import com.lansosdk.videoeditor.LanSongFileUtil;
import com.lansosdk.videoeditor.VideoEditor;

/**
 */
public class ExecuteCanvasLayerActivity extends Activity {
    private static final String TAG = "ExecuteCanvasLayerActivity";

    int videoDuration;
    boolean isRuned = false;
    TextView tvProgressHint;
    TextView tvHint;
    VideoEditor mVideoEditer;
    /**
     * 图片类的Layer
     */
    BitmapLayer bitmapLayer = null;
    /**
     * 使用DrawPad中的Picture执行类来做.
     */
    DrawPadBitmapRunnable mDrawPad = null;
    private String dstPath = null;
    private String picBackGround = null;
    /**
     * 当前是否已经在执行, 以免造成多次执行.
     */
    private boolean isExecuting = false;
    /**
     * 增加一个CanvasLayer,
     */
    private CanvasLayer mCanvasLayer = null;
    /**
     * 用来显示一个心形.
     */
    private ShowHeart mShowHeart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub

        super.onCreate(savedInstanceState);

        setContentView(R.layout.execute_edit_demo_layout);
        tvHint = (TextView) findViewById(R.id.id_video_editor_hint);

        tvHint.setText(R.string.pictureset_execute_demo_hint);

        tvProgressHint = (TextView) findViewById(R.id.id_video_edit_progress_hint);

        findViewById(R.id.id_video_edit_btn).setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        testDrawPadExecute();
                    }
                });

        findViewById(R.id.id_video_edit_btn2).setEnabled(false);
        findViewById(R.id.id_video_edit_btn2).setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (LanSongFileUtil.fileExist(dstPath)) {
                            Intent intent = new Intent(
                                    ExecuteCanvasLayerActivity.this,
                                    VideoPlayerActivity.class);
                            intent.putExtra("videopath", dstPath);
                            startActivity(intent);
                        } else {
                            Toast.makeText(ExecuteCanvasLayerActivity.this,
                                    "目标文件不存在", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        picBackGround = CopyFileFromAssets.copyAssets(getApplicationContext(), "pic720x720.jpg");

        // 在手机的默认路径下创建一个文件名,用来保存生成的视频文件,(在onDestroy中删除)
        dstPath = LanSongFileUtil.newMp4PathInBox();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mDrawPad != null) {
            mDrawPad.release();
            mDrawPad = null;
        }
        if (LanSongFileUtil.fileExist(dstPath)) {
            LanSongFileUtil.deleteFile(dstPath);
        }
    }

    private void testDrawPadExecute() {
        if (isExecuting)
            return;

        isExecuting = true;

        /**
         * 设置宽高,时长, 帧率,码率, 目标文件路径 这里设置的的时长是5秒钟.
         */
        mDrawPad = new DrawPadBitmapRunnable(getApplicationContext(), 480, 480,
                5 * 1000, 25, 1000 * 1024, dstPath);

        mDrawPad.setDrawPadProgressListener(new onDrawPadProgressListener() {

            // currentTimeUs是当前时间戳,单位是微妙,可以根据时间戳/(MediaInfo.vDuration*1000000)来得到当前进度百分比.
            @Override
            public void onProgress(DrawPad v, long currentTimeUs) {
                // TODO Auto-generated method stub
                tvProgressHint.setText(String.valueOf(currentTimeUs));
            }
        });
        /**
         * 处理完毕后的监听
         */
        mDrawPad.setDrawPadCompletedListener(new onDrawPadCompletedListener() {

            @Override
            public void onCompleted(DrawPad v) {
                // TODO Auto-generated method stub
                tvProgressHint.setText("DrawPadExecute Completed!!!");

                isExecuting = false;
                if (LanSongFileUtil.fileExist(dstPath)) {
                    findViewById(R.id.id_video_edit_btn2).setEnabled(true);
                }
            }
        });
        mDrawPad.setDrawpadOutFrameListener(480, 480, 1,
                new onDrawPadOutFrameListener() {

                    @Override
                    public void onDrawPadOutFrame(DrawPad v, Object obj,
                                                  int type, long ptsUs) {
                        // TODO Auto-generated method stub

                    }
                });

        startDrawPad();
    }

    private void startDrawPad() {
        mDrawPad.pauseRecordDrawPad();
        /**
         * 开始处理.
         */
        if (mDrawPad.startDrawPad()) {
            mDrawPad.addBitmapLayer(BitmapFactory.decodeFile(picBackGround),
                    null);// 设置一个背景,
            addCanvasLayer();
            // 增加完Layer后,再次恢复DrawPad,让其工作.
            mDrawPad.resumeRecordDrawPad();
        }
    }

    private void addCanvasLayer() {
        mCanvasLayer = mDrawPad.addCanvasLayer();
        if (mCanvasLayer != null) {

            mCanvasLayer.setClearCanvas(false);
            mShowHeart = new ShowHeart(this, mCanvasLayer.getPadWidth(),
                    mCanvasLayer.getPadHeight());
            mCanvasLayer.addCanvasRunnable(new CanvasRunnable() {

                @Override
                public void onDrawCanvas(CanvasLayer layer, Canvas canvas,
                                         long currentTimeUs) {
                    // TODO Auto-generated method stub
                    mShowHeart.drawTrack(canvas);
                }
            });
        }
    }
}