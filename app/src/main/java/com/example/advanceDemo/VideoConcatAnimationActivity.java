package com.example.advanceDemo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.advanceDemo.utils.CopyFileFromAssets;
import com.example.advanceDemo.utils.DemoLog;
import com.example.advanceDemo.utils.DemoProgressDialog;
import com.example.advanceDemo.utils.DemoUtil;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.BitmapAnimationLayer;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.LSOBitmapAsset;
import com.lansosdk.box.LSOAeAnimation;
import com.lansosdk.box.LSOLayerPosition;
import com.lansosdk.box.LSOMaskAnimation;
import com.lansosdk.box.LSOVideoAsset;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.VideoConcatLayer;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.DrawPadConcatView;
import com.lansosdk.videoeditor.DrawPadConcatExecute;
import com.lansosdk.videoeditor.MediaInfo;

import java.util.ArrayList;
import java.util.Locale;

/**
 * 视频拼接动画类演示
 * 转场.
 */
public class VideoConcatAnimationActivity extends Activity {

    DrawPadConcatView animationView;
    DemoProgressDialog progressDialog;

    TextView textView;

    private ArrayList<Bitmap> bmpList1 = new ArrayList<>();
    private ArrayList<Bitmap> bmpList2 = new ArrayList<>();

    LSOVideoAsset videoAsset;
    LSOVideoAsset videoAsset2;
    LSOVideoAsset videoAsset3;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.bitmap_animation_layout);
        animationView = findViewById(R.id.id_ae_preview2);

        textView = findViewById(R.id.id_ae_preview_hint);

        findViewById(R.id.id_video_concats_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelPreviewAnimation();
            }
        });
        findViewById(R.id.id_video_concats_restart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAEPreview();
            }
        });

        findViewById(R.id.id_video_concats_switch_animation).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playerLayer2 != null) {
                    playerLayer2.switchAnimationAtEnd(new LSOMaskAnimation(bmpList2, 1000 * 1000));
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
        try {
            loadVideoAssets();
            //第三步:根据设置的宽高来重新布局预览界面,开始预览;
            animationView.setDrawPadSize(544, 960, new onDrawPadSizeChangedListener() {
                @Override
                public void onSizeChanged(int viewWidth, int viewHeight) {
                    startAEPreview();
                }
            });

        } catch (Exception e1) {
            e1.printStackTrace();
            DemoUtil.showDialog(VideoConcatAnimationActivity.this, "无法加载素材.请加载素材后再演示");
        }
    }

    private boolean loadAssetSuccess = false;

    private void loadVideoAssets() throws Exception {
        videoAsset = new LSOVideoAsset(CopyFileFromAssets.copyShanChu(getApplicationContext(),"d1.mp4"));
        videoAsset2 = new LSOVideoAsset(CopyFileFromAssets.copyShanChu(getApplicationContext(),"v1920x1080_90du_59fps_OnePlus.mp4"));
        videoAsset3 = new LSOVideoAsset(CopyFileFromAssets.copyShanChu(getApplicationContext(),"v1280x720_90du.mp4"));


        //准备30个图片用做mask动画;
        for (int i = 0; i < 30; i++) {
            String path=String.format(Locale.getDefault(), "shu720x1280_%05d.png", i);

            Bitmap bmp = BitmapFactory.decodeFile(CopyFileFromAssets.copyShanChu(getApplicationContext(),path));
            if (bmp != null) {
                bmpList1.add(bmp);
            }
        }

        loadAssetSuccess = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //当播放完毕进去下一个界面,再次返回的时候,让它直接播放;
        animationView.setOnViewAvailable(new DrawPadConcatView.onViewAvailable() {
            @Override
            public void viewAvailable(DrawPadConcatView v) {
                startAEPreview();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        animationView.cancel();
    }

    private void cancelPreviewAnimation() {

        if (animationView != null) {
            animationView.cancel();
        }
    }

    VideoConcatLayer playerLayer2;

    private void startAEPreview() {
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
        if (animationView.isPlaying()) {
            return;
        }
        if (!loadAssetSuccess) {
            return;
        }

        animationView.setOnLanSongSDKProgressListener(new OnLanSongSDKProgressListener() {
            @Override
            public void onLanSongSDKProgress(long ptsUs, int percent) {
                if (textView != null) {
                    textView.setText("进度" + percent);
                }
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


        //拼接一个视频;
        VideoConcatLayer playerLayer = animationView.concatVideoLayer(videoAsset);
        playerLayer.switchAnimationAtEnd(new LSOMaskAnimation(bmpList1, 1000 * 1000));  //给视频尾部增加一个mask动画


        //拼接一个视频
        playerLayer2 = animationView.concatVideoLayer(videoAsset2);
        playerLayer2.switchAnimationAtEnd(new LSOMaskAnimation(bmpList1, 1000 * 1000)); //给视频尾部增加一个mask动画

        //拼接一个视频
        animationView.concatVideoLayer(videoAsset3);


        //增加一个logo
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ls_logo);
        BitmapLayer bitmapLayer = animationView.addBitmapLayer(new LSOBitmapAsset(bmp));
        if (bitmapLayer != null) {
            bitmapLayer.setPosition(LSOLayerPosition.LeftTop);
        }
        //开始执行
        if (animationView.isLayoutValid() && animationView.startPreview()) {
            DemoLog.d(" ae preview is running.");
        } else {
            DemoUtil.showDialog(VideoConcatAnimationActivity.this, "AE预览开启失败.");
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.release();
            progressDialog = null;
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


        //增加视频
        VideoConcatLayer playerLayer = execute.concatVideoLayer(videoAsset);
        playerLayer.switchAnimationAtEnd(new LSOMaskAnimation(bmpList1, 1000 * 1000));  //1000*1000表示当前动画执行的时长,


        //增加视频
        VideoConcatLayer playerLayer2=execute.concatVideoLayer(videoAsset2);
        playerLayer2.switchAnimationAtEnd(new LSOMaskAnimation(bmpList1, 1000 * 1000));  //1000*1000表示当前动画执行的时长,

        //增加视频
        execute.concatVideoLayer(videoAsset3);


        //--------增加logo
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ls_logo);
        BitmapLayer bitmapLayer = execute.addBitmapLayer(new LSOBitmapAsset(bmp));
        if (bitmapLayer != null) {
            bitmapLayer.setPosition(LSOLayerPosition.LeftTop);
        }


        execute.setOnLanSongSDKProgressListener(new OnLanSongSDKProgressListener() {
            @Override
            public void onLanSongSDKProgress(long ptsUs, int percent) {
                Log.e("DEMO", "DrawPadConcatExecute :ptsUs  is : " + ptsUs + " percent is :" + percent);
            }
        });

        execute.setOnLanSongSDKCompletedListener(new OnLanSongSDKCompletedListener() {
            @Override
            public void onLanSongSDKCompleted(String dstVideo) {
                MediaInfo.checkFile(dstVideo);
            }
        });
        execute.startExport();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        animationView.release();
    }
}
