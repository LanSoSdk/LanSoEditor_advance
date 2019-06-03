package com.lansosdk.videoeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.lansosdk.box.AudioLayer;
import com.lansosdk.box.AudioPad;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.CanvasLayer;
import com.lansosdk.box.CanvasRunnable;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LayerShader;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.LSOTimeRange;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.onAudioPadProgressListener;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadErrorListener;
import com.lansosdk.box.onDrawPadProgressListener;

import java.util.ArrayList;
import java.util.List;

import com.lansosdk.LanSongFilter.LanSongFilter;
import com.lansosdk.box.OnLanSongSDKErrorListener;

/**
 * 用DrawPadVideoExecute2对常见视频的封装.
 * 调用步骤:
 *
 * //第一步:创建对象,并设置各种set;
 * videoOneDo=new  VideoOneDo(getApplicationContext(), inputPath);
 *  videoOneDo.setCropRect(cropX, cropY, cropW, cropH); //裁剪
 * videoOneDo.setLogo(bmp, VideoOneDo.LOGO_POSITION_RIGHT_TOP); //加logo
 *
 * //第二步设置各种回调
 * videoOneDo.setOnVideoOneDoProgressListener(进度监听);
 * videoOneDo.setOnVideoOneDoCompletedListener(完成监听, 返回处理后的结果);
 *
 * //第三步:开始
 * videoOneDo.start(); //开启另一个线程,成功返回true, 失败返回false
 */
public class VideoOneDo {

    public final static int LOGO_POSITION_LELF_TOP = 0;
    public final static int LOGO_POSITION_LEFT_BOTTOM = 1;
    public final static int LOGO_POSITION_RIGHT_TOP = 2;
    public final static int LOGO_POSITION_RIGHT_BOTTOM = 3;


    public final static int VIDEOONEDO_ERROR_DSTERROR = 6001;
    public final static int VIDEOONEDO_ERROR_SRCFILE = 6002;
    public final static int VIDEOONEDO_ERROR_DRAWPAD = 6003;
    private String inputPath = null;
    private MediaInfo mediaInfo;
    private long padDurationUs = 0;// drawpad处理后的视频时长.

    private String drawpadDstPath = null;

    private DrawPadVideoExecute drawPad = null;
    private AudioPadExecute audioPad;
    private String audioPadSavePath = null;

    private boolean isExecuting = false;

    private VideoLayer videoLayer = null;
    private BitmapLayer logoBmpLayer = null;
    private CanvasLayer canvasLayer = null;

    protected Context context;

    // ------------------视频参数.-------------------------------
    private long startTimeUs = 0;
    private long cutDurationUs = 0;
    private int cropX, cropY, cropWidth, cropHeight;
    private LanSongFilter videoFilter = null;

    private Bitmap logoBitmap = null;
    private int logoPosition = LOGO_POSITION_RIGHT_TOP;
    protected int scaleWidth, scaleHeight;
    protected int videoBitRate = 0;
    protected boolean isCheckBitrate=false;

    protected String textAdd = null;
    // -------------------音频参数--------------------
    private String bgMusicPath = null;
    private MediaInfo bgMusicInfo=null;
    private boolean isMixBgMusic; // 是否要混合背景音乐.
    private long bgMusicStartUs = 0;
    private long bgMusicEndUs = 0;
    private float bgMusicVolume = 1.0f;

    private float inputVideoVolume = 1.0f;
    private ArrayList<String> deleteArray = new ArrayList<String>();
    private List<LSOTimeRange> timeStretchArray = null;
    private List<LSOTimeRange> timeFreezeArray = null;
    private List<LSOTimeRange> timeRepeatArray = null;
    private boolean isEditModeVideo=false;
    private OnLanSongSDKProgressListener onProgressListener;
    private OnLanSongSDKCompletedListener onCompletedListener = null;
    private OnLanSongSDKErrorListener onErrorListener = null;

    /**
     * 构造方法
     *
     * @param ctx       android的Context语境
     * @param videoPath 要处理的视频文件;
     */
//    public VideoOneDo(Context ctx, String videoPath) throws FileNotFoundException{
//        this.inputPath = videoPath;
//        context = ctx;
//        mediaInfo = new MediaInfo(inputPath);
//        if (!mediaInfo.prepare()) {
//            throw  new FileNotFoundException(" input path is not found.mediaInfo is:"+mediaInfo.toString());
//        }
//    }

    public VideoOneDo(Context ctx, String videoPath){
        this.inputPath = videoPath;
        context = ctx;
        mediaInfo = new MediaInfo(inputPath);
        if (!mediaInfo.prepare()) {
            LSOLog.w("视频输入错误, 信息是:"+ mediaInfo.toString());
            mediaInfo =null;
        }
    }

    /**
     * 增加背景音乐.
     * <p>
     * 支持 mp3, m4a wav格式
     * <p>
     * 如果背景音乐时间 比视频长,则会从开始截取等长部分.
     *
     * @param path
     */
    public void setBackGroundMusic(String path) {
        bgMusicInfo = new MediaInfo(path);
        if (bgMusicInfo.prepare() && bgMusicInfo.isHaveAudio()) {
            bgMusicPath = path;
        } else {
            LSOLog.e(  "设置背景音乐出错, 音频文件有误.请查看" + bgMusicInfo.toString());
            bgMusicPath = null;
            bgMusicInfo = null;
        }
    }

    /**
     * 增加背景音乐
     * <p>
     * 支持 mp3, m4a wav格式
     * 比视频长,则会从开始截取等长部分.
     *
     * @param path
     * @param isMix  是否混合,即是否保留原视频的声音,
     * @param volume 如增加,则背景音乐的音量调节 =1.0f为不变, 小于1.0降低; 大于1.0提高; 最大2.0;
     */
    public void setBackGroundMusic(String path, boolean isMix, float volume) {
        setBackGroundMusic(path);
        if (bgMusicInfo != null) {
            isMixBgMusic = isMix;
            bgMusicVolume = volume;
        }
    }

    /**
     * 增加背景音乐
     * <p>
     * 支持 mp3, m4a wav格式
     *
     * @param path
     * @param isMix      是否混合
     * @param mainVolume 原音频的音量, 1.0f为原音量; 小于则降低, 大于则放大,
     * @param bgVolume   背景视频的音量
     */
    public void setBackGroundMusic(String path, boolean isMix,
                                   float mainVolume, float bgVolume) {
        setBackGroundMusic(path, isMix, bgVolume);
        inputVideoVolume = mainVolume;


    }

    /**
     * 增加背景音乐,并裁剪背景音乐,
     * <p>
     * 背景音乐是否和原声音混合, 混合时各自的音量.
     *
     * @param path       音乐路径
     * @param startUs  音乐的开始时间, 单位微秒
     * @param endUs    音乐的结束时间, 单位微秒
     * @param isMix      是否混合原声音,
     * @param mainVolume 原声音的音量,1.0f为原音量; 小于则降低, 大于则放大,
     * @param bgVolume   背景音乐的音量
     */
    public void setBackGroundMusic(String path, long startUs, long endUs,
                                   boolean isMix, float mainVolume, float bgVolume) {
        setBackGroundMusic(path, isMix, bgVolume);

        inputVideoVolume = mainVolume;
        if (bgMusicInfo != null && startUs > 0.0f && startUs < endUs
                && endUs <= (long)(bgMusicInfo.aDuration*1000000)) {
            bgMusicStartUs = startUs;
            bgMusicEndUs = endUs;
        }
    }
    /**
     * 直接设置码率
     *
     * @param bitrate
     */
    public void setBitrate(int bitrate) {
        if (bitrate > 0 && !isEditModeVideo) {
            videoBitRate = bitrate;
        }
    }
    public void setBitrate(int bitrate,boolean check) {
        if (bitrate > 0 && !isEditModeVideo) {
            videoBitRate = bitrate;
            isCheckBitrate=check;
        }
    }


    /**
     * 设置视频的开始位置,
     * <p>
     * 等于截取视频中的一段 单位微秒,
     *
     * @param timeUs
     */
    public void setStartPostion(long timeUs) {
        if(timeUs>0 && timeUs<getVdieoDurationUs()){
            startTimeUs = timeUs;
        }
    }
    /**
     * 设置结束时间
     *
     * @param timeUs
     */
    public void setEndPostion(long timeUs) {
        if (timeUs > 0){
            if(timeUs > startTimeUs && timeUs< getVdieoDurationUs()){
                cutDurationUs = timeUs - startTimeUs;
                if (cutDurationUs < 1000 * 1000) {
                    LSOLog.w("警告: 你设置的最终时间小于1秒,可能只有几帧的时间.请注意!");
                }
            }else if(timeUs>getVdieoDurationUs()){
                cutDurationUs=getVdieoDurationUs();
            }else{
                LSOLog.e("VideoOneDo: setEndPostion ERROR: 时间小于开始时间,无效");
            }
        }else{
            LSOLog.e("VideoOneDo: setEndPostion ERROR: 时间必须大于0");
        }
    }


    /**
     * 截取视频中的多长时间.
     * <p>
     * 等于 setEndPostion() - setStartPostionUs();
     * 单位微秒
     *
     * @param timeUs
     */
    public void setCutDuration(long timeUs) {
        if (timeUs > 0 && timeUs<(getVdieoDurationUs()- startTimeUs)) {
            cutDurationUs = timeUs;
            if (cutDurationUs < 1000 * 1000) {
                LSOLog.w("警告: 你设置的最终时间小于1秒,可能只有几帧的时间.请注意!");
            }
        }else{
            LSOLog.w( "剪切时长无效,恢复为0...");
        }
    }

    /**
     * 设置裁剪画面
     *
     * 裁剪和缩放无法同时执行;!!! 如同时设置了,以裁剪为主;
     *
     * @param startX 画面的开始横向坐标,
     * @param startY 画面的开始纵向坐标
     * @param cropW  裁剪多少宽度
     * @param cropH  裁剪多少高度
     */
    public void  setCropRect(int startX, int startY, int cropW, int cropH) {

        if(mediaInfo!=null && mediaInfo.getWidth()>=(startX + cropW) && mediaInfo.getHeight() >= (startY+ cropH))
        {
            this.cropX = startX;
            this.cropY = startY;

            cropWidth = cropW;
            cropHeight = cropH;
            if(cropW%16!=0 || cropH%16!=0){
                LSOLog.w("您要裁剪的宽高不是16的倍数,可能会出现黑边");
            }

        }else{
            LSOLog.e("VideoOneDo setCropRect error.");
        }

    }
    /**
     * 缩放到的目标宽度和高度.
     *
     * 画面裁剪和缩放无法同时执行;!!! 如同时设置了,以裁剪为主;
     * @param scaleW
     * @param scaleH
     */
    public void setScaleWidth(int scaleW, int scaleH) {
        if (scaleW > 0 && scaleH > 0) {
            scaleWidth = scaleW;
            scaleHeight = scaleH;
        }
    }
    /**
     * 这里仅仅是举例,用一个滤镜.如果你要增加多个滤镜,可以判断处理进度,来不断切换滤镜
     *
     * @param filter
     */
    public void setFilter(LanSongFilter filter) {
        videoFilter = filter;
    }

    /**
     * 设置logo的位置, 这里仅仅是举例,您可以拷贝这个代码, 自行定制各种功能.
     *
     * 原理: 增加一个图片图层到容器DrawPad中, 设置他的位置.
     *
     * 位置这里举例是:
     * <p>
     * {@link #LOGO_POSITION_LEFT_BOTTOM}
     * {@link #LOGO_POSITION_LELF_TOP}
     * {@link #LOGO_POSITION_RIGHT_BOTTOM}
     * {@value #LOGO_POSITION_RIGHT_TOP}
     *
     * @param bmp      logo图片对象
     * @param position 位置
     */
    public void setLogo(Bitmap bmp, int position) {
        logoBitmap = bmp;
        if (position <= LOGO_POSITION_RIGHT_BOTTOM) {
            logoPosition = position;
        }
    }

    public void setEditModeVideo() {
        isEditModeVideo = true;
        videoBitRate = 0;
    }


    /**
     * 增加文字, 这里仅仅是举例,
     * <p>
     * 原理: 增加一个CanvasLayer图层, 把文字绘制到Canvas图层上. 文字的位置,
     * <p>
     * 是Canvas绘制出来的.
     *
     * @param text
     */
    public void setText(String text) {
        textAdd = text;
    }

    /**
     * 增加时间拉伸;
     *
     * 调整视频的播放速度;
     *
     * @param startTimeUs 开始时间,
     * @param endTimeUs   结束时间.
     * @param speed       范围0.5---2.0, 0.5是放慢一倍. 2.0是加速快进一倍;
     */
    public void addTimeStretch(long startTimeUs, long endTimeUs, float speed) {
        if (timeStretchArray == null) {
            timeStretchArray = new ArrayList<LSOTimeRange>();
        }
        timeStretchArray.add(new LSOTimeRange(startTimeUs, endTimeUs, speed));
    }

    /**
     * 注释同上; 只是您可以把前台预览收集到的多个拉伸数组放进来;
     *
     * @param timearray
     */
    public void addTimeStretch(List<LSOTimeRange> timearray) {
        timeStretchArray = timearray;
    }

    /**
     * 增加时间冻结
     *
     *即在视频的什么时间段开始冻结, 静止的结束时间;
     *
     * @param startTimeUs  哪个时间点开始冻结,
     * @param endTimeUs   结束时间
     */
    public void addTimeFreeze(long startTimeUs, long endTimeUs) {
        if (timeFreezeArray == null) {
            timeFreezeArray = new ArrayList<LSOTimeRange>();
        }
        timeFreezeArray.add(new LSOTimeRange(startTimeUs, endTimeUs));
    }

    public void addTimeFreeze(List<LSOTimeRange> list) {
        timeFreezeArray = list;
    }

    /**
     * 增加时间重复;
     *
     * 把原视频中的 从开始到结束这一段时间, 重复. 可以设置重复次数;
     *
     * @param startUs 相对原视频/原音频的开始时间;
     * @param endUs   相对原视频/原音频的结束时间;
     * @param loopcnt 重复的次数;
     */
    public void addTimeRepeat(long startUs, long endUs, int loopcnt) {
        if (timeRepeatArray == null) {
            timeRepeatArray = new ArrayList<LSOTimeRange>();
        }
        timeRepeatArray.add(new LSOTimeRange(startUs, endUs, loopcnt));
    }

    public void addTimeRepeat(List<LSOTimeRange> list) {
        timeRepeatArray = list;
    }

    public void setOnVideoOneDoProgressListener(OnLanSongSDKProgressListener li) {
        onProgressListener = li;
    }

    public void setOnVideoOneDoCompletedListener(OnLanSongSDKCompletedListener li) {
        onCompletedListener = li;
    }

    public void setOnVideoOneDoErrorListener(OnLanSongSDKErrorListener li) {
        onErrorListener = li;
    }
    /**
     * 开始执行, 内部会开启一个线程去执行. 开启成功,返回true. 失败返回false;
     *
     * @return
     */
    public boolean start() {
        if (isExecuting || mediaInfo ==null)
            return false;

        if (!mediaInfo.isHaveAudio()) {
            isMixBgMusic = false;// 没有音频则不混合.
        }

        padDurationUs =getVdieoDurationUs();
        if (cutDurationUs > 0) {
            padDurationUs = cutDurationUs;
        }else if(startTimeUs>0){
            padDurationUs-=startTimeUs;
        }

        if (isOnlyDoMusic() && bgMusicInfo == null) { // 如果仅有音频,则用音频容器即可.没有必要把视频执行一遍;
            isExecuting= startAudioPad();
        } else {
            isExecuting= startDrawPad();
        }
        return isExecuting;
    }

    protected boolean startDrawPad() {

        drawpadDstPath = LanSongFileUtil.createMp4FileInBox();
        drawPad = new DrawPadVideoExecute(context, inputPath, drawpadDstPath);
        drawPad.setStartTimeUs(startTimeUs);

        drawPad.setDurationTimeUs(padDurationUs);


        drawPad.setVideoFilter(videoFilter);
        if (videoBitRate > 0) {
            drawPad.setRecordBitrate(videoBitRate);
            if(!isCheckBitrate){
                drawPad.setNotCheckBitRate();
            }
        }

        if (isEditModeVideo) {
            LayerShader.setEditMode();
        }
        if(isNotCheckPadSize){
            drawPad.setNotCheckDrawPadSize();
        }

        if(cropWidth>0 && cropHeight>0){
            drawPad.setDrawPadSize(cropWidth,cropHeight);
        }else if(scaleHeight > 0 && scaleWidth > 0){
            drawPad.setDrawPadSize(scaleWidth, scaleHeight);
        }

        drawPad.setDrawPadErrorListener(new onDrawPadErrorListener() {

            @Override
            public void onError(DrawPad d, int what) {
                if (onErrorListener != null) {
                    onErrorListener.onLanSongSDKError(VIDEOONEDO_ERROR_DRAWPAD);
                }
            }
        });
        /**
         * 设置DrawPad处理的进度监听, 回传的currentTimeUs单位是微秒.
         */
        drawPad.setDrawPadProgressListener(new onDrawPadProgressListener() {
            @Override
            public void onProgress(DrawPad v, long currentTimeUs) {

                if (onProgressListener != null) {
                    int percent = (int)(currentTimeUs * 100 /padDurationUs);
                    onProgressListener.onLanSongSDKProgress(currentTimeUs,percent);
                }
            }
        });
        /**
         * 设置DrawPad处理完成后的监听.
         */
        drawPad.setDrawPadCompletedListener(new onDrawPadCompletedListener() {

            @Override
            public void onCompleted(DrawPad v) {
                completeDrawPad();
            }
        });

        /**
         * 增加音频参数.
         */
        addAudioParamter();

        /**
         * 增加时间拉伸.
         */
        if (timeStretchArray != null) {
            drawPad.addTimeStretch(timeStretchArray);
        }

        if (timeRepeatArray != null) {
            drawPad.addTimeRepeat(timeRepeatArray);
        }

        if (timeFreezeArray != null) {
            drawPad.addTimeFreeze(timeFreezeArray);
        }

        drawPad.pauseRecord();

        if (drawPad.startDrawPad()) {
            videoLayer = drawPad.getMainVideoLayer();

            addBitmapLayer(); // 增加图片图层
            addCanvasLayer(); // 增加文字图层.

            if(cropWidth>0 && cropHeight>0){
                if(cropX==0 && !isNotCheckPadSize &&cropX%16 >=8){
                        cropX=4;  //LSFIXME
                }
                videoLayer.setScaledValue(mediaInfo.getWidth(),mediaInfo.getHeight());
                videoLayer.setPosition(mediaInfo.getWidth()/2.0f-cropX,mediaInfo.getHeight()/2.0f-cropY);
            }else{
                videoLayer.setScaledValue(videoLayer.getPadWidth(), videoLayer.getPadHeight());
            }
            drawPad.resumeRecord();
            return true;
        } else {
            return false;
        }
    }

    protected void completeDrawPad() {
        if (!isExecuting) {
            return;
        }
        MediaInfo info = new MediaInfo(drawpadDstPath);
        if (info.prepare()) {
            if (onCompletedListener != null && isExecuting) {
                onCompletedListener.onLanSongSDKCompleted(drawpadDstPath);
            }
        } else {
            LSOLog.e( "VideoOneDo执行错误!!!");
            if (onErrorListener != null && isExecuting) {
                onErrorListener.onLanSongSDKError(VIDEOONEDO_ERROR_DSTERROR);
            }
        }
        isExecuting = false;
    }

    public void stop() {
        if (isExecuting) {
            isExecuting = false;

            onCompletedListener = null;
            onProgressListener = null;
            if (drawPad != null) {
                drawPad.stopDrawPad();
                drawPad = null;
            }
            if (audioPad != null) {
                audioPad.release();
                audioPad = null;
                LanSongFileUtil.deleteFile(audioPadSavePath);
                audioPadSavePath = null;
            }
            for (String string : deleteArray) {
                LanSongFileUtil.deleteFile(string);
            }
            deleteArray.clear();

            inputPath = null;
            mediaInfo = null;
            drawPad = null;

            logoBitmap = null;
            textAdd = null;
            bgMusicInfo = null;
        }
    }

    public void release() {
        stop();
    }

    private long getVdieoDurationUs()
    {
        if(mediaInfo !=null){
            long du = (long) (mediaInfo.vDuration * 1000 * 1000);
            long aDuration = (long) (mediaInfo.aDuration * 1000 * 1000);
            if (aDuration > 0) {
                du = Math.min(du, aDuration);
            }
            return du;
        }else{
            LSOLog.w("获取视频时长错误...");
            return 1000;
        }
    }
    /**
     * 增加图片图层
     */
    private void addBitmapLayer() {
        // 如果需要增加图片.
        if (logoBitmap != null) {
            logoBmpLayer = drawPad.addBitmapLayer(logoBitmap);
            if (logoBmpLayer != null) {
                int w = logoBmpLayer.getLayerWidth();
                int h = logoBmpLayer.getLayerHeight();
                if (logoPosition == LOGO_POSITION_LELF_TOP) { // 左上角.
                    logoBmpLayer.setPosition(w / 2.0f, h / 2.0f); // setPosition设置的是当前中心点的方向;
                } else if (logoPosition == LOGO_POSITION_LEFT_BOTTOM) { // 左下角
                    logoBmpLayer.setPosition(w / 2.0f, logoBmpLayer.getPadHeight()
                            - h / 2.0f);
                } else if (logoPosition == LOGO_POSITION_RIGHT_TOP) { // 右上角
                    logoBmpLayer.setPosition(
                            logoBmpLayer.getPadWidth() - w / 2.0f, h / 2.0f);

                } else if (logoPosition == LOGO_POSITION_RIGHT_BOTTOM) { // 右下角
                    logoBmpLayer.setPosition(
                            logoBmpLayer.getPadWidth() - w / 2.0f,
                            logoBmpLayer.getPadHeight() - h / 2.0f);
                } else {
                    LSOLog.e( "logo默认居中显示");
                }
            }
        }
    }

    /**
     * 增加Android的Canvas类图层.
     */
    private void addCanvasLayer() {
        if (textAdd != null) {
            canvasLayer = drawPad.addCanvasLayer();

            canvasLayer.addCanvasRunnable(new CanvasRunnable() {
                @Override
                public void onDrawCanvas(CanvasLayer layer, Canvas canvas,
                                         long currentTimeUs) {
                    Paint paint = new Paint();
                    paint.setColor(Color.RED);
                    paint.setAntiAlias(true);
                    paint.setTextSize(20);
                    canvas.drawText(textAdd, 20, 20, paint);
                }
            });
        }
    }

    /**
     * 在运行drawpad的情况下, 增加音频的参数.
     */
    private void addAudioParamter() {
        if (drawPad != null && bgMusicInfo != null) {
            /**
             * 第一步: 增加一个音频;
             */
            AudioLayer source = null;
            if (bgMusicEndUs > bgMusicStartUs && bgMusicStartUs > 0) {
                source = drawPad.addAudioLayer(bgMusicPath,0,bgMusicStartUs,bgMusicEndUs - bgMusicStartUs);
            } else {
                source = drawPad.addAudioLayer(bgMusicPath);
            }

            if(source!=null){
                source.setLooping(true);
                source.setVolume(bgMusicVolume);
            }

            /**
             * 第二步:是否保留原有的声音;
             */
            AudioLayer source1 = drawPad.getMainAudioLayer();
            if (source1 != null) {
                if(isMixBgMusic){
                    source1.setVolume(inputVideoVolume);
                }else{
                    source1.setDisabled(true);// 禁止主音频.
                }
            }
        }
    }

    /**
     * 是否仅仅是处理音频.
     *
     * @return
     */
    protected boolean isOnlyDoMusic() {
        // 各种视频参数都没变;
        if (startTimeUs == 0 && cutDurationUs == 0
                && cropX == 0 && cropY == 0 && cropWidth == 0
                && cropHeight == 0 && videoFilter == null && logoBitmap == null
                && scaleWidth == 0 && scaleHeight == 0
                && videoBitRate==0
                && textAdd == null
                && timeStretchArray == null && timeFreezeArray == null
                && timeRepeatArray == null
                && !isEditModeVideo) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 开启音频容器;
     */
    protected boolean startAudioPad() {

        if (bgMusicInfo != null) {

            audioPad = new AudioPadExecute(context, inputPath, !isMixBgMusic);
            if (inputVideoVolume != 1.0f) {
                audioPad.getMainSource().setVolume(inputVideoVolume);
            }
            /**
             * 增加另一个音频;
             */
            AudioLayer subsource = null;
            if (bgMusicEndUs > bgMusicStartUs && bgMusicStartUs > 0) {
                subsource = audioPad.addAudioLayer(bgMusicPath,0,bgMusicStartUs,bgMusicEndUs - bgMusicStartUs);
            } else {
                subsource = audioPad.addAudioLayer(bgMusicPath, true);
            }
            subsource.setVolume(bgMusicVolume);
            subsource.setLooping(true);

            /**
             * 设置各种回调
             */
            audioPad.setOnAudioPadProgressListener(new onAudioPadProgressListener() {

                @Override
                public void onProgress(AudioPad v, long currentTimeUs) {
                    if (onProgressListener != null) {
                        if (onProgressListener != null && isExecuting) {
                            int percent = (int)(currentTimeUs*100 /(mediaInfo.aDuration*1000*1000));
                            onProgressListener.onLanSongSDKProgress(currentTimeUs,percent);
                        }
                    }
                }
            });
            audioPad.setOnAudioPadCompletedListener(new AudioPadExecute.onAudioPadExecuteCompletedListener() {
                @Override
                public void onCompleted(String path) {
                    if (onCompletedListener != null && isExecuting) {
                        isExecuting = false;
                        onCompletedListener.onLanSongSDKCompleted(path);
                    }

                }
            });
            return audioPad.start();
        } else {
            return false;
        }
    }
    boolean isNotCheckPadSize=false;
    public void setNotCheckDrawPadSize(boolean is){
        isNotCheckPadSize=is;
    }
    /**
     * 举例

     VideoOneDo oneDo= null;
     oneDo = new VideoOneDo(getApplicationContext(),"/sdcard/d1.mp4");
     oneDo.setOnVideoOneDoProgressListener(new OnLanSongSDKProgressListener() {
    @Override
    public void onLanSongSDKProgress(VideoOneDo v, float percent) {
    }
    });
     oneDo.setOnVideoOneDoCompletedListener(new OnLanSongSDKCompletedListener() {
    @Override
    public void onLanSongSDKCompleted(VideoOneDo v, String dstVideo) {

    Intent intent = new Intent(ListMainActivity.this, VideoPlayerActivity.class);
    intent.putExtra("videopath", dstVideo);
    startActivity(intent);
    }
    });

     oneDo.setStartPostionUs(3*1000*1000);
     oneDo.setCutDurationUs(2*1000*1000);

     LanSongFilter filter=new LanSongIF1977Filter(getApplicationContext());
     oneDo.setFilter(filter);
     oneDo.setLogo(BitmapFactory.decodeResource(getResources(),R.drawable.ic_launcher),VideoOneDo.LOGO_POSITION_RIGHT_BOTTOM);

     oneDo.setText("杭州蓝松科技abc123");
     oneDo.setBitrate(2*1024*1204);
     oneDo.setBackGroundMusic("/sdcard/hongdou.mp3",false,3.0f);
     oneDo.setScaleWidth(640,640);
     oneDo.setCropRect((544-480)/2,(960-480)/2,480,480);
     oneDo.start();
     */
}
