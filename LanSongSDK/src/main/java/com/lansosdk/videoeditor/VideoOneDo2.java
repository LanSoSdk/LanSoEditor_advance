package com.lansosdk.videoeditor;

import android.content.Context;
import android.graphics.Bitmap;

import com.lansosdk.LanSongFilter.LanSongFilter;
import com.lansosdk.box.AudioLayer;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.BoxMediaInfo;
import com.lansosdk.box.CanvasLayer;
import com.lansosdk.box.GifLayer;
import com.lansosdk.box.LSOLayerPosition;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.MVLayer;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.OnLanSongSDKThreadProgressListener;
import com.lansosdk.box.OnLayerAlreadyListener;
import com.lansosdk.box.SubLayer;
import com.lansosdk.box.VideoOneDoRunnable;
import com.lansosdk.videoeditor.LanSoEditor;

import java.io.IOException;
import java.util.List;

/**
 * 此类是针对单个视频操作.
 * 此类是针对单个视频操作.
 * 此类是针对单个视频操作.
 *
 *
 * 当前可以完成常见18个功能:
 * 增加音乐, 裁剪时长, 裁剪画面, 缩放, 压缩,增加logo,文字, 设置编辑模式,
 * 设置遮罩, 增加滤镜, 增加美颜,增加mv动画, 增加Gif,提取图片.增加Canvas等.
 */
public class VideoOneDo2 {

    private VideoOneDoRunnable runnable;
    public BoxMediaInfo mediaInfo;
    private int  padWidth;
    private int padHeight;
    /**
     * @param ctx
     * @param path
     * @throws IOException
     */
    public VideoOneDo2(Context ctx, String path) throws Exception {
        runnable=new VideoOneDoRunnable(ctx,path);
        mediaInfo=runnable.getMediaInfo();
        padWidth=mediaInfo.getWidth();
        padHeight=mediaInfo.getHeight();
    }

    public int getVideoWidth(){
        return mediaInfo.getWidth();
    }
    public int getVideoHeight(){
        return mediaInfo.getHeight();
    }
    public long getVideoDurationUs(){
        return (long)(mediaInfo.vDuration *1000*1000);
    }

    /**
     * 设置裁剪时长
     * @param startUs  开始时间
     * @param endUs 结束时间, 如果走到尾,则设置为-1;
     */
    public void setCutDuration(long startUs,long endUs)
    {
        if(runnable!=null){
            runnable.setCutDuration(startUs,endUs);
        }
    }
    /**
     * 设置裁剪画面
     *
     * 如果设置了缩放, 则先裁剪后缩放;
     *
     * @param startX 画面的开始横向坐标,
     * @param startY 画面的开始纵向坐标
     * @param cropW  裁剪多少宽度 最小是32个像素,最大视频宽高
     * @param cropH  裁剪多少高度 最小是32个像素,最大视频宽高
     */
    public void  setCropRect(int startX, int startY, int cropW, int cropH) {

        if(cropH<32 || cropW<32){
            LSOLog.e("setCropRect error  min Size is 32x 32");
            return;
        }

        if(cropH * cropW<=192*160){ //麒麟处理器的裁剪区域是176x144
            LSOLog.e("setCropRect error. qi lin SoC is192*160");
            return;
        }


        if(mediaInfo!=null && startX>=0 && startY>=0 && startX<mediaInfo.getWidth() && startY<mediaInfo.getHeight())
        {
            if(cropW%4!=0 || cropH%4!=0){
                LSOLog.w("setCropRect not a multiple of 4, adjustment:"+ cropW+ " x "+ cropH);
            }
            int cropW2=(startX + cropW) < mediaInfo.getWidth()? cropW:(mediaInfo.getWidth() - startX);
            int cropH2=(startY + cropH)< mediaInfo.getHeight()? cropH :(mediaInfo.getHeight() -startY);

            runnable.setCropRect(startX,startY,cropW2,cropH2);
            padWidth=cropW2;
            padHeight=cropH2;
        }else{
            LSOLog.e("VideoOneDo setCropRect error."+ startX+ " y:"+startY+ " size:"+cropW+ " x "+cropH);
        }
    }
    /**
     * 缩放大小
     * @param w 缩放到的宽度
     * @param h 缩放到的高度
     */
    public void setScaleSize(int w,int h){
        if(runnable!=null){
            padWidth=w;
            padHeight=h;
            if(w%16!=0 || h%16!=0){
                LSOLog.w("not a multiple of 16 set scale size:"+ w+ " x "+ h);
            }
            runnable.setScaleSize(w,h);
        }
    }
    /**
     * 设置编码码率
     * [不建议设置]
     * @param bitRate 码率
     */
    public void setEncoderBitRate(int bitRate) {
      if(runnable!=null){
          runnable.setEncoderBitRate(bitRate);
      }
    }

    /**
     * 设置压缩比;
     * @param percent 0---1.0f
     */
    public void setCompressPercent(float percent){
        if(runnable!=null){
            runnable.setCompressPercent(percent);
        }
    }

    /**
     * 设置视频中的原声音音量
     * @param volume 音量, 1.0为不变. 0.0是静音. 2.0是放大两倍. 5.0是放大五倍
     */
    public void  setVideoVolume(float volume){
        if(runnable!=null){
            runnable.setVideoVolume(volume);
        }
    }
    /**
     * 设置视频静音;
     */
    public void  setVideoMute(){
        if(runnable!=null){
            runnable.setVideoMute();
        }
    }
    /**
     * 增加音频
     * @param srcPath 音频的完整路径(或还有音频的视频路径)
     * @param isLoop 是否循环
     * @return 返回音频图层对象
     */
    public AudioLayer addAudioLayer(String srcPath, boolean isLoop) {
        if(runnable!=null){
            return runnable.addAudioLayer(srcPath,isLoop);
        }else{
            return null;
        }
    }
    /**
     * 增加音频
     * @param srcPath 音频的完整路径(或还有音频的视频路径)
     * @param isLoop 是否循环
     * @param volume 音量.
     * @return
     */
    public AudioLayer addAudioLayer(String srcPath, boolean isLoop, float volume) {
        if(runnable!=null){
            return  runnable.addAudioLayer(srcPath,isLoop,volume);
        }else{
            return null;
        }
    }

    /**
     * 增加音频
     * @param srcPath 音频的完整路径(或还有音频的视频路径)
     * @param startPadUs 从视频的什么时间点增加;
     * @return
     */
    public AudioLayer addAudioLayer(String srcPath, long startPadUs) {
        if(runnable!=null){
            return runnable.addAudioLayer(srcPath,startPadUs);
        }else{
            return null;
        }
    }


    /**
     * 增加音频.
     * 从容器的指定位置开始增加, 增加一段声音;
     * @param srcPath 音频的完整路径(或还有音频的视频路径)
     * @param offsetPadUs 从容器的哪个时间点增加声音
     * @param startAudioUs  增加一段声音的开始位置
     * @param endAudioUs  增加一段声音的结束位置;
     * @return
     */
    public AudioLayer addAudioLayer(String srcPath, long offsetPadUs,
                                    long startAudioUs, long endAudioUs) {
        if(runnable!=null){
            return runnable.addAudioLayer(srcPath, offsetPadUs, startAudioUs,endAudioUs);
        }else{
            return null;
        }
    }
    /**
     * 异步获取当前视频的图层对象.
     *
     * OnLayerAlreadyListener监听的3个方法分别是: Layer图层对象, 容器的宽度, 容器的高度;
     * 此方法的listener运行在内部的GPU线程中, 不可以在此监听中增加UI相关的代码;
     * 请不要在此方法里做过多耗时的操作.
     */
    public void getVideoDataLayerAsync(OnLayerAlreadyListener listener){
        if(runnable!=null){
            runnable.getVideoDataLayerAsync(listener);
        }
    }

    public void  setMaskBitmap(Bitmap bmp){
        if(runnable!=null){
            runnable.setMaskBitmap(bmp);
        }
    }

    public void  setFilter(LanSongFilter filter){
        if(runnable!=null){
            runnable.addFilter(filter);
        }
    }

    /**
     * 增加一张图片,
     * 返回给你根据这张图片创建好的一个图层对象
     * @param bmp 图片
     * @return 返回一个图层对象;
     */
    public BitmapLayer addBitmapLayer(Bitmap bmp) {
        if(runnable!=null){
            return runnable.addBitmapLayer(bmp);
        }else{
            return null;
        }
    }
    /**
     * 增加一张图片,
     * 返回给你根据这张图片创建好的一个图层对象
     * @param bmp  图片
     * @param position  图片放置的位置, 枚举类型
     * @return 返回图片图层对象;
     */
    public BitmapLayer addBitmapLayer(Bitmap bmp , LSOLayerPosition position) {
        if(runnable!=null){
            return runnable.addBitmapLayer(bmp,position);
        }else{
            return null;
        }
    }

    /**
     *  在指定时间段增加图片图层;
     *  如果您的图片大小不和容器的大小相等,则图片居中显示;
     * @param bmp
     * @param startUs
     * @param endUs
     * @return
     */
    public BitmapLayer addBitmapLayer(Bitmap bmp,long startUs,long endUs) {
        if(runnable!=null){
            return runnable.addBitmapLayer(bmp,startUs,endUs);
        }else{
            return null;
        }
    }

    /**
     * 设置封面, 和addBitmap唯一的区别是:缩放到整个容器大小;
     * @param bmp
     * @param startUs
     * @param endUs  推荐一秒, 即1*1000*1000
     * @return
     */
    public BitmapLayer setCoverLayer(Bitmap bmp,long startUs,long endUs) {
        if(runnable!=null){
            return runnable.setCoverLayer(bmp,startUs,endUs);
        }else{
            return null;
        }
    }


    public BitmapLayer setLogoBitmapLayer(Bitmap bmp, LSOLayerPosition position) {
        if(runnable!=null){
            return runnable.setLogoBitmapLayer(bmp,position);
        }else{
            return null;
        }
    }
    public BitmapLayer setBackGroundBitmapLayer(Bitmap bmp) {
        if(runnable!=null){
            return runnable.setBackGroundBitmapLayer(bmp);
        }else{
            return null;
        }
    }

    //----------增加图片图层End;
    /**
     * 增加mv图层, mv默认是循环模式.
     * @param colorPath   mv图层的color视频
     * @param maskPath  mv图层的mask视频
     * @return
     */
    public MVLayer addMVLayer(String colorPath, String maskPath) {
        if(runnable!=null){
            return runnable.addMVLayer(colorPath,maskPath);
        }else{
            return null;
        }
    }

    public GifLayer addGifLayer(String gifPath, LSOLayerPosition position) {
        if(runnable!=null){
            return runnable.addGifLayer(gifPath,position);
        }else{
            return null;
        }
    }

    public GifLayer addGifLayer(int resId) {
        if(runnable!=null){
            return runnable.addGifLayer(resId);
        }else{
            return null;
        }
    }

    /**
     * 增加gif图层
     */
    public GifLayer addGifLayer(int resId, LSOLayerPosition position) {
        if(runnable!=null){
            GifLayer gifLayer= runnable.addGifLayer(resId);
            if(gifLayer!=null){
                gifLayer.setPosition(position);
            }
            return gifLayer;
        }else{
            return null;
        }
    }

    public CanvasLayer addCanvasLayer() {
        if(runnable!=null){
            return runnable.addCanvasLayer();
        }else{
            return null;
        }
    }

    /**
     * 增加子图层.
     * 默认不需要调用,我们会在视频图层绘制后, 直接绘制子图层;
     * 如果你要在视频层上面增加其他层, 然后再增加子图层, 则用这个.
     *
     * 在getVideoDataLayerAsync 回调中增加;
     * 一般用在溶图的场合;
     * 举例如下:
     *    videoOneDo2.getVideoDataLayerAsync(new OnLayerAlreadyListener() {
     *                 @Override
     *                 public void onLayerAlready(VideoDataLayer layer, int padWidth, int padHeight) {
     *
     *                     //先增加一个图片图层
     *                     Bitmap bmp1=BitmapFactory.decodeResource(getResources(),R.drawable.tt3);
     *                     videoOneDo2.addBitmapLayer(bmp1);
     *
     *                    //再次增加子图层;
     *                     SubLayer subLayer =layer.addSubLayer();
     *                     subLayer.setScale(0.3f);
     *                     videoOneDo2.addSubLayer(subLayer);
     *                 }
     *             });
     *
     * @param layer
     * @return
     */
    public boolean addSubLayer(SubLayer layer){
        return runnable!=null && runnable.addSubLayer(layer);
    }


    /**
     * 进度监听
     * 此进度不经过handler+message机制, 直接在处理完当前帧的时候, 调用此监听;
     * OnLanSongSDKThreadProgressListener 的两个方法分别是:
     * long ptsUs : 当前处理视频帧的时间戳. 单位微秒; 1秒=1000*1000微秒
     * int percent : 当前处理的进度百分比. 范围:0--100;
     */
    public void setOnLanSongSDKThreadProgressListener(OnLanSongSDKThreadProgressListener listener) {
        if(runnable!=null){
            runnable.setOnLanSongSDKThreadProgressListener(listener);
        }
    }

    /**
     * 进度监听
     * OnLanSongSDKProgressListener 的两个方法分别是:
     * long ptsUs : 当前处理视频帧的时间戳. 单位微秒; 1秒=1000*1000微秒
     * int percent : 当前处理的进度百分比. 范围:0--100;
     */
    public void setOnVideoOneDoProgressListener(OnLanSongSDKProgressListener listener) {
        if(runnable!=null){
            runnable.setOnVideoOneDoProgressListener(listener);
        }
    }
    /**
     * 完成监听
     */
    public void setOnVideoOneDoCompletedListener(OnLanSongSDKCompletedListener listener){
        if(runnable!=null){
            runnable.setOnVideoOneDoCompletedListener(listener);
        }
    }

    /**
     * 错误监听
     */
    public void setOnVideoOneDoErrorListener(OnLanSongSDKErrorListener listener){
        if(runnable!=null){
            runnable.setOnVideoOneDoErrorListener(listener);
        }
    }
    /**
     * 处理是否在运行
     */
    public boolean isRunning(){
        if(runnable!=null){
            return runnable.isRunning();
        }else {
            return false;
        }
    }

    /**
     * 另外开启一个线程,开始执行,
     * 执行的同时有进度回调, 完成后有完成回调;
     */
    public void start(){
        if(runnable!=null && !runnable.isRunning()){
            runnable.start();
        }else{
            LSOLog.e("VideoOneDo2 startPreview error. runnable is null or is running");
        }
    }
    /**
     * 取消
     */
    public void cancel(){
        if(runnable!=null){
            runnable.cancel();
            runnable=null;
        }
    }

    /**
     * 执行完毕后的回调;
     */
    public void release(){
        if(runnable!=null){
            runnable.release();
            runnable=null;
        }
    }
}
/**************************测试代码**************************************************************************
//demo1:
 VideoOneDo2 videoOneDo2 = null;
 private void testVideoOneDo() throws Exception {

 videoOneDo2 = new VideoOneDo2(getApplicationContext(), SDCARD.file("dy_xialu2.mp4"));

 //裁剪时长
 videoOneDo2.setCutDuration(2 * 1000 * 1000, 10 * 1000 * 1000);

 //裁剪画面
 videoOneDo2.setCropRect(100, 100, 400, 800);

 //缩放
 videoOneDo2.setScaleSize(500, 1200);

 //设置码率
 videoOneDo2.setCompressPercent(0.6f);

 //设置监听
 videoOneDo2.setOnVideoOneDoProgressListener(new OnLanSongSDKProgressListener() {
@Override
public void onLanSongSDKProgress(long ptsUs, int percent) {
    Log.e("TAG", "percent: " + percent);
}
});
        videoOneDo2.setOnVideoOneDoCompletedListener(new OnLanSongSDKCompletedListener() {
@Override
public void onLanSongSDKCompleted(String dstVideo) {
        MediaInfo.checkFileReturnString(dstVideo);
        }
        });
        videoOneDo2.startPreview();

        }


 //demo2: 转换为编码模式
 VideoOneDo2 videoOneDo2;
 private void testVideoOneDo(String file)
 {
 try {
 videoOneDo2= new VideoOneDo2(getApplicationContext(), file);

 videoOneDo2.setEditModeVideo();
 videoOneDo2.setExtractFrame(30);  //<---转换过程中,同时平均得到30帧Bitmap
 //设置监听
 videoOneDo2.setOnVideoOneDoProgressListener(new OnLanSongSDKProgressListener() {
@Override
public void onLanSongSDKProgress(long ptsUs, int percent) {
Log.e("TAG", "percent: "+percent);
}
});
 videoOneDo2.setOnVideoOneDoCompletedListener(new OnLanSongSDKCompletedListener() {
@Override
public void onLanSongSDKCompleted(String dstVideo) {

int cnt=0;
while (true){
Bitmap bmp=videoOneDo2.getExtractFrame();
if(bmp!=null){
cnt++;
Log.e("TAG", "getExtractFrame: "+ bmp.getWidth()+ bmp.getHeight()+ "  cnt is:"+ cnt);
}else {
break;
}
}

MediaInfo.checkFileReturnString(dstVideo);
}
});
 videoOneDo2.startPreview();

 } catch (Exception e) {
 e.printStackTrace();
 }
 }
 */
