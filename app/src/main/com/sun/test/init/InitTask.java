package com.sun.test.init;

import android.util.Log;

import com.sun.base.SunApplication;
import com.sun.logger.SLog;
import com.sun.test.BuildConfig;
import com.sun.utils.AppUtils;

public abstract class InitTask implements Runnable {

    private static final String TAG = "InitTask";
    private static final long DEFAULT_LIMIT_TIME = 500;
    public ProcessStrategy mProcessStrategy = ProcessStrategy.MAIN;
    public int mThreadStrategy = LaunchInitManager.SUB_THREAD_TASK;
    public int mTriggerEvent;
    public long mLimitTime;

    public InitTask(int threadStrategy, int triggerEvent) {
        this(ProcessStrategy.MAIN, threadStrategy, triggerEvent, DEFAULT_LIMIT_TIME);
    }

    public InitTask(ProcessStrategy processStrategy, int threadStrategy, int triggerEvent) {
        this(processStrategy, threadStrategy, triggerEvent, DEFAULT_LIMIT_TIME);
    }

    public InitTask(ProcessStrategy processStrategy, int threadStrategy, int triggerEvent, long limitTime) {
        mProcessStrategy = processStrategy;
        mThreadStrategy = threadStrategy;
        mTriggerEvent = triggerEvent;
        mLimitTime = limitTime;
    }

    @Override
    public void run() {
        try {
            Log.e("TAG", "taskStart, threadName = " + Thread.currentThread().getName() + ", task = " + getClass().getSimpleName() + ", process = " + mProcessStrategy.name() + ", triggerEvent = " + mTriggerEvent);
            long startTime = System.currentTimeMillis();
            execute();
            long endTime = System.currentTimeMillis();
            long deltaTime = endTime - startTime;
            if (deltaTime > mLimitTime && BuildConfig.DEBUG) {
                SLog.e(TAG, "Task execute time has exceed limit time, task = " + this);
            }
            Log.e("TAG", "Task time, task = " + this + ", time = " + deltaTime);
        } catch (Throwable e) {
            AppUtils.remindException(e);
        }
    }

    public boolean isProcessMatch() {
        if (SunApplication.getAppContext().isMainProcess()) {
            return mProcessStrategy.initInMainProc();
        } else {
            return mProcessStrategy.initInSubProc();
        }
    }

    protected abstract void execute();
}
