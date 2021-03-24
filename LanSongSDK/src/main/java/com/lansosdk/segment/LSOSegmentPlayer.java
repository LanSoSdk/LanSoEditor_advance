package com.lansosdk.segment;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.lansosdk.box.LSOAsset;
import com.lansosdk.box.LSOAudioLayer;
import com.lansosdk.box.LSOExportType;
import com.lansosdk.box.LSOFrameLayout;
import com.lansosdk.box.LSOLayer;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LSORatioType;
import com.lansosdk.box.LSOSegmentPlayerRunnable;
import com.lansosdk.box.LSOSegmentVideo;
import com.lansosdk.box.LSOSize;
import com.lansosdk.box.OnAddAssetProgressListener;
import com.lansosdk.box.OnCreateListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKExportCompletedListener;
import com.lansosdk.box.OnLanSongSDKExportProgressListener;
import com.lansosdk.box.OnLanSongSDKPlayCompletedListener;
import com.lansosdk.box.OnLanSongSDKPlayProgressListener;
import com.lansosdk.box.OnResumeListener;
import com.lansosdk.videoeditor.ILSOTouchInterface;
import com.lansosdk.videoeditor.LSOEditPlayer;

import java.util.List;


public class LSOSegmentPlayer extends LSOFrameLayout implements ILSOTouchInterface {

    private LSOSegmentPlayerRunnable render;

    public LSOSegmentPlayer(Context context) {
        super(context);
    }

    public LSOSegmentPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LSOSegmentPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LSOSegmentPlayer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
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
        super.onTextureViewTouchEvent(event);
        return false;
    }


    private int firstCompWidth = 0;
    private int firstCompHeight = 0;


    public void onCreateAsync(LSOSegmentVideo video, OnCreateListener listener) {
        if (video != null) {

            render = new LSOSegmentPlayerRunnable(getContext(), video);
            render.setBackGroundColor(padBGRed, padBGGreen, padBGBlur, padBGAlpha);
            render.setOnErrorListener(new OnLanSongSDKErrorListener() {
                @Override
                public void onLanSongSDKError(int errorCode) {
                    setUpSuccess = false;
                    if (userErrorListener != null) {
                        userErrorListener.onLanSongSDKError(errorCode);
                    }
                }
            });
            setPlayerSizeAsync(video.getWidth(), video.getHeight(), listener);
        } else {
            listener.onCreate();
        }
    }


    public void onCreateAsync(LSORatioType ratio, OnCreateListener listener) {
        if (firstCompWidth == 0 || firstCompHeight == 0) {
            LSOLog.e("onCreateAsync must first call List<LSOAsset> method.");
            return;
        }

        LSOSize size = LSOEditPlayer.getPreviewSizeByRatio(ratio, firstCompWidth, firstCompHeight);
        if (size.width != getCompWidth() || size.height != getCompHeight()) {
            render.setAdjustSurface(true);
            setPlayerSizeAsync((int) size.width, (int) size.height, listener);
        }
    }

    /**
     * 复制一个图层
     */
    public LSOLayer copySegmentLayer() {
        if (render != null) {
            return render.copySegmentLayer();
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

    public void onPause() {
        super.onPause();
        if (render != null) {
            render.onActivityPaused(true);
        }
        pause();
    }

    public void onDestroy() {
        super.onDestroy();
        release();
    }

    //-----------------------------VIEW ADJUST CODE END----------------------------


    /**
     * 获取原始的分割图层;
     *
     * @return
     */
    public LSOLayer getSegmentLayer() {

        if (render != null) {
            return render.getSegmentLayer();
        } else {
            return null;
        }
    }

    /**
     * 获取分割图层的声音图层;
     *
     * @return
     */
    public LSOAudioLayer getSegmentAudioLayer() {
        if (render != null) {
            return render.getSegmentAudioLayer();
        } else {
            return null;
        }
    }


    public boolean prepare() {
        return render != null && setup();
    }

    /**
     * 增加视频时, 暂停当前执行;
     *
     * @param listener1
     * @return
     */
    public void setBackGroundPath(String path, OnAddAssetProgressListener listener1) {

        if (path != null && render != null) {
            try {
                LSOAsset asset = new LSOAsset(path);
                render.setBackGroundPath(asset, listener1);
            } catch (Exception e) {
                e.printStackTrace();
                listener1.onAddAssetCompleted(null,false);
            }
        }
    }

    public void clearBackGround() {
        if (render != null) {
            render.clearBackGround();
        }
    }

    /**
     * 增加视频动画特效, 增加后,返回一个特效图层;
     *
     * @param asset     增加资源
     * @param preview   增加的时候,是否要预览一下;
     * @param listener1 增加完成后的回调
     */
    public void addVideoEffectAsync(LSOAsset asset, boolean preview, OnAddAssetProgressListener listener1) {
        createRender();
        if (render != null && setup() && asset != null && asset.isMV()) {
            render.addVideoEffectAsync(asset, 0, preview, listener1);
        }
    }


    public LSOLayer addBitmapLayer(String path, long atCompUs) {
        createRender();
        LSOLog.d("SegmentLayer addBitmapLayer...");
        if (render != null && setup()) {
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
     * 增加gif图层;
     *
     * @return 返回gif图层对象;
     */
    public LSOLayer addGifLayer(String gifPath, long atCompUs) {

        LSOLog.d("SegmentLayer addGifLayer...");
        if (gifPath != null) {
            int index = gifPath.lastIndexOf('.');
            if (index > -1) {
                String suffix = gifPath.substring(index + 1);
                if ("gif".equalsIgnoreCase(suffix)) {
                    try {
                        LSOAsset asset = new LSOAsset(gifPath);
                        if (render != null && setup() && asset.isGif()) {
                            return render.addGifLayer(asset, atCompUs);
                        }
                    } catch (Exception e) {
                        LSOLog.e("addGifLayer error. ", e);
                    }
                }
            }
        }
        return null;
    }
    /**
     * 异步删除图层;
     * 删除成功后, 会触发容器时长回调, 在回调中你可以重新布局
     *
     * @param layer
     */
    public void removeLayerAsync(LSOLayer layer) {
        if (render != null) {
            LSOLog.d("SegmentLayer removeLayerAsync...");
            render.removeLayerAsync(layer);
        }
    }

    /**
     * 增加声音图层;
     *
     * @param path            声音的完整路径;
     * @return 返回声音对象;
     */
    public LSOAudioLayer addAudioLayer(String path) {
        createRender();
        LSOLog.d("SegmentLayer addAudioLayer...");
        if (render != null && setup()) {
            return render.addAudioLayer(path, 0);
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
            LSOLog.d("SegmentLayer removeAudioLayerAsync...");
            render.removeAudioLayerAsync(layer);
        }
    }

    /**
     * 删除所有的增加的声音图层;
     */
    public void removeALLAudioLayer() {
        if (render != null) {
            LSOLog.d("SegmentLayer removeALLAudioLayer...");
            render.removeALLAudioLayer();
        }
    }

    public LSOLayer getTouchPointLayer(float x, float y) {
        if (render != null) {
            return render.getTouchPointLayer(x, y);
        } else {
            return null;
        }
    }

    public void setBackGroundTouchEnable(boolean is) {
        if (render != null) {
            render.setBackGroundTouchEnable(is);
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
     * 合成是一个线程, 此返回的是当前线程是否在运行,
     * <p>
     * 线程运行不一定画面在播放,有可能画面暂停;
     *
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
     * 获取当前时间.
     *
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
     * @return 图层数组, 叠加的图层;
     */
    public List<LSOLayer> getAllOverLayLayers() {
        if (render != null) {
            return render.getAllOverLayLayers();
        } else {
            LSOLog.e("getOverLayLayers  error.  render is null");
            return null;
        }
    }

    /**
     * @return
     */
    public List<LSOAudioLayer> getAllAudioLayers() {
        if (render != null) {
            return render.getAllAudioLayers();
        } else {
            LSOLog.e("getAllAudioLayers  error.  render is null");
            return null;
        }
    }

    /**
     * 播放进度回调
     * 监听中的两个参数是: onLanSongSDKExportProgress(long ptsUs, int percent);
     * 分别对应 当前处理的时间戳 和百分比;
     * 在seek或pause的时候,此监听不调用;
     *
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
     * 如果你设置了循环播放,则这里不会回调;
     * 一般视频编辑只播放一次,然后seek到指定位置播放;
     *
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

    public void start(boolean pauseAfterFirstFrame) {
        if (render != null) {
            LSOLog.d("SegmentLayer startPreview...");
            render.startPreview(pauseAfterFirstFrame);
        }
    }

    public void startExport() {
        if (render != null) {
            LSOLog.d("SegmentLayer startExport...");
            render.startExport(LSOExportType.TYPE_ORIGINAL);
        }
    }

    public void startExport(LSOExportType type) {
        if (render != null) {
            LSOLog.d("SegmentLayer startExport...");
            render.startExport(type);
        }
    }

    /**
     * 定位到某个位置.
     *
     * @param timeUs time时间
     */
    public void seekToTimeUs(long timeUs) {
        createRender();
        if (render != null && !isExporting()) {
            render.seekToTimeUs(timeUs);
        }
    }

    /**
     * 播放预览暂停;
     * play preview pause.
     */
    public void pause() {
        if (render != null) {
            LSOLog.d("SegmentLayer pause...");
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
    private float padBGBlur = 0.0f;
    private float padBGAlpha = 1.0f;

    //内部使用;
    private void createRender() {
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
                LSOLog.d("LSOSegmentPlayer setup.ret:" + setUpSuccess +
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
        if (render != null) {
            render.release();
            render = null;
        }
        setUpSuccess = false;
    }
}