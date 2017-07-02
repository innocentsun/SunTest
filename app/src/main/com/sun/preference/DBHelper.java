package com.sun.preference;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import com.sun.logger.SLog;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 这个类用于读取和修改sqlite db.
 * <p/>
 * Created by ashercai on 8/9/16.
 */
class DBHelper extends SQLiteOpenHelper {
    private final static String TAG = PrefsConstants.COMMON_PREFS_TAG + "_" + "DBHelper";
    private static final int DB_VERSION = 1;

    private static final String COLUMN_KEY = "KeyName";
    private static final String COLUMN_TYPE = "KeyType";
    private static final String COLUMN_VALUE = "KeyValue";

    private static final int COLUMN_KEY_INDEX = 0;
    private static final int COLUMN_TYPE_INDEX = 1;
    private static final int COLUMN_VALUE_INDEX = 2;

    private static final int COLUMN_KEY_STATEMENT_INDEX = COLUMN_KEY_INDEX + 1;
    private static final int COLUMN_TYPE_STATEMENT_INDEX = COLUMN_TYPE_INDEX + 1;
    private static final int COLUMN_VALUE_STATEMENT_INDEX = COLUMN_VALUE_INDEX + 1;
    private static final String DEFAULT_DB_NAME = "OnePrefs.db";
    private static final String ONE_PREFS_DB_NAME = "OnePrefsDBName";

    private SQLiteDatabase db;

    DBHelper(Context context) {
        super(context, getDBName(context), null, DB_VERSION);
        if (Build.VERSION.SDK_INT >= 16) {
            setWriteAheadLoggingEnabled(true);
        }

        try {
            db = getReadableDatabase();
        } catch (Exception e) {
            SLog.e(TAG, PrefsHelper.printStack(e));
        }
    }

    private static String getDBName(Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = appInfo.metaData;
            if (metaData != null) {
                for (String key : metaData.keySet()) {
                    if (ONE_PREFS_DB_NAME.equals(key)) {
                        String value = metaData.getString(key);
                        if (!TextUtils.isEmpty(value)) {
                            return value;
                        }
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
        return DEFAULT_DB_NAME;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //do nothing
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //do nothing for now
    }

    private void createTableIfNotExists(SQLiteDatabase db, String tableName) {
        synchronized (this) {
            String escapeTableName = "'" + tableName + "'";
            db.execSQL(String.format("CREATE TABLE IF NOT EXISTS %s (%s TEXT PRIMARY KEY, %s INTEGER, %s);", escapeTableName, COLUMN_KEY, COLUMN_TYPE, COLUMN_VALUE));
            db.execSQL(String.format("CREATE INDEX IF NOT EXISTS %s ON %s (%s);", getIndexName(tableName), escapeTableName, COLUMN_KEY));
        }
    }

    private String getIndexName(String tableName) {
        return "'" + tableName + "_idx'";
    }

    public Object readOneRow(String tableName, String keyName) {
        String where = COLUMN_KEY + " = ?";
        String[] whereArgs = new String[]{keyName};

        Object retValue = null;
        SQLiteCursor cursor = null;
        try {
            String escapeTableName = "'" + tableName + "'";
            cursor = (SQLiteCursor) db.query(escapeTableName, null, where, whereArgs, null, null, null, null);
            if (cursor.moveToNext()) {
                retValue = getValueFromCursor(cursor);
            }
        } catch (Exception ex) {
            if (ex.getMessage() != null && !ex.getMessage().contains("no such table")) {
                SLog.e(TAG, PrefsHelper.printStack(ex));
            }
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception ex) {
                    SLog.e(TAG, PrefsHelper.printStack(ex));
                }
            }
        }

        return retValue;
    }

    public int readAllRows(String tableName, OnePrefs.IReadRowCallBack callBack, int version) {
        if (callBack == null) {
            return 0;
        }

        int affectedRows = 0;
        SQLiteCursor cursor = null;
        try {
            String escapeTableName = "'" + tableName + "'";
            cursor = (SQLiteCursor) db.query(escapeTableName, null, null, null, null, null, null, null);
            affectedRows = traverseCursor(callBack, cursor, version);
        } catch (Exception ex) {
            if (ex.getMessage() != null && !ex.getMessage().contains("no such table")) {
                SLog.e(TAG, PrefsHelper.printStack(ex));
            }
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception ex) {
                    SLog.e(TAG, PrefsHelper.printStack(ex));
                }
            }
        }
        return affectedRows;
    }

    public int readSomeRows(String tableName, OnePrefs.IReadRowCallBack callBack, List<String> someKeys, int version) {
        if (callBack == null || someKeys == null || someKeys.size() <= 0) {
            return 0;
        }

        String where = getQueryWhere(someKeys);
        String[] whereArgs = someKeys.toArray(new String[]{});

        int affectedRows = 0;
        SQLiteCursor cursor = null;
        try {
            String escapeTableName = "'" + tableName + "'";
            cursor = (SQLiteCursor) db.query(escapeTableName, null, where, whereArgs, null, null, null, null);
            affectedRows = traverseCursor(callBack, cursor, version);
        } catch (Exception ex) {
            if (ex.getMessage() != null && !ex.getMessage().contains("no such table")) {
                SLog.e(TAG, PrefsHelper.printStack(ex));
            }
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception ex) {
                    SLog.e(TAG, PrefsHelper.printStack(ex));
                }
            }
        }

        return affectedRows;
    }

    private String getQueryWhere(List<String> someKeys) {
        StringBuilder whereBuilder = new StringBuilder();
        whereBuilder.append(COLUMN_KEY);
        whereBuilder.append(" IN (");
        for (int index = 0; index < someKeys.size(); index++) {
            if (index != 0) {
                whereBuilder.append(",");
            }
            whereBuilder.append("?");
        }
        whereBuilder.append(")");
        return whereBuilder.toString();
    }

    public int writeSomeRows(String tableName, Map<String, Object> mapForUpdate, List<String> keysForDelete, boolean clear) {
        int affectedRows = 0;
        try {
            db.beginTransaction();
            createTableIfNotExists(db, tableName);
            String escapeTableName = "'" + tableName + "'";
            clearTable(escapeTableName, clear);
            affectedRows += updateKeys(escapeTableName, mapForUpdate);
            affectedRows += deleteKeys(escapeTableName, keysForDelete);
            db.setTransactionSuccessful();
        } catch (Exception ex) {
            SLog.e(TAG, PrefsHelper.printStack(ex));
        } finally {
            try {
                db.endTransaction();
            } catch (Exception ex) {
                SLog.e(TAG, PrefsHelper.printStack(ex));
            }
        }
        return affectedRows;
    }

    public void updateTable(String tableName, Map<String, Object> allOldData, Map<String, Object> updateTable) {
        try {
            db.beginTransaction();
            writeSomeRows(tableName, allOldData, null, false);
            writeSomeRows(PrefsConstants.UPDATE_PREFS_FILE, updateTable, null, false);
            db.setTransactionSuccessful();
        } catch (Exception ex) {
            SLog.e(TAG, PrefsHelper.printStack(ex));
        } finally {
            try {
                db.endTransaction();
            } catch (Exception ex) {
                SLog.e(TAG, PrefsHelper.printStack(ex));
            }
        }
    }

    private int updateKeys(String tableName, Map<String, Object> keysForUpdate) {
        int affectedRows = 0;
        if (keysForUpdate != null && keysForUpdate.size() > 0) {
            StringBuilder updateSql = new StringBuilder("INSERT OR REPLACE INTO ");
            updateSql.append(tableName);
            updateSql.append("(");
            updateSql.append(COLUMN_KEY);
            updateSql.append(",");
            updateSql.append(COLUMN_TYPE);
            updateSql.append(",");
            updateSql.append(COLUMN_VALUE);
            updateSql.append(") VALUES(?, ?, ?)");
            SLog.d(TAG, "updateKeys 1, updateSql = " + updateSql);

            SQLiteStatement statement = null;
            try {
                statement = db.compileStatement(updateSql.toString());
                Iterator<Map.Entry<String, Object>> updateIterator = keysForUpdate.entrySet().iterator();
                while (updateIterator.hasNext()) {
                    statement.clearBindings();
                    Map.Entry<String, Object> entry = updateIterator.next();
                    if (!bindStatement(statement, entry)) {
                        continue;
                    }
                    if (statement.executeInsert() != -1) {
                        affectedRows++;
                    }
                }
            } catch (Exception ex) {
                SLog.e(TAG, PrefsHelper.printStack(ex));
            } finally {
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (Exception ex) {
                        SLog.e(TAG, PrefsHelper.printStack(ex));
                    }
                }
            }
            SLog.d(TAG, "updateKeys 2, affectedRows = " + affectedRows + ";allRows = " + keysForUpdate.size());
        }
        return affectedRows;
    }

    private int deleteKeys(String tableName, List<String> keysForDelete) {
        int affectedRows = 0;
        if (keysForDelete != null && keysForDelete.size() > 0) {
            StringBuilder deleteSql = new StringBuilder("DELETE FROM ");
            deleteSql.append(tableName);
            deleteSql.append(" WHERE ");
            deleteSql.append(COLUMN_KEY);
            deleteSql.append("=");
            deleteSql.append("?");
            SLog.d(TAG, "deleteKeys 1, deleteSql = " + deleteSql);

            SQLiteStatement statement = null;
            try {
                statement = db.compileStatement(deleteSql.toString());
                for (String key : keysForDelete) {
                    statement.clearBindings();
                    statement.bindString(COLUMN_KEY_STATEMENT_INDEX, key);
                    if (statement.executeUpdateDelete() > 0) {
                        affectedRows++;
                    }
                }
            } catch (Exception ex) {
                SLog.e(TAG, PrefsHelper.printStack(ex));
            } finally {
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (Exception ex) {
                        SLog.e(TAG, PrefsHelper.printStack(ex));
                    }
                }
            }
            SLog.d(TAG, "deleteKeys 2, affectedRows = " + affectedRows + ";allRows = " + keysForDelete.size());
        }
        return affectedRows;
    }

    private void clearTable(String tableName, boolean clear) {
        if (clear) {
            SLog.d(TAG, "clearTable tableName = " + tableName);
            String clearSql = "DELETE FROM " + tableName;
            try {
                db.execSQL(clearSql);
            } catch (Exception ex) {
                SLog.e(TAG, PrefsHelper.printStack(ex));
            }
        }
    }

    private boolean bindStatement(SQLiteStatement statement, Map.Entry<String, Object> entry) {
        if (entry.getValue() instanceof String) {
            statement.bindLong(COLUMN_TYPE_STATEMENT_INDEX, PrefsConstants.TYPE_STRING);
            statement.bindString(COLUMN_VALUE_STATEMENT_INDEX, (String) entry.getValue());
        } else if (entry.getValue() instanceof Integer) {
            statement.bindLong(COLUMN_TYPE_STATEMENT_INDEX, PrefsConstants.TYPE_INT);
            statement.bindLong(COLUMN_VALUE_STATEMENT_INDEX, (Integer) entry.getValue());
        } else if (entry.getValue() instanceof Long) {
            statement.bindLong(COLUMN_TYPE_STATEMENT_INDEX, PrefsConstants.TYPE_LONG);
            statement.bindLong(COLUMN_VALUE_STATEMENT_INDEX, (Long) entry.getValue());
        } else if (entry.getValue() instanceof Float) {
            statement.bindLong(COLUMN_TYPE_STATEMENT_INDEX, PrefsConstants.TYPE_FLOAT);
            statement.bindDouble(COLUMN_VALUE_STATEMENT_INDEX, (Float) entry.getValue());
        } else if (entry.getValue() instanceof Double) {
            statement.bindLong(COLUMN_TYPE_STATEMENT_INDEX, PrefsConstants.TYPE_DOUBLE);
            statement.bindDouble(COLUMN_VALUE_STATEMENT_INDEX, (Double) entry.getValue());
        } else if (entry.getValue() instanceof Boolean) {
            statement.bindLong(COLUMN_TYPE_STATEMENT_INDEX, PrefsConstants.TYPE_BOOLEAN);
            statement.bindLong(COLUMN_VALUE_STATEMENT_INDEX, ((Boolean) entry.getValue()) ? 1 : 0);
        } else if (entry.getValue() instanceof List || entry.getValue() instanceof Set) {
            statement.bindLong(COLUMN_TYPE_STATEMENT_INDEX, PrefsConstants.TYPE_STRING_LIST);
            statement.bindString(COLUMN_VALUE_STATEMENT_INDEX, PrefsHelper.convertCollectionToString((Collection<String>) entry.getValue()));
        } else if (entry.getValue() instanceof byte[]) {
            statement.bindLong(COLUMN_TYPE_STATEMENT_INDEX, PrefsConstants.TYPE_BYTE_ARRAY);
            statement.bindBlob(COLUMN_VALUE_STATEMENT_INDEX, (byte[]) entry.getValue());
        } else {
            return false;
        }
        statement.bindString(COLUMN_KEY_STATEMENT_INDEX, entry.getKey());
        return true;
    }

    private int traverseCursor(OnePrefs.IReadRowCallBack callBack, SQLiteCursor cursor, int version) {
        int affectedRows = 0;
        while (cursor.moveToNext() && callBack.isValid(version)) {
            String key = cursor.getString(COLUMN_KEY_INDEX);
            Object value = getValueFromCursor(cursor);
            if (!callBack.onSingleRowLoaded(version, key, value)) {
                break;
            }
            affectedRows++;
        }
        return affectedRows;
    }

    private Object getValueFromCursor(SQLiteCursor cursor) {
        Object retValue = null;
        int type = cursor.getInt(COLUMN_TYPE_INDEX);
        switch (type) {
            case PrefsConstants.TYPE_STRING:
                retValue = cursor.getString(COLUMN_VALUE_INDEX);
                break;
            case PrefsConstants.TYPE_INT:
                retValue = cursor.getInt(COLUMN_VALUE_INDEX);
                break;
            case PrefsConstants.TYPE_LONG:
                retValue = cursor.getLong(COLUMN_VALUE_INDEX);
                break;
            case PrefsConstants.TYPE_FLOAT:
                retValue = cursor.getFloat(COLUMN_VALUE_INDEX);
                break;
            case PrefsConstants.TYPE_DOUBLE:
                retValue = cursor.getDouble(COLUMN_VALUE_INDEX);
                break;
            case PrefsConstants.TYPE_BOOLEAN:
                retValue = cursor.getInt(COLUMN_VALUE_INDEX) == 1;
                break;
            case PrefsConstants.TYPE_STRING_LIST:
                retValue = PrefsHelper.convertStringToList(cursor.getString(COLUMN_VALUE_INDEX));
                break;
            case PrefsConstants.TYPE_BYTE_ARRAY:
                retValue = cursor.getBlob(COLUMN_VALUE_INDEX);
                break;
            default:
                break;
        }
        return retValue;
    }
}
