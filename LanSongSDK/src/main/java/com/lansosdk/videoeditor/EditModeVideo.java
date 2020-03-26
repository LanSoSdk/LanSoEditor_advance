package com.lansosdk.videoeditor;

import android.content.Context;
import android.graphics.Bitmap;

import com.lansosdk.box.FrameInfo;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;

import java.nio.IntBuffer;
import java.util.ArrayList;

/**
 * 我们定义了一种视频格式,命名为"EditModeVideo", 这种转换的视频格式,会很快的找到视频中的每一帧, 像翻书一样,方便定位, 提取帧,倒序播放等;
 * 功能:
 * 2, 导出
 * 3, 可以检测当前视频 是否是 "EditModeVideo"的视频格式;
 *
 * 区别:
 * "EditModeVideo"格式的视频, 和正常的mp4唯一区别是: 比他大一些;, 也是正常的mp4视频,可以直接分享到微信, 上传到服务器等等;
 *
 * @author Administrator
 */
public class EditModeVideo {

    protected String inputPath;
    protected MediaInfo inputInfo;
    protected VideoOneDo2 oneDo;
    protected Context ctx;
    protected boolean isConvertRunning;
    protected boolean isInputEditMode; // 输入的是否已经是
    protected String editVideoPath;
    private long decoderHandler = 0;
    private IntBuffer decoderRGBBuffer;
    private OnLanSongSDKProgressListener onLanSongSDKProgressListener;
    private OnLanSongSDKCompletedListener onLanSongSDKCompletedListener = null;
    private OnLanSongSDKErrorListener onLanSongSDKErrorListener = null;

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
            LSOLog.e("获取蓝松视频失败,因为:"
                    + (isConvertRunning ? "正在转换中..." : "未知,请联系我们!"));
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
            LSOLog.e( "EditModeVideo 视频还没有准备好");
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
            oneDo.release();
            oneDo = null;
        }
    }
    //-------------
    @Deprecated
    public void export() {
        if (inputInfo.isHaveVideo() && isInputEditMode&& !isConvertRunning) {
            synchronized (this) {
                isConvertRunning = true;
                try {
                    oneDo = new VideoOneDo2(ctx, editVideoPath);
                    oneDo.setOnVideoOneDoCompletedListener(new OnLanSongSDKCompletedListener() {

                        @Override
                        public void onLanSongSDKCompleted(String dstVideo) {
                            if (onLanSongSDKCompletedListener != null) {
                                onLanSongSDKCompletedListener.onLanSongSDKCompleted(dstVideo);
                            }
                            isConvertRunning = false;
                            oneDo.release();
                            oneDo = null;
                        }
                    });
                    oneDo.setOnVideoOneDoProgressListener(onLanSongSDKProgressListener);
                    oneDo.setOnVideoOneDoErrorListener(onLanSongSDKErrorListener);
                    oneDo.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            isConvertRunning = false;
            if (onLanSongSDKCompletedListener != null) {
                onLanSongSDKCompletedListener.onLanSongSDKCompleted(inputPath);
            }
        }
    }
    @Deprecated
    public void setOnVideoOneDoProgressListener(OnLanSongSDKProgressListener li) {
        onLanSongSDKProgressListener = li;
    }
    @Deprecated
    public void setOnVideoOneDoCompletedListener(OnLanSongSDKCompletedListener li) {
        onLanSongSDKCompletedListener = li;
    }
    @Deprecated
    public void setOnVideoOneDoErrorListener(OnLanSongSDKErrorListener li) {
        onLanSongSDKErrorListener = li;
    }

    /**
     * 已废弃.
     * 请使用 VideoOneDo2. 在转换为编码模式的同时, 直接得到视频帧;
     */
    @Deprecated
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
            LSOLog.e("没有获取到 EditModeVideo 格式的视频." + (isConvertRunning ? "正在转换中...." : ""));
            return null;
        }
    }
}
