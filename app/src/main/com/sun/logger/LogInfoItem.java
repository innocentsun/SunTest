package com.sun.logger;

/**
 * Created by sunhzchen on 2017/1/6.
 * 单条日志信息
 */

class LogInfoItem {

    String mFileName;
    String mTag;
    String mMsg;
    int mLevel;
    long mThreadId;

    LogInfoItem(String fileName, String tag, String msg, int level, long threadId) {
        mFileName = fileName;
        mTag = tag;
        mMsg = msg;
        mLevel = level;
        mThreadId = threadId;
    }
}
