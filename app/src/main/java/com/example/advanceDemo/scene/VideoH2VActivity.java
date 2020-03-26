package com.example.advanceDemo.scene;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.example.advanceDemo.utils.DemoUtil;
import com.example.advanceDemo.utils.LSOProgressDialog;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.LanSongFilter.LanSongBlurFilter;
import com.lansosdk.LanSongFilter.LanSongIF1977Filter;
import com.lansosdk.LanSongFilter.LanSongIFHefeFilter;
import com.lansosdk.LanSongFilter.LanSongIFSutroFilter;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.DrawPadUpdateMode;
import com.lansosdk.box.Layer;
import com.lansosdk.box.SubLayer;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadErrorListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.example.advanceDemo.utils.CopyFileFromAssets;
import com.lansosdk.videoeditor.DrawPadVideoExecute;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.LanSongFileUtil;
import com.lansosdk.videoeditor.MediaInfo;

import java.io.IOException;
import java.util.ArrayList;

/**
 * 横屏的视频 转换为 竖屏.
 */
public class VideoH2VActivity extends Activity implements OnClickListener {
    private DrawPadView drawPadPreview;
    private MediaPlayer mplayer = null;
    private VideoLayer videoLayer = null;
    private String dstPath = null;
    private MediaInfo mInfo = null;

    MediaPlayer  videoBgPlayer;
    private ArrayList<Layer> currentLayers=new ArrayList<>();

    private final static int EFFECT_MODE_NO=0;
    private final static int EFFECT_MODE_BITMAP=1;
    private final static int EFFECT_MODE_VIDEO=2;
    private final static int EFFECT_MODE_BLUR=3;
    private final static int EFFECT_MODE_3ROW=4;

    private int effectMode=EFFECT_MODE_NO;

    //--------是横屏的视频转换为 竖屏
    String videoPath;
    private int drawPadWidth=540;
    private int drawPadHeight=960;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.horizontal2v_layout);

        videoPath=CopyFileFromAssets.copyAssets(getApplicationContext(),"ku7s.mp4");

        mInfo = new MediaInfo(videoPath);
        if (!mInfo.prepare()) {
            Toast.makeText(this, "传递过来的视频文件错误", Toast.LENGTH_SHORT).show();
            this.finish();
        }

        drawPadPreview = (DrawPadView) findViewById(R.id.id_horizontal2v_drawpadview);
        initView();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startPlayVideo();
            }
        }, 300);
    }

    private void startPlayVideo() {
        if (videoPath != null) {
            mplayer = new MediaPlayer();
            try {
                mplayer.setDataSource(videoPath);
                mplayer.setOnPreparedListener(new OnPreparedListener() {

                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        initDrawPad();
                    }
                });
                mplayer.setOnCompletionListener(new OnCompletionListener() {

                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        stopDrawPad();
                    }
                });
                mplayer.setLooping(true);
                mplayer.prepareAsync();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            finish();
            return;
        }
    }

    private void initDrawPad() {
        drawPadPreview.setDrawPadSize(drawPadWidth, drawPadHeight, new onDrawPadSizeChangedListener() {
            @Override
            public void onSizeChanged(int viewWidth, int viewHeight) {
                startDrawPad();
            }
        });
        drawPadPreview.setUpdateMode(DrawPadUpdateMode.AUTO_FLUSH, 25);
    }
    /**
     * Step2: 开始运行 Drawpad
     */
    private void startDrawPad() {
        drawPadPreview.pauseDrawPad();

        if (!drawPadPreview.isRunning() && drawPadPreview.startDrawPad()) {
            videoLayer = drawPadPreview.addVideoLayer(mplayer.getVideoWidth(), mplayer.getVideoHeight(), null);
            if (videoLayer != null) {
                mplayer.setSurface(new Surface(videoLayer.getVideoTexture()));
                mplayer.start();
            }
            drawPadPreview.resumeDrawPad();
        }
    }
    private void stopDrawPad() {
        if (drawPadPreview != null && drawPadPreview.isRunning()) {
            drawPadPreview.stopDrawPad();
        }
    }

    @Override
    public void onClick(View v) {
        clearVdieoEffect();
        switch (v.getId()) {
            case R.id.id_horizontal2v_nobg:
                effectMode=EFFECT_MODE_NO;
                break;
            case R.id.id_horizontal2v_bitmapbg:
                effectMode=EFFECT_MODE_BITMAP;
                bitmapBgEffect();
                break;
            case R.id.id_horizontal2v_videobg:
                videoBgEffect();
                break;
            case R.id.id_horizontal2v_blurbg:
                blurBgEffect();
                break;
            case R.id.id_horizontal2v_3rowbg:
                threeRowEffect();
                break;
            case R.id.id_horizontal2v_export:
                export();
                break;
            default:
                break;
        }
    }
    private void clearVdieoEffect(){
        if(drawPadPreview !=null){
            for (Layer layer: currentLayers){
                drawPadPreview.removeLayer(layer);
            }
            currentLayers.clear();
        }
        if(videoLayer!=null){
            videoLayer.setScale(1.0f);
            videoLayer.switchFilterTo(null);
            videoLayer.removeAllSubLayer();
        }
        if(videoBgPlayer!=null){
            videoBgPlayer.stop();
            videoBgPlayer.release();
            videoBgPlayer=null;
        }
    }
    private void bitmapBgEffect(){
        Bitmap bmp= BitmapFactory.decodeResource(getResources(),R.drawable.blurbg);
        if(drawPadPreview !=null && drawPadPreview.isRunning()){
            BitmapLayer layer= drawPadPreview.addBitmapLayer(bmp);
            drawPadPreview.changeLayerPosition(layer,0);
            layer.setScaledValue(layer.getPadWidth(),layer.getPadHeight());
            currentLayers.add(layer);
        }else if(drawPadExport!=null){
            BitmapLayer layer= drawPadExport.addBitmapLayer(bmp);
            drawPadExport.changeLayerPosition(layer,0);
            layer.setScaledValue(layer.getPadWidth(),layer.getPadHeight());
        }
    }
    /**
     * 增加一个背景视频
     */
    private void videoBgEffect(){
        effectMode=EFFECT_MODE_VIDEO;
        String videoBG= CopyFileFromAssets.copyAssets(getApplicationContext(),"videobg.mp4");
        if( LanSongFileUtil.fileExist(videoBG)){
            if(drawPadPreview !=null && drawPadPreview.isRunning()) {
                videoBgPlayer = new MediaPlayer();
                try {
                    videoBgPlayer.setDataSource(videoBG);
                    videoBgPlayer.prepare();
                    VideoLayer layer = drawPadPreview.addVideoLayer(videoBgPlayer.getVideoWidth(), videoBgPlayer.getVideoHeight(), null);
                    if (layer != null) {
                        videoBgPlayer.setSurface(new Surface(layer.getVideoTexture()));
                        videoBgPlayer.setLooping(true);
                        videoBgPlayer.start();

                        layer.setScaledValue(layer.getPadWidth(), layer.getPadHeight());
                        drawPadPreview.changeLayerPosition(layer, 0);//把这个视频调掉最低层
                        currentLayers.add(layer);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else if(drawPadExport!=null){
                VideoLayer layer=drawPadExport.addVideoLayer(videoBG,null);
                drawPadExport.changeLayerPosition(layer,0); //把这个视频调掉最低层
                layer.setScaledValue(layer.getPadWidth(), layer.getPadHeight());
            }
        }else{
            DemoUtil.showToast(getApplication(),"演示背景视频失败,可能该视频不存在.");
            videoBgPlayer.release();
            videoBgPlayer=null;
        }
    }

    /**
     * 模糊当前视频
     */
    private void blurBgEffect(){
        if (videoLayer != null) {
            effectMode=EFFECT_MODE_BLUR;
            videoLayer.switchFilterTo(new LanSongBlurFilter());
            videoLayer.setScaledValue(videoLayer.getPadWidth()*2,videoLayer.getPadHeight()*2);
            //增加一个子图层;
           videoLayer.addSubLayer();
        }
    }

    /**
     * 3个画面并排
     */
    private void threeRowEffect(){
        if (videoLayer != null) {
            effectMode=EFFECT_MODE_3ROW;
            //增加一个子图层;
            SubLayer layer=videoLayer.addSubLayer();
            //左边子图层的中心点在:当前容器的中心点 - 视频图层的宽度.
            layer.setPosition(layer.getPositionX(),(layer.getPadHeight()/2.0f - layer.getLayerHeight()));

            SubLayer layer2=videoLayer.addSubLayer();
            layer2.setPosition(layer2.getPositionX(),(layer2.getPadHeight()/2.0f + layer2.getLayerHeight()));

            //三个视频都设置滤镜;
            videoLayer.switchFilterTo(new LanSongIF1977Filter(getApplicationContext()));
            layer.switchFilterTo(new LanSongIFHefeFilter(getApplicationContext()));
            layer2.switchFilterTo(new LanSongIFSutroFilter(getApplicationContext()));
        }
        bitmapBgEffect(); //如果不到边,再增加一个图片背景;
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPreview();
    }
    private void stopPreview(){
        if (mplayer != null) {
            mplayer.stop();
            mplayer.release();
            mplayer = null;
        }
        if (drawPadPreview != null) {
            drawPadPreview.stopDrawPad();
        }
        if(videoBgPlayer!=null){
            videoBgPlayer.stop();
            videoBgPlayer.release();
            videoBgPlayer=null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LanSongFileUtil.deleteFile(dstPath);
    }

    private void initView() {
        findViewById(R.id.id_horizontal2v_nobg).setOnClickListener(this);
        findViewById(R.id.id_horizontal2v_bitmapbg).setOnClickListener(this);
        findViewById(R.id.id_horizontal2v_videobg).setOnClickListener(this);
        findViewById(R.id.id_horizontal2v_blurbg).setOnClickListener(this);
        findViewById(R.id.id_horizontal2v_3rowbg).setOnClickListener(this);
        findViewById(R.id.id_horizontal2v_export).setOnClickListener(this);
    }

    //-----------------------------------------------后台执行(导出)

    DrawPadVideoExecute drawPadExport;
    LSOProgressDialog progressDialog=new LSOProgressDialog();
    private void export(){
        stopPreview();
        dstPath=LanSongFileUtil.createMp4FileInBox();
        drawPadExport=new DrawPadVideoExecute(getApplication(),videoPath,dstPath);
        drawPadExport.setDrawPadSize(drawPadWidth,drawPadHeight);
        drawPadExport.setDrawPadCompletedListener(new onDrawPadCompletedListener() {
            @Override
            public void onCompleted(DrawPad v) {
                progressDialog.release();
                DemoUtil.startPreviewVideo(VideoH2VActivity.this,dstPath);
            }
        });
        drawPadExport.setDrawPadProgressListener(new onDrawPadProgressListener() {
            @Override
            public void onProgress(DrawPad v, long currentTimeUs) {
                progressDialog.setProgress(drawPadExport.convertToPercent(currentTimeUs));
            }
        });
        drawPadExport.setDrawPadErrorListener(new onDrawPadErrorListener() {
            @Override
            public void onError(DrawPad d, int what) {
                progressDialog.release();
                drawPadExport.stopDrawPad();
                DemoUtil.showDialog(VideoH2VActivity.this, "当前导出失败,请联系我们!");
            }
        });

        drawPadExport.pauseRecord();
        if(drawPadExport.startDrawPad()){
            progressDialog.show(VideoH2VActivity.this);
            videoLayer = drawPadExport.getMainVideoLayer();
            switch (effectMode){
                case EFFECT_MODE_NO:
                    break;
                case EFFECT_MODE_BITMAP:
                    bitmapBgEffect();
                    break;
                case EFFECT_MODE_VIDEO:
                    videoBgEffect();
                    break;
                case EFFECT_MODE_BLUR:
                    blurBgEffect();
                    break;
                case EFFECT_MODE_3ROW:
                    threeRowEffect();
                    break;
                default:
                        break;
            }
            drawPadExport.resumeRecord();
        }
    }
}
