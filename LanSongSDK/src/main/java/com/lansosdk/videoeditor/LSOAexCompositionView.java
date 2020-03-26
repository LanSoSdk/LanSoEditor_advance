package com.lansosdk.videoeditor;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import com.lansosdk.aex.LSOAexDrawable;
import com.lansosdk.box.LSOAexCompositionRunnable;
import com.lansosdk.box.LSOAudioAsset;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LSOMVAsset;
import com.lansosdk.box.LSOVideoAsset;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.OnLanSongSDKThreadProgressListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;


/**
 * 多个图层拼接 预览类
 * 在拼接的过程中, 可以增加动画, 即实现转场, 拼接默认在图层的最后一秒的下方叠加另一个图层; 两个图层有重叠的一秒时间;
 * 2019年9--10.18
 */
public class LSOAexCompositionView extends FrameLayout {

    /**
     * 渲染类;
     */
    private LSOAexCompositionRunnable renderer;

    /**
     * 渲染窗口;
     */
    private TextureRenderView textureRenderView;
    private SurfaceTexture mSurfaceTexture = null;
    private int drawPadWidth, drawPadHeight;
    private onViewAvailable mViewAvailable = null;
    private boolean isLayoutOk=false;
    // ----------------------------------------------
    public LSOAexCompositionView(Context context) {
        super(context);
        initVideoView(context);
    }

    public LSOAexCompositionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView(context);
    }

    public LSOAexCompositionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LSOAexCompositionView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initVideoView(context);
    }

    private void initVideoView(Context context) {
        setTextureView();
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
    }

    private void setTextureView() {
        textureRenderView = new TextureRenderView(getContext());
        textureRenderView.setSurfaceTextureListener(new SurfaceCallback());

        textureRenderView.setDispalyRatio(0);

        View renderUIView = textureRenderView.getView();
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        renderUIView.setLayoutParams(lp);
        addView(renderUIView);
        textureRenderView.setVideoRotation(0);
    }
    /**
     *当前View有效的时候, 回调监听;
     */
    public void setOnViewAvailable(onViewAvailable listener) {
        mViewAvailable = listener;
        if (mSurfaceTexture != null) {
            if(drawPadHeight>0 && drawPadWidth>0 && desireWidth>0 && desireHeight>0){

                float acpect = (float) desireWidth / (float) desireHeight;
                float padAcpect = (float) drawPadWidth / (float) drawPadHeight;

                if (acpect == padAcpect) { // 如果比例已经相等,不需要再调整,则直接显示.
                    isLayoutOk=true;
                    mViewAvailable.viewAvailable(this);
                } else if (Math.abs(acpect - padAcpect) * 1000 < 16.0f) {
                    isLayoutOk=true;
                    mViewAvailable.viewAvailable(this);
                }else{
                    textureRenderView.setVideoSize(desireWidth, desireHeight);
                    textureRenderView.setVideoSampleAspectRatio(1, 1);
                    LSOLog.d("setOnViewAvailable layout again...");
                    requestLayoutPreview();
                }
            }
        }
    }
    public Surface getSurface(){
        if(mSurfaceTexture!=null){
            return  new Surface(mSurfaceTexture);
        }
        return null;
    }
    public interface onViewAvailable {
        void viewAvailable(LSOAexCompositionView v);
    }

    private class SurfaceCallback implements TextureView.SurfaceTextureListener {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                              int width, int height) {
            mSurfaceTexture = surface;
            drawPadHeight = height;
            drawPadWidth = width;
            checkLayoutSize();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                                int width, int height) {
            mSurfaceTexture = surface;
            drawPadHeight = height;
            drawPadWidth = width;
            checkLayoutSize();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            mSurfaceTexture = null;
            isLayoutOk=false;
            release();
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    }

    private int desireWidth;
    private int desireHeight;
    private onDrawPadSizeChangedListener sizeChangedListener;

    /**
     * 设置容器的大小, 在设置后, 我们会根据这个大小来调整 这个类的大小.
     * 从而让画面不变形;
     * @param width
     * @param height
     * @param cb
     */
    public void setDrawPadSize(int width, int height, onDrawPadSizeChangedListener cb) {

        requestLayoutCount=0;
        desireWidth=width;
        desireHeight=height;

        if (width != 0 && height != 0) {
            if(drawPadWidth==0 || drawPadHeight==0){  //直接重新布局UI
                textureRenderView.setVideoSize(width, height);
                textureRenderView.setVideoSampleAspectRatio(1, 1);
                sizeChangedListener = cb;
                requestLayoutPreview();
            }else{
                float setAcpect = (float) width / (float) height;
                float setViewacpect = (float) drawPadWidth / (float) drawPadHeight;

                if (setAcpect == setViewacpect) { // 如果比例已经相等,不需要再调整,则直接显示.
                    if (cb != null) {
                        isLayoutOk=true;
                        cb.onSizeChanged(width, height);
                    }
                } else if (Math.abs(setAcpect - setViewacpect) * 1000 < 16.0f) {
                    if (cb != null) {
                        isLayoutOk=true;
                        cb.onSizeChanged(width, height);
                    }
                } else if (textureRenderView != null) {
                    textureRenderView.setVideoSize(width, height);
                    textureRenderView.setVideoSampleAspectRatio(1, 1);
                    sizeChangedListener = cb;
                }
                requestLayoutPreview();
            }
        }
    }

    private int requestLayoutCount=0;
    /**
     * 检查得到的大小, 如果和view成比例,则直接回调; 如果不成比例,则重新布局;
     */
    private void checkLayoutSize(){
        float desireRatio = (float) desireWidth / (float) desireHeight;
        float padRatio = (float) drawPadWidth / (float) drawPadHeight;

        if (desireRatio == padRatio) { // 如果比例已经相等,不需要再调整,则直接显示.
            isLayoutOk=true;
            if (sizeChangedListener != null) {
                sizeChangedListener.onSizeChanged(drawPadWidth, drawPadHeight);
                sizeChangedListener =null;
            }else if(mViewAvailable!=null){
                mViewAvailable.viewAvailable(this);
            }
        } else if (Math.abs(desireRatio - padRatio) * 1000 < 16.0f) {
            isLayoutOk=true;
            if (sizeChangedListener != null) {
                sizeChangedListener.onSizeChanged(drawPadWidth, drawPadHeight);
                sizeChangedListener =null;
            }else if(mViewAvailable!=null){
                mViewAvailable.viewAvailable(this);
            }
        }else{
            textureRenderView.setVideoSize(desireWidth, desireHeight);
            textureRenderView.setVideoSampleAspectRatio(1, 1);
            LSOLog.d("checkLayoutSize no  right, layout again...");
            requestLayoutPreview();
        }
    }
    private void requestLayoutPreview()
    {
        requestLayoutCount++;
        if(requestLayoutCount>3){
            LSOLog.e("DrawPadAEPreview layout view error.  return  callback");
            if(sizeChangedListener!=null){
                sizeChangedListener.onSizeChanged(drawPadWidth,drawPadHeight);
            }else if(mViewAvailable!=null){
                mViewAvailable.viewAvailable(this);
            }
        }else{
            requestLayout();
        }
    }
    /**
     * 获取当前View的 宽度
     */
    public int getViewWidth() {
        return drawPadWidth;
    }

    /**
     * 获得当前View的高度.
     */
    public int getViewHeight() {
        return drawPadHeight;
    }
    /**
     * 获得当前容器的宽度
     */
    public int getDrawPadWidth() {
        return drawPadWidth;
    }
    /**
     * 获得当前容器的高度
     */
    public int getDrawPadHeight() {
        return drawPadHeight;
    }
   //---------------------------------------------容器代码--------------------------------------------------------


    private void createRender(){
        if(renderer==null){
            renderer =new LSOAexCompositionRunnable(getContext());
            renderer.setCompositionBackGroundColor(padBGRed,padBGGreen,padBGBlur,padBGAlpha);
            setupSuccess=false;
        }
    }

    /**
     * 在预览的时候, 是否设置循环.
     * 默认循环播放
     * @param is 循环
     */
    public void setPreviewLooping(boolean is){
        createRender();
        if(renderer !=null) {
           renderer.setPreviewLooping(is);
        }
    }
    //---------------------------容器背景颜色;
    protected float padBGRed =0.0f;
    protected float padBGGreen =0.0f;
    protected float padBGBlur =0.0f;
    protected float padBGAlpha =1.0f;
    /**
     * 设置容器的背景颜色;
     * @param color
     */
    @Override
    public void setBackgroundColor(int color) {
        int red = Color.red(color);  //<---拷贝这里的代码;3行
        int green = Color.green(color);
        int blue = Color.blue(color);

        padBGRed=(float)red/255f;
        padBGGreen=(float)green/255f;
        padBGBlur=(float)blue/255f;
        if(renderer!=null){
            renderer.setCompositionBackGroundColor(padBGRed,padBGGreen,padBGBlur,1.0f);
        }
    }
    /**
     * 设置容器的 背景颜色RGBA分量
     * 在增加各种图层前调用;
     * @param r 红色分量, 范围0.0  ---1.0;
     * @param g 绿色分量, 范围0.0  ---1.0;
     * @param b 蓝色分量, 范围0.0  ---1.0;
     * @param a 透明分量, 范围0.0  ---1.0; 建议为1.0; 因为TextureView在android9.0一下, 是不透明的,设置为0.0可能不起作用;
     */
    public  void setDrawPadBackGroundColor(float r,float g,float b,float a){
        padBGRed=r;
        padBGGreen=g;
        padBGBlur=b;
        padBGAlpha=a;
        if(renderer!=null){
            renderer.setCompositionBackGroundColor(r,g,b,a);
        }
    }

    private boolean setupSuccess;

    public boolean addFirstLayer(LSOVideoAsset videoAsset){
        createRender();
        if (renderer != null &&  setup() && videoAsset!=null) {
            return renderer.addFirstLayer(videoAsset);
        }else{
            return false;
        }
    }
    public boolean addSecondLayer(LSOAexDrawable drawable){
        createRender();
        if (renderer != null &&  setup() && drawable!=null) {
            return renderer.addSecondLayer(drawable);
        }else{
            return false;
        }
    }
    
    public boolean addMVLayer(LSOMVAsset asset){
        createRender();
        if (renderer != null &&  setup() && asset!=null) {
            return renderer.addMVLayer(asset);
        }else{
            return false;
        }
    }
    //------------------增加音频图层---------------------------------
    /**
     * 增加声音图层;
     * @param audioAsset 声音文件对象;
     * @return
     */
    public boolean addAudioAsset(LSOAudioAsset audioAsset) {
        if (renderer != null && audioAsset!=null) {
             return renderer.addAudioAsset(audioAsset);
        } else {
            LSOLog.e(" addAudioAsset error. path:"+audioAsset);
            return false;
        }
    }
    //------------------------拼接类------------------------------
    public boolean isRunning(){
        return renderer!=null && renderer.isRunning();
    }
    /**
     * 预览 进度监听
     */
    public void setOnLanSongSDKProgressListener(OnLanSongSDKProgressListener listener) {
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKProgressListener(listener);
        }
    }
    public void setOnLanSongSDKThreadProgressListener(OnLanSongSDKThreadProgressListener listener){
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKThreadProgressListener(listener);
        }
    }

    /**
     * 完成回调.
     * 2. 预览后, 导出完成.
     */
    public void setOnLanSongSDKCompletedListener(OnLanSongSDKCompletedListener listener){
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKCompletedListener(listener);
        }
    }
    /**
     * 错误监听
     */
    public void setOnLanSongSDKErrorListener(OnLanSongSDKErrorListener listener){
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKErrorListener(listener);
        }
    }


    public boolean startPreview(){

        if(renderer!=null && setupSuccess){
            return renderer.startPreview();
        }else{
            return false;
        }
    }
    public long getDurationUs(){
        if(renderer!=null){
            return  renderer.getDurationUs();
        }else{
            return 1000;
        }
    }

    /**
     * 布局是否好.
     * @return
     */
    public boolean isLayoutValid(){
        return isLayoutOk;
    }



    public boolean isPlaying(){
        return renderer!=null && renderer.isPlaying();
    }
    public void pausePreview(){
        if(renderer!=null){
            renderer.pausePreview();
        }
    }
    public void resumePreview(){
        if(renderer!=null){
            renderer.resumePreview();
        }
    }

    /**
     * @param index
     */
    public void seekToFrameIndex(int index){
        if(renderer!=null){
            renderer.seekToFrameIndex(index);
        }
    }

    public int getCurrentFrameIndex(){
        if(renderer!=null){
            return renderer.getCurrentFrameIndex();
        }else{
            return 0;
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
     */
    public void seekToNextFrame(){
        if(renderer!=null){
            renderer.showNextFrame();
        }
    }

    /**
     */
    public void seekToPreviousFrame(){
        if(renderer!=null){
            renderer.showPreviousFrame();
        }
    }



    private boolean setup(){
        if(setupSuccess){
            return true;
        }
        if(renderer!=null && mSurfaceTexture!=null && !setupSuccess){
            renderer.updateDrawPadSize(drawPadWidth,drawPadHeight);
            renderer.setSurface(new Surface(mSurfaceTexture));
            setupSuccess= renderer.setup();
            return setupSuccess;
        }else{
            return  false;
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
    public void release(){
        if(renderer!=null){
           renderer.release();
            renderer=null;
        }
        setupSuccess=false;
    }
}
