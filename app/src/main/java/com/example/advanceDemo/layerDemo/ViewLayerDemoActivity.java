package com.example.advanceDemo.layerDemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.example.advanceDemo.utils.DemoUtil;
import com.example.advanceDemo.utils.LSOProgressDialog;
import com.example.advanceDemo.view.ImageTouchView;
import com.example.advanceDemo.view.PaintConstants;
import com.example.advanceDemo.view.StickerView;
import com.example.advanceDemo.view.TextStickerView;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.DrawPadUpdateMode;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.ViewLayer;
import com.lansosdk.box.ViewLayerRelativeLayout;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.AudioEditor;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.LanSongFileUtil;
import com.lansosdk.videoeditor.VideoOneDo2;

import java.io.IOException;

public class ViewLayerDemoActivity extends Activity implements OnClickListener {

    public static final  int RUN_RECORD_MODE=1;
    public static final  int RUN_EXPORT_MODE=2;

    private static final String TAG = "ViewLayerDemoActivity";
    ImageTouchView imgeTouchView;
    long lastTimeUs = 0;
    private String srcVideoPath;
    private DrawPadView drawPadView;
    private MediaPlayer mediaPlayer = null;
    private VideoLayer mainVideoLayer = null;
    //
    private String drawPadRecordPath = null;
    private String dstPath = null;
    private ViewLayer viewLayer = null;
    private ViewLayerRelativeLayout viewLayerRelativeLayout;
    private MediaInfo mediaInfo = null;
    private StickerView stickView;
    private TextStickerView textStickView;
    private int stickCnt = 2;
    private String strInputText = "蓝松文字演示";
    private int runMode=RUN_EXPORT_MODE;
    LSOProgressDialog progressDialog;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vview_drawimage_demo_layout);

        srcVideoPath = getIntent().getStringExtra("videopath");
        mediaInfo = new MediaInfo(srcVideoPath);

        runMode=getIntent().getIntExtra("mode",RUN_EXPORT_MODE);

        if (!mediaInfo.prepare()) {
            Log.e(TAG, " video path is error.finish\n");
            finish();
        }

        drawPadView = findViewById(R.id.id_vview_realtime_drawpadview);

        initView();

        progressDialog = new LSOProgressDialog();
        // 演示例子用到的.
        PaintConstants.SELECTOR.COLORING = true;
        PaintConstants.SELECTOR.KEEP_IMAGE = true;

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                startPlayVideo();
            }
        }, 200);
    }

    private void startPlayVideo() {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(srcVideoPath);
            mediaPlayer.setOnPreparedListener(new OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    initDrawPad();
                }
            });
            mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopDrawPad();
                }
            });
            if(isExportMode()){
                mediaPlayer.setLooping(true);
            }
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private boolean isExportMode(){
        return runMode==RUN_EXPORT_MODE;
    }

    /**
     * 设置DrawPad 容器的尺寸. 并设置是否实时录制容器上的内容.
     */
    private void initDrawPad() {

        int padWidth = mediaPlayer.getVideoWidth();
        int padHeight = mediaPlayer.getVideoHeight();
        int frameRate = 25;



        drawPadView.setUpdateMode(DrawPadUpdateMode.AUTO_FLUSH, frameRate);

        if(!isExportMode()){  //录制模式,则设置录制的路径;
            drawPadRecordPath=LanSongFileUtil.createMp4FileInBox();
            drawPadView.setRealEncodeEnable(padWidth, padHeight, (int) frameRate, drawPadRecordPath);
        }

        drawPadView.setDrawPadSize(padWidth, padHeight, new onDrawPadSizeChangedListener() {
            @Override
            public void onSizeChanged(int viewWidth, int viewHeight) {
                startDrawPad();
            }
        });
    }

    /**
     *容器线程, 并增加一个视频图层和 view图层.
     */
    private void startDrawPad() {
        if (drawPadView.startDrawPad()) {
            mainVideoLayer = drawPadView.addMainVideoLayer(mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight(), null);
            if (mainVideoLayer != null) {
                mediaPlayer.setSurface(new Surface(mainVideoLayer.getVideoTexture()));
                mainVideoLayer.setScaledToPadSize();
            }

            mediaPlayer.start();
            addViewLayer();
        }
    }

    /**
     * Step3: 做好后, 停止容器, 因为容器里没有声音, 这里增加上原来的声音.
     */
    private void stopDrawPad() {
        if (drawPadView != null && drawPadView.isRunning()) {
            drawPadView.stopDrawPad();
            if (LanSongFileUtil.fileExist(drawPadRecordPath)) {
                DemoUtil.showToast(getApplicationContext(),"录制完成, 开始预览");
                dstPath = AudioEditor.mergeAudioNoCheck(srcVideoPath, drawPadRecordPath, true);
                DemoUtil.startPreviewVideo(ViewLayerDemoActivity.this, dstPath);
            }else{
                DemoUtil.showDialog(ViewLayerDemoActivity.this,"录制失败, 请查看打印信息,联系我们.");
            }
        }
    }
    /**
     * 增加一个UI图层: ViewLayer
     */
    private void addViewLayer() {
        if (drawPadView != null && drawPadView.isRunning()) {


            viewLayer = drawPadView.addViewLayer();

            // 绑定
            viewLayerRelativeLayout.bindViewLayer(viewLayer);


            ViewGroup.LayoutParams params = viewLayerRelativeLayout.getLayoutParams();

            params.width = viewLayer.getPadWidth();
            params.height = viewLayer.getPadHeight();
            // 这里调整高度,让他们一致.
            viewLayerRelativeLayout.setLayoutParams(params);
            viewLayerRelativeLayout.invalidate();// 刷新一下.
        }
    }
    @Override
    protected void onPause() {
        super.onPause();

        releasePreview();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LanSongFileUtil.deleteFile(dstPath);
        LanSongFileUtil.deleteFile(drawPadRecordPath);
    }

    /**
     * 释放所有的预览
     */
    private void releasePreview(){
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (drawPadView != null) {
            drawPadView.stopDrawPad();
        }
    }

    // -------------------------------------一下为UI界面-----------------------------------------------------------
    private void initView() {
        findViewById(R.id.id_vview_drawimage_pause).setOnClickListener(this);
        findViewById(R.id.id_vview_drawimage_addstick).setOnClickListener(this);
        findViewById(R.id.id_vview_drawimage_addtext).setOnClickListener(this);

        imgeTouchView = (ImageTouchView) findViewById(R.id.switcher);
        imgeTouchView.setActivity(ViewLayerDemoActivity.this);

        stickView = (StickerView) findViewById(R.id.id_vview_drawimage_stickview);
        textStickView = (TextStickerView) findViewById(R.id.id_vview_drawimage_textstickview);
        viewLayerRelativeLayout = (ViewLayerRelativeLayout) findViewById(R.id.id_vview_realtime_gllayout);



        Button btn=findViewById(R.id.id_vview_drawimage_export);
        if(isExportMode()){
            btn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (viewLayerRelativeLayout != null) {
                        Bitmap bmp = viewLayerRelativeLayout.toggleSnatShot();
                        exportUILayer(bmp);
                    }
                }
            });
        }else{
            btn.setVisibility(View.INVISIBLE);
        }
    }

    VideoOneDo2 videoOneDo2;

    private void exportUILayer(Bitmap bmp) {
        if (videoOneDo2 != null && !videoOneDo2.isRunning()) {
            return;
        }
        releasePreview();
        try {

            videoOneDo2 = new VideoOneDo2(getApplication(), srcVideoPath);

            BitmapLayer bitmapLayer = videoOneDo2.addBitmapLayer(bmp);
            if (bitmapLayer != null) {
                bitmapLayer.setScaledToPadSize();
            }
            //------设置各种listener
            videoOneDo2.setOnVideoOneDoErrorListener(new OnLanSongSDKErrorListener() {
                @Override
                public void onLanSongSDKError(int errorCode) {
                    if(progressDialog!=null){
                        progressDialog.release();
                        progressDialog=null;
                    }
                }
            });
            videoOneDo2.setOnVideoOneDoProgressListener(new OnLanSongSDKProgressListener() {
                @Override
                public void onLanSongSDKProgress(long ptsUs, int percent) {
                    Log.e("demo", "pts Us:: " + ptsUs + "  percnet " + percent);
                    progressDialog.setProgress(percent);
                }
            });
            videoOneDo2.setOnVideoOneDoCompletedListener(new OnLanSongSDKCompletedListener() {
                @Override
                public void onLanSongSDKCompleted(String dstVideo) {
                    if(progressDialog!=null){
                        progressDialog.release();
                        progressDialog=null;
                    }
                    dstPath=dstVideo;
                    DemoUtil.startPreviewVideo(ViewLayerDemoActivity.this, dstVideo);
                }
            });
            progressDialog.show(this);
            videoOneDo2.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_vview_drawimage_pause:
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        drawPadView.pauseDrawPad();

                        // 解除绑定, 让android的UI线程可以更新UI界面;
                        if (viewLayerRelativeLayout != null) {
                            viewLayerRelativeLayout.unBindViewLayer();
                        }
                    } else {
                        mediaPlayer.start();
                        drawPadView.resumeDrawPad();
                        if (viewLayerRelativeLayout != null) {
                            viewLayerRelativeLayout.bindViewLayer(viewLayer);
                        }
                        // 把贴纸的边框去掉.
                        stickView.disappearIconBorder();
                        textStickView.disappearIconBorder();
                    }
                }
                break;
            case R.id.id_vview_drawimage_addstick:
                if (stickView != null) {
                    Bitmap bmp = null;
                    if (stickCnt == 2) {
                        bmp = BitmapFactory.decodeResource(getResources(),
                                R.drawable.stick2);
                    } else if (stickCnt == 3) {
                        bmp = BitmapFactory.decodeResource(getResources(),
                                R.drawable.stick3);
                    } else if (stickCnt == 4) {
                        bmp = BitmapFactory.decodeResource(getResources(),
                                R.drawable.stick4);
                    } else {
                        bmp = BitmapFactory.decodeResource(getResources(),
                                R.drawable.stick5);
                    }
                    stickCnt++;
                    stickView.addBitImage(bmp);
                }
                break;
            case R.id.id_vview_drawimage_addtext:
                showInputDialog();
                break;
            default:
                break;
        }
    }

    private void showInputDialog() {
        final EditText etInput = new EditText(this);
        new AlertDialog.Builder(this).setTitle("请输入文字").setView(etInput)
                .setPositiveButton("确定", new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String input = etInput.getText().toString();
                        if (input != null && !input.equals("")) {
                            strInputText = input;
                            textStickView.setText(strInputText);
                        }
                    }
                }).show();
    }

}
