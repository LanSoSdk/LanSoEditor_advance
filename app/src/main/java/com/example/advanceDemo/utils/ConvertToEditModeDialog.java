package com.example.advanceDemo.utils;

import android.app.Activity;
import android.app.ProgressDialog;

import com.lansosdk.box.DrawPad;
import com.lansosdk.box.LayerShader;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.videoeditor.DrawPadVideoExecute;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.LanSongFileUtil;

/**
 * 封装一下 编辑模式, 在转换的时候, 有提示;
 */
public class ConvertToEditModeDialog {

    private final static String TAG = "ConvertToEditModeDialog";

    ProgressDialog progressDialog;
    private Activity mActivity;
    private boolean isRunning;


    private DrawPadVideoExecute execute2;


    private MediaInfo mediaInfo;
    private String dstPath;

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
        {
            dstPath=LanSongFileUtil.createMp4FileInBox();

            execute2 = new DrawPadVideoExecute(activity,src,dstPath);
            LayerShader.setEditMode();
            execute2.setDrawPadProgressListener(new onDrawPadProgressListener() {

                @Override
                public void onProgress(DrawPad v, long currentTimeUs) {

                    long total=(long)(mediaInfo.vDuration*1000000);
                    int percent=(int)(currentTimeUs*100/total);
                    if(percent>100){
                        percent=100;
                    }
                    if (progressDialog != null) {
                        progressDialog.setMessage("convertEditMode:正在转换为编辑模式..." + percent + "%");
                    }
                }
            });
            execute2.setDrawPadCompletedListener(new onDrawPadCompletedListener() {

                @Override
                public void onCompleted(DrawPad v) {
                    calcelProgressDialog();
                    if(convertToEditModeDialogListener!=null){
                        convertToEditModeDialogListener.onConvertCompleted(dstPath);
                    }
                }
            });
        }
    }

    public void start() {
        if (!isRunning) {
            if (execute2.startDrawPad()) {
                showProgressDialog();
            }
            isRunning=true;
        }
    }

    public void release() {
        if (isRunning) {
            execute2.stopDrawPad();
            isRunning=false;
        }
        calcelProgressDialog();
        convertToEditModeDialogListener=null;
    }

    private void showProgressDialog() {
        progressDialog = new ProgressDialog(mActivity);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("正在转换为编辑模式...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void calcelProgressDialog() {
        if (progressDialog != null) {
            progressDialog.cancel();
            progressDialog = null;
        }
    }
}
