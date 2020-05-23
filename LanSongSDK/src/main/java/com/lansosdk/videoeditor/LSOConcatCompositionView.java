package com.lansosdk.videoeditor;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import com.lansosdk.box.LSOAsset;
import com.lansosdk.box.LSOAudioLayer;
import com.lansosdk.box.LSOBitmapAsset;
import com.lansosdk.box.LSOBitmapLayer;
import com.lansosdk.box.LSOBitmapListLayer;
import com.lansosdk.box.LSOGifAsset;
import com.lansosdk.box.LSOGifLayer;
import com.lansosdk.box.LSOLayer;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LSOVideoAsset;
import com.lansosdk.box.LSOConcatCompositionRender;
import com.lansosdk.box.OnLanSongSDKAddVideoProgressListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKExportCompletedListener;
import com.lansosdk.box.OnLanSongSDKExportProgressListener;
import com.lansosdk.box.OnLanSongSDKPlayCompletedListener;
import com.lansosdk.box.OnLanSongSDKPlayProgressListener;
import com.lansosdk.box.OnLanSongSDKTimeChangedListener;

import java.util.Arrays;
import java.util.List;


public class LSOConcatCompositionView extends FrameLayout {
    /**
     * 渲染类;
     */
    private LSOConcatCompositionRender renderer;

    protected float padBGRed =0.0f;
    protected float padBGGreen =0.0f;
    protected float padBGBlur =0.0f;
    protected float padBGAlpha =1.0f;

    private TextureRenderView textureRenderView;

    public LSOLayerTouchView touchView;


    private SurfaceTexture mSurfaceTexture = null;
    private onViewAvailable mViewAvailable = null;
    private boolean isLayoutOk=false;
    // ----------------------------------------------
    public LSOConcatCompositionView(Context context) {
        super(context);
        initVideoView(context);
    }

    public LSOConcatCompositionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView(context);
    }

    public LSOConcatCompositionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LSOConcatCompositionView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
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

        textureRenderView.setDispalyRatio(0);

        View renderUIView = textureRenderView.getView();
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        renderUIView.setLayoutParams(lp);
        addView(renderUIView);
        textureRenderView.setVideoRotation(0);


        //---------
        touchView=new LSOLayerTouchView(getContext());
        LayoutParams lp2 = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        touchView.setLayoutParams(lp2);
        addView(touchView);

    }
    private void updateTouchViewSize(){
        LayoutParams params=(LayoutParams)touchView.getLayoutParams();
        params.width= viewWidth;
        params.height= viewHeight;
        touchView.setLayoutParams(params);
//        touchView.setBackgroundColor(Color.RED);

        touchView.setVideoCompositionView(this);
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
        void viewAvailable(LSOConcatCompositionView v);
    }

    private class SurfaceCallback implements TextureView.SurfaceTextureListener {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                              int width, int height) {
            mSurfaceTexture = surface;
            viewWidth=width;
            viewHeight=height;
            checkLayoutSize();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                                int width, int height) {
            mSurfaceTexture = surface;
            viewWidth=width;
            viewHeight=height;
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

    private OnCompositionSizeReadyListener sizeChangedListener;

    private int compWidth,compHeight;


    /**
     * 设置容器的大小, 在设置后, 我们会根据这个大小来调整 这个类的大小.
     * 从而让画面不变形;
     * @param width
     * @param height
     * @param listener
     */
    public void setCompositionSizeAsync(int width, int height, OnCompositionSizeReadyListener listener) {

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
        updateTouchViewSize();
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
            LSOLog.e("LSOConcatCompositionView layout view error.  return  callback");
            sendCompositionSizeListener();
            if(mViewAvailable!=null){
                mViewAvailable.viewAvailable(this);
            }
        }else{
            requestLayout();
        }
    }

    /**
     * 获得当前合成容器的宽度
     */
    public int getCompWidth() {
        return compWidth;
    }
    /**
     * 获得当前合成容器的高度
     */
    public int getCompHeight() {
        return compHeight;
    }
    //---------------------------------------------容器代码--------------------------------------------------------

    /**
     *   增加 拼接图层, 异步增加,增加过程中,内部会处理
     *   增加时, 会暂停当前合成的执行;
     * @param assets 当前资源里数组, 支持图片和视频.
     * @param listener 异步增加好后的回调, 里面的List Layer是当前 List<LSOAsset>对应生成的图层对象
     * @return
     */
    public boolean addConcatLayerListAsync(List<LSOAsset> assets, OnLanSongSDKAddVideoProgressListener listener){
        createRender();
        if (renderer != null && setup()) {
            userVideoProgressListener=listener;
            return renderer.addConcatLayerListAsync(assets, bodyVideoProgressListener);
        }else{
            return false;
        }
    }

    /**
     *  增加 拼接图层, 异步增加,增加过程中,内部会处理
     *   增加时, 会暂停当前合成的执行;
     * @param asset 当前的资源, 支持视频和图片
     * @param listener 异步增加好后的回调, 里面的List Layer是当前 LSOAsset对应生成的图层对象
     * @return
     */
    public boolean addConcatLayerAsync(LSOAsset asset,OnLanSongSDKAddVideoProgressListener listener){
        createRender();
        if (renderer != null && setup()) {
            userVideoProgressListener=listener;
            return renderer.addConcatLayerListAsync(Arrays.asList(asset), bodyVideoProgressListener);
        }else{
            return false;
        }
    }
    /**
     * 插入拼接图层;
     * @param videoArray
     * @param atCompUs
     * @param listener1
     * @return
     */
    public boolean insertConcatLayerListAsync(List<LSOAsset> videoArray,long atCompUs,OnLanSongSDKAddVideoProgressListener listener1) {
        if (renderer != null) {
            userVideoProgressListener=listener1;
            return renderer.insertConcatLayerListAsync(videoArray,atCompUs, bodyVideoProgressListener);
        }else{
            return false;
        }
    }
    private OnLanSongSDKAddVideoProgressListener bodyVideoProgressListener =new OnLanSongSDKAddVideoProgressListener() {
        @Override
        public void onAddVideoProgress(int percent, int numberIndex, int totalNumber) {
            if(userVideoProgressListener !=null){
                userVideoProgressListener.onAddVideoProgress(percent,numberIndex,totalNumber);
            }
        }

        @Override
        public void onAddVideoCompleted(List layers) {

            //LSTODO增加的图片和视频暂时不支持旋转移动缩放;
//            List<LSOLayer> layerList=layers;
//            for (LSOLayer layer: layerList){
//                addToTouchView(layer);
//            }


            if(userVideoProgressListener!=null){
                userVideoProgressListener.onAddVideoCompleted(layers);
            }

        }
    };

    private OnLanSongSDKAddVideoProgressListener userVideoProgressListener =null;
    /**
     * 替换拼接图层;
     * @param asset 资源
     * @param replaceLayer 被替换的图层
     * @param listener 异步替换;
     * @return
     */
    public boolean replaceConcatLayerListAsync(LSOAsset asset, LSOLayer replaceLayer,OnLanSongSDKAddVideoProgressListener listener) {
        if (renderer != null) {
            userVideoProgressListener=listener;
            return renderer.replaceConcatLayerListAsync(asset,replaceLayer, bodyVideoProgressListener);
        }else{
            return false;
        }
    }

    /**
     * 根据合成的时间, 得到对应的图层;
     * @param compUs
     * @return
     */
    public LSOLayer getCurrentConcatLayerByTime(long compUs){
        if(renderer!=null){
            return renderer.getCurrentConcatLayerByTime(compUs);
        }else{
            return null;
        }
    }

//-------------------------------------叠加层------------------------------------
    /**
     * 异步增加多个视频资源;
     * 增加完成后, 会通过 setOnAddVideoProgressListener异步返回结果.
     * 增加时, 会暂停当前合成的执行;
     * @param videoAsset 多个视频资源的数组
     */
    public boolean addVideoLayerAsync(LSOVideoAsset videoAsset,long atCompUs,
                                      OnLanSongSDKAddVideoProgressListener listener1){
        createRender();
        if (renderer != null && setup() && videoAsset!=null) {
            userVideoProgressListener=listener1;
            return renderer.addVideoLayerAsync(videoAsset,atCompUs, bodyVideoProgressListener);
        }else{
            return false;
        }
    }

    /**
     * 增加一张图片, 增加后返回一个图片图层对象;
     * @param bitmap 图片对象;
     * @return 返回图片图层对象;
     */
    public LSOBitmapLayer addBitmapLayer(Bitmap bitmap) {
        createRender();
        if (renderer != null && setup()) {
            LSOBitmapLayer layer= renderer.addBitmapLayer(bitmap,0);
            addToTouchView(layer);
            return layer;
        }else{
            return null;
        }
    }

    public LSOBitmapLayer addBitmapLayer(LSOBitmapAsset asset) {
        createRender();
        if (renderer != null && setup()) {
            LSOBitmapLayer layer= renderer.addBitmapLayer(asset,0);
            addToTouchView(layer);
            return layer;
        }else{
            return null;
        }
    }

    /**
     * 增加一张图片
     * @param bitmap 图片对象;
     * @param atCompUs  从合成的什么时间点开始增加, 单位us;
     * @return 返回图片图层对象;
     */
    public LSOBitmapLayer addBitmapLayer(Bitmap bitmap, long atCompUs) {
        createRender();
        if (renderer != null && setup()) {
            LSOBitmapLayer layer= renderer.addBitmapLayer(bitmap,atCompUs);
            addToTouchView(layer);
            return layer;
        }else{
            return null;
        }
    }

    /**
     * 增加图片资源.
     * @param asset
     * @param atCompUs
     * @return
     */
    public LSOBitmapLayer addBitmapLayer(LSOBitmapAsset asset, long atCompUs) {
        createRender();
        if (renderer != null && setup()) {
            LSOBitmapLayer layer= renderer.addBitmapLayer(asset,atCompUs);
            addToTouchView(layer);
            return layer;

        }else{
            return null;
        }
    }
    /**
     * 增加gif图层;
     * @param asset gif图层资源;
     * @return 返回gif图层对象;
     */
    public LSOGifLayer addGifLayer(LSOGifAsset asset) {
        createRender();
        if (asset!=null && renderer != null && setup()) {
            LSOGifLayer layer= renderer.addGifLayer(asset,0);
            addToTouchView(layer);
            return layer;
        }else{
            return null;
        }
    }
    /**
     * 增加gif图层
     * @param asset gif资源
     * @param atCompUs  从合成的什么时间点开始增加, 单位us;
     * @return
     */
    public LSOGifLayer addGifLayer(LSOGifAsset asset, long atCompUs) {
        createRender();
        if (renderer != null && setup()) {
            LSOGifLayer layer= renderer.addGifLayer(asset,atCompUs);
            addToTouchView(layer);
            return layer;
        }else{
            return null;
        }
    }

    /**
     * 增加一组图片图层(图片的完整路径)
     * @param list 一组图片的数组
     * @param frameInterval 帧之间的间隔; 多少微秒;
     * @param atCompUs 从合成的什么时间点开始增加, 单位us;
     * @return 返回图片数组图层;
     */
    public LSOBitmapListLayer addBitmapListLayerFromPaths(List<String> list, long frameInterval, long atCompUs) {
        createRender();
        if (renderer != null && setup()) {
            LSOBitmapListLayer bitmapListLayer= renderer.addBitmapListLayerFromPaths(list,frameInterval,atCompUs);
            addToTouchView(bitmapListLayer);
            return bitmapListLayer;
        }else{
            return null;
        }
    }

    /**
     * 增加一组图片图层(bitmap类型)
     * @param list 一组图片的数组
     * @param frameInterval 一秒钟显示多少帧;
     * @return 返回图片数组图层;
     */
    public LSOBitmapListLayer addBitmapArrayLayerFromBitmaps(List<Bitmap> list, long frameInterval, long atCompUs) {
        createRender();
        if (renderer != null && setup()) {
            LSOBitmapListLayer bitmapListLayer= renderer.addBitmapListLayerFromBitmaps(list,frameInterval,atCompUs);
            addToTouchView(bitmapListLayer);
            return bitmapListLayer;
        }else{
            return null;
        }
    }

    /**
     * 增加声音图层;
     * lsdelete
     * @param path 声音的完整路径;
     * @param startTimeOfComp 从合成的什么位置开始增加;
     * @return 返回声音对象;
     */
    public LSOAudioLayer addAudioLayer(String path, long startTimeOfComp) {
        createRender();
        if (renderer != null && setup()) {
            return renderer.addAudioLayer(path,startTimeOfComp);
        }else{
            return null;
        }
    }

    /**
     * 删除图层;
     * @param layer
     */
    public void removeLayer(LSOLayer layer){
        if(renderer!=null){
            renderer.removeLayer(layer);
            removeFromTouchView(layer);
        }
    }
    /**
     * 删除所有的图层;
     */
    public void removeAllLayers() {
        if(renderer!=null){
            renderer.removeAllLayers();
            if(touchView!=null){
                touchView.clear();
            }
        }
    }
    /**
     * 删除声音图层
     * @param layer
     */
    public void removeAudioLayer(LSOAudioLayer layer) {
        if(renderer!=null){
            renderer.removeAudioLayer(layer);
        }
    }

    /**
     * 删除所有的增加的声音图层;
     */
    public void removeALLAudioLayer() {
        if(renderer!=null){
            renderer.removeALLAudioLayer();
        }
    }

    /**
     * 打印当前拼接的图层时间信息;
     */
    public void printAllConcatLayerTime(){
        if(renderer!=null){
            renderer.printAllConcatLayerTime();
        }
    }
    /**
     * 设置容器的背景颜色;
     * @param color
     */
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
     * 把一张图片作为背景;
     * @param bmp
     * @return
     */
    public LSOLayer setBackGroundBitmap(Bitmap bmp){
        createRender();
        if(renderer!=null && setup() && bmp!=null) {
            try {
                return renderer.setBackGroundBitmapAsset(new LSOBitmapAsset(bmp));
            } catch (Exception e) {
                LSOLog.e("setBackGroundBitmap error", e);
            }
        }
        return null;
    }
    /**
     * 增加背景图片资源;
     * @return
     */
    public LSOLayer setBackGroundBitmapPath(String bmpPath){
        createRender();
        if(renderer!=null && setup() && bmpPath!=null) {
            try {
                return renderer.setBackGroundBitmapAsset(new LSOBitmapAsset(bmpPath));
            } catch (Exception e) {
                LSOLog.e("setBackGroundBitmapPath error", e);
            }
        }
        return null;
    }

    /**
     * 画面是否在播放
     * @return
     */
    public boolean isPlaying(){
        return renderer!=null && renderer.isPlaying();
    }

    /**
     * 合成容器是否在运行.
     * 合成是一个线程, 此返回的是当前线程是否在运行,
     *
     * 线程运行不一定画面在播放,有可能画面暂停;
     * @return
     */
    public boolean isRunning(){
        return renderer!=null && renderer.isRunning();
    }

    /**
     * 是否在导出;
     * @return
     */
    public boolean isExporting(){
        return renderer!=null && renderer.isExporting();
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
     * 获取当前合成容器中的所有图层;
     * @return 图层list
     */
    public List<LSOLayer> getConcatLayers(){
        if(renderer!=null){
            return renderer.getConcatLayers();
        }else {
            return null;
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


    public void setOnLanSongSDKTimeChangedListener(OnLanSongSDKTimeChangedListener listener){
        createRender();
        timeChangedListener=listener;
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
    /**
     * 错误监听
     */
    public void setOnLanSongSDKErrorListener(OnLanSongSDKErrorListener listener){
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKErrorListener(listener);
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
     * 开始导出;
     */
    public void startExport(){
        if(renderer!=null){
            renderer.startExport();
        }
    }

    /**
     * 开始导出.
     * 可设置导出分辨率.
     * @param width
     * @param height
     */
    public void startExport(int width,int height){
        if(renderer!=null){
            renderer.startExport(width,height);
        }
    }

    /**
     * 定位到某个位置.
     * @param timeUs time时间
     */
    public void seekToTimeUs(long timeUs){
        if(renderer!=null){
            renderer.seekToTimeUs(timeUs);
        }
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

        if(touchView!=null){
            touchView.disappearIconBorder();
        }

    }

    /**
     * 获取当前总的合成时间
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
     * 设置容器循环播放;
     * @param is 是否循环;
     */
    public void setLooping(boolean is){
        if(renderer!=null){
            renderer.setLooping(is);
        }
    }
    /**
     * 画面在重新布局后, 是否布局好.
     * @return
     */
    public boolean isLayoutValid(){
        return isLayoutOk;
    }
    /**
     * 取消当前合成.
     * 取消后, 会把内部线程全部退出, 所有增加的图层都会释放;
     */
    public void cancel(){
        if(renderer!=null){
            renderer.cancel();
            renderer=null;
        }
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
    }

    /**
     * 设置帧率
     * [不建议使用]
     * @param frameRate
     */
    public void setFrameRate(int frameRate) {
        if(renderer!=null){
            renderer.setFrameRate(frameRate);
        }
    }

    /**
     * 设置编码时的码率;
     * [不建议使用]
     * @param bitRate
     */
    public void setEncoderBitRate(int bitRate) {
        if(renderer!=null){
            renderer.setEncoderBitRate(bitRate);
        }
    }
    //内部使用;
    private void createRender(){
        if(renderer==null){
            renderer =new LSOConcatCompositionRender(getContext());
            renderer.setCompositionBackGroundColor(padBGRed,padBGGreen,padBGBlur,padBGAlpha);
        }
    }

    private OnLanSongSDKTimeChangedListener timeChangedListener=null;

    private void addToTouchView(LSOLayer layer){
//        if(touchView!=null && layer!=null){
//            touchView.addLayer(layer);
//        }
    }

    private void removeFromTouchView(LSOLayer layer){
        if(touchView!=null && layer!=null){
            touchView.removeLayer(layer);
        }
    }
    //内部使用;
    private boolean setup(){
        if(renderer!=null){
            if(!renderer.isRunning() && mSurfaceTexture!=null) {
                renderer.updateCompositionSize(compWidth, compHeight);
                renderer.setSurface(new Surface(mSurfaceTexture), viewWidth, viewHeight);
                renderer.setOnLanSongSDKPlayProgressListener(new OnLanSongSDKPlayProgressListener() {
                    @Override
                    public void onLanSongSDKPlayProgress(long ptsUs, int percent) {
                        if(touchView!=null){
                            touchView.updateLayerStatus();
                        }

                        if(timeChangedListener!=null){
                            timeChangedListener.onLanSongSDKTimeChanged(ptsUs,percent);
                        }
                    }
                });
                boolean setUpSuccess = renderer.setup();
                LSOLog.d("LSOConcatCompositionView setup.ret:" + setUpSuccess +
                        " comp size:" + compWidth + " x " + compHeight + " view size :" + viewWidth + " x " + viewHeight);
                return setUpSuccess;
            }else{
                return renderer.isRunning();
            }
        }else{
            return  false;
        }
    }

}
