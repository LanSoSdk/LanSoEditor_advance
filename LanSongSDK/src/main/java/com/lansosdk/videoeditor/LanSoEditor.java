package com.lansosdk.videoeditor;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;

import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LanSoEditorBox;
import com.lansosdk.box.OnLanSongLogOutListener;
import com.lansosdk.videoeditor.archApi.LanSongFileUtil;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;

public class LanSoEditor {

    protected static AtomicBoolean isLoadLanSongSDK = new AtomicBoolean(false);
    public static Context context;

    public static void initSDK(Context ctx, String str){

        context=ctx;

        if (isLoadLanSongSDK.get())
            return;

        try {
            loadLibraries();
        }catch (UnsatisfiedLinkError error){
            LSOLog.e("load libraries  error. ");
            error.printStackTrace();
            return ;
        }

        initSo(context, str);


        if(Environment.getExternalStorageDirectory()!=null){
           // setTempFileDir(context.getFilesDir() + "/lansongBox/");
            setTempFileDir(context.getExternalCacheDir() + "/lansongBox/");  //lsdelete实际删除这个, 保留上一个;
        }

        LSOLog.init();
        LanSoEditorBox.deleteDefaultDirFiles();
        LanSongFileUtil.deleteDefaultDir();
        printSDKVersion(context);
        isLoadLanSongSDK.set(true);
    }

    public static void unInitSDK(){
        unInitSo();
    }

    public static void deleteDefaultDir() {
        LanSoEditorBox.deleteDefaultDirFiles();
        LanSongFileUtil.deleteDefaultDir();
    }
    public static void setTempFileDir(String tmpDir) {
        LanSoEditorBox.setTempFileDir(tmpDir);
        LanSongFileUtil.FileCacheDir=tmpDir;
    }

    public static void setOnlySoftWareDecoder(boolean is){
        LanSoEditorBox.setOnlySoftWareDecoder(is);
    }

    public static void setSDKLogOutListener(OnLanSongLogOutListener listener){

        if(listener!=null){
            printSDKVersion(context);
        }
        LSOLog.setLogOutListener(listener);
    }

    /**
     * 是否打印SDK中的调试信息(Log.d的信息);
     * 默认是打印.
     * @param is
     */
    public static void setDebugInfoEnable(boolean is){
        LSOLog.setDebugEnable(is);
    }

    //----------------------------------------------------------------------------------------
    private static void printSDKVersion(Context ctx)
    {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;

        if (!VideoEditor.getCurrentNativeABI().equalsIgnoreCase("arm64-v8a")){
            LSOLog.e("当前你使用的so库不是arm64-v8a. 请尽快优化您所有so文件, 以确保支持,我们将在2020年的6月1号之后,不再对64位设备上运行armeabi-v7a库的问题做技术支持.(Please optimize all your SO files as soon as possible to ensure support for arm64-v8a. After June 1, 2020, we will no longer provide technical support for the problem of running armeabi-v7a library on 64-bit devices.)");
        }

        String nativeVersion="* \tnative version:"+VideoEditor.getSDKVersion()+ " ;  ABI: "+VideoEditor.getCurrentNativeABI()+ " ; type:"+VideoEditor.getLanSongSDKType()
                + "; Limited time: year:"+VideoEditor.getLimitYear()+ " month:" +VideoEditor.getLimitMonth();

        DisplayMetrics dm = new DisplayMetrics();
        dm = ctx.getResources().getDisplayMetrics();

        String deviceInfo="* \tSystem Time is:Year:"+year+ " Month:"+month + " Build.MODEL:--->" + Build.MODEL+"<---VERSION:"+getAndroidVersion() + " cpuInfo:"+LanSoEditorBox.getCpuName()+
                " display screen size:"+ dm.widthPixels+ " x "+ dm.heightPixels;



        LSOLog.i("********************LanSongSDK**********************");
        LSOLog.i("* ");
        LSOLog.i(deviceInfo);
        LSOLog.i(nativeVersion);
        LSOLog.i("* \t"+ LanSoEditorBox.getBoxInfo());
        LSOLog.i("* ");
        LSOLog.i("*************************************************************");
        LSOLog.d("getColorFormat:"+ LanSoEditorBox.getColorFormat());
    }
    private static String getAndroidVersion(){
        switch (Build.VERSION.SDK_INT){
            case 29:
                return "Android-10";
            case 28:
                return "Android-9.0";
            case 27:
                return "Android-8.1";
            case 26:
                return "Android-8.0";
            case 25:
                return "Android-7.1.1";
            case 24:
                return "Android-7.0";
            case 23:
                return "Android-6.0";
            case 22:
                return "Android-5.1";
            case 21:
                return "Android-5.0";
            case 20:
                return "Android-4.4W";
            case 19:
                return "Android-4.4";
            case 18:
                return "Android-4.3";
            default:
                return "unknown-API="+Build.VERSION.SDK_INT;
        }
    }

    public static int getCPULevel() {
        return LanSoEditorBox.getCPULevel();
    }

    private static synchronized void loadLibraries() throws  UnsatisfiedLinkError{
        System.loadLibrary("LanSongffmpeg");
        System.loadLibrary("LanSongdisplay");
        System.loadLibrary("LanSongplayer");
        System.loadLibrary("LanSongSDKDecoder");
        LSOLog.d("loaded native libraries.isQiLinSoC:"+isQiLinSoc());
    }

    private static void initSo(Context context, String str) {
        nativeInit(context, context.getAssets(), str);
        nativeInit2(context, context.getAssets(), str);
        LanSoEditorBox.init(context);
    }
    private static void unInitSo() {
        nativeUninit();
        LanSoEditorBox.unInit();
    }


    public static boolean isQiLinSoc()
    {
        if(LanSoEditorBox.isQiLinSoC()){
            return true;
        }
        if(Build.MODEL!=null) {
            if (Build.MODEL.contains("-AL")
                    || Build.MODEL.contains("-CL")
                    || Build.MODEL.contains("-TL")
                    || Build.MODEL.contains("-UL")
                    || Build.MODEL.contains("-DL")
            ) {
                return true;
            }
        }
        return false;
    }
    private static native void nativeInit(Context ctx, AssetManager ass,String filename);
    private static native void nativeInit2(Context ctx, AssetManager ass,String filename);
    private static native void nativeUninit();
}
