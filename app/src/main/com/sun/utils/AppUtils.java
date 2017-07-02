package com.sun.utils;

import android.content.Context;
import android.text.TextUtils;

import com.sun.base.SunApplication;
import com.sun.logger.SLog;
import com.sun.preference.OnePrefs;
import com.sun.preference.OnePrefsManager;
import com.sun.test.BuildConfig;
import com.sun.test.R;
import com.sun.utils.toast.CommonToast;

/**
 * Created by sunhzchen on 2017/1/4.
 * APP相关工具类
 */

public class AppUtils {

    private static final String TAG = "AppUtils";
    private static final String FIRST_LAUNCH = "first_launch";
    private static final String FIRST_LAUNCH_TIME = "first_launch_time";

    private static final int EXIT_DELAY_MILLIS = 1000;

    private static String sharedPreferencesName;

    /**
     * SharedPreferences公用方法
     */
    public static OnePrefs getAppSharedPreferences() {
        return getSharedPreferences(getAppSharedPrefName());
    }

    public static OnePrefs getSharedPreferences(String name) {
        return OnePrefsManager.getOnePrefs(SunApplication.getAppContext(), name);
    }

    public static String getAppSharedPrefName() {
        if (sharedPreferencesName == null) {
            Context app = SunApplication.getAppContext();
            sharedPreferencesName = app.getPackageName() + Utils.getString(R.string
                    .preference_name);
        }
        return sharedPreferencesName;
    }

    public static void removeValueFromPreferences(String key) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        try {
            getAppSharedPreferences().edit().remove(key).apply();
        } catch (Exception e) {

        }
    }

    public static void setValueToPreferences(String key, boolean value) {
        try {
            getAppSharedPreferences().edit().putBoolean(key, value).apply();
        } catch (Exception e) {

        }
    }

    public static void setValueToPreferencesCommit(String key, boolean value) {
        try {
            getAppSharedPreferences().edit().putBoolean(key, value).apply();
        } catch (Exception e) {

        }
    }

    public static boolean getValueFromPreferences(String key, boolean defaultValue) {
        boolean result = defaultValue;
        try {
            OnePrefs sharedPreferences = getAppSharedPreferences();
            result = sharedPreferences.getBoolean(key, defaultValue);
        } catch (Exception e) {
        }

        return result;
    }


    public static int getValueFromPreferences(String key, int defaultValue) {
        int result = defaultValue;
        try {
            OnePrefs sharedPreferences = getAppSharedPreferences();
            result = sharedPreferences.getInt(key, defaultValue);
        } catch (Exception e) {

        }
        return result;
    }

    public static long getValueFromPreferences(String key, long defaultValue) {
        long result = defaultValue;
        try {
            OnePrefs sharedPreferences = getAppSharedPreferences();
            result = sharedPreferences.getLong(key, defaultValue);
        } catch (Exception e) {

        }
        return result;
    }


    public static String getValueFromPreferences(String key, String defaultValue) {
        String result = defaultValue;
        try {
            OnePrefs sharedPreferences = getAppSharedPreferences();
            result = sharedPreferences.getString(key, defaultValue);
        } catch (Exception e) {

        }

        return result;
    }

    public static void setValueToPreferences(String key, int value) {
        try {
            getAppSharedPreferences().edit().putInt(key, value).apply();
        } catch (Exception e) {
        }
    }

    public static void setValueToPreferences(String key, long value) {
        try {
            getAppSharedPreferences().edit().putLong(key, value).apply();
        } catch (Exception e) {
        }
    }

    public static void setValueToPreferences(String key, String value) {
        try {
            getAppSharedPreferences().edit().putString(key, value).apply();
        } catch (Exception e) {

        }
    }

    /**
     * 首次启动
     */
    public static boolean isFirstLaunch(Context context) {
        if (context == null) {
            return false;
        }
        OnePrefs prefs = getInitPrefs(context);
        return prefs.getBoolean(FIRST_LAUNCH, true);
    }

    public static void setFirstLaunch(Context context) {
        if (context == null) {
            return;
        }
        OnePrefs.Editor mEditor = getInitPrefs(context).edit();
        mEditor.putBoolean(FIRST_LAUNCH, false);
        mEditor.apply();
    }

    public static void setFirstLaunchTime(Context context) {
        if (isFirstLaunch(context)) {
            OnePrefs.Editor editor = getInitPrefs(context).edit();
            editor.putLong(FIRST_LAUNCH_TIME, System.currentTimeMillis());
            editor.apply();
        }
    }

    private static long getFirstLaunchTime(Context context) {
        OnePrefs shares = getInitPrefs(context);
        return shares.getLong(FIRST_LAUNCH_TIME, System.currentTimeMillis());
    }

    private static OnePrefs getInitPrefs(Context context) {
        return OnePrefsManager.getOnePrefs(context, AppUtils.getAppSharedPrefName());
    }

    public static boolean isDaysDelayed(int days) {
        long dayDuration = (System.currentTimeMillis() - getFirstLaunchTime(SunApplication
                .getAppContext())) / (1000 * 60 * 60 * 24);
        if (dayDuration <= days) {
            return true;
        }
        return false;
    }

    /**
     * 异常提醒
     */
    public static void remindException(final Throwable e) {
        SLog.e(TAG, e);
        if (BuildConfig.DEBUG) {
            CommonToast.showToastLong("start notify Exception!!");
            SunApplication.postDelayed(new Runnable() {
                @Override
                public void run() {
                    throw new RuntimeException(e);
                }
            }, EXIT_DELAY_MILLIS);
        }
    }
}
