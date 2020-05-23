package com.example.advanceDemo.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.lansoeditor.advanceDemo.R;

import java.util.Random;

public class ShowHeart {

    int sh;
    int sw;
    int istartx, istarty, lovestartx, lovestarty, ustartx, ustarty;

    int startx;
    int starty;
    int maxh;
    int y_dao;
    double begin; // 起始位置
    Random rm = new Random();
    int old_num = -1;
    float old_xx = 0, old_yy = 0;
    int yadd_1200 = 100;
    BitmapCache bitmapcache;
    Bitmap bb;
    private Context mContext;
    private int drawCount = 0;

    public ShowHeart(Context context, int sw, int sh) {
        this.mContext = context;
        this.sw = sw;
        this.sh = sh;
        init();
    }

    public void init() {
        // 屏幕适配
        istartx = -50 + sw / 2;
        istarty = 50;

        lovestartx = sw / 2 - 16;
        lovestarty = sh / 2 - 68;

        ustartx = -94 + sw / 2;
        ustarty = 150 + sh / 2;

        if (sh / 2 > 180 + 150 + 118 + 20)
            ustarty = 150 + sh / 2 + 20;
        if (sh / 2 > 180 + 150 + 118 + 40)
            ustarty = 150 + sh / 2 + 40;
        if (sh / 2 > 180 + 150 + 118 + 60)
            ustarty = 150 + sh / 2 + 60;
        if (sh >= 1200) {
            istarty = istarty + yadd_1200;
            ustarty = ustarty + yadd_1200;
        }
        startx = sw / 2 - 16;
        starty = sh / 2 - 68;
        maxh = 100;
        y_dao = starty;
        begin = 10; // 起始位置

        old_num = -1;
        old_xx = 0;
        old_yy = 0;
        bitmapcache = BitmapCache.getInstance();
        drawCount = 0;
        bb = bitmapcache.getBitmap(R.drawable.heart, mContext);
    }

    public boolean isDrawEnd() {
        return drawCount > 100;
    }

    public void drawTrack(Canvas cas) {
        drawCount++;
        if (drawCount > 100) {
            // Log.i("sno","draw结束,返回");
            return;
        }
        int hua_num = rm.nextInt(18);
        begin = begin + 0.2; // 密度
        double b = begin / Math.PI;
        double a = 13.5 * (16 * Math.pow(Math.sin(b), 3)); // 这里的13.5可以控制大小
        double d = -13.5
                * (13 * Math.cos(b) - 5 * Math.cos(2 * b) - 2 * Math.cos(3 * b) - Math
                .cos(4 * b));

        float xx = (float) a;
        float yy = (float) d;

        Paint p = new Paint(); // 创建图层
        p.setColor(Color.RED);
        if (old_num != -1) {
            cas.drawBitmap(bb, startx + old_xx, starty + old_yy, p);
        }
        cas.drawBitmap(bb, startx + xx, starty + yy, p);

        old_num = hua_num;
        old_xx = xx;
        old_yy = yy;
    }
}
