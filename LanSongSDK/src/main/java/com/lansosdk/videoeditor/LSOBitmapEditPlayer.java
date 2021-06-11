package com.lansosdk.videoeditor;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.lansosdk.LanSongFilter.LanSongFilter;
import com.lansosdk.box.LSOAsset;
import com.lansosdk.box.LSOBitmapPlayerRunnable;
import com.lansosdk.box.LSOFrameLayout;
import com.lansosdk.box.LSOLayer;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.OnAddAssetProgressListener;
import com.lansosdk.box.OnCreateListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnResumeListener;
import com.lansosdk.box.OnTakePictureListener;


public class LSOBitmapEditPlayer extends LSOFrameLayout implements ILSOTouchInterface {

    private LSOBitmapPlayerRunnable render;

    public LSOBitmapEditPlayer(Context context) {
        super(context);
    }

    public LSOBitmapEditPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LSOBitmapEditPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LSOBitmapEditPlayer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    protected void sendOnCreateListener() {
        super.sendOnCreateListener();
        if (render != null) {
            render.switchCompSurface(getCompWidth(), getCompHeight(), getSurfaceTexture(), getViewWidth(), getViewHeight());
        }
    }

    public void sendOnResumeListener() {
        super.sendOnResumeListener();
        if (render != null) {
            render.switchCompSurface(getCompWidth(), getCompHeight(), getSurfaceTexture(), getViewWidth(), getViewHeight());
        }
    }

    //旋转移动缩放
    public boolean onTextureViewTouchEvent(MotionEvent event) {
        super.onTextureViewTouchEvent(event);
        return false;
    }


    public void onCreateAsync(Bitmap bmp, boolean recycle, OnCreateListener listener) {
        if (bmp != null &&  !bmp.isRecycled()) {

            render = new LSOBitmapPlayerRunnable(getContext(), bmp,recycle);
            render.setOnErrorListener(new OnLanSongSDKErrorListener() {
                @Override
                public void onLanSongSDKError(int errorCode) {
                    setUpSuccess = false;
                    if (userErrorListener != null) {
                        userErrorListener.onLanSongSDKError(errorCode);
                    }
                }
            });
            setPlayerSizeAsync(bmp.getWidth(), bmp.getHeight(), listener);
        } else {
            listener.onCreate();
        }
    }

    public LSOLayer copySegmentLayer() {
        if (render != null) {
            return render.addSegmentLayer();
        } else {
            return null;
        }
    }


    public void onResumeAsync(OnResumeListener listener) {
        super.onResumeAsync(listener);
        if (render != null) {
            render.onActivityPaused(false);
        }
    }
    public void onDestroy() {
        super.onDestroy();
        release();
    }

    /**
     * 获取原始的分割图层;
     */
    public LSOLayer getSegmentLayer() {
        if (render != null) {
            return render.getSegmentLayer();
        } else {
            return null;
        }
    }

    /**
     * @return
     */
    public void setBackGroundPath(String path, OnAddAssetProgressListener listener1) {

        if (path != null && render != null) {
            try {
                LSOAsset asset = new LSOAsset(path);
                render.setBackGroundPath(asset, listener1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public LSOLayer addBitmapLayer(String path) {
        if (render != null && setup()) {
            LSOAsset asset = null;
            try {
                asset = new LSOAsset(path);
                return render.addBitmapLayer(asset);
            } catch (Exception e) {
                LSOLog.e("addBitmap error, " + asset.toString());
            }
        }
        return null;
    }


    public void removeLayerAsync(LSOLayer layer){
        if(layer!=null && render!=null){
            render.removeLayerAsync(layer);
        }
    }


    public LSOLayer getTouchPointLayer(float x, float y) {
        if (render != null) {
            return render.getTouchPointLayer(x, y);
        } else {
            return null;
        }
    }
    private OnLanSongSDKErrorListener userErrorListener;

    /**
     * 错误监听
     */
    public void setOnErrorListener(OnLanSongSDKErrorListener listener) {
        userErrorListener = listener;
    }

    /**
     * 开始预览
     */
    public boolean start() {
        super.start();
        if (render != null &&isLayoutValid() && setup()) {
            render.startPreview(false);
        }
        return true;
    }

    /**
     * 导出;
     * @param listener
     */
    public void exportAsync(OnTakePictureListener listener){

        if(render!=null && render.isRunning()){
            render.takePictureAsync(listener);
        }else if(listener!=null){
            listener.onTakePicture(null);
        }
    }

    /**
     * 设置背景模糊程度,
     * @param percent 0--100, 建议是10;
     */
    public void setBackGroundBlurPercent(int percent) {
        if(render!=null){
            render.setBackGroundBlurPercent(percent);
        }
    }

    /**
     * 获取背景模糊百分比;
     * @return
     */
    public int  getBackGroundBlurPercent(){
        if(render!=null){
            return render.getBackGroundBlurPercent();
        }else{
            return 0;
        }
    }


    /**
     * 背景设置滤镜
     * @param filter
     */
    public void setBackGroundFilter(LanSongFilter filter){

        if(render!=null){
            render.setBackGroundFilter(filter);
        }
    }

    /**
     * 背景获取滤镜;
     * @return
     */
    public LanSongFilter getBackGroundFilter(){
        if(render!=null){
            return render.getBackGroundFilter();
        }else{
            return null;
        }
    }



    private boolean setUpSuccess = false;

    //内部使用;
    private boolean setup() {
        if (render != null && !setUpSuccess) {
            if (!render.isRunning() && getSurfaceTexture() != null) {
                render.setSurface(getSurfaceTexture(), getViewWidth(), getViewHeight());
                setUpSuccess = render.setup();
                LSOLog.d("setup.ret:" + setUpSuccess +
                        " comp size:" + getCompWidth() + " x " + getCompHeight() + " view size :" + getViewWidth() + " x " + getViewHeight());
                return setUpSuccess;
            } else {
                return render.isRunning();
            }
        } else {
            return setUpSuccess;
        }
    }

    /**
     * 取消当前合成.
     * 取消后, 会把内部线程全部退出, 所有增加的图层都会释放;
     */
    public void cancel() {
        if (render != null) {
            render.cancel();
            render = null;
        }
    }

    private void release() {
        if (render != null) {
            render.release();
        }
        setUpSuccess = false;
    }
}