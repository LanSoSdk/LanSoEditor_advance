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

import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.CanvasLayer;
import com.lansosdk.box.DataLayer;
import com.lansosdk.box.DrawPadUpdateMode;
import com.lansosdk.box.DrawPadViewRender2;
import com.lansosdk.box.GifLayer;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.Layer;
import com.lansosdk.box.MVLayer;
import com.lansosdk.box.TextureLayer;
import com.lansosdk.box.TwoVideoLayer;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.ViewLayer;
import com.lansosdk.box.YUVLayer;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadErrorListener;
import com.lansosdk.box.onDrawPadOutFrameListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.box.onDrawPadRunTimeListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.box.onDrawPadSnapShotListener;
import com.lansosdk.box.onDrawPadThreadProgressListener;

import com.lansosdk.LanSongFilter.LanSongFilter;

public class DrawPadView2 extends FrameLayout {

    static final int AR_ASPECT_FIT_PARENT = 0; // without clip
    static final int AR_ASPECT_FILL_PARENT = 1; // may clip
    /**
     * 视频画面显示模式: 自适应大小.当小于画面尺寸时,自动显示.当大于尺寸时,缩放显示.
     */
    static final int AR_ASPECT_WRAP_CONTENT = 2;
    /**
     * 视频画面显示模式:和父view的尺寸对其.完全填充满父view的尺寸
     */
    static final int AR_MATCH_PARENT = 3;
    /**
     * 把画面的宽度等于父view的宽度, 高度按照16:9的形式显示. 大部分的网络视频推荐用这种方式显示.
     */
    static final int AR_16_9_FIT_PARENT = 4;
    /**
     * 把画面的宽度等于父view的宽度, 高度按照4:3的形式显示.
     */
    static final int AR_4_3_FIT_PARENT = 5;
    private static final String TAG = "LanSongSDK";
    private static final boolean VERBOSE = false;
    private TextureRenderView mTextureRenderView;
    private DrawPadViewRender2 renderer;
    private SurfaceTexture mSurfaceTexture = null;
    private boolean isUseMainPts = false;
    private int encWidth, encHeight, encFrameRate;
    private int encBitRate = 0;
    private float encodeSpeed = 1.0f;
    private int drawPadWidth, drawPadHeight;
    private DrawPadUpdateMode mUpdateMode = DrawPadUpdateMode.ALL_VIDEO_READY;
    private int mAutoFlushFps = 0;
    private onViewAvailable mViewAvailable = null;
    private onDrawPadSizeChangedListener mSizeChangedCB = null;
    private onDrawPadRunTimeListener drawpadRunTimeListener = null;
    private onDrawPadProgressListener drawpadProgressListener = null;
    private onDrawPadThreadProgressListener drawPadThreadProgressListener = null;
    private onDrawPadSnapShotListener drawpadSnapShotListener = null;
    private onDrawPadOutFrameListener drawPadPreviewFrameListener = null;
    private int previewFrameWidth;
    private int previewFrameHeight;
    private int previewFrameType;
    private boolean frameListenerInDrawPad = false;
    private onDrawPadCompletedListener drawpadCompletedListener = null;
    private onDrawPadErrorListener drawPadErrorListener = null;
    private boolean isEditModeVideo = false;
    private boolean isPauseRefreshDrawPad = false;
    private boolean isPausePreviewDrawPad = false;
    private boolean isPauseRecord = false;
    // ----------------------------------------------
    private boolean isCheckBitRate = true;
    private boolean isCheckPadSize = true;

    public DrawPadView2(Context context) {
        super(context);
        initVideoView(context);
    }
    public DrawPadView2(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView(context);
    }

    public DrawPadView2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DrawPadView2(Context context, AttributeSet attrs, int defStyleAttr,
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
        mTextureRenderView = new TextureRenderView(getContext());
        mTextureRenderView.setSurfaceTextureListener(new SurfaceCallback());

        mTextureRenderView.setDispalyRatio(AR_ASPECT_FIT_PARENT);

        View renderUIView = mTextureRenderView.getView();
        LayoutParams lp = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        renderUIView.setLayoutParams(lp);
        addView(renderUIView);
        mTextureRenderView.setVideoRotation(0);
    }

    /**
     * 设置DrawPad的刷新模式
     * 两种模式, 视频驱动模式 和 自动刷新模式
     * 当有主视频时, 默认为视频驱动模式;
     * @param mode
     * @param autofps //自动刷新模式时用到的帧率值,每秒钟刷新几次(即视频帧率)
     */
    public void setUpdateMode(DrawPadUpdateMode mode, int autofps) {
        mAutoFlushFps = autofps;

        mUpdateMode = mode;

        if (renderer != null) {
            renderer.setUpdateMode(mUpdateMode, mAutoFlushFps);
        }
    }

    /**
     * 调整 录制视频帧的速度,
     *
     * 建议5个等级: 0.25f,0.5f,1.0f,1.5f,2.0f; 其中 0.25是放慢4倍; 0.5是放慢2倍;
     * @param speed 速度系数,
     */
    public void adjustEncodeSpeed(float speed) {
        if (renderer != null) {
            renderer.adjustEncodeSpeed(speed);
        }
        encodeSpeed = speed;
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

    public int getDrawPadWidth() {
        return drawPadWidth;
    }

    /**
     * 获得当前View的高度.
     *
     * @return
     */
    public int getDrawPadHeight() {
        return drawPadHeight;
    }

    public boolean isTextureAvailable(){
        return mSurfaceTexture!=null && isDrawPadSizeChanged;
    }
    /**
     * 此回调仅仅是作为演示: 当跳入到别的Activity后的返回时,会再次预览当前画面的功能.
     * 你完全可以重新按照你的界面需求来修改这个DrawPadView类.
     */
    public void setOnViewAvailable(onViewAvailable listener) {
        mViewAvailable = listener;
        if(mSurfaceTexture!=null){
            mViewAvailable.viewAvailable(this);
        }
    }
    /**
     * 录制参数设置
     *
     * @param encW    录制视频的宽度
     * @param encH    录制视频的高度
     * @param encBr   录制视频的bitrate,
     * @param encFr   录制视频的 帧率
     */
    public void setRecordParams(int encW, int encH, int encBr, int encFr) {
        if (encW > 0 && encH > 0 && encBr > 0 && encFr > 0) {
            encWidth = encW;
            encHeight = encH;
            encBitRate = encBr;
            encFrameRate = encFr;
            if(renderer!=null){
                renderer.setEncoderEnable(encWidth,encHeight,encBitRate,encFrameRate);
            }
        } else {
            Log.w(TAG, "enable real encode is error");
        }
    }
    boolean isDrawPadSizeChanged=false;
    /**
     * 调整当前DrawPadView的尺寸;
     *
     *
     * 会根据手机屏幕的宽度来做调整,默认是宽度对齐到手机的宽度, 然后调整高度,
     * 把调整后的宽高作为DrawPad渲染线程的宽高.
     *
     * 举例:
     * 设置的宽度和高度是480,480,
     * 而父view的宽度是等于手机分辨率是1080x1920,则DrawPad默认对齐到手机宽度1080,然后把高度也按照比例缩放到1080.
     *
     * @param width  DrawPad宽度
     * @param height DrawPad高度
     * @param cb     UI线程调整好后 返回的回调;
     */
    public void setDrawPadSize(int width, int height,onDrawPadSizeChangedListener cb) {

        isDrawPadSizeChanged=true;
        if (width != 0 && height != 0 && cb != null) {

            if(drawPadWidth==0 || drawPadHeight==0){  //直接重新布局UI
                mTextureRenderView.setVideoSize(width, height);
                mTextureRenderView.setVideoSampleAspectRatio(1, 1);
                mSizeChangedCB = cb;
                requestLayout();
            }else{
                float setAcpect = (float) width / (float) height;
                float setViewacpect = (float) drawPadWidth / (float) drawPadHeight;

                if (setAcpect == setViewacpect) { // 如果比例已经相等,不需要再调整,则直接显示.
                    if (cb != null) {
                        cb.onSizeChanged(width, height);
                    }
                } else if (Math.abs(setAcpect - setViewacpect) * 1000 < 16.0f) {
                    if (cb != null) {
                        cb.onSizeChanged(width, height);
                    }
                } else if (mTextureRenderView != null) {
                    mTextureRenderView.setVideoSize(width, height);
                    mTextureRenderView.setVideoSampleAspectRatio(1, 1);
                    mSizeChangedCB = cb;
                }
                requestLayout();
            }
        }
    }

    /**
     * 当前drawpad容器运行了多长时间, 每渲染一帧, 运行一次
     *
     *
     * listener回调单位是微秒, currentTimeUs; 此时间为即将渲染这一帧的时间,
     *
     * 比如你要在第3秒增加别的图层或调整指定图层的参数 则应该判断时间是否大于或等于3*1000*1000;
     *
     * 可复位这个时间.
     *
     * @param li
     */
    public void setOnDrawPadRunTimeListener(onDrawPadRunTimeListener li) {
        if (renderer != null) {
            renderer.setDrawPadRunTimeListener(li);
        }
        drawpadRunTimeListener = li;
    }

    /**
     * 把运行的时间复位到某一个值,
     *
     * 这样的话, drawpad继续显示, 就会以这个值为参考, 增加相对运行的时间. drawpad已经运行了10秒钟,
     * 你复位到2秒.则drawpad下一个onDrawPadRunTimeListener返回的值,就是2秒+相对运行的值,可能是2000*1000 +
     * 40*1000;
     * @param runtimeUs
     */
    public void resetDrawPadRunTime(long runtimeUs) {
        if (renderer != null) {
            renderer.resetPadRunTime(runtimeUs);
        }
    }
    /**
     * 方法与 setOnDrawPadRunTimeListener不同的地方在于:
     *
     * 即将开始一帧渲染的时候, 直接执行这个回调中的代码,不通过Handler传递出去,你可以精确的增删调整图层
     */
    public void setOnDrawPadThreadRunTimeListener(
            onDrawPadThreadProgressListener listener) {
        if (renderer != null) {
            renderer.setDrawPadThreadProgressListener(listener);
        }
        drawPadThreadProgressListener = listener;
    }

    /**
     * 每录制一帧的回调;
     * @param listener
     */
    public void setOnDrawPadRecordProgressListener(onDrawPadProgressListener listener) {
        if (renderer != null) {
            renderer.setDrawPadProgressListener(listener);
        }
        drawpadProgressListener = listener;
    }

    /**
     * 容器截图回调;
     *
     * 请不要在此方法里做图片的处理,以免造成拥堵;
     * 建议获取到bitmap后,放入到一个链表中,在外面或另开一个线程处理.
     */
    public void setOnDrawPadSnapShotListener(onDrawPadSnapShotListener listener) {
        if (renderer != null) {
            renderer.setDrawpadSnapShotListener(listener);
        }
        drawpadSnapShotListener = listener;
    }

    /**
     * 触发容器截图;
     *
     * 流程:
     * 会在DrawPad内部设置一个标志位,DrawPad线程会检测到这标志位后,
     * 截取DrawPad, 并通过onDrawPadSnapShotListener监听反馈给您. 请不要多次或每一帧都截取DrawPad,
     * 以免操作DrawPad处理过慢.
     */
    public void toggleSnatShot() {
        if (drawpadSnapShotListener != null && renderer != null
                && renderer.isRunning()) {
            renderer.toggleSnapShot(drawPadWidth, drawPadHeight);
        } else {
            Log.e(TAG, "toggle snap shot failed!!!");
        }
    }

    /**
     * 触发容器截图; 并设置截图后返回的图片宽高;
     * @param width
     * @param height
     */
    public void toggleSnatShot(int width, int height) {
        if (drawpadSnapShotListener != null && renderer != null
                && renderer.isRunning()) {
            renderer.toggleSnapShot(width, height);
        } else {
            Log.e(TAG, "toggle snap shot failed!!!");
        }
    }

    /**
     *
     * 抓取容器的图片流
     *
     * 建议在这里拿到数据后, 放到queue中, 然后在其他线程中来异步读取queue中的数据, 请注意queue中数据的总大小, 要及时处理和释放,
     * 以免内存过大,造成OOM问题
     *
     * @param width    可以设置要引出这一帧画面的宽度, 如果宽度不等于drawpad的预览宽度,则会缩放.
     * @param height   画面缩放到的高度,
     * @param type     数据的类型, 当前仅支持Bitmap 这里暂时无效;
     * @param listener 监听对象.
     */
    public void setOnDrawPadOutFrameListener(int width, int height, int type,
                                             onDrawPadOutFrameListener listener) {
        if (renderer != null) {
            renderer.setDrawpadOutFrameListener(width, height, type, listener);
        }
        previewFrameWidth = width;
        previewFrameHeight = height;
        previewFrameType = type;
        drawPadPreviewFrameListener = listener;
    }

    /**
     * 容器释放后的回调;
     * @param listener
     */
    public void setOnDrawPadCompletedListener(onDrawPadCompletedListener listener) {
        if (renderer != null) {
            renderer.setDrawPadCompletedListener(listener);
        }
        drawpadCompletedListener = listener;
    }

    /**
     * 设置当前DrawPad运行错误的回调监听.
     *
     * @param listener
     */
    public void setOnDrawPadErrorListener(onDrawPadErrorListener listener) {
        if (renderer != null) {
            renderer.setDrawPadErrorListener(listener);
        }
        drawPadErrorListener = listener;
    }
    /**
     * 设置在录制的时候, 是否生成 可编辑模式的视频;
     */
    public void setEditModeVideo(boolean is) {
        if (renderer != null) {
            renderer.setEditModeVideo(is);
        } else {
            isEditModeVideo = is;
        }
    }

    /**
     * 建立容器
     */
    public boolean setupDrawPad()
    {
        isPausePreviewDrawPad = true;
        isPauseRefreshDrawPad=true;
        isPauseRecord=true;
        return startDrawPad();
    }

    /**
     * 注销容器.
     * 如果容器正在录制,则先停止,并返回录制好的视频路径
     * @return
     */
    public String releaseDrawPad(){

        String  ret=null;
        if (renderer != null) {
            if(renderer.isRecording()){
                ret=renderer.segmentStop();
            }
            renderer.release();
            renderer = null;
        }
        return ret;
    }

    /**
     * 开始预览
     */
    public void startPreview()
    {
        if(renderer!=null){
            renderer.resumeRefreshDrawPad();
            renderer.resumePreviewDrawPad();
        }else{
            LSOLog.e("当前容器没有创建或已经释放,请先调用setupDrawPad()建立容器,并增加图层.");
        }
        isPausePreviewDrawPad = false;
    }
    /**
     * 暂停预览,
     *
     * 停止预览后, 如果您的后台还在录制中,则会一直录制同一副画面.
     */
    public void pausePreview() {
        if (renderer != null) {
            renderer.pausePreviewDrawPad();
        }
        isPausePreviewDrawPad = true;
    }

    /**
     * 恢复预览;
     */
    public void resumePreview() {
        if(renderer!=null){
            renderer.resumeRefreshDrawPad();
            renderer.resumePreviewDrawPad();
        }
        isPausePreviewDrawPad = false;
    }


    public boolean isRecording() {
        if (renderer != null)
            return renderer.isRecording();
        else
            return false;
    }

    /**
     * 当前DrawPad是否在工作.
     *
     * setupDrawPad后返回true,表示已经工作;
     * releaseDrawPad后,不再工作;
     *
     * 预览暂停时,这里依然返回true;,因为容器在工作,只是没有更新画面而已;
     */
    public boolean isRunning() {
        if (renderer != null)
            return renderer.isRunning();
        else
            return false;
    }

    /**
     * 开始录制
     */
    public void startRecord(){
        if (renderer != null && renderer.isRecording()==false) {
            renderer.segmentStart();
        }
    }

    /**
     * 录制一段结束.


     * 录制完成后, 返回当前录制这一段的视频文件完整路径名
     * 在低端手机上大概100ms,建议用Handler的 HandlerMessage的形式来处理
     */
    public String stopRecord(){
        if (renderer != null && renderer.isRecording()) {
            return renderer.segmentStop();
        } else {
            return null;
        }
    }
    private boolean startDrawPad() {
        boolean ret = false;
        if(renderer!=null){
            renderer.stopDrawPad();
            renderer=null;
        }
        if (mSurfaceTexture != null && renderer == null && drawPadWidth > 0
                && drawPadHeight > 0) {
            renderer = new DrawPadViewRender2(getContext(), drawPadWidth,drawPadHeight);
            if (renderer != null) {
                renderer.setUseMainVideoPts(isUseMainPts);
                renderer.setDisplaySurface(new Surface(mSurfaceTexture));

                if (isCheckPadSize) {
                    encWidth = LanSongUtil.make16Multi(encWidth); // 编码默认16字节对齐.
                    encHeight = LanSongUtil.make16Multi(encHeight);
                }
                if (isCheckBitRate || encBitRate == 0) {
                    encBitRate = LanSongUtil.checkSuggestBitRate(encHeight
                            * encWidth, encBitRate);
                }
                if(encWidth>0 && encHeight>0){
                    renderer.setEncoderEnable(encWidth, encHeight, encBitRate,encFrameRate);
                }
                if (isEditModeVideo) {
                    renderer.setEditModeVideo(isEditModeVideo);
                }
                renderer.setUpdateMode(mUpdateMode, mAutoFlushFps);

                // 设置DrawPad处理的进度监听, 回传的currentTimeUs单位是微秒.
                renderer.setDrawpadSnapShotListener(drawpadSnapShotListener);
                renderer.setDrawpadOutFrameListener(previewFrameWidth,
                        previewFrameHeight, previewFrameType,
                        drawPadPreviewFrameListener);
                renderer.setOutFrameInDrawPad(frameListenerInDrawPad);

                renderer.setDrawPadProgressListener(drawpadProgressListener);
                renderer.setDrawPadCompletedListener(drawpadCompletedListener);
                renderer.setDrawPadThreadProgressListener(drawPadThreadProgressListener);
                renderer.setDrawPadErrorListener(drawPadErrorListener);
                renderer.setDrawPadRunTimeListener(drawpadRunTimeListener);

                if (isPauseRecord) {
                    renderer.pauseRecordDrawPad();
                }
                if (isPauseRefreshDrawPad) {
                    renderer.pauseRefreshDrawPad();
                }
                if (isPausePreviewDrawPad) {
                    renderer.pausePreviewDrawPad();
                }
                renderer.adjustEncodeSpeed(encodeSpeed);

                ret = renderer.startDrawPad();
                if (!ret) {
                    Log.e(TAG,"开启 drawPad 失败, 或许是您之前的DrawPad没有Stop, 或者传递进去的surface对象已经被系统Destory!!,"
                            + "请检测您 的代码或参考本文件中的SurfaceCallback 这个类中的注释;\n");
                }else {
                    Log.i(TAG,"DrawPadView2 is running...");
                }
            }
        } else {
            if (mSurfaceTexture == null) {
                Log.e(TAG,"可能您的UI界面还没有完全启动,您就startDrawPad了, 建议oncreate后延迟300ms再调用");
            } else {
                Log.e(TAG, "无法开启DrawPad, 您当前的参数有问题,您对照下参数:宽度和高度是:"
                        + drawPadWidth + " x " + drawPadHeight
                        + " mSurfaceTexture:" + mSurfaceTexture);
            }
        }
        return ret;
    }
    /**
     * 直接设置容器的宽高, 不让他自动缩放.
     * 要在容器开始前调用.
     */
    public void setDrawpadSizeDirect(int width, int height) {
        drawPadWidth = width;
        drawPadHeight = height;
        if (renderer != null) {
            Log.w(TAG,"aeRenderer maybe is running. your setting is not available!!");
        }
    }

    /**
     * 把当前图层放到最里层, 里面有 一个handler-loop机制, 将会在下一次刷新后执行.
     * @param layer
     */
    public void bringLayerToBack(Layer layer) {
        if (renderer != null) {
            renderer.bringLayerToBack(layer);
        }
    }

    /**
     * 把当前图层放到最外层, 里面有 一个handler-loop机制, 将会在下一次刷新后执行.
     * @param layer
     */
    public void bringLayerToFront(Layer layer) {
        if (renderer != null) {
            renderer.bringLayerToFront(layer);
        }
    }

    /**
     * 设置当前图层对象layer 在DrawPad中所有图层队列中的位置, 您可以认为内部是一个ArrayList的列表, 先add进去的
     * 的position是0, 后面增加的依次是1,2,3等等 可以通过{@link Layer#getIndexLayerInDrawPad()}
     * 来获取当前图层的位置.
     *
     * @param layer
     * @param position
     */
    public void changeLayerPosition(Layer layer, int position) {
        if (renderer != null) {
            renderer.changeLayerLayPosition(layer, position);
        }
    }

    /**
     * 交换两个图层的位置.
     *
     * @param first
     * @param second
     */
    public void swapTwoLayerPosition(Layer first, Layer second) {
        if (renderer != null) {
            renderer.swapTwoLayerPosition(first, second);
        }
    }

    /**
     * 获取当前容器中有多少个图层.
     *
     * @return
     */
    public int getLayerSize() {
        if (renderer != null) {
            return renderer.getLayerSize();
        } else {
            return 0;
        }
    }

    /**
     * 获取一个主视频的 VideoLayer
     *
     * 主视频的意思是: 对一个完整的视频做操作, 从视频的0秒一直运行到视频的最后一帧,
     * 比如对一个视频做滤镜,增加图片图层, 增加mv图层等的场合.
     *
     * 如果你要先把一个视频add进来, 后面再增加另一个视频,则用 addVideoLayer(),不能用这个方法.
     * @param width  主视频的画面宽度 建议用 {@link MediaInfo#vWidth}来赋值
     * @param height 主视频的画面高度
     * @return
     */
    public VideoLayer addMainVideoLayer(int width, int height, LanSongFilter filter) {
        VideoLayer ret = null;

        if (renderer != null)
            ret = renderer.addMainVideoLayer(width, height, filter);
        else {
            Log.e(TAG, "setMainVideoLayer error render is not avalid");
        }
        return ret;
    }
    /**
     * 增加双视频图层, 一个视频是内容,另一个视频是效果画面. 适用在把一个效果视频融合在当前视频中.
     *
     * @param width
     * @param height
     * @return
     */
    public TwoVideoLayer addTwoVideoLayer(int width, int height) {
        TwoVideoLayer ret = null;

        if (renderer != null)
            ret = renderer.addTwoVideoLayer(width, height);
        else {
            Log.e(TAG, "addTwoVideoLayer error render is not avalid");
        }
        return ret;
    }

    /**
     */
    public VideoLayer addVideoLayer(int width, int height, LanSongFilter filter) {
        if (renderer != null)
            return renderer.addVideoLayer(width, height, filter);
        else {
            Log.e(TAG, "addVideoLayer error render is not avalid");
            return null;
        }
    }
    /**
     * 获取一个BitmapLayer
     *
     * 在 startDrawPad之后调用;
     *
     * @param bmp 图片的bitmap对象,
     * @return BitmapLayer对象
     */
    public BitmapLayer addBitmapLayer(Bitmap bmp) {
        if (bmp != null) {
            if (renderer != null && renderer.isRunning())
                return renderer.addBitmapLayer(bmp, null);
            else {
                Log.e(TAG, "addBitmapLayer error render is not avalid");
                return null;
            }
        } else {
            Log.e(TAG, "addBitmapLayer error, byteBuffer is null");
            return null;
        }
    }

    /**
     * 增加图片图层, 并设置一个滤镜;
     * @param bmp
     * @param filter
     * @return
     */
    public BitmapLayer addBitmapLayer(Bitmap bmp, LanSongFilter filter) {
        if (bmp != null) {
            if (renderer != null)
                return renderer.addBitmapLayer(bmp, filter);
            else {
                Log.e(TAG, "addBitmapLayer error render is not avalid");
                return null;
            }
        } else {
            Log.e(TAG, "addBitmapLayer error, byteBuffer is null");
            return null;
        }
    }

    /**
     * 增加纹理图层.
     *
     * @param texid  纹理ID, 这个纹理务必要在我们的ThreadProgress中创建.
     * @param width  纹理的宽度
     * @param height 纹理的高度
     * @param filter 初始化的滤镜.
     * @return
     */
    public TextureLayer addTextureLayer(int texid, int width, int height,
                                        LanSongFilter filter) {
        if (texid != -1) {
            if (renderer != null && renderer.isRunning())
                return renderer.addTextureLayer(texid, width, height, filter);
            else {
                Log.e(TAG, "addTextureLayer error render is not avalid");
                return null;
            }
        } else {
            Log.e(TAG, "addTextureLayer error, texid is error");
            return null;
        }
    }

    /**
     * 获取一个DataLayer的图层,
     * 数据图层, RGBA格式
     *
     * @param dataWidth  图像的宽度
     * @param dataHeight 图像的高度.
     * @return
     */
    public DataLayer addDataLayer(int dataWidth, int dataHeight) {
        if (dataWidth > 0 && dataHeight > 0) {
            if (renderer != null)
                return renderer.addDataLayer(dataWidth, dataHeight);
            else {
                Log.e(TAG, "addDataLayer error render is not avalid");
                return null;
            }
        } else {
            Log.e(TAG, "addDataLayer error, data size is error");
            return null;
        }
    }

    /**
     * 增加一个gif图层.
     *
     * @param gifPath gif的绝对地址,
     * @return
     */
    public GifLayer addGifLayer(String gifPath) {
        if (renderer != null)
            return renderer.addGifLayer(gifPath);
        else {
            Log.e(TAG, "addYUVLayer error! render is not avalid");
            return null;
        }
    }

    /**
     * 增加一个gif图层,
     *
     * resId 来自apk中drawable文件夹下的各种资源文件, 我们会在GifLayer中拷贝这个资源到默认文件夹下面,
     * 然后作为一个普通的gif文件来做处理,使用完后, 会在Giflayer 图层释放的时候, 删除.
     *
     * @param resId 来自apk中drawable文件夹下的各种资源文件.
     * @return
     */
    public GifLayer addGifLayer(int resId) {
        if (renderer != null)
            return renderer.addGifLayer(resId);
        else {
            Log.e(TAG, "addGifLayer error! render is not avalid");
            return null;
        }
    }

    /**
     * 增加一个mv图层,
     * mv图层分为两个视频文件, 一个是彩色的视频, 一个黑白视频
     *
     * [详见我们的MV原理和制作文件]
     *
     * @param colorPath
     * @param maskPath
     * @return
     */
    public MVLayer addMVLayer(String colorPath, String maskPath) {
        if (renderer != null)
            return renderer.addMVLayer(colorPath, maskPath);
        else {
            Log.e(TAG, "addMVLayer error render is not avalid");
            return null;
        }
    }

    /**
     * 增加一个mv图层,
     *
     * 是否在mv好了之后, 直接去显示, 如果不想直接显示, 可以先设置isShow=false,然后在需要显示的使用, 调用
     * {@link MVLayer #setPlayEnable(boolean)}, 此方法暂时只能被调用一次.
     *
     * @param srcPath
     * @param maskPath
     * @param isplay   是否直接显示.
     * @return
     */
    public MVLayer addMVLayer(String srcPath, String maskPath, boolean isplay) {
        if (renderer != null)
            return renderer.addMVLayer(srcPath, maskPath, isplay);
        else {
            Log.e(TAG, "addMVLayer error render is not avalid");
            return null;
        }
    }

    /**
     * 获得一个 ViewLayer,
     * 您可以在获取后,仿照我们的例子,来为视频增加各种UI控件.
     *
     * @return 返回ViewLayer对象.
     */
    public ViewLayer addViewLayer() {
        if (renderer != null)
            return renderer.addViewLayer();
        else {
            Log.e(TAG, "addViewLayer error render is not avalid");
            return null;
        }
    }

    /**
     * 获得一个 CanvasLayer
     * 在 startDrawPad之后,在stopDrawPad之前调用.
     * @return
     */
    public CanvasLayer addCanvasLayer() {
        if (renderer != null)
            return renderer.addCanvasLayer();
        else {
            Log.e(TAG, "addCanvasLayer error render is not avalid");
            return null;
        }
    }

    /**
     * 增加一个yuv图层, 让您可以把YUV数据输入进来,当前仅支持NV21的格式
     * <p>
     * yuv数据,可以是别家SDK处理后的结果, 或Camera的onPreviewFrame回调的数据,
     * 或您本地的视频数据.也可以是您本地的视频数据.
     *
     * @param width
     * @param height
     * @return
     */
    public YUVLayer addYUVLayer(int width, int height) {
        if (renderer != null)
            return renderer.addYUVLayer(width, height);
        else {
            Log.e(TAG, "addCanvasLayer error render is not avalid");
            return null;
        }
    }

    /**
     * 从渲染线程列表中移除并销毁这个Layer;
     *
     * @param layer
     */
    public void removeLayer(Layer layer) {
        if (layer != null) {
            if (renderer != null)
                renderer.removeLayer(layer);
            else {
                Log.w(TAG, "removeLayer error render is not avalid");
            }
        }
    }
    /**
     * 是否在开始运行DrawPad的时候,检查您设置的码率和分辨率是否正常.
     * <p>
     * 默认是检查, 如果您清楚码率大小的设置,请调用此方法,不再检查.
     */
    public void setNotCheckBitRate() {
        isCheckBitRate = false;
    }

    /**
     * 是否在开始运行DrawPad的时候, 检查您设置的DrawPad宽高是否是16的倍数. 默认是检查.
     */
    public void setNotCheckDrawPadSize() {
        isCheckPadSize = false;
    }

    public interface onViewAvailable {
        void viewAvailable(DrawPadView2 v);
    }

    private class SurfaceCallback implements TextureView.SurfaceTextureListener {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                              int width, int height) {
            mSurfaceTexture = surface;
            drawPadHeight = height;
            drawPadWidth = width;
            if (mViewAvailable != null) {
                mViewAvailable.viewAvailable(null);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                                int width, int height) {
            mSurfaceTexture = surface;
            drawPadHeight = height;
            drawPadWidth = width;
            if (mSizeChangedCB != null)
                mSizeChangedCB.onSizeChanged(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            mSurfaceTexture = null;
            // drawPadHeight=0;
            // drawPadWidth=0;
            releaseDrawPad(); // 可以在这里增加以下. 这样当Texture销毁的时候, 停止DrawPad
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    }
}
