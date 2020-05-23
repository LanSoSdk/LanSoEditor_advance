package com.example.advanceDemo.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;

public class DemoProgressDialog {

    ProgressDialog progressDialog;
    boolean isShowing=false;
    private Activity activity;

    public void show(Activity acty) {
        release();
        activity=acty;
        progressDialog = new ProgressDialog(acty);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("正在处理中...");
        progressDialog.setCancelable(true);
        progressDialog.show();
        isShowing=true;
    }

    public void showWithNoCancel(Activity acty) {
        release();
        progressDialog = new ProgressDialog(acty);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("正在处理中...");
//        progressDialog.setCancelable(cancelable);
        progressDialog.show();
        isShowing=true;
    }
    public ProgressDialog getProgressDialog(){
        return progressDialog;
    }

    public void setProgress(int percent){
        if(percent>=0 &&percent<=100){
            progressDialog.setMessage("正在处理中:"+ String.valueOf(percent) + "%");
        }
    }

    public void setMessage(String message){
        progressDialog.setMessage(message);
    }

    public void show(int percent,Activity acty){
        if(progressDialog ==null){
            show(acty);
        }
        progressDialog.setMessage("正在处理中:"+ String.valueOf(percent) + "%");
    }
    public void release() {
        try {
            if (progressDialog != null) {
                progressDialog.cancel();
                isShowing=false;
                progressDialog = null;
            }
        }catch (Exception e){
            e.printStackTrace();
            DemoLog.e("DemoProgress cancel error.");
        }
    }


    private static DemoProgressDialog demoProgressDialog;

    public  static void showPercent(Activity activity,int percent){
        if(demoProgressDialog==null){
            demoProgressDialog=new DemoProgressDialog();
            demoProgressDialog.show(activity);
        }
        demoProgressDialog.setProgress(percent);
    }
    public  static void showMessage(Activity activity,String message){
        if(demoProgressDialog==null){
            demoProgressDialog=new DemoProgressDialog();
            demoProgressDialog.show(activity);
        }
        demoProgressDialog.setMessage(message);
    }
    public static void releaseDialog(){
        if(demoProgressDialog!=null){
            demoProgressDialog.release();
            demoProgressDialog=null;
        }
    }
    private static DemoProgressDialog bufferingDialog;

    /**
     * 显示是否在缓冲中.....
     * @param activity
     * @param text
     * @param isShowing
     */
    public static void showBufferingHint(Activity activity,String text, boolean isShowing){
        if(isShowing){
            if(bufferingDialog!=null){
                bufferingDialog.release();
                bufferingDialog=null;
            }
            bufferingDialog=new DemoProgressDialog();
            bufferingDialog.show(activity);
            bufferingDialog.setMessage(text);
        }else if(bufferingDialog!=null){
            bufferingDialog.release();
            bufferingDialog=null;
        }
    }
}

/**
 progressDialog = new DemoProgressDialog();
 progressDialog.show(this);

 .....
 progressDialog.setProgress
 ......

 progressDialog.releaseOnTask();

 */
