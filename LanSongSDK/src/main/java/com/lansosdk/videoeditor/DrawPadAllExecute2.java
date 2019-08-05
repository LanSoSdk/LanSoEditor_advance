package com.lansosdk.videoeditor;

import android.content.Context;
import android.graphics.Bitmap;

import com.lansosdk.box.AudioLayer;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.CanvasLayer;
import com.lansosdk.box.DataLayer;
import com.lansosdk.box.DrawPadAllRunnable2;
import com.lansosdk.box.GifLayer;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LSOVideoOption;
import com.lansosdk.box.Layer;
import com.lansosdk.box.MVLayer;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.OnLanSongSDKThreadProgressListener;
import com.lansosdk.box.VideoFrameLayer;
import com.lansosdk.box.YUVLayer;

/**
 * 自动执行容器.
 * 注意:此方法没有做视频帧重采样, 可能部分视频的声音和画面不一致.
 * 注意:此方法没有做视频帧重采样, 可能部分视频的声音和画面不一致.
 * 注意:此方法没有做视频帧重采样, 可能部分视频的声音和画面不一致.
 * 注意:此方法没有做视频帧重采样, 可能部分视频的声音和画面不一致.
 */
public class DrawPadAllExecute2 {

    private boolean startSuccess;
    private DrawPadAllRunnable2 runnable;
    private String padDstPath;
    private int padWidth;
    private int padHeight;
    /**
     *  构造方法
     *
     * 注意:此方法没有做视频帧重采样, 可能部分视频的声音和画面不一致.
     * @param ctx
     * @param padWidth 容器的宽度, 即最后生成视频的宽度 强烈建议最大值是720P
     * @param padHeight 容器的高度,即最后生成视频的高度 强烈建议最大值是720P
     * @param durationUS 容器的长度,  最后生成视频的长度;单位微秒;
     */
    public DrawPadAllExecute2(Context ctx, int padWidth, int padHeight, long  durationUS) {


        LanSongFileUtil.deleteFile(padDstPath);
        padDstPath=LanSongFileUtil.createMp4FileInBox();
        runnable=new DrawPadAllRunnable2(ctx,padWidth,padHeight,durationUS);

        this.padWidth=padWidth;
        this.padHeight=padHeight;
    }

    /**
     * 设置帧率
     * [不建议使用]
     * @param rate
     */
    public void setFrameRate(int rate) {
        if(runnable!=null){
            runnable.setDrawPadFrameRate(rate);
        }
    }
    /**
     * 获取容器的宽度
     * @return
     */
    public int getPadWidth(){
        return padWidth;
    }

    /**
     * 获取容器的高度;
     * @return
     */
    public int getPadHeight(){
        return padHeight;
    }
    /**
     * 设置码率
     * [可选,不建议设置]
     * @param bitrate 码率;
     */
    public void setEncodeBitrate(int bitrate) {
        if(runnable!=null){
            runnable.setEncodeBitrate(bitrate);
        }
    }
    /**
     * 增加图片图层
     * @param bmp  图片
     * @param startTimeUs 从容器的什么时间开始增加
     * @param endTimeUs 在容器的什么时间消失, 如果到文件尾,请输入Long.MAX_VALUE
     * @return
     */
    public BitmapLayer addBitmapLayer(Bitmap bmp,long startTimeUs,long endTimeUs) {
        if (runnable != null && setup()) {
            return runnable.addBitmapLayer(bmp,startTimeUs,endTimeUs);
        }else{
            return null;
        }
    }

    /**
     * 增加图片图层
     * @param bmp 图片.注意图片在内部不会被释放;
     * @return 返回图片图层对象
     */
    public BitmapLayer addBitmapLayer(Bitmap bmp) {
        if (runnable != null && setup()) {
            return runnable.addBitmapLayer(bmp,0,Long.MAX_VALUE);
        }else{
            return null;
        }
    }
    /**
     *  增加mv图层
     *  默认是循环
     * @param srcPath
     * @param maskPath
     * @return
     */
    public MVLayer addMVLayer(String srcPath, String maskPath) {
        if (runnable != null && setup()) {
            return runnable.addMVLayer(srcPath,maskPath,0,Long.MAX_VALUE,false);
        }else{
            return null;
        }
    }

    /**
     * 增加视频图层
     * 注意:此方法没有做视频帧重采样, 可能部分视频的声音和画面不一致.
     * @param option 在增加前,设置裁剪时长, 裁剪画面, 缩放, 循环等;
     * @return 返回视频图层
     */
    public VideoFrameLayer addVideoLayer(LSOVideoOption option) {
        if (runnable != null && option!=null && setup()) {
            return runnable.addVideoLayer(option,0,Long.MAX_VALUE,false,false);
        }else{
            return null;
        }
    }

    /**
     * 增加视频图层
     * 注意:此方法没有做视频帧重采样, 可能部分视频的声音和画面不一致.
     * @param option 在增加前,设置裁剪时长, 裁剪画面, 缩放, 循环等;
     * @param startTimeUs 从容器的什么时间开始增加
     * @param endTimeUs 在容器的什么时间消失, 如果到文件尾,请输入Long.MAX_VALUE
     * @param holdFirst 当指定时间段后,是否刚开始就一直显示第一帧
     * @param holdLast  当指定时间段过后, 是否一直显示最后一帧;
     * @return
     */
    public VideoFrameLayer addVideoLayer(LSOVideoOption option,long startTimeUs,long endTimeUs,boolean holdFirst,boolean holdLast) {
        if (runnable != null && option!=null && setup()) {
            return runnable.addVideoLayer(option,startTimeUs,endTimeUs,holdFirst,holdLast);
        }else{
            return null;
        }
    }
    /**
     *  增加mv图层
     * 注意:此方法没有做视频帧重采样, 可能部分视频的声音和画面不一致.
     * @param srcPath
     * @param maskPath
     * @param startTimeUs 从容器的什么时间开始增加
     * @param endTimeUs 在容器的什么时间消失, 如果到文件尾,请输入Long.MAX_VALUE
     * @return
     */
    public MVLayer addMVLayer(String srcPath, String maskPath,long startTimeUs,long endTimeUs) {
        if (runnable != null && setup()) {
            return runnable.addMVLayer(srcPath,maskPath,startTimeUs,endTimeUs,false);
        }else{
            return null;
        }
    }

    /**
     * 增加mv图层
     * 注意:此方法没有做视频帧重采样, 可能部分视频的声音和画面不一致.
     * @param srcPath
     * @param maskPath
     * @param startTimeUs 从容器的什么时间开始增加
     * @param endTimeUs 在容器的什么时间消失, 如果到文件尾,请输入Long.MAX_VALUE
     * @param mute  如果mv中有声音, 是否要静音;默认不静音;
     * @return
     */
    public MVLayer addMVLayer(String srcPath, String maskPath,long startTimeUs,long endTimeUs, boolean mute) {
        if (runnable != null && setup()) {
            return runnable.addMVLayer(srcPath,maskPath,startTimeUs,endTimeUs,mute);
        }else{
            return null;
        }
    }
    /**
     * 增加数据图层/RGBA格式
     * @param dataWidth  数据的宽度
     * @param dataHeight 数据的高度
     * @param startTimeUs 从容器的什么时间开始增加
     * @param endTimeUs 在容器的什么时间消失, 如果到文件尾,请输入Long.MAX_VALUE
     * @return
     */
    public DataLayer addDataLayer(int dataWidth, int dataHeight,long startTimeUs,long endTimeUs) {
        if (runnable != null && setup()) {
            return runnable.addDataLayer(dataWidth,dataHeight,startTimeUs,endTimeUs);
        }else{
            return null;
        }
    }
    /**
     *增加gif图层
     * @param gifPath gif文件的完整路径
     * @param startTimeUs 从容器的什么时间开始增加
     * @param endTimeUs 在容器的什么时间消失, 如果到文件尾,请输入Long.MAX_VALUE
     * @return
     */
    public GifLayer addGifLayer(String gifPath,long startTimeUs,long endTimeUs) {
        if (runnable != null && setup()) {
            return runnable.addGifLayer(gifPath,startTimeUs,endTimeUs);
        }else{
            return null;
        }
    }

    /**
     * 增加gif图层
     * @param gifPath gif文件的完整路径
     * @return
     */
    public GifLayer addGifLayer(String gifPath) {
        if (runnable != null && setup()) {
            return runnable.addGifLayer(gifPath,0,Long.MAX_VALUE);
        }else{
            return null;
        }
    }

    /**
     * 增加Gif图层
     * @param resId
     * @return
     */
    public GifLayer addGifLayer(int resId) {
        if (runnable != null && setup()) {
            return runnable.addGifLayer(resId,0,Long.MAX_VALUE);
        }else{
            return null;
        }
    }

    /**
     * 增加canvas图层
     * @return
     */
    public CanvasLayer addCanvasLayer() {
        if (runnable != null && setup()) {
            return runnable.addCanvasLayer();
        }else{
            return null;
        }
    }

    /**
     * 增加yuv图层
     * 当前仅支持NV12格式
     * @param width 宽度
     * @param height 高度
     * @param startTimeUs 从容器的什么时间开始增加
     * @param endTimeUs 在容器的什么时间消失, 如果到文件尾,请输入Long.MAX_VALUE
     * @return
     */
    public YUVLayer addYUVLayer(int width, int height,long startTimeUs,long endTimeUs) {
        if (runnable != null && setup()) {
            return runnable.addYUVLayer(width,height,startTimeUs,endTimeUs);
        }else{
            return null;
        }
    }
    public AudioLayer addAudioLayer(String srcPath) {
        if(runnable!=null){

            AudioLayer layer= runnable.addAudioLayer(srcPath,0,0, Long.MAX_VALUE);
            return layer;
        }else{
            return null;
        }
    }
    /**
     * 增加其他音频;
     * 支持mp4,wav,mp3,m4a文件;
     *
     * @param srcPath
     * @param isLoop  是否循环;
     * @return 返回增加后音频层, 可以用来设置音量,快慢,变声等.
     */
    public AudioLayer addAudioLayer(String srcPath, boolean isLoop) {
        if(runnable!=null){

            AudioLayer layer= runnable.addAudioLayer(srcPath,0,0, Long.MAX_VALUE);
            if(layer!=null){
                layer.setLooping(isLoop);
            }
            return layer;
        }else{
            return null;
        }
    }
    /**
     * 增加其他音频;
     * 支持mp4,wav,mp3,m4a文件;
     * @param srcPath 声音的完整路径
     * @param isLoop 是否循环
     * @param volume 音频的音量; 范围是0--10; 1.0正常;大于1.0提高音量;小于1.0降低音量;
     * @return
     */
    public AudioLayer addAudioLayer(String srcPath, boolean isLoop, float volume) {
        if(runnable!=null){
            AudioLayer layer= runnable.addAudioLayer(srcPath,0,0, Long.MAX_VALUE);
            if(layer!=null){
                layer.setLooping(isLoop);
                layer.setVolume(volume);
            }
            return layer;
        }else{
            return null;
        }
    }

    /**
     * 增加音频容器, 从容器的什么位置开始增加,
     *
     * @param srcPath  音频文件,或含有音频的视频文件;
     * @param startPadUs 从容器的什么地方增加这个音频
     * @return 返回增加后音频层, 可以用来设置音量,快慢,变声等.
     */
    public AudioLayer addAudioLayer(String srcPath, long startPadUs) {
        if(runnable!=null && srcPath!=null){
            AudioLayer layer= runnable.addAudioLayer(srcPath,startPadUs,0, Long.MAX_VALUE);
            if(layer==null){
                LSOLog.e("DrawPadAllExecute2 addAudioLayer error. mediaInfo is:"+ MediaInfo.checkFile(srcPath,true));
            }
            return layer;
        }else{
            return null;
        }
    }

    /**
     * 把音频的 指定时间段, 增加到audiopad音频容器里.
     * 如果有循环或其他操作, 可以在获取的AudioLayer对象中设置.
     *
     * @param srcPath      音频文件路径, 可以是有音频的视频路径;
     * @param offsetPadUs  从容器的什么时间开始增加.相对容器偏移多少.
     * @param startAudioUs 该音频的开始时间
     * @param endAudioUs   该音频的结束时间. 如果要增加到文件尾,则可以直接填入-1;
     * @return 返回增加后音频层, 可以用来设置音量,快慢,变声等.
     */
    public AudioLayer addAudioLayer(String srcPath, long offsetPadUs,
                                    long startAudioUs, long endAudioUs) {
        if(runnable!=null){
            AudioLayer layer= runnable.addAudioLayer(srcPath,offsetPadUs,startAudioUs, endAudioUs);
            return layer;
        }else{
            return null;
        }
    }
    /**
     * 获取当前容器中的层数.
     * 图层在您addXXXLayer后会增加一层, 在removeLayer后会减少一层;
     * 如果您给图层设置了显示时间段,则在时间段外,该图层还是存在的, 只是不显示出来而已;
     * @return
     */
    public int getLayerSize(){
        if(runnable!=null){
            return runnable.getLayerSize();
        }else{
            return 0;
        }
    }
    /**
     * 把图层放到容器的最底部
     * @param layer
     */
    public void bringToBack(Layer layer) {
        if(runnable!=null){
            runnable.bringToBack(layer);
        }
    }

    /**
     * 把图层放到容器的最外层,
     * @param layer
     */
    public void bringToFront(Layer layer) {
        if(runnable!=null){
            runnable.bringToFront(layer);
        }
    }

    /**
     * 设置图层在容器中的第几层;
     * @param layer
     * @param position
     */
    public void changeLayerPosition(Layer layer, int position) {
        if(runnable!=null){
            runnable.changeLayerPosition(layer,position);
        }
    }

    /**
     * 交互两个图层的上下层关系;
     * @param first
     * @param second
     */
    public void swapTwoLayerPosition(Layer first, Layer second) {
        if(runnable!=null){
            runnable.swapTwoLayerPosition(first,second);
        }
    }
    /**
     * 设置进度监听,  ---经过handle机制,
     * 进度返回的两个参数 long类型是:当前时间戳,单位微秒; int类型是:当前处理的百分比;
     * @param listener
     */
    public void setOnLanSongSDKProgressListener(OnLanSongSDKProgressListener listener) {
        if(runnable!=null){
            runnable.setOnLanSongSDKProgressListener(listener);
        }
    }

    /**
     * 设置进度监听 ----不经过handle机制
     * 进度返回的两个参数 long类型是:当前时间戳,单位微秒; int类型是:当前处理的百分比;
     * @param listener
     */
    public void setOnLanSongSDKThreadProgressListener(OnLanSongSDKThreadProgressListener listener) {
        if(runnable!=null){
            runnable.setOnLanSongSDKThreadProgressListener(listener);
        }
    }

    /**
     * 完成回调
     * @param listener
     */
    public void setOnLanSongSDKCompletedListener(OnLanSongSDKCompletedListener listener) {
        if(runnable!=null){
            runnable.setOnLanSongSDKCompletedListener(listener);
        }
    }

    /**
     * 错误回调
     * @param listener
     */
    public void setOnLanSongSDKErrorListener(OnLanSongSDKErrorListener listener) {
        if(runnable!=null){
            runnable.setOnLanSongSDKErrorListener(listener);
        }
    }

    public void removeLayer(Layer layer) {
        if(runnable!=null){
            runnable.removeLayer(layer);
        }
    }

    /**
     * 删除所有图层
     */
    public void removeAllLayer() {
        if (runnable != null) {
            runnable.removeAllLayer();
        }
    }
    public boolean isRunning() {
        if(runnable!=null){
            return runnable.isRunning();
        }else{
            return false;
        }
    }
    /**
     * 开始执行
     * @return
     */
    public boolean start() {
        return  runnable!=null && runnable.start();
    }
    /**
     * 取消执行
     */
    public void cancel() {
        if(runnable!=null){
            runnable.cancel();
            runnable.release();
            runnable=null;
            startSuccess=false;
        }
    }
    /**
     * 释放;
     */
    public void release() {
        if(runnable!=null){
            runnable.release();
            runnable=null;
            startSuccess=false;
        }
    }

    /**
     * 不检查容器尺寸.
     * 我们默认内部会16字节对齐; 如果调用此方法,则以您设置的宽高为准;
     * [不建议使用]
     */
    public void setNotCheckDrawPadSize() {
       if(runnable!=null){
           runnable.setNotCheckDrawPadSize();
       }
    }

    /**
     * 设置编码;
     */
    public void setNotCheckBitRate() {
        if(runnable!=null){
            runnable.setNotCheckBitRate();
        }
    }
    //---------------------------------------------------------------------------
    private synchronized boolean setup(){
        if(runnable!=null && !runnable.isRunning() && !startSuccess){
            runnable.setup();
            startSuccess=true;
        }
        return startSuccess;
    }
    //---------------------------test Demo测试例子------------------------------------------------
    /**


     DrawPadAllExecute2 allExecute;
     private void testAllexecute() throws Exception {
     //在其他手机上测试下.
     final String dsPath = LanSongFileUtil.createMp4FileInBox();

     allExecute = new DrawPadAllExecute2(getApplicationContext(), 720, 1280, 25 * 1000 * 1000);
     allExecute.setOnLanSongSDKProgressListener(new OnLanSongSDKProgressListener() {
    @Override
    public void onLanSongSDKProgress(long ptsUs, int percent) {
    //                Log.e("TAG", "------ptsUs: "+ptsUs+ " percent :"+percent);
    }
    });

     allExecute.setOnLanSongSDKCompletedListener(new OnLanSongSDKCompletedListener() {
    @Override
    public void onLanSongSDKCompleted(String dstVideo) {
    MediaInfo.checkFileReturnString(dstVideo);
    }
    });

     Bitmap bmp23 = BitmapFactory.decodeResource(getResources(), R.drawable.a1);
     BitmapLayer layer23 = allExecute.addBitmapLayer(bmp23, 5 * 1000 * 1000, Long.MAX_VALUE);
     layer23.setScaledToPadSize();

     LSOVideoOption option = new LSOVideoOption(copyShanChu(getApplicationContext(),"vcore.mp4"));
     option.setLooping(true);
     option.setCropRect(308,308,411,411);
     VideoFrameLayer layer1 = allExecute.addVideoLayer(option);



     LSOVideoOption option2 = new LSOVideoOption(SDCARD.file("kuaishou_H31.mp4"));
     option2.setScaleSize(360,640);
     VideoFrameLayer layer2 = allExecute.addVideoLayer(option2);
     layer2.setPosition(LSOLayerPosition.LeftBottom);




     LSOVideoOption option3 = new LSOVideoOption(SDCARD.file("d1.mp4"));
     option3.setScaleSize(320,320);
     VideoFrameLayer layer3 = allExecute.addVideoLayer(option3);
     layer3.setPosition(LSOLayerPosition.RightTop);



     layer1.setMaskBitmapWithRecycle(BitmapFactory.decodeResource(getResources(),R.drawable.ls_logo),true);
     allExecute.start();


     }

     private ShowHeart showHeart;
     private CanvasLayer canvasLayer;

     private void addCanvasLayer() {

     canvasLayer = allExecute.addCanvasLayer();
     if (canvasLayer != null) {

     canvasLayer.setClearCanvas(false);

     canvasLayer.addCanvasRunnable(new CanvasRunnable() {
    @Override
    public void onDrawCanvas(CanvasLayer layer, Canvas canvas, long currentTimeUs) {
    Paint paint = new Paint();
    paint.setColor(Color.RED);
    paint.setAntiAlias(true);
    paint.setTextSize(50);
    canvas.drawText("蓝松短视频演示之<任意绘制>", 20, canvasLayer.getPadHeight() - 200, paint);
    }
    });
     showHeart = new ShowHeart(this, canvasLayer.getPadWidth(), canvasLayer.getPadHeight());
     canvasLayer.addCanvasRunnable(new CanvasRunnable() {

    @Override
    public void onDrawCanvas(CanvasLayer layer, Canvas canvas, long currentTimeUs) {
    showHeart.drawTrack(canvas);
    }
    });
     }
     }


     */
}
