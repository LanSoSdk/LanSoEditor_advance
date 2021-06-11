package com.lansosdk.videoeditor;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.lansosdk.aex.LSOAexImage;
import com.lansosdk.aex.LSOAexText;
import com.lansosdk.box.LSOAexModule;
import com.lansosdk.box.LSOAexPlayerRender;
import com.lansosdk.box.LSOAexSegmentModule;
import com.lansosdk.box.LSOAexSegmentPlayerRender;
import com.lansosdk.box.LSOAsset;
import com.lansosdk.box.LSOFrameLayout;
import com.lansosdk.box.LSOLayer;
import com.lansosdk.box.LSOLayerPosition;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LSOSegmentMode;
import com.lansosdk.box.OnAddPathListener;
import com.lansosdk.box.OnAexImageSelectedListener;
import com.lansosdk.box.OnAexTextSelectedListener;
import com.lansosdk.box.OnCompressListener;
import com.lansosdk.box.OnCreateListener;
import com.lansosdk.box.OnLSOAexImageChangedListener;
import com.lansosdk.box.OnLanSongSDKCompressListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKExportCompletedListener;
import com.lansosdk.box.OnLanSongSDKExportProgressListener;
import com.lansosdk.box.OnLanSongSDKPlayCompletedListener;
import com.lansosdk.box.OnLanSongSDKPlayProgressListener;
import com.lansosdk.box.OnLanSongSDKTimeChangedListener;
import com.lansosdk.box.OnPrepareListener;
import com.lansosdk.box.OnResumeListener;
import com.lansosdk.box.OnTextureAvailableListener;


public class LSOAexSegmentPlayer extends LSOFrameLayout {




    private LSOAexSegmentPlayerRender renderer;


    public LSOAexSegmentPlayer(Context context) {
        super(context);
    }

    public LSOAexSegmentPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LSOAexSegmentPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LSOAexSegmentPlayer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
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


    public boolean onTextureViewTouchEvent(MotionEvent event) {
        return false;
    }

    public void onCreateAsync(LSOAexSegmentModule module, OnCreateListener listener) {

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


    private void createRender(){
        if(renderer==null){
            renderer =new LSOAexSegmentPlayerRender(getContext());
            setupSuccess=false;
        }
    }


    private boolean setupSuccess;



    /**
     * 增加一个AE模板
     * @param module 模板
     * @throws Exception
     */
    public void addAeModule(LSOAexSegmentModule module) throws Exception{
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

    /**
     * 设置模板声音
     * @param volume 1.0为正常. 0是静音, 2.0是放大一倍;
     */
    public void setModuleVolume(float volume) {
        if(renderer!=null){
            renderer.setModuleAudioVolume(volume);
        }
    }

    /**
     * 获取模板声音
     * @return
     */
    public float getModuleVolume() {
        if(renderer!=null){
            return renderer.getModuleAudioVolume();
        }else{
            return 1.0f;
        }
    }

    /**
     * 设置外部声音,设置后, 会替换掉之前的声音;
     * @param path 路径, 支持mp3, 有音乐的mp4或mov视频
     * @param listener 异步设置后的回调; 设置后, 会替换掉之前的声音;
     */
    public void setAudioPath(String path, OnAddPathListener listener) {
        if(renderer!=null){
            renderer.setAudioPath(path,1.0f,0,Long.MAX_VALUE, listener);
        }
    }

    /**
     * 设置外部声音,设置后, 会替换掉之前的声音;
     * @param path 声音路径
     * @param volume 声音音量
     * @param cutStartUs 对声音的裁剪,开始时间
     * @param cutEndUs 对声音的裁剪结束时间, 如果不裁剪,则为Long.MAX_VALUE
     * @param listener 声音是异步增加, 异步执行后的回调;
     */
    public void setAudioPath(String path, float volume, long cutStartUs, long cutEndUs, OnAddPathListener listener) {
        if(renderer!=null){
            renderer.setAudioPath(path,volume,cutStartUs,cutEndUs, listener);
        }
    }

    /**
     * 删除外部增加的声音
     */
    public void removeAudioPath(){
        if(renderer!=null){
            renderer.setAudioPath(null,0,0,0, null);
        }
    }

    /**
     * 设置增加的声音音量
     * @param volume 1.0为正常. 0是静音, 2.0是放大一倍;
     */
    public void setAddAudioVolume(float volume) {
        if(renderer!=null){
            renderer.setAddAudioVolume(volume);
        }
    }

    /**
     * 获取增加的声音音量
     * @return
     */
    public float getAddAudioVolume() {
        if(renderer!=null){
            return renderer.getAddAudioVolume();
        }else{
            return 1.0f;
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
     * 异步删除一个增加的图层
     * @param layer
     */
    public void removeLayerAsync(LSOLayer layer) {
        if(renderer!=null && layer!=null){
            renderer.removeLayerAsync(layer);
        }
    }





    /**
     * 内部创建的线程是否在运行;
     * @return
     */
    public boolean isRunning(){
        return renderer!=null && renderer.isRunning();
    }



    public void setOnCompressListener(OnCompressListener listener){
        createRender();
        if(renderer!=null){
            renderer.setOnCompressListener(listener);
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
     */
    public void setOnLanSongSDKPlayCompletedListener(OnLanSongSDKPlayCompletedListener listener) {
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKPlayCompletedListener(listener);
        }
    }

    /**
     * 导出进度回调;
     */
    public void setOnLanSongSDKExportProgressListener(OnLanSongSDKExportProgressListener listener) {
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKExportProgressListener(listener);
        }
    }

    /**
     * 导出完成回调;
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
     */
    public void setOnLanSongSDKCompressListener(OnLanSongSDKCompressListener listener){
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKCompressListener(listener);
        }
    }


    /**
     * 准备一下, 异步执行;
     * @param listener
     */
    public void prepareAsync(OnPrepareListener listener){
        if(renderer!=null){
            renderer.prepareAsync(listener);
        }else if(listener!=null){
            listener.onSuccess(false);
        }
    }


    /**
     * LSNEW startPreview改成 start();
     * 开始预览;
     * @return
     */
    public boolean start(){
        super.start();
        if(renderer!=null){
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
     * 设置循环
     * @param is
     */
    public void setLooping(boolean is){
        if(renderer!=null){
            renderer.setLooping(is);
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
     * 获取当前时间, 单位微秒;
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
