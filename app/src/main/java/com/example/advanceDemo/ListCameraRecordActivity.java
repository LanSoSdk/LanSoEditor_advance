package com.example.advanceDemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.example.advanceDemo.camera.CameraLayerFullLandscapeActivity;
import com.example.advanceDemo.camera.CameraLayerFullPortActivity;
import com.example.advanceDemo.camera.CameraLayerFullPortWithMp3Activity;
import com.example.advanceDemo.camera.CameraLayerFullSegmentActivity;
import com.example.advanceDemo.camera.CameraLayerKTVDemoActivity;
import com.example.advanceDemo.camera.CameraSubLayerDemo1Activity;
import com.example.advanceDemo.camera.CameraSubLayerDemo2Activity;
import com.lansoeditor.advanceDemo.R;

public class ListCameraRecordActivity extends Activity implements
        OnClickListener {

    private String videoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.camera_demo_list_layout);
        videoPath = getIntent().getStringExtra("videopath");

        findViewById(R.id.id_cameralist_camerafulllayer).setOnClickListener(
                this);
        findViewById(R.id.id_cameralist_camerafulllayer2).setOnClickListener(
                this);
        findViewById(R.id.id_cameralist_cameralayer_segment)
                .setOnClickListener(this);

        findViewById(R.id.id_cameralist_mp3record).setOnClickListener(this);
        findViewById(R.id.id_cameralist_green_bg_record).setOnClickListener(
                this);
        findViewById(R.id.id_cameralist_sublayer1).setOnClickListener(this);
        findViewById(R.id.id_cameralist_sublayer2).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_cameralist_camerafulllayer:
                startDemoActivity(CameraLayerFullPortActivity.class);
                break;
            case R.id.id_cameralist_camerafulllayer2:
                startDemoActivity(CameraLayerFullLandscapeActivity.class);
                break;
            case R.id.id_cameralist_cameralayer_segment:
                startDemoActivity(CameraLayerFullSegmentActivity.class);
                break;
            case R.id.id_cameralist_mp3record:
                startDemoActivity(CameraLayerFullPortWithMp3Activity.class);
                break;
            case R.id.id_cameralist_sublayer1:
                startDemoActivity(CameraSubLayerDemo1Activity.class);
                break;
            case R.id.id_cameralist_sublayer2:
                startDemoActivity(CameraSubLayerDemo2Activity.class);
                break;
            case R.id.id_cameralist_green_bg_record:
                startDemoActivity(CameraLayerKTVDemoActivity.class);
            default:
                break;
        }
    }

    private void startDemoActivity(Class<?> cls) {
        Intent intent = new Intent(ListCameraRecordActivity.this, cls);
        intent.putExtra("videopath", videoPath);
        startActivity(intent);
    }

}
