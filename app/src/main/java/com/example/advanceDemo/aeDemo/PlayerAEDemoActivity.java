package com.example.advanceDemo.aeDemo;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.TextureView;

import com.lansoeditor.advanceDemo.R;
import com.lansosdk.videoeditor.CopyFileFromAssets;

import java.io.IOException;

public class PlayerAEDemoActivity  extends Activity{
    MediaPlayer mediaPlayer;
    SurfaceTexture  mediaSurface;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.ae_play_aedemofile_layout);
        TextureView textureView=(TextureView)findViewById(R.id.id_aedemo_textureview);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                mediaSurface=surfaceTexture;
                playVideo();
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
    private void playVideo(){
        String video= CopyFileFromAssets.copyAssets(getApplicationContext(),"aeDemo.mp4");
        if(mediaPlayer!=null){
            mediaPlayer.release();
            mediaPlayer=null;
        }
        mediaPlayer=new MediaPlayer();
        try {
            mediaPlayer.setDataSource(video);
            mediaPlayer.setLooping(true);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.setSurface(new Surface(mediaSurface));
                    mediaPlayer.start();
                }
            });
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
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
