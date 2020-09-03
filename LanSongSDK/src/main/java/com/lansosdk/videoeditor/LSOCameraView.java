package com.lansosdk.videoeditor;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import com.lansosdk.LanSongFilter.LanSongFilter;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.CameraLayer;
import com.lansosdk.box.GifLayer;
import com.lansosdk.box.LSOCameraLayer;
import com.lansosdk.box.LSOCameraRunnable;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LSOMVAsset2;
import com.lansosdk.box.LSOScaleType;
import com.lansosdk.box.Layer;
import com.lansosdk.box.MVLayer2;
import com.lansosdk.box.VideoLayer2;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadErrorListener;
import com.lansosdk.box.onDrawPadOutFrameListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.box.onDrawPadRecordCompletedListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.box.onDrawPadSnapShotListener;

import java.util.concurrent.atomic.AtomicBoolean;

public class LSOCameraView extends FrameLayout {

    private  int compWidth=1080;
    private  int compHeight=1920;

    private LSOCameraRunnable renderer;


    // ----------------------------------------------
    private TextureRenderView textureRenderView;
    private SurfaceTexture mSurfaceTexture = null;
    public onViewAvailable viewAvailable = null;
    private boolean layoutOk = false;


    public LSOCameraView(Context context) {
        super(context);
        initVideoView(context);
    }

    public LSOCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView(context);
    }

    public LSOCameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LSOCameraView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
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
        textureRenderView = new TextureRenderView(getContext());
        textureRenderView.setSurfaceTextureListener(new SurfaceCallback());

        textureRenderView.setDisplayRatio(0);

        View renderUIView = textureRenderView.getView();
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        renderUIView.setLayoutParams(lp);
        addView(renderUIView);
        textureRenderView.setVideoRotation(0);
    }

    public void onResume(onViewAvailable listener) {
        viewAvailable = listener;
        if (mSurfaceTexture != null && viewAvailable !=null) {
            if (viewHeight > 0 && viewWidth >0) {

                float wantRadio = (float) compWidth / (float) compHeight;
                float viewRadio = (float) viewWidth / (float) viewHeight;
                if (wantRadio == viewRadio) {
                    layoutOk = true;
                    viewAvailable.viewAvailable();
                } else if (Math.abs(wantRadio - viewRadio) * 1000 < 16.0f) {
                    layoutOk = true;
                    viewAvailable.viewAvailable();
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
        viewAvailable =null;
        stopCamera();
    }

    public Surface getSurface() {
        if (mSurfaceTexture != null) {
            return new Surface(mSurfaceTexture);
        }
        return null;
    }

    public interface onViewAvailable {
        void viewAvailable();
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
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface,int width, int height) {

            mSurfaceTexture = surface;
            viewWidth = width;
            viewHeight = height;
            checkLayoutSize();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            mSurfaceTexture = null;
            layoutOk = false;
            release();
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    }

    private OnCompositionSizeReadyListener onCompositionSizeReadyListener;


    /**
     *准备容器的宽高;
     * 在设置后, 我们会根据这个大小来调整 这个类的大小, 从而让画面不变形;
     * @param listener 自适应屏幕后的回调
     */
    public void prepareAsync(OnCompositionSizeReadyListener listener) {

        DisplayMetrics dm = new DisplayMetrics();
        dm = getResources().getDisplayMetrics();


        if(dm.widthPixels * dm.heightPixels<1080*1920){
            compWidth=720;
            compHeight=1280;
        }

        requestLayoutCount = 0;
        onCompositionSizeReadyListener = listener;
            if (viewWidth == 0 || viewHeight == 0) {  //直接重新布局UI
                textureRenderView.setVideoSize(compWidth, compHeight);
                textureRenderView.setVideoSampleAspectRatio(1, 1);
            } else {
                float setRatio = (float) compWidth / (float) compHeight;
                float setViewRatio = (float) viewWidth / (float) viewHeight;

                if (setRatio == setViewRatio) { // 如果比例已经相等,不需要再调整,则直接显示.
                    layoutOk = true;
                    sendCompositionSizeListener();
                } else if (Math.abs(setRatio - setViewRatio) * 1000 < 16.0f) {
                    if (listener != null) {
                        layoutOk = true;
                        sendCompositionSizeListener();
                    }
                } else if (textureRenderView != null) {
                    textureRenderView.setVideoSize(compWidth, compHeight);
                    textureRenderView.setVideoSampleAspectRatio(1, 1);
                }
            }
        requestLayoutPreview();
    }

    private void sendCompositionSizeListener() {
        if (onCompositionSizeReadyListener != null) {
            onCompositionSizeReadyListener.onSizeReady();
            onCompositionSizeReadyListener = null;
        }
    }

    private int requestLayoutCount = 0;

    private void checkLayoutSize() {
        DisplayMetrics dm = getResources().getDisplayMetrics();

        if(viewWidth ==dm.widthPixels){
            float wantRatio = (float) compWidth / (float) compHeight;
            float padRatio = (float) viewWidth / (float) viewHeight;
            if (wantRatio == padRatio) { // 如果比例已经相等,不需要再调整,则直接显示.
                layoutOk = true;
                sendCompositionSizeListener();
                if (viewAvailable != null) {
                    viewAvailable.viewAvailable();
                }

            } else if (Math.abs(wantRatio - padRatio) * 1000 < 16.0f) {
                layoutOk = true;
                sendCompositionSizeListener();
                if (viewAvailable != null) {
                    viewAvailable.viewAvailable();
                }
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
            LSOLog.e("LSOCameraView layout view error.  return  callback");
            sendCompositionSizeListener();
            if (viewAvailable != null) {
                viewAvailable.viewAvailable();
            }
        } else {
            requestLayout();
        }
    }
    //---------------render start--------------------------------------------------------------------------------------
    private static boolean isCameraOpened = false;
    private int encWidth, encHeight;
    private String encPath;

    private boolean isFrontCam = false;
    private LanSongFilter initFilter = null;
    private float encodeSpeed = 1.0f;
    private onDrawPadSizeChangedListener sizeChangedCB = null;
    private onDrawPadProgressListener drawPadProgressListener = null;
    private onDrawPadRecordCompletedListener drawPadRecordCompletedListener = null;
    private boolean frameListenerInDrawPad = false;
    private onDrawPadCompletedListener drawPadCompletedListener = null;
    private onDrawPadErrorListener drawPadErrorListener = null;

    private boolean recordMic = false;

    private boolean isPauseRefresh = false;
    private boolean isPauseRecord = false;
    private boolean isZoomEvent = false;
    private float touching;
    private OnFocusEventListener onFocusListener;
    private boolean isCheckPadSize = true;
    private boolean isEnableTouch = true;

    public void setCameraFront(boolean front) {
        isFrontCam = front;
    }

    public void setEncodeParam(String path) {
            encWidth = 1088;
            encHeight = 1920;
            encPath = path;
    }

    public void setOnDrawPadProgressListener(onDrawPadProgressListener listener) {
        if (renderer != null) {
            renderer.setDrawPadProgressListener(listener);
        }
        drawPadProgressListener = listener;
    }

    public void getPhotoAsync(onDrawPadSnapShotListener listener) {
        if (renderer != null && renderer.isRunning()) {
            renderer.setDrawPadSnapShotListener(listener);
            renderer.toggleSnapShot(compWidth, compHeight);
        } else if (listener != null) {
            LSOLog.e("getPhotoAsync error.");
            listener.onSnapShot(null, null);
        }
    }


    public void setOnDrawPadCompletedListener(onDrawPadCompletedListener listener) {
        if (renderer != null) {
            renderer.setDrawPadCompletedListener(listener);
        }
        drawPadCompletedListener = listener;
    }

    public void setOnDrawPadErrorListener(onDrawPadErrorListener listener) {
        if (renderer != null) {
            renderer.setDrawPadErrorListener(listener);
        }
        drawPadErrorListener = listener;
    }

    private boolean setupDrawPad() {
        if (renderer == null) {
            pauseRecord();
            pausePreview();
            return startDrawPad(true);
        } else {
            return false;
        }
    }

    private boolean startDrawPad(boolean pauseRecord) {
        boolean ret = false;

        if (isCameraOpened) {
            LSOLog.e("DrawPad opened...");
            return false;
        }

        if (mSurfaceTexture != null && renderer == null) {

         //   renderer = new LSOCameraRunnable(getContext(), compWidth,compHeight);
            renderer = new LSOCameraRunnable(getContext(), viewWidth,viewHeight);
            renderer.setCameraParam(isFrontCam, initFilter);
            if (renderer != null) {
                renderer.setDisplaySurface(textureRenderView, new Surface(mSurfaceTexture));
                if (isCheckPadSize) {
                    encWidth = LanSongUtil.make16Multi(encWidth);
                    encHeight = LanSongUtil.make16Multi(encHeight);
                }
                renderer.setEncoderParams(encWidth, encHeight, encPath);
                // 设置DrawPad处理的进度监听, 回传的currentTimeUs单位是微秒.
                renderer.setDrawPadProgressListener(drawPadProgressListener);
                renderer.setDrawPadCompletedListener(drawPadCompletedListener);
                renderer.setOutFrameInDrawPad(frameListenerInDrawPad);
                renderer.setDrawPadRecordCompletedListener(drawPadRecordCompletedListener);
                renderer.setDrawPadErrorListener(drawPadErrorListener);
                renderer.setRecordMic(true);

                if (pauseRecord || isPauseRecord) {
                    renderer.pauseRecordDrawPad();
                }

                if (isPauseRefresh) {
                    renderer.pauseRefreshDrawPad();
                }

                renderer.adjustEncodeSpeed(encodeSpeed);

                LSOLog.d("starting run draw pad  thread...");
                ret = renderer.startDrawPad();
                isCameraOpened = ret;
                if (!ret) {
                    LSOLog.e("open LSOCameraView error.\n");
                } else {
                    renderer.setDisplaySurface(textureRenderView, new Surface(mSurfaceTexture));
                }
            }
        } else {
            LSOLog.w("start draw pad error.");
        }
        return ret;
    }




    protected BitmapLayer bgBitmapLayer = null;
    protected VideoLayer2 bgVideoLayer = null;
    protected BitmapLayer fgBitmapLayer = null;
    protected MVLayer2 fgMvLayer = null;


    public void setBackGroundBitmap(Bitmap bmp, boolean recycle) {
        if (renderer != null && isRunning() && bmp != null && !bmp.isRecycled()) {
            removeBackGroundLayer();
            bgBitmapLayer = renderer.addBitmapLayer(bmp, recycle);
            bgBitmapLayer.setScaleType(LSOScaleType.CROP_FILL_COMPOSITION);
            renderer.bringToBack(bgBitmapLayer);
        }
    }

    public void setBackGroundVideoPath(String path) {
        if (renderer != null && isRunning() && path != null) {

            removeBackGroundLayer();
            bgVideoLayer = renderer.addVideoLayer2(path);
            if (bgVideoLayer != null) {
                bgVideoLayer.setScaleType(LSOScaleType.CROP_FILL_COMPOSITION);
                bgVideoLayer.setPlayerLooping(true);
                bgVideoLayer.setAudioVolume(0.0f);
                renderer.bringToBack(bgVideoLayer);
            }
        }
    }

    public void setForeGroundBitmap(Bitmap bmp, boolean recycle) {
        if (renderer != null && isRunning() && bmp != null && !bmp.isRecycled()) {
            removeForeGroundLayer();
            fgBitmapLayer = renderer.addBitmapLayer(bmp, recycle);
            if (fgBitmapLayer != null) {
                fgBitmapLayer.setScaleType(LSOScaleType.CROP_FILL_COMPOSITION);
            }
        }
    }

    public void setForeGroundVideoPath(String colorPath, String maskPath) {
        if (renderer != null && isRunning() && colorPath != null && maskPath != null) {

            removeForeGroundLayer();
            try {
                LSOMVAsset2 lsomvAsset2 = new LSOMVAsset2(colorPath, maskPath, false);
                fgMvLayer = renderer.addMVLayer(lsomvAsset2);
                if (fgMvLayer != null) {
                    fgMvLayer.setScaleType(LSOScaleType.CROP_FILL_COMPOSITION);
                    fgMvLayer.setLooping(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    protected LSOCameraLayer cameraLayer = null;


    public void removeBackGroundLayer() {
        if (renderer != null) {
            renderer.removeLayer(bgBitmapLayer);
            bgBitmapLayer = null;
            renderer.removeLayer(bgVideoLayer);
            bgVideoLayer = null;
        }
    }

    public void removeForeGroundLayer() {
        if (renderer != null) {
            renderer.removeLayer(fgBitmapLayer);
            renderer.removeLayer(fgMvLayer);
            fgBitmapLayer = null;
            fgMvLayer = null;
        }
    }

    public boolean isGreenMatting() {
        return cameraLayer != null && cameraLayer.isGreenMatting();
    }

    public void setGreenMatting() {
        if (cameraLayer != null && !cameraLayer.isGreenMatting()) {
            cameraLayer.setGreenMatting();
        }
    }

    public void cancelGreenMatting() {
        if (cameraLayer != null && cameraLayer.isGreenMatting()) {
            cameraLayer.cancelGreenMatting();
        }
    }

    public void setFilter(LanSongFilter filter) {
        if (cameraLayer != null) {
            cameraLayer.switchFilterTo(filter);
        }
    }

    public void setBeautyLevel(float level) {
        if (cameraLayer != null) {
            cameraLayer.setBeautyLevel(level);
        }
    }


    public void changeCamera() {
        if (cameraLayer != null && isRunning() && CameraLayer.isSupportFrontCamera()) {
            // 先把DrawPad暂停运行.
            pausePreview();
            cameraLayer.changeCamera();
            resumePreview(); // 再次开启.
        }
    }

    public void changeFlash() {
        if (cameraLayer != null) {
            cameraLayer.changeFlash();
        }
    }

    public void pausePreview() {
        if (renderer != null) {
            renderer.pauseRefreshDrawPad();
        }
        isPauseRefresh = true;
    }

    public void resumePreview() {
        if (renderer != null) {
            renderer.resumeRefreshDrawPad();
        }
        isPauseRefresh = false;
    }

    public void startRecord() {
        if (renderer != null) {
            renderer.resumeRecordDrawPad();
        }
        isPauseRecord = false;
    }

    public void pauseRecord() {
        if (renderer != null) {
            renderer.pauseRecordDrawPad();
        }
        isPauseRecord = true;
    }

    public void resumeRecord() {
        if (renderer != null) {
            renderer.resumeRecordDrawPad();
        }
        isPauseRecord = false;
    }


    public boolean isRecording() {
        if (renderer != null) {
            boolean ret = renderer.isRecording();
            return ret;
        } else
            return false;
    }

    public boolean isRunning() {
        if (renderer != null)
            return renderer.isRunning();
        else
            return false;
    }


    public boolean startCamera() {
        if (setupDrawPad()) // 建立容器
        {
            cameraLayer = getCameraLayer();
            if (cameraLayer != null) {
                renderer.resumeRefreshDrawPad();
                isPauseRefresh = false;
                cameraLayer.setGreenMatting();
                return true;
            }
        }
        return false;
    }

    public void stopCamera() {
            if (renderer != null) {
                renderer.release();
                renderer = null;
            }
            isCameraOpened = false;
            cameraLayer = null;
            bgBitmapLayer = null;
            bgVideoLayer = null;
    }

    public void bringLayerToFront(Layer layer) {
        if (renderer != null) {
            renderer.bringToFront(layer);
        }
    }

    private LSOCameraLayer getCameraLayer() {
        if (renderer != null && renderer.isRunning())
            return renderer.getCameraLayer();
        else {
            LSOLog.e("getCameraLayer error.");
            return null;
        }
    }

    public BitmapLayer addBitmapLayer(Bitmap bmp) {
        if (bmp != null) {
            if (renderer != null) {
                return renderer.addBitmapLayer(bmp, null);
            }
        }
        LSOLog.e("addBitmapLayer error.");
        return null;
    }


    /**
     * 增加一个gif图层.
     *
     * @param gifPath gif的绝对地址,
     * @return
     */
    public GifLayer addGifLayer(String gifPath) {
        if (renderer != null)
            return renderer.addGifLayer(gifPath);
        else {
            LSOLog.e("addGifLayer error");
            return null;
        }
    }

    public GifLayer addGifLayer(int resId) {
        if (renderer != null)
            return renderer.addGifLayer(resId);
        else {
            LSOLog.e("addGifLayer error");
            return null;
        }
    }

    public MVLayer2 addMVLayer(LSOMVAsset2 mvAsset) {
        if (renderer != null)
            return renderer.addMVLayer(mvAsset);
        else {
            LSOLog.e("addMVLayer error render is null");
            return null;
        }
    }

    public void removeLayer(Layer layer) {
        if (layer != null) {
            if (renderer != null)
                renderer.removeLayer(layer);
            else {
                LSOLog.e("removeLayer error render is null");
            }
        }
    }

    float x1 = 0;
    float x2 = 0;
    float y1 = 0;
    float y2 = 0;

    public boolean onTouchEvent(MotionEvent event) {

        if (!isEnableTouch) { // 如果禁止了touch事件,则直接返回false;
            return false;
        }
        if (getCameraLayer() == null) {
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
                    LSOCameraLayer layer = getCameraLayer();
                    if (layer != null && event.getPointerCount() >= 2) {// 触屏两个点时才执行
                        float endDis = spacing(event);// 结束距离
                        int scale = (int) ((endDis - touching) / 10f); // 每变化10f
                        // zoom变1, 拉近拉远;
                        if (scale >= 1 || scale <= -1) {
                            int zoom = layer.getZoom() + scale;
                            layer.setZoom(zoom);
                            touching = endDis;
                        }
                    }
                }
                break;
            // 手指离开屏幕
            case MotionEvent.ACTION_UP:
                if (isRunning()) {
                    if (!isZoomEvent) {
                        LSOCameraLayer layer = getCameraLayer();
                        if (layer != null) {
                            float x = event.getX();
                            float y = event.getY();
                            if (renderer != null) {
                                x = renderer.getTouchX(x);
                                y = renderer.getTouchY(y);
                            }
                            layer.doFocus((int) x, (int) y);

                            if (onFocusListener != null) {
                                onFocusListener.onFocus((int) x, (int) y);
                            }
                        }
                    }
                }
                isZoomEvent = false;
                break;
        }
        return true;
    }

    private float spacing(MotionEvent event) {
        if (event == null) {
            return 0;
        }
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    public void setCameraFocusListener(OnFocusEventListener listener) {
        this.onFocusListener = listener;
    }


    public void setNotCheckDrawPadSize() {
        isCheckPadSize = false;
    }

    public void setEnableTouch(boolean enable) {
        isEnableTouch = enable;
    }

    public interface OnFocusEventListener {
        void onFocus(int x, int y);
    }
    /**
     * 释放所有.
     */
    public void release() {
//        cancel();
    }

}
