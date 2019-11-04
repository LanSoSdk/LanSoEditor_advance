package com.lansosdk.videoeditor;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import com.lansosdk.LanSongAe.LSOAeDrawable;
import com.lansosdk.LanSongAe.LSOAeImage;
import com.lansosdk.LanSongAe.LSOAeImageLayer;
import com.lansosdk.LanSongAe.LSOAeText;
import com.lansosdk.LanSongFilter.LanSongFilter;
import com.lansosdk.box.AECompositionRunnable;
import com.lansosdk.box.AEJsonLayer;
import com.lansosdk.box.AEMVLayer;
import com.lansosdk.box.AESegmentLayer;
import com.lansosdk.box.AEVideoLayer;
import com.lansosdk.box.AudioLayer;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.LSOAudioAsset;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKExportProgressListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Ae的合成类;
 */
public class AECompositionExecute {


    private boolean secondLayerAdd=false;

    private AECompositionRunnable renderer;


    public AECompositionExecute(Context context){
        if(renderer==null){
            renderer =new AECompositionRunnable(context);
        }
        isStarted=false;
    }
    /**
     * 设置在导出时 码率
     * [不建议使用]
     * @param bitrate 码率,最低1*1024*1024;
     */
    public void setExportBitrate(int bitrate){
        if(renderer!=null){
            renderer.setExportBitrate(bitrate);
        }
    }
    /**
     * 增加第1层
     * 视频图层; 没有则设置为nil
     */
    public AEVideoLayer addFirstLayer(String videoPath) throws IOException {
        if(renderer !=null && !renderer.isRunning() && LanSongFileUtil.fileExist(videoPath)){
            return renderer.addVideoLayer(videoPath);
        }else{
            return null;
        }
    }

    /**
     * 增加第2层
     * Ae json图层;
     */
    public AEJsonLayer addSecondLayer(LSOAeDrawable drawable){
        if(secondLayerAdd){
            LSOLog.e("已经增加第二层(AE图层). 请确认你的增加顺序.");
            return null;
        }
        if(renderer !=null && !renderer.isRunning()) {
            AEJsonLayer layer= renderer.addAeLayer(drawable);
            if(layer==null){
                LSOLog.e("AECompositionView addSecondLayer error.");
            }
            secondLayerAdd= layer!=null;
            return layer;
        }else{
            return null;
        }
    }


    /**
     *
     * 增加第2层
     * Ae json图层;
     *  [重载方法.]
     * LSNEW
     * @param drawable
     * @param startIndex
     * @param endIndex
     * @return
     * @throws Exception
     */
    public AEJsonLayer addSecondLayer(LSOAeDrawable  drawable, int startIndex, int endIndex) {
        if(secondLayerAdd){
            LSOLog.e("已经增加第二层(AE图层). 请确认你的增加顺序.");
            return null;
        }
        drawable.setCutFrame(startIndex,endIndex);
        AEJsonLayer layer= renderer.addAeLayer(drawable);
        if(layer==null){
            LSOLog.e("AECompositionView addSecondLayer error.");
        }
        secondLayerAdd= layer!=null;
        return layer;
    }

    /**
     * 增加第2层
     * Ae json图层;
     * [重载方法.]
     * LSNEW
     */
    public AESegmentLayer addSecondLayer(List<LSOAeDrawable> drawables){
        if(secondLayerAdd){
            LSOLog.e("已经增加第二层(AE图层). 请确认你的增加顺序.");
            return null;
        }
        if(renderer !=null && !renderer.isRunning() && drawables!=null && drawables.size()>0) {
            AESegmentLayer layer= renderer.addAeLayer(drawables);
            if(layer==null){
                LSOLog.e("AECompositionView addSecondLayer error.");
            }
            secondLayerAdd= layer!=null;
            return layer;
        }else{
            return null;
        }
    }


    /**
     * 增加第3层
     * mv图层
     * [没有则不调用]
     */
    public AEMVLayer addThirdLayer(String colorPath, String maskPath){
        if(renderer !=null && !renderer.isRunning()&& colorPath!=null && maskPath!=null) {
            AEMVLayer layer= renderer.addMVLayer(colorPath,maskPath);
            if(layer==null){
                LSOLog.e("AECompositionView addThirdLayer MV Video error.");
            }
            return layer;
        }else{
            return null;
        }
    }

    /**
     * 增加第4层
     * Ae json图层;
     * [没有则不调用]
     * @return
     */
    public AEJsonLayer addForthLayer(LSOAeDrawable drawable){
        if(renderer !=null && !renderer.isRunning()) {
            AEJsonLayer layer= renderer.addAeLayer(drawable);
            if(layer==null){
                LSOLog.e("AECompositionView addForthLayer  Ae Json error.");
            }
            return layer;
        }else{
            return null;
        }
    }



    /**
     * 增加第5层
     * 增加mv图层
     * [没有则不调用]
     * @return
     */
    public AEMVLayer addFifthLayer(String colorPath, String maskPath){
        if(renderer !=null && !renderer.isRunning() && colorPath!=null && maskPath!=null) {
            AEMVLayer layer= renderer.addMVLayer(colorPath,maskPath);
            if(layer==null){
                LSOLog.e("AECompositionView addFifthLayer MV Video error.");
            }
            return layer;
        }else{
            return null;
        }
    }

    /**
     * 增加图片序列,
     * 一般用在动态的透明logo
     * @param bmpList 图片序列
     * @param intervalUs 序列中图片显示间隔,  单位是微秒. 一般建议是40*1000
     * @param loop  是否循环显示, 如果不循环,则停留在最后一帧; 如果您不想循环,也不要最后一帧,则可以设计最后一帧是完全透明的图片;
     * @return 返回图片图层对象
     */
    public BitmapLayer addBitmapLayer(ArrayList<Bitmap> bmpList, long intervalUs,boolean loop) {
        if(renderer !=null && bmpList!=null){
            return  renderer.addBitmapLayer(bmpList,intervalUs,loop);
        }else {
            LSOLog.e("AECompositionView 增加图片图层失败...");
            return  null;
        }
    }
    /**
     * 有些Ae模板是视频的, 则声音会单独导出为mp3格式, 从这里增加;
     * @param audioPath
     */
    public void addAeModuleAudio(String audioPath){
        if (renderer != null && !renderer.isRunning()) {
            renderer.addAeModuleAudio(audioPath);
        }
    }
    /**
     * 有些Ae模板是视频的, 则声音会单独导出为mp3格式, 从这里增加;
     * @param audioPath
     */
    public void addAeModuleAudio(LSOAudioAsset audioPath){
        if (renderer != null && !renderer.isRunning()) {
            renderer.addAeModuleAudio(audioPath);
        }
    }

    /**
     * 在增加其他音频后,获取Ae模板中的声音.
     * 没有增加其他声音,则无效;
     * @return
     */
    public AudioLayer getAEAudioLayer(){
        if(renderer!=null){
            return renderer.getAEAudioLayer();
        }else {
            return null;
        }
    }
    /**
     * 裁剪时长;
     * 裁剪模板的时长.
     * LSNEW :
     * 在视频增加后, 音频图层增加前调用;
     * @param durationUS
     */
    public void setCutDurationUS(long durationUS){
        if(renderer!=null){
            renderer.setCutDurationUS(durationUS);
        }
    }
    /**
     * 增加图片图层,
     * 在start前调用
     */
    public BitmapLayer addBitmapLayer(Bitmap bmp){
        if(renderer !=null && bmp!=null){
            return  renderer.addBitmapLayer(bmp);
        }else {
            LSOLog.e("AECompositionView 增加图片图层失败...");
            return  null;
        }
    }
    /**
     * 增加声音图层;
     * @param audioAsset 声音文件对象;
     * @return
     */
    public AudioLayer addAudioLayer(LSOAudioAsset audioAsset) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer= renderer.addAudioLayer(audioAsset);
            if(layer==null){
                LSOLog.e("AECompositionView addAudioLayer error. path:"+audioAsset);
            }
            return layer;
        } else {
            return null;
        }
    }

    /**
     * 增加音频图层.
     * @param audioAsset 音频资源
     * @param loop 是否循环;
     * @return
     */
    public AudioLayer addAudioLayer(LSOAudioAsset audioAsset,boolean loop) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer= renderer.addAudioLayer(audioAsset);
            if(layer==null){
                LSOLog.e("AECompositionView addAudioLayer error. path:"+audioAsset);
            }else{
                layer.setLooping(loop);
            }
            return layer;
        } else {
            return null;
        }
    }
    /**
     * 增加音频,
     *
     * 在AE线程开始前 + 所有图层增加后 调用;
     *
     * 音频采样率必须和视频的声音采样率一致
     * @param srcPath
     * @return
     */
    public AudioLayer addAudioLayer(String srcPath) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer= renderer.addAudioLayer(srcPath);
            if(layer==null){
                LSOLog.e("AECompositionView addAudioLayer error. path:"+srcPath);
            }
            return layer;
        } else {
            return null;
        }
    }

    /**
     * 增加音频图层,并是否循环
     * 在AE线程开始前 + 所有图层增加后 调用;
     * @return
     */
    public AudioLayer addAudioLayer(String srcPath, boolean loop) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer= renderer.addAudioLayer(srcPath);
            if(layer!=null){
                layer.setLooping(loop);
            }else{
                LSOLog.e("AECompositionView addAudioLayer error. path:"+srcPath);
            }
            return layer;
        } else {
            return null;
        }
    }

    /**
     * 增加音频图层,;
     *
     * 在AE线程开始前 + 所有图层增加后 调用;
     *
     * 音频采样率必须和视频的声音采样率一致
     * @param srcPath
     * @param startFromPadTime 从Ae模板的什么时间开始增加
     * @return  返回增加后音频层, 可以用来设置音量,快慢,变声等.
     */
    public AudioLayer addAudioLayer(String srcPath, long startFromPadTime) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer=  renderer.addAudioLayer(srcPath, 0,startFromPadTime, -1);
            if(layer==null){
                LSOLog.e("AECompositionView addAudioLayer error. path:"+srcPath);
            }
            return layer;
        } else {
            return null;
        }
    }

    /**
     * 增加其他声音;
     *
     *在AE线程开始前 和 所有图层增加后 调用;
     *
     *
     * 音频采样率必须和视频的声音采样率一致
     *
     * @param srcPath        路径, 可以是mp3或m4a或 带有音频的MP4文件;
     * @param startFromPadUs 从主音频的什么时间开始增加
     * @param durationUs     把这段声音多长插入进去.
     * @return 返回一个AudioLayer对象;
     */
    public AudioLayer addAudioLayer(String srcPath, long startFromPadUs,
                                    long durationUs) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer=  renderer.addAudioLayer(srcPath,0, startFromPadUs, durationUs);
            if(layer==null){
                LSOLog.e("AECompositionView addAudioLayer error. path:"+srcPath);
            }
            return layer;
        } else {
            return null;
        }
    }

    /**
     * 如果要调节音量, 则增加拿到对象后, 开始调节.
     *
     * 在AE线程开始前 + 所有图层增加后 调用;
     *
     * @param srcPath
     * @param startFromPadUs   从容器的什么位置开始增加
     * @param startAudioTimeUs 把当前声音的开始时间增加进去.
     * @param durationUs       增加多少, 时长.
     * @return
     */
    public AudioLayer addAudioLayer(String srcPath, long startFromPadUs,
                                    long startAudioTimeUs, long durationUs) {
        if (renderer != null && !renderer.isRunning()) {
            AudioLayer layer=renderer.addAudioLayer(srcPath, startFromPadUs,
                    startAudioTimeUs, durationUs);
            if(layer==null){
                LSOLog.e("AECompositionView addAudioLayer error. path:"+srcPath);
            }
            return layer;
        } else {
            return null;
        }
    }

    /**
     * 是否运行在导出阶段;
     * @return
     */
    public boolean isExportRunning(){
        return  renderer!=null && renderer.isExportMode();
    }
    public boolean isRunning(){
        return renderer!=null && renderer.isRunning();
    }
    /**
     * 导出进度;
     * @param listener
     */
    public void setOnLanSongSDKExportProgressListener(OnLanSongSDKExportProgressListener listener) {
        if(renderer!=null){
            renderer.setOnLanSongSDKExportProgressListener(listener);
        }
    }

    /**
     * 完成回调.
     * 两种情况:
     * 1. 预览没有循环,退出了
     * 2. 预览后, 导出完成.
     */
    public void setOnLanSongSDKCompletedListener(OnLanSongSDKCompletedListener listener){
        if(renderer!=null){
            renderer.setOnLanSongSDKCompletedListener(listener);
        }
    }
    /**
     * 错误监听
     */
    public void setOnLanSongSDKErrorListener(OnLanSongSDKErrorListener listener){
        if(renderer!=null){
            renderer.setOnLanSongSDKErrorListener(listener);
        }
    }

    public boolean isPlaying(){
        return renderer!=null && renderer.isRunning();
    }

    private int lastPercent=0; //上一个百分比;
    private boolean isStarted;
    /**
     * 开始导出Ae模板.
     * 导出后, 会以视频的形式返回给你
     * 你可以不预览,add好各种图层后, 直接调用此方法.
     */
    public boolean startExport(){
        if(renderer !=null) {
            return renderer.startExport();
        }else{
            return false;
        }
    }
    /**
     * 取消预览
     * [阻塞进行,一直等待到整个线程退出才返回.]
     */
    public void cancel(){
        if(renderer!=null){
            renderer.cancel();
            renderer=null;
        }
        isStarted=false;
    }
    /**
     * 释放
     * 如果已经收到完成监听, 则不需要调用;
     */
    public void release(){
        if(renderer!=null){
            renderer.release();
            renderer=null;
        }
        isStarted=false;
    }
}
