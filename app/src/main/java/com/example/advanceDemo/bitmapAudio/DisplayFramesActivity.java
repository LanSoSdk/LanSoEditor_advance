package com.example.advanceDemo.bitmapAudio;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.ExtractVideoFrame;
import com.lansosdk.box.onExtractVideoFrameCompletedListener;
import com.lansosdk.box.onExtractVideoFrameProgressListener;
import com.example.advanceDemo.utils.BitmapLruCache;
import com.lansosdk.videoeditor.MediaInfo;
import com.example.advanceDemo.utils.MemoryDiskCache;

/**
 * 快速获取视频的每一帧, 注意: 在运行此类的时候, 请不要同时运行MediaPlayer或我们的DrawPad类,
 * 因为在高通410,610等低端处理器中, 他们是共用同一个硬件资源,会冲突;但各种旗舰机 不会出现类似问题
 */
public class DisplayFramesActivity extends Activity {

    public static final int FRAME_TYPE_25 = 1;
    public static final int FRAME_TYPE_60 = 2;
    public static final int FRAME_TYPE_ALL = 3;
    private static final String TAG = "DisplayAllFrame";
    String videoPath = null;
    int videoDuration;
    boolean isRuned = false;
    MediaInfo mInfo;

    private boolean isExecuting = false;
    private ExtractVideoFrame mExtractFrame;

    private MemoryDiskCache mDiskCache;
    private BitmapLruCache mLruCache;
    private int count = 0;
    private GridView listView;
    private ImageAdapter mImageAdapter;
    private int mTpye = FRAME_TYPE_25;

    // TextView tvHint;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        videoPath = getIntent().getStringExtra("videopath");

        mTpye = getIntent().getExtras().getInt("TYPE");

        setContentView(R.layout.video_all_frame_grid);

        // tvHint=(TextView)findViewById(R.id.video_frame_tv);

        listView = (GridView) findViewById(R.id.gridview);
        mImageAdapter = new ImageAdapter();
        listView.setAdapter(mImageAdapter);

    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        startGetFrames();
    }

    /**
     * 从这里开始演示.
     */
    private void startGetFrames() {
        if (isExecuting)
            return;

        mInfo = new MediaInfo(videoPath);
        if (!mInfo.prepare() || !mInfo.isHaveVideo()) {
            return;
        }
        isExecuting = true;

        /**
         * 初始化.
         */
        mExtractFrame = new ExtractVideoFrame(DisplayFramesActivity.this,videoPath);
        if (mInfo.vWidth * mInfo.vHeight > 960 * 540) {
            mExtractFrame.setBitmapWH(mInfo.vWidth / 2, mInfo.vHeight / 2); // 视频分辨率过大,则缩小一倍.
        }

        if (mTpye == FRAME_TYPE_25) {
            // 25帧, 先检查用 内存释放够,如果不够,再用SD卡来缓存.
            mExtractFrame.setExtract25Frame();
            long desireSize = mExtractFrame.getBitmapHeight()* mExtractFrame.getBitmapWidth() * 4 * 25;
            long cachesize = BitmapLruCache.getMaxCacheSize();
            if (desireSize > cachesize) {
                mDiskCache = new MemoryDiskCache(getApplication());
                Log.i(TAG, "写入到 硬盘.....");
            } else {
                mLruCache = new BitmapLruCache();
                Log.i(TAG, "写入到 memory....");
            }
        } else if (mTpye == FRAME_TYPE_60) {
            mExtractFrame.setExtract60Frame();
            mDiskCache = new MemoryDiskCache(getApplication());

        } else { // 不设置,则默认是全部解码
            // 全部解码,则用DiskLruCache
            mDiskCache = new MemoryDiskCache(getApplication());
        }
        //设置处理完成监听.
        mExtractFrame.setOnExtractCompletedListener(new onExtractVideoFrameCompletedListener() {

            @Override
            public void onCompleted(ExtractVideoFrame v) {
                mImageAdapter.notifyDataSetChanged();
            }
        });
        //设置处理进度监听.
        mExtractFrame.setOnExtractProgressListener(new onExtractVideoFrameProgressListener() {

            /**
             * 当前帧的画面回调,, ptsUS:当前帧的时间戳,单位微秒.
             */
            @Override
            public void onExtractBitmap(Bitmap bmp, long ptsUS) {
                if (mDiskCache != null) {
                    mDiskCache.pushBitmap(bmp);
                    count++;
                    mImageAdapter.notifyDataSetChanged();
                } else if (mLruCache != null) {
                    mLruCache.pushBitmap(bmp);
                    count++;
                    mImageAdapter.notifyDataSetChanged();
                }
            }
        });
//        mExtractFrame.setExtractSomeFrame();  // 提取自定义的多少帧;
        /**
         * 开始执行. 或者你可以从指定地方开始解码.
         * mExtractFrame.start(10*1000*1000);则从视频的10秒处开始提取.
         */
        mExtractFrame.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mExtractFrame != null) {
            mExtractFrame.stop();
            mExtractFrame = null;
        }

        if (mLruCache != null) {
            mLruCache.clear();
            mLruCache = null;
        }

        if (mDiskCache != null) {
            mDiskCache.clear();
            mDiskCache = null;
        }
    }

    // ------------------------------------
    public class ImageAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return count;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            final ViewHolder holder;
            View view = convertView;

            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.frame_grid_item,
                        parent, false);
                holder = new ViewHolder();
                holder.imageView = (ImageView) view.findViewById(R.id.image);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            if (mDiskCache != null) {
                Bitmap bmp = mDiskCache.getBitmap(position);
                if (bmp != null) {
                    holder.imageView.setImageBitmap(bmp);
                }
            } else if (mLruCache != null) {
                Bitmap bmp = mLruCache.getBitmap(position);
                if (bmp != null) {
                    holder.imageView.setImageBitmap(bmp);
                }
            }
            return view;
        }

        class ViewHolder {
            ImageView imageView;
        }
    }
}
/************************************************************************************************************************************************************

 一下是最简单的回调;

 ExtractVideoFrame mExtractFrame;
 private void testExtract()
 {


 mExtractFrame = new ExtractVideoFrame(ListMainActivity.this, SDCARD.file("d1.mp4"));

 //        mExtractFrame.setExtractIntervalWithTimeUs(1*1000*1000);  //1秒钟一帧;  //ok
 //        mExtractFrame.setExtractInterval(40); //间隔40帧提取一帧;
 //        mExtractFrame.setExtractSomeFrame(40);  //一共提取40帧;

 mExtractFrame.setOnExtractCompletedListener(new onExtractVideoFrameCompletedListener() {
@Override public void onLanSongSDKCompleted(ExtractVideoFrame v) {

}
});
 // 设置处理进度监听.
 mExtractFrame.setOnExtractProgressListener(new onExtractVideoFrameProgressListener() {

 //当前帧的画面回调,, ptsUS:当前帧的时间戳,单位微秒.
 @Override public void onExtractBitmap(Bitmap bmp, long ptsUS) {
 Log.e("TAG", "bmp is : "+bmp.getWidth()+ bmp.getHeight()+ " pts Us:"+ptsUS);
 }
 });

 //开始执行. 或者你可以从指定地方开始解码.
 // mExtractFrame.start(10*1000*1000);则从视频的10秒处开始提取.
 mExtractFrame.start();
 }
 */