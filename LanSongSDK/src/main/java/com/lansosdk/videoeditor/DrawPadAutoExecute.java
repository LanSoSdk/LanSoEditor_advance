package com.lansosdk.videoeditor;

import android.content.Context;
import android.graphics.Bitmap;

import com.lansosdk.box.AudioLayer;
import com.lansosdk.box.AudioPadRunnable;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.CanvasLayer;
import com.lansosdk.box.DataLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.DrawPadAutoRunnable;
import com.lansosdk.box.GifLayer;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.Layer;
import com.lansosdk.box.MVLayer;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.OnLanSongSDKThreadProgressListener;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadErrorListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.box.onDrawPadThreadProgressListener;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 自动刷新容器
 * 后台执行.
 *
 * 使用:没有视频的场合, 把一些图片/canvas, gif等素材合成视频;
 */
public class DrawPadAutoExecute {

    private DrawPadAutoRunnable renderer;

    //4个回调;
    private OnLanSongSDKProgressListener onLanSongSDKProgressListener;
    private OnLanSongSDKCompletedListener onLanSongSDKCompletedListener;
    private OnLanSongSDKErrorListener onLanSongSDKErrorListener;
    private OnLanSongSDKThreadProgressListener onLanSongSDKThreadProgressListener;


    private AtomicBoolean onPauseMode;
    private long durationUS;
    private String dstPath;
    private AudioPadExecute audioPad;
    /**
     * @param ctx 语境
     * @param padWidth  容器的宽度, 也是合成视频的宽度
     * @param padHeight 容器的高度, 也是合成视频的高度
     * @param durationUs 合成视频的时长,单位微秒;
     * @param framerate 合成视频的帧率
     */
    public DrawPadAutoExecute(Context ctx, int padWidth, int padHeight, long durationUs, int framerate) {
        if (renderer == null) {
            if(durationUs<1*1000*1000){
                LSOLog.w("DrawPadAutoExecute init duration  <1 second.");
            }
            if(padWidth *padHeight>1088*1920){
                LSOLog.e("DrawPadAutoExecute size is too big. maybe error. size:"+padWidth +" x "+ padHeight);
            }

            this.durationUS =durationUs;
            dstPath=LanSongFileUtil.createMp4FileInBox();
            audioPad=new AudioPadExecute(ctx,(float)(durationUs/1000000));
            renderer = new DrawPadAutoRunnable(ctx, padWidth, padHeight,durationUs, framerate, dstPath);
            renderer.setDrawPadCompletedListener(new onDrawPadCompletedListener() {
                @Override
                public void onCompleted(DrawPad v) {
                    if(onLanSongSDKCompletedListener!=null){

                        String dstPath2=dstPath;
                        if(audioPad!=null && audioPad.getAudioCount()>0){
                            String audioPath=audioPad.waitComplete();

                            dstPath2= AudioPadRunnable.mergeAudioVideo(dstPath,audioPath,true);
                            LanSongFileUtil.deleteFile(dstPath);

                        }
                        onLanSongSDKCompletedListener.onLanSongSDKCompleted(dstPath2);
                    }
                }
            });
            renderer.setDrawPadErrorListener(new onDrawPadErrorListener() {
                @Override
                public void onError(DrawPad d, int what) {
                    if(audioPad!=null && audioPad.getAudioCount()>0){
                        audioPad.stop();
                        audioPad=null;
                    }
                    if(onLanSongSDKErrorListener!=null){
                        onLanSongSDKErrorListener.onLanSongSDKError(what);
                    }
                }
            });
            renderer.setDrawPadProgressListener(new onDrawPadProgressListener() {
                @Override
                public void onProgress(DrawPad v, long currentTimeUs) {
                    if(onLanSongSDKProgressListener!=null){
                        int percent=(int)(currentTimeUs *100/durationUS);
                        onLanSongSDKProgressListener.onLanSongSDKProgress(currentTimeUs,percent);
                    }
                }
            });
            renderer.setDrawPadThreadProgressListener(new onDrawPadThreadProgressListener() {
                @Override
                public void onThreadProgress(DrawPad v, long currentTimeUs) {
                    if(onLanSongSDKThreadProgressListener!=null){
                        int percent=(int)(currentTimeUs *100/durationUS);
                        onLanSongSDKThreadProgressListener.onLanSongSDKProgress(currentTimeUs,percent);
                    }
                }
            });
        }
        this.durationUS =durationUs;
        onPauseMode=  new AtomicBoolean(false);
    }

    /**
     * 设置码率
     * [可选,不建议设置]
     * @param bitrate 码率;
     */
    public void setEncodeBitrate(int bitrate) {
        if(renderer!=null){
            renderer.setEncodeBitrate(bitrate);
        }
    }
    /**
     * 增加图片图层
     * @param bmp 图片图层
     * @return 返回图片图层对象
     */
    public BitmapLayer addBitmapLayer(Bitmap bmp) {
        if (renderer != null && setupDrawPad()) {
            LSOLog.d("DrawPadAutoExecute addBitmapLayer...");
            return renderer.addBitmapLayer(bmp, null);
        } else {
            return null;
        }
    }

    /**
     * 增加Canvas图层 , 根据CanvasLayer对象可以增加各种Canvas回调;
     * @return 返回图层对象
     */
    public CanvasLayer addCanvasLayer() {
        if (renderer != null && setupDrawPad()) {
            LSOLog.d("DrawPadAutoExecute addCanvasLayer...");
            return renderer.addCanvasLayer();
        } else {
            return null;
        }
    }

    /**
     * 增加MV图层,
     *
     * [如果mv视频帧率和容器帧率不同,可能会引起mv画面速度或快或慢]
     * [如果 MV中有声音,当前暂时不会增加到目标视频中]
     *
     * @param colorPath  mv视频中的 颜色视频
     * @param maskPath mv视频中的mask视频
     * @return
     */
    public MVLayer addMVLayer(String colorPath, String maskPath) {
        if (renderer != null && setupDrawPad()) {
            LSOLog.d("DrawPadAutoExecute addMVLayer...");
            return renderer.addMVLayer(colorPath, maskPath);
        } else {
            return null;
        }
    }

    /**
     * 增加Gif图层
     */
    public GifLayer addGifLayer(String gifPath) {
        if (renderer != null && setupDrawPad()) {
            LSOLog.d("DrawPadAutoExecute addGifLayer...");
            return renderer.addGifLayer(gifPath);
        } else {
            return null;
        }
    }

    /**
     * 增加gif图层
     * @param resId 来自drawable中的资源
     * @return
     */
    public GifLayer addGifLayer(int resId) {
        if (renderer != null && setupDrawPad()) {
            LSOLog.d("DrawPadAutoExecute addGifLayer...");
            return renderer.addGifLayer(resId);
        } else {
            return null;
        }
    }

    /**
     * 增加其他数据
     * 当前仅支持NV21格式
     * @param dataWidth 数据图像的宽度
     * @param dataHeight 数据图像的高度
     * @return
     */
    public DataLayer addDataLayer(int dataWidth, int dataHeight) {
        if (renderer != null && setupDrawPad()) {
            LSOLog.d("DrawPadAutoExecute addDataLayer...");
            return renderer.addDataLayer(dataWidth, dataHeight);
        } else {
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
        if(audioPad!=null){
            return audioPad.addAudioLayer(srcPath,isLoop);
        }else{
            return null;
        }
    }
    /**
     * 增加其他音频;
     * 支持mp4,wav,mp3,m4a文件;
     *
     * @param valume  音频的音量; 范围是0--10; 1.0正常;大于1.0提高音量;小于1.0降低音量;
     * @return 返回增加后音频层, 可以用来设置音量,快慢,变声等.
     */
    public AudioLayer addAudioLayer(String srcPath, boolean isLoop, float valume) {
        if(audioPad!=null){
            return audioPad.addAudioLayer(srcPath,isLoop,valume);
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
        if(audioPad!=null){
            return audioPad.addAudioLayer(srcPath,startPadUs);
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
        if(audioPad!=null){
            return audioPad.addAudioLayer(srcPath,offsetPadUs,startAudioUs,endAudioUs);
        }else{
            return null;
        }
    }
    /**
     * 删除指定图层
     */
    public void removeLayer(Layer lay) {
        if (renderer != null && lay != null) {
            renderer.removeLayer(lay);
        }
    }

    /**
     * 删除所有图层
     */
    public void removeAllLayer() {
        if (renderer != null) {
            renderer.removeAllLayer();
        }
    }

    /**
     * 把图层移动到最底层
     */
    public void bringToBack(Layer lay) {
        if (renderer != null) {
            renderer.bringToBack(lay);
        }
    }

    /**
     * 把图层移动到最外层
     */
    public void bringToFront(Layer lay) {
        if (renderer != null) {
            renderer.bringToFront(lay);
        }
    }


    /**
     * 设置进度监听,  ---经过handle机制,
     * 进度返回的两个参数 long类型是:当前时间戳,单位微秒; int类型是:当前处理的百分比;
     * @param listener
     */
    public void setOnLanSongSDKProgressListener(OnLanSongSDKProgressListener listener) {
        onLanSongSDKProgressListener=listener;
    }

    /**
     * 设置进度监听 ----不经过handle机制
     * 进度返回的两个参数 long类型是:当前时间戳,单位微秒; int类型是:当前处理的百分比;
     * @param listener
     */
    public void setOnLanSongSDKThreadProgressListener(OnLanSongSDKThreadProgressListener listener) {
        onLanSongSDKThreadProgressListener=listener;
    }

    /**
     * 完成回调
     * @param listener
     */
    public void setOnLanSongSDKCompletedListener(OnLanSongSDKCompletedListener listener) {
        onLanSongSDKCompletedListener=listener;
    }

    /**
     * 错误回调
     * @param listener
     */
    public void setOnLanSongSDKErrorListener(OnLanSongSDKErrorListener listener) {
        onLanSongSDKErrorListener=listener;
    }

    /**
     * 当前释放在执行;
     * @return
     */
    public boolean isRunning(){
        return renderer!=null && renderer.isRunning();
    }

    /**
     * 开始执行
     * @return
     */
    public boolean start() {
        boolean ret=false;
        if (renderer != null) {
            if (onPauseMode.get()) {
                renderer.resumeThread();
                ret = true;
            } else {
                ret = renderer.startDrawPad();
            }
        }
        if(ret && audioPad!=null && audioPad.getAudioCount()>0){
            audioPad.start();
        }
        return ret;
    }

    /**
     * 暂停绘制
     * [不建议使用]
     */
    public void pause() {
        if (renderer != null) {
            renderer.pauseThread();
        }
    }

    /**
     * 暂停后的恢复绘制
     * [不建议使用]
     */
    public void resume() {
        if (renderer != null && renderer.isRunning()) {
            renderer.resumeThread();
        }
    }

    /**
     * 取消绘制
     * 取消后, 当前render的线程释放. 如果要再次执行需要重新new
     */
    public void cancel(){
        if (renderer != null) {
            renderer.cancel();
            renderer=null;
        }
    }

    /**
     * 释放
     */
    public void release() {
        if (renderer != null) {
            renderer.release();
            renderer = null;
        }
    }
    //-----------内部方法;
    boolean startSuccess;
    private boolean setupDrawPad(){
        if(renderer!=null && !renderer.isRunning() && !onPauseMode.get()){
            renderer.pauseThread();
            startSuccess =renderer.startDrawPad();
            onPauseMode.set(true);
        }
        return startSuccess;
    }
}
/**------------------测试代码如下


 DrawPadAutoExecute autoExecute;
 private void testDrawPadExecute() {

 autoExecute = new DrawPadAutoExecute(getApplicationContext(), 540, 960,10*1000*1000, 30);

 //增加各种图层;
 autoExecute.addBitmapLayer(BitmapFactory.decodeResource(getResources(),R.drawable.a1));
 addCanvasLayer();
 String mvColor=copyAssets(getApplicationContext(),"zaoan_mvColor.mp4");
 String mvMask=copyAssets(getApplicationContext(),"zaoan_mvMask.mp4");
 autoExecute.addMVLayer(mvColor,mvMask);


 autoExecute.addAudioLayer(SDCARD.file("hongdou10s.mp3"),true);

 //----设置监听,并开始执行;
 autoExecute.setOnLanSongSDKProgressListener(new OnLanSongSDKProgressListener() {
@Override
public void onLanSongSDKProgress(long ptsUs, int percent) {
Log.e("TAG", "DrawPadAutoRunnable  ----curent: "+ptsUs+ " percent:"+percent);
}
});
 autoExecute.setOnLanSongSDKCompletedListener(new OnLanSongSDKCompletedListener() {
@Override
public void onLanSongSDKCompleted(String dstVideo) {
MediaInfo.checkFileReturnString(dstVideo);
DemoUtil.startPlayDstVideo(ListMainActivity.this,dstVideo);
}
});
 autoExecute.setOnLanSongSDKErrorListener(new OnLanSongSDKErrorListener() {
@Override
public void onLanSongSDKError(int errorCode) {
Log.e("TAG", ": ");
}
});
 autoExecute.start();
 }
 private ShowHeart showHeart;
 private CanvasLayer canvasLayer;
 private void addCanvasLayer() {

 canvasLayer = autoExecute.addCanvasLayer();
 if (canvasLayer != null) {

 canvasLayer.setClearCanvas(false);

 canvasLayer.addCanvasRunnable(new CanvasRunnable() {
@Override
public void onDrawCanvas(CanvasLayer layer, Canvas canvas,long currentTimeUs) {
Paint paint = new Paint();
paint.setColor(Color.RED);
paint.setAntiAlias(true);
paint.setTextSize(50);
canvas.drawText("蓝松短视频演示之<任意绘制>", 20,canvasLayer.getPadHeight() - 200, paint);
}
});

 showHeart = new ShowHeart(this, canvasLayer.getPadWidth(),canvasLayer.getPadHeight());
 canvasLayer.addCanvasRunnable(new CanvasRunnable() {

@Override
public void onDrawCanvas(CanvasLayer layer, Canvas canvas,long currentTimeUs) {
showHeart.drawTrack(canvas);
}
});
 }
 }

  */