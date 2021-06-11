package com.lansosdk.videoeditor;

import android.content.Context;

import com.lansosdk.box.LSOAeExecuteRender;
import com.lansosdk.box.OnAERenderCompletedListener;
import com.lansosdk.box.OnAERenderErrorListener;
import com.lansosdk.box.OnAERenderProgressListener;
import com.lansosdk.box.OnAeExecutePrepareListener;
import com.lansosdk.box.OnLanSongSDKExportCompletedListener;
import com.lansosdk.box.OnLanSongSDKExportProgressListener;

import java.util.List;

/**
 * Description : 杭州蓝松科技有限公司
 *
 * @author guozhijun
 * @date 2021/4/10
 */
public class LSOAeExecute {


    private LSOAeExecuteRender render=null;

    public LSOAeExecute(Context context){
        render=new LSOAeExecuteRender(context);

    }
    /**
     * 增加AE模板中的背景视频
     */
    public void setBackGroundVideoPath(String path){
        if(render!=null){
            render.setBackGroundVideoLayer(path);
        }
    }

    /**
     * 增加json文件
     */
    public void setAeJsonPath(String path){
        if(render!=null){
            render.setAeJsonPath(path);
        }
    }

    /**
     * 增加json上层的透明视频
     */
    public void setMVVideoPath(String colorPath, String maskPath){
        if(render!=null){
            render.setMVVideoPath(colorPath,maskPath);
        }
    }

    /**
     * 设置要替换的图片/视频列表;
     * 内部会根据路径的后缀判断是图片或视频
     */
    public void setReplacePathList(List<String> path){
        if(render!=null){
            render.setReplacePathList(path);
        }
    }


    /**
     * 进度监听
     */
    public void setOnAERenderProgressListener(final OnAERenderProgressListener listener) {
        if(render!=null){
            render.setOnAERenderProgressListener(listener);
        }
    }

    /**
     * 完成监听
     * 完成后返回处理好的视频路径
     */
    public void setOnAERenderCompletedListener(final OnAERenderCompletedListener listener) {
        if(render!=null){
            render.setOnAERenderCompletedListener(listener);
        }
    }

    /**
     * 错误回调;
     */
    public void setOnAERenderErrorListener(final OnAERenderErrorListener listener) {
        if(render!=null){
            render.setOnAERenderErrorListener(listener);
        }
    }

    /**
     * 设置好以上代码后, 在开始前执行prepare异步准备一下;
     * 如果json有吴, 则listener中返回错误;
     */
    public void prepareAsync(OnAeExecutePrepareListener listener){
        if(render!=null){
            render.prepareAsync(listener);
        }
    }

    /**
     * 开始执行
     */
    public void start(){
            if(render!=null){
                render.start();
            }
    }

    /**
     * 释放;
     */
    public void release(){
        if(render!=null){
            render.release();
        }
    }
}
