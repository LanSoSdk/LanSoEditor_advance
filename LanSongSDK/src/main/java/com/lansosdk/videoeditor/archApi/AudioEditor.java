package com.lansosdk.videoeditor.archApi;

import com.lansosdk.box.LSOLog;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.VideoEditor;
import com.lansosdk.videoeditor.oldVersion.onVideoEditorProgressListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class AudioEditor {
    private VideoEditor editor;
    public AudioEditor()
    {
        editor=new VideoEditor();
        editor.setOnProgressListener(new onVideoEditorProgressListener() {
            @Override
            public void onProgress(VideoEditor v, int percent) {
                if(monAudioEditorProgressListener!=null){
                    monAudioEditorProgressListener.onProgress(AudioEditor.this,percent);
                }
            }
        });
    }

    public void cancel(){
        if(editor!=null){
            editor.cancel();
        }
    }

    public String executeAudioReverse(String srcPath) {
        if (fileExist(srcPath)) {

            List<String> cmdList = new ArrayList<String>();

            String  retPath= LanSongFileUtil.createMP3FileInBox();
            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-af");
            cmdList.add("areverse");

            cmdList.add("-vn");

            cmdList.add("-acodec");
            cmdList.add("libmp3lame");

            cmdList.add("-b:a");
            cmdList.add("128000");

            cmdList.add("-ac");
            cmdList.add("2");


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
                return null;
            }
        } else {
            return null;
        }
    }


    public static String mergeAudioNoCheck(String audio, String video, boolean deleteVideo) {
        MediaInfo info=new MediaInfo(audio);
        if(info.prepare() && info.isHaveAudio()){
            String retPath=LanSongFileUtil.createMp4FileInBox();

            String inputAudio = audio;
            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(inputAudio);
            cmdList.add("-i");
            cmdList.add(video);

            cmdList.add("-map");
            cmdList.add("0:a");
            cmdList.add("-map");
            cmdList.add("1:v");

            cmdList.add("-acodec");
            cmdList.add("copy");
            cmdList.add("-vcodec");
            cmdList.add("copy");

            cmdList.add("-absf");
            cmdList.add("aac_adtstoasc");

            cmdList.add("-y");
            cmdList.add(retPath);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            VideoEditor editor = new VideoEditor();
            int ret = editor.executeVideoEditor(command);
            if(ret==0){
                if(deleteVideo){
                    LanSongFileUtil.deleteFile(video);
                }
                return retPath;
            }else{
                return video;
            }
        }
        return video;
    }

    private onAudioEditorProgressListener monAudioEditorProgressListener=null;
    public interface  onAudioEditorProgressListener{
        void onProgress(AudioEditor v, int percent);
    }

    public void setOnAudioEditorProgressListener(onAudioEditorProgressListener listener){
        monAudioEditorProgressListener=listener;
    }
    public String executePcmConvertToWav(String srcPcm, int srcSample,
                                              int srcChannel, int dstSample) {

        //            ffmpeg -f s16le -ac 2 -ar 48000 -i huo_48000_2.pcm -ac 2 -ar 44100 huo.wav
        if(LanSongFileUtil.fileExist(srcPcm) && dstSample>0){
            String dstPath=LanSongFileUtil.createWAVFileInBox();


            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-f");
            cmdList.add("s16le");
            cmdList.add("-ac");
            cmdList.add(String.valueOf(srcChannel));
            cmdList.add("-ar");
            cmdList.add(String.valueOf(srcSample));
            cmdList.add("-i");
            cmdList.add(srcPcm);

            cmdList.add("-ac");
            cmdList.add("2");  //目标通道数默认是双通道;
            cmdList.add("-ar");
            cmdList.add(String.valueOf(dstSample));

            //wav格式的数据, 不压缩, 没有码率
            cmdList.add("-y");
            cmdList.add(dstPath);

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }

            int ret=editor.executeVideoEditor(command);
            if(ret==0){
                return dstPath;
            }else{
                LSOLog.e("executePcmConvertSamplerate 失败, 请查看打印信息");
                return null;
            }
        }else{
            LSOLog.e("executePcmConvertSamplerate 执行失败, 文件不存在");
            return null;
        }
    }



    public String  executeConvertToWav(String inputAudio,int dstSample) {
        if(LanSongFileUtil.fileExist(inputAudio)){

            String dstPath=LanSongFileUtil.createWAVFileInBox();

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(inputAudio);

            cmdList.add("-vn");

            cmdList.add("-ac");
            cmdList.add("2");

            if(dstSample>0){
                cmdList.add("-ar");
                cmdList.add(String.valueOf(dstSample));
            }

            cmdList.add("-y");
            cmdList.add(dstPath);

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret=editor.executeVideoEditor(command);
            if(ret==0){
                return dstPath;
            }else{
                LSOLog.e("executeConvertToWav 失败, 请查看打印信息");
                return null;
            }
        }else{
            LSOLog.e("executeConvertToWav 执行失败, 文件不存在");
            return null;
        }
    }

    public String  executeConvertToPCM(String inputAudio) {
        if(LanSongFileUtil.fileExist(inputAudio)){

            String dstPath=LanSongFileUtil.createFileInBox("pcm");

            List<String> cmdList = new ArrayList<String>();


            cmdList.add("-i");
            cmdList.add(inputAudio);

            cmdList.add("-vn");


            cmdList.add("-f");
            cmdList.add("s16le");

            cmdList.add("-acodec");
            cmdList.add("pcm_s16le");


            cmdList.add("-ac");
            cmdList.add("2");



            cmdList.add("-ar");
            cmdList.add("44100");

            cmdList.add("-y");
            cmdList.add(dstPath);

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret=editor.executeVideoEditor(command);
            if(ret==0){
                return dstPath;
            }else{
                return null;
            }
        }else{
            return null;
        }
    }

    public String  executeConvertToMonoWav(String inputAudio,int dstSample) {
        if(LanSongFileUtil.fileExist(inputAudio)){

            String dstPath=LanSongFileUtil.createWAVFileInBox();

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(inputAudio);

            cmdList.add("-vn");

            cmdList.add("-f");
            cmdList.add("s16le");

            cmdList.add("-ac");
            cmdList.add("1");
            cmdList.add("-acodec");
            cmdList.add("pcm_s16le");

            if(dstSample>0){
                cmdList.add("-ar");
                cmdList.add(String.valueOf(dstSample));
            }

            cmdList.add("-y");
            cmdList.add(dstPath);

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret=editor.executeVideoEditor(command);
            if(ret==0){
                return dstPath;
            }else{
                return null;
            }
        }else{
            return null;
        }
    }


    public String executeConvertWavToMp3(String wavInput,int dstSample)
    {
        if(LanSongFileUtil.fileExist(wavInput)){

            String dstPath=LanSongFileUtil.createMP3FileInBox();

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(wavInput);

            cmdList.add("-acodec");
            cmdList.add("libmp3lame");
            cmdList.add("-b:a");
            cmdList.add("128000");

            cmdList.add("-ac");
            cmdList.add("2");

            if(dstSample>0){
                cmdList.add("-ar");
                cmdList.add(String.valueOf(dstSample));
            }
            cmdList.add("-y");
            cmdList.add(dstPath);

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret=editor.executeVideoEditor(command);
            if(ret==0){
                return dstPath;
            }else{
                return null;
            }
        }else{
            return null;
        }
    }


    public String executeConvertWavToM4a(String wavInput,int dstSample)
    {
        if(LanSongFileUtil.fileExist(wavInput)){

            String dstPath=LanSongFileUtil.createM4AFileInBox();

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(wavInput);

            cmdList.add("-acodec");
            cmdList.add("libfaac");
            cmdList.add("-b:a");
            cmdList.add("128000");

            cmdList.add("-ac");
            cmdList.add("2");

            if(dstSample>0){
                cmdList.add("-ar");
                cmdList.add(String.valueOf(dstSample));
            }
            cmdList.add("-y");
            cmdList.add(dstPath);

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret=editor.executeVideoEditor(command);
            if(ret==0){
                return dstPath;
            }else{
                return null;
            }
        }else{
            return null;
        }
    }


    public String executeConvertM4aToMp3(String input,int dstSample)
    {
        if(LanSongFileUtil.fileExist(input)){

            String dstPath=LanSongFileUtil.createMP3FileInBox();

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(input);

            cmdList.add("-vn");

            cmdList.add("-acodec");
            cmdList.add("libmp3lame");
            cmdList.add("-b:a");
            cmdList.add("128000");

            cmdList.add("-ac");
            cmdList.add("2");

            if(dstSample>0){
                cmdList.add("-ar");
                cmdList.add(String.valueOf(dstSample));
            }
            cmdList.add("-y");
            cmdList.add(dstPath);

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret=editor.executeVideoEditor(command);
            if(ret==0){
                return dstPath;
            }else{
                return null;
            }
        }else{
            return null;
        }
    }

    public String executeConvertMp3ToM4a(String input,int dstSample)
    {
        if(LanSongFileUtil.fileExist(input)){

            String dstPath=LanSongFileUtil.createM4AFileInBox();

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(input);

            cmdList.add("-vn");

            cmdList.add("-acodec");
            cmdList.add("libfaac");
            cmdList.add("-b:a");
            cmdList.add("128000");

            cmdList.add("-ac");
            cmdList.add("2");

            if(dstSample>0){
                cmdList.add("-ar");
                cmdList.add(String.valueOf(dstSample));
            }
            cmdList.add("-y");
            cmdList.add(dstPath);

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret=editor.executeVideoEditor(command);
            if(ret==0){
                return dstPath;
            }else{
                return null;
            }
        }else{
            return null;
        }
    }
    public String executePcmMix(String srcPach1, int samplerate, int channel, String srcPach2, int samplerate2, int
            channel2,float value1, float value2) {
        List<String> cmdList = new ArrayList<String>();

        String filter = String.format(Locale.getDefault(), "[0:a]volume=volume=%f[a1]; [1:a]volume=volume=%f[a2]; " +
                "[a1][a2]amix=inputs=2:duration=first:dropout_transition=2", value1, value2);

        String  dstPath=LanSongFileUtil.createFileInBox("pcm");

        cmdList.add("-f");
        cmdList.add("s16le");
        cmdList.add("-ar");
        cmdList.add(String.valueOf(samplerate));
        cmdList.add("-ac");
        cmdList.add(String.valueOf(channel));
        cmdList.add("-i");
        cmdList.add(srcPach1);

        cmdList.add("-f");
        ;
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
        int  ret= editor.executeVideoEditor(command);
        if(ret==0){
            return dstPath;
        }else{
            LanSongFileUtil.deleteFile(dstPath);
            return null;
        }
    }

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
        int ret= editor.executeVideoEditor(command);
        if(ret==0){
            return dstPath;
        }else{
            LanSongFileUtil.deleteFile(dstPath);
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
            int ret= editor.executeVideoEditor(command);
            if(ret==0){
                return dstPath;
            }else{
                LanSongFileUtil.deleteFile(dstPath);
                return null;
            }
        }
        return null;
    }

    public String executeCutAudio(String srcFile, float startS, float durationS) {
        MediaInfo info=new MediaInfo(srcFile);
        if (info.prepare()) {
            editor.setDurationMs((int)(durationS *1000));
            List<String> cmdList = new ArrayList<String>();

            String dstFile=LanSongFileUtil.createM4AFileInBox();

            cmdList.add("-i");
            cmdList.add(srcFile);

            cmdList.add("-ss");
            cmdList.add(String.valueOf(startS));

            cmdList.add("-t");
            cmdList.add(String.valueOf(durationS));

            cmdList.add("-vn");
//            cmdList.add("-acodec");
//            cmdList.add("copy");

            //2019-04-26 09:32:40 强制为faac编码;
            cmdList.add("-acodec");
            cmdList.add("libfaac");

            cmdList.add("-ac");
            cmdList.add("2");

            cmdList.add("-ar");
            cmdList.add("44100");

            cmdList.add("-b:a");
            cmdList.add("128000");


            cmdList.add("-y");
            cmdList.add(dstFile);

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret= editor.executeVideoEditor(command);
            editor.setDurationMs(0);
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


    public String  executeVideoReplaceAudio(String video,String audio){
        return executeVideoMergeAudio(video,audio,0.0f,1.0f);
    }


    public String executeVideoMergeAudio(String video, String  audio,float volume1,float volume2) {

        if(volume2<=0){
            return video;
        }

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

            if(volume1>0 && vInfo.isHaveAudio()){//两个声音混合;
                String filter = String.format(Locale.getDefault(), "[0:a]volume=volume=%f[a1]; [1:a]volume=volume=%f[a2]; " +
                        "[a1][a2]amix=inputs=2:duration=first:dropout_transition=2", volume1, volume2);

                cmdList.add("-filter_complex");
                cmdList.add(filter);

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
            }else if(isAAC && volume2==1.0f) {  //删去视频的原音,直接增加音频

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

                String filter=String.format(Locale.getDefault(), "volume=%f",volume2);
                cmdList.add("-af");
                cmdList.add(filter);

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

    public String  executeConvertToXunFeiAudio(String inputPath) {

        MediaInfo info=new MediaInfo(inputPath);
        if(info.prepare()){

            String dstPath=LanSongFileUtil.createWAVFileInBox();

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(inputPath);



            cmdList.add("-vn");

            if(info.aDuration>60.f){
                cmdList.add("-ss");
                cmdList.add(String.valueOf(0));

                cmdList.add("-t");
                cmdList.add(String.valueOf(60.0));
            }
            cmdList.add("-ac");
            cmdList.add("1");

            cmdList.add("-ar");
            cmdList.add(String.valueOf(16000));

            cmdList.add("-acodec");
            cmdList.add("pcm_s16le");

            cmdList.add("-y");
            cmdList.add(dstPath);

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret=editor.executeVideoEditor(command);
            if(ret==0){
                return dstPath;
            }else{
                LSOLog.e("executeConvertToWav 失败, 请查看打印信息");
                return null;
            }
        }else{
            LSOLog.e("executeConvertToWav 执行失败, 文件不存在");
            return null;
        }
    }


    public String executeCutAudioFast(String srcFile, String dstFile, float startS, float durationS) {
        MediaInfo info=new MediaInfo(srcFile);
        if (info.prepare()) {
            List<String> cmdList = new ArrayList<String>();
            cmdList.add("-i");
            cmdList.add(srcFile);

            cmdList.add("-ss");
            cmdList.add(String.valueOf(startS));

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
            int ret= editor.executeVideoEditor(command);
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
    public String executeCutAudioNormal(String srcFile, String dstFile, float startS, float durationS) {
        MediaInfo info=new MediaInfo(srcFile);
        if (info.prepare()) {
            List<String> cmdList = new ArrayList<String>();
            cmdList.add("-i");
            cmdList.add(srcFile);

            cmdList.add("-ss");
            cmdList.add(String.valueOf(startS));

            cmdList.add("-t");
            cmdList.add(String.valueOf(durationS));

            cmdList.add("-y");
            cmdList.add(dstFile);

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret= editor.executeVideoEditor(command);
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


}
