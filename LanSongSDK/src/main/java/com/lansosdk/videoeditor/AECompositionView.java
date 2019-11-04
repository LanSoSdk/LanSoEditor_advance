package com.lansosdk.videoeditor;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import com.lansosdk.LanSongAe.LSOAeDrawable;
import com.lansosdk.LanSongAe.LSOAeImage;
import com.lansosdk.LanSongAe.LSOAeImageLayer;
import com.lansosdk.LanSongAe.LSOAeText;
import com.lansosdk.LanSongAe.LSOLoadAeJsons;
import com.lansosdk.LanSongFilter.LanSongFilter;
import com.lansosdk.box.AECompositionRunnable;
import com.lansosdk.box.AEJsonLayer;
import com.lansosdk.box.AEMVLayer;
import com.lansosdk.box.AESegmentLayer;
import com.lansosdk.box.AEVideoLayer;
import com.lansosdk.box.AudioLayer;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.LSOAudioAsset;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKExportProgressListener;
import com.lansosdk.box.OnLanSongSDKPreviewBufferingListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Ae的预览合成类.
 *
 */
public class AECompositionView extends FrameLayout {

    private AECompositionRunnable renderer;



    private TextureRenderView textureRenderView;
    private SurfaceTexture mSurfaceTexture = null;
    private int drawPadWidth, drawPadHeight;
    private onViewAvailable mViewAvailable = null;
    private boolean isLayoutOk=false;
    private boolean secondLayerAdd=false;
    // ----------------------------------------------
    public AECompositionView(Context context) {
        super(context);
        initVideoView(context);
    }

    public AECompositionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView(context);
    }

    public AECompositionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AECompositionView(Context context, AttributeSet attrs, int defStyleAttr,
                             int defStyleRes) {
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
        void viewAvailable(AECompositionView v);
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
     * 设置容器的大小, 在设置后, 我们会根据这个大小来调整DrawPadAEPriview这个类的大小.
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
        float setAcpect = (float) desireWidth / (float) desireHeight;
        float setViewacpect = (float) drawPadWidth / (float) drawPadHeight;

        if (setAcpect == setViewacpect) { // 如果比例已经相等,不需要再调整,则直接显示.
            isLayoutOk=true;
            if (sizeChangedListener != null) {
                sizeChangedListener.onSizeChanged(drawPadWidth, drawPadHeight);
                sizeChangedListener =null;
            }else if(mViewAvailable!=null){
                mViewAvailable.viewAvailable(this);
            }
        } else if (Math.abs(setAcpect - setViewacpect) * 1000 < 16.0f) {
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
   //-------------------------------增加Ae的代码....





    /**
     * 增加第1层
     * 视频图层; 没有则设置为nil
     */
    public AEVideoLayer addFirstLayer(String videoPath) throws IOException {
        createRender();
        if(renderer !=null && !renderer.isRunning() && LanSongFileUtil.fileExist(videoPath)){
            return renderer.addVideoLayer(videoPath);
        }else{
            return null;
        }
    }
    /**
     * 增加第2层
     * Ae json图层;
     */
    public AEJsonLayer addSecondLayer(LSOAeDrawable drawable){
        createRender();
        if(secondLayerAdd){
            LSOLog.e("已经增加第二层(AE图层). 请确认你的增加顺序.");
            return null;
        }
        if(renderer !=null && !renderer.isRunning()) {
            AEJsonLayer layer= renderer.addAeLayer(drawable);
            if(layer==null){
                LSOLog.e("AECompositionView addSecondLayer error.");
            }
            secondLayerAdd= layer!=null;
            return layer;
        }else{
            return null;
        }
    }


    /**
     *
     * 增加第2层
     * Ae json图层;
     * @param drawable AE json对象
     * @param startIndex json中的开始帧:(比如从第5帧开始, 则这里填5)
     * @param endIndex  json中的结束帧;
     * @return
     * @throws Exception
     */
    public AEJsonLayer addSecondLayer(LSOAeDrawable  drawable, int startIndex, int endIndex) {
        createRender();
        if(secondLayerAdd){
            LSOLog.e("已经增加第二层(AE图层). 请确认你的增加顺序.");
            return null;
        }
        drawable.setCutFrame(startIndex,endIndex);
        AEJsonLayer layer= renderer.addAeLayer(drawable);
        if(layer==null){
            LSOLog.e("AECompositionView addSecondLayer error.");
        }
        secondLayerAdd= layer!=null;
        return layer;
    }

    /**
     * 增加第2层
     * Ae json图层;
     * [重载方法.]
     */
    public AESegmentLayer addSecondLayer(List<LSOAeDrawable> drawables){
        createRender();
        if(secondLayerAdd){
            LSOLog.e("已经增加第二层(AE图层). 请确认你的增加顺序.");
            return null;
        }
        if(renderer !=null && !renderer.isRunning() && drawables!=null && drawables.size()>0) {
            AESegmentLayer layer= renderer.addAeLayer(drawables);
            if(layer==null){
                LSOLog.e("AECompositionView addSecondLayer error.");
            }
            secondLayerAdd= layer!=null;
            return layer;
        }else{
            return null;
        }
    }


    /**
     * 增加第3层
     * mv图层
     * [没有则不调用]
     */
    public AEMVLayer addThirdLayer(String colorPath, String maskPath){

        createRender();
        if(renderer !=null && !renderer.isRunning()&& colorPath!=null && maskPath!=null) {
            AEMVLayer layer= renderer.addMVLayer(colorPath,maskPath);
            if(layer==null){
                LSOLog.e("AECompositionView addThirdLayer MV Video error.");
            }
            return layer;
        }else{
            return null;
        }
    }

    /**
     * 增加第4层
     * Ae json图层;
     * [没有则不调用]
     * @return
     */
    public AEJsonLayer addForthLayer(LSOAeDrawable drawable){
        createRender();
        if(renderer !=null && !renderer.isRunning()) {
            AEJsonLayer layer= renderer.addAeLayer(drawable);
            if(layer==null){
                LSOLog.e("AECompositionView addForthLayer  Ae Json error.");
            }
            return layer;
        }else{
            return null;
        }
    }



    /**
     * 增加第5层
     * 增加mv图层
     * [没有则不调用]
     * @return
     */
    public AEMVLayer addFifthLayer(String colorPath, String maskPath){

        createRender();
        if(renderer !=null && !renderer.isRunning() && colorPath!=null && maskPath!=null) {
            AEMVLayer layer= renderer.addMVLayer(colorPath,maskPath);
            if(layer==null){
                LSOLog.e("AECompositionView addFifthLayer MV Video error.");
            }
            return layer;
        }else{
            return null;
        }
    }

    /**
     * 在增加其他音频后,获取Ae模板中的声音.
     * 没有增加其他声音,则无效;
     * @return
     */
    public AudioLayer getAEAudioLayer(){
        if(renderer!=null){
            return renderer.getAEAudioLayer();
        }else {
            return null;
        }
    }


    private void createRender(){
        if(renderer==null){
            renderer =new AECompositionRunnable(getContext());
        }
        isStarted=false;
    }

    /**
     * 在预览的时候, 是否设置循环.
     * [导出时无效]
     * @param is 循环
     */
    public void setPreviewLooping(boolean is){
        createRender();
        if(renderer !=null) {
           renderer.setPreviewLooping(is);
        }
    }

    /**
     * 在预览的时候,设置播放器的音量.
     * [导出时无效]
     * @param volume 音量. 1.0原声;0.0静音;2.0是放大两倍;
     */
    public void setPreviewVolume(float volume){
        if(renderer!=null){
            renderer.setPlayerVolume(volume);
        }
    }

    /**
     * 裁剪时长;
     * 裁剪模板的时长.
     * 在视频增加后, 音频图层增加前调用;
     * @param durationUS
     */
    public void setCutDurationUS(long durationUS){
        if(renderer!=null){
            renderer.setCutDurationUS(durationUS);
        }
    }

    /**
     *给整个AE模板增加/切换滤镜;
     *  [当前仅预览有作用,不建议使用]
     * @param filter 单个滤镜, 如果增加滤镜后, 需要删除则设置为null即可.
     */
    public void switchFilterTo(LanSongFilter filter) {
        createRender();
        if(renderer!=null){
            renderer.switchFilterTo(filter);
        }
    }

    /**
     * 给整个AE模板增加/切换滤镜;
     *  [当前仅预览有作用,不建议使用]
     * @param filters 多个滤镜, 如果增加后, 需要删除则设置为null即可.
     */
    public void switchFilterList(List<LanSongFilter> filters) {
        createRender();
        if(renderer!=null){
            renderer.switchFilterList(filters);
        }
    }
    /**
     * 增加图片图层
     * 在start前调用
     * @param bmp 图片对象, 此图片在增加后, 不会释放;
     * @return 返回图片图层对象
     */
    public BitmapLayer addBitmapLayer(Bitmap bmp){
        createRender();
        if(renderer !=null && bmp!=null){
            return  renderer.addBitmapLayer(bmp);
        }else {
            LSOLog.e("AECompositionView 增加图片图层失败...");
            return  null;
        }
    }

    /**
     * 增加图片序列,
     * 一般用在动态的透明logo
     * @param bmpList 图片序列
     * @param intervalUs 序列中图片显示间隔,  单位是微秒. 一般建议是40*1000
     * @param loop  是否循环显示, 如果不循环,则停留在最后一帧; 如果您不想循环,也不要最后一帧,则可以设计最后一帧是完全透明的图片;
     * @return 返回图片图层对象
     */
    public BitmapLayer addBitmapLayer(ArrayList<Bitmap> bmpList, long intervalUs,boolean loop) {
        createRender();
        if(renderer !=null && bmpList!=null){
            return  renderer.addBitmapLayer(bmpList,intervalUs,loop);
        }else {
            LSOLog.e("AECompositionView 增加图片图层失败...");
            return  null;
        }
    }
    /**
     * 有些Ae模板是视频的, 则声音会单独导出为mp3格式, 从这里增加;
     * @param audioPath
     */
    public void addAeModuleAudio(String audioPath){
        if (renderer != null && !renderer.isRunning()) {
            renderer.addAeModuleAudio(audioPath);
        }
    }
    /**
     * 有些Ae模板是视频的, 则声音会单独导出为mp3格式, 从这里增加;
     * @param audioPath
     */
    public void addAeModuleAudio(LSOAudioAsset audioPath){
        if (renderer != null && !renderer.isRunning()) {
            renderer.addAeModuleAudio(audioPath);
        }
    }
    /**
     * 增加声音图层;
     * @param audioAsset 声音文件对象;
     * @return
     */
    public AudioLayer addAudioLayer(LSOAudioAsset audioAsset) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer= renderer.addAudioLayer(audioAsset);
            if(layer==null){
                LSOLog.e("AECompositionView addAudioLayer error. path:"+audioAsset);
            }
            return layer;
        } else {
            return null;
        }
    }

    /**
     * 增加音频图层.
     * @param audioAsset 音频资源
     * @param loop 是否循环;
     * @return
     */
    public AudioLayer addAudioLayer(LSOAudioAsset audioAsset,boolean loop) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer= renderer.addAudioLayer(audioAsset);
            if(layer==null){
                LSOLog.e("AECompositionView addAudioLayer error. path:"+audioAsset);
            }else{
                layer.setLooping(loop);
            }
            return layer;
        } else {
            return null;
        }
    }

    /**
     * 增加音频,
     *
     * 在AE线程开始前 + 所有图层增加后 调用;
     *
     * 音频采样率必须和视频的声音采样率一致
     * @param srcPath
     * @return
     */
    public AudioLayer addAudioLayer(String srcPath) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer= renderer.addAudioLayer(srcPath);
            if(layer==null){
                LSOLog.e("AECompositionView addAudioLayer error. path:"+srcPath);
            }
            return layer;
        } else {
            return null;
        }
    }

    /**
     * 增加音频图层,并是否循环
     * 在AE线程开始前 + 所有图层增加后 调用;
     * @return
     */
    public AudioLayer addAudioLayer(String srcPath, boolean loop) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer= renderer.addAudioLayer(srcPath);
            if(layer!=null){
                layer.setLooping(loop);
            }else{
                LSOLog.e("AECompositionView addAudioLayer error. path:"+srcPath);
            }
            return layer;
        } else {
            return null;
        }
    }
    /**
     * 增加音频图层,;
     *
     * 在AE线程开始前 + 所有图层增加后 调用;
     *
     * 音频采样率必须和视频的声音采样率一致
     * @param srcPath
     * @param startFromPadTime 从Ae模板的什么时间开始增加
     * @return  返回增加后音频层, 可以用来设置音量,快慢,变声等.
     */
    public AudioLayer addAudioLayer(String srcPath, long startFromPadTime) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer=  renderer.addAudioLayer(srcPath, 0,startFromPadTime, -1);
            if(layer==null){
                LSOLog.e("AECompositionView addAudioLayer error. path:"+srcPath);
            }
            return layer;
        } else {
            return null;
        }
    }

    /**
     * 增加其他声音;
     *
     *在AE线程开始前 和 所有图层增加后 调用;
     *
     *
     * 音频采样率必须和视频的声音采样率一致
     *
     * @param srcPath        路径, 可以是mp3或m4a或 带有音频的MP4文件;
     * @param startFromPadUs 从主音频的什么时间开始增加
     * @param durationUs     把这段声音多长插入进去.
     * @return 返回一个AudioLayer对象;
     */
    public AudioLayer addAudioLayer(String srcPath, long startFromPadUs,
                                    long durationUs) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer=  renderer.addAudioLayer(srcPath,0, startFromPadUs, durationUs);
            if(layer==null){
                LSOLog.e("AECompositionView addAudioLayer error. path:"+srcPath);
            }
            return layer;
        } else {
            return null;
        }
    }

    /**
     * 如果要调节音量, 则增加拿到对象后, 开始调节.
     *
     * 在AE线程开始前 + 所有图层增加后 调用;
     *
     * @param srcPath
     * @param startFromPadUs   从容器的什么位置开始增加
     * @param startAudioTimeUs 把当前声音的开始时间增加进去.
     * @param durationUs       增加多少, 时长.
     * @return
     */
    public AudioLayer addAudioLayer(String srcPath, long startFromPadUs,
                                    long startAudioTimeUs, long durationUs) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer=renderer.addAudioLayer(srcPath, startFromPadUs,
                    startAudioTimeUs, durationUs);
            if(layer==null){
                LSOLog.e("AECompositionView addAudioLayer error. path:"+srcPath);
            }
            return layer;
        } else {
            return null;
        }
    }

    /**
     * 是否运行在导出阶段;
     * @return
     */
    public boolean isExportRunning(){
        return  renderer!=null && renderer.isExportMode();
    }
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

    /**
     * 后台导出进度;
     * @param listener
     */
    public void setOnLanSongSDKExportProgressListener(OnLanSongSDKExportProgressListener listener) {
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKExportProgressListener(listener);
        }
    }

    /**
     * 完成回调.
     * 两种情况:
     * 1. 预览没有循环,退出了
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

    /**
     * 当解码很慢的时候, 有缓冲.
     * listener返回的是boolean类型, true表示正在缓冲.
     * @param listener
     */
    public void setOnLanSongSDKPreviewBufferingListener(OnLanSongSDKPreviewBufferingListener listener)
    {
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKPreviewBufferingListener(listener);
        }
    }
    /**
     * 布局是否好.
     * @return
     */
    public boolean isLayoutValid(){
        return isLayoutOk;
    }

    /**
     * 当前界整个
     * @return
     */
    public boolean isPlaying(){
        return renderer!=null && renderer.isPlaying();
    }
    private int lastPercent=0; //上一个百分比;
    private boolean isStarted;

    /**
     * 设置在导出时 码率
     * [不建议使用]
     * @param bitrate 码率,最低1*1024*1024;
     */
    public void setExportBitrate(int bitrate){
        createRender();
        if(renderer!=null){
            renderer.setExportBitrate(bitrate);
        }
    }


    /**
     *  当前是否在缓冲中..
     * @return
     */
    public boolean isBuffering(){
        return renderer!=null && renderer.isBuffering();
    }

    /**
     *  是否禁止在预览过程中,  禁止缓冲.
     * 如果模板很复杂, 渲染很耗时, 默认是返回缓冲监听;
     * 默认是 不禁止.
     * 如果禁止,当模板复杂,耗时严重时,会导致音视频不同步;
     * @param disable
     */
    public void setDisablePreviewBuffering(boolean disable){
        createRender();
        if(renderer!=null){
            renderer.setDisablePreviewBuffering(disable);
        }
    }

    /**
     * 开始运行
     * @return 开启返回true; 失败返回false
     */
    public  boolean startPreview(){

        if(renderer!=null && mSurfaceTexture!=null && !renderer.isRunning()){

            renderer.updateDrawPadSize(drawPadWidth,drawPadHeight);
            renderer.setSurface(new Surface(mSurfaceTexture));
            isStarted= renderer.startPreview();


            LSOLog.i("AEComposition startPreview.ret:"+isStarted);
            secondLayerAdd=!isStarted;
            return isStarted;
        }else{
            if(renderer!=null && !renderer.isRunning()){
                LSOLog.e("开启AePreview 失败.mSurfaceTexture 无效 :");
            }
            return  false;
        }
    }
    /**
     * 开始导出Ae模板.
     * 导出后, 会以视频的形式返回给你
     * 你可以不预览,add好各种图层后, 直接调用此方法.
     */
    public boolean startExport(){
        if(renderer !=null) {
            return renderer.startExport();
        }else{
            return false;
        }
    }
    /**
     * 暂停预览画面.
     * 注意: 不能用在Activity的 onPause中,因为onPause会销毁 TextureView,从而整个OpenGL语境就没有了.
     * 使用请参考demo;
     */
    public void pausePreview(){
        if(renderer!=null){
            renderer.pausePreview();
        }
    }

    /**
     * 暂停后的恢复预览画面.
     *
     * 注意: 不能用在Activity的onResume中,因为onPause时会销毁TextureView,从而整个OpenGL语境就没有了.
     * 使用请参考demo;
     */
    public void resumePreview(){
        if(renderer!=null){
            renderer.resumePreview();
        }
    }


    /**
     *
     * 取消内部的整个线程的执行, 并退出.
     * 取消后, 所有的缓存和图层都会被销毁, 如果您想再次开启, 则需要重新增加各种图层然后开启.
     *
     * [阻塞进行,一直等待到整个线程退出才返回.如果您下一次开启时间大于1s,则可以异步取消]
     */
    public void cancel(){
        if(renderer!=null){
            renderer.cancel();
            renderer=null;
        }
        isStarted=false;
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
        isStarted=false;
    }


    /**
     * 打印json中的所有的图片和文字信息;
     * @param drawable
     */
    public static void  printDrawableInfo(LSOAeDrawable drawable){

        if(drawable!=null){

            LSOLog.i("********************Json info**********************************");
            LSOLog.i("* 图片信息: ");
            Map<String, LSOAeImage> maps = drawable.getJsonImages();
            for (String key : maps.keySet()) {
                LSOAeImage asset = maps.get(key);
                LSOLog.i("* 图片名字: " + asset.getFileName() + " ID号:" + asset.getId() + " width:" + asset.getWidth() +"height:" + asset.getHeight());
            }
            LSOLog.i("* ");

            //打印json中的所有图片图层
            LSOLog.i("* 图片图层(ImageLayer):");
            ArrayList<LSOAeImageLayer> imageList=drawable.getAllAeImageLayer();
            for (LSOAeImageLayer lsoAeImageLayer : imageList){
                LSOLog.i("* No."+lsoAeImageLayer.layerIndexInComposition + " 图层名字:"+ lsoAeImageLayer.layerName+" ID:" +lsoAeImageLayer.imgId+" 时长(us):"
                        + lsoAeImageLayer.durationUs+" 宽高:"+ lsoAeImageLayer.width+ " x "+ lsoAeImageLayer.height+ "" +
                        "开始帧:"+lsoAeImageLayer.startFrame+ " 结束帧:"+ lsoAeImageLayer.endFrame);
            }
            LSOLog.i("* ");
            LSOLog.i("* 文字信息(text):");


            //打印json中的所有文字
            List<LSOAeText> texts = drawable.getJsonTexts();
            for (LSOAeText text : texts) {
                LSOLog.i("* "+ text.text+ "composition:"+text.layerIndexInComposition+ "layerName:"+ text.layerName);
            }

            LSOLog.i("* ");
            LSOLog.i("*************************************************************");
        }
    }
}
