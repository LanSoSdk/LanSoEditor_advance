package com.example.advanceDemo.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.TextView;
import android.widget.Toast;

import com.lansosdk.videoeditor.LanSongFileUtil;

import static com.example.advanceDemo.utils.CopyFileFromAssets.copyAssets;

public class CopyDefaultVideoAsyncTask extends AsyncTask<Object, Object, Boolean> {
    private ProgressDialog mProgressDialog;
    private Context mContext = null;
    private TextView tvHint;
    private String fileName;
    private String dstPath;

    /**
     * @param ctx
     * @param tvhint 拷贝后, 把拷贝到的目标完整路径显示到这个TextView上.
     * @param file   需要拷贝的文件名字.
     */
    public CopyDefaultVideoAsyncTask(Context ctx, TextView tvhint, String file) {
        mContext = ctx;
        tvHint = tvhint;
        fileName = file;
    }

    @Override
    protected void onPreExecute() {
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setMessage("正在拷贝...");
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
        super.onPreExecute();
    }

    @Override
    protected synchronized Boolean doInBackground(Object... params) {
        if (!LanSongFileUtil.fileExist(fileName)) {
            dstPath=copyAssets(mContext, fileName);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if (mProgressDialog != null) {
            mProgressDialog.cancel();
            mProgressDialog = null;
        }
        if (LanSongFileUtil.fileExist(dstPath)) {
            Toast.makeText(mContext, "默认视频文件拷贝完成.视频样片路径:" + dstPath, Toast.LENGTH_SHORT).show();
            if (tvHint != null)
                tvHint.setText(dstPath);
        } else {
            Toast.makeText(mContext, "抱歉! 默认视频文件拷贝失败,请联系我们:视频样片路径:" + dstPath, Toast.LENGTH_SHORT).show();
        }
    }
}
