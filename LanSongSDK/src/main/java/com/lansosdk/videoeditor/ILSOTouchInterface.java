package com.lansosdk.videoeditor;

import com.lansosdk.box.ILayerInterface;
import com.lansosdk.box.LSOLayer;

/**
 * Description : 杭州蓝松科技有限公司
 *
 * @author guozhijun
 * @date 2021/1/8
 */
public interface ILSOTouchInterface {
    public ILayerInterface getTouchPointLayer(float x, float y);
    public int getViewWidth();
    public int getViewHeight();
}
