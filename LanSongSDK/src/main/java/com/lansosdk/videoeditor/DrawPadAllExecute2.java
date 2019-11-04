package com.lansosdk.videoeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;

import com.lansosdk.box.AudioLayer;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.CanvasLayer;
import com.lansosdk.box.DataLayer;
import com.lansosdk.box.DrawPadAllRunnable2;
import com.lansosdk.box.DrawPadBGRunnable;
import com.lansosdk.box.GifLayer;
import com.lansosdk.box.LSOBitmapAsset;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LSOVideoOption;
import com.lansosdk.box.Layer;
import com.lansosdk.box.MVCacheLayer;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.OnLanSongSDKThreadProgressListener;
import com.lansosdk.box.VideoFrameLayer;
import com.lansosdk.box.YUVLayer;

/**
 * 所有的图层,转换为视频
 * 属于后台执行类;
 */
public class DrawPadAllExecute2 {

    private boolean startSuccess;
    private DrawPadAllRunnable2 runnable;
    private DrawPadBGRunnable pixelRunnable;
    private String padDstPath;
    private int padWidth;
    private int padHeight;

    /**
     * 强制使用老版本.
     */
    public static  boolean forceUseOLD =true;

    /**
     *  构造方法
     *
     * 如果您仅仅对一个视频做处理, 则可以设置为这个视频的宽高和时长;
     * @param ctx
     * @param padWidth 容器的宽度, 即最后生成视频的宽度 强烈建议最大值是720P
     * @param padHeight 容器的高度,即最后生成视频的高度 强烈建议最大值是720P
     * @param durationUS 容器的长度,  最后生成视频的长度;单位微秒;
     */
    public DrawPadAllExecute2(Context ctx, int padWidth, int padHeight, long  durationUS) {
        LanSongFileUtil.deleteFile(padDstPath);
        padDstPath=LanSongFileUtil.createMp4FileInBox();
        int w,h;
        w=padWidth;
        h=padHeight;

        if(padWidth%2 !=0 || padHeight%2 !=0){
            LSOLog.e("drawPad size must be a multiple of 2 ");
            w= w >> 1;
            w *=2;
            h= h >> 1;
            h *=2;
        }

        //小于等于8.0 支持nv21, 麒麟处理器.
        if(!forceUseOLD && Build.VERSION.SDK_INT<=Build.VERSION_CODES.O && LanSoEditor.isQiLinSoc() && VideoEditor.isSupportNV21ColorFormat() ){
            pixelRunnable =new DrawPadBGRunnable(ctx,w,h,durationUS);
            LSOLog.d("DrawPadAllExecute2 run  new free_pixel_mode Runnable...");
        }else{
            runnable=new DrawPadAllRunnable2(ctx,w,h,durationUS);
            LSOLog.d("DrawPadAllExecute2 run  COMMON  Runnable(DrawPadAllRunnable2)...");
        }
        this.padWidth= w;
        this.padHeight=h;
    }

    /**
     * 设置帧率
     * [不建议使用]
     * @param rate
     */
    public void setFrameRate(int rate) {
        if(runnable!=null){
            runnable.setFrameRate(rate);
        }else if(pixelRunnable !=null){
            pixelRunnable.setFrameRate(rate);
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
        }else  if(pixelRunnable !=null){
            pixelRunnable.setEncodeBitrate(bitrate);
        }
    }
    //---------------------------容器背景颜色;
    protected float padBGRed =0.0f;
    protected float padBGGreen =0.0f;
    protected float padBGBlur =0.0f;
    protected float padBGAlpha =1.0f;

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
        if(runnable!=null){
            runnable.setDrawPadBackGroundColor(r,g,b,a);
        }else  if(pixelRunnable !=null){
            pixelRunnable.setDrawPadBackGroundColor(r,g,b,a);
        }
    }
    /**
     * 已废弃,请用LSOBitmapAsset类型的addBitmapLayer
     */
    @Deprecated
    public BitmapLayer addBitmapLayer(Bitmap bmp,long startTimeUs,long endTimeUs) {
        if (runnable != null && setup()) {
            return runnable.addBitmapLayer(bmp, startTimeUs, endTimeUs);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addBitmapLayer(bmp,startTimeUs,endTimeUs);
        }else{
            return null;
        }
    }

    /**
     * 已废弃,请用 String类型的addBitmapLayer
     */
    @Deprecated
    public BitmapLayer addBitmapLayer(Bitmap bmp) {
        if (runnable != null && setup()) {
            return runnable.addBitmapLayer(bmp,0,Long.MAX_VALUE);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addBitmapLayer(bmp,0,Long.MAX_VALUE);
        }else {
            return null;
        }
    }

    /**
     * 增加图片图层
     * @param asset  图片路径,
     * @param startTimeUs 从容器的什么时间开始增加
     * @param endTimeUs 在容器的什么时间消失, 如果到文件尾,请输入Long.MAX_VALUE
     * @return
     */
    public BitmapLayer addBitmapLayer(LSOBitmapAsset asset, long startTimeUs, long endTimeUs) {
        if (runnable != null && setup()) {
            return runnable.addBitmapLayer(asset,startTimeUs,endTimeUs);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addBitmapLayer(asset,startTimeUs,endTimeUs);
        }else {
            return null;
        }
    }

    /**
     * 增加图片图层
     * @param asset 图片路径
     * @return 返回图片图层对象
     */
    public BitmapLayer addBitmapLayer(LSOBitmapAsset asset) {
        if (runnable != null && setup()) {
            return runnable.addBitmapLayer(asset,0,Long.MAX_VALUE);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addBitmapLayer(asset,0,Long.MAX_VALUE);
        }else {
            return null;
        }
    }


    /**
     * 请用 addBitmapLayer(LSOBitmapAsset asset, long startTimeUs, long endTimeUs)
     */
    @Deprecated
    public BitmapLayer addBitmapLayer(String path, long startTimeUs, long endTimeUs)  throws  Exception{

        LSOBitmapAsset asset=new LSOBitmapAsset(path);
        if (runnable != null && setup()) {
            return runnable.addBitmapLayer(asset,startTimeUs,endTimeUs);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addBitmapLayer(asset,startTimeUs,endTimeUs);
        }else {
            return null;
        }
    }

    /**
     * 请用BitmapLayer addBitmapLayer(LSOBitmapAsset asset)
     * */
    @Deprecated
    public BitmapLayer addBitmapLayer(String path) throws Exception{
        LSOBitmapAsset asset=new LSOBitmapAsset(path);
        if (runnable != null && setup()) {
            return runnable.addBitmapLayer(asset,0,Long.MAX_VALUE);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addBitmapLayer(asset,0,Long.MAX_VALUE);
        }else {
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
    public MVCacheLayer addMVLayer(String srcPath, String maskPath) {
        if (runnable != null && setup()) {
            return runnable.addMVLayer(srcPath,maskPath,0,Long.MAX_VALUE,false);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addMVLayer(srcPath,maskPath,0,Long.MAX_VALUE,false);
        }else {
            return null;
        }
    }

    /**
     * 增加视频图层
     * @param option 在增加前,设置裁剪时长, 裁剪画面, 缩放, 循环等;
     * @return 返回视频图层
     */
    public VideoFrameLayer addVideoLayer(LSOVideoOption option) {
        if (runnable != null && option!=null && setup()) {
            return runnable.addVideoLayer(option,0,Long.MAX_VALUE,false,false);
        }else if (pixelRunnable != null && option!=null && setup()) {
            return pixelRunnable.addVideoLayer(option,0,Long.MAX_VALUE,false,false);
        }else {
            return null;
        }
    }

    /**
     * 增加视频图层
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
        }else if (pixelRunnable != null && option!=null && setup()) {
            return pixelRunnable.addVideoLayer(option,startTimeUs,endTimeUs,holdFirst,holdLast);
        }else {
            return null;
        }
    }
    /**
     *  增加mv图层
     * @param srcPath
     * @param maskPath
     * @param startTimeUs 从容器的什么时间开始增加
     * @param endTimeUs 在容器的什么时间消失, 如果到文件尾,请输入Long.MAX_VALUE
     * @return
     */
    public MVCacheLayer addMVLayer(String srcPath, String maskPath, long startTimeUs, long endTimeUs) {
        if (runnable != null && setup()) {
            return runnable.addMVLayer(srcPath,maskPath,startTimeUs,endTimeUs,false);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addMVLayer(srcPath,maskPath,startTimeUs,endTimeUs,false);
        }else {
            return null;
        }
    }

    /**
     * 增加mv图层
     * @param srcPath
     * @param maskPath
     * @param startTimeUs 从容器的什么时间开始增加
     * @param endTimeUs 在容器的什么时间消失, 如果到文件尾,请输入Long.MAX_VALUE
     * @param mute  如果mv中有声音, 是否要静音;默认不静音;
     * @return
     */
    public MVCacheLayer addMVLayer(String srcPath, String maskPath, long startTimeUs, long endTimeUs, boolean mute) {
        if (runnable != null && setup()) {
            return runnable.addMVLayer(srcPath,maskPath,startTimeUs,endTimeUs,mute);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addMVLayer(srcPath,maskPath,startTimeUs,endTimeUs,mute);
        }else {
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
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addDataLayer(dataWidth,dataHeight,startTimeUs,endTimeUs);
        }else {
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
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addGifLayer(gifPath,startTimeUs,endTimeUs);
        }else {
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
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addGifLayer(gifPath,0,Long.MAX_VALUE);
        }else {
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
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addGifLayer(resId,0,Long.MAX_VALUE);
        }else {
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
        }else  if (pixelRunnable != null && setup()) {
            return pixelRunnable.addCanvasLayer();
        }else {
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
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addYUVLayer(width,height,startTimeUs,endTimeUs);
        }else {
            return null;
        }
    }
    public AudioLayer addAudioLayer(String srcPath) {
        if(runnable!=null){
            return runnable.addAudioLayer(srcPath,0,0, Long.MAX_VALUE);
        }else  if(pixelRunnable !=null){
            return pixelRunnable.addAudioLayer(srcPath,0,0, Long.MAX_VALUE);
        }else {
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
        }else if(pixelRunnable !=null){
            AudioLayer layer= pixelRunnable.addAudioLayer(srcPath,0,0, Long.MAX_VALUE);
            if(layer!=null){
                layer.setLooping(isLoop);
            }
            return layer;
        }else {
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
        }else if(pixelRunnable !=null){
            AudioLayer layer= pixelRunnable.addAudioLayer(srcPath,0,0, Long.MAX_VALUE);
            if(layer!=null){
                layer.setLooping(isLoop);
                layer.setVolume(volume);
            }
            return layer;
        }else {
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
        }else if(pixelRunnable !=null && srcPath!=null){
            AudioLayer layer= pixelRunnable.addAudioLayer(srcPath,startPadUs,0, Long.MAX_VALUE);
            if(layer==null){
                LSOLog.e("DrawPadAllExecute2 addAudioLayer error. mediaInfo is:"+ MediaInfo.checkFile(srcPath,true));
            }
            return layer;
        }else {
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
        }else if(pixelRunnable !=null){
            AudioLayer layer= pixelRunnable.addAudioLayer(srcPath,offsetPadUs,startAudioUs, endAudioUs);
            return layer;
        }else {
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
        }else if(pixelRunnable !=null){
            return pixelRunnable.getLayerSize();
        }else {
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
        }else if(pixelRunnable !=null){
            pixelRunnable.bringToBack(layer);
        }
    }

    /**
     * 把图层放到容器的最外层,
     * @param layer
     */
    public void bringToFront(Layer layer) {
        if(runnable!=null){
            runnable.bringToFront(layer);
        }else if(pixelRunnable !=null){
            pixelRunnable.bringToFront(layer);
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
        }else if(pixelRunnable !=null){
            pixelRunnable.changeLayerPosition(layer,position);
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
        }else if(pixelRunnable !=null){
            pixelRunnable.swapTwoLayerPosition(first,second);
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
        }else  if(pixelRunnable !=null){
            pixelRunnable.setOnLanSongSDKProgressListener(listener);
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
        }else if(pixelRunnable !=null){
            pixelRunnable.setOnLanSongSDKThreadProgressListener(listener);
        }
    }

    /**
     * 完成回调
     * @param listener
     */
    public void setOnLanSongSDKCompletedListener(OnLanSongSDKCompletedListener listener) {
        if(runnable!=null){
            runnable.setOnLanSongSDKCompletedListener(listener);
        }else  if(pixelRunnable !=null){
            pixelRunnable.setOnLanSongSDKCompletedListener(listener);
        }
    }

    /**
     * 错误回调
     * @param listener
     */
    public void setOnLanSongSDKErrorListener(OnLanSongSDKErrorListener listener) {
        if(runnable!=null){
            runnable.setOnLanSongSDKErrorListener(listener);
        }else if(pixelRunnable !=null){
            pixelRunnable.setOnLanSongSDKErrorListener(listener);
        }
    }

    public void removeLayer(Layer layer) {
        if(runnable!=null){
            runnable.removeLayer(layer);
        }else  if(pixelRunnable !=null){
            pixelRunnable.removeLayer(layer);
        }
    }

    /**
     * 删除所有图层
     */
    public void removeAllLayer() {
        if (runnable != null) {
            runnable.removeAllLayer();
        }else  if (pixelRunnable != null) {
            pixelRunnable.removeAllLayer();
        }
    }
    public boolean isRunning() {
        if(runnable!=null){
            return runnable.isRunning();
        }else  if(pixelRunnable !=null){
            return pixelRunnable.isRunning();
        }else {
            return false;
        }
    }
    /**
     * 开始执行
     * @return
     */
    public boolean start() {
        if(runnable!=null){
            return runnable.start();
        }else if(pixelRunnable !=null){
            return pixelRunnable.start();
        }else{
            return false;
        }
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
        }else if(pixelRunnable !=null){
            pixelRunnable.cancel();
            pixelRunnable.release();
            pixelRunnable =null;
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
        }else if(pixelRunnable !=null){
            pixelRunnable.release();
            pixelRunnable =null;
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
        }else if(pixelRunnable !=null){
            pixelRunnable.setNotCheckDrawPadSize();
        }
    }

    //---------------------------------------------------------------------------
    private synchronized boolean setup(){
        if(runnable!=null && !runnable.isRunning() && !startSuccess){
            runnable.setup();
            startSuccess=true;
        }else if(pixelRunnable !=null && !pixelRunnable.isRunning() && !startSuccess){
            pixelRunnable.setup();
            startSuccess=true;
        }
        return startSuccess;
    }
    //---------------------------test Demo测试例子------------------------------------------------
    /**


     DrawPadAllExecute2 allExecute;
     private void testAllexecute() throws Exception {
     //在其他手机上测试下.
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
    MediaInfo.checkFile(dstVideo);
    }
    });

     Bitmap bmp23 = BitmapFactory.decodeResource(getResources(), R.drawable.a1);
     BitmapLayer layer23 = allExecute.addBitmapLayer(bmp23, 5 * 1000 * 1000, Long.MAX_VALUE);
     layer23.setScaledToPadSize();

     LSOVideoOption option = new LSOVideoOption(copyShanChu(getApplicationContext(),"vcore.mp4"));
     option.setLooping(true);
     option.setCropRect(308,308,411,411);
     VideoFrameLayer layer1 = allExecute.addVideoLayer(option);



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
     showHeart = new ShowHeart(this, canvasLayer.getDrawPadWidth(), canvasLayer.getPadHeight());
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
