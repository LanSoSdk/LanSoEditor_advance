package com.example.advanceDemo.scene;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.advanceDemo.VideoPlayerActivity;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.GifLayer;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.videoeditor.AudioEditor;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.LanSongFileUtil;

/**
 * 演示: 使用DrawPad来实现 视频和图片的实时叠加.
 * <p>
 * 流程是: 先创建一个DrawPad,然后在视频播放过程中,从DrawPad中增加一个BitmapLayer,然后可以调节SeekBar来对Layer的每个
 * 参数进行调节.
 * <p>
 * 可以调节的有:平移,旋转,缩放,RGBA值,显示/不显示(闪烁)效果. 实际使用中, 可用这些属性来做些动画,比如平移+RGBA调节,呈现舒缓移除的效果.
 * 缓慢缩放呈现照片播放效果;旋转呈现欢快的炫酷效果等等.
 */

public class LayerLayoutDemoActivity extends Activity {
    private static final String TAG = "LayerLayoutDemo";

    private String videoPath;

    private DrawPadView drawPadView;
    private MediaPlayer mplayer = null;

    private VideoLayer videoLayer = null;
    private BitmapLayer bmpLayer = null;

    private String editTmpPath = null;
    private String dstPath = null;
    private LinearLayout playVideo;
    private MediaInfo mInfo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.videopicture_layout);

        videoPath = getIntent().getStringExtra("videopath");
        mInfo = new MediaInfo(videoPath);
        if (!mInfo.prepare()) {
            Toast.makeText(this, "传递过来的视频文件错误", Toast.LENGTH_SHORT).show();
            this.finish();
        }

        drawPadView = (DrawPadView) findViewById(R.id.id_videopicture_drawpadview);
        initView();

        // 在手机的默认路径下创建一个文件名,用来保存生成的视频文件,(在onDestroy中删除)
        editTmpPath = LanSongFileUtil.newMp4PathInBox();
        dstPath = LanSongFileUtil.newMp4PathInBox();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startPlayVideo();
            }
        }, 100);
    }

    private void startPlayVideo() {
        if (videoPath != null) {
            mplayer = new MediaPlayer();
            try {
                mplayer.setDataSource(videoPath);
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            finish();
            return;
        }
    }

    private void initDrawPad() {
        int padWidth = 640;
        int padHeight = 640;
        /**
         * 设置当前DrawPad的宽度和高度,并把宽度自动缩放到父view的宽度,然后等比例调整高度.
         */
        drawPadView.setDrawPadSize(padWidth, padHeight, new onDrawPadSizeChangedListener() {
            @Override
            public void onSizeChanged(int viewWidth, int viewHeight) {
                startDrawPad();
            }
        });
        drawPadView.setRealEncodeEnable(padWidth, padHeight, (int) mInfo.vFrameRate, editTmpPath);
    }

    /**
     * Step2: 开始运行 Drawpad
     */
    private void startDrawPad() {
        drawPadView.pauseDrawPad();

        if (!drawPadView.isRunning() && drawPadView.startDrawPad()) {
            BitmapLayer layer = drawPadView.addBitmapLayer(BitmapFactory
                    .decodeResource(getResources(), R.drawable.videobg));
            layer.setScaledValue(layer.getPadWidth(), layer.getPadHeight()); // 填充整个屏幕.

            videoLayer = drawPadView.addMainVideoLayer(mplayer.getVideoWidth(), mplayer.getVideoHeight(), null);
            if (videoLayer != null) {
                mplayer.setSurface(new Surface(videoLayer.getVideoTexture()));
                mplayer.start();

                videoLayer.setScale(0.8f);
                videoLayer.setPosition(videoLayer.getPositionX(),videoLayer.getLayerHeight() / 2.0f);

            }
            addBitmapLayer();

            addGifLayer();
            drawPadView.resumeDrawPad();
        }
    }

    private void addGifLayer() {
        if (drawPadView != null && drawPadView.isRunning()) {
            GifLayer gifLayer = drawPadView.addGifLayer(R.drawable.g06);

            // 把gif图层放到左下角 设置中心点的位置.
            // 整个容器的1/4出., 左下角的坐标是:
            int x = gifLayer.getPadWidth() / 4;
            int y = gifLayer.getPadHeight() * 3 / 4;
            gifLayer.setPosition(x, y);
        }
    }

    /**
     * 从DrawPad中得到一个BitmapLayer,填入要显示的图片,您实际可以是资源图片,也可以是png或jpg,或网络上的图片等,
     * 最后解码转换为统一的 Bitmap格式即可.
     */
    private void addBitmapLayer() {
        if (bmpLayer == null) {
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.pic2);

            bmpLayer = drawPadView.addBitmapLayer(bmp);

            // 增加后, 把图片放到右侧,设置图层的中心点位置.


            bmpLayer.setScaledValue(bmpLayer.getPadWidth() * 0.3f, bmpLayer.getPadHeight() * 0.3f);

            bmpLayer.setPosition(bmpLayer.getPadWidth() * 3.0f / 4.0f, bmpLayer.getPadHeight() * 3.0f / 4.0f);
            bmpLayer.setRotate(360 - 45); // 设置的角度是逆时针旋转的.

//			int layW = bmpLayer.getLayerWidth();
//			int layH = bmpLayer.getLayerHeight();
//			// 减掉上面的视频部分,还剩余的高度, 减去20是预留一部分空白.
//			int allH = bitmapLayer.getPadWidth() - videoLayer.getLayerHeight()
//					- 20;
//
//			/**
//			 * 划定一个区域, 把另一个要显示的尺寸放入到这个区域里,通过等比例计算,得到最后要缩放到的宽高.
//			 * 
//			 * 等比例计算后, 得到显示宽高.
//			 * 
//			 * @param padW  划定的最大要显示的宽度
//			 * @param padH 划定最大要显示的高度
//			 * @param w  要显示原来的宽度
//			 * @param h  要显示原来的高度
//			 * @return
//			 */
//			int inW = (int) AeLayer.getInsideWidth(bmpLayer.getPadWidth() / 2,
//					allH, layW, layH);
//			int inH = (int) AeLayer.getInsideHeight(
//					bmpLayer.getPadWidth() / 2, allH, layW, layH);
//
//			bmpLayer.setScaledValue(inW, inH);
//
//			int x = bitmapLayer.getPadWidth() - inW / 2;
//			int y = bitmapLayer.getPadHeight() - inH / 2;
//			bmpLayer.setPosition(x, y);
//			bmpLayer.setRotate(360 - 45); // 设置的角度是逆时针旋转的.
        }
    }

    /**
     * Step3: cancel drawPad
     */
    private void stopDrawPad() {
        if (drawPadView != null && drawPadView.isRunning()) {

            drawPadView.stopDrawPad();
            toastStop();

            if (LanSongFileUtil.fileExist(editTmpPath)) {
                dstPath = AudioEditor.mergeAudioNoCheck(videoPath, editTmpPath, true);
                playVideo.setVisibility(View.VISIBLE);
            } else {
                Log.e(TAG, " player completion, but file:" + editTmpPath
                        + " is not exist!!!");
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
            drawPadView.stopDrawPad();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LanSongFileUtil.deleteFile(dstPath);
        LanSongFileUtil.deleteFile(editTmpPath);
    }

    private void initView() {
        TextView tvHint = (TextView) findViewById(R.id.id_videopicture_hint);
        tvHint.setText(R.string.cuoluo_layout);

        playVideo = (LinearLayout) findViewById(R.id.id_videopicture_saveplay);
        playVideo.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (LanSongFileUtil.fileExist(dstPath)) {
                    Intent intent = new Intent(LayerLayoutDemoActivity.this, VideoPlayerActivity.class);
                    intent.putExtra("videopath", dstPath);
                    startActivity(intent);
                } else {
                    Toast.makeText(LayerLayoutDemoActivity.this, "目标文件不存在", Toast.LENGTH_SHORT).show();
                }
            }
        });
        playVideo.setVisibility(View.GONE);
    }

    private void toastStop() {
        Toast.makeText(getApplicationContext(), "录制已停止!!", Toast.LENGTH_SHORT)
                .show();
        Log.i(TAG, "录制已停止!!");
    }
}
