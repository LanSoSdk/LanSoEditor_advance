package com.example.advanceDemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.advanceDemo.utils.CopyFileFromAssets;
import com.example.advanceDemo.utils.DemoLog;
import com.example.advanceDemo.utils.DemoProgressDialog;
import com.example.advanceDemo.utils.DemoUtil;
import com.lansosdk.box.LSOBitmapListLayer;
import com.lansosdk.videoeditor.LSOLayerTouchView;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.LSOAsset;
import com.lansosdk.box.LSOAudioLayer;
import com.lansosdk.box.LSOBitmapLayer;
import com.lansosdk.box.LSOConcatVideoLayer;
import com.lansosdk.box.LSOFileNotSupportException;
import com.lansosdk.box.LSOGifAsset;
import com.lansosdk.box.LSOGifLayer;
import com.lansosdk.box.LSOLayer;
import com.lansosdk.box.LSOLayerPosition;
import com.lansosdk.box.LSOMVAsset;
import com.lansosdk.box.LSOMVLayer;
import com.lansosdk.box.LSOVideoAsset;
import com.lansosdk.box.OnLanSongSDKAddVideoProgressListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKExportCompletedListener;
import com.lansosdk.box.OnLanSongSDKExportProgressListener;
import com.lansosdk.box.OnLanSongSDKPlayCompletedListener;
import com.lansosdk.box.OnLanSongSDKPlayProgressListener;
import com.lansosdk.videoeditor.LSOConcatCompositionView;
import com.lansosdk.videoeditor.LanSoEditor;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.OnCompositionSizeRatioListener;
import com.lansosdk.videoeditor.OnCompositionSizeReadyListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class ConcatCompositionActivity extends Activity implements View.OnClickListener {

    LSOConcatCompositionView concatCompView;
    DemoProgressDialog progressDialog;

    TextView textView;
    TextView tvThumbnail;
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
                    DemoUtil.showDialog(ConcatCompositionActivity.this,"加载视频错误.");
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
        concatCompView.cancel();
    }

    private void cancelComposition() {

        if (concatCompView != null) {
            concatCompView.cancel();
        }
    }
    LSOConcatVideoLayer concatVideoLayer;
    LSOConcatVideoLayer testConcatLayer;
    /**
     * 开始预览
     */
    private void startPreview() throws  Exception{
        if (concatCompView.isRunning()) {
            return;
        }

        setCompListeners();

        //异步的形式
        List<LSOAsset> paths=new ArrayList<>();
        String path=CopyFileFromAssets.copyAssets(getApplicationContext(),"dy_xialu2.mp4");

       paths.add(new LSOVideoAsset(path));

        concatCompView.setBackGroundBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.a1));


        //异步增加视频图层,
        concatCompView.addConcatLayerListAsync(paths, new OnLanSongSDKAddVideoProgressListener() {
            @Override
            public void onAddVideoProgress(int percent, int numberIndex, int totalNumber) {
                DemoProgressDialog.showMessage(ConcatCompositionActivity.this," percent:" + percent + " index:"+ numberIndex + "/" + totalNumber);
            }

            @Override
            public void onAddVideoCompleted(List layers) {
                DemoProgressDialog.releaseDialog();
                List<LSOLayer> layerList=layers;
                if(layerList.size()>0){

                    if(layerList.get(0).isConcatVideoLayer()){
                        concatVideoLayer=(LSOConcatVideoLayer)layers.get(0);

                        if(concatVideoLayer!=null){
                            String path=CopyFileFromAssets.copyAssets(getApplicationContext(),"mask_animation_cycle_out.json");
                            try {
                                concatVideoLayer.setTransitionMaskPathAndDuration(path,1000*1000);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
//                        concatVideoLayer.setBeautyLevel(0.5f);
//                        concatVideoLayer.setWhiteBalanceFilterPercent2X(1.5f);
//                        for (int i=0;i<concatVideoLayer.getThumbnailList().size();i++){
//                            Log.e("LSDelete", "----------- tag is : " + concatVideoLayer.getThumbnailList().get(i).getWidth());
//                        }
//                        concatVideoLayer.setLayerMirror(true,true);
//                        concatVideoLayer.setVideoSpeed(2.0f);
//                        concatVideoLayer.setVideoReverse(true);
                    }
//                    for (LSOLayer layer: layers){
//                        layer.setScaleFactor(0.1f);
//                    }
                }

                try {
                    startComposition();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        //特效;
        //设置速度; 速度的时间线;
        //动画;
    }

    private void startExport(){
        if(!concatCompView.isExporting()){
            concatCompView.startExport();
        }
    }
    /**
     *  设置合成的多个监听;
     */
    private void setCompListeners(){

        concatCompView.setOnLanSongSDKPlayProgressListener(new OnLanSongSDKPlayProgressListener() {
            @Override
            public void onLanSongSDKPlayProgress(long ptsUs, int percent) {
                if (textView != null) {
                    textView.setText("ptsUs" + ptsUs);
                }
                if(percent<=100){
                    seekBar.setProgress(percent);
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

                DemoProgressDialog.showMessage(ConcatCompositionActivity.this," 正在导出:" + percent);
                Log.e("LSDelete", "---正在导出...: " + ptsUs);
            }
        });
        concatCompView.setOnLanSongSDKExportCompletedListener(new OnLanSongSDKExportCompletedListener() {
            @Override
            public void onLanSongSDKExportCompleted(String dstVideo) {

                DemoProgressDialog.releaseDialog();
                MediaInfo.checkFile(dstVideo);
                DemoUtil.startPreviewVideo(ConcatCompositionActivity.this,dstVideo);
            }
        });
        
        concatCompView.setOnLanSongSDKErrorListener(new OnLanSongSDKErrorListener() {
            @Override
            public void onLanSongSDKError(int errorCode) {
                concatCompView.release();
                hideProgressDialog();
                DemoUtil.showDialog(ConcatCompositionActivity.this, "AE执行错误,请查看错误信息.我们的TAG是LanSongSDK.");
            }
        });
    }
    /**
     * 开始预览播放
     */
    private void startComposition(){

        if(concatCompView.isRunning()){
            return;
        }

//        图片, videoAsset, 保存视频加字体.
        String path=CopyFileFromAssets.copyAssets(getApplicationContext(),"hongdou10s.mp3");
        audioLayer= concatCompView.addAudioLayer(path,0);
        concatCompView.printAllConcatLayerTime();
        DemoUtil.showToast(getApplication(),"开始预览.");

        //开始执行

        if (concatCompView.isLayoutValid()) {
            concatCompView.startPreview(true);
            DemoLog.d("LSOVideoCompositionView running.");
        } else {
            DemoUtil.showDialog(ConcatCompositionActivity.this, "AE预览开启失败. concatCompView.isLayoutValid():" + concatCompView.isLayoutValid());
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.release();
            progressDialog = null;
        }
    }
    //------------------后台导出
    @Override
    protected void onDestroy() {
        super.onDestroy();
        concatCompView.release();
    }

    private LSOAudioLayer  audioLayer=null;
    private LSOBitmapLayer bitmapLayer=null;



    //gif资源
    private LSOGifAsset gifAsset=null;
    //gif图层;
    private LSOGifLayer gifLayer=null;


    private void resumeComposition(){
        if(concatCompView !=null){
            concatCompView.resume();
        }
    }
    private List<String> bitmapList;
    private LSOBitmapListLayer bitmapListLayer=null;

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.id_video_comp_pause_resume:
                if(!concatCompView.isPlaying()){
                    resumeComposition();
                }else{
                    concatCompView.pause();
                }
                break;
            case R.id.id_video_comp_concat_add_delete:
                if(testConcatLayer==null){
                    try {
                        String path=CopyFileFromAssets.copyAssets(getApplicationContext(),"d6_720x1280.mp4");

                        concatCompView.addConcatLayerAsync(new LSOVideoAsset(path), new OnLanSongSDKAddVideoProgressListener() {
                            @Override
                            public void onAddVideoProgress(int percent, int numberIndex, int totalNumber) {
                                DemoProgressDialog.showMessage(ConcatCompositionActivity.this," percent:" + percent + " index:"+ numberIndex + "/" + totalNumber);
                            }
                            @Override
                            public void onAddVideoCompleted(List layers) {
                                DemoProgressDialog.releaseDialog();
                                List<LSOLayer> layerList=layers;

                                if(layerList.size()>0){
                                    if(layerList.get(0).isConcatVideoLayer()){
                                        testConcatLayer=(LSOConcatVideoLayer)layers.get(0);
                                    }
                                }
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (LSOFileNotSupportException e) {
                        e.printStackTrace();
                    }
                }else{
                    concatCompView.removeLayer(testConcatLayer);
                    testConcatLayer=null;
                }
                break;
            case R.id.id_video_comp_bitmap_list_add_delete:

                if(bitmapListLayer==null){
                    if(bitmapList==null){
                        bitmapList=new ArrayList<>();
                        for (int i=0;i<30;i++){
                            bitmapList.add(CopyFileFromAssets.copyAssets(getApplicationContext(),String.format(Locale.getDefault(),"necklace_%03d.png",i)));
                        }
                    }
                    bitmapListLayer= concatCompView.addBitmapListLayerFromPaths(bitmapList,40*1000,0);
                    bitmapListLayer.setLooping(true);
                }else{
                    concatCompView.removeLayer(bitmapListLayer);
                    bitmapListLayer=null;
                }

                break;
            case R.id.id_video_comp_export:
                startExport();
                break;
            case R.id.id_video_comp_audio_add_delete:
                if(audioLayer==null){
                    String path=CopyFileFromAssets.copyAssets(getApplicationContext(),"hongdou10s.mp3");
                    audioLayer= concatCompView.addAudioLayer(path,0);
                    audioLayer.setLooping(true);
                }else{
                    concatCompView.removeAudioLayer(audioLayer);
                    audioLayer=null;
                }
                break;

                //增加图片图层;
            case R.id.id_video_comp_bitmap_add_delete:

                if(bitmapLayer==null){
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.ic_launcher);
                    bitmapLayer= concatCompView.addBitmapLayer(bitmap);
                    bitmapLayer.setLooping(true);

                }else{
                    concatCompView.removeLayer(bitmapLayer);
                    bitmapLayer=null;
                }
                break;
                //增加gif图层
            case R.id.id_video_comp_gif_add_delete:
                if(gifLayer==null){
                    if(gifAsset==null){
                        try {
                            //资源只创建一次;
                            gifAsset=new LSOGifAsset(getApplicationContext(),R.drawable.g06);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    gifLayer= concatCompView.addGifLayer(gifAsset);
                    if(gifLayer!=null){
                        gifLayer.setPosition(LSOLayerPosition.LEFT_BOTTOM);
                    }
                }else{
                    concatCompView.removeLayer(gifLayer);
                    gifLayer=null;
                }
                break;
            default:
                break;
        }
    }

    private void initView() {

        findViewById(R.id.id_video_comp_audio_add_delete).setOnClickListener(this);
        findViewById(R.id.id_video_comp_bitmap_add_delete).setOnClickListener(this);
        findViewById(R.id.id_video_comp_gif_add_delete).setOnClickListener(this);

        findViewById(R.id.id_video_comp_export).setOnClickListener(this);

        findViewById(R.id.id_video_comp_concat_add_delete).setOnClickListener(this);
        findViewById(R.id.id_video_comp_pause_resume).setOnClickListener(this);

        findViewById(R.id.id_video_comp_bitmap_list_add_delete).setOnClickListener(this);


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
