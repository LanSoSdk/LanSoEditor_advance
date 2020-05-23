package com.lansosdk.videoeditor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.lansosdk.box.LSOLayer;

import java.util.LinkedHashMap;

import static com.lansosdk.videoeditor.LSOLayerTouch.VOID_VALUE;

/**
 * 贴图操作控件
 */
public class LSOLayerTouchView extends View {
    private static int STATUS_IDLE = 0;
    private static int STATUS_MOVE = 1;// 移动状态
    private static int STATUS_DELETE = 2;// 删除状态
    private static int STATUS_ROTATE = 3;// 图片旋转状态
    public static  int STATUS_DOUBLE_POINT = 4; // 双指操作

    private int imageCount;// 已加入照片的数量
    private int currentStatus;// 当前状态
    private LSOLayerTouch currentItem;// 当前操作的贴图数据
    private float oldx, oldy;

    private Paint rectPaint = new Paint();
    private Paint boxPaint = new Paint();

    private LinkedHashMap<Integer, LSOLayerTouch> layerMap = new LinkedHashMap<Integer, LSOLayerTouch>();// 存贮每层贴图数据
    private float heightRatio;
    private float widthRatio;
    /**
     *  记录上一次双指的间隔距离 和 角度
     */
    private float lastSpace;
    private float lastDegrees;
    private float lastScale;

    public LSOLayerTouchView(Context context) {
        super(context);
        init(context);
    }

    public LSOLayerTouchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LSOLayerTouchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        currentStatus = STATUS_IDLE;
        //设置画笔的属性
        rectPaint.setColor(Color.RED);

        rectPaint.setAlpha(100);

    }

    private LSOConcatCompositionView compView;

    public void setVideoCompositionView(LSOConcatCompositionView compView){
        if(compView!=null){
            this.compView=compView;
            this.widthRatio = (float) compView.getWidth() / compView.getCompWidth();
            this.heightRatio = (float) compView.getHeight() / compView.getCompHeight();
        }
    }


    public void addLayer(final LSOLayer layer) {
        if(layer!=null && !layer.isRemovedFromComp()){
            LSOLayerTouch item = new LSOLayerTouch(this.getContext());

            item.init(layer, compView);

            if (currentItem != null) {
                currentItem.drawHelpTool = false;
            }
            currentItem = item;
            layerMap.put(++imageCount, item);
            this.invalidate();// 重绘视图
        }
    }

    public void removeLayer(LSOLayer layer){
        if(layerMap!=null && layerMap.containsValue(layer)){
            layerMap.remove(layer);
        }
    }
    /**
     * 绘制客户页面
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //遍历这个图片数组，拿每一个图片去绘制
        for (Integer id : layerMap.keySet()) {
            LSOLayerTouch item = layerMap.get(id);
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
                for (Integer id : layerMap.keySet()) {

                    LSOLayerTouch item = layerMap.get(id);//拿到当前贴图
                    if (item.detectDeleteRect.contains(x, y)) {// 删除模式
                        Log.d("StickerView", "item.detectDeleteRect.contains(x, y):" + item.detectDeleteRect.contains(x, y));
                        // ret = true;
                        deleteId = id;
                        currentStatus = STATUS_DELETE;//你点到了左上角那么就删除
                    } else if (item.detectRotateRect.contains(x, y)) {// 点击了旋转按钮
                        ret = true;
                        // 当前操作的贴图数据
                        if (currentItem != null) {
                            //是否绘制辅助线？
                            currentItem.drawHelpTool = false;
                        }
                        currentItem = item;
                        currentItem.drawHelpTool = true;
                        currentStatus = STATUS_ROTATE;//转化旋转模式
                        oldx = x;
                        oldy = y;
                    } else if (item.dstRect.contains(x, y)) {// 移动模式
                        // 被选中一张贴图
                        ret = true;
                        if (currentItem != null) {
                            currentItem.drawHelpTool = false;
                        }
                        currentItem = item;
                        currentItem.drawHelpTool = true;
                        currentStatus = STATUS_MOVE;
                        oldx = x;
                        oldy = y;
                    }// end if
                }

                //点击之后的选择
                if (!ret && currentItem != null && currentStatus == STATUS_IDLE) {// 没有贴图被选择
                    currentItem.drawHelpTool = false;
                    currentItem = null;
                    invalidate();
                }

                if (deleteId > 0 && currentStatus == STATUS_DELETE) {// 删除选定贴图
                    LSOLayerTouch item = layerMap.get(deleteId);
                    item.removeLayer();
                    layerMap.remove(deleteId);
                    currentStatus = STATUS_IDLE;// 返回空闲状态
                    disappearIconBorder(); // 移除border

                }// end if

                break;
            //  第二个手指按下
            case MotionEvent.ACTION_POINTER_DOWN:
                // 有两个手指按下了
                if (event.getPointerCount() == 2) {
                    if (currentItem.dstRect.contains(event.getX(1), event.getY(1))) {
                        currentStatus = STATUS_DOUBLE_POINT;
                        lastSpace = currentItem.getSpacing(event);
                        lastDegrees = currentItem.getDegree(event);
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                ret = true;
                if (currentStatus == STATUS_MOVE) {// 移动贴图
                    float dx = x - oldx;
                    float dy = y - oldy;
                    //你手指按下的位置
                    if (currentItem != null) {
                        //更新框和按钮的位置
                        currentItem.updatePos(dx, dy,widthRatio,heightRatio);
                        //重新绘制
                        invalidate();
                    }// end if
                    oldx = x;
                    oldy = y;
                } else if (currentStatus == STATUS_ROTATE) {// 旋转 缩放图片操作
                    // System.out.println("旋转");
                    float dx = x - oldx;
                    float dy = y - oldy;
                    if (currentItem != null) {
                        currentItem.updateRotateAndScale(oldx, oldy, dx, dy);// 旋转
                        invalidate();
                    }// end if
                    oldx = x;
                    oldy = y;
                }else if (currentStatus ==STATUS_DOUBLE_POINT){

                    float currentSpace = currentItem.getSpacing(event);
                    float currentDegrees = currentItem.getDegree(event);
                    if (currentSpace!= VOID_VALUE && currentDegrees!=VOID_VALUE){
                        float scaleRatio = currentSpace / lastSpace;
                        float degrees = currentDegrees - lastDegrees;

                        // 控制精度
                        scaleRatio = (float) (Math.floor(scaleRatio*100)/100);
                        if (lastScale == scaleRatio){
                            return true;
                        }
                        degrees = (float) (Math.floor(degrees*1000)/1000);


                        currentItem.doublePointScaleAndRotate(scaleRatio,degrees);
                        postInvalidate();
                        // 记录上一次缩放值
                        lastScale = scaleRatio;
                        // 记录上一次两指距离
                        lastSpace = currentSpace;
                        lastDegrees = currentDegrees;

                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                ret = false;
                currentStatus = STATUS_IDLE;
            case MotionEvent.ACTION_POINTER_UP:
                currentStatus = STATUS_IDLE;
                break;
        }// end switch
        return ret;
    }

    /**
     * 消除左上角的删除按钮, 右下角的拖动图标, 和黑色的边框.
     */
    public void disappearIconBorder() {
        if (currentItem != null) {
            currentItem.drawHelpTool = false;
        }
        currentItem = null;
        invalidate();
    }

    /**
     * 更新是否要显示的边框;
     */
    public void updateLayerStatus(){
        boolean needUpdate=false;

        for (Integer id : layerMap.keySet()) {
            LSOLayerTouch item = layerMap.get(id);
            if(item.getLayer()!=null){
                if(!item.getLayer().isDisplayAtCurrentTime() && item.drawHelpTool){
                    item.drawHelpTool =false;
                    needUpdate=true;
                }
            }
        }
        if(needUpdate){
            invalidate();
        }

    }


    /**
     * 清除所有的贴纸
     */
    public void clear() {
        layerMap.clear();
        this.invalidate();
    }
}// end class
