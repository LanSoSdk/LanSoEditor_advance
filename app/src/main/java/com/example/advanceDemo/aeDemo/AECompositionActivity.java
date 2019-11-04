package com.example.advanceDemo.aeDemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.example.advanceDemo.utils.CopyFileFromAssets;
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
import com.lansosdk.box.OnLanSongSDKPreviewBufferingListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.AECompositionView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;


public class AECompositionActivity extends Activity {

    AECompositionView aeCompositionView;
    int inputType = 0;
    DemoProgressDialog progressDialog;
    //各种资源;
    AEDemoAsset demoAsset;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.ae_composition_layout);
        aeCompositionView = findViewById(R.id.id_ae_preview2);
        initView();

        inputType = getIntent().getIntExtra("AEType", AEDemoAsset.AE_DEMO_NONE);

        //创建素材;
        demoAsset =new AEDemoAsset(getApplicationContext(),inputType);

        if (demoAsset.json1Path != null) {
            //第一步:拿到各种json对象
            //这里传递是字符串文件路径, 如果你要加密json,则用InpuStream[]类型的loadAsync;
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

                        //第三步:根据Ae模板的宽高来重新布局预览界面的宽高,开始预览;
                        aeCompositionView.setDrawPadSize(demoAsset.drawable1.getJsonWidth(), demoAsset.drawable1.getJsonHeight(), new onDrawPadSizeChangedListener() {
                            @Override
                            public void onSizeChanged(int viewWidth, int viewHeight) {
                                startAEPreview();
                            }
                        });
                    }
                }
            });
        } else {
            DemoUtil.showDialog(AECompositionActivity.this, "没有json文件, 无法加载");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //当播放完毕进去下一个界面,再次返回的时候,让它直接播放;
        aeCompositionView.setOnViewAvailable(new AECompositionView.onViewAvailable() {
            @Override
            public void viewAvailable(AECompositionView v) {
                startAEPreview();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        aeCompositionView.cancel();
    }

    /**
     * 开始预览.
     */
    private void startAEPreview() {
        if (aeCompositionView.isPlaying()) {
            return;
        }

        //第一层:视频层
        if (demoAsset.bgVideo != null) {
            try {
                aeCompositionView.addFirstLayer(demoAsset.bgVideo);
            } catch (IOException e) {
                DemoLog.e("ae preview add videolayer  error. ", e);
                e.printStackTrace();
            }
        }

        //第二层:json层;
        if (demoAsset.drawable1 != null) {
            if(inputType==AEDemoAsset.AE_DEMO_JSON_CUT) {  //json裁剪
                aeCompositionView.addSecondLayer(demoAsset.drawable1, demoAsset.startFrameIndex, demoAsset.endFrameIndex);
            }else  if(inputType ==AEDemoAsset.AE_DEMO_JSON_CONCAT){  //json拼接;
                aeCompositionView.addSecondLayer(Arrays.asList(demoAsset.drawable1,demoAsset.drawable2));  //两个json拼接.
            }else {
                aeCompositionView.addSecondLayer(demoAsset.drawable1);
            }
        }

        //第三层:mv图层;
        if (demoAsset.mvColorPath1 != null && demoAsset.mvMaskPath1 != null) {
            aeCompositionView.addThirdLayer(demoAsset.mvColorPath1, demoAsset.mvMaskPath1);
        }


        //第四层:json图层[通常用不到]
        if (demoAsset.drawable2 != null && inputType!=AEDemoAsset.AE_DEMO_JSON_CONCAT) {
            aeCompositionView.addForthLayer(demoAsset.drawable2);
        }

        //第五层:mv层;[通常用不到]
        if (demoAsset.mvColorPath2 != null && demoAsset.mvMaskPath2!=null) {
            aeCompositionView.addFifthLayer(demoAsset.mvColorPath2,demoAsset.mvMaskPath2);
        }

        //增加其他音频;
        if (demoAsset.audioAsset != null) {
            aeCompositionView.addAeModuleAudio(demoAsset.audioAsset);  //<--------注意,这里更新为单独增加Ae的音乐;
        }

        aeCompositionView.setPreviewLooping(true);  //循环播放;
        aeCompositionView.setOnLanSongSDKCompletedListener(new OnLanSongSDKCompletedListener() {
            @Override
            public void onLanSongSDKCompleted(String dstVideo) {

                hideProgressDialog();
                if (aeCompositionView.isExportRunning()) {
                    DemoUtil.playDstVideo(AECompositionActivity.this, dstVideo);
                } else {
                    DemoUtil.showDialog(AECompositionActivity.this, "预览完成.");
                }
            }
        });
        aeCompositionView.setOnLanSongSDKProgressListener(new OnLanSongSDKProgressListener() {
            @Override
            public void onLanSongSDKProgress(long ptsUs, int percent) {
            }
        });

        aeCompositionView.setOnLanSongSDKExportProgressListener(new OnLanSongSDKExportProgressListener() {
            @Override
            public void onLanSongSDKExportProgress(long ptsUs, int percent) {
                DemoLog.e("----ptsUs: "+ptsUs+ " percent is :"+percent);
                if (progressDialog != null) {
                    progressDialog.setProgress(percent);
                }
            }
        });

        aeCompositionView.setOnLanSongSDKPreviewBufferingListener(new OnLanSongSDKPreviewBufferingListener() {
            @Override
            public void onLanSongSDKBuffering(boolean buffering) {
                DemoProgressDialog.showBufferingHint(AECompositionActivity.this,buffering);
            }
        });
        aeCompositionView.setOnLanSongSDKErrorListener(new OnLanSongSDKErrorListener() {
            @Override
            public void onLanSongSDKError(int errorCode) {
                aeCompositionView.release();
                hideProgressDialog();
                DemoUtil.showDialog(AECompositionActivity.this, "AE执行错误,请查看错误信息.我们的TAG是LanSongSDK.");
            }
        });

//        //增加图标演示
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ls_logo);
        BitmapLayer bmpLayer = aeCompositionView.addBitmapLayer(bmp);
        bmpLayer.setPosition(LSOLayerPosition.RightTop);
        //开始执行



        if (aeCompositionView.isLayoutValid() && aeCompositionView.startPreview()) {
            DemoLog.d(" AECompositionView is running.");
        } else {
            DemoUtil.showDialog(AECompositionActivity.this, "AE预览开启失败.");
        }
    }
    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.release();
            progressDialog = null;
        }
    }

    private void startDstVideoPreview(String path) {

        hideProgressDialog();
        DemoUtil.playDstVideo(AECompositionActivity.this, path);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        aeCompositionView.release();
    }
    private void initView(){
        findViewById(R.id.id_ae_preview_replace_picture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DemoUtil.showDialog(AECompositionActivity.this, "请直接在AEPreviewActivity.java中替换.");
            }
        });

        findViewById(R.id.id_ae_preview_pause).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(aeCompositionView !=null){
                    aeCompositionView.pausePreview();
                }
            }
        });
        findViewById(R.id.id_ae_preview_resume).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(aeCompositionView !=null){
                    aeCompositionView.resumePreview();
                }
            }
        });

        findViewById(R.id.id_ae_preview_export_ae).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (aeCompositionView != null) {
                    progressDialog = new DemoProgressDialog();
                    progressDialog.show(AECompositionActivity.this);
                    boolean ret = aeCompositionView.startExport();
                    if (!ret) {
                        DemoUtil.showDialog(AECompositionActivity.this, "导出执行失败,请查看打印信息.");
                    }
                }
            }
        });
    }
}
