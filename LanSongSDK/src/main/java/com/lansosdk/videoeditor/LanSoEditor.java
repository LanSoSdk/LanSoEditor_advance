package com.lansosdk.videoeditor;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;

import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LanSoEditorBox;
import com.lansosdk.box.OnLanSongLogOutListener;

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
            LSOLog.e("load libraries  error. Maybe it is where your app crashes, causing the entire Activity to restart.(你的APP崩溃后被系统再次启动,查看所有的logcat信息)");
            error.printStackTrace();
            return ;
        }

        initSo(context, str);


        //不再关闭;
        if(Environment.getExternalStorageDirectory()!=null){
            setTempFileDir(context.getFilesDir() + "/lansongBox/");
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

    /**
     * 删除默认文件夹;
     * 如果您在调用此方法前, 设置了文件夹路径,则会删除您指定的文件夹;
     * 如果没有调用,则删除我们默认文件夹;
     */
    public static void deleteDefaultDir() {
        LanSoEditorBox.deleteDefaultDirFiles();
        LanSongFileUtil.deleteDefaultDir();
    }
    /**
     * 设置默认产生文件的文件夹,
     * @param tmpDir  设置临时文件夹的完整路径
     */
    public static void setTempFileDir(String tmpDir) {
        LanSoEditorBox.setTempFileDir(tmpDir);
        LanSongFileUtil.FileCacheDir = tmpDir;
    }

    /**
     * 设置只使用软解码器, 这样兼容性好, 但处理速度可能会慢一些;
     */
    public static void setOnlySoftWareDecoder(boolean is){
        LanSoEditorBox.setOnlySoftWareDecoder(is);
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
            printSDKVersion(context);
        }
        LSOLog.setLogOutListener(listener);
    }

    public static void setForceLSOLayerHWCompress(boolean is){
        LanSoEditorBox.setForceHWCompress(is);
    }

    /**
     * 是否打印SDK中的调试信息(Log.d的信息);
     * 默认是打印.
     * @param is
     */
    public static void setSDKLogOutDebugInfo(boolean is){
        LSOLog.setSDKLogOutDebugEnable(is);
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

    /**
     * 是否是麒麟处理器,
     * 麒麟处理器无法获取到CPU型号, 只能从MODEL判断
     * @return
     */
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
    private static native void setLanSongSDK1();
}
