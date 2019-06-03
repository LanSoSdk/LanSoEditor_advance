package com.example.advanceDemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.example.advanceDemo.view.RangeSeekBar;
import com.example.advanceDemo.view.RangeSeekBar.OnRangeSeekBarChangeListener;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.OnLanSongSDKCompletedListener;
import com.lansosdk.box.OnLanSongSDKProgressListener;
import com.lansosdk.videoeditor.CopyFileFromAssets;
import com.lansosdk.videoeditor.FilterLibrary;
import com.lansosdk.videoeditor.FilterLibrary.OnLanSongFilterChosenListener;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.LanSongFileUtil;
import com.lansosdk.videoeditor.VideoOneDo;

import java.sql.Date;
import java.text.SimpleDateFormat;

import com.lansosdk.LanSongFilter.LanSongFilter;

/**
 * 视频常见功能演示.
 */
public class VideoOneDODemoActivity extends Activity implements
        OnClickListener, OnSeekBarChangeListener {

    private final static String TAG = "VideoOneDODemoActivity";
    private boolean isMusicEnable;
    private boolean isScaleEnable;
    private boolean isCompressEnable;
    private boolean isCutDurationEnable;
    private boolean isFilterEnable;
    private boolean isCropEnable;
    private boolean isAddLogoEnable;
    private boolean isAddWordEnable;
    private boolean isMusicMix = false;
    private CheckBox ckxMusic;
    private TextView tvScale, tvCompress;

    private SeekBar scaleSeek, compressSeek;

    // --------------各个参数.
    private long startTimeUs, cutDurationUs;
    private LanSongFilter mFilter = null;
    private float scaleFactor = 1.0f, compressFactor = 1.0f;

    private boolean isRunning = false;
    private VideoOneDo videoOneDo;
    private Context mContext;

    private MediaInfo mInfo;

    private String videoPath;
    private String dstPath; // 生成的目标文件.
    private ViewGroup rangeGroup;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_one_do_process_layout);
        mContext = getApplicationContext();

        videoPath = getIntent().getStringExtra("videopath");

        mInfo = new MediaInfo(videoPath);
        if (!mInfo.prepare()) {
            Toast.makeText(mContext, "当前文件错误!", Toast.LENGTH_LONG).show();
            finish();
        }
        initUI();
    }

    /**
     * 开始处理.
     */
    @SuppressLint("SimpleDateFormat")
    private void startDrawPadProcess() {
        if (isRunning) {
            return;
        }
        videoOneDo = new VideoOneDo(getApplicationContext(), videoPath);

        videoOneDo.setOnVideoOneDoProgressListener(new OnLanSongSDKProgressListener() {

            @Override
            public void onLanSongSDKProgress(long ptsUs, int percent) {
                if (progressDialog != null) {
                    progressDialog.setMessage("正在处理中..." +percent+ " %");
                }
            }
        });
        videoOneDo.setOnVideoOneDoCompletedListener(new OnLanSongSDKCompletedListener() {

            @Override
            public void onLanSongSDKCompleted(String dstVideo) {
                dstPath = dstVideo;
                isRunning = false;
                cancelProgressDialog();
                showHintDialog("视频执行完毕, 点击开始预览结果.", true);
            }
        });

        if (isMusicEnable) { // 增加音乐
            String music = CopyFileFromAssets.copyAssets(mContext,
                    "summer10s.mp3");
            videoOneDo.setBackGroundMusic(music, isMusicMix, 0.8f);
        }

        if (isScaleEnable) { // 是否缩放.
            videoOneDo.setScaleWidth((int) (mInfo.getWidth() * scaleFactor),
                    (int) (mInfo.getHeight() * scaleFactor));
        }
        if (isCutDurationEnable) // 是否时长剪切
        {
            videoOneDo.setStartPostion(startTimeUs);
            videoOneDo.setCutDuration(cutDurationUs);
        }

        if (isFilterEnable && mFilter != null) { // 是否增加滤镜.
            videoOneDo.setFilter(mFilter);

            // 因为滤镜对象只能被执行一次, 如再次执行,则需要重新创建对象.故这里使用后等于null,并设置UI为no
            mFilter = null;
            isFilterEnable = false;
            switchBackImage(R.id.id_oendo_filter_switch, isFilterEnable);
        }

        if (isCropEnable) { // 是否画面裁剪
            // //这里为了代码清晰, 取视频中间的2/3画面为裁剪, 实际您可以任意裁剪.
            int startX = mInfo.getWidth() / 6; // 中间2/3,则裁剪掉1/3, 则左上角是一半,为1/6;
            int startY = mInfo.getHeight() / 6;

            int cropW = mInfo.getWidth() * 2 / 3;
            int cropH = mInfo.getHeight() * 2 / 3;

            videoOneDo.setCropRect(startX, startY, cropW, cropH);
        }

        if (isAddLogoEnable) { // 增加logo
            Bitmap bmp = BitmapFactory.decodeResource(getResources(),R.drawable.ls_logo);
            videoOneDo.setLogo(bmp, VideoOneDo.LOGO_POSITION_RIGHT_TOP);
        }

        if (isAddWordEnable) { // 增加文字.

            SimpleDateFormat formatter = new SimpleDateFormat( "yyyy年MM月dd日 HH:mm:ss ");
            Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
            String str = formatter.format(curDate);

            videoOneDo.setText("蓝松SDK演示文字:" + str);
        }
        // 开始执行.
        if (videoOneDo.start()) {
            isRunning = true;
            showProgressDialog();
        } else {
            showHintDialog("视频执行错误!,请查看打印信息或联系我们.", false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isRunning && videoOneDo != null) {
            videoOneDo.stop();
            videoOneDo.release();
        }
        cancelProgressDialog();
    }

    private void initUI() {
        findViewById(R.id.id_onedo_addmusic_switch).setOnClickListener(this);
        findViewById(R.id.id_onedo_scale_switch).setOnClickListener(this);
        findViewById(R.id.id_onedo_compress_switch).setOnClickListener(this);
        findViewById(R.id.id_onedo_cutduration_switch).setOnClickListener(this);

        findViewById(R.id.id_oendo_filter_switch).setOnClickListener(this);
        findViewById(R.id.id_onedo_crop_switch).setOnClickListener(this);
        findViewById(R.id.id_onedo_addlogo_switch).setOnClickListener(this);
        findViewById(R.id.id_onedo_addword_switch).setOnClickListener(this);
        findViewById(R.id.id_onedo_start_process).setOnClickListener(this);

        ckxMusic = (CheckBox) findViewById(R.id.id_onedo_addmusic_ckx);
        ckxMusic.setEnabled(isMusicEnable);

        scaleSeek = (SeekBar) findViewById(R.id.id_oendo_scale_seekbar);
        scaleSeek.setOnSeekBarChangeListener(this);

        compressSeek = (SeekBar) findViewById(R.id.id_oendo_compress_seekbar);
        compressSeek.setOnSeekBarChangeListener(this);

        scaleSeek.setEnabled(false);
        compressSeek.setEnabled(false);

        tvScale = (TextView) findViewById(R.id.id_onedo_scale_tv);
        tvCompress = (TextView) findViewById(R.id.id_onedo_compress_tv);

        ckxMusic.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                isMusicMix = isChecked;
            }
        });

        rangeGroup = (ViewGroup) findViewById(R.id.id_onedo_cutduration_vg);

        RangeSeekBar<Float> seekBar = new RangeSeekBar<Float>(0.0f,
                mInfo.vDuration, this);
        seekBar.setOnRangeSeekBarChangeListener(new OnRangeSeekBarChangeListener<Float>() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar,
                                                    Float minValue, Float maxValue) {

                if (maxValue > (minValue + 2.0f)) { // 这里临时显示最小是2秒,
                    startTimeUs = (long) (minValue * 1000000);
                    cutDurationUs = ((long) (maxValue * 1000000) - startTimeUs);
                }
                Log.i(TAG, "selected new range values: MIN=" + minValue
                        + ", MAX=" + maxValue);
            }
        });
        rangeGroup.addView(seekBar);
        rangeGroup.setEnabled(false);
    }

    @Override
    public void onClick(View v) {

        int id = v.getId();
        switch (id) {
            case R.id.id_onedo_addmusic_switch:
                isMusicEnable = !isMusicEnable;
                ckxMusic.setEnabled(isMusicEnable);
                switchBackImage(id, isMusicEnable);
                break;
            case R.id.id_onedo_scale_switch:
                isScaleEnable = !isScaleEnable;
                switchBackImage(id, isScaleEnable);
                scaleSeek.setEnabled(isScaleEnable);
                break;
            case R.id.id_onedo_compress_switch:
                isCompressEnable = !isCompressEnable;
                switchBackImage(id, isCompressEnable);
                compressSeek.setEnabled(isCompressEnable);
                break;
            case R.id.id_onedo_cutduration_switch:
                isCutDurationEnable = !isCutDurationEnable;
                switchBackImage(id, isCutDurationEnable);
                rangeGroup.setEnabled(isCutDurationEnable);
                break;
            case R.id.id_oendo_filter_switch:
                isFilterEnable = !isFilterEnable;
                switchBackImage(id, isFilterEnable);
                if (isFilterEnable) {
                    selectFilter();
                }
                break;
            case R.id.id_onedo_crop_switch:
                isCropEnable = !isCropEnable;
                switchBackImage(id, isCropEnable);
                break;
            case R.id.id_onedo_addlogo_switch:
                isAddLogoEnable = !isAddLogoEnable;
                switchBackImage(id, isAddLogoEnable);
                break;
            case R.id.id_onedo_addword_switch:
                isAddWordEnable = !isAddWordEnable;
                switchBackImage(id, isAddWordEnable);
                break;
            case R.id.id_onedo_start_process: // 开始执行.
                startDrawPadProcess();
                break;
            default:
                break;
        }
    }

    private void switchBackImage(int id, boolean enable) {
        if (enable) {
            findViewById(id).setBackgroundResource(R.drawable.switch_on);
        } else {
            findViewById(id).setBackgroundResource(R.drawable.switch_off);
        }
    }

    /**
     * 选择滤镜效果,
     */
    private void selectFilter() {
        FilterLibrary.showDialog(this, new OnLanSongFilterChosenListener() {

            @Override
            public void onLanSongFilterChosenListener(
                    final LanSongFilter filter, String name) {
                if (filter != null) {
                    mFilter = filter;
                }
            }
        });
    }

    // -------------------seekbar-----------------------------------------
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        if (seekBar == scaleSeek) {
            scaleFactor = (float) progress / 100f;
            tvScale.setText("宽高缩放:" + scaleFactor);
            if (scaleFactor < 0.2f) {
                scaleFactor = 0.2f; // 缩放太小没效果.
            }
        } else if (seekBar == compressSeek) {
            Log.i(TAG, "compressSeek:" + progress);
            compressFactor = (float) progress / 100f;
            tvCompress.setText("压缩比:" + compressFactor);
            if (compressFactor < 0.2f) {
                compressFactor = 0.2f; // 太小则容易导致画面不清晰
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    // /------------------dialog
    private void showHintDialog(String str, final boolean isPlay) {
        new AlertDialog.Builder(this).setTitle("提示").setMessage(str)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (isPlay) {
                            playVideo();
                        }
                    }
                }).show();
    }

    private void playVideo() {
        if (LanSongFileUtil.fileExist(dstPath)) {
            Intent intent = new Intent(this, VideoPlayerActivity.class);
            intent.putExtra("videopath", dstPath);
            startActivity(intent);
        } else {
            Toast.makeText(this, "目标文件不存在", Toast.LENGTH_SHORT).show();
        }
    }

    private void showProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("正在处理中...");
        progressDialog.setCancelable(true);
        progressDialog.show();
    }

    private void cancelProgressDialog() {
        if (progressDialog != null) {
            progressDialog.cancel();
            progressDialog = null;
        }
    }
    /**
     * if(mProgressDialog!=null){
     * mProgressDialog.setMessage("正在处理中..."+String.valueOf(percent)+"%"); }
     */
}

/**
 举例:






 */