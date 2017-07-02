package com.sun.preference;

import android.content.Context;
import android.text.TextUtils;

import java.util.HashMap;

/**
 * 本类负责管理OnePrefs
 * <p>
 * Created by ashercai on 8/9/16.
 */
public final class OnePrefsManager {
    private static final HashMap<String, OnePrefs> prefsHashMap = new HashMap<String, OnePrefs>();
    private volatile static DBHelper dbHelper;

    public static OnePrefs getOnePrefs(Context context, String prefName) {
        if (context == null || TextUtils.isEmpty(prefName)) {
            return null;
        }

        if (prefName.startsWith(PrefsConstants.INNER_PREFS_PREFIX)) {
            throw new RuntimeException("prefName with prefix 'OnePrefs' is occupied by library!");
        }

        init(context);
        return getInnerOnePrefs(context, prefName);
    }

    static OnePrefs getInnerOnePrefs(Context context, String prefName) {
        if (context == null || TextUtils.isEmpty(prefName)) {
            return null;
        }

        Context applicationContext = context.getApplicationContext();
        OnePrefs prefs;
        synchronized (prefsHashMap) {
            prefs = prefsHashMap.get(prefName);
            if (prefs == null) {
                prefs = new OnePrefs(prefName, getDBHelper(applicationContext), applicationContext);
                prefsHashMap.put(prefName, prefs);
            }
        }

        return prefs;
    }

    static DBHelper getDBHelper(Context context) {
        if (dbHelper == null) {
            synchronized (OnePrefsManager.class) {
                if (dbHelper == null) {
                    dbHelper = new DBHelper(context);
                }
            }
        }
        return dbHelper;
    }

    private static boolean isInited = false;

    private static void init(Context context) {
        if (isInited == false) {
            synchronized (OnePrefsManager.class) {
                if (isInited == false) {
                    OnePrefs updatePrefs = OnePrefsManager.getInnerOnePrefs(context, PrefsConstants.UPDATE_PREFS_FILE);
                    updatePrefs.setIsReadSync(true);
                    isInited = true;
                }
            }
        }
    }
}
