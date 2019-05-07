package com.example.advanceDemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;
import com.example.advanceDemo.aeDemo.AERecordFileHintActivity;
import com.example.advanceDemo.scene.GameVideoDemoActivity;
import com.example.advanceDemo.utils.ConvertToEditModeDialog;
import com.example.advanceDemo.utils.DemoUtil;
import com.example.advanceDemo.utils.FileExplorerActivity;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.LanSoEditorBox;
import com.lansosdk.videoeditor.EditModeVideo;
import com.lansosdk.videoeditor.LanSoEditor;
import com.lansosdk.videoeditor.LanSongFileUtil;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.VideoEditor;

import java.io.File;
import java.util.Calendar;

import static com.lansosdk.videoeditor.CopyFileFromAssets.copyAssets;

public class ListMainActivity extends Activity implements OnClickListener {

    private static final String TAG = "ListMainActivity";
    private static final boolean VERBOSE = false;
    private final static int SELECT_FILE_REQUEST_CODE = 10;
    int permissionCnt = 0;
    private TextView tvVideoPath;
    private boolean isPermissionOk = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//		Thread.setDefaultUncaughtExceptionHandler(new LanSoSdkCrashHandler());
        setContentView(R.layout.activity_main);
        /**
         * 初始化SDK
         */
        LanSoEditor.initSDK(getApplicationContext(), null);
        /**
         * 检查权限
         */
        testPermission();

        initView();

        DemoUtil.showVersionDialog(ListMainActivity.this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LanSoEditor.unInitSDK();
        LanSongFileUtil.deleteDefaultDir();
    }

    @Override
    public void onClick(View v) {
        if (!isPermissionOk) {
            testPermission();
        }

        if (isPermissionOk) {
            if (!checkPath())
                return;
            switch (v.getId()) {
                case R.id.id_mainlist_camerarecord:
                    startDemoActivity(ListCameraRecordActivity.class);
                    break;
                case R.id.id_mainlist_somelayer:
                    startDemoActivity(ListLayerDemoActivity.class);
                    break;
                case R.id.id_mainlist_changjing:
                    startDemoActivity(ListSceneDemoActivity.class);
                    break;
                case R.id.id_mainlist_douyin:
                    startDemoActivity(DouYinDemoActivity.class);
                    break;
                case R.id.id_mainlist_weishang:
                    startDemoActivity(AERecordFileHintActivity.class);
                    break;
                case R.id.id_mainlist_gamevideo:
                    startDemoActivity(GameVideoDemoActivity.class);
                    break;
                case R.id.id_mainlist_videoonedo:
                    startDemoActivity(VideoOneDODemoActivity.class);
                    break;
                case R.id.id_mainlist_bitmaps:
                    startDemoActivity(ListBitmapAudioActivity.class);
                    break;
                case R.id.id_mainlist_videoplay:
                    startDemoActivity(VideoPlayerActivity.class);
                    break;
                default:
                    break;
            }
        }
    }

    // -----------------------------
    private void initView() {
        tvVideoPath = (TextView) findViewById(R.id.id_main_tvvideo);
        findViewById(R.id.id_mainlist_camerarecord).setOnClickListener(this);
        findViewById(R.id.id_mainlist_somelayer).setOnClickListener(this);
        findViewById(R.id.id_mainlist_changjing).setOnClickListener(this);
        findViewById(R.id.id_mainlist_douyin).setOnClickListener(this);
        findViewById(R.id.id_mainlist_weishang).setOnClickListener(this);
        findViewById(R.id.id_mainlist_videoonedo).setOnClickListener(this);
        findViewById(R.id.id_mainlist_bitmaps).setOnClickListener(this);
        findViewById(R.id.id_mainlist_videoplay).setOnClickListener(this);
        findViewById(R.id.id_mainlist_gamevideo).setOnClickListener(this);


        //---------------------
        findViewById(R.id.id_main_select_video).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startSelectVideoActivity();
            }
        });

        findViewById(R.id.id_main_use_default_videobtn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new CopyDefaultVideoAsyncTask(ListMainActivity.this, tvVideoPath, "dy_xialu2.mp4").execute();
            }
        });
    }



    private boolean checkPath() {
        if (tvVideoPath.getText() != null && tvVideoPath.getText().toString().isEmpty()) {
            Toast.makeText(ListMainActivity.this, "请输入视频地址", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            String path = tvVideoPath.getText().toString();
            if (!(new File(path)).exists()) {
                Toast.makeText(ListMainActivity.this, "文件不存在", Toast.LENGTH_SHORT).show();
                return false;
            } else {
                MediaInfo info = new MediaInfo(path);
                boolean ret = info.prepare();
                return ret;
            }
        }
    }

    private void startSelectVideoActivity() {
        Intent i = new Intent(this, FileExplorerActivity.class);
        startActivityForResult(i, SELECT_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case RESULT_OK:
                if (requestCode == SELECT_FILE_REQUEST_CODE) {
                    Bundle b = data.getExtras();
                    String seleced = b.getString("SELECT_VIDEO");
                    checkConvertDialog(seleced);
                }
                break;
            default:
                break;
        }
    }

    private void checkConvertDialog(final String file) {
        if (!EditModeVideo.checkEditModeVideo(file)) {
            new AlertDialog.Builder(ListMainActivity.this)
                    .setTitle("提示")
                    .setMessage("是否转换为 编辑模式!")
                    .setPositiveButton("转换", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //转换为编辑模式对话框.
                            ConvertToEditModeDialog editMode = new ConvertToEditModeDialog(ListMainActivity.this, file, new ConvertToEditModeDialog.onConvertToEditModeDialogListener() {
                                @Override
                                public void onConvertCompleted(String video) {
                                    if (tvVideoPath != null) {
                                        tvVideoPath.setText(video);
                                    }
                                }
                            });
                            editMode.start();
                        }
                    })
                    .setNegativeButton("不转", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (tvVideoPath != null) {
                                tvVideoPath.setText(file);
                            }
                        }
                    })
                    .show();
        }
    }

    private void startDemoActivity(Class<?> cls) {
        String path = tvVideoPath.getText().toString();
        Intent intent = new Intent(ListMainActivity.this, cls);
        intent.putExtra("videopath", path);
        startActivity(intent);
    }

    private void testPermission() {
        if (permissionCnt > 2) {
            DemoUtil.showHintDialog(ListMainActivity.this, "Demo没有读写权限,请关闭后重新打开demo,并在弹出框中选中[允许]");
            return;
        }
        permissionCnt++;
        // PermissionsManager采用github上开源库,不属于我们sdk的一部分.
        // 下载地址是:https://github.com/anthonycr/Grant,您也可以使用别的方式来检查app所需权限.
        PermissionsManager.getInstance().requestAllManifestPermissionsIfNecessary(this,
                new PermissionsResultAction() {
                    @Override
                    public void onGranted() {
                        isPermissionOk = true;
                    }

                    @Override
                    public void onDenied(String permission) {
                        isPermissionOk = false;
                    }
                });
    }

    private class CopyDefaultVideoAsyncTask extends AsyncTask<Object, Object, Boolean> {
        private ProgressDialog mProgressDialog;
        private Context mContext = null;
        private TextView tvHint;
        private String fileName;

        /**
         * @param ctx
         * @param tvhint 拷贝后, 把拷贝到的目标完整路径显示到这个TextView上.
         * @param file   需要拷贝的文件名字.
         */
        public CopyDefaultVideoAsyncTask(Context ctx, TextView tvhint, String file) {
            mContext = ctx;
            tvHint = tvhint;
            fileName = file;
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setMessage("正在拷贝...");
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected synchronized Boolean doInBackground(Object... params) {
            if (!LanSongFileUtil.fileExist(fileName)) {
                copyAssets(mContext, fileName);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (mProgressDialog != null) {
                mProgressDialog.cancel();
                mProgressDialog = null;
            }
            String str = LanSongFileUtil.TMP_DIR + fileName;
            if (LanSongFileUtil.fileExist(str)) {
                Toast.makeText(mContext, "默认视频文件拷贝完成.视频样片路径:" + str, Toast.LENGTH_SHORT).show();
                if (tvHint != null)
                    tvHint.setText(str);
            } else {
                Toast.makeText(mContext, "抱歉! 默认视频文件拷贝失败,请联系我们:视频样片路径:" + str, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
