package com.example.advanceDemo.layerDemo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.lansoeditor.advanceDemo.R;
import com.lansosdk.LanSongAe.LSOAeDrawable;
import com.lansosdk.LanSongAe.LSOLoadAeJsons;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.DrawPadUpdateMode;
import com.lansosdk.box.LSOAECompositionLayer;
import com.lansosdk.box.LSOAeCompositionAsset;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.onDrawPadRunTimeListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.DrawPadAllExecute2;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.DrawPadView.onViewAvailable;
import com.lansosdk.videoeditor.MediaInfo;

import static com.example.advanceDemo.utils.CopyFileFromAssets.copyAeAssets;


public class AECompositionLayerDemoActivity extends Activity {
    boolean destroying = false;
    private DrawPadView drawPadView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ae_composition_layer_demo_layout);
        drawPadView = (DrawPadView) findViewById(R.id.DrawPad_view);
        findViewById(R.id.id_ae_composition_layer_export).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startExport();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    initDrawPad();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 300);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (drawPadView.isTextureAvailable()) {
            try {
                initDrawPad();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (drawPadView != null) {
            drawPadView.stopDrawPad();
        }
    }

    LSOAeDrawable drawable1;
    LSOAeDrawable drawable2;

    /**
     * Step1: 初始化DrawPad
     */
    private void initDrawPad() throws Exception {


        String jsonPath = copyAeAssets(getApplicationContext(), "morePicture.json");
        drawable1 = LSOLoadAeJsons.loadSync(jsonPath);
        int width = drawable1.getJsonWidth();
        int height = drawable1.getJsonHeight();


        drawable2 = LSOLoadAeJsons.loadSync(jsonPath);


        drawPadView.setBackgroundColor(Color.RED);

        drawPadView.setUpdateMode(DrawPadUpdateMode.AUTO_FLUSH, (int) drawable1.getJsonFrameRate());

        //设置在两个都走完的时候, 循环.
        drawPadView.setLoopingWhenReachTime(drawable1.getDurationUS() + drawable2.getDurationUS());

        drawPadView.setOnDrawPadRunTimeListener(new onDrawPadRunTimeListener() {
            @Override
            public void onRunTime(DrawPad v, long currentTimeUs) {
                Log.e("LSDelete", "----currentTimeUs: " + currentTimeUs);
            }
        });

        drawPadView.setDrawPadSize(width, height, new onDrawPadSizeChangedListener() {
            @Override
            public void onSizeChanged(int viewWidth, int viewHeight) {
                startDrawPad();
            }
        });
        drawPadView.setOnViewAvailable(new onViewAvailable() {
            @Override
            public void viewAvailable(DrawPadView v) {
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

            /**
             * 增加 AE 合成图层;
             */
            addAECompositionLayer();

            drawPadView.resumeDrawPad();
        }
    }

    LSOAeCompositionAsset compAsset;
    LSOAeCompositionAsset compAsset2;

    private void addAECompositionLayer() {
        for (int i = 0; i < 10; i++) {
            String key = "image_" + i;
            String name = "morePicture_img_" + i + ".jpeg";
            drawable1.updateBitmap(key, copyAeAssets(getApplicationContext(), name));
        }
        compAsset = new LSOAeCompositionAsset();
        compAsset.addSecondLayer(drawable1);
        compAsset.startAeRender();


        LSOAECompositionLayer compositionLayer = drawPadView.addAECompositionLayer(compAsset, 0, drawable1.getDurationUS());
        compositionLayer.setTAG("#合成1");


        ///------------增加第二个

        for (int i = 0; i < 10; i++) {
            String key = "image_" + i;
            String name = "morePicture_img_" + (9 - i) + ".jpeg";
            drawable2.updateBitmap(key, copyAeAssets(getApplicationContext(), name));
        }
        compAsset2 = new LSOAeCompositionAsset();
        compAsset2.addSecondLayer(drawable2);
        compAsset2.startAeRender();

        LSOAECompositionLayer compositionLayer2 = drawPadView.addAECompositionLayer(compAsset2, drawable1.getDurationUS(), drawable1.getDurationUS() + drawable2.getDurationUS());
        compositionLayer2.setTAG("#合成2");

    }

    DrawPadAllExecute2 allExecute;

    private void startExport() throws  Exception{

        if(compAsset==null && compAsset2==null){
            return;
        }
        if(drawPadView!=null){
            drawPadView.stopDrawPad();
        }

        allExecute = new DrawPadAllExecute2(getApplicationContext(),
                drawable1.getJsonWidth(), drawable1.getJsonHeight(), drawable1.getDurationUS() + drawable2.getDurationUS());
        allExecute.setOnLanSongSDKProgressListener(new OnLanSongSDKProgressListener() {
            @Override
            public void onLanSongSDKProgress(long ptsUs, int percent) {
                Log.e("TAG", "------ptsUs: " + ptsUs + " percent :" + percent);
            }
        });

        allExecute.setOnLanSongSDKCompletedListener(new OnLanSongSDKCompletedListener() {
            @Override
            public void onLanSongSDKCompleted(String dstVideo) {
                MediaInfo.checkFile(dstVideo);
            }
        });
        allExecute.addAECompositionLayer(compAsset, 0, drawable1.getDurationUS());
        allExecute.addAECompositionLayer(compAsset2, drawable1.getDurationUS(), Long.MAX_VALUE);
        allExecute.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroying = true;
        if (drawPadView != null) {
            drawPadView.stopDrawPad();
            drawPadView = null;
        }
    }
}
