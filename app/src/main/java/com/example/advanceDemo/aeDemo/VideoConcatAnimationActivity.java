package com.example.advanceDemo.aeDemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.advanceDemo.utils.CopyFileFromAssets;
import com.example.advanceDemo.utils.DemoLog;
import com.example.advanceDemo.utils.DemoProgressDialog;
import com.example.advanceDemo.utils.DemoUtil;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.AudioPreviewLayer;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.LSOAsset;
import com.lansosdk.box.LSOAudioAsset;
import com.lansosdk.box.LSOBitmapAsset;
import com.lansosdk.box.LSOLayerPosition;
import com.lansosdk.box.LSOMVAsset;
import com.lansosdk.box.LSOMaskAnimation;
import com.lansosdk.box.LSOMaskAnimationAsset;
import com.lansosdk.box.LSOVideoAsset;
import com.lansosdk.box.LSOVideoOption2;
import com.lansosdk.box.MVLayer;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.OnLanSongSDKThumbnailListener;
import com.lansosdk.box.VideoConcatLayer;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.DrawPadConcatExecute;
import com.lansosdk.videoeditor.DrawPadConcatView;

import java.io.IOException;
import java.util.ArrayList;


/**
 * 视频拼接动画类演示
 * 转场.
 */
public class VideoConcatAnimationActivity extends Activity {

    DrawPadConcatView animationView;
    DemoProgressDialog progressDialog;

    TextView textView;
    TextView tvThumbnail;

    LSOVideoAsset videoAsset;
    LSOVideoAsset videoAsset2;
    LSOVideoAsset videoAsset3;
    LSOAudioAsset audioAsset1;
    SeekBar seekBar;

    LSOMaskAnimationAsset maskAnimationAsset;

    ArrayList<LSOAsset> assets=new ArrayList<>();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.bitmap_animation_layout);
        animationView = findViewById(R.id.id_ae_preview2);
        textView = findViewById(R.id.id_ae_preview_hint);
        tvThumbnail=findViewById(R.id.id_ae_thumbnail_count);


        findViewById(R.id.id_video_concats_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(animationView.isRunning()){
                    cancelPreviewAnimation();
                }else{
                    startPreview();
                }
            }
        });

        findViewById(R.id.id_video_concats_restart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(animationView.isPreviewing()){
                    animationView.pausePreview();
                }else {
                    animationView.setSeekMode(false);
                    animationView.resumePreview();
                }
            }
        });

        findViewById(R.id.id_video_concats_switch_animation).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (concatLayer != null) {
                    String path= CopyFileFromAssets.copyAssets(getApplicationContext(),"mask_animation_xing_out.json");
                    try {
                        concatLayer.switchAnimationAtEnd(new LSOMaskAnimation(path, 1000 * 1000));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        findViewById(R.id.id_video_concats_export).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startExport();
                } catch (Exception e1) {
                    e1.printStackTrace();
                    DemoUtil.showDialog(VideoConcatAnimationActivity.this, "后台执行失败,请查看打印信息");
                }
            }
        });

        seekBar=findViewById(R.id.id_bitmap_animation_seek_bar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    float percent=(progress*1.0f/100f);
                    long seekUs=(long)(animationView.getDurationUs()*percent);
                    animationView.seekToTimeUs(seekUs);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                animationView.setSeekMode(true);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                animationView.setSeekMode(false);
                animationView.resumePreview();
            }
        });
        /**
         * 准备各种素材;
         */
        try {
            loadVideoAssets();
            prepareMVAsset();

            String path= CopyFileFromAssets.copyAssets(getApplicationContext(),"mask_animation_cycle_out.json");
            maskAnimationAsset=new LSOMaskAnimationAsset(path, 1000 * 1000);


            //第三步:根据设置的宽高来重新布局预览界面,开始预览;
            animationView.setDrawPadSize(544, 960, new onDrawPadSizeChangedListener() {
                @Override
                public void onSizeChanged(int viewWidth, int viewHeight) {
                    startPreview();
                }
            });

        } catch (Exception e1) {
            e1.printStackTrace();
            DemoUtil.showDialog(VideoConcatAnimationActivity.this, "无法加载素材.请加载素材后再演示");
        }
    }

    private boolean loadAssetSuccess = false;



    private void loadVideoAssets() throws Exception {
//        videoAsset = new LSOVideoAsset(SDCARD.gaSha1280x720_90du());
//        assets.add(videoAsset);

        videoAsset2 = new LSOVideoAsset("/sdcard/TEST_VIDEO/v1920x1088_90du.mp4");
        assets.add(videoAsset2);

        videoAsset3 = new LSOVideoAsset("/sdcard/TEST_VIDEO/v1280x720_90du.mp4");
        assets.add(videoAsset3);

        audioAsset1=new LSOAudioAsset(CopyFileFromAssets.copyAssets(getApplicationContext(),"hongdou10s.mp3"));

        loadAssetSuccess = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //当播放完毕进去下一个界面,再次返回的时候,让它直接播放;
        animationView.setOnViewAvailable(new DrawPadConcatView.onViewAvailable() {
            @Override
            public void viewAvailable(DrawPadConcatView v) {
                startPreview();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        animationView.cancel();
    }
    VideoConcatLayer concatLayer=null;


    private void cancelPreviewAnimation() {

        if (animationView != null) {
            animationView.cancel();
        }
    }


    /**
     * 开始预览
     */
    private void startPreview() {
        try {
            startAEPreview2();
        } catch (Exception e1) {
            e1.printStackTrace();
            DemoUtil.showDialog(VideoConcatAnimationActivity.this, "开启预览失败,请查看打印信息");
        }
    }
    /**
     * 开始预览.
     */
    private void startAEPreview2() throws Exception {
        if (animationView.isRunning()) {
            return;
        }

        if (!loadAssetSuccess) {
            return;
        }
//        //拼接一个视频
        if(videoAsset2!=null){
            LSOVideoOption2 option2=new LSOVideoOption2();
            option2.overLapTimeUS=0;
            VideoConcatLayer concatLayer = animationView.concatVideoLayer(videoAsset2,null);
            concatLayer.switchAnimationAtEnd(new LSOMaskAnimation(maskAnimationAsset)); //给视频尾部增加一个mask动画
            concatLayer.setTAG("#2");
        }
        //拼接一个视频
        if(videoAsset3!=null){
            VideoConcatLayer concatLayer3 = animationView.concatVideoLayer(videoAsset3,null);
            concatLayer3.setTAG("#3");
            concatLayer3.switchAnimationAtEnd(new LSOMaskAnimation(maskAnimationAsset)); //给视频尾部增加一个mask动画
        }


        //增加一个logo
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ls_logo);
        BitmapLayer bitmapLayer = animationView.addBitmapLayer(new LSOBitmapAsset(bmp));
        if (bitmapLayer != null) {
            bitmapLayer.setPosition(LSOLayerPosition.LeftTop);
        }

        AudioPreviewLayer layer=animationView.addAudioLayer(audioAsset1);


        //开始执行
        if (animationView.isLayoutValid() && animationView.startPreview()) {
            DemoLog.d(" ae preview is running.");
        } else {
            DemoUtil.showDialog(VideoConcatAnimationActivity.this, "AE预览开启失败. animationView.isLayoutValid():"+animationView.isLayoutValid());
        }
        cnt=0;

        animationView.setOnLanSongSDKProgressListener(new OnLanSongSDKProgressListener() {
            @Override
            public void onLanSongSDKProgress(long ptsUs, int percent) {
                if (textView != null) {
                    textView.setText("ptsUs" + ptsUs);
                }
                seekBar.setProgress(percent);
            }
        });

        animationView.setOnLanSongSDKErrorListener(new OnLanSongSDKErrorListener() {
            @Override
            public void onLanSongSDKError(int errorCode) {
                animationView.release();
                hideProgressDialog();
                DemoUtil.showDialog(VideoConcatAnimationActivity.this, "AE执行错误,请查看错误信息.我们的TAG是LanSongSDK.");
            }
        });

//        videoAsset.getThumbnailBitmapsAsynchronously(new OnLanSongSDKThumbnailListener() {
//            @Override
//            public void onLanSongSDKThumbnail(Bitmap bmp) {
//                if(bmp!=null){
//                    tvThumbnail.setText("缩略图个数: " + cnt++ );
//                }else{
//                    tvThumbnail.setText("缩略图个数:NULL" );
//                }
//            }
//        });


        videoAsset2.getThumbnailBitmapsAsynchronously(new OnLanSongSDKThumbnailListener() {
            @Override
            public void onLanSongSDKThumbnail(Bitmap bmp) {
                if(bmp!=null){
                    cnt++;
                    tvThumbnail.setText("缩略图个数: " + cnt );
                }else{
                    tvThumbnail.setText("缩略图个数:NULL" );
                }
            }
        });
        videoAsset3.getThumbnailBitmapsAsynchronously(new OnLanSongSDKThumbnailListener() {
            @Override
            public void onLanSongSDKThumbnail(Bitmap bmp) {

                cnt++;

                if(bmp!=null){
                    tvThumbnail.setText("缩略图个数: " + cnt);
                }else{
                    tvThumbnail.setText("缩略图个数:NULL" );
                }
            }
        });

    }


    int  cnt;
    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.release();
            progressDialog = null;
        }
    }
    private LSOMVAsset mvAsset;
    private MVLayer mvLayer;
    private void prepareMVAsset(){
        String colorPath= CopyFileFromAssets.copyAssets(getApplicationContext(),"kd_mvColor.mp4");
        String maskPath= CopyFileFromAssets.copyAssets(getApplicationContext(),"kd_mvMask.mp4");
        try {
            mvAsset=new LSOMVAsset(colorPath,maskPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assets.add(mvAsset);
    }
    private void addMVLayer(){
        if(mvLayer!=null){
            animationView.removeLayer(mvLayer);
            mvLayer=null;
        }
        mvLayer=animationView.addMVLayer(mvAsset);
    }
    private void removeMVLayer(){
        if(mvLayer!=null){
            animationView.removeLayer(mvLayer);
            mvAsset.release();
            mvLayer=null;
        }
    }
    //------------------后台导出
    DrawPadConcatExecute execute;
    /**
     * 开始导出
     */
    private void startExport() throws Exception {
        if (animationView != null) {
            animationView.cancel();
        }
        execute = new DrawPadConcatExecute(getApplicationContext(), 720, 1280);

        //增加第一个视频
        VideoConcatLayer playerLayer = execute.concatVideoLayer(videoAsset2,null);
        String path= CopyFileFromAssets.copyAssets(getApplicationContext(),"xing_out_transform.json");
        playerLayer.switchAnimationAtEnd(new LSOMaskAnimation(path, 1000 * 1000));  //1000*1000表示当前动画执行的时长,


        //增加第二个视频
        execute.concatVideoLayer(videoAsset2,null);

        //增加第三个视频
        execute.concatVideoLayer(videoAsset3,null);

        //增加mv图层;
        execute.addMVLayer(mvAsset);

        LSOAudioAsset audioAsset=new LSOAudioAsset(CopyFileFromAssets.copyAssets(getApplicationContext(),"hongdou10s.mp3"));
        execute.addAudioLayer(audioAsset);

        //--------增加logo
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ls_logo);
        BitmapLayer bitmapLayer = execute.addBitmapLayer(new LSOBitmapAsset(bmp));
        if (bitmapLayer != null) {
            bitmapLayer.setPosition(LSOLayerPosition.LeftTop);
        }

        execute.setOnLanSongSDKProgressListener(new OnLanSongSDKProgressListener() {
            @Override
            public void onLanSongSDKProgress(long ptsUs, int percent) {
                Log.e("LSDelete", "ptsUs  is : " + ptsUs + " percent is :" + percent);
                DemoProgressDialog.showPercent(VideoConcatAnimationActivity.this,percent);
            }
        });

        execute.setOnLanSongSDKCompletedListener(new OnLanSongSDKCompletedListener() {
            @Override
            public void onLanSongSDKCompleted(String dstVideo) {
                DemoUtil.playDstVideo(VideoConcatAnimationActivity.this,dstVideo);
            }
        });
        execute.startExport();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        animationView.release();

        //在使用完毕后, 要释放资源;
        for (LSOAsset asset: assets){
            asset.release();
        }

    }
}
