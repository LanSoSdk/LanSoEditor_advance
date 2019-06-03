package com.example.advanceDemo.aeDemo;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import com.example.advanceDemo.VideoPlayerActivity;
import com.example.advanceDemo.utils.DemoUtil;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.videoeditor.CopyFileFromAssets;
import com.lansosdk.videoeditor.IRenderView;
import com.lansosdk.videoeditor.TextureRenderView;

import java.io.IOException;

public class PlayerAEDemoActivity  extends Activity{
    MediaPlayer mediaPlayer;
    SurfaceTexture  mediaSurface;
    TextureRenderView textureView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.ae_play_aedemofile_layout);
        textureView=(TextureRenderView)findViewById(R.id.id_aedemo_textureview);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                mediaSurface=surfaceTexture;
                play(new Surface(mediaSurface));
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        });
    }
    public void play(Surface surface) {


        mediaPlayer = new MediaPlayer();
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                Toast.makeText(PlayerAEDemoActivity.this, "视频播放完毕!",Toast.LENGTH_SHORT).show();
            }
        });

        try {
            String video= CopyFileFromAssets.copyAssets(getApplicationContext(),"aeDemo.mp4");
            mediaPlayer.setDataSource(video);
            mediaPlayer.setSurface(surface);
            mediaPlayer.prepare();
            textureView.setDispalyRatio(IRenderView.AR_ASPECT_FIT_PARENT);

            textureView.setVideoSize(mediaPlayer.getVideoWidth(),
                    mediaPlayer.getVideoHeight());
            textureView.requestLayout();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
            DemoUtil.showDialog(this,"系统的MediaPlayer无法播放此视频, 请联系我们.");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mediaPlayer!=null){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer=null;
        }
    }
}
