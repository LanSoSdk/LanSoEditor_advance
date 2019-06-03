package com.example.advanceDemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.advanceDemo.utils.ConvertToEditModeDialog;
import com.example.advanceDemo.utils.DemoUtil;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.videoeditor.EditModeVideo;
import com.lansosdk.videoeditor.IRenderView;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.TextureRenderView;

import java.io.IOException;

/**
 * 视频播放
 */
public class ReversePlayerActivity extends Activity {

    private TextureRenderView textureView;
    String reverseVideoPath=null;
    private MediaPlayer mediaPlayer = null;
    private  SurfaceTexture surfaceTexture=null;
    private MediaInfo mediaInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reverse_play_layout);

        String videoPath = getIntent().getStringExtra("videopath");

        initView();

        mediaInfo = new MediaInfo(videoPath);
        if (!mediaInfo.prepare()) {
            DemoUtil.showDialog(ReversePlayerActivity.this,"抱歉,暂时不支持当前视频格式");
            this.finish();
        }
        checkEditModeDialog(videoPath);
    }

    private void checkEditModeDialog(final String file) {
        if (!EditModeVideo.checkEditModeVideo(file)) {
            //转换为编辑模式对话框.
            ConvertToEditModeDialog editMode = new ConvertToEditModeDialog(ReversePlayerActivity.this, file, new ConvertToEditModeDialog.onConvertToEditModeDialogListener() {
                @Override
                public void onConvertCompleted(String video) {
                    reverseVideoPath = video;
                    startPlayer();
                }
            });
            editMode.start();
        } else {
            reverseVideoPath = file;
        }

    }


    private void initView() {
        textureView = (TextureRenderView) findViewById(R.id.surface1);
        findViewById(R.id.id_player_testbtn).setOnClickListener(new View.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(View v) {

                                                                        if (playerHandler == null) {
                                                                            startReversePlay();
                                                                        } else {
                                                                            stopReversePlay();
                                                                        }
                                                                    }
                                                                }
        );
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

                surfaceTexture=surface;
                startPlayer();
            }
        });
    }

    public void startPlayer() {

        if(mediaPlayer!=null || surfaceTexture==null  || reverseVideoPath==null){
            return;
        }

        mediaPlayer = new MediaPlayer();
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                Toast.makeText(ReversePlayerActivity.this, "视频播放完毕!",
                        Toast.LENGTH_SHORT).show();
            }
        });

        try {
            mediaPlayer.setDataSource(reverseVideoPath);
            mediaPlayer.setSurface(new Surface(surfaceTexture));
            mediaPlayer.prepare();
            mediaPlayer.setLooping(true);
            textureView.setDispalyRatio(IRenderView.AR_ASPECT_FIT_PARENT);

            textureView.setVideoSize(mediaPlayer.getVideoWidth(),
                    mediaPlayer.getVideoHeight());
            textureView.requestLayout();
            mediaPlayer.start();

            startReversePlay();

        } catch (IOException e) {
            e.printStackTrace();
            DemoUtil.showDialog(this, "系统的MediaPlayer无法播放此视频, 请联系我们.");
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (playerHandler != null) {
            playerHandler.removeCallbacks(playerRunnable);
            playerHandler = null;
            playerRunnable = null;
        }

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }


    /**
     * 倒序播放
     * av reverse player
     */
    private void startReversePlay() {
        intervalMs = 1000 / 25;
        isReversing = true;
        currentSeekMs = (int) (mediaInfo.vDuration * 1000);
        playerHandler = new Handler();

        if (mediaPlayer != null) {
            mediaPlayer.setVolume(0.0f, 0.0f);
            mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(MediaPlayer mp) {
                    mp.pause();
                }
            });
        }
        playerRunnable = new Runnable() {
            @Override
            public void run() {
                currentSeekMs = currentSeekMs - intervalMs;
                if (currentSeekMs < 0) {
                    currentSeekMs = (int) (mediaInfo.vDuration * 1000);
                }
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(currentSeekMs);
                    if (playerHandler != null && isReversing) {
                        playerHandler.postDelayed(this, intervalMs);
                    }
                }
            }
        };
        playerHandler.postDelayed(playerRunnable, intervalMs);
    }

    public void stopReversePlay() {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(1.0f, 1.0f);
            mediaPlayer.setOnSeekCompleteListener(null);
            mediaPlayer.seekTo(0);
            mediaPlayer.start();
        }
        isReversing = false;
        if (playerHandler != null && playerRunnable != null) {
            playerHandler.removeCallbacks(playerRunnable);
        }
        playerHandler = null;
    }

    private int intervalMs = 1000 / 25;
    private int currentSeekMs = 0;

    private Handler playerHandler = null;
    private Runnable playerRunnable = null;
    private boolean isReversing = false;


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
