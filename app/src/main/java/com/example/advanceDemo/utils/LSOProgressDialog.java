package com.example.advanceDemo.utils;

import android.app.Activity;
import android.app.ProgressDialog;

public class LSOProgressDialog {

    ProgressDialog progressDialog;
    boolean isShowing=false;
    public void show(Activity acty) {
        release();
        progressDialog = new ProgressDialog(acty);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("正在处理中...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        isShowing=true;
    }

    public void setProgress(int percent){
            if(percent>=0 &&percent<=100){
                progressDialog.setMessage("正在处理中..."+ String.valueOf(percent) + "%");
            }
    }

    public void show(int percent,Activity acty){
            if(progressDialog ==null){
                show(acty);
            }
            progressDialog.setMessage("正在处理中..."+ String.valueOf(percent) + "%");
    }
    public void release() {
        if (progressDialog != null) {
            progressDialog.cancel();
            isShowing=false;
            progressDialog = null;
        }
    }
}
