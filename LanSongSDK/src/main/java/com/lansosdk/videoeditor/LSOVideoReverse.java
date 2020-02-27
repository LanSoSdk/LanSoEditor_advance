package com.lansosdk.videoeditor;

import com.lansosdk.box.LSOVideoReverseRunnable;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;


/**
 * 当前的视频倒序功能,
 * 此功能仅仅是完成视频倒序, 没有其他功能.
 * 临时使用;
 */
public class LSOVideoReverse {

    LSOVideoReverseRunnable runnable;

    /**
     * 构造方法;
     * @param path
     */
    public LSOVideoReverse(String  path) throws  Exception{


        if(!LSOVideoReverseRunnable.isSupport()){
            throw  new Exception("LSOVideoReverseRunnable not support this video.");
        }
        runnable=new LSOVideoReverseRunnable(path);
        if(!runnable.prepare()){
            throw  new Exception("LSOVideoReverseRunnable not support this video." + path);
        }



    }
    /**
     * 开始执行
     * @return
     */
    public boolean start(){
        return runnable!=null && runnable.start();
    }

    /**
     * 预览 进度监听
     */
    public void setOnLanSongSDKProgressListener(OnLanSongSDKProgressListener listener) {
        if(runnable!=null){
            runnable.setOnLanSongSDKProgressListener(listener);
        }
    }
    /**
     * 完成
     * @param listener
     */
    public void setOnLanSongSDKCompletedListener(OnLanSongSDKCompletedListener listener){
        if(runnable!=null){
            runnable.setOnLanSongSDKCompletedListener(listener);
        }
    }

    public void cancel(){
        if(runnable!=null){
            runnable.cancel();
            runnable=null;
        }
    }

    public  void release(){
        if(runnable!=null){
            runnable.release();
            runnable=null;
        }
    }
}
