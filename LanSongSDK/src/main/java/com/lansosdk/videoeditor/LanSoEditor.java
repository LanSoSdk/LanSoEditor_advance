package com.lansosdk.videoeditor;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LanSoEditorBox;

import com.lansosdk.box.OnLanSongLogOutListener;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;

public class LanSoEditor {

    private static boolean isLoaded = false;

    public static void initSDK(Context context, String str){
        try {
            loadLibraries(); // 拿出来单独加载库文件.
        }catch (UnsatisfiedLinkError error){
            LSOLog.e("load libraries  error. Maybe it is where your app crashes, causing the entire Activity to restart.(你的APP崩溃了,查看所有的logcat信息)");
            error.printStackTrace();
        }

        initSo(context, str);
        LanSoEditor.setTempFileDir(Environment.getExternalStorageDirectory().getPath() + "/lansongBox/");
        printSDKVersion();
    }

    public static void unInitSDK(){
        unInitSo();
    }
    /**
     * 设置默认产生文件的文件夹,
     * 默认是:/sdcard/lansongBox/
     * @param tmpDir  设置临时文件夹的完整路径
     */
    public static void setTempFileDir(String tmpDir) {
        LanSoEditorBox.setTempFileDir(tmpDir);
        LanSongFileUtil.FileCacheDir = tmpDir;
    }


    /**
     * 是否不限制Ae模板的尺寸;
     * 默认是限制, 最大是1200x1920;
     * @param is  如果您不想被限制,则设置为true;
     */
    public static void setNoLimiteAESize(boolean is){
        LanSoEditorBox.setNoLimiteAESize(is);

    }
    /**
     * 设置临时文件夹的路径
     * 并设置文件名的前缀和后缀 我们默认是以当前时间年月日时分秒毫秒:yymmddhhmmss_ms为当前文件名字.
     * 你可以给这个名字增加一个前缀和后缀.比如xiaomi5_yymmddhhmmss_ms_version54.mp4等.
     * @param tmpDir  设置临时文件夹的完整路径
     * @param prefix  设置文件的前缀
     * @param subfix  设置文件的后缀.
     */
    public static void setTempFileDir(String tmpDir,String prefix,String subfix) {
        if(tmpDir!=null && prefix!=null && subfix!=null){

            if (!tmpDir.endsWith("/")) {
                tmpDir += "/";
            }

            LanSoEditorBox.setTempFileDir(tmpDir,prefix,subfix);
            LanSongFileUtil.FileCacheDir = tmpDir;
            LanSongFileUtil.mTmpFilePreFix=prefix;
            LanSongFileUtil.mTmpFileSubFix=subfix;
        }
    }

    /**
     * 设置SDK的log输出信息回调.
     * 您可以保存到文本里或打印出来.
     * @param listener
     */
    public static void setSDKLogOutListener(OnLanSongLogOutListener listener){

        if(listener!=null){
            printSDKVersion();
        }
        LSOLog.setLogOutListener(listener);
    }

    /**
     * 是否打开LSOLog的Log.d输出, 默认是打开的;
     * @param is
     */
    public static void setSDKLogOutDebugInfo(boolean is){
        LSOLog.setSDKLogOutDebugEnalbe(is);
    }

    //----------------------------------------------------------------------------------------
    private static void printSDKVersion()
    {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;

        String nativeVersion="* \tnative version:"+VideoEditor.getSDKVersion()+ " ;  ABI: "+VideoEditor.getCurrentNativeABI()+ " ; type:"+VideoEditor.getLanSongSDKType()
                + "; Limited time: year:"+VideoEditor.getLimitYear()+ " month:" +VideoEditor.getLimitMonth();

        String deviceInfo="* \tSystem Time is:Year:"+year+ " Month:"+month + " Build.MODEL:--->" + Build.MODEL+"<---VERSION:"+getAndroidVersion() + " cpuInfo:"+LanSoEditorBox.getCpuName();


        LSOLog.i("********************LanSongSDK**********************");
        LSOLog.i("* ");
        LSOLog.i(deviceInfo);
        LSOLog.i(nativeVersion);
        LSOLog.i("* \t"+ LanSoEditorBox.getBoxInfo());
        LSOLog.i("* ");
        LSOLog.i("*************************************************************");
    }
    private static String getAndroidVersion(){
        switch (Build.VERSION.SDK_INT){
            case 28:
                return "Androdi-9.0";
            case 27:
                return "Androdi-8.1";
            case 26:
                return "Androdi-8.0";
            case 25:
                return "Androdi-7.1.1";
            case 24:
                return "Androdi-7.0";
            case 23:
                return "Androdi-6.0";
            case 22:
                return "Androdi-5.1";
            case 21:
                return "Androdi-5.0";
            case 20:
                return "Androdi-4.4W";
            case 19:
                return "Androdi-4.4";
            case 18:
                return "Androdi-4.3";
            default:
                return "unknow-API="+Build.VERSION.SDK_INT;
        }
    }
    public static int getCPULevel() {
        return LanSoEditorBox.getCPULevel();
    }
    private static String getDiskCacheDir(Context context) {
        String cachePath = null;
        //  LanSoEditor.setTempFileDir(Environment.getExternalStorageDirectory().getPath() + "/lansongBox/");
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return cachePath;
    }
    private static synchronized void loadLibraries() throws  UnsatisfiedLinkError{
        if (isLoaded)
            return;


        System.loadLibrary("LanSongffmpeg");
        System.loadLibrary("LanSongdisplay");
        System.loadLibrary("LanSongplayer");

        LSOLog.d("loaded native libraries.isQiLinSoC:"+VideoEditor.isQiLinSoc());
        isLoaded = true;
    }

    private static void initSo(Context context, String str) {
        nativeInit(context, context.getAssets(), str);
        LanSoEditorBox.init(context);
    }
    private static void unInitSo() {
        nativeUninit();
    }




    private static native void nativeInit(Context ctx, AssetManager ass,String filename);
    private static native void nativeUninit();
    private static native void setLanSongSDK1();
}
