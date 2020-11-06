package com.lansosdk.videoeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;

import com.lansosdk.LanSongFilter.LanSongFilter;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.CameraLayer;
import com.lansosdk.box.LSOAsset;
import com.lansosdk.box.LSOAudioLayer;
import com.lansosdk.box.LSOCamAudioLayer;
import com.lansosdk.box.LSOCamLayer;
import com.lansosdk.box.LSOCameraRunnable;
import com.lansosdk.box.LSOFrameLayout;
import com.lansosdk.box.LSOLayerPosition;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LSORecordFile;
import com.lansosdk.box.LSOScaleType;
import com.lansosdk.box.MVLayer2;
import com.lansosdk.box.OnCreateListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnRecordCompletedListener;
import com.lansosdk.box.OnRecordProgressListener;
import com.lansosdk.box.OnResumeListener;
import com.lansosdk.box.OnTakePictureListener;
import com.lansosdk.box.VideoLayer2;

import java.util.List;

public class LSOCamera extends LSOFrameLayout {

    private int compWidth = 1080;
    private int compHeight = 1920;

    private LSOCameraRunnable render;

    public LSOCamera(Context context) {
        super(context);
    }

    public LSOCamera(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LSOCamera(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public LSOCamera(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    //---------------------copy code start---------------------
    protected void sendOnCreateListener() {
        super.sendOnCreateListener();
        if (render != null) {
            render.setSurface(compWidth, compHeight, getSurfaceTexture(), getViewWidth(), getViewHeight());
        }
    }

    public void sendOnResumeListener() {

        super.sendOnResumeListener();
        if (render != null) {
            render.setSurface(compWidth, compHeight, getSurfaceTexture(), getViewWidth(), getViewHeight());
        }

    }

    //旋转移动缩放
    public boolean onTextureViewTouchEvent(MotionEvent event) {
        super.onTextureViewTouchEvent(event);
//        if(render !=null){
//            render.onTextureViewTouchEvent(event);
//        }
        return true;
    }


    public void onCreateAsync(OnCreateListener listener) {
        DisplayMetrics dm = new DisplayMetrics();
        dm = getResources().getDisplayMetrics();

        LSOLog.d(getClass().getName() +" onCreateAsync  size :" + dm.widthPixels + " x "+ dm.heightPixels);

        setup();

        if (dm.widthPixels * dm.heightPixels < 1080 * 1920) {
            compWidth = 720;
            compHeight = 1280;
        }

        setCompositionSizeAsync(compWidth, compHeight, listener);
    }

    public void onResumeAsync(OnResumeListener listener) {
        super.onResumeAsync(listener);
        if (render != null) {
            render.onActivityPaused(false);
        }
    }

    public void onPause() {
        super.onPause();
        if (render != null) {
            if(isRecording()){
                pauseRecord();
            }
            render.onActivityPaused(true);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        cancel();
    }

    //---------------render start-----------------------------------------
    private static boolean isCameraOpened = false;

    private boolean frontCamera = false;
    private long recordDurationUs = Long.MAX_VALUE;

    private OnFocusEventListener onFocusListener;

    /**
     * 设置前置摄像头,在开始前设置;默认是后置摄像头;在录制前设置;
     * Set the front camera, set it before starting; the default is the back camera; set it before recording;
     * @param is
     */
    public void setFrontCamera(boolean is) {
        if (!isRunning()) {
            frontCamera = is;
        } else {
            LSOLog.e("setFrontCamera error render have been setup .");
        }
    }

    /**
     * 设置录制的时长, 默认无限长; 在录制前设置;
     * @param durationUs 录制时长
     */
    public void setRecordDuration(long durationUs) {
        if (durationUs > 0 && !isRunning() && getRecordDurationUs()==0) {
            recordDurationUs = durationUs;
        }
    }

    /**
     * 禁止麦克风的声音;
     * 增加外部音频时,自动禁止mic声音;
     *
     * @param is
     */
    public void setMicMute(boolean is) {

        if (render != null && !isRecording()) {
            render.setMicMute(is);
        }
    }


    public boolean isRunning() {
        return render != null && render.isRunning();
    }


    public void setOnRecordCompletedListener(OnRecordCompletedListener listener) {
        if (render != null) {
            render.setOnRecordCompletedListener(listener);
        }
    }

    /**
     * OnRecordProgressListener中的两个参数: 当前录制的时长, 总录制的时长;
     *
     * @param listener
     */
    public void setOnRecordProgressListener(OnRecordProgressListener listener) {
        if (render != null) {
            render.setOnRecordProgressListener(listener);
        }
    }

    public void setOnLanSongSDKErrorListener(OnLanSongSDKErrorListener listener) {
        if (render != null) {
            render.setOnLanSongSDKErrorListener(listener);
        }
    }

    public boolean start() {
        super.start();
        if (isCameraOpened) {
            LSOLog.d("LSOCamera  start error. is opened...");
            return true;
        }
        if (getSurfaceTexture() != null) {
            render.setFrontCamera(frontCamera);
            render.setRecordDurationUs(recordDurationUs);

            if (render != null) {
                render.setDisplaySurface(getSurfaceTexture(), getViewWidth(), getViewHeight());
                isCameraOpened = render.start();
                if (!isCameraOpened) {
                    LSOLog.e("open LSOCameraView error.\n");
                } else {
                    LSOLog.d("LSOCameraView start preview...");
                }
            }
        } else {
            LSOLog.w("mSurfaceTexture error.");
        }
        return isCameraOpened;
    }

    /**
     * @param filter
     */
    public void setFilter(LanSongFilter filter) {
        if (render != null) {
            render.setFilter(filter);
        }
    }

    /**
     * 美颜, 范围是0.0---1.0; 0.0 不做磨皮, 1.0:完全磨皮;
     *
     * @param level
     */
    public void setBeautyLevel(float level) {
        if (render != null) {
            render.setBeautyLevel(level);
        }
    }

    /**
     * 禁止美颜;
     */
    public void setDisableBeauty() {
        if (render != null) {
            render.setBeautyLevel(0.0f);
        }
    }


    /**
     * 禁止绿幕抠图
     * @return
     */
    public boolean isGreenMatting() {
        return render != null && render.isGreenMatting();
    }

    /**
     * 设置绿幕抠图
     */
    public void setGreenMatting() {
        if (render != null) {
            render.setGreenMatting();
        }else{
            LSOLog.e("setGreenMatting error. render is null");
        }
    }

    /**
     * 取消绿幕抠图
     * cancel  green matting;
     */
    public void cancelGreenMatting() {
        if (render != null) {
            render.cancelGreenMatting();
        }
    }


    private String bgPath=null;
    /**
     * 设置背景图片;
     *
     * @param path
     */
    public void setBackGroundBitmapPath(String path) {
        setBackGroundPath(path);
    }


    /**
     * 设置背景路径, 路径可以是图片或视频
     * path  support  image and video.
     * @param path 路径
     */
    public void setBackGroundPath(String path) {
        if(bgPath!=null && bgPath.equals(path)){
            return;
        }
        if (render != null && isRunning() && path != null) {
            try {
                bgPath=path;
                render.setBackGroundAsset(new LSOAsset(path));
            } catch (Exception e) {
                e.printStackTrace();
                LSOLog.e("setBackGroundPath error, input is:"+ path);
            }
        }
    }

    public void removeBackGroundLayer() {
        if (render != null) {
            bgPath=null;
            render.removeBackGroundLayer();
        }
    }


    private String fgBitmapPath=null;
    private String fgColorPath=null;

    /**
     * 设置前景图片;
     * @param path 图片路径
     */
    public void setForeGroundBitmap(String path) {

        if(fgBitmapPath!=null && fgBitmapPath.equals(path)){
            return;
        }

        if (render != null && isRunning()) {
            try {
                fgBitmapPath=path;
                fgColorPath=null;
                LSOLog.d("Camera setForeGroundBitmap...");
                render.setForeGroundBitmap(new LSOAsset(path));
            } catch (Exception e) {
                e.printStackTrace();
                fgBitmapPath=null;
            }
        }
    }

    /**
     * 设置前景透明动画
     * @param colorPath mv color path
     * @param maskPath mv mask path
     */
    public void setForeGroundVideoPath(String colorPath, String maskPath) {

        if(fgColorPath!=null && fgColorPath.equals(colorPath)){
            return;
        }

        if (render != null && isRunning()) {
            fgBitmapPath=null;
            fgColorPath=colorPath;
            render.setForeGroundVideoPath(colorPath, maskPath);
        } else {
            LSOLog.e("add MVLayer error!");
        }
    }
    public void removeForeGroundLayer() {
        fgBitmapPath=null;
        fgColorPath=null;

        if (render != null) {
            render.removeForeGroundLayer();
        }
    }
    /**
     * 拍照
     * @param listener
     */
    public void takePictureAsync(OnTakePictureListener listener) {
        if (render != null && render.isRunning()) {
            render.takePictureAsync(listener);
        } else if (listener != null) {
            listener.onTakePicture(null);
        }
    }

    /**
     * 切换摄像头.
     * change front or back camera;
     */
    public void changeCamera() {
        if (render != null && !isRecording() && CameraLayer.isSupportFrontCamera()) {
            frontCamera = !frontCamera;
            render.changeCamera();
        }
    }
    /**
     * 是否是前置摄像头
     * @return
     */
    public boolean isFrontCamera() {
        return frontCamera;
    }

    /**
     * 开启或关闭闪光灯; 默认是不开启;
     * Turn on or off the flash; the default is not to turn on;
     */
    public void changeFlash() {
        if (render != null) {
            render.changeFlash();
        }
    }


    /**
     * 开始录制
     */
    public void startRecord() {
        if (render != null && !render.isRecording()) {
            render.startRecord();
        }
    }

    /**
     * 是否在录制中.
     * @return
     */
    public boolean isRecording() {
        return render != null && render.isRecording();
    }

    /**
     * 暂停录制
     * 暂停后, 会录制一段视频, 录制的这段视频在onDestory中释放;
     */
    public void pauseRecord() {
        if (render != null && render.isRecording()) {
            render.pauseRecord();
        }
    }

    /**
     * 异步停止录制, 停止后会通过完成回调返回录制后的路径;
     * Stop the recording asynchronously,
     * and return to the recorded path through the completion callback after stopping;
     */
    public void stopRecordAsync() {
        if (render != null) {
            render.stopRecordAsync();
        }
    }

    /**
     * 删除上一段的录制
     *
     */
    public void deleteLastRecord() {
        if (render != null) {
            render.deleteLastRecord();
        }
    }

    /**
     * 获取录制总时长;
     *
     * @return
     */
    public long getRecordDurationUs() {
        if (render != null) {
            return render.getRecordDurationUs();
        } else {
            return 10;
        }
    }
    public List<LSORecordFile> getRecordFiles(){
        if(render!=null){
            return render.getRecordFiles();
        }else{
            LSOLog.e("getRecordFile error. render is null. ");
            return null;
        }
    }
    /**
     * add  audio path
     * 有外部声音的时候, 自动屏蔽mic的声音; 因为mic会存在音乐的声音;故屏蔽mic
     *
     * @param path
     * @param looping
     * @return
     */
    public void setAudioLayer(String path, boolean looping) {
        setAudioLayer(path,0,Long.MAX_VALUE,looping);
    }
    /**
     * 增加外部声音.
     *
     * @param path
     * @param cutStartUs
     * @param cutEndUs
     * @param looping
     * @return
     */
    public void setAudioLayer(String path, long cutStartUs, long cutEndUs, boolean looping) {
        if (render!=null && !render.isRecording()) {
            render.removeAudioLayer();
            if(path!=null){
                render.addAudioLayer(path, cutStartUs, cutEndUs,looping);
            }
        }
    }
    public void removeAudioLayer(){
        if (render!=null && !render.isRecording()) {
            render.removeAudioLayer();
        }
    }


    /**
     * 如果不设置logo,则不设置或设置为null
     * @param asset
     * @param position
     * @return
     */
    public LSOCamLayer setLogoLayer(LSOAsset asset, LSOLayerPosition position) {
        if (render != null && isRunning()) {
            return render.setLogoLayer(asset, position);
        } else {
            return null;
        }
    }

    private float spacing(MotionEvent event) {
        if (event == null) {
            return 0;
        }
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private boolean isEnableTouch = true;

    public void setCameraFocusListener(OnFocusEventListener listener) {
        this.onFocusListener = listener;
    }

    public void setTouchEnable(boolean enable) {
        isEnableTouch = enable;
    }

    public interface OnFocusEventListener {
        void onFocus(int x, int y);
    }

    float x1 = 0;
    float x2 = 0;
    float y1 = 0;
    float y2 = 0;
    private boolean isZoomEvent = false;
    private float touching;

    public boolean onTouchEvent(MotionEvent event) {

        if (!isEnableTouch || render == null) { // 如果禁止了touch事件,则直接返回false;
            return false;
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            // 手指压下屏幕
            case MotionEvent.ACTION_DOWN:
                isZoomEvent = false;
                x1 = event.getX();
                y1 = event.getY();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                // 计算两个手指间的距离
                if (isRunning()) {
                    touching = spacing(event);
                    isZoomEvent = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isZoomEvent && isRunning()) {
                    if (event.getPointerCount() >= 2) {// 触屏两个点时才执行
                        float endDis = spacing(event);// 结束距离
                        int scale = (int) ((endDis - touching) / 10f); // 每变化10f
                        // zoom变1, 拉近拉远;
                        if (scale != 0) {
                            int zoom = render.getZoom() + scale;
                            render.setZoom(zoom);
                            touching = endDis;
                        }
                    }
                }
                break;
            // 手指离开屏幕
            case MotionEvent.ACTION_UP:
                if (isRunning()) {
                    if (!isZoomEvent) {
                        float x = event.getX();
                        float y = event.getY();
                        render.doFocus((int) x, (int) y);

                        if (onFocusListener != null) {
                            onFocusListener.onFocus((int) x, (int) y);
                        }
                    }
                }
                isZoomEvent = false;
                break;
        }
        return true;
    }

    private void setup() {
        if (render == null) {
            render = new LSOCameraRunnable(getContext(), compWidth, compHeight);
        }
    }

    public void cancel() {
        isCameraOpened = false;
        if (render != null) {
            render.cancel();
            render = null;
        }
    }
}
