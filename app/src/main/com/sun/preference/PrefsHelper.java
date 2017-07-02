package com.sun.preference;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.sun.logger.SLog;

import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by fredliao on 2016/8/12.
 */
class PrefsHelper {

    private final static String TAG = PrefsConstants.COMMON_PREFS_TAG + "_Helper";

    static String convertCollectionToString(Collection<String> collection) {
        if (collection == null || collection.size() <= 0) {
            return "";
        }

        JSONArray jsonArray = new JSONArray(collection);
        return jsonArray.toString();
    }

    static List<String> convertStringToList(String input) {
        if (TextUtils.isEmpty(input)) {
            return null;
        }

        ArrayList<String> stringList = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(input);
            for (int i = 0, size = jsonArray.length(); i < size; i++) {
                stringList.add(jsonArray.getString(i));
            }
        } catch (Exception e) {

        }
        return stringList;
    }

    static Uri getPrefsAuthority(Context context) {
        String notifyAuthorities = PrefsConstants.CONTENT_SCHEME + context.getPackageName() + PrefsConstants.AUTHORITY_SUFFIX;
        return Uri.parse(notifyAuthorities);
    }

    static void logCollection(String TAG, String preSLog, Collection<String> collection) {
        SLog.i(TAG, preSLog + ":" + collection);
    }

    static String printStack(Throwable t) {
        if (t == null)
            return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            try {
                t.printStackTrace();//输出到system.err
                t.printStackTrace(new PrintStream(baos));
            } finally {
                baos.close();
            }
        } catch (IOException e) {

        }

        return baos.toString();
    }

    static void printBundle(Bundle bundle) {
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                SLog.i(PrefsConstants.COMMON_PREFS_TAG, String.format("printBundle, key=%s, value=%s", key, bundle.get(key)));
            }
        }
    }

    static boolean isPrefsFileOld(Context context, String prefName) {
        if (prefName.contains(PrefsConstants.INNER_PREFS_PREFIX)) {
            SLog.d(TAG, "isPrefsFileOld 1, prefName = " + prefName);
            return false;
        }

        if (PrefsConstants.UPDATE_PREFS_FILE.equals(prefName)) {
            SLog.d(TAG, "isPrefsFileOld 2, prefName = " + prefName);
            return false;
        }

        File file = getSharedPreferencesPath(context, prefName + ".xml");
        if (file == null || !file.exists()) {
            SLog.d(TAG, "isPrefsFileOld 3, prefName = " + prefName);
            return false;
        }

        boolean isOld = !OnePrefsManager.getInnerOnePrefs(context, PrefsConstants.UPDATE_PREFS_FILE).getBoolean(prefName, false);
        SLog.d(TAG, "isPrefsFileOld 4, prefName = " + prefName + ", isOld = " + isOld);
        return isOld;
    }

    private static File getSharedPreferencesPath(Context context, String prefName) {
        File filesDir = context.getFilesDir();
        if (filesDir == null) {
            return null;
        }
        String sharedPrefDir = filesDir.getAbsolutePath().replace("files", "shared_prefs/");
        if (prefName.indexOf(File.separatorChar) < 0) {
            return new File(sharedPrefDir, prefName);
        }
        throw new IllegalArgumentException(
                "File " + prefName + " contains a path separator");
    }
}
