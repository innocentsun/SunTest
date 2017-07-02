package com.sun.logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by sunhzchen on 2017/1/7.
 * 非阻塞切不会创建临时对象栈
 */

final class Stack<T> {

    private final int mCapacity;
    private final Object[] mTs;
    private final AtomicBoolean mMutex;
    private int mCurrentIndex;

    Stack(int capacity) {
        mCapacity = capacity;
        mCurrentIndex = 0;
        mTs = new Object[capacity];
        mMutex = new AtomicBoolean(false);
    }

    final boolean push(T obj) {
        if (mMutex.compareAndSet(false, true)) {
            if (mCurrentIndex < mCapacity) {
                mTs[mCurrentIndex] = obj;
                mCurrentIndex++;
                mMutex.set(false);
                return true;
            }
            mMutex.set(false);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    final T pop() {
        if (mMutex.compareAndSet(false, true)) {
            if (mCurrentIndex > 0) {
                mCurrentIndex--;
                Object obj = mTs[mCurrentIndex];
                mTs[mCurrentIndex] = null;
                mMutex.set(false);
                return (T) obj;
            }
            mMutex.set(false);
        }
        return null;
    }
}
