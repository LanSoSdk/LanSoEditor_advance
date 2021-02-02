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
import com.lansosdk.box.OnLanSongSDKTimeChangedListener;
import com.lansosdk.box.OnLanSongSDKUserSelectedLayerListener;
import com.lansosdk.box.OnResumeListener;

import java.util.List;


public class LSOEditPlayer extends LSOFrameLayout implements ILSOTouchInterface{
    private LSOEditPlayerRender render;

    public LSOEditPlayer(Context context) {
        super(context);
    }

    public LSOEditPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LSOEditPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LSOEditPlayer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
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


    private int firstCompWidth = 0;
    private int firstCompHeight = 0;

    public void onCreateAsync(LSORatioType ratio, OnCreateListener listener) {
        if (firstCompWidth == 0 || firstCompHeight == 0) {
            LSOLog.e("onCreateAsync must first call List<LSOAsset> method.");
            return;
        }

        LSOSize size = getPreviewSizeByRatio(ratio,firstCompWidth,firstCompHeight);
        if (size.width != getCompWidth() || size.height != getCompHeight()) {
            render.setAdjustSurface(true);
            setPlayerSizeAsync((int) size.width, (int) size.height, listener);
        }
    }

    private List<LSOAsset> createAssetArray;


    public void onCreateAsync(List<LSOAsset> arrays, OnCreateListener listener) {
        if (arrays != null && arrays.size() > 0) {
            int width = arrays.get(0).getWidth();
            int height = arrays.get(0).getHeight();

            createAssetArray = arrays;

            if (firstCompWidth == 0 || firstCompHeight == 0) {
                firstCompWidth = width;
                firstCompHeight = height;
            }

            setPlayerSizeAsync(width, height, listener);
        } else {
            listener.onCreate();
        }
    }



    public void onCreateAsync(List<LSOAsset> arrays,int width,int height, OnCreateListener listener) {
        if (arrays != null && arrays.size() > 0) {
            createAssetArray = arrays;
            if (firstCompWidth == 0 || firstCompHeight == 0) {
                firstCompWidth = width;
                firstCompHeight = height;
            }
            setPlayerSizeAsync(width, height, listener);
        } else {
            listener.onCreate();
        }
    }


    /**
     * 调整播放器的预览画布的大小；
     * @param width
     * @param height
     * @param listener
     */
    public void onCreateAsync(int width,int height, OnCreateListener listener) {
        if (firstCompWidth == 0 || firstCompHeight == 0 || width<192 ||height<192) {
            LSOLog.e("onCreateAsync must first call List<LSOAsset> method.");
            return;
        }
        setPlayerSizeAsync(width, height, listener);
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

    /**
     * 在当前时间点插入图片或视频
     *
     * @param assetArray
     * @param listener1
     */
    public void insertConcatAssetAtCurrentTime(List<LSOAsset> assetArray, OnAddAssetProgressListener listener1) {
        insertConcatAssetWithTime(assetArray, getCurrentPositionUs(), listener1);
    }

    /**
     * 在指定的时间点插入图片或视频.
     * 内部流程是:
     * 1. 先找指定时间点 在某个图层时间段内.
     * 2.找到图层后, 如果时间点在图层前半部分,则向前插入; 如后半部分,则向后插入;
     *
     * @param assetArray
     * @param atCompUs
     * @param listener1
     */
    public void insertConcatAssetWithTime(List<LSOAsset> assetArray, long atCompUs, OnAddAssetProgressListener listener1) {
        if (render != null) {
            render.insertConcatLayerListAsync(assetArray, atCompUs, listener1);
        }
    }

    /**
     * 替换拼接图层;
     * @param asset        资源
     * @param replaceLayer 被替换的图层
     * @param listener     异步替换;
     * @return
     */
    public void replaceConcatLayerAsync(LSOAsset asset, LSOLayer replaceLayer, OnAddAssetProgressListener listener) {
        if (render != null) {
            render.replaceConcatLayerListAsync(asset, replaceLayer, listener);
        }
    }

    /**
     * 根据合成的时间, 得到对应的图层;
     *
     * @param compUs
     * @return
     */
    public LSOLayer getCurrentConcatLayerByTime(long compUs) {
        if (render != null) {
            return render.getCurrentConcatLayerByTime(compUs);
        } else {
            return null;
        }
    }

//-------------------------------------overlay some asset------------------------------------

    /**
     * 异步增加资源;
     * 增加完成后, 会通过 OnAddAssetProgressListener异步返回结果.
     * 增加时, 会暂停当前合成的执行;
     *
     * @param asset 资源
     */
    public void addAssetAsync(LSOAsset asset, long atCompUs,
                              OnAddAssetProgressListener listener1) {
        createRender();
        LSOLog.d("EditPlayer addAssetAsync...");
        if (render != null && setup() && asset != null) {
            render.addAssetAsync(asset, atCompUs, listener1);
        }
    }


    /**
     * 增加视频动画特效, 增加后,返回一个特效图层;
     * @param asset 增加资源
     * @param atCompUs 从播放器的什么位置开始增加
     * @param preview 增加的时候,是否要预览一下;
     * @param listener1 增加完成后的回调
     */
    public void addVideoEffectAsync(LSOAsset asset, long atCompUs, boolean preview, OnAddAssetProgressListener listener1) {
        createRender();
        if (render != null && setup() && asset != null && asset.isMV()) {
            render.addVideoEffectAsync(asset, atCompUs,preview, listener1);
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
        LSOLog.d("EditPlayer addBitmapLayer...");
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

        LSOLog.d("EditPlayer addGifLayer...");
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

        LSOLog.d("EditPlayer addGifLayer...");
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
        LSOLog.d("EditPlayer addAudioLayer...");
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
     * 删除所有的叠加图层.
     * 叠加图层有:图片图层, gif图层, 图片序列图层等;
     */
    public void removeAllOverlayLayersAsync() {
        if (render != null) {
            LSOLog.d("EditPlayer removeAllOverlayLayersAsync...");
            render.removeAllOverlayLayersAsync();
        }
    }

    /**
     * 删除声音图层
     *
     * @param layer
     */
    public void removeAudioLayerAsync(LSOAudioLayer layer) {
        if (render != null) {
            LSOLog.d("EditPlayer removeAudioLayerAsync...");

            render.removeAudioLayerAsync(layer);
        }
    }

    /**
     * 删除所有的增加的声音图层;
     */
    public void removeALLAudioLayer() {
        if (render != null) {
            LSOLog.d("EditPlayer removeALLAudioLayer...");
            render.removeALLAudioLayer();
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
     * 打印当前拼接的图层时间信息;
     */
    public void printAllConcatLayerTime() {
        if (render != null) {
            render.printAllConcatLayerTime();
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
        padBGBlur = (float) blue / 255f;
        if (render != null) {
            render.setBackGroundColor(padBGRed, padBGGreen, padBGBlur, 1.0f);
        }
    }
    /**
     * 增加背景图片资源;
     *
     * @return
     */
    public LSOLayer setBackGroundBitmapPath(String bmpPath) {
        createRender();
        if (render != null && setup() && bmpPath != null) {
            try {
                LSOLog.d("EditPlayer setBackGroundBitmapPath...");
                return render.setBackGroundBitmapAsset(new LSOAsset(bmpPath));
            } catch (Exception e) {
                LSOLog.e("setBackGroundBitmapPath error", e);
            }
        }
        return null;
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
     * 获取当前合成容器中的所有拼接图层;
     *
     * @return 图层list 拼接图层;
     */
    public List<LSOLayer> getAllConcatLayers() {
        if (render != null) {
            return render.getAllConcatLayers();
        } else {
            LSOLog.e("getConcatLayers  error.  render is null");
            return null;
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
     * 容器的总时长改变监听;
     * 返回的是当前总时长;
     *
     * @param listener 时长改变监听;
     */
    public void setOnDurationChangedListener(OnLanSongSDKDurationChangedListener listener) {
        createRender();
        if (render != null) {
            render.setOnCompDurationChangedListener(listener);
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
     * 容器的当前播放时间改变回调;
     * 只要是改变了, 不管是seek的改变,还是自动播放的改变,都执行这里;
     *
     * @param listener
     */
    public void setOnTimeChangedListener(OnLanSongSDKTimeChangedListener listener) {
        createRender();
        if (render != null) {
            render.setOnTimeChangedListener(listener);
        }
    }

    /**
     * 视频播放完成进度;
     * 如果你设置了循环播放,则这里不会回调;
     * 一般视频编辑只播放一次,然后seek到指定位置播放;
     * <p>
     * LSLISONG :为什么没有完成回调;
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
     * @param pauseAfterFirstFrame
     */
    public void startPreview(boolean pauseAfterFirstFrame) {
        if (render != null) {
            LSOLog.d("EditPlayer startPreview...");
            render.startPreview(pauseAfterFirstFrame);
        }
    }

    public void startExport(LSOExportType type) {
        if (render != null) {
            LSOLog.d("EditPlayer startExport...");
            render.startExport(type);
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
    private float padBGBlur = 0.0f;
    private float padBGAlpha = 1.0f;

    //内部使用;
    private void createRender() {
        if (render == null) {
            setUpSuccess = false;
            render = new LSOEditPlayerRender(getContext());
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
                LSOLog.d("LSOEditPlayer setup.ret:" + setUpSuccess +
                        " comp size:" + getCompWidth() + " x " + getCompHeight() + " view size :" + getViewWidth() + " x " + getViewHeight());
                return setUpSuccess;
            } else {
                return render.isRunning();
            }
        } else {
            return setUpSuccess;
        }
    }


    public static LSOSize getPreviewSizeByRatio(LSORatioType ratio, int firstCompWidth,int firstCompHeight) {


        if (ratio == LSORatioType.RATIO_NONE) {
            return new LSOSize(firstCompWidth, firstCompHeight);

        } else if (ratio == LSORatioType.RATIO_9_16) {

            return new LSOSize(720, 1280);

        } else if (ratio == LSORatioType.RATIO_16_9) {

            return new LSOSize(1280, 720);

        } else if (ratio == LSORatioType.RATIO_1_1) {

            return new LSOSize(1088, 1088);

        } else if (ratio == LSORatioType.RATIO_4_3) {

            return new LSOSize(1280, 960);

        } else if (ratio == LSORatioType.RATIO_3_4) {

            return new LSOSize(960, 1280);

        } else if (ratio == LSORatioType.RATIO_2_1) {

            return new LSOSize(1280, 640);

        } else if (ratio == LSORatioType.RATIO_1_2) {

            return new LSOSize(640, 1280);

        } else if (ratio == LSORatioType.RATIO_235_1) {
            //2.35:1
            return new LSOSize(1280, 544);

        } else if (ratio == LSORatioType.RATIO_4_5) {
            //4:5
            return new LSOSize(720, 896);

        } else if (ratio == LSORatioType.RATIO_6_7) {
            //6:7
            return new LSOSize(720, 848);
        } else if (ratio == LSORatioType.RATIO_185_1) {
            //1.85:1
            return new LSOSize(1280, 688);
        } else {
            return new LSOSize(firstCompWidth, firstCompHeight);
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
        hasSetConcatAsset = false;
    }
}