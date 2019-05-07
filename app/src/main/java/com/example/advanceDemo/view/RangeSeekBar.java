package com.example.advanceDemo.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ImageView;

import com.lansoeditor.advanceDemo.R;

import java.math.BigDecimal;

/**
 * Widget that lets users select a minimum and maximum value on a given
 * numerical range. The range value types can be one of Long, Double, Integer,
 * Float, Short, Byte or BigDecimal.<br />
 * <br />
 * Improved {@link MotionEvent} handling for smoother use, anti-aliased painting
 * for improved aesthetics.
 *
 * @param <T> The Number type of the range values. One of Long, Double, Integer,
 *            Float, Short, Byte or BigDecimal.
 * @author Stephan Tittel (stephan.tittel@kom.tu-darmstadt.de)
 * @author Peter Sinnott (psinnott@gmail.com)
 * @author Thomas Barrasso (tbarrasso@sevenplusandroid.org)
 * @author Yao (chuan_27049@126.com)
 */
public class RangeSeekBar<T extends Number> extends ImageView {
    /**
     * Default color of a {@link RangeSeekBar}, #FF33B5E5. This is also known as
     * "Ice Cream Sandwich" blue.
     */
    public static final int DEFAULT_COLOR = Color.argb(0xFF, 0x33, 0xB5, 0xE5);
    /**
     * An invalid pointer id.
     */
    public static final int INVALID_POINTER_ID = 255;
    // Localized constants from MotionEvent for compatibility
    // with API < 8 "Froyo".
    public static final int ACTION_POINTER_UP = 0x6,
            ACTION_POINTER_INDEX_MASK = 0x0000ff00,
            ACTION_POINTER_INDEX_SHIFT = 8;
    private static final String TAG = RangeSeekBar.class.getSimpleName();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbValuePaint = getThumbValuePaint();
    private final Bitmap thumbImage = BitmapFactory.decodeResource(
            getResources(), R.drawable.range_seek_bar_imb);
    private final Bitmap thumbPressedImage = BitmapFactory.decodeResource(
            getResources(), R.drawable.range_seek_bar_imb_pressed);
    private final float thumbWidth = thumbImage.getWidth();
    private final float thumbHalfWidth = 0.5f * thumbWidth;
    private final float thumbHalfHeight = 0.5f * thumbImage.getHeight();
    private final float padding = thumbHalfWidth;
    private final T absoluteMinValue, absoluteMaxValue;
    private final NumberType numberType;
    private final double absoluteMinValuePrim, absoluteMaxValuePrim;
    /**
     * On touch, this offset plus the scaled value from the position of the
     * touch will form the progress value. Usually 0.
     */
    float mTouchProgressOffset;
    private double normalizedMinValue = 0d;
    private double normalizedMaxValue = 1d;// normalized：规格化的--点坐标占总长度的比例值，范围从0-1
    private Thumb pressedThumb = null;
    private boolean notifyWhileDragging = false;
    private OnRangeSeekBarChangeListener<T> listener;
    private float mDownMotionX;// 记录touchEvent按下时的X坐标
    private int mActivePointerId = INVALID_POINTER_ID;
    private int mScaledTouchSlop;
    private boolean mIsDragging;

    /**
     * Creates a new RangeSeekBar.
     *
     * @param absoluteMinValue The minimum value of the selectable range.
     * @param absoluteMaxValue The maximum value of the selectable range.
     * @param context
     * @throws IllegalArgumentException Will be thrown if min/max value type is not one of Long,
     *                                  Double, Integer, Float, Short, Byte or BigDecimal.
     */
    public RangeSeekBar(T absoluteMinValue, T absoluteMaxValue, Context context)
            throws IllegalArgumentException {
        super(context);
        this.absoluteMinValue = absoluteMinValue;
        this.absoluteMaxValue = absoluteMaxValue;
        absoluteMinValuePrim = absoluteMinValue.doubleValue();// 都转换为double类型的值
        absoluteMaxValuePrim = absoluteMaxValue.doubleValue();
        numberType = NumberType.fromNumber(absoluteMinValue);// 得到输入数字的枚举类型

        // make RangeSeekBar focusable. This solves focus handling issues in
        // case EditText widgets are being used along with the RangeSeekBar
        // within ScollViews.
        setFocusable(true);
        setFocusableInTouchMode(true);
        init();
    }

    private final void init() {
        // 被认为是触摸滑动的最短距离
        mScaledTouchSlop = ViewConfiguration.get(getContext())
                .getScaledTouchSlop();
    }

    /**
     * 供外部activity调用，控制是都在拖动的时候打印log信息，默认是false不打印
     */
    public boolean isNotifyWhileDragging() {
        return notifyWhileDragging;
    }

    /**
     * Should the widget notify the listener callback while the user is still
     * dragging a thumb? Default is false.
     *
     * @param flag
     */
    public void setNotifyWhileDragging(boolean flag) {
        this.notifyWhileDragging = flag;
    }

    /**
     * Returns the absolute minimum value of the range that has been set at
     * construction time.
     *
     * @return The absolute minimum value of the range.
     */
    public T getAbsoluteMinValue() {
        return absoluteMinValue;
    }

    /**
     * Returns the absolute maximum value of the range that has been set at
     * construction time.
     *
     * @return The absolute maximum value of the range.
     */
    public T getAbsoluteMaxValue() {
        return absoluteMaxValue;
    }

    /**
     * Returns the currently selected min value.
     *
     * @return The currently selected min value.
     */
    public T getSelectedMinValue() {
        return normalizedToValue(normalizedMinValue);
    }

    /**
     * Sets the currently selected minimum value. The widget will be invalidated
     * and redrawn.
     *
     * @param value The Number value to set the minimum value to. Will be clamped
     *              to given absolute minimum/maximum range.
     */
    public void setSelectedMinValue(T value) {
        // in case absoluteMinValue == absoluteMaxValue, avoid division by zero
        // when normalizing.
        if (0 == (absoluteMaxValuePrim - absoluteMinValuePrim)) {
            // activity设置的最大值和最小值相等
            setNormalizedMinValue(0d);
        } else {
            setNormalizedMinValue(valueToNormalized(value));
        }
    }

    /**
     * Returns the currently selected max value.
     *
     * @return The currently selected max value.
     */
    public T getSelectedMaxValue() {
        return normalizedToValue(normalizedMaxValue);
    }

    /**
     * Sets the currently selected maximum value. The widget will be invalidated
     * and redrawn.
     *
     * @param value The Number value to set the maximum value to. Will be clamped
     *              to given absolute minimum/maximum range.
     */
    public void setSelectedMaxValue(T value) {
        // in case absoluteMinValue == absoluteMaxValue, avoid division by zero
        // when normalizing.
        if (0 == (absoluteMaxValuePrim - absoluteMinValuePrim)) {
            setNormalizedMaxValue(1d);
        } else {
            setNormalizedMaxValue(valueToNormalized(value));
        }
    }

    /**
     * Registers given listener callback to notify about changed selected
     * values.
     *
     * @param listener The listener to notify about changed selected values.
     */
    public void setOnRangeSeekBarChangeListener(
            OnRangeSeekBarChangeListener<T> listener) {
        this.listener = listener;
    }

    /**
     * Handles thumb selection and movement. Notifies listener callback on
     * certain events.
     * <p>
     * <p>
     * ACTION_MASK在Android中是应用于多点触摸操作，字面上的意思大概是动作掩码的意思吧。
     * 在onTouchEvent(MotionEvent event)中，使用switch
     * (event.getAction())可以处理ACTION_DOWN和ACTION_UP事件；使用switch
     * (event.getAction() & MotionEvent.ACTION_MASK)
     * 就可以处理处理多点触摸的ACTION_POINTER_DOWN和ACTION_POINTER_UP事件。
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (!isEnabled())
            return false;

        int pointerIndex;// 记录点击点的index

        final int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN:
                // Remember where the motion event started
                // event.getPointerCount() -
                // 1得到最后一个点击屏幕的点，点击的点id从0到event.getPointerCount() - 1
                mActivePointerId = event.getPointerId(event.getPointerCount() - 1);
                pointerIndex = event.findPointerIndex(mActivePointerId);
                mDownMotionX = event.getX(pointerIndex);// 得到pointerIndex点击点的X坐标

                pressedThumb = evalPressedThumb(mDownMotionX);// 判断touch到的是最大值thumb还是最小值thumb

                // Only handle thumb presses.
                if (pressedThumb == null)
                    return super.onTouchEvent(event);

                setPressed(true);// 设置该控件被按下了
                invalidate();// 通知执行onDraw方法
                onStartTrackingTouch();// 置mIsDragging为true，开始追踪touch事件
                trackTouchEvent(event);
                attemptClaimDrag();

                break;
            case MotionEvent.ACTION_MOVE:
                if (pressedThumb != null) {

                    if (mIsDragging) {
                        trackTouchEvent(event);
                    } else {
                        pointerIndex = event.findPointerIndex(mActivePointerId);
                        final float x = event.getX(pointerIndex);// 手指在控件上点的X坐标
                        if (Math.abs(x - mDownMotionX) > mScaledTouchSlop) {
                            setPressed(true);
                            invalidate();
                            onStartTrackingTouch();
                            trackTouchEvent(event);
                            attemptClaimDrag();
                        }
                    }

                    if (notifyWhileDragging && listener != null) {
                        listener.onRangeSeekBarValuesChanged(this,
                                getSelectedMinValue(), getSelectedMaxValue());
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    // Touch up when we never crossed the touch slop threshold
                    // should be interpreted as a tap-seek to that location.
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                }

                pressedThumb = null;// 手指抬起，则置被touch到的thumb为空
                invalidate();
                if (listener != null) {
                    listener.onRangeSeekBarValuesChanged(this,
                            getSelectedMinValue(), getSelectedMaxValue());
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN: {
                final int index = event.getPointerCount() - 1;
                // final int index = ev.getActionIndex();
                mDownMotionX = event.getX(index);
                mActivePointerId = event.getPointerId(index);
                invalidate();
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate(); // see above explanation
                break;
        }
        return true;
    }

    private final void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;

        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose
            // a new active pointer and adjust accordingly.
            // TODO: Make this decision more intelligent.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mDownMotionX = ev.getX(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }
    }

    /**
     * 一直追踪touch事件，刷新view
     *
     * @param event
     */
    private final void trackTouchEvent(MotionEvent event) {
        final int pointerIndex = event.findPointerIndex(mActivePointerId);// 得到按下点的index
        final float x = event.getX(pointerIndex);// 得到当前pointerIndex在屏幕上的x坐标

        if (Thumb.MIN.equals(pressedThumb)) {
            // screenToNormalized(x)-->得到规格化的0-1的值
            setNormalizedMinValue(screenToNormalized(x));
        } else if (Thumb.MAX.equals(pressedThumb)) {
            setNormalizedMaxValue(screenToNormalized(x));
        }
    }

    /**
     * Tries to claim the user's drag motion, and requests disallowing any
     * ancestors from stealing events in the drag.
     * <p>
     * 试图告诉父view不要拦截子控件的drag
     */
    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    /**
     * This is called when the user has started touching this widget.
     */
    void onStartTrackingTouch() {
        mIsDragging = true;
    }

    /**
     * This is called when the user either releases his touch or the touch is
     * canceled.
     */
    void onStopTrackingTouch() {
        mIsDragging = false;
    }

    /**
     * Ensures correct size of the widget.
     */
    @Override
    protected synchronized void onMeasure(int widthMeasureSpec,
                                          int heightMeasureSpec) {
        int width = 200;
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec)) {
            width = MeasureSpec.getSize(widthMeasureSpec);
        }
        int height = thumbImage.getHeight();
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)) {
            // 高度为滑动条高度+文字画笔高度
            height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec))
                    + (int) getFontHeight(thumbValuePaint) * 2;
        }
        setMeasuredDimension(width, height);
    }

    /**
     * Draws the widget on the given canvas.
     */
    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Bitmap l_bg = BitmapFactory.decodeResource(getResources(),
                R.drawable.range_seek_bar_bg_l);
        Bitmap m_bg = BitmapFactory.decodeResource(getResources(),
                R.drawable.range_seek_bar_bg_m);
        Bitmap r_bg = BitmapFactory.decodeResource(getResources(),
                R.drawable.range_seek_bar_bg_r);
        Bitmap m_progress = BitmapFactory.decodeResource(getResources(),
                R.drawable.range_seek_bar_progress);

        canvas.drawBitmap(l_bg, padding - thumbHalfWidth,
                0.5f * (getHeight() - l_bg.getHeight()), paint);

        float bg_middle_left = padding - thumbHalfWidth + l_bg.getWidth();// 需要平铺的中间背景的开始坐标
        float bg_middle_right = getWidth() - padding + thumbHalfWidth
                - l_bg.getWidth();// 需要平铺的中间背景的开始坐标

        float scale = (bg_middle_right - bg_middle_left)
                / m_progress.getWidth();// 上层最大最小值间距离与m_progress比例
        Matrix mx = new Matrix();
        mx.postScale(scale, 1f);
        Bitmap m_bg_new = Bitmap.createBitmap(m_bg, 0, 0,
                m_progress.getWidth(), m_progress.getHeight(), mx, true);
        canvas.drawBitmap(m_bg_new, bg_middle_left,
                0.5f * (getHeight() - m_bg.getHeight()), paint);

        canvas.drawBitmap(r_bg, bg_middle_right,
                0.5f * (getHeight() - r_bg.getHeight()), paint);

        float rangeL = normalizedToScreen(normalizedMinValue);
        float rangeR = normalizedToScreen(normalizedMaxValue);
        // float length = rangeR - rangeL;
        float pro_scale = (rangeR - rangeL) / m_progress.getWidth();// 上层最大最小值间距离与m_progress比例
        if (pro_scale > 0) {

            Matrix pro_mx = new Matrix();
            pro_mx.postScale(pro_scale, 1f);
            try {

                Bitmap m_progress_new = Bitmap.createBitmap(m_progress, 0, 0,
                        m_progress.getWidth(), m_progress.getHeight(), pro_mx,
                        true);

                canvas.drawBitmap(m_progress_new, rangeL,
                        0.5f * (getHeight() - m_progress.getHeight()), paint);
            } catch (Exception e) {
                // 当pro_scale非常小，例如width=12，Height=48，pro_scale=0.01979065时，
                // 宽高按比例计算后值为0.237、0.949，系统强转为int型后宽就变成0了。就出现非法参数异常
                Log.e(TAG,
                        "IllegalArgumentException--width="
                                + m_progress.getWidth() + "Height="
                                + m_progress.getHeight() + "pro_scale="
                                + pro_scale, e);

            }

        }

        // 添加minThumb的对应的值
        drawThumbMinValue(normalizedToScreen(normalizedMinValue), ""
                + getSelectedMinValue(), canvas);

        // 添加maxThumb的对应的值
        drawThumbMaxValue(normalizedToScreen(normalizedMaxValue), ""
                + getSelectedMaxValue(), canvas);

        // draw minimum thumb
        drawThumb(normalizedToScreen(normalizedMinValue),
                Thumb.MIN.equals(pressedThumb), canvas);

        // draw maximum thumb
        drawThumb(normalizedToScreen(normalizedMaxValue),
                Thumb.MAX.equals(pressedThumb), canvas);

    }

    /**
     * Overridden to save instance state when device orientation changes. This
     * method is called automatically if you assign an id to the RangeSeekBar
     * widget using the {@link #setId(int)} method. Other members of this class
     * than the normalized min and max values don't need to be saved.
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable("SUPER", super.onSaveInstanceState());
        bundle.putDouble("MIN", normalizedMinValue);
        bundle.putDouble("MAX", normalizedMaxValue);
        return bundle;
    }

    /**
     * Overridden to restore instance state when device orientation changes.
     * This method is called automatically if you assign an id to the
     * RangeSeekBar widget using the {@link #setId(int)} method.
     */
    @Override
    protected void onRestoreInstanceState(Parcelable parcel) {
        final Bundle bundle = (Bundle) parcel;
        super.onRestoreInstanceState(bundle.getParcelable("SUPER"));
        normalizedMinValue = bundle.getDouble("MIN");
        normalizedMaxValue = bundle.getDouble("MAX");
    }

    /**
     * Draws the "normal" resp. "pressed" thumb image on specified x-coordinate.
     *
     * @param screenCoord The x-coordinate in screen space where to draw the image.
     * @param pressed     Is the thumb currently in "pressed" state?
     * @param canvas      The canvas to draw upon.
     */
    private void drawThumb(float screenCoord, boolean pressed, Canvas canvas) {
        canvas.drawBitmap(pressed ? thumbPressedImage : thumbImage, screenCoord
                        - thumbHalfWidth,
                (float) ((0.5f * getHeight()) - thumbHalfHeight), paint);
    }

    /**
     * 画thumb对应的最小值
     *
     * @param screenCoord
     * @param text
     * @param canvas
     */
    private void drawThumbMinValue(float screenCoord, String text, Canvas canvas) {

        // 得到maxThumb的left在屏幕上的绝对坐标
        float maxThumbleft = normalizedToScreen(normalizedMaxValue)
                - thumbHalfWidth;

        // 该文字的right坐标
        float textRight = screenCoord - thumbHalfWidth
                + getFontlength(thumbValuePaint, text);

        if (textRight >= maxThumbleft) {
            // 当文字的右边界到达了maxThumb的左边界时
            if (pressedThumb == Thumb.MIN) {
                // touch的为min
                canvas.drawText(text,
                        maxThumbleft - getFontlength(thumbValuePaint, text),
                        (float) ((0.5f * getHeight()) - thumbHalfHeight) - 3,
                        thumbValuePaint);

            } else {

                canvas.drawText(text,
                        textRight - getFontlength(thumbValuePaint, text),
                        (float) ((0.5f * getHeight()) - thumbHalfHeight) - 3,
                        thumbValuePaint);

            }

            // Log.e(TAG, "textRight>=maxThumbleft***textRight=" + textRight +
            // "maxThumbleft=" + maxThumbleft);
        } else {
            // 文字也从thumb点左边界画起
            canvas.drawText(text, screenCoord - thumbHalfWidth,
                    (float) ((0.5f * getHeight()) - thumbHalfHeight) - 3,
                    thumbValuePaint);

            // Log.i(TAG, "textRight<maxThumbleft***textRight=" + textRight +
            // "maxThumbleft=" + maxThumbleft);
        }

    }

    private void drawThumbMaxValue(float screenCoord, String text, Canvas canvas) {

        // 得到minThumb文字的right在屏幕上的绝对坐标
        float minThumbValueRight = normalizedToScreen(normalizedMinValue)
                - thumbHalfWidth
                + getFontlength(thumbValuePaint, "" + getSelectedMinValue());

        // 该文字的right坐标
        float textRight = screenCoord - thumbHalfWidth
                + getFontlength(thumbValuePaint, text);

        if (textRight >= getWidth()) {
            // 文字的right边界坐标达到整个屏幕宽度时
            canvas.drawText(text,
                    getWidth() - getFontlength(thumbValuePaint, text),
                    (float) ((0.5f * getHeight()) - thumbHalfHeight) - 3,
                    thumbValuePaint);

        } else if ((screenCoord - thumbHalfWidth) <= minThumbValueRight) {
            // maxThumb文字left边界达到minThumb文字right边界
            if (pressedThumb == Thumb.MAX) {

                canvas.drawText(text, minThumbValueRight,
                        (float) ((0.5f * getHeight()) - thumbHalfHeight) - 3,
                        thumbValuePaint);

            } else {

                canvas.drawText(text, screenCoord - thumbHalfWidth,
                        (float) ((0.5f * getHeight()) - thumbHalfHeight) - 3,
                        thumbValuePaint);

            }

        } else {

            canvas.drawText(text, screenCoord - thumbHalfWidth,
                    (float) ((0.5f * getHeight()) - thumbHalfHeight) - 3,
                    thumbValuePaint);
        }

    }

    /**
     * 取得thumb值的paint
     */
    private Paint getThumbValuePaint() {
        Paint p = new Paint();
        p.setColor(Color.RED);
        p.setAntiAlias(true);// 去除锯齿
        p.setFilterBitmap(true);// 对位图进行滤波处理
        p.setTextSize(20);

        return p;
    }

    /**
     * @return 返回指定笔和指定字符串的长度
     */
    private float getFontlength(Paint paint, String str) {
        return paint.measureText(str);
    }

    /**
     * @return 返回指定笔的文字高度
     */
    private float getFontHeight(Paint paint) {
        FontMetrics fm = paint.getFontMetrics();
        return fm.descent - fm.ascent;
    }

    /**
     * Decides which (if any) thumb is touched by the given x-coordinate.
     * <p>
     * eval ：n. 重新运算求出参数的内容
     *
     * @param touchX The x-coordinate of a touch event in screen space.
     * @return The pressed thumb or null if none has been
     * touched.//被touch的是空还是最大值或最小值
     */
    private Thumb evalPressedThumb(float touchX) {
        Thumb result = null;
        boolean minThumbPressed = isInThumbRange(touchX, normalizedMinValue);// 触摸点是否在最小值图片范围内
        boolean maxThumbPressed = isInThumbRange(touchX, normalizedMaxValue);
        if (minThumbPressed && maxThumbPressed) {
            // if both thumbs are pressed (they lie on top of each other),
            // choose the one with more room to drag. this avoids "stalling" the
            // thumbs in a corner, not being able to drag them apart anymore.

            // 如果两个thumbs重叠在一起，无法判断拖动哪个，做以下处理
            // 触摸点在屏幕右侧，则判断为touch到了最小值thumb，反之判断为touch到了最大值thumb
            result = (touchX / getWidth() > 0.5f) ? Thumb.MIN : Thumb.MAX;
        } else if (minThumbPressed) {
            result = Thumb.MIN;
        } else if (maxThumbPressed) {
            result = Thumb.MAX;
        }
        return result;
    }

    /**
     * Decides if given x-coordinate in screen space needs to be interpreted as
     * "within" the normalized thumb x-coordinate.
     *
     * @param touchX               The x-coordinate in screen space to check.
     * @param normalizedThumbValue The normalized x-coordinate of the thumb to check.
     * @return true if x-coordinate is in thumb range, false otherwise.
     */
    private boolean isInThumbRange(float touchX, double normalizedThumbValue) {
        // 当前触摸点X坐标-最小值图片中心点在屏幕的X坐标之差<=最小点图片的宽度的一般
        // 即判断触摸点是否在以最小值图片中心为原点，宽度一半为半径的圆内。
        return Math.abs(touchX - normalizedToScreen(normalizedThumbValue)) <= thumbHalfWidth;
    }

    /**
     * Sets normalized min value to value so that 0 <= value <= normalized max
     * value <= 1. The View will get invalidated when calling this method.
     *
     * @param value The new normalized min value to set.
     */
    public void setNormalizedMinValue(double value) {
        normalizedMinValue = Math.max(0d,
                Math.min(1d, Math.min(value, normalizedMaxValue)));
        invalidate();// 重新绘制此view
    }

    /**
     * Sets normalized max value to value so that 0 <= normalized min value <=
     * value <= 1. The View will get invalidated when calling this method.
     *
     * @param value The new normalized max value to set.
     */
    public void setNormalizedMaxValue(double value) {
        normalizedMaxValue = Math.max(0d,
                Math.min(1d, Math.max(value, normalizedMinValue)));
        invalidate();
    }

    /**
     * Converts a normalized value to a Number object in the value space between
     * absolute minimum and maximum.
     *
     * @param normalized 比例值，即改点的位置在该线总长度的的位置比例，改点在线中间，那么它的比例值就是0.5
     * @return 通过比例值与总长度计算得出的实际坐标知道
     */
    @SuppressWarnings("unchecked")
    private T normalizedToValue(double normalized) {
        return (T) numberType.toNumber(absoluteMinValuePrim + normalized
                * (absoluteMaxValuePrim - absoluteMinValuePrim));
    }

    /**
     * Converts the given Number value to a normalized double.
     *
     * @param value The Number value to normalize.
     * @return The normalized double.
     */
    private double valueToNormalized(T value) {
        if (0 == absoluteMaxValuePrim - absoluteMinValuePrim) {
            // prevent division by zero, simply return 0.
            return 0d;
        }
        return (value.doubleValue() - absoluteMinValuePrim)
                / (absoluteMaxValuePrim - absoluteMinValuePrim);
    }

    /**
     * Converts a normalized value into screen space.
     *
     * @param normalizedCoord The normalized value to convert.
     * @return The converted value in screen space.//图片中心点在屏幕上的绝对坐标值
     */
    private float normalizedToScreen(double normalizedCoord) {
        // getWidth() - 2 * padding --> 整个View宽度减去左右padding，
        // 即减去一个thumb的宽度,即两个thumb可滑动的范围长度

        // normalizedCoord * (getWidth() - 2 * padding)
        // 规格化值与长度的成绩，即该点在屏幕上的相对x坐标值

        // padding + normalizedCoord * (getWidth() - 2 * padding)
        // 该点在屏幕上的绝对x坐标值

        return (float) (padding + normalizedCoord * (getWidth() - 2 * padding));
        // return (float) (normalizedCoord * getWidth());
    }

    /**
     * Converts screen space x-coordinates into normalized values.
     *
     * @param screenCoord The x-coordinate in screen space to convert.
     * @return The normalized value.
     */
    private double screenToNormalized(float screenCoord) {
        int width = getWidth();
        if (width <= 2 * padding) {
            // prevent division by zero, simply return 0.
            return 0d;
        } else {
            double result = (screenCoord - padding) / (width - 2 * padding);
            return Math.min(1d, Math.max(0d, result));// 保证该该值为0-1之间，但是什么时候这个判断有用呢？
        }
    }

    /**
     * Thumb constants (min and max). 只有两个值，一个代表滑动条上的最大值，一个代表滑动条上的最小值
     */
    private static enum Thumb {
        MIN, MAX
    }

    /**
     * Utility enumaration used to convert between Numbers and doubles.
     *
     * @author Stephan Tittel (stephan.tittel@kom.tu-darmstadt.de)
     */
    private static enum NumberType {
        LONG, DOUBLE, INTEGER, FLOAT, SHORT, BYTE, BIG_DECIMAL;

        public static <E extends Number> NumberType fromNumber(E value)
                throws IllegalArgumentException {
            if (value instanceof Long) {
                return LONG;
            }
            if (value instanceof Double) {
                return DOUBLE;
            }
            if (value instanceof Integer) {
                return INTEGER;
            }
            if (value instanceof Float) {
                return FLOAT;
            }
            if (value instanceof Short) {
                return SHORT;
            }
            if (value instanceof Byte) {
                return BYTE;
            }
            if (value instanceof BigDecimal) {
                return BIG_DECIMAL;
            }
            throw new IllegalArgumentException("Number class '"
                    + value.getClass().getName() + "' is not supported");
        }

        public Number toNumber(double value) {
            // this代表调用该方法的对象，即枚举类中的枚举类型之一
            switch (this) {
                case LONG:
                    return new Long((long) value);
                case DOUBLE:
                    return value;
                case INTEGER:
                    return new Integer((int) value);
                case FLOAT:
                    return new Float(value);
                case SHORT:
                    return new Short((short) value);
                case BYTE:
                    return new Byte((byte) value);
                case BIG_DECIMAL:
                    return new BigDecimal(value);
            }
            throw new InstantiationError("can't convert " + this
                    + " to a Number object");
        }
    }

    ;

    /**
     * Callback listener interface to notify about changed range values.
     *
     * @param <T> The Number type the RangeSeekBar has been declared with.
     * @author Stephan Tittel (stephan.tittel@kom.tu-darmstadt.de)
     */
    public interface OnRangeSeekBarChangeListener<T> {
        public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar,
                                                T minValue, T maxValue);
    }
}