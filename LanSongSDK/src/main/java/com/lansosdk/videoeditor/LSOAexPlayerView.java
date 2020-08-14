package com.lansosdk.videoeditor;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import com.lansosdk.aex.LSOAexImage;
import com.lansosdk.aex.LSOAexText;
import com.lansosdk.box.LSOAexModule;
import com.lansosdk.box.LSOAexPlayerRunnable;
import com.lansosdk.box.LSOAudioLayer;
import com.lansosdk.box.LSOBitmapAsset;
import com.lansosdk.box.LSOBitmapLayer;
import com.lansosdk.box.LSOBitmapListLayer;
import com.lansosdk.box.LSOGifAsset;
import com.lansosdk.box.LSOGifLayer;
import com.lansosdk.box.LSOLayer;
import com.lansosdk.box.LSOLayerPosition;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.OnAexTextSelectedListener;
import com.lansosdk.box.OnLSOAexImageChangedListener;
import com.lansosdk.box.OnAexImageSelectedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKExportCompletedListener;
import com.lansosdk.box.OnLanSongSDKExportProgressListener;
import com.lansosdk.box.OnLanSongSDKPlayCompletedListener;
import com.lansosdk.box.OnLanSongSDKPlayProgressListener;
import com.lansosdk.box.OnLanSongSDKCompressListener;
import com.lansosdk.box.OnLanSongSDKTimeChangedListener;

import java.util.List;


public class LSOAexPlayerView extends FrameLayout {
    /**
     * 渲染类;
     */
    private LSOAexPlayerRunnable renderer;


    protected float padBGRed =0.0f;
    protected float padBGGreen =0.0f;
    protected float padBGBlur =0.0f;
    protected float padBGAlpha =1.0f;

    private TextureRenderView textureRenderView;

    private SurfaceTexture mSurfaceTexture = null;
    private onViewAvailable mViewAvailable = null;
    private boolean isLayoutOk=false;
    // ----------------------------------------------
    public LSOAexPlayerView(Context context) {
        super(context);
        initVideoView(context);
    }

    public LSOAexPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView(context);
    }

    public LSOAexPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LSOAexPlayerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initVideoView(context);
    }

    private void initVideoView(Context context) {
        setTextureView();
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
    }

    private int viewWidth,viewHeight;

    private void setTextureView() {
        textureRenderView = new TextureRenderView(getContext());
        textureRenderView.setSurfaceTextureListener(new SurfaceCallback());

        textureRenderView.setDisplayRatio(0);

        View renderUIView = textureRenderView.getView();
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        renderUIView.setLayoutParams(lp);
        addView(renderUIView);
        textureRenderView.setVideoRotation(0);

        textureRenderView.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return onTextureViewTouchEvent(event);
            }
        });
    }

    /**
     *当前View有效的时候, 回调监听;
     */
    public void setOnViewAvailable(onViewAvailable listener) {
        mViewAvailable = listener;
        if (mSurfaceTexture != null) {
            if(viewHeight >0 && viewWidth >0 && compWidth>0 && compHeight>0){

                float wantRadio = (float) compWidth / (float) compHeight;
                float viewRadio = (float) viewWidth / (float) viewHeight;

                if (wantRadio == viewRadio) { // 如果比例已经相等,不需要再调整,则直接显示.
                    isLayoutOk=true;
                    mViewAvailable.viewAvailable(this);
                } else if (Math.abs(wantRadio - viewRadio) * 1000 < 16.0f) {
                    isLayoutOk=true;
                    mViewAvailable.viewAvailable(this);
                }else{
                    textureRenderView.setVideoSize(compWidth, compHeight);
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
        void viewAvailable(LSOAexPlayerView v);
    }

    private class SurfaceCallback implements TextureView.SurfaceTextureListener {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                              int width, int height) {


            LSOLog.d(" LSOAexPlayerView onSurfaceTextureAvailable...");
            mSurfaceTexture = surface;
            viewWidth=width;
            viewHeight=height;
            checkLayoutSize();

        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                                int width, int height) {

            LSOLog.d(" LSOAexPlayerView onSurfaceTextureSizeChanged...");
            mSurfaceTexture = surface;
            viewWidth=width;
            viewHeight=height;
            checkLayoutSize();

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {

            LSOLog.d(" LSOAexPlayerView onSurfaceTextureDestroyed...");
            mSurfaceTexture = null;
            isLayoutOk=false;
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {



        }
    }
    public interface OnSizeReadyListener {
        /**
         * 合成(容器)的宽高
         */
        void onSizeReady();
    }


    private OnSizeReadyListener sizeChangedListener;

    private int compWidth,compHeight;

    public void onPause(){
        pause();
    }
    public void onResume(){


    }
    /**
     * 设置容器的大小, 在设置后, 我们会根据这个大小来调整 这个类的大小.
     * 从而让画面不变形;
     * @param width
     * @param height
     * @param listener
     */
    public void setAexPlayerViewSizeAsync(int width, int height, OnSizeReadyListener listener) {
//        这里有设置的容器尺寸, 和实际显示的尺寸;
        requestLayoutCount=0;
        compWidth=width;
        compHeight=height;
        sizeChangedListener = listener;
        if (width != 0 && height != 0) {
            if(viewWidth ==0 || viewHeight ==0){  //直接重新布局UI
                textureRenderView.setVideoSize(width, height);
                textureRenderView.setVideoSampleAspectRatio(1, 1);
                requestLayoutPreview();
            }else{
                float setRatio = (float) width / (float) height;
                float setViewRatio = (float) viewWidth / (float) viewHeight;

                if (setRatio == setViewRatio) { // 如果比例已经相等,不需要再调整,则直接显示.
                        isLayoutOk=true;
                        sendCompositionSizeListener();
                } else if (Math.abs(setRatio - setViewRatio) * 1000 < 16.0f) {
                    if (listener != null) {
                        isLayoutOk=true;
                        sendCompositionSizeListener();
                    }
                } else if (textureRenderView != null) {
                    textureRenderView.setVideoSize(width, height);
                    textureRenderView.setVideoSampleAspectRatio(1, 1);
                    sizeChangedListener = listener;
                }
                requestLayoutPreview();
            }
        }
    }
    private void sendCompositionSizeListener(){
        if(sizeChangedListener!=null){
            sizeChangedListener.onSizeReady();
        }
    }

    private int requestLayoutCount=0;
    /**
     * 检查得到的大小, 如果和view成比例,则直接回调; 如果不成比例,则重新布局;
     */
    private void checkLayoutSize(){
        float desireRatio = (float) compWidth / (float) compHeight;
        float padRatio = (float) viewWidth / (float) viewHeight;

        if (desireRatio == padRatio) { // 如果比例已经相等,不需要再调整,则直接显示.
            isLayoutOk=true;
            sendCompositionSizeListener();
            if(mViewAvailable!=null){
                mViewAvailable.viewAvailable(this);
            }
        } else if (Math.abs(desireRatio - padRatio) * 1000 < 16.0f) {
            isLayoutOk=true;
            sendCompositionSizeListener();
            if(mViewAvailable!=null){
                mViewAvailable.viewAvailable(this);
            }
        }else{
            textureRenderView.setVideoSize(compWidth, compHeight);
            textureRenderView.setVideoSampleAspectRatio(1, 1);
            LSOLog.d("checkLayoutSize no  right, layout again...");
            requestLayoutPreview();
        }
    }
    private void requestLayoutPreview()
    {
        requestLayoutCount++;
        if(requestLayoutCount>3){
            LSOLog.e("LSOAexPlayerView layout view error.  return  callback");
            sendCompositionSizeListener();
            if(mViewAvailable!=null){
                mViewAvailable.viewAvailable(this);
            }
        }else{
            requestLayout();
        }
    }


    public boolean onTextureViewTouchEvent(MotionEvent event) {
        if(renderer!=null){
            renderer.onTextureViewTouchEvent(event);
        }
        return true;
    }

    //---------------------------------------------容器代码--------------------------------------------------------

    private void createRender(){
        if(renderer==null){
            renderer =new LSOAexPlayerRunnable(getContext());
            renderer.setBackGroundColor(padBGRed,padBGGreen,padBGBlur,padBGAlpha);
            setupSuccess=false;
        }
    }

    public int getCompWidth() {
        return compWidth;
    }
    public int getCompHeight() {
        return compHeight;
    }

    //容器背景颜色;
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
            renderer.setBackGroundColor(padBGRed,padBGGreen,padBGBlur,1.0f);
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
    public LSOAudioLayer addAudioLayer(String path, long startTimeOfComp) {
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
    public void removeAudioLayerAsync(LSOAudioLayer layer) {
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
    public LSOBitmapLayer addBitmapLayer(Bitmap bmp, long atCompUs) {
        createRender();
        if(renderer!=null && setup()){
            return renderer.addBitmapLayer(bmp,atCompUs);
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
    public LSOBitmapLayer addBitmapLayer(LSOBitmapAsset asset, long atCompUs) {
        createRender();
        if(renderer!=null && setup()){
            return renderer.addBitmapLayer(asset,atCompUs);
        }else{
            return null;
        }
    }

    /**
     * 增加图片序列
     * @param list 图片序列列表
     * @param frameIntervalUs  两张图片的间隔
     * @param atCompUs 从容器的什么时间点开始增加;
     * @return
     */
    public LSOBitmapListLayer addBitmapListLayerFromPaths(List<String> list, long frameIntervalUs, long atCompUs) {
        createRender();
        if(renderer!=null && setup()){
            return renderer.addBitmapListLayerFromPaths(list,frameIntervalUs,atCompUs);
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
    public LSOGifLayer addGifLayer(LSOGifAsset asset, long atCompUs){
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
     * 模板导出;
     * 可设置宽度和高度, 宽度和高度一定和模板的宽高等比例
     * @param width
     * @param height
     */
    public void startExport(int width, int height) {
        if(renderer!=null){
            renderer.startExport(width,height);
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

    /**
     * 当前布局是否好.
     * @return
     */
    public boolean isLayoutValid(){
        return isLayoutOk;
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
    public void release(){
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
        if(renderer!=null && mSurfaceTexture!=null){
            renderer.updateDrawPadSize(compWidth,compHeight);
            renderer.setSurface(new Surface(mSurfaceTexture),viewWidth,viewHeight);
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
