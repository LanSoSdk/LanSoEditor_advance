package com.example.advanceDemo.bitmapAudio;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.ExtractVideoFrame;
import com.lansosdk.box.onExtractVideoFrameCompletedListener;
import com.lansosdk.box.onExtractVideoFrameProgressListener;
import com.lansosdk.videoeditor.AVDecoder;
import com.lansosdk.videoeditor.MediaInfo;

import java.nio.IntBuffer;

/**
 * 快速获取视频的每一帧, 注意: 在运行此类的时候, 请不要同时运行MediaPlayer或我们的DrawPad类,
 * 因为在高通410,610等低端处理器中, 他们是共用同一个硬件资源,会冲突;但各种旗舰机 不会出现类似问题.
 */
public class ExtractVideoFrameDemoActivity extends Activity {

    private static final String TAG = "ExtractVideoFrameDemoActivity";
    static int bmtcnt = 0;
    String videoPath = null;
    ProgressDialog mProgressDialog;
    int videoDuration;
    boolean isRuned = false;
    MediaInfo mInfo;
    TextView tvProgressHint;
    TextView tvHint;
    private boolean isExecuting = false;
    private ExtractVideoFrame mExtractFrame;
    private long startTime;
    private int frameCount = 0;
    private int seekCount = 0;

    /**
     * 临时为了获取一个bitmap图片,临时测试. 可以用来作为视频的封面.
     *
     * @param src
     */
    public static void testGetFirstOnekey(String src) {
        long decoderHandler = 0;
        IntBuffer mGLRgbBuffer;
        MediaInfo info = new MediaInfo(src);
        if (info.prepare()) {
            decoderHandler = AVDecoder.decoderInit(src);
            if (decoderHandler != 0) {
                mGLRgbBuffer = IntBuffer.allocate(info.vWidth * info.vHeight);
                mGLRgbBuffer.position(0);
                AVDecoder
                        .decoderFrame(decoderHandler, -1, mGLRgbBuffer.array());
                AVDecoder.decoderRelease(decoderHandler);

                // 转换为bitmap
                Bitmap stitchBmp = Bitmap.createBitmap(info.vWidth,
                        info.vHeight, Bitmap.Config.ARGB_8888);
                stitchBmp.copyPixelsFromBuffer(mGLRgbBuffer);
//                /**
//                 * 这里是保存到文件, 仅仅用来测试, 实际您可以不用保存到文件.
//                 */
//                saveBitmap(stitchBmp); // 您可以修改下, 然后返回bitmap

                // 这里得到的图像在mGLRgbBuffer中, 可以用来返回一张图片.
                decoderHandler = 0;
            }
        } else {
            Log.e("TAG", "get first one key error!");
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        videoPath = getIntent().getStringExtra("videopath");
        setContentView(R.layout.extract_video_frame_layout);

        initUI();
    }

    /**
     * 从这里开始演示.
     */
    private void testExtractVideoFrame() {
        if (isExecuting)
            return;

        isExecuting = true;

        mInfo = new MediaInfo(videoPath);
        if (!mInfo.prepare() || !mInfo.isHaveVideo()) {
            return;
        }
        /**
         * 初始化.
         */
        mExtractFrame = new ExtractVideoFrame(ExtractVideoFrameDemoActivity.this, videoPath);
        /**
         * 设置在获取图片的时候, 可以指定图片的宽高, 指定后, 视频帧画面会被缩放到指定的宽高.
         *
         * 不调用则内部认为, 如果视频大于等于1280x720则自动缩小一倍.不然使用原来大小. 大部分的使用场景是:作为预览横条用, 建议缩放,
         * 这样可减少app内存
         *
         * @param width
         *            缩放宽度
         * @param height
         *            缩放高度
         */
        if (mInfo.vWidth * mInfo.vHeight > 960 * 540) {
            mExtractFrame.setBitmapWH(mInfo.vWidth / 2, mInfo.vHeight / 2);
        }
        //
        /**
         * 设置提取间隔. 取bitmap的间隔, 即解码好后, 每隔几帧返回一个bitmap,
         * 用在需要列出视频一部分,但不需要全部的场合,比如预览缩略图.
         *
         * 如果设置时间,则从开始时间后, 查当前解码好几个图片,然后做间隔返回bitmap
         *
         * 比如设置间隔是3, 则bitmap在第 0个返回, 第4 8,12,16个返回.
         *
         * 可以用MediaInfo或FrameInfo得到当前视频中总共有多少帧,用FrameInfo可以得到每一帧的时间戳.
         *
         * @param frames
         */
        // mExtractFrame.setExtractInterval(5);


        /**
         *  设置提取多少帧
         */
        mExtractFrame.setExtractSomeFrame(30);
        /**
         * 设置处理完成监听.
         */
        mExtractFrame.setOnExtractCompletedListener(new onExtractVideoFrameCompletedListener() {

                    @Override
                    public void onCompleted(ExtractVideoFrame v) {

                        long timeOut = System.currentTimeMillis() - startTime; // 单位毫秒.
                        float leftF = ((float) timeOut / 1000);
                        float b = (float) (Math.round(leftF * 10)) / 10; // 保留一位小数.

                        if (mInfo != null) {
                            String str = "解码结束:\n" + "解码的视频总帧数:" + frameCount
                                    + "\n" + "解码耗时:" + b + "(秒)" + "\n" + "\n"
                                    + "\n" + "视频宽高:" + mInfo.vWidth + " x "
                                    + mInfo.vHeight + "\n" + "解码后图片缩放宽高:"
                                    + mExtractFrame.getBitmapWidth() + " x "
                                    + mExtractFrame.getBitmapHeight() + "\n";

                            Log.i("TIME", "解码结束::" + str);
                            tvProgressHint.setText("Completed" + str);
                            isExecuting = false;
                            frameCount = 0;
                        }

                    }
                });
        /**
         * 设置处理进度监听.
         */
        mExtractFrame.setOnExtractProgressListener(new onExtractVideoFrameProgressListener() {

                    /**
                     * 当前帧的画面回调,, ptsUS:当前帧的时间戳,单位微秒. 拿到图片后,建议放到ArrayList中,
                     * 不要直接在这里处理.
                     */
                    @Override
                    public void onExtractBitmap(Bitmap bmp, long ptsUS) {
                        frameCount++;
                        String hint = " 当前是第" + frameCount + "帧" + "\n"
                                + "当前帧的时间戳是:" + String.valueOf(ptsUS) + "微秒";

                        tvProgressHint.setText(hint);

                        // saveBitmap(bmp); //测试使用.
                        // if(bmp!=null && bmp.isRecycled()){
                        // bmp.recycle();
                        // bmp=null;
                        // }
                        // if(ptsUS>15*1000*1000){ // 你可以在指定的时间段停止.
                        // mExtractFrame.stop(); //这里演示在15秒的时候停止.
                        // }
                    }
                });
        frameCount = 0;
        /**
         * 开始执行. 或者你可以从指定地方开始解码.
         * mExtractFrame.start(10*1000*1000);则从视频的10秒处开始提取.
         */
        mExtractFrame.start();

        /**
         * 您可以一帧一帧的读取, seekPause是指:seek到指定帧后, 调用回调就暂停. 单位是us, 微秒. 单独读取的话, 把这里打开
         * 如果您频繁的读取, 建议直接一次性读取完毕,放到sd卡里,然后用的时候, 从sd卡中读取.
         */
        // mExtractFrame.seekPause(seekCount*1000*1000);
        startTime = System.currentTimeMillis();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mExtractFrame != null) {
            mExtractFrame.release();
            mExtractFrame = null;
        }
    }

    private void initUI() {
        tvHint = (TextView) findViewById(R.id.id_extract_frame_hint);
        tvHint.setText(R.string.extract_video_frame_hint);
        tvProgressHint = (TextView) findViewById(R.id.id_extract_frame_progress_hint);

        findViewById(R.id.id_extract_frame_btn).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        testExtractVideoFrame();
                    }
                });
        findViewById(R.id.id_extract_frame_btn2).setVisibility(View.GONE);
        /**
         * 一下是测试,读取指定视频位置的图片, 读取速度比上面慢一些, 如果您频繁的读取, 建议直接一次性读取完毕,放到sd卡里,然后用的时候,
         * 从sd卡中读取.
         */
        // findViewById(R.id.id_extract_frame_btn2).setOnClickListener(new
        // OnClickListener() {
        //
        // @Override
        // public void onClick(View v) {
        // // TODO Auto-generated method stub
        // if(mExtractFrame!=null){
        // seekCount++;
        // mExtractFrame.seekPause(seekCount*1000*1000);
        // }
        // }
        // });
    }
}