package com.lansosdk.segment;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.lansosdk.LanSongFilter.LanSongFilter;
import com.lansosdk.box.LSOAsset;
import com.lansosdk.box.LSOExportType;
import com.lansosdk.box.LSOFrameLayout;
import com.lansosdk.box.LSOLayer;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LSOSegmentMode;
import com.lansosdk.box.LSOSegmentPlayerRunnable;
import com.lansosdk.box.OnAddAssetProgressListener;
import com.lansosdk.box.OnCreateListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKExportCompletedListener;
import com.lansosdk.box.OnLanSongSDKExportProgressListener;
import com.lansosdk.box.OnLanSongSDKPlayCompletedListener;
import com.lansosdk.box.OnLanSongSDKPlayProgressListener;
import com.lansosdk.box.OnLanSongSDKThumbnailBitmapListener;
import com.lansosdk.box.OnResumeListener;
import com.lansosdk.box.OnTextureAvailableListener;
import com.lansosdk.videoeditor.ILSOTouchInterface;


public class LSOSegmentPlayer extends LSOFrameLayout implements ILSOTouchInterface {

    public static void initSDKFromAsset(Context context, String key, String modelName) {

        LSOSegmentPlayerRunnable.initSDKFromAsset(context,key,modelName);
    }

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


    public void onCreateAsync(String path, OnCreateListener listener) {
        if (path != null) {
            render = new LSOSegmentPlayerRunnable(getContext(), path);
            if(render.prepare()){
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
                setPlayerSizeAsync(render.getWidth(), render.getHeight(), listener);
            }else{
                listener.onCreate();
            }
        } else {
            listener.onCreate();
        }
    }

    /**
     * 裁剪的宽度和高度 一定在onCreateAsync之后调用;
     * @param startTimeUs 开始时间
     * @param endTimeUs 结束时间; 单位us;
     */
    public void setCutDuration(long startTimeUs, long endTimeUs){
        if(render!=null && !isRunning()){
            render.setCutDuration(startTimeUs,endTimeUs);
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
        setOnTextureAvailableListener(new OnTextureAvailableListener() {
            @Override
            public void onTextureUpdate(int width, int height) {
                if (render != null) {
                    render.switchCompSurface(getCompWidth(), getCompHeight(), getSurfaceTexture(), getViewWidth(), getViewHeight());
                }
            }
        });

        if (render != null) {
            render.onActivityPaused(true);
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
     *  准备以下
     */
    public boolean prepare() {
        return render != null && setup();
    }

    /**
     */
    public void setBackGroundPath(String path,OnAddAssetProgressListener listener1) {

        if (path != null && render != null) {
            render.setBackGroundPath(path,listener1);
        }
    }


    /**
     * 清空背景;
     */
    public void clearBackGround() {
        if (render != null) {
            render.clearBackGround();
        }
    }

    /**
     * 给抠图背景设置模糊,
     * @param percent 模糊百分比, 0是不模糊. 100是深度模糊; 建议设置10;
     */
    public void setBackGroundBlurPercent(int percent) {
        if (render != null && render.isRunning()) {
            render.setBackGroundBlurPercent(percent);
        }
    }

    /**
     * 获取背景模糊百分比;
     *
     * @return
     */
    public int getBackGroundBlurPercent() {
        if (render != null) {
            return render.getBackGroundBlurPercent();
        } else {
            return 0;
        }
    }

    /**
     * 背景设置滤镜
     * @param filter 滤镜对象
     */
    public void setBackGroundFilter(LanSongFilter filter) {
        if (render != null) {
            render.setBackGroundFilter(filter);
        }
    }

    /**
     * 获取背景滤镜对象;
     * @return
     */
    public LanSongFilter getBackGroundFilter() {
        if (render != null) {
            return render.getBackGroundFilter();
        } else {
            return null;
        }
    }

    //-----------------------------------------


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
     * 设置外部声音;
     * @param path
     * @return
     */
    public boolean setAudioPath(String path) {
        createRender();
        LSOLog.d("SegmentLayer setAudioPath...");
        if (render != null && setup()) {
            return render.setAudioPath(path,1.0F,0,Long.MAX_VALUE);
        } else {
            return false;
        }
    }


    /**
     * 设置外部声音
     * @param path 声音路径
     * @param volume    声音的音量
     * @param cutStartUs 开始裁剪位置, 最低是0
     * @param cutEndUs   结束裁剪, 如到文件尾:则是Long.MAX_VALUE
     * @return
     */
    public boolean setAudioPath(String path, float volume, long cutStartUs, long cutEndUs) {
        createRender();
        LSOLog.d("SegmentLayer setAudioPath...");
        if (render != null && setup()) {
            return render.setAudioPath(path,volume,cutStartUs,cutEndUs);
        } else {
            return false;
        }
    }

    /**
     * 获取背景视频或图片的图层对象
     * @return
     */
    public LSOLayer getBackGroundLayer(){
        if (render != null && setup()) {
            return render.getBackGroundLayer();
        }else{
            return null;
        }
    }

    /**
     * 设置分割视频的声音大小;
     * @param volume
     */
    public void setSegmentVolume(float volume){
        if (render != null && setup()) {
            render.setSegmentVolume(volume);
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
     * 播放进度回调
     */
    public void setOnLanSongSDKPlayProgressListener(OnLanSongSDKPlayProgressListener listener) {
        createRender();
        if (render != null) {
            render.setOnPlayProgressListener(listener);
        }
    }

    /**
     * 视频播放完成进度;
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
            render.startPreview();
        }
        return true;
    }


    /**
     * 设置分割模式;
     */
    public void setSegmentMode(LSOSegmentMode mode){
        if(render!=null){
            render.setSegmentModeWhenExport(mode);
        }
    }


    /**
     * 获取分割模式
     * @return
     */
    public LSOSegmentMode getSegmentMode(){
        if(render!=null){
            return render.getSegmentModeWhenExport();
        }else{
            return LSOSegmentMode.GOOD;
        }
    }


    /**
     * 导出
     */
    public void startExport() {
        if (render != null) {
            LSOLog.d("SegmentLayer startExport...");
            render.startExport(LSOExportType.TYPE_ORIGINAL);
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
        }
        setUpSuccess = false;
    }
}