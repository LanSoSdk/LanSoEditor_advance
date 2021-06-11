package com.lansosdk.videoeditor;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;


import com.lansosdk.box.ILayerInterface;
import com.lansosdk.box.LSOPoint;
import com.lansosdk.box.LSORect;

import java.util.LinkedHashMap;


/**
 *
 * 图层的点击, 手指操作辅助类;
 */
public class LSOLayerTouchView extends View {

    private static int STATUS_IDLE = 0;
    private static int STATUS_MOVE = 1;
    private static int STATUS_DELETE = 2;
    private static int STATUS_ROTATE = 3;
    //双指缩放;
    private static int STATUS_POINT_MOVE = 4;
    private static int STATUS_MIRROR = 5;

    private int imageCount;// 已加入照片的数量
    private Context mContext;
    private int currentStatus;// 当前状态
    private LayerItem currentItem;// 当前操作的贴图数据
    private float startX, startY;

    private Paint rectPaint = new Paint();

    private LinkedHashMap<Integer, LayerItem> layerMaps = new LinkedHashMap<Integer, LayerItem>();// 存贮每层贴图数据

    private Point mPoint = new Point(0 , 0);
    protected static boolean drawSelect = true;

    private static boolean forceDisableSelectLine=false;

    private boolean isShowMirrorIcon = false;

    public void setShowMirrorIcon(boolean isShowMirrorIcon) {
        this.isShowMirrorIcon = isShowMirrorIcon;
        invalidate();
    }

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
        this.mContext = context;
        currentStatus = STATUS_IDLE;

        rectPaint.setColor(Color.RED);
        rectPaint.setAlpha(100);
        forceDisableSelectLine=false;
    }

    public void setDeleteScaleIcon(Bitmap delete,Bitmap scale){
        deleteBmp =delete;
        scaleBmp =scale;
    }

    private ILSOTouchInterface touchInterface;
    private ILayerInterface currentLayer;


    public void setTouchPlayer(ILSOTouchInterface player){
        touchInterface =player;
        resetLayerTouchView();
    }
    public void setTouchPlayer(ILSOTouchInterface player, boolean relayout){
        touchInterface =player;
        if(relayout){
            resetLayerTouchView();
        }
    }

    /**
     * 禁止选中线
     * @param is
     */
    public void setDisableSelectLine(boolean is){
        forceDisableSelectLine=is;
    }

    /**
     * 对一个图层增加控制
     * @param layer
     */
    public void setLayer(ILSOTouchInterface player, ILayerInterface layer) {

        this.touchInterface =player;
        currentLayer=layer;

        //先把所有的图层清空
        clearAllLayers();
        LayerItem item = new LayerItem(this.getContext(),getWidth(),getHeight());
        item.init(layer, this);
        layerMaps.put(++imageCount, item);
        this.invalidate();// 重绘视图
    }

    public void clearAllLayers() {
        layerMaps.clear();
        this.invalidate();
    }



    public void resetLayerTouchView(){
        if(touchInterface !=null){
            ViewGroup.LayoutParams params = getLayoutParams();
            if (params.height != touchInterface.getViewHeight() || params.width != touchInterface.getViewWidth()) {
                params.height = touchInterface.getViewHeight();
                params.width = touchInterface.getViewWidth();
                setLayoutParams(params);
            }
        }
    }
    /**
     * 绘制客户页面
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Integer id : layerMaps.keySet()) {
            LayerItem item = layerMaps.get(id);
            item.draw(canvas);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    public boolean contains(RectF rectF, float x, float y) {
        boolean ret=  rectF.left < rectF.right
                && rectF.top < rectF.bottom
                && x >= (rectF.left-10)
                && x < (rectF.right +10)
                && y >= (rectF.top -10)
                && y < (rectF.bottom +10);
        return ret;
    }


    public interface OnLayerTouchListener {

        /**
         * 图层选中
         * @param layer 选中的图层
         */
        void onLayerSelected(ILayerInterface layer);

        /**
         *
         * @param layer
         * @param x 相对屏幕尺寸的x坐标
         * @param y 相对屏幕尺寸的y坐标;
         */
        void onLayerMove(ILayerInterface layer, float x, float y);

        /**
         * @param layer
         * @param scaleWidth
         * @param scaleHeight
         * @param rotate
         */
        void onLayerScaleRotate(ILayerInterface layer, float scaleWidth, float scaleHeight, float rotate);

        /**
         *
         * @param layer
         */
        void onLayerDeleted(ILayerInterface layer);

        /**
         *
         * @param layer
         */
        void onLayerMirror(ILayerInterface layer);

        /**
         * 点击图层外面;
         */
        void onTouchOutSide();

        /**
         * 抬起;
         */
        void onLayerTouchUp();
    }

    private OnLayerTouchListener onLayerTouchListener =null;

    /**
     * 注意:没有选中状态.因为选中是点击图层时才setLayer;
     * @param listener
     */
    public void setOnLayerTouchListener(OnLayerTouchListener listener){
        onLayerTouchListener =listener;
    }

    private void sendSelectedListener(final ILayerInterface layer){
        if(onLayerTouchListener !=null){
            post(new Runnable() {
                @Override
                public void run() {
                    onLayerTouchListener.onLayerSelected(layer);
                }
            });
        }
    }

    private void sendDeleteListener(final ILayerInterface layer){
        if(onLayerTouchListener !=null){
            post(new Runnable() {
                @Override
                public void run() {
                    onLayerTouchListener.onLayerDeleted(layer);
                }
            });
        }
    }
    private void sendMoveListener(final ILayerInterface layer){
        if(onLayerTouchListener !=null){
            post(new Runnable() {
                @Override
                public void run() {
                    LSOPoint point=layer.getCenterPositionInView();
                    onLayerTouchListener.onLayerMove(layer,point.x,point.y);

                }
            });
        }
    }
    private void sendScaleRotateListener(final ILayerInterface layer){
        if(onLayerTouchListener !=null){
            post(new Runnable() {
                @Override
                public void run() {
                    LSORect rect=layer.getCurrentRectInView();
                    onLayerTouchListener.onLayerScaleRotate(layer,rect.width,rect.height,layer.getRotation());
                }
            });
        }
    }

    private void sendMirrorListener(final ILayerInterface layer){
        if(onLayerTouchListener !=null){
            post(new Runnable() {
                @Override
                public void run() {
                    onLayerTouchListener.onLayerMirror(layer);
                }
            });
        }
    }
    private boolean touchEnable = true;
    public void setTouchEnable(boolean touchEnable) {
        this.touchEnable = touchEnable;
    }

    public boolean isTouchEnable() {
        return touchEnable;
    }

    private void sendOutSideListener(){
        if(onLayerTouchListener !=null){
            post(new Runnable() {
                @Override
                public void run() {
                    onLayerTouchListener.onTouchOutSide();
                }
            });
        }
    }

    private void sendTouchUpListener(){
        if(onLayerTouchListener !=null){
            post(new Runnable() {
                @Override
                public void run() {
                    onLayerTouchListener.onLayerTouchUp();
                }
            });
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean ret = super.onTouchEvent(event);// 是否向下传递事件标志 true为消耗

        if (!touchEnable){
            return false;
        }

        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();
        switch (action & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN:
                int deleteId = -1;
                boolean outSide=false;

                if(layerMaps.isEmpty() && touchInterface !=null){
                    ILayerInterface layer= (ILayerInterface)touchInterface.getTouchPointLayer(x,y);
                    if(layer!=null){
                        setLayer(touchInterface,layer);
                        sendSelectedListener(layer);
                    }else{
                        //没有选中任意一个图层
                    }
                }else{
                    for (Integer id : layerMaps.keySet()) {
                        final LayerItem item = layerMaps.get(id);
                        if (item.detectDeleteRect.contains(x, y)) {
                            deleteId = id;
                            currentStatus = STATUS_DELETE;
                            sendDeleteListener(item.lsoLayer);
                            break;
                        } else if (contains(item.detectRotateRect,x,y)) {
                            ret = true;
                            currentItem = item;
                            drawSelect = true;
                            currentStatus = STATUS_ROTATE;
                            startX = x;
                            startY = y;
                            break;
                        }else if (item.detectMirrorRect.contains(x,y) && isShowMirrorIcon){
                            ret = true;
                            currentItem = item;
                            drawSelect = true;
                            startX = x;
                            startY = y;
                            currentStatus = STATUS_MIRROR;
                            sendMirrorListener(item.lsoLayer);
                            invalidate();
                            break;
                        } else if (detectInItemContent(item , x , y)) {
                            ret = true;
                            currentItem = item;
                            drawSelect = true;
                            currentStatus = STATUS_MOVE;
                            startX = x;
                            startY = y;
                            invalidate();
                            break;
                        }else{
                            outSide=true;
                            sendOutSideListener();
                        }
                    }
                    if(touchInterface !=null && currentLayer!=null){
                        //点击了别的图层,则清空当前选中的图层;
                        ILayerInterface layer= touchInterface.getTouchPointLayer(x,y);
                        if(layer!=null && layer!=currentLayer
                                && currentStatus!=STATUS_DELETE
                                && currentStatus != STATUS_ROTATE){
                            clearAllLayers();
                            sendOutSideListener();
                            return false;
                        }
                    }
                    if(outSide){
                        //如果在外面, 清空图层;
                        clearAllLayers();
                    }
                }

                if (!ret && currentItem != null && currentStatus == STATUS_IDLE) {// 没有贴图被选择
                    drawSelect = false;
                    currentItem = null;
                    invalidate();
                }

                if (deleteId > 0 && (currentStatus == STATUS_DELETE || currentStatus == STATUS_MIRROR)) {// 删除选定贴图
                    layerMaps.remove(deleteId);
                    currentStatus = STATUS_IDLE;// 返回空闲状态
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:

                currentStatus = STATUS_POINT_MOVE;
                if(currentItem==null){
                    for (Integer id : layerMaps.keySet()) {
                        LayerItem item = layerMaps.get(id);
                        if (detectInItemContent(item , x , y)) {// 移动模式
                            // 被选中一张贴图
                            ret = true;
                            if (currentItem != null) {
                                drawSelect = false;
                            }
                            currentItem = item;
                            drawSelect = true;
                            currentStatus = STATUS_MOVE;
                            startX = x;
                            startY = y;
                        }
                    }
                    if (!ret && currentItem != null && currentStatus == STATUS_IDLE) {// 没有贴图被选择

                        drawSelect = false;
                        currentItem = null;
                        invalidate();
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                ret = true;
                if (currentStatus == STATUS_MOVE) {
                    float dx = x - startX;
                    float dy = y - startY;
                    if (currentItem != null) {
                        currentItem.updatePosition(dx, dy);
                        sendMoveListener(currentItem.lsoLayer);
                        invalidate();
                    }
                    startX = x;
                    startY = y;

                } else if (currentStatus == STATUS_ROTATE) {
                    float dx = x - startX;
                    float dy = y - startY;
                    if (currentItem != null) {
                        currentItem.updateRotateAndScale(startX, startY, dx, dy);// 旋转
                        sendScaleRotateListener(currentItem.lsoLayer);
                        invalidate();
                    }
                    startX = x;
                    startY = y;

                }else if(currentStatus == STATUS_POINT_MOVE){  //双指放大;
                    if (currentItem != null) {
                        currentItem.updatePointMoveEvent(x,y);
                        sendScaleRotateListener(currentItem.lsoLayer);
                        invalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                ret = false;
                if(currentItem!=null){
                    currentItem.reset();
                }
                sendTouchUpListener();
                currentStatus = STATUS_IDLE;
                break;
            default:
                break;
        }
        return ret;
    }

    public void updatePos(float dx , float dy){
        if (currentItem != null) {
            currentItem.updatePosition(dx, dy);
            invalidate();
        }
    }

    public void updateRotate(float angle){
        if (currentItem != null) {
            currentItem.updateRotate(angle);
        }
        invalidate();
    }

    private boolean detectInItemContent(LayerItem item , float x , float y){
        //reset
        mPoint.set((int)x , (int)y);

        rotatePoint(mPoint , item.helpBox.centerX() , item.helpBox.centerY() , -item.rotateAngle);
        return item.helpBox.contains(mPoint.x, mPoint.y);
    }

    public static void rotatePoint(Point p, float center_x, float center_y,
                                   float roatetAngle) {
        float sinA = (float) Math.sin(Math.toRadians(roatetAngle));
        float cosA = (float) Math.cos(Math.toRadians(roatetAngle));
        // calc new point
        float newX = center_x + (p.x - center_x) * cosA - (p.y - center_y) * sinA;
        float newY = center_y + (p.y - center_y) * cosA + (p.x - center_x) * sinA;
        p.set((int)newX , (int)newY);
    }

    protected static Bitmap deleteBmp;

    protected static Bitmap scaleBmp;

    protected static Bitmap mirrorBmp;
    //-------------类: item

    class LayerItem {
        private static final float MIN_SCALE = 0.20f;
        private static final float MAX_SCALE = 1.95f;

        private static final int HELP_BOX_PAD = 10;

        private static final int BUTTON_WIDTH = 30;

        public ILayerInterface lsoLayer;

        public Rect srcRect;// 原始图片坐标
        public RectF dstRect;// 绘制目标坐标


        private Rect helpToolsRect;  //四方框的线;
        public RectF deleteRect;// 删除按钮位置
        public RectF rotateRect;// 旋转按钮位置
        public RectF mirrorRect; // 镜像按钮位置

        public RectF helpBox;
        public Matrix matrix;// 变化矩阵
        public float rotateAngle = 0;



        private Paint dstPaint = new Paint();
        private Paint helpBoxPaint = new Paint();

        private float initWidth;// 加入屏幕时原始宽度



        private Paint debugPaint = new Paint();
        public RectF detectRotateRect;
        public RectF detectDeleteRect;
        public RectF detectMirrorRect;

        private int viewWidth, viewHeight;

        public LayerItem(Context context, int width, int height) {


            this.viewWidth = width;
            this.viewHeight = height;

            helpBoxPaint.setColor(Color.WHITE);
            helpBoxPaint.setStyle(Paint.Style.STROKE);
            helpBoxPaint.setAntiAlias(true);
            helpBoxPaint.setStrokeWidth(6);

            dstPaint = new Paint();
            dstPaint.setColor(Color.RED);
            dstPaint.setAlpha(120);

            debugPaint = new Paint();
            debugPaint.setColor(Color.GREEN);
            debugPaint.setAlpha(120);

            // 导入工具按钮位图
            if (deleteBmp == null) {
                deleteBmp = BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.icon_delete);
            }

            if (scaleBmp == null) {
                scaleBmp = BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.icon_zoom);
            }

            if (mirrorBmp == null){
                mirrorBmp = BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.icon_mirror);
            }
        }


        public void init(ILayerInterface layer, View parentView) {
            this.lsoLayer = layer;

            LSORect rect = layer.getCurrentRectInView();
            this.srcRect = new Rect(0, 0, (int) rect.width, (int) rect.height);

            int bitWidth = (int) rect.width;

            int bitHeight = (int) rect.height;

            int left = (int) rect.x;
            int top = (int) rect.y;

            this.dstRect = new RectF(left, top, left + bitWidth, top + bitHeight);

            this.matrix = new Matrix();
            this.matrix.postTranslate(this.dstRect.left, this.dstRect.top);
            this.matrix.postScale((float) bitWidth / (int) rect.width,
                    (float) bitHeight / (int) rect.height, this.dstRect.left,
                    this.dstRect.top);

            initWidth = parentView.getWidth(); // 记录原始宽度
            drawSelect = true;
            this.helpBox = new RectF(this.dstRect);
            computeHelpBoxRect();

            helpToolsRect = new Rect(0, 0, deleteBmp.getWidth(),
                    deleteBmp.getHeight());

            deleteRect = new RectF(helpBox.left - BUTTON_WIDTH, helpBox.top
                    - BUTTON_WIDTH, helpBox.left + BUTTON_WIDTH, helpBox.top
                    + BUTTON_WIDTH);

            rotateRect = new RectF(helpBox.right - BUTTON_WIDTH, helpBox.bottom
                    - BUTTON_WIDTH, helpBox.right + BUTTON_WIDTH, helpBox.bottom
                    + BUTTON_WIDTH);

            mirrorRect =  new RectF(helpBox.left - BUTTON_WIDTH, helpBox.bottom
                    - BUTTON_WIDTH, helpBox.left + BUTTON_WIDTH, helpBox.bottom
                    + BUTTON_WIDTH);

            detectRotateRect = new RectF(rotateRect);
            detectDeleteRect = new RectF(deleteRect);
            detectMirrorRect = new RectF(mirrorRect);

            rotateAngle = layer.getRotation();

            this.matrix.postRotate(rotateAngle, this.dstRect.centerX(), this.dstRect.centerY());

            rotateAllRect();
        }

        private void computeHelpBoxRect() {
            this.helpBox.left -= HELP_BOX_PAD;
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
        public void updatePosition(final float dx, final float dy) {

            this.matrix.postTranslate(dx, dy);// 记录到矩阵中

            dstRect.offset(dx, dy);

            // 工具按钮随之移动
            helpBox.offset(dx, dy);
            deleteRect.offset(dx, dy);
            rotateRect.offset(dx, dy);
            mirrorRect.offset(dx,dy);
            this.detectRotateRect.offset(dx, dy);
            this.detectDeleteRect.offset(dx, dy);
            this.detectMirrorRect.offset(dx,dy);
            float x = dstRect.left + dstRect.width() / 2.0f;
            float y = dstRect.top + dstRect.height() / 2.0f;

            if (lsoLayer != null) {
                lsoLayer.setCenterPositionInView(x, y);
            }

        }

        public void updateRotate(float angle){
            if (lsoLayer != null) {
                lsoLayer.setRotation(lsoLayer.getRotation() + angle);
            }
            rotateAngle += angle;

            this.matrix.postRotate(angle, this.dstRect.centerX(), this.dstRect.centerY());

            rotateAllRect();
        }

        private float startSpace = 0;
        private float lastSpace = 0;

        public void reset() {
            startSpace = 0;
        }


        public void scaleRect(RectF rect, float scale) {
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


        private float scaleWidth, scaleHeight;

        //双指缩放
        public void updatePointMoveEvent(float dx, final float dy) {

            float x = (dx - dstRect.centerX());
            float y = (dy - dstRect.centerY());
            float space2 = (float) Math.sqrt(x * x + y * y);

            if (startSpace == 0) {
                startSpace = space2;
                lastSpace = startSpace;
                scaleWidth = lsoLayer.getScaleWidth();
                scaleHeight = lsoLayer.getScaleHeight();
            }

            float scale = space2 / lastSpace;

            float newWidth = dstRect.width() * scale;
            if (newWidth / initWidth < MIN_SCALE) {// 最小缩放值检测
                return;
            }
            if (newWidth / initWidth > MAX_SCALE) {// 最小缩放值检测
                return;
            }

            lastSpace = space2;

            if (startSpace != 0) {
                float ret = space2 / startSpace;
                lsoLayer.setScaledValue(scaleWidth * ret, scaleHeight * ret);
            }


            this.matrix.postScale(scale, scale, this.dstRect.centerX(),
                    this.dstRect.centerY());// 存入scale矩阵

            scaleRect(this.dstRect, scale);// 缩放目标矩形


            helpBox.set(dstRect);

            computeHelpBoxRect();// 重新计算

            updateRectPosition();

            //------------
            rotateAllRect();
        }

        private void rotateAllRect() {
            rotateRect(this.detectRotateRect, this.dstRect.centerX(),
                    this.dstRect.centerY(), rotateAngle);

            rotateRect(this.detectDeleteRect, this.dstRect.centerX(),
                    this.dstRect.centerY(), rotateAngle);

            rotateRect(this.detectMirrorRect, this.dstRect.centerX(),
                    this.dstRect.centerY(), rotateAngle);
        }

        private void updateRectPosition() {
            rotateRect.offsetTo(helpBox.right - BUTTON_WIDTH,
                    helpBox.bottom - BUTTON_WIDTH);

            deleteRect.offsetTo(helpBox.left - BUTTON_WIDTH,
                    helpBox.top - BUTTON_WIDTH);

            mirrorRect.offsetTo(helpBox.left - BUTTON_WIDTH,
                    helpBox.bottom - BUTTON_WIDTH);

            detectRotateRect.offsetTo(helpBox.right - BUTTON_WIDTH, helpBox.bottom
                    - BUTTON_WIDTH);

            detectDeleteRect.offsetTo(helpBox.left - BUTTON_WIDTH, helpBox.top
                    - BUTTON_WIDTH);

            detectMirrorRect.offsetTo(helpBox.left - BUTTON_WIDTH,helpBox.bottom
                    - BUTTON_WIDTH);

        }

        protected boolean updateRotateAndScale(final float oldx, final float oldy,
                                               final float dx, final float dy) {

            float centerX = dstRect.centerX();
            float centerY = dstRect.centerY();

            float scaleIconCenterX = this.detectRotateRect.centerX();

            float scaleIconCenterY = this.detectRotateRect.centerY();

            float n_x = scaleIconCenterX + dx;
            float n_y = scaleIconCenterY + dy;

            float xDelta = scaleIconCenterX - centerX;
            float ya = scaleIconCenterY - centerY;

            float xb = n_x - centerX;
            float yb = n_y - centerY;

            float srcLen = (float) Math.sqrt(xDelta * xDelta + ya * ya);
            float curLen = (float) Math.sqrt(xb * xb + yb * yb);


            float scale = curLen / srcLen;// 计算缩放比

            float newWidth = dstRect.width() * scale;

            // 最小缩放值检测
            if (newWidth / initWidth >= MIN_SCALE && newWidth / initWidth <= MAX_SCALE) {


                if (lsoLayer != null) {
                    lsoLayer.setScaledValue(lsoLayer.getScaleWidth() * scale, lsoLayer.getScaleHeight() * scale);
                }


                this.matrix.postScale(scale, scale, this.dstRect.centerX(),
                        this.dstRect.centerY());// 存入scale矩阵

                scaleRect(this.dstRect, scale);// 缩放目标矩形

            }

            // 重新计算工具箱坐标
            helpBox.set(dstRect);

            computeHelpBoxRect();// 重新计算

            updateRectPosition();

            double cos = (xDelta * xb + ya * yb) / (srcLen * curLen);
            if (cos > 1 || cos < -1)
                return false;

            float angle = (float) Math.toDegrees(Math.acos(cos));

            // 定理
            float calMatrix = xDelta * yb - xb * ya;// 行列式计算 确定转动方向

            int flag = calMatrix > 0 ? 1 : -1;
            angle = flag * angle;

            updateRotate(angle);

            return true;

        }

        public void draw(Canvas canvas) {

            canvas.save();

            canvas.rotate(rotateAngle, helpBox.centerX(), helpBox.centerY());

            if (drawSelect && !forceDisableSelectLine) {// 绘制辅助工具线
                canvas.drawRect(helpBox, helpBoxPaint);
                canvas.drawBitmap(deleteBmp, helpToolsRect, deleteRect, null);
                canvas.drawBitmap(scaleBmp, helpToolsRect, rotateRect, null);
                if (isShowMirrorIcon) {
                    canvas.drawBitmap(mirrorBmp, helpToolsRect, mirrorRect, null);
                }
            }

            canvas.restore();
        }

        /**
         * 矩形绕指定点旋转
         *
         * @param rect
         * @param roatetAngle
         */
        public  void rotateRect(RectF rect, float center_x, float center_y,
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

    }// end class

}
