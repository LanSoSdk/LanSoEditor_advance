package com.example.advanceDemo.view;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.lansoeditor.advanceDemo.R;

/**
 * 此代码来源于网络, 不属于SDK的一部分, 仅仅作为演示聚焦时的动画使用.
 * <p>
 * 一定要在外面套上 FrameLayout <FrameLayout android:layout_width="match_parent"
 * android:layout_height="match_parent">
 * <org.yanzi.camera.preview.FocusImageView android:id="@+id/video_focus_view"
 * android:layout_width="75dip" android:layout_height="75dip"/>
 * <p>
 * </FrameLayout>
 */
public class FocusImageView extends ImageView {
    public final static String TAG = "FocusImageView";
    private Animation mAnimation;
    private Handler mHandler;

    public FocusImageView(Context context) {
        super(context);
        mAnimation = AnimationUtils.loadAnimation(getContext(),
                R.anim.focusview_show);
        setVisibility(View.GONE);
        mHandler = new Handler();
        setImageResource(R.drawable.focus_focused);
        setVisibility(View.GONE);
    }

    public FocusImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAnimation = AnimationUtils.loadAnimation(getContext(),
                R.anim.focusview_show);
        mHandler = new Handler();

        setImageResource(R.drawable.focus_focused);
        setVisibility(View.GONE);
    }

    /**
     * 显示聚焦图案
     *
     * @param point
     */
    public void startFocus(int x, int y) {
        // 根据触摸的坐标设置聚焦图案的位置
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
        params.topMargin = y - getMeasuredHeight() / 2;
        params.leftMargin = x - getMeasuredWidth() / 2;
        setLayoutParams(params);
        // 设置控件可见，并开始动画
        setVisibility(View.VISIBLE);
        startAnimation(mAnimation);

        // startAnimation(this, 80, 0, 0, 1.25f, 1f);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                setVisibility(View.GONE);
            }
        }, 80);
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

    // /**
    // * 聚焦成功回调
    // */
    // public void onFocusSuccess(){
    // mHandler.removeCallbacks(null, null);
    // mHandler.postDelayed(new Runnable() {
    // @Override
    // public void run() {
    // // TODO Auto-generated method stub
    // setVisibility(View.GONE);
    // }
    // },200);
    // }
    //
    // /**
    // * 聚焦失败回调
    // */
    // public void onFocusFailed(){
    // setImageResource(mFocusFailedImg);
    // //移除在startFocus中设置的callback，1秒后隐藏该控件
    // mHandler.removeCallbacks(null, null);
    // mHandler.postDelayed(new Runnable() {
    // @Override
    // public void run() {
    // // TODO Auto-generated method stub
    // setVisibility(View.GONE);
    // }
    // },1000);
    // }

}
