package com.lansosdk.videoeditor;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import com.lansosdk.LanSongAe.LSOAeDrawable;
import com.lansosdk.LanSongAe.LSOAeImage;
import com.lansosdk.LanSongAe.LSOAeImageLayer;
import com.lansosdk.LanSongAe.LSOAeText;
import com.lansosdk.LanSongFilter.LanSongFilter;
import com.lansosdk.box.AECompositionRunnable;
import com.lansosdk.box.AEJsonLayer;
import com.lansosdk.box.AEMVLayer;
import com.lansosdk.box.AESegmentLayer;
import com.lansosdk.box.AEVideoLayer;
import com.lansosdk.box.AudioLayer;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.LSOAudioAsset;
import com.lansosdk.box.LSOLayerPosition;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKExportProgressListener;
import com.lansosdk.box.OnLanSongSDKPreviewBufferingListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.OnLanSongSDKRenderProgressListener;
import com.lansosdk.box.OnLanSongSDKThumbnailListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Ae的预览和导出合成类.
 *
 *
 * 如果你不需要预览,则可以用AECompositionExecute直接导出;
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
                isLayoutOk=true;
                sizeChangedListener.onSizeChanged(drawPadWidth,drawPadHeight);
            }else if(mViewAvailable!=null){
                mViewAvailable.viewAvailable(this);
            }
        }else{
            requestLayout();
        }
    }
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
                        cb.onSizeChanged(width, height);
                    }
                    isLayoutOk=true;
                } else if (Math.abs(setAcpect - setViewacpect) * 1000 < 16.0f) {
                    if (cb != null) {
                        cb.onSizeChanged(width, height);
                    }
                    isLayoutOk=true;
                } else if (textureRenderView != null) {
                    textureRenderView.setVideoSize(width, height);
                    textureRenderView.setVideoSampleAspectRatio(1, 1);
                    sizeChangedListener = cb;
                }
                requestLayoutPreview();
            }
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
   //------------------------------------------Ae的代码-----------------------------------------------------------
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
    public void addBitmap3DLayer(LSOAeDrawable drawables){
        createRender();
//        if(renderer !=null && !renderer.isRunning() && drawables!=null && drawables.size()>0) {
//            AESegmentLayer layer= renderer.addAeLayer(drawables);
//            if(layer==null){
//                LSOLog.e("AECompositionView addSecondLayer error.");
//            }
//            secondLayerAdd= layer!=null;
//            return layer;
//        }else{
//            return null;
//        }
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
     * 设置声音的音量.
     * @param volume 音量. 1.0原声;0.0静音;2.0是放大两倍;
     */
    public void setPreviewVolume(float volume){
        if(renderer!=null){
            renderer.setPlayerVolume(volume);
        }
    }

    /**
     * 设置所有图层的声音音量;
     *
     * 同setPreviewVolume 设置声音音量,
     * 在预览和最后生成的声音都变化;
     * @param volume
     */
    public void setAudioVolume(float volume){
        setPreviewVolume(volume);
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
     * 增加一个图片图片logo
     * @param bmp
     * @param position
     * @return
     */
    public BitmapLayer addLogoLayer(Bitmap bmp, LSOLayerPosition position){
        createRender();
        if(renderer !=null && bmp!=null){
            return  renderer.addLogoLayer(bmp,position);
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

    @Deprecated
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
    public AudioLayer addAudioLayer(LSOAudioAsset audioAsset, boolean loop) {
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
     * @param srcPath 声音路径文件, 声音支持mp3, wav, mp4,m4a
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
     * 在AE线程开始前 和 所有图层增加后 调用;
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
     * @param startAudioTimeUs 裁剪声音的开始时间
     * @param endAudioTimeUs   裁剪声音的结束时间;
     * @return
     */
    public AudioLayer addAudioLayer(String srcPath, long startFromPadUs, long startAudioTimeUs, long endAudioTimeUs) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer=renderer.addAudioLayer(srcPath, startFromPadUs,
                    startAudioTimeUs, endAudioTimeUs);
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

    /**
     * 我们的容器本质是是一个线程,这个是获取当前容器线程是否在运行;
     * 线程运行不一定正在播放;
     * @return
     */
    public boolean isRunning(){
        return renderer!=null && renderer.isRunning();
    }
    /**
     * 预览播放进度监听
     * @param listener 此监听有两个参数,分别是long ptsUs(当前渲染的时间戳) + int percent, 当前的百分比;
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
     * 我们的渲染是另外一个线程执行的,你可以用此监听得到当前渲染的进度;
     * 渲染进度和播放进度的区别是: 先渲染好一帧,然后才可以播放;类似网络播放器有两个进度条一样;
     *
     * @param listener 当前渲染进度; 此监听有两个参数,分别是long ptsUs(当前渲染的时间戳) + int percent, 当前的百分比;
     */
    public void setOnLanSongSDKRenderProgressListener(OnLanSongSDKRenderProgressListener listener){
        createRender();
        if(renderer!=null){
            renderer.setOnLanSongSDKRenderProgressListener(listener);
        }
    }

    /**
     * 异步获取Ae模板合成后的视频 的缩略图.
     * 注意:请不要在这里做过多的图片处理操作, 以免造成线程卡顿, 应该放到一个list中;
     * @param frameCnt 要获取的张数 建议不要超过30张;
     * @param listener 缩略图监听 返回的是bitmap类型的数据;
     */
    public void getThumbnailBitmapsAsynchronously(int frameCnt, OnLanSongSDKThumbnailListener listener){
        createRender();
        //判断最大是50张;
        if(renderer!=null && frameCnt>0 && frameCnt<=50 && listener!=null){
            renderer.getThumbnailBitmapsAsynchronously(frameCnt,listener);
        }
    }

    /**
     * 异步获取Ae模板合成后的视频 的缩略图.
     * @param listener
     */
    public void getThumbnailBitmapsAsynchronously(OnLanSongSDKThumbnailListener listener){
        createRender();
        if(renderer!=null){
            renderer.getThumbnailBitmapsAsynchronously(10,listener);
        }
    }


    //---------------------------------------------------------------------------------------------
    /**
     * 布局是否好.
     * @return
     */
    public boolean isLayoutValid(){
        return isLayoutOk;
    }

    /**
     * 当前是否在播放;
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
     * 默认是 禁止.
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
     * 直接播放预览.
     * 不暂停;
     * @return
     */
    public  boolean startPreview(){
        return startPreview(false);
    }
    /**
     * 开始运行
     * @param pauseAfterFirstFrame 显示第一帧后, 是否要暂停;
     * @return 开启返回true; 失败返回false
     */
    public  boolean startPreview(boolean pauseAfterFirstFrame){
        if(!LanSoEditor.isLoadLanSongSDK.get()){
            LSOLog.e("没有加载SDK, 或你的APP崩溃后,重新启动当前Activity,请查看完整的logcat:" +
                    "(No SDK is loaded, or the current activity is restarted after your app crashes, please see the full logcat)");
            return false;
        }

        if(renderer!=null && mSurfaceTexture!=null && !renderer.isRunning()){

            renderer.updateDrawPadSize(drawPadWidth,drawPadHeight);
            renderer.setSurface(new Surface(mSurfaceTexture));
            isStarted= renderer.startPreview(pauseAfterFirstFrame);
            LSOLog.i("AEComposition startPreview.ret:"+isStarted);
            secondLayerAdd=!isStarted;
            return isStarted;
        }else{
            boolean ret=renderer!=null && renderer.isRunning();
            if(ret){
                LSOLog.w("AeCompositionView  is running...");
            }
            return ret;
        }
    }
    /**
     * 开始导出Ae模板.
     * 导出后, 会把生成的视频完整路径返回给你
     * @return 已经开始,返回true, 无法导出返回false
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
     * 注意: 不能用在Activity的onResume中,
     * 因为onPause时会销毁TextureView,从而整个OpenGL语境就没有了.
     * 使用请参考demo;
     */
    public void resumePreview(){
        if(renderer!=null){
            renderer.resumePreview();
        }
    }

    /**
     * 设置为seek模式, 用在SeekBar的场合
     * 在开始seek的时候,设置为true,当seekbar结束的时候, 设置为false;
     * @param is
     */
    public void setSeekMode(boolean is){
        if(renderer!=null){
            renderer.setSeekMode(is);
        }
    }

    /**
     * 定位到某一点, 在定位的时候, 默认是不播放声音, 暂停状态;
     * @param us
     */
    public void seekToTimeUs(long us){
        if(renderer!=null){
            renderer.seekToTimeUs(us);
        }
    }

    /**
     * 获取Ae模板的总时长,
     * @return
     */
    public long getAeDurationUs(){
        long  ret=0;
        if(renderer!=null && renderer.isRunning()){
            ret=renderer.getAeDurationUs();
        }
        if(ret==0){
            ret=100*1000;
        }
        return ret;
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
    private void createRender(){
        if(renderer==null){
            renderer =new AECompositionRunnable(getContext());
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
