package com.lansosdk.videoeditor;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.AttributeSet;

import com.lansosdk.LanSongFilter.LanSongFilter;
import com.lansosdk.VLog.LSOVLogAsset;
import com.lansosdk.box.LSOCamAudioLayer;
import com.lansosdk.box.LSOFrameLayout;
import com.lansosdk.box.LSOLayerPosition;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LSOVLogLayer;
import com.lansosdk.box.LSOVLogModule;
import com.lansosdk.box.LSOVLogPlayerRender;
import com.lansosdk.box.OnCompressListener;
import com.lansosdk.box.OnCreateListener;
import com.lansosdk.box.OnPrepareListener;
import com.lansosdk.box.OnLSOVLogAssetChangedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKExportCompletedListener;
import com.lansosdk.box.OnLanSongSDKExportProgressListener;
import com.lansosdk.box.OnLanSongSDKPlayCompletedListener;
import com.lansosdk.box.OnLanSongSDKPlayProgressListener;
import com.lansosdk.box.OnLanSongSDKStateChangedListener;
import com.lansosdk.box.OnResumeListener;
import com.lansosdk.box.OnSetCompletedListener;
import com.lansosdk.box.OnTextureAvailableListener;


public class LSOVLogPlayer extends LSOFrameLayout {
    /**
     * 渲染类;
     */
    private LSOVLogPlayerRender renderer;


    public LSOVLogPlayer(Context context) {
        super(context);
    }

    public LSOVLogPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LSOVLogPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LSOVLogPlayer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    //-----------copy code
    protected void sendOnCreateListener() {
        super.sendOnCreateListener();
        if (renderer != null) {
            renderer.switchCompSurface(getCompWidth(), getCompHeight(), getSurfaceTexture(), getViewWidth(), getViewHeight());
        }
    }

    public void sendOnResumeListener() {
        super.sendOnResumeListener();
        if (renderer != null) {
            renderer.switchCompSurface(getCompWidth(), getCompHeight(), getSurfaceTexture(), getViewWidth(), getViewHeight());
        }
    }


    public void onCreateAsync(LSOVLogModule module, OnCreateListener listener) {

        if (module!=null && module.getWidth()>0 && module.getHeight()>0) {
            setPlayerSizeAsync(module.getWidth(),module.getHeight(), listener);
        }else{
            listener.onCreate();
        }
    }


    public void onResumeAsync(OnResumeListener listener) {
        super.onResumeAsync(listener);
        if (renderer != null) {
            renderer.onActivityPaused(false);
        }
    }

    public void onPause() {
        super.onPause();
        setOnTextureAvailableListener(new OnTextureAvailableListener() {
            @Override
            public void onTextureUpdate(int width, int height) {
                if (renderer != null) {
                    renderer.switchCompSurface(getCompWidth(), getCompHeight(), getSurfaceTexture(), getViewWidth(), getViewHeight());
                }
            }
        });

        if (renderer != null) {
            renderer.onActivityPaused(true);
        }
        pause();
    }

    public void onDestroy() {
        super.onDestroy();
        release();
    }
    //-----------------------------VIEW ADJUST CODE END----------------------------
    //---------------------------------------------容器代码--------------------------------------------------------

    private void createRender(){
        if(renderer==null){
            renderer =new LSOVLogPlayerRender(getContext());
            setupSuccess=false;
        }
    }

    private boolean setupSuccess;

    /**
     * 增加一个AE模板
     * @param module 模板
     * @throws Exception
     */
    public void addVLogModule(LSOVLogModule module) throws Exception{
        if(module!=null){
            createRender();
            if(renderer!=null && setup()){
                renderer.addVLogModule(module);
            }
        }
    }

    /**
     * 根据一个视频资源, 获取对应的视频图层对象,
     * 获取后, 可利用此对象设置滤镜等信息;
     * @param asset
     * @return
     */
    public LSOVLogLayer getAssetLayerWithAsset(LSOVLogAsset asset){
        if(renderer!=null && renderer.isRunning()){
            return renderer.getLayerWithAsset(asset);
        }else {
            LSOLog.e("getAssetLayerWithAsset error.  render is not running.");
            return null;
        }
    }

    /**
     * 给所有的视频设置滤镜;
     * @param filter
     */
    public void setFilterToAllVideo(LanSongFilter filter){
        if(renderer!=null){
            renderer.setFilterForAllVideo(filter);
        }
    }


    /**
     * 异步增加片尾
     * @param jsonPath 片尾的json路径, 可以为null
     * @param videoPath 片尾的背景视频路径, 可以是null
     * @param listener 异步增加成功后的回调;
     */
    public void setEndJsonVideoAsync(String jsonPath, String videoPath, OnSetCompletedListener listener){
        if(renderer!=null && renderer.isRunning()){
            renderer.setEndTextVideo(jsonPath,videoPath,listener);
        }
    }

    /**
     * 定位到片尾, 定位后, 默认是播放的;
     */
    public void seekToEndJsonVideo(){
        if(renderer!=null && renderer.isRunning()){
            renderer.seekToEndJsonVideo();
        }
    }


    /**
     * 取消片尾
     */
    public void cancelEndJsonVideo(){
        setEndJsonVideoAsync(null,null,null);
    }

    /**
     * 更新片尾的文字
     * @param text 要更新的文字
     */
    public void updateEndJsonText(String text){
        if(renderer!=null && renderer.isRunning()){
            renderer.updateEndText(text);
        }
    }


    /**
     * 为替换的视频,设置所有的声音;
     * @param volume
     */
    public void setVolumeForAllVideo(float volume){
        if(renderer!=null && renderer.isRunning()){
            renderer.setVolumeForAllVideo(volume);
        }
    }

    /**
     * 增加logo的图片;
     * @param bmp 图片
     * @param position 枚举位置类型
     */
    public void addLogoBitmap(Bitmap bmp, LSOLayerPosition position){
        createRender();
        if(renderer!=null && setup()){
            if(isPlaying()){
                LSOLog.w("addLogoBitmap error 已经开始预览了, 则只能在导出时有效");
            }
            renderer.addLogoBitmap(bmp,position);
        }
    }

    /**
     * 增加logo图片
     * @param bmp 图片
     * @param x 图片中心点的X位置
     * @param y 图片中心点的Y位置;
     */
    public void addLogoBitmap(Bitmap bmp, int x, int y){
        createRender();
        if(renderer!=null && setup()){
            if(isPlaying()){
                LSOLog.w("addLogoBitmap error 已经开始预览了, 则只能在导出时有效");
            }
            renderer.addLogoBitmap(bmp,x,y);
        }
    }

    /**
     * 增加一个声音图层;
     * @param path 声音路径;
     * @param startTimeOfComp 从容器的什么位置增加;
     * @return
     */
    public LSOCamAudioLayer addAudioLayer(String path, long startTimeOfComp) {
        createRender();
        if(renderer!=null){
            return renderer.addAudioLayer(path,startTimeOfComp);
        }else{
            return null;
        }
    }


    /**
     * 删除指定的声音图层;
     * @param layer
     */
    public void removeAudioLayerAsync(LSOCamAudioLayer layer) {
        if(renderer!=null){
            renderer.removeAudioLayerAsync(layer);
        }
    }

    /**
     * 删除所有声音图层;
     */
    public void removeALLAudioLayer() {
        if(renderer!=null){
            renderer.removeALLAudioLayer();
        }
    }

    /**
     * 获取当前时间点的图片信息;
     * @return
     */
    public LSOVLogAsset getCurrentAsset(){
        if(renderer!=null){
            return renderer.getCurrentAssetInfo();
        }else{
            return null;
        }
    }

    /**
     * 内部创建的线程是否在运行;
     * @return
     */
    public boolean isRunning(){
        return renderer!=null && renderer.isRunning();
    }


    /**
     * 设置压缩进度, 在视频裁剪或替换时被调用;
     * @param listener
     */
    public void setOnCompressListener(OnCompressListener listener){
        createRender();
        if(renderer!=null){
            renderer.setOnCompressListener(listener);
        }
    }

    /**
     * 播放状态改变回调;
     * @param listener
     */
    public void setOnLanSongSDKStateChangedListener(OnLanSongSDKStateChangedListener listener){
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKStateChangedListener(listener);
        }
    }

    /**
     * 当在播放过程中, 一个图片播放完毕后, 切换到下一张图片, 会触发此监听;
     * 当外部seek也会触发此监听;
     * @param listener 监听两个参数, int:index表示当前是总图片中的第几个,从0开始; LSOVLogAsset:表示当前视频;
     */
    public void setOnLSOVLogAssetChangedListener(OnLSOVLogAssetChangedListener listener){
        createRender();
        if(renderer!=null){
            renderer.setOnLSOVLogAssetChangedListener(listener);
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
        if(renderer!=null){
            renderer.setOnLanSongSDKPlayProgressListener(listener);
        }
    }

    /**
     * 视频播放完成进度;
     * 如果你设置了循环播放,则这里不会回调;
     * 一般视频编辑只播放一次,然后seek到指定位置播放;
     * @param listener
     */
    public void setOnLanSongSDKPlayCompletedListener(OnLanSongSDKPlayCompletedListener listener) {
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKPlayCompletedListener(listener);
        }
    }

    /**
     * 导出进度回调;
     *
     * 监听中的两个参数是: onLanSongSDKExportProgress(long ptsUs, int percent);
     * 分别对应 当前处理的时间戳 和百分比;
     * @param listener
     */
    public void setOnLanSongSDKExportProgressListener(OnLanSongSDKExportProgressListener listener) {
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKExportProgressListener(listener);
        }
    }

    /**
     * 导出完成回调;
     * 完成后, 有 void onLanSongSDKExportCompleted(String dstVideo);
     * 对应的是:返回完成后的目标视频路径;
     * @param listener
     */
    public void setOnLanSongSDKExportCompletedListener(OnLanSongSDKExportCompletedListener listener) {
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKExportCompletedListener(listener);
        }
    }
    private OnLanSongSDKErrorListener userErrorListener;

    /**
     * 错误监听
     */
    public void setOnLanSongSDKErrorListener(OnLanSongSDKErrorListener listener){
        createRender();
        userErrorListener=listener;
    }


    public void setLooping(boolean is) {
        if(renderer!=null){
            renderer.setLooping(is);
        }
    }

    public void prepareAsync(OnPrepareListener listener){
        if(renderer!=null){
            renderer.prepareAsync(listener);
        }else if(listener!=null){
            listener.onSuccess(false);
        }
    }


    /**
     * 开始预览, 默认是循环播放;
     */
    public boolean start(){
        super.start();
        if(renderer!=null){
            renderer.setLooping(true);
            renderer.start();
        }
        return true;
    }

    /**
     * 视频导出
     */
    public void startExport() {
        if(renderer!=null){
            renderer.startExport();
        }
    }

    /**
     * 获取模板的有效时长;
     * @return
     */
    public long getDurationUs(){
        if(renderer!=null){
            return  renderer.getDurationUs();
        }else{
            return 1000;
        }
    }

    @Override
    public boolean isLayoutValid(){
        return super.isLayoutValid();
    }


    /**
     * 是否在播放
     * @return
     */
    public boolean isPlaying(){
        return renderer!=null && renderer.isPlaying();
    }

    /**
     * 暂停
     */
    public void pause(){
        if(renderer!=null){
            renderer.pause();
        }
    }

    /**
     * @param seekUs
     */
    public void seekToTimeUs(long seekUs){
        if(renderer!=null){
            renderer.seekToTimeUs(seekUs,true);
        }
    }

    /**
     * @param logAsset logAsset
     */
    public void seekToVLogAsset(LSOVLogAsset logAsset){
        if(renderer!=null && logAsset!=null){
            renderer.seekToVLogAsset(logAsset);
        }
    }

    /**
     * 获取当前时间.
     * @return
     */
    public long getCurrentPositionUs(){
        if(renderer!=null){
            return renderer.getCurrentTimeUs();
        }else{
            return 0;
        }
    }

    /**
     * 获取当前时间, 单位微秒;
     * @return 当前时间
     */
    public long getCurrentTimeUs(){
        if(renderer!=null){
            return renderer.getCurrentTimeUs();
        }else{
            return 0;
        }
    }

    /**
     * 取消预览
     * [阻塞进行,一直等待到整个线程退出才返回.]
     */
    public void cancel(){
        if(renderer!=null){
            renderer.cancel();
            renderer=null;
        }
        setupSuccess=false;
    }
    /**
     * 释放
     * 如果已经收到完成监听, 则不需要调用;
     */
    private void release(){
        if(renderer!=null){
            renderer.release();
            renderer=null;
        }
        setupSuccess=false;
    }


    //------内部使用;
    private boolean setup(){
        if(setupSuccess){
            return true;
        }
        if(renderer!=null && getSurfaceTexture() !=null){
            renderer.updatePlayerSize(getCompWidth(),getCompHeight());
            renderer.setSurface(getSurfaceTexture(),getViewWidth(),getViewHeight());
            renderer.setOnLanSongSDKErrorListener(new OnLanSongSDKErrorListener() {
                @Override
                public void onLanSongSDKError(int errorCode) {
                    if(userErrorListener!=null){
                        userErrorListener.onLanSongSDKError(errorCode);
                    }
                }
            });
            setupSuccess= renderer.setup();
            return setupSuccess;
        }else{
            return  false;
        }
    }
}
