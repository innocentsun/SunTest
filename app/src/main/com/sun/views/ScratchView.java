package com.sun.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.sun.test.R;
import com.sun.utils.Constants;
import com.sun.utils.Utils;

/**
 * Created by sunhzchen on 2016/9/12.
 * 刮奖效果自定义视图
 */
public class ScratchView extends View {

    public static final int MIN_SLOP = 8;
    public static final int MAX_PERCENT = 75;

    private Path mErasePath;
    private Paint mMaskPaint;
    private Paint mErasePaint;
    private BitmapDrawable mWaterMark;
    private Bitmap mMaskBitmap;
    private Canvas mMaskCanvas;
    private int[] mPixels;
    private int mWidth;
    private int mHeight;
    private float mStartX;
    private float mStartY;
    private float mErasePixelCount = 0;
    private float mTotalErasePixel = 0;
    private int mPercent;
    private OnEraseStatusChangedListener mListener;
    private boolean mIsCompleted;

    public ScratchView(Context context) {
        super(context);
        init(context, null);
    }

    public ScratchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ScratchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ScratchView);
            int maskColor = typedArray.getColor(R.styleable.ScratchView_maskColor, Utils.getColor(R.color.grey));
            float eraseSize = typedArray.getFloat(R.styleable.ScratchView_eraseSize, Constants.ONE);
            int waterMark = typedArray.getResourceId(R.styleable.ScratchView_waterMark, Constants.INVALID);

            setMaskPaint(maskColor);
            setErasePaint(eraseSize);
            setWaterMark(waterMark);
        }
        mErasePath = new Path();
    }

    private void setMaskPaint(int maskColor) {
        mMaskPaint = new Paint();
        mMaskPaint.setAntiAlias(true);
        mMaskPaint.setDither(true);
        mMaskPaint.setColor(maskColor);
    }

    private void setErasePaint(float eraseSize) {
        mErasePaint = new Paint();
        mErasePaint.setAntiAlias(true);
        mErasePaint.setDither(true);
        mErasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mErasePaint.setStyle(Paint.Style.STROKE);
        mErasePaint.setStrokeCap(Paint.Cap.ROUND);
        mErasePaint.setStrokeWidth(eraseSize);
    }

    private void setWaterMark(int resId) {
        if (resId == Constants.INVALID) {
            mWaterMark = null;
        } else {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
            mWaterMark = new BitmapDrawable(bitmap);
            mWaterMark.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mMaskBitmap, 0, 0, mMaskPaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        createMaskBitmap(w, h);
        mPixels = new int[w * h];
    }

    private void createMaskBitmap(int width, int height) {
        createBitmapIfNecessary(width, height);
        drawMaskWithWaterMark(width, height);
    }

    private void createBitmapIfNecessary(int width, int height) {
        if (width != mWidth || height != mHeight || mMaskBitmap == null || mMaskCanvas == null) {
            mMaskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mMaskCanvas = new Canvas(mMaskBitmap);
            mWidth = width;
            mHeight = height;
        }
    }

    private void drawMaskWithWaterMark(int width, int height) {
        Rect rect = new Rect(0, 0, width, height);
        mMaskCanvas.drawRect(rect, mMaskPaint);
        if (mWaterMark != null) {
            Rect bound = new Rect(rect);
            mWaterMark.setBounds(bound);
            mWaterMark.draw(mMaskCanvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                startErase(event.getX(), event.getY());
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                erase(event.getX(), event.getY());
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                stopErase();
                invalidate();
                return true;

        }
        return super.onTouchEvent(event);
    }

    private void startErase(float x, float y) {
        mErasePath.reset();
        mErasePath.moveTo(x, y);
        mStartX = x;
        mStartY = y;
    }

    private void erase(float x, float y) {
        int deltaX = (int) Math.abs(x - mStartX);
        int deltaY = (int) Math.abs(y - mStartY);
        if (deltaX >= MIN_SLOP || deltaY >= MIN_SLOP) {
            mStartX = x;
            mStartY = y;
            mErasePath.lineTo(x, y);
            mMaskCanvas.drawPath(mErasePath, mErasePaint);
            mErasePath.reset();
            mErasePath.moveTo(x, y);
//            onErase();
            onEraseInMainProcess();
        }
    }

    private void onErase() {
        int width = getWidth();
        int height = getHeight();
        new AsyncTask<Integer, Integer, Boolean>() {
            @Override
            protected Boolean doInBackground(Integer... params) {
                int width = params[0];
                int height = params[1];
                int[] pixels = new int[width * height];
                mMaskBitmap.getPixels(pixels, 0, width, 0, 0, width, height);
                float erasePixelCount = 0;
                float totalErasePixel = width * height;
                for (int position = 0; position < totalErasePixel; position++) {
                    if (pixels[position] == 0) {
                        erasePixelCount++;
                    }
                }
                int percent = 0;
                if (erasePixelCount >= 0 && totalErasePixel > 0) {
                    percent = Math.round(erasePixelCount * 100 / totalErasePixel);
                    publishProgress(percent);
                }
                return percent >= MAX_PERCENT;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                mPercent = values[0];
                if (mListener != null) {
                    mListener.onProgress(mPercent);
                }
            }

        }.execute(width, height);
    }

    private void onEraseInMainProcess() {
        mMaskBitmap.getPixels(mPixels, 0, mWidth, 0, 0, mWidth, mHeight);
        mErasePixelCount = 0;
        mTotalErasePixel = mWidth * mHeight;
        for (int position = 0; position < mTotalErasePixel; position++) {
            if (mPixels[position] == 0) {
                mErasePixelCount++;
            }
        }
        if (mErasePixelCount >= 0 && mTotalErasePixel > 0) {
            mPercent = Math.round(mErasePixelCount * 100 / mTotalErasePixel);
        }
        if (mListener != null) {
            mListener.onProgress(mPercent);
        }
    }

    private void stopErase() {
        mErasePath.reset();
        mStartX = 0;
        mStartY = 0;
        if (!mIsCompleted && mPercent >= MAX_PERCENT && mListener != null) {
            mIsCompleted = true;
            mListener.onCompleted(ScratchView.this);
        }
    }

    public void setEraseStatusChangedListener(OnEraseStatusChangedListener listener) {
        mListener = listener;
    }

    public interface OnEraseStatusChangedListener {

        void onProgress(int percent);

        void onCompleted(View view);

    }

    public void reset() {
        mIsCompleted = false;
        int width = getWidth();
        int height = getHeight();
        createMaskBitmap(width, height);
        invalidate();
//        onErase();
        onEraseInMainProcess();
    }

    public void clear() {
        for(int i = 0; i< mPixels.length; i++){
            mPixels[i] = 0;
        }
        mMaskBitmap.setPixels(mPixels, 0, mWidth, 0, 0, mWidth, mHeight);
        invalidate();
//        onErase();
        onEraseInMainProcess();
    }
}
