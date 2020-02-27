package com.lansosdk.videoeditor;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

import com.lansosdk.box.LanSoEditorBox;

public class LanSongUtil {

    static int bmtcnt = 0;

    /**
     * 检查是否有 摄像头和麦克风的权限.
     *
     * @param ctx
     * @return
     */
    public static boolean checkRecordPermission(Context ctx) {
        boolean ret1 = LanSoEditorBox.cameraIsCanUse();
        boolean ret2 = LanSoEditorBox.checkMicPermission(ctx);
        return ret1 && ret2;
    }

    /**
     * 隐藏虚拟按键，并且全屏
     */
    public static void hideBottomUIMenu(Activity act) {
        // 隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT < 19) { // lower
            // api
            View v = act.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else {
            // for new api versions.
            View decorView = act.getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }
    public static boolean isFullScreenRatio(int padWidth, int padHeight) {
        if (padWidth > padHeight) { // 其他则是屏幕比大于16:9的屏幕
            float ratio = (float) padWidth / (float) padHeight;
            return ratio > 16f / 9f;
        } else {
            float ratio = (float) padHeight / (float) padWidth;
            return ratio > 16f / 9f;
        }
    }

    /**
     * 当数据不是16的倍数的时候, 把他调整成16的倍数,
     * <p>
     * 如果是18,19这样接近16,则等于16, 等于缩小了原有的画面, 如果是25,28这样接近32,则等于32, 等于稍微拉伸了原来的画面,
     * 因为最多缩小或拉伸8个像素, 还不至于画面严重变形,而又兼容编码器的要求,故可以这样做.
     * <p>
     * 16, 17, 18, 19,20,21,22,23 ==>16; 24,25,26,27,28,29,30,31,32==>32;
     *
     * @param value
     * @return
     */
    public static int make16Multi(int value) {

        if (value < 16) {
            return value;
        } else {
            value += 8;
            int val2 = value / 16;
            val2 *= 16;
            return val2;
        }
    }

    /**
     * 获取lansosdk的建议码率; 这个码率不是唯一的, 仅仅是我们建议这样设置,
     * 如果您对码率理解很清楚或有一定的压缩要求,则完全可以不用我们的建议,自行设置.
     *
     * @param wxh 宽度和高度的乘积;
     * @return
     */
    public static int getSuggestBitRate(int wxh) {
        if (wxh <= 480 * 480) {
            return 1000 * 1024;
        } else if (wxh <= 640 * 480) {
            return 1500 * 1024;
        } else if (wxh <= 800 * 480) {
            return 1800 * 1024;
        } else if (wxh <= 960 * 544) {
            return 2300 * 1024;
        } else if (wxh <= 1280 * 720) {
            return 2800 * 1024;
        } else if (wxh <= 1920 * 1088) {
            return 3000 * 1024;
        } else {
            return 3500 * 1024;
        }
    }

    public static int checkSuggestBitRate(int wxh, int bitrate) {
        int sugg = getSuggestBitRate(wxh);
        return bitrate < sugg ? sugg : bitrate; // 如果设置过来的码率小于建议码率,则返回建议码率,不然返回设置码率
    }

    /**
     * 获取到当前Activity的角度, [只是放到这里, 暂时没有测试] //TODO
     *
     * @param ctx
     * @return
     */
    public static int getActivityAngle(Context ctx) {
        int rotation = ((WindowManager) ctx
                .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        return degrees;
    }

}
