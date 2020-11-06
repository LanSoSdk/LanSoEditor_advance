package com.lansosdk.videoeditor.oldVersion;

import android.content.Context;

import com.lansosdk.box.AudioLayer;
import com.lansosdk.box.AudioPadRunnable;
import com.lansosdk.box.OnAudioPadExecuteCompletedListener;
import com.lansosdk.box.onAudioPadProgressListener;
import com.lansosdk.box.onAudioPadThreadProgressListener;


@Deprecated
public class AudioPadExecute {


    AudioPadRunnable render;

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
     * @param endAudioUs   该音频的结束时间. 如果要增加到文件尾,则填入Long.MAX_VALUE
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


    public void setOnAudioPadCompletedListener(OnAudioPadExecuteCompletedListener listener) {
        if(render!=null){
            render.setOnAudioPadCompletedListener(listener);
        }
    }

    public boolean start() {
        if(render!=null){
            return render.start();
        } else {
            return false;
        }
    }

    public String waitComplete() {
        if(render!=null){
            return render.waitComplete();
        }else{
            return null;
        }
    }


    public void stop() {
        if(render!=null){
            render.stop();
        }
    }

    public void release() {
        if(render!=null){
            render.release();
            render=null;
        }
    }



    // ----------------------------一下为测试代码-------------------------------------------
    /**

     给视频增加一个声音;

     float source1Volume=1.0f;
     AudioLayer audioLayer;

     private void testFile4() throws  Exception{
     String videoPath= CopyFileFromAssets.copyAssets(getApplicationContext(),"dy_xialu2.mp4");

     AudioPadExecute execute = new AudioPadExecute(getApplicationContext(), videoPath);
     //增加一个音频
     audioLayer = execute.addAudioLayer("/sdcard/hongdou10s.mp3");

     //主音频静音;
     AudioLayer audioLayer = execute.getMainAudioLayer();
     audioLayer.setMute(true);

     execute.setOnAudioPadThreadProgressListener(new onAudioPadThreadProgressListener() {
    @Override
    public void onProgress(AudioPad v, long currentTimeUs) {
    }
    });
     execute.setOnAudioPadCompletedListener(new OnAudioPadExecuteCompletedListener() {
    @Override
    public void onCompleted(String videoPath) {
    MediaInfo.checkFile(videoPath);
    }
    });
     execute.start();
     }
///举例2----------音频拼接举例------------------------

     ArrayList<String> audios=new ArrayList<>();
     audios.add("/sdcard/audio1/record1.mp3");
     audios.add("/sdcard/audio1/record2.mp3");
     audios.add("/sdcard/audio1/record3.mp3");
     audios.add("/sdcard/audio1/record4.mp3");
     audios.add("/sdcard/audio1/record5.mp3");
     audios.add("/sdcard/audio1/record6.mp3");


     ArrayList<MediaInfo> audioInfoArray=new ArrayList<>();

     float durationS=0;
     for (String str: audios){

     MediaInfo info=new MediaInfo(str);
     if(info.prepare() && info.isHaveAudio()){
         audioInfoArray.add(info);
         durationS+=info.aDuration;
     }
     }


     long startUs=0;
     final long durationUs=(long)(durationS*1000*1000);

     AudioPadExecute execute=new AudioPadExecute(getApplication(),durationUs);

     for (MediaInfo info:audioInfoArray){
     execute.addAudioLayer(info.filePath,startUs);
     startUs+= (long)(info.aDuration*1000*1000);
     }

     execute.setOnAudioPadCompletedListener(new OnAudioPadExecuteCompletedListener() {
    @Override
    public void onCompleted(String videoPath) {

    MediaInfo.checkFile(videoPath);
    }
    });

     execute.setOnAudioPadProgressListener(new onAudioPadProgressListener() {
    @Override
    public void onProgress(AudioPad v, long currentTimeUs) {
    Log.e("TAG", "----currentTimeUs: "+currentTimeUs + " percent is :"+(currentTimeUs *100/durationUs));
    }
    });
     execute.start();

     */
}
