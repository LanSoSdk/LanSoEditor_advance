package com.example.advanceDemo.aeDemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.example.advanceDemo.utils.DemoLog;
import com.example.advanceDemo.utils.DemoProgressDialog;
import com.example.advanceDemo.utils.DemoUtil;
import com.example.advanceDemo.view.RecyclerViewAdapter;
import com.example.advanceDemo.view.SpacesItemDecoration;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.LanSongAe.LSOAeDrawable;
import com.lansosdk.LanSongAe.LSOAeText;
import com.lansosdk.LanSongAe.LSOLoadAeJsons;
import com.lansosdk.LanSongAe.OnLSOAeJsonLoadedListener;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.LSOLayerPosition;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKExportProgressListener;
import com.lansosdk.box.OnLanSongSDKPreviewBufferingListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.OnLanSongSDKRenderProgressListener;
import com.lansosdk.box.OnLanSongSDKThumbnailListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.AECompositionView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class AECompositionActivity extends Activity {

    AECompositionView aeCompositionView;
    int inputType = 0;
    DemoProgressDialog progressDialog;
    //各种资源;
    AEDemoAsset demoAsset;
    ProgressBar progressBar;
    private SeekBar seekLineView;
    private RelativeLayout relativeLayout;

    private RecyclerView thumbnailRecyclerView;
    private LinearLayoutManager layoutManager;
    private List<Bitmap> thumbnailList;
    private RecyclerViewAdapter thumbnailListAdapter;

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
            //这里传递是字符串文件路径, 如果你要加密json,则用InpuStream[]类型的loadAsync;.

            LSOLoadAeJsons.loadAsync(getApplicationContext(), new String[]{demoAsset.json1Path, demoAsset.json2Path}, new OnLSOAeJsonLoadedListener() {
                @Override
                public void onCompositionsLoaded(@Nullable LSOAeDrawable[] drawables) {
                    if (drawables != null && drawables.length > 0) {
                        demoAsset.drawable1 = drawables[0];

                        AECompositionView.printDrawableInfo(demoAsset.drawable1);
                        if(drawables.length>1){
                            demoAsset.drawable2=drawables[1];
                        }
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

    private void startAEPreview() {
        if (aeCompositionView.isRunning()) {
            return;
        }
        demoAsset.waitForCopyCompleted(new AEDemoAsset.OnAeAssetCopyedListener() {
            @Override
            public void onCopyed() {
                startAEPreview2();
            }
        });
    }

    /**
     * 把导出的每一层数据, 加载到Ae容器里, 设置各种进度,完成监听,开始预览
     *
     */
    private void startAEPreview2() {

        if (aeCompositionView.isRunning()) {
            return;
        }
        demoAsset.replaceJsonAsset();

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


        int index=0;
        List<LSOAeText> texts = demoAsset.drawable1.getJsonTexts();
        for(LSOAeText text: texts){

            if(index++==0){
                demoAsset.drawable1.updateTextWithJsonText(text.text,text.text);
            }else{
                demoAsset.drawable1.updateTextWithJsonText(text.text,text.text);
            }
        }




//        AudioLayer layer=aeCompositionView.addAudioLayer(copyAssets(getApplicationContext(),"hongdou10s.mp3"),true);
//        if(layer!=null){
//            layer.setVolume(0.5f);
//        }
//        AudioLayer layer1=aeCompositionView.getAEAudioLayer();
//        if(layer1!=null){
//            layer1.setVolume(0.5f);
//        }
//        aeCompositionView.setPreviewVolume(0.5f);


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
        //播放进度监听
        aeCompositionView.setOnLanSongSDKProgressListener(new OnLanSongSDKProgressListener() {
            @Override
            public void onLanSongSDKProgress(long ptsUs, int percent) {
                progressBar.setProgress(percent);
                if(seekLineView!=null){
                    seekLineView.setProgress(percent);
                }
            }
        });

        //导出进度;
        aeCompositionView.setOnLanSongSDKExportProgressListener(new OnLanSongSDKExportProgressListener() {
            @Override
            public void onLanSongSDKExportProgress(long ptsUs, int percent) {
                if (progressDialog != null) {
                    progressDialog.setProgress(percent);
                }
            }
        });

        //缓冲回调...
        aeCompositionView.setOnLanSongSDKPreviewBufferingListener(new OnLanSongSDKPreviewBufferingListener() {
            @Override
            public void onLanSongSDKBuffering(boolean buffering) {
                DemoProgressDialog.showBufferingHint(AECompositionActivity.this,"底层正在加速渲染中...",buffering);
            }
        });
        //错误监听;
        aeCompositionView.setOnLanSongSDKErrorListener(new OnLanSongSDKErrorListener() {
            @Override
            public void onLanSongSDKError(int errorCode) {
                aeCompositionView.release();
                hideProgressDialog();
                DemoUtil.showDialog(AECompositionActivity.this, "AE执行错误,请查看错误信息.我们的TAG是LanSongSDK.");
            }
        });

        //渲染监听;
        aeCompositionView.setOnLanSongSDKRenderProgressListener(new OnLanSongSDKRenderProgressListener() {
            @Override
            public void onLanSongSDKRenderProgress(long ptsUs, int percent) {
                progressBar.setSecondaryProgress(percent);
            }
        });

        //获取缩略图
        aeCompositionView.getThumbnailBitmapsAsynchronously(30, new OnLanSongSDKThumbnailListener() {
            @Override
            public void onLanSongSDKThumbnail(Bitmap bmp) {
                thumbnailList.add(bmp);
                thumbnailListAdapter.notifyDataSetChanged();
            }
        });

//        //增加图标演示
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ls_logo);
        BitmapLayer bmpLayer = aeCompositionView.addLogoLayer(bmp, LSOLayerPosition.Center);
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
    @SuppressLint("ClickableViewAccessibility")
    private void initView() {
        progressBar=(ProgressBar)findViewById(R.id.id_ae_progressbar);

        findViewById(R.id.id_ae_preview_replace_picture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DemoUtil.showDialog(AECompositionActivity.this, "请直接在AEPreviewActivity.java中替换.");
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
        findViewById(R.id.id_ae_preview_resume).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(aeCompositionView.isPlaying()){
                    aeCompositionView.pausePreview();
                }else{
                    aeCompositionView.resumePreview();
                }
            }
        });
        initSeekLine();
        initThumbnailRecyclerView();

    }
    float seekLinePercent;
    @SuppressLint("ClickableViewAccessibility")
    private void initSeekLine(){
        seekLineView = findViewById(R.id.id_ae_preview_seek_line);
        seekLineView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if(fromUser){
                    aeCompositionView.seekToTimeUs((long) (aeCompositionView.getAeDurationUs() * progress/100f));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                aeCompositionView.setSeekMode(true);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                aeCompositionView.setSeekMode(false);
            }
        });
    }


    private void initThumbnailRecyclerView() {
        thumbnailList = new ArrayList<>();
        thumbnailRecyclerView = findViewById(R.id.id_ae_thumbnail_recycle_view);
        thumbnailListAdapter = new RecyclerViewAdapter(R.layout.start_recycler_item, thumbnailList);
        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false) {
            @Override
            public boolean canScrollHorizontally() {
                return false;
            }
        };
        thumbnailRecyclerView.setLayoutManager(layoutManager);
        HashMap<String, Integer> map = new HashMap<>();
        map.put(SpacesItemDecoration.LEFT_DECORATION, 0);
        thumbnailRecyclerView.addItemDecoration(new SpacesItemDecoration(map));
        thumbnailRecyclerView.setAdapter(thumbnailListAdapter);
    }





}
