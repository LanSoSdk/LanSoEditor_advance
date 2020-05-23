package com.lansosdk.videoeditor;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.widget.FrameLayout;

import com.lansosdk.LanSongFilter.LanSongFilter;
import com.lansosdk.box.AudioLine;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.CanvasLayer;
import com.lansosdk.box.DataLayer;
import com.lansosdk.box.DrawPadUpdateMode;
import com.lansosdk.box.DrawPadViewRender;
import com.lansosdk.box.GifLayer;
import com.lansosdk.box.LSOAECompositionLayer;
import com.lansosdk.box.LSOAeCompositionAsset;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LSOMVAsset;
import com.lansosdk.box.LSOPhotoAlbumAsset;
import com.lansosdk.box.LSOPhotoAlbumLayer;
import com.lansosdk.box.Layer;
import com.lansosdk.box.MVLayer;
import com.lansosdk.box.SubLayer;
import com.lansosdk.box.TextureLayer;
import com.lansosdk.box.TwoVideoLayer;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.VideoLayer2;
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

public class DrawPadView extends FrameLayout {

    static final int AR_ASPECT_FIT_PARENT = 0; // without clip
    private static final boolean VERBOSE = false;
    private TextureRenderView mTextureRenderView;
    private DrawPadViewRender renderer;
    protected float padBGRed =0.0f;
    protected float padBGGreen =0.0f;
    protected float padBGBlur =0.0f;
    protected float padBGAlpha =1.0f;


    private SurfaceTexture mSurfaceTexture = null;
    private boolean isUseMainPts = false;
    private int encWidth, encHeight, encFrameRate;
    private int encBitRate = 0;
    private float encodeSpeed = 1.0f;
    private int drawPadWidth, drawPadHeight;
    private String encodeOutput = null; // 编码输出路径
    private DrawPadUpdateMode mUpdateMode = DrawPadUpdateMode.ALL_VIDEO_READY;
    private int mAutoFlushFps = 0;
    private onViewAvailable mViewAvailable = null;
    private onDrawPadSizeChangedListener mSizeChangedCB = null;
    private onDrawPadRunTimeListener drawPadRunTimeListener = null;
    private onDrawPadProgressListener drawPadProgressListener = null;
    private onDrawPadThreadProgressListener drawPadThreadProgressListener = null;
    private onDrawPadSnapShotListener drawPadSnapShotListener = null;
    private onDrawPadOutFrameListener drawPadPreviewFrameListener = null;
    private int previewFrameWidth;
    private int previewFrameHeight;
    private int previewFrameType;
    private boolean frameListenerInDrawPad = false;
    private onDrawPadCompletedListener drawpadCompletedListener = null;
    private onDrawPadErrorListener drawPadErrorListener = null;
    private boolean editModeVideo = false;
    /**
     * 是否也录制mic的声音.
     */
    private boolean isRecordMic = false;
    /**
     * 暂停和恢复有3种方法, 分别是:刷新,预览,录制;
     * <p>
     * 暂停刷新,则都暂停了, 暂停录制则只暂停录制,刷新和预览一样在走动. 暂停预览,则录制还在继续,但只是录制了同一副画面.
     */
    private boolean isPauseRefreshDrawPad = false;
    private boolean isPausePreviewDrawPad = false;
    private boolean isPauseRecord = false;
    private boolean isRecordExtPcm = false; // 是否使用外面的pcm输入.
    private int pcmSampleRate = 44100;
    private int pcmBitRate = 64000;
    private int pcmChannels = 2; // 音频格式. 音频默认是双通道.
    // ----------------------------------------------
    private boolean isCheckBitRate = true;
    private boolean isCheckPadSize = true;

    public DrawPadView(Context context) {
        super(context);
        initVideoView(context);
    }
    public DrawPadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView(context);
    }

    public DrawPadView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DrawPadView(Context context, AttributeSet attrs, int defStyleAttr,
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
     * 设置DrawPad的刷新模式,默认 {@link DrawPadUpdateMode#ALL_VIDEO_READY};
     *
     * @param mode
     * @param autoFps //自动刷新的参数,每秒钟刷新几次(即视频帧率).当自动刷新的时候有用, 不是自动,则不起作用.
     */
    public void setUpdateMode(DrawPadUpdateMode mode, int autoFps) {
        mAutoFlushFps = autoFps;
        mUpdateMode = mode;
    }

    /**
     * 调整在录制时的速度, 比如你想预览的时候是正常的, 录制好后, 一部分要快进或慢放,则可以在这里设置 支持在任意时刻来变速; 当前暂时不支持音频,
     * 只是视频的加减速, 请注意!!! 这个只是录制时的有效, 正常界面显示还是原来的界面,请注意!!!
     * <p>
     * 建议5个等级: 0.25f,0.5f,1.0f,1.5f,2.0f; 其中 0.25是放慢4倍; 0.5是放慢2倍;
     * 1.0是采用和预览同样的速度; 1.5是加快一半, 2.0是加快2倍.
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
     * 获取当前View的 宽度
     *
     * @return
     */
    public int getViewWidth() {
        return drawPadWidth;
    }

    /**
     * 获得当前View的高度.
     *
     * @return
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
     * 设置当前view显示有效后的监听.
     * 当界面有效后, 执行listener;
     * @param listener
     */
    public void setOnViewAvailable(onViewAvailable listener) {
        mViewAvailable = listener;
    }
    @Deprecated
    public void setRealEncodeEnable(int encW, int encH, int encBr, int encFr,
                                    String outPath) {
        if (encW > 0 && encH > 0 && encBr > 0 && encFr > 0) {
            encWidth = encW;
            encHeight = encH;
            encBitRate = encBr;
            encFrameRate = encFr;
            encodeOutput = outPath;
            if(renderer!=null){
                renderer.setEncoderEnable(encWidth,encHeight,encBitRate,encFrameRate,encodeOutput);
            }
        } else {
            LSOLog.w(  "enable real encode is error");
        }
    }

    /**
     * 设置使能 实时保存, 即把正在DrawPad中呈现的画面实时的保存下来,实现所见即所得的模式,
     * @param encW 录制视频的宽度
     * @param encH 录制视频的宽度
     * @param encFr  录制视频的 帧率
     * @param outPath 录制视频的保存路径.
     */
    public void setRealEncodeEnable(int encW, int encH, int encFr,String outPath) {
        if (encW > 0 && encH > 0 && encFr > 0) {
            encWidth = encW;
            encHeight = encH;
            encBitRate = LanSongUtil.checkSuggestBitRate(encHeight* encWidth, encBitRate);
            encFrameRate = encFr;
            encodeOutput = outPath;
            if(renderer!=null){
                renderer.setEncoderEnable(encWidth,encHeight,encBitRate,encFrameRate,encodeOutput);
            }
        } else {
            LSOLog.w(  "enable real encode is error");
        }
    }
    boolean isDrawPadSizeChanged=false;
    /**
     * 这里的宽度和高度,会根据手机屏幕的宽度来做调整, 然后自适应等比例调整后, 把真正布局后的宽高返回给你.
     *
     * 默认是宽度对齐到手机的左右两边, 然后调整高度,
     * 把调整后的高度作为DrawPad渲染线程的真正宽度和高度.
     *
     * @param width  设置的容器的等比例宽度, 我们会根据你的宽高和布局的最大宽高, 做等比例缩放, 缩放后,把缩放后的宽高通过 listener 返回给你;
     * @param height 设置容器的等比例高度
     * @param listener    自适应等比例后, 把实际布局的宽高返回.
     */
    public void setDrawPadSize(int width, int height,onDrawPadSizeChangedListener listener) {

        isDrawPadSizeChanged=true;
        if (width != 0 && height != 0 && listener != null) {
            float setAcpect = (float) width / (float) height;

            float setViewacpect = (float) drawPadWidth / (float) drawPadHeight;

            LSOLog.i(  "setAcpect=" + setAcpect + " setViewacpect:"
                    + setViewacpect + "set width:" + width + "x" + height
                    + " view width:" + drawPadWidth + "x" + drawPadHeight);

            if (setAcpect == setViewacpect) { // 如果比例已经相等,不需要再调整,则直接显示.
                if (listener != null) {
                    listener.onSizeChanged(width, height);
                }
            } else if (Math.abs(setAcpect - setViewacpect) * 1000 < 16.0f) {
                if (listener != null) {
                    listener.onSizeChanged(width, height);
                }
            } else if (mTextureRenderView != null) {
                mTextureRenderView.setVideoSize(width, height);
                mTextureRenderView.setVideoSampleAspectRatio(1, 1);
                mSizeChangedCB = listener;
            }
            requestLayout();
        }
    }

    /**
     * 当前drawpad容器运行了多长时间, 仅供参考使用. 没有特别的意义.
     *
     * @param li
     */
    public void setOnDrawPadRunTimeListener(onDrawPadRunTimeListener li) {
        if (renderer != null) {
            renderer.setDrawPadRunTimeListener(li);
        }
        drawPadRunTimeListener = li;
    }
    /**
     * 把运行的时间复位到某一个值,
     * drawpad继续显示, 就会以这个值为参考, 增加相对运行的时间. drawpad已经运行了10秒钟,
     *
     * @param runtimeUs
     */
    public void resetDrawPadRunTime(long runtimeUs) {
        if (renderer != null) {
            renderer.resetPadRunTime(runtimeUs);
        }
    }

    public void setOnDrawPadProgressListener(onDrawPadProgressListener listener) {
        if (renderer != null) {
            renderer.setDrawPadProgressListener(listener);
        }
        drawPadProgressListener = listener;
    }

    public void setOnDrawPadThreadProgressListener(
            onDrawPadThreadProgressListener listener) {
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

    public void toggleSnatShot() {
        if (drawPadSnapShotListener != null && renderer != null && renderer.isRunning()) {
            renderer.toggleSnapShot(drawPadWidth, drawPadHeight);
        } else {
            LSOLog.e(  "toggle snap shot failed!!!");
        }
    }

    /**
     * 截图一次, 设置截图的宽度和高度; 如果你连续截图,则宽度和高度需每次都一致;
     *
     * @param width
     * @param height
     */
    public void toggleSnatShot(int width, int height) {
        if (drawPadSnapShotListener != null && renderer != null
                && renderer.isRunning()) {
            renderer.toggleSnapShot(width, height);
        } else {
            LSOLog.e( "toggle snap shot failed!!!");
        }
    }

    /**
     * 设置每处理一帧的数据预览监听, 等于把当前处理的这一帧的画面拉出来, 您可以根据这个画面来自行的编码保存, 或网络传输.
     * <p>
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
     * 设置setOnDrawPadOutFrameListener后, 你可以设置这个方法来让listener是否运行在Drawpad线程中.
     * 如果你要直接使用里面的数据, 则不用设置, 如果你要开启另一个线程, 把listener传递过来的数据送过去,则建议设置为true;
     * [建议设置为true, 在DrawPad内部执行listener, 间隔取图片后,放入到列表中,然后在另外线程中使用.]
     *
     * @param en
     */
    public void setOutFrameInDrawPad(boolean en) {
        if (renderer != null) {
            renderer.setOutFrameInDrawPad(en);
        }
        frameListenerInDrawPad = en;
    }

    // ---------------
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
     * 此方法仅仅使用在录制视频的同时,您也设置了录制音频
     *
     * @return 在录制结束后, 返回录制mic的音频文件路径,
     */
    public String getMicPath() {
        if (renderer != null) {
            return renderer.getAudioRecordPath();
        } else {
            return null;
        }
    }

    /**
     * 如果在边预览,边录制,则在录制的时候, 是否设置生成的视频为编辑模式的视频;
     * @param is
     */
    public void setEditModeVideo(boolean is) {
        if (renderer != null) {
            renderer.setEditModeVideo(is);
        } else {
            editModeVideo = is;
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
            renderer.setDrawPadBackGroundColor(padBGRed,padBGGreen,padBGBlur,1.0f);
        }
    }

    /**
     * 设置容器的 背景颜色RGBA分量
     * 在startDrawPad前调用
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
    /**
     * 开始DrawPad的渲染线程, 阻塞执行, 直到DrawPad真正开始执行后才退出当前方法.
     * <p>
     * 可以先通过 {@link #setDrawPadSize(int, int, onDrawPadSizeChangedListener)}
     * 来设置宽高,然后在回调中执行此方法. 如果您已经在xml中固定了view的宽高,则可以直接调用这里, 无需再设置DrawPadSize
     *
     * @return
     */
    public boolean startDrawPad() {
        return startDrawPad(isPauseRecord);
    }

    /**
     * 开始DrawPad的渲染线程, 阻塞执行, 直到DrawPad真正开始执行后才退出当前方法. 如果DrawPad设置了录制功能,
     * 这里可以在开启后暂停录制. 适用在当您开启录制后, 需要先增加一个图层的场合后,在让它开始录制的场合,
     */
    public boolean startDrawPad(boolean pauseRecord) {
        boolean ret = false;
        if(!LanSoEditor.isLoadLanSongSDK.get()){
           LSOLog.e("没有加载SDK, 或你的APP崩溃后,重新启动当前Activity,请查看完整的logcat:(No SDK is loaded, or the current activity is restarted after your app crashes, please see the full logcat)");
           return false;
        }

        if(renderer!=null){
            renderer.stopDrawPad();
            renderer=null;
        }

        if (mSurfaceTexture != null && drawPadWidth > 0 && drawPadHeight > 0) {
            renderer = new DrawPadViewRender(getContext(), drawPadWidth,drawPadHeight);
                renderer.setUseMainVideoPts(isUseMainPts);
                // 因为要预览,这里设置显示的Surface,当然如果您有特殊情况需求,也可以不用设置,但displayersurface和EncoderEnable要设置一个,DrawPadRender才可以工作.
                renderer.setDisplaySurface(new Surface(mSurfaceTexture));

                if (isCheckPadSize) {
                    encWidth = LanSongUtil.make16Multi(encWidth); // 编码默认16字节对齐.
                    encHeight = LanSongUtil.make16Multi(encHeight);
                }
                if (isCheckBitRate || encBitRate == 0) {
                    encBitRate = LanSongUtil.checkSuggestBitRate(encHeight
                            * encWidth, encBitRate);
                }
                if(encWidth>0 && encHeight>0 && encodeOutput!=null){
                    renderer.setEncoderEnable(encWidth, encHeight, encBitRate,
                            encFrameRate, encodeOutput);
                }
                renderer.setEditModeVideo(editModeVideo);
                renderer.setUpdateMode(mUpdateMode, mAutoFlushFps);


                renderer.setDrawPadBackGroundColor(padBGRed,padBGGreen,padBGBlur,padBGAlpha);

                // 设置DrawPad处理的进度监听, 回传的currentTimeUs单位是微秒.
                renderer.setDrawPadSnapShotListener(drawPadSnapShotListener);
                renderer.setDrawpadOutFrameListener(previewFrameWidth,
                        previewFrameHeight, previewFrameType,
                        drawPadPreviewFrameListener);
                renderer.setOutFrameInDrawPad(frameListenerInDrawPad);

                renderer.setDrawPadProgressListener(drawPadProgressListener);
                renderer.setDrawPadCompletedListener(drawpadCompletedListener);
                renderer.setDrawPadThreadProgressListener(drawPadThreadProgressListener);
                renderer.setDrawPadErrorListener(drawPadErrorListener);
                renderer.setDrawPadRunTimeListener(drawPadRunTimeListener);

                renderer.setLoopingWhenReachTime(reachTimeLoopTimeUs);
                if (isRecordMic) {
                    renderer.setRecordMic(true);
                } else if (isRecordExtPcm) {
                    renderer.setRecordExtraPcm(true, pcmChannels,pcmSampleRate, pcmBitRate);
                }

                if (pauseRecord || isPauseRecord) {
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
                    LSOLog.e("开启 drawPad 失败, 或许是您之前的DrawPad没有Stop, 或者传递进去的surface对象已经被系统Destory!!,"
                                    + "请检测您 的代码或参考本文件中的SurfaceCallback 这个类中的注释;\n");
                }else {
                    LSOLog.i("DrawPadView is running..."+ret);
                }
        } else {
            if (mSurfaceTexture == null) {
                LSOLog.e(
                        "可能没有您的UI界面还没有完全启动,您就startDrawPad了, 建议oncreate后延迟300ms再调用");
            } else {
                LSOLog.e("无法开启DrawPad, 您当前的参数有问题,您对照下参数:宽度和高度是:"
                        + drawPadWidth + " x " + drawPadHeight
                        + " mSurfaceTexture:" + mSurfaceTexture);
            }
        }
        return ret;
    }

    /**
     * 暂停DrawPad的画面刷新, 暂停后, 预览界面和后台录制被同时暂停. 在一些场景里,您需要开启DrawPad后,暂停下,
     * 然后增加各种Layer后,安排好各种事宜后,再让其画面更新,则用到这个方法.
     * <p>
     * 此方法是对DrawPad线程 暂停和恢复的, 不能用在一个Activity的onPause和onResume中.
     * 如果您要跳入到别的Activity, 则应该这里 {@link #stopDrawPad()} 在回到当前Activity的时候, 调用
     * {@link #startDrawPad()}
     */
    public void pauseDrawPad() {
        if (renderer != null) {
            renderer.pauseRefreshDrawPad();
        }
        isPauseRefreshDrawPad = true;
    }

    /**
     * 恢复之前暂停的DrawPad,让其继续画面刷新. 与{@link #pauseDrawPad()}配对使用.
     * 此方法是对DrawPad线程 暂停和恢复的, 不能用在一个Activity的onPause和onResume中.
     * 如果您要跳入到别的Activity, 则应该这里 {@link #stopDrawPad()} 在回到当前Activity的时候, 调用
     * {@link #startDrawPad()}
     */
    public void resumeDrawPad() {
        if (renderer != null) {
            renderer.resumeRefreshDrawPad();
        }
        isPauseRefreshDrawPad = false;
    }

    /**
     * 暂停预览刷新, 停止预览后, 如果您的后台还在录制中,则会一直录制同一副画面. 一般的场合不建议调用.
     * <p>
     * 暂停和恢复有3种方法, 分别是:刷新,预览,录制;
     * <p>
     * 暂停刷新,则都暂停了, 暂停录制则只暂停录制,刷新和预览一样在走动. 暂停预览,则录制还在继续,但只是录制了同一副画面.
     * <p>
     * 此方法是对DrawPad线程 暂停和恢复的, 不能用在一个Activity的onPause和onResume中.
     * 如果你要从一个Activity切换到另一个Activity,则需要 {@link #stopDrawPad()} 然后重新开始
     */
    public void pausePreview() {
        if (renderer != null) {
            renderer.pausePreviewDrawPad();
        }
        isPausePreviewDrawPad = true;
    }

    /**
     * 不建议使用.
     * <p>
     * 此方法是对DrawPad线程 暂停和恢复的, 不能用在一个Activity的onPause和onResume中.
     * 如果你要从一个Activity切换到另一个Activity,则需要 {@link #stopDrawPad()} 然后重新开始
     */
    public void resumePreview() {
        if (renderer != null) {
            renderer.resumePreviewDrawPad();
        }
        isPausePreviewDrawPad = false;
    }

    /**
     * 暂停drawpad的录制,这个方法使用在暂停录制后, 在当前画面做其他的一些操作, 不可用来暂停后跳入到别的Activity中.
     * <p>
     * 此方法是对DrawPad线程 暂停和恢复的, 不能用在一个Activity的onPause和onResume中.
     * 如果你要从一个Activity切换到另一个Activity,则需要 {@link #stopDrawPad()} 然后重新开始
     */
    public void pauseDrawPadRecord() {
        if (renderer != null) {
            renderer.pauseRecordDrawPad();
        }
        isPauseRecord = true;
    }

    /**
     * 恢复drawpad的录制. 如果刷新也是暂停状态, 同时恢复刷新.
     * <p>
     * 此方法是对DrawPad线程 暂停和恢复的, 不能用在一个Activity的onPause和onResume中.
     * 如果你要从一个Activity切换到另一个Activity,则需要 {@link #stopDrawPad()} 然后重新开始
     */
    public void resumeDrawPadRecord() {
        if (renderer != null) {
            renderer.resumeRecordDrawPad();
        }
        isPauseRecord = false;
    }

    /**
     * 是否在CameraLayer录制的同时, 录制mic的声音. 在drawpad开始前调用.
     * <p>
     * 此方法仅仅使用在录像的时候, 同时录制Mic的场合.录制的采样默认是44100, 码率64000, 编码为aac格式. 录制的同时,
     * 视频编码以音频的时间戳为参考. 可通过 {@link #stopDrawPad2()}停止,停止后返回mic录制的音频文件 m4a格式的文件,
     *
     * @param record
     */
    public void setRecordMic(boolean record) {
        if (renderer != null && renderer.isRecording()) {
            LSOLog.e("DrawPadView is running. set Mic Error!");
        } else {
            isRecordMic = record;
        }
    }

    /**
     * 是否在录制画面的同时,录制外面的push进去的音频数据 .
     * 注意:当设置了录制外部的pcm数据后, 当前容器上录制的视频帧,就以音频的帧率为参考时间戳,从而保证音视频同步进行. 故您在投递音频的时候,
     * 需要严格按照音频播放的速度投递.
     * <p>
     * 如采用外面的pcm数据,则视频在录制过程中,会参考音频时间戳,来计算得出视频的时间戳,
     * 如外界音频播放完毕,无数据push,应及时stopDrawPad2
     * <p>
     * 可以通过 AudioLine 的getFrameSize()方法获取到每次应该投递多少个字节,此大小为固定大小,
     * 每次投递必须push这么大小字节的数据,
     * <p>
     * 可通过 {@link #stopDrawPad2()}停止,停止后返回mic录制的音频文件 m4a格式的文件,
     *
     * @param isrecord   是否录制
     * @param channels   声音通道个数, 如是mp3或aac文件,可根据MediaInfo获得
     * @param samplerate 采样率 如是mp3或aac文件,可根据MediaInfo获得
     * @param bitrate    码率 如是mp3或aac文件,可根据MediaInfo获得
     */
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
     * 获取一个音频输入对象, 向内部投递数据, 只有当开启容器录制,并设置了录制外面数据的情况下,才有效.
     *
     * @return
     */
    public AudioLine getAudioLine() {
        if (renderer != null) {
            return renderer.getAudioLine();
        } else {
            return null;
        }
    }

    /**
     * 分段录制, 开始录制一段.
     */
    public void segmentStart() {
        if (renderer != null) {
            renderer.segmentStart();
        }
    }

    /**
     * 录制一段结束.
     * <p>
     * 录制完成后, 返回当前录制这一段的视频文件完整路径名, 因为这里会等待 编码和音频采集模块处理完毕释放后才返回,
     * 故有一定的阻塞时间(在低端手机上大概100ms), 建议用Handler的 HandlerMessage的形式来处理
     */
    public String segmentStop() {
        if (renderer != null) {
            return renderer.segmentStop();
        } else {
            return null;
        }
    }

    public boolean isRecording() {
        if (renderer != null)
            return renderer.isRecording();
        else
            return false;
    }

    /**
     * 设置是否使用主视频的时间戳为录制视频的时间戳, 如果您传递过来的是一个完整的视频, 只是需要在此视频上做一些操作,
     * 操作完成后,时长等于源视频的时长, 则使用主视频的时间戳, 如果视频是从中间截取一般开始的 则不建议使用, 默认是这里为false;
     * <p>
     * 注意:需要在DrawPad开始前使用.
     */
    public void setUseMainVideoPts(boolean use) {
        isUseMainPts = use;
    }

    /**
     * 当前DrawPad是否在工作.
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
     * 停止DrawPad的渲染线程
     */
    public void stopDrawPad() {
        if (renderer != null) {
            renderer.release();
            renderer = null;
        }
    }

    /**
     * 停止DrawPad的渲染线程 如果设置了在录制的时候,设置了录制mic或extPcm, 则返回录制音频的文件路径.
     *
     * @return
     */
    public String stopDrawPad2() {
        String ret = null;
        if (renderer != null) {
            renderer.release();
            ret = renderer.getAudioRecordPath();
            renderer = null;
        }
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
        drawPadWidth = width;
        drawPadHeight = height;
        if (renderer != null) {
            LSOLog.w("aeRenderer maybe is running. your setting is not available!!");
        }
    }

    /**
     * 把当前图层放到最里层, 里面有 一个handler-loop机制, 将会在下一次刷新后执行.
     *
     * @param layer
     */
    public void bringLayerToBack(Layer layer) {
        if (renderer != null) {
            renderer.bringLayerToBack(layer);
        }
    }

    /**
     * 把当前图层放到最外层, 里面有 一个handler-loop机制, 将会在下一次刷新后执行.
     *
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
     * 获取一个主视频的 VideoLayer 主视频的意思是: 对一个完整的视频做操作, 从视频的0秒一直运行到视频的最后一帧, 比如对一个视频做滤镜,
     * 增加图片图层, 增加mv图层等等.
     * <p>
     * 如果你要先把一个视频add进来, 后面再增加另一个视频,则用 addVideoLayer(),不能用这个方法.
     *
     * @param width  主视频的画面宽度 建议用 {@link MediaInfo#vWidth}来赋值
     * @param height 主视频的画面高度
     * @return
     */
    public VideoLayer addMainVideoLayer(int width, int height,
                                        LanSongFilter filter) {
        VideoLayer ret = null;
        if (renderer != null)
            ret = renderer.addMainVideoLayer(width, height, filter);
        else {
            LSOLog.e("setMainVideoLayer error render is not avalid");
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
            LSOLog.e("addTwoVideoLayer error render is not avalid");
        }
        return ret;
    }
    /**
     * 2.8.5版本后新增的,是经过优化后的VideoLayer, 取名字叫VideoLayer2;
     * 用法和VideoLayer相同,增加子图层功能;
     *
     * @param width  视频的宽度
     * @param height 视频的高度
     * @param filter 增加滤镜, 如果不需要滤镜,设置为null;
     * @return
     */
    public VideoLayer addVideoLayer(int width, int height, LanSongFilter filter) {
        if (renderer != null)
            return renderer.addVideoLayer(width, height, filter);
        else {
            LSOLog.e( "addVideoLayer error render is invalid");
            return null;
        }
    }


    /**
     * 输入一个视频资源, 内部自动启动播放器;
     * @return
     */
    public VideoLayer2 addVideoLayer2(String path){
        if(renderer!=null){
            return renderer.addVideoLayer2(path);
        }else{
            LSOLog.e( "addVideoLayer error render is invalid");
            return null;
        }
    }

    /**
     *
     * @param startTimeUs 从容器的什么时间点开始
     * @param endTimeUs 从容器的什么时间点结束;
     * @return
     */
    public VideoLayer2 addVideoLayer2(String path, long startTimeUs, long endTimeUs){
        if(renderer!=null){
            VideoLayer2 layer2= renderer.addVideoLayer2(path);
            if(layer2!=null){
                layer2.setDisplayTimeRange(startTimeUs,endTimeUs);
            }
            return layer2;
        }else{
            LSOLog.e( "addVideoLayer error render is invalid");
            return null;
        }
    }


    /**
     * 增加AE模板层;
     * @param asset
     * @param startTimeUs
     * @param endTimeUs
     * @return
     */
    public LSOAECompositionLayer addAECompositionLayer(LSOAeCompositionAsset asset, long startTimeUs, long endTimeUs){
        if(renderer!=null){
            return renderer.addAECompositionLayer(asset,startTimeUs,endTimeUs);
        }else{
            LSOLog.e( "addAECompositionLayer error render is not avalid");
            return null;
        }
    }
    /**
     * 增加相册影集图层, 只需要用户选择多张图片+ 一个Ae的json文件, 我们内部会自动根据图片数量来裁剪json或拼接json;

     相册影集资源类的两个参数:
     bitmaps: 多张图片列表.
     jsonPath: 用AE导出的json动画;
     LSOPhotoAlbumAsset(List<Bitmap> bitmaps, String jsonPath) throws Exception



     用AE制作动画的规则:
     1. 不能使用预合成,
     2. 每个图层对应一张图片, 不能一张图片应用到多个图层;
     3. json总时长不能超过20秒,每个图片时间建议是2--3秒,分辨率建议720x1280,帧率是20fps或15fps;
     4. 图片数量,建议不超过20张.
     5. 我们内部会根据你的图片多少,和json的时长来裁剪或拼接
     6. LSOPhotoAlbumAsset在使用完毕后,确认不再使用时, 一定要调用release释放资源,比如在让用户重新选择图片的前一行调用;
     7.演示例子,见我们的PhotoAlbumLayerDemoActivity.java

     * @param asset 影集图层资源.
     * @return
     */
    public LSOPhotoAlbumLayer addPhotoAlbumLayer(LSOPhotoAlbumAsset asset){
        return addPhotoAlbumLayer(asset,0,Long.MAX_VALUE);
    }

    /**
     * 注释同上.
     *
     * @param asset
     * @param startTimeUs
     * @param endTimeUs
     * @return
     */
    public LSOPhotoAlbumLayer addPhotoAlbumLayer(LSOPhotoAlbumAsset asset, long startTimeUs, long endTimeUs){
        if(renderer!=null){
            return renderer.addPhotoAlbumLayer(asset,startTimeUs,endTimeUs);
        }else{
            LSOLog.e( "addPhotoAlbumLayer error render is not avalid");
            return null;
        }
    }



    long reachTimeLoopTimeUs=-1;

    /**
     * 设置当刷新时间走到哪里的时候, 恢复到 0,循环播放.
     * 只在addAECompositionLayer 演示的时候, 临时使用.
     * @param timeUs 时间,单位 us, ;
     */
    public void setLoopingWhenReachTime(long timeUs){
        if(renderer!=null){
            renderer.setLoopingWhenReachTime(timeUs);
        }else{
            reachTimeLoopTimeUs=timeUs;
        }
    }

    /**
     * 获取一个BitmapLayer 注意:此方法一定在 startDrawPad之后,在stopDrawPad之前调用.
     *
     * @param bmp 图片的bitmap对象
     * @return 一个BitmapLayer对象
     */
    public BitmapLayer addBitmapLayer(Bitmap bmp) {
        if (bmp != null) {
            if (renderer != null && renderer.isRunning())
                return renderer.addBitmapLayer(bmp, null);
            else {
                LSOLog.e( "addBitmapLayer error render is not avalid");
                return null;
            }
        } else {
            LSOLog.e( "addBitmapLayer error, byteBuffer is null");
            return null;
        }
    }

    /**
     * 增加一个图片
     * @param bmp 图片
     * @param filter 给图片设置一个滤镜;
     * @return
     */
    public BitmapLayer addBitmapLayer(Bitmap bmp, LanSongFilter filter) {
        if (bmp != null) {
            if (renderer != null){
                return renderer.addBitmapLayer(bmp, filter);
            }else {
                LSOLog.e( "addBitmapLayer error render is not avalid");
                return null;
            }
        } else {
            LSOLog.e( "addBitmapLayer error, byteBuffer is null");
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
                LSOLog.e( "addTextureLayer error render is not avalid");
                return null;
            }
        } else {
            LSOLog.e( "addTextureLayer error, texid is error");
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
                LSOLog.e("addDataLayer error render is not avalid");
                return null;
            }
        } else {
            LSOLog.e( "addDataLayer error, data size is error");
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
     * resId 来自apk中drawable文件夹下的各种资源文件
     * @param resId 来自apk中drawable文件夹下的各种资源文件.
     * @return
     */
    public GifLayer addGifLayer(int resId) {
        if (renderer != null)
            return renderer.addGifLayer(resId);
        else {
            LSOLog.e( "addGifLayer error! render is not avalid");
            return null;
        }
    }



    /**
     * 增加子图层.
     * 默认不需要调用,我们会在视频图层绘制后, 直接绘制子图层;
     * 如果你要在视频层上面增加其他层, 然后再增加子图层, 则用这个.
     * @param layer
     * @return
     */
    public boolean addSubLayer(SubLayer layer){
        return renderer!=null && renderer.addSubLayer(layer);
    }

    /**
     * 增加一个mv图层, mv图层分为两个视频文件, 一个是彩色的视频, 一个黑白视频
     *
     * @param srcPath
     * @param maskPath
     * @return
     */
    public MVLayer addMVLayer(String srcPath, String maskPath) {
        if (renderer != null)
            return renderer.addMVLayer(srcPath, maskPath);
        else {
            LSOLog.e("addMVLayer error render is not avalid");
            return null;
        }
    }
    public MVLayer addMVLayer(LSOMVAsset asset) {
        if (renderer != null && asset!=null)
            return renderer.addMVLayer(asset);
        else {
            LSOLog.e("addMVLayer error render is not avalid");
            return null;
        }
    }

    /**
     * 获得一个 ViewLayer,您可以在获取后,仿照我们的例子,来为视频增加各种UI空间. 注意:此方法一定在
     * startDrawPad之后,在stopDrawPad之前调用.
     *
     * @return 返回ViewLayer对象.
     */
    public ViewLayer addViewLayer() {
        if (renderer != null)
            return renderer.addViewLayer();
        else {
            LSOLog.e("addViewLayer error render is not avalid");
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
            LSOLog.e("addCanvasLayer error render is not avalid");
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
            LSOLog.e( "addCanvasLayer error render is not avalid");
            return null;
        }
    }

    /**
     * 从渲染线程列表中移除并销毁这个Layer; 注意:此方法一定在 startDrawPad之后,在stopDrawPad之前调用.
     * @param layer
     */
    public void removeLayer(Layer layer) {
        if (layer != null) {
            if (renderer != null)
                renderer.removeLayer(layer);
            else {
                LSOLog.w( "removeLayer error render is not avalid");
            }
        }
    }

    public void removeAllLayer(){
        if(renderer!=null){
            renderer.removeAllLayer();
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
        void viewAvailable(DrawPadView v);
    }

    private class SurfaceCallback implements SurfaceTextureListener {

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


            stopDrawPad(); //当Texture销毁的时候, 停止DrawPad


            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    }
}
