package com.lansosdk.videoeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;

import com.lansosdk.LanSongFilter.LanSongFilter;
import com.lansosdk.box.AudioLayer;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.BoxMediaInfo;
import com.lansosdk.box.CanvasLayer;
import com.lansosdk.box.GifLayer;
import com.lansosdk.box.LSOLayerPosition;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LanSoEditorBox;
import com.lansosdk.box.MVLayer;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.OnLayerAlreadyListener;
import com.lansosdk.box.VideoOneDoRunnable;

import java.io.IOException;
import java.util.List;

/**
 * 当前可以完成常见18个功能:
 * 增加音乐, 裁剪时长, 裁剪画面, 缩放, 压缩,增加logo,文字, 设置编辑模式,
 * 设置遮罩, 增加滤镜, 增加美颜,增加mv, 增加Gif,提取图片.增加Canvas等.
 *
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
     * @param cropW  裁剪多少宽度
     * @param cropH  裁剪多少高度
     */
    public void  setCropRect(int startX, int startY, int cropW, int cropH) {

        if(mediaInfo!=null && startX>=0 && startY>=0 && startX<mediaInfo.getWidth() && startY<mediaInfo.getHeight())
        {

            if(cropW%16!=0 || cropH%16!=0){
                LSOLog.w("您要裁剪的宽高不是16的倍数,我们内部可能会调整 crop size:"+ cropW+ " x "+ cropH);
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
     * 如果您同时设置了裁剪画面, 则流程是:把裁剪后的画面进行缩放;
     * @param w 缩放到的宽度
     * @param h 缩放到的高度
     */
    public void setScaleSize(int w,int h){
        if(runnable!=null){
            padWidth=w;
            padHeight=h;
            if(w%16!=0 || h%16!=0){
                LSOLog.w("您缩放的不是16的倍数,我们内部可能会调整 set scale size:"+ w+ " x "+ h);
            }
            runnable.setScaleSize(w,h);
        }
    }

    /**
     * 获取处理中的容器宽度
     * (也是最后生成视频的宽度)
     * @return
     */
    public int getPadWidth(){
        LSOLog.w("如果您在调用后, 再次设置了裁剪或缩放,则此值会变化,请注意!");
        return padWidth;
    }

    /**
     * 获取处理中的容器高度
     * (也是最后生成视频的高度)
     * @return
     */
    public int getPadHeight(){
        LSOLog.w("如果您在调用后, 再次设置了裁剪或缩放,则此值会变化,请注意!");
        return padHeight;
    }
    /**
     * 设置在处理的同时,提取多少张图片.
     *
     * @param cnt 提取图片的总个数, 内部会平均提取
     * @param scale 在提取出来的时候, 图片缩放系数, 因为大部分提取为了缩略图,建议缩小一些,以减少内存使用
     */
    public void setExtractFrame(int cnt,float scale)
    {
        if(runnable!=null){
            if(scale>1.0f || scale<=0) scale=1.0f;
            runnable.setExtractFrame(cnt,scale);
        }
    }

    /**
     * 设置在处理的同时,提取多少张图片.
     * @param cnt 提取图片的总个数, 内部会平均提取
     */
    public void setExtractFrame(int cnt)
    {
        if(runnable!=null){
            float scale=1.0f;
            if(padHeight *padWidth>=1080*1920){  //1080p的视频,则缩小一半;
                scale=0.5f;
            }else if(padHeight *padWidth>=720*1280){  //720p缩放到0.75
                scale=0.75f;
            }
            runnable.setExtractFrame(cnt,scale);
        }
    }
    /**
     * 获取要提取的每一张得到图片.
     *
     * 内部维护一个队列queue, 每次处理完一张图片,放到queue中.
     *
     * 您可以实时读取,也可以在执行到"完成监听"后一次性读取. 如果您最后一次性读取,建议不要缓冲太多图片,以免造成OOM
     *
     * 读取到最后一张图片,则返回null;
     * 先得到的是开始的图片, 后得到的是后来的图片.
     * @return
     */
    public Bitmap getExtractFrame(){
        if(runnable!=null){
            return runnable.getExtractFrame();
        }else{
            return null;
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
     * 如果小于我们规定的最小值, 则等于最小值;
     * @param percent 0---1.0f
     */
    public void setCompressPercent(float percent){
        if(runnable!=null){
            runnable.setCompressPercent(percent);
        }
    }

    /**
     * 是否设置为编辑模式;
     */
    public void setEditModeVideo(){
        if(runnable!=null){
            runnable.setEditModeVideo();
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
     * @param startPadUs
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
     *
     * @param srcPath 音频的完整路径(或还有音频的视频路径)
     * @param offsetPadUs
     * @param startAudioUs
     * @param endAudioUs
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
    //---------------gpu render start

    //----------增加滤镜和别的图层信息;
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
    /**
     * 增加滤镜
     */
    public void  addFilter(LanSongFilter filter){
        if(runnable!=null){
            runnable.addFilter(filter);
        }
    }

    /**
     * 设置滤镜列表;
     */
    public void addFilterList(List<LanSongFilter> filterList){
        if(runnable!=null){
            runnable.addFilterList(filterList);
        }
    }
    /**
     * 增加图片图层;
     */
    public BitmapLayer addBitmapLayer(Bitmap bmp ,LSOLayerPosition position) {
        if(runnable!=null){
            return runnable.addBitmapLayer(bmp,position);
        }else{
            return null;
        }
    }

    public BitmapLayer addBitmapLayer(Bitmap bmp) {
        if(runnable!=null){
            return runnable.addBitmapLayer(bmp);
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
     * @param endUs  推荐一秒;
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
    //------------------------
    public MVLayer addMVLayer(String srcPath, String maskPath, boolean isAsync) {
        if(runnable!=null){
            return runnable.addMVLayer(srcPath,maskPath,isAsync);
        }else{
            return null;
        }
    }

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

    public GifLayer addGifLayer(String gifPath,LSOLayerPosition position) {
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
    public GifLayer addGifLayer(int resId,LSOLayerPosition position) {
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
     * 进度监听
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
     * 开始执行,
     * 执行的同时有进度回调, 完成后有完成回调;
     */
    public void start(){
        if(runnable!=null && !runnable.isRunning()){
            runnable.start();
        }else{
            LSOLog.e("VideoOneDo2 start error. runnable is null or is running");
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
 private void testVideoOneDo()
 {
 try {
 VideoOneDo2 videoOneDo2= new VideoOneDo2(getApplicationContext(), SDCARD.file("d1.mp4"));

 //裁剪时长
 videoOneDo2.setCutDurationUs(2*1000*1000,10*1000*1000);

 //裁剪画面
 videoOneDo2.setCropRect(100,100,400,800);

 //缩放
 videoOneDo2.setScaleSize(500,1200);

 //设置码率
 videoOneDo2.setCompressPercent(0.6f);

 //设置监听
 videoOneDo2.setOnVideoOneDoProgressListener(new OnLanSongSDKProgressListener() {
@Override
public void onLanSongSDKProgress(long ptsUs, float percent) {
Log.e("TAG", "percent: "+percent);
}
});
 videoOneDo2.setOnVideoOneDoCompletedListener(new OnLanSongSDKCompletedListener() {
@Override
public void onLanSongSDKCompleted(String dstVideo) {
MediaInfo.checkFile(dstVideo);
}
});
 videoOneDo2.start();

 } catch (IOException e) {
 e.printStackTrace();
 }
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

MediaInfo.checkFile(dstVideo);
}
});
 videoOneDo2.start();

 } catch (Exception e) {
 e.printStackTrace();
 }
 }
 */
