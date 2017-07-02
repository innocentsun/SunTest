package com.sun.preference;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.text.TextUtils;

import com.sun.base.ListenerManager;
import com.sun.logger.SLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 这个类用于读取和存储preferences
 * Created by ashercai on 8/9/16.
 */
public class OnePrefs {
    public interface OnOnePrefsChangeListener {
        void onOnePrefsChanged(OnePrefs prefs, String key);
    }

    public interface IReadRowCallBack {
        boolean isValid(int version);

        boolean onSingleRowLoaded(int version, String key, Object value);
    }

    static class PrefsValue {
        Object value;
        int version;
    }

    static class PrefsTask {
        public int version;
        public String modified;

        @Override
        public String toString() {
            return "version = " + version + "; modified = " + modified;
        }
    }

    static class ApplyTask {
        public boolean clear;
        public Bundle modified = new Bundle();
        public int localVersion;
        public boolean update;
    }

    public class Editor {
        private boolean clear;
        private final Map<String, Object> modified = new HashMap<String, Object>();

        public Editor putString(String key, String value) {
            synchronized (this) {
                modified.put(key, value);
                return this;
            }
        }

        public Editor putStringList(String key, List<String> values) {
            synchronized (this) {
                modified.put(key, values);
                return this;
            }
        }

        public Editor putInt(String key, int value) {
            synchronized (this) {
                modified.put(key, value);
                return this;
            }
        }

        public Editor putLong(String key, long value) {
            synchronized (this) {
                modified.put(key, value);
                return this;
            }
        }

        public Editor putFloat(String key, float value) {
            synchronized (this) {
                modified.put(key, value);
                return this;
            }
        }

        public Editor putDouble(String key, double value) {
            synchronized (this) {
                modified.put(key, value);
                return this;
            }
        }

        public Editor putBoolean(String key, boolean value) {
            synchronized (this) {
                modified.put(key, value);
                return this;
            }
        }

        public Editor remove(String key) {
            synchronized (this) {
                modified.put(key, null);
                return this;
            }
        }

        public Editor clear() {
            synchronized (this) {
                clear = true;
                return this;
            }
        }

        private void update() {
            ApplyTask updateTask = new ApplyTask();
            updateTask.update = true;
            synchronized (applyTaskQueue) {
                applyTaskQueue.add(updateTask);
            }
            executeApplyTaskQueueAsysnc();
        }

        public void apply() {
            Bundle bundle = null;
            boolean tempClear = false;
            synchronized (Editor.this) {
                if (clear == false && modified.isEmpty()) {
                    return;
                }

                bundle = convertModifiedToBundle();
                tempClear = clear;
                modified.clear();
                clear = false;
            }

            // 没有数据更新，则返回
            if (bundle.keySet().isEmpty() && tempClear == false) {
                return;
            }

            int localVersion = localVersionCounter.incrementAndGet();
            // 先更新内存
            applyToMemoryIgnoreVersion(tempClear, bundle, localVersion);
            generateApplyTask(bundle, tempClear, localVersion);
            executeApplyTaskQueueAsysnc();
        }

        private void generateApplyTask(Bundle bundle, boolean clear, int localVersion) {
            ApplyTask applyTask = new ApplyTask();
            applyTask.clear = clear;
            applyTask.modified = bundle;
            applyTask.localVersion = localVersion;

            synchronized (applyTaskQueue) {
                applyTaskQueue.add(applyTask);
            }
        }

        private void executeApplyTaskQueueAsysnc() {
            if (isApplyTaskQueueRunning) {
                return;
            }

            OnePrefsConfig.getConfigExecutorService().execute(new Runnable() {
                @Override
                public void run() {
                    synchronized (applyTaskQueueLock) {
                        isApplyTaskQueueRunning = true;
                        while (true) {
                            ApplyTask applyTask;
                            synchronized (applyTaskQueue) {
                                if (applyTaskQueue.size() <= 0) {
                                    isApplyTaskQueueRunning = false;
                                    return;
                                } else {
                                    applyTask = getMergeApplyTask();
                                }
                            }

                            if (applyTask.update) {
                                executeUpdateTask(applyTask);
                            } else {
                                executeSingleApplyTask(applyTask);
                            }
                        }
                    }
                }
            });
        }

        private ApplyTask getMergeApplyTask() {
            ApplyTask mergeTask = applyTaskQueue.remove(0);
            if (!mergeTask.update) {
                Iterator<ApplyTask> taskIterator = applyTaskQueue.iterator();
                while (taskIterator.hasNext()) {
                    ApplyTask task = taskIterator.next();
                    if (task.update) {
                        break;
                    }

                    Set<String> keySet = task.modified.keySet();
                    if (task.clear) {
                        mergeTask.clear = true;
                        mergeTask.modified.clear();
                        mergeTask.localVersion = task.localVersion;
                    } else if (keySet.size() + mergeTask.modified.keySet().size() > 50) {
                        break;
                    }

                    mergeBundle(mergeTask.modified, task.modified);
                    taskIterator.remove();
                }
            }
            return mergeTask;
        }

        private void mergeBundle(Bundle target, Bundle source) {
            for (String srcKey : source.keySet()) {
                bindSingleKV(target, srcKey, source.get(srcKey));
            }
        }

        private void executeUpdateTask(ApplyTask updateTask) {
            if (PrefsHelper.isPrefsFileOld(context, prefName)) {
                int callVersion = callContentProvider(updateTask);
                SLog.e(TAG, "callContentProvider, callVersion = " + callVersion);
            }
        }

        private void executeSingleApplyTask(ApplyTask applyTask) {
            // 如果有晚version的操作先回来了呢?所以这里的操作也应该比较version,clear也是
            int callVersion = callContentProvider(applyTask);
            SLog.i(TAG, "callContentProvider, callVersion = " + callVersion);
            if (callVersion == INVALID_VERSION) {
                return;
            }
            applyToMemory(applyTask.clear, applyTask.modified, callVersion);
            if (applyTask.localVersion >= localClearVersion) {
                localClearVersion = 0;
            }
            modifyVersion = callVersion;
            if (applyTask.clear) {
                setClearVersion(callVersion);
            }
            notifyListeners(applyTask.modified.keySet());
        }

        private void applyToMemoryIgnoreVersion(boolean clear, Bundle bundle, int localVersion) {
            SLog.d(TAG, String.format("applyToMemoryIgnoreVersion, clear = %s", clear));
            PrefsHelper.printBundle(bundle);
            if (clear) {
                synchronized (keyValues) {
                    keyValues.clear();
                    loadStatus = PrefsConstants.STATUS_LOADED;
                    localClearVersion = localVersion;
                    SLog.d(TAG, String.format("applyToMemoryIgnoreVersion 2, size = %d", keyValues.size()));
                }
            }

            for (String item : bundle.keySet()) {
                updateSingleKVToMemory(item, bundle.get(item), IGNORE_VERSION);
            }
        }

        private Bundle convertModifiedToBundle() {
            Bundle bundle = new Bundle();
            Iterator<Map.Entry<String, Object>> iterator = modified.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Object> item = iterator.next();
                String key = item.getKey();
                Object value = item.getValue();
                if (!clear) {
                    synchronized (keyValues) {
                        if (keyValues.containsKey(key) && equals(value, keyValues.get(key).value)) {
                            continue;
                        }
                    }
                }
                bindSingleKV(bundle, key, value);
            }
            return bundle;
        }

        private int callContentProvider(ApplyTask applyTask) {
            String args = "";
            Bundle callExtraData = null;
            int method;
            if (applyTask.update) {
                args = Uri.encode(prefName);
                callExtraData = null;
                method = PrefsConstants.METHOD_UPDATE;
            } else {
                args = Uri.encode(prefName) + "&" + pid + "&" + applyTask.clear;
                callExtraData = applyTask.modified;
                method = PrefsConstants.METHOD_APPLY;
            }

            for (int retryCount = 0; retryCount < 5; retryCount++) {
                try {
                    Bundle callResult = contentResolver.call(uri, String.valueOf(method), args, callExtraData);
                    if (callResult != null) {
                        return callResult.getInt(PrefsConstants.VERSION, INVALID_VERSION);
                    }
                } catch (Exception ex) {
                    SLog.e(TAG, PrefsHelper.printStack(ex));
                }
            }
            return INVALID_VERSION;
        }

        private void applyToMemory(boolean clear, Bundle bundle, int version) {
            SLog.d(TAG, String.format("applyToMemory, clear = %s, version = %s", clear, version));
            PrefsHelper.printBundle(bundle);
            handleClear(clear, version, true);

            for (String item : bundle.keySet()) {
                updateSingleKVConsiderLocal(item, bundle.get(item), version);
            }
        }

        private boolean equals(Object first, Object second) {
            return (first == null) ? (second == null) : (first.equals(second));
        }

        private void bindSingleKV(Bundle bundle, String key, Object value) {
            if (value instanceof String) {
                bundle.putCharSequence(key, (String) value);
            } else if (value instanceof Integer) {
                bundle.putInt(key, (int) value);
            } else if (value instanceof Long) {
                bundle.putLong(key, (long) value);
            } else if (value instanceof Float) {
                bundle.putFloat(key, (float) value);
            } else if (value instanceof Double) {
                bundle.putDouble(key, (double) value);
            } else if (value instanceof Boolean) {
                bundle.putBoolean(key, (boolean) value);
            } else if (value instanceof List) {
                bundle.putStringArrayList(key, (ArrayList<String>) value);
            } else if (value instanceof byte[]) {
                bundle.putByteArray(key, (byte[]) value);
            } else if (value == null) {
                bundle.putParcelable(key, null);
            }
        }
    }

    private void updateSingleKVConsiderLocal(String key, Object value, int version) {
        if (isApplyTaskContainsKey(key)) {
            return;
        }

        SLog.d(TAG, String.format("updateSingleKVConsiderLocal, key = %s, value = %s, version = %s", key, value, version));
        updateSingleKVToMemory(key, value, version);
    }

    private boolean isApplyTaskContainsKey(String key) {
        synchronized (applyTaskQueue) {
            for (ApplyTask applyTask : applyTaskQueue) {
                if (applyTask.clear || applyTask.modified.containsKey(key)) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * @param version 取值为IGNORE_VERSION时，忽略版本号比对。
     */
    private void updateSingleKVToMemory(String key, Object value, int version) {
        synchronized (keyValues) {
            if (!keyValues.containsKey(key)) {
                PrefsValue prefsValue = new PrefsValue();
                prefsValue.version = version;
                prefsValue.value = value;
                keyValues.put(key, prefsValue);
            } else if (version == IGNORE_VERSION) {
                keyValues.get(key).value = value;
            } else if (keyValues.get(key).version < version) {
                keyValues.get(key).version = version;
                keyValues.get(key).value = value;
            }
        }
    }

    private final static String TAG = PrefsConstants.COMMON_PREFS_TAG + "_One";
    public static final int IGNORE_VERSION = -1;
    private static final int INVALID_VERSION = -2;
    private final DBHelper dbHelper;
    private final String prefName;
    private final HashMap<String, PrefsValue> keyValues;
    private final HashSet<String> keyLockSet;
    private volatile int loadStatus = PrefsConstants.STATUS_UNLOADED;
    private final Uri uri;
    private final ContentResolver contentResolver;
    private final int pid;
    private final ListenerManager<OnOnePrefsChangeListener> prefsChangeListenerListenerMgr;
    private final List<PrefsTask> changeTaskQueue;
    private volatile boolean isChangeTaskQueueRunning = false;
    private final Object changeTaskQueueLock = new Object();
    private volatile int modifyVersion = 0;
    private volatile int clearVersion = 0;
    private volatile int localClearVersion = 0;
    private final AtomicInteger localVersionCounter;

    private final List<ApplyTask> applyTaskQueue;
    private volatile boolean isApplyTaskQueueRunning = false;
    private final Object applyTaskQueueLock = new Object();
    private final Object loadOldDataLock = new Object();
    private final Context context;
    private volatile boolean isReadSync = false;

    OnePrefs(String prefName, DBHelper dbHelper, Context context) {
        this.prefName = prefName;
        this.dbHelper = dbHelper;
        this.context = context;
        keyValues = new HashMap<String, PrefsValue>();
        keyLockSet = new HashSet<String>();
        uri = PrefsHelper.getPrefsAuthority(context).buildUpon().appendPath(prefName).build();
        contentResolver = context.getContentResolver();
        contentResolver.registerContentObserver(uri, true, prefsObserver);
        pid = Process.myPid();
        prefsChangeListenerListenerMgr = new ListenerManager<>();
        changeTaskQueue = new LinkedList<>();
        applyTaskQueue = new LinkedList<>();
        localVersionCounter = new AtomicInteger();
        edit().update();
        executeLoadAllRowAsync();
    }

    void setIsReadSync(boolean sync) {
        this.isReadSync = sync;
    }

    class ReadRowCallback implements IReadRowCallBack {
        @Override
        public boolean isValid(int version) {
            return version >= clearVersion && localClearVersion == 0;
        }

        @Override
        public boolean onSingleRowLoaded(int version, String key, Object value) {
            synchronized (keyValues) {
                /***
                 * 用keyValues作锁，希望这里是原子操作，与setClearVersion函数的锁结合看，这样一旦setClearVersion成功
                 * clearVersion生效，这里的不合法数据就不会写进来
                 */
                if (!isValid(version)) {
                    return false;
                }

                SLog.d(TAG, String.format("onSingleRowLoaded, version = %s, key = %s, value = %s", version, key, value));
                updateSingleKVToMemory(key, value, version);
                return true;
            }
        }
    }

    class ReadAllRowCallback extends ReadRowCallback {
        @Override
        public boolean isValid(int version) {
            return version >= clearVersion && localClearVersion == 0 && loadStatus != PrefsConstants.STATUS_LOADED;
        }
    }

    private void setClearVersion(int version) {
        // 考虑到read那边的操作,这边要做加锁操作
        synchronized (keyValues) {
            clearVersion = version;
        }
    }

    private ContentObserver prefsObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            SLog.i(TAG, "onChange, prefName = " + prefName + ";selfChange = " + selfChange + ";uri = " + uri);
            handleChange(uri);
        }
    };

    private void handleChange(Uri uri) {
        final List<String> pathSegments = uri.getPathSegments();
        if (pathSegments == null || pathSegments.size() <= 2) {
            return;
        }

        // 匹配表名
        if (!matchTable(pathSegments)) {
            return;
        }

        // 匹配进程号
        if (matchPid(pathSegments)) {
            return;
        }

        generateTask(pathSegments);
        executeChangeTaskQueueAsync();
    }

    private int updateVersion(List<String> pathSegments) {
        try {
            int version = Integer.parseInt(pathSegments.get(3));
            modifyVersion = version;
            return version;
        } catch (Exception ex) {
            SLog.e(TAG, PrefsHelper.printStack(ex));
        }
        return 0;
    }

    private void generateTask(final List<String> pathSegments) {
        boolean clear = Boolean.parseBoolean(pathSegments.get(2));
        int version = updateVersion(pathSegments);
        String modified = "";
        if (pathSegments.size() > 4) {
            modified = pathSegments.get(4);
        }

        if (clear) {
            setClearVersion(version);
        }

        PrefsTask task = new PrefsTask();
        task.version = version;
        task.modified = modified;

        synchronized (changeTaskQueue) {
            changeTaskQueue.add(task);
        }
    }

    private boolean matchTable(List<String> pathSegments) {
        String tableNameSeg = pathSegments.get(0);
        if (prefName.equals(tableNameSeg)) {
            return true;
        }
        return false;
    }

    private boolean matchPid(List<String> pathSegments) {
        String strProcessIdSeg = pathSegments.get(1);
        try {
            int processIdSeg = Integer.parseInt(strProcessIdSeg);
            if (pid == processIdSeg) {
                return true;
            }
        } catch (Exception ex) {
            SLog.e(TAG, PrefsHelper.printStack(ex));
            return false;
        }
        return false;
    }

    private void handleClear(boolean clear, int version, boolean considerLocal) {
        SLog.d(TAG, "handleClear, clear = " + clear);
        if (clear) {
            synchronized (keyValues) {
                Iterator<String> iterator = keyValues.keySet().iterator();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    if (considerLocal && isApplyTaskContainsKey(key)) {
                        continue;
                    }

                    if (keyValues.get(key).version < version) {
                        iterator.remove();
                    }
                }
            }

            loadStatus = PrefsConstants.STATUS_LOADED;
        }
    }

    private Set<String> updateModifiedValues(String modified, Set<String> unModifiedKeys, Set<String> allKeys, int version) {
        String[] modifiedArray = modified.split("&");
        for (String item : modifiedArray) {
            String[] itemArray = item.split(":");
            String key = Uri.decode(itemArray[0]);
            String type = itemArray.length > 1 ? itemArray[1] : "-1";
            String value = itemArray.length > 2 ? Uri.decode(itemArray[2]) : null;
            if (!modifySingle(key, type, value, version)) {
                unModifiedKeys.add(key);
            }
            allKeys.add(key);
        }
        return unModifiedKeys;
    }

    private boolean modifySingle(String key, String strType, String value, int version) {
        int type = -1;
        try {
            type = Integer.parseInt(strType);
        } catch (Exception ex) {
            SLog.e(TAG, PrefsHelper.printStack(ex));
            return true;
        }

        Object realValue = null;

        try {
            switch (type) {
                case PrefsConstants.TYPE_INT:
                    realValue = Integer.parseInt(value);
                    break;
                case PrefsConstants.TYPE_LONG:
                    realValue = Long.parseLong(value);
                    break;
                case PrefsConstants.TYPE_DOUBLE:
                    realValue = Double.parseDouble(value);
                    break;
                case PrefsConstants.TYPE_FLOAT:
                    realValue = Float.parseFloat(value);
                    break;
                case PrefsConstants.TYPE_BOOLEAN:
                    realValue = Boolean.parseBoolean(value);
                    break;
                case PrefsConstants.TYPE_STRING:
                    realValue = value;
                    break;
                case PrefsConstants.TYPE_STRING_LIST:
                case PrefsConstants.TYPE_BYTE_ARRAY:
                    return false;
                default:
                    removeKey(key, version);
                    return true;
            }
        } catch (Exception ex) {
            SLog.e(TAG, PrefsHelper.printStack(ex));
            return false;
        }

        if (realValue != null) {
            updateSingleKVToMemory(key, realValue, version);
        }
        return true;
    }

    private void removeKey(String key, int version) {
        synchronized (keyValues) {
            SLog.d(TAG, "modifySingle remove key = " + key);
            if (loadStatus == PrefsConstants.STATUS_LOADED) {
                keyValues.remove(key);
            } else {
                updateSingleKVToMemory(key, null, version);
            }
        }
    }

    private void notifyListeners(final Set<String> allKeys) {
        // 消息队列会持有Handler，不用考虑生命周期的问题，主线程通知回调，和系统实现保持一致
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                for (final String key : allKeys) {
                    prefsChangeListenerListenerMgr.startNotify(new ListenerManager.INotifyCallback<OnOnePrefsChangeListener>() {
                        @Override
                        public void onNotify(OnOnePrefsChangeListener listener) {
                            listener.onOnePrefsChanged(OnePrefs.this, key);
                        }
                    });
                }
            }
        });
    }

    public Map<String, ?> getAll() {
        synchronized (keyValues) {
            Map<String, Object> retValues = new HashMap<>();
            for (String key : keyValues.keySet()) {
                retValues.put(key, keyValues.get(key).value);
            }
            return retValues;
        }
    }

    public String getString(final String key, String defValue) {
        SLog.d(TAG, String.format("getString, key = %s, defValue = %s", key, defValue));
        String retValue = (String) getValue(key);
        return retValue != null ? retValue : defValue;
    }

    public List<String> getStringList(String key, List<String> defValues) {
        List<String> retValue = (List<String>) getValue(key);
        return retValue != null ? retValue : defValues;
    }

    public int getInt(String key, int defValue) {
        SLog.d(TAG, String.format("getInt, key = %s, defValue = %s", key, defValue));
        Integer retValue = (Integer) getValue(key);
        return retValue != null ? retValue : defValue;
    }

    public long getLong(String key, long defValue) {
        SLog.d(TAG, String.format("getLong, key = %s, defValue = %s", key, defValue));
        Long retValue = (Long) getValue(key);
        return retValue != null ? retValue : defValue;
    }

    public float getFloat(String key, float defValue) {
        SLog.d(TAG, String.format("getFloat, key = %s, defValue = %s", key, defValue));
        Float retValue = (Float) getValue(key);
        return retValue != null ? retValue : defValue;
    }

    public double getDouble(String key, double defValue) {
        SLog.d(TAG, String.format("getDouble, key = %s, defValue = %s", key, defValue));
        Double retValue = (Double) getValue(key);
        return retValue != null ? retValue : defValue;
    }

    public boolean getBoolean(String key, boolean defValue) {
        SLog.d(TAG, String.format("getBoolean, key = %s, defValue = %s", key, defValue));
        Boolean retValue = (Boolean) getValue(key);
        return retValue != null ? retValue : defValue;
    }

    public boolean contains(String key) {
        return getValue(key) != null;
    }

    public Editor edit() {
        return new Editor();
    }

    public void registerOnOnePrefsChangeListener(OnOnePrefsChangeListener listener) {
        prefsChangeListenerListenerMgr.register(listener);
    }

    public void unregisterOnOnePrefsChangeListener(OnOnePrefsChangeListener listener) {
        prefsChangeListenerListenerMgr.unregister(listener);
    }

    public void readSomeRows(final List<String> someKeys) {
        OnePrefsConfig.getConfigExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                dbHelper.readSomeRows(prefName, new ReadRowCallback(), someKeys, modifyVersion);
            }
        });
    }

    private Object getValue(final String key) {
        if (isValueLoaded(key)) {
            return getValueFromMemory(key);
        }

        if (isReadSync) {
            return readOneRowFromDb(key);
        } else {
            return getValueAsynchronous(key);
        }
    }

    private boolean isValueLoaded(String key) {
        if (loadStatus == PrefsConstants.STATUS_LOADED) {
            return true;
        }

        synchronized (keyValues) {
            if (keyValues.containsKey(key)) {
                return true;
            }
        }

        return false;
    }

    private Object getValueFromMemory(String key) {
        // 有可能被包含remove掉以及经单次查询没有数据的情况，这时候在内存中的存储状态是value为空
        synchronized (keyValues) {
            return keyValues.get(key) == null ? null : keyValues.get(key).value;
        }
    }

    private Object getValueAsynchronous(String key) {
        if (PrefsHelper.isPrefsFileOld(context, prefName)) {
            return getValueFromOld(key);
        } else {
            SLog.d(TAG, "getValueFromDbAynsc, key = " + key);
            return getValueFromDbAynsc(key);
        }
    }

    private Object getValueFromOld(String key) {
        synchronized (loadOldDataLock) {
            awaitQueryLock(loadOldDataLock, key);
        }

        SLog.d(TAG, "getValueFromOld, key = " + key);
        return getValueFromMemory(key);
    }

    private Object getValueFromDbAynsc(String key) {
        final String keyLock = key.intern();
        boolean needNewThread = false;
        synchronized (keyLockSet) {
            if (!keyLockSet.contains(keyLock)) {
                needNewThread = true;
                keyLockSet.add(keyLock);
            }
        }

        if (needNewThread) {
            SLog.d(TAG, String.format("getValueFromDbAynsc 1, key = %s", key));
            if (isValueLoaded(key)) {
                SLog.d(TAG, String.format("getValueFromDbAynsc 2, key = %s", key));
                return getValueFromMemory(key);
            }
            doQueryAsync(key, keyLock);
        }

        synchronized (keyLock) {
            awaitQueryLock(keyLock, key);
        }

        SLog.d(TAG, String.format("getValueFromDbAynsc 3, key = %s", key));
        return getValueFromMemory(key);
    }

    private void awaitQueryLock(Object keyLock, String key) {
        if (isValueLoaded(key)) {
            SLog.d(TAG, String.format("awaitQueryLock 1, key = %s", key));
            return;
        }

        // 循环等待，将等待粒度变小
        int totalTimes = 0;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            totalTimes = (int) (OnePrefsConfig.sMainThreadWaitTime / OnePrefsConfig.sUnitWaitTime);
        } else {
            totalTimes = (int) (OnePrefsConfig.sNonMainThreadWaitTime / OnePrefsConfig.sUnitWaitTime);
        }

        int currentTimes = 0;
        while (currentTimes++ < totalTimes && !isValueLoaded(key)) {
            try {
                keyLock.wait(OnePrefsConfig.sUnitWaitTime);
            } catch (Exception ex) {
                SLog.e(TAG, PrefsHelper.printStack(ex));
            }
        }
        SLog.d(TAG, String.format("awaitQueryLock 2, currentTimes = %s, totalTimes=%s", currentTimes, totalTimes));
    }

    private void doQueryAsync(final String key, final String keyLock) {
        executeLoadSingleRowAsync(key, keyLock);
    }

    private void executeLoadSingleRowAsync(final String key, final String keyLock) {
        OnePrefsConfig.getConfigExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                SLog.d(TAG, String.format("executeLoadSingleRowAsync 1, key = %s", key));
                readOneRowFromDb(key);
                SLog.d(TAG, String.format("executeLoadSingleRowAsync 2, key = %s", key));
                synchronized (keyLockSet) {
                    keyLockSet.remove(keyLock);
                }

                synchronized (keyLock) {
                    // 可能有多个线程同时在查询同一个key值
                    keyLock.notifyAll();
                }
                SLog.d(TAG, String.format("executeLoadSingleRowAsync 3, key = %s", key));
            }
        });
    }

    private Object readOneRowFromDb(String key) {
        int version = modifyVersion;
        Object value = dbHelper.readOneRow(prefName, key);
        updateSingleKVToMemory(key, value, version);
        return value;
    }

    private void executeLoadAllRowAsync() {
        if (loadStatus != PrefsConstants.STATUS_UNLOADED) {
            SLog.d(TAG, "executeLoadAllRowAsync 0, prefName = " + prefName);
            return;
        }

        OnePrefsConfig.getConfigExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                if (loadStatus != PrefsConstants.STATUS_UNLOADED) {
                    SLog.d(TAG, "executeLoadAllRowAsync 1, prefName = " + prefName);
                    return;
                }

                loadStatus = PrefsConstants.STATUS_LOADING;
                SLog.d(TAG, "executeLoadAllRowAsync 2, prefName = " + prefName);
                if (PrefsHelper.isPrefsFileOld(context, prefName)) {
                    SLog.i(TAG, "migrateData, prefName = " + prefName);
                    OldPrefsMigrator.migrateData(context, prefName, new ReadAllRowCallback(), modifyVersion);
                    synchronized (loadOldDataLock) {
                        loadOldDataLock.notifyAll();
                    }
                } else {
                    SLog.i(TAG, "executeLoadAllRowAsync 3, prefName = " + prefName);
                    dbHelper.readAllRows(prefName, new ReadAllRowCallback(), modifyVersion);
                }
                loadStatus = PrefsConstants.STATUS_LOADED;
            }
        });
    }

    private void executeChangeTaskQueueAsync() {
        if (isChangeTaskQueueRunning) {
            return;
        }

        OnePrefsConfig.getConfigExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                synchronized (changeTaskQueueLock) {
                    isChangeTaskQueueRunning = true;
                    while (true) {
                        Set<String> allKeys = new HashSet<String>();
                        Set<String> unModifiedKeys = new HashSet<String>();
                        synchronized (changeTaskQueue) {
                            if (changeTaskQueue.size() <= 0) {
                                isChangeTaskQueueRunning = false;
                                return;
                            } else {
                                handleAllTaskModifiedData(unModifiedKeys, allKeys);
                            }
                        }
                        // 合并所有数据批量去读db，用modifyVersion是因为db的数据已经是最新的版本数据了
                        dbHelper.readSomeRows(prefName, new ReadRowCallback(), new ArrayList<String>(unModifiedKeys), modifyVersion);
                        notifyListeners(allKeys);
                    }
                }
            }
        });
    }

    private void handleAllTaskModifiedData(Set<String> unModifiedKeys, Set<String> allKeys) {
        while (changeTaskQueue.size() > 0) {
            PrefsTask prefsTask = changeTaskQueue.remove(0);
            if (prefsTask.version < clearVersion) {
                continue;
            }

            if (prefsTask.version == clearVersion) {
                handleClear(true, prefsTask.version, false);
            }

            if (!TextUtils.isEmpty(prefsTask.modified)) {
                updateModifiedValues(prefsTask.modified, unModifiedKeys, allKeys, prefsTask.version);
                PrefsHelper.logCollection(TAG, "handleAllTaskModifiedData, prefsTask = " + prefsTask, unModifiedKeys);
            }
        }
    }
}
