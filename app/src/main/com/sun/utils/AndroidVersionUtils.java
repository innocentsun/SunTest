package com.sun.utils;

import android.os.Build;

/**
 * Created by sunhzchen on 2017/1/4.
 * Android版本工具类
 */

public class AndroidVersionUtils {

    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= 9;
    }

    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= 11;
    }

    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= 12;
    }

    public static boolean hasHoneycombMR2() {
        return Build.VERSION.SDK_INT >= 13;
    }

    public static boolean hasIceCreamSandwich() {
        return Build.VERSION.SDK_INT >= 14;
    }

    public static boolean hasJellyBean() {
        return Build.VERSION.SDK_INT >= 16;
    }

    public static boolean hasJellyBeanMR1() {
        return Build.VERSION.SDK_INT >= 17;
    }

    public static boolean hasJellyBeanMR2() {
        return Build.VERSION.SDK_INT >= 18;
    }

    public static boolean hasKitKat() {
        return Build.VERSION.SDK_INT >= 19;
    }

    public static boolean hasLollipop() {
        return Build.VERSION.SDK_INT >= 21;
    }

    public static boolean hasMarshmallow() {
        return Build.VERSION.SDK_INT >= 23;
    }

    public static boolean hasNougat() {
        return Build.VERSION.SDK_INT >= 24;
    }
}
