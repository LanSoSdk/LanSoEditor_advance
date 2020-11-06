package com.lansosdk.videoeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.lansosdk.box.LSOLog;
import com.lansosdk.videoeditor.archApi.LanSongFileUtil;
import com.lansosdk.videoeditor.oldVersion.LanSongLogCollector;
import com.lansosdk.videoeditor.oldVersion.onVideoEditorProgressListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.lansosdk.videoeditor.archApi.LanSongFileUtil.fileExist;


public class VideoEditor {

    public static final String version="VideoEditor";
    public static String[] useSoftDecoderlist ={
            "SM919",
            "SM901"
    };

    public static boolean  isForceHWEncoder=false;

    public static boolean  isForceSoftWareEncoder=false;



    public static boolean  isForceSoftWareDecoder=false;


    private static boolean noCheck16Multi=false;

    public int encodeBitRate=0;

    public static final int VIDEO_EDITOR_EXECUTE_SUCCESS1 = 0;
    public static final int VIDEO_EDITOR_EXECUTE_SUCCESS2 = 1;
    public static final int VIDEO_EDITOR_EXECUTE_FAILED = -101;  //文件不存在。


    private final int VIDEO_EDITOR_HANDLER_PROGRESS = 203;
    private final int VIDEO_EDITOR_HANDLER_COMPLETED = 204;


    private static LanSongLogCollector lanSongLogCollector =null;


    public void setEncodeBitRate(int bitRate){
        encodeBitRate=bitRate;
    }

    public static void logEnable(Context ctx){

        if(ctx!=null){
            lanSongLogCollector =new LanSongLogCollector(ctx);
        }else{
            if(lanSongLogCollector !=null && lanSongLogCollector.isRunning()){
                lanSongLogCollector.stop();
                lanSongLogCollector =null;
            }
        }
    }

    public static String getErrorLog(){
        if(lanSongLogCollector !=null && lanSongLogCollector.isRunning()){
            return lanSongLogCollector.stop();
        }else{
            return null;
        }
    }

    public VideoEditor() {
        Looper looper;
        if ((looper = Looper.myLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else {
            LSOLog.e("Unable to obtain the thread Looper, may not be able to send listener;");
            mEventHandler = null;
        }
    }




    public onVideoEditorProgressListener mProgressListener = null;

    public void setOnProgressListener(onVideoEditorProgressListener listener) {
        mProgressListener = listener;
    }

    private void doOnProgressListener(int percent) {
        if (mProgressListener != null){
            mProgressListener.onProgress(this, percent);
        }
    }

    private EventHandler mEventHandler;

    private class EventHandler extends Handler {
        private final WeakReference<VideoEditor> mWeakExtract;

        public EventHandler(VideoEditor mp, Looper looper) {
            super(looper);
            mWeakExtract = new WeakReference<VideoEditor>(mp);
        }

        @Override
        public void handleMessage(Message msg) {
            VideoEditor videoEditor = mWeakExtract.get();
            if (videoEditor == null) {
                LSOLog.e(  "VideoEditor went away with unhandled events");
                return;
            }
            switch (msg.what) {
                case VIDEO_EDITOR_HANDLER_PROGRESS:
                    videoEditor.doOnProgressListener(msg.arg1);
                    break;
                default:
                    break;
            }
        }
    }

    public int executeVideoEditor(String[] array) {
        return execute(array);
    }
    public int executeVideoEditor2(String[] array) {
        return execute2(array);
    }



    public void  postAVLogFromNative(String log){
        LSOLog.e("native log:"+log);
    }
    @SuppressWarnings("unused")
    /* Used from JNI */
    private void postEventFromNative(int what, int arg1, int arg2) {
        LSOLog.i("postEvent from native  is:" + what);
        if (mEventHandler != null) {
            Message msg = mEventHandler.obtainMessage(VIDEO_EDITOR_HANDLER_PROGRESS);
            msg.arg1 = what;
            mEventHandler.sendMessage(msg);
        }
    }


    public static native int getLimitYear();

    public static native int getLimitMonth();

    public static native String getSDKVersion();

    public static native String  getCurrentNativeABI();

    public static native String nativeGetVideoDescription(String videoPath);


    public static native int getLanSongSDKType();


    private native int execute(Object cmdArray);

    private native int execute2(Object cmdArray);


    protected int durationMs=0;

    public native int setDurationMs(int durationMS);
    private native int setForceColorFormat(int format);


    public native void cancel();

    public String executePicture2Video(String srcPath, float duration) {
        if (fileExist(srcPath)) {
            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-loop");
            cmdList.add("1");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-t");
            cmdList.add(String.valueOf(duration));
            return executeAutoSwitch(cmdList);
        }
        return null;
    }

    @Deprecated
    public String executePcmMix(String srcPach1, int samplerate, int channel, String srcPach2, int samplerate2, int
            channel2,float value1, float value2) {
        List<String> cmdList = new ArrayList<String>();

        String filter = String.format(Locale.getDefault(), "[0:a]volume=volume=%f[a1]; [1:a]volume=volume=%f[a2]; " +
                "[a1][a2]amix=inputs=2:duration=first:dropout_transition=2", value1, value2);

        String  dstPath= LanSongFileUtil.createFileInBox("pcm");

        cmdList.add("-f");
        cmdList.add("s16le");
        cmdList.add("-ar");
        cmdList.add(String.valueOf(samplerate));
        cmdList.add("-ac");
        cmdList.add(String.valueOf(channel));
        cmdList.add("-i");
        cmdList.add(srcPach1);

        cmdList.add("-f");
        cmdList.add("s16le");
        cmdList.add("-ar");
        cmdList.add(String.valueOf(samplerate2));
        cmdList.add("-ac");
        cmdList.add(String.valueOf(channel2));
        cmdList.add("-i");
        cmdList.add(srcPach2);

        cmdList.add("-y");
        cmdList.add("-filter_complex");
        cmdList.add(filter);
        cmdList.add("-f");
        cmdList.add("s16le");
        cmdList.add("-acodec");
        cmdList.add("pcm_s16le");
        cmdList.add(dstPath);


        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        int  ret= executeVideoEditor(command);
        if(ret==0){
            return dstPath;
        }else{
            LanSongFileUtil.deleteFile(dstPath);
            return null;
        }
    }


    @Deprecated
    public String executePcmEncodeAac(String srcPach, int samplerate, int channel) {
        List<String> cmdList = new ArrayList<String>();

        String dstPath=LanSongFileUtil.createM4AFileInBox();
        cmdList.add("-f");
        cmdList.add("s16le");
        cmdList.add("-ar");
        cmdList.add(String.valueOf(samplerate));
        cmdList.add("-ac");
        cmdList.add(String.valueOf(channel));
        cmdList.add("-i");
        cmdList.add(srcPach);


        cmdList.add("-acodec");
        cmdList.add("libfaac");
        cmdList.add("-b:a");
        cmdList.add("64000");
        cmdList.add("-y");

        cmdList.add(dstPath);


        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        int ret= executeVideoEditor(command);
        if(ret==0){
            return dstPath;
        }else{
            LanSongFileUtil.deleteFile(dstPath);
            return null;
        }
    }


    public String executePcmComposeVideo(String srcPcm, int samplerate, int channel, String srcVideo) {
        List<String> cmdList = new ArrayList<String>();

        String dstPath=LanSongFileUtil.createMp4FileInBox();
        cmdList.add("-f");
        cmdList.add("s16le");
        cmdList.add("-ar");
        cmdList.add(String.valueOf(samplerate));
        cmdList.add("-ac");
        cmdList.add(String.valueOf(channel));
        cmdList.add("-i");
        cmdList.add(srcPcm);

        cmdList.add("-i");
        cmdList.add(srcVideo);

        cmdList.add("-acodec");
        cmdList.add("libfaac");
        cmdList.add("-b:a");
        cmdList.add("64000");
        cmdList.add("-y");

        cmdList.add("-vcodec");
        cmdList.add("copy");

        cmdList.add(dstPath);


        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        int ret= executeVideoEditor(command);
        if(ret==0){
            return dstPath;
        }else{
            LanSongFileUtil.deleteFile(dstPath);
            return null;
        }
    }


    public String executePcmMovToMp4(String srcVideo) {
        List<String> cmdList = new ArrayList<String>();

        String dstPath=LanSongFileUtil.createMp4FileInBox();

        cmdList.add("-i");
        cmdList.add(srcVideo);

        cmdList.add("-acodec");
        cmdList.add("libfaac");
        cmdList.add("-b:a");
        cmdList.add("128000");


        cmdList.add("-vcodec");
        cmdList.add("copy");

        cmdList.add("-y");
        cmdList.add(dstPath);
        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        int ret= executeVideoEditor(command);
        if(ret==0){
            return dstPath;
        }else{
            LanSongFileUtil.deleteFile(dstPath);
            return null;
        }
    }


    @Deprecated
    public String executeAudioVolumeMix(String audioPath1, String audioPath2, float value1, float value2) {
        List<String> cmdList = new ArrayList<String>();

        String filter = String.format(Locale.getDefault(), "[0:a]volume=volume=%f[a1]; [1:a]volume=volume=%f[a2]; " +
                "[a1][a2]amix=inputs=2:duration=first:dropout_transition=2", value1, value2);

        String dstPath=LanSongFileUtil.createM4AFileInBox();
        cmdList.add("-i");
        cmdList.add(audioPath1);

        cmdList.add("-i");
        cmdList.add(audioPath2);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-acodec");
        cmdList.add("libfaac");

        cmdList.add("-b:a");
        cmdList.add("128000");

        cmdList.add("-ac");
        cmdList.add("2");

        cmdList.add("-vn");

        cmdList.add("-y");
        cmdList.add(dstPath);

        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        int ret= executeVideoEditor(command);
        if(ret==0){
            return dstPath;
        }else{
            LanSongFileUtil.deleteFile(dstPath);
            return null;
        }
    }

    public String executeVideoCompress(String srcPath, float percent) {
        if (fileExist(srcPath)) {

            MediaInfo info = new MediaInfo(srcPath);
            if (info.prepare()) {

                setEncodeBitRate((int)(info.vBitRate *percent));


                List<String> cmdList = new ArrayList<String>();
                cmdList.add("-vcodec");
                cmdList.add("lansoh264_dec");

                cmdList.add("-i");
                cmdList.add(srcPath);
                cmdList.add("-acodec");
                cmdList.add("copy");

                return executeAutoSwitch(cmdList);
            }
        }
        return null;
    }

    public String executeGIF2MP4(String srcPath) {
        if (fileExist(srcPath)) {
            MediaInfo info = new MediaInfo(srcPath);
            if (info.prepare() && "gif".equalsIgnoreCase(info.fileSuffix)) {

                List<String> cmdList = new ArrayList<String>();
                cmdList.add("-i");
                cmdList.add(srcPath);
                if(info.vWidth%2!=0 || info.vHeight%2!=0){

                    int width=info.vWidth/2;
                    width*=2;

                    int height=info.vHeight/2;
                    height*=2;

                    String filter = String.format(Locale.getDefault(), "crop=%d:%d:%d:%d",width, height, 0,0);
                    cmdList.add("-vf");
                    cmdList.add(filter);
                }

                isForceSoftWareEncoder=true;
                isForceSoftWareDecoder=true;
                String ret=executeAutoSwitch(cmdList);
                isForceSoftWareEncoder=false;
                isForceSoftWareDecoder=false;
                return ret;
            }
        }
        return null;
    }

    public String executeGetAudioTrack(String srcMp4Path) {
        MediaInfo info = new MediaInfo(srcMp4Path);
        if(info.prepare() && info.isHaveAudio()){
            String audioPath = null;
            if ("aac".equalsIgnoreCase(info.aCodecName)) {
                audioPath = LanSongFileUtil.createFileInBox(".m4a");
            } else if ("mp3".equalsIgnoreCase(info.aCodecName))
                audioPath = LanSongFileUtil.createFileInBox(".mp3");

            if (audioPath != null) {

                List<String> cmdList = new ArrayList<String>();
                cmdList.add("-i");
                cmdList.add(srcMp4Path);
                cmdList.add("-acodec");
                cmdList.add("copy");
                cmdList.add("-vn");
                cmdList.add("-y");
                cmdList.add(audioPath);
                String[] command = new String[cmdList.size()];
                for (int i = 0; i < cmdList.size(); i++) {
                    command[i] = (String) cmdList.get(i);
                }
                int ret= executeVideoEditor(command);
                if(ret==0){
                    return audioPath;
                }else{
                    LanSongFileUtil.deleteFile(audioPath);
                }
            }
        }
        return null;
    }


    public String executeGetMp3FromVideo(String mp4Path,float startS, float durationS){
        MediaInfo info = new MediaInfo(mp4Path);
        if(info.prepare() && info.isHaveAudio()){
            String audioPath  = LanSongFileUtil.createFileInBox(".mp3");

            if (audioPath != null) {

                List<String> cmdList = new ArrayList<String>();
                cmdList.add("-i");
                cmdList.add(mp4Path);

                if(startS>=0){
                    cmdList.add("-ss");
                    cmdList.add(String.valueOf(startS));
                }


                if(durationS>0){
                    cmdList.add("-t");
                    cmdList.add(String.valueOf(durationS));
                }

                cmdList.add("-acodec");
                cmdList.add("libmp3lame");
                cmdList.add("-b:a");
                cmdList.add("128000");

                cmdList.add("-ac");
                cmdList.add("2");

                cmdList.add("-ar");
                cmdList.add(String.valueOf(info.aSampleRate));


                cmdList.add("-vn");
                cmdList.add("-y");
                cmdList.add(audioPath);
                String[] command = new String[cmdList.size()];
                for (int i = 0; i < cmdList.size(); i++) {
                    command[i] = (String) cmdList.get(i);
                }
                int ret= executeVideoEditor(command);
                if(ret==0){
                    return audioPath;
                }else{
                    LanSongFileUtil.deleteFile(audioPath);
                }
            }
        }
        return null;
    }


    public String executeGetVideoTrack(String srcMp4Path) {
        if(fileExist(srcMp4Path)){
            String videoPath  = LanSongFileUtil.createMp4FileInBox();
            List<String> cmdList = new ArrayList<String>();
            cmdList.add("-i");
            cmdList.add(srcMp4Path);
            cmdList.add("-vcodec");
            cmdList.add("copy");
            cmdList.add("-an");
            cmdList.add("-y");
            cmdList.add(videoPath);

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret= executeVideoEditor(command);
            if(ret==0){
                return videoPath;
            }else{
                LanSongFileUtil.deleteFile(videoPath);
            }
        }
        LSOLog.e( "执行获取视频轨道 错误, !!!!");
        return null;
    }

    public String executeVideoMergeAudio(String video, String audio) {

        MediaInfo vInfo=new MediaInfo(video);
        MediaInfo aInfo=new MediaInfo(audio);

        if(vInfo.prepare() && aInfo.prepare() && aInfo.isHaveAudio()){

            String retPath=LanSongFileUtil.createMp4FileInBox();
            boolean isAAC="aac".equals(aInfo.aCodecName);

            List<String> cmdList = new ArrayList<String>();
            cmdList.add("-i");
            cmdList.add(video);

            cmdList.add("-i");
            cmdList.add(audio);

            cmdList.add("-t");
            cmdList.add(String.valueOf(vInfo.vDuration));

            if(isAAC) {  //删去视频的原音,直接增加音频

                cmdList.add("-map");
                cmdList.add("0:v");

                cmdList.add("-map");
                cmdList.add("1:a");

                cmdList.add("-vcodec");
                cmdList.add("copy");

                cmdList.add("-acodec");
                cmdList.add("copy");

                cmdList.add("-absf");
                cmdList.add("aac_adtstoasc");

            }else { //删去视频的原音,并对音频编码
                cmdList.add("-map");
                cmdList.add("0:v");

                cmdList.add("-map");
                cmdList.add("1:a");

                cmdList.add("-vcodec");
                cmdList.add("copy");

                cmdList.add("-acodec");
                cmdList.add("libfaac");

                cmdList.add("-ac");
                cmdList.add("2");

                cmdList.add("-ar");
                cmdList.add("44100");

                cmdList.add("-b:a");
                cmdList.add("128000");
            }

            cmdList.add("-y");
            cmdList.add(retPath);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            VideoEditor editor = new VideoEditor();
            int ret = editor.executeVideoEditor(command);
            if(ret==0){
                return retPath;
            }else{
                return video;
            }
        }
        return video;
    }

    @Deprecated
    public String executeVideoMergeAudio(String video, String audio,float audiostartS) {
        MediaInfo vInfo=new MediaInfo(video);
        MediaInfo aInfo=new MediaInfo(audio);

        if(vInfo.prepare() && aInfo.prepare() && aInfo.isHaveAudio()){

            String retPath=LanSongFileUtil.createMp4FileInBox();
            boolean isAAC="aac".equals(aInfo.aCodecName);

            List<String> cmdList = new ArrayList<String>();
            cmdList.add("-i");
            cmdList.add(video);

            cmdList.add("-ss");
            cmdList.add(String.valueOf(audiostartS));

            cmdList.add("-i");
            cmdList.add(audio);

            cmdList.add("-t");
            cmdList.add(String.valueOf(vInfo.vDuration));

            if(isAAC) {  //删去视频的原音,直接增加音频
                cmdList.add("-map");
                cmdList.add("0:v");

                cmdList.add("-map");
                cmdList.add("1:a");

                cmdList.add("-vcodec");
                cmdList.add("copy");

                cmdList.add("-acodec");
                cmdList.add("copy");

                cmdList.add("-absf");
                cmdList.add("aac_adtstoasc");

            }else { //删去视频的原音,并对音频编码
                cmdList.add("-map");
                cmdList.add("0:v");

                cmdList.add("-map");
                cmdList.add("1:a");

                cmdList.add("-vcodec");
                cmdList.add("copy");

                cmdList.add("-acodec");
                cmdList.add("libfaac");

                cmdList.add("-ac");
                cmdList.add("2");

                cmdList.add("-ar");
                cmdList.add("44100");

                cmdList.add("-b:a");
                cmdList.add("128000");
            }

            cmdList.add("-y");
            cmdList.add(retPath);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            VideoEditor editor = new VideoEditor();
            int ret = editor.executeVideoEditor(command);
            if(ret==0){
                return retPath;
            }else{
                return video;
            }
        }
        return video;
    }



    @Deprecated
    public String executeCutAudio(String srcFile, float startS, float durationS) {
        MediaInfo info=new MediaInfo(srcFile);
        if (info.prepare()) {



            durationMs=((int)(durationS *1000));

            List<String> cmdList = new ArrayList<String>();

            String dstFile=LanSongFileUtil.createFileInBox(LanSongFileUtil.getFileSuffix(srcFile));
            cmdList.add("-ss");
            cmdList.add(String.valueOf(startS));

            cmdList.add("-i");
            cmdList.add(srcFile);

            cmdList.add("-t");
            cmdList.add(String.valueOf(durationS));

            cmdList.add("-acodec");
            cmdList.add("copy");
            cmdList.add("-y");
            cmdList.add(dstFile);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret= executeVideoEditor(command);
            if(ret==0){
                return dstFile;
            }else{
                LanSongFileUtil.deleteFile(dstFile);
                return null;
            }
        } else {
            return null;
        }
    }


    public String executeCutVideo(String videoFile, float startS, float durationS) {
        if (LanSongFileUtil.fileExist(videoFile)) {
            durationMs=((int)(durationS *1000));
            String dstFile=LanSongFileUtil.createMp4FileInBox();

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-ss");
            cmdList.add(String.valueOf(startS));

            cmdList.add("-i");
            cmdList.add(videoFile);

            cmdList.add("-t");
            cmdList.add(String.valueOf(durationS));

            cmdList.add("-vcodec");
            cmdList.add("copy");
            cmdList.add("-acodec");
            cmdList.add("copy");
            cmdList.add("-y");
            cmdList.add(dstFile);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret= executeVideoEditor(command);
            if(ret==0){
                return dstFile;
            }else{
                LanSongFileUtil.deleteFile(dstFile);
                return null;
            }
        } else {
            return null;
        }
    }

    public String executeCutVideoExact(String videoFile, float startS, float durationS) {
        MediaInfo info=new MediaInfo(videoFile);
        if (info.prepare()) {

            durationMs=( (int)(durationS *1000));

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(videoFile);

            cmdList.add("-ss");
            cmdList.add(String.valueOf(startS));

            cmdList.add("-t");
            cmdList.add(String.valueOf(durationS));

            cmdList.add("-acodec");
            cmdList.add("copy");
            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }




    public String executeCutVideoExact(String videoFile, float startS, float durationS, int width, int height) {
        MediaInfo info=new MediaInfo(videoFile);
        if (info.prepare()) {

            durationMs=((int)(durationS *1000));
            List<String> cmdList = new ArrayList<String>();

            String scalecmd = String.format(Locale.getDefault(), "scale=%d:%d", width, height);

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(videoFile);

            cmdList.add("-ss");
            cmdList.add(String.valueOf(startS));

            cmdList.add("-t");
            cmdList.add(String.valueOf(durationS));

            cmdList.add("-vf");
            cmdList.add(scalecmd);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    public int executeGetAllFrames(String videoFile, String dstDir, String jpgPrefix) {
        String dstPath = dstDir + jpgPrefix + "_%3d.jpeg";
        if (fileExist(videoFile)) {

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(videoFile);

            cmdList.add("-qscale:v");
            cmdList.add("2");

            cmdList.add(dstPath);

            cmdList.add("-y");

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            return executeVideoEditor(command);

        } else {
            return VIDEO_EDITOR_EXECUTE_FAILED;
        }
    }

    public int executeGetSomeFrames(String videoFile, String dstDir, String jpgPrefix, float sampeRate) {
        String dstPath = dstDir + jpgPrefix + "_%3d.jpeg";
        if (fileExist(videoFile)) {

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(videoFile);

//					cmdList.add("-qscale:v");
//					cmdList.add("2");

            cmdList.add("-vsync");
            cmdList.add("1");

            cmdList.add("-r");
            cmdList.add(String.valueOf(sampeRate));

//					cmdList.add("-f");
//					cmdList.add("image2");

            cmdList.add("-y");

            cmdList.add(dstPath);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            return executeVideoEditor(command);

        } else {
            return VIDEO_EDITOR_EXECUTE_FAILED;
        }
    }

    public int executeGetKeyFrames(String videoFile, String dstDir, String jpgPrefix) {
        String dstPath = dstDir + "/" + jpgPrefix + "_%3d.png";
        if (fileExist(videoFile)) {

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(videoFile);

            cmdList.add("-vf");
            cmdList.add("select=eq(pict_type\\,I)");

            cmdList.add("-vsync");
            cmdList.add("vfr");

            cmdList.add("-y");

            cmdList.add(dstPath);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            return executeVideoEditor(command);
        } else {
            return VIDEO_EDITOR_EXECUTE_FAILED;
        }
    }

    public static Bitmap createVideoThumbnail(String path, int width, int height) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        if (TextUtils.isEmpty(path)) {
            return null;
        }

        File file = new File(path);
        if (!file.exists()) {
            return null;
        }

        try {
            retriever.setDataSource(path);
            bitmap = retriever.getFrameAtTime(-1); //取得指定时间的Bitmap，即可以实现抓图（缩略图）功能
        } catch (IllegalArgumentException ex) {
            // Assume this is a corrupt video file
        } catch (RuntimeException ex) {
            // Assume this is a corrupt video file.
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
                // Ignore failures while cleaning up.
            }
        }

        if (bitmap == null) {
            return null;
        }

        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        return bitmap;
    }

    public String executeGetOneFrame(String videoSrcPath,float postionS) {
        if (fileExist(videoSrcPath)) {

            List<String> cmdList = new ArrayList<String>();
//
            String dstPng=LanSongFileUtil.createFileInBox("png");
            cmdList.add("-i");
            cmdList.add(videoSrcPath);

            cmdList.add("-ss");
            cmdList.add(String.valueOf(postionS));

            cmdList.add("-vframes");
            cmdList.add("1");

            cmdList.add("-y");

            cmdList.add(dstPng);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret= executeVideoEditor(command);
            if(ret==0){
                return dstPng;
            }else{
                LanSongFileUtil.deleteFile(dstPng);
                return null;
            }
        } else {
            return null;
        }
    }

    public String executeGetOneFrame(String videoSrcPath, float postionS, int pngWidth, int pngHeight) {
        if (fileExist(videoSrcPath)) {

            List<String> cmdList = new ArrayList<String>();

            String dstPng=LanSongFileUtil.createFileInBox("png");

            String resolution = String.valueOf(pngWidth);
            resolution += "x";
            resolution += String.valueOf(pngHeight);

            cmdList.add("-i");
            cmdList.add(videoSrcPath);

            cmdList.add("-ss");
            cmdList.add(String.valueOf(postionS));

            cmdList.add("-s");
            cmdList.add(resolution);

            cmdList.add("-vframes");
            cmdList.add("1");

            cmdList.add("-y");

            cmdList.add(dstPng);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret= executeVideoEditor(command);
            if(ret==0){
                return dstPng;
            }else {
                LanSongFileUtil.deleteFile(dstPng);
                return null;
            }
        }
        return null;
    }

    @Deprecated
    public String executeConvertMp3ToAAC(String mp3Path) {
        if (fileExist(mp3Path)) {

            String dstFile=LanSongFileUtil.createFileInBox("m4a");

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(mp3Path);

            cmdList.add("-acodec");
            cmdList.add("libfaac");

            cmdList.add("-y");
            cmdList.add(dstFile);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret= executeVideoEditor(command);
            if(ret==0){
                return dstFile;
            }else{
                LanSongFileUtil.deleteFile(dstFile);
                return null;
            }
        } else {
            return null;
        }
    }
    public String executeConvertMp3ToAAC(String mp3Path,float startS,float durationS) {
        if (fileExist(mp3Path)) {

            List<String> cmdList = new ArrayList<String>();

            String  dstPath=LanSongFileUtil.createM4AFileInBox();
            cmdList.add("-i");
            cmdList.add(mp3Path);

            cmdList.add("-ss");
            cmdList.add(String.valueOf(startS));

            cmdList.add("-t");
            cmdList.add(String.valueOf(durationS));

            cmdList.add("-acodec");
            cmdList.add("libfaac");

            cmdList.add("-y");
            cmdList.add(dstPath);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret= executeVideoEditor(command);
            if(ret==0){
                return dstPath;
            }else{
                LanSongFileUtil.deleteFile(dstPath);
                return null;
            }
        }
        return null;
    }
    protected String executeConvertMp4toTs(String mp4Path) {
        if (fileExist(mp4Path)) {

            List<String> cmdList = new ArrayList<String>();

            String dstTs = LanSongFileUtil.createFileInBox("ts");
            cmdList.add("-i");
            cmdList.add(mp4Path);

            cmdList.add("-c");
            cmdList.add("copy");

            cmdList.add("-bsf:v");
            cmdList.add("h264_mp4toannexb");

            cmdList.add("-f");
            cmdList.add("mpegts");

            cmdList.add("-y");
            cmdList.add(dstTs);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret = executeVideoEditor(command);
            if (ret == 0) {
                return dstTs;
            } else {
                LanSongFileUtil.deleteFile(dstTs);
                return null;
            }
        }
        return null;
    }
    protected String executeConvertTsToMp4(String[] tsArray) {
        if (LanSongFileUtil.filesExist(tsArray)) {

            String dstFile=LanSongFileUtil.createMp4FileInBox();
            String concat = "concat:";
            for (int i = 0; i < tsArray.length - 1; i++) {
                concat += tsArray[i];
                concat += "|";
            }
            concat += tsArray[tsArray.length - 1];

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(concat);

            cmdList.add("-c");
            cmdList.add("copy");

            cmdList.add("-bsf:a");
            cmdList.add("aac_adtstoasc");

            cmdList.add("-y");

            cmdList.add(dstFile);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret= executeVideoEditor(command);
            if(ret==0){
                return dstFile;
            }else{
                LanSongFileUtil.deleteFile(dstFile);
                return null;
            }
        } else {
            return null;
        }
    }

    public String  executeConcatMP4(String[] mp4Array) {

        //第一步,先把所有的mp4转换为ts流
        ArrayList<String> tsPathArray = new ArrayList<String>();
        for (int i = 0; i < mp4Array.length; i++) {
            String segTs1 = executeConvertMp4toTs(mp4Array[i]);
            tsPathArray.add(segTs1);
        }

        //第二步: 把ts流拼接成mp4
        String[] tsPaths = new String[tsPathArray.size()];
        for (int i = 0; i < tsPathArray.size(); i++) {
            tsPaths[i] = (String) tsPathArray.get(i);
        }
        String dstVideo=executeConvertTsToMp4(tsPaths);


        //第三步:删除临时生成的ts文件.
        for (int i = 0; i < tsPathArray.size(); i++) {
            LanSongFileUtil.deleteFile(tsPathArray.get(i));
        }
        return dstVideo;
    }
    public String  executeConcatMP4(List<String> mp4Array) {

        //第一步,先把所有的mp4转换为ts流
        ArrayList<String> tsPathArray = new ArrayList<String>();
        for (int i = 0; i < mp4Array.size(); i++) {
            String segTs1 = executeConvertMp4toTs(mp4Array.get(i));
            tsPathArray.add(segTs1);
        }

        //第二步: 把ts流拼接成mp4
        String[] tsPaths = new String[tsPathArray.size()];
        for (int i = 0; i < tsPathArray.size(); i++) {
            tsPaths[i] = (String) tsPathArray.get(i);
        }
        String dstVideo=executeConvertTsToMp4(tsPaths);


        //第三步:删除临时生成的ts文件.
        for (int i = 0; i < tsPathArray.size(); i++) {
            LanSongFileUtil.deleteFile(tsPathArray.get(i));
        }
        return dstVideo;
    }

    public String executeConcatDiffentMp4(ArrayList<String> videos,boolean ignorecheck) {
        if(videos!=null && videos.size()>1){
            if(ignorecheck || checkVideoSizeSame(videos)){
                String dstPath=LanSongFileUtil.createMp4FileInBox();

                String filter = String.format(Locale.getDefault(), "concat=n=%d:v=1:a=1", videos.size());

                List<String> cmdList = new ArrayList<String>();
                cmdList.add("-vcodec");
                cmdList.add("lansoh264_dec");

                cmdList.add("-i");
                cmdList.add(videos.get(0));

                for (int i=1;i<videos.size();i++){
                    cmdList.add("-i");
                    cmdList.add(videos.get(i));
                }
                cmdList.add("-filter_complex");
                cmdList.add(filter);

                cmdList.add("-acodec");
                cmdList.add("libfaac");
                cmdList.add("-b:a");
                cmdList.add("128000");


                return executeAutoSwitch(cmdList);
            }
        }
        return null;
    }

    private boolean checkVideoSizeSame(ArrayList<String> videos){
        int w=0;
        int h=0;
        for (String item: videos){
            MediaInfo info=new MediaInfo(item);
            if(info.prepare()){

                if(w ==0&& h==0){
                    w=info.getWidth();
                    h=info.getHeight();
                }else if(info.getWidth()!=w || info.getHeight() !=h){
                    LSOLog.e( "视频拼接中, 有个视频的分辨率不等于其他分辨率");
                    return false;
                }
            }else{
                return false;
            }
        }
        return true;  //返回正常;
    }


    @Deprecated
    public String executeCropVideoFrame(String videoFile, int cropWidth, int cropHeight, int x, int y) {
        if (fileExist(videoFile)) {
            if(LanSoEditor.isQiLinSoc()){
                cropWidth=make16Before(cropWidth);
                cropHeight=make16Before(cropHeight);
            }
            String filter = String.format(Locale.getDefault(), "crop=%d:%d:%d:%d", cropWidth, cropHeight, x, y);
            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(videoFile);

            cmdList.add("-vf");
            cmdList.add(filter);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        }
        return null;
    }


    @Deprecated
    public String executeScaleVideoFrame(String videoFile, int scaleWidth, int scaleHeight) {
        if (fileExist(videoFile)) {

            List<String> cmdList = new ArrayList<String>();
            scaleWidth=(scaleWidth/2)*2;
            scaleHeight=(scaleHeight/2)*2;

            String scalecmd = String.format(Locale.getDefault(), "scale=%d:%d", scaleWidth, scaleHeight);

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(videoFile);

            cmdList.add("-vf");
            cmdList.add(scalecmd);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        }
        return null;
    }

    @Deprecated
    public String executeScaleOverlay(String videoFile, String pngPath, int scaleWidth, int scaleHeight, int overX,
                                      int overY) {
        if (fileExist(videoFile)) {

            List<String> cmdList = new ArrayList<String>();
            String filter = String.format(Locale.getDefault(), "[0:v]scale=%d:%d [scale];[scale][1:v] overlay=%d:%d",
                    scaleWidth, scaleHeight, overX, overY);

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(videoFile);

            cmdList.add("-i");
            cmdList.add(pngPath);

            cmdList.add("-filter_complex");
            cmdList.add(filter);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    @Deprecated
    public String  executeOverLayVideoFrame(String videoFile, String picturePath, int overX, int overY)
    {
        String filter = String.format(Locale.getDefault(), "overlay=%d:%d", overX, overY);

        List<String> cmdList = new ArrayList<String>();

        cmdList.add("-vcodec");
        cmdList.add("lansoh264_dec");

        cmdList.add("-i");
        cmdList.add(videoFile);

        cmdList.add("-i");
        cmdList.add(picturePath);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-acodec");
        cmdList.add("copy");
        return executeAutoSwitch(cmdList);
    }

    @Deprecated
    public String  executeCropOverlay(String videoFile, String pngPath, int cropX, int cropY, int
            cropWidth, int cropHeight, int overX, int overY)
    {
        if (fileExist(videoFile)) {
            if(LanSoEditor.isQiLinSoc()){
                cropWidth=make16Before(cropWidth);
                cropHeight=make16Before(cropHeight);
            }
            String filter = String.format(Locale.getDefault(), "[0:v]crop=%d:%d:%d:%d [crop];[crop][1:v] " +
                    "overlay=%d:%d", cropWidth, cropHeight, cropX, cropY, overX, overY);

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(videoFile);

            cmdList.add("-i");
            cmdList.add(pngPath);

            cmdList.add("-filter_complex");
            cmdList.add(filter);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    @Deprecated
    public String executeCutCrop(String videoFile, float startTimeS, float
            duationS, int cropX, int cropY, int cropWidth, int cropHeight) {

        if(LanSoEditor.isQiLinSoc()){
            cropWidth=make16Before(cropWidth);
            cropHeight=make16Before(cropHeight);
        }
        String filter = String.format(Locale.getDefault(), "crop=%d:%d:%d:%d", cropWidth, cropHeight, cropX, cropY);

        List<String> cmdList = new ArrayList<String>();
        cmdList.add("-vcodec");
        cmdList.add("lansoh264_dec");

        cmdList.add("-ss");
        cmdList.add(String.valueOf(startTimeS));

        cmdList.add("-t");
        cmdList.add(String.valueOf(duationS));

        cmdList.add("-i");
        cmdList.add(videoFile);


        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-acodec");
        cmdList.add("copy");

        return executeAutoSwitch(cmdList);
    }

    public String executeCutCropOverlay(String videoFile, String pngPath, float startTimeS, float
            duationS, int cropX, int cropY, int cropWidth, int cropHeight, int overX, int overY) {

        if(LanSoEditor.isQiLinSoc()){
            cropWidth=make16Before(cropWidth);
            cropHeight=make16Before(cropHeight);
        }
        String filter = String.format(Locale.getDefault(), "[0:v]crop=%d:%d:%d:%d [crop];[crop][1:v] " +
                "overlay=%d:%d", cropWidth, cropHeight, cropX, cropY, overX, overY);

        List<String> cmdList = new ArrayList<String>();
        cmdList.add("-vcodec");
        cmdList.add("lansoh264_dec");

        cmdList.add("-ss");
        cmdList.add(String.valueOf(startTimeS));

        cmdList.add("-t");
        cmdList.add(String.valueOf(duationS));

        cmdList.add("-i");
        cmdList.add(videoFile);

        cmdList.add("-i");
        cmdList.add(pngPath);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-acodec");
        cmdList.add("copy");

        return executeAutoSwitch(cmdList);
    }


    public String executeConvertPictureToVideo(String picDir, String jpgprefix, float framerate) {

        String picSet = picDir + jpgprefix + "_%3d.jpeg";

        List<String> cmdList = new ArrayList<String>();

        cmdList.add("-framerate");
        cmdList.add(String.valueOf(framerate));

        cmdList.add("-i");
        cmdList.add(picSet);

        cmdList.add("-r");
        cmdList.add("25");

        return executeAutoSwitch(cmdList);
    }

    public String executePadVideo(String videoFile, int padWidth, int padHeight, int padX, int padY) {
        if (fileExist(videoFile)) {
            MediaInfo info = new MediaInfo(videoFile);
            if (info.prepare()) {
                int minWidth = info.vWidth + padX;
                int minHeight = info.vHeight + padY;
                if (minWidth > padWidth || minHeight > padHeight) {
                    LSOLog.e( "pad set position is error. min Width>pading width.or min height > padding height");
                    return null;  //失败.
                }
            } else {
                LSOLog.e("media info prepare is error!!!");
                return null;
            }

            //第二步: 开始padding.
            String filter = String.format(Locale.getDefault(), "pad=%d:%d:%d:%d:black", padWidth, padHeight, padX,padY);

            List<String> cmdList = new ArrayList<String>();
            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(videoFile);

            cmdList.add("-vf");
            cmdList.add(filter);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    public String executeRotateAngle(String srcPath, float angle) {
        if (fileExist(srcPath)) {

            String filter = String.format(Locale.getDefault(), "rotate=%f*(PI/180),format=yuv420p", angle);
            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-vf");
            cmdList.add(filter);

            cmdList.add("-metadata:s:v");
            cmdList.add("rotate=0");

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    public String executeSetVideoMetaAngle(String srcPath, int angle) {
        if (fileExist(srcPath)) {

            List<String> cmdList = new ArrayList<String>();

            String dstPath=LanSongFileUtil.createMp4FileInBox();
            String filter = String.format(Locale.getDefault(), "rotate=%d", angle);


            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-c");
            cmdList.add("copy");

            cmdList.add("-metadata:s:v:0");
            cmdList.add(filter);

            cmdList.add("-y");
            cmdList.add(dstPath);

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret= executeVideoEditor(command);
            if(ret==0){
                return dstPath;
            }else {
                LanSongFileUtil.deleteFile(dstPath);
                return null;
            }
        } else {
            return null;
        }
    }

    public String executeOverLaySpeed(String srcPath, String pngPath,int overX,int overY, float speed){

        if (fileExist(srcPath)) {

            String filter = String.format(Locale.getDefault(),
                    "[0:v][1:v]overlay=%d:%d[overlay];[overlay]setpts=%f*PTS[v];[0:a]atempo=%f[a]",overX,overY, 1 / speed,
                    speed);

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-i");
            cmdList.add(pngPath);

            cmdList.add("-filter_complex");
            cmdList.add(filter);

            cmdList.add("-map");
            cmdList.add("[v]");
            cmdList.add("-map");
            cmdList.add("[a]");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    public String executeAdjustVideoSpeed(String srcPath, float speed){
        MediaInfo mediaInfo=new MediaInfo(srcPath);
        if (mediaInfo.prepare() && mediaInfo.isHaveVideo()) {
            String filter = String.format(Locale.getDefault(), "[0:v]setpts=%f*PTS[v];[0:a]atempo=%f[a]", 1 / speed,
                    speed);
            //如果没有声音.
            if(!mediaInfo.isHaveAudio()){
                filter = String.format(Locale.getDefault(), "[0:v]setpts=%f*PTS[v]", 1 / speed);
            }

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-filter_complex");
            cmdList.add(filter);

            cmdList.add("-map");
            cmdList.add("[v]");
            if(mediaInfo.isHaveAudio()) {
                cmdList.add("-map");
                cmdList.add("[a]");
            }
            durationMs=( (int)(mediaInfo.vDuration*1000/speed));
            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }


    @Deprecated
    public String executeAdjustVideoSpeed2(String srcPath, float speed, int bitrate) {
        if (fileExist(srcPath)) {

            String filter = String.format(Locale.getDefault(), "[0:v]setpts=%f*PTS[v]", 1 / speed);

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-filter_complex");
            cmdList.add(filter);

            cmdList.add("-map");
            cmdList.add("[v]");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }


    public String executeVideoMirrorH(String srcPath) {
        if (fileExist(srcPath)) {

            String filter = String.format(Locale.getDefault(), "crop=iw/2:ih:0:0,split[left][tmp];[tmp]hflip[right];" +
                    "[left][right] hstack");

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-vf");
            cmdList.add(filter);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }


    public String executeVideoMirrorV(String srcPath) {
        if (fileExist(srcPath)) {

            String filter = String.format(Locale.getDefault(), "crop=iw:ih/2:0:0,split[top][tmp];[tmp]vflip[bottom];" +
                    "[top][bottom] vstack");

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-vf");
            cmdList.add(filter);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }


    public String executeVideoRotateVertically(String srcPath) {
        if (fileExist(srcPath)) {

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-vf");
            cmdList.add("vflip");

            cmdList.add("-c:a");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }


    public String executeVideoRotateHorizontally(String srcPath) {
        if (fileExist(srcPath)) {

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-vf");
            cmdList.add("hflip");

            cmdList.add("-c:a");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    public String  executeVideoRotate90Clockwise(String srcPath) {
        if (fileExist(srcPath)) {

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-vf");
            cmdList.add("transpose=1");

            cmdList.add("-c:a");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    public String executeVideoRotate90CounterClockwise(String srcPath) {
        if (fileExist(srcPath)) {

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-vf");
            cmdList.add("transpose=2");

            cmdList.add("-c:a");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    public String executeVideoReverse(String srcPath) {
        if (fileExist(srcPath)) {

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-vf");
            cmdList.add("reverse");

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);

        } else {
            return null;
        }
    }

    public String executeAVReverse(String srcPath) {
        if (fileExist(srcPath)) {
            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-vf");
            cmdList.add("reverse");

            cmdList.add("-af");
            cmdList.add("areverse");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    public String executeDeleteLogo(String video,int startX,int startY,int w,int h){
        if (fileExist(video)) {

            String filter = String.format(Locale.getDefault(), "delogo=x=%d:y=%d:w=%d:h=%d",startX,
                    startY,w,h);

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(video);

            cmdList.add("-vf");
            cmdList.add(filter);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    public String executeDeleteLogoInTimeRange(String video,int startX,int startY,int w,int h, float startS,float endS){
        MediaInfo info=new MediaInfo(video);
        if(info.prepare()){

            int dstW=info.getWidth() >(startX + w)? w: (info.getWidth() - startX);
            int dstH=info.getHeight() > (startY  + h) ? h:(info.getHeight() - startY);

            String filter = String.format(Locale.getDefault(), "delogo=x=%d:y=%d:w=%d:h=%d:enable='between(t,%f,%f)'",startX,startY,dstW,dstH,startS,endS);

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(video);

            cmdList.add("-vf");
            cmdList.add(filter);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }


    public String executeDeleteLogo(String video,int startX1,int startY1,int width1,int height1,
                                    int startX2,int startY2,int width2,int height2,
                                    int startX3,int startY3,int width3,int height3,
                                    int startX4,int startY4,int width4,int height4){
        if (fileExist(video)) {

            String filter = String.format(Locale.getDefault(), "delogo=x=%d:y=%d:w=%d:h=%d [d1]",
                    startX1,startY1,width1,height1);

            if(startX2>=0 && startY2>=0 && width2>0 && height2>0){
                String filter2 = String.format(Locale.getDefault(), ";[d1]delogo=x=%d:y=%d:w=%d:h=%d [d2]",
                        startX2,startY2,width2,height2);
                filter+=filter2;
            }

            if(startX3>=0 && startY3>=0 && width3>0 && height3>0){
                String filter3 = String.format(Locale.getDefault(), ";[d2]delogo=x=%d:y=%d:w=%d:h=%d [d3]",
                        startX3,startY3,width3,height3);
                filter+=filter3;
            }

            if(startX4>=0 && startY4>=0 && width4>0 && height4>0){
                String filter4 = String.format(Locale.getDefault(), ";[d3]delogo=x=%d:y=%d:w=%d:h=%d",
                        startX4,startY4,width4,height4);
                filter+=filter4;
            }
            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(video);

            cmdList.add("-vf");
            cmdList.add(filter);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    public String executeDeleteLogoInTimeRange(
            String video,
            int startX1,int startY1,int width1,int height1, float startS1,float endS1,
            int startX2,int startY2,int width2,int height2, float startS2,float endS2,
            int startX3,int startY3,int width3,int height3, float startS3,float endS3,
            int startX4,int startY4,int width4,int height4, float startS4,float endS4){
        if (fileExist(video)) {

            String filter = String.format(Locale.getDefault(), "delogo=x=%d:y=%d:w=%d:h=%d:enable='between(t,%f,%f)' [d1]",
                    startX1,startY1,width1,height1,startS1,endS1);

            if(startX2>=0 && startY2>=0 && width2>0 && height2>0 && startS2>0 && endS2>startS2){
                String filter2 = String.format(Locale.getDefault(), ";[d1]delogo=x=%d:y=%d:w=%d:h=%d:enable='between(t,%f,%f)' [d2]",
                        startX2,startY2,width2,height2,startS2,endS2);
                filter+=filter2;
            }

            if(startX3>=0 && startY3>=0 && width3>0 && height3>0&& startS3>0 && endS3>startS3){
                String filter3 = String.format(Locale.getDefault(), ";[d2]delogo=x=%d:y=%d:w=%d:h=%d:enable='between(t,%f,%f)' [d3]",
                        startX3,startY3,width3,height3,startS3,endS3);
                filter+=filter3;
            }

            if(startX4>=0 && startY4>=0 && width4>0 && height4>0 && startS4>0 && endS4>startS4){
                String filter4 = String.format(Locale.getDefault(), ";[d3]delogo=x=%d:y=%d:w=%d:h=%d:enable='between(t,%f,%f)'",
                        startX4,startY4,width4,height4,startS4,endS4);
                filter+=filter4;
            }

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(video);

            cmdList.add("-vf");
            cmdList.add(filter);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }


    public String executeAdjustFrameRate(String video,float framerate,int bitrate){
        if (fileExist(video)) {

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(video);

            cmdList.add("-r");
            cmdList.add(String.valueOf(framerate));

            cmdList.add("-acodec");
            cmdList.add("copy");

            encodeBitRate=bitrate;

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    public String executeCutCropAdjustFps(String videoFile, float startTimeS, float
            duationS, int cropX, int cropY, int cropWidth, int cropHeight,float framerate) {

        if(LanSoEditor.isQiLinSoc()){
            cropWidth=make16Before(cropWidth);
            cropHeight=make16Before(cropHeight);
        }
        String filter = String.format(Locale.getDefault(), "crop=%d:%d:%d:%d", cropWidth, cropHeight, cropX, cropY);

        List<String> cmdList = new ArrayList<String>();
        cmdList.add("-vcodec");
        cmdList.add("lansoh264_dec");

        cmdList.add("-ss");
        cmdList.add(String.valueOf(startTimeS));

        cmdList.add("-t");
        cmdList.add(String.valueOf(duationS));

        cmdList.add("-i");
        cmdList.add(videoFile);

        cmdList.add("-r");
        cmdList.add(String.valueOf(framerate));

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-acodec");
        cmdList.add("copy");

        return executeAutoSwitch(cmdList);
    }

    private boolean executeExtractFrame(String input,float interval, int scaleW, int scaleH,String dstBmp)
    {
        if (fileExist(input)) {

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(input);

            cmdList.add("-vsync");
            cmdList.add("1");

            cmdList.add("-qscale:v");
            cmdList.add("2");

            cmdList.add("-r");
            cmdList.add(String.valueOf(interval));

            if(scaleW>0 && scaleH>0){
                cmdList.add("-s");
                cmdList.add(String.format(Locale.getDefault(),"%dx%d",scaleW,scaleH));
            }

            cmdList.add("-f");
            cmdList.add("image2");

            cmdList.add("-y");

            cmdList.add(dstBmp);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int  ret= executeVideoEditor(command);
            if(ret==0){
                return true;
            }else{
                return false;
            }
        } else {
            return false;
        }
    }
    private String executeConvertBmpToGif(String bmpPaths,float framerate)
    {
        String gifPath=LanSongFileUtil.createGIFFileInBox();


        List<String> cmdList = new ArrayList<String>();

        cmdList.add("-f");
        cmdList.add("image2");

        cmdList.add("-framerate");
        cmdList.add(String.valueOf(framerate));

        cmdList.add("-i");
        cmdList.add(bmpPaths);

        cmdList.add("-y");
        cmdList.add(gifPath);
        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        int  ret= executeVideoEditor(command);
        if(ret==0){
            return gifPath;
        }else{
            LanSongFileUtil.deleteFile(gifPath);
            return null;
        }
    }


    @Deprecated
    public String executeConvertToGif(String videoInput, float inteval,int scaleW,int scaleH,float frameRate)
    {
//        ffmpeg -i d1.mp4 -r 1 -f image2 foo-%03d.jpeg
//        ffmpeg -f image2 -framerate 5 -i foo-%03d.jpeg c.gif

        String  subfix="jpeg";
        LanSongFileUtil.deleteNameFiles("lansonggif",subfix);

        String bmpPaths=LanSongFileUtil.getCreateFileDir();
        bmpPaths+="/lansonggif_%05d."+subfix;

        if(executeExtractFrame(videoInput,inteval,scaleW,scaleH,bmpPaths)){

            String  ret=executeConvertBmpToGif(bmpPaths,frameRate);

            LanSongFileUtil.deleteNameFiles("lansonggif",subfix);
            return ret;
        }
        return null;
    }

    public String executeConvertVideoToGif(String videoPath,int interval,int scaleWidth,int scaleHeight,float speed) {
        List<String> cmdList = new ArrayList<String>();


        String dstPath=LanSongFileUtil.createGIFFileInBox();

        int width=scaleWidth/2;
        width*=2;

        int height=scaleHeight/2;
        height*=2;

        String filter = String.format(Locale.getDefault(), "setpts=%f*PTS,scale=%dx%d", speed,width, height);

        cmdList.add("-i");
        cmdList.add(videoPath);
        cmdList.add("-an");


        cmdList.add("-r");
        cmdList.add(String.valueOf(interval));

        cmdList.add("-vf");
        cmdList.add(filter);

        cmdList.add("-y");
        cmdList.add(dstPath);

        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        int ret= executeVideoEditor(command);
        if(ret==0){
            return dstPath;
        }else{
            LanSongFileUtil.deleteFile(dstPath);
            return null;
        }
    }

    public String executeAddPitureAtTime(String srcPath,String picPath,float startTimeS,float endTimeS)
    {
        if (fileExist(srcPath) && fileExist(picPath)) {

            List<String> cmdList = new ArrayList<String>();

            String filter = String.format(Locale.getDefault(), "[0:v][1:v] overlay=0:0:enable='between(t,%f,%f)'",
                    startTimeS, endTimeS);

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-i");
            cmdList.add(picPath);


            cmdList.add("-filter_complex");
            cmdList.add(filter);

            cmdList.add("-acodec");
            cmdList.add("copy");
            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    public String executeAddPitureAtXYTime(String srcPath,String picPath,int x,int y,float startTimeS,float endTimeS)
    {
        if (fileExist(srcPath) && fileExist(picPath)) {

            List<String> cmdList = new ArrayList<String>();

            String filter = String.format(Locale.getDefault(), "[0:v][1:v] overlay=%d:%d:enable='between(t,%f,%f)'",
                    x,y,startTimeS, endTimeS);

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-i");
            cmdList.add(picPath);


            cmdList.add("-filter_complex");
            cmdList.add(String.valueOf(filter));

            cmdList.add("-acodec");
            cmdList.add("copy");
            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }


    public String  executeGifOverLayPicture(String gifFile, String picturePath, int overX, int overY)
    {
        String filter = String.format(Locale.getDefault(), "overlay=%d:%d", overX, overY);

        List<String> cmdList = new ArrayList<String>();


        String gifPath=LanSongFileUtil.createGIFFileInBox();

        cmdList.add("-i");
        cmdList.add(gifFile);

        cmdList.add("-i");
        cmdList.add(picturePath);

        cmdList.add("-filter_complex");
        cmdList.add(filter);


        cmdList.add("-y");
        cmdList.add(gifPath);

        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        int  ret= executeVideoEditor(command);
        if(ret==0){
            return gifPath;
        }else{
            LanSongFileUtil.deleteFile(gifPath);
            return null;
        }
    }
    public String executeAddTextToMp4(String srcPath,String text)
    {
        // ffmpeg -i d1.mp4 -metadata description="LanSon\"g \"Text"
        //  -acodec copy -vcodec copy t1.mp4
        if(fileExist(srcPath) && text!=null) {
            String retPath = LanSongFileUtil.createMp4FileInBox();

            List<String> cmdList = new ArrayList<String>();


            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-metadata");
            cmdList.add("description="+text);

            cmdList.add("-acodec");
            cmdList.add("copy");
            cmdList.add("-vcodec");
            cmdList.add("copy");

            cmdList.add("-y");
            cmdList.add(retPath);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret = executeVideoEditor(command);
            if (ret == 0) {
                return retPath;
            } else {
                LanSongFileUtil.deleteFile(retPath);
                return null;
            }
        }else{
            LSOLog.e("file is not exist. file:"+srcPath);
            return null;
        }
    }


    public String executeGetTextFromMp4(String srcPath)
    {

        if(srcPath!=null && LanSongFileUtil.fileExist(srcPath)){
            return nativeGetVideoDescription(srcPath);  //如果没有,则返回""空
        }else {
            LSOLog.e("executeGetTextFromMp4 error.file is:"+srcPath);
            return null;
        }

    }

    public static int getSuggestBitRate(int wxh) {
        if (wxh <= 480 * 480) {
            return 1000 * 1024;
        } else if (wxh <= 640 * 480) {
            return 1500 * 1024;
        } else if (wxh <= 800 * 480) {
            return 1800 * 1024;
        } else if (wxh <= 960 * 544) {
            return 2000 * 1024;
        } else if (wxh <= 1280 * 720) {
            return 2500 * 1024;
        } else if (wxh <= 1920 * 1088) {
            return 3000 * 1024;
        } else {
            return 3500 * 1024;
        }
    }
    public static int checkSuggestBitRate(int wxh, int bitrate) {
        int sugg = getSuggestBitRate(wxh);
        return bitrate < sugg ? sugg : bitrate;   //如果设置过来的码率小于建议码率,则返回建议码率,不然返回设置码率
    }

    private MediaInfo _inputInfo=null;

    public String executeAutoSwitch(List<String> cmdList)
    {
        int ret=0;
        int bitrate=0;
        boolean useSoftWareEncoder=false;
        if(encodeBitRate>0){
            bitrate=encodeBitRate;
        }

        if(durationMs>0){
            setDurationMs(durationMs);
        }

        String dstPath=LanSongFileUtil.createMp4FileInBox();
        if(isForceSoftWareDecoder || checkSoftDecoder()){
            for(int i=0;i<cmdList.size();i++){
                String cmd=cmdList.get(i);
                if("lansoh264_dec".equals(cmd)){
                    if(i>0){
                        cmdList.remove(i-1);
                        cmdList.remove(i-1);
                    }
                    break;
                }
            }
        }
        if(isForceHWEncoder){
            ret=executeWithEncoder(cmdList, bitrate, dstPath, true);
        }else if(isForceSoftWareEncoder || useSoftWareEncoder || checkSoftEncoder()) {
            ret = executeWithEncoder(cmdList, bitrate, dstPath, false);
        }else{

            ret=executeWithEncoder(cmdList, bitrate, dstPath, true);

            if(ret!=0){
                ret=executeWithEncoder(cmdList, bitrate, dstPath, false);
            }
        }

        if(ret!=0) {
            for(int i=0;i<cmdList.size();i++){
                String cmd=cmdList.get(i);
                if("lansoh264_dec".equals(cmd)){
                    if(i>0){
                        cmdList.remove(i-1);
                        cmdList.remove(i-1);
                    }
                    break;
                }
            }
            ret=executeWithEncoder(cmdList, bitrate, dstPath, false);
        }

        if(ret!=0){
            if(lanSongLogCollector !=null){
                lanSongLogCollector.start();
            }
            ret=executeWithEncoder(cmdList, bitrate, dstPath, false);
            if(lanSongLogCollector !=null && lanSongLogCollector.isRunning()){
                lanSongLogCollector.stop();
            }
            LanSongFileUtil.deleteFile(dstPath);

            durationMs=0;
            setDurationMs(0);
            return null;
        }else{
            return dstPath;
        }
    }

    public int executeWithEncoder(List<String> cmdList,int bitrate, String dstPath, boolean isHWEnc)
    {
        List<String> cmdList2 = new ArrayList<String>();
        for(String item: cmdList){
            cmdList2.add(item);
        }
        cmdList2.add("-vcodec");

        if(isHWEnc){
            cmdList2.add("lansoh264_enc");
            cmdList2.add("-pix_fmt");


            if(LanSoEditor.isQiLinSoc()){
                cmdList2.add("nv21");
                setForceColorFormat(21);
            }else{
                cmdList2.add("yuv420p");
            }
            cmdList2.add("-b:v");
            cmdList2.add(String.valueOf(bitrate));
        }else{
            cmdList2.add("libx264");

            cmdList2.add("-bf");
            cmdList2.add("0");

            cmdList2.add("-pix_fmt");
            cmdList2.add("yuv420p");

            cmdList2.add("-g");
            cmdList2.add("30");

            if(bitrate==0){
                if(_inputInfo!=null){
                    bitrate=getSuggestBitRate(_inputInfo.vWidth * _inputInfo.vHeight);
                }else{
                    bitrate=(int)(2.5f*1024*1024);
                }
            }

            cmdList2.add("-b:v");
            cmdList2.add(String.valueOf(bitrate));
        }


        cmdList2.add("-y");
        cmdList2.add(dstPath);
        String[] command = new String[cmdList2.size()];
        for (int i = 0; i < cmdList2.size(); i++) {
            command[i] = (String) cmdList2.get(i);
        }
        int ret=executeVideoEditor(command);
        return ret;
    }

    public boolean checkSoftEncoder()
    {
        if(LanSoEditor.isQiLinSoc() && !isSupportNV21ColorFormat()){
            isForceSoftWareEncoder=true;
            return true;
        }
        if(Build.MODEL!=null && !isSupportNV21ColorFormat()) {
            if (Build.MODEL.contains("-AL00") || Build.MODEL.contains("-CL00")) {
                isForceSoftWareEncoder = true;
                return true;
            }
        }
        return false;
    }

    public boolean checkSoftDecoder()
    {
        for(String item: useSoftDecoderlist){
            if(item.equalsIgnoreCase(Build.MODEL)){
                return true;
            }else if(item.contains(Build.MODEL)){
                return true;
            }
        }
        return false;
    }


    public static int make16Closest(int value) {

        if (value < 16) {
            return value;
        } else {
            value += 8;
            int val2 = value / 16;
            val2 *= 16;
            return val2;
        }
    }

    public static int make16Next(int value) {
        if(value%16==0){
            return value;
        }else{
            return ((int)(value/16.0f +1)*16) ;
        }
    }
    public static int make16Before(int value) {
        if(value%16==0){
            return value;
        }else{
            return ((int)(value/16.0f)*16) ;
        }
    }


    private static int checkCPUName() {
        String str1 = "/proc/cpuinfo";
        String str2 = "";
        try {
            FileReader fr = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(fr, 8192);
            str2 = localBufferedReader.readLine();
            while (str2 != null) {
                str2 = localBufferedReader.readLine();
                if(str2.contains("SDM845")){  //845的平台;

                }
            }
            localBufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }




    private static final String MIME_TYPE_AVC = "video/avc"; // H.264 Advanced

    private static boolean isSupportNV21=false;

    public static boolean isSupportNV21ColorFormat()
    {
        if(isSupportNV21){
            return true;
        }

        MediaCodecInfo codecInfo = selectCodec(MIME_TYPE_AVC);
        if (codecInfo == null) {
            return false;
        }
        isSupportNV21=selectColorFormat(codecInfo, MIME_TYPE_AVC);
        return isSupportNV21;
    }

    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }
    private static boolean selectColorFormat(MediaCodecInfo codecInfo,
                                             String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo
                .getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];

            if(colorFormat==21){
                return true;
            }
        }
        return false;
    }


    public String executeCutScaleVideoExact(String videoFile,
                                            float startS,
                                            float durationS,
                                            int width,
                                            int height,
                                            float framerate,
                                            int bitrate) {
        if (fileExist(videoFile)) {
            List<String> cmdList = new ArrayList<String>();

            String filter = String.format(Locale.getDefault(), "scale=%d:%d", width, height);

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(videoFile);

            cmdList.add("-ss");
            cmdList.add(String.valueOf(startS));

            cmdList.add("-t");
            cmdList.add(String.valueOf(durationS));

            //---测试调整码率、帧率+裁剪
            cmdList.add("-r");
            cmdList.add(String.valueOf(framerate));
            encodeBitRate = bitrate;
            //---测试调整码率、帧率+裁剪

            cmdList.add("-vf");
            cmdList.add(filter);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }


    public String executeAlphaMaskVideo(String bgImgPath,
                                        String videoPath,
                                        String maskImgPath) {
        MediaInfo info=new MediaInfo(videoPath);
        if (info.prepare()) {
            List<String> cmdList = new ArrayList<String>();

            //  ffmpeg -y -i TEST_720P_120s.mp4 -loop 1 -i a12801.png -i out.jpg -filter_complex
            // "[1:v]alphaextract[alf];[0:v][alf]alphamerge[al2]; [2:v][al2] overlay=0:0" -c:v libx264 output21.mp4

            String filter = "[1:v]alphaextract[alf];[0:v][alf]alphamerge[al2]; [2:v][al2] overlay=0:0";


            cmdList.add("-i");
            cmdList.add(videoPath);

            cmdList.add("-loop");
            cmdList.add("1");

            cmdList.add("-i");
            cmdList.add(maskImgPath);

            cmdList.add("-i");
            cmdList.add(bgImgPath);

            cmdList.add("-filter_complex");
            cmdList.add(filter);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }


    public String executePictureDeleteLogo(String picturePath,int startX,int startY,int w,int h){
        if (fileExist(picturePath)) {


            String dstPath=LanSongFileUtil.createFileInBox("png");

            if(startX==0) startX=1;
            if(startY==0) startY=1;


            String filter = String.format(Locale.getDefault(), "delogo=x=%d:y=%d:w=%d:h=%d",startX,
                    startY,w,h);

            List<String> cmdList = new ArrayList<String>();



            cmdList.add("-i");
            cmdList.add(picturePath);

            cmdList.add("-vf");
            cmdList.add(filter);


            cmdList.add("-y");
            cmdList.add(dstPath);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret=executeVideoEditor(command);

            if(ret==0){
                return dstPath;
            }else{
                LanSongFileUtil.deleteFile(dstPath);
                return null;
            }
        } else {
            return null;
        }
    }


    public String executeAacToM4a(String picturePath){
        if (fileExist(picturePath)) {


            String dstPath=LanSongFileUtil.createFileInBox("m4a");

            List<String> cmdList = new ArrayList<String>();



            cmdList.add("-i");
            cmdList.add(picturePath);

            cmdList.add("-acodec");
            cmdList.add("copy");


            cmdList.add("-y");
            cmdList.add(dstPath);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret=executeVideoEditor(command);

            if(ret==0){
                return dstPath;
            }else{
                LanSongFileUtil.deleteFile(dstPath);
                return null;
            }
        } else {
            return null;
        }
    }

    public String executeAacToM4a22(String picturePath){
        if (fileExist(picturePath)) {


            String dstPath=LanSongFileUtil.createFileInBox("m4a");

            List<String> cmdList = new ArrayList<String>();



            cmdList.add("-i");
            cmdList.add(picturePath);


            cmdList.add("-ss");
            cmdList.add("0.0");


            cmdList.add("-t");
            cmdList.add("20.0");

            cmdList.add("-acodec");
            cmdList.add("copy");


            cmdList.add("-y");
            cmdList.add(dstPath);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret=executeVideoEditor(command);

            if(ret==0){
                return dstPath;
            }else{
                LanSongFileUtil.deleteFile(dstPath);
                return null;
            }
        } else {
            return null;
        }
    }
}
