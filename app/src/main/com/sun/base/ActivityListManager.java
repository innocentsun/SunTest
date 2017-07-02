package com.sun.base;

import android.util.SparseArray;

import com.sun.utils.Utils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by sunhzchen on 2017/1/4.
 * Activity统一管理类
 */

public class ActivityListManager {

    private static final AtomicInteger BASE_ACTIVITY_ID = new AtomicInteger(1);

    private static SparseArray<BaseActivity> sActivityList = new SparseArray<>();
    private static int sTopActivityId;

    public static int createActivityId() {
        return BASE_ACTIVITY_ID.getAndIncrement();
    }

    public static void putActivity(BaseActivity activity) {
        int key = activity.getActivityId();
        if (key > sTopActivityId) {
            sTopActivityId = key;
            sActivityList.put(key, activity);
        }
    }

    public static void removeActivity(BaseActivity activity) {
        int key = activity.getActivityId();
        sActivityList.remove(key);
        if (key == sTopActivityId) {
            updateTopActivity();
        }
    }

    private static void updateTopActivity() {
        int size = sActivityList.size();
        if (size > 0) {
            BaseActivity topActivity = sActivityList.valueAt(size - 1);
            sTopActivityId = topActivity.getActivityId();
        } else {
            sTopActivityId = 0;
        }
    }

    public static void releaseAllActivity() {
        int size = sActivityList.size();
        for (int i = 0; i < size; i++) {
            BaseActivity activity = sActivityList.valueAt(i);
            activity.superFinish();
        }
        sActivityList.clear();
        updateTopActivity();
    }

    public static BaseActivity getTopActivity() {
        if (sTopActivityId > 0) {
            return sActivityList.get(sTopActivityId);
        }
        return null;
    }

    public static void finishActivity(Class cls) {
        if (cls == null || Utils.isEmpty(sActivityList)) {
            return;
        }
        for (int i = 0; i < sActivityList.size(); i++) {
            BaseActivity activity = sActivityList.valueAt(i);
            if (Utils.isEqual(activity.getClass().getName(), cls.getName())) {
                activity.finish();
            }
        }
    }

    public static void limitActivityNumber(Class cls, int keepNum) {
        if (cls == null || Utils.isEmpty(sActivityList)) {
            return;
        }
        int startKey = -1;
        int endKey = -1;
        int clsNum = 0;
        for (int k = sActivityList.size() - 1; k >= 0; k--) {
            BaseActivity activity = sActivityList.valueAt(k);
            if (Utils.isEqual(activity.getClass().getName(), cls.getName())) {
                clsNum = clsNum + 1;
                if (clsNum == keepNum) {
                    endKey = activity.getActivityId();
                } else if (clsNum == keepNum + 1) {
                    startKey = activity.getActivityId();
                }
            }
        }
        if (startKey > 1 && endKey > 1) {
            for (int key = startKey; key < endKey; key++) {
                BaseActivity activity = sActivityList.get(key);
                if (activity != null && !activity.isFinishing()) {
                    activity.finish();
                }
            }
        }
    }

    public static SparseArray<BaseActivity> getActivityList() {
        return sActivityList;
    }
}
