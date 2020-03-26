package com.example.advanceDemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.example.advanceDemo.utils.ButtonEnable;
import com.example.advanceDemo.utils.DemoLog;
import com.example.advanceDemo.utils.DemoProgressDialog;
import com.example.advanceDemo.utils.DemoUtil;
import com.example.advanceDemo.view.ShowHeart;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.LanSongFilter.LanSongIF1977Filter;
import com.lansosdk.box.CanvasLayer;
import com.lansosdk.box.CanvasRunnable;
import com.lansosdk.box.LSOLayerPosition;
import com.lansosdk.box.MVLayer;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.videoeditor.BeautyManager;
import com.example.advanceDemo.utils.CopyFileFromAssets;
import com.lansosdk.videoeditor.LanSongFileUtil;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.VideoOneDo2;

import java.util.ArrayList;
import java.util.List;

/**
 * 视频常见功能演示.
 */
public class VideoOneDO2Activity extends Activity {

    private final static String TAG = "VideoOneDO2Activity";

    private List<Integer> buttonIds = new ArrayList<>();
    private VideoOneDo2 videoOneDo;
    private String srcVideoPath;
    private DemoProgressDialog progressDialog;
    private boolean isExtractEnable; //是否设置的读取帧;
    private boolean isAddBgPicture; //是否增加了背景图片
    private String dstVideoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_one_do_layout);


        srcVideoPath= getIntent().getStringExtra("videopath");
        initView();

        progressDialog=new DemoProgressDialog();
        findViewById(R.id.id_onedoe_export_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                exportVideo();
            }
        });

    }
    private void exportVideo(){
        if(!isSelectOne()){
            DemoUtil.showDialog(VideoOneDO2Activity.this,"您至少选中一个功能来演示");
            return;
        }

        if(videoOneDo!=null && videoOneDo.isRunning()){
            return;
        }

        try {
            videoOneDo=new VideoOneDo2(getApplicationContext(),srcVideoPath);
            settingAndStart();
        } catch (Exception e) {

//            e.printStackTrace();
            DemoUtil.showDialog(VideoOneDO2Activity.this,"创建对象异常,可能不支持当前视频");
        }
    }

    private boolean isSelectOne(){
        for(Integer value: buttonIds){
            if(isEnable(value)){
                return true;
            }
        }
        return false;
    }
    private void settingAndStart(){
        isExtractEnable=false;
        isAddBgPicture=false;

        for(Integer value: buttonIds){
            if(!isEnable(value)){
                continue;
            }
            //一下都是enable按下的;
            switch (value){
                case R.id.id_onedo_bgmisuc_btn:  //增加背景音乐

                    String audioPath=CopyFileFromAssets.copyAssets(getApplicationContext(),"hongdou10s.mp3");
                    videoOneDo.addAudioLayer(audioPath,true);
                    break;
                case R.id.id_onedo_audiovolume_btn:  //调整音量,音量范围是
                    videoOneDo.setVideoVolume(0.1f);  //设置降低10倍;
                    break;
                case R.id.id_onedo_cutduration_btn:  //裁剪时长

                    //举例,从1/3时间处裁剪
                    long total=videoOneDo.getVideoDurationUs();
                    long startUs=total/3;
                    videoOneDo.setCutDuration(startUs,total);
                    break;
                case R.id.id_onedo_cropframe_btn: //裁剪画面;
                    //举例:从xy的1/6处裁剪, 裁剪2/3的宽高;

                    int x=videoOneDo.getVideoWidth()/6;  //因先裁剪的画面.后缩放,故可以设置;
                    int y=videoOneDo.getVideoHeight()/6;

                    int cropWidth=videoOneDo.getVideoWidth()*2/3;
                    int cropHeight=videoOneDo.getVideoHeight()*2/3;

                    videoOneDo.setCropRect(x,y,cropWidth,cropHeight);
                    break;
                case R.id.id_onedo_scale_btn: //缩放. 缩放为原来的2/3;
                    videoOneDo.setScaleSize(videoOneDo.getPadWidth()*2/3,videoOneDo.getPadHeight()*2/3);
                    break;
                case R.id.id_onedo_compress_btn: //压缩
                    videoOneDo.setCompressPercent(0.5f);
                    break;
                case R.id.id_onedo_extractframe_btn:  //提取30帧;
                    videoOneDo.setExtractFrame(30);
                    isExtractEnable=true;
                    break;
                case R.id.id_onedo_editmode_btn: //设置编辑模式
                    videoOneDo.setEditModeVideo();
                    break;
                case R.id.id_onedo_filter_btn: //设置普通滤镜
                    videoOneDo.addFilter(new LanSongIF1977Filter(getApplicationContext()));
                    break;
                case R.id.id_onedo_beautiful_btn:  //设置美颜滤镜
                    videoOneDo.addFilterList(BeautyManager.getBeaufulFilters(getApplicationContext()));
                    break;
                case R.id.id_onedo_mask_btn:  //设置遮罩,
                {
                    Bitmap  bmp3=BitmapFactory.decodeFile(CopyFileFromAssets.copyAssets(getApplicationContext(),"alpha_corners.png"));
                    videoOneDo.setMaskBitmap(bmp3);

                    if(!isAddBgPicture){
                        Bitmap bmp4= CopyFileFromAssets.copyAsset2Bmp(getApplicationContext(),"bgPicture.jpg");
                        videoOneDo.setBackGroundBitmapLayer(bmp4);
                    }
                }
                break;
                case R.id.id_onedo_background_bitmap_btn:  //增加背景图片

                    Bitmap bmp= BitmapFactory.decodeResource(getResources(),R.drawable.a2);
                    videoOneDo.setBackGroundBitmapLayer(bmp);
                    isAddBgPicture=true;
                    DemoLog.w("设置的背景图片, 只有在视频设置了透明或者视频图层缩小后才可以看到. 不然会被视频挡住, 一般用在视频透明会圆角的场合");
                    break;
                case R.id.id_onedo_cover_bitmap_btn://设置封面;
                    Bitmap bmp1= BitmapFactory.decodeResource(getResources(),R.drawable.a1);
                    videoOneDo.setCoverLayer(bmp1,0,1*1000*1000);
                    break;
                case R.id.id_onedo_logo_btn: //增加logo
                    Bitmap bmp2= BitmapFactory.decodeResource(getResources(),R.drawable.ls_logo);
                    videoOneDo.setLogoBitmapLayer(bmp2, LSOLayerPosition.LeftTop);
                    break;
                case R.id.id_onedo_mv_btn:  //增加mv图层;
                {
                    String  mvColor=CopyFileFromAssets.copyAssets(getApplicationContext(),"mei.mp4");
                    String  mvMask=CopyFileFromAssets.copyAssets(getApplicationContext(),"mei_b.mp4");
                    MVLayer mvLayer=videoOneDo.addMVLayer(mvColor,mvMask);
                    if(mvLayer!=null)mvLayer.setScaledToPadSize(); //缩放到整个容器;
                }
                break;
                case R.id.id_onedo_gif_btn:

                    videoOneDo.addGifLayer(R.drawable.g06,LSOLayerPosition.LeftBottom);
                    break;
                case R.id.id_onedo_canvas_btn:
                    addCanvasLayer();
                    break;
                default:
                    break;
            }
        }
        //之所以在最后增加文字,是为了获取容器的宽高,
        // 文字可以用Canvas直接绘制, 也可以先绘制成Bitmap,然后用addBitmap的形式来调用;
        if(isEnable(R.id.id_onedo_text_btn)){
            String str1 = "蓝松视频SDK,文字演示";
            int fontSize = 30;
            Bitmap bitmap = Bitmap.createBitmap(videoOneDo.getPadWidth(),videoOneDo.getPadHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            paint.setTextSize(fontSize);
            paint.setColor(Color.RED);

            int y = 40;
            canvas.drawText(str1, 0, y, paint);
            canvas.save(Canvas.ALL_SAVE_FLAG);
            canvas.restore();
            videoOneDo.addBitmapLayer(bitmap);
        }


        //---------增加完毕---增加错误/进度/完成回调;

        videoOneDo.setOnVideoOneDoErrorListener(new OnLanSongSDKErrorListener() {
            @Override
            public void onLanSongSDKError(int errorCode) {
                DemoUtil.showDialog(VideoOneDO2Activity.this,"VideoOneDo处理错误");
                videoOneDo.cancel();
                videoOneDo=null;
                if(progressDialog!=null){
                    progressDialog.release();
                }
            }
        });
        videoOneDo.setOnVideoOneDoProgressListener(new OnLanSongSDKProgressListener() {
            @Override
            public void onLanSongSDKProgress(long ptsUs, int percent) {
                if(progressDialog!=null){
                    progressDialog.setProgress(percent);
                }
            }
        });
        videoOneDo.setOnVideoOneDoCompletedListener(new OnLanSongSDKCompletedListener() {
            @Override
            public void onLanSongSDKCompleted(String dstVideo) {

                if(progressDialog!=null){
                    progressDialog.release();
                }
                dstVideoPath=dstVideo;
                if(isExtractEnable){
                    showExtractFrames();
                }else{
                    startPlayDstVideo();
                }
            }
        });
        progressDialog.showWithNoCancel(VideoOneDO2Activity.this);
        progressDialog.getProgressDialog().setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if(videoOneDo!=null){
                    videoOneDo.cancel();
                    DemoLog.w("您已经取消了执行...");
                }
            }
        });

        videoOneDo.start();
    }
    private boolean isEnable(int id){
        ButtonEnable enable=findViewById(id);
        return enable!=null && enable.isSelecteButton();
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (videoOneDo != null && videoOneDo.isRunning()) {
            videoOneDo.cancel();
            videoOneDo.release();
            videoOneDo=null;
        }
    }

    private void showExtractFrames(){
            if(isExtractEnable&& videoOneDo!=null){
                int cnt=0;
                Bitmap bmp=videoOneDo.getExtractFrame();
                if(bmp!=null) cnt++;
                while(bmp!=null){
                    bmp=videoOneDo.getExtractFrame();
                    if(bmp!=null) cnt++;
                }

                String str="已经读取了:"+ cnt + " 帧, 代码在VideoOneDo2Activity中";
                DemoUtil.showToast(getApplicationContext(),str);
                startPlayDstVideo();
            }
    }
    private void startPlayDstVideo(){
        videoOneDo=null;
        if(LanSongFileUtil.fileExist(dstVideoPath)){
            DemoUtil.playDstVideo(VideoOneDO2Activity.this,dstVideoPath);
        }else{
            DemoUtil.showDialog(VideoOneDO2Activity.this,"VideoOneDo处理错误");
        }
    }



    private void initView() {
        buttonIds.add(R.id.id_onedo_bgmisuc_btn);
        buttonIds.add(R.id.id_onedo_audiovolume_btn);
        buttonIds.add(R.id.id_onedo_cutduration_btn);
        buttonIds.add(R.id.id_onedo_cropframe_btn);
        buttonIds.add(R.id.id_onedo_scale_btn);
        buttonIds.add(R.id.id_onedo_compress_btn);
        buttonIds.add(R.id.id_onedo_extractframe_btn);
        buttonIds.add(R.id.id_onedo_editmode_btn);
        buttonIds.add(R.id.id_onedo_text_btn);
        buttonIds.add(R.id.id_onedo_filter_btn);
        buttonIds.add(R.id.id_onedo_beautiful_btn);
        buttonIds.add(R.id.id_onedo_mask_btn);
        buttonIds.add(R.id.id_onedo_background_bitmap_btn);
        buttonIds.add(R.id.id_onedo_cover_bitmap_btn);
        buttonIds.add(R.id.id_onedo_logo_btn);
        buttonIds.add(R.id.id_onedo_mv_btn);
        buttonIds.add(R.id.id_onedo_gif_btn);
        buttonIds.add(R.id.id_onedo_canvas_btn);
    }
    CanvasLayer canvasLayer;
    private ShowHeart showHeart;
    private void addCanvasLayer() {
        if(videoOneDo!=null){
            canvasLayer = videoOneDo.addCanvasLayer();
            canvasLayer.setClearCanvas(false);  //是否在每次绘制前都擦除上一次绘制的内容;
            if (canvasLayer != null) {
                if(showHeart==null){
                    showHeart = new ShowHeart(this, videoOneDo.getPadWidth(), videoOneDo.getPadHeight());
                }

                canvasLayer.addCanvasRunnable(new CanvasRunnable() {

                    @Override
                    public void onDrawCanvas(CanvasLayer layer, Canvas canvas,
                                             long currentTimeUs) {
                        showHeart.drawTrack(canvas);
                    }
                });
            }
        }
    }
}