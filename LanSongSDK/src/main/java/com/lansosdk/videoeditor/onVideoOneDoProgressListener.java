package com.lansosdk.videoeditor;

public interface onVideoOneDoProgressListener {

    /**
     * 进度百分比, 最小是0.0,最大是1.0; 如果运行结束, 会回调{@link onVideoOneDoCompletedListener},
     * 只有调用Complete才是正式完成回调.
     *
     * @param v
     * @param percent
     */
    void onProgress(VideoOneDo v, float percent);
}
