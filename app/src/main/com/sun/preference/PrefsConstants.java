package com.sun.preference;

/**
 * Created by fredliao on 2016/8/12.
 */
class PrefsConstants {
    final static int STATUS_UNLOADED = 0;
    final static int STATUS_LOADING = 1;
    final static int STATUS_LOADED = 2;

    final static String CONTENT_SCHEME = "content://";
    final static String AUTHORITY_SUFFIX = ".oneprefs.provider";

    final static int METHOD_APPLY = 1;
    final static int METHOD_UPDATE = 2;

    static final int TYPE_STRING = 0;
    static final int TYPE_INT = 1;
    static final int TYPE_LONG = 2;
    static final int TYPE_FLOAT = 3;
    static final int TYPE_DOUBLE = 4;
    static final int TYPE_BOOLEAN = 5;
    static final int TYPE_STRING_LIST = 6;
    static final int TYPE_BYTE_ARRAY = 7;

    static final String VERSION = "version";

    static final String COMMON_PREFS_TAG = "Prefs";

    static final String INNER_PREFS_PREFIX = "OnePrefs_";
    static final String UPDATE_PREFS_FILE = INNER_PREFS_PREFIX + "UpdatePrefsFile";
}
