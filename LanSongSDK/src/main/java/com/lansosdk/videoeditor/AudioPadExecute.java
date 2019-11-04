package com.lansosdk.videoeditor;

import android.content.Context;

import com.lansosdk.box.AudioLayer;
import com.lansosdk.box.AudioPad;
import com.lansosdk.box.AudioPadRunnable;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.OnAudioPadExecuteCompletedListener;
import com.lansosdk.box.onAudioPadCompletedListener;
import com.lansosdk.box.onAudioPadProgressListener;
import com.lansosdk.box.onAudioPadThreadProgressListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 音频容器;
 *
 * 使用1:
 *          给视频增加其他声音,并设置各自的音量,循环,快慢,变声等.
 *          视频可以有声音,或无声音.
 * 使用2:
 *          先设置容器的固定时长, 分别在指定的时间点增加上声音.
 *          当前的音频格式支持 MP3,WAV, AAC(m4a后缀)
 *          最终生成一个设定时长的m4a文件(aac编码)
 */
public class AudioPadExecute {


    AudioPadRunnable render;
    /**
     * 构造方法
     *
     * @param ctx
     * @param input 输入如是音频则返回的是m4a的音频文件; 如是视频 则返回的是mp4的视频文件
     */
    public AudioPadExecute(Context ctx, String input) throws Exception {
        if(render==null){
            render=new AudioPadRunnable(ctx,input);
        }
    }

    /**
     * 构造方法
     *
     * @param ctx
     * @param input  输入如是音频则返回的是m4a的音频文件; 如是视频 则返回的是mp4的视频文件
     * @param isMute 如果是视频的话,则视频中的声音是否会静音;
     */
    public AudioPadExecute(Context ctx, String input, boolean isMute) {
        if(render==null){
            render=new AudioPadRunnable(ctx,input,isMute);
        }
    }

    /**
     * 因为其他add都是long类型, 废弃这个长度是秒的构造方法;
     * @param ctx
     * @param durationS
     */
    @Deprecated
    public AudioPadExecute(Context ctx, float durationS) {
        if(render==null){
            render=new AudioPadRunnable(ctx,durationS);
        }
    }

    /**
     * 因为其他add都是long类型, 废弃这个长度是秒的构造方法;
     * 请用 AudioPadRunnable(Context ctx, long durationUS);
     * @param ctx
     * @param durationS
     * @param sampleRate
     */
    @Deprecated
    public AudioPadExecute(Context ctx, float durationS, int sampleRate) {
        if(render==null){
            render=new AudioPadRunnable(ctx,durationS,sampleRate);
        }
    }

    /**
     * 构造方法
     * 先设置一段时长 单位微秒( 1秒=1000*1000微秒);
     * @param ctx
     * @param durationUS 微秒; <---注意这里是微秒;
     */
    public AudioPadExecute(Context ctx, long durationUS) {
        if(render==null){
            render=new AudioPadRunnable(ctx,durationUS);
        }
    }

    public long getDurationUs(){
        if(render!=null){
            return render.getDurationUs();
        }else{
            return 1000;
        }
    }

    /**
     * 在构造方法设置后, 会生成一个主音频的AudioLayer对象,从而对音频做调节;
     * @return 返回增加后音频层, 可以用来设置音量,快慢,变声等.
     */
    public AudioLayer getMainAudioLayer() {
        if(render!=null){
            return render.getMainAudioLayer();
        }else{
            return null;
        }
    }

    public AudioLayer addAudioLayer(String srcPath) {
        if(render!=null){
            return render.addAudioLayer(srcPath);
        }else{
            return null;
        }
    }

    /**
     * 增加其他音频;
     * 支持mp4,wav,mp3,m4a文件;
     *
     * @param srcPath
     * @param isLoop  是否循环;
     * @return 返回增加后音频层, 可以用来设置音量,快慢,变声等.
     */
    public AudioLayer addAudioLayer(String srcPath, boolean isLoop) {

        if(render!=null){
            return render.addAudioLayer(srcPath,isLoop);
        }else{
            return null;
        }
    }

    /**
     * 增加其他音频;
     * 支持mp4,wav,mp3,m4a文件;
     *
     * @param volume  音频的音量; 范围是0--10; 1.0正常;大于1.0提高音量;小于1.0降低音量;
     * @return 返回增加后音频层, 可以用来设置音量,快慢,变声等.
     */
    public AudioLayer addAudioLayer(String srcPath, boolean isLoop, float volume) {
        if(render!=null){
            return render.addAudioLayer(srcPath,isLoop,volume);
        }else{
            return null;
        }
    }

    /**
     * 增加音频容器, 从容器的什么位置开始增加,
     *
     * @param srcPath
     * @param startPadUs 从容器的什么地方增加这个音频
     * @return 返回增加后音频层, 可以用来设置音量,快慢,变声等.
     */
    public AudioLayer addAudioLayer(String srcPath, long startPadUs) {
        if(render!=null){
            return render.addAudioLayer(srcPath,startPadUs);
        }else{
            return null;
        }
    }

    /**
     * 把音频的 指定时间段, 增加到audiopad音频容器里.
     *
     *
     * 如果有循环或其他操作, 可以在获取的AudioLayer对象中设置.
     *
     * @param srcPath      音频文件路径, 可以是有音频的视频路径;
     * @param offsetPadUs  从容器的什么时间开始增加.相对容器偏移多少.
     * @param startAudioUs 该音频的开始时间
     * @param endAudioUs   该音频的结束时间. 如果要增加到文件尾,则可以直接填入-1;
     * @return 返回增加后音频层, 可以用来设置音量,快慢,变声等.
     */
    public AudioLayer addAudioLayer(String srcPath, long offsetPadUs,
                                    long startAudioUs, long endAudioUs) {
        if(render!=null){
            return render.addAudioLayer(srcPath,offsetPadUs,startAudioUs,endAudioUs);
        }else{
            return null;
        }
    }
    public int getAudioCount(){
        if(render!=null){
            return render.getAudioCount();
        }else{
            return 0;
        }

    }

    /**
     * 设置监听当前audioPad的处理进度.
     * <p>
     * 此监听是通过handler机制,传递到UI线程的, 你可以在里面增加ui的代码. 因为经过了handler机制,
     * 可能会进度比正在处理延迟一些,不完全等于当前处理的帧时间戳.
     *
     * @param listener
     */
    public void setOnAudioPadProgressListener(onAudioPadProgressListener listener) {
        if(render!=null){
            render.setOnAudioPadProgressListener(listener);
        }
    }

    /**
     * 设置监听当前audioPad的处理进度. 一个音频帧处理完毕后, 直接执行您listener中的代码.
     * 在audioPad线程中执行,不能在里面增加UI代码.
     * <p>
     * 建议使用这个.
     * <p>
     * 如果您声音在40s一下,建议使用这个, 因为音频本身很短,处理时间很快.
     *
     * @param listener
     */
    public void setOnAudioPadThreadProgressListener(onAudioPadThreadProgressListener listener) {
        if(render!=null){
            render.setOnAudioPadThreadProgressListener(listener);
        }
    }


    /**
     * 完成监听. 经过handler传递到主线程, 可以在里面增加UI代码.
     *
     * @param listener
     */
    public void setOnAudioPadCompletedListener(OnAudioPadExecuteCompletedListener listener) {
        if(render!=null){
            render.setOnAudioPadCompletedListener(listener);
        }
    }
    /**
     * 开启另一个线程, 并开始音频处理
     *
     * @return
     */
    public boolean start() {
        if(render!=null){
            return render.start();
        } else {
            return false;
        }
    }

    /**
     * 等待执行完毕;[大部分情况下不需要调用]
     * <p>
     * 适合在音频较短,为了代码的整洁, 不想设置listener回调的场合;
     * <p>
     * 注意:这里设置后,
     * 当前线程将停止在这个方法处,直到音频执行完毕退出为止.建议放到另一个线程中执行. 可选使用.
     */
    public String waitComplete() {
        if(render!=null){
            return render.waitComplete();
        }else{
            return null;
        }
    }

    /**
     * 停止当前audioPad的处理;
     */
    public void stop() {
        if(render!=null){
            render.stop();
        }
    }

    /**
     * 释放AudioPad容器;
     */
    public void release() {
        if(render!=null){
            render.release();
            render=null;
        }
    }



    // ----------------------------一下为测试代码-------------------------------------------

    /**

     float source1Volume=1.0f;
     AudioLayer audioLayer;

     private void testFile3(){
     AudioPadExecute execute = new AudioPadExecute(getApplicationContext(), "/sdcard/d1.mp4");
     //增加一个音频
     audioLayer = execute.addAudioLayer("/sdcard/hongdou10s.mp3", 0, 0, -1);
     if (audioLayer != null) {
     audioLayer.setVolume(0.01f);
     }
     audioLayer.setSoundPitchLittleGirl();

     //主音频静音;
     AudioLayer audioSource = execute.getMainAudioLayer();
     if (audioSource != null) {
     audioSource.setMute(true);
     }
     execute.setOnAudioPadThreadProgressListener(new onAudioPadThreadProgressListener() {
    @Override
    public void onProgress(AudioPad v, long currentTimeUs) {
    if(currentTimeUs<2*1000*1000){
    audioLayer.setVolume(source1Volume);
    source1Volume-=0.015f;
    if(source1Volume<0.0){
    source1Volume=0.0f;
    }
    }else{
    audioLayer.setVolume(1.0f);
    }
    }
    });
     execute.setOnAudioPadCompletedListener(new OnAudioPadExecuteCompletedListener() {
    @Override
    public void onCompleted(String path) {
    MediaInfo.checkFile(path);
    }
    });
     execute.startPreview();
     }

     */
}
