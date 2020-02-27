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

import com.lansosdk.box.AudioPreviewLayer;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.CanvasLayer;
import com.lansosdk.box.DrawPadConcatViewRender;
import com.lansosdk.box.GifLayer;
import com.lansosdk.box.LSOAudioAsset;
import com.lansosdk.box.LSOBitmapAsset;
import com.lansosdk.box.LSOGifAsset;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LSOMVAsset;
import com.lansosdk.box.LSOVideoAsset;
import com.lansosdk.box.LSOVideoOption2;
import com.lansosdk.box.Layer;
import com.lansosdk.box.MVCacheLayer;
import com.lansosdk.box.MVLayer;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.VideoConcatLayer;
import com.lansosdk.box.onDrawPadSizeChangedListener;


/**
 * 多个图层拼接 预览类
 * 在拼接的过程中, 可以增加动画, 即实现转场, 拼接默认在图层的最后一秒的下方叠加另一个图层; 两个图层有重叠的一秒时间;
 * 2019年9--10.18
 */
public class DrawPadConcatView extends FrameLayout {

    /**
     * 渲染类;
     */
    private DrawPadConcatViewRender renderer;


    private TextureRenderView textureRenderView;
    private SurfaceTexture mSurfaceTexture = null;
    private int drawPadWidth, drawPadHeight;
    private onViewAvailable mViewAvailable = null;
    private boolean isLayoutOk=false;
    // ----------------------------------------------
    public DrawPadConcatView(Context context) {
        super(context);
        initVideoView(context);
    }

    public DrawPadConcatView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView(context);
    }

    public DrawPadConcatView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DrawPadConcatView(Context context, AttributeSet attrs, int defStyleAttr,int defStyleRes) {
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
        void viewAvailable(DrawPadConcatView v);
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
            renderer =new DrawPadConcatViewRender(getContext());
            renderer.setDrawPadBackGroundColor(padBGRed,padBGGreen,padBGBlur,padBGAlpha);
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
    public void setBackgroundColor(int color) {
        int red = Color.red(color);  //<---拷贝这里的代码;3行
        int green = Color.green(color);
        int blue = Color.blue(color);

        padBGRed=(float)red/255f;
        padBGGreen=(float)green/255f;
        padBGBlur=(float)blue/255f;
        if(renderer!=null){
            renderer.setDrawPadBackGroundColor(padBGRed,padBGGreen,padBGBlur,1.0f);
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
            renderer.setDrawPadBackGroundColor(r,g,b,a);
        }
    }

    private boolean setupSuccess;



    public void setDurationUs(long durationUs){
        createRender();
        if (renderer != null && !isRunning()) {
            renderer.setDrawPadDurationUs(durationUs);
        }
    }
    public BitmapLayer addBitmapLayer(LSOBitmapAsset asset) {
        createRender();
        if (renderer != null && setup()) {
            return renderer.addBitmapLayer(asset,0,Long.MAX_VALUE);
        }else{
            return null;
        }
    }
    public BitmapLayer addBitmapLayer(LSOBitmapAsset asset, long startTimeUs, long endTimeUs) {
        createRender();
        if (renderer != null && setup()) {
            return renderer.addBitmapLayer(asset,startTimeUs,endTimeUs);
        }else{
            return null;
        }
    }
    public CanvasLayer addCanvasLayer() {
        createRender();
        if (renderer != null && setup()) {
            return renderer.addCanvasLayer();
        }else{
            return null;
        }
    }
    @Deprecated
    public GifLayer addGifLayer(String gifPath, long startTimeUs, long endTimeUs) {
        createRender();
        if (renderer != null && setup()) {
            return renderer.addGifLayer(gifPath,startTimeUs,endTimeUs);
        }else{
            return null;
        }
    }

    /**
     * 增加gif图层
     * @param asset
     * @param startTimeUs  从容器的什么位置增加
     * @param endTimeUs 加到容器的什么位置, 如果把gif全部增加,则这里填入Long.MAX_VALUE
     * @return
     */
    public GifLayer addGifLayer(LSOGifAsset asset, long startTimeUs, long endTimeUs) {
        createRender();
        if (renderer != null && setup()) {
            return renderer.addGifLayer(asset,startTimeUs,endTimeUs);
        }else{
            return null;
        }
    }
    /**
     * 增加透明图片
     * 暂时不支持透明视频里的声音
     * @param colorPath
     * @param maskPath
     * @param startTimeUs 从容器的什么位置增加
     * @param endTimeUs 增加到容器的什么位置, 如果把mv全部增加,则这里填入Long.MAX_VALUE
     * @return
     */
    @Deprecated
    public MVCacheLayer addMVLayer(String colorPath, String maskPath, long startTimeUs, long endTimeUs) {
        createRender();
        if (renderer != null && setup()) {
            return renderer.addMVLayer(colorPath,maskPath,startTimeUs,endTimeUs);
        }else{
            return null;
        }
    }
    public MVLayer addMVLayer(LSOMVAsset asset) {
        createRender();
        if (renderer != null && asset!=null  && !asset.isReleased() && setup()) {
            return renderer.addMVLayer(asset,0,Long.MAX_VALUE);
        }else{
            return null;
        }
    }
    public MVLayer addMVLayer(LSOMVAsset asset, long startTimeUs, long endTimeUs) {
        createRender();
        if (renderer != null && asset!=null  && !asset.isReleased() && setup()) {
            return renderer.addMVLayer(asset,startTimeUs,endTimeUs);
        }else{
            return null;
        }
    }

    /**
     * 删除一个图层
     * @param layer
     */
    public void removeLayer(Layer layer){
        if (renderer != null &&  renderer.isRunning()) {
            renderer.removeLayer(layer);
        }
    }

    /**
     * 删除所有的图层, (不包括音频图层)
     */
    public void removeAllLayers(){
        if (renderer != null &&  renderer.isRunning()) {
            renderer.removeAllLayers();
        }
    }

    /**
     * 删除音频图层
     * @param layer
     */
    public void removeAudioLayer(AudioPreviewLayer layer){
        if (renderer != null &&  renderer.isRunning()) {
            renderer.removeAudioLayer(layer);
        }
    }

    /**
     * 删除所有音频图层;
     */
    public void removeAllAudioLayers(){
        if (renderer != null &&  renderer.isRunning()) {
            renderer.removeALLAudioLayer();
        }
    }

    //------------------增加音频图层---------------------------------
    /**
     * 增加声音图层;
     * @param audioAsset 声音文件对象;
     * @return
     */
    public AudioPreviewLayer addAudioLayer(LSOAudioAsset audioAsset) {
        if (renderer != null && audioAsset!=null) {
            AudioPreviewLayer layer= renderer.addAudioLayer(audioAsset,0,0,Long.MAX_VALUE);
            if(layer==null){
                LSOLog.e("DrawPadConcatExecute addAudioLayer 1 error. path:"+audioAsset);
            }
            return layer;
        } else {
            LSOLog.e("DrawPadConcatExecute addAudioLayer error. path:"+audioAsset);
            return null;
        }
    }

    /**
     * 增加音频图层.
     * @param audioAsset 音频资源
     * @param loop 是否循环;
     * @return
     */
    public AudioPreviewLayer addAudioLayer(LSOAudioAsset audioAsset,boolean loop) {
        if (renderer != null&& audioAsset!=null) {
            AudioPreviewLayer layer= renderer.addAudioLayer(audioAsset,0,0,Long.MAX_VALUE);
            if(layer==null){
                LSOLog.e("DrawPadConcatExecute addAudioLayer 2 error. path:"+audioAsset);
            }else{
                layer.setLooping(loop);
            }
            return layer;
        } else {
            LSOLog.e("DrawPadConcatExecute addAudioLayer error. path:"+audioAsset);
            return null;
        }
    }
    /**
     * 增加音频图层,;
     * 所有图层增加后 调用;
     * @param audioAsset
     * @param volume  音频音量, 1.0是正常音量,0.5是降低一倍; 2.0是提高一倍;
     * @return
     */
    public AudioPreviewLayer addAudioLayer(LSOAudioAsset audioAsset, float volume) {
        if (renderer != null&& audioAsset!=null) {
            AudioPreviewLayer layer=  renderer.addAudioLayer(audioAsset, 0,0, Long.MAX_VALUE);
            if(layer==null){
                layer.setVolume(volume);
                LSOLog.e("DrawPadConcatExecute addAudioLayer 3 error. path:"+audioAsset);
            }
            return layer;
        } else {
            LSOLog.e("DrawPadConcatExecute addAudioLayer error. path:"+audioAsset);
            return null;
        }
    }

    /**
     *  增加其他声音;
     *  所有图层增加后 调用;
     *
     * @param audioAsset
     * @param startFromPadUs 从容器的什么位置开始增加
     * @return
     */
    public AudioPreviewLayer addAudioLayer(LSOAudioAsset audioAsset, long startFromPadUs) {
        if (renderer != null&& audioAsset!=null) {
            AudioPreviewLayer layer=  renderer.addAudioLayer(audioAsset,startFromPadUs, 0, Long.MAX_VALUE);
            if(layer==null){
                LSOLog.e("DrawPadConcatExecute addAudioLayer 4 error. path:"+audioAsset);
            }
            return layer;
        } else {
            LSOLog.e("DrawPadConcatExecute addAudioLayer error. path:"+audioAsset);
            return null;
        }
    }

    /**
     * 如果要调节音量, 则增加拿到对象后, 开始调节.
     *
     * 所有图层增加后 调用;
     * @param audioAsset
     * @param startFromPadUs   从容器的什么位置开始增加
     * @param startAudioTimeUs 裁剪声音的开始时间
     * @param endAudioTimeUs   裁剪声音的结束时间;
     * @return
     */
    public AudioPreviewLayer addAudioLayer(LSOAudioAsset audioAsset, long startFromPadUs,long startAudioTimeUs, long endAudioTimeUs) {
        if (renderer != null && audioAsset!=null) {
            AudioPreviewLayer layer=renderer.addAudioLayer(audioAsset, startFromPadUs,
                    startAudioTimeUs, endAudioTimeUs);
            if(layer==null){
                LSOLog.e("DrawPadConcatExecute addAudioLayer  5 error. path:"+audioAsset);
            }
            return layer;
        } else {
            LSOLog.e("DrawPadConcatExecute addAudioLayer error. path:"+audioAsset);
            return null;
        }
    }

    //------------------------拼接类------------------------------
    /**
     * 拼接图片;
     * @param asset
     * @param durationUs
     * @return
     */
    public BitmapLayer concatBitmapLayer(LSOBitmapAsset asset, long durationUs) {
        createRender();
        if (renderer != null && asset!=null && setup()) {
            return renderer.concatBitmapLayer(asset,durationUs);
        }else{
            return null;
        }
    }

    /**
     * 拼接一个视频层
     * 先调用的在前面, 后调用的在后面;
     * @param asset
     * @param option2
     * @return
     */
    public VideoConcatLayer concatVideoLayer(LSOVideoAsset asset, LSOVideoOption2 option2){
        createRender();
        if (renderer != null && asset!=null && setup()) {
            return renderer.concatVideoLayer(asset,option2);
        }else{
            return null;
        }
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

    /**
     * 如果设置为is, 则
     * 如果您seek完毕后, 需要再次调用resumePreview 画面才真正开始播放;
     * @param is
     */
    public void setSeekMode(boolean is){
        if(renderer!=null){
            renderer.setSeekMode(is);
        }
    }

    public void seekToTimeUs(long timeUs){
        if(renderer!=null &&renderer.isRunning()){
            renderer.seekToUs(timeUs);
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



    public boolean isPreviewing(){
        return renderer!=null && renderer.isPreviewing();
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

    private boolean setup(){
        if(setupSuccess){
            return true;
        }
        if(renderer!=null && mSurfaceTexture!=null && !setupSuccess){
            renderer.updateDrawPadSize(drawPadWidth,drawPadHeight);
            renderer.setSurface(new Surface(mSurfaceTexture));
            setupSuccess= renderer.setup();
            LSOLog.e("AEComposition startPreview.ret:"+setupSuccess);
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
