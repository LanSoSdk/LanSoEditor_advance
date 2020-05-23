package com.example.advanceDemo.scene;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.advanceDemo.utils.ConvertToEditModeDialog;
import com.example.advanceDemo.view.ThumbnailView;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.videoeditor.EditModeVideo;
import com.lansosdk.videoeditor.IRenderView;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.TextureRenderView;
import com.warkiz.widget.IndicatorSeekBar;

import java.io.IOException;
import java.util.ArrayList;

public class VideoSeekActivity extends Activity {

    protected static final String TAG = "VideoSeekActivity";
    String videoPath;
    MediaInfo mediaInfo;
    IndicatorSeekBar seekBar;
    CheckBox ckxseekplayer;
    ImageView ivImage;
    private TextureRenderView textureView;
    private ThumbnailView thumbnailView;
    private LinearLayout layoutThumbnail;
    private MediaPlayer mediaPlayer = null;
    private int screenWidth, screenHeight;
    private EditModeVideo editVideoMode;
    private  int seekTimeMs=-1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.videoplay_seek_layout);

        videoPath = getIntent().getStringExtra("videopath");
        mediaInfo = new MediaInfo(videoPath);
        if (mediaInfo.prepare()) {
            initView();

            DisplayMetrics dm = new DisplayMetrics();
            dm = getResources().getDisplayMetrics();
            screenWidth = dm.widthPixels;
            screenHeight = dm.heightPixels;

            editVideoMode = new EditModeVideo(getApplication(), videoPath);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    initThumbs();
                }
            }, 300);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        isPaused = true;
        loopHandle.removeCallbacks(loopRunnable);

        if (editVideoMode != null) {
            editVideoMode.release();
            editVideoMode = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    private void initView() {
        layoutThumbnail = (LinearLayout) findViewById(R.id.id_videoseek_thumbnail_layout);
        thumbnailView = (ThumbnailView) findViewById(R.id.id_videoseek_thumbnailview);

        textureView = (TextureRenderView) findViewById(R.id.id_videoseek_texturerenderview);
        ckxseekplayer = (CheckBox) findViewById(R.id.id_videoseek_seekplayer_ckbox);

        ckxseekplayer.setChecked(true);
        findViewById(R.id.id_videoseek_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VideoSeekActivity.this.finish();
            }
        });

        textureView.setSurfaceTextureListener(new SurfaceTextureListener() {

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                                    int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                                  int width, int height) {
                if (mediaInfo.isHaveVideo()) {
                    play(new Surface(surface));
                }
            }
        });
        seekBar = (IndicatorSeekBar) findViewById(R.id.id_videoseek_indicatorbar);
        seekBar.setOnSeekChangeListener(new IndicatorSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(IndicatorSeekBar seekBar, int progress, float progressFloat, boolean
                    fromUserTouch) {

                if (fromUserTouch) {
                    //拿到图片
                    if (ivImage == null) {
                        LinearLayout view = (LinearLayout) seekBar.getIndicator().getmContentView();
                        ivImage = (ImageView) view.findViewById(R.id.id_videoseek_thumb_imageview);
                    }

                    //seek时间;
                    long seekMs = (long) ((progress / 100f) * mediaInfo.vDuration * 1000);

                    seekTimeMs=(int) seekMs;
                    if (ckxseekplayer.isChecked() && mediaPlayer != null) {
                        mediaPlayer.seekTo((int) seekMs);
                        mediaPlayer.start();
                    }

                    Bitmap bmp = editVideoMode.getVideoFrame(seekTimeMs * 1000);
                    if (bmp != null && ivImage != null) {
                        ivImage.setImageBitmap(bmp);
                    }
                }
            }

            @Override
            public void onSectionChanged(IndicatorSeekBar seekBar, int thumbPosOnTick, String textBelowTick, boolean
                    fromUserTouch) {

            }

            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar, int thumbPosOnTick) {
                if(mediaPlayer!=null){
                    mediaPlayer.pause();
                    mediaPlayer.setVolume(0.0f, 0.0f);
                }

            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
                if(mediaPlayer!=null){
                    mediaPlayer.setVolume(1.0f, 1.0f);
                    if(seekTimeMs>=0&& !ckxseekplayer.isChecked()){
                        mediaPlayer.seekTo(seekTimeMs);
                        seekTimeMs=-1;
                    }
                    mediaPlayer.start();
                }
            }
        });
    }

    /**
     * 播放视频
     *
     * @param surface
     */
    public void play(Surface surface) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        mediaPlayer = new MediaPlayer();
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
//				Toast.makeText(VideoSeekActivity.this, "视频播放完毕!",Toast.LENGTH_SHORT).show();
            }
        });

        try {
            mediaPlayer.setDataSource(videoPath);
            mediaPlayer.setSurface(surface);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    // 因为是竖屏.宽度小于高度.
                    if (screenWidth > mediaInfo.getWidth()) {
                        textureView.setDispalyRatio(IRenderView.AR_ASPECT_WRAP_CONTENT);
                    } else { // 大于屏幕的宽度
                        textureView.setDispalyRatio(IRenderView.AR_ASPECT_FIT_PARENT);
                    }

                    textureView.setVideoSize(mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight());
                    textureView.requestLayout();
                    mediaPlayer.start();
                    loopHandle.post(loopRunnable);
                }
            });
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    ArrayList<Bitmap> bitmaps;
    /**
     * 初始化缩略图
     */
    private void initThumbs() {

        new GetThumbsAsyncTask().execute();
    }
    private class GetThumbsAsyncTask extends AsyncTask<Object, Object, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected synchronized Boolean doInBackground(Object... params) {
             bitmaps = editVideoMode.getVideoFrames(15);  //如果是编辑模式,则很快!
            return null;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if(bitmaps!=null && bitmaps.size()>0){
                int thumbnailWidth = layoutThumbnail.getWidth() / bitmaps.size();
                for (int i = 0; i < bitmaps.size(); i++) {
                    ImageView imageView = new ImageView(VideoSeekActivity.this);
                    imageView.setLayoutParams(new ViewGroup.LayoutParams(thumbnailWidth, ViewGroup.LayoutParams.MATCH_PARENT));
                    imageView.setBackgroundColor(Color.parseColor("#666666"));
                    imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                    imageView.setImageBitmap(bitmaps.get(i));  //直接填入;
                    layoutThumbnail.addView(imageView);
                }
            }
        }
    }
    // --------------------------------实时获取当前播放器的播放位置-----测试使用.
    private boolean isPaused = false;
    private Handler loopHandle = new Handler();
    private Runnable loopRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && seekBar != null) {
                long timeMs = mediaPlayer.getCurrentPosition();
                float percent = (float) timeMs / (float) mediaPlayer.getDuration();
                seekBar.setProgress(percent * 100);
            }
            if (loopHandle != null && !isPaused) {
                loopHandle.postDelayed(loopRunnable, 100);
            }
        }
    };

}
	
	
