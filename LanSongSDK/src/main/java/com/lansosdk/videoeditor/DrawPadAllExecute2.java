package com.lansosdk.videoeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;

import com.lansosdk.LanSongAe.LSOAeDrawable;
import com.lansosdk.box.AEJsonLayer;
import com.lansosdk.box.AudioLayer;
import com.lansosdk.box.BitmapListLayer;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.CanvasLayer;
import com.lansosdk.box.DataLayer;
import com.lansosdk.box.DrawPadAllRunnable2;
import com.lansosdk.box.DrawPadPixelRunnable;
import com.lansosdk.box.GifLayer;
import com.lansosdk.box.LSOAECompositionLayer;
import com.lansosdk.box.LSOAeCompositionAsset;
import com.lansosdk.box.LSOPhotoAlbumLayer;
import com.lansosdk.box.LSOBitmapAsset;
import com.lansosdk.box.LSOGifAsset;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LSOMVAsset;
import com.lansosdk.box.LSOPhotoAlbumAsset;
import com.lansosdk.box.LSOVideoOption;
import com.lansosdk.box.Layer;
import com.lansosdk.box.MVCacheLayer;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.OnLanSongSDKThreadProgressListener;
import com.lansosdk.box.SubLayer;
import com.lansosdk.box.VideoFrameLayer;

import java.util.List;
import java.util.Map;

public class DrawPadAllExecute2 {

    private boolean success;
    private DrawPadAllRunnable2 commonRunnable;
    private DrawPadPixelRunnable pixelRunnable;
    private String padDstPath;
    private int padWidth;
    private int padHeight;


    /**
     * 强制使用老版本.
     */
    public static  boolean forceUseOLD =false;

    /**
     *  构造方法
     *   如果您仅仅对一个视频做处理, 则可以设置为这个视频的宽高和时长;
     *
     * @param ctx
     * @param padWidth 容器的宽度, 即最后生成视频的宽度 强烈建议最大值是720P
     * @param padHeight 容器的高度,即最后生成视频的高度 强烈建议最大值是720P
     * @param durationUS 容器的长度,  最后生成视频的长度;单位微秒;
     * @throws Exception 创建时抛出异常;
     */
    public DrawPadAllExecute2(Context ctx, int padWidth, int padHeight, long  durationUS) throws Exception {
        if(!LanSoEditor.isLoadLanSongSDK.get()){
            throw  new Exception("没有加载SDK, 或你的APP崩溃后,重新启动当前Activity,请查看完整的logcat:(No SDK is loaded, or the current activity is restarted after your app crashes, please see the full logcat)");
        }

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

        if(padWidth * padHeight<=192*160){ //麒麟处理器的裁剪区域是176x144
            LSOLog.e("setCropRect error. qi lin SoC is192*160");
            if(padHeight>padWidth){
                w=160;
                h=1920;
            }else{
                w=192;
                h=160;
            }
        }

        //小于等于8.0 支持nv21, 麒麟处理器.
        if(!forceUseOLD && Build.VERSION.SDK_INT<=Build.VERSION_CODES.O && LanSoEditor.isQiLinSoc() && VideoEditor.isSupportNV21ColorFormat() ){
            pixelRunnable =new DrawPadPixelRunnable(ctx,w,h,durationUS);
            LSOLog.i("DrawPadAllExecute2 run  new pixel_mode Runnable...");
        }else{
            commonRunnable =new DrawPadAllRunnable2(ctx,w,h,durationUS);
            LSOLog.i("DrawPadAllExecute2 run  COMMON  Runnable(DrawPadAllRunnable2)...");
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
        if(commonRunnable !=null){
            commonRunnable.setFrameRate(rate);
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
        if(commonRunnable !=null){
            commonRunnable.setEncodeBitrate(bitrate);
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
     * 设置容器的背景颜色;
     * @param color
     */
    public void setBackgroundColor(int color) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        padBGRed=(float)red/255f;
        padBGGreen=(float)green/255f;
        padBGBlur=(float)blue/255f;
        if(commonRunnable !=null){
            commonRunnable.setCompositionBackGroundColor(padBGRed,padBGGreen,padBGBlur,1.0f);
        }else  if(pixelRunnable !=null){
            pixelRunnable.setCompositionBackGroundColor(padBGRed,padBGGreen,padBGBlur,1.0f);
        }
    }

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
        if(commonRunnable !=null){
            commonRunnable.setCompositionBackGroundColor(r,g,b,a);
        }else  if(pixelRunnable !=null){
            pixelRunnable.setCompositionBackGroundColor(r,g,b,a);
        }
    }

    /**
     * 增加一张图片到容器中. 增加后返回图片图层对象;
     * @param bmp 图片对象
     * @param startTimeUs 从容器的什么时间点开始增加
     * @param endTimeUs 增加到容器的
     * @return
     */
    public BitmapLayer addBitmapLayer(Bitmap bmp,long startTimeUs,long endTimeUs) {
        if (commonRunnable != null && setup()) {
            return commonRunnable.addBitmapLayer(bmp, startTimeUs, endTimeUs);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addBitmapLayer(bmp,startTimeUs,endTimeUs);
        }else{
            LSOLog.e("DrawPadAllExecute2  addBitmapLayer error. return null, success status is:"+ success);
            return null;
        }
    }
    /**
     * 增加一张图片到容器中. 增加后返回图片图层对象;
     * @param bmp 图片对象
     * @return 返回图片图层;
     */
    public BitmapLayer addBitmapLayer(Bitmap bmp) {
        if (commonRunnable != null && setup()) {
            return commonRunnable.addBitmapLayer(bmp,0,Long.MAX_VALUE);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addBitmapLayer(bmp,0,Long.MAX_VALUE);
        } else {
            LSOLog.e("DrawPadAllExecute2  addBitmapLayer error. return null, success status is:"+ success);
            return null;
        }
    }

    /**
     * 增加一张图片到容器中. 增加后返回图片图层对象;
     * @param asset  图片路径,
     * @param startTimeUs 从容器的什么时间开始增加
     * @param endTimeUs 在容器的什么时间消失, 如果到文件尾,请输入Long.MAX_VALUE
     * @return
     */
    public BitmapLayer addBitmapLayer(LSOBitmapAsset asset, long startTimeUs, long endTimeUs) {
        if (commonRunnable != null && setup()) {
            return commonRunnable.addBitmapLayer(asset,startTimeUs,endTimeUs);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addBitmapLayer(asset,startTimeUs,endTimeUs);
        }else {
            LSOLog.e("DrawPadAllExecute2  addBitmapLayer error. return null, success status is:"+ success);
            return null;
        }
    }

    /**
     * 增加一张图片到容器中. 增加后返回图片图层对象;
     * @param asset 图片路径
     * @return 返回图片图层对象
     */
    public BitmapLayer addBitmapLayer(LSOBitmapAsset asset) {
        if (commonRunnable != null && setup()) {
            return commonRunnable.addBitmapLayer(asset,0,Long.MAX_VALUE);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addBitmapLayer(asset,0,Long.MAX_VALUE);
        }else {
            LSOLog.e("DrawPadAllExecute2  addBitmapLayer error. return null, success status is:"+ success);
            return null;
        }
    }

    /**
     * 增加一张图片到容器中. 增加后返回图片图层对象;
     * @param path  图片路径
     * @param startTimeUs 在容器中的开始时间, 单位微秒
     * @param endTimeUs 在容器中的结束时间, 单位微秒;
     * @return 返回
     * @throws Exception
     */
    public BitmapLayer addBitmapLayer(String path, long startTimeUs, long endTimeUs)  throws  Exception{

        LSOBitmapAsset asset=new LSOBitmapAsset(path);
        if (commonRunnable != null && setup()) {
            return commonRunnable.addBitmapLayer(asset,startTimeUs,endTimeUs);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addBitmapLayer(asset,startTimeUs,endTimeUs);
        }else {
            LSOLog.e("DrawPadAllExecute2  addBitmapLayer error. return null, success status is:"+ success);
            return null;
        }
    }

    /**
     *  增加一张图片到容器中. 增加后返回图片图层对象;
     * @param path 图片路径
     * @return 返回图片图层对象
     * @throws Exception 抛出异常;
     */
    public BitmapLayer addBitmapLayer(String path) throws Exception{
        LSOBitmapAsset asset=new LSOBitmapAsset(path);
        if (commonRunnable != null && setup()) {
            return commonRunnable.addBitmapLayer(asset,0,Long.MAX_VALUE);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addBitmapLayer(asset,0,Long.MAX_VALUE);
        }else {
            LSOLog.e("DrawPadAllExecute2  addBitmapLayer error. return null, success status is:"+ success);
            return null;
        }
    }


    /**
     * 增加一组图片数据, 增加后,返回图片数组图层对象
     * @param list 图片数组
     * @param frameIntervalUs 两帧之间的间隔, 单位微秒; 建议是40*1000;  如果你是视频帧率,则是1000000L/frameRate;
     * @param loop 是否循环;, 默认不循环;
     * @return 返回图片数组;
     */
    public BitmapListLayer addBitmapListLayer(List<Bitmap> list, long frameIntervalUs, boolean loop){
        if (commonRunnable != null && setup()) {
            return commonRunnable.addBitmapListLayer(list,frameIntervalUs,loop,0,Long.MAX_VALUE);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addBitmapListLayer(list,frameIntervalUs,loop,0,Long.MAX_VALUE);
        }else {
            LSOLog.e("DrawPadAllExecute2  addBitmapListLayer error.");
            return null;
        }
    }


    /**
     * 增加一组图片数据
     * 建议是40*1000; 如果你是视频帧率,则是1000000L/frameRate;
     * @param list 图片数组
     * @param frameIntervalUs 两帧之间的间隔, 单位微秒;
     * @return 返回图片数组;
     */
    public BitmapListLayer addBitmapListLayer(List<String> list, long frameIntervalUs){
        if (commonRunnable != null && setup()) {
            return commonRunnable.addBitmapListLayer(list,frameIntervalUs,false);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addBitmapListLayer(list,frameIntervalUs,false);
        }else {
            LSOLog.e("DrawPadAllExecute2  addBitmapListLayer error.");
            return null;
        }
    }

    public BitmapListLayer addBitmapListLayer(List<String> bitmapPaths, List<String> maskPaths, long frameIntervalUs){
        if (commonRunnable != null && setup()) {
            return commonRunnable.addBitmapListLayer(bitmapPaths, maskPaths, frameIntervalUs,false);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addBitmapListLayer(bitmapPaths, maskPaths, frameIntervalUs,false);
        }else {
            LSOLog.e("DrawPadAllExecute2  addBitmapListLayer error.");
            return null;
        }
    }


    public BitmapListLayer addBitmapListLayer(Map<Long, String> bitmapTimes) {
        if (commonRunnable != null && setup()) {
            return commonRunnable.addBitmapListLayer(bitmapTimes);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addBitmapListLayer(bitmapTimes);
        }else {
            LSOLog.e("DrawPadAllExecute2  addBitmapListLayer error.");
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
        if (commonRunnable != null && setup()) {
            return commonRunnable.addMVLayer(srcPath,maskPath,0,Long.MAX_VALUE,false);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addMVLayer(srcPath,maskPath,0,Long.MAX_VALUE,false);
        }else {
            LSOLog.e("DrawPadAllExecute2  addMVLayer error. return null, success status is:"+ success);
            return null;
        }
    }

    /**
     * 增加视频图层
     * option 中可以设置旋转, 裁剪时长, 裁剪画面, 缩放等. 顺序是,先裁剪时长-->旋转(90/180/270)--->裁剪画面-->缩放;
     * 如果你要任意角度的旋转, 则用图层的旋转属性;
     *
     * @param option 在增加前,设置裁剪时长, 裁剪画面, 缩放, 循环等;
     * @return 返回视频图层
     */
    public VideoFrameLayer addVideoLayer(LSOVideoOption option) {
        if (commonRunnable != null && option!=null && setup()) {
            return commonRunnable.addVideoLayer(option,0,Long.MAX_VALUE,false,false);
        }else if (pixelRunnable != null && option!=null && setup()) {
            return pixelRunnable.addVideoLayer(option,0,Long.MAX_VALUE,false,false);
        }else {
            LSOLog.e("DrawPadAllExecute2  addVideoLayer error. return null, success status is:"+ success);
            return null;
        }
    }
    /**
     * 增加视频图层
     * option 中可以设置旋转, 裁剪时长, 裁剪画面, 缩放等. 顺序是,先裁剪时长-->旋转(90/180/270)--->裁剪画面-->缩放;
     * 如果你要任意角度的旋转, 则用图层的旋转属性;
     *
     * @param option 在增加前,设置裁剪时长, 裁剪画面, 缩放, 循环等;
     * @param startTimeUs 从容器的什么时间开始增加
     * @param endTimeUs 在容器的什么时间消失, 如果到文件尾,请输入Long.MAX_VALUE
     * @param holdFirst 当指定时间段后,是否刚开始就一直显示第一帧
     * @param holdLast  当指定时间段过后, 是否一直显示最后一帧;
     * @return
     */
    public VideoFrameLayer addVideoLayer(LSOVideoOption option,long startTimeUs,long endTimeUs,boolean holdFirst,boolean holdLast) {
        if (commonRunnable != null && option!=null && setup()) {
            return commonRunnable.addVideoLayer(option,startTimeUs,endTimeUs,holdFirst,holdLast);
        }else if (pixelRunnable != null && option!=null && setup()) {
            return pixelRunnable.addVideoLayer(option,startTimeUs,endTimeUs,holdFirst,holdLast);
        }else {
            LSOLog.e("DrawPadAllExecute2  addVideoLayer error. return null, success status is:"+ success);
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
        if (commonRunnable != null && setup()) {
            return commonRunnable.addMVLayer(srcPath,maskPath,startTimeUs,endTimeUs,false);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addMVLayer(srcPath,maskPath,startTimeUs,endTimeUs,false);
        }else {
            LSOLog.e("DrawPadAllExecute2  addMVLayer error. return null, success status is:"+ success);
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
    @Deprecated
    public MVCacheLayer addMVLayer(String srcPath, String maskPath, long startTimeUs, long endTimeUs, boolean mute) {
        if (commonRunnable != null && setup()) {
            return commonRunnable.addMVLayer(srcPath,maskPath,startTimeUs,endTimeUs,mute);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addMVLayer(srcPath,maskPath,startTimeUs,endTimeUs,mute);
        }else {
            LSOLog.e("DrawPadAllExecute2  addMVLayer error. return null, success status is:"+ success);
            return null;
        }
    }

    public MVCacheLayer addMVLayer(LSOMVAsset asset) {
        if (commonRunnable != null && setup()) {
            return commonRunnable.addMVLayer(asset,0,Long.MAX_VALUE,false);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addMVLayer(asset,0,Long.MAX_VALUE,false);
        }else {
            LSOLog.e("DrawPadAllExecute2  addMVLayer error. return null, success status is:"+ success);
            return null;
        }
    }
    /**
     * 增加mv图层
     * @param asset 透明视频的资源;
     * @param startTimeUs 从容器的什么时间开始增加
     * @param endTimeUs 在容器的什么时间消失, 如果到文件尾,请输入Long.MAX_VALUE
     * @param mute  如果mv中有声音, 是否要静音;默认不静音;
     * @return
     */
    public MVCacheLayer addMVLayer(LSOMVAsset asset, long startTimeUs, long endTimeUs, boolean mute) {
        if (commonRunnable != null && setup()) {
            return commonRunnable.addMVLayer(asset,startTimeUs,endTimeUs,mute);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addMVLayer(asset,startTimeUs,endTimeUs,mute);
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
        if (commonRunnable != null && setup()) {
            return commonRunnable.addDataLayer(dataWidth,dataHeight,startTimeUs,endTimeUs);
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
    @Deprecated
    public GifLayer addGifLayer(String gifPath,long startTimeUs,long endTimeUs) {
        if (commonRunnable != null && setup()) {
            return commonRunnable.addGifLayer(gifPath,startTimeUs,endTimeUs);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addGifLayer(gifPath,startTimeUs,endTimeUs);
        }else {
            return null;
        }
    }

    /**
     *增加gif图层
     * @param gifAsset gif文件的资源;
     * @param startTimeUs 从容器的什么时间开始增加
     * @param endTimeUs 在容器的什么时间消失, 如果到文件尾,请输入Long.MAX_VALUE
     * @return
     */
    public GifLayer addGifLayer(LSOGifAsset gifAsset, long startTimeUs, long endTimeUs) {
        if (commonRunnable != null && setup()) {
            return commonRunnable.addGifLayer(gifAsset,startTimeUs,endTimeUs);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addGifLayer(gifAsset,startTimeUs,endTimeUs);
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
        if (commonRunnable != null && setup()) {
            return commonRunnable.addGifLayer(gifPath,0,Long.MAX_VALUE);
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
        if (commonRunnable != null && setup()) {
            return commonRunnable.addGifLayer(resId,0,Long.MAX_VALUE);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addGifLayer(resId,0,Long.MAX_VALUE);
        }else {
            return null;
        }
    }

    /**
     * 已经废弃, 请不要使用;
     * @return
     */
    @Deprecated
    public CanvasLayer addCanvasLayer() {
        LSOLog.e("addCanvasLayer 已经废弃, 请不要使用. 请用addBitmapListLayer");
        if (commonRunnable != null && setup()) {
            return commonRunnable.addCanvasLayer();
        }else  if (pixelRunnable != null && setup()) {
            return pixelRunnable.addCanvasLayer();
        }else {
            return null;
        }
    }

    /**
     * 增加子图层. 子图层是通过父类图层createSubLayer得到的.
     * 默认不需要调用,我们会在视频图层绘制后, 直接绘制子图层;
     * 如果你要在视频层上面增加其他层, 然后再增加子图层, 则用这个.
     *
     * 一般用在溶图的场合;
     * 举例如下
     *  try {
     *             LSOVideoOption videoOption=new LSOVideoOption(SDCARD.d1());
     *
     *
     *             VideoFrameLayer videoLayer=allExecute.addVideoLayer(videoOption);
     *
     *             Bitmap bmp= BitmapFactory.decodeResource(getResources(),R.drawable.tt3);
     *             allExecute.addBitmapLayer(bmp);
     *
     *
     *             //创建一个子图层
     *             SubLayer layer=videoLayer.createSubLayer();
     *             layer.setScale(0.5f);
     *             LanSongBlurFilter maskFilter=new LanSongBlurFilter();
     *             layer.switchFilterTo(maskFilter);
     *             //增加到容器里, 因为先增加了图片,这个在图片的上层;
     *             allExecute.addSubLayer(layer);
     *
     *
     *         } catch (Exception e) {
     *             e.printStackTrace();
     *         }
     * @param layer 子图层 通过父类图层createSubLayer得到的
     * @return
     */
    public boolean addSubLayer(SubLayer layer){
        if (commonRunnable != null && setup()) {
            return commonRunnable.addSubLayer(layer);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addSubLayer(layer);
        }else {
            return false;
        }
    }
    /**
     * 增加AE合成的资源;
     * 你可以把各种Ae素材通过LSOAeCompositionAsset 创建一个合成, 把这个合成, 作为一个图层输入到容器中.
     * 注意:当前暂时不支持Ae合成资源中的声音, 如果有声音,则声音则通过addAudioLayer另外增加;
     *
     * 举例代码如下:
     * 把一个Ae模板作为一个资源,增加到容器中, 增加后, 返回一个"Ae合成图层"
     String jsonPath= copyShanChu(getApplicationContext(),"cs.json");
     String bgVideo=copyShanChu(getApplicationContext(),"cs_Bg.mp4");
     String colorPath=copyShanChu(getApplicationContext(),"cs_mvColor.mp4");
     String maskPath=copyShanChu(getApplicationContext(),"cs_mvMask.mp4");


     LSOAeDrawable drawable= LSOLoadAeJsons.loadSync(jsonPath);
     drawable.updateBitmap("image_0",copyShanChu2Bmp(getApplicationContext(),"img_0.png"));

     LSOAeCompositionAsset compositionAsset=new LSOAeCompositionAsset();
     compositionAsset.addFirstLayer(bgVideo);
     compositionAsset.addSecondLayer(drawable);
     compositionAsset.addThirdLayer(colorPath,maskPath);
     compositionAsset.startAeRender();
     * @param asset Ae合成资源;
     * @return
     */
    public LSOAECompositionLayer addAECompositionLayer(LSOAeCompositionAsset asset){
        return addAECompositionLayer(asset,0,Long.MAX_VALUE);
    }



    /**
     * 增加Ae合成的图层;
     * 你可以把各种Ae素材通过LSOAeCompositionAsset 创建一个合成, 把这个合成, 作为一个图层输入到容器中.
     * 注意:当前暂时不支持Ae合成资源中的声音, 如果有声音,则声音则通过addAudioLayer另外增加;
     * @param asset
     * @param startTimeUs 从容器什么时间点开始增加
     * @param endTimeUs 增加到容器的什么时间点;
     * @return
     */
    public LSOAECompositionLayer addAECompositionLayer(LSOAeCompositionAsset asset,long startTimeUs, long endTimeUs){
        if(asset!=null && asset.prepare() && setup()){
            asset.startAeRender();
            if (commonRunnable != null) {
                return commonRunnable.addAECompositionLayer(asset,startTimeUs,endTimeUs);
            }else if (pixelRunnable != null) {
                asset.startAeRender();
                return pixelRunnable.addAECompositionLayer(asset, startTimeUs, endTimeUs);
            }
        }
        return null;
    }

    /**
     * 增加相册影集图层,

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
     6. LSOPhotoAlbumAsset在使用完毕后,确认不再使用时, 一定要调用release,比如在让用户重新选择图片的前一行调用;


     String jsonPath = copyAeAssets(getApplicationContext(), "morePicture.json");
     List<Bitmap> bitmaps=new ArrayList<>();

     for (int i = 0; i <10;i++) {
     String name = "morePicture_img_" + i + ".jpeg";
     bitmaps.add(BitmapFactory.decodeFile(copyAeAssets(getApplicationContext(),name)));
     }
     LSOPhotoAlbumAsset albumAsset=new LSOPhotoAlbumAsset(bitmaps,jsonPath);
     allExecute.addPhotoAlbumLayer(albumAsset);

     * @param asset 影集图层资源.
     * @return
     */
    public LSOPhotoAlbumLayer addPhotoAlbumLayer(LSOPhotoAlbumAsset asset) {
        if(asset!=null && setup()) {
            if (commonRunnable != null) {
                return commonRunnable.addPhotoAlbumLayer(asset);
            } else if (pixelRunnable != null) {
                return pixelRunnable.addPhotoAlbumLayer(asset);
            }
        }
        return null;
    }

    /**
     * 增加Ae图层, 把Ae的json作为一个图层增加到容器中;
     * @param dr
     * @return
     */
    public AEJsonLayer addAeJsonLayer(LSOAeDrawable dr){
        return addAeJsonLayer(dr,0,Long.MAX_VALUE);
    }

    /**
     * 增加Ae图层, 把Ae的json作为一个图层增加到容器中;
     * @param dr 解析并替换各种素材后的drawable对象
     * @param startTimeUs 这个图层在容器中的开始时间
     * @param endTimeUs  图层在容器中的结束时间;
     * @return
     */
    public AEJsonLayer addAeJsonLayer(LSOAeDrawable dr, long startTimeUs, long endTimeUs){
        if(dr!=null && setup()){
            if (commonRunnable != null) {
                return commonRunnable.addAeJsonLayer(dr,startTimeUs,endTimeUs);
            }else if (pixelRunnable != null) {
                return pixelRunnable.addAeJsonLayer(dr, startTimeUs, endTimeUs);
            }
        }
        return null;
    }


    /**
     * 增加声音图层;
     * @param srcPath 声音的完整路径
     * @return
     */
    public AudioLayer addAudioLayer(String srcPath) {
        if(commonRunnable !=null){
            return commonRunnable.addAudioLayer(srcPath,0,0, Long.MAX_VALUE);
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
        if(commonRunnable !=null){
            AudioLayer layer= commonRunnable.addAudioLayer(srcPath,0,0, Long.MAX_VALUE);
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
        if(commonRunnable !=null){
            AudioLayer layer= commonRunnable.addAudioLayer(srcPath,0,0, Long.MAX_VALUE);
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
        if(commonRunnable !=null && srcPath!=null){
            AudioLayer layer= commonRunnable.addAudioLayer(srcPath,startPadUs,0, Long.MAX_VALUE);
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
     * 把音频的 指定时间段, 增加到audio pad音频容器里.
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
        if(commonRunnable !=null){
            AudioLayer layer= commonRunnable.addAudioLayer(srcPath,offsetPadUs,startAudioUs, endAudioUs);
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
        if(commonRunnable !=null){
            return commonRunnable.getLayerSize();
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
        if(commonRunnable !=null){
            commonRunnable.bringToBack(layer);
        }else if(pixelRunnable !=null){
            pixelRunnable.bringToBack(layer);
        }
    }

    /**
     * 把图层放到容器的最外层,
     * @param layer
     */
    public void bringToFront(Layer layer) {
        if(commonRunnable !=null){
            commonRunnable.bringToFront(layer);
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
        if(commonRunnable !=null){
            commonRunnable.changeLayerPosition(layer,position);
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
        if(commonRunnable !=null){
            commonRunnable.swapTwoLayerPosition(first,second);
        }else if(pixelRunnable !=null){
            pixelRunnable.swapTwoLayerPosition(first,second);
        }
    }

    /**
     * 进度监听  ---经过handle机制, 可以在主线程中调用UI界面;
     * OnLanSongSDKProgressListener 的两个方法分别是:
     * long ptsUs : 当前处理视频帧的时间戳. 单位微秒; 1秒=1000*1000微秒
     * int percent : 当前处理的进度百分比. 范围:0--100;
     */
    public void setOnLanSongSDKProgressListener(OnLanSongSDKProgressListener listener) {
        if(commonRunnable !=null){
            commonRunnable.setOnLanSongSDKProgressListener(listener);
        }else  if(pixelRunnable !=null){
            pixelRunnable.setOnLanSongSDKProgressListener(listener);
        }
    }

    /**
     * 设置进度监听 ----不经过handle机制,不可以在主线程中调用UI界面;
     * 进度返回的两个参数 long类型是:当前时间戳,单位微秒; int类型是:当前处理的百分比;
     * @param listener
     */
    public void setOnLanSongSDKThreadProgressListener(OnLanSongSDKThreadProgressListener listener) {
        if(commonRunnable !=null){
            commonRunnable.setOnLanSongSDKThreadProgressListener(listener);
        }else if(pixelRunnable !=null){
            pixelRunnable.setOnLanSongSDKThreadProgressListener(listener);
        }
    }

    /**
     * 完成回调
     * 回调完成后, 返回给你处理后的视频完整路径.
     * @param listener
     */
    public void setOnLanSongSDKCompletedListener(OnLanSongSDKCompletedListener listener) {
        if(commonRunnable !=null){
            commonRunnable.setOnLanSongSDKCompletedListener(listener);
        }else  if(pixelRunnable !=null){
            pixelRunnable.setOnLanSongSDKCompletedListener(listener);
        }
    }

    /**
     * 错误回调
     * @param listener
     */
    public void setOnLanSongSDKErrorListener(OnLanSongSDKErrorListener listener) {
        if(commonRunnable !=null){
            commonRunnable.setOnLanSongSDKErrorListener(listener);
        }else if(pixelRunnable !=null){
            pixelRunnable.setOnLanSongSDKErrorListener(listener);
        }
    }

    public void removeLayer(Layer layer) {
        if(commonRunnable !=null){
            commonRunnable.removeLayer(layer);
        }else  if(pixelRunnable !=null){
            pixelRunnable.removeLayer(layer);
        }
    }

    /**
     * 删除所有图层
     */
    public void removeAllLayer() {
        if (commonRunnable != null) {
            commonRunnable.removeAllLayer();
        }else  if (pixelRunnable != null) {
            pixelRunnable.removeAllLayer();
        }
    }
    public boolean isRunning() {
        if(commonRunnable !=null){
            return commonRunnable.isRunning();
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
        if(commonRunnable !=null){
            return commonRunnable.start();
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
        if(commonRunnable !=null){
            commonRunnable.cancel();
            commonRunnable.release();
            commonRunnable =null;
        }else if(pixelRunnable !=null){
            pixelRunnable.cancel();
            pixelRunnable.release();
            pixelRunnable =null;
        }
        success =false;
    }
    /**
     * 释放;
     */
    public void release() {
        if(commonRunnable !=null){
            commonRunnable.release();
            commonRunnable =null;
            success =false;
        }else if(pixelRunnable !=null){
            pixelRunnable.release();
            pixelRunnable =null;
            success =false;
        }
    }

    /**
     * 不检查容器尺寸.
     * 我们默认内部会16字节对齐; 如果调用此方法,则以您设置的宽高为准;
     * [不建议使用]
     */
    public void setNotCheckDrawPadSize() {
        if(commonRunnable !=null){
            commonRunnable.setNotCheckDrawPadSize();
        }else if(pixelRunnable !=null){
            pixelRunnable.setNotCheckDrawPadSize();
        }
    }
    //---------------------------------------------------------------------------
    private synchronized boolean setup(){
        if(commonRunnable !=null && !commonRunnable.isRunning() && !success){
            commonRunnable.setup();
            success =true;
        }else if(pixelRunnable !=null && !pixelRunnable.isRunning() && !success){
            pixelRunnable.setup();
            success =true;
        }
        return success;
    }
    //---------------------------test Demo测试例子------------------------------------------------
    /**


     DrawPadAllExecute2 allExecute;
     private void testAllexecute() throws Exception {
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
     layer3.setPosition(LSOLayerPosition.RIGHT_TOP);


     layer1.setMaskBitmapWithRecycle(BitmapFactory.decodeResource(getResources(),R.drawable.ls_logo),true);
     allExecute.play();


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

     private void imgToVideo() {
     try {
     DrawPadAllExecute2 allExecute2 = new DrawPadAllExecute2(getApplicationContext(), 720, 1280, 10*1000*1000);
     allExecute2.setFrameRate(29);
     Bitmap bgBitmap = Bitmap.createBitmap(720, 1280,Bitmap.Config.ARGB_8888);
     bgBitmap.eraseColor(Color.parseColor("#009900"));
     allExecute2.addBitmapLayer(bgBitmap);


     List<String> path2 = new ArrayList<>();
     for (int i = 1; i < 307; i++) {
     String videoPath ="/sdcard/TmpVideoMattingImg/"+ i + ".png";
     path2.add(videoPath);
     }

     allExecute2.addBitmapListLayer(path2, (int) 29);


     allExecute2.setOnLanSongSDKProgressListener(new OnLanSongSDKProgressListener() {
    @Override
    public void onLanSongSDKProgress(long ptsUs, int percent) {
    LSOLog.e("blackVideo7------>" + percent);
    }
    });
     allExecute2.setOnLanSongSDKCompletedListener(new OnLanSongSDKCompletedListener() {
    @Override
    public void onLanSongSDKCompleted(String dstVideo) {

    MediaInfo.checkFile(dstVideo);
    }
    });
     allExecute2.start();


     } catch (Exception e) {
     e.printStackTrace();
     }
     }
     */
}
