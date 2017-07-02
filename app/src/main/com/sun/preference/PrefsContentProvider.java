package com.sun.preference;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;

import com.sun.logger.SLog;
import com.sun.utils.AppUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 本类负责将prefs写入db, 并且在写入完成后通过 ContentObserver 通知监听者
 * <p/>
 * Created by ashercai on 8/9/16.
 */
public class PrefsContentProvider extends ContentProvider {
    private final static String TAG = PrefsConstants.COMMON_PREFS_TAG + "_ContentProvider";
    private Uri baseUri;
    private Context appContext;
    private DBHelper dbHelper;
    private HandlerThread handlerThread;
    private Handler handler;
    private AtomicInteger versionCounter;
    private static final String CONTENT_PROVIDER_THREAD = "content_provider_thread";
    private String mainSpName;

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context != null) {
            dbHelper = OnePrefsManager.getDBHelper(context);
            appContext = context.getApplicationContext();
            baseUri = PrefsHelper.getPrefsAuthority(context);
            versionCounter = new AtomicInteger();
            mainSpName = AppUtils.getAppSharedPrefName();
            return true;
        }
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private void ensureHandlerCreated() {
        synchronized (this) {
            if (handler == null) {
                handlerThread = new HandlerThread(CONTENT_PROVIDER_THREAD);
                handlerThread.start();
                handler = new Handler(handlerThread.getLooper());
            }
        }
    }

    @Override
    public synchronized Bundle call(final String method, final String arg, final Bundle extras) {
        ensureHandlerCreated();
        SLog.i(TAG, "provider call, method = " + method);
        final int version = versionCounter.incrementAndGet();
        handler.post(new Runnable() {
            @Override
            public void run() {
                int methodCode = getMethodCode(method);
                SLog.d(TAG, "methodCode = " + methodCode);
                switch (methodCode) {
                    case PrefsConstants.METHOD_APPLY:
                        handleApply(arg, extras, version);
                        break;
                    case PrefsConstants.METHOD_UPDATE:
                        handleUpdate(arg, version);
                        break;
                    default:
                        break;
                }
            }
        });

        Bundle bundle = new Bundle();
        bundle.putInt(PrefsConstants.VERSION, version);
        return bundle;
    }

    private int getMethodCode(String method) {
        int methodCode = -1;
        try {
            methodCode = Integer.parseInt(method);
        } catch (Exception ex) {
            SLog.e(TAG, PrefsHelper.printStack(ex));
        }
        return methodCode;
    }

    private void handleApply(String args, Bundle bundle, int version) {
        SLog.i(TAG, "handleApply 1, args = " + args);
        if (TextUtils.isEmpty(args)) {
            return;
        }

        String[] argArray = args.split("&");
        if (argArray.length < 3) {
            return;
        }

        String tableName = Uri.decode(argArray[0]);
        int pid = -1;
        try {
            pid = Integer.parseInt(argArray[1]);
        } catch (Exception ex) {
            SLog.e(TAG, PrefsHelper.printStack(ex));
            return;
        }
        boolean clear = Boolean.parseBoolean(argArray[2]);

        Map<String, Object> mapForUpdate = new HashMap<String, Object>();
        List<String> keysForDelete = new ArrayList<String>();

        generateApplyData(bundle, mapForUpdate, keysForDelete);
        PrefsHelper.logCollection(TAG, "keysForDelete", keysForDelete);
        dbHelper.writeSomeRows(tableName, mapForUpdate, keysForDelete, clear);
        notifyChange(tableName, pid, clear, version, mapForUpdate, keysForDelete);
    }

    private void handleUpdate(String args, int version) {
        SLog.i(TAG, "handleUpdate 1, args = " + args);
        if (TextUtils.isEmpty(args)) {
            return;
        }

        String[] argArray = args.split("&");
        String tableName = Uri.decode(argArray[0]);
        if (PrefsHelper.isPrefsFileOld(appContext, tableName)) {
            SLog.i(TAG, "handleUpdate 2");
            SLog.i(TAG, "getSharedPreferences, name = " + tableName);
            SharedPreferences sharedPreferences = appContext.getSharedPreferences(tableName, Context.MODE_PRIVATE);
            Map<String, ?> allOldData = sharedPreferences.getAll();
            Map<String, Object> updateTable = new HashMap<String, Object>();
            updateTable.put(tableName, true);
            dbHelper.updateTable(tableName, new HashMap<>(allOldData), updateTable);
            notifyChange(PrefsConstants.UPDATE_PREFS_FILE, -1, false, version, updateTable, null);
            clearMainSharedPreferenceFile(tableName, sharedPreferences);
        }
    }

    private void clearMainSharedPreferenceFile(String tableName, SharedPreferences sharedPreferences) {
        if (mainSpName.equals(tableName)) {
            SLog.i(TAG, "clearMainSharedPreferenceFile");
            sharedPreferences.edit().clear().apply();
        }
    }

    private void generateApplyData(Bundle bundle, Map<String, Object> mapForUpdate, List<String> keysForDelete) {
        Set<String> keySet = bundle.keySet();
        for (String key : keySet) {
            Object value = bundle.get(key);
            SLog.d(TAG, "providerApply, key = " + key + ", value = " + value);
            if (value == null) {
                keysForDelete.add(key);
            } else {
                mapForUpdate.put(key, value);
            }
        }
    }

    private void notifyChange(String tableName, int pid, boolean isClear, int version, Map<String, Object> mapForUpdate, List<String> keysForDelete) {
        String clearPath = String.valueOf(isClear);
        String modifiedPath = generateModifiedPath(mapForUpdate, keysForDelete);
        Uri notifyUri = baseUri.buildUpon().appendPath(tableName).appendPath(String.valueOf(pid)).appendPath(clearPath).appendPath(String.valueOf(version)).appendPath(modifiedPath).build();
        SLog.i(TAG, "notifyChange notifyUri = " + notifyUri);
        appContext.getContentResolver().notifyChange(notifyUri, null);
    }

    private String generateModifiedPath(Map<String, Object> mapForUpdate, List<String> keysForDelete) {
        StringBuilder updatePathBuilder = generateUpdatePath(mapForUpdate);
        StringBuilder deletePathBuilder = generateDeletePath(keysForDelete);

        return updatePathBuilder.length() == 0
                ? deletePathBuilder.toString()
                : (deletePathBuilder.length() == 0 ? updatePathBuilder.toString() : updatePathBuilder.toString() + "&" + deletePathBuilder.toString());
    }

    private StringBuilder generateUpdatePath(Map<String, Object> mapForUpdate) {
        StringBuilder updatePathBuilder = new StringBuilder();
        for (Map.Entry<String, Object> item : mapForUpdate.entrySet()) {
            String singlePath = generateSinglePath(item.getKey(), item.getValue());
            if (updatePathBuilder.length() == 0) {
                updatePathBuilder.append(singlePath);
            } else {
                updatePathBuilder.append("&");
                updatePathBuilder.append(singlePath);
            }
        }
        return updatePathBuilder;
    }

    private StringBuilder generateDeletePath(List<String> keysForDelete) {
        StringBuilder deletePathBuilder = new StringBuilder();
        if (keysForDelete == null) {
            return deletePathBuilder;
        }

        for (String item : keysForDelete) {
            String singlePath = generateSinglePath(item, null);
            if (deletePathBuilder.length() == 0) {
                deletePathBuilder.append(singlePath);
            } else {
                deletePathBuilder.append("&");
                deletePathBuilder.append(singlePath);
            }
        }
        return deletePathBuilder;
    }

    private String generateSinglePath(String key, Object value) {
        String path = Uri.encode(key);
        StringBuilder stringBuilder = new StringBuilder(path);
        if (value instanceof String) {
            stringBuilder.append(":").append(PrefsConstants.TYPE_STRING);
            if (((String) value).length() < 10) {
                stringBuilder.append(":").append(Uri.encode((String) value));
            }
        } else if (value instanceof Integer) {
            stringBuilder.append(":").append(PrefsConstants.TYPE_INT);
            stringBuilder.append(":").append(value);
        } else if (value instanceof Long) {
            stringBuilder.append(":").append(PrefsConstants.TYPE_LONG);
            stringBuilder.append(":").append(value);
        } else if (value instanceof Float) {
            stringBuilder.append(":").append(PrefsConstants.TYPE_FLOAT);
            stringBuilder.append(":").append(value);
        } else if (value instanceof Double) {
            stringBuilder.append(":").append(PrefsConstants.TYPE_DOUBLE);
            stringBuilder.append(":").append(value);
        } else if (value instanceof Boolean) {
            stringBuilder.append(":").append(PrefsConstants.TYPE_BOOLEAN);
            stringBuilder.append(":").append(value);
        } else if (value instanceof List) {
            stringBuilder.append(":").append(PrefsConstants.TYPE_STRING_LIST);
        } else if (value instanceof byte[]) {
            stringBuilder.append(":").append(PrefsConstants.TYPE_BYTE_ARRAY);
        }
        return stringBuilder.toString();
    }
}
