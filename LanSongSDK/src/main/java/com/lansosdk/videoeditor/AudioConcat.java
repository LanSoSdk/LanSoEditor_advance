package com.lansosdk.videoeditor;

import android.content.Context;

import com.lansosdk.box.AudioConcatManager;

/**
 * 音频拼接.
 * <p>
 * 比如第一个音频MP3和第二个mp3, 拼接在一起形成新的mp3;
 * <p>
 * 比如 一个mp3和一个m4a 拼接成新的m4a
 */
public class AudioConcat {

    private AudioConcatManager concatMng;

    public AudioConcat(Context ctx) {
        concatMng = new AudioConcatManager(ctx);
    }

    /**
     * 增加音频, 增加多次, 以此拼接, 先增加的放在前面, 后增加的放到后面. 当然音频仅支持 mp3和 m4a(aac)两种格式.
     *
     * @param srcPath
     * @return 增加失败返回false;
     */
    public boolean addAudio(String srcPath) {
        if (concatMng != null)
            return concatMng.addAudio(srcPath);
        else
            return false;
    }

    /**
     * 这里是阻塞执行, 建议另开一个线程或放到ASyncTask中执行.
     * <p>
     * 内部会先检测是否会直接拼接, 如果可以直接拼接, 则直接拼接, 如果不可以,则先解码后再编码.
     * <p>
     * 如果手机硬件支持 音频编码器, 则执行很快,大概1--2秒即可完成. 如果不支持则采用软件编解码.
     *
     * @return
     */
    public String executeAudioConcat() {
        if (concatMng != null)
            return concatMng.executeConcat();
        else
            return null;
    }

    public void release() {
        if (concatMng != null) {
            concatMng.release();
            concatMng = null;
        }
    }

    /**
     *
     * private void testFile() { AudioConcatManager mng=new
     * AudioConcatManager(getApplicationContext());
     *
     * mng.addAudio("/sdcard/g1.mp3"); mng.addAudio("/sdcard/niu30s.m4a");
     * mng.addAudio("/sdcard/g2.mp3");
     *
     * String str=mng.executeAudioMix(); Log.i(TAG,"str is:"+str);
     *
     * mng.release();
     *
     * //--------播放. MediaPlayer player=new MediaPlayer(); try {
     * player.setDataSource(str); player.prepare(); player.start(); }catch
     * (IOException e) { // TODO Auto-generated catch block e.printStackTrace();
     * } }
     */
}
