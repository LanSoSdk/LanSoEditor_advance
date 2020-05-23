package com.example.advanceDemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.example.advanceDemo.scene.VideoH2VActivity;
import com.example.advanceDemo.scene.VideoV2HActivity;
import com.example.advanceDemo.scene.VideoConcatAcvivity;
import com.example.advanceDemo.scene.VideoSpeedDemoActivity;
import com.example.advanceDemo.scene.LayerLayoutDemoActivity;
import com.example.advanceDemo.scene.MoreLayHeadSeekActivity;
import com.example.advanceDemo.scene.ParticleDemoActivity;
import com.example.advanceDemo.scene.PicturesSlideDemoActivity;
import com.example.advanceDemo.scene.VViewImage3DDemoActivity;
import com.example.advanceDemo.scene.TwoVideoLayoutActivity;
import com.example.advanceDemo.scene.VideoLayerTransformActivity;
import com.example.advanceDemo.scene.VideoSeekActivity;
import com.lansoeditor.advanceDemo.R;

public class ListSceneDemoActivity extends Activity implements OnClickListener {

    private String videoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.list_scene_demo_layout);

        videoPath = getIntent().getStringExtra("videopath");

        findViewById(R.id.id_screne_pictures).setOnClickListener(this);

        findViewById(R.id.id_screne_videotransform).setOnClickListener(this);


        findViewById(R.id.id_screne_h2v).setOnClickListener(this);
        findViewById(R.id.id_screne_v2h).setOnClickListener(this);
        findViewById(R.id.id_screne_concat).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_screne_pictures: // 图片影集
                startDemoActivity(PicturesSlideDemoActivity.class);
                break;
            case R.id.id_screne_videotransform:
                startDemoActivity(VideoLayerTransformActivity.class);
                break;
            case  R.id.id_screne_h2v:
                startDemoActivity(VideoH2VActivity.class);
                break;
            case  R.id.id_screne_v2h:
                startDemoActivity(VideoV2HActivity.class);
                break;
            case  R.id.id_screne_concat:
                startDemoActivity(VideoConcatAcvivity.class);
                break;
            default:
                break;
        }
    }


    private void startDemoActivity(Class<?> cls) {
        Intent intent = new Intent(ListSceneDemoActivity.this, cls);
        intent.putExtra("videopath", videoPath);
        startActivity(intent);
    }

}
