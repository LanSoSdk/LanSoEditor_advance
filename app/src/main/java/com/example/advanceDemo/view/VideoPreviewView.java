package com.example.advanceDemo.view;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.Display;
import android.view.SurfaceView;
import android.view.WindowManager;

public class VideoPreviewView extends SurfaceView {
    public static float DONT_CARE = 0.0f;
    private double mAspectRatio = DONT_CARE;
    private int mCameraW = 0;
    private int mCameraH = 0;
    private int mSqueraW;
    private Context mContext;

    public VideoPreviewView(Context context) {
        super(context);
        mContext = context;
    }

    public VideoPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public VideoPreviewView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    public void setAspectRatio(int cameraW, int cameraH) {

        if (mCameraW == 0 || mCameraH == 0) {
            mCameraW = cameraW;
            mCameraH = cameraH;

            Display display = ((WindowManager) mContext
                    .getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);

            mSqueraW = size.x; // 相机预览画面的宽度默认等于屏幕的宽度来调节高度.
            double ratio = (double) mCameraW / mCameraH;
            if (ratio <= 0.0) {
                throw new IllegalArgumentException();
            }
            if (mAspectRatio != ratio) {
                mAspectRatio = ratio;
                requestLayout();
                invalidate();
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mAspectRatio != DONT_CARE) {
            int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

            int width = widthSpecSize;
            int height = heightSpecSize;

            if (width > 0 && height > 0) {

                int min = Math.min(mCameraW, mCameraH);
                /**
                 * TODO 这里应该修改成
                 */
                if (min < mSqueraW) {
                    float scale = (float) mSqueraW / min;
                    setMeasuredDimension((int) (mCameraW * scale),
                            (int) (mCameraH * scale));
                } else {
                    float scale = (float) min / mSqueraW;
                    setMeasuredDimension((int) (mCameraW / scale),
                            (int) (mCameraH / scale));
                }

                return;
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
