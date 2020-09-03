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
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.box.onDrawPadSnapShotListener;
import com.lansosdk.box.onDrawPadThreadProgressListener;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.lansosdk.LanSongFilter.LanSongFilter;

public class DrawPadCameraView extends FrameLayout {

    private DrawPadCameraRunnable renderer;


    static final int AR_4_3_FIT_PARENT = 5;
    private static final boolean VERBOSE = false;
    private static boolean isCameraOpened = false;
    private int mVideoRotationDegree;
    private TextureRenderView mTextureRenderView;
    private SurfaceTexture surfaceTexture = null;
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
    private String encodeOutput = null; // 编码输出路径
    private onViewAvailable mViewAvailable = null;
    private onDrawPadSizeChangedListener mSizeChangedCB = null;
    private onDrawPadRunTimeListener drawpadRunTimeListener = null;
    // ----------------------------
    private onDrawPadProgressListener drawpadProgressListener = null;
    private onDrawPadRecordCompletedListener drawPadRecordCompletedListener=null;
    private onDrawPadThreadProgressListener drawPadThreadProgressListener = null;
    private onDrawPadSnapShotListener drawPadSnapShotListener = null;
    private onDrawPadOutFrameListener drawPadOutFrameListener = null;
    private int outFrameWidth;
    private int outFrameHeight;
    private int outFrameType;
    private boolean frameListenerInDrawPad = false;
    private onDrawPadCompletedListener drawPadCompletedListener = null;
    private onDrawPadErrorListener drawPadErrorListener = null;
    private CameraLayer extCameraLayer = null;
    private boolean editModeVideo = false;
    /**
     * 是否也录制mic的声音.
     */
    private boolean recordMic = false;
    private boolean isPauseRefresh = false;
    private boolean isPauseRecord = false;
    private boolean recordExtPcm = false; // 是否使用外面的pcm输入.
    private int pcmSampleRate = 44100;
    private int pcmBitRate = 64000;
    private int pcmChannels = 2; // 音频格式. 音频默认是双通道.
    private String recordExtMp3 = null;
    private long recordOffsetUs = 0;
    private AtomicBoolean stoping = new AtomicBoolean(false);
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

        mTextureRenderView.setDisplayRatio(0);

        View renderUIView = mTextureRenderView.getView();
        LayoutParams lp = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT, Gravity.CENTER);
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
        if(surfaceTexture !=null){
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

    public void setDrawPadSize(int width, int height,onDrawPadSizeChangedListener cb) {

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


    public void setOnDrawPadThreadProgressListener(onDrawPadThreadProgressListener listener) {
        if (renderer != null) {
            renderer.setDrawPadThreadProgressListener(listener);
        }
        drawPadThreadProgressListener = listener;
    }
    public void setOnDrawPadSnapShotListener(onDrawPadSnapShotListener listener) {
        if (renderer != null) {
            renderer.setDrawPadSnapShotListener(listener);
        }
        drawPadSnapShotListener = listener;
    }


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

    public void toggleSnatShot() {
        if (drawPadSnapShotListener != null && renderer != null && renderer.isRunning()) {
            renderer.toggleSnapShot(padWidth, padHeight);
        } else {
            LSOLog.e( "toggle snap shot failed!!!");
        }
    }

    public void toggleSnatShot(int width, int height) {
        if (drawPadSnapShotListener != null && renderer != null
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
        drawPadCompletedListener = listener;
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
            editModeVideo = is;
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
    public boolean setupDrawPad() {
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

        if (surfaceTexture != null && renderer == null && padWidth > 0 && padHeight > 0) {

            renderer = new DrawPadCameraRunnable(getContext(), padWidth,padHeight); // <----从这里去建立DrawPad线程.
            renderer.setCameraParam(isFrontCam, initFilter);

            DisplayMetrics dm = getResources().getDisplayMetrics();

            if (dm != null && padWidth == dm.widthPixels
                    && padHeight == dm.heightPixels) {
                renderer.setFullScreen(true);
            }

            if (renderer != null) {
                renderer.setDisplaySurface(mTextureRenderView, new Surface(
                        surfaceTexture));

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
                if (editModeVideo) {
                    renderer.setEditModeVideo(true);
                }
                // 设置DrawPad处理的进度监听, 回传的currentTimeUs单位是微秒.
                renderer.setDrawPadSnapShotListener(drawPadSnapShotListener);
                renderer.setDrawpadOutFrameListener(outFrameWidth,outFrameHeight, outFrameType, drawPadOutFrameListener);
                renderer.setDrawPadProgressListener(drawpadProgressListener);
                renderer.setDrawPadCompletedListener(drawPadCompletedListener);
                renderer.setDrawPadThreadProgressListener(drawPadThreadProgressListener);
                renderer.setOutFrameInDrawPad(frameListenerInDrawPad);
                renderer.setDrawPadRunTimeListener(drawpadRunTimeListener);
                renderer.setDrawPadRecordCompletedListener(drawPadRecordCompletedListener);
                renderer.setDrawPadErrorListener(drawPadErrorListener);
                if (recordMic) {
                    renderer.setRecordMic(true);
                } else if (recordExtPcm) {
                    renderer.setRecordExtraPcm(true, pcmChannels,pcmSampleRate, pcmBitRate);
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

                LSOLog.d("starting run draw pad  thread...");
                ret = renderer.startDrawPad();

                isCameraOpened = ret;
                if (!ret) { // 用中文注释.
                    LSOLog.e( "开启 drawPad 失败, 或许是您之前的DrawPad没有Stop, 或者传递进去的surface对象已经被系统Destory!!,"
                                    + "请检测您 的代码或直接拷贝我们的代码过去,在我们代码基础上修改参数;\n");
                } else {
                    renderer.setDisplaySurface(mTextureRenderView, new Surface(surfaceTexture));
                }
            }
        } else {
            LSOLog.w(  "开启 drawPad 失败, 您设置的宽度和高度是:" + padWidth + " x " + padHeight);
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
        recordMic = record;
    }

    public void setRecordExtraPcm(boolean isrecord, int channels,
                                  int samplerate, int bitrate) {
        if (renderer != null) {
            renderer.setRecordExtraPcm(isrecord, channels, samplerate, bitrate);
        } else {
            recordExtPcm = isrecord;
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
        if (!stoping.get()) {
            stoping.set(true);

            if (renderer != null) {
                renderer.release();
                renderer = null;
            }

            stoping.set(false);
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
            LSOLog.e("aeRenderer maybe is running. your setting is not available!!");
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
     * @return
     */
    public MVLayer2 addMVLayer(LSOMVAsset2 mvAsset) {
        if (renderer != null)
            return renderer.addMVLayer(mvAsset);
        else {
            LSOLog.e( "addMVLayer error render is null");
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
            LSOLog.e( "addViewLayer error render is null");
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
            LSOLog.e(  "addCanvasLayer error render is null");
            return null;
        }
    }

    // ------------------------------------

    /**
     * 从渲染线程列表中移除并销毁这个Layer; 注意:此方法一定在 startDrawPad之后,在stopDrawPad之前调用.
     */
    public void removeLayer(Layer layer) {
        if (layer != null) {
            if (renderer != null)
                renderer.removeLayer(layer);
            else {
                LSOLog.e(  "removeLayer error render is null");
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
                        }else if(!isSlideToRight && slideFilterPercent<0.3f) {
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

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                              int width, int height) {

            surfaceTexture = surface;
            padHeight = height;
            padWidth = width;
            if (mViewAvailable != null) {
                mViewAvailable.viewAvailable(DrawPadCameraView.this);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                                int width, int height) {
            surfaceTexture = surface;
            padHeight = height;
            padWidth = width;

            if (mSizeChangedCB != null) {
                mSizeChangedCB.onSizeChanged(width, height);
            }
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            surfaceTexture = null;
            padHeight = 0;
            padWidth = 0;
            stopDrawPad(); // 可以在这里增加以下. 这样当Texture销毁的时候, 停止DrawPad
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    }



}
