package com.sun.logger;

import android.text.TextUtils;
import android.util.Log;

import com.sun.test.BuildConfig;

/**
 * Created by sunhzchen on 2017/1/5.
 * 自定义Log
 */

public final class SLog {

    public static int v(Object object, String msg) {
        return v(getTag(object), msg);
    }

    public static int v(String tag, String msg) {
        return logV(tag, msg);
    }

    public static int d(Object object, String msg) {
        return d(getTag(object), msg);
    }

    public static int d(String tag, String msg) {
        return logD(tag, msg);
    }

    public static int i(Object object, String msg) {
        return i(getTag(object), msg);
    }

    public static int i(String tag, String msg) {
        return logI(tag, msg);
    }

    public static int i(String tag, Object... msg) {
        if (msg != null && msg.length > 0) {
            String realMsg = TextUtils.join("", msg);
            return i(tag, realMsg);
        } else {
            return LogConstants.INVALID;
        }
    }

    public static int i(String tag, String format, Object... msg) {
        if (format != null) {
            String realMsg = format;
            try {
                realMsg = String.format(format, msg);
            } catch (Throwable th) {
                th.printStackTrace();
            }
            return i(tag, realMsg);
        } else {
            return LogConstants.INVALID;
        }
    }

    public static int w(Object object, String msg) {
        return w(getTag(object), msg);
    }

    public static int w(String tag, String msg) {
        return logW(tag, msg);
    }

    public static int e(Object object, String msg) {
        return e(getTag(object), msg);
    }

    public static int e(String tag, String msg) {
        return logE(tag, msg);
    }

    public static int e(String tag, Throwable t) {
        return e(tag, t, "");
    }

    public static int e(String tag, Throwable t, String msg) {
        String logMsg = "";
        if (!TextUtils.isEmpty(msg)) {
            logMsg = msg + "\n";
        }
        if (t != null) {
            logMsg = logMsg + Log.getStackTraceString(t);
        }
        return e(tag, logMsg);
    }

    private static int logV(String tag, String msg) {
        Logger.getInstance().log(LogConstants.LOG_FILE_NAME, tag, msg, LogConstants.LEVEL_V);
        return 0;
    }

    private static int logD(String tag, String msg) {
        Logger.getInstance().log(LogConstants.LOG_FILE_NAME, tag, msg, LogConstants.LEVEL_D);
        return 0;
    }

    private static int logI(String tag, String msg) {
        Logger.getInstance().log(LogConstants.LOG_FILE_NAME, tag, msg, LogConstants.LEVEL_I);
        return 0;
    }

    private static int logW(String tag, String msg) {
        Logger.getInstance().log(LogConstants.LOG_FILE_NAME, tag, msg, LogConstants.LEVEL_W);
        return 0;
    }

    private static int logE(String tag, String msg) {
        Logger.getInstance().log(LogConstants.LOG_FILE_NAME, tag, msg, LogConstants.LEVEL_E);
        return 0;
    }

    public static void printStack(String tag) {
        if (BuildConfig.DEBUG) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stackTrace) {
                Log.e(tag, element.toString());
            }
        }
    }

    private static String getTag(Object object) {
        return object.getClass().getSimpleName();
    }

    public static void finish() {
        Logger.getInstance().flush();
    }

}
