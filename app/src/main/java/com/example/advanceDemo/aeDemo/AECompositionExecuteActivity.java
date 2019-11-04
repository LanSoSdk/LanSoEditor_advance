package com.example.advanceDemo.aeDemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.example.advanceDemo.utils.DemoLog;
import com.example.advanceDemo.utils.DemoProgressDialog;
import com.example.advanceDemo.utils.DemoUtil;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.LanSongAe.LSOAeDrawable;
import com.lansosdk.LanSongAe.LSOLoadAeJsons;
import com.lansosdk.LanSongAe.OnLSOAeJsonLoadedListener;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.LSOLayerPosition;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKExportProgressListener;
import com.lansosdk.videoeditor.AECompositionExecute;
import com.lansosdk.videoeditor.AECompositionView;

import java.io.IOException;
import java.util.Arrays;


public class AECompositionExecuteActivity extends Activity {

    AECompositionExecute aeExecute;
    int inputType = 0;
    //各种资源;
    AEDemoAsset demoAsset;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.ae_only_export_hint_layout);

        inputType = getIntent().getIntExtra("AEType", AEDemoAsset.AE_DEMO_NONE);
        //创建素材;
        demoAsset =new AEDemoAsset(getApplicationContext(),inputType);


        findViewById(R.id.id_only_ae_export_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parseAeJson();
            }
        });
    }

    /**
     * 解析AE中的json
     */
    private void parseAeJson(){
        if (demoAsset.json1Path != null) {

            //第一步:拿到各种json对象
            LSOLoadAeJsons.loadAsync(getApplicationContext(), new String[]{demoAsset.json1Path, demoAsset.json2Path}, new OnLSOAeJsonLoadedListener() {
                @Override
                public void onCompositionsLoaded(@Nullable LSOAeDrawable[] drawables) {
                    if (drawables != null && drawables.length > 0) {
                        demoAsset.drawable1 = drawables[0];

                        AECompositionView.printDrawableInfo(demoAsset.drawable1);
                        if(drawables.length>1){
                            demoAsset.drawable2=drawables[1];
                        }
                        //第二步替换资源;
                        demoAsset.replaceJsonAsset();
                        startAEExport();
                    }
                }
            });
        } else {
            DemoUtil.showDialog(AECompositionExecuteActivity.this, "没有json文件, 无法加载");
        }
    }
    boolean isRunning;

    /**
     * 开始预览.
     */
    private void startAEExport() {


        if(isRunning){
            return;
        }

        isRunning=true;

        DemoProgressDialog.showPercent(AECompositionExecuteActivity.this,0);
        aeExecute=new AECompositionExecute(getApplicationContext());

        //第一层:视频层
        if (demoAsset.bgVideo != null) {
            try {
                aeExecute.addFirstLayer(demoAsset.bgVideo);
            } catch (IOException e) {
                DemoLog.e("ae preview add videolayer  error. ", e);
                e.printStackTrace();
            }
        }

        //第二层:json层;
        if (demoAsset.drawable1 != null) {
            if(inputType== AEDemoAsset.AE_DEMO_JSON_CUT) {  //json裁剪
                aeExecute.addSecondLayer(demoAsset.drawable1, demoAsset.startFrameIndex, demoAsset.endFrameIndex);
            }else  if(inputType == AEDemoAsset.AE_DEMO_JSON_CONCAT){  //json拼接;
                aeExecute.addSecondLayer(Arrays.asList(demoAsset.drawable1,demoAsset.drawable2));  //两个json拼接.
            }else {
                aeExecute.addSecondLayer(demoAsset.drawable1);
            }
        }

        //第三层:mv图层;
        if (demoAsset.mvColorPath1 != null && demoAsset.mvMaskPath1 != null) {
            aeExecute.addThirdLayer(demoAsset.mvColorPath1, demoAsset.mvMaskPath1);
        }


        //第四层:json图层[通常用不到]
        if (demoAsset.drawable2 != null && inputType!= AEDemoAsset.AE_DEMO_JSON_CONCAT) {
            aeExecute.addForthLayer(demoAsset.drawable2);
        }

        //第五层:mv层;[通常用不到]
        if (demoAsset.mvColorPath2 != null && demoAsset.mvMaskPath2!=null) {
            aeExecute.addFifthLayer(demoAsset.mvColorPath2,demoAsset.mvMaskPath2);
        }


        //增加其他音频;
        if (demoAsset.audioAsset != null) {
            aeExecute.addAudioLayer(demoAsset.audioAsset);
        }


        aeExecute.setOnLanSongSDKCompletedListener(new OnLanSongSDKCompletedListener() {
            @Override
            public void onLanSongSDKCompleted(String dstVideo) {
                DemoProgressDialog.releaseDialog();

                isRunning=false;
                if(aeExecute!=null){
                    aeExecute.release();
                    aeExecute=null;
                }
                DemoUtil.playDstVideo(AECompositionExecuteActivity.this, dstVideo);
            }
        });
        aeExecute.setOnLanSongSDKExportProgressListener(new OnLanSongSDKExportProgressListener() {
            @Override
            public void onLanSongSDKExportProgress(long ptsUs, int percent) {
               DemoProgressDialog.showPercent(AECompositionExecuteActivity.this,percent);
            }
        });

        aeExecute.setOnLanSongSDKErrorListener(new OnLanSongSDKErrorListener() {
            @Override
            public void onLanSongSDKError(int errorCode) {
                if(aeExecute!=null){
                    aeExecute.cancel();
                    aeExecute=null;
                }
                isRunning=false;
                DemoProgressDialog.releaseDialog();
                DemoUtil.showDialog(AECompositionExecuteActivity.this, "AE执行错误,请查看错误信息.我们的TAG是LanSongSDK.");
            }
        });

        //增加图标演示
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        BitmapLayer bmpLayer = aeExecute.addBitmapLayer(bmp);
        bmpLayer.setScale(1.0f);
        bmpLayer.setPosition(LSOLayerPosition.RightTop);

        //开始执行
        if (aeExecute.startExport()) {
            DemoLog.d(" ae preview is running.");
            isRunning=true;
        } else {
            isRunning=false;
            DemoUtil.showDialog(AECompositionExecuteActivity.this, "AE预览开启失败.");
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        isRunning=false;
        if(aeExecute!=null){
            aeExecute.cancel();
            aeExecute=null;
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
