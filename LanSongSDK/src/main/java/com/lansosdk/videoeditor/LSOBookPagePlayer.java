package com.lansosdk.videoeditor;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.lansosdk.box.LSOAsset;
import com.lansosdk.box.LSOAudioLayer;
import com.lansosdk.box.LSOExportType;
import com.lansosdk.box.LSOFrameLayout;
import com.lansosdk.box.LSOLayer;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LSOBookPagePlayerRender;
import com.lansosdk.box.OnAddAssetProgressListener;
import com.lansosdk.box.OnCreateListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKExportCompletedListener;
import com.lansosdk.box.OnLanSongSDKExportProgressListener;
import com.lansosdk.box.OnLanSongSDKPlayCompletedListener;
import com.lansosdk.box.OnLanSongSDKPlayProgressListener;
import com.lansosdk.box.OnLanSongSDKStateChangedListener;
import com.lansosdk.box.OnLanSongSDKUserSelectedLayerListener;
import com.lansosdk.box.OnResumeListener;

import java.util.List;


public class LSOBookPagePlayer extends LSOFrameLayout implements ILSOTouchInterface{
    private LSOBookPagePlayerRender render;

    public LSOBookPagePlayer(Context context) {
        super(context);
    }

    public LSOBookPagePlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LSOBookPagePlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LSOBookPagePlayer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    protected void sendOnCreateListener() {
        super.sendOnCreateListener();
        if (render != null) {
            render.setInputSize(getInputCompWidth(), getInputCompHeight());
            render.switchCompSurface(getCompWidth(), getCompHeight(), getSurfaceTexture(), getViewWidth(), getViewHeight());
        }
    }

    public void sendOnResumeListener() {
        super.sendOnResumeListener();
        if (render != null) {
            render.setInputSize(getInputCompWidth(), getInputCompHeight());
            render.switchCompSurface(getCompWidth(), getCompHeight(), getSurfaceTexture(), getViewWidth(), getViewHeight());
        }
    }

    //旋转移动缩放
    public boolean onTextureViewTouchEvent(MotionEvent event) {
        if(enableTouch){
            super.onTextureViewTouchEvent(event);
            return render!=null && render.onTextureViewTouchEvent(event);
        }
        return false;
    }


    private boolean enableTouch = true;
    public void setTouchEnable(boolean enable) {
        enableTouch = enable;
    }




    private List<Bitmap> createAssetArray;


    /**
     * 创建
     * @param arrays 数组最大是50张, 每个图片最大是1080P
     * @param listener
     */
    public void onCreateAsync(List<Bitmap> arrays, OnCreateListener listener) {


        if (arrays != null && arrays.size() > 0) {
            if(arrays.size()>50){
                LSOLog.e("BookPage max item is 50.  input size is :" + arrays.size());
            }

            int width = arrays.get(0).getWidth();
            int height = arrays.get(0).getHeight();

            createAssetArray = arrays;
            setPlayerSizeAsync(width, height, listener);
        } else {
            listener.onCreate();
        }
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
            render.onActivityPaused(true);
        }

        pause();
    }

    public void onDestroy() {
        super.onDestroy();
        if (render != null) {
            render.release();
            render = null;
        }
        setUpSuccess = false;
    }

    //------------------------------------------------player code-----------------------------------------------

    /**
     * 设置翻页的时候, 背景页的颜色;默认是
     * @param r
     * @param g
     * @param b
     */
    public void setBackPageColor(float r,float g, float b){
        if(render!=null){
            render.setBackPageColor(r,g,b);
        }
    }

    /**
     * 设置需要拼接的资源, 只能设置一次.
     *
     * @param listener
     */
    public void prepare(OnAddAssetProgressListener listener) {
        if (createAssetArray != null && createAssetArray.size() > 0) {
            createRender();
            if (render != null && setup()) {
                render.setListAsync(createAssetArray, listener);
            }
        } else {
            LSOLog.e("setConcatAssetList error.  hasSetConcatAsset==true");
        }
    }


    public LSOLayer addBitmapLayer(String path, long atCompUs) {
        createRender();
        LSOLog.d("EditPlayer addBitmapLayer...");
        if (render != null && setup() ) {
            LSOAsset asset = null;
            try {
                asset = new LSOAsset(path);
                return render.addBitmapLayer(asset, atCompUs);
            } catch (Exception e) {
                LSOLog.e("addBitmap error, " + asset.toString());
            }
        }
        return null;
    }


    /**
     * 增加图片图层;
     *
     * @param bmp      图片
     * @param atCompUs
     * @return
     */
    public LSOLayer addBitmapLayer(Bitmap bmp, long atCompUs) {
        createRender();
        try {
            if (render != null && setup() && bmp != null && !bmp.isRecycled()) {
                return render.addBitmapLayer(new LSOAsset(bmp), atCompUs);
            }
        } catch (Exception e) {
            LSOLog.e("add BitmapLayer  error. " ,e);
        }
        LSOLog.e("addBitmap error, ");
        return null;
    }



    /**
     * 增加声音图层;
     *
     * @param path            声音的完整路径;
     * @param startTimeOfComp 从合成的什么位置开始增加;
     * @return 返回声音对象;
     */
    public LSOAudioLayer addAudioLayer(String path, long startTimeOfComp) {
        createRender();
        if (render != null && setup()) {
            return render.addAudio(path, startTimeOfComp);
        } else {
            return null;
        }
    }



    /**
     * 删除声音图层
     *
     * @param layer
     */
    public void removeAudioLayerAsync(LSOAudioLayer layer) {
        if (render != null) {
            render.removeAudioLayerAsync(layer);
        }
    }



    public LSOLayer getTouchPointLayer(float x, float y){
        if(render!=null){
            return render.getTouchPointLayer(x,y);
        }else {
            return null;
        }
    }

    /**
     * 设置容器的背景颜色;
     *
     * @param color
     */
    public void setBackgroundColor(int color) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        padBGRed = (float) red / 255f;
        padBGGreen = (float) green / 255f;
        padBGBlue = (float) blue / 255f;
        if (render != null) {
            render.setBackGroundColor(padBGRed, padBGGreen, padBGBlue, 1.0f);
        }
    }

    /**
     * 画面是否在播放
     *
     * @return
     */
    public boolean isPlaying() {
        return render != null && render.isPlaying();
    }

    /**
     * 合成容器是否在运行.
     * @return
     */
    public boolean isRunning() {
        return render != null && render.isRunning();
    }

    /**
     * 是否在导出;
     *
     * @return
     */
    public boolean isExporting() {
        return render != null && render.isExporting();
    }


    /**
     * 获取当前时间, 单位微秒;
     *
     * @return 当前时间
     */
    public long getCurrentTimeUs() {
        if (render != null) {
            return render.getCurrentTimeUs();
        } else {
            return 0;
        }
    }

    /**
     * 播放进度回调
     * @param listener
     */
    public void setOnLanSongSDKPlayProgressListener(OnLanSongSDKPlayProgressListener listener) {
        createRender();
        if (render != null) {
            render.setOnPlayProgressListener(listener);
        }
    }

    /**
     * 视频播放完成进度;
     * @param listener
     */
    public void setOnPlayCompletedListener(OnLanSongSDKPlayCompletedListener listener) {
        createRender();
        if (render != null) {
            render.setOnPlayCompletedListener(listener);
        }
    }

    /**
     * 导出进度回调;
     * @param listener
     */
    public void setOnExportProgressListener(OnLanSongSDKExportProgressListener listener) {
        createRender();
        if (render != null) {
            render.setOnExportProgressListener(listener);
        }
    }

    /**
     * 导出完成回调;
     * @param listener
     */
    public void setOnExportCompletedListener(OnLanSongSDKExportCompletedListener listener) {
        createRender();
        if (render != null) {
            render.setOnExportCompletedListener(listener);
        }
    }
    /**
     * 当播放状态改变后, 触发回调. 播放有暂停和播放两种状态;
     * @param listener
     */
    public void setOnLanSongSDKStateChangedListener(OnLanSongSDKStateChangedListener listener){
        createRender();
        if(render!=null){
            render.setOnLanSongSDKStateChangedListener(listener);
        }
    }


    private OnLanSongSDKErrorListener userErrorListener;

    /**
     * 错误监听
     */
    public void setOnErrorListener(OnLanSongSDKErrorListener listener) {
        createRender();
        userErrorListener = listener;
    }

    /**
     * 开始预览
     */
    public boolean start() {
        super.start();
        if (render != null) {
            render.startPreview(false);
        }
        return true;
    }

    /**
     */
    public void startPreview(boolean pauseAfterFirstFrame) {
        if (render != null) {
            render.startPreview(pauseAfterFirstFrame);
        }
    }

    public void startExport(LSOExportType type) {
        if (render != null) {
            render.startExport(type);
        }
    }

    public void startExport() {
        if (render != null) {
            render.startExport(LSOExportType.TYPE_720P);
        }
    }

    /**
     * 定位到某个位置.
     *
     * @param timeUs time时间
     */
    public void seekToTimeUs(long timeUs) {
        createRender();
        if (render != null) {
            render.seekToTimeUs(timeUs);
        }
    }

    /**
     * 播放预览暂停;
     * play preview pause.
     */
    public void pause() {
        if (render != null) {
            LSOLog.d("EditPlayer pause...");
            render.pause();
        }
    }

    /**
     * 获取当前总的合成时间
     * 也是所有拼接图层的时间;
     *
     * @return
     */
    public long getDurationUs() {
        if (render != null) {
            return render.getDurationUs();
        } else {
            return 1000;
        }
    }


    /**
     * 设置容器循环播放;
     *
     * @param is 是否循环;
     */
    public void setLooping(boolean is) {
        if (render != null) {
            render.setLooping(is);
        }
    }

    /**
     * cancel  export  when exporting.
     */
    public void cancelExport() {
        if (render != null) {
            render.cancelExport();
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

    /**
     * 设置帧率
     *
     * @param frameRate
     */
    public void setFrameRate(int frameRate) {
        if (render != null && !isExporting()) {
            render.setFrameRate(frameRate);
        }
    }

    /**
     * 设置编码时的码率;
     *
     * @param bitRate
     */
    public void setExportBitRate(int bitRate) {
        if (render != null && !isExporting()) {
            render.setExportBitRate(bitRate);
        }
    }
    private float padBGRed = 1.0f;
    private float padBGGreen = 0.0f;
    private float padBGBlue = 0.0f;
    private float padBGAlpha = 1.0f;

    //内部使用;
    private void createRender() {
        if (render == null) {
            setUpSuccess = false;
            render = new LSOBookPagePlayerRender(getContext());
            render.setBackGroundColor(padBGRed, padBGGreen, padBGBlue, padBGAlpha);
            render.setOnErrorListener(new OnLanSongSDKErrorListener() {
                @Override
                public void onLanSongSDKError(int errorCode) {
                    setUpSuccess = false;
                    if (userErrorListener != null) {
                        userErrorListener.onLanSongSDKError(errorCode);
                    }
                }
            });
        }
    }


    private boolean setUpSuccess = false;

    //内部使用;
    private boolean setup() {
        if (render != null && !setUpSuccess) {
            if (!render.isRunning() && getSurfaceTexture() != null) {
                render.updateCompositionSize(getCompWidth(), getCompHeight());
                render.setInputSize(getInputCompWidth(), getInputCompHeight());
                render.setSurface(getSurfaceTexture(), getViewWidth(), getViewHeight());
                setUpSuccess = render.setup();
                LSOLog.d(this.getClass().getName()+" setup.ret:" + setUpSuccess +
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
     * 释放
     * 如果已经收到完成监听, 则不需要调用;
     */
    private void release() {

    }
}