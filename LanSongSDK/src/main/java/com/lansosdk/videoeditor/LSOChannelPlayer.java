package com.lansosdk.videoeditor;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.Surface;


import com.lansosdk.box.LSOAsset;
import com.lansosdk.box.LSOChannelMode;
import com.lansosdk.box.LSOChannelRender;
import com.lansosdk.box.LSOFakeLayer;
import com.lansosdk.box.LSOFrameLayout;
import com.lansosdk.box.LSOLayerPosition;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LSOSize;
import com.lansosdk.box.OnAudioOutFrameListener;
import com.lansosdk.box.OnCreateListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnOutFrameListener;
import com.lansosdk.box.OnResumeListener;


public class LSOChannelPlayer extends LSOFrameLayout {

    private LSOChannelRender render;

    public LSOChannelPlayer(Context context) {
        super(context);
    }

    public LSOChannelPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LSOChannelPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LSOChannelPlayer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    //-----------copy code
    protected void sendOnCreateListener() {
        super.sendOnCreateListener();
    }

    public void sendOnResumeListener() {
        super.sendOnResumeListener();
    }

    //旋转移动缩放
    public boolean onTextureViewTouchEvent(MotionEvent event) {
        super.onTextureViewTouchEvent(event);
        if (render != null) {
            render.onTextureViewTouchEvent(event);
        }
        return true;
    }

    public void onCreateAsync(OnCreateListener listener) {
        LSOSize size = new LSOSize(1080, 1920);
        setPlayerSizeAsync((int) size.width, (int) size.height, listener);
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
    }

    public void onDestroy() {
        super.onDestroy();
        release();
    }

//-----------------------------------------------------------------
    /**
     * 把原来Camera的后置, 设置到指定id;
     * @param id
     */
    public static void setCameraBack0To(int id){
        LSOChannelRender.setCameraBack0To(id);
    }

    /**
     * 把原来Camera的前置, 设置到指定id;
     * @param id
     */
    public static void setCameraFront1To(int id){
        LSOChannelRender.setCameraFront1To(id);
    }

    public LSOFakeLayer setFirstChannelPath(String path) {
        if(render!=null){
            try {
                return render.setChannel1Asset(new LSOAsset(path));
            } catch (Exception e) {
                e.printStackTrace();
                LSOLog.e("setChannel2Path error. path is : ",e);
            }
        }
        return null;
    }

    public LSOFakeLayer setSecondChannelPath(String path) {
        if(render!=null){
            try {
                return render.setChannel2Asset(new LSOAsset(path));
            } catch (Exception e) {
                e.printStackTrace();
                LSOLog.e("setChannel2Path error. path is : ",e);
            }
        }
        return null;
    }



    public static final int CAMERA_ID0=0;  //后置镜头
    public static final int CAMERA_ID1=1;  //前置镜头;
    /**
     * 设置第一个通道为摄像头;
     * @param cameraId 只能是0 或者1
     */
    public LSOFakeLayer setFirstChannelCamera(int cameraId) {
        if(render!=null && LSOChannelRender.activity!=null){
            return render.setChannel1Camera(cameraId);
        }else{
            return null;
        }
    }

    /**
     *
     * @param cameraId 只能是0, 或1
     */
    public LSOFakeLayer setSecondChannelCamera(int cameraId) {
        if(render!=null && LSOChannelRender.activity!=null){
            return render.setChannel2Camera(cameraId);
        }else{
            return null;
        }
    }

    public void setChannel1Matting(boolean is){
        if(render!=null){
            render.setChannel1Matting(is);
        }
    }
    public boolean isGreenMatting(){
        return render!=null && render.isGreenMatting();
    }

    /**
     * 设置模式
     * @param type
     */
    public void setChannelShowMode(LSOChannelMode type) {
        if(render!=null){
            render.setChannelLayoutMode(type);
        }
    }

    public static final int BG_AUDIO_CHANNEL1=0;
    public static final int BG_AUDIO_CHANNEL2=1;
    public static final int BG_AUDIO_MP3=2;
    public static final int BG_AUDIO_NONE=3;
    /**
     * 背景声音的ID, id只能是 BG_AUDIO_CHANNEL1 / BG_AUDIO_CHANNEL2 / BG_AUDIO_MP3 / BG_AUDIO_NONE;
     * @param id
     */
    public void setAudioId(int id) {
        if(render!=null){
            render.setBackGroundMusicId(id);
        }
    }


    public void setAudioMusic(String mp3Path){
        if(render!=null){
            render.setAudioMusic(mp3Path);
        }
    }

    /**
     * 设置mic的音量;
     * @param volume 范围0--3.0f;
     */
    public void setMicVolume(float volume){
        if(render!=null){
            render.setMicVolume(volume);
        }
    }

    /**
     * 设置背景音乐的音量;
     * @param volume 范围0--3.0f
     */
    public void setMusicVolume(float volume){
        if(render!=null){
            render.setMusicVolume(volume);
        }
    }

    public boolean isCamera0Using(){
        return render!=null && render.isCamera0Using();
    }

    public boolean isCamera1Using(){
        return render!=null && render.isCamera1Using();
    }

    /**
     * 移出第一个通道
     */
    public void removeFirstChannel(){
        if(render!=null){
            render.removeFirstChannel();
        }
    }

    /**
     * 移出第二个通道
     */
    public void removeSecondChannel(){
        if(render!=null){
            render.removeSecondChannel();
        }
    }

    /**
     * 移出所有通道
     */
    public void removeAllChannels(){
        if(render!=null){
            render.removeAllChannels();
        }
    }

    /**
     * 取消选中通道
     */
    public void cancelSelectChannel(){
        if(render!=null){
            render.cancelSelectChannel();
        }
    }

    /**
     * 设置选中的颜色;
     * @param color
     */
    public void setSelectedColor(int color){
        if(render!=null){
            render.setSelectedColor(color);
        }
    }



    public boolean isRunning() {
        return render != null && render.isRunning();
    }

    public void setActivity(Activity is){
        LSOChannelRender.activity=is;
    }

    private OnLanSongSDKErrorListener userErrorListener;

    /**
     * 错误监听
     */
    public void setOnLanSongSDKErrorListener(OnLanSongSDKErrorListener listener) {
        createRender();
        userErrorListener = listener;
    }

    public void setOnOutFrameListener(OnOutFrameListener listener){
        if(render!=null){
            render.setOnOutFrameListener(listener);
        }
    }


    public void setCustomSize(int w,int h){
        if(render!=null){
            render.setCustomSize(w,h);
        }
    }
    /**
     * 在start之后调用;
     * 调用后, 并切换到摄像头 .
     * @return
     */
    public Surface getSurface(){
        if(render!=null){
            return render.getSurface();
        }
        return null;
    }
    /**
     *  设置录制
     * @param record 是否录制
     * @param dirPath 录制的视频保存到的文件夹;
     */
    public void setRecordVideoEnable(boolean record,String dirPath){
        if(render!=null){
            render.setRecordVideoEnable(record,dirPath);
        }
    }


    public void setOnAudioOutFrameListener(OnAudioOutFrameListener listener){
        if(render!=null){
            render.setOnAudioOutFrameListener(listener);
        }
    }

    /**
     * 是否要
     * @param is
     */
    public void setTouchEnable(boolean is){
        if(render!=null){
            render.setTouchEnable(is);
        }
    }


    public LSOFakeLayer setBitmapLogo(Bitmap bmp, LSOLayerPosition position){

        if(render!=null){
            return render.setBitmapLogo(bmp,position);
        }else{
            return null;
        }
    }

    public LSOFakeLayer setBitmapText(Bitmap bmp, LSOLayerPosition position){
        if(render!=null){
            return render.setBitmapText(bmp,position);
        }else{
            return null;
        }
    }

    /**
     * 删除logo 和文本图层;
     * @param layer
     */
    public void removeFakeLayer(LSOFakeLayer layer){
        if(render!=null){
            render.removeFakeLayer(layer);
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

    private void createRender() {
        if (render == null) {
            setUpSuccess = false;
            render = new LSOChannelRender(getContext());
            render.setOnLanSongSDKErrorListener(new OnLanSongSDKErrorListener() {
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
    public boolean start() {
        super.start();

        if (render != null && !setUpSuccess && getSurfaceTexture() != null) {
            render.updateCompositionSize(getCompWidth(), getCompHeight());
            render.setSurface(getSurfaceTexture(), getViewWidth(), getViewHeight());
            render.setup();
            setUpSuccess = true;
            LSOLog.d("LSOChannelPlayer comp size:" + getCompWidth() + " x " + getCompHeight() + " view size :" + getViewWidth() + " x " + getViewHeight());
        }
        return setUpSuccess;
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