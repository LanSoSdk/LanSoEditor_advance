package com.lansosdk.videoeditor;

import android.media.MediaPlayer;

import com.lansosdk.videoeditor.archApi.LanSongFileUtil;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 声音播放器;
 */
public class LSOAudioPlayer {

    private MediaPlayer mediaPlayer;

    private String inputPath;
    private String audioTrackPath;
    private long startTimeUs;
    private long endTimeUs;

    private AtomicBoolean cancel = new AtomicBoolean(false);

    public static void play(String path) {
        LSOAudioPlayer audioPlayer = new LSOAudioPlayer();
        audioPlayer.start(path);
    }

    public static void play(String path, long startUs, long endUs) {
        LSOAudioPlayer audioPlayer = new LSOAudioPlayer();
        audioPlayer.start(path, startUs, endUs);
    }

    public void start(String path) {
        start(path, 0, Long.MAX_VALUE);
    }

    public void start(String path, long startUs, long endUs) {
        inputPath = path;
        mediaPlayer = new MediaPlayer();
        startTimeUs = startUs;
        endTimeUs = endUs;

        //get audio track
        if(isVideoSuffix(inputPath)){
            VideoEditor editor=new VideoEditor();
            audioTrackPath=editor.executeGetAudioTrack(inputPath);
        }
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(audioTrackPath!=null){
                        mediaPlayer.setDataSource(audioTrackPath);
                    }else{
                        mediaPlayer.setDataSource(inputPath);
                    }

                    //同步;
                    mediaPlayer.prepare();
                    mediaPlayer.setVolume(1.0f, 1.0f);
                    mediaPlayer.start();
                    mediaPlayer.seekTo((int) (startTimeUs / 1000));
                    //start play
                    while (!cancel.get()) {
                        if (mediaPlayer.getCurrentPosition() * 1000 > endTimeUs) {
                            mediaPlayer.pause();
                        }else{
                            try {
                                Thread.sleep(30);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                    LanSongFileUtil.deleteFile(audioTrackPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public void  seekTo(long timeUs){
        if(mediaPlayer!=null){
            mediaPlayer.seekTo((int)(timeUs/1000));
        }
    }
    public void start(){
        if(mediaPlayer!=null){
            mediaPlayer.start();
        }
    }
    public void playTimeRange(long startUs,long endUs){
        if(mediaPlayer!=null && endUs> startUs){
            startTimeUs=startUs;
            endTimeUs=endUs;
            mediaPlayer.seekTo((int)(startUs/1000));
            mediaPlayer.start();
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public void release() {
        cancel.set(true);
    }



    private static String getFileSuffix(String path) {
        if (path == null)
            return "";
        int index = path.lastIndexOf('.');
        if (index > -1)
            return path.substring(index + 1);
        else
            return "";
    }
    private boolean isVideoSuffix(String path) {
        String suffix=getFileSuffix(path);
        return "mp4".equalsIgnoreCase(suffix)
                || "mov".equalsIgnoreCase(suffix);
    }
}