package com.lansosdk.videoeditor;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.lansosdk.aex.LSOAexImage;
import com.lansosdk.aex.LSOAexText;
import com.lansosdk.box.LSOAexModule;
import com.lansosdk.box.LSOAexPlayerRender;
import com.lansosdk.box.LSOAsset;
import com.lansosdk.box.LSOCamAudioLayer;
import com.lansosdk.box.LSOFrameLayout;
import com.lansosdk.box.LSOLayer;
import com.lansosdk.box.LSOLayerPosition;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.OnAexImageSelectedListener;
import com.lansosdk.box.OnAexTextSelectedListener;
import com.lansosdk.box.OnCreateListener;
import com.lansosdk.box.OnLSOAexImageChangedListener;
import com.lansosdk.box.OnLanSongSDKCompressListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKExportCompletedListener;
import com.lansosdk.box.OnLanSongSDKExportProgressListener;
import com.lansosdk.box.OnLanSongSDKPlayCompletedListener;
import com.lansosdk.box.OnLanSongSDKPlayProgressListener;
import com.lansosdk.box.OnLanSongSDKTimeChangedListener;
import com.lansosdk.box.OnResumeListener;


public class LSOAexPlayer extends LSOFrameLayout {
    /**
     * 渲染类;
     */
    private LSOAexPlayerRender renderer;


    public LSOAexPlayer(Context context) {
        super(context);
    }

    public LSOAexPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LSOAexPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LSOAexPlayer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
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

    private boolean isEnableTouch = true;
    public void setTouchEnable(boolean enable) {
        isEnableTouch = enable;
    }

    public boolean onTextureViewTouchEvent(MotionEvent event) {
        if(isEnableTouch){
            super.onTextureViewTouchEvent(event);
            return renderer!=null && renderer.onTextureViewTouchEvent(event);
        }
        return false;
    }

    public void onCreateAsync(LSOAexModule module, OnCreateListener listener) {

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
            renderer =new LSOAexPlayerRender(getContext());
            setupSuccess=false;
        }
    }


    private boolean setupSuccess;

    /**
     * 增加一个AE模板
     * @param module 模板
     * @throws Exception
     */
    public void addAeModule(LSOAexModule module) throws Exception{
        if(module!=null){
            createRender();
            if(renderer!=null && setup()){
                renderer.addAeModule(module);
            }
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
//    public void setAexModule

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
     * 增加图片图层
     * @param bmp 图片
     * @param atCompUs 从容器的什么时间点开始增加;
     * @return
     */
    public LSOLayer addBitmapLayer(Bitmap bmp, long atCompUs) {
        createRender();
        if(renderer!=null && setup()){
            try {
                return renderer.addBitmapLayer(new LSOAsset(bmp),atCompUs);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }else{
            return null;
        }
    }

    /**
     * 增加图片图层
     * @param asset 图片图层资源
     * @param atCompUs 从容器的什么时间点开始增加;
     * @return
     */
    public LSOLayer addBitmapLayer(LSOAsset asset, long atCompUs) {
        createRender();
        if(renderer!=null && setup()){
            return renderer.addBitmapLayer(asset,atCompUs);
        }else{
            return null;
        }
    }
    /**
     * 增加gif图层
     * @param asset 图层资源;
     * @param atCompUs
     * @return
     */
    public LSOLayer addGifLayer(LSOAsset asset, long atCompUs){
        createRender();
        if(renderer!=null && setup()){
            return renderer.addGifLayer(asset,atCompUs);
        }else{
            return null;
        }
    }

    /**
     * 异步删除一个增加的图层
     * @param layer
     */
    public void removeLayerAsync(LSOLayer layer) {
        if(renderer!=null && layer!=null){
            renderer.removeLayerAsync(layer);
        }
    }

    /**
     * 异步删除所有图层;
     */
    public void removeAllOverlayLayersAsync() {
        if(renderer!=null){
            renderer.removeAllOverlayLayersAsync();
        }
    }

    /**
     * 获取当前时间点的图片信息;
     * @return
     */
    public LSOAexImage getCurrentAexImage(){
        if(renderer!=null){
            return renderer.getCurrentImageInfo();
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
     * 当用户点击屏幕, 选中一个图片时, 会返回这个图片的对象;
     * 监听返回的是 LSOAexImage对象, aexImage对象有index可以得到图片的index;
     * @param listener
     */
    public void setOnAexImageSelectedListener(OnAexImageSelectedListener listener){
        createRender();
        if(renderer!=null){
            renderer.setOnAexImageSelectedListener(listener);
        }
    }

    /**
     * 选中的图片;
     * @param listener
     */
    public void setOnAexTextSelectedListener(OnAexTextSelectedListener listener){
        createRender();
        if(renderer!=null){
            renderer.setOnAexTextSelectedListener(listener);
        }
    }


    /**
     * 当在播放过程中, 一个图片播放完毕后, 切换到下一张图片, 会触发此监听;
     * 当外部seek也会触发此监听;
     * @param listener 监听两个参数, int:index表示当前是总图片中的第几个,从0开始; LSOAeImage:表示当前图片对象;
     */
    public void setOnAexImageChangedListener(OnLSOAexImageChangedListener listener){
        createRender();
        if(renderer!=null){
            renderer.setOnAexImageChangedListener(listener);
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
    //-------------监听;
    public void setOnLanSongSDKTimeChangedListener(OnLanSongSDKTimeChangedListener listener){
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKTimeChangedListener(listener);
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


    /**
     * 压缩进度监听;
     * 视频或图片在播放前会有一个压缩处理的过程;,这里监听此过程;
     *
     * 监听的两个方法:
     *  percent 当前素材的百分比, numberIndex当前第几个;totalNumber总共多少个;
     *  void onCompressProgress(int percent, int numberIndex, int totalNumber);
     *
     *  监听完成;
     *  void onCompressCompleted();
     * @param listener
     */
    public void setOnLanSongSDKCompressListener(OnLanSongSDKCompressListener listener){
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKCompressListener(listener);
        }
    }

    /**
     * 开始预览
     */
    public void startPreview(){
        if(renderer!=null){
            renderer.startPreview(false);
        }
    }


    /**
     * 开始预览, 在播放完第一帧的时候, 是否要暂停;
     * @param pauseAfterFirstFrame
     */
    public void startPreview(boolean pauseAfterFirstFrame){
        if(renderer!=null){
            renderer.startPreview(pauseAfterFirstFrame);
        }
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
     * 暂停播放后的恢复;
     */
    public void resume(){
        if(renderer!=null){
            renderer.startPreview(false);
        }
    }



    public void setDisableTouchImage(boolean is){
        if(renderer!=null){
            renderer.setDisableTouchImage(is);
        }
    }
    public void setDisableTouchText(boolean is){
        if(renderer!=null){
            renderer.setDisableTouchText(is);
        }
    }


    public boolean isDisableTouchImage(){
        return renderer!=null && renderer.isDisableTouchImage();
    }
    public boolean isDisableTouchText(){
        return renderer!=null && renderer.isDisableTouchText();
    }


    public void setDisableTouchAdjust(boolean is){
        createRender();
        if(renderer!=null){
            renderer.setDisableTouchAdjust(is);
        }
    }
    /**
     * @param seekUs
     */
    public void seekToTimeUs(long seekUs){
        if(renderer!=null){
            renderer.seekToTimeUs(seekUs);
        }
    }

    /**
     * seek到指定的aexImage对象;
     * @param aexImage
     */
    public void seekToAexImage(LSOAexImage aexImage){
        if(renderer!=null && aexImage!=null){
            renderer.seekToAexImage(aexImage);
        }
    }

    /**
     * seek到指定的AexText对象;
     * @param aexText 文本对象;
     */
    public void seekToAexText(LSOAexText aexText){
        if(renderer!=null && aexText!=null){
            renderer.seekToAexText(aexText);
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
            renderer.updateDrawPadSize(getCompWidth(),getCompHeight());
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
