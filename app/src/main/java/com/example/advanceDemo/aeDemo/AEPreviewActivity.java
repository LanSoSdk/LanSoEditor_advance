package com.example.advanceDemo.aeDemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
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
import com.lansosdk.box.OnAERenderCompletedListener;
import com.lansosdk.box.OnAERenderErrorListener;
import com.lansosdk.box.OnAERenderProgressListener;
import com.lansosdk.box.OnAudioPadExecuteCompletedListener;
import com.lansosdk.box.OnDrawPadCancelAsyncListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.AERenderExecute;
import com.lansosdk.videoeditor.AudioPadExecute;
import com.lansosdk.videoeditor.DrawPadAEPreview;
import com.lansosdk.videoeditor.DrawPadAEPreview.OnAePreviewCompletedListener;
import com.lansosdk.videoeditor.DrawPadAEPreview.OnAePreviewErrorListener;
import com.lansosdk.videoeditor.DrawPadAEPreview.OnAePreviewProgressListener;
import com.lansosdk.videoeditor.DrawPadAEPreview.onViewAvailable;
import com.lansosdk.videoeditor.LanSongFileUtil;

import java.io.IOException;

public class AEPreviewActivity extends Activity {





    AEDemoAsset demoAsset;
    DrawPadAEPreview aePreview;

    int inputType = 0;
    DemoProgressDialog progressDialog;

    //-------------后台的类
    AERenderExecute aeRenderExecute;
    private AudioPadExecute audioExecute;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.ae_preview_layout);
        aePreview = findViewById(R.id.id_ae_preview2);
        findViewById(R.id.id_ae_preview_replace_picture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DemoUtil.showDialog(AEPreviewActivity.this, "请直接在AEPreviewActivity.java中替换.");
            }
        });
        findViewById(R.id.id_ae_preview_export_ae).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAEExecute();
            }
        });


        inputType = getIntent().getIntExtra("AEType", AEDemoAsset.AE_DEMO_NONE);

        inputType=AEDemoAsset.AE_DEMO_HAOKAN;


        demoAsset=new AEDemoAsset(getApplicationContext(),inputType);



        if (demoAsset.json1Path != null) {
            LSOLoadAeJsons.loadAsync(getApplicationContext(), new String[]{demoAsset.json1Path, demoAsset.json2Path}, new OnLSOAeJsonLoadedListener() {
                @Override
                public void onCompositionsLoaded(@Nullable LSOAeDrawable[] drawables) {
                    if (drawables != null && drawables.length > 0) {
                        demoAsset.drawable1 = drawables[0];

                        //有第二个json
                        if (drawables.length > 1) {
                            demoAsset.drawable2 = drawables[1];
                        }
                        DrawPadAEPreview.printDrawableInfo(demoAsset.drawable1);
                        DrawPadAEPreview.printDrawableInfo(demoAsset.drawable2);


                        demoAsset.replaceJsonAsset();

                        //根据Ae模板的宽高来重新布局预览界面的宽高;
                        aePreview.setDrawPadSize(demoAsset.drawable1.getJsonWidth(), demoAsset.drawable1.getJsonHeight(), new onDrawPadSizeChangedListener() {
                            @Override
                            public void onSizeChanged(int viewWidth, int viewHeight) {
                                startAEPreview();
                            }
                        });
                    }
                }
            });
        } else {
            DemoUtil.showDialog(AEPreviewActivity.this, "没有json文件, 无法加载");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //当播放完毕进去下一个界面,再次返回的时候,让它直接播放;
        aePreview.setOnViewAvailable(new onViewAvailable() {
            @Override
            public void viewAvailable(DrawPadAEPreview v) {
                startAEPreview();
            }
        });
    }



    @Override
    protected void onPause() {
        super.onPause();
        aePreview.cancelWithAsync(new OnDrawPadCancelAsyncListener() {
            @Override
            public void onCanceled() {
                Log.e("TAG", "-------已经取消....: ");
            }
        });
    }

    //给json替换各种资源; 这里只有一个json; 故只有一个drawable1
    private void startAEPreview() {
        if (aePreview.isPlaying()) {
            return;
        }
        //取消上一个
        aePreview.cancelWithAsync(null);

        // 在您从AE工程里中导出的时候, 是分别导出多个文件的,
        // 我们因为事先知道哪个在哪一层. 就没有写一个json来管理这些层.
        // 实际如果您多层文件,是需要写一个json来管理这些层.让开发端知道哪个层,先add,哪个后add
        //当然如果仅仅是3层, 也可以通过文件的名字来区分.
        //----------------正式开始:
        //增加视频图层
        if (demoAsset.bgVideo != null) {
            try {
                aePreview.addVideoLayer(demoAsset.bgVideo);
            } catch (IOException e) {
                DemoLog.e("ae preview add videolayer  error. ", e);
                e.printStackTrace();
            }
        }

        //增加json图层
        if (demoAsset.drawable1 != null) {
            aePreview.addAeLayer(demoAsset.drawable1);
        }

        //增加mv图层;
        if (demoAsset.mvColorPath1 != null && demoAsset.mvMaskPath1 != null) {
            aePreview.addMVLayer(demoAsset.mvColorPath1, demoAsset.mvMaskPath1);
        }

        if (demoAsset.drawable2 != null) {
            aePreview.addAeLayer(demoAsset.drawable2);
        }

        //增加音频层
        if (demoAsset.audioPath != null) {
            aePreview.addAudioLayer(demoAsset.audioPath);
        }

        //设置监听
        aePreview.setOnAePreviewProgressListener(new OnAePreviewProgressListener() {
            @Override
            public void aePreviewProgress(int percent) {
            }
        });
        aePreview.setOnAePreviewCompletedListener(new OnAePreviewCompletedListener() {
            @Override
            public void aePreviewCompleted() {
                DemoUtil.showDialog(AEPreviewActivity.this, "播放完毕");
            }
        });
        aePreview.setOnAePreviewErrorListener(new OnAePreviewErrorListener() {
            @Override
            public void aePreviewError() {
                DemoUtil.showDialog(AEPreviewActivity.this, "播放错误,请把所有logcat打印信息发给我们.");
            }
        });


        //增加图标演示
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        BitmapLayer bmpLayer = aePreview.addBitmapLayer(bmp);


        aePreview.setbackgroundColor(1.0f, 0.0f, 0.0f);
        //开始执行
        if (aePreview.isLayoutValid() && aePreview.start()) {
            bmpLayer.setScale(1.0f);
            bmpLayer.setPosition(LSOLayerPosition.RightTop);
        } else {
            DemoUtil.showDialog(AEPreviewActivity.this, "AE预览开启失败.");
        }
    }

    /**
     * AE的后台执行(类似导出操作);
     */
    public void startAEExecute() {
        if (aeRenderExecute != null) {
            DemoUtil.showDialog(AEPreviewActivity.this, "AE正在执行...!");
            return;
        }

        progressDialog = new DemoProgressDialog();
        progressDialog.show(AEPreviewActivity.this);

        if (aePreview.isPlaying()) {
            aePreview.cancelWithAsync(null);
        }


        aeRenderExecute = new AERenderExecute(getApplication());

        //增加一个背景视频层
        if (demoAsset.bgVideo != null) {
            aeRenderExecute.addVideoLayer(demoAsset.bgVideo);
        }

        //增加json层
        if (demoAsset.drawable1 != null) {
            aeRenderExecute.addAeLayer(demoAsset.drawable1);  //因为drawable1在预览的时候, 所有的素材已经替换到drawable1对象里了, 这里不再替换.
            for (String key : demoAsset.json1ReplaceVideos.keySet()) {
                demoAsset.drawable1.updateVideoBitmap(key, demoAsset.json1ReplaceVideos.get(key));  //视频需要重新替换一次;
            }
        }

        //增加mv层
        if (demoAsset.mvColorPath1 != null && demoAsset.mvMaskPath1 != null) {
            aeRenderExecute.addMVLayer(demoAsset.mvColorPath1, demoAsset.mvMaskPath1);
        }

        if (demoAsset.drawable2 != null) {
            aeRenderExecute.addAeLayer(demoAsset.drawable2);
            for (String key : demoAsset.json2ReplaceVideos.keySet()) {
                demoAsset.drawable2.updateVideoBitmap(key, demoAsset.json2ReplaceVideos.get(key));  //视频需要重新替换一次;
            }
        }


        //如果有额外的音频,增加音频层
        if (demoAsset.audioPath != null) {
            aeRenderExecute.addAudioLayer(demoAsset.audioPath);
        }


        //设置监听
        aeRenderExecute.setOnAERenderProgressListener(new OnAERenderProgressListener() {
            @Override
            public void onProgress(long timeUs, int percent) {
                if (progressDialog != null) {
                    progressDialog.setProgress(percent);
                }
            }
        });

        aeRenderExecute.setOnAERenderCompletedListener(new OnAERenderCompletedListener() {
            @Override
            public void onCompleted(String dstPath) {
                hideProgressDialog();
                startDstVideoPreview(dstPath);
            }
        });
        aeRenderExecute.setOnAERenderErrorListener(new OnAERenderErrorListener() {
            @Override
            public void onError(String error) {
                hideProgressDialog();
                releaseAE();
            }
        });
        //增加logo
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        BitmapLayer logoLayer = aeRenderExecute.addLogoLayer(bmp, LSOLayerPosition.RightTop);

        //开始执行
        if (!aeRenderExecute.start()) {
            hideProgressDialog();
            DemoUtil.showDialog(AEPreviewActivity.this, "AE合成错误,请联系我们!");
        } else {
            //因为预览容器是屏幕大小.这里等比例缩放;
            float scaleWidth = bmp.getWidth() * aeRenderExecute.getWidth() * 1.0f / (float) aePreview.getDrawPadWidth();
            float scaleHeight = bmp.getHeight() * aeRenderExecute.getHeight() * 1.0f / (float) aePreview.getDrawPadHeight();

            logoLayer.setScaledValue(scaleWidth, scaleHeight);
            logoLayer.setPosition(LSOLayerPosition.RightTop);
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.release();
            progressDialog = null;
        }
    }

    private void startDstVideoPreview(String path) {
        if (demoAsset.audioPath != null) {
            try {
                addAudio(path);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } else {
            hideProgressDialog();
            DemoUtil.playDstVideo(AEPreviewActivity.this, path);
        }
    }

    private void releaseAE() {
        if (aeRenderExecute != null) {
            aeRenderExecute.release();
            aeRenderExecute = null;
        }
    }

    //增加音频;
    private void addAudio(final String srcPath) throws Exception {
        audioExecute = new AudioPadExecute(getApplication(), srcPath);
        audioExecute.addAudioLayer(demoAsset.audioPath, true, 1.0f);
        audioExecute.setOnAudioPadCompletedListener(new OnAudioPadExecuteCompletedListener() {
            @Override
            public void onCompleted(String path) {

                audioExecute.release();
                audioExecute = null;

                hideProgressDialog();
                DemoUtil.playDstVideo(AEPreviewActivity.this, path);

                LanSongFileUtil.deleteFile(srcPath);
            }
        });
        audioExecute.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    //--------------- 一下是测试.
    public void testJson() {  //只需要填写素材即可.
    }
}
