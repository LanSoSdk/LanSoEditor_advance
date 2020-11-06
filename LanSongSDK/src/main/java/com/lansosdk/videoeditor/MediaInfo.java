package com.lansosdk.videoeditor;

import android.media.MediaExtractor;
import android.media.MediaFormat;

import com.lansosdk.box.LSOLog;

import java.io.File;
import java.io.IOException;


public class MediaInfo {


    private  String[] supportAudioFix={"wav","mp3","m4a","aac"};
    private  String[] supportVideoFix={"mp4","mov","flv"};

    private static final boolean VERBOSE = true;
    public final String filePath;
    public final String fileName; // 视频的文件名, 路径的最后一个/后的字符串.
    public final String fileSuffix; // 文件的后缀名.
    public final long fileLength; // 文件的总大小, 单位字节;
    protected int vWidth;

    /**
     * 视频编码高度
     * 不建议使用
     */
    protected int vHeight;


    protected int vCodecHeight;
    protected int vCodecWidth;
    /**
     * 视频的码率,
     */
    public int vBitRate;
    /**
     * 视频文件中的视频流总帧数.
     */
    public int vTotalFrames;
    /**
     * mp4文件中的视频轨道的总时长
     */
    public float vDuration;
    /**
     * 视频帧率,可能有浮点数
     */
    public float vFrameRate;
    /**
     * 视频旋转角度
     */
    public float vRotateAngle;
    /**
     * 该视频是否有B帧,
     */
    public boolean vHasBFrame;
    /**
     * 视频可以使用的解码器
     */
    public String vCodecName;
    /**
     * 视频的 像素格式.
     */
    public String vPixelFmt;

    /******************** audio track info ******************/
    /**
     * 音频采样率
     */
    public int aSampleRate;
    /**
     * 音频通道数量
     */
    public int aChannels;
    /**
     * 视频文件中的音频流 总帧数.
     */
    public int aTotalFrames;
    /**
     * 音频的码率
     */
    public int aBitRate;
    public int aMaxBitRate;
    /**
     * 多媒体文件中的音频总时长
     */
    public float aDuration;
    /**
     * 编码格式的名字;
     */
    public String aCodecName;
    private boolean getSuccess = false;
    private boolean isPngFile;



    public MediaInfo(String path) {
        filePath = path;
        fileName = getFileNameFromPath(path);
        fileSuffix = getFileSuffix(path);
        if (path != null){
            File file = new File(path);
            if (file.exists()){
                fileLength=file.length();
            }else{
                fileLength=0;
            }
            isPngFile="png".equalsIgnoreCase(fileSuffix);
        }else{
            fileLength=0;
        }
    }
    /**
     * 准备当前媒体的信息
     * 去底层运行相关方法, 得到媒体信息.
     *
     * @return 如获得当前媒体信息并支持格式, 则返回true, 否则返回false;
     */
    public boolean prepare() {
        int ret = 0;
        if (fileExist(filePath)) { // 这里检测下mfilePath是否是多媒体后缀.
            ret=nativePrepare(filePath,false);
            if (ret >= 0) {
                getSuccess = true;
                return isSupport();
            } else {
                if (ret == -13) {
                    LSOLog.e( "MediaInfo执行失败，可能您没有打开读写文件授权导致的，我们提供了PermissionsManager类来检测,可参考使用");
                } else {
                    LSOLog.e( "MediaInfo执行失败，" + prepareErrorInfo(filePath));
                }
                return false;
            }
        } else {
            LSOLog.e( "MediaInfo执行失败,你设置的文件不存在.您的设置是:"+filePath );
            return false;
        }
    }
    /**
     * 是否支持.
     *
     * @param videoPath
     * @return
     */
    public static boolean isSupport(String videoPath) {
        if (fileExist(videoPath)) {
            MediaInfo info = new MediaInfo(videoPath);
            return info.prepare() ;
        } else {
            return false;
        }
    }

    public static boolean isHaveVideo(String videoPath) {
        if (fileExist(videoPath)) {
            MediaInfo info = new MediaInfo(videoPath);
            return info.prepare() && info.isHaveVideo();
        } else {
            return false;
        }
    }
    /**
     * 如果在调试中遇到问题了, 首先应该执行这里, 这样可以检查出60%以上的错误信息. 在出错代码的上一行增加： 2018年1月4日20:54:07:
     * 新增, 在内部直接打印, 外部无效增加Log
     *
     * @param videoPath
     * @return
     */
    public static String checkFile(String videoPath) {
        return checkFile(videoPath,false);
    }

    private static boolean fileExist(String absolutePath) {
        if (absolutePath == null){
            return false;
        }else{
            return (new File(absolutePath)).exists();
        }
    }
    /**
     * 获取当前视频在显示的时候, 图像的宽度;
     * 因为有些视频是90度或270度旋转显示的, 旋转的话, 就宽高对调了
     * @return
     */
    public int getWidth() {
        if (getSuccess) {
            if (vRotateAngle == 90 || vRotateAngle == 270) {
                return vHeight;
            } else {
                return vWidth;
            }
        }
        return 0;
    }

    /**
     * 获取当前视频在显示的时候
     * @return
     */
    public int getHeight() {
        if (getSuccess) {
            if (vRotateAngle == 90 || vRotateAngle == 270) {
                return vWidth;
            } else {
                return vHeight;
            }
        }
        return 0;
    }
    public long getDurationUs(){
        if(vDuration>0){
            return (long)(vDuration *1000*1000);
        }else{
            LSOLog.e("MediaInfo getDurationUs error. vDuration =0;");
            return 1000;
        }

    }
    public long getVideoTrackDurationUs(){

        if(vDuration>0){
            return (long)(vDuration *1000*1000);
        }else{
            return 1;
        }
    }
    public long getAudioTrackDurationUs(){

        if(aDuration>0){
            return (long)(aDuration *1000*1000);
        }else{
            return 1;
        }
    }
    public String getVideoPath(){
        return  filePath;
    }

    public void release() {
        getSuccess = false;
    }

    /**
     * 是否是竖屏的视频.
     *
     * @return
     */
    public boolean isPortVideo() {
        if (vWidth > 0 && vHeight > 0) {
            // 高度大于宽度, 或者旋转角度等于90/270,则是竖屏, 其他认为是横屏.
            if ((vHeight > vWidth) && (vRotateAngle == 0)) {
                return true;
            } else if (vRotateAngle == 90 || vRotateAngle == 270) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean isHaveAudio() {
        // 有音频
        if (aBitRate > 0)
        {
            if (aChannels == 0) {
                return false;
            }
            if (aCodecName == null || aCodecName.isEmpty() || aDuration==0) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean isHaveVideo() {
        if(isPngFile && vWidth>0 && vHeight>0){
            return true;
        }else if (vBitRate > 0 || vWidth > 0 || vHeight > 0) {
            if (vHeight == 0 || vWidth == 0) {
                return false;
            }
            if (vCodecHeight == 0 || vCodecWidth == 0) {
                return false;
            }
            if (vCodecName == null || vCodecName.isEmpty())
                return false;

            if(vDuration<0 && ("mp4".equalsIgnoreCase(fileSuffix)
                    ||  "mov".equalsIgnoreCase(fileSuffix)
                    ||  "flv".equalsIgnoreCase(fileSuffix))){
                return false;
            }
            return true;
        }
        return false;
    }
    private boolean isInAudioSupportList(){
        for (String fix: supportAudioFix){
            if (fix.equalsIgnoreCase(fileSuffix)){
                return true;
            }
        }
        return false;
    }
    /**
     * 传递过来的文件是否支持
     *
     * @return
     */
    public boolean isSupport() {
        if(isHaveVideo()){
            return true;
        }else if(isInAudioSupportList()){
            return  isHaveAudio();
        }else{
            return false;
        }
    }
    int flvWidth;
    int flvHeight;
    long flvDurationUs;
    private void checkFLV(String path){
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(path);
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    flvWidth = format.getInteger(MediaFormat.KEY_WIDTH);
                    flvHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
                    flvDurationUs= format.getLong(MediaFormat.KEY_DURATION);
                    break;
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        extractor.release();
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        String info = "file name:" + filePath + "\n";
        info += "fileName:" + fileName + "\n";
        info += "fileSuffix:" + fileSuffix + "\n";
        info += "fileLength:" + fileLength + "\n";
        info += "vWidth:" + vWidth + "\n";
        info += "vHeight:" + vHeight + "\n";
        info += "vCodecWidth:" + vCodecWidth + "\n";
        info += "vCodecHeight:" + vCodecHeight + "\n";
        info += "vBitRate:" + vBitRate + "\n";
        info += "vTotalFrames:" + vTotalFrames + "\n";
        info += "vDuration:" + vDuration + "\n";
        info += "vFrameRate:" + vFrameRate + "\n";
        info += "vRotateAngle:" + vRotateAngle + "\n";
        info += "vHasBFrame:" + vHasBFrame + "\n";
        info += "vCodecName:" + vCodecName + "\n";
        info += "vPixelFmt:" + vPixelFmt + "\n";

        info += "aSampleRate:" + aSampleRate + "\n";
        info += "aChannels:" + aChannels + "\n";
        info += "aTotalFrames:" + aTotalFrames + "\n";
        info += "aBitRate:" + aBitRate + "\n";
        info += "aMaxBitRate:" + aMaxBitRate + "\n";
        info += "aDuration:" + aDuration + "\n";
        info += "aCodecName:" + aCodecName + "\n";

        // if(getSuccess) //直接返回,更直接, 如果执行错误, 更要返回
        return info;
        // else
        // return "MediaInfo is not ready.or call failed";
    }
    public native int nativePrepare(String filepath, boolean checkCodec);
    public native int nativePrepare2(String filepath, boolean checkCodec);
    // used by JNI
    private void setVideoCodecName(String name) {
        this.vCodecName = name;
    }

    // used by JNI
    private void setVideoPixelFormat(String pxlfmt) {
        this.vPixelFmt = pxlfmt;
    }

    // used by JNI
    private void setAudioCodecName(String name) {
        this.aCodecName = name;
    }

    private String prepareErrorInfo(String videoPath) {
        String ret = " ";
        if (videoPath == null) {
            ret = "文件名为空指针, null";
        } else {
            File file = new File(videoPath);
            if (!file.exists()) {
                ret = "文件不存在," + videoPath;
            } else if (file.isDirectory()) {
                ret = "您设置的路径是一个文件夹," + videoPath;
            } else if (file.length() == 0) {
                ret = "文件存在,但文件的大小为0字节." + videoPath;
            } else {
                if (fileSuffix.equals("pcm") || fileSuffix.equals("yuv")) {
                    String str = "文件路径:" + filePath + "\n";
                    str += "文件名:" + fileName + "\n";
                    str += "文件后缀:" + fileSuffix + "\n";
                    str += "文件大小(字节):" + file.length() + "\n";
                    ret = "文件存在,但文件的后缀可能表示是裸数据";
                    ret += str;
                } else {
                    ret = "文件存在, 但MediaInfo.prepare获取媒体信息失败,请查看下 文件是否是音频或视频, 或许演示工程APP名字不是我们demo中的名字:" + videoPath;
                }
            }

        }
        return ret;
    }

    private String getFileNameFromPath(String path) {
        if (path == null) {
            return "";
        }
        int index = path.lastIndexOf('/');
        if (index > -1) {
            return path.substring(index + 1);
        } else {
            return path;
        }
    }

    private String getFileSuffix(String path) {
        if (path == null) {
            return "";
        }
        int index = path.lastIndexOf('.');
        if (index > -1) {
            return path.substring(index + 1);
        } else {
            return "";
        }
    }
    /**
     * 是否不需要打印.
     * @param videoPath
     * @param noPrint
     * @return
     */
    public static String checkFile(String videoPath,boolean noPrint) {
        String ret = " ";
        if (videoPath == null) {
            ret = "文件名为空指针, null";
        } else {
            File file = new File(videoPath);
            if (!file.exists()) {
                ret = "文件不存在," + videoPath;
            } else if (file.isDirectory()) {
                ret = "您设置的路径是一个文件夹," + videoPath;
            } else if (file.length() == 0) {
                ret = "文件存在,但文件的大小为0字节(可能您只创建文件,但没有进行各种调用设置导致的.)." + videoPath;
            } else {
                MediaInfo info = new MediaInfo(videoPath);
                if (info.fileSuffix.equals("pcm")|| info.fileSuffix.equals("yuv")) {
                    String str = "文件路径:" + info.filePath + "\n";
                    str += "文件名:" + info.fileName + "\n";
                    str += "文件后缀:" + info.fileSuffix + "\n";
                    str += "文件大小(字节):" + file.length() + "\n";
                    ret = "文件存在,但文件的后缀可能表示是裸数据,我们的SDK需要多媒体格式的后缀是mp4/mp3/wav/aac/m4a/mov/gif常见格式";
                    ret += str;
                } else if (info.prepare()) {
                    ret = "文件内的信息是:\n";
                    String str = "文件路径:" + info.filePath + "\n";
                    str += "文件名:" + info.fileName + "\n";
                    str += "文件后缀:" + info.fileSuffix + "\n";
                    str += "文件大小(字节):" + file.length() + "\n";
                    if (info.isHaveVideo()) {
                        str += "视频信息-----:\n";
                        str += "宽度:" + info.vWidth + "\n";
                        str += "高度:" + info.vHeight + "\n";
                        str += "编码宽度:" + info.vCodecWidth + "\n";
                        str += "编码高度:" + info.vCodecHeight + "\n";
                        str += "时长:" + info.vDuration + "\n";
                        str += "帧率:" + info.vFrameRate + "\n";
                        str += "码率:" + info.vBitRate + "\n";
                        str += "总帧数:" + info.vTotalFrames + "\n";
                        str += "旋转角度:" + info.vRotateAngle + "\n";
                        str += "编码器名字:" + info.vCodecName + "\n";
                        str += "是否有B帧:" + info.vHasBFrame + "\n";
                        str += "像素格式:" + info.vPixelFmt + "\n";
                    } else {
                        str += "<无视频信息>\n";
                    }

                    if (info.isHaveAudio()) {
                        str += "音频信息-----:\n";
                        str += "采样率:" + info.aSampleRate + "\n";
                        str += "通道数:" + info.aChannels + "\n";
                        str += "码率:" + info.aBitRate + "\n";
                        str += "时长:" + info.aDuration + "\n";
                        str += "编码器:" + info.aCodecName + "\n";
                    } else {
                        str += "<无音频信息>\n";
                    }
                    ret += str;
                } else {
                    ret = "文件存在, 但MediaInfo.prepare获取媒体信息失败,请查看下 文件是否是音频或视频, 或许演示工程APP名字不是我们demo中的名字:"
                            + videoPath;
                }
            }
        }
        if(!noPrint){
            LSOLog.i( "当前文件的音视频信息是:" + ret);
        }
        return ret;
    }

    /**


     使用 : MediaInfo创建后, 执行prepare, 则得到各种文件信息;

     */
}
