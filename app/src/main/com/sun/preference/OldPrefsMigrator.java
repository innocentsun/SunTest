package com.sun.preference;

import android.content.Context;

import com.sun.logger.SLog;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * Created by fredliao on 2016/10/20.
 */

public class OldPrefsMigrator {

    private final static String TAG = "OldPrefsMigrator";

    static int migrateData(Context context, String prefName, OnePrefs.IReadRowCallBack callBack, int version) {
        if (callBack == null) {
            return 0;
        }

        int affectedRows = 0;
        SLog.i(TAG, "getSharedPreferences, name = " + prefName);
        Map<String, ?> allOldData = context.getSharedPreferences(prefName, Context.MODE_PRIVATE).getAll();
        Set<? extends Map.Entry<String, ?>> entrySet = allOldData.entrySet();
        for (Map.Entry<String, ?> entry : entrySet) {
            String key = entry.getKey();
            Object value = getConvertedValue(entry);
            if (!callBack.isValid(version) || !callBack.onSingleRowLoaded(version, key, value)) {
                break;
            }

            affectedRows++;
        }

        return affectedRows;
    }

    private static Object getConvertedValue(Map.Entry<String, ?> entry) {
        Object value = entry.getValue();
        if (value instanceof Set) {
            return new ArrayList<String>((Set) value);
        }
        return value;
    }
}
