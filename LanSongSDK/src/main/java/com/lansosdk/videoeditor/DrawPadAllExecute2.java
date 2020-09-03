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
    public static  boolean forceUseOLD =false;
    public DrawPadAllExecute2(Context ctx, int padWidth, int padHeight, long  durationUS) throws Exception {
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

    public void setFrameRate(int rate) {
        if(commonRunnable !=null){
            commonRunnable.setFrameRate(rate);
        }else if(pixelRunnable !=null){
            pixelRunnable.setFrameRate(rate);
        }
    }
    public int getPadWidth(){
        return padWidth;
    }

    public int getPadHeight(){
        return padHeight;
    }
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
    public MVCacheLayer addMVLayer(LSOMVAsset asset, long startTimeUs, long endTimeUs, boolean mute) {
        if (commonRunnable != null && setup()) {
            return commonRunnable.addMVLayer(asset,startTimeUs,endTimeUs,mute);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addMVLayer(asset,startTimeUs,endTimeUs,mute);
        }else {
            return null;
        }
    }
    public DataLayer addDataLayer(int dataWidth, int dataHeight,long startTimeUs,long endTimeUs) {
        if (commonRunnable != null && setup()) {
            return commonRunnable.addDataLayer(dataWidth,dataHeight,startTimeUs,endTimeUs);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addDataLayer(dataWidth,dataHeight,startTimeUs,endTimeUs);
        }else {
            return null;
        }
    }
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

    public GifLayer addGifLayer(LSOGifAsset gifAsset, long startTimeUs, long endTimeUs) {
        if (commonRunnable != null && setup()) {
            return commonRunnable.addGifLayer(gifAsset,startTimeUs,endTimeUs);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addGifLayer(gifAsset,startTimeUs,endTimeUs);
        }else {
            return null;
        }
    }
    public GifLayer addGifLayer(String gifPath) {
        if (commonRunnable != null && setup()) {
            return commonRunnable.addGifLayer(gifPath,0,Long.MAX_VALUE);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addGifLayer(gifPath,0,Long.MAX_VALUE);
        }else {
            return null;
        }
    }
    public GifLayer addGifLayer(int resId) {
        if (commonRunnable != null && setup()) {
            return commonRunnable.addGifLayer(resId,0,Long.MAX_VALUE);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addGifLayer(resId,0,Long.MAX_VALUE);
        }else {
            return null;
        }
    }
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
    public boolean addSubLayer(SubLayer layer){
        if (commonRunnable != null && setup()) {
            return commonRunnable.addSubLayer(layer);
        }else if (pixelRunnable != null && setup()) {
            return pixelRunnable.addSubLayer(layer);
        }else {
            return false;
        }
    }
    public LSOAECompositionLayer addAECompositionLayer(LSOAeCompositionAsset asset){
        return addAECompositionLayer(asset,0,Long.MAX_VALUE);
    }
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


    public AEJsonLayer addAeJsonLayer(LSOAeDrawable dr){
        return addAeJsonLayer(dr,0,Long.MAX_VALUE);
    }

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



    public AudioLayer addAudioLayer(String srcPath) {
        if(commonRunnable !=null){
            return commonRunnable.addAudioLayer(srcPath,0,0, Long.MAX_VALUE);
        }else  if(pixelRunnable !=null){
            return pixelRunnable.addAudioLayer(srcPath,0,0, Long.MAX_VALUE);
        }else {
            return null;
        }
    }


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

    public int getLayerSize(){
        if(commonRunnable !=null){
            return commonRunnable.getLayerSize();
        }else if(pixelRunnable !=null){
            return pixelRunnable.getLayerSize();
        }else {
            return 0;
        }
    }

    public void bringToBack(Layer layer) {
        if(commonRunnable !=null){
            commonRunnable.bringToBack(layer);
        }else if(pixelRunnable !=null){
            pixelRunnable.bringToBack(layer);
        }
    }


    public void bringToFront(Layer layer) {
        if(commonRunnable !=null){
            commonRunnable.bringToFront(layer);
        }else if(pixelRunnable !=null){
            pixelRunnable.bringToFront(layer);
        }
    }


    public void changeLayerPosition(Layer layer, int position) {
        if(commonRunnable !=null){
            commonRunnable.changeLayerPosition(layer,position);
        }else if(pixelRunnable !=null){
            pixelRunnable.changeLayerPosition(layer,position);
        }
    }


    public void swapTwoLayerPosition(Layer first, Layer second) {
        if(commonRunnable !=null){
            commonRunnable.swapTwoLayerPosition(first,second);
        }else if(pixelRunnable !=null){
            pixelRunnable.swapTwoLayerPosition(first,second);
        }
    }


    public void setOnLanSongSDKProgressListener(OnLanSongSDKProgressListener listener) {
        if(commonRunnable !=null){
            commonRunnable.setOnLanSongSDKProgressListener(listener);
        }else  if(pixelRunnable !=null){
            pixelRunnable.setOnLanSongSDKProgressListener(listener);
        }
    }


    public void setOnLanSongSDKThreadProgressListener(OnLanSongSDKThreadProgressListener listener) {
        if(commonRunnable !=null){
            commonRunnable.setOnLanSongSDKThreadProgressListener(listener);
        }else if(pixelRunnable !=null){
            pixelRunnable.setOnLanSongSDKThreadProgressListener(listener);
        }
    }

    public void setOnLanSongSDKCompletedListener(OnLanSongSDKCompletedListener listener) {
        if(commonRunnable !=null){
            commonRunnable.setOnLanSongSDKCompletedListener(listener);
        }else  if(pixelRunnable !=null){
            pixelRunnable.setOnLanSongSDKCompletedListener(listener);
        }
    }


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

    public boolean start() {
        if(commonRunnable !=null){
            return commonRunnable.start();
        }else if(pixelRunnable !=null){
            return pixelRunnable.start();
        }else{
            return false;
        }
    }

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

}
