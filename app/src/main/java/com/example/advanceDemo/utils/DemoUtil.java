package com.example.advanceDemo.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.example.advanceDemo.VideoPlayerActivity;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.LanSoEditorBox;
import com.lansosdk.videoeditor.VideoEditor;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DemoUtil {

    static int bmtcnt = 0;
    private int interval = 5;
    private List<Bitmap> bmpLists = new ArrayList<Bitmap>();


    /**
     * 显示版本号.
     * @param activity
     */
    public static void showVersionDialog(Activity activity) {
        DisplayMetrics dm = new DisplayMetrics();
        dm = activity.getResources().getDisplayMetrics();

        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;


        int limitYear = VideoEditor.getLimitYear();
        int limitMonth = VideoEditor.getLimitMonth();


        if(year>limitYear || month>=limitMonth){
            DemoUtil.showDialog(activity, "SDK 已经过期,请联系我们更新.(time out.)");
        }else{
            String timeHint = activity.getResources().getString(R.string.sdk_limit);
            String version = VideoEditor.getSDKVersion() + ";\n BOX:" + LanSoEditorBox.VERSION_BOX;
            version += dm.widthPixels + " x" + dm.heightPixels;
            timeHint = String.format(timeHint, version, limitYear, limitMonth);
            timeHint+= " ABI: "+VideoEditor.getCurrentNativeABI();
            DemoUtil.showDialog(activity, timeHint);
        }
    }

    static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // 目录此时为空，可以删除
        return dir.delete();
    }

    /**
     * 把图片保存到文件, 这里只是用来调试程序使用.
     *
     * @param bmp
     */
    public static void savePng(Bitmap bmp) {
        if (bmp != null) {
            File dir = new File("/sdcard/extractf/");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            try {
                BufferedOutputStream bos;
                String name = "/sdcard/extractf/tt" + bmtcnt++ + ".png";
                Log.i("saveBitmap", "name:" + name);

                bos = new BufferedOutputStream(new FileOutputStream(name));
                bmp.compress(Bitmap.CompressFormat.PNG, 90, bos);
                bos.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            Log.i("saveBitmap", "error  bmp  is null");
        }
    }

    public static void showToast(Context ctx, String str) {
        Toast.makeText(ctx, str, Toast.LENGTH_SHORT).show();
        Log.i("x", str);
    }

    public static void showDialog(Activity aty, int stringId) {
        try {
            new AlertDialog.Builder(aty).setTitle("提示").setMessage(stringId)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).show();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void showDialog(Activity aty, String str) {
        try {
            if(!aty.isDestroyed()){
                new AlertDialog.Builder(aty).setTitle("提示").setMessage(str)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).show();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void pushBitmap(Bitmap bmp) {

        interval++;
        if (bmpLists.size() < 5 && bmp != null && interval % 5 == 0) {
            bmpLists.add(bmp);
        } else {
            Log.i("T", " size >20; push error!");
        }
    }

    /**
     * 同步执行, 很慢.
     */
    public void saveToSdcard() {
        for (Bitmap bmp : bmpLists) {
            savePng(bmp);
        }
    }

    /**
     * 开始播放目标文件
     */
    public static void playDstVideo(Activity act, String videoPath){
        Intent intent = new Intent(act, VideoPlayerActivity.class);
        intent.putExtra("videopath", videoPath);
        act.startActivity(intent);
    }
    public static void startPreviewVideo(Activity act, String videoPath){
        Intent intent = new Intent(act, VideoPlayerActivity.class);
        intent.putExtra("videopath", videoPath);
        act.startActivity(intent);
    }
    // mhandler.sendMessageDelayed(mhandler.obtainMessage(23),10); //别地方调用
    // private HandlerLoop mhandler=new HandlerLoop();
    // private int maskCnt=1;
    // private class HandlerLoop extends Handler
    // {
    // @Override
    // public void handleMessage(Message msg) {
    // super.handleMessage(msg);
    //
    // switchBitmap();
    // mhandler.sendMessageDelayed(mhandler.obtainMessage(23),10);
    // }
    // }
}
