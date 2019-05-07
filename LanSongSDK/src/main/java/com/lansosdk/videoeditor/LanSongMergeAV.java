package com.lansosdk.videoeditor;

import android.util.Log;

import com.lansosdk.box.LSLog;
import java.util.ArrayList;
import java.util.List;

/**
 * 不再使用.
 * 此类将在2019-6-1日后删除.
 */
@Deprecated
public class LanSongMergeAV extends VideoEditor {

    private static final String TAG = LSLog.TAG;
    protected ArrayList<String> deleteArray = new ArrayList<String>();
    public static String mergeAVDirectly(String audio, String video,boolean deleteVideo) {
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
    /**
     * 合并音视频,  替换背景音乐;
     *
     * @param audio  含有音乐的完整路径, 可以是 mp3/m4a的音乐, 也可以是含有音频的mp4文件;
     * @param video  含有视频轨道的文件, 可以是无声声音或有其他声音的视频; 如果视频中有声音,则声音会被替换
     * @param dstMp4 生成的目标文件, 后缀是mp4;
     * @return
     */
    public int mergeAudioVideo(String audio, String video, String dstMp4) {
        MediaInfo aInfo = new MediaInfo(audio);
        MediaInfo vInfo = new MediaInfo(video);

        if (aInfo.prepare() && aInfo.isHaveAudio() && vInfo.prepare() && vInfo.isHaveVideo()) {

            String inputAudio = audio;
            List<String> cmdList = new ArrayList<String>();
            if (aInfo.aDuration > vInfo.vDuration) {

                if("aac".equals(aInfo.aCodecName)){
                    cmdList.add("-t");
                    cmdList.add(String.valueOf(vInfo.vDuration));
                } else{
                    String aac= LanSongFileUtil.createAACFileInBox();
                    deleteArray.add(aac);

                    convertAudioToAAC(inputAudio,vInfo.vDuration,aac);
                    inputAudio=aac;
                }


            } else if (Math.abs(aInfo.aDuration - vInfo.vDuration) < 1.0f) {  //长度大致相等,则什么都不做

            } else {//如果长度不够,则音频循环
                inputAudio = getEnoughAudio(aInfo, vInfo.vDuration);
            }
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
            cmdList.add(dstMp4);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            VideoEditor editor = new VideoEditor();
            int ret = editor.executeVideoEditor(command);

            for (String item : deleteArray) {
                LanSongFileUtil.deleteFile(item);
            }
            return ret;
        } else {
            Log.w(TAG, "");
            return -1;
        }
    }

    /**
     * 音频不够, 则拼接音频
     *
     * @param input
     * @return
     */
    private String getEnoughAudio(MediaInfo input, float vDuration) {

        String audioPath = input.filePath;
        //先把mp3转换aac,再拼接aac
        if ("mp3".equals(input.aCodecName)) {

            String aacAudio = LanSongFileUtil.createAACFileInBox();
            deleteArray.add(aacAudio);

            convertAudioToAAC(input.filePath,0,aacAudio);
            audioPath = aacAudio;
        }

        if(input.fileSuffix.equalsIgnoreCase("m4a")){
            String aacAudio = LanSongFileUtil.createAACFileInBox();
            deleteArray.add(aacAudio);

            convertM4aToAAC(input.filePath,aacAudio);

            audioPath = aacAudio;
        }

        int num = (int) (vDuration / input.aDuration + 1.0f);
        String[] array = new String[num];

        for (int i = 0; i < num; i++) {
            array[i] = audioPath;
        }

        String m4aAudio = LanSongFileUtil.createAACFileInBox();
        deleteArray.add(m4aAudio);

        concatAudio(array, m4aAudio); //拼接好.

        return m4aAudio;
    }

    /**
     * 把多个aac文件拼接成一个;
     *
     * @param tsArray
     * @param dstFile
     * @return
     */
    private int concatAudio(String[] tsArray, String dstFile) {
        if (LanSongFileUtil.filesExist(tsArray)) {
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

            cmdList.add("-y");

            cmdList.add(dstFile);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            VideoEditor editor = new VideoEditor();
            return editor.executeVideoEditor(command);
        } else {
            return -1;
        }
    }

    /**
     * 把非aac编码的音频，先裁剪， 然后转换为aac
     * @param mp3Path  输入音频
     * @param duration  从开裁剪多少 转换为aac， 如果为0， 则全部转换；
     * @param dstAacPath  转换后的音频
     * @return
     */
    private int convertAudioToAAC(String mp3Path, float duration,String dstAacPath) {
        List<String> cmdList = new ArrayList<String>();

        if(duration>0){
            cmdList.add("-t");
            cmdList.add(String.valueOf(duration));
        }
        cmdList.add("-i");
        cmdList.add(mp3Path);

        cmdList.add("-acodec");
        cmdList.add("libfaac");

        cmdList.add("-y");
        cmdList.add(dstAacPath);
        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        return executeVideoEditor(command);
    }
    private int convertM4aToAAC(String m4aPath,String dstAACPath){
        List<String> cmdList = new ArrayList<String>();

        cmdList.add("-i");
        cmdList.add(m4aPath);

        cmdList.add("-acodec");
        cmdList.add("copy");

        cmdList.add("-y");
        cmdList.add(dstAACPath);
        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        return executeVideoEditor(command);
    }


//    //-----一下是测试代码
//    public static void test() {
//
//        LanSongMergeAV mergeAV=new LanSongMergeAV();
////        String audio="/sdcard/hongdou.mp3";
////        String audio="/sdcard/hongdou10s.mp3";
//
////        String audio="/sdcard/liang.m4a";
////        String audio="/sdcard/niu10s.aac";
//
//        String audio="/sdcard/h.wav";
//
//        String video="/sdcard/ping20s.mp4";
//
//        String dstVideo="/sdcard/av12.mp4";
//
//        mergeAV.mergeAudioVideo(audio,video,dstVideo);
//
//    }
}
