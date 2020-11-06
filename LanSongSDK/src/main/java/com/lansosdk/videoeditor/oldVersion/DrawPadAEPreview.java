package com.lansosdk.videoeditor.oldVersion;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import com.lansosdk.LanSongAe.LSOAeDrawable;
import com.lansosdk.LanSongAe.LSOAeImage;
import com.lansosdk.LanSongAe.LSOAeImageLayer;
import com.lansosdk.LanSongAe.LSOAeText;
import com.lansosdk.box.AEJsonLayer;
import com.lansosdk.box.AEMVLayer;
import com.lansosdk.box.AEVideoLayer;
import com.lansosdk.box.AudioLayer;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.DrawPadAePreviewRunnable;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.OnDrawPadCancelAsyncListener;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadErrorListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.archApi.LanSongFileUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Deprecated
public class DrawPadAEPreview extends FrameLayout {

    private TextureRenderView2 mTextureRenderView;
    private SurfaceTexture mSurfaceTexture = null;
    private int drawPadWidth, drawPadHeight;
    private onViewAvailable mViewAvailable = null;
    private boolean isLayoutOk=false;
    // ----------------------------------------------
    public DrawPadAEPreview(Context context) {
        super(context);
        initVideoView(context);
    }

    public DrawPadAEPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView(context);
    }

    public DrawPadAEPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DrawPadAEPreview(Context context, AttributeSet attrs, int defStyleAttr,
                            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initVideoView(context);
    }

    private void initVideoView(Context context) {
        setTextureView();
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
    }

    private void setTextureView() {
        mTextureRenderView = new TextureRenderView2(getContext());
        mTextureRenderView.setSurfaceTextureListener(new SurfaceCallback());

        mTextureRenderView.setDisplayRatio(0);

        View renderUIView = mTextureRenderView.getView();
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        renderUIView.setLayoutParams(lp);
        addView(renderUIView);
        mTextureRenderView.setVideoRotation(0);
    }
    /**
     *当前View有效的时候, 回调监听;
     */
    public void setOnViewAvailable(onViewAvailable listener) {
        mViewAvailable = listener;
        if (mSurfaceTexture != null) {
            if(drawPadHeight>0 && drawPadWidth>0 && desireWidth>0 && desireHeight>0){

                float acpect = (float) desireWidth / (float) desireHeight;
                float padAcpect = (float) drawPadWidth / (float) drawPadHeight;

                if (acpect == padAcpect) { // 如果比例已经相等,不需要再调整,则直接显示.
                    isLayoutOk=true;
                    mViewAvailable.viewAvailable(this);
                } else if (Math.abs(acpect - padAcpect) * 1000 < 16.0f) {
                    isLayoutOk=true;
                    mViewAvailable.viewAvailable(this);
                }else{
                    mTextureRenderView.setVideoSize(desireWidth, desireHeight);
                    mTextureRenderView.setVideoSampleAspectRatio(1, 1);
                    LSOLog.d("setOnViewAvailable layout again...");
                    requestLayoutPreview();
                }
            }
        }
    }
    public Surface getSurface(){
        if(mSurfaceTexture!=null){
            return  new Surface(mSurfaceTexture);
        }
        return null;
    }
    public interface onViewAvailable {
        void viewAvailable(DrawPadAEPreview v);
    }

    private class SurfaceCallback implements TextureView.SurfaceTextureListener {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                              int width, int height) {
            mSurfaceTexture = surface;
            drawPadHeight = height;
            drawPadWidth = width;
            checkLayoutSize();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                                int width, int height) {
            mSurfaceTexture = surface;
            drawPadHeight = height;
            drawPadWidth = width;
            checkLayoutSize();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            mSurfaceTexture = null;
            isLayoutOk=false;
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    }

    private int desireWidth;
    private int desireHeight;
    private onDrawPadSizeChangedListener sizeChangedListener;

    /**
     * 设置容器的大小, 在设置后, 我们会根据这个大小来调整DrawPadAEPriview这个类的大小.
     * 从而让画面不变形;
     * @param width
     * @param height
     * @param cb
     */
    public void setDrawPadSize(int width, int height, onDrawPadSizeChangedListener cb) {

        requestLayoutCount=0;
        desireWidth=width;
        desireHeight=height;

        if (width != 0 && height != 0) {
            if(drawPadWidth==0 || drawPadHeight==0){  //直接重新布局UI
                mTextureRenderView.setVideoSize(width, height);
                mTextureRenderView.setVideoSampleAspectRatio(1, 1);
                sizeChangedListener = cb;
                requestLayoutPreview();
            }else{
                float setAcpect = (float) width / (float) height;
                float setViewacpect = (float) drawPadWidth / (float) drawPadHeight;

                if (setAcpect == setViewacpect) { // 如果比例已经相等,不需要再调整,则直接显示.
                    if (cb != null) {
                        isLayoutOk=true;
                        cb.onSizeChanged(width, height);
                    }
                } else if (Math.abs(setAcpect - setViewacpect) * 1000 < 16.0f) {
                    if (cb != null) {
                        cb.onSizeChanged(width, height);
                    }
                } else if (mTextureRenderView != null) {
                    mTextureRenderView.setVideoSize(width, height);
                    mTextureRenderView.setVideoSampleAspectRatio(1, 1);
                    sizeChangedListener = cb;
                }
                requestLayoutPreview();
            }
        }
    }

    private int requestLayoutCount=0;
    /**
     * 检查得到的大小, 如果和view成比例,则直接回调; 如果不成比例,则重新布局;
     */
    private void checkLayoutSize(){
        float setAcpect = (float) desireWidth / (float) desireHeight;
        float setViewacpect = (float) drawPadWidth / (float) drawPadHeight;

        if (setAcpect == setViewacpect) { // 如果比例已经相等,不需要再调整,则直接显示.
            isLayoutOk=true;
            if (sizeChangedListener != null) {
                sizeChangedListener.onSizeChanged(drawPadWidth, drawPadHeight);
                sizeChangedListener =null;
            }else if(mViewAvailable!=null){
                mViewAvailable.viewAvailable(this);
            }
        } else if (Math.abs(setAcpect - setViewacpect) * 1000 < 16.0f) {
            isLayoutOk=true;
            if (sizeChangedListener != null) {
                sizeChangedListener.onSizeChanged(drawPadWidth, drawPadHeight);
                sizeChangedListener =null;
            }else if(mViewAvailable!=null){
                mViewAvailable.viewAvailable(this);
            }
        }else{
            mTextureRenderView.setVideoSize(desireWidth, desireHeight);
            mTextureRenderView.setVideoSampleAspectRatio(1, 1);
            LSOLog.d("checkLayoutSize no  right, layout again...");
            requestLayoutPreview();
        }
    }
    private void requestLayoutPreview()
    {
        requestLayoutCount++;
        if(requestLayoutCount>3){
            LSOLog.e("DrawPadAEPreview layout view error.  return  callback");
            if(sizeChangedListener!=null){
                sizeChangedListener.onSizeChanged(drawPadWidth,drawPadHeight);
            }else if(mViewAvailable!=null){
                mViewAvailable.viewAvailable(this);
            }
        }else{
            requestLayout();
        }
    }
    /**
     * 获取当前View的 宽度
     */
    public int getViewWidth() {
        return drawPadWidth;
    }

    /**
     * 获得当前View的高度.
     */
    public int getViewHeight() {
        return drawPadHeight;
    }
    /**
     * 获得当前容器的宽度
     */
    public int getDrawPadWidth() {
        return drawPadWidth;
    }
    /**
     * 获得当前容器的高度
     */
    public int getDrawPadHeight() {
        return drawPadHeight;
    }
   //-------------------------------增加Ae的代码....
    public DrawPadAePreviewRunnable renderer;
    private void createRender(){
        if(renderer==null){
            renderer =new DrawPadAePreviewRunnable(getContext());
            isStarted=false;
        }
    }
    /**
     * 增加视频图层;
     */
    public AEVideoLayer addVideoLayer(String videoPath) throws IOException {
        createRender();
        if(renderer !=null && LanSongFileUtil.fileExist(videoPath)){
            return renderer.addVideoLayer(videoPath);
        }else{
            return null;
        }
    }
    /**
     * 增加Ae json图层;
     * 在start前调用
     */
    public AEJsonLayer addAeLayer(LSOAeDrawable drawable){
        createRender();
        if(renderer !=null) {
            return renderer.addAeLayer(drawable);
        }else{
            return null;
        }
    }
    /**
     * 增加mv图层
     * 在start前调用
     */
    public AEMVLayer addMVLayer(String colorPath, String maskPath){

        createRender();
        if(renderer !=null){
            return renderer.addMVLayer(colorPath,maskPath);
        }else{
            return null;
        }
    }

    /**
     * 设置合成视频的背景颜色
     * [不建议使用]
     * @param red  红色分量--范围0.0--1.0f
     * @param green 绿色分量---范围0.0--1.0f;
     * @param blue  蓝色分量---范围0.0--1.0f;
     */
    public void setbackgroundColor(float red,float green,float blue){
        if (renderer != null) {
            renderer.setbackgroundColor(red,green,blue);
        } else {
            LSOLog.e("setBackGroundColor error.");
        }
    }
    /**
     * 获取Ae模板的声音,在AudioPad中的对象;
     * 在 addAudioLayer后, 调用
     * 如果没有增加其他声音, 则调用无效果;
     */
    public AudioLayer getAEAudioLayer(){
        if (renderer != null) {
            return renderer.getAEAudioLayer();
        }else{
            return null;
        }
    }
    /**
     * 增加图片图层,
     * 在start前调用
     */
    public BitmapLayer addBitmapLayer(Bitmap bmp){

        createRender();
        if(renderer !=null && bmp!=null){
            return  renderer.addBitmapLayer(bmp);
        }else {
            LSOLog.e("DrawPadAEPreview 增加图片图层失败...");
            return  null;
        }
    }
    /**
     * 增加音频,
     *
     * 在AE线程开始前 + 所有图层增加后 调用;
     *
     * 音频采样率必须和视频的声音采样率一致
     * @param srcPath
     * @return
     */
    public AudioLayer addAudioLayer(String srcPath) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer= renderer.addAudioLayer(srcPath);
            if(layer==null){
                LSOLog.e("DrawPadAEPreview addAudioLayer error. videoPath:"+srcPath);
            }
            return layer;
        } else {
            return null;
        }
    }

    /**
     * 增加音频图层,并是否循环
     * 在AE线程开始前 + 所有图层增加后 调用;
     * @return
     */
    public AudioLayer addAudioLayer(String srcPath, boolean loop) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer= renderer.addAudioLayer(srcPath);
            if(layer!=null){
                layer.setLooping(loop);
            }else{
                LSOLog.e("DrawPadAEPreview addAudioLayer error. videoPath:"+srcPath);
            }
            return layer;
        } else {
            return null;
        }
    }

    /**
     * 增加音频图层,;
     *
     * 在AE线程开始前 + 所有图层增加后 调用;
     *
     * 音频采样率必须和视频的声音采样率一致
     * @param srcPath
     * @param startFromPadTime 从Ae模板的什么时间开始增加
     * @return  返回增加后音频层, 可以用来设置音量,快慢,变声等.
     */
    public AudioLayer addAudioLayer(String srcPath, long startFromPadTime) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer=  renderer.addAudioLayer(srcPath, 0,startFromPadTime, -1);
            if(layer==null){
                LSOLog.e("DrawPadAEPreview addAudioLayer error. videoPath:"+srcPath);
            }
            return layer;
        } else {
            return null;
        }
    }

    /**
     * 增加其他声音;
     *
     *在AE线程开始前 和 所有图层增加后 调用;
     *
     *
     * 音频采样率必须和视频的声音采样率一致
     *
     * @param srcPath        路径, 可以是mp3或m4a或 带有音频的MP4文件;
     * @param startFromPadUs 从主音频的什么时间开始增加
     * @param durationUs     把这段声音多长插入进去.
     * @return 返回一个AudioLayer对象;
     */
    public AudioLayer addAudioLayer(String srcPath, long startFromPadUs,
                                    long durationUs) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer=  renderer.addAudioLayer(srcPath,0, startFromPadUs, durationUs);
            if(layer==null){
                LSOLog.e("DrawPadAEPreview addAudioLayer error. videoPath:"+srcPath);
            }
            return layer;
        } else {
            return null;
        }
    }

    /**
     * 如果要调节音量, 则增加拿到对象后, 开始调节.
     *
     * 在AE线程开始前 + 所有图层增加后 调用;
     *
     * @param srcPath
     * @param startFromPadUs   从容器的什么位置开始增加
     * @param startAudioTimeUs 裁剪声音的开始时间
     * @param endAudioTimeUs   裁剪声音的结束时间;
     * @return
     */
    public AudioLayer addAudioLayer(String srcPath, long startFromPadUs, long startAudioTimeUs, long endAudioTimeUs) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer=renderer.addAudioLayer(srcPath, startFromPadUs,startAudioTimeUs, endAudioTimeUs);
            if(layer==null){
                LSOLog.e("DrawPadAEPreview addAudioLayer error. videoPath:"+srcPath);
            }
            return layer;
        } else {
            return null;
        }
    }


    /**
     * 获取当前正在播放的第几帧;
     * @return
     */
    public int getCurrentFrame(){
        if(renderer!=null){
            return renderer.getCurrentFrame();
        }else{
            LSOLog.e("DrawPadAePreview#getCurrentFrame error.render is null");
            return 0;
        }
    }
    /**
     * 返回处理的时长
     * 单位us
     * @return
     */
    public long getDurationUS(){
        if(renderer !=null){
            return renderer.getDurationUS();
        }else {
            LSOLog.e("get duration error, aeRenderer==null.here return 1000");
            return  1000;
        }
    }

    private float playerVolume=1.0f;
    /**
     * 设置播放时的声音大小.
     * 范围是0---5.0;
     *
     * =1.0为正常.
     * =0是静音
     * =2.0是提高一倍;
     * @param value
     */
    public void setPlayerVolume(float value){
        if(renderer!=null && value>=0){
            if(isStarted){
                renderer.setPlayerVolume(value);
            }else{
                playerVolume=value;
            }
        }
    }

    private OnAePreviewProgressListener onAePreviewProgressListener=null;
    public interface  OnAePreviewProgressListener{
        void aePreviewProgress(int percent);
    }

    public void setOnAePreviewProgressListener(OnAePreviewProgressListener listener)
    {
        onAePreviewProgressListener=listener;
    }
//---------------完成
    private OnAePreviewCompletedListener onAePreviewCompletedListener=null;
    public interface  OnAePreviewCompletedListener{
        void aePreviewCompleted();
    }

    public void setOnAePreviewCompletedListener(OnAePreviewCompletedListener listener)
    {
        onAePreviewCompletedListener=listener;
    }
    //-----------错误
    private OnAePreviewErrorListener onAePreviewErrorListener=null;
    public interface  OnAePreviewErrorListener{
        void aePreviewError();
    }

    public void setOnAePreviewErrorListener(OnAePreviewErrorListener listener)
    {
        onAePreviewErrorListener=listener;
    }
    /**
     * 布局是否好.
     * @return
     */
    public boolean isLayoutValid(){
        return isLayoutOk;
    }
    public boolean isPlaying(){
        return renderer!=null && renderer.isRunning();
    }

    private int lastPercent=0; //上一个百分比;

    private boolean isStarted;
    /**
     * 开始运行
     * @return
     */
    public  boolean start(){

        if(renderer!=null && mSurfaceTexture!=null && !renderer.isRunning()){
            renderer.updateDrawPadSize(drawPadWidth,drawPadHeight);
            renderer.setSurface(new Surface(mSurfaceTexture));
            renderer.setPlayerVolume(playerVolume);
            renderer.setDrawPadProgressListener(new onDrawPadProgressListener() {
                @Override
                public void onProgress(DrawPad v, long currentTimeUs) {
                    if(onAePreviewProgressListener!=null){
                        int percent=(int)(currentTimeUs*100/getDurationUS());
                        if(percent>lastPercent){
                            lastPercent=percent;
                            onAePreviewProgressListener.aePreviewProgress(percent);
                        }
                    }
                }
            });
            renderer.setDrawPadCompletedListener(new onDrawPadCompletedListener() {
                @Override
                public void onCompleted(DrawPad v) {
                    isStarted=false;
                    if(onAePreviewCompletedListener!=null){
                        onAePreviewCompletedListener.aePreviewCompleted();
                    }
                }
            });
            renderer.setDrawPadErrorListener(new onDrawPadErrorListener() {
                @Override
                public void onError(DrawPad d, int what) {
                    if(onAePreviewErrorListener!=null){
                        onAePreviewErrorListener.aePreviewError();
                    }
                }
            });

            isStarted= renderer.startDrawPad();
            LSOLog.i("DrawpadAEPreview startPreview.ret:"+isStarted);
            return isStarted;
        }else{
            if(renderer!=null && !renderer.isRunning()){
                LSOLog.e("开启AePreview 失败.mSurfaceTexture 无效 :");
            }
            return  false;
        }
    }
    /**
     * 取消预览
     * [阻塞进行,一直等待到整个线程退出才返回.]
     */
    public void cancel(){
        if(renderer!=null){
            renderer.cancelDrawPad();
            isStarted=false;
            renderer=null;
        }
    }

    /**
     * 取消预览.
     * 异步执行, 内部会创建一个线程,直接异步取消,取消后会有回调;
     * @param listener 回调的监听
     */
    public void cancelWithAsync(OnDrawPadCancelAsyncListener listener){
        if(renderer!=null){
            renderer.cancelWithAsync(listener);
            isStarted=false;
            renderer=null;
        }
    }

    /**
     * 释放
     * 如果已经收到完成监听, 则不需要调用;
     */
    public void release(){
        if(renderer!=null){
            if( renderer.isRunning()){
                renderer.cancelWithAsync(null);
            }else{
                renderer.releaseDrawPad();
            }
        }
        isStarted=false;
    }
    /**
     * 打印json中的所有的图片和文字信息;
     * @param drawable
     */
    public static void  printDrawableInfo(LSOAeDrawable drawable){

        if(drawable!=null){

            LSOLog.i("********************Json info**********************************");
            LSOLog.i("* Json信息: ");
            LSOLog.i("* 时长(us): "+ drawable.getDurationUS());
            LSOLog.i("* 总帧数: "+drawable.getTotalFrame());
            LSOLog.i("* ");
            LSOLog.i("* ");

            LSOLog.i("* 图片信息: ");
            Map<String, LSOAeImage> maps = drawable.getJsonImages();
            for (String key : maps.keySet()) {
                LSOAeImage asset = maps.get(key);
                LSOLog.i("* 图片名字: " + asset.getFileName() + " ID号:" + asset.getId() + " width:" + asset.getWidth() +"height:" + asset.getHeight());
            }
            LSOLog.i("* ");

            //打印json中的所有图片图层
            LSOLog.i("* 图片图层(ImageLayer):");
            ArrayList<LSOAeImageLayer> imageList=drawable.getAllAeImageLayer();
            for (LSOAeImageLayer lsoAeImageLayer : imageList){
                LSOLog.i("* No."+lsoAeImageLayer.layerIndexInComposition + " 图层名字:"+ lsoAeImageLayer.layerName+" ID:" +lsoAeImageLayer.imgId+" 时长(us):"
                        + lsoAeImageLayer.durationUs+" 宽高:"+ lsoAeImageLayer.width+ " x "+ lsoAeImageLayer.height+ "" +
                        "开始帧:"+lsoAeImageLayer.startFrame+ " 结束帧:"+ lsoAeImageLayer.endFrame);
            }
            LSOLog.i("* ");
            LSOLog.i("* 文字信息(text):");


            //打印json中的所有文字
            List<LSOAeText> texts = drawable.getJsonTexts();
            for (LSOAeText text : texts) {
                LSOLog.i("* "+ text.text+ "composition:"+text.layerIndexInComposition+ "layerName:"+ text.layerName);
            }

            LSOLog.i("* ");
            LSOLog.i("*************************************************************");
        }
    }
}
