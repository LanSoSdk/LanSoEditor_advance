package com.example.advanceDemo.layerDemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.example.advanceDemo.utils.DemoProgressDialog;
import com.example.advanceDemo.utils.DemoUtil;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.LanSongAe.LSOAeDrawable;
import com.lansosdk.LanSongAe.LSOLoadAeJsons;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.DrawPadUpdateMode;
import com.lansosdk.box.LSOAECompositionLayer;
import com.lansosdk.box.LSOAeCompositionAsset;
import com.lansosdk.box.LSOPhotoAlbumAsset;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.onDrawPadRunTimeListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.DrawPadAllExecute2;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.DrawPadView.onViewAvailable;
import com.lansosdk.videoeditor.MediaInfo;

import java.util.ArrayList;
import java.util.List;

import static com.example.advanceDemo.utils.CopyFileFromAssets.copyAeAssets;
import static com.example.advanceDemo.utils.CopyFileFromAssets.copyShanChu;


public class PhotoAlbumLayerDemoActivity extends Activity {
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
                    DemoUtil.showDialog(PhotoAlbumLayerDemoActivity.this,"运行抛出错误信息, 请查看logcat");
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

    LSOPhotoAlbumAsset albumAsset;
    /**
     * Step1: 初始化DrawPad
     */
    private void initDrawPad() throws Exception {

        String jsonPath =copyAeAssets(getApplicationContext(),"morePicture.json");

        List<Bitmap> bitmaps=new ArrayList<>();
        for (int i = 0; i <10;i++) {
            String name = "morePicture_img_" + i + ".jpeg";
            bitmaps.add(BitmapFactory.decodeFile(copyAeAssets(getApplicationContext(),name)));
        }

        albumAsset=new LSOPhotoAlbumAsset(bitmaps,jsonPath,true);

        drawPadView.setBackgroundColor(Color.RED);
        drawPadView.setUpdateMode(DrawPadUpdateMode.AUTO_FLUSH, (int) albumAsset.getFrameRate());

        //设置在两个都走完的时候, 循环.
        drawPadView.setLoopingWhenReachTime(albumAsset.getDurationUs());

        drawPadView.setOnDrawPadRunTimeListener(new onDrawPadRunTimeListener() {
            @Override
            public void onRunTime(DrawPad v, long currentTimeUs) {
            }
        });

        drawPadView.setDrawPadSize(albumAsset.getWidth(), albumAsset.getHeight(), new onDrawPadSizeChangedListener() {
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

            drawPadView.addPhotoAlbumLayer(albumAsset);

            drawPadView.resumeDrawPad();
        }
    }



    DrawPadAllExecute2 allExecute;

    private void startExport() throws Exception{

        if(albumAsset==null ){
            return;
        }
        if(drawPadView!=null){
            drawPadView.stopDrawPad();
        }

        allExecute = new DrawPadAllExecute2(getApplicationContext(), albumAsset.getWidth(), albumAsset.getHeight(), albumAsset.getDurationUs());
        allExecute.addPhotoAlbumLayer(albumAsset);

        allExecute.setOnLanSongSDKProgressListener(new OnLanSongSDKProgressListener() {
            @Override
            public void onLanSongSDKProgress(long ptsUs, int percent) {
                Log.e("TAG", "------ptsUs: "+ptsUs+ " percent :"+percent);
                DemoProgressDialog.showPercent(PhotoAlbumLayerDemoActivity.this,percent);
            }
        });

        allExecute.setOnLanSongSDKCompletedListener(new OnLanSongSDKCompletedListener() {
            @Override
            public void onLanSongSDKCompleted(String dstVideo) {
                DemoProgressDialog.releaseDialog();
                DemoUtil.startPreviewVideo(PhotoAlbumLayerDemoActivity.this,dstVideo);
            }
        });

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
        if(albumAsset!=null){
            albumAsset.release();
            albumAsset=null;
        }
    }
}
