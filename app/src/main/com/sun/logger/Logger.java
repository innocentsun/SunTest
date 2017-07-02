package com.sun.logger;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.sun.test.BuildConfig;
import com.sun.utils.AndroidVersionUtils;
import com.sun.utils.ProcessUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sunhzchen on 2017/1/6.
 * 日志写入文件
 */

class Logger {

    private static final String EMPTY = "";
    private static final String PID = "PID";
    private static final String LOG_THREAD = "Log_Thread";
    private static final String TAG = "Logger";
    private static final String LOG_FILE_SEPARATOR = "_";
    private static final String LOG_HEAD_SEPARATOR = " # ";
    private static final String LOG_LEFT_SEPARATOR = " [";
    private static final String LOG_RIGHT_SEPARATOR = "] ";
    private static final String LOG_MIDDLE_SEPARATOR = ",";
    private static final String LOG_FILE_SUFFIX = ".log";

    private static final int MESSAGE_INIT = 0;
    private static final int MESSAGE_LOOP = 1;
    private static final int MESSAGE_LOG = 2;
    private static final int MESSAGE_FLUSH = 3;
    private static final int MESSAGE_QUIT = 4;

    private String mPid;
    private String mProcessSuffix = EMPTY;
    private StringBuilder mStringBuilder;
    private HandlerThread mLogThread;
    private volatile Handler mLogHandler;
    private Pattern mLogRegex = Pattern.compile("(.*)_(\\d*)\\.log");
    private Stack<LogInfoItem> mLogInfoStack;
    private Map<String, LogFileInfo> mLogFileMap;
    private Map<String, WriterInfo> mWriterMap;
    private Date mDate = new Date();
    private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale
            .CHINA);

    private static class InstanceHolder {
        private static Logger sInstance = new Logger();
    }

    static Logger getInstance() {
        return InstanceHolder.sInstance;
    }

    private Logger() {
        init();
        checkCloseLoop();
    }

    private void init() {
        mPid = String.valueOf(Process.myPid());
        String processName = ProcessUtils.getProcessName();
        if (TextUtils.isEmpty(processName)) {
            mProcessSuffix = PID + mPid;
        } else {
            String[] processNameStrings = processName.split(":");
            if (processNameStrings.length >= 2) {
                mProcessSuffix = processNameStrings[processNameStrings.length - 1];
            }
        }
        mStringBuilder = new StringBuilder();
        mLogInfoStack = new Stack<>(LogConstants.LOG_INFO_CACHE_COUNT);
        mLogFileMap = new HashMap<>();
        mWriterMap = new HashMap<>();
        mLogThread = new HandlerThread(LOG_THREAD);
        mLogThread.setUncaughtExceptionHandler((t, e) -> Log.e(TAG, "UncaughtException", e));
        mLogThread.start();
        mLogHandler = new Handler(mLogThread.getLooper(), mCallback);
        mLogHandler.sendEmptyMessage(MESSAGE_INIT);
    }

    private Handler.Callback mCallback = msg -> {
        switch (msg.what) {
            case MESSAGE_INIT: {
                doInit();
                break;
            }
            case MESSAGE_LOOP: {
                checkClose();
                checkCloseLoop();
                break;
            }
            case MESSAGE_LOG: {
                LogInfoItem logInfoItem = (LogInfoItem) msg.obj;
                doLog(logInfoItem.mFileName, logInfoItem.mTag, logInfoItem.mMsg, logInfoItem
                        .mLevel, logInfoItem.mThreadId);
                recycleLogInfoItem(logInfoItem);
                break;
            }
            case MESSAGE_FLUSH: {
                doFlush();
                Object lock = msg.obj;
                if (lock != null) {
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
                break;
            }
            case MESSAGE_QUIT: {
                doQuit();
                break;
            }
        }
        return false;
    };

    private void doInit() {
        File logDirectory = new File(LogConstants.LOG_DIRECTORY);
        boolean success = logDirectory.mkdirs();
        File[] logFiles = logDirectory.listFiles();
        if (!success || logFiles == null || logFiles.length == 0) {
            Log.e(TAG, "Log files is empty");
            return;
        }
        for (File file : logFiles) {
            Matcher matcher = mLogRegex.matcher(file.getName());
            if (matcher.matches()) {
                String fileName = matcher.group(0);
                int index = Integer.parseInt(matcher.group(1));
                LogFileInfo info = mLogFileMap.get(fileName);
                if (info == null) {
                    info = new LogFileInfo();
                    info.mIndex = index;
                    info.mLastModified = file.lastModified();
                    mLogFileMap.put(fileName, info);
                } else if (info.mLastModified < file.lastModified()) {
                    info.mIndex = index;
                    info.mLastModified = file.lastModified();
                }
            }
        }
    }

    private void checkClose() {
        long aliveTime = WriterInfo.now() - LogConstants.CHECK_CLOSE_LOOP_INTERVAL;
        Iterator<Map.Entry<String, WriterInfo>> iterator = mWriterMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, WriterInfo> item = iterator.next();
            WriterInfo writerInfo = item.getValue();
            if (writerInfo.mLastWriteTime < aliveTime) {
                closeWriter(writerInfo.mWriter);
                iterator.remove();
            } else {
                flushWriter(writerInfo.mWriter);
            }
        }
    }

    private void checkCloseLoop() {
        if (mLogHandler != null) {
            mLogHandler.sendEmptyMessageDelayed(MESSAGE_LOOP, LogConstants
                    .CHECK_CLOSE_LOOP_INTERVAL);
        }
    }

    private void closeWriter(BufferedWriter writer) {
        if (writer != null) {
            try {
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void flushWriter(BufferedWriter writer) {
        if (writer != null) {
            try {
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void log(String filename, String tag, String log, int level) {
        if (mLogHandler == null) {
            return;
        }
        long threadId = Thread.currentThread().getId();
        if (threadId != mLogThread.getId()) {
            Message message = Message.obtain(mLogHandler);
            message.what = MESSAGE_LOG;
            message.obj = fetchLogInfoItem(filename, tag, log, level, threadId);
            mLogHandler.sendMessage(message);
        } else {
            doLog(filename, tag, log, level, threadId);
        }
    }

    private void doLog(String filename, final String tag, final String msg, final int level,
                       final long threadId) {
        String realFileName = getRealFileName(filename);
        WriterInfo writerInfo = getWriterInfo(realFileName);
        if (writerInfo == null) {
            Log.e(TAG, "WriterInfo is empty!");
            return;
        }
        BufferedWriter writer = writerInfo.mWriter;
        if (writer == null) {
            Log.e(TAG, "BufferedWriter is empty!");
            return;
        }
        formatLog(tag, msg, level, threadId, writerInfo, writer);
        if (writerInfo.mFileSize > LogConstants.MAX_FILE_SIZE) {
            closeWriter(writer);
            mWriterMap.remove(realFileName);
            LogFileInfo info = mLogFileMap.get(realFileName);
            info.mIndex++;
            getWriterInfo(realFileName);
            File needDeleteFile = new File(getLogPath(realFileName, (info.mIndex - LogConstants
                    .MAX_FILE_COUNT + LogConstants.MAX_FILE_COUNT_NUMBER) % LogConstants
                    .MAX_FILE_COUNT_NUMBER));
            if (needDeleteFile.exists()) {
                boolean success = needDeleteFile.delete();
                Log.e(TAG, "delete file: " + needDeleteFile.getName() + " " + success);
            }
        }
    }

    private String getRealFileName(String fileName) {
        if (TextUtils.isEmpty(mProcessSuffix)) {
            return fileName;
        }
        return fileName + LOG_FILE_SEPARATOR + mProcessSuffix;
    }

    private WriterInfo getWriterInfo(String fileName) {
        WriterInfo writerInfo = mWriterMap.get(fileName);
        if (writerInfo != null) {
            writerInfo.updateWriteTime();
            return writerInfo;
        }

        LogFileInfo logFileInfo = mLogFileMap.get(fileName);
        if (logFileInfo == null) {
            logFileInfo = new LogFileInfo();
            logFileInfo.mIndex = 0;
            logFileInfo.mLastModified = WriterInfo.now();
            mLogFileMap.put(fileName, logFileInfo);
        }

        File file = new File(getLogPath(fileName, logFileInfo.mIndex));
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new
                    FileOutputStream(file, true)));
            writerInfo = new WriterInfo(writer, file.length());
            mWriterMap.put(fileName, writerInfo);
            return writerInfo;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getLogPath(String fileName, int index) {
        return LogConstants.LOG_DIRECTORY + fileName + LOG_FILE_SEPARATOR + index % LogConstants
                .MAX_FILE_COUNT_NUMBER + LOG_FILE_SUFFIX;
    }

    private void formatLog(String tag, String msg, int level, long threadId, WriterInfo
            writerInfo, BufferedWriter writer) {
        mStringBuilder.append(LOG_HEAD_SEPARATOR).append(now()).append
                (LOG_LEFT_SEPARATOR).append(mPid).append(LOG_MIDDLE_SEPARATOR).append(threadId)
                .append(LOG_RIGHT_SEPARATOR).append(LogConstants.LEVEL_MESSAGE[level]).append
                (LOG_HEAD_SEPARATOR).append(LOG_LEFT_SEPARATOR).append(tag).append
                (LOG_RIGHT_SEPARATOR).append(msg).append('\n');
        try {
            String string = mStringBuilder.toString();
            writer.write(string);
            writerInfo.mFileSize += string.length();
            if (BuildConfig.DEBUG) {
                printSystemLog(tag, msg, level);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "stop write log", e);
        }
        mStringBuilder.setLength(0);
    }

    private String now() {
        mDate.setTime(System.currentTimeMillis());
        return mDateFormat.format(mDate);
    }

    private void printSystemLog(String tag, String log, int level) {
        switch (level) {
            case LogConstants.LEVEL_V:
                Log.v(tag, log);
                break;
            case LogConstants.LEVEL_D:
                Log.d(tag, log);
                break;
            case LogConstants.LEVEL_I:
                Log.i(tag, log);
                break;
            case LogConstants.LEVEL_W:
                Log.w(tag, log);
                break;
            case LogConstants.LEVEL_E:
                Log.e(tag, log);
                break;
            case LogConstants.LEVEL_A:
                Log.e(tag, log);
                break;
        }
    }

    private LogInfoItem fetchLogInfoItem(String filename, String tag, String log, int level, long
            threadId) {
        LogInfoItem logInfoItem = mLogInfoStack.pop();
        if (logInfoItem == null) {
            logInfoItem = new LogInfoItem(filename, tag, log, level, threadId);
        } else {
            logInfoItem.mFileName = filename;
            logInfoItem.mTag = tag;
            logInfoItem.mMsg = log;
            logInfoItem.mLevel = level;
            logInfoItem.mThreadId = threadId;
        }
        return logInfoItem;
    }

    private boolean recycleLogInfoItem(LogInfoItem item) {
        return mLogInfoStack.push(item);
    }

    private void doFlush() {
        for (Map.Entry<String, WriterInfo> itemEntry : mWriterMap.entrySet()) {
            WriterInfo writerItem = itemEntry.getValue();
            flushWriter(writerItem.mWriter);
        }
    }

    private void doQuit() {
        mLogHandler.removeMessages(MESSAGE_LOOP);
        mLogHandler.removeMessages(MESSAGE_LOG);
        for (Map.Entry<String, WriterInfo> itemEntry : mWriterMap.entrySet()) {
            WriterInfo writerItem = itemEntry.getValue();
            closeWriter(writerItem.mWriter);
        }
        mWriterMap.clear();
        if (AndroidVersionUtils.hasJellyBeanMR2()) {
            mLogThread.quitSafely();
        } else {
            mLogThread.quit();
        }
        mLogThread = null;
    }

    void flush() {
        if (mLogHandler != null) {
            mLogHandler.sendEmptyMessage(MESSAGE_FLUSH);
        }
    }

    public void quit() {
        if (mLogHandler != null) {
            mLogHandler.sendEmptyMessage(MESSAGE_QUIT);
            mLogHandler = null;
        }
    }
}
