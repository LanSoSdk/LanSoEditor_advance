package com.example.advanceDemo.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class ImageTouchView extends ImageView {
    public Matrix matrix = new Matrix();

    Matrix savedMatrix = new Matrix();
    /**
     * 当前模式
     */
    int mode = PaintConstants.MODE.NONE;
    /**
     * 存储float类型的x，y值，就是你点下的坐标的X和Y
     */
    PointF prev = new PointF();
    PointF curPosition = new PointF();
    PointF mid = new PointF();
    float dist = 1f;
    float oldRotation = 0;
    float oldDistX = 1f;
    float oldDistY = 1f;
    // 定义一个内存中的图片，该图片将作为缓冲区
    Bitmap cacheBitmap = null;
    // 定义cacheBitmap上的Canvas对象
    Canvas cacheCanvas = null;
    int x = 0;
    int y = 0;
    Activity mActivity;
    /**
     * 屏幕的分辨率
     */
    private DisplayMetrics dm;
    /**
     * 位图对象
     */
    private Bitmap bitmap = null;
    private Paint paint;
    private Context context;

    // private String TAG = "APP";
    private Path path;
    private Path tempPath;
    private Paint cachePaint = null;

    public ImageTouchView(Context context) {
        super(context);
    }

    public ImageTouchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        // Log.i(TAG, "ImageTouchView(Context context, AttributeSet attrs)=>");

        setupView();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mode == PaintConstants.MODE.COLORING) {
            canvas.drawPath(tempPath, paint);
        }

    }

    public void setActivity(Activity acty) {
        mActivity = acty;
    }

    public void setupView() {

        // 获取屏幕分辨率,需要根据分辨率来使用图片居中
        dm = getContext().getResources().getDisplayMetrics();

        // 从ImageView中获取bitmap对象
        BitmapDrawable bd = (BitmapDrawable) this.getDrawable();
        if (bd != null) {
            bitmap = bd.getBitmap();
            center(true, true);
        }
        setCoverBitmap(bitmap);
        this.setImageMatrix(matrix);

        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                Matrix matrixTemp = new Matrix();
                matrixTemp.set(matrix);
                // view的触摸坐标的转换
                matrixTemp.invert(matrixTemp);
                // Log.i(TAG, "Touch screen.");

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    // 主点按下
                    case MotionEvent.ACTION_DOWN:
                        savedMatrix.set(matrix);
                        prev.set(event.getX(), event.getY());

                        float[] pointPrevInit = new float[]{prev.x, prev.y};
                        matrixTemp.mapPoints(pointPrevInit);
                        path.moveTo(pointPrevInit[0], pointPrevInit[1]);
                        tempPath.moveTo(event.getX(), event.getY());

                        mode = PaintConstants.MODE.DRAG;
                        // Log.i(TAG, "ACTION_DOWN=>.");
                        break;
                    // 副点按下
                    case MotionEvent.ACTION_POINTER_DOWN:
                        dist = spacing(event);
                        oldRotation = rotation(event);
                        oldDistX = spacingX(event);
                        oldDistY = spacingY(event);
                        // 如果连续两点距离大于10，则判定为多点模式
                        if (spacing(event) > 10f) {
                            savedMatrix.set(matrix);
                            midPoint(mid, event);
                            mode = PaintConstants.MODE.ZOOM;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        // Log.i(TAG, "ACTION_UP=>.");
                        if (mode == PaintConstants.MODE.COLORING) {
                            cachePaint.setColor(PaintConstants.PEN_COLOR);
                            cachePaint.setStrokeWidth(PaintConstants.PEN_SIZE);
                            cachePaint.setAlpha(PaintConstants.TRANSPARENT);
                            cachePaint.setMaskFilter(new BlurMaskFilter(5,
                                    PaintConstants.BLUR_TYPE));

                            cacheCanvas.drawPath(path, cachePaint);
                            path.reset();
                            tempPath.reset();
                        }
                        break;

                    case MotionEvent.ACTION_POINTER_UP:
                        mode = PaintConstants.MODE.NONE;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (!PaintConstants.SELECTOR.KEEP_IMAGE) {
                            if (mode == PaintConstants.MODE.DRAG) {
                                matrix.set(savedMatrix);
                                matrix.postTranslate(event.getX() - prev.x,
                                        event.getY() - prev.y);
                            } else if (mode == PaintConstants.MODE.ZOOM) {
                                float rotation = (rotation(event) - oldRotation) / 2;
                                float newDistX = spacingX(event);
                                float newDistY = spacingY(event);
                                float scaleX = newDistX - oldDistX;
                                float scaleY = newDistY - oldDistY;

                                float newDist = spacing(event);
                                if (newDist > 10f) {
                                    matrix.set(savedMatrix);
                                    float tScale = newDist / dist;
                                    tScale = tScale > 1 ? 1 + ((tScale - 1) / 2)
                                            : 1 - (1 - tScale) / 2;
                                    if (PaintConstants.SELECTOR.KEEP_SCALE) {
                                        matrix.postScale(tScale, tScale, mid.x,
                                                mid.y);// 縮放
                                    } else {
                                        if (Math.abs(scaleX) >= Math.abs(scaleY)) {
                                            matrix.postScale(tScale, 1, mid.x,
                                                    mid.y);// 縮放
                                        } else {
                                            matrix.postScale(1, tScale, mid.x,
                                                    mid.y);// 縮放
                                        }
                                    }
                                    if (PaintConstants.SELECTOR.HAIR_RURN)
                                        matrix.postRotate(rotation, mid.x, mid.y);// 旋轉
                                }
                            }
                        } else {
                            float[] pointPrev = new float[]{prev.x, prev.y};
                            float[] pointStop = new float[]{event.getX(),
                                    event.getY()};

                            // view的触摸坐标的转换
                            matrixTemp.mapPoints(pointPrev);
                            matrixTemp.mapPoints(pointStop);

                            if (PaintConstants.SELECTOR.COLORING) {
                                // 染色功能
                                mode = PaintConstants.MODE.COLORING;
                                paint.reset();
                                paint = new Paint(Paint.DITHER_FLAG);
                                paint.setColor(Color.RED);
                                // 设置图层风格
                                paint.setStyle(Paint.Style.STROKE);
                                paint.setStrokeWidth(1);
                                // 反锯齿
                                paint.setAntiAlias(true);
                                paint.setDither(true);
                                paint.setColor(PaintConstants.PEN_COLOR);
                                paint.setStrokeWidth(PaintConstants.PEN_SIZE);

                                path.quadTo(pointPrev[0], pointPrev[1],
                                        pointStop[0], pointStop[1]);
                                tempPath.quadTo(prev.x, prev.y, event.getX(),
                                        event.getY());

                                // 更新开始点的位置
                                prev.set(event.getX(), event.getY());

                                ImageTouchView.this.setImageBitmap(cacheBitmap);

                            } else if (PaintConstants.SELECTOR.ERASE) {
                                // 橡皮擦功能

                                mode = PaintConstants.MODE.ERASE;

                                paint.reset();
                                paint.setColor(Color.TRANSPARENT);
                                paint.setAntiAlias(false);
                                paint.setStyle(Paint.Style.STROKE);
                                paint.setStrokeWidth(16);
                                paint.setStrokeJoin(Paint.Join.ROUND);
                                paint.setStrokeCap(Paint.Cap.ROUND);
                                paint.setAlpha(0);
                                paint.setXfermode(new PorterDuffXfermode(
                                        Mode.DST_IN));
                                paint.setStrokeWidth(PaintConstants.ERASE_SIZE);

                                prev.set(event.getX(), event.getY());

                                cacheCanvas.drawLine(pointPrev[0], pointPrev[1],
                                        pointStop[0], pointStop[1], paint);
                                ImageTouchView.this.setImageBitmap(cacheBitmap);
                            }
                        }
                }
                ImageTouchView.this.setImageMatrix(matrix);
                invalidate();

                return true;
            }
        });
    }

    /**
     * 横向、纵向居中
     */
    protected void center(boolean horizontal, boolean vertical) {
        RectF rect = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());

        float height = rect.height();
        float width = rect.width();

        float deltaX = 0, deltaY = 0;

        if (vertical) {
            // 图片小于屏幕大小，则居中显示。大于屏幕，上方留空则往上移，下方留空则往下移
            int screenHeight = dm.heightPixels;
            if (height < screenHeight) {
                deltaY = (screenHeight - height) / 2 - rect.top;
            } else if (rect.top > 0) {
                deltaY = -rect.top;
            } else if (rect.bottom < screenHeight) {
                deltaY = this.getHeight() - rect.bottom;
            }
        }

        if (horizontal) {
            int screenWidth = dm.widthPixels;
            if (width < screenWidth) {
                deltaX = (screenWidth - width) / 2 - rect.left;
            } else if (rect.left > 0) {
                deltaX = -rect.left;
            } else if (rect.right < screenWidth) {
                deltaX = screenWidth - rect.right;
            }
        }
        matrix.postTranslate(deltaX, deltaY);
    }

    private float spacingX(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        return x;
    }

    private float spacingY(MotionEvent event) {
        float y = event.getY(0) - event.getY(1);
        return y;
    }

    // 取旋转角度
    private float rotation(MotionEvent event) {
        double delta_x = (event.getX(0) - event.getX(1));
        double delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    /**
     * 两点的距离
     */
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 两点的中点
     */
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    /**
     * @param bm
     * @note set cover byteBuffer , which overlay on background.
     */
    private void setCoverBitmap(Bitmap bitmap) {
        // setting paint
        paint = new Paint();
        cacheCanvas = new Canvas();

        if (bitmap != null) {
            cacheBitmap = Bitmap.createBitmap(bitmap.getWidth(),
                    bitmap.getHeight(), Config.ARGB_8888);
            cacheCanvas.setBitmap(cacheBitmap);
            cacheCanvas.drawBitmap(bitmap, 0, 0, null);
        }

        path = new Path();
        tempPath = new Path();

        // 设置图层的颜色
        cachePaint = new Paint();
        // 设置图层风格
        cachePaint.setStyle(Paint.Style.STROKE);
        // 反锯齿
        cachePaint.setAntiAlias(true);
        cachePaint.setStrokeJoin(Paint.Join.ROUND);
        cachePaint.setStrokeCap(Paint.Cap.ROUND);
        cachePaint.setXfermode(new PorterDuffXfermode(Mode.SRC_ATOP));
        // 设置图层模糊效果
        cachePaint
                .setMaskFilter(new BlurMaskFilter(5, PaintConstants.BLUR_TYPE));

    }

}
