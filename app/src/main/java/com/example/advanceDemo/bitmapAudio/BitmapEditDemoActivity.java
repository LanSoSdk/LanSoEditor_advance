package com.example.advanceDemo.bitmapAudio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.example.advanceDemo.view.ImageTouchView;
import com.example.advanceDemo.view.StickerView;
import com.example.advanceDemo.view.TextStickerView;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.DrawPadUpdateMode;
import com.lansosdk.box.ViewLayer;
import com.lansosdk.box.ViewLayerRelativeLayout;
import com.lansosdk.box.onDrawPadOutFrameListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.example.advanceDemo.utils.CopyFileFromAssets;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.FilterLibrary;
import com.lansosdk.videoeditor.FilterLibrary.FilterAdjuster;
import com.lansosdk.videoeditor.FilterLibrary.OnLanSongFilterChosenListener;

import com.lansosdk.LanSongFilter.LanSongFilter;

/**
 * 图片编辑, 编辑后, 输出一张图片.
 */
public class BitmapEditDemoActivity extends Activity implements OnClickListener {
    private static final String TAG = "BitmapEditDemoActivity";
    boolean isDestorying = false; // 是否正在销毁, 因为销毁会停止DrawPad
    private Bitmap srcBmp = null;
    private DrawPadView drawPadView;
    private BitmapLayer bmpLayer = null;
    private FilterAdjuster filterAdjuster;
    private SeekBar adjusterFilter;
    private ViewLayer viewLayer = null;
    private ViewLayerRelativeLayout viewLayerRelativeLayout;
    private ImageTouchView imgeTouchView;
    private StickerView stickView;
    private TextStickerView textStickView;
    private ImageView ivShowImg;
    private int stickCnt = 2;
    private String strInputText = "蓝松文字演示";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bitmap2bitmap_layout);

        initView();

        String bmpPath = CopyFileFromAssets.copyAssets(getApplicationContext(), "t14.jpg");
        srcBmp = BitmapFactory.decodeFile(bmpPath);
    }

    @Override
    protected void onResume() {
        super.onResume();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                initDrawPad();
            }
        }, 100);
    }

    @Override
    protected void onPause() {
        super.onPause();
        drawPadView.stopDrawPad();
    }

    /**
     * Step1: 初始化DrawPad容器
     */
    private void initDrawPad() {
        // 设置为自动刷新模式, 帧率为25
        drawPadView.setUpdateMode(DrawPadUpdateMode.AUTO_FLUSH, 30);
        drawPadView.setDrawPadSize(srcBmp.getWidth(), srcBmp.getHeight(), new onDrawPadSizeChangedListener() {
            @Override
            public void onSizeChanged(int viewWidth, int viewHeight) {
                startDrawPad();
            }
        });
    }

    /**
     * Step2: 开始运行 Drawpad容器.
     */
    private void startDrawPad() {
        drawPadView.pauseDrawPad();
        if (drawPadView.startDrawPad()) {
            // 增加一个图片图层
            bmpLayer = drawPadView.addBitmapLayer(srcBmp);
            // 再增加一个UI图层, UI图层在图片图层上面.
            addViewLayer();
        }
        drawPadView.resumeDrawPad();
    }

    /**
     * 增加UI图层: ViewLayer
     */
    private void addViewLayer() {
        if (drawPadView != null && drawPadView.isRunning()) {
            viewLayer = drawPadView.addViewLayer();

            // 绑定
            ViewGroup.LayoutParams params = viewLayerRelativeLayout
                    .getLayoutParams();
            params.width = viewLayer.getPadWidth();
            params.height = viewLayer.getPadHeight();
            viewLayerRelativeLayout.setLayoutParams(params);


            setLayout(imgeTouchView, viewLayer.getPadWidth(), viewLayer.getPadHeight());

            setLayout(stickView, viewLayer.getPadWidth(), viewLayer.getPadHeight());

            setLayout(textStickView, viewLayer.getPadWidth(), viewLayer.getPadHeight());

            viewLayerRelativeLayout.bindViewLayer(viewLayer);
            viewLayerRelativeLayout.invalidate();
        }
    }

    private void setLayout(View v, int w, int h) {
        ViewGroup.LayoutParams params = v.getLayoutParams();
        params.width = w;
        params.height = h;
        v.setLayoutParams(params);
    }

    /**
     * 选择滤镜效果,
     */
    private void selectFilter() {
        if (drawPadView != null && drawPadView.isRunning()) {
            FilterLibrary.showDialog(this, new OnLanSongFilterChosenListener() {
                @Override
                public void onLanSongFilterChosenListener(
                        final LanSongFilter filter, String name) {

                    if (bmpLayer != null) {
                        bmpLayer.switchFilterTo(filter);
                        filterAdjuster = new FilterAdjuster(filter);
                        // 如果这个滤镜 可调, 显示可调节进度条.
                        findViewById(R.id.id_bmp2bmp_filter_seekbar).setVisibility(filterAdjuster.canAdjust() ? View.VISIBLE : View.GONE);
                    }
                }
            });
        }
    }

    private void initView() {
        drawPadView = (DrawPadView) findViewById(R.id.id_bmp2bmp_drawpadview);

        viewLayerRelativeLayout = (ViewLayerRelativeLayout) findViewById(R.id.id_bmp2bmp_gllayout);

        imgeTouchView = (ImageTouchView) findViewById(R.id.id_bmp2bmp_switcher);
        imgeTouchView.setActivity(BitmapEditDemoActivity.this);

        stickView = (StickerView) findViewById(R.id.id_bmp2bmp_stickview);
        textStickView = (TextStickerView) findViewById(R.id.id_bmp2bmp_textstickview);

        findViewById(R.id.id_bmp2bmp_export_btn).setOnClickListener(this);
        findViewById(R.id.id_bmp2bmp_btnfilter).setOnClickListener(this);
        findViewById(R.id.id_bmp2bmp_addstick).setOnClickListener(this);
        findViewById(R.id.id_bmp2bmp_addtext).setOnClickListener(this);

        // 滤镜的设置.
        adjusterFilter = (SeekBar) findViewById(R.id.id_bmp2bmp_filter_seekbar);
        adjusterFilter.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (filterAdjuster != null) {
                    filterAdjuster.adjust(progress);
                }
            }
        });
        ivShowImg = (ImageView) findViewById(R.id.id_bmp2bmp_showimg_iv);

        findViewById(R.id.id_bmp2bmp_showlayout).setVisibility(View.GONE);
        findViewById(R.id.id_bmp2bmp_showimg_btn).setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestorying = true;
        if (drawPadView != null) {
            drawPadView.stopDrawPad();
            drawPadView = null;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_bmp2bmp_btnfilter:
                selectFilter();
                break;
            case R.id.id_bmp2bmp_addstick:
                if (stickView != null) {
                    Bitmap bmp = null;
                    if (stickCnt == 2) {
                        bmp = BitmapFactory.decodeResource(getResources(),
                                R.drawable.stick2);
                    } else if (stickCnt == 3) {
                        bmp = BitmapFactory.decodeResource(getResources(),
                                R.drawable.stick3);
                    } else if (stickCnt == 4) {
                        bmp = BitmapFactory.decodeResource(getResources(),
                                R.drawable.stick4);
                    } else {
                        bmp = BitmapFactory.decodeResource(getResources(),
                                R.drawable.stick5);
                    }
                    stickCnt++;
                    stickView.addBitImage(bmp);
                }
                break;
            case R.id.id_bmp2bmp_addtext:
                showInputDialog();
                break;

            case R.id.id_bmp2bmp_export_btn:  //导出图片;
                stickView.disappearIconBorder();
                textStickView.disappearIconBorder();
                drawPadView.setOnDrawPadOutFrameListener(srcBmp.getWidth(),
                        srcBmp.getHeight(), 1, new onDrawPadOutFrameListener() {

                            @Override
                            public void onDrawPadOutFrame(DrawPad v, Object obj,
                                                          int type, long ptsUs) {
                                Bitmap bmp = (Bitmap) obj;
                                drawPadView.setOnDrawPadOutFrameListener(0, 0, 0,
                                        null); // 禁止再次提取图片.
                                ivShowImg.setImageBitmap(bmp);// 显示图片.
                                findViewById(R.id.id_bmp2bmp_showlayout)
                                        .setVisibility(View.VISIBLE);
                                // startShowOneBitmapActivity(DemoUtil.saveBitmap(bmp));//也可以保存,在另外地方显示,建议异步保存,因为耗时.
                            }
                        });
                break;
            case R.id.id_bmp2bmp_showimg_btn:
                findViewById(R.id.id_bmp2bmp_showlayout).setVisibility(View.GONE);
                ;
                break;
            default:
                break;
        }
    }

    private void showInputDialog() {
        final EditText etInput = new EditText(this);

        new AlertDialog.Builder(this).setTitle("请输入文字").setView(etInput)
                .setPositiveButton("确定", new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String input = etInput.getText().toString();
                        if (input != null && !input.equals("")) {
                            strInputText = input;
                            textStickView.setText(strInputText);
                        }
                    }
                }).show();
    }

    // private void startShowOneBitmapActivity(String pngPath)
    // {
    // if(pngPath!=null){
    // Intent intent=new
    // Intent(BitmapEditDemoActivity.this,ShowOneBitmapActivity.class);
    // intent.putExtra("pngPath", pngPath);
    // startActivity(intent);
    // }
    // }
}
