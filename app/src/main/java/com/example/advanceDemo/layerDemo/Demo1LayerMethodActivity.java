package com.example.advanceDemo.layerDemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.example.advanceDemo.DemoApplication;
import com.example.advanceDemo.utils.DemoUtil;
import com.example.advanceDemo.utils.YUVLayerDemoData;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.DrawPadUpdateMode;
import com.lansosdk.box.GifLayer;
import com.lansosdk.box.LSOMVAsset;
import com.lansosdk.box.Layer;
import com.lansosdk.box.MVLayer;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.YUVLayer;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.box.onDrawPadThreadProgressListener;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoplayer.OnLSOPlayerPreparedListener;
import com.lansosdk.videoplayer.VPlayer;
import com.lansosdk.videoplayer.VideoPlayer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static com.example.advanceDemo.utils.CopyFileFromAssets.copyAssets;

/**
 * 平移,旋转,缩放,RGBA值,显示/不显示(闪烁)效果. 实际使用中, 可用这些属性来做些动画,比如平移+RGBA调节,呈现舒缓移除的效果.
 * 缓慢缩放呈现照片播放效果;旋转呈现欢快的炫酷效果等等.
 */

public class Demo1LayerMethodActivity extends Activity implements OnSeekBarChangeListener{
    private String videoPath;
    private DrawPadView drawPadView;
    private VideoLayer videoLayer = null;
    private BitmapLayer bitmapLayer = null;
    private YUVLayer mYuvLayer = null;
    private YUVLayerDemoData mData;
    private int count = 0;

    private VPlayer vPlayer;
    private ArrayList<LSOMVAsset> mvAssetList=new ArrayList<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawpad_layout);

        videoPath = DemoApplication.getInstance().currentEditVideo;
        MediaInfo.checkFile(videoPath);


        drawPadView = (DrawPadView) findViewById(R.id.id_drawpad_drawpadview);
        initView();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startPlayVideo();
            }
        }, 100);
    }


    private void startPlayVideo() {
        vPlayer = new VPlayer(this);
        try {
            vPlayer.setVideoPath(videoPath);
            vPlayer.setOnPreparedListener(new OnLSOPlayerPreparedListener() {
                @Override
                public void onPrepared(VideoPlayer mp) {
                    vPlayer.setLooping(true);
                    initDrawPad();
                }
            });
            vPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        drawPadView.setDrawPadSize(vPlayer.getVideoWidth(), vPlayer.getVideoHeight(), new onDrawPadSizeChangedListener() {
            @Override
            public void onSizeChanged(int viewWidth, int viewHeight) {
                startDrawPad();
            }
        });
    }

    /**
     * 开始容器
     */
    private void startDrawPad() {
        drawPadView.pauseDrawPad();
        if (!drawPadView.isRunning() && drawPadView.startDrawPad()) {

            videoLayer = drawPadView.addVideoLayer(vPlayer.getVideoWidth(), vPlayer.getVideoHeight(), null);
            if (videoLayer != null) {
                vPlayer.setSurface(new Surface(videoLayer.getVideoTexture()));
                vPlayer.start();
            }
            drawPadView.resumeDrawPad();


        }
    }


    private MVLayer mvLayer=null;

    private void addMVLayer(){
        if(drawPadView!=null){
            mvLayer=drawPadView.addMVLayer(copyAssets(getApplicationContext(),"kd_mvColor.mp4"),copyAssets(getApplicationContext(),"kd_mvMask.mp4"));
        }
    }

    private void removeMVLayer(){
        if(drawPadView!=null){
            drawPadView.removeLayer(mvLayer);
            mvLayer=null;
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
        if (vPlayer != null) {
            vPlayer.stop();
            vPlayer.release();
            vPlayer = null;
        }
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
        for (LSOMVAsset lsomvAsset: mvAssetList){
            lsomvAsset.release();
        }
        mvAssetList=null;
    }
    private void initView() {
        initSeekBar(R.id.id_DrawPad_skbar_rotate, 360);
        initSeekBar(R.id.id_DrawPad_skbar_scaleX, 800);
        initSeekBar(R.id.id_DrawPad_skbar_scaleXY, 800);

        initSeekBar(R.id.id_DrawPad_skbar_moveX, 100);
        initSeekBar(R.id.id_DrawPad_skbar_moveY, 100);
    }


    private void initSeekBar(int resId, int maxvalue) {
        SeekBar seekBar = (SeekBar) findViewById(resId);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setMax(maxvalue);
    }



    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        if (!fromUser) {
            return;
        }
        switch (seekBar.getId()) {
            case R.id.id_DrawPad_skbar_rotate:
                if (videoLayer != null) {
                    videoLayer.setRotate(progress);
                }
                break;
            case R.id.id_DrawPad_skbar_scaleX:
                if (videoLayer != null) {
                    videoLayer.setScale((float) progress / 100f, 1.0f);
                }

                break;
            case R.id.id_DrawPad_skbar_scaleXY:
                if (videoLayer != null) {
                    videoLayer.setScale((float) progress / 100f, (float) progress / 100f);
                }
                break;
            case R.id.id_DrawPad_skbar_moveX:
                if (videoLayer != null) {

                    //图层从容器的最左边移动到最右边
                    float percent = progress * 1.0f / 100f;//百分比;
                    float posX = (videoLayer.getPadWidth() + videoLayer.getLayerWidth()) * percent - videoLayer.getLayerWidth() / 2.0f;
                    videoLayer.setPosition((int) posX, videoLayer.getPositionY());

                    //左侧移出容器如下
                    //videoLayer.setPosition(-videoLayer.getLayerWidth()/2, videoLayer.getPositionY());
                    //最右侧,移出屏幕如下:
                    //videoLayer.setPosition(videoLayer.getDrawPadWidth()+videoLayer.getLayerWidth()/2, videoLayer.getPositionY());
                }
                break;
            case R.id.id_DrawPad_skbar_moveY:
                if (videoLayer != null) {

                    float percent = progress * 1.0f / 100f;  //百分比;

                    float posY = (videoLayer.getPadHeight() + videoLayer.getLayerHeight()) * percent - videoLayer.getLayerHeight() / 2.0f;
                    videoLayer.setPosition(videoLayer.getPositionX(), posY);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    //---------------------可以增加其他图层;
    private GifLayer gifLayer;
    private void addGifLayer() {
        if (drawPadView != null && drawPadView.isRunning()) {
            gifLayer=  drawPadView.addGifLayer(R.drawable.g07);
        }
    }
    private void addBitmapLayer() {
        if (bitmapLayer == null) {
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.tt3);
            bitmapLayer = drawPadView.addBitmapLayer(bmp);
        }
    }
    private void removeLayer(Layer layer){
        if(drawPadView!=null && layer!=null){
            drawPadView.removeLayer(layer);
        }
    }

}
