package com.example.advanceDemo.layerDemo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.example.advanceDemo.utils.DemoLog;
import com.example.advanceDemo.utils.DemoProgressDialog;
import com.example.advanceDemo.utils.DemoUtil;
import com.example.advanceDemo.view.FilterDemoAdapter;
import com.example.advanceDemo.view.HorizontalListView;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.LanSongFilter.LanSongIF1977Filter;
import com.lansosdk.box.BitmapGetFilters;
import com.lansosdk.box.DrawPadUpdateMode;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.box.onGetFiltersOutFrameListener;
import com.lansosdk.videoeditor.AVDecoder;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.FilterLibrary;
import com.lansosdk.videoeditor.FilterLibrary.FilterAdjuster;
import com.lansosdk.videoeditor.FilterLibrary.OnLanSongFilterChosenListener;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.LanSongFileUtil;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;

import com.lansosdk.LanSongFilter.LanSongFilter;
import com.lansosdk.LanSongFilter.LanSongIFAmaroFilter;
import com.lansosdk.LanSongFilter.LanSongIFBrannanFilter;
import com.lansosdk.LanSongFilter.LanSongIFEarlybirdFilter;
import com.lansosdk.LanSongFilter.LanSongIFHefeFilter;
import com.lansosdk.LanSongFilter.LanSongIFHudsonFilter;
import com.lansosdk.LanSongFilter.LanSongIFInkwellFilter;
import com.lansosdk.LanSongFilter.LanSongIFLomofiFilter;
import com.lansosdk.LanSongFilter.LanSongIFLordKelvinFilter;
import com.lansosdk.LanSongFilter.LanSongIFNashvilleFilter;
import com.lansosdk.LanSongFilter.LanSongIFRiseFilter;
import com.lansosdk.LanSongFilter.LanSongIFSierraFilter;
import com.lansosdk.LanSongFilter.LanSongIFSutroFilter;
import com.lansosdk.LanSongFilter.LanSongIFToasterFilter;
import com.lansosdk.LanSongFilter.LanSongIFValenciaFilter;
import com.lansosdk.LanSongFilter.LanSongIFWaldenFilter;
import com.lansosdk.LanSongFilter.LanSongIFXproIIFilter;
import com.lansosdk.LanSongFilter.LanSongBeautyAdvanceFilter;
import com.lansosdk.videoeditor.VideoOneDo2;

/**
 * 滤镜的操作
 */
public class Demo3LayerFilterActivity extends Activity {
    private static final String TAG = "Demo3LayerFilterActivity";
    boolean isDestorying = false; // 是否正在销毁, 因为销毁会停止DrawPad
    private String mVideoPath;
    private DrawPadView drawPadView;
    private MediaPlayer mediaPlayer = null;
    private VideoLayer videoLayer = null;
    private MediaInfo mediaInfo;
    private SeekBar skbarFilterAdjuster;
    private String dstPath = null;
    private FilterDemoAdapter listAdapter;
    private HorizontalListView listFilterView;
    private ArrayList<LanSongFilter> filters = new ArrayList<>();
    private int filterIndex = 0;

    private FilterAdjuster mFilterAdjuster;

    // -------------------------------------------------后台执行.
    private VideoOneDo2 videoOneDo;

    private void createFilters() {
        filters.add(new LanSongFilter("无"));
        filters.add(new LanSongBeautyAdvanceFilter("美颜"));
        filters.add(new LanSongIFAmaroFilter(getApplicationContext(), "1AMARO")); //苦味
        filters.add(new LanSongIFRiseFilter(getApplicationContext(), "2RISE")); //玫瑰
        filters.add(new LanSongIFHudsonFilter(getApplicationContext(), "3HUDSON"));  //天蓝
        filters.add(new LanSongIFXproIIFilter(getApplicationContext(), "4XPROII"));  //甘菊
        filters.add(new LanSongIFSierraFilter(getApplicationContext(), "5SIERRA")); //常青树
        filters.add(new LanSongIFLomofiFilter(getApplicationContext(), "6LOMOFI")); //湛蓝
        filters.add(new LanSongIFEarlybirdFilter(getApplicationContext(), "7EARLYBIRD")); //早起
        filters.add(new LanSongIFSutroFilter(getApplicationContext(), "8SUTRO")); //枫树
        filters.add(new LanSongIFToasterFilter(getApplicationContext(), "9TOASTER"));  //收获
        filters.add(new LanSongIFBrannanFilter(getApplicationContext(), "10BRANNAN"));//布兰南
        filters.add(new LanSongIFInkwellFilter(getApplicationContext(), "11INKWELL"));  //黑白
        filters.add(new LanSongIFWaldenFilter(getApplicationContext(), "12WALDEN"));  //华尔兹
        filters.add(new LanSongIFHefeFilter(getApplicationContext(), "13HEFE"));  //黄昏
        filters.add(new LanSongIFValenciaFilter(getApplicationContext(), "14VALENCIA"));  //零点
        filters.add(new LanSongIFNashvilleFilter(getApplicationContext(), "15NASHVILLE"));  //乳酪
        filters.add(new LanSongIFLordKelvinFilter(getApplicationContext(), "16LORDKELVIN"));//金黄
        filters.add(new LanSongIF1977Filter(getApplicationContext(), "17if1977"));  //粉红
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.filter_layer_demo_layout);

        initView();

        createFilters();

        mVideoPath = getIntent().getStringExtra("videopath");
        dstPath = LanSongFileUtil.newMp4PathInBox();
        mediaInfo = new MediaInfo(mVideoPath);

        if (mediaInfo.prepare()) {
            new GetBitmapFiltersTask(mVideoPath).execute();  //开启给图片增加滤镜;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startPlayVideo();
                }
            }, 100);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (videoOneDo != null) {
            videoOneDo.release();
            videoOneDo = null;
        }
    }

    private void startPlayVideo() {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(mVideoPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.setOnPreparedListener(new OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                initDrawPad(mp.getVideoWidth(), mp.getVideoHeight());
            }
        });
        mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                if (drawPadView != null && drawPadView.isRunning()) {
                    drawPadView.stopDrawPad();
                }
            }
        });
        mediaPlayer.prepareAsync();
    }

    /**
     * 第一步:初始化容器;
     */
    private void initDrawPad(int w, int h) {
        drawPadView.setUpdateMode(DrawPadUpdateMode.AUTO_FLUSH, 25);
        drawPadView.setDrawPadSize(w, h, new onDrawPadSizeChangedListener() {
            @Override
            public void onSizeChanged(int viewWidth, int viewHeight) {
                startDrawPad();
            }
        });
    }

    /**
     * 第二步:开始运行
     */
    private void startDrawPad() {

        if (drawPadView.startDrawPad()) {
            videoLayer = drawPadView.addVideoLayer(mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight(), null);
            if (videoLayer != null) {
                mediaPlayer.setSurface(new Surface(videoLayer.getVideoTexture()));
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            }
        }
    }

    private void initView() {
        findViewById(R.id.id_filterLayer_demo_next).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                drawPadView.stopDrawPad();
                filterExecute();
            }
        });
        listFilterView = (HorizontalListView) findViewById(R.id.id_filterlayer_filterlist);
        listFilterView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                if (videoLayer != null) { //切换滤镜
                    LanSongFilter filter = filters.get(arg2);
                    videoLayer.switchFilterTo(filter);
                    filterIndex = arg2;
                }
            }
        });

        drawPadView = (DrawPadView) findViewById(R.id.id_filterLayer_demo_view);
        skbarFilterAdjuster = (SeekBar) findViewById(R.id.id_filter_seek1);
        skbarFilterAdjuster.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar,
                                          int progress, boolean fromUser) {
                if (mFilterAdjuster != null) {
                    mFilterAdjuster.adjust(progress);
                }
            }
        });
        skbarFilterAdjuster.setMax(100);
        findViewById(R.id.id_filter_select).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFilter();
            }
        });
    }

    // -----------------获取第一张图片, 并获取所有图片滤镜的异步执行------------------------

    /**
     * 选择滤镜效果,
     */
    private void selectFilter() {
        FilterLibrary.showDialog(this, new OnLanSongFilterChosenListener() {

            @Override
            public void onLanSongFilterChosenListener(
                    final LanSongFilter filter, String name) {
                if (videoLayer != null) {
                    videoLayer.switchFilterTo(filter);
                    mFilterAdjuster = new FilterAdjuster(filter);
                    // 如果这个滤镜 可调, 显示可调节进度条.
                    findViewById(R.id.id_filter_seek1).setVisibility(mFilterAdjuster.canAdjust() ? View.VISIBLE
                            : View.GONE);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        isDestorying = true;
        if (drawPadView != null) {
            drawPadView.stopDrawPad();
            drawPadView = null;
        }

        LanSongFileUtil.deleteFile(dstPath);
    }

    /**
     * 获取第一帧, 根据第一帧去获取所有滤镜.
     */
    public boolean getFirstFrame(String src) {
        long decoderHandler = 0;
        IntBuffer mGLRgbBuffer;
        MediaInfo info = new MediaInfo(src);
        if (info.prepare()) {
            decoderHandler = AVDecoder.decoderInit(src);
            if (decoderHandler != 0) {
                mGLRgbBuffer = IntBuffer.allocate(info.getWidth() * info.getHeight());
                mGLRgbBuffer.position(0);
                AVDecoder.decoderFrame(decoderHandler, -1, mGLRgbBuffer.array());
                AVDecoder.decoderRelease(decoderHandler);
                // 转换为bitmap
                Bitmap bmp = Bitmap.createBitmap(info.getWidth(), info.getHeight(),
                        Bitmap.Config.ARGB_8888);
                bmp.copyPixelsFromBuffer(mGLRgbBuffer);
                decoderHandler = 0;

                // 拿到图片, 去获取多个滤镜.
                getBitmapFilters(bmp, info.vRotateAngle);

                return true;
            }
        }
        return false;
    }


    private int bitmapIndex = 0;

    /**
     * 获取一张图片的所有滤镜效果;
     */
    private void getBitmapFilters(Bitmap bmp, float angle) {

        BitmapGetFilters getFilter = new BitmapGetFilters(getApplicationContext(), bmp, filters);
        // 如果图片太大了,则把滤镜后的图片缩小一倍输出.
        if (bmp.getWidth() * bmp.getHeight() > 480 * 480) {
            getFilter.setScaleWH(bmp.getWidth() / 2, bmp.getHeight() / 2);
        }
        getFilter.setRorate(angle);
        getFilter.setDrawpadOutFrameListener(new onGetFiltersOutFrameListener() {
            @Override
            public void onOutFrame(BitmapGetFilters v, Object obj) {
                Bitmap bmp2 = (Bitmap) obj;
                bitmaps.add(new NameBitmap(filters.get(bitmapIndex).getFilterName(), bmp2));
                bitmapIndex++;
            }
        });
        getFilter.start();// 开始线程.
        getFilter.waitForFinish();// 等待执行完毕, 您也可以不用等待,用[完成监听]来判断是否结束.
    }

    /**
     * 后台处理
     */
    private void filterExecute() {

        try {
            videoOneDo = new VideoOneDo2(getApplicationContext(), mVideoPath);
            videoOneDo.addFilter(filters.get(filterIndex));
            videoOneDo.setOnVideoOneDoProgressListener(new OnLanSongSDKProgressListener() {
                @Override
                public void onLanSongSDKProgress(long ptsUs, int percent) {
                    DemoProgressDialog.showPercent(Demo3LayerFilterActivity.this,percent);
                }

            });
            videoOneDo.setOnVideoOneDoCompletedListener(new OnLanSongSDKCompletedListener() {

                @Override
                public void onLanSongSDKCompleted(String dstVideo) {
                    DemoProgressDialog.releaseDialog();

                    dstPath = dstVideo;
                    videoOneDo.release();
                    videoOneDo = null;

                    if (LanSongFileUtil.fileExist(dstVideo)) {
                        DemoUtil.startPreviewVideo(Demo3LayerFilterActivity.this, dstVideo);
                    } else {
                        DemoUtil.showDialog(Demo3LayerFilterActivity.this, "生成的文件错误,请联系我们");
                    }
                }
            });
            videoOneDo.start();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public class GetBitmapFiltersTask extends AsyncTask<Object, Object, Boolean> {
        private String video;

        public GetBitmapFiltersTask(String videoPath) {
            video = videoPath;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected synchronized Boolean doInBackground(Object... params) {
            getFirstFrame(video);
            return null;
        }

        @Override
        protected void onPostExecute(Boolean result) {

            listAdapter = new FilterDemoAdapter(Demo3LayerFilterActivity.this, bitmaps);
            listAdapter.notifyDataSetChanged();
            listFilterView.setAdapter(listAdapter);
        }
    }

    private ArrayList<NameBitmap> bitmaps = new ArrayList<>();

    public class NameBitmap {

        public String name;
        public Bitmap bitmap;

        public NameBitmap(String name, Bitmap bmp) {
            this.name = name;
            this.bitmap = bmp;
        }
    }
}
