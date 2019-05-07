package com.example.advanceDemo.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import com.example.advanceDemo.ListMainActivity;
import com.lansoeditor.advanceDemo.R;

import java.util.ArrayList;

public class StartActivity extends Activity {
    private ViewPager mViewPager;
    private int currIndex = 0;
    private ImageView mPage1;
    private ImageView mPage2;
    private ImageView mPage3;
    private ImageView mPage4;
    private ImageView mPage5;
    private ImageView mPage6;
    private ImageView mPage7;
    private ImageView mPage8;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.viewpager);
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mViewPager.setOnPageChangeListener(new MyOnPageChangeListener());

        mPage1 = (ImageView) findViewById(R.id.page1);
        mPage2 = (ImageView) findViewById(R.id.page2);
        mPage3 = (ImageView) findViewById(R.id.page3);
        mPage4 = (ImageView) findViewById(R.id.page4);
        mPage5 = (ImageView) findViewById(R.id.page5);
        mPage6 = (ImageView) findViewById(R.id.page6);
        mPage7 = (ImageView) findViewById(R.id.page7);
        mPage8 = (ImageView) findViewById(R.id.page8);

        // 将要分页显示的View装入数组中
        LayoutInflater mLi = LayoutInflater.from(this);
        View view1 = mLi.inflate(R.layout.view1, null);
        View view2 = mLi.inflate(R.layout.view2, null);
        View view3 = mLi.inflate(R.layout.view3, null);
        View view4 = mLi.inflate(R.layout.view4, null);
        View view5 = mLi.inflate(R.layout.view5, null);
        View view6 = mLi.inflate(R.layout.view6, null);
        View view7 = mLi.inflate(R.layout.view7, null);
        View view8 = mLi.inflate(R.layout.view8, null);

        // 每个页面的view数据
        final ArrayList<View> views = new ArrayList<View>();
        views.add(view1);
        views.add(view2);
        views.add(view3);
        views.add(view4);
        views.add(view5);
        views.add(view6);
        views.add(view7);
        views.add(view8);

        PagerAdapter mPagerAdapter = new PagerAdapter() {

            @Override
            public boolean isViewFromObject(View arg0, Object arg1) {
                return arg0 == arg1;
            }

            @Override
            public int getCount() {
                return views.size();
            }

            @Override
            public void destroyItem(View container, int position, Object object) {
                ((ViewPager) container).removeView(views.get(position));
            }

            @Override
            public Object instantiateItem(View container, int position) {
                ((ViewPager) container).addView(views.get(position));
                return views.get(position);
            }
        };
        mViewPager.setAdapter(mPagerAdapter);
    }

    public void startbutton(View v) {
        Intent intent = new Intent(StartActivity.this, ListMainActivity.class);
        startActivity(intent);
        StartActivity.this.finish();
    }

    public class MyOnPageChangeListener implements OnPageChangeListener {

        @Override
        public void onPageSelected(int arg0) {
            Animation animation = null;
            switch (arg0) {
                case 0:
                    mPage1.setImageDrawable(getResources().getDrawable(
                            R.drawable.page_now));
                    mPage2.setImageDrawable(getResources().getDrawable(
                            R.drawable.page));
                    if (currIndex == arg0 + 1) {
                        animation = new TranslateAnimation(20 * (arg0 + 1),
                                20 * arg0, 0, 0);
                    }
                    break;
                case 1:
                    mPage2.setImageDrawable(getResources().getDrawable(
                            R.drawable.page_now));
                    mPage1.setImageDrawable(getResources().getDrawable(
                            R.drawable.page));
                    mPage3.setImageDrawable(getResources().getDrawable(
                            R.drawable.page));
                    if (currIndex == arg0 - 1) {
                        animation = new TranslateAnimation(20 * (arg0 - 1),
                                20 * arg0, 0, 0);

                    } else if (currIndex == arg0 + 1) {
                        animation = new TranslateAnimation(20 * (arg0 + 1),
                                20 * arg0, 0, 0);
                    }
                    break;
                case 2:
                    mPage3.setImageDrawable(getResources().getDrawable(
                            R.drawable.page_now));
                    mPage2.setImageDrawable(getResources().getDrawable(
                            R.drawable.page));
                    mPage4.setImageDrawable(getResources().getDrawable(
                            R.drawable.page));
                    if (currIndex == arg0 - 1) {
                        animation = new TranslateAnimation(20 * (arg0 - 1),
                                20 * arg0, 0, 0);
                    } else if (currIndex == arg0 + 1) {
                        animation = new TranslateAnimation(20 * (arg0 + 1),
                                20 * arg0, 0, 0);
                    }
                    break;
                case 3:
                    mPage4.setImageDrawable(getResources().getDrawable(
                            R.drawable.page_now));
                    mPage3.setImageDrawable(getResources().getDrawable(
                            R.drawable.page));
                    mPage5.setImageDrawable(getResources().getDrawable(
                            R.drawable.page));
                    if (currIndex == arg0 - 1) {
                        animation = new TranslateAnimation(20 * (arg0 - 1),
                                20 * arg0, 0, 0);

                    } else if (currIndex == arg0 + 1) {
                        animation = new TranslateAnimation(20 * (arg0 + 1),
                                20 * arg0, 0, 0);
                    }
                    break;
                case 4:
                    mPage5.setImageDrawable(getResources().getDrawable(
                            R.drawable.page_now));
                    mPage4.setImageDrawable(getResources().getDrawable(
                            R.drawable.page));
                    mPage6.setImageDrawable(getResources().getDrawable(
                            R.drawable.page));
                    if (currIndex == arg0 - 1) {
                        animation = new TranslateAnimation(20 * (arg0 - 1),
                                20 * arg0, 0, 0);
                    } else if (currIndex == arg0 + 1) {
                        animation = new TranslateAnimation(20 * (arg0 + 1),
                                20 * arg0, 0, 0);
                    }
                    break;
                case 5:
                    mPage6.setImageDrawable(getResources().getDrawable(
                            R.drawable.page_now));
                    mPage5.setImageDrawable(getResources().getDrawable(
                            R.drawable.page));
                    mPage7.setImageDrawable(getResources().getDrawable(
                            R.drawable.page));
                    if (currIndex == arg0 - 1) {
                        animation = new TranslateAnimation(20 * (arg0 - 1),
                                20 * arg0, 0, 0);
                    } else if (currIndex == arg0 + 1) {
                        animation = new TranslateAnimation(20 * (arg0 + 1),
                                20 * arg0, 0, 0);
                    }
                    break;
                case 6:
                    mPage7.setImageDrawable(getResources().getDrawable(
                            R.drawable.page_now));
                    mPage6.setImageDrawable(getResources().getDrawable(
                            R.drawable.page));
                    mPage8.setImageDrawable(getResources().getDrawable(
                            R.drawable.page));
                    if (currIndex == arg0 - 1) {
                        animation = new TranslateAnimation(20 * (arg0 - 1),
                                20 * arg0, 0, 0);
                    } else if (currIndex == arg0 + 1) {
                        animation = new TranslateAnimation(20 * (arg0 + 1),
                                20 * arg0, 0, 0);
                    }
                    break;
                case 7:
                    mPage8.setImageDrawable(getResources().getDrawable(
                            R.drawable.page_now));
                    mPage7.setImageDrawable(getResources().getDrawable(
                            R.drawable.page));
                    mPage1.setImageDrawable(getResources().getDrawable(
                            R.drawable.page));
                    if (currIndex == arg0 - 1) {
                        animation = new TranslateAnimation(20 * (arg0 - 1),
                                20 * arg0, 0, 0);
                    } else if (currIndex == arg0 + 1) {
                        animation = new TranslateAnimation(20 * (arg0 + 1),
                                20 * arg0, 0, 0);
                    }
                    break;
            }
            currIndex = arg0;
            animation.setFillAfter(true);
            animation.setDuration(300);
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }
    }

}
