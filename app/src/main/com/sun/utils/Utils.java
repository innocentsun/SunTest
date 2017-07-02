package com.sun.utils;

import android.content.res.Resources;
import android.os.Looper;
import android.util.SparseArray;

import com.sun.base.SunApplication;

import java.util.Collection;
import java.util.Map;

/**
 * Created by sunhzchen on 2017/1/4.
 * 基础工具类
 */

public class Utils {

    public static boolean isEmpty(final String str) {
        return str == null || str.length() <= 0;
    }

    public static boolean isEmpty(final Collection<? extends Object> collection) {
        return collection == null || collection.size() <= 0;
    }

    public static boolean isEmpty(final Map<? extends Object, ? extends Object> list) {
        return list == null || list.size() <= 0;
    }

    public static boolean isEmpty(final byte[] bytes) {
        return bytes == null || bytes.length <= 0;
    }

    public static boolean isEmpty(final String[] strArr) {
        return strArr == null || strArr.length <= 0;
    }

    public static boolean isEmpty(final SparseArray array) {
        return array == null || array.size() <= 0;
    }

    public static boolean isEqual(final Object arg1, final Object arg2) {
        if (arg1 == null || arg2 == null) {
            return false;
        }
        return arg1.equals(arg2);
    }

    public static Resources getResources() {
        Resources resources = SunApplication.getAppContext().getResources();
        boolean needRetry = (resources == null) && isSubThread();
        while (resources == null && needRetry) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
            }
            resources = SunApplication.getAppContext().getResources();
        }
        return resources;
    }

    public static boolean isUIThread() {
        return Thread.currentThread().equals(Looper.getMainLooper().getThread());
    }

    private static boolean isSubThread() {
        return Thread.currentThread() != Looper.getMainLooper().getThread();
    }

    public static String getString(int resId) {
        try {
            return getResources().getString(resId);
        } catch (Exception e) {
            return "";
        }
    }

    public static String getString(int resId, Object... args) {
        return getResources().getString(resId, args);
    }

    public static int getColor(int colorRes) {
        return getResources().getColor(colorRes);
    }
}
