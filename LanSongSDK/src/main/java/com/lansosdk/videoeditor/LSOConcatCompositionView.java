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

import com.lansosdk.box.LSOAsset;
import com.lansosdk.box.LSOAudioLayer;
import com.lansosdk.box.LSOBitmapAsset;
import com.lansosdk.box.LSOBitmapLayer;
import com.lansosdk.box.LSOBitmapListLayer;
import com.lansosdk.box.LSOGifAsset;
import com.lansosdk.box.LSOGifLayer;
import com.lansosdk.box.LSOLayer;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LSOMVAsset;
import com.lansosdk.box.LSOVideoAsset;
import com.lansosdk.box.LSOConcatCompositionRender;
import com.lansosdk.box.OnLanSongSDKAddVideoProgressListener;
import com.lansosdk.box.OnLanSongSDKBeforeRenderFrameListener;
import com.lansosdk.box.OnLanSongSDKCompDurationChangedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKExportCompletedListener;
import com.lansosdk.box.OnLanSongSDKExportProgressListener;
import com.lansosdk.box.OnLanSongSDKLayerTouchEventListener;
import com.lansosdk.box.OnLanSongSDKPlayCompletedListener;
import com.lansosdk.box.OnLanSongSDKPlayProgressListener;
import com.lansosdk.box.OnLanSongSDKTimeChangedListener;
import com.lansosdk.box.OnLanSongSDKUserSelectedLayerListener;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class LSOConcatCompositionView extends FrameLayout {
    /**
     * 渲染类;
     */
    private LSOConcatCompositionRender renderer;

    protected float padBGRed = 0.0f;
    protected float padBGGreen = 0.0f;
    protected float padBGBlur = 0.0f;
    protected float padBGAlpha = 1.0f;

    private TextureRenderView textureRenderView;


    private SurfaceTexture mSurfaceTexture = null;
    private onViewAvailable mViewAvailable = null;
    private boolean isLayoutOk = false;
    // --------------------

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

    private int viewWidth, viewHeight;

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
     * 当前View有效的时候, 回调监听;
     */
    public void setOnViewAvailable(onViewAvailable listener) {
        mViewAvailable = listener;
        if (mSurfaceTexture != null) {
            if (viewHeight > 0 && viewWidth > 0 && compWidth > 0 && compHeight > 0) {

                float wantRadio = (float) compWidth / (float) compHeight;
                float viewRadio = (float) viewWidth / (float) viewHeight;

                if (wantRadio == viewRadio) { // 如果比例已经相等,不需要再调整,则直接显示.
                    isLayoutOk = true;
                    mViewAvailable.viewAvailable(this);
                } else if (Math.abs(wantRadio - viewRadio) * 1000 < 16.0f) {
                    isLayoutOk = true;
                    mViewAvailable.viewAvailable(this);
                } else {
                    textureRenderView.setVideoSize(compWidth, compHeight);
                    textureRenderView.setVideoSampleAspectRatio(1, 1);
                    LSOLog.d("setOnViewAvailable layout again...");
                    requestLayoutPreview();
                }
            }
        }
    }

    public Surface getSurface() {
        if (mSurfaceTexture != null) {
            return new Surface(mSurfaceTexture);
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
            viewWidth = width;
            viewHeight = height;
            checkLayoutSize();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                                int width, int height) {
            mSurfaceTexture = surface;
            viewWidth = width;
            viewHeight = height;
            checkLayoutSize();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            mSurfaceTexture = null;
            isLayoutOk = false;
            release();
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    }

    private OnCompositionSizeReadyListener sizeChangedListener;

    private int compWidth, compHeight;

    private int inputWidth, inputHeight;

    public int getInputCompWidth(){
        return inputWidth;
    }
    public int getInputCompHeight(){
        return inputHeight;
    }

    /**
     * 设置容器的宽度和高度;
     *
     * 在设置后, 我们会根据这个大小来调整 这个类的大小, 从而让画面不变形;
     *
     * @param width 合成容器的宽度
     * @param height 合成容器的高度
     * @param listener 自适应屏幕后的回调
     */
    public void setCompositionSizeAsync(int width, int height, OnCompositionSizeReadyListener listener) {

        inputWidth =width;
        inputHeight =height;


        if (width * height > 1088 * 1920) {
            LSOLog.e("setCompositionSize too bigger divide by 2 :"+ inputWidth+ " x "+ inputHeight);
            inputWidth/=2;
            inputHeight/=2;
        }

        requestLayoutCount = 0;
        compWidth = make16Next(inputWidth);
        compHeight = make16Next(inputHeight);


        sizeChangedListener = listener;
        if (inputWidth != 0 && inputHeight != 0) {
            if (viewWidth == 0 || viewHeight == 0) {  //直接重新布局UI
                textureRenderView.setVideoSize(inputWidth, inputHeight);
                textureRenderView.setVideoSampleAspectRatio(1, 1);
                requestLayoutPreview();
            } else {
                float setRatio = (float) inputWidth / (float) inputHeight;
                float setViewRatio = (float) viewWidth / (float) viewHeight;

                if (setRatio == setViewRatio) { // 如果比例已经相等,不需要再调整,则直接显示.
                    isLayoutOk = true;
                    sendCompositionSizeListener();
                } else if (Math.abs(setRatio - setViewRatio) * 1000 < 16.0f) {
                    if (listener != null) {
                        isLayoutOk = true;
                        sendCompositionSizeListener();
                    }
                } else if (textureRenderView != null) {
                    textureRenderView.setVideoSize(inputWidth, inputHeight);
                    textureRenderView.setVideoSampleAspectRatio(1, 1);
                    sizeChangedListener = listener;
                }
                requestLayoutPreview();
            }
        }
    }

    private void sendCompositionSizeListener() {
        if (sizeChangedListener != null) {
            sizeChangedListener.onSizeReady();
            sizeChangedListener = null;
        }
    }

    private int requestLayoutCount = 0;

    /**
     * 检查得到的大小, 如果和view成比例,则直接回调; 如果不成比例,则重新布局;
     */
    private void checkLayoutSize() {
        float desireRatio = (float) compWidth / (float) compHeight;
        float padRatio = (float) viewWidth / (float) viewHeight;

        if (desireRatio == padRatio) { // 如果比例已经相等,不需要再调整,则直接显示.
            isLayoutOk = true;
            sendCompositionSizeListener();
            if (mViewAvailable != null) {
                mViewAvailable.viewAvailable(this);
            }
        } else if (Math.abs(desireRatio - padRatio) * 1000 < 16.0f) {
            isLayoutOk = true;
            sendCompositionSizeListener();
            if (mViewAvailable != null) {
                mViewAvailable.viewAvailable(this);
            }
        } else {
            textureRenderView.setVideoSize(compWidth, compHeight);
            textureRenderView.setVideoSampleAspectRatio(1, 1);
            LSOLog.d("checkLayoutSize no  right, layout again...");
            requestLayoutPreview();
        }
    }

    private void requestLayoutPreview() {
        requestLayoutCount++;
        if (requestLayoutCount > 3) {
            LSOLog.e("LSOConcatCompositionView layout view error.  return  callback");
            sendCompositionSizeListener();
            if (mViewAvailable != null) {
                mViewAvailable.viewAvailable(this);
            }
        } else {
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


    //-------------------旋转移动缩放------------------------
    public boolean onTextureViewTouchEvent(MotionEvent event) {
        if(renderer!=null){
            renderer.onTextureViewTouchEvent(event);
        }
        return true;
    }


    //--------容器代码----------------------

    /**
     * 增加 拼接图层, 异步增加,增加过程中,内部会处理
     * 增加时, 会暂停当前合成的执行;
     *
     * @param assets   当前资源里数组, 支持图片和视频.
     * @param listener 异步增加好后的回调, 里面的List Layer是当前 List<LSOAsset>对应生成的图层对象
     * @return
     */
    public void addConcatLayerListAsync(List<LSOAsset> assets, OnLanSongSDKAddVideoProgressListener listener) {
        createRender();
        if (renderer != null && setup()) {
            renderer.addConcatLayerListAsync(assets, listener);
        }
    }

    /**
     * 增加 拼接图层, 异步增加,增加过程中,内部会处理
     * 增加时, 会暂停当前合成的执行;
     *
     * @param asset    当前的资源, 支持视频和图片
     * @param listener 异步增加好后的回调, 里面的List Layer是当前 LSOAsset对应生成的图层对象
     * @return
     */
    public void addConcatLayerAsync(LSOAsset asset, OnLanSongSDKAddVideoProgressListener listener) {
        createRender();
        if (renderer != null && setup()) {
            renderer.addConcatLayerListAsync(Arrays.asList(asset), listener);
        }
    }

    /**
     * 插入拼接图层;
     *
     * @param assetArray
     * @param atCompUs
     * @param listener1
     * @return
     */
    public void insertConcatLayerListAsync(List<LSOAsset> assetArray, long atCompUs, OnLanSongSDKAddVideoProgressListener listener1) {
        if (renderer != null) {
            renderer.insertConcatLayerListAsync(assetArray, atCompUs, listener1);
        }
    }
    /**
     * 替换拼接图层;
     *
     * @param asset        资源
     * @param replaceLayer 被替换的图层
     * @param listener     异步替换;
     * @return
     */
    public void replaceConcatLayerListAsync(LSOAsset asset, LSOLayer replaceLayer, OnLanSongSDKAddVideoProgressListener listener) {
        if (renderer != null) {
            renderer.replaceConcatLayerListAsync(asset, replaceLayer, listener);
        }
    }

    /**
     * 根据合成的时间, 得到对应的图层;
     *
     * @param compUs
     * @return
     */
    public LSOLayer getCurrentConcatLayerByTime(long compUs) {
        if (renderer != null) {
            return renderer.getCurrentConcatLayerByTime(compUs);
        } else {
            return null;
        }
    }

//-------------------------------------叠加层------------------------------------

    /**
     * 异步增加多个视频资源;
     * 增加完成后, 会通过 setOnAddVideoProgressListener异步返回结果.
     * 增加时, 会暂停当前合成的执行;
     *
     * @param videoAsset 多个视频资源的数组
     */
    public void addVideoLayerAsync(LSOVideoAsset videoAsset, long atCompUs,
                                   OnLanSongSDKAddVideoProgressListener listener1) {
        createRender();
        if (renderer != null && setup() && videoAsset != null) {
            renderer.addVideoLayerAsync(videoAsset, atCompUs, listener1);
        }
    }


    /**
     * 增加透明的mv动画;
     * @param mvAsset mv资源
     * @param atCompUs 从容器的什么时间开始
     * @param listener1
     */
    public void addMVLayerAsync(LSOMVAsset mvAsset, long atCompUs,
                                OnLanSongSDKAddVideoProgressListener listener1) {
        createRender();
        if (renderer != null && setup() && mvAsset != null) {
            renderer.addMVLayerAsync(mvAsset, atCompUs, listener1);
        }
    }
    /**
     * 增加一张图片, 增加后返回一个图片图层对象;
     *
     * @param bitmap 图片对象;
     * @return 返回图片图层对象;
     */
    public LSOBitmapLayer addBitmapLayer(Bitmap bitmap) {
        createRender();
        if (renderer != null && setup()) {
            LSOBitmapLayer layer = renderer.addBitmapLayer(bitmap, 0);
            return layer;
        } else {
            return null;
        }
    }

    public LSOBitmapLayer addBitmapLayer(LSOBitmapAsset asset) {
        createRender();
        if (renderer != null && setup()) {
            LSOBitmapLayer layer = renderer.addBitmapLayer(asset, 0);
            return layer;
        } else {
            return null;
        }
    }

    /**
     * 增加一张图片
     *
     * @param bitmap   图片对象;
     * @param atCompUs 从合成的什么时间点开始增加, 单位us;
     * @return 返回图片图层对象;
     */
    public LSOBitmapLayer addBitmapLayer(Bitmap bitmap, long atCompUs) {
        createRender();
        if (renderer != null && setup()) {
            LSOBitmapLayer layer = renderer.addBitmapLayer(bitmap, atCompUs);
            return layer;
        } else {
            return null;
        }
    }

    /**
     * 增加图片资源.
     *
     * @param asset
     * @param atCompUs
     * @return
     */
    public LSOBitmapLayer addBitmapLayer(LSOBitmapAsset asset, long atCompUs) {
        createRender();
        if (renderer != null && setup()) {
            LSOBitmapLayer layer = renderer.addBitmapLayer(asset, atCompUs);
            return layer;
        } else {
            return null;
        }
    }

    /**
     * 增加gif图层;
     *
     * @param asset gif图层资源;
     * @return 返回gif图层对象;
     */
    public LSOGifLayer addGifLayer(LSOGifAsset asset) {
        createRender();
        if (asset != null && renderer != null && setup()) {
            LSOGifLayer layer = renderer.addGifLayer(asset, 0);
            return layer;
        } else {
            return null;
        }
    }

    /**
     * 增加gif图层
     *
     * @param asset    gif资源
     * @param atCompUs 从合成的什么时间点开始增加, 单位us;
     * @return
     */
    public LSOGifLayer addGifLayer(LSOGifAsset asset, long atCompUs) {
        createRender();
        if (renderer != null && setup()) {
            LSOGifLayer layer = renderer.addGifLayer(asset, atCompUs);
            return layer;
        } else {
            return null;
        }
    }

    /**
     * 增加一组图片图层(图片的完整路径)
     * 连续图片:必须每张图片的宽度和高度一致;
     * @param list          一组图片的数组
     * @param frameInterval 帧之间的间隔; 多少微秒;
     * @param atCompUs      从合成的什么时间点开始增加, 单位us;
     * @return 返回图片数组图层;
     */
    public LSOBitmapListLayer addBitmapListLayerFromPaths(List<String> list, long frameInterval, long atCompUs) {
        createRender();
        if (renderer != null && setup()) {
            LSOBitmapListLayer bitmapListLayer = renderer.addBitmapListLayerFromPaths(list, frameInterval, atCompUs);
            return bitmapListLayer;
        } else {
            return null;
        }
    }

    /**
     * 增加一组图片图层(bitmap类型)
     * 连续图片:必须每张图片的宽度和高度一致;
     *
     * @param list          一组图片的数组
     * @param frameInterval 一秒钟显示多少帧;
     * @return 返回图片数组图层;
     */
    public LSOBitmapListLayer addBitmapListLayerFromBitmaps(List<Bitmap> list, long frameInterval, long atCompUs) {
        createRender();
        if (renderer != null && setup()) {
            LSOBitmapListLayer bitmapListLayer = renderer.addBitmapListLayerFromBitmaps(list, frameInterval, atCompUs);
            return bitmapListLayer;
        } else {
            return null;
        }
    }


    /**
     * 增加一组有时间点的连续图片数组到容器中
     * 连续图片:必须每张图片的宽度和高度一致;
     * 有时间点的图片数组, 比如第一秒图片A,第三秒图片B, 第4.5秒图片C,则 map中的是
     * maps.put(1*1000*1000,bitmapA); 此时间从0开始, 是整个图片数组开始的起始时间, 默认是0;
     * maps.put(3*1000*1000,bitmapA);
     * maps.put(4*500*1000,bitmapA);
     * @param maps 有时间点的图片数组
     * @param durationUs 这组图片的总时长; 如果等于视频时间的长度,
     * @param atCompUs 从容器的什么时间点开始增加;
     * @return
     */
    public LSOBitmapListLayer addBitmapListLayerFromMap(Map<Long, Bitmap> maps,long durationUs, long atCompUs) {
        createRender();
        if (renderer != null && setup()) {
            LSOBitmapListLayer bitmapListLayer = renderer.addBitmapListLayerFromMaps(maps,durationUs, atCompUs);
            return bitmapListLayer;
        } else {
            return null;
        }
    }


    /**
     * 把语音识别后的时间,增加到容器;
     * 连续图片:必须每张图片的宽度和高度一致;
     * @param maps 识别的结果, 是有时间点的一组文字图片(文字转换后的图片);
     * @param fromLayer 对哪个图层的识别;
     * @return
     */
    public LSOBitmapListLayer addVoiceRecognitionFromLayer(Map<Long, Bitmap> maps,LSOLayer fromLayer) {
        createRender();
        if (renderer != null && setup() && fromLayer!=null) {
            LSOBitmapListLayer bitmapListLayer = renderer.addVoiceRecognitionFromLayer(maps,fromLayer);
            return bitmapListLayer;
        } else {
            return null;
        }
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
        if (renderer != null && setup()) {
            return renderer.addAudioLayer(path, startTimeOfComp);
        } else {
            return null;
        }
    }
    /**
     * 异步删除图层;
     * 删除成功后, 会触发容器时长回调, 在回调中你可以重新布局
     * @param layer
     */
    public void removeLayerAsync(LSOLayer layer) {
        if (renderer != null) {
            renderer.removeLayerAsync(layer);
        }
    }

    /**
     * 删除所有的叠加图层.
     * 叠加图层有:图片图层, gif图层, 图片序列图层等;
     */
    public void removeAllOverlayLayersAsync() {
        if (renderer != null) {
            renderer.removeAllOverlayLayersAsync();
        }
    }

    /**
     * 删除声音图层
     * @param layer
     */
    public void removeAudioLayerAsync(LSOAudioLayer layer) {
        if (renderer != null) {
            renderer.removeAudioLayerAsync(layer);
        }
    }

    /**
     * 删除所有的增加的声音图层;
     */
    public void removeALLAudioLayer() {
        if (renderer != null) {
            renderer.removeALLAudioLayer();
        }
    }

    /**
     * 打印当前拼接的图层时间信息;
     */
    public void printAllConcatLayerTime() {
        if (renderer != null) {
            renderer.printAllConcatLayerTime();
        }
    }

    /**
     * 设置容器的背景颜色;
     *
     * @param color
     */
    public void setBackgroundColor(int color) {
        int red = Color.red(color);  //<---拷贝这里的代码;3行
        int green = Color.green(color);
        int blue = Color.blue(color);

        padBGRed = (float) red / 255f;
        padBGGreen = (float) green / 255f;
        padBGBlur = (float) blue / 255f;
        if (renderer != null) {
            renderer.setBackGroundColor(padBGRed, padBGGreen, padBGBlur, 1.0f);
        }
    }
    /**
     * 把一张图片作为背景;
     *
     * @param bmp
     * @return
     */
    public LSOLayer setBackGroundBitmap(Bitmap bmp) {
        createRender();
        if (renderer != null && setup() && bmp != null) {
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
     *
     * @return
     */
    public LSOLayer setBackGroundBitmapPath(String bmpPath) {
        createRender();
        if (renderer != null && setup() && bmpPath != null) {
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
     *
     * @return
     */
    public boolean isPlaying() {
        return renderer != null && renderer.isPlaying();
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
        return renderer != null && renderer.isRunning();
    }

    /**
     * 是否在导出;
     *
     * @return
     */
    public boolean isExporting() {
        return renderer != null && renderer.isExporting();
    }


    /**
     * 获取当前时间.
     *
     * @return
     */
    public long getCurrentPositionUs() {
        if (renderer != null) {
            return renderer.getCurrentTimeUs();
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
        if (renderer != null) {
            return renderer.getCurrentTimeUs();
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
        if (renderer != null) {
            return renderer.getAllConcatLayers();
        } else {
            LSOLog.e("getConcatLayers  error.  render is null" );
            return null;
        }
    }
    /**
     * @return 图层数组, 叠加的图层;
     */
    public List<LSOLayer> getAllOverLayLayers(){
        if(renderer!=null){
            return renderer.getAllOverLayLayers();
        }else{
            LSOLog.e("getOverLayLayers  error.  render is null" );
            return null;
        }
    }

    /**
     * @return
     */
    public List<LSOAudioLayer> getAllAudioLayers(){
        if(renderer!=null){
            return renderer.getAllAudioLayers();
        }else{
            LSOLog.e("getAllAudioLayers  error.  render is null" );
            return null;
        }
    }

    /**
     * 在每一帧绘制前的回调, 里面没有经过handler, 你增加的代码, 在我们render线程中执行,
     *
     * LSNEW
     * @param listener 返回的是时间戳, 单位us;
     */
    public void setOnLanSongSDKBeforeRenderFrameListener(OnLanSongSDKBeforeRenderFrameListener listener){
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongBeforeRenderFrameListener(listener);
        }
    }

    /**
     * 当一个图层按下, 抬起, 移动, 旋转,缩放的时候, 会调用这里;
     * 移动,缩放, 返回的值, 是相对于容器本身宽高而言,是当前时间点的绝对值;
     * LSNEW
     * @param listener
     */
    public void setOnLanSongSDKLayerTouchEventListener(OnLanSongSDKLayerTouchEventListener listener){
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKLayerTouchEventListener(listener);
        }
    }

    /**
     * 容器的总时长改变监听;
     * @param listener 时长改变监听;
     */
    public void setOnLanSongSDKCompDurationChangedListener(OnLanSongSDKCompDurationChangedListener listener){
        createRender();
        if (renderer != null) {
            renderer.setOnLanSongSDKCompDurationChangedListener(listener);
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
        if (renderer != null) {
            renderer.setOnLanSongSDKPlayProgressListener(listener);
        }
    }

    /**
     * 容器的当前播放时间改变回调;
     * 只要是改变了, 不管是seek的改变,还是自动播放的改变,都执行这里;
     * @param listener
     */
    public void setOnLanSongSDKTimeChangedListener(OnLanSongSDKTimeChangedListener listener) {
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKTimeChangedListener(listener);
        }
    }
    /**
     * 视频播放完成进度;
     * 如果你设置了循环播放,则这里不会回调;
     * 一般视频编辑只播放一次,然后seek到指定位置播放;
     *
     * @param listener
     */
    public void setOnLanSongSDKPlayCompletedListener(OnLanSongSDKPlayCompletedListener listener) {
        createRender();
        if (renderer != null) {
            renderer.setOnLanSongSDKPlayCompletedListener(listener);
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
    public void setOnLanSongSDKExportProgressListener(OnLanSongSDKExportProgressListener listener) {
        createRender();
        if (renderer != null) {
            renderer.setOnLanSongSDKExportProgressListener(listener);
        }
    }

    /**
     * 导出完成回调;
     * 完成后, 有 void onLanSongSDKExportCompleted(String dstVideo);
     * 对应的是:返回完成后的目标视频路径;
     *
     * @param listener
     */
    public void setOnLanSongSDKExportCompletedListener(OnLanSongSDKExportCompletedListener listener) {
        createRender();
        if (renderer != null) {
            renderer.setOnLanSongSDKExportCompletedListener(listener);
        }
    }
    /**
     * 容器中图层有点击事件, 当选中后, 可以移动旋转缩放;
     * 这里监听是哪个图层被点击了, 或全部取消;
     * @param listener
     */
    public void setOnLanSongSDKUserSelectedLayerListener(OnLanSongSDKUserSelectedLayerListener listener){
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKUserSelectedLayerListener(listener);
        }
    }

    /**
     * 当用户通过时间轴选中一个图层后, 可通过这里设置, 让内部有选中状态;
     * @param layer 图层选中;
     */
    public void setSelectLayer(LSOLayer layer){
        if(renderer!=null){
            renderer.setSelectLayer(layer);
        }
    }
    /**
     * 设置所有的图层禁止/启动 touch事件
     * @param is 是否禁止/ 开启
     */
    public void setAllLayerTouchEnable(boolean is){
        if(renderer!=null){
            renderer.setAllLayerTouchEnable(is);
        }
    }

    private OnLanSongSDKErrorListener userErrorListener;
    /**
     * 错误监听
     */
    public void setOnLanSongSDKErrorListener(OnLanSongSDKErrorListener listener) {
        createRender();
        userErrorListener=listener;
    }

    /**
     * 开始预览
     */
    public void startPreview() {
        if (renderer != null) {
            renderer.startPreview(false);
        }
    }

    /**
     * 开始预览, 在播放完第一帧的时候, 是否要暂停;
     *
     * @param pauseAfterFirstFrame
     */
    public void startPreview(boolean pauseAfterFirstFrame) {
        if (renderer != null) {
            renderer.startPreview(pauseAfterFirstFrame);
        }
    }

    /**
     * 开始导出;
     */
    public void startExport() {
        if (renderer != null) {
            renderer.startExport();
        }
    }

    /**
     * 开始导出.
     *
     * 可设置导出分辨率.
     *
     * 如果导出分辨率和容器分辨率不成比例, 则画面会变形;
     *
     * @param width 导出分辨率宽度
     * @param height 导出分辨率 高度;
     */
    public void startExport(int width, int height) {
        if (renderer != null) {
            float ratio = (float) width / (float) height;
            float compRatio = (float) compWidth / (float) compHeight;

            if (Math.abs(ratio - compRatio) > 0.01f) {
                LSOLog.e("导出分辨率和容器的分辨率不成比例, 可能导出的会变形, 请注意!!");
            }
            int width2=width;
            int height2=height;

            if(width2*height2<320*320){
                width2=270;
                height2=480;
                LSOLog.e("设置错误, 这里错误的认为导出为竖形");
            }

            if(width%16!=0 || height %16 !=0) {
                width2 = make16Multi(width);
                height2 =make16Multi(height);
            }
            renderer.startExport(width2, height2);
        }
    }
    public static int make16Multi(int value) {
        if (value < 16) {
            return value;
        } else {
            value += 8;
            int val2 = value / 16;
            val2 *= 16;
            return val2;
        }
    }

    /**
     * 定位到某个位置.
     *
     * @param timeUs time时间
     */
    public void seekToTimeUs(long timeUs) {
        if (renderer != null) {
            renderer.seekToTimeUs(timeUs);
        }
    }


    /**
     * 暂停
     */
    public void pause() {
        if (renderer != null) {
            renderer.pause();
        }
    }

    /**
     * 暂停播放后的恢复;
     */
    public void resume() {
        if (renderer != null) {
            renderer.startPreview(false);
        }


    }

    /**
     * 获取当前总的合成时间
     * 也是所有拼接图层的时间;
     *
     * @return
     */
    public long getDurationUs() {
        if (renderer != null) {
            return renderer.getDurationUs();
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
        if (renderer != null) {
            renderer.setLooping(is);
        }
    }

    /**
     * 画面在重新布局后, 是否布局好.
     *
     * @return
     */
    public boolean isLayoutValid() {
        return isLayoutOk;
    }

    /**
     * 取消当前合成.
     * 取消后, 会把内部线程全部退出, 所有增加的图层都会释放;
     */
    public void cancel() {
        if (renderer != null) {
            renderer.cancel();
            renderer = null;
        }
    }

    /**
     * 设置帧率
     * [不建议使用]
     *
     * @param frameRate
     */
    public void setFrameRate(int frameRate) {
        if (renderer != null) {
            renderer.setFrameRate(frameRate);
        }
    }

    /**
     * 设置编码时的码率;
     * [不建议使用]
     *
     * @param bitRate
     */
    public void setEncoderBitRate(int bitRate) {
        if (renderer != null) {
            renderer.setEncoderBitRate(bitRate);
        }
    }

    //内部使用;
    private void createRender() {
        if (renderer == null) {
            setUpSuccess = false;
            renderer = new LSOConcatCompositionRender(getContext());
            renderer.setBackGroundColor(padBGRed, padBGGreen, padBGBlur, padBGAlpha);
            renderer.setOnLanSongSDKErrorListener(new OnLanSongSDKErrorListener() {
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
        if (renderer != null && !setUpSuccess) {
            if (!renderer.isRunning() && mSurfaceTexture != null) {
                renderer.updateCompositionSize(compWidth, compHeight);
                renderer.setSurface(new Surface(mSurfaceTexture), viewWidth, viewHeight);
                setUpSuccess = renderer.setup();
                LSOLog.d("LSOConcatCompositionView setup.ret:" + setUpSuccess +
                        " comp size:" + compWidth + " x " + compHeight + " view size :" + viewWidth + " x " + viewHeight);
                return setUpSuccess;
            } else {
                return renderer.isRunning();
            }
        } else {
            return setUpSuccess;
        }
    }

    /**
     * 释放
     * 如果已经收到完成监听, 则不需要调用;
     */
    public void release() {
        if (renderer != null) {
            renderer.release();
            renderer = null;
        }
        setUpSuccess = false;
    }

    public static int make16Next(int value) {
        if (value % 16 == 0) {
            return value;
        } else {
            return ((int) ((float) value / 16.0f + 1.0f) * 16);
        }
    }
}
