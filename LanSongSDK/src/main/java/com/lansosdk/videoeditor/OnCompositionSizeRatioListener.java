package com.lansosdk.videoeditor;

/**
 * Description : 杭州蓝松科技有限公司
 *
 * @author guozhijun
 * @date 2020-03-04
 */
public interface OnCompositionSizeRatioListener {
    /**
     * 合成(容器)的宽高
     *
     * @param viewWidth  调整后的容器宽高
     * @param viewHeight 调整后的容器宽高
     */
    void onSizeChanged(int viewWidth, int viewHeight);
}
