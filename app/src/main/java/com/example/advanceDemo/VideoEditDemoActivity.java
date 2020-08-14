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
import com.lansosdk.videoeditor.OnCompositionSizeReadyListener;

import java.util.ArrayList;
import java.util.List;


public class VideoEditDemoActivity extends Activity implements View.OnClickListener {

    LSOConcatCompositionView composition;

    TextView textView;
    SeekBar seekBar;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.lansongsdk_video_editor_demo_layout2);
        composition = findViewById(R.id.id_video_comp_composition_view);

        initView();

        composition.setCompositionSizeAsync(720, 1280, new OnCompositionSizeReadyListener() {
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
        if (composition != null) {
            composition.pause();
        }
    }

    private void startPreview() throws Exception {

        setCompListeners();

        //异步的形式
        List<LSOAsset> paths = new ArrayList<>();

        String path = CopyFileFromAssets.copyAssets(getApplicationContext(), "dy_xialu2.mp4");
        paths.add(new LSOVideoAsset(path));

        //异步增加视频图层,
        composition.addConcatLayerListAsync(paths, new OnLanSongSDKAddVideoProgressListener() {

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
        if (!composition.isExporting()) {
            composition.startExport();
        }
    }

    /**
     * 设置合成的多个监听;
     */
    private void setCompListeners() {

        composition.setOnLanSongSDKUserSelectedLayerListener(new OnLanSongSDKUserSelectedLayerListener() {
            @Override
            public void onSelected(LSOLayer layer) {
                Log.e("LSDelete", "-----select one : " + System.currentTimeMillis());
            }

            @Override
            public void onCancel() {
                Log.e("LSDelete", "--cancel=====> " + System.currentTimeMillis());
            }
        });


        composition.setOnLanSongSDKCompDurationChangedListener(new OnLanSongSDKCompDurationChangedListener() {
            @Override
            public void onLanSongSDKDurationChanged(long ptsUs) {
                if (textView != null) {
                    textView.setText(convertTime(ptsUs) + " / " + convertTime(composition.getDurationUs()));
                }
                DemoUtil.showToast(VideoEditDemoActivity.this,"duration changed.");
            }
        });

        composition.setOnLanSongSDKTimeChangedListener(new OnLanSongSDKTimeChangedListener() {
            @Override
            public void onLanSongSDKTimeChanged(long ptsUs, int percent) {
                if (textView != null) {
                    textView.setText(convertTime(ptsUs) + " / " + convertTime(composition.getDurationUs()));
                }
            }
        });
        composition.setOnLanSongSDKPlayProgressListener(new OnLanSongSDKPlayProgressListener() {
            @Override
            public void onLanSongSDKPlayProgress(long ptsUs, int percent) {
                if (percent <= 100) {
                    seekBar.setProgress(percent);
                }
                if (textView != null) {
                    textView.setText(convertTime(ptsUs) + " / " + convertTime(composition.getDurationUs()));
                }
            }
        });

        composition.setOnLanSongSDKPlayCompletedListener(new OnLanSongSDKPlayCompletedListener() {
            @Override
            public void onLanSongSDKPlayCompleted() {
                Log.e("LSDelete", "play complete: ");
            }
        });

        composition.setOnLanSongSDKExportProgressListener(new OnLanSongSDKExportProgressListener() {
            @Override
            public void onLanSongSDKExportProgress(long ptsUs, int percent) {
                DemoProgressDialog.showMessage(VideoEditDemoActivity.this, "exporting " + percent);
            }
        });
        composition.setOnLanSongSDKExportCompletedListener(new OnLanSongSDKExportCompletedListener() {
            @Override
            public void onLanSongSDKExportCompleted(String dstVideo) {
                DemoProgressDialog.releaseDialog();
                DemoUtil.startPreviewVideo(VideoEditDemoActivity.this,dstVideo);
            }
        });

        composition.setOnLanSongSDKErrorListener(new OnLanSongSDKErrorListener() {
            @Override
            public void onLanSongSDKError(int errorCode) {
                composition.release();
                DemoUtil.showDialog(VideoEditDemoActivity.this, "Error, TAG is : LanSongSDK.");
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

        if (composition.isRunning()) {
            return;
        }
        String path = CopyFileFromAssets.copyAssets(getApplicationContext(), "hongdou10s.mp3");

        composition.printAllConcatLayerTime();
        //开始执行
        if (composition.isLayoutValid()) {
            composition.startPreview(true);
            DemoLog.d("LSOVideoCompositionView running.");
        } else {
            DemoUtil.showDialog(VideoEditDemoActivity.this, "startPreview error. concatCompView.isLayoutValid():" + composition.isLayoutValid());
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        composition.release();
    }
    private LSOLayer concatBmpLayer =null;
    private LSOLayer overlayLayer=null;

    @Override
    public void onClick(View v) {

        if (composition == null) {
            return;
        }

        switch (v.getId()) {
            case R.id.id_video_comp_pause_resume:
                if (!composition.isPlaying()) {
                    composition.resume();
                } else {
                    composition.pause();
                }
                break;
            case R.id.id_video_comp_concat_add_delete: {

                if(concatBmpLayer ==null){
                    String path=CopyFileFromAssets.copyAssets(getApplicationContext(),"pic720x720.jpg");
                    try {
                        composition.addConcatLayerAsync(new LSOBitmapAsset(path), new OnLanSongSDKAddVideoProgressListener() {
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
                    composition.removeLayerAsync(concatBmpLayer);
                    concatBmpLayer =null;
                }
            }
            break;

            case R.id.id_video_comp_concat_overlay_bitmap:
                    if(overlayLayer==null){
                        Bitmap bmp=BitmapFactory.decodeResource(getResources(),R.drawable.ic_launcher);
                        overlayLayer= composition.addBitmapLayer(bmp);
                        overlayLayer.setLooping(true);
                    }else{
                        composition.removeLayerAsync(overlayLayer);
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
                    long seekUs = (long) (composition.getDurationUs() * percent);
                    composition.seekToTimeUs(seekUs);
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
