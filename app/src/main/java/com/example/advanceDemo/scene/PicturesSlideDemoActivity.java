package com.example.advanceDemo.scene;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.example.advanceDemo.VideoPlayerActivity;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.DrawPadUpdateMode;
import com.lansosdk.box.LSLog;
import com.lansosdk.box.Layer;
import com.lansosdk.box.MVLayer;
import com.lansosdk.box.MoveAnimation;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.CopyFileFromAssets;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.DrawPadView.onViewAvailable;
import com.lansosdk.videoeditor.LanSongFileUtil;

import java.io.IOException;

/**
 * 演示: 图片合成视频的同时保存成文件. 流程: 把DrawPadView设置为自动刷新模式,
 * 然后一次性增加多个BitmapLayer,根据画面走动的时间戳来 操作每个BitmapLayer是否移动,是否显示.
 * <p>
 * 这里仅仅演示移动的属性, 您实际中可以移动,缩放,旋转,RGBA值调节来混合使用,因为BitmapLayer继承自ILayer,故有这些特性.
 * <p>
 * 比如你根据时间戳来调节图片的RGBA中的A值(alpha透明度),则实现图片的淡入淡出效果.
 * <p>
 * 使用移动+缩放+RGBA调节,则实现一些缓慢照片变化的效果,浪漫文艺范的效果.
 * <p>
 * 视频标记就是一个典型的BitmapLayer的使用场景.
 */
public class PicturesSlideDemoActivity extends Activity {
    private static final String TAG = LSLog.TAG;
    boolean isSwitched = false;
    boolean isDestorying = false; // 是否正在销毁, 因为销毁会停止DrawPad
    private DrawPadView drawPadView;
    private String dstPath = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picture_set_layout);
        initView();

        drawPadView = (DrawPadView) findViewById(R.id.DrawPad_view);

        dstPath = LanSongFileUtil.newMp4PathInBox();
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                initDrawPad();
            }
        }, 300);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(drawPadView.isTextureAvailable()){
            initDrawPad();
        }
        findViewById(R.id.id_DrawPad_saveplay).setVisibility(View.GONE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mediaPlayer!=null){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer=null;
        }
        if(drawPadView!=null){
            drawPadView.stopDrawPad();
        }
    }

    /**
     * Step1: 初始化DrawPad
     */
    private void initDrawPad() {
        int width=720;
        int height=720;
        drawPadView.setUpdateMode(DrawPadUpdateMode.AUTO_FLUSH, 30);
        drawPadView.setRealEncodeEnable(width, height, 3*1024*1024, (int) 30, dstPath);

        drawPadView.setOnDrawPadCompletedListener(new DrawPadCompleted());
        drawPadView.setOnDrawPadProgressListener(new DrawPadProgressListener());
        drawPadView.setDrawPadSize(width, height,new onDrawPadSizeChangedListener() {
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

            addVideoLayer();
            // 这里同时增加多个,只是不显示出来.
            addBitmapLayer(R.drawable.tt, 0, 5000); // 1--5秒.
            addBitmapLayer(R.drawable.tt3, 5000, 10000); // 5--10秒.
            addBitmapLayer(R.drawable.pic3, 10000, 15000); // 10---15秒
            addBitmapLayer(R.drawable.pic4, 15000, 20000); // 15---20秒
            addBitmapLayer(R.drawable.pic5, 20000, 25000); // 20---25秒

            // 增加一个MV图层
            // addMVLayer();
            drawPadView.resumeDrawPad();
        }
    }

    MediaPlayer mediaPlayer;
    /**
     * 增加视频背景
     */
    private void addVideoLayer(){
        mediaPlayer=new MediaPlayer();
        String video=CopyFileFromAssets.copyAssets(getApplicationContext(),"bg10s.mp4");
        try {
            mediaPlayer.setDataSource(video);
            mediaPlayer.prepare();
            mediaPlayer.setLooping(true);
            VideoLayer layer2=drawPadView.addVideoLayer(mediaPlayer.getVideoWidth(),mediaPlayer.getVideoHeight(),null);
            layer2.setScaledValue(layer2.getPadWidth(),layer2.getPadHeight());


            mediaPlayer.setSurface(new Surface(layer2.getVideoTexture()));
            mediaPlayer.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addMVLayer() {
        String colorMVPath = CopyFileFromAssets.copyAssets(getApplicationContext(), "mei.mp4");
        String maskMVPath = CopyFileFromAssets.copyAssets(PicturesSlideDemoActivity.this, "mei_b.mp4");

        MVLayer layer = drawPadView.addMVLayer(colorMVPath, maskMVPath); // <-----增加MVLayer
        layer.setScaledValue(layer.getPadWidth(), layer.getPadHeight());
    }

    private void addBitmapLayer(int resId, long startMS, long endMS) {
        Layer item = drawPadView.addBitmapLayer(BitmapFactory.decodeResource( getResources(), resId));
        item.setVisibility(Layer.INVISIBLE);

        // 开始1/3, 中间停1/3,最后划走1/3;
        long total = endMS - startMS;
        long start2 = endMS - total / 3;

        // 第一段运行
        MoveAnimation move1 = new MoveAnimation(startMS * 1000,
                total * 1000 / 3,
                0,
                item.getPadHeight() / 2, /* 开始位置, 结束位置 */
                item.getPadWidth() / 2,
                item.getPadHeight() / 2);
        move1.setVisibleWhenValid(true);

        // 中间停止在那里.

        // 最后一段运行.
        MoveAnimation move2 = new MoveAnimation(
                start2 * 1000,
                total * 1000 / 3,
                item.getPadWidth() / 2,
                item.getPadHeight() / 2,

                item.getPadWidth() + item.getPadWidth() / 2,
                item.getPadHeight() / 2);


        move2.setVisibleWhenValid(true);
        item.addAnimation(move1);
        item.addAnimation(move2);
    }

    private void initView() {

        findViewById(R.id.id_DrawPad_saveplay).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (LanSongFileUtil.fileExist(dstPath)) {
                    Intent intent = new Intent(PicturesSlideDemoActivity.this, VideoPlayerActivity.class);
                    intent.putExtra("videopath", dstPath);
                    startActivity(intent);
                } else {
                    Toast.makeText(PicturesSlideDemoActivity.this,
                            "目标文件不存在", Toast.LENGTH_SHORT).show();
                }
            }
        });
        findViewById(R.id.id_DrawPad_saveplay).setVisibility(View.GONE);
    }

    private void toastStop() {
        Toast.makeText(getApplicationContext(), "录制已停止!!", Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestorying = true;
        if (drawPadView != null) {
            drawPadView.stopDrawPad();
            drawPadView = null;
        }

        if (LanSongFileUtil.fileExist(dstPath)) {
            LanSongFileUtil.deleteFile(dstPath);
        }
    }

    // DrawPad完成时的回调.
    private class DrawPadCompleted implements onDrawPadCompletedListener {

        @Override
        public void onCompleted(DrawPad v) {
            if (!isDestorying) {
                if (LanSongFileUtil.fileExist(dstPath)) {
                    findViewById(R.id.id_DrawPad_saveplay).setVisibility(
                            View.VISIBLE);
                }
                toastStop();
            }
        }
    }

    // DrawPad进度回调.
    private class DrawPadProgressListener implements onDrawPadProgressListener {

        @Override
        public void onProgress(DrawPad v, long currentTimeUs) { // 单位是微妙
            if (currentTimeUs >= 26 * 1000 * 1000) // 26秒.多出一秒,让图片走完.
            {
                drawPadView.stopDrawPad();
            }
        }
    }
}
