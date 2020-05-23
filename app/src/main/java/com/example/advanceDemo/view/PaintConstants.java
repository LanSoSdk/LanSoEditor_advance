package com.example.advanceDemo.view;

import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Color;

/**
 * 软件中涉及的一些常量
 */
public class PaintConstants {

    public static final float TOUCH_TOLERANCE = 4;
    public static int PEN_SIZE = 16;
    public static int PEN_COLOR = Color.RED;
    public static int ERASE_SIZE = 4;
    public static Blur BLUR_TYPE = BlurMaskFilter.Blur.NORMAL;
    public static int TRANSPARENT = 15;

    // 画透明度的大小

    private PaintConstants() {

    }

    /**
     * 图片缩放比例阈值
     */
    public static final class SCALE {
        /**
         * 最大缩放比例
         */
        public static final float MAX_SCALE = 15f;
        /**
         * 最小缩放比例
         */
        public static final float MIN_SCALE = 1.0f;
    }

    /**
     * 画布模式的选择
     */
    public static class SELECTOR {
        // 是否可以旋转
        public static boolean HAIR_RURN = false;
        // 是否保持比例
        public static boolean KEEP_SCALE = true;
        // 固定
        public static boolean KEEP_IMAGE = false;
        // 挑染
        public static boolean COLORING = false;
        // 橡皮擦
        public static boolean ERASE = false;
    }

    /**
     * 状态
     */
    public static final class MODE {
        /**
         * 初始状态
         */
        public static final int NONE = 0;
        /**
         * 拖动
         */
        public static final int DRAG = 1;
        /**
         * 缩放
         */
        public static final int ZOOM = 2;
        /**
         * 擦除
         */
        public static final int ERASE = 2;
        /**
         * 染色
         */
        public static final int COLORING = 2;
    }

    /**
     * 存储图片的默认路径
     */
    public static final class PATH {
        public static final String SAVE_PATH = "/sdcard/lansongBox";
    }

    /**
     * 画壁类型的选择
     */
    public static final class PEN_TYPE {
        public static final int PLAIN_PEN = 1;
        public static final int ERASER = 2;
        public static final int BLUR = 3;
    }

    /**
     * 默认图层的颜色和背景色
     */
    public static final class DEFAULT {
        public static final int PEN_COLOR = Color.BLACK;
        public static final int BACKGROUND_COLOR = Color.WHITE;
    }
}
