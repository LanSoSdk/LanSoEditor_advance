package com.example.advanceDemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.example.advanceDemo.bitmapAudio.BitmapEditDemoActivity;
import com.example.advanceDemo.bitmapAudio.DisplayFramesActivity;
import com.example.advanceDemo.bitmapAudio.ExtractVideoFrameDemoActivity;
import com.lansoeditor.advanceDemo.R;

public class ListBitmapAudioActivity extends Activity implements OnClickListener {
    String videoPath = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        videoPath = getIntent().getStringExtra("videopath");
        setContentView(R.layout.get_frames_list_layout);

        findViewById(R.id.id_getframe_testtime).setOnClickListener(this);
        findViewById(R.id.id_getframe_get25frame).setOnClickListener(this);
        findViewById(R.id.id_getframe_get60frame).setOnClickListener(this);
        findViewById(R.id.id_getframe_allframe).setOnClickListener(this);
        findViewById(R.id.id_getframe_bmpedit).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_getframe_testtime:
                startDemoActivity(ExtractVideoFrameDemoActivity.class, 0);
                break;
            case R.id.id_getframe_get25frame:
                startDemoActivity(DisplayFramesActivity.class,
                        DisplayFramesActivity.FRAME_TYPE_25);
                break;
            case R.id.id_getframe_get60frame:
                startDemoActivity(DisplayFramesActivity.class,
                        DisplayFramesActivity.FRAME_TYPE_60);
                break;
            case R.id.id_getframe_allframe:
                startDemoActivity(DisplayFramesActivity.class,
                        DisplayFramesActivity.FRAME_TYPE_ALL);
                break;
            case R.id.id_getframe_bmpedit:
                startDemoActivity(BitmapEditDemoActivity.class, 0);
                break;
            default:
                break;
        }
    }

    private void startDemoActivity(Class<?> cls, int Type) {
        Intent intent = new Intent(this, cls);
        intent.putExtra("videopath", videoPath);
        intent.putExtra("TYPE", Type);
        startActivity(intent);
    }

}
