package com.lansosdk.videoeditor;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import com.lansosdk.LanSongFilter.LanSongFilter;
import com.lansosdk.box.AudioLine;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.ExtCameraLayer;
import com.lansosdk.box.ExtDrawPadCameraRunnable;
import com.lansosdk.box.GifLayer;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LSOMVAsset2;
import com.lansosdk.box.Layer;
import com.lansosdk.box.MVLayer2;
import com.lansosdk.box.MicLine;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.ViewLayer;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadErrorListener;
import com.lansosdk.box.onDrawPadOutFrameListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.box.onDrawPadRecordCompletedListener;
import com.lansosdk.box.onDrawPadRunTimeListener;
import com.lansosdk.box.onDrawPadSnapShotListener;
import com.lansosdk.box.onDrawPadThreadProgressListener;

import java.util.concurrent.atomic.AtomicBoolean;


public class ExtDrawPadCameraView extends FrameLayout {

    private ExtDrawPadCameraRunnable renderer;



    protected float padBGRed =0.0f;
    protected float padBGGreen =0.0f;
    protected float padBGBlur =0.0f;
    protected float padBGAlpha =1.0f;

    private TextureRenderView textureRenderView;
    private SurfaceTexture mSurfaceTexture = null;
    private onViewAvailable mViewAvailable = null;
    private boolean isLayoutOk=false;
    // ----------------------------------------------
    public ExtDrawPadCameraView(Context context) {
        super(context);
        initVideoView(context);
    }

    public ExtDrawPadCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView(context);
    }

    public ExtDrawPadCameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ExtDrawPadCameraView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
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
    }
    /**
     *当前View有效的时候, 回调监听;
     */
    public void setOnViewAvailable(onViewAvailable listener) {
        mViewAvailable = listener;
        if (mSurfaceTexture != null) {
            if(viewHeight >0 && viewWidth >0 && compWidth>0 && compHeight>0){

                float acpect = (float) compWidth / (float) compHeight;
                float padAcpect = (float) viewWidth / (float) viewHeight;

                if (acpect == padAcpect) { // 如果比例已经相等,不需要再调整,则直接显示.
                    isLayoutOk=true;
                    mViewAvailable.viewAvailable(this);
                } else if (Math.abs(acpect - padAcpect) * 1000 < 16.0f) {
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
        void viewAvailable(ExtDrawPadCameraView v);
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
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    }

    private OnCompositionSizeRatioListener sizeChangedListener;

    private int compWidth,compHeight;


    /**
     * 设置容器的大小, 在设置后, 我们会根据这个大小来调整 这个类的大小.
     * 从而让画面不变形;
     * @param width
     * @param height
     * @param listener
     */
    public void setDrawPadSizeAsync(int width, int height, OnCompositionSizeRatioListener listener) {

//        这里有设置的容器尺寸, 和实际显示的尺寸;
        requestLayoutCount=0;
        compWidth=width;
        compHeight=height;

        if (width != 0 && height != 0) {
            if(viewWidth ==0 || viewHeight ==0){  //直接重新布局UI
                textureRenderView.setVideoSize(width, height);
                textureRenderView.setVideoSampleAspectRatio(1, 1);
                sizeChangedListener = listener;
                requestLayoutPreview();
            }else{
                float setRatio = (float) width / (float) height;
                float setViewRatio = (float) viewWidth / (float) viewHeight;

                if (setRatio == setViewRatio) { // 如果比例已经相等,不需要再调整,则直接显示.
                    if (listener != null) {
                        isLayoutOk=true;
                        listener.onSizeChanged(width, height);
                    }
                } else if (Math.abs(setRatio - setViewRatio) * 1000 < 16.0f) {
                    if (listener != null) {
                        isLayoutOk=true;
                        listener.onSizeChanged(width, height);
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

    private int requestLayoutCount=0;
    /**
     * 检查得到的大小, 如果和view成比例,则直接回调; 如果不成比例,则重新布局;
     */
    private void checkLayoutSize(){
        float desireRatio = (float) compWidth / (float) compHeight;
        float padRatio = (float) viewWidth / (float) viewHeight;

        if (desireRatio == padRatio) { // 如果比例已经相等,不需要再调整,则直接显示.
            isLayoutOk=true;
            if (sizeChangedListener != null) {
                sizeChangedListener.onSizeChanged(viewWidth, viewHeight);
                sizeChangedListener =null;
            }else if(mViewAvailable!=null){
                mViewAvailable.viewAvailable(this);
            }
        } else if (Math.abs(desireRatio - padRatio) * 1000 < 16.0f) {
            isLayoutOk=true;
            if (sizeChangedListener != null) {
                sizeChangedListener.onSizeChanged(viewWidth, viewHeight);
                sizeChangedListener =null;
            }else if(mViewAvailable!=null){
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
            if(sizeChangedListener!=null){
                sizeChangedListener.onSizeChanged(viewWidth, viewHeight);
            }else if(mViewAvailable!=null){
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
    private int padWidth, padHeight;
    private boolean isFrontCam = false;
    private LanSongFilter initFilter = null;
    private int encWidth, encHeight, encFrameRate;
    private int encBitRate = 0;
    private String encodeOutput = null; // 编码输出路径
    private onDrawPadRunTimeListener drawpadRunTimeListener = null;
    private onDrawPadProgressListener drawpadProgressListener = null;
    private onDrawPadRecordCompletedListener drawPadRecordCompletedListener=null;
    private onDrawPadThreadProgressListener drawPadThreadProgressListener = null;
    private onDrawPadSnapShotListener drawpadSnapShotListener = null;
    private onDrawPadOutFrameListener drawPadOutFrameListener = null;
    private int outFrameWidth;
    private int outFrameHeight;
    private int outFrameType;
    private boolean frameListenerInDrawPad = false;
    private onDrawPadCompletedListener drawpadCompletedListener = null;
    private onDrawPadErrorListener drawPadErrorListener = null;
    private ExtCameraLayer extCameraLayer = null;
    private boolean isEditModeVideo = false;

    /**
     * 设置摄像头图层的餐宿, 是否前置, 是否设置滤镜,如不设置滤镜,填入null
     * @param front
     * @param filter
     */
    public void setCameraParam(boolean front, LanSongFilter filter) {
        isFrontCam = front;
        initFilter = filter;
    }

    /**
     * 设置录制视频的宽高, 建议和预览的宽高成比例, 比如预览的全屏是16:9则设置的编码宽度和高度也要16:9;
     * 如果是18:9,则这里的encW:encH也要18:9; 从而保证录制后的视频不变形;
     */
    public void setRealEncodeEnable(int encW, int encH, int encBr, int encFr,
                                    String outPath) {
        if (encW > 0 && encH > 0 && encBr > 0 && encFr > 0) {
            encWidth = encW;
            encHeight = encH;
            encBitRate = encBr;
            encFrameRate = encFr;
            encodeOutput = outPath;
        } else {
            LSOLog.e(  "enable real encode is error");
        }
    }

    public void setRealEncodeEnable(int encW, int encH, int encFr,
                                    String outPath) {
        if (encW > 0 && encH > 0 && encFr > 0) {
            encWidth = encW;
            encHeight = encH;
            encBitRate = 0;
            encFrameRate = encFr;
            encodeOutput = outPath;
        } else {
            LSOLog.e(  "enable real encode is error");
        }
    }


    public void setOnDrawPadRunTimeListener(onDrawPadRunTimeListener li) {
        if (renderer != null) {
            renderer.setDrawPadRunTimeListener(li);
        }
        drawpadRunTimeListener = li;
    }

    public void resetDrawPadRunTime(long runtimeUs) {
        if (renderer != null) {
            renderer.resetPadRunTime(runtimeUs);
        }
    }

    /**
     * 录制执行的回调.
     * DrawPad每录制完一帧画面,会通过Handler机制,回传调用这个Listener,返回的timeUs是当前录制画面的时间戳(微妙),
     *LSTODO: 应该有 录制的时候的进度, 因为录制用户可能会暂停, 这个进度会把录制暂停的时间也计算进去;
     */
    public void setOnDrawPadProgressListener(onDrawPadProgressListener listener) {
        if (renderer != null) {
            renderer.setDrawPadProgressListener(listener);
        }
        drawpadProgressListener = listener;
    }
    /**
     * 方法与 onDrawPadProgressListener不同的地方在于: 即将开始一帧渲染的时候,
     *
     *
     * 直接执行这个回调中的代码,不通过Handler传递出去,你可以精确的执行一些这一帧的如何操作.
     *
     * 不能在回调 内增加各种UI相关的代码
     *
     * 不要在代码中增加过多的耗时的代码, 以造成内部处理线程的阻塞.
     *
     * @param listener
     */
    public void setOnDrawPadThreadProgressListener(
            onDrawPadThreadProgressListener listener) {
        if (renderer != null) {
            renderer.setDrawPadThreadProgressListener(listener);
        }
        drawPadThreadProgressListener = listener;
    }

    /**
     *
     * 设置 获取当前DrawPad这一帧的画面的监听, 注意,因为Camera是全速运行, 会根据手机cpu的速度来执行, 可能一秒钟执行50帧,
     * 或60帧, 也可能一秒钟执行30帧. 设置截图监听,当截图完成后, 返回当前图片的btimap格式的图片. 此方法工作在主线程.
     * 请不要在此方法里做图片的处理,以免造成拥堵; 建议获取到bitmap后,放入到一个链表中,在外面或另开一个线程处理.
     */
    public void setOnDrawPadSnapShotListener(onDrawPadSnapShotListener listener) {
        if (renderer != null) {
            renderer.setDrawPadSnapShotListener(listener);
        }
        drawpadSnapShotListener = listener;
    }

    /**
     * 设置每处理一帧的数据预览监听, 等于把当前处理的这一帧的画面拉出来, 您可以根据这个画面来自行的编码保存, 或网络传输.
     * 【注意:此回调是在编码的时候执行的, 您需要先设置编码的各种参数, 并启动编码,这里才会触发. 回调的频率等于编码的帧率】
     * <p>
     * 可以通过 {@link #setOutFrameInDrawPad(boolean)}
     * 来设置listener运行在DrawPad线程中,还是运行UI主线程.
     * <p>
     * 建议在这里拿到数据后, 放到queue中, 然后在其他线程中来异步读取queue中的数据, 请注意queue中数据的总大小, 要及时处理和释放,
     * 以免内存过大,造成OOM问题
     *
     * @param width    可以设置要引出这一帧画面的宽度, 如果宽度不等于drawpad的预览宽度,则会缩放.
     * @param height   画面缩放到的高度,
     * @param type     数据的类型, 当前仅支持Bitmap, 后面或许会NV21等.
     * @param listener 监听对象. 【注意:此回调是在编码的时候执行的, 您需要先设置编码的各种参数, 并启动编码,这里才会触发.
     *                 回调的频率等于编码的帧率】
     */
    public void setOnDrawPadOutFrameListener(int width, int height, int type,
                                             onDrawPadOutFrameListener listener) {
        if (renderer != null) {
            renderer.setDrawpadOutFrameListener(width, height, type, listener);
        }
        outFrameWidth = width;
        outFrameHeight = height;
        outFrameType = type;
        drawPadOutFrameListener = listener;
    }

    /**
     * 设置setOnDrawPadOutFrameListener后,
     *
     * 可以设置这个方法来让listener是否运行在Drawpad线程中.
     *
     * 如果你要直接使用里面的数据, 则不用设置, 如果你要开启另一个线程, 把listener传递过来的数据送过去,则建议设置为true;
     *
     * @param en
     */
    public void setOutFrameInDrawPad(boolean en) {
        if (renderer != null) {
            renderer.setOutFrameInDrawPad(en);
        }
        frameListenerInDrawPad = en;
    }

    /**
     * 触发一下截取当前DrawPad中的内容. 触发后, 会在DrawPad内部设置一个标志位,DrawPad线程会检测到这标志位后,
     * 截取DrawPad, 并通过onDrawPadSnapShotListener监听反馈给您. 请不要多次或每一帧都截取DrawPad,
     * 以免操作DrawPad处理过慢.
     */
    public void toggleSnatShot() {
        if (drawpadSnapShotListener != null && renderer != null
                && renderer.isRunning()) {
            renderer.toggleSnapShot(padWidth, padHeight);
        } else {
            LSOLog.e( "toggle snap shot failed!!!");
        }
    }

    public void toggleSnatShot(int width, int height) {
        if (drawpadSnapShotListener != null && renderer != null
                && renderer.isRunning()) {
            renderer.toggleSnapShot(width, height);
        } else {
            LSOLog.e(  "toggle snap shot failed!!!");
        }
    }

    /**
     * DrawPad容器执行完毕后的监听.
     *
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

    public void setEditModeVideo(boolean is) {
        if (renderer != null) {
            renderer.setEditModeVideo(is);
        } else {
            isEditModeVideo = is;
        }
    }

    /**
     * 建立一个容器,
     *
     *
     * @return
     */
    public boolean setupDrawPad() {
        if (renderer == null) {
            padWidth=viewWidth;
            padHeight=viewHeight;

            pauseRecord();
            pausePreview();
            return startDrawPad(true);
        } else {
            return false;
        }
    }
    /**
     * 是否也录制mic的声音.
     */
    private boolean isRecordMic = false;
    private boolean isPauseRefresh = false;
    private boolean isPauseRecord = false;
    private boolean isRecordExtPcm = false; // 是否使用外面的pcm输入.
    private int pcmSampleRate = 44100;
    private int pcmBitRate = 64000;
    private int pcmChannels = 2; // 音频格式. 音频默认是双通道.
    private String recordExtMp3 = null;
    private long recordOffsetUs = 0;
    private static boolean isCameraOpened = false;
    private AtomicBoolean stopping = new AtomicBoolean(false);
    public boolean startDrawPad() {
        return startDrawPad(isPauseRecord);
    }

    private boolean startDrawPad(boolean pauseRecord) {
        boolean ret = false;

        if (isCameraOpened) {
            LSOLog.e( "DrawPad线程已经开启.,如果您是从下一个Activity返回的,请先stopDrawPad后,再次开启.");
            return false;
        }

        if (mSurfaceTexture != null && renderer == null && padWidth > 0
                && padHeight > 0) {

            renderer = new ExtDrawPadCameraRunnable(getContext(), padWidth,
                    padHeight); // <----从这里去建立DrawPad线程.


            renderer.setCameraParam(isFrontCam, initFilter);

            DisplayMetrics dm = getResources().getDisplayMetrics();

            if (dm != null && padWidth == dm.widthPixels && padHeight == dm.heightPixels) {
                renderer.setFullScreen(true);
            }

            if (renderer != null) {
                renderer.setDisplaySurface(textureRenderView, new Surface(mSurfaceTexture));

                if (isCheckPadSize) {
                    encWidth = LanSongUtil.make16Multi(encWidth);
                    encHeight = LanSongUtil.make16Multi(encHeight);
                }
                if (isCheckBitRate || encBitRate == 0) {
                    encBitRate = LanSongUtil.checkSuggestBitRate(encHeight
                            * encWidth, encBitRate);
                }
                renderer.setEncoderEnable(encWidth, encHeight, encBitRate,
                        encFrameRate, encodeOutput);
                if (isEditModeVideo) {
                    renderer.setEditModeVideo(isEditModeVideo);
                }
                // 设置DrawPad处理的进度监听, 回传的currentTimeUs单位是微秒.
                renderer.setDrawPadSnapShotListener(drawpadSnapShotListener);
                renderer.setDrawpadOutFrameListener(outFrameWidth,outFrameHeight, outFrameType, drawPadOutFrameListener);
                renderer.setDrawPadProgressListener(drawpadProgressListener);
                renderer.setDrawPadCompletedListener(drawpadCompletedListener);
                renderer.setDrawPadThreadProgressListener(drawPadThreadProgressListener);
                renderer.setOutFrameInDrawPad(frameListenerInDrawPad);
                renderer.setDrawPadRunTimeListener(drawpadRunTimeListener);
                renderer.setDrawPadRecordCompletedListener(drawPadRecordCompletedListener);
                renderer.setDrawPadErrorListener(drawPadErrorListener);
                if (isRecordMic) {
                    renderer.setRecordMic(isRecordMic);
                } else if (isRecordExtPcm) {
                    renderer.setRecordExtraPcm(isRecordExtPcm, pcmChannels,pcmSampleRate, pcmBitRate);
                } else if (recordExtMp3 != null) {
                    if (renderer.setRecordExtraMp3(recordExtMp3, recordOffsetUs) == null) {
                        LSOLog.e(  "设置外部mp3音频错误, 请检查下您的音频文件是否正常.");
                    }
                }

                if (extCameraLayer != null) {
                    renderer.setExtCameraLayer(extCameraLayer);
                }
                if (pauseRecord || isPauseRecord) {
                    renderer.pauseRecordDrawPad();
                }
                if (isPauseRefresh) {
                    renderer.pauseRefreshDrawPad();
                }
                renderer.adjustEncodeSpeed(1.0f);

                LSOLog.i(  "starting run drawpad  thread...");
                ret = renderer.startDrawPad();

                isCameraOpened = ret;
                if (!ret) { // 用中文注释.
                    LSOLog.e( "开启 drawPad 失败, 或许是您之前的DrawPad没有Stop, 或者传递进去的surface对象已经被系统Destory!!,"
                            + "请检测您 的代码或直接拷贝我们的代码过去,在我们代码基础上修改参数;\n");
                } else {
                    renderer.setDisplaySurface(textureRenderView, new Surface(
                            mSurfaceTexture));
                }
            }
        } else {
            LSOLog.w(  "开启 drawPad 失败, 您设置的宽度和高度是:" + padWidth + " x "
                    + padHeight);
        }
        return ret;
    }

    /**
     * 开始预览, 应在setupDrawPad之后调用.
     */
    public void startPreview() {
        if (renderer != null) {
            renderer.resumeRefreshDrawPad();
        }
        isPauseRefresh = false;
    }

    /**
     * 暂停预览
     */
    public void pausePreview() {
        if (renderer != null) {
            renderer.pauseRefreshDrawPad();
        }
        isPauseRefresh = true;
    }

    /**
     * 恢复预览
     * <p>
     * 不可用来暂停后跳入到别的Activity中. 如果您要跳入到别的Activity, 则应该这里 {@link #stopDrawPad()}
     * 在回到当前Activity的时候,
     */
    public void resumePreview() {
        if (renderer != null) {
            renderer.resumeRefreshDrawPad();
        }
        isPauseRefresh = false;
    }

    /**
     * 开始录制和恢复录制. 录制前需要你 在 setupDrawPad前,通过
     * {@link #setRealEncodeEnable(int, int, int, String)};来配置好各种参数.
     */
    public void startRecord() {
        if (renderer != null) {
            renderer.resumeRecordDrawPad();
        }
        isPauseRecord = false;
    }

    /**
     * 暂停录制 此方法仅仅在内部设置一个暂停/恢复录制的标志位;
     */
    public void pauseRecord() {
        if (renderer != null) {
            renderer.pauseRecordDrawPad();
        }
        isPauseRecord = true;
    }

    /**
     * 恢复录制. (等同于startDrawPad) (只是为了逻辑清晰而已.)
     */
    public void resumeRecord() {
        if (renderer != null) {
            renderer.resumeRecordDrawPad();
        }
        isPauseRecord = false;
    }

    /**
     * [不好理解, 不再使用]
     * drawpad流程更改为:建立容器(setupDrawPad)===>开始预览(startPreview)===>开始录制
     * (startRecord); (原来的代码一行不变, 一样可以使用)
     */
    @Deprecated
    public void pauseDrawPad() {
        if (renderer != null) {
            renderer.pauseRefreshDrawPad();
        }
        isPauseRefresh = true;
    }

    /**
     * [不好理解, 不再使用]
     * drawpad流程更改为:建立容器(setupDrawPad)===>开始预览(startPreview)===>开始录制
     * (startRecord); (原来的代码一行不变, 一样可以使用)
     */
    @Deprecated
    public void resumeDrawPad() {
        if (renderer != null) {
            renderer.resumeRefreshDrawPad();
        }
        isPauseRefresh = false;
    }

    /**
     * [不再使用] drawpad流程更改为:建立容器(setupDrawPad)===>开始预览(startPreview)===>开始录制(
     * startRecord); (原来的代码一行不变, 一样可以使用)
     */
    @Deprecated
    public void pauseDrawPadRecord() {
        if (renderer != null) {
            renderer.pauseRecordDrawPad();
        }
        isPauseRecord = true;
    }

    /**
     * [不好理解, 不再使用]
     * drawpad流程更改为:建立容器(setupDrawPad)===>开始预览(startPreview)===>开始录制
     * (startRecord); (原来的代码一行不变, 一样可以使用)
     */
    @Deprecated
    public void resumeDrawPadRecord() {
        if (renderer != null) {
            renderer.resumeRecordDrawPad();
        }
        isPauseRecord = false;
    }

    /**
     * 是否在CameraLayer录制的同时, 录制mic的声音. 在容器建立后, 在startRecord前调用.
     * <p>
     * 此方法仅仅使用在录像的时候, 同时录制Mic的场合.录制的采样默认是44100, 码率64000, 编码为aac格式. 录制的同时,
     * 编码以音频的时间戳为参考.
     * <p>
     * 录制好后, 声音文件默认已经在您设置的编码录制中了.
     *
     * @param record 如果要设置mic录制,请务必设置为true;只是为了兼容老版本才保留这个boolean
     */
    public void setRecordMic(boolean record) {
        if (renderer != null) {
            renderer.setRecordMic(record);
        }
        isRecordMic = record;
    }

    public void setRecordExtraPcm(boolean isrecord, int channels,
                                  int samplerate, int bitrate) {
        if (renderer != null) {
            renderer.setRecordExtraPcm(isrecord, channels, samplerate, bitrate);
        } else {
            isRecordExtPcm = isrecord;
            pcmSampleRate = samplerate;
            pcmBitRate = bitrate;
            pcmChannels = channels;
        }
    }

    /**
     * 使用外部的mp3文件, 作为录制视频的音频部分. 容器建立后, 在startRecord前调用.
     *
     * 此方法增加后, 会一边播放音频, 一边录制. 我们会内部经过处理, 从而使录制的音频和视频画面 同步.
     * 适合用在 舞蹈等随着音乐节拍而舞动的画面.
     *
     * 在开始录制前调用, 您可以让用户选择几个曲子, 然后用MediaPlayer播放, 当真正开始录制时候, 在startRecord前调用这里.
     *
     * @param mp3Path mp3文件, 当前仅支持44100的采样率,2通道
     * @param endloop 当播放到文件结束后, 是否要重新循环播放, 当前暂时不支持循环, 这里仅预留.
     */
    public AudioLine setRecordExtraMp3(String mp3Path, boolean endloop) {
        if (renderer != null) {
            if (renderer.isRecording()) {
                LSOLog.e( "drawPad is recording. set mp3 Error!");
            } else {
                AudioLine line = renderer.setRecordExtraMp3(mp3Path, 0);
                if (line == null) {
                    LSOLog.e(  "设置外部mp3音频错误, 请检查下您的音频文件是否正常.");
                }
                return line;
            }
        }
        recordExtMp3 = mp3Path;
        recordOffsetUs = 0;
        return null;
    }

    /**
     * 使用外部的mp3文件, 作为录制视频的音频部分. 容器建立后, 在startRecord前调用.
     *
     * 有些场景,比如:对口型之类,或模仿人说话等场合, 人的实际说话, 会比mp3慢一点, 这里在录制的时候, 设置一个偏移量, 让他对准;
     * encodeOffsetUs 是指: 在编码的时候, 是否把当前画面提前一些, 或滞后一些; 比如提前100ms, 则这里是 设置为
     * -100*1000(负数) 如果要滞后1秒,则是设置为1000*1000(正数);
     *
     * @param mp3Path
     * @param endloop
     * @param encodeOffsetUs
     * @return
     */
    public AudioLine setRecordExtraMp3(String mp3Path, boolean endloop,
                                       long encodeOffsetUs) {
        if (renderer != null) {
            if (renderer.isRecording()) {
                LSOLog.e(  "drawPad is recording. set mp3 Error!");
            } else {
                AudioLine line = renderer.setRecordExtraMp3(mp3Path,
                        encodeOffsetUs);
                if (line == null) {
                    LSOLog.e(  "设置外部mp3音频错误, 请检查下您的音频文件是否正常.");
                    return null;
                } else {
                    recordOffsetUs = encodeOffsetUs;
                    return line;
                }
            }
        }
        recordExtMp3 = mp3Path;
        return null;
    }

    /**
     * 在开始预览后有效.
     * @return
     */
    public MicLine getMicLine() {
        if (renderer != null) {
            return renderer.getMicLine();
        } else {
            return null;
        }
    }

    public AudioLine getAudioLine() {
        if (renderer != null) {
            return renderer.getAudioLine();
        } else {
            return null;
        }
    }

    /**
     * 当是录制外部音乐, 分段录制的时候, 因为要保证音频的完整性, 这里单独拿过来. 如果您是正常录制, 则可以使用,
     * 如果是调速使用了,则建议用原音和分段合成后的声音合并(用VideoOneDo即可).
     *
     * @return
     */
    public String getRecordMusicPath() {
        if (renderer != null) {
            return renderer.getMusicRecordPath();
        } else {
            return null;
        }
    }

    /**
     * 此代码只是用在分段录制的Camera的过程中, 其他地方不建议使用.
     */
    public void segmentStart() {
        if (renderer != null) {
            renderer.segmentStart();
        }
    }

    public String segmentStop() {
        if (renderer != null) {
            return renderer.segmentStop();
        } else {
            return null;
        }
    }

    public String segmentStop(boolean pauseMp3) {
        if (renderer != null) {
            return renderer.segmentStop(pauseMp3);
        } else {
            return null;
        }
    }


    /**
     * 当前是否正在录制.
     *
     * @return
     */
    public boolean isRecording() {
        if (renderer != null){
            boolean ret=renderer.isRecording();
            return ret;
        } else
            return false;
    }

    /**
     * 当前DrawPad容器 线程是否在工作.
     *
     * @return
     */
    public boolean isRunning() {
        if (renderer != null)
            return renderer.isRunning();
        else
            return false;
    }

    /**
     * 停止DrawPad的渲染线程. 此方法执行后, DrawPad会释放内部所有Layer对象,您外界拿到的各种图层对象将无法再使用.
     */
    public void stopDrawPad() {
        if (!stopping.get()) {
            stopping.set(true);

            if (renderer != null) {
                renderer.release();
                renderer = null;
            }

            stopping.set(false);
            isCameraOpened = false;
        }
    }
    /**
     * 把当前图层放到最里层, 里面有 一个handler-loop机制, 将会在下一次刷新后执行.
     *
     * @param layer
     */
    public void bringLayerToBack(Layer layer) {
        if (renderer != null) {
            renderer.bringToBack(layer);
        }
    }

    /**
     * 把当前图层放到最外层, 里面有 一个handler-loop机制, 将会在下一次刷新后执行.
     *
     * @param layer
     */
    public void bringLayerToFront(Layer layer) {
        if (renderer != null) {
            renderer.bringToFront(layer);
        }
    }

    /**
     * 使用下面这个.
     * @param layer
     * @param position
     */
    public void changeLayerLayPosition(Layer layer, int position) {
        if (renderer != null) {
            renderer.changeLayerLayPosition(layer, position);
        }
    }

    /**
     * 设置当前图层对象layer 在DrawPad中所有图层队列中的位置, 您可以认为内部是一个ArrayList的列表, 先add进去的
     * 的position是0, 后面增加的依次是1,2,3等等
     *
     * @param layer
     * @param position
     */
    public void changeLayerPosition(Layer layer, int position) {
        if (renderer != null) {
            renderer.changeLayerLayPosition(layer, position);
        }
    }
    public void setLayerPosition(Layer layer, int position) {
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
     * 获取当前摄像头图层, 此方法只能在startDrawPad开启后调用, 如果在之前调用,则返回null;
     * 建议每次使用摄像头图层的时候,通过getCameraLayer拿到对象来设置CameraLayer的方法;比如drawpad.getCameraLayer().setXXX;
     *
     * @return
     */
    public ExtCameraLayer getCameraLayer() {
        if (renderer != null && renderer.isRunning())
            return renderer.getCameraLayer();
        else {
            LSOLog.e(  "getCameraLayer error render is not avalid");
            return null;
        }
    }

    public VideoLayer addVideoLayer(int width, int height, LanSongFilter filter) {
        if (renderer != null)
            return renderer.addVideoLayer(width, height, filter);
        else {
            LSOLog.e( "addVideoLayer error render is not avalid");
            return null;
        }
    }
    /**
     */
    public BitmapLayer addBitmapLayer(Bitmap bmp) {
        if (bmp != null) {
            if (renderer != null)
                return renderer.addBitmapLayer(bmp, null);
            else {
                LSOLog.e(  "addBitmapLayer error render is not avalid");
                return null;
            }
        } else {
            LSOLog.e(  "addBitmapLayer error, byteBuffer is null");
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
            LSOLog.e( "addYUVLayer error! render is not avalid");
            return null;
        }
    }

    /**
     * 增加一个gif图层,
     * @param resId 来自apk中drawable文件夹下的各种资源文件.
     * @return
     */
    public GifLayer addGifLayer(int resId) {
        if (renderer != null)
            return renderer.addGifLayer(resId);
        else {
            LSOLog.e(  "addGifLayer error! render is not avalid");
            return null;
        }
    }

    /**
     * 增加一个mv图层, mv图层分为两个文件, 一个是彩色的, 一个黑白
     *
     * @return
     */
    public MVLayer2 addMVLayer(LSOMVAsset2 mvAsset) {
        if (renderer != null && mvAsset!=null)
            return renderer.addMVLayer(mvAsset);
        else {
            LSOLog.e( "addMVLayer error render is not avalid");
            return null;
        }
    }

    /**
     * @return 返回ViewLayer对象.
     */
    public ViewLayer addViewLayer() {
        if (renderer != null)
            return renderer.addViewLayer();
        else {
            LSOLog.e( "addViewLayer error render is not avalid");
            return null;
        }
    }
    /**
     * 放大缩小画面
     */
    private boolean isZoomEvent = false;
    private float pointering; // 两个手指按下.
    private doFousEventListener mDoFocusListener;
    // ----------------------------------------------
    private boolean isCheckBitRate = true;
    private boolean isCheckPadSize = true;
    private boolean isEnableTouch = true;

    /**
     * 从渲染线程列表中移除并销毁这个Layer; 注意:此方法一定在 startDrawPad之后,在stopDrawPad之前调用.
     */
    public void removeLayer(Layer layer) {
        if (layer != null) {
            if (renderer != null)
                renderer.removeLayer(layer);
            else {
                LSOLog.e(  "removeLayer error render is not avalid");
            }
        }
    }


    float x1 = 0;
    float x2 = 0;
    float y1 = 0;
    float y2 = 0;


    public boolean onTouchEvent(MotionEvent event) {

        if (!isEnableTouch) { // 如果禁止了touch事件,则直接返回false;
            return false;
        }
        if(getCameraLayer()==null){
            return false;
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            // 手指压下屏幕
            case MotionEvent.ACTION_DOWN:
                isZoomEvent = false;
                x1 = event.getX();
                y1 = event.getY();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                // 计算两个手指间的距离
                if (isRunning()) {
                    pointering = spacing(event);
                    isZoomEvent = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isZoomEvent && isRunning()) {
                    ExtCameraLayer layer = getCameraLayer();
                    if (layer != null && event.getPointerCount() >= 2) {// 触屏两个点时才执行
                        float endDis = spacing(event);// 结束距离
                        int scale = (int) ((endDis - pointering) / 10f); // 每变化10f
                        // zoom变1
                        if (scale >= 1 || scale <= -1) {
                            int zoom = layer.getZoom() + scale;
                            layer.setZoom(zoom);
                            pointering = endDis;
                        }
                    }
                }
                break;
            // 手指离开屏幕
            case MotionEvent.ACTION_UP:
                if(isRunning()){
                    if (!isZoomEvent) {
                        ExtCameraLayer layer = getCameraLayer();
                        if (layer != null) {
                            float x = event.getX();
                            float y = event.getY();
                            if (renderer != null) {
                                x = renderer.getTouchX(x);
                                y = renderer.getTouchY(y);
                            }
                            layer.doFocus((int) x, (int) y);

                            if (mDoFocusListener != null) {
                                mDoFocusListener.onFocus((int) x, (int) y);
                            }
                        }
                    }
                }
                isZoomEvent = false;
                break;
        }
        return true;
    }

    private float spacing(MotionEvent event) {
        if (event == null) {
            return 0;
        }
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 当用户按下Drawpad画面后, 会去聚焦,这里返回一个监听,显示一个聚焦的动画.
     *
     * @param mCameraFocusListener
     */
    public void setCameraFocusListener(doFousEventListener mCameraFocusListener) {
        this.mDoFocusListener = mCameraFocusListener;
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

    public void setEnableTouch(boolean enable) {
        isEnableTouch = enable;
    }


    public interface doFousEventListener {
        void onFocus(int x, int y);
    }



}
