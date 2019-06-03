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
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.widget.FrameLayout;

import com.lansosdk.box.AudioLine;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.CameraLayer;
import com.lansosdk.box.CanvasLayer;
import com.lansosdk.box.DataLayer;
import com.lansosdk.box.DrawPadCameraRunnable;
import com.lansosdk.box.GifLayer;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.Layer;
import com.lansosdk.box.MVLayer;
import com.lansosdk.box.MicLine;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.ViewLayer;
import com.lansosdk.box.YUVLayer;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadErrorListener;
import com.lansosdk.box.onDrawPadOutFrameListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.box.onDrawPadRecordCompletedListener;
import com.lansosdk.box.onDrawPadRunTimeListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.box.onDrawPadSnapShotListener;
import com.lansosdk.box.onDrawPadThreadProgressListener;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.lansosdk.LanSongFilter.LanSongFilter;

/**
 * 处理预览和实时保存的View, 继承自FrameLayout.
 * <p>
 * 适用在增加到UI界面中, 一边预览,一边实时保存的场合.
 */
public class DrawPadCameraView extends FrameLayout {

    /**
     * 把画面的宽度等于父view的宽度, 高度按照4:3的形式显示.
     */
    static final int AR_4_3_FIT_PARENT = 5;
    private static final boolean VERBOSE = false;
    private static boolean isCameraOpened = false;
    private int mVideoRotationDegree;
    private TextureRenderView mTextureRenderView;
    private DrawPadCameraRunnable renderer;
    private SurfaceTexture mSurfaceTexture = null;
    private int encWidth, encHeight, encFrameRate;
    private int encBitRate = 0;
    /**
     * 经过宽度对齐到手机的边缘后, 缩放后的宽高,作为drawpad(容器)的宽高.
     */
    private int padWidth, padHeight;
    /**
     * 摄像头是否是前置摄像头
     */
    private boolean isFrontCam = false;
    /**
     * 初始化CameraLayer的时候, 是否需要设置滤镜. 当然您也可以在后面实时切换为别的滤镜.
     */
    private LanSongFilter initFilter = null;
    private float encodeSpeed = 1.0f;
    // private FocusImageView focusView;
    // /**
    // * 把外界的用来聚焦的动画
    // * @param view
    // */
    // public void setFocusView(FocusImageView view)
    // {
    // focusView=view;
    // }
    private String encodeOutput = null; // 编码输出路径
    private onViewAvailable mViewAvailable = null;
    private onDrawPadSizeChangedListener mSizeChangedCB = null;
    private onDrawPadRunTimeListener drawpadRunTimeListener = null;
    // ----------------------------
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
    private CameraLayer extCameraLayer = null;
    private boolean isEditModeVideo = false;
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
    private AtomicBoolean isStoping = new AtomicBoolean(false);
    /**
     * 放大缩小画面
     */
    private boolean isZoomEvent = false;
    private boolean isSlideEvent = false;
    private boolean isSlideToRight = false;
    private float pointering; // 两个手指按下.
    private doFousEventListener mDoFocusListener;
    // ----------------------------------------------
    private boolean isCheckBitRate = true;
    private boolean isCheckPadSize = true;
    private boolean isEnableTouch = true;
    public DrawPadCameraView(Context context) {
        super(context);
        initVideoView(context);
    }
    public DrawPadCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView(context);
    }
    public DrawPadCameraView(Context context, AttributeSet attrs,
                             int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DrawPadCameraView(Context context, AttributeSet attrs,
                             int defStyleAttr, int defStyleRes) {
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

        mTextureRenderView.setDispalyRatio(0);

        View renderUIView = mTextureRenderView.getView();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        renderUIView.setLayoutParams(lp);
        addView(renderUIView);
        mTextureRenderView.setVideoRotation(mVideoRotationDegree);

        mTextureRenderView.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return onTouchEvent(event);
            }
        });
    }

    /**
     * 获取当前View的 宽度
     *
     * @return
     */
    public int getViewWidth() {
        return padWidth;
    }

    /**
     * 获得当前View的高度.
     *
     * @return
     */
    public int getViewHeight() {
        return padHeight;
    }

    public int getDrawPadWidth() {
        return padWidth;
    }

    /**
     * 获得当前View的高度.
     *
     * @return
     */
    public int getDrawPadHeight() {
        return padHeight;
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

    @Deprecated
    public void setCameraParam(boolean front, LanSongFilter filter,boolean beauful) {
        isFrontCam = front;
        initFilter = filter;
    }

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
     * 调整在录制时的速度, 比如你想预览的时候是正常的, 录制好后, 一部分要快进或慢放,则可以在这里设置 支持在任意时刻变速;
     * 甚至你可以设置一个按钮, 长按下的时候, 加快或放慢, 松开时正常. 当前暂时不支持音频, 只是视频的加减速, 请注意!!!
     * <p>
     * 1,如果不录制外部音频(麦克风), 则速度建议; 建议5个等级: 0.25f,0.5f,1.0f,1.5f,2.0f; 其中 0.25是放慢2倍;
     * 0.5是放慢一倍; 1.0是采用和预览同样的速度; 2.0是加快1倍.
     * <p>
     * 2,如果录制外部音频(麦克风),则速度范围是0.5--2.0; 0.5是放慢一倍, 1.0是原速; 2.0是放大一倍;
     * <p>
     * 3,如果录制mp3,则只是调整视频画面的速度, mp3在录制完成后生成的文件速度不变.
     *
     * @param speed 速度系数,
     */
    public void adjustEncodeSpeed(float speed) {
        if (renderer != null) {
            renderer.adjustEncodeSpeed(speed);
        }
        encodeSpeed = speed;
    }

    /**
     * 设置录制视频的宽高, 建议和预览的宽高成比例, 比如预览的全屏是16:9则设置的编码宽度和高度也要16:9;
     * 如果是18:9,则这里的encW:encH也要18:9; 从而保证录制后的视频不变形;
     *
     * @param encW    录制宽度
     * @param encH    录制高度
     * @param encBr   录制bitrate,
     * @param encFr   录制帧率
     * @param outPath 录制 的保存路径. 注意:这个路径在分段录制功能时无效.即调用 {@link #segmentStart()}时无效
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
    public void setExtCameraLayer(CameraLayer layer){
        extCameraLayer=layer;
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

    /**
     * 设置当前DrawPad的宽度和高度,并把宽度自动缩放到父view的宽度,然后等比例调整高度.
     * <p>
     * 如果在父view中已经预设好了希望的宽高,则可以不调用这个方法,直接
     * 可以通过 {@link #getViewHeight()} 和 {@link #getViewWidth()}来得到当前view的宽度和高度.
     * <p>
     * <p>
     * 注意: 这里的宽度和高度,会根据手机屏幕的宽度来做调整,默认是宽度对齐到手机的左右两边, 然后调整高度,
     * 把调整后的高度作为DrawPad渲染线程的真正宽度和高度. 注意: 此方法需要在
     * 前调用. 比如设置的宽度和高度是480,480,
     * 而父view的宽度是等于手机分辨率是1080x1920,则DrawPad默认对齐到手机宽度1080,然后把高度也按照比例缩放到1080.
     *
     * @param width  DrawPad宽度
     * @param height DrawPad高度
     * @param cb     设置好后的回调, 注意:如果预设值的宽度和高度经过调整后
     *               已经和父view的宽度和高度一致,则不会触发此回调(当然如果已经是希望的宽高,您也不需要调用此方法).
     */
    public void setDrawPadSize(int width, int height,
                               onDrawPadSizeChangedListener cb) {

        if (width != 0 && height != 0 && cb != null) {

            if(padWidth==0 || padHeight==0){  //直接重新布局UI
                mTextureRenderView.setVideoSize(width, height);
                mTextureRenderView.setVideoSampleAspectRatio(1, 1);
                mSizeChangedCB = cb;
                requestLayout();
            }else{
                float setAcpect = (float) width / (float) height;
                float setViewacpect = (float) padWidth / (float) padHeight;

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
     * 当前drawpad容器运行了多长时间, 仅供参考使用. 没有特别的意义. 内部每渲染一帧, 则会回调这里.
     * 仅仅作为drawpad容器运行时间的参考, 如果你要看当前视频图层的运行时间,则应设置图层的监听,而不是容器运行时间的监听, 可以通过
     * {@link #resetDrawPadRunTime(long)} 来复位这个时间.
     *
     * @param li
     */
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
     *
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
     * 设置 获取当前DrawPad这一帧的画面的监听, 注意,因为Camera是全速运行, 会根据手机cpu的速度来执行, 可能一秒钟执行50帧,
     * 或60帧, 也可能一秒钟执行30帧. 设置截图监听,当截图完成后, 返回当前图片的btimap格式的图片. 此方法工作在主线程.
     * 请不要在此方法里做图片的处理,以免造成拥堵; 建议获取到bitmap后,放入到一个链表中,在外面或另开一个线程处理.
     */
    public void setOnDrawPadSnapShotListener(onDrawPadSnapShotListener listener) {
        if (renderer != null) {
            renderer.setDrawpadSnapShotListener(listener);
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
     * <p>
     * 此方法,仅在前台工作时有效. (注意:截取的仅仅是各种图层的内容, 不会截取DrawPad的黑色背景)
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
     * 容器的大小是您设置的DrawPad的size(如果是全屏,则是布局的大小)
     *
     * 建立后, 里面自动增加Camera图层, 您需要startPreview则开始预览;
     *
     * @return
     */
    public boolean setupDrawpad() {
        if (renderer == null) {
            pauseRecord();
            pausePreview();
            return startDrawPad(true);
        } else {
            return false;
        }
    }

    /**
     * [不好理解, 不再使用] 开始DrawPad的渲染线程, 阻塞执行, 直到DrawPad真正开始执行后才退出当前方法.
     * <p>
     * 可以先通过 {@link #setDrawPadSize(int, int, onDrawPadSizeChangedListener)}
     * 来设置宽高,然后在回调中执行此方法. 如果您已经在xml中固定了view的宽高,则可以直接调用这里, 无需再设置DrawPadSize
     *
     * @return
     */
    @Deprecated
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

            renderer = new DrawPadCameraRunnable(getContext(), padWidth,
                    padHeight); // <----从这里去建立DrawPad线程.


            renderer.setCameraParam(isFrontCam, initFilter);

            DisplayMetrics dm = getResources().getDisplayMetrics();

            if (dm != null && padWidth == dm.widthPixels
                    && padHeight == dm.heightPixels) {
                renderer.setFullScreen(true);
            }

            if (renderer != null) {
                renderer.setDisplaySurface(mTextureRenderView, new Surface(
                        mSurfaceTexture));

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
                renderer.setDrawpadSnapShotListener(drawpadSnapShotListener);
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
                renderer.adjustEncodeSpeed(encodeSpeed);

                LSOLog.i(  "starting run drawpad  thread...");
                ret = renderer.startDrawPad();

                isCameraOpened = ret;
                if (!ret) { // 用中文注释.
                    LSOLog.e( "开启 drawPad 失败, 或许是您之前的DrawPad没有Stop, 或者传递进去的surface对象已经被系统Destory!!,"
                                    + "请检测您 的代码或直接拷贝我们的代码过去,在我们代码基础上修改参数;\n");
                } else {

//                    if(slideFilterArray!=null){
//                        aeRenderer.getCameraLayer().setSlideFilterArray(slideFilterArray);
//                    }

                    renderer.setDisplaySurface(mTextureRenderView, new Surface(
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
     * <p>
     * 不可用来暂停后跳入到别的Activity中. 如果您要跳入到别的Activity, 则应该这里 {@link #stopDrawPad()}
     * 在回到当前Activity的时候, 调用 {@link #setupDrawpad()}
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
     * 在回到当前Activity的时候, 调用 {@link #setupDrawpad()}
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
     * <p>
     * 一般用在开始录制后的暂停; 不可用来暂停后跳入到别的Activity中.
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
        if (!isStoping.get()) {
            isStoping.set(true);

            if (renderer != null) {
                renderer.release();
                renderer = null;
            }

            isStoping.set(false);
            isCameraOpened = false;
        }
    }

    @Deprecated
    public String stopDrawPad2() {
        String ret = null;
        if (renderer != null) {
            renderer.release();
            ret = renderer.getMusicRecordPath();
            renderer = null;
        }
        isCameraOpened = false;
        return ret;
    }

    /**
     * 作用同 {@link #setDrawPadSize(int, int, onDrawPadSizeChangedListener)},
     * 只是有采样宽高比, 如用我们的VideoPlayer则使用此方法, 建议用
     * {@link #setDrawPadSize(int, int, onDrawPadSizeChangedListener)}
     *
     * @param width
     * @param height
     * @param sarnum 如mediaplayer设置后,可以为1,
     * @param sarden 如mediaplayer设置后,可以为1,
     * @param cb
     */
    public void setDrawPadSize(int width, int height, int sarnum, int sarden,
                               onDrawPadSizeChangedListener cb) {
        if (width != 0 && height != 0) {
            if (mTextureRenderView != null) {
                mTextureRenderView.setVideoSize(width, height);
                mTextureRenderView.setVideoSampleAspectRatio(sarnum, sarden);
            }
            mSizeChangedCB = cb;
            requestLayout();
        }
    }

    /**
     * 直接设置容器的宽高, 不让他自动缩放.
     * <p>
     * 要在容器开始前调用.
     *
     * @param width
     * @param height
     */
    public void setDrawpadSizeDirect(int width, int height) {
        padWidth = width;
        padHeight = height;
        if (renderer != null) {
            LSOLog.e(
                    "aeRenderer maybe is running. your setting is not available!!");
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
     *
     * @param layer
     * @param position
     */
    @Deprecated
    public void changeLayerLayPosition(Layer layer, int position) {
        if (renderer != null) {
            renderer.changeLayerLayPosition(layer, position);
        }
    }

    /**
     * 设置当前图层对象layer 在DrawPad中所有图层队列中的位置, 您可以认为内部是一个ArrayList的列表, 先add进去的
     * 的position是0, 后面增加的依次是1,2,3等等
     * 可以通过Layer的getIndexLayerInDrawPad属性来获取当前图层的位置.
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
     * 获取当前摄像头图层, 此方法只能在startDrawPad开启后调用, 如果在之前调用,则返回null;
     * 建议每次使用摄像头图层的时候,通过getCameraLayer拿到对象来设置CameraLayer的方法;比如drawpad.getCameraLayer().setXXX;
     *
     * @return
     */
    public CameraLayer getCameraLayer() {
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
     * 获取一个BitmapLayer 注意:此方法一定在 startDrawPad之后,在stopDrawPad之前调用.
     *
     * @param bmp 图片的bitmap对象,可以来自png或jpg等类型,这里是通过BitmapFactory.
     *            decodeXXX的方法转换后的bitmap对象.
     * @return 一个BitmapLayer对象
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
     * 获取一个DataLayer的图层, 数据图层, 是一个RGBA格式的数据, 内部是一个RGBA格式的图像.
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
                LSOLog.e(  "addDataLayer error render is not avalid");
                return null;
            }
        } else {
            LSOLog.e(  "addDataLayer error, data size is error");
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
     * <p>
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
            LSOLog.e(  "addGifLayer error! render is not avalid");
            return null;
        }
    }

    /**
     * 增加一个mv图层, mv图层分为两个文件, 一个是彩色的, 一个黑白
     *
     * @param srcPath
     * @param maskPath
     * @return
     */
    public MVLayer addMVLayer(String srcPath, String maskPath) {
        if (renderer != null)
            return renderer.addMVLayer(srcPath, maskPath);
        else {
            LSOLog.e( "addMVLayer error render is not avalid");
            return null;
        }
    }

    /**
     * 获得一个 ViewLayer,您可以在获取后,仿照我们的例子,来为增加各种UI空间. 注意:此方法一定在
     * startDrawPad之后,在stopDrawPad之前调用.
     *
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
     * 获得一个 CanvasLayer 注意:此方法一定在 startDrawPad之后,在stopDrawPad之前调用.
     *
     * @return
     */
    public CanvasLayer addCanvasLayer() {
        if (renderer != null)
            return renderer.addCanvasLayer();
        else {
            LSOLog.e(  "addCanvasLayer error render is not avalid");
            return null;
        }
    }

    // ------------------------------------

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
            LSOLog.e( "addYUVLayer error render is not avalid");
            return null;
        }
    }

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

    private List<LanSongFilter> slideFilterArray;
    public void setSlideFilterArray(List<LanSongFilter> filters){
        if(renderer!=null && getCameraLayer()!=null){
            getCameraLayer().setSlideFilterArray(filters);
        }
        slideFilterArray=filters;
    }

    float x1 = 0;
    float x2 = 0;
    float y1 = 0;
    float y2 = 0;
    float slideFilterPercent;


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
                    CameraLayer layer = getCameraLayer();
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
                else if(getCameraLayer().isSlideFilterEnable() ){


                    x2 = event.getX();
                    y2 = event.getY();
                    if(Math.abs(x2-x1)>10){
                        isSlideEvent=true;
                        float percent=x2*1.0f  / (float) getWidth();
                        if(percent>1.0f){
                            percent=1.0f;
                        }
                        slideFilterPercent=percent;
                        if(x2>x1){ //向右滑动;
                            isSlideToRight=true;
                            getCameraLayer().slideFilterToRight(percent);
                        }else{  //向左滑动;
                            isSlideToRight=false;
                            getCameraLayer().slideFilterToLeft(percent);
                        }
                    }
                }
                break;
            // 手指离开屏幕
            case MotionEvent.ACTION_UP:
                if(isRunning()){
                    if(isSlideEvent){

                        if(isSlideToRight && slideFilterPercent>0.7f){
                            getCameraLayer().didSlideFilter();
                        }else if(isSlideToRight==false && slideFilterPercent<0.3f) {
                            getCameraLayer().didSlideFilter();
                        }else {
                            getCameraLayer().cancelSlideFilter();
                        }

                        slideFilterPercent=0;

                    }else if (!isZoomEvent) {
                        CameraLayer layer = getCameraLayer();
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
                isSlideEvent=false;
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

    public interface onViewAvailable {
        void viewAvailable(DrawPadCameraView v);
    }

    public interface doFousEventListener {
        void onFocus(int x, int y);
    }

    private class SurfaceCallback implements SurfaceTextureListener {

        /**
         * Invoked when a {@link TextureView}'s SurfaceTexture is ready for use.
         * 当画面呈现出来的时候, 会调用这个回调.
         * <p>
         * 当Activity跳入到别的界面后,这时会调用
         * {@link #onSurfaceTextureDestroyed(SurfaceTexture)} 销毁这个Texture,
         * 如果您想在再次返回到当前Activity时,再次显示预览画面, 可以在这个方法里重新设置一遍DrawPad,并再次startDrawPad
         * <p>
         * 有些手机在从当前Activity进入另一个activity,再次返回时, 不会调用这里,比如魅族MX5.
         */

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                              int width, int height) {

            mSurfaceTexture = surface;
            padHeight = height;
            padWidth = width;
            if (mViewAvailable != null) {
                mViewAvailable.viewAvailable(DrawPadCameraView.this);
            }
        }

        /**
         * Invoked when the {@link SurfaceTexture}'s buffers size changed.
         * 当创建的TextureView的大小改变后, 会调用回调.
         * <p>
         * 当您本来设置的大小是480x480,而DrawPad会自动的缩放到父view的宽度时,会调用这个回调,提示大小已经改变,
         * 这时您可以开始startDrawPad 如果你设置的大小更好等于当前Texture的大小,则不会调用这个, 详细的注释见
         */
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                                int width, int height) {
            mSurfaceTexture = surface;
            padHeight = height;
            padWidth = width;

            if (mSizeChangedCB != null) {
                mSizeChangedCB.onSizeChanged(width, height);
            }
        }

        /**
         * Invoked when the specified {@link SurfaceTexture} is about to be
         * destroyed.
         * <p>
         * 当您跳入到别的Activity的时候, 会调用这个,销毁当前Texture;
         */
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            mSurfaceTexture = null;
            padHeight = 0;
            padWidth = 0;
            stopDrawPad(); // 可以在这里增加以下. 这样当Texture销毁的时候, 停止DrawPad
            return false;
        }

        /**
         * Invoked when the specified {@link SurfaceTexture} is updated through
         * {@link SurfaceTexture#updateTexImage()}.
         * <p>
         * 每帧如果更新了, 则会调用这个!!!!
         */
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    }



}
