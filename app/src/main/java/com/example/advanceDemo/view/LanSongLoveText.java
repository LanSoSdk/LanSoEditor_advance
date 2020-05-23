package com.example.advanceDemo.view;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import com.lansoeditor.advanceDemo.R;

import su.levenetc.android.textsurface.Text;
import su.levenetc.android.textsurface.TextBuilder;
import su.levenetc.android.textsurface.TextSurface;
import su.levenetc.android.textsurface.animations.Delay;
import su.levenetc.android.textsurface.animations.Parallel;
import su.levenetc.android.textsurface.animations.Sequential;
import su.levenetc.android.textsurface.animations.ShapeReveal;
import su.levenetc.android.textsurface.animations.SideCut;
import su.levenetc.android.textsurface.animations.TransSurface;
import su.levenetc.android.textsurface.contants.Align;
import su.levenetc.android.textsurface.contants.Pivot;
import su.levenetc.android.textsurface.contants.Side;
import su.levenetc.android.textsurface.interfaces.IEndListener;

/**
 * Created by Eugene Levenetc.
 * <p>
 * <p>
 * AbstractSurfaceAnimation.java 用于对基本的属性进行动画，比如alpha, translation,scale
 * 等等。参见（Alpha.java 或者 ChangeColor.java） 。 ITextEffect.java
 * 用于更复杂动画的接口（参见Rotate3D.java 或者 ShapeReveal.java）
 */
public class LanSongLoveText {

    public static final int FONT_SMALL = 30;
    public static final int FONT_BIG = 60;
    public static final int INTERVAL_TIME = 1200; // 2S

    public static Text createText(String strWord, Paint paint, int color,
                                  int align, int size) {

        return TextBuilder.create(strWord).setPaint(paint).setSize(size)
                .setAlpha(0).setColor(color).setPosition(align).build();
    }

    public static Text createText(String strWord, Paint paint, int color,
                                  int align, Text previous, int size) {

        return TextBuilder.create(strWord).setPaint(paint).setSize(size)
                .setAlpha(0).setColor(color).setPosition(align, previous)
                .build();
    }

    /**
     * 这里仅仅是演示, 因为没有修改源代码的方法, 写起来比较繁琐, 实际使用中, 可以根据需要,修改代码为List<>的形式,这样代码清晰更好一些.
     *
     * @param ctx
     * @param textSurface
     * @param assetManager
     */
    public static void play(Context ctx, TextSurface textSurface,
                            AssetManager assetManager, IEndListener endListener) {

        final Typeface robotoBlack = Typeface.createFromAsset(assetManager,"fonts/Roboto-Black.ttf");
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTypeface(robotoBlack);

        // 这是一段, 实际UI中可让用户输入一段文字, 然后下一段, 这样一段一段的来操作.
        Text gaoBai1 = createText(ctx.getString(R.string.gaobai_1), paint,
                Color.WHITE, Align.SURFACE_CENTER, FONT_BIG);
        Text gaoBai2 = createText(ctx.getString(R.string.gaobai_2), paint,
                Color.RED, Align.BOTTOM_OF, gaoBai1, FONT_SMALL);
        Text gaoBai3 = createText(ctx.getString(R.string.gaobai_3), paint,
                Color.RED, Align.BOTTOM_OF, gaoBai2, FONT_SMALL);
        Text gaoBai4 = createText(ctx.getString(R.string.gaobai_4), paint,
                Color.RED, Align.BOTTOM_OF, gaoBai3, FONT_SMALL);

        // 因为没有修改源代码的方法, 写起来比较繁琐, 实际使用中, 可以根据需要,修改代码为List<>的形式,这样代码清晰更好一些.
        textSurface
                .play(new Sequential(
                                ShapeReveal.create(gaoBai1, 750,
                                        SideCut.show(Side.LEFT), false),
                                Delay.duration(INTERVAL_TIME),
                                new Parallel(new TransSurface(500, gaoBai2,
                                        Pivot.CENTER), ShapeReveal.create(gaoBai2,
                                        1300, SideCut.show(Side.LEFT), false)),
                                Delay.duration(INTERVAL_TIME),
                                new Parallel(new TransSurface(500, gaoBai3,
                                        Pivot.CENTER), ShapeReveal.create(gaoBai3,
                                        1300, SideCut.show(Side.LEFT), false)),
                                Delay.duration(INTERVAL_TIME),
                                new Parallel(new TransSurface(500, gaoBai4,
                                        Pivot.CENTER), ShapeReveal.create(gaoBai4,
                                        1300, SideCut.show(Side.LEFT), false)),
                                Delay.duration(INTERVAL_TIME),
                                // 消失
                                new Parallel(ShapeReveal.create(gaoBai1, 1500,
                                        SideCut.hide(Side.LEFT), true), new Sequential(
                                        Delay.duration(250), ShapeReveal.create(
                                        gaoBai2, 1500, SideCut.hide(Side.LEFT),
                                        true)), new Sequential(Delay
                                        .duration(500), ShapeReveal.create(gaoBai3,
                                        1500, SideCut.hide(Side.LEFT), true)),
                                        new Sequential(Delay.duration(500), ShapeReveal
                                                .create(gaoBai4, 1500,
                                                        SideCut.hide(Side.LEFT), true)))),
                        endListener);
    }

    public static void play2(Context ctx, TextSurface textSurface,
                             AssetManager assetManager, IEndListener listener) {

        final Typeface robotoBlack = Typeface.createFromAsset(assetManager,
                "fonts/Roboto-Black.ttf");
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTypeface(robotoBlack);

        // 这是一段, 实际UI中可让用户输入一段文字, 然后下一段, 这样一段一段的来操作.
        // 第二段
        Text gaoBai5 = createText(ctx.getString(R.string.gaobai_5), paint,
                Color.WHITE, Align.SURFACE_CENTER, FONT_BIG);
        Text gaoBai6 = createText(ctx.getString(R.string.gaobai_6), paint,
                Color.RED, Align.BOTTOM_OF, gaoBai5, FONT_SMALL);
        Text gaoBai7 = createText(ctx.getString(R.string.gaobai_7), paint,
                Color.RED, Align.BOTTOM_OF, gaoBai6, FONT_SMALL);

        // 因为没有修改源代码的方法, 写起来比较繁琐, 实际使用中, 可以根据需要,修改代码为List<>的形式,这样代码清晰更好一些.
        textSurface.play(
                // 创建一个串行依次执行的动画队列(从前到后执行动画).
                new Sequential(

                        ShapeReveal.create(gaoBai5, 750,
                                SideCut.show(Side.LEFT), false), Delay
                        .duration(INTERVAL_TIME), new Parallel(
                        new TransSurface(500, gaoBai6, Pivot.CENTER),
                        ShapeReveal.create(gaoBai6, 1300,
                                SideCut.show(Side.LEFT), false)), Delay
                        .duration(INTERVAL_TIME), new Parallel(
                        new TransSurface(500, gaoBai7, Pivot.CENTER),
                        ShapeReveal.create(gaoBai7, 1300,
                                SideCut.show(Side.LEFT), false)), Delay
                        .duration(INTERVAL_TIME),
                        // 消失
                        new Parallel(ShapeReveal.create(gaoBai5, 1500,
                                SideCut.hide(Side.LEFT), true), new Sequential(
                                Delay.duration(250), ShapeReveal.create(
                                gaoBai6, 1500, SideCut.hide(Side.LEFT),
                                true)), new Sequential(Delay
                                .duration(500), ShapeReveal.create(gaoBai7,
                                1500, SideCut.hide(Side.LEFT), true)))),
                listener);
    }

    public static void play3(Context ctx, TextSurface textSurface,
                             AssetManager assetManager, IEndListener listener) {

        final Typeface robotoBlack = Typeface.createFromAsset(assetManager,
                "fonts/Roboto-Black.ttf");
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTypeface(robotoBlack);

        Text gaoBai8 = createText(ctx.getString(R.string.gaobai_8), paint,
                Color.WHITE, Align.SURFACE_CENTER, FONT_BIG);
        Text gaoBai9 = createText(ctx.getString(R.string.gaobai_9), paint,
                Color.RED, Align.BOTTOM_OF, gaoBai8, FONT_SMALL);
        Text gaoBai10 = createText(ctx.getString(R.string.gaobai_10), paint,
                Color.RED, Align.BOTTOM_OF, gaoBai9, FONT_SMALL);
        Text gaoBai11 = createText(ctx.getString(R.string.gaobai_11), paint,
                Color.RED, Align.BOTTOM_OF, gaoBai10, FONT_SMALL);
        // 因为没有修改源代码的方法, 写起来比较繁琐, 实际使用中, 可以根据需要,修改代码为List<>的形式,这样代码清晰更好一些.
        textSurface
                .play(
                        // 创建一个串行依次执行的动画队列(从前到后执行动画).
                        new Sequential(

                                // //第三段----8 9 10 11
                                ShapeReveal.create(gaoBai8, 750,
                                        SideCut.show(Side.LEFT), false),
                                Delay.duration(INTERVAL_TIME),
                                new Parallel(new TransSurface(500, gaoBai9,
                                        Pivot.CENTER), ShapeReveal.create(gaoBai9,
                                        1300, SideCut.show(Side.LEFT), false)),
                                Delay.duration(INTERVAL_TIME),
                                new Parallel(new TransSurface(500, gaoBai10,
                                        Pivot.CENTER), ShapeReveal.create(gaoBai10,
                                        1300, SideCut.show(Side.LEFT), false)),

                                Delay.duration(INTERVAL_TIME),
                                new Parallel(new TransSurface(500, gaoBai11,
                                        Pivot.CENTER), ShapeReveal.create(gaoBai11,
                                        1300, SideCut.show(Side.LEFT), false)),

                                Delay.duration(INTERVAL_TIME),
                                // 消失
                                new Parallel(ShapeReveal.create(gaoBai8, 1500,
                                        SideCut.hide(Side.LEFT), true), new Sequential(
                                        Delay.duration(250), ShapeReveal.create(
                                        gaoBai9, 1500, SideCut.hide(Side.LEFT),
                                        true)), new Sequential(Delay
                                        .duration(500), ShapeReveal.create(gaoBai10,
                                        1500, SideCut.hide(Side.LEFT), true)),
                                        new Sequential(Delay.duration(500), ShapeReveal
                                                .create(gaoBai11, 1500,
                                                        SideCut.hide(Side.LEFT), true)))),
                        listener);
    }

    public static void play4(Context ctx, TextSurface textSurface,
                             AssetManager assetManager, IEndListener listener) {

        final Typeface robotoBlack = Typeface.createFromAsset(assetManager,
                "fonts/Roboto-Black.ttf");
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTypeface(robotoBlack);

        Text gaoBai12 = createText(ctx.getString(R.string.gaobai_12), paint,
                Color.WHITE, Align.SURFACE_CENTER, FONT_BIG);
        Text gaoBai13 = createText(ctx.getString(R.string.gaobai_13), paint,
                Color.RED, Align.BOTTOM_OF, gaoBai12, FONT_SMALL);
        Text gaoBai14 = createText(ctx.getString(R.string.gaobai_14), paint,
                Color.RED, Align.BOTTOM_OF, gaoBai13, FONT_SMALL);
        Text gaoBai15 = createText(ctx.getString(R.string.gaobai_15), paint,
                Color.RED, Align.BOTTOM_OF, gaoBai14, FONT_SMALL);

        // 因为没有修改源代码的方法, 写起来比较繁琐, 实际使用中, 可以根据需要,修改代码为List<>的形式,这样代码清晰更好一些.
        textSurface
                .play(
                        // 创建一个串行依次执行的动画队列(从前到后执行动画).
                        new Sequential(

                                // //第四段----12 13 14 15
                                ShapeReveal.create(gaoBai12, 750,
                                        SideCut.show(Side.LEFT), false),
                                Delay.duration(INTERVAL_TIME),
                                new Parallel(new TransSurface(500, gaoBai13,
                                        Pivot.CENTER), ShapeReveal.create(gaoBai13,
                                        1300, SideCut.show(Side.LEFT), false)),
                                Delay.duration(INTERVAL_TIME),
                                new Parallel(new TransSurface(500, gaoBai14,
                                        Pivot.CENTER), ShapeReveal.create(gaoBai14,
                                        1300, SideCut.show(Side.LEFT), false)),

                                Delay.duration(INTERVAL_TIME),
                                new Parallel(new TransSurface(500, gaoBai15,
                                        Pivot.CENTER), ShapeReveal.create(gaoBai15,
                                        1300, SideCut.show(Side.LEFT), false)),

                                Delay.duration(INTERVAL_TIME),
                                // 消失
                                new Parallel(
                                        ShapeReveal.create(gaoBai12, 1500,
                                                SideCut.hide(Side.LEFT), true),
                                        new Sequential(Delay.duration(250), ShapeReveal
                                                .create(gaoBai13, 1500,
                                                        SideCut.hide(Side.LEFT), true)),
                                        new Sequential(Delay.duration(500), ShapeReveal
                                                .create(gaoBai14, 1500,
                                                        SideCut.hide(Side.LEFT), true)),
                                        new Sequential(Delay.duration(500), ShapeReveal
                                                .create(gaoBai15, 1500,
                                                        SideCut.hide(Side.LEFT), true)))),
                        listener);
    }

    public static void play5(Context ctx, TextSurface textSurface,
                             AssetManager assetManager, IEndListener listener) {

        final Typeface robotoBlack = Typeface.createFromAsset(assetManager,
                "fonts/Roboto-Black.ttf");
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTypeface(robotoBlack);

        Text gaoBai16 = createText(ctx.getString(R.string.gaobai_16), paint,
                Color.WHITE, Align.SURFACE_CENTER, FONT_BIG);
        Text gaoBai17 = createText(ctx.getString(R.string.gaobai_17), paint,
                Color.RED, Align.BOTTOM_OF, gaoBai16, FONT_SMALL);
        Text gaoBai18 = createText(ctx.getString(R.string.gaobai_18), paint,
                Color.RED, Align.BOTTOM_OF, gaoBai17, FONT_SMALL);
        Text gaoBai19 = createText(ctx.getString(R.string.gaobai_19), paint,
                Color.RED, Align.BOTTOM_OF, gaoBai18, FONT_SMALL);
        Text gaoBai20 = createText(ctx.getString(R.string.gaobai_20), paint,
                Color.RED, Align.BOTTOM_OF, gaoBai19, FONT_SMALL);

        // 因为没有修改源代码的方法, 写起来比较繁琐, 实际使用中, 可以根据需要,修改代码为List<>的形式,这样代码清晰更好一些.
        textSurface
                .play(
                        // 创建一个串行依次执行的动画队列(从前到后执行动画).
                        new Sequential(
                                // 第五段----16 17 18 19 20
                                ShapeReveal.create(gaoBai16, 750,
                                        SideCut.show(Side.LEFT), false),
                                Delay.duration(INTERVAL_TIME),
                                new Parallel(new TransSurface(500, gaoBai17,
                                        Pivot.CENTER), ShapeReveal.create(gaoBai17,
                                        1300, SideCut.show(Side.LEFT), false)),
                                Delay.duration(INTERVAL_TIME),
                                new Parallel(new TransSurface(500, gaoBai18,
                                        Pivot.CENTER), ShapeReveal.create(gaoBai18,
                                        1300, SideCut.show(Side.LEFT), false)),

                                Delay.duration(INTERVAL_TIME),
                                new Parallel(new TransSurface(500, gaoBai19,
                                        Pivot.CENTER), ShapeReveal.create(gaoBai19,
                                        1300, SideCut.show(Side.LEFT), false)),

                                Delay.duration(INTERVAL_TIME),
                                new Parallel(new TransSurface(500, gaoBai20,
                                        Pivot.CENTER), ShapeReveal.create(gaoBai20,
                                        1300, SideCut.show(Side.LEFT), false)),
                                Delay.duration(INTERVAL_TIME),
                                // 消失
                                new Parallel(
                                        ShapeReveal.create(gaoBai16, 1500,
                                                SideCut.hide(Side.LEFT), true),
                                        new Sequential(Delay.duration(250), ShapeReveal
                                                .create(gaoBai17, 1500,
                                                        SideCut.hide(Side.LEFT), true)),
                                        new Sequential(Delay.duration(500), ShapeReveal
                                                .create(gaoBai18, 1500,
                                                        SideCut.hide(Side.LEFT), true)),
                                        new Sequential(Delay.duration(500), ShapeReveal
                                                .create(gaoBai19, 1500,
                                                        SideCut.hide(Side.LEFT), true)),
                                        new Sequential(Delay.duration(500), ShapeReveal
                                                .create(gaoBai20, 1500,
                                                        SideCut.hide(Side.LEFT), true)))),
                        listener);
    }

    public static void play6(Context ctx, TextSurface textSurface,
                             AssetManager assetManager, IEndListener listener) {

        final Typeface robotoBlack = Typeface.createFromAsset(assetManager,
                "fonts/Roboto-Black.ttf");
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTypeface(robotoBlack);

        Text end1 = createText(ctx.getString(R.string.gaobai_end1), paint,
                Color.WHITE, Align.SURFACE_CENTER, FONT_BIG);
        Text end2 = createText(ctx.getString(R.string.gaobai_end2), paint,
                Color.RED, Align.BOTTOM_OF, end1, FONT_SMALL);
        Text end3 = createText(ctx.getString(R.string.gaobai_end3), paint,
                Color.RED, Align.BOTTOM_OF, end2, FONT_SMALL);

        // 因为没有修改源代码的方法, 写起来比较繁琐, 实际使用中, 可以根据需要,修改代码为List<>的形式,这样代码清晰更好一些.
        textSurface.play(
                // 创建一个串行依次执行的动画队列(从前到后执行动画).
                new Sequential(
                        // 第五段----16 17 18 19 20
                        ShapeReveal.create(end1, 750, SideCut.show(Side.LEFT),
                                false), Delay.duration(INTERVAL_TIME),
                        new Parallel(new TransSurface(500, end2, Pivot.CENTER),
                                ShapeReveal.create(end2, 1300,
                                        SideCut.show(Side.LEFT), false)), Delay
                        .duration(INTERVAL_TIME), new Parallel(
                        new TransSurface(500, end3, Pivot.CENTER),
                        ShapeReveal.create(end3, 1300,
                                SideCut.show(Side.LEFT), false))),
                listener);
    }

}
