package com.example.advanceDemo.bitmapAudio;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.example.advanceDemo.VideoPlayerActivity;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.DrawPadUpdateMode;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.example.advanceDemo.utils.CopyFileFromAssets;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.FilterLibrary;
import com.lansosdk.videoeditor.FilterLibrary.FilterAdjuster;
import com.lansosdk.videoeditor.FilterLibrary.OnLanSongFilterChosenListener;
import com.lansosdk.videoeditor.LanSongFileUtil;

import com.lansosdk.LanSongFilter.LanSongFilter;

/**
 * 单单增加一个图片在容器里, 然后把这张图片转换为视频. 您如果想在后台执行, 请使用DrawPadPictureExecute;
 *
 * @author Administrator
 */
public class BitmapLayerFilterDemoActivity extends Activity {
    private static final String TAG = "BitmapLayerFilterDemoActivity";
    boolean isDestorying = false; // 是否正在销毁, 因为销毁会停止DrawPad
    private DrawPadView drawPadView;
    private String dstPath = null;
    private Context mContext = null;
    private BitmapLayer bmpLayer = null;
    private SeekBar AdjusterFilter;
    private FilterAdjuster mFilterAdjuster;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bitmaplayer_filter_layout);
        initView();

        drawPadView = (DrawPadView) findViewById(R.id.DrawPad_view);

        // 在手机的默认路径下创建一个文件名,用来保存生成的视频文件,(在onDestroy中删除)
        dstPath = LanSongFileUtil.newMp4PathInBox();
        mContext = getApplicationContext();

        findViewById(R.id.id_bitmapfilter_demo_selectbtn).setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        selectFilter();
                    }
                });
        AdjusterFilter = (SeekBar) findViewById(R.id.id_bitmapfilter_demo_seek1);
        AdjusterFilter
                .setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar,
                                                  int progress, boolean fromUser) {
                        if (mFilterAdjuster != null) {
                            mFilterAdjuster.adjust(progress);
                        }
                    }
                });
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                initDrawPad();
            }
        }, 200);
    }

    /**
     * Step1: 初始化DrawPad
     */
    private void initDrawPad() {
        // 设置为自动刷新模式, 帧率为25
        drawPadView.setUpdateMode(DrawPadUpdateMode.AUTO_FLUSH, 30);
        // 使能实时录制,并设置录制后视频的宽度和高度, 码率, 帧率,保存路径.
        drawPadView.setRealEncodeEnable(480, 480, (int) 30,dstPath);

        drawPadView
                .setOnDrawPadCompletedListener(new onDrawPadCompletedListener() {

                    @Override
                    public void onCompleted(DrawPad v) {
                        // TODO Auto-generated method stub
                        if (!isDestorying) {
                            if (LanSongFileUtil.fileExist(dstPath)) {
                                findViewById(R.id.id_DrawPad_saveplay)
                                        .setVisibility(View.VISIBLE);
                            }
                            toastStop();
                        }
                    }
                });
        drawPadView
                .setOnDrawPadProgressListener(new onDrawPadProgressListener() {

                    @Override
                    public void onProgress(DrawPad v, long currentTimeUs) {
                        if (currentTimeUs >= 20 * 1000 * 1000) // 26秒.多出一秒,让图片走完.
                        {
                            drawPadView.stopDrawPad();
                        }
                    }
                });
        // 设置DrawPad的宽高, 这里设置为480x480,如果您已经在xml中固定大小,则不需要再次设置,
        // 可以直接调用startDrawPad来开始录制.
        drawPadView.setDrawPadSize(480, 480,
                new onDrawPadSizeChangedListener() {

                    @Override
                    public void onSizeChanged(int viewWidth, int viewHeight) {
                        startDrawPad();
                    }
                });

    }

    /**
     * Step2: 开始运行 Drawpad线程. (停止是在进度监听中, 根据时间来停止的.)
     */
    private void startDrawPad() {
        drawPadView.pauseDrawPad();
        if (drawPadView.startDrawPad()) {
            String picPath = CopyFileFromAssets.copyAssets(mContext,
                    "pic720x720.jpg");
            bmpLayer = drawPadView.addBitmapLayer(BitmapFactory
                    .decodeFile(picPath));
        }
        drawPadView.resumeDrawPad();
    }

    /**
     * 选择滤镜效果,
     */
    private void selectFilter() {
        if (drawPadView != null && drawPadView.isRunning()) {
            FilterLibrary.showDialog(this,
                    new OnLanSongFilterChosenListener() {

                        @Override
                        public void onLanSongFilterChosenListener(
                                final LanSongFilter filter, String name) {

                            if (bmpLayer != null) {
                                bmpLayer.switchFilterTo(filter);
                                mFilterAdjuster = new FilterAdjuster(filter);

                                // 如果这个滤镜 可调, 显示可调节进度条.
                                findViewById(R.id.id_bitmapfilter_demo_seek1)
                                        .setVisibility(
                                                mFilterAdjuster.canAdjust() ? View.VISIBLE
                                                        : View.GONE);
                            }
                        }
                    });
        }
    }

    private void initView() {
        findViewById(R.id.id_DrawPad_saveplay).setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (LanSongFileUtil.fileExist(dstPath)) {
                            Intent intent = new Intent(mContext,
                                    VideoPlayerActivity.class);
                            intent.putExtra("videopath", dstPath);
                            startActivity(intent);
                        } else {
                            Toast.makeText(mContext, "目标文件不存在",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        findViewById(R.id.id_DrawPad_saveplay).setVisibility(View.GONE);
    }

    private void toastStop() {
        Toast.makeText(getApplicationContext(), "录制已停止!!", Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestorying = true;
        if (drawPadView != null) {
            drawPadView.stopDrawPad();
            drawPadView = null;
        }
        LanSongFileUtil.deleteFile(dstPath);
    }
}
