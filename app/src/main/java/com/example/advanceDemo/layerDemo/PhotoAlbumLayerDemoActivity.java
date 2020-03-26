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
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.DrawPadUpdateMode;
import com.lansosdk.box.LSOPhotoAlbumAsset;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.onDrawPadRunTimeListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.DrawPadAllExecute2;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.DrawPadView.onViewAvailable;

import java.util.ArrayList;
import java.util.List;

import static com.example.advanceDemo.utils.CopyFileFromAssets.copyAeAssets;

/**
 * 增加相册影集图层, 只需要用户选择多张图片+ 一个Ae的json文件, 我们内部会自动根据图片数量来裁剪json或拼接json;

 相册影集资源类的两个参数:
 bitmaps: 多张图片列表.
 jsonPath: 用AE导出的json动画;
 LSOPhotoAlbumAsset(List<Bitmap> bitmaps, String jsonPath) throws Exception



 用AE制作动画的规则:
 1. 不能使用预合成,
 2. 每个图层对应一张图片, 不能一张图片应用到多个图层;
 3. json总时长不能超过20秒,每个图片时间建议是2--3秒,分辨率建议720x1280,帧率是20fps或15fps;
 4. 图片数量,建议不超过20张.
 5. 我们内部会根据你的图片多少,和json的时长来裁剪或拼接
 6. LSOPhotoAlbumAsset在使用完毕后,确认不再使用时, 一定要调用release释放资源,比如在让用户重新选择图片的前一行调用;
 7.演示例子,见我们的PhotoAlbumLayerDemoActivity.java

 */
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

        //准备各种资源.
        String jsonPath = copyAeAssets(getApplicationContext(), "morePicture.json");
        List<Bitmap> bitmaps=new ArrayList<>();
        for (int i = 0; i <10;i++) {
            String name = "morePicture_img_" + i + ".jpeg";
            bitmaps.add(BitmapFactory.decodeFile(copyAeAssets(getApplicationContext(),name)));
        }

        for (int i = 0; i <3;i++) {
            String name = "morePicture_img_" + i + ".jpeg";
            bitmaps.add(BitmapFactory.decodeFile(copyAeAssets(getApplicationContext(),name)));
        }
        albumAsset=new LSOPhotoAlbumAsset(bitmaps,jsonPath);

        drawPadView.setBackgroundColor(Color.RED);
        drawPadView.setUpdateMode(DrawPadUpdateMode.AUTO_FLUSH, (int) albumAsset.getFrameRate());

        //设置在两个都走完的时候, 循环.
        drawPadView.setLoopingWhenReachTime(albumAsset.getDurationUs());

        drawPadView.setOnDrawPadRunTimeListener(new onDrawPadRunTimeListener() {
            @Override
            public void onRunTime(DrawPad v, long currentTimeUs) {
                Log.e("LSDelete", "----currentTimeUs: " + currentTimeUs);
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

        if(albumAsset==null&& allExecute!=null ){
            return;
        }

        if(drawPadView!=null){
            drawPadView.stopDrawPad();
        }

        allExecute = new DrawPadAllExecute2(getApplicationContext(), albumAsset.getWidth(), albumAsset.getHeight(), albumAsset.getDurationUs());
        allExecute.addPhotoAlbumLayer(albumAsset);

        allExecute.setOnLanSongSDKErrorListener(new OnLanSongSDKErrorListener() {
            @Override
            public void onLanSongSDKError(int i) {
                allExecute=null;
                DemoProgressDialog.releaseDialog();
                DemoUtil.showDialog(PhotoAlbumLayerDemoActivity.this,"导出错误, 请查看logcat信息.");
            }
        });

        allExecute.setOnLanSongSDKProgressListener(new OnLanSongSDKProgressListener() {
            @Override
            public void onLanSongSDKProgress(long ptsUs, int percent) {
//                Log.e("TAG", "------ptsUs: "+ptsUs+ " percent :"+percent);
                DemoProgressDialog.showPercent(PhotoAlbumLayerDemoActivity.this,percent);
            }
        });

        allExecute.setOnLanSongSDKCompletedListener(new OnLanSongSDKCompletedListener() {
            @Override
            public void onLanSongSDKCompleted(String dstVideo) {
//                MediaInfo.checkFile(dstVideo);
                allExecute=null;
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
