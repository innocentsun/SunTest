package com.sun.base;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.sun.utils.AndroidVersionUtils;

/**
 * Created by sunhzchen on 2017/1/4.
 * 最基础的公用Activity类
 */

public class BaseActivity extends FragmentActivity {

    private int mActivityId;
    private boolean mIsDestroyed;
    private boolean mIsFinishing;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivityId = ActivityListManager.createActivityId();
        ActivityListManager.putActivity(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mIsDestroyed = true;
        ActivityListManager.removeActivity(this);
    }

    @Override
    public void finish() {
        superFinish();
        ActivityListManager.removeActivity(this);
    }

    public void superFinish() {
        mIsFinishing = true;
        try {
            super.finish();
        } catch (Exception e) {
        }
    }

    @Override
    public boolean isFinishing() {
        return mIsFinishing || super.isFinishing();
    }

    @Override
    public boolean isDestroyed() {
        boolean isDestroyed = mIsDestroyed;
        if (AndroidVersionUtils.hasJellyBeanMR1()) {
            isDestroyed = (isDestroyed || super.isDestroyed());
        }
        return isDestroyed || isFinishing();
    }

    public int getActivityId() {
        return mActivityId;
    }

    public String getName() {
        return getClass().getSimpleName();
    }
}
