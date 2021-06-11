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
import com.lansosdk.box.LSOEditPlayerRender;
import com.lansosdk.box.LSOExportType;
import com.lansosdk.box.LSOFrameLayout;
import com.lansosdk.box.LSOLayer;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LSORatioType;
import com.lansosdk.box.LSOSize;
import com.lansosdk.box.LSOStickerPlayerRender;
import com.lansosdk.box.OnAddAssetProgressListener;
import com.lansosdk.box.OnCreateListener;
import com.lansosdk.box.OnLanSongSDKBeforeRenderFrameListener;
import com.lansosdk.box.OnLanSongSDKDurationChangedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKExportCompletedListener;
import com.lansosdk.box.OnLanSongSDKExportProgressListener;
import com.lansosdk.box.OnLanSongSDKLayerTouchEventListener;
import com.lansosdk.box.OnLanSongSDKPlayCompletedListener;
import com.lansosdk.box.OnLanSongSDKPlayProgressListener;
import com.lansosdk.box.OnLanSongSDKStateChangedListener;
import com.lansosdk.box.OnLanSongSDKThumbnailBitmapListener;
import com.lansosdk.box.OnLanSongSDKTimeChangedListener;
import com.lansosdk.box.OnLanSongSDKUserSelectedLayerListener;
import com.lansosdk.box.OnResumeListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class LSOStickerPlayer extends LSOFrameLayout implements ILSOTouchInterface{
    private LSOStickerPlayerRender render;

    public LSOStickerPlayer(Context context) {
        super(context);
    }

    public LSOStickerPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LSOStickerPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LSOStickerPlayer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
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


    private List<LSOAsset> createAssetArray=new ArrayList<>();


    /**
     * 如果是图片, 则默认是15秒;
     * @param stickerPath
     * @param listener
     */
    public void onCreateAsync(String stickerPath,OnCreateListener listener) {
        if (fileExist(stickerPath)) {

            try {
                LSOAsset asset = new LSOAsset(stickerPath);
                createAssetArray.add(asset);
                setPlayerSizeAsync(asset.getWidth(), asset.getHeight(), listener);
            } catch (Exception e) {
                e.printStackTrace();
                listener.onCreate();
            }

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
        hasSetConcatAsset = false;
    }

    //-----------------------------VIEW ADJUST CODE END----------------------------
    //------------------------------------------------player code-----------------------------------------------

    private boolean hasSetConcatAsset = false;

    /**
     * 设置需要拼接的资源, 只能设置一次.
     *
     * @param listener
     */
    public void prepareConcatAssets(OnAddAssetProgressListener listener) {
        if (!hasSetConcatAsset && createAssetArray != null && createAssetArray.size() > 0) {
            createRender();
            hasSetConcatAsset = true;
            if (render != null && setup()) {
                render.setConcatLayerListAsync(createAssetArray, listener);
            }
        } else {
            LSOLog.e("setConcatAssetList error.  hasSetConcatAsset==true");
        }
    }


    public void setDurationUs(long durationUs){
        if(render!=null){
            render.setDurationUs(durationUs);
        }
    }


    /**
     * 增加绿幕视频
     * @param videoPath 绿色背景视频的路径
     * @param listener1 增加后返回的监听
     */
    public void addGreenBackFileAsync(String videoPath,
                              OnAddAssetProgressListener listener1) {
        createRender();
        if (render != null && setup()  && fileExist(videoPath)) {
            render.addAssetAsync(videoPath, listener1);
        }
    }


    public LSOLayer addBitmapLayer(String path, long atCompUs) {
        createRender();
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
     * 增加gif图层;
     *
     * @return 返回gif图层对象;
     */
    public LSOLayer addGifLayer(String gifPath, long atCompUs) {

        if (gifPath != null) {
            int index = gifPath.lastIndexOf('.');
            if (index > -1) {
                String suffix = gifPath.substring(index + 1);
                if ("gif".equalsIgnoreCase(suffix)) {
                    try {
                        LSOAsset asset=new LSOAsset(gifPath);
                        if (render != null && setup() && asset.isGif()) {
                            return render.addGifLayer(asset, atCompUs);
                        }
                    } catch (Exception e) {
                        LSOLog.e("addGifLayer error. " ,e);
                    }
                }
            }
        }
        return null;
    }

    public LSOLayer addGifLayer(LSOAsset asset, long atCompUs) {

        try {
            if (render != null && setup() && asset.isGif()) {
                return render.addGifLayer(asset, atCompUs);
            }
        } catch (Exception e) {
            LSOLog.e("addGifLayer error. ", e);
        }
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
     * 异步删除图层;
     * 删除成功后, 会触发容器时长回调, 在回调中你可以重新布局
     *
     * @param layer
     */
    public void removeLayerAsync(LSOLayer layer) {
        if (render != null) {
            LSOLog.d("EditPlayer removeLayerAsync...");
            render.removeLayerAsync(layer);
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
     * @return
     */
    public boolean isExporting() {
        return render != null && render.isExporting();
    }


    /**
     * 获取当前时间.
     * @return
     */
    public long getCurrentPositionUs() {
        if (render != null) {
            return render.getCurrentTimeUs();
        } else {
            return 0;
        }
    }

    /**
     * 设置背景图片; 切换背景图片;
     * @param path  图片的路径,
     * @param listener1 替换后的完成监听;
     */
    public void setBackGroundPathAsync(String path, OnAddAssetProgressListener listener1) {
        if(render!=null){
            render.setBackGroundPathAsync(path,listener1);
        }
    }


    /**
     * 获取背景图片的缩略图;
     * @param listener
     */
    public void getBackGroundThumbnailAsync(OnLanSongSDKThumbnailBitmapListener listener) {
        if(render!=null){
            render.getBackGroundThumbnailAsync(listener);
        }
    }


    /**
     * 在每一帧绘制前的回调, 里面没有经过handler, 你增加的代码, 在我们render线程中执行,
     *
     * @param listener 返回的是时间戳, 单位us;
     */
    public void setOnBeforeRenderFrameListener(OnLanSongSDKBeforeRenderFrameListener listener) {
        createRender();
        if (render != null) {
            render.setOnLanSongBeforeRenderFrameListener(listener);
        }
    }

    /**
     * 当一个图层按下, 抬起, 移动, 旋转,缩放的时候, 会调用这里;
     * 移动,缩放, 返回的值, 是相对于容器本身宽高而言,是当前时间点的绝对值;
     *
     * @param listener
     */
    public void setOnLayerTouchEventListener(OnLanSongSDKLayerTouchEventListener listener) {
        createRender();
        if (render != null) {
            render.setOnLayerTouchEventListener(listener);
        }
    }

    /**
     * 播放进度回调
     * 监听中的两个参数是: onLanSongSDKExportProgress(long ptsUs, int percent);
     * 分别对应 当前处理的时间戳 和百分比;
     * 在seek或pause的时候,此监听不调用;
     * @param listener
     */
    public void setOnLanSongSDKPlayProgressListener(OnLanSongSDKPlayProgressListener listener) {
        createRender();
        if (render != null) {
            render.setOnPlayProgressListener(listener);
        }
    }


    /**
     * 播放有暂停和播放两种状态;
     * @param listener
     */
    public void setOnLanSongSDKStateChangedListener(OnLanSongSDKStateChangedListener listener){
        createRender();
        if(render!=null){
            render.setOnLanSongSDKStateChangedListener(listener);
        }
    }


    /**
     * 视频播放完成进度;
     * 如果你设置了循环播放,则这里不会回调;
     * 一般视频编辑只播放一次,然后seek到指定位置播放;
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
     * <p>
     * 监听中的两个参数是: onLanSongSDKExportProgress(long ptsUs, int percent);
     * 分别对应 当前处理的时间戳 和百分比;
     *
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
     * 完成后, 有 void onLanSongSDKExportCompleted(String dstVideo);
     * 对应的是:返回完成后的目标视频路径;
     *
     * @param listener
     */
    public void setOnExportCompletedListener(OnLanSongSDKExportCompletedListener listener) {
        createRender();
        if (render != null) {
            render.setOnExportCompletedListener(listener);
        }
    }

    /**
     * 容器中图层有点击事件, 当选中后, 可以移动旋转缩放;
     * 这里监听是哪个图层被点击了, 或全部取消;
     *
     * @param listener
     */
    public void setOnUserSelectedLayerListener(OnLanSongSDKUserSelectedLayerListener listener) {
        createRender();
        if (render != null) {
            render.setOnUserSelectedLayerListener(listener);
        }
    }

    /**
     * 当用户通过时间轴选中一个图层后, 可通过这里设置, 让内部有选中状态;
     *
     * @param layer 图层选中;
     */
    public void setSelectLayer(LSOLayer layer) {
        if (render != null) {
            render.setSelectLayer(layer);
        }
    }

    /**
     * 禁止图层touch
     */
    public void setDisableTouchEvent(boolean is) {
        createRender();
        if (render != null) {
            render.setDisableTouchEvent(is);
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
        if (render != null && hasSetConcatAsset) {
            render.startPreview(false);
        }
        return true;
    }

    /**
     */
    public void startPreview(boolean pauseAfterFirstFrame) {
        if (render != null) {
            LSOLog.d("EditPlayer startPreview...");
            render.startPreview(pauseAfterFirstFrame);
        }
    }

    public void startExport() {
        if (render != null) {
            LSOLog.d("EditPlayer startExport...");
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

    private float padBGRed = 0.0f;
    private float padBGGreen = 0.0f;
    private float padBGBlue = 0.0f;
    private float padBGAlpha = 1.0f;

    //内部使用;
    private void createRender() {
        if (render == null) {
            setUpSuccess = false;
            render = new LSOStickerPlayerRender(getContext());
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

    public static boolean fileExist(String absolutePath) {
        if (absolutePath == null)
            return false;
        else {
            File file = new File(absolutePath);
            if (file.exists()){
                return true;
            }
        }
        return false;
    }
}