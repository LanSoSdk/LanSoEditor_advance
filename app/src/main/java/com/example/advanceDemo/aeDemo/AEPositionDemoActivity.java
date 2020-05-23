package com.example.advanceDemo.aeDemo;


import android.app.Activity;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lansoeditor.advanceDemo.R;

/**
 * 2018年12月14日11:10:45 测试.
 * 动态创建一个view,并盖在图片的上面. 用来测试在指定的位置叠加UI, 单位是像素.
 *
 * 测试过720P, 1080P 2k的屏幕. 可以.
 */
public class AEPositionDemoActivity extends Activity {

    TextView textView;
    ImageView imageView;
    RelativeLayout relativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ae_position_demo_layout);

        textView=(TextView)findViewById(R.id.id_aepostion_textview);
        imageView=(ImageView)findViewById(R.id.id_aepostion_imageview);
        relativeLayout=(RelativeLayout)findViewById(R.id.id_aepostion_relativelayout);
    }

    @Override
    protected void onResume() {
        super.onResume();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                DisplayMetrics dm = new DisplayMetrics();
                dm = getResources().getDisplayMetrics();


                String str= "屏幕宽高: "+dm.widthPixels+ " x "+ dm.heightPixels;
                str+="\n";
                str+="控件宽高 x:"+imageView.getX()+ " Y:"+imageView.getY()+ " width:"+imageView.getWidth()+ " height:"+ imageView.getHeight();

                Matrix matrix = imageView.getImageMatrix();
                RectF imageRectF = new RectF();
                Drawable drawable = imageView.getDrawable();
                if (drawable != null) {
                    imageRectF.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                    matrix.mapRect(imageRectF);
                    str+="\n";
                    str+="图片在其位置:"+"left:"+imageRectF.left +"top:"+ imageRectF.top+ " right:"+ imageRectF.right+ " bottom:"+imageRectF.bottom;
                }

                textView.setText(str);
                /**
                 * 第一个方块
                 * 844x 708; 大小是 213x213
                 *
                 * 第二个方块:
                 * xy:185 x 1252;
                 * 宽高:391 x507;
                 *
                 * 第三个方块:
                 * 592,1252, 宽高:391,507
                 *
                 * @param imageRectF
                 */
                createView(imageRectF,844f,708f,213f,213f);
                createView(imageRectF,185f,1252f,391f,507f);
                createView(imageRectF,592,1252f,391f,507f);
            }
        },1000);
    }
    private void createView(RectF imageRectF, float x, float y, float width,float height)
    {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        View view = new View(this);
        view.setBackgroundColor(Color.GREEN);

        //宽高是实际的宽高, 单位是px,像素点;

        float radioWidth=(imageRectF.right - imageRectF.left)/1080.0f;
        float radioHeight=(imageRectF.bottom - imageRectF.top)/1920f;



        layoutParams.width = (int)(width*radioWidth);  //宽度,X坐标,和图片一致, 正好盖住.
        layoutParams.height =(int)(height*radioHeight);

        //坐标是实际坐标, 单位是px
        layoutParams.leftMargin =(int)(x*radioWidth+imageView.getX()+imageRectF.left);
        layoutParams.topMargin = (int)(y*radioHeight+imageView.getY()+imageRectF.top);

        relativeLayout.addView(view, layoutParams);
    }

    /**
     * 密度值, 转 像素;返回的是像素点;,
     * 暂时没有用到.
     * @param dpValue
     * @return
     */
    public  int dp2px(float dpValue){
        final float scale=AEPositionDemoActivity.this.getResources().getDisplayMetrics().density;
        return (int)(dpValue*scale+0.5f);
    }

}
