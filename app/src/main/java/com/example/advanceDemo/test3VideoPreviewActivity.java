package com.example.advanceDemo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.example.advanceDemo.utils.DemoProgressDialog;
import com.example.advanceDemo.utils.DemoUtil;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.DrawPadUpdateMode;
import com.lansosdk.box.LSOScaleType;
import com.lansosdk.box.LSOVideoAsset;
import com.lansosdk.box.LSOVideoOption;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.VideoFrameLayer;
import com.lansosdk.box.VideoLayer2;
import com.lansosdk.box.onDrawPadRunTimeListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.DrawPadAllExecute2;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.MediaInfo;

/**
 * 平移,旋转,缩放,RGBA值,显示/不显示(闪烁)效果. 实际使用中, 可用这些属性来做些动画,比如平移+RGBA调节,呈现舒缓移除的效果.
 * 缓慢缩放呈现照片播放效果;旋转呈现欢快的炫酷效果等等.
 */

public class test3VideoPreviewActivity extends Activity{
    private String videoPath;
    private DrawPadView drawPadView;
    private VideoLayer2 videoLayer = null;
    private BitmapLayer bitmapLayer = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_video_concat);

        videoPath = DemoApplication.getInstance().currentEditVideo;
        MediaInfo.checkFile(videoPath);

        drawPadView = (DrawPadView) findViewById(R.id.id_drawpad_drawpadview);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                initDrawPad();
            }
        }, 100);
        findViewById(R.id.id_btn_export).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startExport();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }



    boolean isInitDrawPad = false;
    private void initDrawPad() {
        if (isInitDrawPad) {
            return;
        }
        isInitDrawPad = true;

        drawPadView.setUpdateMode(DrawPadUpdateMode.AUTO_FLUSH,25);
        /**
         * 设置显示容器的尺寸;
         */
        drawPadView.setDrawPadSize(540,960, new onDrawPadSizeChangedListener() {
            @Override
            public void onSizeChanged(int viewWidth, int viewHeight) {
                try {
                    startDrawPad();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        drawPadView.setOnDrawPadRunTimeListener(new onDrawPadRunTimeListener() {
            @Override
            public void onRunTime(DrawPad v, long currentTimeUs) {
                Log.e("LSDelete", "------currentTimeUs: " + currentTimeUs);
            }
        });



    }

    LSOVideoAsset asset1;
    LSOVideoAsset asset2;
    LSOVideoAsset asset3;
    /**
     * 开始容器
     */
    private void startDrawPad() throws  Exception{
        drawPadView.pauseDrawPad();
        if (!drawPadView.isRunning() && drawPadView.startDrawPad()) {


            //lsdelete;
            asset1=new LSOVideoAsset("/sdcard/d1.mp4");
            asset2=new LSOVideoAsset("/sdcard/TEST_VIDEO/v1920x1088_90du.mp4");
            asset3=new LSOVideoAsset("/sdcard/TEST_VIDEO/gasha_1280x720.mp4");


            videoLayer = drawPadView.addVideoLayer2(asset1,0,asset1.getDurationUs());

            long startTimeUs=asset1.getDurationUs();
            long endTimeUs=(asset1.getDurationUs() + asset2.getDurationUs());

            drawPadView.addVideoLayer2(asset2,startTimeUs,endTimeUs);

            startTimeUs=endTimeUs;
            endTimeUs=endTimeUs+ asset3.getDurationUs();

            drawPadView.addVideoLayer2(asset3,startTimeUs,endTimeUs);


            drawPadView.resumeDrawPad();

            drawPadView.setLoopingWhenReachTime(endTimeUs);
        }
    }





    private void stopDrawPad() {
        if (drawPadView != null && drawPadView.isRunning()) {
            drawPadView.stopDrawPad();
            DemoUtil.showToast(getApplicationContext(), "录制已停止!!");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (drawPadView != null) {
            drawPadView.stopDrawPad();
        }
    }

    /**
     * 删除最终的几个文件s
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    DrawPadAllExecute2 allExecute;
    private void startExport() throws Exception {
        if(drawPadView!=null){
            drawPadView.stopDrawPad();

        }


        allExecute = new DrawPadAllExecute2(getApplicationContext(), 720, 1280, asset1.getDurationUs()+ asset2.getDurationUs()+ asset3.getDurationUs());
        allExecute.setOnLanSongSDKProgressListener(new OnLanSongSDKProgressListener() {
            @Override
            public void onLanSongSDKProgress(long ptsUs, int percent) {

                DemoProgressDialog.showPercent(test3VideoPreviewActivity.this,percent);
            }
        });
        allExecute.setOnLanSongSDKErrorListener(new OnLanSongSDKErrorListener() {
            @Override
            public void onLanSongSDKError(int errorCode) {

            }
        });
        allExecute.setOnLanSongSDKCompletedListener(new OnLanSongSDKCompletedListener() {
            @Override
            public void onLanSongSDKCompleted(String dstVideo) {
                DemoProgressDialog.releaseDialog();
                DemoUtil.startPreviewVideo(test3VideoPreviewActivity.this,dstVideo);
            }
        });


        VideoFrameLayer videoFrameLayer=allExecute.addVideoLayer(new LSOVideoOption(asset1.getVideoPath()),0,asset1.getDurationUs(),false,false);
        videoFrameLayer.setScaleType(LSOScaleType.VIDEO_SCALE_TYPE);

        long startTimeUs=asset1.getDurationUs();
        long endTimeUs=(asset1.getDurationUs() + asset2.getDurationUs());

        VideoFrameLayer videoFrameLayer2=allExecute.addVideoLayer(new LSOVideoOption(asset2.getVideoPath()),startTimeUs,endTimeUs,false,false);
        videoFrameLayer2.setScaleType(LSOScaleType.VIDEO_SCALE_TYPE);

        startTimeUs=endTimeUs;
        endTimeUs=endTimeUs+ asset3.getDurationUs();

        VideoFrameLayer videoFrameLayer3=allExecute.addVideoLayer(new LSOVideoOption(asset3.getVideoPath()),startTimeUs,endTimeUs,false,false);
        videoFrameLayer3.setScaleType(LSOScaleType.VIDEO_SCALE_TYPE);

        allExecute.start();

    }
}
