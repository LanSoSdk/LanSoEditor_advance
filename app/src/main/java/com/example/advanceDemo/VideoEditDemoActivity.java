package com.example.advanceDemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.advanceDemo.utils.CopyFileFromAssets;
import com.example.advanceDemo.utils.DemoLog;
import com.example.advanceDemo.utils.DemoProgressDialog;
import com.example.advanceDemo.utils.DemoUtil;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.LSOAsset;
import com.lansosdk.box.LSOLayer;
import com.lansosdk.box.OnAddAssetProgressListener;
import com.lansosdk.box.OnCreateListener;
import com.lansosdk.box.OnLanSongSDKDurationChangedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKExportCompletedListener;
import com.lansosdk.box.OnLanSongSDKExportProgressListener;
import com.lansosdk.box.OnLanSongSDKPlayCompletedListener;
import com.lansosdk.box.OnLanSongSDKPlayProgressListener;
import com.lansosdk.box.OnLanSongSDKTimeChangedListener;
import com.lansosdk.box.OnLanSongSDKUserSelectedLayerListener;
import com.lansosdk.box.OnResumeListener;
import com.lansosdk.videoeditor.LSOEditPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class VideoEditDemoActivity extends Activity implements View.OnClickListener {

    LSOEditPlayer editPlayer;

    TextView textView;
    SeekBar seekBar;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.lansongsdk_video_editor_demo_layout2);
        editPlayer = findViewById(R.id.id_video_comp_composition_view);

        initView();

        DemoUtil.showDialog(VideoEditDemoActivity.this,"最简单的工程演示, 完整演示请下载演示APP");

        String path = CopyFileFromAssets.copyAssets(getApplicationContext(), "dy_xialu2.mp4");

        try {
            LSOAsset asset=new LSOAsset(path);
            List<LSOAsset> assets=new ArrayList<>();

            assets.add(asset);

            editPlayer.onCreateAsync(assets, new OnCreateListener() {
                @Override
                public void onCreate() {
                    try {
                        startPreview();
                    } catch (Exception e) {
                        e.printStackTrace();
                        DemoUtil.showDialog(VideoEditDemoActivity.this, "加载视频错误.");
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        editPlayer.onResumeAsync(new OnResumeListener() {
            @Override
            public void onResume() {
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
    protected void onPause() {
        super.onPause();
        editPlayer.onPause();
    }

    private void startPreview() throws Exception {

        if(editPlayer.isRunning()){
            return;
        }

        setCompListeners();

        editPlayer.prepareConcatAssets(new OnAddAssetProgressListener() {
            @Override
            public void onAddAssetProgress(int percent, int numberIndex, int totalNumber) {
                DemoProgressDialog.showMessage(VideoEditDemoActivity.this, " percent:" + percent + " index:" + numberIndex + "/" + totalNumber);
            }

            @Override
            public void onAddAssetCompleted(List<LSOLayer> list, boolean b) {
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
        if (!editPlayer.isExporting()) {
            editPlayer.startExport();
        }
    }

    /**
     * 设置合成的多个监听;
     */
    private void setCompListeners() {
        editPlayer.setOnUserSelectedLayerListener(new OnLanSongSDKUserSelectedLayerListener() {
            @Override
            public void onSelected(LSOLayer lsoLayer) {

            }

            @Override
            public void onCancel() {

            }
        });

        editPlayer.setOnDurationChangedListener(new OnLanSongSDKDurationChangedListener() {
            @Override
            public void onDurationChanged(long ptsUs) {
                if (textView != null) {
                    textView.setText(convertTime(ptsUs) + " / " + convertTime(editPlayer.getDurationUs()));
                }
                DemoUtil.showToast(VideoEditDemoActivity.this,"duration changed.");
            }
        });
        editPlayer.setOnTimeChangedListener(new OnLanSongSDKTimeChangedListener() {
            @Override
            public void onLanSongSDKTimeChanged(long ptsUs, int percent) {
                if (textView != null) {
                    textView.setText(convertTime(ptsUs) + " / " + convertTime(editPlayer.getDurationUs()));
                }
            }
        });
        editPlayer.setOnLanSongSDKPlayProgressListener(new OnLanSongSDKPlayProgressListener() {
            @Override
            public void onLanSongSDKPlayProgress(long ptsUs, int percent) {
                if (percent <= 100) {
                    seekBar.setProgress(percent);
                }
                if (textView != null) {
                    textView.setText(convertTime(ptsUs) + " / " + convertTime(editPlayer.getDurationUs()));
                }
            }
        });
        editPlayer.setOnPlayCompletedListener(new OnLanSongSDKPlayCompletedListener() {
            @Override
            public void onLanSongSDKPlayCompleted() {

            }
        });

        editPlayer.setOnExportProgressListener(new OnLanSongSDKExportProgressListener() {
            @Override
            public void onLanSongSDKExportProgress(long ptsUs, int percent) {
                DemoProgressDialog.showMessage(VideoEditDemoActivity.this, "exporting " + percent);
            }
        });
        editPlayer.setOnExportCompletedListener(new OnLanSongSDKExportCompletedListener() {
            @Override
            public void onLanSongSDKExportCompleted(String dstVideo) {
                DemoProgressDialog.releaseDialog();
                DemoUtil.startPreviewVideo(VideoEditDemoActivity.this,dstVideo);
            }
        });

        editPlayer.setOnErrorListener(new OnLanSongSDKErrorListener() {
            @Override
            public void onLanSongSDKError(int errorCode) {
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

        if (editPlayer.isRunning()) {
            return;
        }
        String path = CopyFileFromAssets.copyAssets(getApplicationContext(), "hongdou10s.mp3");

        editPlayer.printAllConcatLayerTime();
        //开始执行
        if (editPlayer.isLayoutValid()) {
            editPlayer.startPreview(true);
            DemoLog.d("LSOVideoCompositionView running.");
        } else {
            DemoUtil.showDialog(VideoEditDemoActivity.this, "startPreview error. concatCompView.isLayoutValid():" + editPlayer.isLayoutValid());
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        editPlayer.onDestroy();
    }
    private LSOLayer concatBmpLayer =null;
    private LSOLayer overlayLayer=null;

    @Override
    public void onClick(View v) {

        if (editPlayer == null) {
            return;
        }

        switch (v.getId()) {
            case R.id.id_video_comp_pause_resume:
                if (!editPlayer.isPlaying()) {
                    editPlayer.start();
                } else {
                    editPlayer.pause();
                }
                break;
            case R.id.id_video_comp_concat_add_delete: {

                if(concatBmpLayer ==null){
                    String path=CopyFileFromAssets.copyAssets(getApplicationContext(),"pic720x720.jpg");
                    try {

                        editPlayer.insertConcatAssetAtCurrentTime(Arrays.asList(new LSOAsset(path)), new OnAddAssetProgressListener() {
                            @Override
                            public void onAddAssetProgress(int i, int i1, int i2) {

                            }

                            @Override
                            public void onAddAssetCompleted(List<LSOLayer> list, boolean b) {
                                if(list!=null && list.size()>0){
                                    concatBmpLayer =(LSOLayer)list.get(0);
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else{
                    editPlayer.removeLayerAsync(concatBmpLayer);
                    concatBmpLayer =null;
                }
            }
            break;

            case R.id.id_video_comp_concat_overlay_bitmap:
                    if(overlayLayer==null){
                        Bitmap bmp=BitmapFactory.decodeResource(getResources(),R.drawable.ic_launcher);
                        overlayLayer= editPlayer.addBitmapLayer(bmp,0);
                        overlayLayer.setLooping(true);
                    }else{
                        editPlayer.removeLayerAsync(overlayLayer);
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
                    long seekUs = (long) (editPlayer.getDurationUs() * percent);
                    editPlayer.seekToTimeUs(seekUs);
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
