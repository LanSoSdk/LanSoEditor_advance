package com.example.advanceDemo.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;

import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.VideoOneDo2;

import java.io.IOException;

/**
 * 封装一下 编辑模式, 在转换的时候, 有提示;
 */
public class ConvertToEditModeDialog {

    ProgressDialog progressDialog;
    private Activity mActivity;
    private boolean isRunning;

    private VideoOneDo2 videoOneDo2;
    private MediaInfo mediaInfo;

    public interface onConvertToEditModeDialogListener {
        void onConvertCompleted(String video);
    }
    private  onConvertToEditModeDialogListener convertToEditModeDialogListener;
    /**
     * 把视频转换为编辑模式的视频, 有
     * @param activity
     * @param src  输入的视频.
     */
    public ConvertToEditModeDialog(Activity activity, String src,onConvertToEditModeDialogListener listener) {
        mActivity = activity;

        convertToEditModeDialogListener=listener;
        mediaInfo=new MediaInfo(src);
        if(mediaInfo.prepare())
            useVideoOneDo2(activity,src);
    }
    //使用VideoOneDo2
    private void useVideoOneDo2(Activity activity, String src)
    {
        try {
            videoOneDo2 = new VideoOneDo2(activity,src);
            videoOneDo2.setEditModeVideo();
            videoOneDo2.setOnVideoOneDoProgressListener(new OnLanSongSDKProgressListener() {
                @Override
                public void onLanSongSDKProgress(long ptsUs, int percent) {
                    if (progressDialog != null) {
                        progressDialog.setMessage("转换编辑模式2..." + percent + "%");
                    }
                }
            });
            videoOneDo2.setOnVideoOneDoCompletedListener(new OnLanSongSDKCompletedListener() {
                @Override
                public void onLanSongSDKCompleted(String dstVideo) {
                    if(videoOneDo2!=null){
                        videoOneDo2.release();
                        videoOneDo2=null;
                    }
                    cancelProgressDialog();
                    if(convertToEditModeDialogListener!=null){
                        convertToEditModeDialogListener.onConvertCompleted(dstVideo);
                    }
                }
            });
            videoOneDo2.setOnVideoOneDoErrorListener(new OnLanSongSDKErrorListener() {
                @Override
                public void onLanSongSDKError(int errorCode) {
                    Log.e("DEMO", "videoOneDo2.setOnVideoOneDoErrorListener: ");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void start() {
        if (!isRunning) {
            if(videoOneDo2!=null){
                videoOneDo2.start();
            }
            showProgressDialog();
            isRunning=true;
        }
    }

    public void release() {
        if (isRunning) {
            if(videoOneDo2!=null){
                videoOneDo2.release();
                videoOneDo2=null;
            }
            isRunning=false;
        }
        cancelProgressDialog();
        convertToEditModeDialogListener=null;
    }

    private void showProgressDialog() {
        progressDialog = new ProgressDialog(mActivity);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("正在转换为编辑模式...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void cancelProgressDialog() {
        if (progressDialog != null) {
            progressDialog.cancel();
            progressDialog = null;
        }
    }
}
