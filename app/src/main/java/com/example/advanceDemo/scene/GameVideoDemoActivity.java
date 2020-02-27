package com.example.advanceDemo.scene;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.advanceDemo.utils.DemoUtil;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.DrawPadView2;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.VideoEditor;

import java.io.IOException;
import java.util.ArrayList;

public class GameVideoDemoActivity extends Activity implements OnClickListener {
    private static final String TAG = "GameVideoDemoActivity";
    private String srcVideoPath;
    private DrawPadView2 drawPadView;
    private MediaPlayer mplayer = null;
    private VideoLayer mainVideoLayer = null;
    private MediaInfo mediaInfo = null;
    private ArrayList<String> videoList=new ArrayList<>();

    private TextView tvProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_video_demo_layout);

        srcVideoPath = getIntent().getStringExtra("videopath");
        mediaInfo = new MediaInfo(srcVideoPath);
        if (!mediaInfo.prepare()) {
            Log.e(TAG, " video path is error.finish\n");
            finish();
        }
        drawPadView = (DrawPadView2) findViewById(R.id.id_gamevideo_drawpadview);
        initView();

    }
    @Override
    protected void onResume() {
        super.onResume();
        drawPadView.setOnViewAvailable(new DrawPadView2.onViewAvailable() {
            @Override
            public void viewAvailable(DrawPadView2 v) {
                startPlayVideo();
            }
        });
    }

    private void startPlayVideo() {
        mplayer = new MediaPlayer();
        mplayer.setLooping(true);
        try {
            mplayer.setDataSource(srcVideoPath);
            mplayer.setOnPreparedListener(new OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    initDrawPad();
                }
            });
            mplayer.setOnCompletionListener(new OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopDrawPad();
                }
            });
            mplayer.prepareAsync();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //初始化容器
    private void initDrawPad() {
            int padWidth = 640;
            int padHeight = 640;
            drawPadView.setRecordParams(padWidth, padHeight, 2 * 1024 * 1024, (int)mediaInfo.vFrameRate);

            drawPadView.setOnDrawPadRecordProgressListener(new onDrawPadProgressListener() {
                @Override
                public void onProgress(DrawPad v, long currentTimeUs) {
                    tvProgress.setText(""+currentTimeUs);
                }
            });
            drawPadView.setDrawPadSize(padWidth, padHeight, new onDrawPadSizeChangedListener() {
                @Override
                public void onSizeChanged(int viewWidth, int viewHeight) {
                    startDrawPad();
                }
            });
    }

     //开始容器线程, 并增加一个视频图层
    private void startDrawPad() {
        drawPadView.pausePreview();
        if (drawPadView.setupDrawPad())  //建立容器
        {
            mainVideoLayer = drawPadView.addMainVideoLayer(mplayer.getVideoWidth(), mplayer.getVideoHeight(), null);
            if (mainVideoLayer != null) {
                mplayer.setSurface(new Surface(mainVideoLayer.getVideoTexture()));
            }
            drawPadView.startPreview();  //开始预览
            mplayer.start();
        }
    }
    /**
     *
     */
    private void stopDrawPad() {
        if (drawPadView != null && drawPadView.isRunning()) {
            String path=drawPadView.releaseDrawPad();
            if(MediaInfo.isHaveVideo(path)){
                videoList.add(path);
            }
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (mplayer != null) {
            mplayer.stop();
            mplayer.release();
            mplayer = null;
        }
        if (drawPadView != null) {
            drawPadView.releaseDrawPad();
        }
    }
    // --------------------------------------一下为UI界面-----------------------------------------------------------
    private float xpos = 0, ypos = 0;
    private void initView() {

        tvProgress=(TextView)findViewById(R.id.id_gamevideo_tvrecord);

        findViewById(R.id.id_gamevideo_startrecord).setOnClickListener(this);
        findViewById(R.id.id_gamevideo_stoprecord).setOnClickListener(this);

        findViewById(R.id.id_gamevideo_startpreview).setOnClickListener(this);
        findViewById(R.id.id_gamevideo_pausepreview).setOnClickListener(this);
        findViewById(R.id.id_gamevideo_resumepreview).setOnClickListener(this);
        findViewById(R.id.id_gamevideo_videoconcat).setOnClickListener(this);

        SeekBar seekBar=findViewById(R.id.id_gamevideo_seekbar1);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {

                if (mainVideoLayer != null) {
                    mainVideoLayer.setScale((float) progress / 100f, (float) progress / 100f);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        seekBar=findViewById(R.id.id_gamevideo_seekbar2);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {

                if (mainVideoLayer != null) {
                    xpos += 10;
                    if (xpos > drawPadView.getDrawPadWidth())
                        xpos = 0;
                    mainVideoLayer.setPosition(xpos, mainVideoLayer.getPositionY());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }


    @Override
    public void onClick(View v) {
        if(drawPadView==null){
            return;
        }
        switch (v.getId()) {
            case R.id.id_gamevideo_startrecord:  //开始录制
                if(drawPadView.isRecording()==false) {
                    mplayer.setLooping(false);
                    drawPadView.startRecord();
                }
                break;
            case R.id.id_gamevideo_stoprecord:  //停止录制
                    if(drawPadView.isRecording()){
                        mplayer.setLooping(true);
                        String recordFile=drawPadView.stopRecord();


                         if(MediaInfo.isHaveVideo(recordFile)){
                             videoList.add(recordFile);
                         }else{
                             DemoUtil.showToast(getApplicationContext(),"录制失败, 无法获取到一个视频");
                         }
                         MediaInfo.checkFile(recordFile);
                    }
                break;
            case R.id.id_gamevideo_startpreview: //开始预览
                   if(drawPadView.isRunning()==false) {
                       startDrawPad();
                   }
                break;
            case R.id.id_gamevideo_pausepreview: //停止预览
                if (mplayer.isPlaying()) {
                    mplayer.pause();
                    drawPadView.pausePreview();
                }
                break;

            case R.id.id_gamevideo_resumepreview:  //恢复预览
                    if(mplayer.isPlaying()==false){
                        mplayer.start();
                        drawPadView.resumePreview();
                    }
                break;
            case R.id.id_gamevideo_videoconcat: //视频拼接
                if(videoList.size()>0){
                    stopDrawPad();  //先停止容器; 也可以不停止;

                    if(videoList.size()==1){
                        DemoUtil.startPlayDstVideo(GameVideoDemoActivity.this,videoList.get(0));
                    }else{
                        VideoEditor editor=new VideoEditor();
                        String[] command = new String[videoList.size()];
                        for (int i = 0; i < videoList.size(); i++) {
                            command[i] = (String) videoList.get(i);
                        }
                        String  path=editor.executeConcatMP4(command);
                        DemoUtil.startPlayDstVideo(GameVideoDemoActivity.this,path);
                    }
                }else{
                    DemoUtil.showToast(getApplication(),"videoList size is 0; 无法拼接;");
                }
                break;
            default:
                break;
        }
    }
}
