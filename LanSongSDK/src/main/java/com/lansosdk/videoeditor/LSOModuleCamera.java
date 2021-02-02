package com.lansosdk.videoeditor;

import android.annotation.TargetApi;
import android.app.Activity;
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

import com.lansosdk.box.LSOCamModule;
import com.lansosdk.box.LSOLayerPosition;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LSOModuleCameraRunnable;
import com.lansosdk.box.OnCreateListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnRecordCompletedListener;
import com.lansosdk.box.OnRecordProgressListener;
import com.lansosdk.box.OnYTDrawFrameListener;
import com.lansosdk.videoeditor.oldVersion.TextureRenderView2;

public class LSOModuleCamera extends FrameLayout {

    private LSOModuleCameraRunnable renderer;

    private final int compWidth=720;
    private final int compHeight=1280;

    // ----------------------------------------------
    private TextureRenderView2 textureRenderView;
    private SurfaceTexture mSurfaceTexture = null;
    public onViewAvailable mViewAvailable = null;
    private boolean isLayoutOk = false;


    public LSOModuleCamera(Context context) {
        super(context);
        initVideoView(context);
    }

    public LSOModuleCamera(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView(context);
    }

    public LSOModuleCamera(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LSOModuleCamera(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initVideoView(context);
    }

    private void initVideoView(Context context) {
        setTextureView();
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
    }

    private int viewWidth, viewHeight;

    private void setTextureView() {
        textureRenderView = new TextureRenderView2(getContext());
        textureRenderView.setSurfaceTextureListener(new LSOModuleCamera.SurfaceCallback());

        textureRenderView.setDisplayRatio(0);

        View renderUIView = textureRenderView.getView();
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        renderUIView.setLayoutParams(lp);
        addView(renderUIView);
        textureRenderView.setVideoRotation(0);
    }

    /**
     * 当前View有效的时候, 回调监听;
     */
    public void onResume(onViewAvailable listener) {
        mViewAvailable = listener;
        if (mSurfaceTexture != null && mViewAvailable!=null) {
            if (viewHeight > 0 && viewWidth >0) {

                float wantRadio = (float) compWidth / (float) compHeight;
                float viewRadio = (float) viewWidth / (float) viewHeight;

                if (wantRadio == viewRadio) { // 如果比例已经相等,不需要再调整,则直接显示.
                    isLayoutOk = true;
                    mViewAvailable.viewAvailable(this);
                } else if (Math.abs(wantRadio - viewRadio) * 1000 < 16.0f) {
                    isLayoutOk = true;
                    mViewAvailable.viewAvailable(this);
                } else {
                    textureRenderView.setVideoSize(compWidth, compHeight);
                    textureRenderView.setVideoSampleAspectRatio(1, 1);
                    LSOLog.d("setOnViewAvailable layout again...");
                    requestLayoutPreview();
                }
            }
        }
    }
    public void onPause(){
        mViewAvailable=null;
    }

    public Surface getSurface() {
        if (mSurfaceTexture != null) {
            return new Surface(mSurfaceTexture);
        }
        return null;
    }

    public interface onViewAvailable {
        void viewAvailable(LSOModuleCamera v);
    }

    private class SurfaceCallback implements TextureView.SurfaceTextureListener {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                              int width, int height) {
            mSurfaceTexture = surface;
            viewWidth = width;
            viewHeight = height;
            checkLayoutSize();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                                int width, int height) {
            mSurfaceTexture = surface;
            viewWidth = width;
            viewHeight = height;
            checkLayoutSize();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            mSurfaceTexture = null;
            isLayoutOk = false;
            release();
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    }

    private OnCreateListener sizeChangedListener;


    /**
     *准备容器的宽高;
     * 在设置后, 我们会根据这个大小来调整 这个类的大小, 从而让画面不变形;
     * @param listener 自适应屏幕后的回调
     */
    public void prepareAsync(OnCreateListener listener) {
        requestLayoutCount = 0;
        sizeChangedListener = listener;
        if (viewWidth == 0 || viewHeight == 0) {  //直接重新布局UI
            textureRenderView.setVideoSize(compWidth, compHeight);
            textureRenderView.setVideoSampleAspectRatio(1, 1);
        } else {
            float setRatio = (float) compWidth / (float) compHeight;
            float setViewRatio = (float) viewWidth / (float) viewHeight;

            if (setRatio == setViewRatio) { // 如果比例已经相等,不需要再调整,则直接显示.
                isLayoutOk = true;
                sendCompositionSizeListener();
            } else if (Math.abs(setRatio - setViewRatio) * 1000 < 16.0f) {
                if (listener != null) {
                    isLayoutOk = true;
                    sendCompositionSizeListener();
                }
            } else if (textureRenderView != null) {
                textureRenderView.setVideoSize(compWidth, compHeight);
                textureRenderView.setVideoSampleAspectRatio(1, 1);
                sizeChangedListener = listener;
            }
        }
        requestLayoutPreview();
    }

    private void sendCompositionSizeListener() {
        if (sizeChangedListener != null) {
            sizeChangedListener.onCreate();
            sizeChangedListener = null;
        }
    }

    private int requestLayoutCount = 0;

    /**
     * 检查得到的大小, 如果和view成比例,则直接回调; 如果不成比例,则重新布局;
     */
    private void checkLayoutSize() {
        float desireRatio = (float) compWidth / (float) compHeight;
        float padRatio = (float) viewWidth / (float) viewHeight;

        if (desireRatio == padRatio) { // 如果比例已经相等,不需要再调整,则直接显示.
            isLayoutOk = true;
            sendCompositionSizeListener();
            if (mViewAvailable != null) {
                mViewAvailable.viewAvailable(this);
            }
        } else if (Math.abs(desireRatio - padRatio) * 1000 < 16.0f) {
            isLayoutOk = true;
            sendCompositionSizeListener();
            if (mViewAvailable != null) {
                mViewAvailable.viewAvailable(this);
            }
        } else {
            textureRenderView.setVideoSize(compWidth, compHeight);
            textureRenderView.setVideoSampleAspectRatio(1, 1);
            LSOLog.d("checkLayoutSize no  right, layout again...");
            requestLayoutPreview();
        }
    }

    private void requestLayoutPreview() {
        requestLayoutCount++;
        if (requestLayoutCount > 3) {
            LSOLog.e("YTCameraView layout view error.  return  callback");
            sendCompositionSizeListener();
            if (mViewAvailable != null) {
                mViewAvailable.viewAvailable(this);
            }
        } else {
            requestLayout();
        }
    }

    //-------------------------------camera code  start--------------------------------
    /**
     * 设置模板资源;
     * @param asset
     * @throws Exception
     */
    public void setModule(LSOCamModule asset){
        setup();
        if (renderer != null) {
            renderer.setModule(asset);
        }
    }

    /**
     * 增加logo图片, 此图片没有做大小缩放, 容器相对是720P的大小而言, 如果你图片太大需要缩放处理
     * @param bmp 图片
     * @param position 位置
     */
    public void setBitmapLogo(Bitmap bmp, LSOLayerPosition position){
        setup();
        if (renderer != null) {
            renderer.setBitmapLogo(bmp,position);
        }
    }
    /**
     * 增加logo图片,
     * @param bmp 图片
     * @param x 图片在容器的中心点X坐标
     * @param y 图片在容器的中心点y坐标;
     */
    public void setBitmapLogo(Bitmap bmp,float x, float y){
        setup();
        if (renderer != null) {
            renderer.setBitmapLogo(bmp,x,y);
        }
    }

    /**
     * 是否显示前景的mv效果;默认是显示. 如果前景影响了效果,可以关闭这一层;
     * @param is
     */
    public void setDisplayMV(boolean is){
        setup();
        if (renderer != null) {
            renderer.setDisplayMV(is);
        }
    }



    /**
     *
     * @param act
     * @param isBackCamera
     */
    public void setBackCamera(Activity act, boolean isBackCamera) {
        setup();
        if(renderer!=null){
            renderer.setBackCamera(act,isBackCamera);
        }
    }

    /**
     * 打开闪光灯
     */
    public void turnOffFlash(){
        if(renderer!=null){
            renderer.turnOffFlash();
        }
    }


    /**
     * 关闭闪光灯
     */
    public void turnOnFlash(){
        if(renderer!=null){
            renderer.turnOnFlash();
        }
    }

    /**
     * 闪光灯是否打开
     * @return
     */
    public boolean isFlashOn(){
        return renderer!=null && renderer.isFlashOn();
    }



    public void setRecordMic(boolean is){
        if(renderer!=null){
            renderer.setRecordMic(is);
        }
    }
    public void setExportModuleVolume(float value){
        if(renderer!=null){
            renderer.setExportModuleVolume(value);
        }
    }

    private boolean mirrorHorizontal =false;
    /**
     * 设置画面镜像
     * @param flipHorizontal 水平镜像
     */
    public void setCameraMirror(boolean flipHorizontal) {
        if(renderer!=null){
            this.mirrorHorizontal =flipHorizontal;
            renderer.setCameraMirror(flipHorizontal);
        }
    }

    /**
     * 摄像头是否水平镜像;
     * @return
     */
    public boolean isMirrorHorizontal(){
        return mirrorHorizontal;
    }

    /**
     * 开始预览
     */
    public void startPreview() {
        setup();
        if (renderer != null) {
            renderer.startPreview();
        }else{
            LSOLog.e(" render is null. mSurfaceTexture:"+ mSurfaceTexture);
        }
    }

    /**
     * 停止预览;
     */
    public void stopPreview(){
        if(renderer!=null){
            renderer.cancel();
        }
    }

    /**
     * 当前DrawPad容器 线程是否在工作.
     *
     * @return
     */
    public boolean isRunning() {
        if (renderer != null)
            return renderer.isRunning();
        else
            return false;
    }

    public  void setAudioPlayerVolume(float volume){
        if(renderer!=null){
            renderer.setAudioPlayerVolume(volume);
        }
    }

    /**
     * 切换前后相机;
     */
    public void changeCamera(){
        if(renderer!=null){
            renderer.changeCamera();
        }
    }

    /**
     * 当前相机是否是前置摄像头;
     * @return
     */
    public boolean isFrontCamera(){
        return renderer!=null && renderer.isFrontCamera();
    }

    /**
     * 开始录制
     */
    public void startRecord() {
        setup();
        if (renderer != null && renderer.isRunning()) {
            renderer.startRecord();
        }
    }

    public void pauseRecord(){
        if (renderer != null && renderer.isRunning()) {
            renderer.pauseRecord();
        }
    }

    /**
     * 停止录制
     */
    public void stopRecordASync() {
        if (renderer != null && renderer.isRunning()) {
            renderer.stopRecord();
        }
    }

    /**
     * 是否在录制
     * @return
     */
    public boolean isRecording() {
        return renderer != null && renderer.isRecording();
    }
    /**
     * 录制进度
     * @param listener 录制返回的时间戳,是相对于整个模板时长而言;
     */
    public void setOnRecordProgressListener(OnRecordProgressListener listener){
        setup();
        if(renderer!=null){
            renderer.setOnRecordProgressListener(listener);
        }
    }
    public void setOnRecordCompletedListener(OnRecordCompletedListener listener){
        setup();
        if(renderer!=null){
            renderer.setOnRecordCompletedListener(listener);
        }
    }

    /**
     * 错误监听;
     * @param listener
     */
    public void setOnLanSongSDKErrorListener(OnLanSongSDKErrorListener listener){
        setup();
        if(renderer!=null){
            renderer.setOnLanSongSDKErrorListener(listener);
        }
    }

    /**
     * 外部分割回调;
     * @param listener
     */
    public void setOnYTCustomFrameOutListener(OnYTDrawFrameListener listener) {
        setup();
        if (renderer != null) {
            renderer.setOnYTCustomFrameOutListener(listener);
        }
    }

    /**
     * 取消预览;
     */
    public void cancel(){
        if (renderer != null) {
            renderer.release();
            renderer = null;
        }
    }

    private void setup() {
        if (mSurfaceTexture != null && renderer == null) {
            renderer = new LSOModuleCameraRunnable(getContext(), compWidth, compHeight);
            renderer.setDisplaySurface(viewWidth,viewHeight, new Surface(mSurfaceTexture));
        }
    }


    /**
     * 释放所有.
     */
    public void release() {
        cancel();
    }

}
