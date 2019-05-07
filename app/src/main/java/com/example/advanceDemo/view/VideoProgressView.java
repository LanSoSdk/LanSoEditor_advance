package com.example.advanceDemo.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Iterator;
import java.util.LinkedList;

public class VideoProgressView extends SurfaceView implements
        SurfaceHolder.Callback, Runnable {

    private volatile State currentState = State.PAUSE;

    private boolean isVisible = true;
    /**
     * 已经绘制后的长度.
     */
    private float countWidth = 0;
    private float perProgress = 0;
    /**
     * 上一次绘制的时间.
     */
    private long initTime;
    private long drawFlashTime = 0;

    private volatile boolean drawing = false;

    private DisplayMetrics displayMetrics;
    private int screenWidth, progressHeight;

    private Paint backgroundPaint, progressPaint, flashPaint, minTimePaint,
            breakPaint, rollbackPaint;

    private float perWidth;
    private float flashWidth = 3f;
    private float minTimeWidth = 5f;
    private float breakWidth = 2f;

    private LinkedList<Integer> timeList = new LinkedList<Integer>();

    private Canvas canvas = null;
    private Thread thread = null;
    private SurfaceHolder holder = null;
    private float minRecordTimeMS = 2 * 1000f;
    private float maxRecordTimeMS = 15 * 1000f;
    private long curProgressTimeMs = 0;
    private long lastProgressTimeMs = 0;

    public VideoProgressView(Context context) {
        super(context);
        init(context);
    }

    public VideoProgressView(Context paramContext,
                             AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
        init(paramContext);

    }
    public VideoProgressView(Context paramContext,
                             AttributeSet paramAttributeSet, int paramInt) {
        super(paramContext, paramAttributeSet, paramInt);
        init(paramContext);
    }

    private void init(Context paramContext) {
        this.setZOrderOnTop(true);
        this.setZOrderMediaOverlay(true);
        displayMetrics = getResources().getDisplayMetrics();
        screenWidth = displayMetrics.widthPixels;

        progressPaint = new Paint();
        flashPaint = new Paint();
        minTimePaint = new Paint();
        breakPaint = new Paint();
        rollbackPaint = new Paint();
        backgroundPaint = new Paint();

        // setBackgroundColor(Color.parseColor("#222222"));
        // setBackgroundColor(Color.parseColor("#4db288"));
        //
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(Color.parseColor("#222222"));

        progressPaint.setStyle(Paint.Style.FILL);
        progressPaint.setColor(Color.parseColor("#4db288"));

        flashPaint.setStyle(Paint.Style.FILL);
        flashPaint.setColor(Color.parseColor("#FFFF00"));

        minTimePaint.setStyle(Paint.Style.FILL);
        minTimePaint.setColor(Color.parseColor("#ff0000"));

        breakPaint.setStyle(Paint.Style.FILL);
        breakPaint.setColor(Color.parseColor("#000000"));

        rollbackPaint.setStyle(Paint.Style.FILL);
        // rollbackPaint.setColor(Color.parseColor("#FF3030"));
        rollbackPaint.setColor(Color.parseColor("#f15369"));

        holder = getHolder();
        holder.addCallback(this);
    }

    @Override
    public void run() {
        while (drawing) {
            try {
                myDraw();
                Thread.sleep(40); // 这里40毫秒更新一次.
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 设置最小录制时间, 单位毫秒
     *
     * @param minS
     */
    public void setMinRecordTime(float minMs) {
        minRecordTimeMS = minMs;
    }

    /**
     * 设置最大录制时间, 单位毫秒.
     *
     * @param maxMs
     */
    public void setMaxRecordTime(float maxMs) {
        maxRecordTimeMS = maxMs;
        perWidth = screenWidth / maxRecordTimeMS;
    }

    /**
     * 当一段录制完成后, 把这一段的时长放进来, 单位是毫秒.
     *
     * @param timeMs
     */
    public void putTimeList(int timeMs) {
        timeList.add(timeMs);
    }

    /**
     * 清空放进来的所有段时长.
     */
    public void clearTimeList() {
        timeList.clear();
    }

    /**
     * 返回最后一段视频的时长, 单位是Ms
     *
     * @return
     */
    public int getLastTime() {
        if ((timeList != null) && (!timeList.isEmpty())) {
            return timeList.getLast();
        }
        return 0;
    }

    public boolean isTimeListEmpty() {
        return timeList.isEmpty();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        thread = new Thread(this);
        drawing = true;
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        drawing = false;
    }

    /**
     * 设置当前绘制的状态, 如果要执行删除,则从timeList中删除最后一个
     *
     * @param state
     */
    public void setCurrentState(State state) {
        currentState = state;
        if (state != State.START) {
            perProgress = perWidth;
        }
        curProgressTimeMs = 0;
        lastProgressTimeMs = 0;

        if (state == State.DELETE) {
            if ((timeList != null) && (!timeList.isEmpty())) {
                timeList.removeLast();

                Log.i("T", "timeList  size is:" + timeList.size());

            }
        }

    }

    private void myDraw() {
        canvas = holder.lockCanvas();
        progressHeight = getMeasuredHeight();

        if (canvas != null) {
            canvas.drawRect(0, 0, screenWidth, progressHeight, backgroundPaint);
        }

        countWidth = 0;

        if (!timeList.isEmpty()) {
            long curSegmentTime = 0;

            Iterator<Integer> iterator = timeList.iterator();
            while (iterator.hasNext()) {

                curSegmentTime = iterator.next(); // 当前这一段的时间.

                float left = countWidth;
                countWidth += curSegmentTime * perWidth;
                if (canvas != null) {
                    canvas.drawRect(left, 0, countWidth, progressHeight,
                            progressPaint);
                    canvas.drawRect(countWidth, 0, countWidth + breakWidth,
                            progressHeight, breakPaint);
                }
                countWidth += breakWidth;
            }
        }

        if (timeList.isEmpty()
                || (!timeList.isEmpty() && timeList.getLast() <= minRecordTimeMS)) {
            float left = perWidth * minRecordTimeMS;
            if (canvas != null) {
                canvas.drawRect(left, 0, left + minTimeWidth, progressHeight,
                        minTimePaint);
            }
        }
        if (currentState == State.BACKSPACE) {
            float left = countWidth - timeList.getLast() * perWidth; // 应该减去最后一段的时间.
            float right = countWidth;
            if (canvas != null) {
                canvas.drawRect(left, 0, right, progressHeight, rollbackPaint);
            }
        }
        /**
         * 手指按下时，绘制新进度条
         *
         * 绘制一个新的刻度.
         *
         * 当设置新的时间过来后, 这里应该是两个时间戳的相减
         */
        if (currentState == State.START) {
            perProgress += perWidth * (curProgressTimeMs - lastProgressTimeMs);

            float right = (countWidth + perProgress) >= screenWidth ? screenWidth
                    : (countWidth + perProgress);

            if (canvas != null) {
                canvas.drawRect(countWidth, 0, right, progressHeight,
                        progressPaint);
            }
        }

        long curSystemTime = System.currentTimeMillis();
        if (drawFlashTime == 0 || curSystemTime - drawFlashTime >= 500) {
            isVisible = !isVisible;
            drawFlashTime = curSystemTime;
        }

        if (isVisible) {
            if (currentState == State.START) {
                if (canvas != null) {
                    canvas.drawRect(countWidth + perProgress, 0, countWidth
                                    + flashWidth + perProgress, progressHeight,
                            flashPaint);
                }
            } else {
                if (canvas != null) {
                    canvas.drawRect(countWidth, 0, countWidth + flashWidth,
                            progressHeight, flashPaint);
                }
            }
        }

        lastProgressTimeMs = curProgressTimeMs;
        if (canvas != null) {
            holder.unlockCanvasAndPost(canvas);
        }
    }

    /**
     * 设置当前段的 进度, 单位是毫秒.
     *
     * @param progressTimeMs
     */
    public void setProgressTime(long progressTimeMs) {
        curProgressTimeMs = progressTimeMs;
    }

    public static enum State {

        START(0x1), PAUSE(0x2), BACKSPACE(0x3), DELETE(0x4);

        private int mIntValue;

        State(int intValue) {
            mIntValue = intValue;
        }

        static State mapIntToValue(final int stateInt) {
            for (State value : State.values()) {
                if (stateInt == value.getIntValue()) {
                    return value;
                }
            }
            return PAUSE;
        }

        int getIntValue() {
            return mIntValue;
        }
    }
}
