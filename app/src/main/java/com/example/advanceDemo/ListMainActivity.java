package com.example.advanceDemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;
import com.example.advanceDemo.utils.DemoUtil;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.LanSoEditorBox;
import com.lansosdk.videoeditor.LanSoEditor;
import com.lansosdk.videoeditor.LanSongFileUtil;
import com.lansosdk.videoeditor.VideoEditor;


public class ListMainActivity extends Activity implements OnClickListener {

    int permissionCnt = 0;
    private boolean isPermissionOk = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * 检查权限
         */
        testPermission();

        findViewById(R.id.id_main_list_lso_layer).setOnClickListener(this);
        findViewById(R.id.id_main_list_aex).setOnClickListener(this);

        //显示版本提示
        DemoUtil.showVersionDialog(ListMainActivity.this);

        TextView textView=findViewById(R.id.id_main_version_hint);

        String version = VideoEditor.getSDKVersion() + "\n BOX:" + LanSoEditorBox.VERSION_BOX;

        textView.setText("version:   " + version);
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

        if (isPermissionOk ) {
                switch (v.getId()) {
                    case R.id.id_main_list_lso_layer:
                        startDemoActivity(VideoEditDemoActivity.class);
                        break;
                    case R.id.id_main_list_aex:
                        startDemoActivity(AexPlayerDemoActivity.class);
                        break;
                    default:
                        break;
                }
        }
    }

    private void startDemoActivity(Class<?> cls) {
        Intent intent = new Intent(ListMainActivity.this, cls);
        startActivity(intent);
    }

    private void testPermission() {
        if (permissionCnt > 2) {
            DemoUtil.showDialog(ListMainActivity.this, "Demo没有读写权限,请关闭后重新打开demo,并在弹出框中选中[允许]");
            return;
        }
        permissionCnt++;
        // PermissionsManager采用github上开源库,不属于sdk的一部分.
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

}
