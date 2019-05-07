package com.example.advanceDemo.scene;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.example.advanceDemo.utils.DemoUtil;
import com.example.advanceDemo.VideoPlayerActivity;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.Layer;
import com.lansosdk.box.MoveAnimation;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.videoeditor.CopyFileFromAssets;
import com.lansosdk.videoeditor.DrawPadPictureExecute;
import com.lansosdk.videoeditor.LanSongFileUtil;
import com.lansosdk.videoeditor.VideoEditor;

/**
 * 后台执行 照片影集的功能. 使用DrawPad的扩展类:DrawPadPictureExecute来操作.
 */
public class ExecuteBitmapLayerActivity extends Activity {

    private static final String TAG = "ExecuteBitmapLayerActivity";
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
    DrawPadPictureExecute mDrawPad = null;
    private String dstPath = null;
    private String picBackGround = null;
    private int padWidth, padHeight;
    /**
     * 当前是否已经在执行, 以免造成多次执行.
     */
    private boolean isExecuting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

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
                                    ExecuteBitmapLayerActivity.this,
                                    VideoPlayerActivity.class);
                            intent.putExtra("videopath", dstPath);
                            startActivity(intent);
                        } else {
                            Toast.makeText(ExecuteBitmapLayerActivity.this,
                                    "目标文件不存在", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        //这里增加一个图层, 即作为最底部的一张图片.
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
        LanSongFileUtil.deleteFile(dstPath);
    }

    private void testDrawPadExecute() {
        if (isExecuting)
            return;

        padWidth = 480;
        padHeight = 480;
        isExecuting = true;

        mDrawPad = new DrawPadPictureExecute(getApplicationContext(), 480, 480,
                26 * 1000, 25, 1000000, dstPath);

        /**
         * 设置DrawPad的处理进度监听, 您可以在每一帧的过程中对ILayer做各种变化,
         * 比如平移,缩放,旋转,颜色变化,增删一个Layer等,来实现各种动画画面.
         */
        mDrawPad.setDrawPadProgressListener(new onDrawPadProgressListener() {

            // currentTimeUs是当前时间戳,单位是微妙,可以根据时间戳/(MediaInfo.vDuration*1000000)来得到当前进度百分比.
            @Override
            public void onProgress(DrawPad v, long currentTimeUs) {
                tvProgressHint.setText(String.valueOf(currentTimeUs));
            }
        });
        /**
         * 处理完毕后的监听
         */
        mDrawPad.setDrawPadCompletedListener(new onDrawPadCompletedListener() {

            @Override
            public void onCompleted(DrawPad v) {
                tvProgressHint.setText("DrawPadExecute Completed!!!");
                isExecuting = false;
                if (LanSongFileUtil.fileExist(dstPath)) {
                    findViewById(R.id.id_video_edit_btn2).setEnabled(true);
                }
            }
        });
        /**
         * 开始前先设置暂停标记.暂停画面的走动.比如想一次性增加多个Layer对象后, 在让DrawPad执行,这样比在画面走动中获取更精确一些.
         */
        mDrawPad.pauseRecord();
        if (mDrawPad.startDrawPad()) {
            mDrawPad.addBitmapLayer(BitmapFactory.decodeFile(picBackGround),
                    null);

            // 这里同时增加多个,只是不显示出来.
            addLayerToArray(R.drawable.pic1, 0, 5000); // 1--5秒.
            addLayerToArray(R.drawable.pic2, 5000, 10000); // 5--10秒.
            addLayerToArray(R.drawable.pic3, 10000, 15000); // 10---15秒
            addLayerToArray(R.drawable.pic4, 15000, 20000); // 15---20秒
            addLayerToArray(R.drawable.pic5, 20000, 25000); // 20---25秒
        } else {
            DemoUtil.showToast(getApplicationContext(), "drawpad容器执行失败,请查看打印信息");
        }
        mDrawPad.resumeRecord();
    }

    private void addLayerToArray(int resId, long startMS, long endMS) {
        BitmapLayer item = mDrawPad.addBitmapLayer(
                BitmapFactory.decodeResource(getResources(), resId), null);

        item.setVisibility(Layer.INVISIBLE);

        // 开始1/3, 中间停1/3,最后划走1/3;
        long total = endMS - startMS;
        long start2 = endMS - total / 3;

        // 第一段运行
        MoveAnimation move1 = new MoveAnimation(startMS * 1000,
                total * 1000 / 3, /* 开始时间, 持续时长 */
                0, item.getPadHeight() / 2, /* 开始位置, 结束位置 */
                item.getPadWidth() / 2, item.getPadHeight() / 2);

        // 中间停止在那里.

        // 最后一段运行.
        MoveAnimation move2 = new MoveAnimation(start2 * 1000,
                total * 1000 / 3, item.getPadWidth() / 2,
                item.getPadHeight() / 2,

                item.getPadWidth() + item.getPadWidth() / 2,
                item.getPadHeight() / 2);

        item.addAnimation(move1);
        item.addAnimation(move2);
    }
}