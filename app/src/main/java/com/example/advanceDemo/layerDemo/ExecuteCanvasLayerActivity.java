package com.example.advanceDemo.layerDemo;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.example.advanceDemo.utils.DemoUtil;
import com.example.advanceDemo.view.ShowHeart;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.CanvasLayer;
import com.lansosdk.box.CanvasRunnable;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.OnLanSongSDKThreadProgressListener;
import com.lansosdk.videoeditor.CopyFileFromAssets;
import com.lansosdk.videoeditor.DrawPadAutoExecute;
import com.lansosdk.videoeditor.LanSongFileUtil;

public class ExecuteCanvasLayerActivity extends Activity {
    TextView tvProgressHint;
    TextView tvHint;
    DrawPadAutoExecute autoExecute = null;
    private String dstPath = null;
    private String picBackGround = null;
    private boolean isExecuting = false;
    private CanvasLayer canvasLayer = null;
    private ShowHeart showHeart;

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
                        exportAutoExecute();
                    }
                });

        findViewById(R.id.id_video_edit_btn2).setEnabled(false);
        findViewById(R.id.id_video_edit_btn2).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (LanSongFileUtil.fileExist(dstPath)) {
                    DemoUtil.startPlayDstVideo(ExecuteCanvasLayerActivity.this, dstPath);
                } else {
                    Toast.makeText(ExecuteCanvasLayerActivity.this,
                            "目标文件不存在", Toast.LENGTH_SHORT).show();
                }
            }
        });
        picBackGround = CopyFileFromAssets.copyAssets(getApplicationContext(), "pic720x720.jpg");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (autoExecute != null) {
            autoExecute.release();
            autoExecute = null;
        }
        LanSongFileUtil.deleteFile(dstPath);
    }

    private void exportAutoExecute() {
        if (isExecuting)
            return;

        isExecuting = true;
        autoExecute = new DrawPadAutoExecute(getApplicationContext(), 640, 640, 10 * 1000 * 1000, 30);
        autoExecute.setOnLanSongSDKProgressListener(new OnLanSongSDKProgressListener() {
            @Override
            public void onLanSongSDKProgress(long ptsUs, int percent) {
                tvProgressHint.setText("处理进度:" + percent + " %");
            }
        });
        /**
         * 处理完毕后的监听
         */
        autoExecute.setOnLanSongSDKCompletedListener(new OnLanSongSDKCompletedListener() {
            @Override
            public void onLanSongSDKCompleted(String dstVideo) {
                isExecuting = false;
                LanSongFileUtil.deleteFile(dstPath);
                dstPath = dstVideo;
                if (LanSongFileUtil.fileExist(dstPath)) {
                    findViewById(R.id.id_video_edit_btn2).setEnabled(true);
                } else {
                    DemoUtil.showDialog(ExecuteCanvasLayerActivity.this, "转换错误,请查看log信息");
                }
            }
        });
        autoExecute.setOnLanSongSDKThreadProgressListener(new OnLanSongSDKThreadProgressListener() {
            @Override
            public void onLanSongSDKProgress(long ptsUs, int percent) {
                if (ptsUs > 6 * 1000 * 1000) {
                    if (secondLayer != null) {
                        autoExecute.removeLayer(secondLayer);
                        secondLayer = null;
                    }
                } else if (ptsUs >= 3 * 1000 * 1000 && secondLayer == null) {
                    secondLayer = autoExecute.addBitmapLayer(BitmapFactory.decodeResource(getResources(), R.drawable.ls_logo));
                }
            }
        });

        autoExecute.addBitmapLayer(BitmapFactory.decodeFile(picBackGround));// 设置一个背景,
        addCanvasLayer();

        autoExecute.addAudioLayer(CopyFileFromAssets.copyAssets(getApplicationContext(), "summer10s.mp3"), true);

        autoExecute.start();
    }

    BitmapLayer secondLayer = null;

    private void addCanvasLayer() {
        canvasLayer = autoExecute.addCanvasLayer();
        if (canvasLayer != null) {
            canvasLayer.setClearCanvas(false);
            showHeart = new ShowHeart(this, canvasLayer.getPadWidth(), canvasLayer.getPadHeight());
            canvasLayer.addCanvasRunnable(new CanvasRunnable() {

                @Override
                public void onDrawCanvas(CanvasLayer layer, Canvas canvas,
                                         long currentTimeUs) {
                    showHeart.drawTrack(canvas);
                }
            });
        }
    }
}