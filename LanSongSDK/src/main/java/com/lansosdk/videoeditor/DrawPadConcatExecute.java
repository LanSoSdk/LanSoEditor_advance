package com.lansosdk.videoeditor;

import android.content.Context;

import com.lansosdk.box.AudioLayer;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.CanvasLayer;
import com.lansosdk.box.DrawPadConcatExeRender;
import com.lansosdk.box.GifLayer;
import com.lansosdk.box.LSOAudioAsset;
import com.lansosdk.box.LSOBitmapAsset;
import com.lansosdk.box.LSOGifAsset;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LSOMVAsset;
import com.lansosdk.box.LSOVideoAsset;
import com.lansosdk.box.LSOVideoOption2;
import com.lansosdk.box.MVLayer;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.OnLanSongSDKThreadProgressListener;
import com.lansosdk.box.VideoConcatLayer;

/**
 * 多画面拼接容器;
 */
public class DrawPadConcatExecute {

    private boolean startSuccess;
    private int drawPadWidth;
    private int padHeight;

    private DrawPadConcatExeRender renderer;



    public DrawPadConcatExecute(Context ctx, int padWidth, int padHeight) {

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

        renderer =new DrawPadConcatExeRender(ctx,w,h);
        this.drawPadWidth =w;
        this.padHeight=h;
    }

    /**
     * 设置帧率
     * [不建议使用]
     * @param rate
     */
    public void setFrameRate(int rate) {
        if(renderer !=null){
            renderer.setFrameRate(rate);
        }
    }
    /**
     * 获取容器的宽度
     * @return
     */
    public int getDrawPadWidth(){
        return drawPadWidth;
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
        if(renderer !=null){
            renderer.setEncodeBitrate(bitrate);
        }
    }

    //-----------------------拼接类--------
    /**
     * 拼接一个图片图层,
     * 拼接动画
     * @param asset
     * @param durationUs
     * @return
     */
    public BitmapLayer concatBitmapLayer(LSOBitmapAsset asset, long durationUs) {
        if (renderer != null && asset!=null && setup()) {
            return renderer.concatBitmapLayer(asset,durationUs);
        }else{
            LSOLog.e("DrawPadConcatExecute concatBitmapLayer ERROR. LSOBitmapAsset is "+ asset);
            return null;
        }
    }
    /**
     * 拼接一个视频 先增加的第一个位置, 后增加的,依次向后排列;
     * @param asset 视频, 如果你要增加拼接动画,则拿到VideoConcatLayer对象后, 可以在指定点增加LanSongAnimation对象;
     * @param option 资源的一些选项,比如裁剪,循环等;
     * @return
     */
    public VideoConcatLayer concatVideoLayer(LSOVideoAsset asset, LSOVideoOption2 option) {
        if (renderer != null && asset!=null && setup()) {
            return renderer.concatVideoLayer(asset,option);
        }else{
            LSOLog.e("DrawPadConcatExecute concatVideoLayer ERROR. LSOVideoAsset is "+ asset);
            return null;
        }
    }
    //----------------------在拼接层的上面增加别的图层,比如增加logo等-------


    public BitmapLayer addBitmapLayer(LSOBitmapAsset asset) {
        if (renderer != null && asset!=null && setup()) {
            return renderer.addBitmapLayer(asset,0, Long.MAX_VALUE);
        }else{
            return null;
        }
    }


    public BitmapLayer addBitmapLayer(LSOBitmapAsset asset, long startTimeUs, long endTimeUs) {
        if (renderer != null && asset!=null && setup()) {
            return renderer.addBitmapLayer(asset,startTimeUs,endTimeUs);
        }else{
            return null;
        }
    }
    public CanvasLayer addCanvasLayer() {
        if (renderer != null && setup()) {
            return renderer.addCanvasLayer();
        }else{
            return null;
        }
    }
    public GifLayer addGifLayer(String gifPath, long startTimeUs, long endTimeUs) {
        if (renderer != null && setup()) {
            return renderer.addGifLayer(gifPath,startTimeUs,endTimeUs);
        }else{
            return null;
        }
    }

    public GifLayer addGifLayer(LSOGifAsset asset, long startTimeUs, long endTimeUs) {
        if (renderer != null && setup()) {
            return renderer.addGifLayer(asset,startTimeUs,endTimeUs);
        }else{
            return null;
        }
    }



    public MVLayer addMVLayer(LSOMVAsset asset){
        if (renderer != null && setup()) {
            return renderer.addMVLayer(asset,0,Long.MAX_VALUE,false);
        }else{
            return null;
        }
    }


    public MVLayer addMVLayer(LSOMVAsset asset, long startTimeUs, long endTimeUs, boolean isMute){
        if (renderer != null && setup()) {
            return renderer.addMVLayer(asset,startTimeUs,endTimeUs,isMute);
        }else{
            return null;
        }
    }
    //------------------增加音频图层---------------------------------
    /**
     * 增加声音图层;
     * @param audioAsset 声音文件对象;
     * @return
     */
    public AudioLayer addAudioLayer(LSOAudioAsset audioAsset) {
        if (renderer != null && audioAsset!=null) {
            AudioLayer layer= renderer.addAudioLayer(audioAsset,0,0,Long.MAX_VALUE);
            if(layer==null){
                LSOLog.e("DrawPadConcatExecute addAudioLayer 1 error. path:"+audioAsset);
            }
            return layer;
        } else {
            LSOLog.e("DrawPadConcatExecute addAudioLayer error. path:"+audioAsset);
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
        if (renderer != null&& audioAsset!=null) {
            AudioLayer layer= renderer.addAudioLayer(audioAsset,0,0,Long.MAX_VALUE);
            if(layer==null){
                LSOLog.e("DrawPadConcatExecute addAudioLayer 2 error. path:"+audioAsset);
            }else{
                layer.setLooping(loop);
            }
            return layer;
        } else {
            LSOLog.e("DrawPadConcatExecute addAudioLayer error. path:"+audioAsset);
            return null;
        }
    }
    /**
     * 增加音频图层,;
     * 所有图层增加后 调用;
     * @param audioAsset
     * @param volume  音频音量, 1.0是正常音量,0.5是降低一倍; 2.0是提高一倍;
     * @return
     */
    public AudioLayer addAudioLayer(LSOAudioAsset audioAsset, float volume) {
        if (renderer != null&& audioAsset!=null) {
            AudioLayer layer=  renderer.addAudioLayer(audioAsset, 0,0, Long.MAX_VALUE);
            if(layer==null){
                layer.setVolume(volume);
                LSOLog.e("DrawPadConcatExecute addAudioLayer 3 error. path:"+audioAsset);
            }
            return layer;
        } else {
            LSOLog.e("DrawPadConcatExecute addAudioLayer error. path:"+audioAsset);
            return null;
        }
    }

    /**
     *  增加其他声音;
     *  所有图层增加后 调用;
     *
     * @param audioAsset
     * @param startFromPadUs 从容器的什么位置开始增加
     * @return
     */
    public AudioLayer addAudioLayer(LSOAudioAsset audioAsset, long startFromPadUs) {
        if (renderer != null&& audioAsset!=null) {
            AudioLayer layer=  renderer.addAudioLayer(audioAsset,startFromPadUs, 0, Long.MAX_VALUE);
            if(layer==null){
                LSOLog.e("DrawPadConcatExecute addAudioLayer 4 error. path:"+audioAsset);
            }
            return layer;
        } else {
            LSOLog.e("DrawPadConcatExecute addAudioLayer error. path:"+audioAsset);
            return null;
        }
    }

    /**
     * 如果要调节音量, 则增加拿到对象后, 开始调节.
     *
     * 所有图层增加后 调用;
     * @param audioAsset
     * @param startFromPadUs   从容器的什么位置开始增加
     * @param startAudioTimeUs 裁剪声音的开始时间
     * @param endAudioTimeUs   裁剪声音的结束时间;
     * @return
     */
    public AudioLayer addAudioLayer(LSOAudioAsset audioAsset, long startFromPadUs,long startAudioTimeUs, long endAudioTimeUs) {
        if (renderer != null && audioAsset!=null) {
            AudioLayer layer=renderer.addAudioLayer(audioAsset, startFromPadUs,
                    startAudioTimeUs, endAudioTimeUs);
            if(layer==null){
                LSOLog.e("DrawPadConcatExecute addAudioLayer  5 error. path:"+audioAsset);
            }
            return layer;
        } else {
            LSOLog.e("DrawPadConcatExecute addAudioLayer error. path:"+audioAsset);
            return null;
        }
    }

//-------------------------listener 监听设置 -------------------------------------------
    /**
     * 进度监听  ---经过handle机制, 可以在主线程中调用UI界面;
     * OnLanSongSDKProgressListener 的两个方法分别是:
     * long ptsUs : 当前处理视频帧的时间戳. 单位微秒; 1秒=1000*1000微秒
     * int percent : 当前处理的进度百分比. 范围:0--100;
     */
    public void setOnLanSongSDKProgressListener(OnLanSongSDKProgressListener listener) {
        if(renderer !=null){
            renderer.setOnLanSongSDKProgressListener(listener);
        }
    }
    /**
     * 设置进度监听 ----不经过handle机制,不可以在主线程中调用UI界面;
     * 进度返回的两个参数 long类型是:当前时间戳,单位微秒; int类型是:当前处理的百分比;
     * @param listener
     */
    public void setOnLanSongSDKThreadProgressListener(OnLanSongSDKThreadProgressListener listener) {
        if(renderer !=null){
            renderer.setOnLanSongSDKThreadProgressListener(listener);
        }
    }

    /**
     * 完成回调
     */
    public void setOnLanSongSDKCompletedListener(OnLanSongSDKCompletedListener listener) {
        if(renderer !=null){
            renderer.setOnLanSongSDKCompletedListener(listener);
        }
    }

    /**
     * 错误回调
     */
    public void setOnLanSongSDKErrorListener(OnLanSongSDKErrorListener listener) {
        if(renderer !=null){
            renderer.setOnLanSongSDKErrorListener(listener);
        }
    }
    //-------------------------listener end -------------------------------------------
    public boolean isRunning() {
        if(renderer !=null){
            return renderer.isRunning();
        }else {
            return false;
        }
    }
    /**
     * 开始执行
     * @return
     */
    public boolean startExport() {
        if(renderer !=null){
            renderer.startExport();
            return startSuccess;
        }else{
            return false;
        }
    }
    /**
     * 取消执行
     */
    public void cancel() {
        if(renderer !=null){
            renderer.cancel();
            renderer.release();
            renderer =null;
            startSuccess=false;
        }

    }
    /**
     * 释放;
     */
    public void release() {
        if(renderer !=null){
            renderer.release();
            renderer =null;
            startSuccess=false;
        }
    }
    /**
     * 不检查容器尺寸.
     * 我们默认内部会16字节对齐; 如果调用此方法,则以您设置的宽高为准;
     * [不建议使用]
     */
    public void setNotCheckDrawPadSize() {
        if(renderer !=null){
            renderer.setNotCheckDrawPadSize();
        }
    }

    /**
     * 设置是否检查码率
     */
    public void setNotCheckBitRate() {
        if(renderer !=null){
            renderer.setNotCheckBitRate();
        }
    }
    //---------------------------------------------------------------------------
    private synchronized boolean setup(){
        if(renderer !=null && !renderer.isRunning() && !startSuccess){
            renderer.setup();
            startSuccess=true;
        }
        return startSuccess;
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
        if(renderer !=null){
            renderer.setCompositionBackGroundColor(r,g,b,a);
        }
    }
    //---------------------------test Demo测试例子------------------------------------------------
    /**

     */
}
