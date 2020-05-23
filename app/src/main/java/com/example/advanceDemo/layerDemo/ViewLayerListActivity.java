package com.example.advanceDemo.layerDemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;

import android.view.View;

import com.lansoeditor.advanceDemo.R;

public class ViewLayerListActivity extends Activity {



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.viewlayer_list_layout);

        findViewById(R.id.viewlayer_list_recordmode).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMode(ViewLayerDemoActivity.RUN_RECORD_MODE);
            }
        });

        findViewById(R.id.viewlayer_list_exportmode).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMode(ViewLayerDemoActivity.RUN_EXPORT_MODE);
            }
        });
    }


    private void startMode(int mode)
    {
        String path = getIntent().getStringExtra("videopath");

        Intent intent = new Intent(ViewLayerListActivity.this, ViewLayerDemoActivity.class);
        intent.putExtra("videopath", path);
        intent.putExtra("mode",mode);
        startActivity(intent);
    }

}
