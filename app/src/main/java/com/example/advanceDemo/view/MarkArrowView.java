package com.example.advanceDemo.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.Layer;
import com.lansosdk.videoeditor.DrawPadView;

/**
 * 继承自DrawPadView, 用来演示在视频中做标记的功能.
 * <p>
 * 原理是: 根据onTouch事件, 按下时,从DrawPad中增加一个BitmapLayer, 移动时,把获取到BitmapLayer实时的移动坐标.
 * 抬起时,从DrawPad中删除BitmapLayer
 */
public class MarkArrowView extends DrawPadView {

    Layer bitmapLayer = null;

    public MarkArrowView(Context context) {
        super(context);
    }

    public MarkArrowView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MarkArrowView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MarkArrowView(Context context, AttributeSet attrs, int defStyleAttr,
                         int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (bitmapLayer == null) {
                    // 继承自DrawPadView, 在按下时获取一个BitmapLayer
                    bitmapLayer = addBitmapLayer(BitmapFactory.decodeResource(
                            getResources(), R.drawable.mianju2));
                    if (bitmapLayer != null) {
                        bitmapLayer.setVisibility(Layer.INVISIBLE);
                    }
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (bitmapLayer != null) {
                    bitmapLayer.setPosition(event.getX(), event.getY());
                    bitmapLayer.setVisibility(Layer.VISIBLE);
                }
                return true;
            case MotionEvent.ACTION_UP:
                // Log.i("test","ACTION_UP:"+event.getX()+" Y:"+event.getY());
                // 当抬起时, 删除这个Layer
                // if(byteBuffer!=null){
                // byteBuffer.setVisibility(AeLayer.INVISIBLE);
                // removeLayer(byteBuffer);
                // }
                break;
        }
        return super.onTouchEvent(event);
    }

}
