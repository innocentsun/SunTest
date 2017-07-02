package com.sun.logger;

/**
 * Created by sunhzchen on 2017/1/6.
 * 每个日志文件信息
 */

class LogFileInfo {
    long mLastModified;  //最近修改时间
    int mIndex;  //文件编号（日志文件过大，需要截断存储，解决文件重名问题）
}
