package com.lansosdk.videoeditor;

import android.content.Context;
import android.graphics.Bitmap;

import com.lansosdk.box.BoxMediaInfo;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.VideoOneDoRunnable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 是对VideoOneDo的优化,

 当前暂时只支持:
 1.剪切时长,
 2.裁剪画面,
 3.缩放,
 4.压缩/设置码率/设置编辑模式
 5.在处理过程中得到视频帧;

 .....后面增加 LSTODO
 */
public class VideoOneDo2 {

    private VideoOneDoRunnable runnable;
    public BoxMediaInfo mediaInfo;
    private int videoWxH;

    public static boolean checkSupport(MediaInfo info){
        if (info==null || !info.prepare() || !info.isHaveVideo()) {
           return false;
        }
        return "yuv420p".equals(info.vPixelFmt) || "yuvj420p".equals(info.vPixelFmt);
    }
    /**
     *
     *  裁剪是擦
     * @param ctx
     * @param path
     * @throws IOException
     */
    public VideoOneDo2(Context ctx, String path) throws Exception {
        runnable=new VideoOneDoRunnable(ctx,path);
        mediaInfo=runnable.getMediaInfo();
        videoWxH=mediaInfo.getWidth() * mediaInfo.getHeight();
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

        if(mediaInfo!=null && mediaInfo.getWidth()>=(startX + cropW) && mediaInfo.getHeight() >= (startY+ cropH))
        {
            if(cropW%16!=0 || cropH%16!=0){
                LSOLog.w("您要裁剪的宽高不是16的倍数,可能会出现黑边");
            }
            runnable.setCropRect(startX,startY,cropW,cropH);
            videoWxH=cropW * cropH;
        }else{
            LSOLog.e("VideoOneDo setCropRect error.");
        }
    }

    /**
     * 设置裁剪时长
     * @param startUs  开始时间
     * @param endUs 结束时间, 如果走到尾,则可以设置为-1;
     */
    public void setCutDuration(long startUs,long endUs)
    {
        if(runnable!=null){
            runnable.setCutDuration(startUs,endUs);
        }
    }
    /**
     * 设置在处理的同时,提取多少张图片.
     *
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
            if(videoWxH>=1080*1920){  //1080p的视频,则缩小一半;
                scale=0.5f;
            }else if(videoWxH>=720*1280){  //720p缩放到0.75
                scale=0.75f;
            }
            runnable.setExtractFrame(cnt,scale);
        }
    }

    /**
     * 获取要提取的每一张得到图片.
     *
     * 内部维护一个队列queue, 每次处理完一张图片,放到queue中. 您可以实时读取,也可以在执行到"完成监听"后一次性读取.
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
     * 缩放大小
     * 如果您同时设置了裁剪画面, 则流程是:把裁剪后的画面进行缩放;
     * @param w 宽度
     * @param h 高度
     */
    public void setScaleSize(int w,int h){
       if(runnable!=null){
           runnable.setScaleSize(w,h);
       }
    }
    /**
     * 设置编码码率
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
 videoOneDo2.setCutDuration(2*1000*1000,10*1000*1000);

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
