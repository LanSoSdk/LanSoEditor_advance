package com.example.advanceDemo;

import android.app.Activity;
import android.content.Intent;
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
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.aex.LSOAexImage;
import com.lansosdk.aex.LSOAexOption;
import com.lansosdk.box.LSOAexModule;
import com.lansosdk.box.LSOLayerPosition;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.OnAexImageSelectedListener;
import com.lansosdk.box.OnAexJsonPrepareListener;
import com.lansosdk.box.OnCreateListener;
import com.lansosdk.box.OnLSOAexImageChangedListener;
import com.lansosdk.box.OnLanSongSDKCompressListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKExportCompletedListener;
import com.lansosdk.box.OnLanSongSDKExportProgressListener;
import com.lansosdk.box.OnLanSongSDKPlayCompletedListener;
import com.lansosdk.box.OnLanSongSDKPlayProgressListener;
import com.lansosdk.box.OnLanSongSDKTimeChangedListener;
import com.lansosdk.box.OnResumeListener;
import com.lansosdk.videoeditor.LSOAexPlayer;

import java.util.Locale;

/**
 * @author sno
 */
public class AexPlayerDemoActivity extends Activity implements View.OnClickListener {

    private LSOAexPlayer aexPlayerView;


    private TextView textView;
    private TextView tvCurrentImage;
    private LSOAexModule module;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.test_aex_layout);
        aexPlayerView = findViewById(R.id.id_test_ae_gpu_preview2);
        initView();
        //prepare Ae Template;
        prepareOneAeTemplate();
    }


    private void prepareOneAeTemplate(){
        String jsonPath=CopyFileFromAssets.copyAssets(getApplicationContext(),"ae_august.json");
        String mvColorPath=CopyFileFromAssets.copyAssets(getApplicationContext(),"ae_august_mvColor.mp4");
        String mvMaskPath=CopyFileFromAssets.copyAssets(getApplicationContext(),"ae_august_mvMask.mp4");

        module= new LSOAexModule(jsonPath);
        try {
            module.setMvVideo(mvColorPath,mvMaskPath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        module.prepareAsync(getApplicationContext(), new OnAexJsonPrepareListener() {
            @Override
            public void onPrepared(boolean success, String errorCode) {

                if(success){
                    startPlayAe();
                }else{
                    DemoUtil.showDialog(AexPlayerDemoActivity.this,"解析json错误,请联系我们");
                }
            }
        });
    }

    private void startPlayAe(){
        //replace some  picture;
        //替换一些图片,实际用相册中选择的;
        for (int i=0;i<module.getAexImageList().size();i++){
            String path= CopyFileFromAssets.copyAssets(getApplicationContext(),String.format(Locale.getDefault(),"kadian_img_%d.jpeg",i));
            try {
                module.updatePathAtIndex(i,path);  //<--------here replace  some
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        aexPlayerView.onCreateAsync(module, new OnCreateListener() {
            @Override
            public void onCreate() {
                try {
                    startAEPreview();
                } catch (Exception e1) {
                    e1.printStackTrace();
                    DemoUtil.showDialog(AexPlayerDemoActivity.this, "开启预览失败,请查看打印信息");
                }
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        aexPlayerView.onResumeAsync(new OnResumeListener() {
            @Override
            public void onResume() {

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        aexPlayerView.onPause();
    }

    private int currentImageIndex=0;

    /**
     * 开始预览.
     */
    private void startAEPreview() throws Exception {

        if (aexPlayerView.isRunning()) {
            return;
        }



        aexPlayerView.addAeModule(module);

        aexPlayerView.setOnAexImageSelectedListener(new OnAexImageSelectedListener() {
            @Override
            public void onSelected(LSOAexImage aexImage) {
                if(tvCurrentImage!=null){
                    tvCurrentImage.setText("current: " + aexImage.index);
                }
                currentImageIndex=aexImage.index;
            }
            @Override
            public void onCancel() {
                Log.e("LSDelete", "--setOnAexImageSelectedListener cancel...: ");
            }
        });

        aexPlayerView.setOnAexImageChangedListener(new OnLSOAexImageChangedListener() {
            @Override
            public void onAexPlayerAexImageChanged(int index, LSOAexImage image) {
                if(tvCurrentImage!=null){
                    tvCurrentImage.setText("current: " + index);
                }
                currentImageIndex=image.index;
            }
        });

        aexPlayerView.setOnLanSongSDKTimeChangedListener(new OnLanSongSDKTimeChangedListener() {
            @Override
            public void onLanSongSDKTimeChanged(long ptsUs, int percent) {
                if (textView != null) {
                    //保留2位小数;
                    int time = (int) (ptsUs * 100 / 1000000L);
                    float timeS = (float) time / 100.0f;
                    textView.setText("progress:" + timeS);
                }
            }
        });

        aexPlayerView.setOnLanSongSDKPlayProgressListener(new OnLanSongSDKPlayProgressListener() {
            @Override
            public void onLanSongSDKPlayProgress(long ptsUs, int percent) {
                if (progressSeekBar != null) {
                    progressSeekBar.setProgress(percent);
                }
            }
        });

        aexPlayerView.setOnLanSongSDKPlayCompletedListener(new OnLanSongSDKPlayCompletedListener() {
            @Override
            public void onLanSongSDKPlayCompleted() {
                DemoUtil.showDialog(AexPlayerDemoActivity.this, "play complete!");
            }
        });

        aexPlayerView.setOnLanSongSDKErrorListener(new OnLanSongSDKErrorListener() {
            @Override
            public void onLanSongSDKError(int errorCode) {
                DemoUtil.showDialog(AexPlayerDemoActivity.this, "AE执行错误,请查看错误信息.我们的TAG是LanSongSDK.");
            }
        });
        aexPlayerView.setOnLanSongSDKExportProgressListener(new OnLanSongSDKExportProgressListener() {
            @Override
            public void onLanSongSDKExportProgress(long ptsUs, int percent) {

                DemoProgressDialog.showMessage(AexPlayerDemoActivity.this, "export :" + percent);
            }
        });

        aexPlayerView.setOnLanSongSDKExportCompletedListener(new OnLanSongSDKExportCompletedListener() {
            @Override
            public void onLanSongSDKExportCompleted(String dstVideo) {
                DemoProgressDialog.releaseDialog();
                DemoUtil.startPreviewVideo(AexPlayerDemoActivity.this, dstVideo);
            }
        });

        aexPlayerView.setOnLanSongSDKCompressListener(new OnLanSongSDKCompressListener() {
            @Override
            public void onCompressProgress(int percent, int numberIndex, int totalNumber) {
                DemoProgressDialog.showMessage(AexPlayerDemoActivity.this, "preparing: " + percent + " index:" + numberIndex + "/" + totalNumber);
            }

            @Override
            public void onCompressCompleted() {
                DemoProgressDialog.releaseDialog();
            }
        });


        Bitmap bmp= BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);

        aexPlayerView.addLogoBitmap(bmp, LSOLayerPosition.LEFT_BOTTOM);

        if (aexPlayerView.isLayoutValid()) {
            aexPlayerView.startPreview();
            DemoLog.d(" ae preview is running.");
        } else {
            DemoUtil.showDialog(AexPlayerDemoActivity.this, "ae preview failed layoutValid:" + aexPlayerView.isLayoutValid());
        }

    }


    private SeekBar progressSeekBar;
    private void initView() {

        textView = (TextView) findViewById(R.id.id_test_ae_gpu_preview_hint);
        tvCurrentImage=findViewById(R.id.id_test_ae_gpu_current_item);

        findViewById(R.id.id_test_ae_gpu_pause).setOnClickListener(this);
        findViewById(R.id.id_test_ae_gpu_replace_video).setOnClickListener(this);
        findViewById(R.id.id_test_ae_gpu_replace_image).setOnClickListener(this);
        findViewById(R.id.id_test_ae_gpu_export).setOnClickListener(this);

        progressSeekBar = (SeekBar) findViewById(R.id.id_ae_gpu_seek_bar);
        progressSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if (fromUser) {
                    float percent = (float) progress / 100.0f;
                    aexPlayerView.seekToTimeUs((long) (percent * aexPlayerView.getDurationUs()));
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_test_ae_gpu_pause:
                if (aexPlayerView.isPlaying()) {
                    aexPlayerView.pause();
                } else {
                    aexPlayerView.resume();
                }
                break;

            case R.id.id_test_ae_gpu_replace_video:
                try {
                    if(currentImageIndex<module.getAexImageList().size()){
                        String path= CopyFileFromAssets.copyAssets(getApplicationContext(),"dy_xialu2.mp4");
                        module.getAexImageList().get(currentImageIndex).updatePath(path,null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case R.id.id_test_ae_gpu_replace_image:
                if(currentImageIndex<module.getAexImageList().size()){
                    String path= CopyFileFromAssets.copyAssets(getApplicationContext(),"pic720x720.jpg");
                    module.getAexImageList().get(currentImageIndex).updatePath(path,null);
                }
                break;
            case R.id.id_test_ae_gpu_export:
                aexPlayerView.startExport();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        aexPlayerView.onDestroy();
    }
}
