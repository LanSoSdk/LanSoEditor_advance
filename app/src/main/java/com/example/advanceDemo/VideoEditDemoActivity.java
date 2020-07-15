package com.example.advanceDemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.advanceDemo.utils.CopyFileFromAssets;
import com.example.advanceDemo.utils.DemoLog;
import com.example.advanceDemo.utils.DemoProgressDialog;
import com.example.advanceDemo.utils.DemoUtil;
import com.lansosdk.box.LSOBitmapAsset;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.LSOAsset;
import com.lansosdk.box.LSOLayer;
import com.lansosdk.box.LSOVideoAsset;
import com.lansosdk.box.OnLanSongSDKAddVideoProgressListener;
import com.lansosdk.box.OnLanSongSDKCompDurationChangedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKExportCompletedListener;
import com.lansosdk.box.OnLanSongSDKExportProgressListener;
import com.lansosdk.box.OnLanSongSDKPlayCompletedListener;
import com.lansosdk.box.OnLanSongSDKPlayProgressListener;
import com.lansosdk.box.OnLanSongSDKTimeChangedListener;
import com.lansosdk.box.OnLanSongSDKUserSelectedLayerListener;
import com.lansosdk.videoeditor.LSOConcatCompositionView;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.OnCompositionSizeReadyListener;

import java.util.ArrayList;
import java.util.List;


public class VideoEditDemoActivity extends Activity implements View.OnClickListener {

    LSOConcatCompositionView concatCompView;

    TextView textView;
    SeekBar seekBar;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.lansongsdk_video_editor_demo_layout2);
        concatCompView = findViewById(R.id.id_video_comp_composition_view);

        initView();

        concatCompView.setCompositionSizeAsync(720, 1280, new OnCompositionSizeReadyListener() {
            @Override
            public void onSizeReady() {
                try {
                    startPreview();
                } catch (Exception e) {
                    e.printStackTrace();
                    DemoUtil.showDialog(VideoEditDemoActivity.this, "加载视频错误.");
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (concatCompView != null) {
            concatCompView.pause();
        }
    }

    private void startPreview() throws Exception {

        setCompListeners();

        //异步的形式
        List<LSOAsset> paths = new ArrayList<>();

        String path = CopyFileFromAssets.copyAssets(getApplicationContext(), "dy_xialu2.mp4");
        paths.add(new LSOVideoAsset(path));

        //异步增加视频图层,
        concatCompView.addConcatLayerListAsync(paths, new OnLanSongSDKAddVideoProgressListener() {

            @Override
            public void onAddVideoProgress(int percent, int numberIndex, int totalNumber) {
                DemoProgressDialog.showMessage(VideoEditDemoActivity.this, " percent:" + percent + " index:" + numberIndex + "/" + totalNumber);
            }

            @Override
            public void onAddVideoCompleted(List layers, boolean success) {
                DemoProgressDialog.releaseDialog();
                try {
                    startComposition();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void startExport() {
        if (!concatCompView.isExporting()) {
            concatCompView.startExport();
        }
    }

    /**
     * 设置合成的多个监听;
     */
    private void setCompListeners() {

        concatCompView.setOnLanSongSDKUserSelectedLayerListener(new OnLanSongSDKUserSelectedLayerListener() {
            @Override
            public void onSelected(LSOLayer layer) {
                Log.e("LSDelete", "------- 选中了一个图层: " + System.currentTimeMillis());
            }

            @Override
            public void onCancel() {
                Log.e("LSDelete", "---取消了一个图层=======> " + System.currentTimeMillis());
            }
        });


        concatCompView.setOnLanSongSDKCompDurationChangedListener(new OnLanSongSDKCompDurationChangedListener() {
            @Override
            public void onLanSongSDKDurationChanged(long ptsUs) {
                DemoUtil.showDialog(VideoEditDemoActivity.this, "容器总时长改变了,请刷新缩略图");
            }
        });

        concatCompView.setOnLanSongSDKTimeChangedListener(new OnLanSongSDKTimeChangedListener() {
            @Override
            public void onLanSongSDKTimeChanged(long ptsUs, int percent) {
                if (textView != null) {
                    textView.setText(convertTime(ptsUs) + " / " + convertTime(concatCompView.getDurationUs()));
                }
            }
        });
        concatCompView.setOnLanSongSDKPlayProgressListener(new OnLanSongSDKPlayProgressListener() {
            @Override
            public void onLanSongSDKPlayProgress(long ptsUs, int percent) {
                if (percent <= 100) {
                    seekBar.setProgress(percent);
                }
                if (textView != null) {
                    textView.setText(convertTime(ptsUs) + " / " + convertTime(concatCompView.getDurationUs()));
                }
            }
        });

        concatCompView.setOnLanSongSDKPlayCompletedListener(new OnLanSongSDKPlayCompletedListener() {
            @Override
            public void onLanSongSDKPlayCompleted() {
                Log.e("LSDelete", "--播放完毕.: ");
            }
        });

        concatCompView.setOnLanSongSDKExportProgressListener(new OnLanSongSDKExportProgressListener() {
            @Override
            public void onLanSongSDKExportProgress(long ptsUs, int percent) {
                DemoProgressDialog.showMessage(VideoEditDemoActivity.this, " 正在导出:" + percent);
            }
        });
        concatCompView.setOnLanSongSDKExportCompletedListener(new OnLanSongSDKExportCompletedListener() {
            @Override
            public void onLanSongSDKExportCompleted(String dstVideo) {
                DemoProgressDialog.releaseDialog();
                MediaInfo.checkFile(dstVideo);
            }
        });

        concatCompView.setOnLanSongSDKErrorListener(new OnLanSongSDKErrorListener() {
            @Override
            public void onLanSongSDKError(int errorCode) {
                concatCompView.release();
                DemoUtil.showDialog(VideoEditDemoActivity.this, "AE执行错误,请查看错误信息.我们的TAG是LanSongSDK.");
            }
        });
    }

    private float convertTime(long time) {
        int time3 = (int) (time * 100 / 1000000);
        return (float) time3 / 100f;
    }

    /**
     * 开始预览播放
     */
    private void startComposition() {

        if (concatCompView.isRunning()) {
            return;
        }
        String path = CopyFileFromAssets.copyAssets(getApplicationContext(), "hongdou10s.mp3");

        concatCompView.printAllConcatLayerTime();
        //开始执行
        if (concatCompView.isLayoutValid()) {
            concatCompView.startPreview(true);
            DemoLog.d("LSOVideoCompositionView running.");
        } else {
            DemoUtil.showDialog(VideoEditDemoActivity.this, "AE预览开启失败. concatCompView.isLayoutValid():" + concatCompView.isLayoutValid());
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        concatCompView.release();
    }
    private LSOLayer concatBmpLayer =null;
    private LSOLayer overlayLayer=null;

    @Override
    public void onClick(View v) {

        if (concatCompView == null) {
            return;
        }

        switch (v.getId()) {
            case R.id.id_video_comp_pause_resume:
                if (!concatCompView.isPlaying()) {
                    concatCompView.resume();
                } else {
                    concatCompView.pause();
                }
                break;
            case R.id.id_video_comp_concat_add_delete: {

                if(concatBmpLayer ==null){
                    String path=CopyFileFromAssets.copyAssets(getApplicationContext(),"pic720x720.jpg");
                    try {
                        concatCompView.addConcatLayerAsync(new LSOBitmapAsset(path), new OnLanSongSDKAddVideoProgressListener() {
                            @Override
                            public void onAddVideoProgress(int i, int i1, int i2) {

                            }

                            @Override
                            public void onAddVideoCompleted(List list, boolean b) {
                                if(list!=null && list.size()>0){
                                    concatBmpLayer =(LSOLayer)list.get(0);
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else{
                    concatCompView.removeLayerAsync(concatBmpLayer);
                    concatBmpLayer =null;
                }
            }
            break;

            case R.id.id_video_comp_concat_overlay_bitmap:
                    if(overlayLayer==null){
                        Bitmap bmp=BitmapFactory.decodeResource(getResources(),R.drawable.ic_launcher);
                        overlayLayer=concatCompView.addBitmapLayer(bmp);
                        overlayLayer.setLooping(true);
                    }else{
                        concatCompView.removeLayerAsync(overlayLayer);
                        overlayLayer=null;
                    }
                break;
            case R.id.id_video_comp_export:
                startExport();
                break;
            default:
                break;
        }
    }


    private void initView() {
        findViewById(R.id.id_video_comp_export).setOnClickListener(this);

        findViewById(R.id.id_video_comp_concat_overlay_bitmap).setOnClickListener(this);
        findViewById(R.id.id_video_comp_concat_add_delete).setOnClickListener(this);
        findViewById(R.id.id_video_comp_pause_resume).setOnClickListener(this);

        textView = findViewById(R.id.id_video_comp_composition_hint);
        seekBar = findViewById(R.id.id_video_comp_composition_seek_bar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {

                    float percent = (progress * 1.0f / 100f);
                    long seekUs = (long) (concatCompView.getDurationUs() * percent);
                    concatCompView.seekToTimeUs(seekUs);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }


}
