package com.sun.logger;

import java.io.BufferedWriter;

/**
 * Created by sunhzchen on 2017/1/6.
 * 每个日志文件对应一个记录信息
 */

class WriterInfo {

    final BufferedWriter mWriter;
    long mLastWriteTime;
    long mFileSize;

    WriterInfo(BufferedWriter writer, long fileSize) {
        mWriter = writer;
        mFileSize = fileSize;
        mLastWriteTime = now();
    }

    static long now() {
        return System.currentTimeMillis();
    }

    void updateWriteTime() {
        mLastWriteTime = now();
    }
}
