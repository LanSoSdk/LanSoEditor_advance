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

    /**
     * 蓝松SDK的版本号, 请勿修改;
     */
    public static String VERSION = "4.5.6";


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
            setTempFileDir(context.getExternalCacheDir() + "/lansongBox/");
        }

        LSOLog.init();
        LanSoEditorBox.deleteDefaultDirFiles();
        LanSongFileUtil.deleteDefaultDir();
        printSDKVersion(context);
        isLoadLanSongSDK.set(true);
    }

    public static void setTempFileDir(String tmpDir) {
        LanSoEditorBox.setTempFileDir(tmpDir);
        LanSongFileUtil.FileCacheDir=tmpDir;
    }

    public static void setSDKLogOutListener(OnLanSongLogOutListener listener){

        if(listener!=null){
            printSDKVersion(context);
        }
        LSOLog.setLogOutListener(listener);
    }

    public static void setDebugInfoEnable(boolean is){
        LSOLog.setDebugEnable(is);
    }



    //----------------------------------------------------------------------------------------
    private static void printSDKVersion(Context ctx)
    {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;


        String nativeVersion="* \t version:"+ VERSION + " ;  ABI: "+ VideoEditor.getCurrentNativeABI()+ "; Limited time: year:"+ VideoEditor.getLimitYear()+ " month:" + VideoEditor.getLimitMonth();


        DisplayMetrics dm = new DisplayMetrics();
        if (ctx != null && ctx.getResources() != null) {
            dm = ctx.getResources().getDisplayMetrics();
        }

        String deviceInfo="* \tSystem Time is:Year:"+year+ " Month:"+month + " Build.MODEL:--->" + Build.MODEL+"<---VERSION:"+getAndroidVersion() + " cpuInfo:"+ LanSoEditorBox.getCpuName()+
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


    private static synchronized void loadLibraries() throws  UnsatisfiedLinkError{
        System.loadLibrary("LanSongffmpeg");
        System.loadLibrary("LanSongdisplay");
        System.loadLibrary("LanSongplayer");

        System.loadLibrary("LanSongSDKDecoder");
        LSOLog.d("loaded native libraries.isQiLinSoC:"+isQiLinSoc());
    }

    private static void initSo(Context context, String str) {
        nativeInit(context, context.getAssets(), str);

        LSOLog.d("loaded native2 ...");
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
