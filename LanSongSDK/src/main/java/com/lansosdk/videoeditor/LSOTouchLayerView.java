package com.lansosdk.videoeditor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.lansosdk.box.Layer;

import java.util.LinkedHashMap;

/**
 * 图层的控制类
 * 使用方法是:
 * 1. 覆盖在DrawPadView上面, 同一个位置, 同一个大小.
 * 2, 在要增加图层的时候, 把图层对象设置到这个类中即可. 有:addLayer方法;
 *
 * 修改自 panyi
 */
public class LSOTouchLayerView extends View {
    private static int STATUS_IDLE = 0;
    private static int STATUS_MOVE = 1;// 移动状态
    private static int STATUS_DELETE = 2;// 删除状态
    private static int STATUS_ROTATE = 3;// 图片旋转状态

    private int imageCount;// 已加入照片的数量
    private Context mContext;
    private int currentStatus;// 当前状态
    private LSOTouchLayerItem currentItem;// 当前操作的贴图数据
    private float oldx, oldy;

    private Paint rectPaint = new Paint();
    private Paint boxPaint = new Paint();

    private LinkedHashMap<Integer, LSOTouchLayerItem> layerItems = new LinkedHashMap<Integer, LSOTouchLayerItem>();// 存贮每层贴图数据

    public LSOTouchLayerView(Context context) {
        super(context);
        init(context);
    }

    public LSOTouchLayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LSOTouchLayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        currentStatus = STATUS_IDLE;//首先什么状态都不是
        //设置画笔的属性
        rectPaint.setColor(Color.RED);

        rectPaint.setAlpha(100);

    }


    /**
     * 增加一个图层控制,
     * 增加后, 会在图层的四周出现黑框,并在左上角出现x号, 在右下角出现白点, 用来拖动旋转缩放图层
     * @param layer 图层对象;
     */
    public void addLayer(final Layer layer) {
        if(layer!=null && !layer.isDeleteFromPad()){
            LSOTouchLayerItem item = new LSOTouchLayerItem(this.getContext());
            item.init(layer, this);
            if (currentItem != null) {
                currentItem.isDrawHelpTool = false;
            }
            currentItem = item;
            layerItems.put(++imageCount, item);
            this.invalidate();// 重绘视图
        }
    }

    /**
     * 绘制客户页面
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Integer id : layerItems.keySet()) {
            LSOTouchLayerItem item = layerItems.get(id);
            item.draw(canvas);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean ret = super.onTouchEvent(event);// 是否向下传递事件标志 true为消耗

        int action = event.getAction();
        float x = event.getX(); //---------------当前用户的X值；
        float y = event.getY();//-----------------当前用户的Y值

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:

                int deleteId = -1;
                for (Integer id : layerItems.keySet()) {

                    LSOTouchLayerItem item = layerItems.get(id);//拿到当前贴图
                    if (item.detectDeleteRect.contains(x, y)) {// 删除模式
                        // ret = true;
                        deleteId = id;
                        currentStatus = STATUS_DELETE;//你点到了左上角那么就删除
                    } else if (item.detectRotateRect.contains(x, y)) {// 点击了旋转按钮
                        ret = true;
                        // 当前操作的贴图数据
                        if (currentItem != null) {
                            //是否绘制辅助线？
                            currentItem.isDrawHelpTool = false;
                        }
                        currentItem = item;
                        currentItem.isDrawHelpTool = true;
                        currentStatus = STATUS_ROTATE;//转化旋转模式
                        oldx = x;
                        oldy = y;
                    } else if (item.dstRect.contains(x, y)) {// 移动模式
                        // 被选中一张贴图
                        ret = true;
                        if (currentItem != null) {
                            currentItem.isDrawHelpTool = false;
                        }
                        currentItem = item;
                        currentItem.isDrawHelpTool = true;
                        currentStatus = STATUS_MOVE;
                        oldx = x;
                        oldy = y;
                    }// end if
                }


                //点击之后的选择
                if (!ret && currentItem != null && currentStatus == STATUS_IDLE) {// 没有贴图被选择
                    currentItem.isDrawHelpTool = false;
                    currentItem = null;
                    invalidate();
                }

                if (deleteId > 0 && currentStatus == STATUS_DELETE) {// 删除选定贴图
                   LSOTouchLayerItem item= layerItems.get(deleteId);
                   item.removeLayer();
                    layerItems.remove(deleteId);
                    currentStatus = STATUS_IDLE;// 返回空闲状态
                    invalidate();
                }// end if

                break;
            case MotionEvent.ACTION_MOVE:
                ret = true;
                if (currentStatus == STATUS_MOVE) {// 移动贴图
                    float dx = x - oldx;
                    float dy = y - oldy;
                    //你手指按下的位置
                    if (currentItem != null) {
                        //更新框和按钮的位置
                        currentItem.updatePos(dx, dy);
                        //重新绘制
                        invalidate();
                    }// end if
                    oldx = x;
                    oldy = y;
                } else if (currentStatus == STATUS_ROTATE) {// 旋转 缩放图片操作
                    float dx = x - oldx;
                    float dy = y - oldy;
                    if (currentItem != null) {
                        currentItem.updateRotateAndScale(oldx, oldy, dx, dy);// 旋转
                        invalidate();
                    }// end if
                    oldx = x;
                    oldy = y;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                ret = false;
                currentStatus = STATUS_IDLE;
                break;
        }// end switch
        return ret;
    }

    public LinkedHashMap<Integer, LSOTouchLayerItem> getLayerItems() {
        return layerItems;
    }

    /**
     * 消除左上角的删除按钮, 右下角的拖动图标, 和黑色的边框.
     */
    public void disappearIconBorder() {
        if (currentItem != null) {
            currentItem.isDrawHelpTool = false;
        }
        currentItem = null;
        invalidate();
    }
    public void clear() {
        layerItems.clear();
        this.invalidate();
    }
}
