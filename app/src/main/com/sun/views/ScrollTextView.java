package com.sun.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextView;

import com.sun.test.R;
import com.sun.utils.Constants;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by sunhzchen on 2016/7/26.
 * 轮播滚动视图
 */
public class ScrollTextView extends TextView {

    private int mIndex = 0;
    private int mNextIndex = 1;
    private float mStartY = 0f;
    private float mMovedDistance = 0f;
    private float mOffsetY = 0f;
    private float mSuffixLen = 0f;
    private int mMaxTextLen = 0;
    private int mWidth;
    private Paint mPaint;
    private ArrayList<String> mList = new ArrayList<>();
    private ValueAnimator mValueAnimator;
    private int mScrollTime;
    private static int mStopTime;



    private Runnable mCycleRunnable = new Runnable() {

        @Override
        public void run() {
            if (isRunning()) {
                return;
            }
            mValueAnimator = ValueAnimator.ofFloat(0, mMovedDistance);
            mValueAnimator.setDuration(mScrollTime != 0 ? mScrollTime : Constants.MILLI_500);
            mValueAnimator.addListener(new ListenerAdapter(ScrollTextView.this));
            mValueAnimator.start();
            mOffsetY = 0f;
            invalidate();
        }
    };

    private static class ListenerAdapter extends AnimatorListenerAdapter{
        private WeakReference<ScrollTextView> mScrollWeakReference;
        public ListenerAdapter(ScrollTextView scrollTextView) {
            mScrollWeakReference = new WeakReference<>(scrollTextView);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            if(mScrollWeakReference == null){
                return;
            }
            ScrollTextView scrollTextView = mScrollWeakReference.get();
            if (scrollTextView != null) {
                scrollTextView.postAnim();
            }
        }
    }

    public ScrollTextView(Context context) {
        super(context);
    }

    public ScrollTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setText(String text) {
        ArrayList<String> list = new ArrayList<>();
        list.add(text);
        setText(list);
    }

    public void setText(ArrayList<String> list) {
        if (isRunning()) {
            removeCallbacks(mCycleRunnable);
        }
        mList.clear();
        mList.addAll(list);
        mOffsetY = 0f;
        resetIndex();
        if (mWidth != 0) {
            update();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mList.isEmpty()) {
            return;
        }

        if (mOffsetY < mMovedDistance) {
            drawText(canvas, mIndex, mStartY - mOffsetY);
        }

        if (mOffsetY > 0) {
            drawText(canvas, mNextIndex, mStartY + mMovedDistance - mOffsetY);
        }

        if (isRunning()) {
            mOffsetY = (Float) mValueAnimator.getAnimatedValue();
            invalidate();
        } else {
            if (mOffsetY > 0) {
                mOffsetY = 0f;
                setIndex();
                invalidate();
            }
        }
    }

    private void drawText(Canvas canvas, int index, float y) {
        canvas.drawText(mList.get(index), 0f, y, mPaint);
    }

    public boolean isRunning() {
        return mValueAnimator != null && mValueAnimator.isRunning();
    }

    private void setIndex() {
        mIndex = mNextIndex;
        mNextIndex++;
        if (mNextIndex >= mList.size()) {
            mNextIndex = 0;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        mWidth = right - left;
        mMovedDistance = bottom - top;
        calculateSize();
        update();
    }

    private void calculateSize() {
        int textSize = (int) getTextSize();
        mPaint = createPaint(getCurrentTextColor(), textSize);
        mSuffixLen = mPaint.measureText(getResources().getString(R.string.three_dot_default));
        mMaxTextLen = mWidth / textSize;
        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        mStartY = -fontMetrics.top;
    }

    private Paint createPaint(int colorId, int textSize) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(textSize);
        paint.setColor(colorId);
        return paint;
    }

    private void update() {
        removeCallbacks(mCycleRunnable);
        checkDataLen();
        resetIndex();
        postInvalidate();
        postAnim();
    }

    private void resetIndex() {
        mIndex = 0;
        mNextIndex = 1;
    }

    private void checkDataLen() {
        int size = mList.size();
        for (int i = 0; i < size; i++) {
            String data = mList.get(i);
            mList.set(i, getSubset(data));
        }
    }

    private String getSubset(String str) {
        if (needSubset(str)) {
            if (str.length() > mMaxTextLen) {
                str = str.substring(0, mMaxTextLen);
            }
            str = subset(str) + getResources().getString(R.string.three_dot_default);
        }
        return str;
    }

    private boolean needSubset(String title) {
        float width = mPaint.measureText(title);
        return width > mWidth;
    }

    private String subset(String title) {
        float width = mPaint.measureText(title);
        if (width < (mWidth - mSuffixLen)) {
            return title;
        }
        return subset(title.substring(0, title.length() - 1));
    }

//    private Runnable mCycleRunnable = new Runnable() {
//
//        @Override
//        public void run() {
//            if (isRunning()) {
//                return;
//            }
//            mValueAnimator = ValueAnimator.ofFloat(0, mMovedDistance);
//            mValueAnimator.setDuration(mScrollTime != 0 ? mScrollTime : Constants.MILLI_500);
//            mValueAnimator.addListener(new AnimatorListenerAdapter() {
//                @Override
//                public void onAnimationEnd(Animator animation) {
//                    postAnim();
//                }
//            });
//            mValueAnimator.start();
//            mOffsetY = 0f;
//            invalidate();
//        }
//    };

    public  void postAnim() {
        if (mList.size() > 1) {
            removeCallbacks(mCycleRunnable);
            postDelayed(mCycleRunnable, mStopTime != 0 ? mStopTime : Constants.SECOND_1);
        }
    }

    public int getCurrentIndex() {
        return (mOffsetY < mMovedDistance / 2f) ? mIndex : mNextIndex;
    }

    public void setScrollTime(int scrollTime) {
        mScrollTime = scrollTime;
    }

    public void setStopTime(int stopTime) {
        mStopTime = stopTime;
    }
}
