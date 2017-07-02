package com.sun.utils;

import android.graphics.Bitmap;

/**
 * Created by sunhzchen on 2017/1/11.
 * 图片处理相关工具类
 */

public class BitmapUtils {

    public static boolean isValidBitmap(Bitmap bitmap) {
        return bitmap != null && !bitmap.isRecycled();
    }
}
