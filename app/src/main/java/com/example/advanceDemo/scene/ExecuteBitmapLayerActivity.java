package com.example.advanceDemo.scene;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.example.advanceDemo.utils.DemoUtil;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.Layer;
import com.lansosdk.box.MoveAnimation;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.example.advanceDemo.utils.CopyFileFromAssets;
import com.lansosdk.videoeditor.DrawPadAutoExecute;
import com.lansosdk.videoeditor.LanSongFileUtil;

/**
 * 后台执行 照片影集的功能. 使用DrawPad的扩展类:DrawPadPictureExecute来操作.
 * 不建议使用.
 */
public class ExecuteBitmapLayerActivity extends Activity {

    private static final String TAG = "ExecuteBitmapLayerActivity";
    TextView tvProgressHint;
    TextView tvHint;
    DrawPadAutoExecute drawPadExecute = null;
    private String picBackGround = null;
    /**
     * 当前是否已经在执行, 以免造成多次执行.
     */
    private boolean executing = false;

    private String dstPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.execute_edit_demo_layout);
        tvHint = (TextView) findViewById(R.id.id_video_editor_hint);

        tvHint.setText(R.string.pictureset_execute_demo_hint);

        tvProgressHint = (TextView) findViewById(R.id.id_video_edit_progress_hint);

        findViewById(R.id.id_video_edit_btn).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                startDrawPad();
            }
        });

        findViewById(R.id.id_video_edit_btn2).setEnabled(false);
        findViewById(R.id.id_video_edit_btn2).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (LanSongFileUtil.fileExist(dstPath)) {
                    DemoUtil.startPlayDstVideo(ExecuteBitmapLayerActivity.this, dstPath);
                } else {
                    Toast.makeText(ExecuteBitmapLayerActivity.this, "目标文件不存在", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //这里增加一个图层, 即作为最底部的一张图片.
        picBackGround = CopyFileFromAssets.copyAssets(getApplicationContext(), "pic720x720.jpg");
        dstPath = LanSongFileUtil.newMp4PathInBox();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (drawPadExecute != null) {
            drawPadExecute.release();
            drawPadExecute = null;
        }
        LanSongFileUtil.deleteFile(dstPath);
    }

    private void startDrawPad() {
        if (executing)
            return;

        executing = true;
        drawPadExecute = new DrawPadAutoExecute(getApplicationContext(), 640, 640, 26 * 1000 * 1000, 25);


        drawPadExecute.setOnLanSongSDKProgressListener(new OnLanSongSDKProgressListener() {
            @Override
            public void onLanSongSDKProgress(long ptsUs, int percent) {
                tvProgressHint.setText("当前进度是:" + percent + " %");
            }
        });
        /**
         * 处理完毕后的监听
         */
        drawPadExecute.setOnLanSongSDKCompletedListener(new OnLanSongSDKCompletedListener() {
            @Override
            public void onLanSongSDKCompleted(String dstVideo) {
                LanSongFileUtil.fileExist(dstPath);
                executing=false;
                dstPath = dstVideo;
                if (LanSongFileUtil.fileExist(dstPath)) {
                    findViewById(R.id.id_video_edit_btn2).setEnabled(true);
                } else {
                    DemoUtil.showToast(getApplicationContext(), "合成失败,请查看log信息.");
                }
            }
        });
        drawPadExecute.setOnLanSongSDKErrorListener(new OnLanSongSDKErrorListener() {
            @Override
            public void onLanSongSDKError(int errorCode) {
                executing=false;
                DemoUtil.showDialog(ExecuteBitmapLayerActivity.this,"处理失败,请查看log信息");
            }
        });

        drawPadExecute.addBitmapLayer(BitmapFactory.decodeFile(picBackGround));

        addLayerToArray(R.drawable.pic1, 0, 5000); // 1--5秒.
        addLayerToArray(R.drawable.pic2, 5000, 10000); // 5--10秒.
        addLayerToArray(R.drawable.pic3, 10000, 15000); // 10---15秒
        addLayerToArray(R.drawable.pic4, 15000, 20000); // 15---20秒
        addLayerToArray(R.drawable.pic5, 20000, 25000); // 20---25秒


        drawPadExecute.start();
    }

    private void addLayerToArray(int resId, long startMS, long endMS) {
        BitmapLayer item = drawPadExecute.addBitmapLayer(BitmapFactory.decodeResource(getResources(), resId));

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

        item.addAnimationOLD(move1);
        item.addAnimationOLD(move2);
    }
}