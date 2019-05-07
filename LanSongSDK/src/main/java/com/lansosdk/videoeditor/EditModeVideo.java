package com.lansosdk.videoeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.lansosdk.box.FrameInfo;
import com.lansosdk.box.LSLog;

import java.io.FileNotFoundException;
import java.nio.IntBuffer;
import java.util.ArrayList;

/**
 * 我们定义了一种视频格式,命名为"EditModeVideo", 这种转换的视频格式,会很快的找到视频中的每一帧, 像翻书一样,方便定位, 提取帧,倒序播放等;
 * <p>
 * 功能:
 * 2, 导出
 * 3, 可以检测当前视频 是否是 "EditModeVideo"的视频格式;
 * <p>
 *
 * 区别:
 * "EditModeVideo"格式的视频, 和正常的mp4唯一区别是: 比他大一些;, 也是正常的mp4视频,可以直接分享到微信, 上传到服务器等等;
 *
 * @author Administrator
 */
public class EditModeVideo {

    private static final String TAG = LSLog.TAG;
    protected String inputPath;
    protected MediaInfo inputInfo;
    protected VideoOneDo oneDo;
    protected Context ctx;
    protected boolean isConvertRunning;
    protected boolean isInputEditMode; // 输入的是否已经是
    protected String editVideoPath;
    private long decoderHandler = 0;
    private IntBuffer decoderRGBBuffer;
    private onVideoOneDoProgressListener monVideoOneDoProgressListener;
    private onVideoOneDoCompletedListener monVideoOneDOCompletedListener = null;
    private onVideoOneDoErrorListener monVideoOneDoErrorListener = null;

    public EditModeVideo(Context ctx, String input) {
        inputPath = input;
        this.ctx = ctx;
        inputInfo = new MediaInfo(inputPath);
        inputInfo.prepare();
        isInputEditMode = checkEditModeVideo(inputPath);

        if (isInputEditMode) { // 输入的已经是蓝松video
            editVideoPath = inputPath;
        }
    }

    /**
     * 检查视频是否是编辑模式;
     * @param path
     * @return
     */
    public static boolean checkEditModeVideo(String path) {
        return FrameInfo.isLanSongVideo2(path);
    }
    /**
     * 获取
     * @return
     */
    public String getEditModeVideoPath() {
        if (LanSongFileUtil.fileExist(editVideoPath)) {
            return editVideoPath;
        } else {
            LSLog.e("获取蓝松视频失败,因为:"
                    + (isConvertRunning ? "正在转换中..." : "未知,请联系我们!"));
            return null;
        }
    }

    /**
     * 把"EditModeVideo"格式的视频, 导出到正常的视频, 从而您上传到服务器, 分享到其他的地方; 当然不导出也可以直接用,
     * 只是文件大一些而已,也是正常的mp4文件;
     * <p>
     * 如您在实际使用中, 用到DrawPadVideoExecute或VideoOneDo等类,做了其他各种操作时, 可以不用再次调用这个方法, 直接把视频码率设置下即可;
     * 异步执行;
     */
    public void export() {
        if (inputInfo.isHaveVideo() && isInputEditMode&& !isConvertRunning) {
            synchronized (this) {
                isConvertRunning = true;
                oneDo = new VideoOneDo(ctx, editVideoPath);
                oneDo.setBitrate(LanSongUtil.getSuggestBitRate(inputInfo.vWidth * inputInfo.vHeight));
                oneDo.setOnVideoOneDoCompletedListener(new onVideoOneDoCompletedListener() {

                    @Override
                    public void onCompleted(VideoOneDo v, String dstVideo) {
                        if (monVideoOneDOCompletedListener != null) {
                            monVideoOneDOCompletedListener.onCompleted(oneDo, dstVideo);
                        }
                        isConvertRunning = false;
                        oneDo.release();
                        oneDo = null;
                    }
                });
                oneDo.setOnVideoOneDoProgressListener(monVideoOneDoProgressListener);
                oneDo.setOnVideoOneDoErrorListener(monVideoOneDoErrorListener);
                if (!oneDo.start()) {
                    editVideoPath = null;
                    isConvertRunning = false;
                }
            }
        } else {
            isConvertRunning = false;
            if (monVideoOneDOCompletedListener != null) {
                monVideoOneDOCompletedListener.onCompleted(oneDo, inputPath);
            }
        }
    }

    public void setOnVideoOneDoProgressListener(onVideoOneDoProgressListener li) {
        monVideoOneDoProgressListener = li;
    }

    public void setOnVideoOneDoCompletedListener(
            onVideoOneDoCompletedListener li) {
        monVideoOneDOCompletedListener = li;
    }

    public void setOnVideoOneDoErrorListener(onVideoOneDoErrorListener li) {
        monVideoOneDoErrorListener = li;
    }

    /**
     * 平均 获得 EditModeVideo 格式的视频 几帧;
     * 一般用在缩略图中;
     * <p>
     * 平均获取多少帧,内部根据时长,除以当前个数,得到帧的间隔,然后逐一读取;
     * <p>
     * 阻塞执行, 如果您视频分辨率720P一下, 读取10--15张,几乎可以直接执行;
     * <p>
     * <p>
     * 也可以读取一次后,放到全局变量中,然后每次直接使用, 省的每次都执行一遍;
     *
     * @param count 个数,建议10或15张;
     * @return
     */
    public ArrayList<Bitmap> getVideoFrames(int count) {
        if (LanSongFileUtil.fileExist(editVideoPath) && count > 0) {
            ArrayList<Bitmap> bitmapArray = new ArrayList<>();
            long decoderHandler;
            IntBuffer mGLRgbBuffer;

            decoderHandler = AVDecoder.decoderInit(editVideoPath);
            mGLRgbBuffer = IntBuffer.allocate(inputInfo.vWidth * inputInfo.vHeight);
            long seekUs = 0;
            long seekInterval = (long) (inputInfo.vDuration * 1000 * 1000) / count; //帧的间隔;
            for (int i = 0; i < count; i++) {
                mGLRgbBuffer.position(0);
                long ptsus = AVDecoder.decoderFrame(decoderHandler, seekUs, mGLRgbBuffer.array());
                seekUs += seekInterval;

                //得到帧, 转换为bitmap
                Bitmap stitchBmp = Bitmap.createBitmap(inputInfo.vWidth, inputInfo.vHeight, Bitmap.Config.ARGB_8888);
                stitchBmp.copyPixelsFromBuffer(mGLRgbBuffer);
                bitmapArray.add(stitchBmp);
                if (AVDecoder.decoderIsEnd(decoderHandler)) {  //如果到文件尾,则退出;
                    break;
                }
            }
            AVDecoder.decoderRelease(decoderHandler);
            return bitmapArray;
        } else {
            Log.e(TAG, "没有获取到 EditModeVideo 格式的视频." + (isConvertRunning ? "正在转换中...." : ""));
            return null;
        }
    }

    /**
     * 获取视频中的指定帧;
     *
     * @param ptsUs 指定时间; 如果时间超过总时长,则读取最后一帧画面;
     * @return
     */
    public Bitmap getVideoFrame(long ptsUs) {
        if (!LanSongFileUtil.fileExist(editVideoPath)) {
            Log.e(TAG, "EditModeVideo 视频还没有准备好");
            return null;
        }
        if (decoderHandler == 0) {
            decoderHandler = AVDecoder.decoderInit(editVideoPath);
            decoderRGBBuffer = IntBuffer.allocate(inputInfo.vWidth * inputInfo.vHeight);
        }

        decoderRGBBuffer.position(0);
        long framePtsUs = AVDecoder.decoderFrame(decoderHandler, ptsUs, decoderRGBBuffer.array());
        while (true) {
            if (framePtsUs >= ptsUs + 15000) {
                Bitmap bmp = Bitmap.createBitmap(inputInfo.vWidth, inputInfo.vHeight, Bitmap.Config.ARGB_8888);
                bmp.copyPixelsFromBuffer(decoderRGBBuffer);
                return bmp;
            }
            if (AVDecoder.decoderIsEnd(decoderHandler)) {
                break;
            }
            decoderRGBBuffer.position(0);
            framePtsUs = AVDecoder.decoderFrame(decoderHandler, -1, decoderRGBBuffer.array());
        }
        return null;
    }

    public void release() {
        if (decoderHandler != 0) {
            AVDecoder.decoderRelease(decoderHandler);
            decoderHandler = 0;
        }
        if (oneDo != null) {
            oneDo.stop();
            oneDo = null;
        }
    }
}
