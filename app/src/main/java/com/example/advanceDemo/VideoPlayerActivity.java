package com.example.advanceDemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.advanceDemo.utils.DemoUtil;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.oldVersion.IRenderView2;
import com.lansosdk.videoeditor.oldVersion.TextureRenderView2;
import com.lansosdk.videoplayer.OnLSOPlayeFrameUpdateListener;
import com.lansosdk.videoplayer.VideoPlayer;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * 视频播放
 */
public class VideoPlayerActivity extends Activity {

    private static final boolean VERBOSE = true;
    private static final String TAG = "VideoPlayerActivity";
    String videoPath = null;
    private TextureRenderView2 textureView;
    private MediaPlayer mediaPlayer = null;
    private boolean isSupport = false;
    private int screenWidth, screenHeight;
    private MediaInfo mediaInfo;
    TextView tvScreen;
    TextView tvProgress;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_layout);
        textureView = (TextureRenderView2) findViewById(R.id.surface1);

        videoPath = getIntent().getStringExtra("videopath");
        if(videoPath==null){
            videoPath= DemoApplication.getInstance().currentEditVideo;
        }

        initView();
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

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
                if (isSupport) {
                    play(new Surface(surface)); // 采用系统本身的MediaPlayer播放
                }
            }
        });
    }

    private void showHintDialog() {

        new AlertDialog.Builder(this).setTitle("提示")
                .setMessage("抱歉,暂时不支持当前视频格式")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();

    }

    public void play(Surface surface) {

        if (videoPath == null)
            return;

        String str=tvScreen.getText().toString();
        tvScreen.setText(String.format("%s;播放器:MediaPlayer ", str));

        mediaPlayer = new MediaPlayer();
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                Toast.makeText(VideoPlayerActivity.this, "视频播放完毕!",
                        Toast.LENGTH_SHORT).show();
            }
        });

        try {
            mediaPlayer.setDataSource(videoPath);
            mediaPlayer.setSurface(surface);
            mediaPlayer.prepare();
            mediaPlayer.setLooping(true);
            if(screenWidth>= mediaInfo.getWidth() && screenHeight>= mediaInfo.getHeight()){
                textureView.setDisplayRatio(IRenderView2.AR_ASPECT_FIT_PARENT);
            }else{
                textureView.setDisplayRatio(IRenderView2.AR_ASPECT_WRAP_CONTENT);
            }
            textureView.setVideoSize(mediaPlayer.getVideoWidth(),
                    mediaPlayer.getVideoHeight());
            textureView.requestLayout();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
            DemoUtil.showDialog(this,"系统的MediaPlayer无法播放此视频, 请联系我们.");
        }
    }
    boolean actPaused=false;




    private float convertTime(long time){
        int time3=(int)(time*100/1000000);
        return (float)time3/100f;
    }

    private void initView()
    {
        tvScreen = (TextView) findViewById(R.id.id_player_screen_hint);
        tvProgress=(TextView)findViewById(R.id.id_player_current_time);


        DisplayMetrics dm = new DisplayMetrics();
        dm = getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;

        String   textInfo = "屏幕：";
        textInfo += String.valueOf(screenWidth);
        textInfo += "x";
        textInfo += String.valueOf(screenHeight);

        mediaInfo = new MediaInfo(videoPath);

        if (!mediaInfo.prepare()) {
            showHintDialog();
            isSupport = false;
        } else {
            Log.i(TAG, "info:\n" + mediaInfo.toString());
            isSupport = true;
            textInfo += ";视频：";
            textInfo += String.valueOf(mediaInfo.getWidth());
            textInfo += "x";
            textInfo += String.valueOf(mediaInfo.getHeight());

            textInfo += ";时长:";
            textInfo += String.valueOf(mediaInfo.vDuration);
        }
        tvScreen.setText(textInfo);
    }

    @Override
    protected void onResume() {
        super.onResume();
        actPaused=false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        actPaused=true;
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
