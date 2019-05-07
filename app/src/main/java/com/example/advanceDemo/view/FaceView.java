package com.example.advanceDemo.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.hardware.Camera.Face;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.lansoeditor.advanceDemo.R;
import com.lansosdk.box.CameraLayer;

public class FaceView extends ImageView {
    private static final String TAG = "YanZi";
    private Context mContext;
    private Paint mLinePaint;
    private Face[] mFaces;
    private RectF mRect = new RectF();
    private Drawable mFaceIndicator = null;

    public FaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        initPaint();
        mContext = context;
        mFaceIndicator = getResources().getDrawable(R.drawable.face_find_rect);
    }

    public void setFaces(Face[] faces) {
        this.mFaces = faces;
        invalidate();
    }

    public void clearFaces() {
        mFaces = null;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        if (mFaces == null || mFaces.length < 1) {
            return;
        }
        canvas.save();
        canvas.rotate(-0); // Canvas.rotate()默认是逆时针

        /**
         * 获取人脸的个数.
         */
        for (int i = 0; i < mFaces.length; i++) {
            // 得到人脸的矩形
            mRect.set(mFaces[i].rect);

            CameraLayer.faceMapRect(mRect);

            mFaceIndicator.setBounds(Math.round(mRect.left),
                    Math.round(mRect.top), Math.round(mRect.right),
                    Math.round(mRect.bottom));

            mFaceIndicator.draw(canvas);
        }
        canvas.restore();
        super.onDraw(canvas);
    }

    private void initPaint() {
        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // int color = Color.rgb(0, 150, 255);
        int color = Color.rgb(98, 212, 68);
        // mLinePaint.setColor(Color.RED);
        mLinePaint.setColor(color);
        mLinePaint.setStyle(Style.STROKE);
        mLinePaint.setStrokeWidth(5f);
        mLinePaint.setAlpha(180);
    }
}
