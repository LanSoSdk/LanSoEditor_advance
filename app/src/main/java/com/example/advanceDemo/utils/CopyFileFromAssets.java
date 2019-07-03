package com.example.advanceDemo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.lansosdk.videoeditor.LanSongFileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

//LSDELETE 移动到demo的util中. SDK不再提供这个类;
public class CopyFileFromAssets {
    /**
     * 拷贝资源文件夹中的文件到默认地址. 如果文件已经存在,则直接返回文件路径
     *
     * @param context
     * @param assetsName
     * @return 返回 拷贝文件的目标路径
     */
    public static String copyAssets(Context context, String assetsName) {

        String filePath;
        if(LanSongFileUtil.getPath() !=null && !LanSongFileUtil.getPath().endsWith("/")){
            filePath = LanSongFileUtil.getPath() + "/" + assetsName;
        }else{
            filePath = LanSongFileUtil.getPath() +  assetsName;
        }

        File dir = new File(LanSongFileUtil.getPath());
        // 如果目录不中存在，创建这个目录
        if (!dir.exists())
            dir.mkdirs();
        try {
            if (!(new File(filePath)).exists()) { // 如果不存在.
//                InputStream is = mContext.getResources().getAssets().open(assetsName);  //原来的
                InputStream is = context.getAssets().open(assetsName);
                FileOutputStream fos = new FileOutputStream(filePath);
                byte[] buffer = new byte[7168];
                int count = 0;
                while ((count = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, count);
                }
                fos.close();
                is.close();
            } else {
                Log.i("copyFile","CopyFileFromAssets.copyAssets() is not work. file existe:"+filePath);
            }
            return filePath;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public static String copyAssets(Context context,String dirName, String assetsName,String dstFileName) {


        String dir = LanSongFileUtil.getPath() +  dirName;
        File dirFile = new File(dir);
        // 如果目录不中存在，创建这个目录
        if (!dirFile.exists())
            dirFile.mkdirs();

        String  dstName=dir+"/"+dstFileName;
        try {
            if (!(new File(dstName)).exists()) { // 如果不存在.
                InputStream is = context.getAssets().open(assetsName);
                FileOutputStream fos = new FileOutputStream(dstName);
                byte[] buffer = new byte[7168];
                int count = 0;
                while ((count = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, count);
                }
                fos.close();
                is.close();
            } else {
                Log.i("copyFile","CopyFileFromAssets.copyAssets() is not work. file existe:"+dstName);
            }
            return dstName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public static Bitmap copyAsset2Bmp(Context context, String assetsName){
        String path=copyAssets(context,assetsName);
        if(LanSongFileUtil.fileExist(path)){
            return BitmapFactory.decodeFile(path);
        }else{
            return null;
        }
    }

    public static Bitmap copyShanChu2Bmp(Context context, String assetsName){
        String path=copyShanChu(context,assetsName);
        if(LanSongFileUtil.fileExist(path)){
            return BitmapFactory.decodeFile(path);
        }else{
            return null;
        }
    }
    /**
     * 调试代码用的一些需要是删除的文件;
     *
     * @param context
     * @param assetsName
     * @return
     */
    public static String copyShanChu(Context context, String assetsName) {

        String filePath;


            if(LanSongFileUtil.getPath().endsWith("/")==false){
                filePath = LanSongFileUtil.getPath() + "/" + assetsName;
            }else{
                filePath = LanSongFileUtil.getPath() +  assetsName;
            }

        File dir = new File(LanSongFileUtil.getPath());
        // 如果目录不中存在，创建这个目录
        if (!dir.exists())
            dir.mkdirs();

        try {
            if (!(new File(filePath)).exists()) { // 如果不存在.

                String str="000shanchu/"+assetsName;

                InputStream is = context.getAssets().open(str);
                FileOutputStream fos = new FileOutputStream(filePath);
                byte[] buffer = new byte[7168];
                int count = 0;
                while ((count = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, count);
                }
                fos.close();
                is.close();
                Log.i("copyFile","CopyFileFromAssets.copyAssets() is  success. file save to:"+filePath);
            } else {
                Log.w("copyFile","CopyFileFromAssets.copyAssets() is  not work. file existe:"+filePath);
            }
            return filePath;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
