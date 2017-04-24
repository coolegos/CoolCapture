package com.egos.capture.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.egos.capture.R;

/**
 * Created by Egos on 2017/4/13.
 */
public class CropView extends View {

    private final static int DEFAULT_WIDTH = 200;
    private final static int DEFAULT_HEIGHT = DEFAULT_WIDTH;
    private final static int MIN_WIDTH = 200;
    private final static int MIN_HEIGHT = 200;

    private int mScreenWidth;
    private int mScreenHeight;

    private int mWidth;
    private int mHeight;
    private int mLeft;
    private int mTop;

    private Bitmap mBitmap;
    private int mBitmapWidth;
    private int mBitmapHeight;

    private final Paint mCropRectPaint = new Paint();
    private final Paint mBorderReactPaint = new Paint();
    private final Paint mBitmapPaint = new Paint();
    private final TextPaint mTextPaint = new TextPaint();

    private PorterDuffXfermode mPorterDuffXfermode;

    private boolean mShowButton = true;
    private int mShowIndex = -1;

    private int[] mDeltaX;
    private int[] mDeltaY;

    private int mLastX;
    private int mLastY;

    public CropView(Context context) {
        super(context);
        init();
    }

    public CropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CropView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public CropView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }


    private void init() {
        initPaint();
        initBitmap();
    }

    public void setDefaultSize(int width, int height) {
        mScreenWidth = width;
        mScreenHeight = height;
        invalidate();
    }

    /**
     * 初始化Paint。矩形需要透明。
     */
    private void initPaint() {

        mPorterDuffXfermode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);

        mCropRectPaint.setAntiAlias(true);
        mCropRectPaint.setColor(0x00000000);
        mCropRectPaint.setStyle(Paint.Style.FILL);
        mCropRectPaint.setStrokeWidth(20);

        mBorderReactPaint.setAntiAlias(true);
        mBorderReactPaint.setColor(0x66000000);
        mBorderReactPaint.setStyle(Paint.Style.FILL);
        mBorderReactPaint.setStrokeWidth(20);

        mBitmapPaint.setAntiAlias(true);

        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(20);
        mTextPaint.setColor(0xffff0000);
    }

    private void initBitmap() {
        mBitmap = ((BitmapDrawable) getResources().getDrawable(R.mipmap.camera_crop, null)).getBitmap();
        if (mBitmap != null) {
            mBitmapWidth = mBitmap.getWidth();
            mBitmapHeight = mBitmap.getHeight();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = DEFAULT_WIDTH;
        int height = DEFAULT_HEIGHT;
        if (mWidth == 0 && mHeight == 0) {
            int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            int widthSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            int heightSize = MeasureSpec.getSize(heightMeasureSpec);
            if (widthMode == MeasureSpec.AT_MOST) {
                widthSize = DEFAULT_WIDTH;
            }
            if (heightMode == MeasureSpec.AT_MOST) {
                heightSize = DEFAULT_HEIGHT;
            }

            if (widthSize > 0 && widthSize <= mScreenWidth) {
                width = widthSize;
            }

            if (heightSize > 0 && heightSize <= mScreenHeight) {
                height = heightSize;
            }
            mWidth = width;
            mHeight = height;

            mLeft = (mScreenWidth - width) / 2;
            mTop = (mScreenHeight - height) / 2;
        }

        calculate();
        setMeasuredDimension(mScreenWidth, mScreenHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        /**
         * 1.drawReact
         * 2.drawBitmap
         */
        canvas.drawRect(0, 0, mScreenWidth, mScreenHeight, mBorderReactPaint);
        mCropRectPaint.setXfermode(mPorterDuffXfermode);
        canvas.drawRect(mLeft, mTop, mLeft + mWidth, mTop + mHeight, mCropRectPaint);

        if (mBitmapWidth > 0 && mBitmapHeight > 0) {
            drawButtons(canvas);
        }

        canvas.drawText(mWidth + "X" + mHeight, mLeft, mTop + mHeight, mTextPaint);
    }

    public int getCropLeft() {
        return mLeft;
    }

    public int getCropTop() {
        return mTop;
    }

    public int getCropWidth() {
        return mWidth;
    }

    public int getCropHeight() {
        return mHeight;
    }

    /**
     * 1.移动，点击非Button区域 2.调整大小，点击Button区域
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mShowIndex = calculateIndex(x, y);
                break;
            case MotionEvent.ACTION_UP:
                mShowIndex = -1;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mShowIndex != -1) {
                    resizeCrop(mShowIndex, x, y);
                } else {
                    moveCrop(x, y);
                }
                break;
        }
        mLastX = x;
        mLastY = y;
        calculate();
        invalidate();
        return true;
    }

    public void hideButtons() {
        mShowButton = false;
    }

    public void showButtons() {
        mShowButton = true;
    }

    /**
     * 调整Crop大小
     */
    private void resizeCrop(int index, int x, int y) {
        int oriLeft = mLeft;
        int oriTop = mTop;
        int oriRight = mLeft + mWidth;
        int oriBottom = mTop + mHeight;
        switch (index) {
            case 0:
                mWidth = oriRight - x;
                checkWidth();
                mHeight = oriBottom - y;
                checkHeight();

                mLeft = oriRight - mWidth;
                mTop = oriBottom - mHeight;
                break;
            case 2:
                mWidth = x - mLeft;
                checkWidth();
                mHeight = oriBottom - y;
                checkHeight();

                mLeft = x - mWidth;
                mTop = oriBottom - mHeight;
                break;
            case 4:
                mWidth = x - mLeft;
                checkWidth();
                mHeight = y - mTop;
                checkHeight();

                break;
            case 6:
                mWidth = oriRight - x;
                checkWidth();
                mHeight = y - mTop;
                checkHeight();

                mLeft = oriRight - mWidth;
                mTop = y - mHeight;
                break;
            case 1:
                mHeight = oriBottom - y;
                checkHeight();

                mTop = oriBottom - mHeight;
                break;
            case 3:
                mWidth = x - oriLeft;
                checkWidth();

                mLeft = x - mWidth;
                break;
            case 5:
                mHeight = y - oriTop;
                checkHeight();
                break;
            case 7:
                mWidth = oriRight - x;
                checkWidth();

                mLeft = oriRight - mWidth;
                break;
            default:
                moveCrop(x, y);
                break;
        }
    }

    /**
     * 移动Crop
     */
    private void moveCrop(int x, int y) {
        int deltaX = x - mLastX;
        int deltaY = y - mLastY;
        mLeft += deltaX;
        if (mLeft < 0) {
            mLeft = 0;
        }
        if (mLeft + mWidth > mScreenWidth) {
            mLeft = mScreenWidth - mWidth;
        }
        mTop += deltaY;
        if (mTop < 0) {
            mTop = 0;
        }
        if (mTop + mHeight > mScreenHeight) {
            mTop = mScreenHeight - mHeight;
        }
    }

    private void checkWidth() {
        if (mWidth < MIN_WIDTH) {
            mWidth = MIN_WIDTH;
        }
    }

    private void checkHeight() {
        if (mHeight < MIN_HEIGHT) {
            mHeight = MIN_HEIGHT;
        }
    }

    private int checkX(int x) {
        if (x < 0) {
            return 0;
        }

        if (x > mWidth) {
            return mWidth;
        }

        return x;
    }

    private int checkY(int y) {
        if (y < 0) {
            return 0;
        }

        if (y > mHeight) {
            return mHeight;
        }

        return y;
    }

    /**
     * 计算应该展示的Index
     */
    private int calculateIndex(float x, float y) {
        RectF rectF = new RectF();
        if (isValid()) {
            for (int i = 0; i < mDeltaX.length; i++) {
                rectF.set(mDeltaX[i] - mBitmapWidth / 2, mDeltaY[i] - mBitmapHeight / 2,
                        mDeltaX[i] + mBitmapWidth / 2, mDeltaY[i] + mBitmapHeight / 2);

                if (rectF.contains(x, y)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean isValid() {
        return mDeltaX != null && mDeltaY != null && mDeltaX.length == mDeltaY.length;
    }

    /**
     * 计算Button位置
     */
    private void calculate() {
        if (mBitmapWidth > 0 && mBitmapHeight > 0) {
            mDeltaX = new int[]{mLeft, mLeft + mWidth / 2, mLeft + mWidth, mLeft + mWidth,
                    mLeft + mWidth, mLeft + mWidth / 2, mLeft, mLeft};

            mDeltaY = new int[]{mTop, mTop, mTop, mTop + mHeight / 2,
                    mTop + mHeight, mTop + mHeight, mTop + mHeight, mTop + mHeight / 2};
        }
    }

    private void drawButtons(Canvas canvas) {
        if (isValid()) {
            for (int i = 0; i < mDeltaX.length; i++) {
                boolean show = mShowButton && (mShowIndex == -1 || mShowIndex == i);
                drawButton(canvas, mDeltaX[i] - mBitmapWidth / 2, mDeltaY[i] - mBitmapHeight / 2, show);
            }
        }
    }

    private void drawButton(Canvas canvas, int x, int y, boolean show) {
        if (show) {
            canvas.drawBitmap(mBitmap, x, y, mBitmapPaint);
        }
    }
}
