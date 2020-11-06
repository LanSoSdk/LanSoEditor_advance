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
import com.lansosdk.LanSongFilter.LanSongFilter;
import com.lansosdk.box.AECompositionRunnable;
import com.lansosdk.box.AEJsonLayer;
import com.lansosdk.box.AEMVLayer;
import com.lansosdk.box.AESegmentLayer;
import com.lansosdk.box.AEVideoLayer;
import com.lansosdk.box.AudioLayer;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.LSOAudioAsset;
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
import com.lansosdk.videoeditor.LanSoEditor;
import com.lansosdk.videoeditor.archApi.LanSongFileUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Deprecated
public class AECompositionView extends FrameLayout {

    private AECompositionRunnable renderer;

    private TextureRenderView2 textureRenderView;
    private SurfaceTexture mSurfaceTexture = null;
    private int drawPadWidth, drawPadHeight;
    private onViewAvailable mViewAvailable = null;
    private boolean isLayoutOk=false;
    private boolean secondLayerAdd=false;
    // ----------------------------------------------
    public AECompositionView(Context context) {
        super(context);
        initVideoView(context);
    }

    public AECompositionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView(context);
    }

    public AECompositionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AECompositionView(Context context, AttributeSet attrs, int defStyleAttr,
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
        textureRenderView = new TextureRenderView2(getContext());
        textureRenderView.setSurfaceTextureListener(new SurfaceCallback());

        textureRenderView.setDisplayRatio(0);

        View renderUIView = textureRenderView.getView();
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        renderUIView.setLayoutParams(lp);
        addView(renderUIView);
        textureRenderView.setVideoRotation(0);
    }
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
                    textureRenderView.setVideoSize(desireWidth, desireHeight);
                    textureRenderView.setVideoSampleAspectRatio(1, 1);
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
        void viewAvailable(AECompositionView v);
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
            release();
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    }

    private int desireWidth;
    private int desireHeight;
    private onDrawPadSizeChangedListener sizeChangedListener;
    private int requestLayoutCount=0;
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
            textureRenderView.setVideoSize(desireWidth, desireHeight);
            textureRenderView.setVideoSampleAspectRatio(1, 1);
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
                isLayoutOk=true;
                sizeChangedListener.onSizeChanged(drawPadWidth,drawPadHeight);
            }else if(mViewAvailable!=null){
                mViewAvailable.viewAvailable(this);
            }
        }else{
            requestLayout();
        }
    }

    public void setDrawPadSize(int width, int height, onDrawPadSizeChangedListener cb) {

        requestLayoutCount=0;
        desireWidth=width;
        desireHeight=height;

        if (width != 0 && height != 0) {
            if(drawPadWidth==0 || drawPadHeight==0){  //直接重新布局UI
                textureRenderView.setVideoSize(width, height);
                textureRenderView.setVideoSampleAspectRatio(1, 1);
                sizeChangedListener = cb;
                requestLayoutPreview();
            }else{
                float setAcpect = (float) width / (float) height;
                float setViewacpect = (float) drawPadWidth / (float) drawPadHeight;

                if (setAcpect == setViewacpect) { // 如果比例已经相等,不需要再调整,则直接显示.
                    if (cb != null) {
                        cb.onSizeChanged(width, height);
                    }
                    isLayoutOk=true;
                } else if (Math.abs(setAcpect - setViewacpect) * 1000 < 16.0f) {
                    if (cb != null) {
                        cb.onSizeChanged(width, height);
                    }
                    isLayoutOk=true;
                } else if (textureRenderView != null) {
                    textureRenderView.setVideoSize(width, height);
                    textureRenderView.setVideoSampleAspectRatio(1, 1);
                    sizeChangedListener = cb;
                }
                requestLayoutPreview();
            }
        }
    }



    public int getViewWidth() {
        return drawPadWidth;
    }


    public int getViewHeight() {
        return drawPadHeight;
    }

    public int getDrawPadWidth() {
        return drawPadWidth;
    }

    public int getDrawPadHeight() {
        return drawPadHeight;
    }
    //------------------------------------------Ae的代码-----------------------------------------------------------

    public AEVideoLayer addFirstLayer(String videoPath) throws IOException {
        createRender();
        if(renderer !=null && !renderer.isRunning() && LanSongFileUtil.fileExist(videoPath)){
            return renderer.addVideoLayer(videoPath);
        }else{
            return null;
        }
    }

    public AEJsonLayer addSecondLayer(LSOAeDrawable drawable){
        createRender();
        if(secondLayerAdd){
            LSOLog.e("已经增加第二层(AE图层). 请确认你的增加顺序.");
            return null;
        }
        if(renderer !=null && !renderer.isRunning()) {
            AEJsonLayer layer= renderer.addAeLayer(drawable);
            if(layer==null){
                LSOLog.e("AECompositionView addSecondLayer error.");
            }
            secondLayerAdd= layer!=null;
            return layer;
        }else{
            return null;
        }
    }


    public AEJsonLayer addSecondLayer(LSOAeDrawable  drawable, int startIndex, int endIndex) {
        createRender();
        if(secondLayerAdd){
            LSOLog.e("已经增加第二层(AE图层). 请确认你的增加顺序.");
            return null;
        }
        drawable.setCutFrame(startIndex,endIndex);
        AEJsonLayer layer= renderer.addAeLayer(drawable);
        if(layer==null){
            LSOLog.e("AECompositionView addSecondLayer error.");
        }
        secondLayerAdd= layer!=null;
        return layer;
    }


    public AESegmentLayer addSecondLayer(List<LSOAeDrawable> drawables){
        createRender();
        if(secondLayerAdd){
            LSOLog.e("已经增加第二层(AE图层). 请确认你的增加顺序.");
            return null;
        }
        if(renderer !=null && !renderer.isRunning() && drawables!=null && drawables.size()>0) {
            AESegmentLayer layer= renderer.addAeLayer(drawables);
            if(layer==null){
                LSOLog.e("AECompositionView addSecondLayer error.");
            }
            secondLayerAdd= layer!=null;
            return layer;
        }else{
            return null;
        }
    }
    public void addBitmap3DLayer(LSOAeDrawable drawables){
        createRender();
//        if(renderer !=null && !renderer.isRunning() && drawables!=null && drawables.size()>0) {
//            AESegmentLayer layer= renderer.addAeLayer(drawables);
//            if(layer==null){
//                LSOLog.e("AECompositionView addSecondLayer error.");
//            }
//            secondLayerAdd= layer!=null;
//            return layer;
//        }else{
//            return null;
//        }
    }


    public AEMVLayer addThirdLayer(String colorPath, String maskPath){

        createRender();
        if(renderer !=null && !renderer.isRunning()&& colorPath!=null && maskPath!=null) {
            AEMVLayer layer= renderer.addMVLayer(colorPath,maskPath);
            if(layer==null){
                LSOLog.e("AECompositionView addThirdLayer MV Video error.");
            }
            return layer;
        }else{
            return null;
        }
    }


    public AEJsonLayer addForthLayer(LSOAeDrawable drawable){
        createRender();
        if(renderer !=null && !renderer.isRunning()) {
            AEJsonLayer layer= renderer.addAeLayer(drawable);
            if(layer==null){
                LSOLog.e("AECompositionView addForthLayer  Ae Json error.");
            }
            return layer;
        }else{
            return null;
        }
    }


    public AEMVLayer addFifthLayer(String colorPath, String maskPath){

        createRender();
        if(renderer !=null && !renderer.isRunning() && colorPath!=null && maskPath!=null) {
            AEMVLayer layer= renderer.addMVLayer(colorPath,maskPath);
            if(layer==null){
                LSOLog.e("AECompositionView addFifthLayer MV Video error.");
            }
            return layer;
        }else{
            return null;
        }
    }

    public AudioLayer getAEAudioLayer(){
        if(renderer!=null){
            return renderer.getAEAudioLayer();
        }else {
            return null;
        }
    }

    public void setPreviewLooping(boolean is){
        createRender();
        if(renderer !=null) {
            renderer.setPreviewLooping(is);
        }
    }

    public void setPreviewVolume(float volume){
        if(renderer!=null){
            renderer.setPlayerVolume(volume);
        }
    }

    public void setAudioVolume(float volume){
        setPreviewVolume(volume);
    }

    public void setCutDurationUS(long durationUS){
        if(renderer!=null){
            renderer.setCutDurationUS(durationUS);
        }
    }

    public void switchFilterTo(LanSongFilter filter) {
        createRender();
        if(renderer!=null){
            renderer.switchFilterTo(filter);
        }
    }


    public void switchFilterList(List<LanSongFilter> filters) {
        createRender();
        if(renderer!=null){
            renderer.switchFilterList(filters);
        }
    }

    public BitmapLayer addBitmapLayer(Bitmap bmp){
        createRender();
        if(renderer !=null && bmp!=null){
            return  renderer.addBitmapLayer(bmp);
        }else {
            LSOLog.e("AECompositionView 增加图片图层失败...");
            return  null;
        }
    }

    public BitmapLayer addLogoLayer(Bitmap bmp, LSOLayerPosition position){
        createRender();
        if(renderer !=null && bmp!=null){
            return  renderer.addLogoLayer(bmp,position);
        }else {
            LSOLog.e("AECompositionView 增加图片图层失败...");
            return  null;
        }
    }

    public BitmapLayer addBitmapLayer(ArrayList<Bitmap> bmpList, long intervalUs,boolean loop) {
        createRender();
        if(renderer !=null && bmpList!=null){
            return  renderer.addBitmapLayer(bmpList,intervalUs,loop);
        }else {
            LSOLog.e("AECompositionView 增加图片图层失败...");
            return  null;
        }
    }

    @Deprecated
    public void addAeModuleAudio(String audioPath){
        if (renderer != null && !renderer.isRunning()) {
            renderer.addAeModuleAudio(audioPath);
        }
    }

    public void addAeModuleAudio(LSOAudioAsset audioPath){
        if (renderer != null && !renderer.isRunning()) {
            renderer.addAeModuleAudio(audioPath);
        }
    }

    public AudioLayer addAudioLayer(LSOAudioAsset audioAsset) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer= renderer.addAudioLayer(audioAsset);
            if(layer==null){
                LSOLog.e("AECompositionView addAudioLayer error. path:"+audioAsset);
            }
            return layer;
        } else {
            return null;
        }
    }


    public AudioLayer addAudioLayer(LSOAudioAsset audioAsset, boolean loop) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer= renderer.addAudioLayer(audioAsset);
            if(layer==null){
                LSOLog.e("AECompositionView addAudioLayer error. path:"+audioAsset);
            }else{
                layer.setLooping(loop);
            }
            return layer;
        } else {
            return null;
        }
    }


    public AudioLayer addAudioLayer(String srcPath) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer= renderer.addAudioLayer(srcPath);
            if(layer==null){
                LSOLog.e("AECompositionView addAudioLayer error. path:"+srcPath);
            }
            return layer;
        } else {
            return null;
        }
    }


    public AudioLayer addAudioLayer(String srcPath, boolean loop) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer= renderer.addAudioLayer(srcPath);
            if(layer!=null){
                layer.setLooping(loop);
            }else{
                LSOLog.e("AECompositionView addAudioLayer error. path:"+srcPath);
            }
            return layer;
        } else {
            return null;
        }
    }

    public AudioLayer addAudioLayer(String srcPath, long startFromPadTime) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer=  renderer.addAudioLayer(srcPath, 0,startFromPadTime, -1);
            if(layer==null){
                LSOLog.e("AECompositionView addAudioLayer error. path:"+srcPath);
            }
            return layer;
        } else {
            return null;
        }
    }

    public AudioLayer addAudioLayer(String srcPath, long startFromPadUs,
                                    long durationUs) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer=  renderer.addAudioLayer(srcPath,0, startFromPadUs, durationUs);
            if(layer==null){
                LSOLog.e("AECompositionView addAudioLayer error. path:"+srcPath);
            }
            return layer;
        } else {
            return null;
        }
    }

    public AudioLayer addAudioLayer(String srcPath, long startFromPadUs, long startAudioTimeUs, long endAudioTimeUs) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer=renderer.addAudioLayer(srcPath, startFromPadUs,
                    startAudioTimeUs, endAudioTimeUs);
            if(layer==null){
                LSOLog.e("AECompositionView addAudioLayer error. path:"+srcPath);
            }
            return layer;
        } else {
            return null;
        }
    }

    public boolean isExportRunning(){
        return  renderer!=null && renderer.isExportMode();
    }

    public boolean isRunning(){
        return renderer!=null && renderer.isRunning();
    }

    public void setOnLanSongSDKProgressListener(OnLanSongSDKProgressListener listener) {
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKProgressListener(listener);
        }
    }

    public void setOnLanSongSDKExportProgressListener(OnLanSongSDKExportProgressListener listener) {
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKExportProgressListener(listener);
        }
    }

    public void setOnLanSongSDKCompletedListener(OnLanSongSDKCompletedListener listener){
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKCompletedListener(listener);
        }
    }

    public void setOnLanSongSDKErrorListener(OnLanSongSDKErrorListener listener){
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKErrorListener(listener);
        }
    }

    public void setOnLanSongSDKPreviewBufferingListener(OnLanSongSDKPreviewBufferingListener listener)
    {
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKPreviewBufferingListener(listener);
        }
    }


    public void setOnLanSongSDKRenderProgressListener(OnLanSongSDKRenderProgressListener listener){
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKRenderProgressListener(listener);
        }
    }

    public void getThumbnailBitmapsAsynchronously(int frameCnt, OnLanSongSDKThumbnailListener listener){
        createRender();
        //判断最大是50张;
        if(renderer!=null && frameCnt>0 && frameCnt<=50 && listener!=null){
            renderer.getThumbnailBitmapsAsynchronously(frameCnt,listener);
        }
    }


    public void getThumbnailBitmapsAsynchronously(OnLanSongSDKThumbnailListener listener){
        createRender();
        if(renderer!=null){
            renderer.getThumbnailBitmapsAsynchronously(10,listener);
        }
    }

    public boolean isLayoutValid(){
        return isLayoutOk;
    }

    public boolean isPlaying(){
        return renderer!=null && renderer.isPlaying();
    }
    private int lastPercent=0; //上一个百分比;
    private boolean isStarted;


    public void setExportBitrate(int bitrate){
        createRender();
        if(renderer!=null){
            renderer.setExportBitrate(bitrate);
        }
    }


    public boolean isBuffering(){
        return renderer!=null && renderer.isBuffering();
    }


    public void setDisablePreviewBuffering(boolean disable){
        createRender();
        if(renderer!=null){
            renderer.setDisablePreviewBuffering(disable);
        }
    }

    public  boolean startPreview(){
        return startPreview(false);
    }

    public  boolean startPreview(boolean pauseAfterFirstFrame){
        if(renderer!=null && mSurfaceTexture!=null && !renderer.isRunning()){

            renderer.updateDrawPadSize(drawPadWidth,drawPadHeight);
            renderer.setSurface(new Surface(mSurfaceTexture));
            isStarted= renderer.startPreview(pauseAfterFirstFrame);
            LSOLog.i("AEComposition startPreview.ret:"+isStarted);
            secondLayerAdd=!isStarted;
            return isStarted;
        }else{
            boolean ret=renderer!=null && renderer.isRunning();
            if(ret){
                LSOLog.w("AeCompositionView  is running...");
            }
            return ret;
        }
    }

    public boolean startExport(){
        if(renderer !=null) {
            return renderer.startExport();
        }else{
            return false;
        }
    }


    public void pausePreview(){
        if(renderer!=null){
            renderer.pausePreview();
        }
    }


    public void resumePreview(){
        if(renderer!=null){
            renderer.resumePreview();
        }
    }


    public void setSeekMode(boolean is){
        if(renderer!=null){
            renderer.setSeekMode(is);
        }
    }


    public void seekToTimeUs(long us){
        if(renderer!=null){
            renderer.seekToTimeUs(us);
        }
    }

    public long getAeDurationUs(){
        long  ret=0;
        if(renderer!=null && renderer.isRunning()){
            ret=renderer.getAeDurationUs();
        }
        if(ret==0){
            ret=100*1000;
        }
        return ret;
    }

    public void cancel(){
        if(renderer!=null){
            renderer.cancel();
            renderer=null;
        }
        isStarted=false;
    }

    public void release(){
        if(renderer!=null){
            renderer.release();
            renderer=null;
        }
        isStarted=false;
    }
    private void createRender(){
        if(renderer==null){
            renderer =new AECompositionRunnable(getContext());
        }
        isStarted=false;
    }

}
