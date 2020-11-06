package com.lansosdk.videoeditor.archApi;

import android.os.Environment;

import com.lansosdk.box.LSOLog;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;



public class LanSongFileUtil {

    private static final Object mLock = new Object();

    private static final String DEFAULT_DIR= Environment.getExternalStorageDirectory().getPath()+"/lansongBox/";
    public static  String FileCacheDir =DEFAULT_DIR;
    protected static String mTmpFileSubFix="";  //后缀,
    protected static String mTmpFilePreFix="";  //前缀;



    public static String getPath() {
        File file = new File(FileCacheDir);
        if (!file.exists()) {
            if(!file.mkdir()){
                FileCacheDir =DEFAULT_DIR;
                if (!file.exists()) {
                    file.mkdir();
                }
            }
        }
        return FileCacheDir;
    }

    /**
     * 获取文件创建的所在的文件夹
     * @return
     */
    public static String getCreateFileDir() {
        return getPath();
    }

    /**
     * 在指定的文件夹里创建一个文件名字, 名字是当前时间,指定后缀.
     *
     * @return
     */
    public static String createFile(String dir, String suffix) {
        synchronized (mLock) {
            Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH) + 1;
            int day = c.get(Calendar.DAY_OF_MONTH);
            int second = c.get(Calendar.SECOND);
            int millisecond = c.get(Calendar.MILLISECOND);
            year = year - 2000;

            String dirPath = dir;
            File d = new File(dirPath);
            if (!d.exists())
                d.mkdirs();

            if (!dirPath.endsWith("/")) {
                dirPath += "/";
            }

            String name=mTmpFilePreFix;
            name += String.valueOf(year);
            name += String.valueOf(month);
            name += String.valueOf(day);
            name += String.valueOf(hour);
            name += String.valueOf(minute);
            name += String.valueOf(second);
            name += String.valueOf(millisecond);
            name+=mTmpFileSubFix;
            if (!suffix.startsWith(".")) {
                name += ".";
            }
            name += suffix;


            try {
                Thread.sleep(1); // 保持文件名的唯一性.
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            String retPath=dirPath+name;
            File file = new File(retPath);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return retPath;
        }
    }
    /**
     * 在box目录下生成一个mp4的文件,并返回名字的路径.
     *
     * @return
     */
    public static String createMp4FileInBox() {
        return createFile(FileCacheDir, ".mp4");
    }

    /**
     * 在box目录下生成一个aac的文件,并返回名字的路径.
     *
     * @return
     */
    public static String createAACFileInBox() {
        return createFile(FileCacheDir, ".aac");
    }

    public static String createM4AFileInBox() {
        return createFile(FileCacheDir, ".m4a");
    }

    public static String createMP3FileInBox() {
        return createFile(FileCacheDir, ".mp3");
    }


    /**
     * 创建wav格式的文件路径字符串
     * @return
     */
    public static String createWAVFileInBox() {
        return createFile(FileCacheDir, ".wav");
    }

    /**
     * 创建Gif文件路径字符串;
     * @return
     */
    public static String createGIFFileInBox() {
        return createFile(FileCacheDir, ".gif");
    }

    /**
     * 在box目录下生成一个指定后缀名的文件,并返回名字的路径.这里仅仅创建一个名字.
     *
     * @param suffix 指定的后缀名.
     * @return
     */
    public static String createFileInBox(String suffix) {
        return createFile(FileCacheDir, suffix);
    }

    /**
     * 只是在box目录生成一个路径字符串,但这个文件并不存在.
     *
     * @return
     */
    public static String newMp4PathInBox() {
        return newFilePath(FileCacheDir, ".mp4");
    }

    /**
     * 在指定的文件夹里 定义一个文件名字, 名字是当前时间,指定后缀.
     * 注意: 和 {@link #createFile(String, String)}的区别是,这里不生成文件,只是生成这个路径的字符串.
     *
     * @param suffix ".mp4"
     * @return
     */
    public static String newFilePath(String dir, String suffix) {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;
        int day = c.get(Calendar.DAY_OF_MONTH);
        int second = c.get(Calendar.SECOND);
        int millisecond = c.get(Calendar.MILLISECOND);
        year = year - 2000;
        String name = dir;
        File d = new File(name);

        // 如果目录不中存在，创建这个目录
        if (!d.exists())
            d.mkdir();
        name += "/";

        name += String.valueOf(year);
        name += String.valueOf(month);
        name += String.valueOf(day);
        name += String.valueOf(hour);
        name += String.valueOf(minute);
        name += String.valueOf(second);
        name += String.valueOf(millisecond);
        name += suffix;

        try {
            Thread.sleep(1);  //保持文件名的唯一性.
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return name;
    }

    /**
     * 删除指定的文件.
     */
    public static void deleteFile(String path) {
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
        }
    }
    public static void  deleteNameFiles(String prefix,String subfix)
    {
        deleteNameFiles(getCreateFileDir(),prefix,subfix);
    }
    /**
     * 删除包含某些字符串名字的 文件;
     * 比如要删除: /sdcard/lansongBox/lansong*.bmp(所有开头是lansong,后缀是bmp)
     * 则prefix=lansong;  subfix=bmp;
     * @param dir
     * @param prefix
     * @param subfix
     */
    public static void  deleteNameFiles(String dir,String prefix,String subfix)
    {
        File file=new File(dir);
        if(!file.exists()){
            file.mkdirs();
        }
        for (File item : file.listFiles()){
            if(!item.isDirectory()){
                String path=item.getAbsolutePath();
                String name=LanSongFileUtil.getFileNameFromPath(path);
                String subfix2=LanSongFileUtil.getFileSuffix(path);

                if(prefix!=null && subfix!=null){
                    if(name != null && name.contains(prefix) && subfix2.equals(subfix)){
                        LanSongFileUtil.deleteFile(path);
                    }
                }else if(prefix!=null){
                    if(name!=null && name.contains(prefix)){
                        LanSongFileUtil.deleteFile(path);
                    }
                }else if (subfix!=null){
                    if(subfix2!=null && subfix2.equals(subfix)){
                        LanSongFileUtil.deleteFile(path);
                    }
                }else{
                    LSOLog.e("删除指定文件失败,您设置的参数都是null");
                }
            }
        }
    }

    public static String getFileNameFromPath(String path) {
        if (path == null)
            return "";
        int index = path.lastIndexOf('/');
        if (index > -1)
            return path.substring(index + 1);
        else
            return path;
    }


    public static boolean fileExist(String absolutePath) {
        if (absolutePath == null)
            return false;
        else {
            File file = new File(absolutePath);
            if (file.exists())
                return true;
        }
        return false;
    }

    public static boolean filesExist(String[] fileArray) {

        for (String file : fileArray) {
            if (!fileExist(file))
                return false;
        }
        return true;
    }

    /** 获取文件后缀*/
    public static String getFileSuffix(String path) {
        if (path == null)
            return "";
        int index = path.lastIndexOf('.');
        if (index > -1)
            return path.substring(index + 1);
        else
            return "";
    }




    public static boolean close(Closeable closeable) {
        if (closeable != null)
            try {
                closeable.close();
                return true;
            } catch (IOException e) {
            }
        return false;
    }
    /**
     * 递归删除目录下的所有文件及子目录下所有文件
     *
     * @param dir 将要删除的文件目录
     * @return boolean Returns "true" if all deletions were successful.
     * If a deletion fails, the method stops attempting to
     * delete and returns "false".
     */
    public static boolean deleteDir(File dir) {
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



    public static boolean deleteDefaultDir() {
        File file=new File(FileCacheDir);
        if (file.isDirectory()) {
            String[] children = file.list();
            if(children!=null){
                for (int i = 0; i < children.length; i++) {
                    boolean success = deleteDir(new File(file, children[i]));
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        // 目录此时为空，可以删除
        return file.delete();
    }
}
