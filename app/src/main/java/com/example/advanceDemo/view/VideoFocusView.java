package com.example.advanceDemo.view;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.lansoeditor.advanceDemo.R;

public class VideoFocusView extends RelativeLayout {

    public static final int FOCUS_IMG_WH = 120;
    private static final String VIDEO_SETTING_NAME = "VIDEO_SETTING";
    float downY;
    private boolean mHaveTouch = false;
    private boolean mShowGrid = false;
    private ImageView mFocusImg;
    private GuideStep mCurrentStep = GuideStep.STEP1;
    private Context context;

    @SuppressWarnings("deprecation")
    public VideoFocusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        mHaveTouch = false;

        mFocusImg = new ImageView(context);
        mFocusImg.setVisibility(View.GONE);
        mFocusImg.setBackgroundResource(R.drawable.video_record_image_focus);
        addView(mFocusImg);
    }

    public float getDownY() {
        return downY;
    }

    public void setDownY(float downY) {
        this.downY = downY;
    }

    public void setHaveTouch(boolean val, Rect rect) {
        mHaveTouch = val;
        if (mHaveTouch) {
            LayoutParams params = (LayoutParams) mFocusImg.getLayoutParams();
            params.leftMargin = rect.left;
            params.topMargin = rect.top;
            params.width = rect.right - rect.left;
            // params.width = rect.right - rect.left+50;
            params.height = rect.bottom - rect.top;
            mFocusImg.setLayoutParams(params);
            mFocusImg.setVisibility(View.VISIBLE);
            startAnimation(mFocusImg, 600, 0, 0, 1.25f, 1f);
        } else {
            mFocusImg.setVisibility(View.GONE);
            mFocusImg.clearAnimation();
        }
    }

    private AnimatorSet startAnimation(View view, long duration,
                                       int repeatCount, long delay, float... scale) {

        AnimatorSet as = new AnimatorSet();
        as.setInterpolator(new AccelerateDecelerateInterpolator());
        as.setStartDelay(delay);
        ObjectAnimator animY = ObjectAnimator.ofFloat(view, "scaleY", scale);
        ObjectAnimator animX = ObjectAnimator.ofFloat(view, "scaleX", scale);
        animY.setDuration(duration);
        animX.setDuration(duration);
        animY.setRepeatCount(repeatCount);
        animX.setRepeatCount(repeatCount);

        as.playTogether(animY, animX);
        as.start();
        return as;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int specWidthSize = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(specWidthSize, specWidthSize);
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    public GuideStep getCurrentStep() {
        return mCurrentStep;
    }

    public enum GuideStep {
        STEP1, STEP2, STEP3
    }

    ;

}
