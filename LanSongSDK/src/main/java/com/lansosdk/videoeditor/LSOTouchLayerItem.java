package com.lansosdk.videoeditor;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import com.lansosdk.box.Layer;

import java.io.IOException;
import java.io.InputStream;


/**
 * 每个图层的控制类;
 */
public class LSOTouchLayerItem {
    private static final float MIN_SCALE = 0.15f;


    private static final int HELP_BOX_PAD = 25;

    private static final int BUTTON_WIDTH = 30;
    private static Bitmap deleteBit;
    private static Bitmap rotateBit;


    public Rect srcRect;// 原始图片坐标


    public RectF dstRect;// 绘制目标坐标


    public RectF dstRect_one;

    public RectF deleteRect;// 删除按钮位置
    public RectF rotateRect;// 旋转按钮位置


    public RectF detectRotateRect;

    public RectF detectRotateRect_one;


    public RectF detectDeleteRect;

    private Layer layer;
    RectF helpBox;
    boolean isDrawHelpTool = false;
    private Rect helpToolsRect;
    private float roatetAngle = 0;
    private Paint dstPaint = new Paint();
    private Paint paint = new Paint();
    private Paint helpBoxPaint = new Paint();
    private float initWidth;// 加入屏幕时原始宽度
    private Paint greenPaint = new Paint();

    public LSOTouchLayerItem(Context context) {

        helpBoxPaint.setColor(Color.BLACK);
        helpBoxPaint.setStyle(Style.STROKE);
        helpBoxPaint.setAntiAlias(true);
        helpBoxPaint.setStrokeWidth(4);

        dstPaint = new Paint();
        dstPaint.setColor(Color.RED);
        dstPaint.setAlpha(120);

        greenPaint = new Paint();
        greenPaint.setColor(Color.GREEN);
        greenPaint.setAlpha(120);

        // 导入工具按钮位图
        if (deleteBit == null) {
            AssetManager assetManager = context.getAssets();
            try {
                InputStream inputStream = assetManager.open("sticker_delete.png");
                deleteBit = BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (rotateBit == null) {
            AssetManager assetManager = context.getAssets();
            try {
                InputStream inputStream = assetManager.open("sticker_rotate.png");
                rotateBit = BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void init(Layer layer, View parentView) {

        if(layer!=null && layer.getScaleWidth() >0 && layer.getScaleHeight()>0){
            this.layer=layer;
            //得到缩放后的宽高;
            int width=(int)layer.getScaleWidth();
            int height=(int)layer.getScaleHeight();



            this.srcRect = new Rect(0, 0, width, height);


            int bitWidth = Math.min(width, parentView.getWidth() >> 1);
            int bitHeight = (int) bitWidth * height / width;
            layer.setScaledValue(bitWidth,bitHeight);

            int left = (parentView.getWidth() >> 1) - (bitWidth >> 1);
            int top = (parentView.getHeight() >> 1) - (bitHeight >> 1);
            this.dstRect = new RectF(left, top, left + bitWidth, top + bitHeight);


            initWidth = this.dstRect.width();// 记录原始宽度
            this.isDrawHelpTool = true;
            this.helpBox = new RectF(this.dstRect);//记录原始矩形
            this.dstRect_one = new RectF(this.dstRect);

            updateHelpBoxRect();

            helpToolsRect = new Rect(0, 0,deleteBit.getWidth(),deleteBit.getHeight());

            deleteRect = new RectF(helpBox.left - BUTTON_WIDTH, helpBox.top
                    - BUTTON_WIDTH, helpBox.left + BUTTON_WIDTH, helpBox.top
                    + BUTTON_WIDTH);
            rotateRect = new RectF(helpBox.right - BUTTON_WIDTH, helpBox.bottom
                    - BUTTON_WIDTH, helpBox.right + BUTTON_WIDTH, helpBox.bottom
                    + BUTTON_WIDTH);

            detectRotateRect = new RectF(rotateRect);
            detectRotateRect_one = new RectF(rotateRect);
            detectDeleteRect = new RectF(deleteRect);
        }

    }

    private void updateHelpBoxRect() {
        this.helpBox.left -= HELP_BOX_PAD;//每次减25
        this.helpBox.right += HELP_BOX_PAD;
        this.helpBox.top -= HELP_BOX_PAD;
        this.helpBox.bottom += HELP_BOX_PAD;
    }

    /**
     * 位置更新
     *
     * @param dx
     * @param dy
     */
    public void updatePos(final float dx, final float dy) {

        dstRect.offset(dx, dy);

        // 工具按钮随之移动
        helpBox.offset(dx, dy);
        deleteRect.offset(dx, dy);
        rotateRect.offset(dx, dy);

        this.detectRotateRect.offset(dx, dy);
        this.detectDeleteRect.offset(dx, dy);

        if(layer!=null){
            layer.setPosition(dstRect.centerX(),dstRect.centerY());
        }
    }

    public void removeLayer(){
        if(layer!=null){
            layer.setDeleteFromPad();
            layer=null;
        }
    }

    /**
     * 旋转 缩放 更新
     *
     * @param dx
     * @param dy
     */
    public void updateRotateAndScale(final float oldx, final float oldy,
                                     final float dx, final float dy) {
        //先拿到中心点的坐标，这个是你要绘制的中心坐标
        float c_x = dstRect.centerX();
        float c_y = dstRect.centerY();

        //第一次旋转按钮的中心坐标
        float x_one = detectRotateRect_one.centerX();
        float y_one = detectRotateRect_one.centerY();
        //和中心点的距离
        float xa_one = y_one - c_x;
        float ya_one = y_one - c_y;
        float oneLen = (float) Math.sqrt(ya_one * ya_one + xa_one * xa_one);
        //旋转按钮的中心坐标
        float x = this.detectRotateRect.centerX();
        float y = this.detectRotateRect.centerY();


        // float x = oldx;
        // float y = oldy;
        //先拿到旋转位置中心坐标的位置改变
        float n_x = x + dx;
        float n_y = y + dy;

        float xa = x - c_x;
        float ya = y - c_y;

        float xb = n_x - c_x;
        float yb = n_y - c_y;
        //算出中点和，缩放按钮之间的距离
        float srcLen = (float) Math.sqrt(xa * xa + ya * ya);
        float curLen = (float) Math.sqrt(xb * xb + yb * yb);


        float scale = curLen / srcLen;// 计算缩放比
        //用中心点矩阵的真实高度去*缩放比
        float newWidth = dstRect.width() * scale;
        if (newWidth / initWidth < MIN_SCALE) {// 最小缩放值检测
            return;
        }

        scaleRect(this.dstRect, scale);// 缩放目标矩形

        // 重新计算工具箱坐标
        helpBox.set(dstRect);
        updateHelpBoxRect();// 重新计算
        //移动按钮
        rotateRect.offsetTo(helpBox.right - BUTTON_WIDTH, helpBox.bottom
                - BUTTON_WIDTH);
        deleteRect.offsetTo(helpBox.left - BUTTON_WIDTH, helpBox.top
                - BUTTON_WIDTH);

        detectRotateRect.offsetTo(helpBox.right - BUTTON_WIDTH, helpBox.bottom
                - BUTTON_WIDTH);
        detectDeleteRect.offsetTo(helpBox.left - BUTTON_WIDTH, helpBox.top
                - BUTTON_WIDTH);

        double cos = (xa * xb + ya * yb) / (srcLen * curLen);
        if (cos > 1 || cos < -1)
            return;
        //通过余弦值返回一个弧度
        float angle = (float) Math.toDegrees(Math.acos(cos));

        // 定理

        //通过坐标系去确定转向
        float calMatrix = xa * yb - xb * ya;// 行列式计算 确定转动方向
        int flag = calMatrix > 0 ? 1 : -1;
        angle = flag * angle;

        roatetAngle += angle;//相对原本位置的角度-------------------------------------------------



        if(layer!=null){
            layer.setRotate(roatetAngle);

            float width= helpBox.width() ;
            float height=helpBox.height();

            layer.setScaledValue(width,height);
        }
        //两个按钮区域
        rotateRect(this.detectRotateRect, this.dstRect.centerX(),
                this.dstRect.centerY(), roatetAngle);
        rotateRect(this.detectDeleteRect, this.dstRect.centerX(),
                this.dstRect.centerY(), roatetAngle);

    }


    public void draw(Canvas canvas) {

        if (this.isDrawHelpTool) {// 绘制辅助工具线
            //用来保存canvas的状态
            canvas.save();
            //让你的框开始旋转
            canvas.rotate(roatetAngle, helpBox.centerX(), helpBox.centerY());
            //画一个圆角矩形
            /*left 长方形左侧的x坐标
            top 长方形顶的Y坐标
            right 长方形右侧的X坐标
            bottom 长方形底的Y坐标*/
            canvas.drawRoundRect(helpBox, 10, 10, helpBoxPaint);
            // 绘制工具按钮
            canvas.drawBitmap(deleteBit, helpToolsRect, deleteRect, null);
            canvas.drawBitmap(rotateBit, helpToolsRect, rotateRect, null);
            canvas.restore();
        }
    }
    /**
     * 缩放指定矩形
     *
     * @param rect
     * @param scale
     */
    public static void scaleRect(RectF rect, float scale) {
        float w = rect.width();
        float h = rect.height();

        float newW = scale * w;
        float newH = scale * h;

        float dx = (newW - w) / 2;
        float dy = (newH - h) / 2;

        rect.left -= dx;
        rect.top -= dy;
        rect.right += dx;
        rect.bottom += dy;
    }

    /**
     * 矩形绕指定点旋转
     *
     * @param rect
     * @param roatetAngle
     */
    public static void rotateRect(RectF rect, float center_x, float center_y,
                                  float roatetAngle) {
        float x = rect.centerX();
        float y = rect.centerY();
        float sinA = (float) Math.sin(Math.toRadians(roatetAngle));
        float cosA = (float) Math.cos(Math.toRadians(roatetAngle));
        float newX = center_x + (x - center_x) * cosA - (y - center_y) * sinA;
        float newY = center_y + (y - center_y) * cosA + (x - center_x) * sinA;

        float dx = newX - x;
        float dy = newY - y;

        rect.offset(dx, dy);
    }

}
