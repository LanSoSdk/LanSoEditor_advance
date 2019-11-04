package com.example.advanceDemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.example.advanceDemo.aeDemo.AECompositionActivity;
import com.example.advanceDemo.aeDemo.AECompositionExecuteActivity;
import com.example.advanceDemo.aeDemo.AEDemoAsset;
import com.example.advanceDemo.aeDemo.AEPositionDemoActivity;
import com.example.advanceDemo.utils.ButtonEnable;
import com.lansoeditor.advanceDemo.R;

public class ListAEActivity extends Activity implements OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.ae_activity_main);
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_aemain_aobama:
                showActivity(AEDemoAsset.AE_DEMO_AOBAMA);
                break;
            case R.id.id_aemain_xiaohuangya:
                showActivity(AEDemoAsset.AE_DEMO_XIAOHUANGYA);
                break;
            case R.id.id_aemain_zhemehaokan:
                showActivity( AEDemoAsset.AE_DEMO_HAOKAN);
                break;
            case R.id.id_aemain_xianzi:
                showActivity(AEDemoAsset.AE_DEMO_XIANZI);
                break;
            case R.id.id_aemain_kuaishan:
                showActivity(AEDemoAsset.AE_DEMO_KUAISHAN);
                break;
            case R.id.id_aemain_videobtimap:
                showActivity(AEDemoAsset.AE_DEMO_VIDEOBITMAP);
                break;
            case R.id.id_aemain_more_picture:
                showActivity(AEDemoAsset.AE_DEMO_MORE_PICTURE);
                break;
            case R.id.id_aemain_json_cut:
                showActivity(AEDemoAsset.AE_DEMO_JSON_CUT);
                break;
            case R.id.id_aemain_json_concat:
                showActivity(AEDemoAsset.AE_DEMO_JSON_CONCAT);
                break;
            case R.id.id_aemain_json_overlay:
                showActivity(AEDemoAsset.AE_DEMO_TWO_JSON_OVERLAY);
                break;

            case R.id.id_aemain_json_blur1:
                showActivity(AEDemoAsset.AE_DEMO_JSON_GAUSSIAN_BLUR1);
                break;

            case R.id.id_aemain_json_blur2:
                showActivity(AEDemoAsset.AE_DEMO_JSON_GAUSSIAN_BLUR2);
                break;

            case R.id.id_aemain_ka_dian:
                showActivity(AEDemoAsset.AE_DEMO_JSON_KA_DIAIN);
                break;
            //-------其他版本;
            case R.id.id_aemain_bitmapxy:  //图片坐标;
            {
                Intent intent = new Intent(ListAEActivity.this, AEPositionDemoActivity.class);
                startActivity(intent);
            }
            break;
            default:
                break;
        }
    }
    // -----------------------------


    private void showActivity(int type){

        ButtonEnable enable=findViewById(R.id.id_listae_onlyexport);
        if(!enable.isSelecteButton()){
            Intent intent = new Intent(ListAEActivity.this, AECompositionActivity.class);
            intent.putExtra("AEType", type);
            startActivity(intent);
        }else{

            //测试后台执行;
            Intent intent = new Intent(ListAEActivity.this, AECompositionExecuteActivity.class);
            intent.putExtra("AEType", type);
            startActivity(intent);
        }
    }


    private void initView() {
        findViewById(R.id.id_aemain_aobama).setOnClickListener(this);
        findViewById(R.id.id_aemain_xiaohuangya).setOnClickListener(this);
        findViewById(R.id.id_aemain_zhemehaokan).setOnClickListener(this);

        findViewById(R.id.id_aemain_xianzi).setOnClickListener(this);
        findViewById(R.id.id_aemain_kuaishan).setOnClickListener(this);
        findViewById(R.id.id_aemain_videobtimap).setOnClickListener(this);
        findViewById(R.id.id_aemain_bitmapxy).setOnClickListener(this);
        findViewById(R.id.id_aemain_more_picture).setOnClickListener(this);


        findViewById(R.id.id_aemain_json_cut).setOnClickListener(this);
        findViewById(R.id.id_aemain_json_concat).setOnClickListener(this);

        findViewById(R.id.id_aemain_json_overlay).setOnClickListener(this);
        findViewById(R.id.id_aemain_json_blur1).setOnClickListener(this);
        findViewById(R.id.id_aemain_json_blur2).setOnClickListener(this);
        findViewById(R.id.id_aemain_ka_dian).setOnClickListener(this);
    }





}
